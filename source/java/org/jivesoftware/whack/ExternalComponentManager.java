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

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.component.ComponentManagerFactory;
import org.xmpp.component.IQResultListener;
import org.xmpp.component.Log;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

/**
 * Implementation of the ComponentManager interface for external components.
 * This implementation follows JEP-0014.
 *
 * @author Matt Tucker
 */
public class ExternalComponentManager implements ComponentManager {

	private static final Logger Logger = LoggerFactory.getLogger(ExternalComponentManager.class);
	
    /**
     * Keeps the IP address or hostname of the server. This value will be used only for creating
     * connections.
     */
    private String host;
    /**
     * Port of the server used for establishing new connections.
     */
    private int port;
    /**
     * Keeps the domain of the XMPP server. The domain may or may not match the host. The domain
     * will be used mainly for the XMPP packets while the host is used mainly for creating
     * connections to the server.
     */
    private String domain;
    /**
     * Timeout to use when trying to connect to the server.
     */
    private int connectTimeout = 2000;
    /**
     * This is a global secret key that will be used during the handshake with the server. If a
     * secret key was not defined for the specific component then the global secret key will be
     * used.
     */
    private String defaultSecretKey;
    /**
     * Keeps the secret keys to use for each subdomain. If a key was not found for a specific
     * subdomain then the global secret key will used for the handshake with the server.
     */
    private Map<String, String> secretKeys = new Hashtable<String,String>();
    /**
     * Holds the settings for whether we will tell the XMPP server that a given component can connect
     * to the same JID multiple times.  This is a custom Openfire extension and will not work
     * with any other XMPP server. Other servers should ignore this setting.
     */
    private Map<String, Boolean> allowMultiple = new Hashtable<String,Boolean>();

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

    @Deprecated
    private Log oldLogger;

    /**
     * Constructs a new ExternalComponentManager that will make connections
     * to the specified XMPP server on the default external component port (5275).
     *
     * @param host the IP address or name of the XMPP server to connect to (e.g. "example.com").
     */
    public ExternalComponentManager(String host) {
        this(host, 5275);
    }

