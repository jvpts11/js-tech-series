/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.pad;

import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.blockentity.FieldFluidTank;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A Pad Service Port as its block entity: where the pad's lines come up. Its water, piped in, and the energy for its
 * pumps feed the deluge, poured over the pad at ignition to dampen the sound and spare the concrete; without a full
 * load of both there is no deluge.
 */
public final class PadServicePortBlockEntity extends SyncedBlockEntity {

    private final FieldFluidTank water;
    private final FieldEnergyStorage energy;

    /** The water a deluge pours, in millibuckets, and the energy its pumps spend doing it, in joules. */
    public static final int DELUGE_WATER = 64_000;
    public static final int DELUGE_ENERGY = 400_000;
    /* How fast its pumps' store fills, in joules a tick. */
    private static final int ENERGY_IN = 20_000;

    public PadServicePortBlockEntity(final BlockPos pos, final BlockState state) {
        super(PadBlocks.PORT_ENTITY.get(), pos, state);
        this.water = fields().fluid("Water", DELUGE_WATER).save().toClient().exposed();
        this.water.setValidator(stack -> stack.is(FluidTags.WATER));
        this.energy = fields().energy("Energy", DELUGE_ENERGY, ENERGY_IN, 0).save().toClient().exposed();
    }

    /** The water it holds for the deluge, in millibuckets. */
    public int water() {
        return this.water.getFluidAmount();
    }

    /** The energy its pumps hold, in joules. */
    public int energy() {
        return this.energy.getEnergyStored();
    }

    /** Whether it holds a whole deluge: its water and its pumps' energy. */
    public boolean ready() {
        return water() >= DELUGE_WATER && energy() >= DELUGE_ENERGY;
    }

    /** Pours the deluge, spending its water and its pumps' energy; false, pouring nothing, where it is not ready. */
    public boolean pour() {
        if (!ready()) {
            return false;
        }
        this.water.drain(DELUGE_WATER, IFluidHandler.FluidAction.EXECUTE);
        this.energy.consume(DELUGE_ENERGY);
        setChanged();
        return true;
    }
}
