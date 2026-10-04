/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.nextgre;

import dev.jstech.computers.advancement.Acting;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.NextgreStudioApp;
import dev.jstech.computers.engine.EngineRequirements;
import dev.jstech.computers.engine.INetworkEngine;
import dev.jstech.computers.engine.nextgre.NextgreEngine;
import dev.jstech.computers.engine.nextgre.NextgrePlanView;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.IqlResultPayload;
import dev.jstech.computers.operation.payload.NextgreActionPayload;
import dev.jstech.computers.operation.payload.NextgreStudioPayload;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.Text;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
 * The payloads of the Nextgre Planner Studio: what a studio window asks of its network's NextgreIQL, and what it is
 * shown back. Every answer carries the number of the window that asked, so two studios open at once each hear their
 * own.
 */
public final class NextgrePayloads {

    private static final String EXPLAIN = "EXPLAIN ";
    private static final String ANALYZE = "ANALYZE ";

    private NextgrePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, NextgreActionPayload.TYPE, NextgreActionPayload.STREAM_CODEC,
                ComputerAccess.machine(NextgreActionPayload::hostPos), NextgrePayloads::handleAction);
        registrar.playToClient(NextgreStudioPayload.TYPE, NextgreStudioPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(NextgrePayloads::handleStudio));
    }

    /**
     * Does what a studio on {@code host} asked, as {@code player}, and says what the studio shows now. A network whose
     * Mainframe does not run NextgreIQL says so and shows nothing else.
     */
    public static NextgreStudioPayload act(final ServerLevel level, final IComputerTerminalHost host,
                                           @Nullable final ServerPlayer player, final NextgreActionPayload payload) {
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = net == null ? null : resolveMainframe(level, net);
        final INetworkEngine running = mainframe == null ? null : mainframe.runningEngine();
        final Text network = net == null ? Text.EMPTY : Text.literal(networkLabel(net));
        if (!(running instanceof NextgreEngine nextgre)) {
            final ProgramSpec studio = Programs.get(Programs.NEXTGRE_STUDIO);
            final Text unmet = studio == null ? null : EngineRequirements.unmet(studio, mainframe);
            return new NextgreStudioPayload(payload.window(), false, Text.EMPTY, network,
                    unmet == null ? Text.EMPTY : unmet, Optional.empty(), List.of(), List.of(), List.of());
        }
        boolean ok = true;
        Text message = Text.EMPTY;
        NextgrePlanView plan = null;
        switch (payload.action()) {
            case NextgreActionPayload.EXPLAIN, NextgreActionPayload.EXPLAIN_ANALYZE -> {
                final String statement = (payload.action() == NextgreActionPayload.EXPLAIN ? EXPLAIN
                        : EXPLAIN + ANALYZE) + bare(payload.arg());
                final ServerCliComputer computer = new ServerCliComputer(host, level);
                final AtomicReference<IqlEngine.Outcome> outcome = new AtomicReference<>();
                Acting.as(player, () -> outcome.set(mainframe.networkOperations().query(IqlEngine.viewOf(computer),
                        statement, IqlResultPayload.MAX_ROWS)));
                ok = outcome.get().ok();
                message = outcome.get().said();
                final List<NextgrePlanView> history = nextgre.history(mainframe);
                if (!history.isEmpty() && history.get(0).statement().equals(statement)) {
                    plan = history.get(0);
                }
            }
            case NextgreActionPayload.TOGGLE_RULE -> ok = nextgre.toggleRule(mainframe, payload.arg());
            case NextgreActionPayload.ANALYZE -> message = nextgre.analyze(mainframe);
            case NextgreActionPayload.OPEN, NextgreActionPayload.REFRESH -> plan = number(payload.arg()) < 0 ? null
                    : nextgre.plan(mainframe, number(payload.arg()));
            default -> ok = false;
        }
        final List<NextgreStudioPayload.HistoryRow> rows = new ArrayList<>();
        for (final NextgrePlanView kept : nextgre.history(mainframe)) {
            final NextgrePlanView.Alternative chosen = kept.chosen();
            rows.add(new NextgreStudioPayload.HistoryRow(kept.id(), kept.statement(),
                    chosen == null ? -1L : chosen.cost(), kept.executionTicks(), kept.state(), kept.at(),
                    kept.analyze()));
        }
        final String version = mainframe.installedEngines().getOrDefault(nextgre.def().program(), "");
        final ProgramSpec engineSpec = Programs.get(nextgre.def().program());
        final String engineName = engineSpec == null ? nextgre.def().dialect() : engineSpec.displayName();
        return new NextgreStudioPayload(payload.window(), ok, Text.literal(engineName + " " + version), network,
                message, Optional.ofNullable(plan), rows, nextgre.rules(mainframe), nextgre.statistics(mainframe));
    }

    private static void handleAction(final NextgreActionPayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        if (level.getBlockEntity(payload.hostPos()) instanceof IComputerTerminalHost host) {
            PacketDistributor.sendToPlayer(player, act(level, host, player, payload));
        }
    }

    private static void handleStudio(final NextgreStudioPayload payload, final Player player) {
        NextgreStudioApp.accept(payload);
    }

    /* The statement without an EXPLAIN or EXPLAIN ANALYZE the player typed before it, which the studio adds itself. */
    private static String bare(final String typed) {
        String rest = typed.strip();
        for (final String word : List.of(EXPLAIN, ANALYZE)) {
            if (rest.toUpperCase(Locale.ROOT).startsWith(word)) {
                rest = rest.substring(word.length()).strip();
            }
        }
        return rest;
    }

    private static int number(final String text) {
        try {
            return Integer.parseInt(text.strip());
        } catch (final NumberFormatException e) {
            return -1;
        }
    }
}
