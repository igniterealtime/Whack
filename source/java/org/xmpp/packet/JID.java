package org.xmpp.packet;

import org.jivesoftware.stringprep.IDNA;
import org.jivesoftware.stringprep.Stringprep;

/**
 *
 * @author Matt
 */
public class JID {

    private String node;
    private String domain;
    private String resource;

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

        try {
            this.node = Stringprep.nodeprep(node);
            this.domain = IDNA.toASCII(domain);
            this.resource = Stringprep.resourceprep(resource);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Illegal JID format.", e);
        }
    }

    public JID(String node, String domain) {
        this(node, domain, null);
    }

    public JID(String node, String domain, String resource) {
        if (domain == null) {
            throw new NullPointerException("Domain cannot be null");
        }
        try {
            this.node = Stringprep.nodeprep(node);
            this.domain = IDNA.toASCII(domain);
            this.resource = Stringprep.resourceprep(resource);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Illegal JID format.", e);
        }
    }

    public String getNode() {
        return node;
    }

    public String getDomain() {
        return domain;
    }

    public String getResource() {
        return resource;
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

    public static boolean equals(String jid1, String jid2) {
        return new JID(jid1).equals(new JID(jid2));
    }
}
