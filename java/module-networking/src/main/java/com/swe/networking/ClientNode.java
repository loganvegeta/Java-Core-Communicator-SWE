package com.swe.networking;

/**
 * A simple data class to hold client information (hostname and port).
 * This is the Java equivalent of a tuple(string, port).
 * @param hostName The IP address.
 * @param port The PORT.
 */
public record ClientNode(String hostName, int port) {
}
