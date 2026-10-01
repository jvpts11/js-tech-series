/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The ends the pipe tests pour from and into: chiseled tuff only gives fluid, as a machine's output does, and chiseled
 * tuff bricks only take it, as a machine's input does, each from a tank of its own kept by its position. Only the test
 * mod gives the two blocks the capability, and only in development.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestFluidEnds {

    public static final int CAPACITY = 64_000;

    private static final Map<BlockPos, FluidTank> TANKS = new ConcurrentHashMap<>();

    private TestFluidEnds() {
    }

    /** The tank behind the end at the world's {@code pos}. */
    public static FluidTank tankAt(final BlockPos pos) {
        return TANKS.computeIfAbsent(pos.immutable(), key -> new FluidTank(CAPACITY));
    }

    /** Forgets the tank behind the end at {@code pos}, so a test starts with an empty one. */
    public static void clear(final BlockPos pos) {
        TANKS.remove(pos.immutable());
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(Capabilities.FluidHandler.BLOCK,
                (level, pos, state, entity, side) -> new Ends(tankAt(pos), false), Blocks.CHISELED_TUFF);
        event.registerBlock(Capabilities.FluidHandler.BLOCK,
                (level, pos, state, entity, side) -> new Ends(tankAt(pos), true), Blocks.CHISELED_TUFF_BRICKS);
    }

    /* A tank seen from one side: taking fluid in and giving none, or giving and taking none. */
    private record Ends(FluidTank tank, boolean takesIn) implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(final int index) {
            return this.tank.getFluid();
        }

        @Override
        public int getTankCapacity(final int index) {
            return this.tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(final int index, final FluidStack stack) {
            return this.takesIn;
        }

        @Override
        public int fill(final FluidStack resource, final FluidAction action) {
            return this.takesIn ? this.tank.fill(resource, action) : 0;
        }

        @Override
        public FluidStack drain(final FluidStack resource, final FluidAction action) {
            return this.takesIn ? FluidStack.EMPTY : this.tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(final int maxDrain, final FluidAction action) {
            return this.takesIn ? FluidStack.EMPTY : this.tank.drain(maxDrain, action);
        }
    }
}
