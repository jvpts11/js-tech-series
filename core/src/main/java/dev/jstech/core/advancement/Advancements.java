/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.advancement;

import dev.jstech.core.progression.AxisStepReachedEvent;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.progression.ProgressionAxis;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.progression.TeamProgressEvent;
import dev.jstech.core.team.CoreTeams;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * How a mod reports what its advancements are earned by, and how the Core pays out the steps a player reaches along
 * the progression axes and the milestones their team reaches: at once to whoever is here, and again each time a player
 * joins, so a step or a milestone reached while they were away, or an advancement added since, is earned all the
 * same.
 */
public final class Advancements {

    private Advancements() {
    }

    /** Reports that {@code player} caused {@code event}; nothing happens for nobody, or for a machine acting as one. */
    public static void award(@Nullable final Player player, final ResourceLocation event) {
        award(player, event, "");
    }

    /** The same, with the detail that tells apart the criteria of an advancement asking for all of something. */
    public static void award(@Nullable final Player player, final ResourceLocation event,
                             @Nullable final String detail) {
        if (player instanceof ServerPlayer server && !(player instanceof FakePlayer)) {
            CoreTriggers.EVENT.get().trigger(server, event, detail == null ? "" : detail);
        }
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(AxisStepReachedEvent.class, Advancements::onStepReached);
        NeoForge.EVENT_BUS.addListener(TeamProgressEvent.MilestoneReached.class, Advancements::onMilestoneReached);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, Advancements::onLoggedIn);
    }

    public static void onStepReached(final AxisStepReachedEvent event) {
        final ServerPlayer player = event.server().getPlayerList().getPlayer(event.player());
        if (player != null) {
            CoreTriggers.AXIS_STEP.get().trigger(player, event.axis().id(), event.to().level());
        }
    }

    /*
     * A milestone belongs to the team: every member here earns what it gives. A source of teams that cannot list a
     * team's players leaves the players here to be asked which team they are on.
     */
    public static void onMilestoneReached(final TeamProgressEvent.MilestoneReached event) {
        final List<UUID> members = CoreTeams.membersOf(event.server(), event.team());
        final List<ServerPlayer> here = members.isEmpty() ? event.server().getPlayerList().getPlayers().stream()
                .filter(player -> CoreTeams.teamOf(event.server(), player.getUUID()).equals(event.team())).toList()
                : members.stream().map(event.server().getPlayerList()::getPlayer).filter(Objects::nonNull).toList();
        for (final ServerPlayer player : here) {
            if (!(player instanceof FakePlayer)) {
                CoreTriggers.MILESTONE.get().trigger(player, event.milestone().id());
            }
        }
    }

    public static void onLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            for (final ProgressionAxis<?> axis : ProgressionAxes.all()) {
                final int level = PlayerProgress.reached(player.server, player.getUUID(), axis).level();
                if (level > 0) {
                    CoreTriggers.AXIS_STEP.get().trigger(player, axis.id(), level);
                }
            }
            for (final String milestone : TeamProgress.ofPlayer(player.server, player.getUUID()).milestones()) {
                final ResourceLocation id = ResourceLocation.tryParse(milestone);
                if (id != null) {
                    CoreTriggers.MILESTONE.get().trigger(player, id);
                }
            }
        }
    }
}
