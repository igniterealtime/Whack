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

/**
 * Presence packet. Presence packets are used to express an entity's current
 * network availability and to notify other entities of that availability.
 * Presence packets are also used to negotiate and manage subscriptions to the
 * presence of other entities.<p>
 *
 * A presence optionally has a {@link Type}.
 *
 * @author Matt Tucker
 */
public class Presence extends Packet {

    /**
     * Constructs a new Presence.
     */
    public Presence() {
        super(docFactory.createDocument().addElement("presence"));
    }

    /**
     * Constructs a new Presence using an existing Element. This is useful
     * for parsing incoming presence Elements into Presence objects.
     *
     * @param element the presence Element.
     */
    public Presence(Element element) {
        super(element);
    }

    /**
     * Returns the type of this presence.
     *
     * @return the presence type.
     * @see Type
     */
    public Type getType() {
        String type = element.attributeValue("type");
        return Type.fromString(type);
    }

    /**
     * Sets the type of this presence.
     *
     * @param type the presence type.
     * @see Type
     */
    public void setType(Type type) {
        if (type == Type.AVAILABLE) {
            element.addAttribute("type", null);
        }
        else {
            element.addAttribute("type", type==null?null:type.toString());
        }
    }

    /**
     * Returns the presence "show" value, which specifies a particular availability
     * status. If the &lt;show&gt; element is not present, this method will return
     * <tt>null</tt>. The show value can only be set if the presence type is
     * {@link Type#AVAILABLE}.
     *
     * @return the presence show value..
     * @see Show
     */
    public Show getShow() {
        return Show.fromString(element.elementText("show"));
    }

    /**
     * Sets the presence "show" value, which specifies a particular availability
     * status. The show value can only be set if the presence type is
     * {@link Type#AVAILABLE}.
     *
     * @param show the presence show value.
     * @throws IllegalArgumentException if the presence type is not {@link Type#AVAILABLE};
     * @see Show
     */
    public void setShow(Show show) {
        Element showElement = element.element("show");
        // If show is null, clear the subject.
        if (show == null && showElement != null) {
            element.remove(showElement);
            return;
        }
        if (showElement == null) {
            if (getType() != Type.AVAILABLE) {
                throw new IllegalArgumentException("Cannot set 'show' if 'type' attribute is set.");
            }
            showElement = element.addElement("show");
        }
        showElement.setText(show.toString());
    }

    /**
     * Returns the status of this presence packet, a natural-language description
     * of availability status.
     *
     * @return the status.
     */
    public String getStatus() {
        return element.elementText("status");
    }

    /**
     * Sets the status of this presence packet, a natural-language description
     * of availability status.
     *
     * @param status the status.
     */
    public void setStatus(String status) {
        Element statusElement = element.element("status");
        // If subject is null, clear the subject.
        if (status == null && statusElement != null) {
            element.remove(statusElement);
            return;
        }

        if (statusElement == null) {
            statusElement = element.addElement("status");
        }
        statusElement.setText(status);
    }

    /**
     * Returns the priority. The valid priority range is -128 through 128.
     * If no priority element exists in the packet, this method will return
     * the default value of 0.
     *
     * @return the priority.
     */
    public int getPriority() {
        String priority = element.elementText("priority");
        if (priority == null) {
            return 0;
        }
        else {
            try {
                return Integer.parseInt(priority);
            }
            catch (Exception e) {
                return 0;
            }
        }
    }

    /**
     * Sets the priority. The valid priority range is -128 through 128.
     *
     * @param priority the priority.
     * @throws IllegalArgumentException if the priority is less than -128 or greater
     *      than 128.
     */
    public void setPriority(int priority) {
        if (priority < -128 || priority > 128) {
            throw new IllegalArgumentException("Priority value of " + priority +
                    " is outside the valid range of -128 through 128");
        }
        Element priorityElement = element.element("priority");
        if (priorityElement == null) {
            priorityElement = element.addElement("priority");
        }
        priorityElement.setText(Integer.toString(priority));
    }

