/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gateway.GatewayRefusedException;
import dev.jstech.computers.gateway.IGatewayBridge;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, String> peripherals() {
        final Map<String, String> out = new LinkedHashMap<>();
        element.peripherals().forEach((name, found) -> out.put(name, found.getType()));
        return out;
    }

    @Override
    public List<String> methodsOf(final String name) {
        final IPeripheral found = element.peripherals().get(name);
        return found == null ? List.of() : GatewayCalls.methodsOf(found);
    }

    @Override
    public Object call(final String name, final String method, final List<Object> arguments)
            throws GatewayRefusedException {
        final IPeripheral found = element.peripherals().get(name);
        if (found == null) {
            throw new GatewayRefusedException(BridgeTexts.NOT_ON_WIRE.with(name));
        }
        try {
            return GatewayCalls.call(found, method, arguments);
        } catch (final LuaException refused) {
            // What the peripheral said is its own words, in whatever language its mod wrote them in.
            throw new GatewayRefusedException(refused.getMessage() == null
                    ? BridgeTexts.CALL_FAILED.with(name + "." + method) : Text.literal(refused.getMessage()));
        }
    }

    @Override
    public List<Map<String, Object>> computers() {
        final List<Map<String, Object>> out = new ArrayList<>();
        element.peripherals().forEach((name, found) -> {
            if (!GatewayWiredElement.isComputer(found)) {
                return;
            }
            final Map<String, Object> row = new LinkedHashMap<>();
            row.put("Name", name);
            row.put("Id", numberOf(found, "getID"));
            row.put("Label", textOf(found, "getLabel"));
            row.put("Online", Boolean.TRUE.equals(quietly(found, "isOn")));
            out.add(row);
        });
        return out;
    }

    @Override
    public boolean power(final int computerId, final String what) {
        for (final IPeripheral found : element.peripherals().values()) {
            if (!GatewayWiredElement.isComputer(found) || numberOf(found, "getID") != computerId) {
                continue;
            }
            final String method = switch (what) {
                case "on" -> "turnOn";
                case "off" -> "shutdown";
                default -> "reboot";
            };
            try {
                GatewayCalls.call(found, method, List.of());
                return true;
            } catch (final LuaException refused) {
                return false;
            }
        }
        return false;
    }

    /* What a computer answers about itself, or nothing at all when it will not say. */
    private static Object quietly(final IPeripheral found,
                                  final String method) {
        try {
            return GatewayCalls.call(found, method, List.of());
        } catch (final LuaException refused) {
            return null;
        }
    }

    private static long numberOf(final IPeripheral found,
                                 final String method) {
        return quietly(found, method) instanceof Number number ? number.longValue() : -1L;
    }

    private static String textOf(final IPeripheral found,
                                 final String method) {
        final Object said = quietly(found, method);
        return said == null ? "" : String.valueOf(said);
    }
}
