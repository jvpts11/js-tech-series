/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Who a process tells when something is said to it: the handler it gave {@code Program.OnMessage}, the one it gave
 * {@code Gateway.OnMessage}, and the Gateway it chose to reach through.
 *
 * <p>Each is set by the program and kept across a reload like anything else the program holds. A handler is null
 * while nobody listens.
 */
final class ProgramListeners {

    /** Who to tell when another program sends a line, or null for nobody. */
    private Values.DelegateValue onMessage;
    /** Who to tell when a ComputerCraft computer says something through a Gateway, or null for nobody. */
    private Values.DelegateValue onGatewayMessage;
    /** The Gateway this program chose to reach through, or nothing for whichever the machine lists first. */
    private String gateway = "";
    /** Who to tell when any Operation of the network settles, or null for nobody. */
    private Values.DelegateValue onSettled;
    /** Who to tell when one Operation settles, by its short id in lower case; each is told once, and forgotten. */
    private final Map<String, Values.DelegateValue> settledFor = new LinkedHashMap<>();
    /** Who to tell of each entry written to the network's event log, or null for nobody. */
    private Values.DelegateValue onEvent;

    /** Who to tell when another program sends a line; null takes the listener away. */
    void hearMessages(final Values.DelegateValue handler) {
        this.onMessage = handler;
    }

    /** Who to tell when a ComputerCraft computer says something; null takes the listener away. */
    void hearGateway(final Values.DelegateValue handler) {
        this.onGatewayMessage = handler;
    }

    /** Remembers the Gateway the program chose; null or empty means whichever the machine lists first. */
    void chooseGateway(final String name) {
        this.gateway = name == null ? "" : name;
    }

    /** Who hears another program's lines, or null for nobody. */
    Values.DelegateValue onMessage() {
        return this.onMessage;
    }

    /** Who hears what comes through a Gateway, or null for nobody. */
    Values.DelegateValue onGatewayMessage() {
        return this.onGatewayMessage;
    }

    /** The Gateway chosen, empty for whichever the machine lists first. */
    String gateway() {
        return this.gateway;
    }

    /** Who to tell when any Operation of the network settles; null takes the listener away. */
    void hearSettled(final Values.DelegateValue handler) {
        this.onSettled = handler;
    }

    /** Who to tell when the Operation with that short id settles, once; null takes the listener away. */
    void hearSettled(final String operation, final Values.DelegateValue handler) {
        final String id = operation == null ? "" : operation.toLowerCase(Locale.ROOT);
        if (handler == null) {
            this.settledFor.remove(id);
        } else if (!id.isEmpty()) {
            this.settledFor.put(id, handler);
        }
    }

    /** Who to tell of each entry written to the network's event log; null takes the listener away. */
    void hearEvents(final Values.DelegateValue handler) {
        this.onEvent = handler;
    }

    /** Who hears every Operation that settles, or null for nobody. */
    Values.DelegateValue onSettled() {
        return this.onSettled;
    }

    /** Who hears one Operation settle, by its short id, as the save writes them down. */
    Map<String, Values.DelegateValue> settledFor() {
        return Map.copyOf(this.settledFor);
    }

    /** Takes the one who waits on that Operation away and gives it, or null when nobody waits on it. */
    Values.DelegateValue takeSettled(final String operation) {
        return operation == null ? null : this.settledFor.remove(operation.toLowerCase(Locale.ROOT));
    }

    /** Who hears the network's event log, or null for nobody. */
    Values.DelegateValue onEvent() {
        return this.onEvent;
    }

    /** Whether anything here waits on what happens on the network: an Operation settling, or the event log. */
    boolean listensToNetwork() {
        return this.onSettled != null || !this.settledFor.isEmpty() || this.onEvent != null;
    }

    /** Puts back who was listening and the Gateway chosen when the program was saved; nulls mean nobody. */
    void restore(final Values.DelegateValue onMessage, final Values.DelegateValue onGatewayMessage,
                 final String gateway) {
        this.hearMessages(onMessage);
        this.hearGateway(onGatewayMessage);
        this.chooseGateway(gateway);
    }

    /** Puts back who was waiting on the network when the program was saved; nulls mean nobody. */
    void restoreNetwork(final Values.DelegateValue settled, final Map<String, Values.DelegateValue> settledOne,
                        final Values.DelegateValue event) {
        this.hearSettled(settled);
        this.settledFor.clear();
        settledOne.forEach(this::hearSettled);
        this.hearEvents(event);
    }
}
