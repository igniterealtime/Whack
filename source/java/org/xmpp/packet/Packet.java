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

package org.xmpp.packet;

import org.dom4j.Element;
import org.dom4j.DocumentFactory;
import org.dom4j.io.XMLWriter;
import org.dom4j.io.OutputFormat;

import java.io.StringWriter;

/**
 * An XMPP Packet.
 *
 * @author Matt Tucker
 */
public abstract class Packet {

    protected static DocumentFactory docFactory = DocumentFactory.getInstance();

    protected Element element;

    /**
     * Constructs a new Packet.
     *
     * @param element the XML Element that contains the packet contents.
     */
    public Packet(Element element) {
        this.element = element;
    }

    /**
     * Returns the packet ID, or <tt>null</tt> if the packet does not have an ID.
     * Packet ID's are optional, except for IQ packets.
     *
     * @return the packet ID.
     */
    public String getID() {
        return element.attributeValue("id");
    }

    /**
     * Sets the packet ID. Packet ID's are optional, except for IQ packets.
     *
     * @param ID the packet ID.
     */
    public void setID(String ID) {
        element.attribute("id");
        element.addAttribute("id", ID);
    }

    /**
     * Returns the XMPP address (JID) that the packet is addressed to, or <tt>null</tt>
     * if the "to" attribute is not set. The XMPP protocol often makes the "to"
     * attribute optional, so it does not always need to be set.
     *
     * @return the XMPP address (JID) that the packet is addressed to, or <tt>null</tt>
     *      if not set.
     */
    public JID getTo() {
        String to = element.attributeValue("to");
        if (to == null) {
            return null;
        }
        else {
            return new JID(to);
        }
    }

    /**
     * Sets the XMPP address (JID) that the packet is addressed to. The XMPP protocol
     * often makes the "to" attribute optional, so it does not always need to be set.
     *
     * @param to the XMPP address (JID) that the packet is addressed to.
     */
    public void setTo(String to) {
        element.addAttribute("to", to);
    }

    /**
     * Sets the XMPP address (JID) that the packet is address to. The XMPP protocol
     * often makes the "to" attribute optional, so it does not always need to be set.
     *
     * @param to the XMPP address (JID) that the packet is addressed to.
     */
    public void setTo(JID to) {
        setTo(to.toString());
    }

    /**
     * Returns the XMPP address (JID) that the packet is from, or <tt>null</tt>
     * if the "from" attribute is not set. The XMPP protocol often makes the "from"
     * attribute optional, so it does not always need to be set.
     *
     * @return the XMPP address that the packet is from, or <tt>null</tt>
     *      if not set.
     */
    public JID getFrom() {
        String from = element.attributeValue("from");
        if (from == null) {
            return null;
        }
        else {
            return new JID(from);
        }
    }

    /**
     * Sets the XMPP address (JID) that the packet comes from. The XMPP protocol
     * often makes the "from" attribute optional, so it does not always need to be set.
     *
     * @param from the XMPP address (JID) that the packet comes from.
     */
    public void setFrom(String from) {
        element.addAttribute("from", from);
    }

    /**
     * Sets the XMPP address (JID) that the packet comes from. The XMPP protocol
     * often makes the "from" attribute optional, so it does not always need to be set.
     *
     * @param from the XMPP address (JID) that the packet comes from.
     */
    public void setFrom(JID from) {
        element.addAttribute("from", from.toString());
    }

    /**
     * Returns the packet error, or <tt>null</tt> if there is no packet error.
     *
     * @return the packet error.
     */
    public PacketError getError() {
        Element error = element.element("error");
        if (error != null) {
            return new PacketError(element);
        }
        return null;
    }

    /**
     * Sets the packet error. Calling this method will automatically set
     * the packet "type" attribute to "error".
     *
     * @param error the packet error.
     */
    public void setError(PacketError error) {
        if (element == null) {
            throw new NullPointerException("Error cannot be null");
        }
        // Force the packet type to "error".
        element.addAttribute("type", "error");
        // Remove an existing error packet.
        if (element.element("error") != null) {
            element.remove(element.element("error"));
        }
        // Add the error element.
        element.add(error.getElement());
    }

    /**
     * Returns the DOM4J Element that backs the packet. The element is the definitive
     * representation of the packet and can be manipulated directly to change
     * packet contents.
     *
     * @return the DOM4J Element.
     */
    public Element getElement() {
        return element;
    }

    public String toString() {
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter(out, OutputFormat.createPrettyPrint());
        try {
            writer.write(element);
        }
        catch (Exception e) { }
        return out.toString();
    }
}