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

import org.jivesoftware.stringprep.IDNA;
import org.jivesoftware.stringprep.Stringprep;

/**
 * An XMPP address (JID). A JID is made up of a node (generally a username), a domain,
 * and a resource. The node and resource are optional; domain is required. In simple
 * ABNF form:
 *
 * <ul><tt>jid = [ node "@" ] domain [ "/" resource ]</tt></ul>
 *
 * Some sample JID's:
 * <ul>
 *      <li><tt>user@example.com</tt></li>
 *      <li><tt>user@example.com/home</tt></li>
 *      <li><tt>example.com</tt></li>
 * </ul>
 *
 * Each allowable portion of a JID (node, domain, and resource) must not be more
 * than 1023 bytes in length, resulting in a maximum total size (including the '@'
 * and '/' separators) of 3071 bytes.
 *
 * @author Matt Tucker
 */
public class JID implements Comparable {

    private String node;
    private String domain;
    private String resource;

    /**
     * Constructs a JID from it's String representation.
     *
     * @param jid a valid JID.
     * @throws IllegalArgumentException if the JID is not valid.
     */
    public JID(String jid) {
        if (jid == null) {
            throw new NullPointerException("JID cannot be null");
        }
        String node = null;
        String domain = null;
        String resource = null;

        int atIndex = jid.indexOf("@");
        int slashIndex = jid.indexOf("/");

        // Node
        if (atIndex > 0) {
            node = jid.substring(0, atIndex);
        }

        // Domain
        if (atIndex + 1 > jid.length()) {
            throw new IllegalArgumentException("JID with empty domain not valid");
        }
        if (atIndex < 0) {
            if (slashIndex > 0) {
                domain = jid.substring(0, slashIndex);
            }
            else {
                domain = jid;
            }
        }
        else {
            if (slashIndex > 0) {
                domain = jid.substring(atIndex + 1, slashIndex);
            }
            else {
                domain = jid.substring(atIndex + 1);
            }
        }

        // Resource
        if (slashIndex + 1 > jid.length() || slashIndex < 0) {
            resource = null;
        }
        else {
            resource = jid.substring(slashIndex + 1);
        }

        init(node, domain,resource);
    }

    /**
     * Constructs a JID given a node, domain, and resource.
     *
     * @param node the node.
     * @param domain the domain, which must not be <tt>null</tt>.
     * @param resource the resource.
     * @throws IllegalArgumentException if the JID is not valid.
     */
    public JID(String node, String domain, String resource) {
        if (domain == null) {
            throw new NullPointerException("Domain cannot be null");
        }
        init(node, domain, resource);
    }

    /**
     * Constructs a new JID, bypassing all stringprep profiles. This
     * is useful for constructing a JID object when it's already known
     * that the String representation is well-formed.
     *
     * @param jid the JID.
     * @param fake an extra param to create a different method signature.
     *      The value <tt>null</tt> should be passed in as this argument.
     */
    JID(String jid, Object fake) {
        fake = null; // Workaround IDE warnings for unused param.
        if (jid == null) {
            throw new NullPointerException("JID cannot be null");
        }

        int atIndex = jid.indexOf("@");
        int slashIndex = jid.indexOf("/");

        // Node
        if (atIndex > 0) {
            node = jid.substring(0, atIndex);
        }

        // Domain
        if (atIndex + 1 > jid.length()) {
            throw new IllegalArgumentException("JID with empty domain not valid");
        }
        if (atIndex < 0) {
            if (slashIndex > 0) {
                domain = jid.substring(0, slashIndex);
            }
            else {
                domain = jid;
            }
        }
        else {
            if (slashIndex > 0) {
                domain = jid.substring(atIndex + 1, slashIndex);
            }
            else {
                domain = jid.substring(atIndex + 1);
            }
        }

        // Resource
        if (slashIndex + 1 > jid.length() || slashIndex < 0) {
            resource = null;
        }
        else {
            resource = jid.substring(slashIndex + 1);
        }
    }

