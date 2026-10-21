/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example.machine;

import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.example.ExampleContent;
import dev.jstech.example.ExampleSettings;
import dev.jstech.example.ExampleState;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The generator: it burns what a furnace burns, one piece at a time, and makes energy every tick a piece burns, as
 * much as the settings say. A cable beside it takes what it holds.
 *
 * <p>It is a machine of the Core's: the Core saves its slot and its energy, offers both to pipes and cables, shows
 * them on the standard machine screen, and ticks it only while it has something to do. It works in {@link #work},
 * whose answer is its state, and sleeps while it waits for fuel or for room until either changes.
 */
public final class GeneratorBlockEntity extends MachineBlockEntity {

    private final IntField burning;
    private final IntField burnLength;

    /** Its one slot, for fuel. */
    public static final int FUEL = 0;
    /** What it holds. */
    public static final int CAPACITY = 20_000;
    /** What it gives to cables at most each tick. */
    public static final int GIVES = 1_000;

    public GeneratorBlockEntity(final BlockPos pos, final BlockState state) {
        super(ExampleContent.GENERATOR_ENTITY.get(), pos, state, 1, CAPACITY, 0, GIVES);
        // Both saved with the block, and both sent to its screen while it is open.
        this.burning = fields().integer("Burning", 0).save().toMenu();
        this.burnLength = fields().integer("BurnLength", 0).save().toMenu();
        getInventory().accepts((slot, stack) -> stack.getBurnTime(null) > 0);
    }

    /** How many ticks the piece it burns has left. */
    public int burning() {
        return burning.get();
    }

    @Override
    protected MachineStatus work(final ServerLevel level) {
        final FieldEnergyStorage energy = Objects.requireNonNull(getEnergy(), "the generator runs on FE");
        if (energy.getEnergyStored() >= energy.getMaxEnergyStored()) {
            return MachineStatus.OUTPUT_FULL;
        }
        if (burning.get() <= 0 && !lightFuel()) {
            return MachineStatus.WAITING;
        }
        burning.add(-1);
        final int made = ExampleSettings.generatorOutput();
        energy.generate(made);
        ExampleState.ENERGY_MADE.update(level.getServer(), total -> total + made);
        return MachineStatus.WORKING;
    }

    @Override
    protected double progressShare() {
        return burnLength.get() == 0 ? 0.0 : (double) burning.get() / burnLength.get();
    }

    /* Takes the next piece of fuel from its slot and lights it, leaving behind what it leaves (a bucket of lava's). */
    private boolean lightFuel() {
        final ItemStack fuel = getInventory().getStackInSlot(FUEL);
        final int length = fuel.getBurnTime(null);
        if (length <= 0) {
            return false;
        }
        final ItemStack left = fuel.getCraftingRemainingItem();
        getInventory().extractItem(FUEL, 1, false);
        if (!left.isEmpty() && getInventory().getStackInSlot(FUEL).isEmpty()) {
            getInventory().setStackInSlot(FUEL, left);
        }
        burning.set(length);
        burnLength.set(length);
        return true;
    }
}
