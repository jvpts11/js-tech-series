/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.machine.DebugSession;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.computers.vm.program.Debugger;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The debugger of Virtual Studio and Virtual Studio Code, on the server: each player debugs at most one program at a
 * time, started on the machine their studio runs on, and is sent what it shows whenever that changes.
 *
 * <p>A state goes out when the program stops or ends, when it printed something, and every so often while it runs, so
 * the credits on the status bar and in the Diagnostic Tools go up as it spends them. A player who leaves the game, or
 * a server that stops, ends the program they were debugging, as the program belongs to its debugger.
 */
public final class DebugPayloads {

    /** The program each player is debugging, by who they are. */
    private static final Map<UUID, Desk> DESKS = new HashMap<>();
    /** How often a running program's state goes out when nothing else about it changed, in ticks. */
    private static final int REFRESH_TICKS = 10;

    private DebugPayloads() {
    }

    /** Registers the payloads. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, DebugRequestPayload.TYPE, DebugRequestPayload.STREAM_CODEC,
                ComputerAccess.machineOrSession(DebugRequestPayload::hostPos), DebugPayloads::handle);
        registrar.playToClient(DebugStatePayload.TYPE, DebugStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> IDebugStateSink.Holder.accept(payload)));
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> endAll());
    }

    /** What the program a player is debugging shows now, as the next state would say it, for a test to read. */
    public static DebugStatePayload stateOf(final ServerPlayer player, final ServerLevel level) {
        final Desk desk = DESKS.get(player.getUUID());
        return desk == null ? DebugStatePayload.none(BlockPos.ZERO) : view(desk, level.getGameTime(), List.of());
    }

    /** Answers what a player's studio asked of its debugger. */
    public static void handle(final DebugRequestPayload request, final ServerPlayer player,
                              final ServerLevel level) {
        final Desk desk = DESKS.get(player.getUUID());
        switch (request.action()) {
            case DebugRequestPayload.START -> start(request, player, level);
            case DebugRequestPayload.STOP -> {
                if (desk != null) {
                    desk.session.end();
                    DESKS.remove(player.getUUID());
                }
                PacketDistributor.sendToPlayer(player, DebugStatePayload.none(request.hostPos()));
            }
            case DebugRequestPayload.WATCHES -> {
                if (desk != null) {
                    desk.watches = List.copyOf(request.lines());
                    send(player, desk, level.getGameTime(), List.of());
                }
            }
            default -> {
                if (desk != null && !desk.session.ended()) {
                    steer(desk, request);
                    send(player, desk, level.getGameTime(), desk.session.output());
                }
            }
        }
    }

    /* What a request asks of a program already being debugged. */
    private static void steer(final Desk desk, final DebugRequestPayload request) {
        final Debugger debugger = desk.session.debugger();
        switch (request.action()) {
            case DebugRequestPayload.CONTINUE -> debugger.resume();
            case DebugRequestPayload.STEP_INTO -> debugger.stepInto();
            case DebugRequestPayload.STEP_OVER -> debugger.stepOver();
            case DebugRequestPayload.STEP_OUT -> debugger.stepOut();
            case DebugRequestPayload.PAUSE -> debugger.interrupt();
            case DebugRequestPayload.BREAKPOINTS -> {
                debugger.deleteAll();
                placeBreakpoints(debugger, request.lines());
            }
            case DebugRequestPayload.INPUT -> {
                if (desk.session.waitingForInput() && !request.lines().isEmpty()) {
                    desk.session.offerInput(request.lines().getFirst());
                }
            }
            default -> { }
        }
    }

    /*
     * Starts the program under the debugger on the machine the studio runs on, ending the one the player was debugging
     * before, with the studio's breakpoints in place before it runs a single instruction.
     */
    private static void start(final DebugRequestPayload request, final ServerPlayer player, final ServerLevel level) {
        final Desk before = DESKS.remove(player.getUUID());
        if (before != null) {
            before.session.end();
        }
        if (!(level.getBlockEntity(request.hostPos()) instanceof IComputerTerminalHost host)) {
            return;
        }
        final DebugSession.Started started = new ServerCliComputer(host, level).debugSigma(request.listing(),
                List.of());
        if (started == null || !started.ok()) {
            final Text why = started == null ? Text.literal("") : started.refusal();
            PacketDistributor.sendToPlayer(player, new DebugStatePayload(request.hostPos(), DebugStatePayload.ENDED,
                    "", 0, why, List.of(), List.of(), List.of(), 0, 0, List.of(), 0, 0, false, List.of()));
            return;
        }
        placeBreakpoints(started.session().debugger(), request.lines());
        final Desk desk = new Desk(player, level.dimension(), request.hostPos(), started.session(),
                level.getGameTime());
        DESKS.put(player.getUUID(), desk);
        send(player, desk, level.getGameTime(), List.of());
    }

