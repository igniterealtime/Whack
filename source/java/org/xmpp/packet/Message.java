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
 * Message packet. A message can be one of several types:
 *
 * <ul>
 *      <li>Message.Type.NORMAL -- (Default) a normal text message used in email like interface.
 *      <li>Message.Type.CHAT -- a typically short text message used in line-by-line chat interfaces.
 *      <li>Message.Type.GROUP_CHAT -- a chat message sent to a groupchat server for group chats.
 *      <li>Message.Type.HEADLINE -- a text message to be displayed in scrolling marquee displays.
 *      <li>Message.Type.ERROR -- indicates a messaging error.
 * </ul>
 *
 * For each message type, different message fields are typically used as follows:
 * <p>
 * <table border="1">
 * <tr><td>&nbsp;</td><td colspan="5"><b>Message type</b></td></tr>
 * <tr><td><i>Field</i></td><td><b>Normal</b></td><td><b>Chat</b></td><td><b>Group Chat</b></td><td><b>Headline</b></td><td><b>Error</b></td></tr>
 * <tr><td><i>subject</i></td> <td>SHOULD</td><td>SHOULD NOT</td><td>SHOULD NOT</td><td>SHOULD NOT</td><td>SHOULD NOT</td></tr>
 * <tr><td><i>thread</i></td>  <td>OPTIONAL</td><td>SHOULD</td><td>OPTIONAL</td><td>OPTIONAL</td><td>SHOULD NOT</td></tr>
 * <tr><td><i>body</i></td>    <td>SHOULD</td><td>SHOULD</td><td>SHOULD</td><td>SHOULD</td><td>SHOULD NOT</td></tr>
 * <tr><td><i>error</i></td>   <td>MUST NOT</td><td>MUST NOT</td><td>MUST NOT</td><td>MUST NOT</td><td>MUST</td></tr>
 * </table>
 */
public class Message extends Packet {

    /**
     * Constructs a new Message.
     */
    public Message() {
        super(docFactory.createDocument().addElement("message"));
    }

     /**
     * Constructs a new Message using an existing Element. This is useful
     * for parsing incoming message Elements into Message objects.
     *
     * @param element the message Element.
     */
    public Message(Element element) {
        super(element);
    }

    /**
     * Returns the type of this message
     *
     * @return the message type.
     * @see Type
     */
    public Type getType() {
        String type = element.attributeValue("type");
        if (type != null) {
            return Type.fromString(type);
        }
        else {
            return null;
        }
    }

    /**
     * Sets the type of this message.
     *
     * @param type the message type.
     * @see Type
     */
    public void setType(Type type) {
        element.addAttribute("type", type==null?null:type.toString());
    }

    /**
     * Returns the subject of this message or <tt>null</tt> if there is no subject..
     *
     * @return the subject.
     */
    public String getSubject() {
        return element.elementText("subject");
    }

    /**
     * Sets the subject of this message.
     *
     * @param subject the subject.
     */
    public void setSubject(String subject) {
        Element subjectElement = element.element("subject");
        // If subject is null, clear the subject.
        if (subject == null && subjectElement != null) {
            element.remove(subjectElement);
            return;
        }

        if (subjectElement == null) {
            subjectElement = element.addElement("subject");
        }
        subjectElement.setText(subject);
    }

    /**
     * Returns the body of this message or <tt>null</tt> if there is no body.
     *
     * @return the body.
     */
    public String getBody() {
        return element.elementText("body");
    }

    /**
     * Sets the body of this message.
     *
     * @param body the body.
     */
    public void setBody(String body) {
        Element bodyElement = element.element("body");
        // If body is null, clear the body.
        if (body == null) {
            if (bodyElement != null) {
                element.remove(bodyElement);
            }
            return;
        }

        if (bodyElement == null) {
            bodyElement = element.addElement("body");
        }
        bodyElement.setText(body);
    }

    /**
     * Returns the thread value of this message, an identifier that is used for
     * tracking a conversation thread ("instant messaging session")
     * between two entities. If the thread is not set, <tt>null</tt> will be
     * returned.
     *
     * @return the thread value.
     */
    public String getThread() {
        return element.elementText("thread");
    }

    /**
     * Sets the thread value of this message, an identifier that is used for
     * tracking a conversation thread ("instant messaging session")
     * between two entities.
     *
     * @param thread thread value.
     */
    public void setThread(String thread) {
        Element threadElement = element.element("thread");
        // If thread is null, clear the thread.
        if (thread == null) {
            if (threadElement != null) {
                element.remove(threadElement);
            }
            return;
        }

        if (threadElement == null) {
            threadElement = element.addElement("thread");
        }
        threadElement.setText(thread);
    }

    /**
     * Represents the type of a message. The types are:
     *
     *  <ul>
     *      <li>Message.Type.NORMAL -- (Default) a normal text message used in email like interface.
     *      <li>Message.Type.CHAT -- a typically short text message used in line-by-line chat interfaces.
     *      <li>Message.Type.GROUP_CHAT -- a chat message sent to a groupchat server for group chats.
     *      <li>Message.Type.HEADLINE -- a text message to be displayed in scrolling marquee displays.
     *      <li>Message.Type.ERROR -- indicates a messaging error.
     * </ul>
     */
    public static class Type {

        /**
         * (Default) a normal text message used in email like interface.
         */
        public static final Type NORMAL = new Type("normal");

        /**
         * Typically short text message used in line-by-line chat interfaces.
         */
        public static final Type CHAT = new Type("chat");

        /**
         * Chat message sent to a groupchat server for group chats.
         */
        public static final Type GROUP_CHAT = new Type("groupchat");

        /**
         * Text message to be displayed in scrolling marquee displays.
         */
        public static final Type HEADLINE = new Type("headline");

        /**
         * Indicates a messaging error.
         */
        public static final Type ERROR = new Type("error");

        /**
         * Converts a String value into its Type representation.
         *
         * @param type the String value.
         * @return the Type corresponding to the String.
         */
        public static Type fromString(String type) {
            // No type attribute means "normal".
            if (type == null) {
                return NORMAL;
            }
            type = type.toLowerCase();
            if (CHAT.toString().equals(type)) {
                return CHAT;
            }
            else if (GROUP_CHAT.toString().equals(type)) {
                return GROUP_CHAT;
            }
            else if (HEADLINE.toString().equals(type)) {
                return HEADLINE;
            }
            else if (ERROR.toString().equals(type)) {
                return ERROR;
            }
            // From the XMPP spec: [if] the application does not understand the
            // value of the 'type' attribute provided, it MUST consider the message
            // to be of type "normal".
            else {
                return NORMAL;
            }
        }

        private String value;

        private Type(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }
}