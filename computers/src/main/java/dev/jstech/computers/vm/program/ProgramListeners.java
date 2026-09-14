/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

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

    /** Puts back who was listening and the Gateway chosen when the program was saved; nulls mean nobody. */
    void restore(final Values.DelegateValue onMessage, final Values.DelegateValue onGatewayMessage,
                 final String gateway) {
        this.hearMessages(onMessage);
        this.hearGateway(onGatewayMessage);
        this.chooseGateway(gateway);
    }
}
