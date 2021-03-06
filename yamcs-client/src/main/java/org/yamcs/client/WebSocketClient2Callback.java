package org.yamcs.client;

public interface WebSocketClient2Callback {

    /**
     * When a connection attempt is underway
     */
    default void connecting() {
    }

    /**
     * When the connection was successfully established
     */
    default void connected() {
    }

    /**
     * When the initial connection attempt failed
     */
    default void connectionFailed(Throwable t) {
    }

    /**
     * When a previously successful connection was disconnected
     */
    default void disconnected() {
    }
}
