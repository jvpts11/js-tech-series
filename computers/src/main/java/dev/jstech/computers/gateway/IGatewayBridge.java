/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gateway;

/**
 * The ComputerCraft side of a Network Gateway, as the block entity sees it. The block entity never names a
 * ComputerCraft class: without CC: Tweaked there is no bridge at all, and with it the integration hands the
 * block entity one of these, so the Gateway compiles and runs the same either way.
 */
public interface IGatewayBridge {

    /** Publishes (or re-publishes, after a rename) the Gateway on CC's wired network under {@code name}. */
    void publish(String name);

    /** Takes the Gateway off CC's wired network; called when the block leaves the world. */
    void remove();

    /** Whether anything at all sits on the wired network this Gateway joined. */
    boolean onWire();

    /** How many computers (computers, turtles, pocket computers) the wired network shows. */
    int computersOnWire();

    /** How many other peripherals the wired network shows. */
    int devicesOnWire();

    /** Queues an event on every ComputerCraft computer attached to the Gateway; how many got it. */
    int sendEvent(String event, Object... arguments);

    /** Queues an event on one attached computer, by id; whether it is still attached. */
    boolean eventTo(int computerId, String event, Object... arguments);

    /**
     * Brings the folders mounted on every attached computer in line with what the network shares and what
     * the Gateway allows: new shares appear, closed ones go, and a change of the files permission
     * remounts them the other way.
     */
    void refreshMounts();
}
