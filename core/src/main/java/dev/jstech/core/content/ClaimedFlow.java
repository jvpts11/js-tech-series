/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.jetbrains.annotations.ApiStatus;

/**
 * The flowing fluids every fluid of the Core is made as, a mod's declared ones and the materials' liquids alike: they
 * spread as the game's own do, and ask before they flow into a place ({@link ProtectedAction#FLOW}), as the world's own
 * work coming from where they flow from, so a claim keeps out a flood poured beside it. Public only so the Core's
 * builders of fluids in other packages make theirs with it.
 */
@ApiStatus.Internal
public final class ClaimedFlow {

    private ClaimedFlow() {
    }

    /** The still fluid of a source block, asking before it flows. */
    public static final class Source extends BaseFlowingFluid.Source {

        public Source(final BaseFlowingFluid.Properties properties) {
            super(properties);
        }

        @Override
        protected boolean canSpreadTo(final BlockGetter level, final BlockPos from, final BlockState fromState,
                                      final Direction way, final BlockPos to, final BlockState toState,
                                      final FluidState toFluid, final Fluid fluid) {
            return super.canSpreadTo(level, from, fromState, way, to, toState, toFluid, fluid)
                    && mayFlow(level, from, to);
        }
    }

    /** The fluid flowing from a source, asking before it flows on. */
    public static final class Flowing extends BaseFlowingFluid.Flowing {

        public Flowing(final BaseFlowingFluid.Properties properties) {
            super(properties);
        }

        @Override
        protected boolean canSpreadTo(final BlockGetter level, final BlockPos from, final BlockState fromState,
                                      final Direction way, final BlockPos to, final BlockState toState,
                                      final FluidState toFluid, final Fluid fluid) {
            return super.canSpreadTo(level, from, fromState, way, to, toState, toFluid, fluid)
                    && mayFlow(level, from, to);
        }
    }

    /* Whether a fluid flowing from {@code from} may go into {@code to}: on a server, as no protection stops it. */
    private static boolean mayFlow(final BlockGetter level, final BlockPos from, final BlockPos to) {
        return !(level instanceof ServerLevel server) || Protection.may(Actor.world(from), ProtectedAction.FLOW,
                server, to);
    }
}
