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

import java.util.Iterator;

/**
 * A packet error. Errors must have a type and condition. Optionally, they
 * can include explanation text.
 *
 * @author Matt Tucker
 */
public class PacketError {

    private static DocumentFactory docFactory = DocumentFactory.getInstance();

    private Element element;

    /**
     * Constructs a new PacketError.
     *
     * @param type the error type.
     * @param condition the error condition.
     */
    public PacketError(Type type, Condition condition) {
        this.element = docFactory.createElement("error");
        setType(type);
        setCondition(condition);
    }

    /**
     * Constructs a new PacketError.
     *
     * @param type the error type.
     * @param condition the error condition.
     * @param text the text description of the error.
     */
    public PacketError(Type type, Condition condition, String text) {
        this.element = docFactory.createElement("error");
        setType(type);
        setCondition(condition);
        setText(text);
    }

    /**
     * Constructs a new PacketError using an existing Element. This is useful
     * for parsing incoming error Elements into PacketError objects.
     *
     * @param element the IQ Element.
     */
    public PacketError(Element element) {
        this.element = element;
    }

    /**
     * Returns the error type.
     *
     * @return the error type.
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
     * Sets the error type.
     *
     * @param type the error type.
     * @see Type
     */
    public void setType(Type type) {
        element.addAttribute("type", type==null?null:type.toString());
    }

    /**
     * Returns the error condition.
     *
     * @return the error condition.
     * @see Condition
     */
    public Condition getCondition() {
        for (Iterator i=element.elementIterator(); i.hasNext(); ) {
            Element el = (Element)i.next();
            if (el.getNamespaceURI().equals("urn:ietf:params:xml:ns:xmpp-stanzas") &&
                    !el.getName().equals("text"))
            {
                return Condition.fromString(el.getName());
            }
        }
        return null;
    }

    /**
     * Sets the error condition.
     *
     * @param condition the error condition.
     * @see Condition
     */
    public void setCondition(Condition condition) {
        if (condition == null) {
            throw new NullPointerException("Condition cannot be null");
        }
        Element conditionElement = null;
        for (Iterator i=element.elementIterator(); i.hasNext(); ) {
            Element el = (Element)i.next();
            if (el.getNamespaceURI().equals("urn:ietf:params:xml:ns:xmpp-stanzas") &&
                    !el.getName().equals("text"))
            {
                conditionElement = el;
            }
        }
        if (conditionElement != null) {
            element.remove(conditionElement);
        }

        conditionElement = docFactory.createElement(condition.toString(),
                "urn:ietf:params:xml:ns:xmpp-stanzas");
        element.add(conditionElement);
    }

    /**
     * Returns a text description of the error, or <tt>null</tt> if there
     * is no text description.
     *
     * @return the text description of the error.
     */
    public String getText() {
        return element.elementText("text");
    }

    /**
     * Sets the text description of the error.
     *
     * @param text the text description of the error.
     */
    public void setText(String text) {
        Element textElement = element.element("text");
        // If text is null, clear the text.
        if (text == null) {
            if (textElement != null) {
                element.remove(textElement);
            }
            return;
        }

        if (textElement == null) {
            textElement = docFactory.createElement("text", "urn:ietf:params:xml:ns:xmpp-stanzas");
            element.add(textElement);
        }
        textElement.setText(text);
    }

    /**
     * Returns the DOM4J Element that backs the error. The element is the definitive
     * representation of the error and can be manipulated directly to change
     * error contents.
     *
     * @return the DOM4J Element.
     */
    public Element getElement() {
        return element;
    }

    /**
     * Error condition.
     */
    public static class Condition {

        /**
         * The sender has sent XML that is malformed or that cannot be processed
         * (e.g., an IQ stanza that includes an unrecognized value of the 'type'
         * attribute); the associated error type SHOULD be "modify".
         */
        public static final Condition BAD_REQUEST = new Condition("bad-request");

        /**
         * Access cannot be granted because an existing resource or session
         * exists with the same name or address; the associated error type
         * SHOULD be "cancel".
         */
        public static final Condition CONFLICT = new Condition("conflict");

        /**
         * The feature requested is not implemented by the recipient or
         * server and therefore cannot be processed; the associated error
         * type SHOULD be "cancel".
         */
        public static final Condition FEATURE_NOT_IMPLEMENTED = new Condition(
                "feature-not-implemented");

        /**
         * The requesting entity does not possess the required permissions to
         * perform the action; the associated error type SHOULD be "auth".
         */
        public static final Condition FORBIDDEN = new Condition("forbidden");

        /**
         * The recipient or server can no longer be contacted at this address
         * (the error stanza MAY contain a new address in the XML character
         * data of the <gone/> element); the associated error type SHOULD be
         * "modify".
         */
        public static final Condition GONE = new Condition("gone");

