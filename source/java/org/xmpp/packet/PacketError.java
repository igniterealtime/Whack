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
            return Type.fromXMPP(type);
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
        element.addAttribute("type", type==null?null:type.toXMPP());
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
                return Condition.fromXMPP(el.getName());
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

        conditionElement = docFactory.createElement(condition.toXMPP(),
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
     * Type-safe enumeration for the error condition.<p>
     *
     * Implementation note: XMPP error conditions use "-" characters in
     * their names such as "bad-request". Because "-" characters are not valid
     * identifier parts in Java, they have been converted to "_" characters in
     * the  enumeration names, such as <tt>bad_request</tt>. The {@link #toXMPP()} and
     * {@link #fromXMPP(String)} methods can be used to convert between the
     * enumertation values and XMPP error code strings.
     */
    public enum Condition {

        /**
         * The sender has sent XML that is malformed or that cannot be processed
         * (e.g., an IQ stanza that includes an unrecognized value of the 'type'
         * attribute); the associated error type SHOULD be "modify".
         */
        bad_request("bad-request"),

        /**
         * Access cannot be granted because an existing resource or session
         * exists with the same name or address; the associated error type
         * SHOULD be "cancel".
         */
        conflict("conflict"),

        /**
         * The feature requested is not implemented by the recipient or
         * server and therefore cannot be processed; the associated error
         * type SHOULD be "cancel".
         */
        feature_not_implemented("feature-not-implemented"),

        /**
         * The requesting entity does not possess the required permissions to
         * perform the action; the associated error type SHOULD be "auth".
         */
        forbidden("forbidden"),

        /**
         * The recipient or server can no longer be contacted at this address
         * (the error stanza MAY contain a new address in the XML character
         * data of the <gone/> element); the associated error type SHOULD be
         * "modify".
         */
        gone("gone"),

        /**
         * The server could not process the stanza because of a misconfiguration
         * or an otherwise-undefined internal server error; the associated error
         * type SHOULD be "wait".
         */
        internal_server_error("internal-server-error"),

        /**
         * The addressed JID or item requested cannot be found; the associated
         * error type SHOULD be "cancel".
         */
        item_not_found("item-not-found"),

        /**
         * The sending entity has provided or communicated an XMPP address
         * (e.g., a value of the 'to' attribute) or aspect thereof (e.g.,
         * a resource identifier) that does not adhere to the syntax defined
         * in Addressing Scheme (Section 3); the associated error type SHOULD
         * be "modify".
         */
        jid_malformed("jid-malformed"),

        /**
         * The recipient or server understands the request but is refusing
         * to process it because it does not meet criteria defined by the
         * recipient or server (e.g., a local policy regarding acceptable
         * words in messages); the associated error type SHOULD be "modify".
         */
        not_acceptable("not-acceptable"),

        /**
         * The recipient or server does not allow any entity to perform
         * the action; the associated error type SHOULD be "cancel".
         */
        not_allowed("not-allowed"),

        /**
         * The sender must provide proper credentials before being allowed
         * to perform the action, or has provided improper credentials;
         * the associated error type SHOULD be "auth".
         */
        not_authorized("not-authorized"),

        /**
         * The requesting entity is not authorized to access the requested
         * service because payment is required; the associated error type
         * SHOULD be "auth".
         */
        payment_required("payment-required"),

        /**
         * The intended recipient is temporarily unavailable; the associated
         * error type SHOULD be "wait" (note: an application MUST NOT return
         * this error if doing so would provide information about the intended
         * recipient's network availability to an entity that is not authorized
         * to know such information).
         */
        recipient_unavailable("recipient-unavailable"),

        /**
         * The recipient or server is redirecting requests for this
         * information to another entity, usually temporarily (the error
         * stanza SHOULD contain the alternate address, which MUST be a
         * valid JID, in the XML character data of the &lt;redirect/&gt; element);
         * the associated error type SHOULD be "modify".
         */
        redirect("redirect"),

        /**
         * The requesting entity is not authorized to access the requested
         * service because registration is required; the associated error
         * type SHOULD be "auth".
         */
        registration_required("registration-required"),

        /**
         * A remote server or service specified as part or all of the JID
         * of the intended recipient does not exist; the associated error
         * type SHOULD be "cancel".
         */
        remote_server_not_found("remote-server-not-found"),

        /**
         * A remote server or service specified as part or all of the JID of
         * the intended recipient (or required to fulfill a request) could not
         * be contacted within a reasonable amount of time; the associated
         * error type SHOULD be "wait".
         */
        remote_server_timeout("remote-server-timeout"),

        /**
         * The server or recipient lacks the system resources necessary to
         * service the request; the associated error type SHOULD be "wait".
         */
        resource_constraint("resource-constraint"),

        /**
         * The server or recipient does not currently provide the requested
         * service; the associated error type SHOULD be "cancel".
         */
        service_unavailable("service-unavailable"),

        /**
         * The requesting entity is not authorized to access the requested
         * service because a subscription is required; the associated error
         * type SHOULD be "auth".
         */
        subscription_required("subscription-required"),

        /**
         * The error condition is not one of those defined by the other
         * conditions in this list; any error type may be associated with
         * this condition, and it SHOULD be used only in conjunction with
         * an application-specific condition.
         */
        undefined_condition("undefined-condition"),

        /**
         * The recipient or server understood the request but was not
         * expecting it at this time (e.g., the request was out of order);
         * the associated error type SHOULD be "wait".
         */
        unexpected_condition("unexpected-condition");

        /**
         * Converts a String value into its Condition representation.
         *
         * @param condition the String value.
         * @return the condition corresponding to the String.
         */
        public static Condition fromXMPP(String condition) {
            if (condition == null) {
                throw new NullPointerException();
            }
            condition = condition.toLowerCase();
            if (bad_request.toXMPP().equals(condition)) {
                return bad_request;
            }
            else if (conflict.toXMPP().equals(condition)) {
                return conflict;
            }
            else if (feature_not_implemented.toXMPP().equals(condition)) {
                return feature_not_implemented;
            }
            else if (forbidden.toXMPP().equals(condition)) {
                return forbidden;
            }
            else if (gone.toXMPP().equals(condition)) {
                return gone;
            }
            else if (internal_server_error.toXMPP().equals(condition)) {
                return internal_server_error;
            }
            else if (item_not_found.toXMPP().equals(condition)) {
                return item_not_found;
            }
            else if (jid_malformed.toXMPP().equals(condition)) {
                return jid_malformed;
            }
            else if (not_acceptable.toXMPP().equals(condition)) {
                return not_acceptable;
            }
            else if (not_allowed.toXMPP().equals(condition)) {
                return not_allowed;
            }
            else if (not_authorized.toXMPP().equals(condition)) {
                return not_authorized;
            }
            else if (payment_required.toXMPP().equals(condition)) {
                return payment_required;
            }
            else if (recipient_unavailable.toXMPP().equals(condition)) {
                return recipient_unavailable;
            }
            else if (redirect.toXMPP().equals(condition)) {
                return redirect;
            }
            else if (registration_required.toXMPP().equals(condition)) {
                return registration_required;
            }
            else if (remote_server_not_found.toXMPP().equals(condition)) {
                return remote_server_not_found;
            }
            else if (remote_server_timeout.toXMPP().equals(condition)) {
                return remote_server_timeout;
            }
            else if (resource_constraint.toXMPP().equals(condition)) {
                return resource_constraint;
            }
            else if (service_unavailable.toXMPP().equals(condition)) {
                return service_unavailable;
            }
            else if (subscription_required.toXMPP().equals(condition)) {
                return subscription_required;
            }
            else if (undefined_condition.toXMPP().equals(condition)) {
                return undefined_condition;
            }
            else if (unexpected_condition.toXMPP().equals(condition)) {
                return unexpected_condition;
            }
            else {
                throw new IllegalArgumentException("Condition invalid:" + condition);
            }
        }

        private String value;

        private Condition(String value) {
            this.value = value;
        }

        /**
         * Returns the error code as a valid XMPP error code string.
         *
         * @return the XMPP error code value.
         */
        public String toXMPP() {
            return value;
        }
    }

    /**
     * Error type. Valid types are:<ul>
     *
     *      <li>{@link #cancel Error.Type.cancel} -- do not retry (the error is unrecoverable).
     *      <li>{@link #continue_processing Error.Type.continue_processing}  -- proceed
     *          (the condition was only a warning). Equivalent to the XMPP error type
     *          "continue".
     *      <li>{@link #modify Error.Type.modify} -- retry after changing the data sent.
     *      <li>{@link #auth Eror.Type.auth} -- retry after providing credentials.
     *      <li>{@link #wait Error.Type.wait} -- retry after waiting (the error is temporary).
     * </ul>
     *
     * Implementation note: one of the XMPP error types is "continue". Because "continue"
     * is a reserved Java keyword, the enum name is <tt>continue_processing</tt>. The
     * {@link #toXMPP()} and {@link #fromXMPP(String)} methods can be used to convert
     * between the enumertation values and XMPP error type strings.
     */
    public enum Type {

        /**
         * Do not retry (the error is unrecoverable).
         */
        cancel("cancel"),

        /**
         * Proceed (the condition was only a warning). This represents
         * the "continue" error code in XMPP; because "continue" is a
         * reserved keyword in Java the enum name has been changed.
         */
        continue_processing("continue"),

        /**
         * Retry after changing the data sent.
         */
        modify("modify"),

        /**
         * Retry after providing credentials.
         */
        auth("auth"),

        /**
         * Retry after waiting (the error is temporary).
         */
        wait("wait");

        /**
         * Converts a String value into its Type representation.
         *
         * @param type the String value.
         * @return the condition corresponding to the String.
         */
        public static Type fromXMPP(String type) {
            if (type == null) {
                throw new NullPointerException();
            }
            type = type.toLowerCase();
            if (cancel.toXMPP().equals(type)) {
                return cancel;
            }
            else if (continue_processing.toXMPP().equals(type)) {
                return continue_processing;
            }
            else if (modify.toXMPP().equals(type)) {
                return modify;
            }
            else if (auth.toXMPP().equals(type)) {
                return auth;
            }
            else if (wait.toXMPP().equals(type)) {
                return wait;
            }
            else {
                throw new IllegalArgumentException("Type invalid:" + type);
            }
        }

        private String value;

        private Type(String value) {
            this.value = value;
        }

        /**
         * Returns the error code as a valid XMPP error code string.
         *
         * @return the XMPP error code value.
         */
        public String toXMPP() {
            return value;
        }
    }
}