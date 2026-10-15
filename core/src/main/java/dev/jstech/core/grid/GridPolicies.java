/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.energy.LevelEnergy;
import dev.jstech.core.fluid.LevelFluids;
import dev.jstech.core.inventory.LevelItems;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.LevelMatter;
import dev.jstech.core.matter.MatterCapabilities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The Core's policies, which a mod's grid takes unless it brings its own: energy, shared by what each machine asks;
 * fluids, from outputs to inputs and tanks; each other state of matter, and items, the same way; groups, for a line
 * that joins machines to work together; the data network, which carries a network's identity; and nothing, for a grid
 * that only joins.
 */
public final class GridPolicies {

    /** Energy, as the game's energy capability gives and takes it: each wire plugs into any block that offers it. */
    public static final IGridPolicy ENERGY = new IGridPolicy() {
        @Override
        public Optional<IGridMover> mover(final GridKind kind) {
            return Optional.of(new LevelEnergy(kind));
        }

        @Override
        public boolean plugsInto(final ServerLevel level, final BlockPos pos, final Direction face) {
            return level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, face) != null;
        }
    };

    /** Fluids, as the game's fluid handler holds them: each pipe plugs into any block that holds fluids. */
    public static final IGridPolicy FLUID = new IGridPolicy() {
        @Override
        public Optional<IGridMover> mover(final GridKind kind) {
            return Optional.of(new LevelFluids(kind));
        }

        @Override
        public boolean plugsInto(final ServerLevel level, final BlockPos pos, final Direction face) {
            return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, face) != null;
        }
    };

    /** Items, as the game's item handler holds them: each line plugs into any block with an inventory. */
    public static final IGridPolicy ITEMS = new IGridPolicy() {
        @Override
        public Optional<IGridMover> mover(final GridKind kind) {
            return Optional.of(new LevelItems(kind));
        }

        @Override
        public boolean plugsInto(final ServerLevel level, final BlockPos pos, final Direction face) {
            return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, face) != null;
        }
    };

    /**
     * Nothing carried: each connected run groups the machines it joins ({@link LineGroups}), which plug in by offering
     * {@link LineGroups#MEMBER}.
     */
    public static final IGridPolicy GROUP = new IGridPolicy() {
        @Override
        public Optional<IGridMover> mover(final GridKind kind) {
            return Optional.of(new LineGroups(kind));
        }

        @Override
        public boolean plugsInto(final ServerLevel level, final BlockPos pos, final Direction face) {
            return LineGroups.plugsInto(level, pos, face);
        }
    };

    /** The data network: its grid carries each network's identity and is kept with the network. */
    public static final IGridPolicy NETWORK = kind -> Optional.empty();

    /** Nothing: the grid only joins what it joins, for its mod to read. */
    public static final IGridPolicy JOINS_ONLY = kind -> Optional.empty();

    private GridPolicies() {
    }

    /**
     * A mod's own energy, along the lines of one of its classes: each line plugs into any block with a port of that
     * energy, of any class, so a port of another class is told what befalls it.
     */
    public static IGridPolicy energy(final Energy energy, final EnergyClass lineClass) {
        return new IGridPolicy() {
            @Override
            public Optional<IGridMover> mover(final GridKind kind) {
                return Optional.of(new LevelEnergy(kind, energy, lineClass));
            }

            @Override
            public boolean plugsInto(final ServerLevel level, final BlockPos pos, final Direction face) {
                return LevelEnergy.plugsInto(energy, level, pos, face);
            }
        };
    }

    /**
     * A state of matter that is neither solid nor liquid, as the Core's stores of it hold it, and other mods' through
     * their bridges: each line plugs into any block that keeps that state.
     *
     * @throws IllegalArgumentException for a solid or a liquid, which have the game's own items and fluids
     */
    public static IGridPolicy matter(final MatterState state) {
        if (MatterCapabilities.block(state).isEmpty()) {
            throw new IllegalArgumentException("a line of matter carries no " + state.serializedName()
                    + ", which the game's own items and fluids hold");
        }
        return new IGridPolicy() {
            @Override
            public Optional<IGridMover> mover(final GridKind kind) {
                return Optional.of(new LevelMatter(kind, state));
            }

            @Override
            public boolean plugsInto(final ServerLevel level, final BlockPos pos, final Direction face) {
                return LevelMatter.plugsInto(state, level, pos, face);
            }
        };
    }
}
