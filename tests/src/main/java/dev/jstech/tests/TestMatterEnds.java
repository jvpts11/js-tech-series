/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.matter.MatterTank;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The ends the tests of the lines of matter fill from and into, each with a tank of its own kept by its position: of
 * gas, chiseled copper only gives, as a machine's output does, waxed chiseled copper only takes, as a machine's input
 * does, and the copper grate both takes and gives, as a tank does; of slurry, packed mud only gives and mud bricks only
 * take. Only the test mod gives these blocks the capabilities, and only in development.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestMatterEnds {

    /** Each tank's volume, in millibuckets, and the pressure it holds. */
    public static final long VOLUME = 1_000L;
    public static final long PRESSURE = 64L;

    private static final Map<End, MatterTank> TANKS = new ConcurrentHashMap<>();

    private TestMatterEnds() {
    }

    /** The tank of {@code state} behind the end at {@code pos} in {@code level}'s dimension. */
    public static MatterTank tankAt(final Level level, final BlockPos pos, final MatterState state) {
        return TANKS.computeIfAbsent(new End(level.dimension(), pos.immutable(), state),
                key -> new MatterTank(state, VOLUME, PRESSURE, () -> { }));
    }

    /** Forgets every tank behind the end at {@code pos}, so a test starts with empty ones. */
    public static void clear(final Level level, final BlockPos pos) {
        for (final MatterState state : MatterState.values()) {
            TANKS.remove(new End(level.dimension(), pos.immutable(), state));
        }
    }

    /** How much of {@code material} the tank of {@code state} at {@code pos} holds. */
    public static long amountAt(final Level level, final BlockPos pos, final MatterState state,
                                final Material material) {
        final Optional<MatterStack> held = tankAt(level, pos, state).held();
        return held.isPresent() && held.get().material() == material ? held.get().amount() : 0L;
    }

    /** Lets go of every tank when a world closes, so the next world does not inherit its matter. */
    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        TANKS.clear();
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        end(event, MatterState.GAS, Blocks.CHISELED_COPPER, false, true);
        end(event, MatterState.GAS, Blocks.WAXED_CHISELED_COPPER, true, false);
        end(event, MatterState.GAS, Blocks.COPPER_GRATE, true, true);
        end(event, MatterState.SLURRY, Blocks.PACKED_MUD, false, true);
        end(event, MatterState.SLURRY, Blocks.MUD_BRICKS, true, false);
    }

    private static void end(final RegisterCapabilitiesEvent event, final MatterState state, final Block block,
                            final boolean takes, final boolean gives) {
        event.registerBlock(MatterCapabilities.block(state).orElseThrow(),
                (level, pos, blockState, entity, side) -> new Ends(tankAt(level, pos, state), takes, gives), block);
    }

    /* A place a tank is kept for: the same coordinates in two dimensions, or of two states, are two ends. */
    private record End(ResourceKey<Level> dimension, BlockPos pos, MatterState state) {
    }

    /* A tank seen from outside: taking in, giving out, or both. */
    private record Ends(MatterTank tank, boolean takes, boolean gives) implements IMatterHandler {

        @Override
        public MatterState state() {
            return this.tank.state();
        }

        @Override
        public int tanks() {
            return 1;
        }

        @Override
        public Optional<MatterStack> stack(final int index) {
            return this.tank.stack(index);
        }

        @Override
        public long capacity(final int index) {
            return this.tank.capacity(index);
        }

        @Override
        public long insert(final MatterStack stack, final boolean simulate) {
            return this.takes ? this.tank.insert(stack, simulate) : 0L;
        }

        @Override
        public long extract(final Material material, final long amount, final boolean simulate) {
            return this.gives ? this.tank.extract(material, amount, simulate) : 0L;
        }

        @Override
        public Optional<MatterStack> extractAny(final long amount, final boolean simulate) {
            return this.gives ? this.tank.extractAny(amount, simulate) : Optional.empty();
        }
    }
}
