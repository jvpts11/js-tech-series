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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    /**
     * What each attached computer has mounted: by computer id, a share (its location and whether it was
     * mounted writable) to the name ComputerCraft gave the mount, which is what unmounting takes.
     */
    private final Map<Integer, Map<String, String>> mounted = new LinkedHashMap<>();

    GatewayPeripheral(final NetworkGatewayBlockEntity gateway) {
        this.gateway = gateway;
    }

    /** Mounts, remounts or unmounts the shared folders on every attached computer, as things stand now. */
    void refreshMounts() {
        final GatewayService service;
        try {
            service = GatewayService.of(gateway);
        } catch (final GatewayRefusedException unlinked) {
            for (final IComputerAccess computer : attached.values()) {
                unmountAll(computer);
            }
            return;
        }
        final List<GatewayService.SharedFolder> wanted = service.sharedFolders();
        for (final IComputerAccess computer : attached.values()) {
            reconcile(computer, service, wanted);
        }
    }

    private void reconcile(final IComputerAccess computer, final GatewayService service,
                           final List<GatewayService.SharedFolder> wanted) {
        final Map<String, String> have = mounted.computeIfAbsent(computer.getID(), k -> new LinkedHashMap<>());
        final Set<String> keep = new HashSet<>();
        for (final GatewayService.SharedFolder folder : wanted) {
            keep.add(mountKey(folder));
        }
        /*
         * Stale mounts go first: a share remounted the other way round wants its location back, and
         * ComputerCraft gives a taken location a number instead.
         */
        for (final Iterator<Map.Entry<String, String>> it = have.entrySet().iterator(); it.hasNext();) {
            final Map.Entry<String, String> entry = it.next();
            if (!keep.contains(entry.getKey())) {
                computer.unmount(entry.getValue());
                it.remove();
            }
        }
        for (final GatewayService.SharedFolder folder : wanted) {
            final String key = mountKey(folder);
            if (have.containsKey(key)) {
                continue;
            }
            final ShareMount mount = new ShareMount(service, folder.hostname(), folder.share(), folder.writable());
            final String name = folder.writable()
                    ? computer.mountWritable(folder.location(), mount)
                    : computer.mount(folder.location(), mount);
            if (name != null) {
                have.put(key, name);
            }
        }
    }

    /** A share and the way it is mounted, so a change of either remounts it. */
    private static String mountKey(final GatewayService.SharedFolder folder) {
        return folder.location() + (folder.writable() ? " rw" : " ro");
    }

    private void unmountAll(final IComputerAccess computer) {
        final Map<String, String> have = mounted.remove(computer.getID());
        if (have != null) {
            for (final String name : have.values()) {
                computer.unmount(name);
            }
        }
    }

    /** Where {@code computerId} has the shares mounted right now, by share location; for the tests and the manager. */
    public Map<String, String> mountsOf(final int computerId) {
        return new LinkedHashMap<>(mounted.getOrDefault(computerId, Map.of()));
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

    private static LuaException error(final GatewayRefusedException refused) {
        return new LuaException(refused.getMessage());
    }

    // Reads

    /** How much data the network can hold. */
    @LuaFunction(mainThread = true)
    public long capacity(final IComputerAccess computer) throws LuaException {
        try {
            return service().capacity(caller(computer));
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** How much data the network holds. */
    @LuaFunction(mainThread = true)
    public long used(final IComputerAccess computer) throws LuaException {
        try {
            return service().used(caller(computer));
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** Everything the network holds: a table of name to total. */
    @LuaFunction(mainThread = true)
    public Map<String, Long> types(final IComputerAccess computer) throws LuaException {
        try {
            return service().types(caller(computer));
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** How much of {@code name} the whole network holds. */
    @LuaFunction(mainThread = true)
    public long total(final IComputerAccess computer, final String name) throws LuaException {
        try {
            return service().total(caller(computer), name);
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** Which servers hold {@code name}: a list of {@code {server=, quantity=}}. */
    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> find(final IComputerAccess computer, final String name) throws LuaException {
        try {
            return service().find(caller(computer), name);
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** The network's servers: a list of {@code {name=, used=, capacity=, online=}}. */
    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> servers(final IComputerAccess computer) throws LuaException {
        try {
            return service().servers(caller(computer));
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** The computers on the network: a list of {@code {name=, label=, os=, type=, online=, shares=}}. */
    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> computers(final IComputerAccess computer) throws LuaException {
        try {
            return service().computers(caller(computer));
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    // Operations

    /** Pulls {@code quantity} of {@code name} from the network into the buffer; the operation's id. */
    @LuaFunction(mainThread = true)
    public String pull(final IComputerAccess computer, final String name, final long quantity,
                       final Optional<String> priority) throws LuaException {
        try {
            return service().pull(caller(computer), name, quantity, priority.orElse(null));
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** Pushes up to {@code quantity} of {@code name} from the buffer into the network; the operation's id. */
    @LuaFunction(mainThread = true)
    public String push(final IComputerAccess computer, final String name, final long quantity,
                       final Optional<String> priority) throws LuaException {
        try {
            return service().push(caller(computer), name, quantity, priority.orElse(null));
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** Asks the network to craft {@code quantity} of {@code name}; the operation's id. */
    @LuaFunction(mainThread = true)
    public String craft(final IComputerAccess computer, final String name, final long quantity,
                        final Optional<String> priority) throws LuaException {
        try {
            return service().craft(caller(computer), name, quantity, priority.orElse(null));
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** One operation by id: {@code {id=, type=, status=, item=, requested=, moved=, priority=}}, or nil. */
    @LuaFunction(mainThread = true)
    @Nullable
    public Map<String, Object> operation(final IComputerAccess computer, final String id) throws LuaException {
        try {
            return service().operation(caller(computer), id);
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** Every operation in flight on the network. */
    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> operations(final IComputerAccess computer) throws LuaException {
        try {
            return service().operations(caller(computer));
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** Stops an operation in flight; whether it was still running. */
    @LuaFunction(mainThread = true)
    public boolean cancel(final IComputerAccess computer, final String id) throws LuaException {
        try {
            return service().cancel(caller(computer), id);
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
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
        try {
            return service().run(caller(computer), target, program, args, null);
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    // Watches and the log

    /** Asks for a {@code jsc_stock} event whenever the total of {@code name} moves; the total now. */
    @LuaFunction(mainThread = true)
    public long watch(final IComputerAccess computer, final String name) throws LuaException {
        try {
            return service().watch(caller(computer), name);
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** Stops watching {@code name}; whether it was being watched. */
    @LuaFunction(mainThread = true)
    public boolean unwatch(final IComputerAccess computer, final String name) throws LuaException {
        try {
            return service().unwatch(caller(computer), name);
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
    }

    /** Writes a line into the Gateway's log: {@code log(level, text)}, the level {@code info}, {@code warn} or {@code error}. */
    @LuaFunction(mainThread = true)
    public void log(final IComputerAccess computer, final String level, final String text) throws LuaException {
        try {
            service().log(caller(computer), level, text);
        } catch (final GatewayRefusedException refused) {
            throw error(refused);
        }
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
        try {
            final GatewayService service = GatewayService.of(gateway);
            reconcile(computer, service, service.sharedFolders());
        } catch (final GatewayRefusedException unlinked) {
            // Not linked to a computer yet: the shares are mounted once it is, on the next refresh.
        }
    }

    @Override
    public void detach(final IComputerAccess computer) {
        unmountAll(computer);
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