        /**
         * The server could not process the stanza because of a misconfiguration
         * or an otherwise-undefined internal server error; the associated error
         * type SHOULD be "wait".
         */
        public static final Condition INTERNAL_SERVER_ERROR = new Condition(
                "internal-server-error");

        /**
         * The addressed JID or item requested cannot be found; the associated
         * error type SHOULD be "cancel".
         */
        public static final Condition ITEM_NOT_FOUND = new Condition("item-not-found");

        /**
         * The sending entity has provided or communicated an XMPP address
         * (e.g., a value of the 'to' attribute) or aspect thereof (e.g.,
         * a resource identifier) that does not adhere to the syntax defined
         * in Addressing Scheme (Section 3); the associated error type SHOULD
         * be "modify".
         */
        public static final Condition JID_MALFORMED = new Condition("jid-malformed");

        /**
         * The recipient or server understands the request but is refusing
         * to process it because it does not meet criteria defined by the
         * recipient or server (e.g., a local policy regarding acceptable
         * words in messages); the associated error type SHOULD be "modify".
         */
        public static final Condition NOT_ACCEPTABLE = new Condition("not-acceptable");

        /**
         * The recipient or server does not allow any entity to perform
         * the action; the associated error type SHOULD be "cancel".
         */
        public static final Condition NOT_ALLOWED = new Condition("not-allowed");

        /**
         * The sender must provide proper credentials before being allowed
         * to perform the action, or has provided improper credentials;
         * the associated error type SHOULD be "auth".
         */
        public static final Condition NOT_AUTHORIZED = new Condition("not-authorized");

        /**
         * The requesting entity is not authorized to access the requested
         * service because payment is required; the associated error type
         * SHOULD be "auth".
         */
        public static final Condition PAYMENT_REQUIRED = new Condition("payment-required");

        /**
         * The intended recipient is temporarily unavailable; the associated
         * error type SHOULD be "wait" (note: an application MUST NOT return
         * this error if doing so would provide information about the intended
         * recipient's network availability to an entity that is not authorized
         * to know such information).
         */
        public static final Condition RECIPIENT_UNAVAILABLE = new Condition("recipient-unavailable");

        /**
         * The recipient or server is redirecting requests for this
         * information to another entity, usually temporarily (the error
         * stanza SHOULD contain the alternate address, which MUST be a
         * valid JID, in the XML character data of the &lt;redirect/&gt; element);
         * the associated error type SHOULD be "modify".
         */
        public static final Condition REDIRECT = new Condition("redirect");

        /**
         * The requesting entity is not authorized to access the requested
         * service because registration is required; the associated error
         * type SHOULD be "auth".
         */
        public static final Condition REGISTRATION_REQUIRED = new Condition("registration-required");

        /**
         * A remote server or service specified as part or all of the JID
         * of the intended recipient does not exist; the associated error
         * type SHOULD be "cancel".
         */
        public static final Condition REMOTE_SERVER_NOT_FOUND = new Condition(
                "remote-server-not-found");

        /**
         * A remote server or service specified as part or all of the JID of
         * the intended recipient (or required to fulfill a request) could not
         * be contacted within a reasonable amount of time; the associated
         * error type SHOULD be "wait".
         */
        public static final Condition REMOTE_SERVER_TIMEOUT = new Condition(
                "remote-server-timeout");

        /**
         * The server or recipient lacks the system resources necessary to
         * service the request; the associated error type SHOULD be "wait".
         */
        public static final Condition RESOURCE_CONSTRAINT = new Condition("resource-constraint");

        /**
         * The server or recipient does not currently provide the requested
         * service; the associated error type SHOULD be "cancel".
         */
        public static final Condition SERVICE_UNAVAILABLE = new Condition("service-unavailable");

        /**
         * The requesting entity is not authorized to access the requested
         * service because a subscription is required; the associated error
         * type SHOULD be "auth".
         */
        public static final Condition SUBSCRIPTION_REQUIRED = new Condition("subscription-required");

        /**
         * The error condition is not one of those defined by the other
         * conditions in this list; any error type may be associated with
         * this condition, and it SHOULD be used only in conjunction with
         * an application-specific condition.
         */
        public static final Condition UNDEFINED_REQUEST = new Condition("undefined-condition");

        /**
         * The recipient or server understood the request but was not
         * expecting it at this time (e.g., the request was out of order);
         * the associated error type SHOULD be "wait".
         */
        public static final Condition UNEXPECTED_CONDITION = new Condition("unexpected-condition");

