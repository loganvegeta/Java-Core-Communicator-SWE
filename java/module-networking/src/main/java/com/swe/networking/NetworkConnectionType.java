package com.swe.networking;

import java.util.HashMap;

/**
 * Enum data connection type for different types in network header.
 */
public enum NetworkConnectionType {
    /** HELLO packet type. */
    HELLO,
    /** ALIVE packet type. */
    ALIVE,
    /** ADD packet type. */
    ADD,
    /** REMOVE packet type. */
    REMOVE,
    /** NETWORK packet type. */
    NETWORK,
    /** MODULE packet type. */
    MODULE,
    /** CLOSE packet type. */
    CLOSE;

    /** REMOVE packet ID. */
    private static final int HELLOID = 0;
    /** REMOVE packet ID. */
    private static final int ALIVEID = 1;
    /** REMOVE packet ID. */
    private static final int ADDID = 2;
    /** REMOVE packet ID. */
    private static final int REMOVEID = 3;
    /** NETWORK packet ID. */
    private static final int NETWORKID = 4;
    /** NETWORK packet ID. */
    private static final int MODULEID = 5;
    /** CLOES packet ID. */
    private static final int CLOSEID = 7;

    /**
     * Hashmap storing the mapping from integer to NetworkConnectionType.
     */
    private static final HashMap<Integer, NetworkConnectionType> MAP = new HashMap<>() {
        {
            put(HELLOID, HELLO);
            put(ALIVEID, ALIVE);
            put(ADDID, ADD);
            put(REMOVEID, REMOVE);
            put(NETWORKID, NETWORK);
            put(MODULEID, MODULE);
            put(CLOSEID, CLOSE);
        }
    };

    /**
     * Making the constructor class private.
     */
    NetworkConnectionType() {
    }

    public static NetworkConnectionType getType(final int n) {
        return MAP.get(n);
    }
}
