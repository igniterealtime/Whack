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
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.xmpp.packet.StreamError;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private boolean shutdown = false;

    private Thread keepAliveThread;
    /**
     * Timestamp when the last stanza was sent to the server. This information is used
     * by the keep alive process to only send heartbeats when the connection has been idle.
     */
    private long lastActive = System.currentTimeMillis();

    private String connectionID;
    /**
     * Hold the full domain of this component. The full domain is composed by the subdomain plus
     * the domain of the server. E.g. conference.jivesoftware.com. The domain may change after a
     * connection has been established with the server.
     */
    private String domain;
    /**
     * Holds the subdomain that is associated to this component. The subdomain is the initial part
     * of the domain. The subdomain cannot be affected after establishing a connection with the
     * server. E.g. conference.
     */
    private String subdomain;
    /**
     * Holds the IP address or host name where the connection must be made.
     */
    private String host;
    private int port;

    /**
     * Pool of threads that are available for processing the requests.
     */
    private ThreadPoolExecutor threadPool;
    /**
     * Thread that will read the XML from the socket and ask this component to process the read
     * packets.
     */
    private SocketReadThread readerThread;

    private Map<String, IQResultListener> resultListeners = new ConcurrentHashMap<String, IQResultListener>();

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
     * @param subdomain     the subdomain that this component will be handling.
     * @throws ComponentException if an error happens during the connection and authentication steps.
     */
    public void connect(String host, int port, String subdomain) throws ComponentException {
        try {
            // Open a socket to the server
            this.socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), manager.getConnectTimeout());
            this.domain = subdomain + "." + manager.getServerName();
            this.subdomain = subdomain;
            // Keep these variables that will be used in case a reconnection is required
            this.host= host;
            this.port = port;

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
                if (xpp.getAttributeValue("", "from") != null) {
                    this.domain = xpp.getAttributeValue("", "from");
                }
                xmlSerializer = new XMLWriter(writer);

                // Handshake with the server
                stream = new StringBuilder();
                stream.append("<handshake>");
                stream.append(StringUtils.hash(connectionID + manager.getSecretKey(subdomain)));
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
                    // Everything went fine
                    // Start keep alive thread to send every 30 seconds of inactivity a heart beat
                    int keepAliveInterval = 30000;
                    KeepAliveTask task = new KeepAliveTask(keepAliveInterval);
                    keepAliveThread = new Thread(task);
                    task.setThread(keepAliveThread);
                    keepAliveThread.setDaemon(true);
                    keepAliveThread.setName("Keep Alive of component " + subdomain);
                    keepAliveThread.start();

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
            try { if (socket != null) socket.close(); } catch (IOException e) {}
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
     * Returns the domain provided by this component in the connected server. The domain is
     * composed by the subdomain plus the domain of the server. E.g. conference.jivesoftware.com.
     * The domain may change after a connection has been established with the server.
     *
     * @return the domain provided by this component in the connected server.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Returns the subdomain provided by this component in the connected server. E.g. conference.
     *
     * @return the subdomain provided by this component in the connected server.
     */
    public String getSubdomain() {
        return subdomain;
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
                if (packet instanceof IQ) {
                    IQ iq = (IQ) packet;
                    IQ.Type iqType = iq.getType();
                    if (IQ.Type.result == iqType || IQ.Type.error == iqType) {
                        // The server got an answer to an IQ packet that was sent from the component
                        IQResultListener iqResultListener = resultListeners.remove(iq.getID());
                        if (iqResultListener != null) {
                            try {
                                iqResultListener.receivedAnswer(iq);
                            }
                            catch (Exception e) {
                                 manager.getLog().error("Error processing answer of remote entity", e);
                            }
                            return;
                        }
                    }
                }
                component.processPacket(packet);
            }
        });
    }

    public void send(Packet packet) {
        synchronized (writer) {
            try {
                xmlSerializer.write(packet.getElement());
                xmlSerializer.flush();
                // Keep track of the last time a stanza was sent to the server
                lastActive = System.currentTimeMillis();
            }
            catch (IOException e) {
                // Log the exception
                manager.getLog().error(e);
                if (!shutdown) {
                    // Connection was lost so try to reconnect
                    connectionLost();
                }
            }
        }
    }

    public void initialize(JID jid, ComponentManager componentManager) throws ComponentException {
        component.initialize(jid, componentManager);
    }

    public void start() {
        // Everything went fine so start reading packets from the server
        readerThread = new SocketReadThread(this, reader);
        readerThread.setDaemon(true);
        readerThread.start();
        // Notify the component that it will be notified of new received packets 
        component.start();
    }

    public void shutdown() {
        shutdown = true;
        disconnect();
    }

    private void disconnect() {
        if (readerThread != null) {
            readerThread.shutdown();
        }
        threadPool.shutdown();
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

    /**
     * Notification message that the connection with the server was lost unexpectedly. We will try
     * to reestablish the connection for ever until the connection has been reestablished or this
     * thread has been stopped.
     */
    public void connectionLost() {
        readerThread = null;
        boolean isConnected = false;
        if (!shutdown) {
            // Notify the component that connection was lost so it needs to shutdown. The component is
            // still registered in the local component manager but just not connected to the server
            component.shutdown();
        }
        while (!isConnected && !shutdown) {
            try {
                connect(host, port, subdomain);
                isConnected = true;
                // It may be possible that while a new connection was being established the
                // component was required to shutdown so in this case we need to close the new
                // connection
                if (shutdown) {
                    disconnect();
                }
                else {
                    // Component is back again working so start it up again
                    start();
                }
            } catch (ComponentException e) {
                manager.getLog().error("Error trying to reconnect with the server", e);
                // Wait for 5 seconds until the next retry
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {}
            }
        }
    }

    /**
     * Adds an {@link IQResultListener} that will be invoked when an IQ result is sent to the
     * server itself and is of type result or error. This is a nice way for the server to
     * send IQ packets to other XMPP entities and be waked up when a response is received back.<p>
     *
     * Once an IQ result was received, the listener will be invoked and removed from
     * the list of listeners.
     *
     * @param id the id of the IQ packet being sent from the server to an XMPP entity.
     * @param listener the IQResultListener that will be invoked when an answer is received
     */
    void addIQResultListener(String id, IQResultListener listener) {
        // be generated by the server and simulate like the client sent it. This will let listeners
        // react and be removed from the collection
        resultListeners.put(id, listener);
    }

    /**
     * A TimerTask that keeps connections to the server alive by sending a space
     * character on an interval.
     */
    private class KeepAliveTask implements Runnable {

        private int delay;
        private Thread thread;

        public KeepAliveTask(int delay) {
            this.delay = delay;
        }

        protected void setThread(Thread thread) {
            this.thread = thread;
        }

        public void run() {
            try {
                // Sleep 15 seconds before sending first heartbeat. This will give time to
                // properly finish TLS negotiation and then start sending heartbeats.
                Thread.sleep(15000);
            }
            catch (InterruptedException ie) {
                // Do nothing
            }
            while (!shutdown && keepAliveThread == thread) {
                synchronized (writer) {
                    // Send heartbeat if no packet has been sent to the server for a given time
                    if (System.currentTimeMillis() - lastActive >= delay) {
                        try {
                            writer.write(" ");
                            writer.flush();
                        }
                        catch (IOException e) {
                            // Log the exception
                            manager.getLog().error(e);
                            if (!shutdown) {
                                // Connection was lost so try to reconnect
                                connectionLost();
                            }
                        }
                        catch (Exception e) {
                            // Do nothing
                        }
                    }
                }
                try {
                    // Sleep until we should write the next keep-alive.
                    Thread.sleep(delay);
                }
                catch (InterruptedException ie) {
                    // Do nothing
                }
            }
        }
    }
}
