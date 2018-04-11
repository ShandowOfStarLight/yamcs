package org.yamcs.api;

import org.yamcs.protobuf.Yamcs.Event;

public interface EventProducer {

    public abstract void sendEvent(Event event);

    public abstract void setSource(String source);

    public abstract void setSeqNo(int sn);

    /**
     * @deprecated not according to XTCE levels, use {@link #sendCritical(String, String)}
     * 
     */
    @Deprecated
    public abstract void sendError(String type, String msg);

    public abstract void sendWarning(String type, String msg);

    public abstract void sendInfo(String type, String msg);
    
    public abstract void sendWatch(String type, String msg);
    public abstract void sendDistress(String type, String msg);
    public abstract void sendCritical(String type, String msg);
    public abstract void sendSevere(String type, String msg);

    /**
     * Enable/disable repeated event reduction. If enabled, the events that are equal are not sent until the timeout expires, 
     * case in which an event containing the number of events skipped will be sent.
     * 
     * Two events are considered equal if their source, type, severity and message are equal. The sequence count and timestamp do not need to be equal.
     * 
     * The event sent in case the timeout is expired is a copy of the last event except that the message is replaced with 
     *   "Repeated x times: original message" 
     * 
     * @param repeatedEventReduction if true - enable the reduction of events.
     * @param repeatedEventTimeoutMillisec - how long to keep quiet in case of equal events being sent
     */
    public abstract void setRepeatedEventReduction(boolean repeatedEventReduction, long repeatedEventTimeoutMillisec);
        
    /**
     * Creates a default Event Builder with these fields pre-filled: source,
     * seqNo, receptionTime, generationTime
     */
    public abstract Event.Builder newEvent();
    
    /**
     * Closes the connection to the server; the producer is unusable after this is called
     */
    public void close();
    
    
    static final public String TYPE_ALGO_COMPILE = "ALGO_COMPILE"; 
    static final public String TYPE_ALGO_RUN = "ALGO_RUN"; 
}