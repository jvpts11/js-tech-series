/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;

/**
 * What stands around a block as the world's mesh is built: the 26 blocks of the three by three by three around it, so
 * every face's neighbours, the blocks in front of them and the block in front of the face itself are all at hand;
 * where the block is, for the tiles picked by place; and its biome and its name where a rule asks. Kept in the
 * model's data, so a face's texture is picked from what stood there when the mesh was built.
 *
 * @param cells the blocks around, and the block itself in the middle, by {@link #cell}
 * @param pos   where the block is
 * @param biome its biome's id, or empty when no rule asked
 * @param name  the name given to it, or empty for none or when no rule asked
 * @param extra what a model adds of its own about each cell, such as a space's microblocks; none for nothing
 */
record PackSurroundings(BlockState[] cells, BlockPos pos, String biome, String name, @Nullable Object[] extra) {

    /** Where a model finds the block's surroundings. */
    static final ModelProperty<PackSurroundings> PROPERTY = new ModelProperty<>();

    private static final int SIDE = 3;
    private static final int CELLS = SIDE * SIDE * SIDE;

    PackSurroundings {
        cells = cells.clone();
        extra = extra == null ? null : extra.clone();
    }

    /**
     * The surroundings of the block in {@code state} at {@code pos}, read from {@code level}, with its biome and its
     * name where {@code withBiome} and {@code withName}.
     */
    static PackSurroundings of(final BlockGetter level, final BlockPos pos, final BlockState state,
                               final boolean withBiome, final boolean withName) {
        final BlockState[] cells = new BlockState[CELLS];
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    cells[cell(dx, dy, dz)] = dx == 0 && dy == 0 && dz == 0 ? state
                            : level.getBlockState(at.setWithOffset(pos, dx, dy, dz));
                }
            }
        }
        String name = "";
        if (withName && level.getBlockEntity(pos) instanceof Nameable named && named.hasCustomName()
                && named.getCustomName() != null) {
            name = named.getCustomName().getString();
        }
        return new PackSurroundings(cells, pos.immutable(), withBiome ? biomeAt(pos) : "", name, null);
    }

    /** The data {@code data} with the surroundings added. */
    ModelData into(final ModelData data) {
        return data.derive().with(PROPERTY, this).build();
    }

    /** The block one step along each of {@code ways} away: none for the block itself, one, two or three of them. */
    BlockState at(final Direction... ways) {
        return this.cells[cellAlong(ways)];
    }

    /** What the model added about the block one step along each of {@code ways}, if anything. */
    @Nullable Object extraAt(final Direction... ways) {
        return this.extra == null ? null : this.extra[cellAlong(ways)];
    }

    /** The same, with {@code cellsExtra} added about each cell, by {@link #cell}. */
    PackSurroundings with(final Object[] cellsExtra) {
        return new PackSurroundings(this.cells, this.pos, this.biome, this.name, cellsExtra);
    }

    /** The index of the cell {@code dx}, {@code dy}, {@code dz} away, each from -1 to 1. */
    static int cell(final int dx, final int dy, final int dz) {
        return (dx + 1) * SIDE * SIDE + (dy + 1) * SIDE + dz + 1;
    }

    /** How many cells there are. */
    static int count() {
        return CELLS;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof PackSurroundings that && this.pos.equals(that.pos) && this.biome.equals(that.biome)
                && this.name.equals(that.name) && Arrays.equals(this.cells, that.cells)
                && Arrays.equals(this.extra, that.extra);
    }

    @Override
    public int hashCode() {
        return 31 * this.pos.hashCode() + Arrays.hashCode(this.cells);
    }

    @Override
    public String toString() {
        return "PackSurroundings[" + this.pos + "]";
    }

    private static int cellAlong(final Direction... ways) {
        int dx = 0;
        int dy = 0;
        int dz = 0;
        for (final Direction way : ways) {
            dx += way.getStepX();
            dy += way.getStepY();
            dz += way.getStepZ();
        }
        return cell(Math.clamp(dx, -1, 1), Math.clamp(dy, -1, 1), Math.clamp(dz, -1, 1));
    }

    private static String biomeAt(final BlockPos pos) {
        final ClientLevel level = Minecraft.getInstance().level;
        return level == null ? "" : level.getBiome(pos).unwrapKey().map(key -> key.location().toString())
                .orElse("");
    }
}
