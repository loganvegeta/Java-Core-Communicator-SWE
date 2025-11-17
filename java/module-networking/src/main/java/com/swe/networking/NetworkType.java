package com.swe.networking;

import java.util.HashMap;

/**
 * Enum data type for different types in network header.
 */
public enum NetworkType {
    /** CLUSTERSERVER packet type. */
    CLUSTERSERVER,
    /** SAMECLUSTER packet type. */
    SAMECLUSTER,
    /** OTHERCLUSTER packet type. */
    OTHERCLUSTER,
    /** USE packet type. */
    USE;

    /** CLUSTERSERVER packet ID. */
    private static final int CLUSTERSERVERID = 0;
    /** SAMECLUSTER packet ID. */
    private static final int SAMECLUSTERID = 1;
    /** OTHERCLUSTER packet ID. */
    private static final int OTHERCLUSTERID = 2;
    /** USE packet ID. */
    private static final int USEID = 3;

    /**
     * Hashmap storing the mapping from integer to NetworkType.
     */
    private static final HashMap<Integer, NetworkType> MAP = new HashMap<>() {
        {
            put(CLUSTERSERVERID, CLUSTERSERVER);
            put(SAMECLUSTERID, SAMECLUSTER);
            put(OTHERCLUSTERID, OTHERCLUSTER);
            put(USEID, USE);
        }
    };

    /**
     * Making the constructor class private.
     */
    NetworkType() {
    }

    public static NetworkType getType(final int n) {
        return MAP.get(n);
    }
}
