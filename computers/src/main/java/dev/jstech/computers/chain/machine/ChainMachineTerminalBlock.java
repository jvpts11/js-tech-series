/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.content.Device;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A chain machine's own terminal: the Machine Terminal as it became when its machine's shape stood whole round it.
 * It runs that machine, and is still the Machine Terminal in a player's hand: broken, it gives one back, and a player
 * picking it picks one.
 */
public final class ChainMachineTerminalBlock extends ChainFormedBlock {

    private final String machine;

    public ChainMachineTerminalBlock(final Properties properties, final Device<?> device, final String machine) {
        super(properties, device);
        this.machine = machine;
    }

    /** The id of the machine it runs. */
    public String machine() {
        return machine;
    }

    @Override
    public ItemStack getCloneItemStack(final LevelReader level, final BlockPos pos, final BlockState state) {
        return new ItemStack(ChainMachineBlocks.TERMINAL.get());
    }

    /* It has no item of its own: the Machine Terminal it was placed as comes back. */
    @Override
    protected void spawnAfterBreak(final BlockState state, final ServerLevel level, final BlockPos pos,
                                   final ItemStack tool, final boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        Block.popResource(level, pos, new ItemStack(ChainMachineBlocks.TERMINAL.get()));
    }
}
