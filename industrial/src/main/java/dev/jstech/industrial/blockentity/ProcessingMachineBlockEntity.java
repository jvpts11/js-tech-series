/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.blockentity;

import dev.jstech.core.blockentity.IntField;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * A machine that turns one input into one output: while its input makes something and the output has room, it spends
 * its FE each tick and, once the processing time has run, takes one input and puts the result out. What an input
 * makes and how long it takes is each machine's own.
 */
public abstract class ProcessingMachineBlockEntity extends AbstractMachineBlockEntity {

    private final IntField progress;
    private final IntField maxProgress;
    private final int fePerTick;

    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    protected ProcessingMachineBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state,
                                           final int energyCapacity, final int energyMaxReceive,
                                           final int fePerTick) {
        super(type, pos, state, 2, energyCapacity, energyMaxReceive, 0);
        this.fePerTick = fePerTick;
        this.progress = fields().integer("Progress", 0).save().toMenu();
        this.maxProgress = fields().integer("MaxProgress", 0).toMenu();
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final ProcessingMachineBlockEntity machine) {
        machine.tick(level);
    }

    public int getProgress() {
        return progress.get();
    }

    public int getMaxProgress() {
        return maxProgress.get();
    }

    /** What the machine makes of {@code input}, and in how many ticks; empty when it makes nothing of it. */
    protected abstract Optional<Processing> process(Level level, ItemStack input);

    private void tick(final Level level) {
        final ItemStack input = getInventory().getStackInSlot(INPUT_SLOT);
        final Optional<Processing> processing = input.isEmpty() ? Optional.empty() : process(level, input);
        if (processing.isEmpty() || !hasOutputSpace(processing.get().result())) {
            progress.set(0);
            return;
        }
        maxProgress.set(processing.get().ticks());
        if (getEnergy().consume(fePerTick)) {
            progress.add(1);
            if (progress.get() >= maxProgress.get()) {
                getInventory().extractItem(INPUT_SLOT, 1, false);
                getInventory().insertItem(OUTPUT_SLOT, processing.get().result().copy(), false);
                progress.set(0);
            }
        }
    }

    private boolean hasOutputSpace(final ItemStack result) {
        final ItemStack output = getInventory().getStackInSlot(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    /**
     * What a machine makes of an input, and how long it takes.
     *
     * @param result what one input becomes
     * @param ticks  how many ticks of work that takes
     */
    protected record Processing(ItemStack result, int ticks) {
    }
}
