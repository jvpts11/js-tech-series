/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.eventlog;

import dev.jstech.computers.advancement.MachineOperators;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.core.progression.ProgressionAxis;
import dev.jstech.core.progression.TeamProgressEvent;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * What a team's progress sets off on the data networks it owns. When a team reaches an era of hardware, or a tier of
 * industry, each of its networks writes it into its event log, as an INFO line, and wakes the jobs waiting on it
 * ({@code WHEN ERA standard REACHED}). A network is the team's when its Mainframe's operator is on the team, and a
 * network hears it while its Mainframe is loaded, as it hears the world's events. Nothing else shows: the advancement
 * already marks the era.
 */
@TextHolder
public final class NetworkProgress {

    public static final TextKey ERA_REACHED = TextKey.of("jsc.event_log.era_reached", "Era %s reached by %s");
    public static final TextKey TIER_REACHED = TextKey.of("jsc.event_log.tier_reached", "Tier %s reached by %s");
    /** Who reached it, when the team has no name the server can tell. */
    public static final TextKey SOMEONE = TextKey.of("jsc.event_log.someone", "someone");

    private NetworkProgress() {
    }

    /** Hears each step a team reaches, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(TeamProgressEvent.StepReached.class, NetworkProgress::onReached);
    }

    /* Writes the step into the log of every loaded network the team owns, and tells its jobs. */
    private static void onReached(final TeamProgressEvent.StepReached reached) {
        final TextKey words = wordsFor(reached.axis());
        if (words == null) {
            return;
        }
        final MinecraftServer server = reached.server();
        final Text message = words.with(reached.to().text(), who(server, reached.team()));
        final String axis = reached.axis().id().toString();
        final String step = reached.to().serializedName();
        for (final ServerLevel level : server.getAllLevels()) {
            final NetworkSystem system = NetworkSystem.get(level);
            for (final NetworkUuid network : system.networks()) {
                final MainframeBlockEntity mainframe = mainframeOf(level, system, network);
                if (mainframe != null && ownedBy(server, mainframe, reached.team())) {
                    NetworkEvents.record(level, network, EventLevel.INFO, null, "", message);
                    mainframe.stepReached(axis, step);
                }
            }
        }
    }

    /* The words for a step of {@code axis}: only the eras and the tiers are told. */
    @Nullable
    private static TextKey wordsFor(final ProgressionAxis<?> axis) {
        if (axis.id().equals(SeriesAxes.HARDWARE_ERA.id())) {
            return ERA_REACHED;
        }
        if (axis.id().equals(SeriesAxes.INDUSTRIAL_TIER.id())) {
            return TIER_REACHED;
        }
        return null;
    }

    /* The network's Mainframe, when its chunk is loaded now. */
    @Nullable
    private static MainframeBlockEntity mainframeOf(final ServerLevel level, final NetworkSystem system,
                                                    final NetworkUuid network) {
        final Optional<Long> pos = system.mainframePositionOf(network);
        if (pos.isEmpty()) {
            return null;
        }
        return Loaded.blockEntity(level, BlockPos.of(pos.get())) instanceof MainframeBlockEntity mainframe
                ? mainframe : null;
    }

    /* Whether the Mainframe's operator is on {@code team}. */
    private static boolean ownedBy(final MinecraftServer server, final MainframeBlockEntity mainframe,
                                   final String team) {
        final Optional<UUID> operator = MachineOperators.idOf(mainframe);
        return operator.isPresent() && CoreTeams.teamOf(server, operator.get()).equals(team);
    }

    /* The team's name, or its player's for a player alone. */
    private static Object who(final MinecraftServer server, final String team) {
        final Optional<String> name = CoreTeams.teamName(server, team)
                .or(() -> CoreTeams.soloPlayer(team).flatMap(player -> CoreTeams.nameOf(server, player)));
        return name.isPresent() ? name.get() : SOMEONE.text();
    }
}
