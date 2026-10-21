/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import dev.jstech.core.team.CoreTeams;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * How far a player's team has come toward a goal a quest mod sets, by id as a quest's file writes it: how much of a
 * piece of knowledge it knows, whether it passed a milestone, whether it reached a step along an axis. A goal naming
 * something nothing declared is never met, so a quest written for a mod that is not installed waits rather than
 * failing; FTB Quests' tasks of the Core ask these.
 */
public final class QuestGoals {

    private QuestGoals() {
    }

    /** How much of the knowledge {@code knowledge} the team of {@code player} knows, from 0 to 1. */
    public static double knowledge(final MinecraftServer server, final UUID player, final String knowledge) {
        final ResourceLocation id = ResourceLocation.tryParse(knowledge);
        if (id == null || Knowledges.byId(id).isEmpty()) {
            return 0.0;
        }
        return TeamProgress.share(server, CoreTeams.teamOf(server, player), id);
    }

    /** Whether the team of {@code player} has passed the milestone {@code milestone}. */
    public static boolean milestone(final MinecraftServer server, final UUID player, final String milestone) {
        final ResourceLocation id = ResourceLocation.tryParse(milestone);
        final Optional<Milestone> found = id == null ? Optional.empty() : Milestones.byId(id);
        return found.isPresent() && TeamProgress.reached(server, CoreTeams.teamOf(server, player), found.get());
    }

    /** Whether the team of {@code player} has reached the step named {@code step} along {@code axis}, or past it. */
    public static boolean step(final MinecraftServer server, final UUID player, final String axis,
                               final String step) {
        final ResourceLocation id = ResourceLocation.tryParse(axis);
        final @Nullable ProgressionAxis<?> found = id == null ? null : ProgressionAxes.byId(id);
        return found != null && reached(server, CoreTeams.teamOf(server, player), found, step);
    }

    private static <S extends IAxisStep> boolean reached(final MinecraftServer server, final String team,
                                                         final ProgressionAxis<S> axis, final String step) {
        final S named = axis.byName(step);
        return named != null && TeamProgress.reached(server, team, axis).level() >= named.level();
    }
}
