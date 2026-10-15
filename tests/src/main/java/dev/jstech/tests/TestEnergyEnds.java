/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.GeneratorPort;
import dev.jstech.core.energy.LoadFollower;
import dev.jstech.core.energy.WrongClass;
import java.util.Map;
import java.util.Set;
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
 * The ends the tests of the test energy's lines feed from and into, each a store of its own kept by its position:
 * cut copper gives low, exposed cut copper takes low, weathered cut copper takes medium, oxidized cut copper gives high,
 * and waxed cut copper takes extra-high; the copper bulb is a low generator that follows its load. An end an arc flash
 * hits is remembered. Only the test mod gives these blocks the capability, and only in development.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class TestEnergyEnds {

    public static final long CAPACITY = 1_000_000L;
    /** What a taking end takes at most in a tick. */
    public static final long TAKES = 1_000L;
    /** What a giving end gives at most in a tick. */
    public static final long GIVES = 100_000L;

    /** The generator's most a tick, its least while it runs, and how much its output changes in a tick at most. */
    public static final long GENERATOR_MOST = 1_000L;
    public static final long GENERATOR_LEAST = 100L;
    public static final long GENERATOR_RAMP = 200L;

    private static final Map<End, EnergyStore> STORES = new ConcurrentHashMap<>();
    private static final Map<End, GeneratorPort> GENERATORS = new ConcurrentHashMap<>();
    private static final Set<End> ARCED = ConcurrentHashMap.newKeySet();

    private TestEnergyEnds() {
    }

    /** The store behind the end at {@code pos} in {@code level}'s dimension, made as {@code block} makes it. */
    public static EnergyStore storeAt(final Level level, final BlockPos pos) {
        final End end = new End(level.dimension(), pos.immutable());
        return STORES.computeIfAbsent(end, key -> make(level.getBlockState(pos).getBlock(), key));
    }

    /** The low generator behind the copper bulb at {@code pos}, which follows its load. */
    public static GeneratorPort generatorAt(final Level level, final BlockPos pos) {
        return GENERATORS.computeIfAbsent(new End(level.dimension(), pos.immutable()),
                key -> new GeneratorPort(TestEnergies.VOLTS, TestEnergies.LOW,
                        new LoadFollower(GENERATOR_MOST, GENERATOR_LEAST, GENERATOR_RAMP)));
    }

    /** Whether an arc flash hit the end at {@code pos}. */
    public static boolean arced(final Level level, final BlockPos pos) {
        return ARCED.contains(new End(level.dimension(), pos.immutable()));
    }

    /** Forgets the end at {@code pos}, so a test starts with an empty store. */
    public static void clear(final Level level, final BlockPos pos) {
        final End end = new End(level.dimension(), pos.immutable());
        STORES.remove(end);
        GENERATORS.remove(end);
        ARCED.remove(end);
    }

    /** Lets go of every store when a world closes. */
    @SubscribeEvent
    public static void onServerStopped(final ServerStoppedEvent event) {
        STORES.clear();
        GENERATORS.clear();
        ARCED.clear();
    }

    @SubscribeEvent
    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlock(EnergyPorts.PORT, (level, pos, state, entity, side) -> storeAt(level, pos),
                Blocks.CUT_COPPER, Blocks.EXPOSED_CUT_COPPER, Blocks.WEATHERED_CUT_COPPER, Blocks.OXIDIZED_CUT_COPPER,
                Blocks.WAXED_CUT_COPPER);
        event.registerBlock(EnergyPorts.PORT, (level, pos, state, entity, side) -> generatorAt(level, pos),
                Blocks.COPPER_BULB);
    }

    private static EnergyStore make(final Block block, final End end) {
        final EnergyStore store;
        if (block == Blocks.CUT_COPPER) {
            store = giving(TestEnergies.LOW);
        } else if (block == Blocks.OXIDIZED_CUT_COPPER) {
            store = giving(TestEnergies.HIGH);
        } else if (block == Blocks.WEATHERED_CUT_COPPER) {
            store = taking(TestEnergies.MEDIUM);
        } else if (block == Blocks.WAXED_CUT_COPPER) {
            store = taking(TestEnergies.EXTRA);
        } else {
            store = taking(TestEnergies.LOW);
        }
        store.onFault(what -> {
            if (what == WrongClass.ARC_FLASH) {
                ARCED.add(end);
            }
        });
        return store;
    }

    private static EnergyStore giving(final EnergyClass of) {
        return new EnergyStore("TestEnergy", TestEnergies.VOLTS, of, CAPACITY, 0L, GIVES, () -> { });
    }

    private static EnergyStore taking(final EnergyClass of) {
        return new EnergyStore("TestEnergy", TestEnergies.VOLTS, of, CAPACITY, TAKES, 0L, () -> { });
    }

    /* A place a store is kept for. */
    private record End(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