    /* Places each breakpoint written "file:line". */
    private static void placeBreakpoints(final Debugger debugger, final List<String> written) {
        for (final String each : written) {
            final int colon = each.lastIndexOf(':');
            if (colon <= 0) {
                continue;
            }
            try {
                debugger.breakAt(each.substring(0, colon), Integer.parseInt(each.substring(colon + 1).trim()));
            } catch (final NumberFormatException notALine) {
                // A breakpoint that names no line stands nowhere, and is passed over.
            }
        }
    }

    private static void tick(final MinecraftServer server) {
        final Iterator<Map.Entry<UUID, Desk>> each = DESKS.entrySet().iterator();
        while (each.hasNext()) {
            final Map.Entry<UUID, Desk> entry = each.next();
            final Desk desk = entry.getValue();
            // The player as the server holds them now, who is someone else after a respawn; gone once they left.
            final ServerPlayer found = server.getPlayerList().getPlayer(entry.getKey());
            final ServerPlayer player = found != null ? found : desk.player.hasDisconnected() ? null : desk.player;
            final ServerLevel level = server.getLevel(desk.dimension);
            if (player == null || level == null) {
                desk.session.end();
                each.remove();
                continue;
            }
            final long now = level.getGameTime();
            final List<String> output = desk.session.output();
            final boolean ended = desk.session.ended();
            final boolean paused = !ended && desk.session.debugger().paused();
            if (ended || paused != desk.paused || !output.isEmpty() || (!paused && now - desk.sent >= REFRESH_TICKS)) {
                send(player, desk, now, output);
            }
            if (ended) {
                each.remove();
            }
        }
    }

    private static void endAll() {
        DESKS.values().forEach(desk -> desk.session.end());
        DESKS.clear();
    }

    private static void send(final ServerPlayer player, final Desk desk, final long now, final List<String> output) {
        desk.sent = now;
        desk.paused = !desk.session.ended() && desk.session.debugger().paused();
        PacketDistributor.sendToPlayer(player, view(desk, now, output));
    }

    /* What the studio shows of the program now. */
    private static DebugStatePayload view(final Desk desk, final long now, final List<String> output) {
        final DebugSession session = desk.session;
        final Debugger debugger = session.debugger();
        final List<DebugStatePayload.Cost> costs = new ArrayList<>();
        debugger.calls().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .forEach(cost -> costs.add(new DebugStatePayload.Cost(cost.getKey(), cost.getValue())));
        if (session.ended()) {
            return new DebugStatePayload(desk.host, DebugStatePayload.ENDED, "", session.exitCode(),
                    session.halted() ? session.haltMessage() : Text.literal(""), List.of(), List.of(), List.of(),
                    debugger.spent(), debugger.steps(), costs, 0, session.elapsed(now), false, output);
        }
        final boolean paused = debugger.paused();
        final List<DebugStatePayload.Frame> stack = new ArrayList<>();
        for (final Debugger.Place place : debugger.stack()) {
            stack.add(new DebugStatePayload.Frame(place.method(), place.file(), place.line(), place.column(),
                    variables(place.arguments())));
        }
        final List<DebugStatePayload.Variable> watches = new ArrayList<>();
        for (final String name : desk.watches) {
            final DebugStatePayload.Variable unknown = new DebugStatePayload.Variable(name, "", "");
            watches.add(paused ? debugger.find(name, 0).map(DebugPayloads::variable).orElse(unknown) : unknown);
        }
        final String reason = paused ? debugger.stop().name().toLowerCase(Locale.ROOT) : "";
        return new DebugStatePayload(desk.host, paused ? DebugStatePayload.PAUSED : DebugStatePayload.RUNNING, reason,
                0, Text.literal(""), stack, paused ? variables(debugger.locals(0)) : List.of(), watches,
                debugger.spent(), debugger.steps(), costs, session.memoryBytes(), session.elapsed(now),
                session.waitingForInput(), output);
    }

    private static List<DebugStatePayload.Variable> variables(final List<Debugger.Variable> variables) {
        return variables.stream().map(DebugPayloads::variable).toList();
    }

    private static DebugStatePayload.Variable variable(final Debugger.Variable variable) {
        return new DebugStatePayload.Variable(variable.name(), variable.type(), variable.value());
    }

    /** The program one player is debugging, and what was last sent of it. */
    private static final class Desk {

        private final ServerPlayer player;
        private final ResourceKey<Level> dimension;
        private final BlockPos host;
        private final DebugSession session;
        private List<String> watches = List.of();
        private long sent;
        private boolean paused;

        Desk(final ServerPlayer player, final ResourceKey<Level> dimension, final BlockPos host,
             final DebugSession session, final long now) {
            this.player = player;
            this.dimension = dimension;
            this.host = host.immutable();
            this.session = session;
            this.sent = now;
        }
    }
}
