/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableParts;
import dev.jstech.core.diagnostic.Diagnostics;
import dev.jstech.core.energy.internal.EnergyNetwork;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.Grid;
import dev.jstech.core.grid.GridKind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * The energy grid of one dimension as it moves energy: each connected part of the grid with the machines its wires
 * plug into, and what each part moved in the last tick. The parts are worked out again only when the grid's shape
 * changes or a wire comes to meet another machine; a tick only shares the energy out. Nothing here is saved: it is
 * worked out from the wires as they load.
 *
 * <p>A machine gives energy when it can give and not take (a generator), takes it when it can take and not give, and
 * holds it when it can do both, as the game's energy capability on the face a wire plugs into says.
 */
public final class LevelEnergy {

    private final List<EnergyNetwork> parts = new ArrayList<>();
    private final Map<Long, Integer> partOfWire = new HashMap<>();
    private List<EnergyDistributionResult> lastTick = List.of();
    private long builtVersion = -1L;
    private long builtTaps = -1L;
    private long taps;

    /** What the debug screen calls the time the energy grid takes to move a tick's energy. */
    public static final ResourceLocation TIMING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "energy_grid");

    /** What each connected part of the grid moved in the last tick. */
    public List<EnergyDistributionResult> lastTick() {
        return this.lastTick;
    }

    /** What the part of the grid the wire numbered {@code wire} is in moved in the last tick; empty for none. */
    public Optional<EnergyDistributionResult> lastTickOf(final long wire) {
        final Integer part = this.partOfWire.get(wire);
        return part == null || part >= this.lastTick.size() ? Optional.empty()
                : Optional.of(this.lastTick.get(part));
    }

    /** A wire came to meet a machine or stopped meeting one: the parts are worked out again before the next tick. */
    public void tapsChanged() {
        this.taps++;
    }

    /** Moves the tick's energy through every part of the grid of {@code level}. */
    public void tick(final ServerLevel level) {
        final Grid grid = CoreGrids.of(level, GridKind.POWER);
        if (grid.size() == 0) {
            this.parts.clear();
            this.partOfWire.clear();
            this.lastTick = List.of();
            return;
        }
        if (grid.version() != this.builtVersion || this.taps != this.builtTaps) {
            build(level, grid);
            this.builtVersion = grid.version();
            this.builtTaps = this.taps;
        }
        final long start = System.nanoTime();
        final List<EnergyDistributionResult> results = new ArrayList<>(this.parts.size());
        for (final EnergyNetwork part : this.parts) {
            results.add(part.tickDistribute());
        }
        this.lastTick = results;
        Diagnostics.record(TIMING, start);
    }

    /*
     * Each wire of a part is a cable of its network; each machine a wire plugs into is a node of it, numbered below
     * zero so it never meets a wire's number.
     */
    private void build(final ServerLevel level, final Grid grid) {
        this.parts.clear();
        this.partOfWire.clear();
        for (final CableParts.Part part : CableParts.of(level, grid)) {
            final EnergyNetwork network = new EnergyNetwork();
            for (final CableParts.WireAt wire : part.wires()) {
                final long carries = wire.runTooLong() ? 0L : wire.type().throughput();
                network.addCable(wire.number(), new WireCable(carries, wire.type().loss()));
                this.partOfWire.put(wire.number(), this.parts.size());
            }
            for (final CableParts.WireAt wire : part.wires()) {
                for (final long next : grid.neighbours(wire.number())) {
                    if (next > wire.number() && network.contains(next)) {
                        network.connect(wire.number(), next);
                    }
                }
            }
            final Map<Long, Long> machines = new HashMap<>();
            for (final CableParts.Plug plug : part.plugs()) {
                Long node = machines.get(plug.at().asLong());
                if (node == null) {
                    node = -(machines.size() + 1L);
                    machines.put(plug.at().asLong(), node);
                    network.addNode(node, new MachineNode(BlockCapabilityCache.create(
                            Capabilities.EnergyStorage.BLOCK, level, plug.at(), plug.face())));
                }
                network.connect(plug.wire(), node);
            }
            this.parts.add(network);
        }
    }

    /* A wire as a cable of its part: what it carries and what it loses. */
    private record WireCable(long maxThroughput, int loss) implements IEnergyCable {
    }

    /* A machine a wire plugs into, seen through the game's energy capability on the face the wire meets. */
    private record MachineNode(BlockCapabilityCache<IEnergyStorage, @Nullable Direction> cache)
            implements IEnergyNode {

        @Override
        public EnergyNodeRole role() {
            final IEnergyStorage storage = this.cache.getCapability();
            if (storage == null) {
                return EnergyNodeRole.CONSUMER;
            }
            if (storage.canExtract() && storage.canReceive()) {
                return EnergyNodeRole.STORAGE;
            }
            return storage.canExtract() ? EnergyNodeRole.GENERATOR : EnergyNodeRole.CONSUMER;
        }

        @Override
        public long supply() {
            final IEnergyStorage storage = this.cache.getCapability();
            return storage == null || !storage.canExtract() ? 0L
                    : Math.max(0, storage.extractEnergy(Integer.MAX_VALUE, true));
        }

        @Override
        public long demand() {
            final IEnergyStorage storage = this.cache.getCapability();
            return storage == null || !storage.canReceive() ? 0L
                    : Math.max(0, storage.receiveEnergy(Integer.MAX_VALUE, true));
        }

        @Override
        public void onSupplied(final long amount) {
            final IEnergyStorage storage = this.cache.getCapability();
            long left = amount;
            while (storage != null && left > 0) {
                final int given = storage.extractEnergy((int) Math.min(Integer.MAX_VALUE, left), false);
                if (given <= 0) {
                    return;
                }
                left -= given;
            }
        }

        @Override
        public void onConsumed(final long amount) {
            final IEnergyStorage storage = this.cache.getCapability();
            long left = amount;
            while (storage != null && left > 0) {
                final int taken = storage.receiveEnergy((int) Math.min(Integer.MAX_VALUE, left), false);
                if (taken <= 0) {
                    return;
                }
                left -= taken;
            }
        }
    }
}
