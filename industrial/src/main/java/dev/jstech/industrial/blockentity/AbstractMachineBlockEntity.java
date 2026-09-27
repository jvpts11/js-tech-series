/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Base for the industrial machines: an item inventory and an FE buffer, both saved, both offered to pipes and cables
 * on every side, the stored energy shown to the machine's menu and the items spilled when the machine is broken.
 */
public abstract class AbstractMachineBlockEntity extends SyncedBlockEntity {

    private final FieldItemHandler inventory;
    private final FieldEnergyStorage energy;

    protected AbstractMachineBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                         final int inventorySize, final int energyCapacity,
                                         final int energyMaxReceive, final int energyMaxExtract) {
        super(type, pos, state);
        this.inventory = fields().items("Inventory", inventorySize).save().exposed().dropsWhenBroken();
        this.energy = fields().energy("Energy", energyCapacity, energyMaxReceive, energyMaxExtract)
                .save().toMenu().exposed();
    }

    public FieldItemHandler getInventory() {
        return inventory;
    }

    public FieldEnergyStorage getEnergy() {
        return energy;
    }
}
