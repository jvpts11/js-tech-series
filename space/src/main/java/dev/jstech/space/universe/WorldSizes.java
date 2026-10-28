/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import dev.jstech.core.dimension.DimensionBorders;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * How big each round world is, fixed the first time it loads: the Overworld's border times the world's width over the
 * Earth's (the Moon 27%, Mars 53%), never more than the Overworld's, its border at the middle of its far side (±180°)
 * and at its poles, kept by J's Core's border of its own however the Overworld's changes after. The Overworld keeps the
 * game's own.
 */
public final class WorldSizes {

    private WorldSizes() {
    }

    /**
     * Hands the game's bus what sizes the worlds, once, from the mod's constructor: as the server is ready, since the
     * Overworld's border is only set from its saved settings once every world has been made, and then each world
     * made while the game runs.
     */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> event.getServer().getAllLevels()
                .forEach(WorldSizes::fix));
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getServer().isReady()) {
                fix(level);
            }
        });
    }

    /**
     * Fixes the size of the round world {@code level} is, if it is one and has none yet.
     *
     * @return its bounds, if it is a round world
     */
    public static Optional<DimensionBorders.Bounds> fix(final ServerLevel level) {
        if (level.dimension().equals(Level.OVERWORLD)) {
            return Optional.empty();
        }
        final Optional<SpaceWorld> world = SpaceWorlds.ofDimension(level, level.dimension());
        if (world.isEmpty()) {
            return Optional.empty();
        }
        final double width = SphereMap.widthOf(level.getServer().overworld().getWorldBorder().getSize(),
                world.get().diameter());
        return Optional.of(DimensionBorders.fix(level, new DimensionBorders.Bounds(-width / 2, width / 2,
                -width / 4, width / 4)));
    }
}
