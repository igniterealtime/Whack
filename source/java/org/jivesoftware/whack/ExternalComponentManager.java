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
import org.xmpp.packet.Packet;
import org.xmpp.packet.JID;

import java.util.Map;
import java.util.Hashtable;
import java.util.prefs.Preferences;

/**
 * Implementation of the ComponentManager interface for external components.
 * This implementation follows JEP-0014.
 *
 * @author Matt Tucker
 */
public class ExternalComponentManager implements ComponentManager {

    private String domain;
    private int port;
    private String defaultSecretKey;
    private Map<String, String> secretKeys = new Hashtable<String,String>();
    Preferences preferences = Preferences.userRoot();
    private String preferencesPrefix;
    private Map<String, Component> components = new Hashtable<String,Component>();

    /**
     * Constructs a new ExternalComponentManager that will make connections
     * to the specified XMPP server on the default port (5222).
     *
     * @param domain the domain of the XMPP server to connect to (e.g. "example.com").
     */
    public ExternalComponentManager(String domain) {
        this(domain, 5222);
    }

    /**
     * Constructs a new ExternalComponentManager that will make connections to
     * the specified XMPP server on the given port.
     *
     * @param domain the domain of the XMPP server to connect to (e.g. "example.com").
     * @param port the port to connect on.
     */
    public ExternalComponentManager(String domain, int port) {
        this.domain = domain;
        this.port = port;
        this.preferencesPrefix = "whack." + domain + ".";
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
        // Find the proper secret key to connect as the subdomain.
        String secretKey = secretKeys.get(subdomain);
        if (secretKey == null) {
            secretKey = defaultSecretKey;
        }
        // TODO: connect over network to domain and register component

        // TODO: actual JID should come from server
        JID componentJID = new JID(null, subdomain + domain, null);

        component.initialize(componentJID, this);
        components.put(subdomain, component);
    }

    public void removeComponent(String subdomain) throws ComponentException {
        Component component = components.remove(subdomain);
        if (component != null) {
            component.shutdown();
            // TODO: shut down network connection
        }
    }

    public void sendPacket(Component component, Packet packet) {

    }

    public String getProperty(String name) {
        return preferences.get(preferencesPrefix + name, null);
    }

    public void setProperty(String name, String value) {
        preferences.put(preferencesPrefix + name, value);
    }

    public boolean isExternalMode() {
        return true;
    }
}