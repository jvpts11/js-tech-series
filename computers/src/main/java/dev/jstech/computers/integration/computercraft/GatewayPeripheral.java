/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.gateway.GatewayRefusedException;
import dev.jstech.computers.gateway.GatewayService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * The Gateway as a ComputerCraft computer sees it: a peripheral of type {@code jsc_gateway}. Whether it
 * is up, its name and its host; what the network holds and where; pulling into and pushing out of the
 * buffer, crafting, following and cancelling operations; watching a total; a line in the Gateway's log;
 * and starting a program on one of our computers. Every call is answered by the same service the host
 * computer's own programs use, out of the host's budget, under the Gateway's permissions, and a request
 * the Gateway will not carry out is the error the Lua program sees.
 *
 * <p>Events: {@code jsc_operation(id, status)} on the computer that started an operation when it settles,
 * {@code jsc_stock(name, total, previous)} on a computer watching a name when its total moves.
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

    /** Queues {@code event} on one attached computer; whether it is still attached. */
    boolean queueEventTo(final int computerId, final String event, final Object... arguments) {
        final IComputerAccess computer = attached.get(computerId);
        if (computer == null) {
            return false;
        }
        computer.queueEvent(event, arguments);
        return true;
    }

    private GatewayService service() throws LuaException {
        try {
            return GatewayService.of(gateway);
        } catch (final GatewayRefusedException refused) {
            throw new LuaException(refused.getMessage());
        }
    }

    private static GatewayService.Caller caller(final IComputerAccess computer) {
        return new GatewayService.Caller(computer.getID());
    }

    /** Runs one service call for the calling computer, turning a refusal into the error the program sees. */
    private <T> T answer(final IComputerAccess computer, final ServiceCall<T> call) throws LuaException {
        final GatewayService service = service();
        try {
            return call.apply(service, caller(computer));
        } catch (final GatewayRefusedException refused) {
            throw new LuaException(refused.getMessage());
        }
    }

    // Reads

    /** How much data the network can hold. */
    @LuaFunction(mainThread = true)
    public long capacity(final IComputerAccess computer) throws LuaException {
        return answer(computer, (s, c) -> s.capacity(c));
    }

    /** How much data the network holds. */
    @LuaFunction(mainThread = true)
    public long used(final IComputerAccess computer) throws LuaException {
        return answer(computer, (s, c) -> s.used(c));
    }

    /** Everything the network holds: a table of name to total. */
    @LuaFunction(mainThread = true)
    public Map<String, Long> types(final IComputerAccess computer) throws LuaException {
        return answer(computer, (s, c) -> s.types(c));
    }

    /** How much of {@code name} the whole network holds. */
    @LuaFunction(mainThread = true)
    public long total(final IComputerAccess computer, final String name) throws LuaException {
        return answer(computer, (s, c) -> s.total(c, name));
    }

    /** Which servers hold {@code name}: a list of {@code {server=, quantity=}}. */
    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> find(final IComputerAccess computer, final String name) throws LuaException {
        return answer(computer, (s, c) -> s.find(c, name));
    }

    /** The network's servers: a list of {@code {name=, used=, capacity=, online=}}. */
    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> servers(final IComputerAccess computer) throws LuaException {
        return answer(computer, (s, c) -> s.servers(c));
    }

    /** The computers on the network: a list of {@code {name=, label=, os=, type=, online=, shares=}}. */
    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> computers(final IComputerAccess computer) throws LuaException {
        return answer(computer, (s, c) -> s.computers(c));
    }

    // Operations

    /** Pulls {@code quantity} of {@code name} from the network into the buffer; the operation's id. */
    @LuaFunction(mainThread = true)
    public String pull(final IComputerAccess computer, final String name, final long quantity,
                       final Optional<String> priority) throws LuaException {
        return answer(computer, (s, c) -> s.pull(c, name, quantity, priority.orElse(null)));
    }

    /** Pushes up to {@code quantity} of {@code name} from the buffer into the network; the operation's id. */
    @LuaFunction(mainThread = true)
    public String push(final IComputerAccess computer, final String name, final long quantity,
                       final Optional<String> priority) throws LuaException {
        return answer(computer, (s, c) -> s.push(c, name, quantity, priority.orElse(null)));
    }

    /** Asks the network to craft {@code quantity} of {@code name}; the operation's id. */
    @LuaFunction(mainThread = true)
    public String craft(final IComputerAccess computer, final String name, final long quantity,
                        final Optional<String> priority) throws LuaException {
        return answer(computer, (s, c) -> s.craft(c, name, quantity, priority.orElse(null)));
    }

    /** One operation by id: {@code {id=, type=, status=, item=, requested=, moved=, priority=}}, or nil. */
    @LuaFunction(mainThread = true)
    @Nullable
    public Map<String, Object> operation(final IComputerAccess computer, final String id) throws LuaException {
        return answer(computer, (s, c) -> s.operation(c, id));
    }

    /** Every operation in flight on the network. */
    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> operations(final IComputerAccess computer) throws LuaException {
        return answer(computer, (s, c) -> s.operations(c));
    }

    /** Stops an operation in flight; whether it was still running. */
    @LuaFunction(mainThread = true)
    public boolean cancel(final IComputerAccess computer, final String id) throws LuaException {
        return answer(computer, (s, c) -> s.cancel(c, id));
    }

    /** Starts a compiled program on one of our computers: {@code run(computer, program, ...)}; the process id. */
    @LuaFunction(mainThread = true)
    public int run(final IComputerAccess computer, final IArguments arguments) throws LuaException {
        final String target = arguments.getString(0);
        final String program = arguments.getString(1);
        final List<String> args = new ArrayList<>();
        for (int i = 2; i < arguments.count(); i++) {
            args.add(arguments.getStringCoerced(i));
        }
        return answer(computer, (s, c) -> s.run(c, target, program, args, null));
    }

    // Watches and the log

    /** Asks for a {@code jsc_stock} event whenever the total of {@code name} moves; the total now. */
    @LuaFunction(mainThread = true)
    public long watch(final IComputerAccess computer, final String name) throws LuaException {
        return answer(computer, (s, c) -> s.watch(c, name));
    }

    /** Stops watching {@code name}; whether it was being watched. */
    @LuaFunction(mainThread = true)
    public boolean unwatch(final IComputerAccess computer, final String name) throws LuaException {
        return answer(computer, (s, c) -> s.unwatch(c, name));
    }

    /**
     * Writes a line into the Gateway's log: {@code log(level, text)}, the level being {@code info},
     * {@code warn} or {@code error}.
     */
    @LuaFunction(mainThread = true)
    public void log(final IComputerAccess computer, final String level, final String text) throws LuaException {
        answer(computer, (s, c) -> {
            s.log(c, level, text);
            return null;
        });
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /** Whether the Gateway is linked to one of our computers, so the network is within reach. */
    @LuaFunction(mainThread = true)
    public boolean online() {
        return gateway.online();
    }

    /** The Gateway's name, the one its host gave it. */
    @LuaFunction(mainThread = true)
    public String name() {
        return gateway.name();
    }

    /**
     * Says something to the machine the Gateway is linked to, for a program of ours listening for it.
     *
     * <p>This is the other half of {@code Gateway.Send}: a line of text, from this computer, waiting for
     * whoever is listening on the other side. Nobody listening means nobody hears it.
     */
    @LuaFunction(mainThread = true)
    public boolean send(final IComputerAccess computer, final String text) throws LuaException {
        service();
        gateway.said(computer.getID(), text, gateway.getLevel() == null ? 0L : gateway.getLevel().getGameTime());
        gateway.loggedSaid(new GatewayService.Caller(computer.getID()).label());
        return true;
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

    /** One call on the service for a given caller, which may be refused. */
    @FunctionalInterface
    private interface ServiceCall<T> {

        T apply(GatewayService service, GatewayService.Caller caller) throws GatewayRefusedException;
    }
}