    /**
     * Constructs a new ExternalComponentManager that will make connections to
     * the specified XMPP server on the given port.
     *
     * @param host the IP address or name of the XMPP server to connect to (e.g. "example.com").
     * @param port the port to connect on.
     */
    public ExternalComponentManager(String host, int port) {
        if (host == null) {
            throw new IllegalArgumentException("Host of XMPP server cannot be null");
        }
        this.host = host;
        this.port = port;

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

    /**
     * Returns if we want components to be able to connect multiple times to the same JID.  This is a custom
     * Openfire extension and will not work with any other XMPP server. Other XMPP servers should ignore
     * this extra setting.
     *
     * @param subdomain the sub-domain.
     * @return True or false if we are allowing multiple connections.
     */
    public boolean isMultipleAllowed(String subdomain) {
        Boolean allowed = allowMultiple.get(subdomain);
        return allowed != null && allowed;
    }

    /**
     * Sets whether we will tell the XMPP server that we want multiple components to be able to connect
     * to the same JID.  This is a custom Openfire extension and will not work with any other XMPP server.
     * Other XMPP servers should ignore this extra setting.
     *
     * @param subdomain the sub-domain.
     * @param allowMultiple Set to true if we want to allow multiple connections to same JID.
     */
    public void setMultipleAllowed(String subdomain, boolean allowMultiple) {
        this.allowMultiple.put(subdomain, allowMultiple);
    }

    public void addComponent(String subdomain, Component component) throws ComponentException {
        addComponent(subdomain, component, this.port);
    }

    public void addComponent(String subdomain, Component component, Integer port) throws ComponentException {
        if (componentsByDomain.containsKey(subdomain)) {
            if (componentsByDomain.get(subdomain).getComponent() == component) {
                // Do nothing since the component has already been registered
                return;
            }
            else {
                throw new IllegalArgumentException("Subdomain already in use by another component");
            }
        }
        // Create a wrapping ExternalComponent on the component
        ExternalComponent externalComponent = new ExternalComponent(component, this);
        try {
            // Register the new component
            componentsByDomain.put(subdomain, externalComponent);
            components.put(component, externalComponent);
            // Ask the ExternalComponent to connect with the remote server
            externalComponent.connect(host, port, subdomain);
            // Initialize the component
            JID componentJID = new JID(null, externalComponent.getDomain(), null);
            externalComponent.initialize(componentJID, this);
        }
        catch (ComponentException e) {
            // Unregister the new component
            componentsByDomain.remove(subdomain);
            components.remove(component);
            // Re-throw the exception
            throw e;
        }
        // Ask the external component to start processing incoming packets
        externalComponent.start();
    }

    public void removeComponent(String subdomain) throws ComponentException {
        ExternalComponent externalComponent = componentsByDomain.remove(subdomain);
        if (externalComponent != null) {
            components.remove(externalComponent.getComponent());
            externalComponent.shutdown();
        }
    }

    public void sendPacket(Component component, Packet packet) {
        // Get the ExternalComponent that is wrapping the specified component and ask it to
        // send the packet
        components.get(component).send(packet);
    }

    public IQ query(Component component, IQ packet, long timeout) throws ComponentException {
        final LinkedBlockingQueue<IQ> answer = new LinkedBlockingQueue<IQ>(8);
        ExternalComponent externalComponent = components.get(component);
        externalComponent.addIQResultListener(packet.getID(), new IQResultListener() {
            public void receivedAnswer(IQ packet) {
                answer.offer(packet);
            }

            public void answerTimeout(String packetId) {
                //Do nothing
            }
        }, timeout);
        sendPacket(component, packet);
        IQ reply = null;
        try {
            reply = answer.poll(timeout, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            // Ignore
        }
        return reply;
    }

    public void query(Component component, IQ packet, IQResultListener listener) throws ComponentException {
        ExternalComponent externalComponent = components.get(component);
        // Add listenet with a timeout of 5 minutes to prevent memory leaks
        externalComponent.addIQResultListener(packet.getID(), listener, 300000);
        sendPacket(component, packet);
    }

    public String getProperty(String name) {
        return preferences.get(getPreferencesPrefix() + name, null);
    }

    public void setProperty(String name, String value) {
        preferences.put(getPreferencesPrefix() + name, value);
    }

    private String getPreferencesPrefix() {
        if (preferencesPrefix == null) {
            preferencesPrefix = "whack." + domain + ".";
        }
        return preferencesPrefix;
    }

    /**
     * Sets the domain of the XMPP server. The domain may or may not match the host. The domain
     * will be used mainly for the XMPP packets while the host is used mainly for creating
     * connections to the server.
     *
     * @param domain the domain of the XMPP server.
     */
    public void setServerName(String domain) {
        this.domain = domain;
    }

    /**
     * Returns the domain of the XMPP server where we are connected to or <tt>null</tt> if
     * this value was never configured. When the value is null then the component will
     * register with just its subdomain and we expect the server to accept the component and
     * append its domain to form the JID of the component.
     *
     * @return the domain of the XMPP server or null if never configured.
     */
    public String getServerName() {
        return domain;
    }

    /**
     * Returns the timeout (in milliseconds) to use when trying to connect to the server.
     * The default value is 2 seconds.
     *
     * @return the timeout to use when trying to connect to the server.
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the timeout (in milliseconds) to use when trying to connect to the server.
     * The default value is 2 seconds.
     *
     * @param connectTimeout the timeout, in milliseconds, to use when trying to connect to the server.
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public boolean isExternalMode() {
        return true;
    }

    @Deprecated
    public Log getLog() {
        return oldLogger;
    }

    private void createDummyLogger() {
        this.oldLogger = new Log() {
            public void error(String message) {
            	Logger.error(message);
            }

            public void error(String message, Throwable throwable) {
            	Logger.error(message, throwable);
            }

            public void error(Throwable throwable) {
            	Logger.error("", throwable);
            }

            public void warn(String message) {
                Logger.warn(message);
            }

            public void warn(String message, Throwable throwable) {
                Logger.warn(message, throwable);
            }

            public void warn(Throwable throwable) {
                Logger.warn("", throwable);
            }

            public void info(String message) {
                Logger.info(message);
            }

            public void info(String message, Throwable throwable) {
            	Logger.info(message, throwable);
            }

            public void info(Throwable throwable) {
            	Logger.info("", throwable);
            }

            public void debug(String message) {
                Logger.debug(message);
            }

            public void debug(String message, Throwable throwable) {
            	Logger.debug(message, throwable);
            }

            public void debug(Throwable throwable) {
                Logger.debug("", throwable);
            }
        };
    }
}