    /**
     * Transforms the JID parts using the appropriate Stringprep profiles, then
     * validates them. If they are fully valid, the field values are saved, otherwise
     * an IllegalArgumentException is thrown.
     *
     * @param node the node.
     * @param domain the domain.
     * @param resource the resource.
     */
    private void init(String node, String domain, String resource) {
        // Set node and resource to null if they are the empty string.
        if (node != null && node.equals("")) {
            node = null;
        }
        if (resource != null && resource.equals("")) {
            resource = null;
        }
        // Stringprep (node prep, resourceprep, etc).
        try {
            this.node = Stringprep.nodeprep(node);
            // XMPP specifies that domains should be run through IDNA and
            // that they should be run through nameprep before doing any
            // comparisons. We always run the domain through nameprep to
            // make comparisons easier later.
            this.domain = Stringprep.nameprep(IDNA.toASCII(domain), false);
            this.resource = Stringprep.resourceprep(resource);
        }
        catch (Exception e) {
            String errorMessage = "Offending JID:"+node+"@"+domain+"/"+resource;
            throw new IllegalArgumentException("Illegal JID format: "+errorMessage+": "+ e.getMessage());
        }

        // Validate each field is not greater than 1023 bytes. UTF-8 characters use two bytes.
        if (node != null && node.length()*2 > 1023) {
            throw new IllegalArgumentException("Node cannot be larger than 1023 bytes. Size is " +
                    (node.length() * 2) + " bytes.");
        }
        if (domain.length()*2 > 1023) {
            throw new IllegalArgumentException("Domain cannot be larger than 1023 bytes. Size is " +
                    (domain.length() * 2) + " bytes.");
        }
        if (resource != null && resource.length()*2 > 1023) {
            throw new IllegalArgumentException("Resource cannot be larger than 1023 bytes. Size is " +
                    (resource.length() * 2) + " bytes.");
        }
    }

    /**
     * Returns the node, or <tt>null</tt> if this JID does not contain node information.
     *
     * @return the node.
     */
    public String getNode() {
        return node;
    }

    /**
     * Returns the domain.
     *
     * @return the domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Returns the resource, or <tt>null</tt> if this JID does not contain resource information.
     *
     * @return the resource.
     */
    public String getResource() {
        return resource;
    }

    /**
     * Returns the String representation of the bare JID, which is the JID with
     * resource information removed.
     *
     * @return the bare JID.
     */
    public String toBareJID() {
        StringBuffer buf = new StringBuffer();
        if (node != null) {
            buf.append(node).append("@");
        }
        buf.append(domain);
        return buf.toString();
    }

    /**
     * Returns a String representation of the JID.
     *
     * @return a String representation of the JID.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        if (node != null) {
            buf.append(node).append("@");
        }
        buf.append(domain);
        if (resource != null) {
            buf.append("/").append(resource);
        }
        return buf.toString();
    }

    public int hashCode() {
        return toString().hashCode();
    }

    public boolean equals(Object object) {
        if (!(object instanceof JID)) {
            return false;
        }
        JID jid = (JID)object;
        // Node. If node isn't null, compare.
        if (node != null) {
            if (!node.equals(jid.node)) {
                return false;
            }
        }
        // Otherwise, jid.node must be null.
        else if (jid.node != null) {
            return false;
        }
        // Compare domain, which must be null.
        if (!domain.equals(jid.domain)) {
            return false;
        }
        // Resource. If resource isn't null, compare.
        if (resource != null) {
            if (!resource.equals(jid.resource)) {
                return false;
            }
        }
        // Otherwise, jid.resource must be null.
        else if (jid.resource != null) {
            return false;
        }
        // Passed all checks, so equal.
        return true;
    }

    public int compareTo(Object o) {
        if (!(o instanceof JID)) {
            throw new ClassCastException("Ojbect not instanceof JID: " + o);
        }
        JID jid = (JID)o;

        // Comparison order is domain, node, resource.
        int compare = domain.compareTo(jid.domain);
        if (compare == 0 && node != null && jid.node != null) {
            compare = node.compareTo(jid.node);
        }
        if (compare == 0 && resource != null && jid.resource != null) {
            compare = resource.compareTo(jid.resource);
        }
        return compare;
    }

    /**
     * Returns true if two JID's are equivalent. The JID components are compared using
     * the following rules:<ul>
     *      <li>Nodes are normalized using nodeprep (case insensitive).
     *      <li>Domains are normalized using IDNA and then nameprep (case insensitive).
     *      <li>Resources are normalized using resourceprep (case sensitive).</ul>
     *
     * These normalization rules ensure, for example, that
     * <tt>User@EXAMPLE.com/home</tt> is considered equal to <tt>user@example.com/home</tt>.
     *
     * @param jid1 a JID.
     * @param jid2 a JID.
     * @return true if the JIDs are equivalent; false otherwise.
     * @throws IllegalArgumentException if either JID is not valid.
     */
    public static boolean equals(String jid1, String jid2) {
        return new JID(jid1).equals(new JID(jid2));
    }
}