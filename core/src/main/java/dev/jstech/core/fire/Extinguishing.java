/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import dev.jstech.core.JsCore;
import dev.jstech.core.energy.EnergyDamage;
import dev.jstech.core.explosion.BlastMath;
import dev.jstech.core.explosion.Explosions;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Putting a fire out with an agent, as the agent does on the fire's class: it goes out; it goes out and may light
 * again a moment later; nothing happens; whoever sprays it takes the current; the burning liquid spreads and the fire
 * flares up round it; or the burning metal bursts. An extinguisher, a sprinkler or a mod's machine calls it.
 */
public final class Extinguishing {

    /** The explosive a burning metal struck by water bursts as, for the servers' settings of explosions. */
    public static final ResourceLocation BURNING_METAL = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "burning_metal");
    /* How long a fire that may light again smoulders first, in ticks, and the chance it does. */
    private static final long SMOULDERS_FOR = 60;
    private static final double LIGHTS_AGAIN = 0.5;
    /* What the current costs whoever sprays it on live equipment, in half hearts. */
    private static final float SHOCK = 4F;
    /* How many places round a flaring fire catch, and how far round. */
    private static final int FLARE_LIGHTS = 6;
    private static final int FLARE_REACH = 1;
    /* How strong a burning metal's burst is, in kilograms of TNT. */
    private static final double BURST_TNT = 0.05;

    private Extinguishing() {
    }

    /**
     * Puts the fire at {@code fire} out with {@code agent}, sprayed by {@code by} if anyone; what the agent did. A
     * place with no fire is left as it is.
     */
    public static ExtinguishingAgent.Outcome extinguish(final ServerLevel level, final BlockPos fire,
                                                        final ExtinguishingAgent agent, @Nullable final Entity by) {
        if (!(level.getBlockState(fire).getBlock() instanceof BaseFireBlock)) {
            return ExtinguishingAgent.Outcome.NOTHING;
        }
        final ExtinguishingAgent.Outcome outcome = agent.on(FireClasses.at(level, fire));
        switch (outcome) {
            case PUTS_OUT -> level.removeBlock(fire, false);
            case REKINDLES -> {
                level.removeBlock(fire, false);
                if (level.random.nextDouble() < LIGHTS_AGAIN) {
                    Fires.of(level).rekindle(fire, SMOULDERS_FOR);
                }
            }
            case NOTHING -> {
                // The agent does nothing to a fire of this class.
            }
            case SHOCKS -> {
                if (by instanceof LivingEntity sprayer) {
                    sprayer.hurt(EnergyDamage.electric(level), SHOCK);
                }
            }
            case FLARES -> flare(level, fire, by);
            case REACTS -> Explosions.blast(level, Vec3.atCenterOf(fire), BURNING_METAL,
                    BURST_TNT * BlastMath.JOULES_PER_KILOGRAM_OF_TNT).by(by).incendiary().detonate();
        }
        return outcome;
    }

    /* The burning liquid spreads: the fire catches on what stands round it, as the world and the sprayer may. */
    private static void flare(final ServerLevel level, final BlockPos fire, @Nullable final Entity by) {
        final Actor actor = by instanceof ServerPlayer player ? Actor.of(player) : Actor.world(fire);
        int lit = 0;
        for (final BlockPos near : BlockPos.betweenClosed(fire.offset(-FLARE_REACH, 0, -FLARE_REACH),
                fire.offset(FLARE_REACH, FLARE_REACH, FLARE_REACH))) {
            if (lit >= FLARE_LIGHTS) {
                return;
            }
            if (Loaded.at(level, near) && level.getBlockState(near).isAir()
                    && BaseFireBlock.canBePlacedAt(level, near, Direction.UP)
                    && Protection.may(actor, ProtectedAction.BURN, level, near)) {
                level.setBlockAndUpdate(near, BaseFireBlock.getState(level, near));
                lit++;
            }
        }
    }
}
