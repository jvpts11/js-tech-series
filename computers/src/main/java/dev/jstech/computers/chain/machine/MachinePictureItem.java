/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/**
 * A chain machine's picture: its own terminal's item, drawn as the formed machine, which only shows the machine in the
 * recipe viewers, in no creative tab. It places nothing: a machine is built of its shape round a Machine Terminal.
 */
public final class MachinePictureItem extends BlockItem {

    public MachinePictureItem(final Block terminal, final Properties properties) {
        super(terminal, properties);
    }

    @Override
    public InteractionResult place(final BlockPlaceContext context) {
        return InteractionResult.FAIL;
    }
}
