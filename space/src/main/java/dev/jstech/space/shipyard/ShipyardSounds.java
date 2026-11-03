/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import static dev.jstech.space.SpaceContent.CONTENT;

import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.multiblock.plan.PlanArms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The sounds of the Small Shipyard: each robot arm's motor as it runs to its feeder and swings its block over to its
 * place, and the knock as it stops and sets the block down, heard from where the arm's tool stands as it begins.
 */
public final class ShipyardSounds {

    /** An arm running and swinging its block over, and setting it down with a knock. */
    public static final SoundKey ARM = CONTENT.sound("shipyard/arm").range(24)
            .subtitle("Robot arm moves").register();

    private ShipyardSounds() {
    }

    /** Declares the sounds, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * Each arm of {@code arms} that began a block at {@code now}, the world's time, and was not heard beginning it,
     * heard; {@code heard} keeps when each arm was last heard, one place an arm.
     *
     * @return when each arm was last heard, {@code heard} itself or a new one when the arms are more
     */
    public static long[] armsBegun(final ServerLevel level, final PlanArms arms, final long now, final long[] heard) {
        final int count = arms.arms().size();
        final long[] kept = heard.length == count ? heard : new long[count];
        for (int arm = 0; arm < count; arm++) {
            final PlanArms.Job job = arms.job(arm).orElse(null);
            if (job == null || job.set() || job.start() != now || kept[arm] == now) {
                continue;
            }
            kept[arm] = now;
            final Vec3 tool = job.tool();
            Audio.at(level, tool.x, tool.y, tool.z, ARM, 1.0F, 1.0F);
        }
        return kept;
    }
}
