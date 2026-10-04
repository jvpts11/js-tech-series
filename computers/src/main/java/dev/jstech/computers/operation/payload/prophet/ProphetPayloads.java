/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.prophet;

import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.ProphetConsoleApp;
import dev.jstech.computers.engine.EngineRequirements;
import dev.jstech.computers.engine.INetworkEngine;
import dev.jstech.computers.engine.prophet.IProphetStatement;
import dev.jstech.computers.engine.prophet.ProphetEngine;
import dev.jstech.computers.engine.prophet.ProphetTexts;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.IqlResultPayload;
import dev.jstech.computers.operation.payload.ProphetActionPayload;
import dev.jstech.computers.operation.payload.ProphetConsolePayload;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.program.iql.IqlParseResult;
import dev.jstech.computers.program.iql.IqlParser;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.operation.payload.network.NetworkLookup.networkLabel;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;

/**
 * The payloads of the Prophet Reactive Console: what a console window asks of its network's Prophet YourIQL, and what
 * it is shown back. Every answer carries the number of the window that asked.
 */
public final class ProphetPayloads {

    private static final String FORGET = "FORGET ";
    private static final String SEPARATOR = ";";

    private ProphetPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, ProphetActionPayload.TYPE, ProphetActionPayload.STREAM_CODEC,
                ComputerAccess.machine(ProphetActionPayload::hostPos), ProphetPayloads::handleAction);
        registrar.playToClient(ProphetConsolePayload.TYPE, ProphetConsolePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(ProphetPayloads::handleConsole));
    }

    /**
     * Does what a console on {@code host} asked, as {@code player}, and says what the console shows now. A network
     * whose Mainframe does not run Prophet YourIQL says so and shows nothing else.
     */
    public static ProphetConsolePayload act(final ServerLevel level, final IComputerTerminalHost host,
                                            @Nullable final ServerPlayer player, final ProphetActionPayload payload) {
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = net == null ? null : resolveMainframe(level, net);
        final INetworkEngine running = mainframe == null ? null : mainframe.runningEngine();
        final Text network = net == null ? Text.EMPTY : Text.literal(networkLabel(net));
        if (!(running instanceof ProphetEngine prophet)) {
            final ProgramSpec console = Programs.get(Programs.PROPHET_CONSOLE);
            final Text unmet = console == null ? null : EngineRequirements.unmet(console, mainframe);
            return new ProphetConsolePayload(payload.window(), false, Text.EMPTY, network,
                    unmet == null ? Text.EMPTY : unmet, List.of(), List.of(), List.of(),
                    new ProphetEngine.Settings(0, 0L, false), level.getGameTime());
        }
        boolean ok = true;
        Text message = Text.EMPTY;
        switch (payload.action()) {
            case ProphetActionPayload.APPLY, ProphetActionPayload.FORGET -> {
                final String statement = payload.action() == ProphetActionPayload.APPLY ? payload.arg()
                        : FORGET + payload.arg();
                final IqlEngine.Outcome outcome = run(level, host, player, mainframe, statement);
                ok = outcome.ok();
                message = outcome.said();
            }
            case ProphetActionPayload.CHECK -> {
                final Text refused = check(payload.arg());
                ok = refused == null;
                message = refused == null ? ProphetTexts.READS_WELL.text() : refused;
            }
            case ProphetActionPayload.SETTINGS -> prophet.configure(mainframe, settings(payload.arg()));
            default -> { }
        }
        final String version = mainframe.installedEngines().getOrDefault(prophet.def().program(), "");
        final ProgramSpec engineSpec = Programs.get(prophet.def().program());
        final String engineName = engineSpec == null ? prophet.def().dialect() : engineSpec.displayName();
        return new ProphetConsolePayload(payload.window(), ok, Text.literal(engineName + " " + version), network,
                message, prophet.states(mainframe), prophet.watches(mainframe), prophet.reactions(mainframe),
                prophet.settings(mainframe), level.getGameTime());
    }

    private static IqlEngine.Outcome run(final ServerLevel level, final IComputerTerminalHost host,
                                         @Nullable final ServerPlayer player, final MainframeBlockEntity mainframe,
                                         final String statement) {
        final ServerCliComputer computer = new ServerCliComputer(host, level);
        final AtomicReference<IqlEngine.Outcome> outcome = new AtomicReference<>();
        Acting.as(player, () -> outcome.set(mainframe.networkOperations().query(IqlEngine.viewOf(computer),
                statement, IqlResultPayload.MAX_ROWS)));
        return outcome.get();
    }

    /* Why {@code statement} does not read, or null when it reads well, in YourIQL or in the language's core. */
    @Nullable
    private static Text check(final String statement) {
        final IProphetStatement parsed = IProphetStatement.parse(statement);
        if (parsed instanceof IProphetStatement.Malformed bad) {
            return switch (bad.what()) {
                case IProphetStatement.BAD_WATCH -> ProphetTexts.USAGE_WATCH.text();
                case IProphetStatement.BAD_FORGET -> ProphetTexts.USAGE_FORGET.text();
                default -> ProphetTexts.USAGE_KEEP.text();
            };
        }
        if (parsed != null) {
            return null;
        }
        final IqlParseResult core = IqlParser.tryParse(statement);
        return core.ok() ? null : core.error();
    }

    /* Settings written as interval;batch;reacting, any part that does not read left at its default. */
    private static ProphetEngine.Settings settings(final String written) {
        final String[] parts = written.split(SEPARATOR, -1);
        return new ProphetEngine.Settings(parts.length > 0 ? (int) number(parts[0]) : 0,
                parts.length > 1 ? number(parts[1]) : 0L, parts.length < 3 || Boolean.parseBoolean(parts[2].strip()));
    }

    private static long number(final String text) {
        try {
            return Long.parseLong(text.strip());
        } catch (final NumberFormatException e) {
            return 0L;
        }
    }

    private static void handleAction(final ProphetActionPayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IComputerTerminalHost host) {
            PacketDistributor.sendToPlayer(player, act(level, host, player, payload));
        }
    }

    private static void handleConsole(final ProphetConsolePayload payload, final Player player) {
        ProphetConsoleApp.accept(payload);
    }
}
