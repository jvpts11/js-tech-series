/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.kit;

import dev.jstech.core.blockentity.FieldEnergyStorage;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.testkit.gametest.Expect;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * A scene for machines: a machine of the Core's kit given its inputs and its energy, and run until a recipe comes out
 * of it, its outputs handed to the test.
 *
 * <pre>{@code
 * final KilnBlockEntity kiln = helper.getBlockEntity(KILN);
 * MachineScenes.runRecipe(helper, kiln, outputs -> Expect.equal(helper, KILN, "what the kiln made",
 *         Items.IRON_INGOT, outputs.getFirst().getItem()), new ItemStack(Items.RAW_IRON));
 * }</pre>
 */
public final class MachineScenes {

    private MachineScenes() {
    }

    /** Puts {@code inputs} into the machine's input slots, one to a slot, in order. */
    public static void feed(final ProcessingMachineBlockEntity machine, final ItemStack... inputs) {
        final IItemHandlerModifiable slots = machine.getInventory();
        int input = 0;
        for (final ItemStack stack : inputs) {
            while (input < slots.getSlots() && !machine.layout().isInput(input)) {
                input++;
            }
            if (input >= slots.getSlots()) {
                throw new IllegalArgumentException("the machine has " + input + " input slots, fewer than the "
                        + inputs.length + " inputs given");
            }
            slots.setStackInSlot(input++, stack.copy());
        }
    }

    /** Fills the machine's store of energy, whichever kind it has. */
    public static void power(final MachineBlockEntity machine) {
        final EnergyStore store = machine.power().portStore();
        if (store != null) {
            store.setStored(store.capacity());
            return;
        }
        final FieldEnergyStorage fe = machine.power().feStore();
        if (fe != null) {
            fe.setEnergyStored(fe.getMaxEnergyStored());
        }
    }

    /** What stands in the machine's output slots now. */
    public static List<ItemStack> outputs(final ProcessingMachineBlockEntity machine) {
        final List<ItemStack> out = new ArrayList<>();
        for (int slot = 0; slot < machine.getInventory().getSlots(); slot++) {
            final ItemStack stack = machine.getInventory().getStackInSlot(slot);
            if (machine.layout().isOutput(slot) && !stack.isEmpty()) {
                out.add(stack.copy());
            }
        }
        return out;
    }

    /**
     * Gives the machine {@code inputs} and its energy, waits until something comes out, and hands what came out to
     * {@code check}: the test passes once it holds, and fails if nothing that holds comes out in the test's time.
     */
    public static void runRecipe(final GameTestHelper helper, final ProcessingMachineBlockEntity machine,
                                 final Consumer<List<ItemStack>> check, final ItemStack... inputs) {
        feed(machine, inputs);
        power(machine);
        final BlockPos at = helper.relativePos(machine.getBlockPos());
        helper.succeedWhen(() -> {
            final List<ItemStack> made = outputs(machine);
            Expect.that(helper, at, !made.isEmpty(), "a recipe run to its end: expected an output, the machine "
                    + machine.status(), "nothing");
            check.accept(made);
        });
    }
}