    /**
     * Represents the type of a presence packet. The types are:
     *
     *  <ul>
     *      <li>Presence.Type.AVAILABLE -- (default) the sender is available. Note: the
     *          available type is assumed whenever the type attribute of the packet is
     *          <tt>null</tt> is assumed.
     *      <li>Presence.Type.UNAVAILABLE -- signals that the entity is no
     *          longer available for communication.
     *      <li>Presence.Type.SUBSCRIBE -- the sender wishes to subscribe to the
     *          recipient's presence.
     *      <li>Presence.Type.SUBSCRIBED -- the sender has allowed the recipient to
     *          receive their presence.
     *      <li>Presence.Type.UNSUBSCRIBE -- the sender is unsubscribing from
     *          another entity's presence.
     *      <li>Presence.Type.UNSUBSCRIBED -- the subscription request has been
     *          denied or a previously-granted subscription has been cancelled.
     *      <li>Presence.Type.PROBE -- a request for an entity's current presence; SHOULD be
     *          generated only by a server on behalf of a user.
     *      <li>Presence.Type.ERROR -- an error has occurred regarding processing or delivery
     *          of a previously-sent presence stanza.
     * </ul>
     */
    public static class Type {

        /**
         * (Default) the sender is available. Note: the available type is assumed
         * whenever the type attribute of the packet is <tt>null</tt> is assumed.
         */
        public static final Type AVAILABLE = new Type("");

        /**
         * Typically short text message used in line-by-line chat interfaces.
         */
        public static final Type UNAVAILABLE = new Type("unavailable");

        /**
         * The sender wishes to subscribe to the recipient's presence.
         */
        public static final Type SUBSCRIBE = new Type("subscribe");

        /**
         * The sender has allowed the recipient to receive their presence.
         */
        public static final Type SUBSCRIBED = new Type("subscribed");

        /**
         * The sender is unsubscribing from another entity's presence.
         */
        public static final Type UNSUBSCRIBE = new Type("unsubscribe");

        /**
         * The subscription request has been denied or a previously-granted
         * subscription has been cancelled.
         */
        public static final Type UNSUBSCRIBED = new Type("unsubscribed");

        /**
         * A request for an entity's current presence; SHOULD be
         * generated only by a server on behalf of a user.
         */
        public static final Type PROBE = new Type("probe");

        /**
         * An error has occurred regarding processing or delivery
         * of a previously-sent presence stanza.
         */
        public static final Type ERROR = new Type("error");

        /**
         * Converts a String value into its Type representation.
         *
         * @param type the String value.
         * @return the Type corresponding to the String.
         */
        public static Type fromString(String type) {
            if (type == null) {
                return AVAILABLE;
            }
            type = type.toLowerCase();
            if (UNAVAILABLE.toString().equals(type)) {
                return UNAVAILABLE;
            }
            else if (SUBSCRIBE.toString().equals(type)) {
                return SUBSCRIBE;
            }
            else if (SUBSCRIBED.toString().equals(type)) {
                return SUBSCRIBED;
            }
            else if (UNSUBSCRIBE.toString().equals(type)) {
                return UNSUBSCRIBED;
            }
            else if (PROBE.toString().equals(type)) {
                return PROBE;
            }
            else if (ERROR.toString().equals(type)) {
                return ERROR;
            }
            return null;
        }

        private String value;

        private Type(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    /**
     * Represents the presence "show" value.
     */
    public static class Show {

        public static final Show CHAT = new Show("chat");
        public static final Show AWAY =  new Show("away");
        public static final Show EXTENDED_AWAY = new Show("xa");
        public static final Show DO_NOT_DISTURB = new Show("dnd");

        private String value;

        private Show(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        /**
         * Returns the constant associated with the String value.
         */
        public static Show fromString(String value) {
            if (value == null) {
                return null;
            }
            value = value.toLowerCase();
            if (value.equals("chat")) {
                return CHAT;
            }
            else if (value.equals("away")) {
                return AWAY;
            }
            else if (value.equals("xa")) {
                return EXTENDED_AWAY;
            }
            else if (value.equals("dnd")) {
                return DO_NOT_DISTURB;
            }
            else {
                return null;
            }
        }
    }
}