/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.sable;

import dev.jstech.core.integration.PinnedVersions;
import dev.jstech.core.moving.ForeignMover;
import dev.jstech.core.moving.ForeignMovers;
import dev.jstech.core.moving.IForeignKind;
import dev.jstech.core.optional.OptionalMod;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Soft integration with Sable, whose sub-levels Create Aeronautics flies: the Core's moving structures meet a sub-level
 * as an obstacle that moves, never take a block of the region Sable keeps its sub-levels in, and ask the world where a
 * place of that region is. It reaches Sable only through Sable's companion library, which Sable carries, and starts
 * only on the versions of Sable it is tested against; on another it is left off, with a warning in the log, and the
 * kit then only refuses to take blocks from where Sable keeps them by default.
 */
public final class SableIntegration {

    public static final String MOD_ID = "sable";
    /** Sable, behind its guard. */
    public static final OptionalMod MOD = OptionalMod.of(MOD_ID);
    /** What a player calls the mod. */
    static final String NAME = "Sable";
    /** The versions of Sable the work with it is tested against. */
    public static final String VERSIONS = "[2.0.6,2.1)";
    /*
     * Where Sable keeps its sub-levels by default, in blocks along x and along z: a grid of 128 by 128 plots of 128
     * chunks a side, from the plot 10,000. The kit refuses these blocks on a version it is not tested against.
     */
    private static final int GRID_FROM = 10_000 << 7 << 4;
    private static final int GRID_TO = (10_000 + (1 << 7)) << 7 << 4;

    private static volatile boolean meeting;

    private SableIntegration() {
    }

    public static boolean isLoaded() {
        return MOD.isLoaded();
    }

    /** Whether the kit meets Sable's sub-levels: Sable is installed, of a version the work is tested against. */
    public static boolean meeting() {
        return meeting;
    }

    /** Hands the kit Sable's sub-levels to meet, from the Core's constructor; a no-op when Sable is absent. */
    public static void bootstrap() {
        if (!MOD.isLoaded()) {
            return;
        }
        if (PinnedVersions.covers(MOD, VERSIONS)) {
            meeting = MOD.ifLoaded(() -> SableIntegration::meet);
        }
        if (!meeting) {
            ForeignMovers.declare(new Refused());
        }
    }

    // Kept in its own method so the sub-levels' kind (and Sable's companion behind it) is only loaded here.
    private static void meet() {
        ForeignMovers.declare(new SableSubLevels());
    }

    /* Sable of a version the work is not tested against: nothing of it is met, and none of its blocks taken. */
    private static final class Refused implements IForeignKind {

        @Override
        public String name() {
            return NAME;
        }

        @Override
        public void near(final Level level, final AABB box, final Consumer<ForeignMover> into) {
            // Nothing of Sable is read on a version the work is not tested against.
        }

        @Override
        public boolean holds(final Level level, final BlockPos pos) {
            return pos.getX() >= GRID_FROM && pos.getX() < GRID_TO && pos.getZ() >= GRID_FROM && pos.getZ() < GRID_TO;
        }

        @Override
        public Vec3 inWorld(final Level level, final Vec3 place) {
            return place;
        }
    }
}
