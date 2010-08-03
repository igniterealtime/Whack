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

package org.jivesoftware.whack.container;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jivesoftware.util.XMLProperties;
import org.jivesoftware.whack.ExternalComponentManager;
import org.xmpp.component.ComponentManager;

import java.io.File;
import java.io.IOException;

/**
 * Starts the web server and components finder. A bootstrap class that will start the Jetty server
 * for which it requires to receive two parameters when launched. The first parameter is the
 * absolute path to the root folder that contains:
 * <pre><ul>
 *      <li><tt>conf</tt> - folder that holds Whack's configuration file</li>
 *      <li><tt>components</tt> - folder that holds the components' jar files</li>
 *      <li><tt>resources/security</tt> - folder that holds the key stores for the https protocol</li>
 *      <li><tt>webapp</tt> - folder that holds the JSP pages of the admin console</li>
 * </ul></pre>
 * The second parameter is the name of the configuration file that holds Whack's configuration.
 * This file must be located in the <tt>conf</tt> folder under the root folder.
 *
 * @author Gaston Dombiak
 */
public class ServerContainer {

    private static final ServerContainer instance = new ServerContainer();
    private Server jetty;
    private ExternalComponentManager manager;
    private ComponentFinder componentFinder;

    /**
     * True if in setup mode
     */
    private boolean setupMode = true;

    private String homeDir;
    private XMLProperties properties;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage ServerContainer <absolute path to home folder> <config filename>");
            return;
        }
        String homeDir = args[0];
        XMLProperties properties = null;
        try {
            properties = new XMLProperties(homeDir + "/conf/" + args[1]);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        instance.setHomeDirectory(homeDir);
        instance.setProperties(properties);
        instance.start();

    }

    public static ServerContainer getInstance() {
        return instance;
    }

    public String getHomeDirectory() {
        return homeDir;
    }

    void setHomeDirectory(String homeDir) {
        this.homeDir = homeDir;
    }

    public XMLProperties getProperties() {
        return properties;
    }

    void setProperties(XMLProperties properties) {
        this.properties = properties;
    }

    public void start() {
        try {
            jetty = new Server();

            // Configure HTTP socket listener
            boolean plainStarted = false;
            // Setting this property to a not null value will imply that the Jetty server will only
            // accept connect requests to that IP address
            String interfaceName = properties.getProperty("adminConsole.inteface");
            String port = properties.getProperty("adminConsole.port");
            int adminPort = (port == null ? 9090 : Integer.parseInt(port));
            
            SelectChannelConnector connector0 = new SelectChannelConnector();
            connector0.setPort(adminPort);
            if (interfaceName != null) {
                connector0.setHost(interfaceName);
            }
            if (adminPort > 0) {
            	jetty.addConnector(connector0);
                plainStarted = true;
            }

            boolean secureStarted = false;
            String securePortProperty = properties.getProperty("adminConsole.securePort");
            int adminSecurePort = 9091;
            try {
                adminSecurePort = (securePortProperty == null ? 9091 : Integer.parseInt(securePortProperty));
                if (adminSecurePort > 0) {
                	SslSelectChannelConnector listener = new SslSelectChannelConnector();

                    // Get the keystore location. The default location is security/keystore
                    String keyStoreLocation = properties.getProperty("xmpp.socket.ssl.keystore");
                    keyStoreLocation = (keyStoreLocation == null ?
                            "resources" + File.separator + "security" + File.separator +
                            "keystore" :
                            keyStoreLocation);
                    keyStoreLocation = homeDir + File.separator + keyStoreLocation;

                    // Get the keystore password. The default password is "changeit".
                    String keypass = properties.getProperty("xmpp.socket.ssl.keypass");
                    keypass = (keypass == null ? "changeit" : keypass);
                    keypass = keypass.trim();

                    // Get the truststore location; default at security/truststore
                    String trustStoreLocation = properties.getProperty("xmpp.socket.ssl.truststore");
                    trustStoreLocation = (trustStoreLocation == null ?
                            "resources" + File.separator + "security" + File.separator +
                            "truststore" :
                            trustStoreLocation);
                    trustStoreLocation = homeDir + File.separator + trustStoreLocation;

                    // Get the truststore passwprd; default is "changeit".
                    String trustpass = properties.getProperty("xmpp.socket.ssl.trustpass");
                    trustpass = (trustpass == null ? "changeit" : trustpass);
                    trustpass = trustpass.trim();

                    listener.setKeystore(keyStoreLocation);
                    listener.setKeyPassword(keypass);
                    listener.setPassword(keypass);

                    listener.setHost(interfaceName);
                    listener.setPort(adminSecurePort);

                    jetty.addConnector(listener);
                    secureStarted = true;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            if ("true".equals(properties.getProperty("setup"))) {
                setupMode = false;
            }

            // Start the ExternalComponentManager
            String xmppServerHost = properties.getProperty("xmppServer.host");
            port = properties.getProperty("xmppServer.port");
            int xmppServerPort = (port == null ? 10015 : Integer.parseInt(port));
            manager = new ExternalComponentManager(xmppServerHost, xmppServerPort);
            String serverDomain = properties.getProperty("xmppServer.domain");
            if (serverDomain != null) {
                manager.setServerName(serverDomain);
            }
            if (properties.getProperty("xmppServer.defaultSecretKey") != null) {
                manager.setDefaultSecretKey(properties.getProperty("xmppServer.defaultSecretKey"));
            }

            // Add web-app
            WebAppContext webapp = new WebAppContext();
            webapp.setContextPath("/");
            // if this doesn't work, try referencing the web.xml programatically.
            webapp.setResourceBase(homeDir + File.separator + "webapp");
            webapp.setWelcomeFiles(new String[]{"index.jsp"});
            webapp.setParentLoaderPriority(true);
            jetty.setHandler(webapp);            

            // Start the http server
            jetty.start();
            jetty.join();

            if (!plainStarted && !secureStarted) {
                manager.getLog().info("Warning: admin console not started due to configuration settings.");
                System.out.println("Warning: admin console not started due to configuration settings.");
            }
            else if (!plainStarted && secureStarted) {
                manager.getLog().info("Admin console listening at secure port: " + adminSecurePort);
                System.out.println("Admin console listening at secure port: " + adminSecurePort);
            }
            else if (!secureStarted && plainStarted) {
                manager.getLog().info("Admin console listening at port: " + adminPort);
                System.out.println("Admin console listening at port: " + adminPort);
            }
            else {
                String msg = "Admin console listening at:\n" +
                        "  port: " + adminPort + "\n" +
                        "  secure port: " + adminSecurePort;
                manager.getLog().info(msg);
                //System.out.println(msg);
            }

            // Load detected components.
            File componentDir = new File(homeDir, "components");
            componentFinder = new ComponentFinder(this, componentDir);
            componentFinder.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isSetupMode() {
        return setupMode;
    }

    public ComponentManager getManager() {
        return manager;
    }
}
