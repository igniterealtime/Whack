package org.xmpp.packet;

import org.dom4j.Element;

import java.util.*;

/**
 * IQ (Info/Query) packet. IQ packets are used to get and set information
 * on the server, including authentication, roster operations, and creating
 * accounts. Each IQ packet has a specific type that indicates what type of action
 * is being taken: "get", "set", "result", or "error".<p>
 *
 * IQ packets can contain a single child element that exists in a extended XML
 * namespace.
 */
public class IQ extends Packet {

    // Sequence and random number generator used for creating unique ID's.
    private static int sequence = 0;
    private static Random random = new Random();

    /**
     * Constructs a new IQ with an automatically generated ID.
     */
    public IQ() {
        super(docFactory.createDocument().addElement("iq"));
        String id = String.valueOf(random.nextInt(1000) + "-" + sequence++);
        setID(id);
    }

    /**
     * Constructs a new IQ.
     *
     * @param ID the packet ID of the IQ.
     */
    public IQ(String ID) {
        super(docFactory.createDocument().addElement("iq"));
        setID(ID);
    }

    /**
     * Constructs a new IQ using an existing Element. This is useful
     * for parsing incoming IQ Elements into IQ objects.
     *
     * @param element the IQ Element.
     */
    public IQ(Element element) {
        super(element);
    }

    /**
     * Returns the type of this IQ.
     *
     * @return the IQ type.
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
     * Sets the type of this IQ.
     *
     * @param type the IQ type.
     * @see Type
     */
    public void setType(Type type) {
        element.addAttribute("type", type==null?null:type.toString());
    }

    /**
     * Returns the child element of this IQ. IQ packets may have a single child
     * element in an extended namespace. This method is a convenience method
     * to avoid manipulating the underlying packet's Element instance directly.
     *
     * @return the child element.
     */
    public Element getChildElement() {
        List elements = element.elements();
        if (elements.isEmpty()) {
            return null;
        }
        else {
            // Search for a child element that is in a different namespace.
            for (int i=0; i<elements.size(); i++) {
                Element element = (Element)elements.get(i);
                String namespace = element.getNamespaceURI();
                if (!namespace.equals("") && !namespace.equals("jabber:client") &&
                        !namespace.equals("jabber:server"))
                {
                    return element;
                }
            }
            return null;
        }
    }

    /**
     * Sets the child element of this IQ. IQ packets may have a single child
     * element in an extended namespace. This method is a convenience method
     * to avoid manipulating the underlying packet's Element instance directly.<p>
     *
     * A sample use of this method might look like the following:
     * <pre>
     * IQ iq = new IQ("time_1");
     * iq.setTo("mary@example.com");
     * iq.setType(IQ.Type.GET);
     * iq.setChildElement(docFactory.createElement("query", "jabber:iq:time"));</pre>
     *
     * @param childElement the child element.
     */
    public void setChildElement(Element childElement) {
        for (Iterator i=element.elementIterator(); i.hasNext(); ) {
            element.remove((Element)i.next());
        }
        element.add(childElement);
    }

    /**
     * Sets the child element of this IQ by constructing a new Element with the
     * given name and namespace. The newly created child element is returned.
     * IQ packets may have a single child element in an extended namespace.
     * This method is a convenience method to avoid manipulating the underlying
     * packet's Element instance directly.<p>
     *
     * A sample use of this method might look like the following:
     * <pre>
     * IQ iq = new IQ("time_1");
     * iq.setTo("mary@example.com");
     * iq.setType(IQ.Type.GET);
     * iq.setChildElement("query", "jabber:iq:time");</pre>
     *
     * @param name the child element name.
     * @param namespace the child element namespace.
     * @return the newly created child element.
     */
    public Element setChildElement(String name, String namespace) {
        for (Iterator i=element.elementIterator(); i.hasNext(); ) {
            element.remove((Element)i.next());
        }
        return element.addElement(name, namespace);
    }

    /**
     * Convenience method to create a new {@link Type#RESULT IQ.Type.RESULT} IQ based
     * on a {@link Type#GET IQ.Type.GET} or {@link Type#SET IQ.Type.SET} IQ. The new
     * packet will be initialized with:<ul>
     *
     *      <li>The sender set to the recipient of the originating IQ.
     *      <li>The recipient set to the sender of the originating IQ.
     *      <li>The type set to {@link Type#RESULT IQ.Type.RESULT}.
     *      <li>The id set to the id of the originating IQ.
     *      <li>An empty child element using the same element name and namespace
     *          as the orginiating IQ.
     * </ul>
     *
     * @param iq the {@link Type#GET IQ.Type.GET} or {@link Type#SET IQ.Type.SET} IQ packet.
     * @throws IllegalArgumentException if the IQ packet does not have a type of
     *      {@link Type#GET IQ.Type.GET} or {@link Type#SET IQ.Type.SET}.
     * @return a new {@link Type#RESULT IQ.Type.RESULT} IQ based on the originating IQ.
     */
    public static IQ createResultIQ(IQ iq) {
        if (!(iq.getType() == Type.GET || iq.getType() == Type.RESULT)) {
            throw new IllegalArgumentException("IQ must be of type 'set' or 'get'.");
        }
        IQ result = new IQ(iq.getID());
        result.setFrom(iq.getTo());
        result.setTo(iq.getFrom());
        result.setType(Type.RESULT);
        Element childElement = iq.getChildElement();
        if (childElement != null) {
            Element resultChild = docFactory.createElement(childElement.getName(),
                    childElement.getNamespaceURI());
            result.setChildElement(resultChild);
        }
        return result;
    }

    /**
     * A class to represent the type of the IQ packet. The types are:
     *
     * <ul>
     *      <li>IQ.Type.GET -- the stanza is a request for information or requirements.
     *      <li>IQ.Type.SET -- the stanza provides required data, sets new values, or
     *          replaces existing values.
     *      <li>IQ.Type.RESULT -- the stanza is a response to a successful get or set request.
     *      <li>IQ.Type.ERROR -- an error has occurred regarding processing or delivery of a
     *          previously-sent get or set.
     * </ul>
     *
     * If {@link #GET IQ.Type.GET} or {@link #SET IQ.Type.SET} is received the response
     * must be {@link #RESULT IQ.Type.RESULT} or {@link #ERROR IQ.Type.ERROR}. The id of the
     * originating {@link #GET IQ.Type.GET} of {@link #SET IQ.Type.SET} IQ must be preserved
     * when sending {@link #RESULT IQ.Type.RESULT} or {@link #ERROR IQ.Type.ERROR}.
     */
    public static class Type {

        public static final Type GET = new Type("get");
        public static final Type SET = new Type("set");
        public static final Type RESULT = new Type("result");
        public static final Type ERROR = new Type("error");

        /**
         * Converts a String into the corresponding types. Valid String values
         * that can be converted to types are: "get", "set", "result", and "error".
         *
         * @param type the String value to covert.
         * @return the corresponding Type.
         */
        public static Type fromString(String type) {
            if (type == null) {
                return null;
            }
            type = type.toLowerCase();
            if (GET.toString().equals(type)) {
                return GET;
            }
            else if (SET.toString().equals(type)) {
                return SET;
            }
            else if (ERROR.toString().equals(type)) {
                return ERROR;
            }
            else if (RESULT.toString().equals(type)) {
                return RESULT;
            }
            else {
                return null;
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
