/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.JsCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jetbrains.annotations.Nullable;

/**
 * Surge arresters: a block on a line of energy that takes a lightning surge to earth, so the machines on the line are
 * spared. A line plugs into a block that offers {@link #ARRESTER} on the face it meets, as into a machine; a mod gives
 * its arresters.
 */
public final class SurgeArresters {

    /** A surge arrester, on the face a line meets it. */
    public static final BlockCapability<ISurgeArrester, @Nullable Direction> ARRESTER = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "surge_arrester"), ISurgeArrester.class);

    private SurgeArresters() {
    }

    /** A surge arrester. */
    public interface ISurgeArrester {

        /** It took a surge to earth, at {@code pos} in {@code level}: a mod's arrester may wear, or flash. */
        default void took(final ServerLevel level, final BlockPos pos) {
        }
    }
}
