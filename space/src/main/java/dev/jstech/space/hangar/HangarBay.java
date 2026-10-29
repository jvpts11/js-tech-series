/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * A formed Rocket Hangar's bay: the box inside its walls, from its lowest corner to its highest, and its crane: the
 * walls its rails run along the tops of, which way its bridge spans the bay and where, and where its hoist hangs.
 *
 * @param low    the bay's lowest corner, inside the walls, on the floor
 * @param high   the bay's highest corner, inside the walls, under the roof
 * @param span   the way the bridge spans the bay, from rail to rail
 * @param bridge where along the rails the bridge stands, a block of the bay's top row
 * @param hoist  where the hoist hangs, under the bridge
 */
public record HangarBay(BlockPos low, BlockPos high, Direction.Axis span, BlockPos bridge, BlockPos hoist) {

    public static final Codec<HangarBay> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("low").forGetter(HangarBay::low),
            BlockPos.CODEC.fieldOf("high").forGetter(HangarBay::high),
            Direction.Axis.CODEC.fieldOf("span").forGetter(HangarBay::span),
            BlockPos.CODEC.fieldOf("bridge").forGetter(HangarBay::bridge),
            BlockPos.CODEC.fieldOf("hoist").forGetter(HangarBay::hoist)
    ).apply(instance, HangarBay::new));

    /** How wide the bay is, east to west, in blocks. */
    public int width() {
        return this.high.getX() - this.low.getX() + 1;
    }

    /** How deep the bay is, north to south, in blocks. */
    public int depth() {
        return this.high.getZ() - this.low.getZ() + 1;
    }

    /** How high the bay is, floor to roof, in blocks. */
    public int height() {
        return this.high.getY() - this.low.getY() + 1;
    }

    /** Whether {@code pos} is inside the bay. */
    public boolean holds(final BlockPos pos) {
        return pos.getX() >= this.low.getX() && pos.getX() <= this.high.getX() && pos.getY() >= this.low.getY()
                && pos.getY() <= this.high.getY() && pos.getZ() >= this.low.getZ() && pos.getZ() <= this.high.getZ();
    }
}
