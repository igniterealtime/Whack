package org.jivesoftware.whack;

import org.xmpp.packet.IQ;

/**
 * An IQResultListener will be invoked when a previously IQ packet sent by the server was answered.
 *
 * @author Gaston Dombiak
 */
public interface IQResultListener {

    /**
     * Notification method indicating that a previously sent IQ packet has been answered.
     * The received IQ packet might be of type ERROR or RESULT.
     *
     * @param packet the IQ packet answering a previously sent IQ packet.
     */
    void receivedAnswer(IQ packet);
}
