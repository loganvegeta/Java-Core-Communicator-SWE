package com.swe.networking;

import java.util.HashMap;

/**
 * Enum type for the modules present.
 */
public enum ModuleType {
    /** Networking module. */
    NETWORKING,
    /** Screensharing module. */
    SCREENSHARING,
    /** Canvas module. */
    CANVAS,
    /** UIUX module. */
    UIUX,
    /** Controller module. */
    CONTROLLER,
    /** AI module. */
    AI,
    /** Cloud module. */
    CLOUD,
    /** Chat module. */
    CHAT;

    /** NETWORKING module ordinal. */
    private static final int NETWORKING_TYPE = 0;
    /** SCREENSHARING module ordinal. */
    private static final int SCREENSHARING_TYPE = 1;
    /** CANVAS module ordinal. */
    private static final int CANVAS_TYPE = 2;
    /** UIUX module ordinal. */
    private static final int UIUX_TYPE = 3;
    /** CONTROLLER module ordinal. */
    private static final int CONTROLLER_TYPE = 4;
    /** AI module ordinal. */
    private static final int AI_TYPE = 5;
    /** CLOUD module ordinal. */
    private static final int CLOUD_TYPE = 6;
    /** CHAT module ordinal. */
    private static final int CHAT_TYPE = 7;

    /**
     * static map to store mapping from integer to module type.
     */
    private static final HashMap<Integer, ModuleType> MAP = new HashMap<>() {
        {
            put(NETWORKING_TYPE, NETWORKING);
            put(SCREENSHARING_TYPE, SCREENSHARING);
            put(CANVAS_TYPE, CANVAS);
            put(UIUX_TYPE, UIUX);
            put(CONTROLLER_TYPE, CONTROLLER);
            put(AI_TYPE, AI);
            put(CLOUD_TYPE, CLOUD);
            put(CHAT_TYPE, CHAT);
        }
    };

    /**
     * Function to map integer to module type.
     *
     * @param n the integer
     * @return the module it represents
     */
    public ModuleType getType(final int n) {
        return MAP.get(n);
    }
}
