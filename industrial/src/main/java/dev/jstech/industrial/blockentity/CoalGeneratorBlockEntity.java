/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.machine.FaceSlot;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineSides;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.industrial.IndustrialModule;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Coal Generator, the first FE source: burns furnace fuel (coal, charcoal and the like) to make
 * {@value #FE_PER_TICK} FE per tick while it has burn time left and room for what it makes.
 *
 * <p>It is a machine of the Core's, worked by its kit: it shows its fuel slot and its energy on the screen every
 * machine has, and pushes what it stores into the blocks beside it through the faces its Sides tab gives energy out
 * of, as it does from the start, at most {@value #ENERGY_MAX_EXTRACT} FE a tick.
 */
public class CoalGeneratorBlockEntity extends MachineBlockEntity {

    private final IntField burnTime;
    private final IntField maxBurnTime;

    public static final int FUEL_SLOT = 0;
    public static final int FE_PER_TICK = 20;
    private static final int ENERGY_CAPACITY = 16_000;
    private static final int ENERGY_MAX_EXTRACT = 1_000;
    private static final int TICKS_A_SECOND = 20;

    public CoalGeneratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(IndustrialModule.COAL_GENERATOR_BE.get(), pos, state, 1, ENERGY_CAPACITY, 0, ENERGY_MAX_EXTRACT);
        this.burnTime = fields().integer("BurnTime", 0).save().toMenu();
        this.maxBurnTime = fields().integer("MaxBurnTime", 0).save().toMenu();
        getInventory().accepts((slot, stack) -> stack.getBurnTime(null) > 0);
        // A generator placed beside a machine feeds it, unless its Sides tab says otherwise.
        sides().setAutomation(MachineResource.ENERGY,
                MachineSides.Automation.NONE.pushing(true, ENERGY_MAX_EXTRACT * TICKS_A_SECOND));
    }

    public boolean isBurning() {
        return burnTime.get() > 0;
    }

    @Override
    public List<FaceSlot> mainSlots() {
        final var at = MachineScreenLayout.laneSlot(0, 1, 0, false, 1);
        return List.of(new FaceSlot(FUEL_SLOT, at.x(), at.y(), false));
    }

    @Override
    protected MachineStatus work(final ServerLevel level) {
        final FieldEnergyStorage energy = Objects.requireNonNull(getEnergy(), "the Coal Generator runs on FE");
        if (energy.getEnergyStored() >= energy.getMaxEnergyStored()) {
            return MachineStatus.OUTPUT_FULL;
        }
        if (burnTime.get() <= 0 && !lightFuel()) {
            return MachineStatus.WAITING;
        }
        burnTime.add(-1);
        energy.generate(FE_PER_TICK);
        return MachineStatus.WORKING;
    }

    @Override
    protected double progressShare() {
        return maxBurnTime.get() == 0 ? 0.0 : (double) burnTime.get() / maxBurnTime.get();
    }

    /* Takes the next piece of fuel from its slot and lights it, giving back what it leaves (a lava bucket's bucket). */
    private boolean lightFuel() {
        final ItemStack fuel = getInventory().getStackInSlot(FUEL_SLOT);
        final int duration = fuel.getBurnTime(null);
        if (duration <= 0) {
            return false;
        }
        final ItemStack remainder = fuel.getCraftingRemainingItem();
        getInventory().extractItem(FUEL_SLOT, 1, false);
        if (!remainder.isEmpty() && getInventory().getStackInSlot(FUEL_SLOT).isEmpty()) {
            getInventory().setStackInSlot(FUEL_SLOT, remainder);
        }
        burnTime.set(duration);
        maxBurnTime.set(duration);
        return true;
    }
}
