/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2004 Jive Software.
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

package org.xmpp.component;

import org.xmpp.packet.Packet;
import org.xmpp.packet.JID;

/**
 * A component, which enhances the functionality of an XMPP server.
 *
 * Components are JavaBeans and will have their properties exposed as ad-hoc commands.
 *
 * @author Matt Tucker
 */
public interface Component {

    /**
     * Returns the name of this component.
     *
     * @return the name of this component.
     */
    public String getName();

    /**
     * Returns the description of this component.
     *
     * @return the description of this component.
     */
    public String getDescription();

    /**
     * Processes a packet sent to this Component.
     *
     * @param packet the packet.
     * @see ComponentManager#sendPacket(Packet)
     */
    public void processPacket(Packet packet);

    /**
     * Initializes this component with a ComponentManager and the JID
     * that this component is available at (e.g. <tt>service.example.com</tt>).
     * After being initialized, this Component must be ready to process
     * incoming packets.
     *
     * @param jid the XMPP address that this component is available at.
     * @param componentManager the component manager.
     */
    public void initialize(JID jid, ComponentManager componentManager);

    /**
     * Shuts down this component. All component resources must be released as
     * part of shutdown.
     */
    public void shutdown();
}