        /**
         * Converts a String value into its Condition representation.
         *
         * @param condition the String value.
         * @return the condition corresponding to the String.
         */
        public static Condition fromString(String condition) {
            if (condition == null) {
                throw new NullPointerException();
            }
            condition = condition.toLowerCase();
            if (BAD_REQUEST.toString().equals(condition)) {
                return BAD_REQUEST;
            }
            else if (CONFLICT.toString().equals(condition)) {
                return CONFLICT;
            }
            else if (FEATURE_NOT_IMPLEMENTED.toString().equals(condition)) {
                return FEATURE_NOT_IMPLEMENTED;
            }
            else if (FORBIDDEN.toString().equals(condition)) {
                return FORBIDDEN;
            }
            else if (GONE.toString().equals(condition)) {
                return GONE;
            }
            else if (INTERNAL_SERVER_ERROR.toString().equals(condition)) {
                return INTERNAL_SERVER_ERROR;
            }
            else if (ITEM_NOT_FOUND.toString().equals(condition)) {
                return ITEM_NOT_FOUND;
            }
            else if (JID_MALFORMED.toString().equals(condition)) {
                return JID_MALFORMED;
            }
            else if (NOT_ACCEPTABLE.toString().equals(condition)) {
                return NOT_ACCEPTABLE;
            }
            else if (NOT_ALLOWED.toString().equals(condition)) {
                return NOT_ALLOWED;
            }
            else if (NOT_AUTHORIZED.toString().equals(condition)) {
                return NOT_AUTHORIZED;
            }
            else if (PAYMENT_REQUIRED.toString().equals(condition)) {
                return PAYMENT_REQUIRED;
            }
            else if (RECIPIENT_UNAVAILABLE.toString().equals(condition)) {
                return RECIPIENT_UNAVAILABLE;
            }
            else if (REDIRECT.toString().equals(condition)) {
                return REDIRECT;
            }
            else if (REGISTRATION_REQUIRED.toString().equals(condition)) {
                return REGISTRATION_REQUIRED;
            }
            else if (REMOTE_SERVER_NOT_FOUND.toString().equals(condition)) {
                return REMOTE_SERVER_NOT_FOUND;
            }
            else if (REMOTE_SERVER_TIMEOUT.toString().equals(condition)) {
                return REMOTE_SERVER_TIMEOUT;
            }
            else if (RESOURCE_CONSTRAINT.toString().equals(condition)) {
                return RESOURCE_CONSTRAINT;
            }
            else if (SERVICE_UNAVAILABLE.toString().equals(condition)) {
                return SERVICE_UNAVAILABLE;
            }
            else if (SUBSCRIPTION_REQUIRED.toString().equals(condition)) {
                return SUBSCRIPTION_REQUIRED;
            }
            else if (UNDEFINED_REQUEST.toString().equals(condition)) {
                return UNDEFINED_REQUEST;
            }
            else if (UNEXPECTED_CONDITION.toString().equals(condition)) {
                return UNEXPECTED_CONDITION;
            }
            else {
                throw new IllegalArgumentException("Condition invalid:" + condition);
            }
        }

        private String value;

        private Condition(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    /**
     * Error type. Valid types are:<ul>
     *
     *      <li>Error.Type.CANCEL -- do not retry (the error is unrecoverable).
     *      <li>Error.Type.CONTINUE -- proceed (the condition was only a warning).
     *      <li>Error.Type.MODIFY -- retry after changing the data sent.
     *      <li>Eror.Type.AUTH -- retry after providing credentials.
     *      <li>Error.Type.WAIT -- retry after waiting (the error is temporary).
     * </ul>
     */
    public static class Type {

        /**
         * Do not retry (the error is unrecoverable).
         */
        public static final Type CANCEL = new Type("cancel");

        /**
         * Proceed (the condition was only a warning).
         */
        public static final Type CONTINUE = new Type("continue");

        /**
         * Retry after changing the data sent.
         */
        public static final Type MODIFY = new Type("modify");

        /**
         * Retry after providing credentials.
         */
        public static final Type AUTH = new Type("auth");

        /**
         * Retry after waiting (the error is temporary).
         */
        public static final Type WAIT = new Type("wait");

        /**
         * Converts a String value into its Type representation.
         *
         * @param type the String value.
         * @return the type corresponding to the String.
         */
        public static Type fromString(String type) {
            if (type == null) {
                throw new NullPointerException();
            }
            type = type.toLowerCase();
            if (CANCEL.toString().equals(type)) {
                return CANCEL;
            }
            else if (CONTINUE.toString().equals(type)) {
                return CONTINUE;
            }
            else if (MODIFY.toString().equals(type)) {
                return MODIFY;
            }
            else if (AUTH.toString().equals(type)) {
                return AUTH;
            }
            else if (WAIT.toString().equals(type)) {
                return WAIT;
            }
            else {
                throw new IllegalArgumentException("Type invalid:" + type);
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
