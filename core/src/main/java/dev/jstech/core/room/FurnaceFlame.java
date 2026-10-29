/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.mixin.AbstractFurnaceBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The game's furnaces, smokers and blast furnaces, and every mod's furnace built on them, as flames: lit, they burn
 * their fuel in the air round them, and where it holds too little oxygen for a fire, while fire needs oxygen in the
 * Core's settings, their fuel goes out and does not light again.
 */
final class FurnaceFlame implements IFlame {

    /* The ticks left to a fuel put out: the furnace's own next tick ends it and shows it unlit. */
    private static final int LAST_TICK = 1;

    @Override
    public boolean burning(final BlockState state) {
        return RoomBalance.fireNeedsOxygen() && state.getBlock() instanceof AbstractFurnaceBlock
                && state.getValue(AbstractFurnaceBlock.LIT);
    }

    @Override
    public void putOut(final ServerLevel level, final BlockPos pos, final BlockState state) {
        if (level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntityAccessor furnace
                && furnace.jscore$litTime() > LAST_TICK) {
            furnace.jscore$setLitTime(LAST_TICK);
        }
    }
}
