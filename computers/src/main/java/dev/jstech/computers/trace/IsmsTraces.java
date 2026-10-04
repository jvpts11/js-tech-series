/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.trace;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.engine.INetworkEngine;
import dev.jstech.computers.operation.payload.IsmsTracePayload;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Who is tracing what: the IQL Server Profiler windows that began a trace on a network, each with the groups of
 * events it watches, and what the network's work tells them as it happens.
 *
 * <p>Nothing is built for an event nobody watches: whoever posts one hands a supplier, called only when a trace on
 * that network picks the event's group, so a network nobody traces pays one map read for each thing that happens.
 * Everything here runs on the server thread.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class IsmsTraces {

    private static final Map<NetworkUuid, List<Listener>> LISTENERS = new HashMap<>();
    private static final ThreadLocal<Origin> ORIGIN = new ThreadLocal<>();
    /** What a trace says asked for work nobody in particular asked for. */
    private static final String NETWORK = "network";

    private IsmsTraces() {
    }

    /**
     * A trace on a network: the player and the window it is shown in, or, for one heard on the server itself, where
     * its events go; and the groups it watches.
     */
    private record Listener(@Nullable UUID player, int window, int mask, @Nullable Consumer<TraceEvent> sink) {
    }

    /** Who asked for the work being done now and on which computer, as a trace says it: a player at {@code desk}. */
    public record Origin(String requester, String computer) {
    }

    /** Begins {@code player}'s trace in {@code window} on {@code network}, watching the groups {@code mask} picks. */
    public static void start(final ServerPlayer player, final NetworkUuid network, final int window, final int mask) {
        stop(player, window);
        LISTENERS.computeIfAbsent(network, any -> new ArrayList<>()).add(new Listener(player.getUUID(), window,
                mask, null));
    }

    /** Ends {@code player}'s trace in {@code window}, wherever it was. */
    public static void stop(final ServerPlayer player, final int window) {
        final Iterator<Map.Entry<NetworkUuid, List<Listener>>> networks = LISTENERS.entrySet().iterator();
        while (networks.hasNext()) {
            final List<Listener> listeners = networks.next().getValue();
            listeners.removeIf(listener -> player.getUUID().equals(listener.player()) && listener.window() == window);
            if (listeners.isEmpty()) {
                networks.remove();
            }
        }
    }

    /**
     * Hears {@code network}'s work on the server itself, the groups {@code mask} picks going to {@code sink}, the way
     * a Profiler window hears it; the returned action ends it.
     */
    public static Runnable listen(final NetworkUuid network, final int mask, final Consumer<TraceEvent> sink) {
        final Listener listener = new Listener(null, 0, mask, sink);
        LISTENERS.computeIfAbsent(network, any -> new ArrayList<>()).add(listener);
        return () -> {
            final List<Listener> listeners = LISTENERS.get(network);
            if (listeners != null && listeners.remove(listener) && listeners.isEmpty()) {
                LISTENERS.remove(network);
            }
        };
    }

    /** Whether anybody traces {@code network}. */
    public static boolean tracing(@Nullable final NetworkUuid network) {
        return network != null && LISTENERS.containsKey(network);
    }

    /**
     * Tells every trace on {@code network} that watches {@code kind}'s group what {@code event} builds; a trace whose
     * player has left is ended.
     */
    public static void post(final ServerLevel level, @Nullable final NetworkUuid network, final TraceEventClass kind,
                            final Supplier<TraceEvent> event) {
        final List<Listener> listeners = network == null ? null : LISTENERS.get(network);
        if (listeners == null || level.getServer() == null) {
            return;
        }
        TraceEvent built = null;
        final Iterator<Listener> each = listeners.iterator();
        while (each.hasNext()) {
            final Listener listener = each.next();
            if (!kind.group().in(listener.mask())) {
                continue;
            }
            final ServerPlayer player = listener.player() == null ? null
                    : level.getServer().getPlayerList().getPlayer(listener.player());
            if (listener.sink() == null && player == null) {
                each.remove();
                continue;
            }
            if (built == null) {
                built = event.get();
            }
            if (listener.sink() != null) {
                listener.sink().accept(built);
            } else {
                PacketDistributor.sendToPlayer(player, new IsmsTracePayload(listener.window(), List.of(built)));
            }
        }
        if (listeners.isEmpty()) {
            LISTENERS.remove(network);
        }
    }

    /**
     * An event of {@code kind} on {@code network} that says {@code text}, asked for by whoever the work being done now
     * is for: the studio and computer that named themselves, else the player acting, else the network's engine on its
     * Mainframe when it is for nobody in particular.
     */
    public static TraceEvent event(final ServerLevel level, final NetworkUuid network, final TraceEventClass kind,
                                   final Text text, final long items, final long duration, final List<Text> detail) {
        final Origin origin = ORIGIN.get();
        final MainframeBlockEntity mainframe = NetworkLookup.resolveMainframe(level, network);
        final String requester;
        final String computer;
        final ServerPlayer acting = Acting.current().map(id -> level.getServer().getPlayerList().getPlayer(id))
                .orElse(null);
        if (origin != null) {
            requester = origin.requester();
            computer = origin.computer();
        } else if (acting != null) {
            // Work a player set going from some screen of the network is theirs, though no studio named a computer.
            requester = acting.getGameProfile().getName();
            computer = "";
        } else {
            final INetworkEngine engine = mainframe == null ? null : mainframe.runningEngine();
            requester = engine == null ? NETWORK : engineName(engine);
            computer = mainframe == null ? "" : mainframe.hostname();
        }
        return new TraceEvent(kind, text, requester, computer, items, duration, level.getGameTime(), detail);
    }

    /** Runs {@code work} with {@code origin} as who asked for whatever it does. */
    public static <T> T as(final Origin origin, final Supplier<T> work) {
        final Origin outer = ORIGIN.get();
        ORIGIN.set(origin);
        try {
            return work.get();
        } finally {
            if (outer == null) {
                ORIGIN.remove();
            } else {
                ORIGIN.set(outer);
            }
        }
    }

    /** Ends every trace when the server stops, so none outlives the world it watched. */
    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        LISTENERS.clear();
    }

    /* The engine's name as its package is called, in the machine's language, which a trace keeps. */
    private static String engineName(final INetworkEngine engine) {
        final ProgramSpec spec = Programs.get(engine.def().program());
        return spec == null ? engine.def().program().getPath() : Text.of(spec.name()).english();
    }
}
