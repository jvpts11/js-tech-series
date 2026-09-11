/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * The Gateway as a ComputerCraft computer sees it: a peripheral of type {@code jsc_gateway}. This is the
 * whole of it for now: whether it is up, its name and its host. The network reads, the operations and the
 * events come next, each through the same adapters Cannon programs use.
 */
public final class GatewayPeripheral implements IPeripheral {

    public static final String TYPE = "jsc_gateway";

    private final NetworkGatewayBlockEntity gateway;
    /** The computers attached right now, by id, so an event can be queued on each of them. */
    private final Map<Integer, IComputerAccess> attached = new LinkedHashMap<>();

    GatewayPeripheral(final NetworkGatewayBlockEntity gateway) {
        this.gateway = gateway;
    }

    /** Queues {@code event} on every attached computer; how many got it. */
    int queueEvent(final String event, final Object... arguments) {
        for (final IComputerAccess computer : attached.values()) {
            computer.queueEvent(event, arguments);
        }
        return attached.size();
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /** Whether the Gateway is linked to one of our computers, so the network is within reach. */
    @LuaFunction
    public boolean online() {
        return gateway.online();
    }

    /** The Gateway's name, the one its host gave it. */
    @LuaFunction
    public String name() {
        return gateway.name();
    }

    /** The name of the computer the Gateway is linked to, or an empty string while it is not. */
    @LuaFunction(mainThread = true)
    public String host() {
        return gateway.hostName();
    }

    @Override
    public void attach(final IComputerAccess computer) {
        attached.put(computer.getID(), computer);
        gateway.ccAttached(computer.getID());
    }

    @Override
    public void detach(final IComputerAccess computer) {
        attached.remove(computer.getID());
        gateway.ccDetached(computer.getID());
    }

    @Override
    @Nullable
    public Object getTarget() {
        return gateway;
    }

    @Override
    public boolean equals(@Nullable final IPeripheral other) {
        return other == this || (other instanceof GatewayPeripheral that && that.gateway == gateway);
    }
}
