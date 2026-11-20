/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.vein;

import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.OrePiecesBlock;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The pieces of a vein's ore lying on the surface over it, as a prospector's float (core/docs/design/world.md): about
 * {@link #A_VEIN} a vein, scattered within its reach, each of one of its minerals as the vein holds them, lying one of
 * the ways its block knows turned any way, where the ground's top is solid and the place over it open and dry. Other
 * mods' ores moved into veins have none. The world's generation lays them a chunk at a time with their vein
 * ({@link VeinFeature}); anything else can lay a vein's where it is wanted.
 */
public final class OrePieces {

    /** How many pieces a vein lays over itself, at most: those over water or on no solid ground are left out. */
    public static final int A_VEIN = 8;
    /* Mixed into a vein's seed, so its pieces' places are its own and not its ore's blocks'. */
    private static final long SALT = 0x5EEDF10A7L;
    private static final double TURN = 2 * Math.PI;
    private static final int UPDATE = Block.UPDATE_CLIENTS;

    private OrePieces() {
    }

    /**
     * Lays {@code vein}'s pieces that fall on the columns from {@code x0, z0} to {@code x1, z1}, the chunk being made;
     * how many it laid. The same vein lays the same pieces whichever chunk it is laid over, each chunk its own.
     */
    public static int lay(final WorldGenLevel level, final Deposits.Entry kind, final VeinField.Vein vein,
                          final int x0, final int z0, final int x1, final int z1) {
        if (!kind.targets().isEmpty() || kind.type().minerals().isEmpty()) {
            return 0;
        }
        final RandomSource random = RandomSource.create(vein.seed() ^ SALT);
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        int laid = 0;
        for (int i = 0; i < A_VEIN; i++) {
            // Every piece draws the same numbers wherever it falls, so each chunk lays the same vein's alike.
            final double angle = random.nextDouble() * TURN;
            final double reach = Math.sqrt(random.nextDouble()) * vein.radius();
            final double pick = random.nextDouble();
            final int layout = random.nextInt(OrePiecesBlock.LAYOUTS);
            final Direction facing = Direction.from2DDataValue(random.nextInt(4));
            final int x = (int) Math.floor(vein.x() + Math.cos(angle) * reach);
            final int z = (int) Math.floor(vein.z() + Math.sin(angle) * reach);
            if (x < x0 || x > x1 || z < z0 || z > z1) {
                continue;
            }
            final Optional<Block> pieces = mineral(kind.type().minerals(), pick);
            if (pieces.isEmpty()) {
                continue;
            }
            at.set(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z), z);
            if (open(level, at)) {
                level.setBlock(at, pieces.get().defaultBlockState().setValue(OrePiecesBlock.LAYOUT, layout)
                        .setValue(OrePiecesBlock.FACING, facing), UPDATE);
                laid++;
            }
        }
        return laid;
    }

    /* The pieces of the mineral {@code pick}, from 0 to 1, falls on among the vein's by their weights. */
    private static Optional<Block> mineral(final List<DepositType.Mineral> minerals, final double pick) {
        double total = 0;
        for (final DepositType.Mineral mineral : minerals) {
            total += mineral.weight();
        }
        double left = pick * total;
        for (final DepositType.Mineral mineral : minerals) {
            left -= mineral.weight();
            if (left < 0) {
                return Materials.byId(mineral.material()).flatMap(MaterialUses::piecesOf);
            }
        }
        return Materials.byId(minerals.getLast().material()).flatMap(MaterialUses::piecesOf);
    }

    /* Whether a piece may lie at {@code at}: an open, dry place on a solid top of ground, no tree's. */
    private static boolean open(final WorldGenLevel level, final BlockPos at) {
        if (!level.getBlockState(at).isAir() || !level.getFluidState(at).isEmpty()) {
            return false;
        }
        final BlockPos below = at.below();
        final BlockState ground = level.getBlockState(below);
        return ground.isFaceSturdy(level, below, Direction.UP) && !ground.is(BlockTags.LEAVES)
                && !ground.is(BlockTags.LOGS) && ground.getFluidState().isEmpty();
    }
}
