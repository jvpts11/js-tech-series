/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.matter;

import dev.jstech.core.cable.BreachRules;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.explosion.BlastMath;
import dev.jstech.core.explosion.Explosions;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * What befalls a vessel or a pipe held under pressure, in the world: a tank's relief valve letting a little out, a tank
 * bursting with the force of what it held, and a pipe bursting the same way when what reaches it comes at more than it
 * holds ({@link CableType.Builder#holds}). A burst goes off as one of the Core's blasts ({@link Explosions}) of the
 * energy the gas held above the air around it ({@link BlastMath#burstEnergy}), named by the block that burst, so its
 * mod's setting of what explosions do to the world holds.
 *
 * <pre>{@code
 * private final MatterTank gas = new MatterTank(MatterState.GAS, 1_000L, 200L, this::setChanged)
 *         .relief(240L, 500L).bursts(300L);
 *
 * public void serverTick() {
 *     PressureBursts.hold((ServerLevel) level, worldPosition, gas);
 * }
 * }</pre>
 */
public final class PressureBursts {

    private static final double MILLIBUCKETS_PER_CUBIC_METRE = 1_000;

    private PressureBursts() {
    }

    /**
     * Sees to {@code tank}, kept by the block at {@code pos}, once a tick, from its block entity: its relief valve lets
     * out what it lets out, drawn escaping; and once it is held past what it stands it bursts: it is emptied, its block
     * is gone, what it held is let out and a blast of the energy it held goes off where it stood.
     */
    public static Held hold(final ServerLevel level, final BlockPos pos, final MatterTank tank) {
        final Optional<MatterStack> relieved = tank.relieve();
        relieved.ifPresent(out -> MatterRelease.release(level, pos, tank.state(), out));
        if (!tank.bursting()) {
            return relieved.isPresent() ? Held.RELIEVED : Held.HELD;
        }
        final double joules = tank.burstEnergy();
        final ResourceLocation explosive = BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock());
        tank.release().ifPresent(out -> MatterRelease.release(level, pos, tank.state(), out));
        level.removeBlock(pos, false);
        Explosions.blast(level, Vec3.atCenterOf(pos), explosive, joules).detonate();
        return Held.BURST;
    }

    /**
     * What follows a pipe that bursts ({@link BreachRules#breaksAfter}): a blast, named by its cable, of the energy
     * {@code innerVolume} millibuckets of it held at the pressure that burst it.
     */
    public static BreachRules.IBroken pipe(final long innerVolume) {
        if (innerVolume <= 0) {
            throw new IllegalArgumentException("a pipe holds something inside, not " + innerVolume + " mB");
        }
        return (level, breach) -> Explosions.blast(level, Vec3.atCenterOf(breach.at()), breach.type().id(),
                BlastMath.burstEnergy(breach.pressure(), innerVolume / MILLIBUCKETS_PER_CUBIC_METRE)).detonate();
    }

    /** What a tank did in a tick. */
    public enum Held {
        /** It held, its valve shut. */
        HELD,
        /** Its relief valve let some out, and it held. */
        RELIEVED,
        /** It burst. */
        BURST
    }
}
