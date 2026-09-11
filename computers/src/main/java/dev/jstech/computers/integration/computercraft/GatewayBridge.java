/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gateway.IGatewayBridge;

/**
 * A Gateway's ComputerCraft side: the peripheral CC computers wrap and the node that joins CC's wired
 * network. Made only when CC: Tweaked is present.
 */
final class GatewayBridge implements IGatewayBridge {

    private final GatewayPeripheral peripheral;
    private final GatewayWiredElement element;

    GatewayBridge(final NetworkGatewayBlockEntity gateway) {
        this.peripheral = new GatewayPeripheral(gateway);
        this.element = new GatewayWiredElement(gateway);
    }

    GatewayPeripheral peripheral() {
        return peripheral;
    }

    GatewayWiredElement element() {
        return element;
    }

    @Override
    public void publish(final String name) {
        element.publish(name, peripheral);
    }

    @Override
    public void remove() {
        element.remove();
    }

    @Override
    public boolean onWire() {
        return element.onWire();
    }

    @Override
    public int computersOnWire() {
        return element.computersOnWire();
    }

    @Override
    public int devicesOnWire() {
        return element.devicesOnWire();
    }

    @Override
    public int sendEvent(final String event, final Object... arguments) {
        return peripheral.queueEvent(event, arguments);
    }

    @Override
    public boolean eventTo(final int computerId, final String event, final Object... arguments) {
        return peripheral.queueEventTo(computerId, event, arguments);
    }

    @Override
    public void refreshMounts() {
        peripheral.refreshMounts();
    }
}
