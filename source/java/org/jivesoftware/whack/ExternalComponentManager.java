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

import org.xmpp.component.*;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;
import org.jivesoftware.whack.container.ServerContainer;

import javax.net.SocketFactory;
import java.util.Hashtable;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Implementation of the ComponentManager interface for external components.
 * This implementation follows JEP-0014.
 *
 * @author Matt Tucker
 */
public class ExternalComponentManager implements ComponentManager {

    private String host;
    private int port;
    private String defaultSecretKey;
    private Map<String, String> secretKeys = new Hashtable<String,String>();
    Preferences preferences = Preferences.userRoot();
    private String preferencesPrefix;

    /**
     * Keeps a map that associates a domain with the external component thas is handling the domain.
     */
    private Map<String, ExternalComponent> componentsByDomain = new Hashtable<String,ExternalComponent>();
    /**
     * Keeps a map that associates a component with the wrapping ExternalComponent.
     */
    private Map<Component, ExternalComponent> components  = new Hashtable<Component,ExternalComponent>();

    private Log logger;

    /**
     * Constructs a new ExternalComponentManager that will make connections
     * to the specified XMPP server on the default port (5222).
     *
     * @param host the IP address or name of the XMPP server to connect to (e.g. "example.com").
     */
    public ExternalComponentManager(String host) {
        this(host, 5222);
    }

    /**
     * Constructs a new ExternalComponentManager that will make connections to
     * the specified XMPP server on the given port.
     *
     * @param host the IP address or name of the XMPP server to connect to (e.g. "example.com").
     * @param port the port to connect on.
     */
    public ExternalComponentManager(String host, int port) {
        this.host = host;
        this.port = port;
        this.preferencesPrefix = "whack." + host + ".";

        createDummyLogger();

        // Set this ComponentManager as the current component manager
        ComponentManagerFactory.setComponentManager(this);
    }

    /**
     * Sets a secret key for a sub-domain, for future use by a component
     * connecting to the server. Keys are used as an authentication mechanism
     * when connecting to the server. Some servers may require a different
     * key for each component, while others may use a global secret key.
     *
     * @param subdomain the sub-domain.
     * @param secretKey the secret key
     */
    public void setSecretKey(String subdomain, String secretKey) {
        secretKeys.put(subdomain, secretKey);
    }

    /**
     * Returns the secret key for a sub-domain. If no key was found then the default secret key
     * will be returned.
     *
     * @param subdomain the subdomain to return its secret key.
     * @return the secret key for a sub-domain.
     */
    public String getSecretKey(String subdomain) {
        // Find the proper secret key to connect as the subdomain.
        String secretKey = secretKeys.get(subdomain);
        if (secretKey == null) {
            secretKey = defaultSecretKey;
        }
        return secretKey;
    }

    /**
     * Sets the default secret key, which will be used when connecting if a
     * specific secret key for the component hasn't been sent. Keys are used
     * as an authentication mechanism when connecting to the server. Some servers
     * may require a different key for each component, while others may use
     * a global secret key.
     *
     * @param secretKey the default secret key.
     */
    public void setDefaultSecretKey(String secretKey) {
        this.defaultSecretKey = secretKey;
    }

    public void addComponent(String subdomain, Component component) throws ComponentException {
        if (componentsByDomain.containsKey(subdomain)) {
            if (componentsByDomain.get(subdomain).getComponent() == component) {
                // Do nothing since the component has already been registered
                return;
            }
            else {
                throw new IllegalArgumentException("Subdomain already in use by another component");
            }
        }
        // Find the proper secret key to connect as the subdomain.
        String secretKey = secretKeys.get(subdomain);
        if (secretKey == null) {
            secretKey = defaultSecretKey;
        }
        // Create a wrapping ExternalComponent on the component
        ExternalComponent externalComponent = new ExternalComponent(component, this);
        try {
            // Register the new component
            componentsByDomain.put(subdomain, externalComponent);
            components.put(component, externalComponent);
            // Ask the ExternalComponent to connect with the remote server
            externalComponent.connect(host, port, SocketFactory.getDefault(), subdomain);
        } catch (ComponentException e) {
            // Unregister the new component
            componentsByDomain.put(subdomain, externalComponent);
            components.put(component, externalComponent);
            // Re-throw the exception
            throw e;
        }
        // Initialize the component
        JID componentJID = new JID(null, externalComponent.getDomain(), null);
        externalComponent.initialize(componentJID, this);
        // Asl the external component to start processing incoming packets
        externalComponent.start();
    }

    public void removeComponent(String subdomain) throws ComponentException {
        ExternalComponent externalComponent = componentsByDomain.remove(subdomain);
        components.remove(externalComponent.getComponent());
        if (externalComponent != null) {
            externalComponent.shutdown();
        }
    }

    public void sendPacket(Component component, Packet packet) {
        // Get the ExternalComponent that is wrapping the specified component and ask it to
        // send the packet
        components.get(component).send(packet);
    }

    public String getProperty(String name) {
        return preferences.get(preferencesPrefix + name, null);
    }

    public void setProperty(String name, String value) {
        preferences.put(preferencesPrefix + name, value);
    }

    public String getServerName() {
        // We are assuming here that the host name (or IP address) matches the name of the
        // XMPP server
        return host;
    }

    public boolean isExternalMode() {
        return true;
    }

    /**
     * Returns the location of the <code>home</code> directory.
     *
     * @return the location of the home directory.
     */
    public String getHomeDirectory() {
        return ServerContainer.getInstance().getHomeDirectory();
    }

    public Log getLog() {
        return logger;
    }

    private void createDummyLogger() {
        this.logger = new Log() {
            public void error(String message) {
                System.out.println(message);
            }

            public void error(String message, Throwable throwable) {
                System.err.println(message);
                throwable.printStackTrace();
            }

            public void error(Throwable throwable) {
                throwable.printStackTrace();
            }

            public void warn(String message) {
                System.out.println(message);
            }

            public void warn(String message, Throwable throwable) {
                System.out.println(message);
                throwable.printStackTrace();
            }

            public void warn(Throwable throwable) {
                throwable.printStackTrace();
            }

            public void info(String message) {
                System.out.println(message);
            }

            public void info(String message, Throwable throwable) {
                System.out.println(message);
                throwable.printStackTrace();
            }

            public void info(Throwable throwable) {
                throwable.printStackTrace();
            }

            public void debug(String message) {
                System.out.println(message);
            }

            public void debug(String message, Throwable throwable) {
                System.out.println(message);
                throwable.printStackTrace();
            }

            public void debug(Throwable throwable) {
                throwable.printStackTrace();
            }
        };
    }
}