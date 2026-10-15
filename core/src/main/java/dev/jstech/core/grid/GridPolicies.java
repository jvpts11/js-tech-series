/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.energy.LevelEnergy;
import dev.jstech.core.fluid.LevelFluids;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The Core's policies, which a mod's grid takes unless it brings its own: energy, shared by what each machine asks;
 * fluids, from outputs to inputs and tanks; the data network, which carries a network's identity; and nothing, for a
 * grid that only joins.
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

    /** The data network: its grid carries each network's identity and is kept with the network. */
    public static final IGridPolicy NETWORK = kind -> Optional.empty();

    /** Nothing: the grid only joins what it joins, for its mod to read. */
    public static final IGridPolicy JOINS_ONLY = kind -> Optional.empty();

    private GridPolicies() {
    }
}
