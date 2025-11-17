package com.swe.networking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * Timer class to monitor client timeouts.
 */
public class Timer {

    /**
     * Timer duration for client timeouts in milliseconds.
     */
    private final long timeoutDuration;

    /**
     * List of clients being monitored.
     */
    private final List<ClientNode> clients;

    /**
     * Map to store the last active time for each client.
     */
    private final HashMap<ClientNode, Long> clientTimeouts;

    /**
     * Thread to run the timer.
     */
    private final Thread timerThread;

    /**
     * Callback function to handle timed out clients.
     */
    private final Consumer<ClientNode> onTimeout;

    /**
     * Constructor for Timer class.
     *
     * @param durationMillis the duration for the timeout in milliseconds
     * @param timeoutCallback the callback function to handle timed out clients
     */
    public Timer(final long durationMillis,
            final Consumer<ClientNode> timeoutCallback) {
        this.timeoutDuration = durationMillis;
        this.clients = new ArrayList<>();
        this.clientTimeouts = new HashMap<>();
        this.onTimeout = timeoutCallback;
        for (ClientNode client : clients) {
            clientTimeouts.put(client, System.currentTimeMillis());
        }
        this.timerThread = new Thread(this::start);
        this.timerThread.start();
    }

    /**
     * update the timeout for a client.
     *
     * @param client the client to update
     */
    public void updateTimeout(final ClientNode client) {
        clientTimeouts.put(client, System.currentTimeMillis());
    }

    /**
     * add client to timer monitoring.
     *
     * @param client the client to add
     */
    public void addClient(final ClientNode client) {
        System.out.println("Adding client to timer monitoring: " + client);
        clients.add(client);
        clientTimeouts.put(client, System.currentTimeMillis());
    }

    /**
     * remove client from timer monitoring.
     *
     * @param client the client to remove
     */
    public void removeClient(final ClientNode client) {
        clients.remove(client);
        clientTimeouts.remove(client);
    }

    /**
     * check if a client has timed out.
     */
    public void checkTimeouts() {
        final List<ClientNode> timedOutClients = new ArrayList<>();
        final long currentTime = System.currentTimeMillis();
        synchronized (clients) {
            System.out.println("Timer clients " + clients);
            for (ClientNode c : clients) {
                if (currentTime - clientTimeouts.get(c) > timeoutDuration) {
                    timedOutClients.add(c);
                }
            }
        }

        for (ClientNode c : timedOutClients) {
            onTimeout.accept(c);
            removeClient(c);
        }
    }

    /**
     * Start the timer thread and monitor for timeouts.
     */
    public void start() {
        while (true) {
            checkTimeouts();
            try {
                Thread.sleep(timeoutDuration);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Close the timer thread.
     */
    public void close() {
        this.timerThread.interrupt();
    }
}
