/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNetworkChange;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Gateway as a node of ComputerCraft's wired network: a networking cable against the front face
 * joins it, and the Gateway's peripheral is published to every computer on that network under the
 * Gateway's name. It also keeps track of what else the network shows, for the manager's status card.
 */
final class GatewayWiredElement implements WiredElement {

    private static final Set<String> COMPUTER_TYPES = Set.of("computer", "turtle", "pocket");

    private final NetworkGatewayBlockEntity gateway;
    @Nullable
    private WiredNode node;
    /** Everything else on the wire, by the name ComputerCraft knows it by: computers and devices alike. */
    private final Map<String, IPeripheral> seen = new LinkedHashMap<>();

    GatewayWiredElement(final NetworkGatewayBlockEntity gateway) {
        this.gateway = gateway;
    }

    @Override
    public WiredNode getNode() {
        if (node == null) {
            node = ComputerCraftAPI.createWiredNodeForElement(this);
        }
        return node;
    }

    @Override
    public Level getLevel() {
        final Level level = gateway.getLevel();
        if (level == null) {
            throw new IllegalStateException("the Gateway is not in a world");
        }
        return level;
    }

    @Override
    public Vec3 getPosition() {
        return Vec3.atCenterOf(gateway.getBlockPos());
    }

    @Override
    public String getSenderID() {
        return gateway.peripheralName();
    }

    @Override
    public void networkChanged(final WiredNetworkChange change) {
        change.peripheralsRemoved().keySet().forEach(seen::remove);
        seen.putAll(change.peripheralsAdded());
    }

    void publish(final String name, final IPeripheral peripheral) {
        getNode().updatePeripherals(Map.of(name, peripheral));
    }

    void remove() {
        if (node != null) {
            node.remove();
            node = null;
        }
        seen.clear();
    }

    boolean onWire() {
        return !seen.isEmpty();
    }

    int computersOnWire() {
        int count = 0;
        for (final IPeripheral peripheral : seen.values()) {
            if (COMPUTER_TYPES.contains(peripheral.getType())) {
                count++;
            }
        }
        return count;
    }

    /** Everything on the wire, by name, so a program on our side can list it and call it. */
    Map<String, IPeripheral> peripherals() {
        return new LinkedHashMap<>(seen);
    }

    /** Whether a peripheral of that name is one of ComputerCraft's own computers. */
    static boolean isComputer(final IPeripheral peripheral) {
        return COMPUTER_TYPES.contains(peripheral.getType());
    }

    int devicesOnWire() {
        return seen.size() - computersOnWire();
    }
}
