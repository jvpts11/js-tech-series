/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.advancement;

import dev.jstech.core.progression.Milestone;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.space.exploration.SpaceMilestones;
import dev.jstech.space.universe.SolarSystemWorlds;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The firsts of spaceflight as a crew makes them, told where they happen: a rocket launched, orbit reached, the Moon
 * reached, a world landed on, a starship let out of its dock. Each is the player's own advancement event
 * ({@link SpaceEvents}), and a first of the team is the team's milestone too ({@link SpaceMilestones}), whose
 * advancement every member of the team is paid.
 */
public final class SpaceFirsts {

    private SpaceFirsts() {
    }

    /** {@code player} was aboard a rocket as it lifted off, or launched it from its panel. */
    public static void launched(final ServerPlayer player) {
        SpaceEvents.award(player, SpaceEvents.ROCKET_LAUNCHED);
    }

    /** {@code player} went up over a world's ceiling into its orbit, aboard something or alone. */
    public static void inOrbit(final ServerPlayer player) {
        reach(player, SpaceMilestones.PERSON_IN_ORBIT);
    }

    /** {@code player} came into the orbit of the world {@code world} at the end of a cruise. */
    public static void arrived(final ServerPlayer player, final ResourceLocation world) {
        if (world.equals(SolarSystemWorlds.MOON)) {
            // Coming round the Moon, the Earth rises over its horizon.
            SpaceEvents.award(player, SpaceEvents.EARTHRISE);
        }
    }

    /** {@code player} landed on the world {@code world}, aboard what set down whole. */
    public static void landed(final ServerPlayer player, final ResourceLocation world) {
        SpaceEvents.award(player, SpaceEvents.LANDED, world.toString());
        if (world.equals(SolarSystemWorlds.MOON)) {
            reach(player, SpaceMilestones.MOON_LANDING);
        } else if (world.equals(SolarSystemWorlds.MARS)) {
            reach(player, SpaceMilestones.PLANET_LANDING);
            reach(player, SpaceMilestones.PERSON_ON_ANOTHER_PLANET);
        }
    }

    /** The player {@code owner} let a starship out of its dock, their team's own. */
    public static void starship(final MinecraftServer server, final UUID owner) {
        TeamProgress.reach(server, CoreTeams.teamOf(server, owner), SpaceMilestones.STARSHIP);
    }

    private static void reach(final ServerPlayer player, final Milestone milestone) {
        TeamProgress.reach(player.server, CoreTeams.teamOf(player.server, player.getUUID()), milestone);
    }
}
