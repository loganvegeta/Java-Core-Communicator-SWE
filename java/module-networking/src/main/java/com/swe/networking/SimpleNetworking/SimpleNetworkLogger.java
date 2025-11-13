package com.swe.networking.SimpleNetworking;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to print logs of networking.
 */
public class SimpleNetworkLogger {

    /**
     * Variable to store the logger object.
     */
    private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * Function to print the info log to the file.
     *
     * @param str the string to log
     * @param module the name of the module
     */
    public static void printInfo(final String module, final String str) {
        LOGGER.log(Level.INFO, "[SIMPLENETWORKING]" + module + " " + str);
    }

    /**
     * Function to print the warning log to the file.
     *
     * @param str the string to log
     * @param module the name of the module
     */
    public static void printWarning(final String module, final String str) {
        LOGGER.log(Level.WARNING, "[SIMPLENETWORKING]" + module + " " + str);
    }

    /**
     * Function to print the error log to the file.
     *
     * @param str the string to log
     * @param module the name of the module
     */
    public static void printError(final String module, final String str) {
        LOGGER.log(Level.SEVERE, "[SIMPLENETWORKING]" + module + " " + str);
    }

}
