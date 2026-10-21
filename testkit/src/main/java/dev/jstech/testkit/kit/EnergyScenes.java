/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.kit;

import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.testkit.gametest.Expect;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * A scene for energy: what a block holds, read through its port of the Core's energy or its Forge Energy storage,
 * whichever it offers; energy put into it; and a gauge that says how much it gained or lost since it was set, to
 * check what flows along a line.
 *
 * <pre>{@code
 * final EnergyScenes.Gauge lamp = EnergyScenes.gauge(helper, LAMP);
 * EnergyScenes.fill(helper, BATTERY, 10_000);
 * helper.runAfterDelay(40, () -> {
 *     Expect.atLeast(helper, LAMP, "the energy the lamp took", 1, lamp.gained());
 *     helper.succeed();
 * });
 * }</pre>
 */
public final class EnergyScenes {

    private EnergyScenes() {
    }

    /** What the block at the arena's place {@code at} holds, in its energy's units; a failure when it holds none. */
    public static long stored(final GameTestHelper helper, final BlockPos at) {
        return store(helper, at).stored();
    }

    /** How much the block at {@code at} can hold. */
    public static long capacity(final GameTestHelper helper, final BlockPos at) {
        return store(helper, at).capacity();
    }

    /**
     * Puts up to {@code amount} into the block at {@code at}, as a line would.
     *
     * @return how much it took
     */
    public static long fill(final GameTestHelper helper, final BlockPos at, final long amount) {
        return store(helper, at).receive(amount);
    }

    /** A gauge on the block at {@code at}, set to what it holds now. */
    public static Gauge gauge(final GameTestHelper helper, final BlockPos at) {
        final Store store = store(helper, at);
        return new Gauge(store, store.stored());
    }

    private static Store store(final GameTestHelper helper, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(at);
        final Optional<IEnergyPort> port = EnergyPorts.at(level, pos, null);
        if (port.isPresent()) {
            return new PortStore(port.get());
        }
        final IEnergyStorage fe = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (fe != null) {
            return new FeStore(fe);
        }
        throw Expect.failure(helper, at, "energy read: expected a port of energy or a Forge Energy storage, found "
                + level.getBlockState(pos));
    }

    /** What a gauge reads: how much a block gained since the gauge was set, less what it lost. */
    public static final class Gauge {

        private final Store store;
        private long from;

        private Gauge(final Store store, final long from) {
            this.store = store;
            this.from = from;
        }

        /** How much the block gained since the gauge was set; less than nothing when it lost. */
        public long gained() {
            return store.stored() - from;
        }

        /** Sets the gauge again, to what the block holds now. */
        public void reset() {
            this.from = store.stored();
        }
    }

    /* A block's energy, whichever way it offers it. */
    private interface Store {

        long stored();

        long capacity();

        long receive(long amount);
    }

    private record PortStore(IEnergyPort port) implements Store {

        @Override
        public long stored() {
            return port.stored();
        }

        @Override
        public long capacity() {
            return port.capacity();
        }

        @Override
        public long receive(final long amount) {
            return port.receive(amount, false);
        }
    }

    private record FeStore(IEnergyStorage fe) implements Store {

        @Override
        public long stored() {
            return fe.getEnergyStored();
        }

        @Override
        public long capacity() {
            return fe.getMaxEnergyStored();
        }

        @Override
        public long receive(final long amount) {
            return fe.receiveEnergy((int) Math.min(Integer.MAX_VALUE, amount), false);
        }
    }
}
