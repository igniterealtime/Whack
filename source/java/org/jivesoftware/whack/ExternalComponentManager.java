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

import org.xmpp.component.ComponentManager;
import org.xmpp.component.Component;
import org.xmpp.packet.Packet;

import java.util.Map;
import java.util.Hashtable;


/**
 * Implementation of the ComponentManager interface for external components.
 *
 * @author Matt Tucker
 */
public class ExternalComponentManager implements ComponentManager {

    private String domain;
    private int port;
    private String defaultSecretKey;
    private Map<String, String> secretKeys = new Hashtable<String,String>();

    public ExternalComponentManager(String domain) {
        this(domain, 5223);
    }

    public ExternalComponentManager(String domain, int port) {
        this.domain = domain;
        this.port = port;
    }

    /**
     * Sets a secret key for a sub-domain, for future use by a component
     * connecting to the server. Keys are used as an authentication mechanism
     * when connecting to the server. Some servers may require a different
     * key for each component, while others may use a global secret key.
     *
     *
     * @param subdomain
     * @param secretKey
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
     * @param secretKey
     */
    public void setDefaultSecretKey(String secretKey) {
        this.defaultSecretKey = secretKey;
    }

    public void addComponent(String subdomain, Component component) {

    }

    public void removeComponent(String subdomain) {

    }

    public void sendPacket(Component component, Packet packet) {

    }

    public String getProperty(String name) {
        return null;
    }

    public void setProperty(String name, String value) {

    }

    public boolean isExternalMode() {
        return true;
    }

}
