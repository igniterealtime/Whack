/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2005 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.whack;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.XPPPacketReader;
import org.jivesoftware.whack.util.StringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.StreamError;

import javax.net.SocketFactory;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ExternalComponents are responsible for connecting and authenticating with a remote server and
 * for sending and processing received packets. In fact, an ExternalComponent is a wrapper on a
 * Component that provides remote connection capabilities. The actual processing of the packets is
 * done by the wrapped Component.
 *
 * @author Gaston Dombiak
 */
public class ExternalComponent implements Component {
    
    /**
     * The utf-8 charset for decoding and encoding XMPP packet streams.
     */
    private static String CHARSET = "UTF-8";

    private Component component;
    private ExternalComponentManager manager;

    private Socket socket;
    private XMLWriter xmlSerializer;
    private XmlPullParserFactory factory = null;
    private XPPPacketReader reader = null;
    private Writer writer = null;

    private String connectionID;
    private String domain;

    /**
     * Pool of threads that are available for processing the requests.
     */
    private ThreadPoolExecutor threadPool;

    public ExternalComponent(Component component, ExternalComponentManager manager) {
        // Be default create a pool of 25 threads to process the received requests
        this(component, manager, 25);
    }

    public ExternalComponent(Component component, ExternalComponentManager manager, int maxThreads) {
        this.component = component;
        this.manager = manager;

        // Create a pool of threads that will process requests received by this component. If more
        // threads are required then the command will be executed on the SocketReadThread process
        threadPool = new ThreadPoolExecutor(1, maxThreads, 15, TimeUnit.SECONDS,
                        new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Generates a connection with the server and tries to authenticate. If an error occurs in any
     * of the steps then a ComponentException is thrown.
     *
     * @param host          the host to connect with.
     * @param port          the port to use.
     * @param socketFactory SocketFactory to be used for generating the socket.
     * @param domain        the domain that this component will be handling.
     * @throws ComponentException if an error happens during the connection and authentication steps.
     */
    public void connect(String host, int port, SocketFactory socketFactory, String domain)
            throws ComponentException {
        try {
            // Open a socket to the server
            this.socket = socketFactory.createSocket(host, port);
            this.domain = domain;

            try {
                factory = XmlPullParserFactory.newInstance();
                reader = new XPPPacketReader();
                reader.setXPPFactory(factory);

                reader.getXPPParser().setInput(new InputStreamReader(socket.getInputStream(),
                        CHARSET));

                // Get a writer for sending the open stream tag
                writer =
                        new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),
                                CHARSET));
                // Open the stream.
                StringBuilder stream = new StringBuilder();
                stream.append("<stream:stream");
                stream.append(" xmlns=\"jabber:component:accept\"");
                stream.append(" xmlns:stream=\"http://etherx.jabber.org/streams\"");
                stream.append(" to=\"" + domain + "\">");
                writer.write(stream.toString());
                writer.flush();
                stream = null;

                // Get the answer from the server
                XmlPullParser xpp = reader.getXPPParser();
                for (int eventType = xpp.getEventType(); eventType != XmlPullParser.START_TAG;) {
                    eventType = xpp.next();
                }

                // Set the streamID returned from the server
                connectionID = xpp.getAttributeValue("", "id");
                xmlSerializer = new XMLWriter(writer);

                // Handshake with the server
                stream = new StringBuilder();
                stream.append("<handshake>");
                stream.append(StringUtils.hash(connectionID + manager.getSecretKey(domain)));
                stream.append("</handshake>");
                writer.write(stream.toString());
                writer.flush();
                stream = null;

                // Get the answer from the server
                try {
                    Element doc = reader.parseDocument().getRootElement();
                    if ("error".equals(doc.getName())) {
                        StreamError error = new StreamError(doc);
                        // Close the connection
                        socket.close();
                        socket = null;
                        // throw the exception with the wrapped error
                        throw new ComponentException(error);
                    }

                    // Everything went fine so start reading packets from the server
                    SocketReadThread readerThread = new SocketReadThread(this, reader);
                    readerThread.setDaemon(true);
                    readerThread.start();

                } catch (DocumentException e) {
                    try { socket.close(); } catch (IOException ioe) {}
                    throw new ComponentException(e);
                } catch (XmlPullParserException e) {
                    try { socket.close(); } catch (IOException ioe) {}
                    throw new ComponentException(e);
                }
            } catch (XmlPullParserException e) {
                try { socket.close(); } catch (IOException ioe) {}
                throw new ComponentException(e);
            }
        }
        catch (UnknownHostException uhe) {
            try { socket.close(); } catch (IOException e) {}
            throw new ComponentException(uhe);
        }
        catch (IOException ioe) {
            try { if (socket != null) socket.close(); } catch (IOException e) {}
            throw new ComponentException(ioe);
        }
    }

    public Component getComponent() {
        return component;
    }

    public String getName() {
        return component.getName();
    }

    public String getDescription() {
        return component.getDescription();
    }

    /**
     * Returns the ComponentManager that created this component.
     *
     * @return the ComponentManager that created this component.
     */
    ExternalComponentManager getManager() {
        return manager;
    }

    public void processPacket(final Packet packet) {
        threadPool.execute(new Runnable() {
            public void run() {
                component.processPacket(packet);
            }
        });
    }

    public void send(Packet packet) {
        synchronized (writer) {
            try {
                xmlSerializer.write(packet.getElement());
                xmlSerializer.flush();
            }
            catch (IOException e) {
                manager.getLog().error(e);
                try {
                    manager.removeComponent(domain);
                } catch (ComponentException e1) {
                    manager.getLog().error(e);
                }
            }
        }
    }

    public void initialize(JID jid, ComponentManager componentManager) {
        component.initialize(jid, componentManager);
    }

    public void shutdown() {
        threadPool.shutdown();
        // TODO Stop the SocketReadThread
        if (socket != null && !socket.isClosed()) {
            try {
                synchronized (writer) {
                    try {
                        writer.write("</stream:stream>");
                        xmlSerializer.flush();
                    }
                    catch (IOException e) {}
                }
            }
            catch (Exception e) {
                // Do nothing
            }
            try {
                socket.close();
            }
            catch (Exception e) {
                manager.getLog().error(e);
            }
        }
    }
}
