/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The world showing contamination. Each second a few columns round each player are looked at: where the mods'
 * contaminations ({@link #declare}) add up to what living things bear, grass dies into what a mod gives it to wither
 * into ({@link #into}), flowers, saplings and grasses wither to dead bushes, and crops stop growing; where it falls
 * below half of that, what died comes back to life ({@link #recovers}), slowly. The server turns it off as a whole.
 */
public final class Withering {

    /** From how much contamination, as a share of what plants bear, they wither. */
    public static final double WITHERS_AT = 1;
    /** Below how much contamination what withered comes back. */
    public static final double RECOVERS_BELOW = 0.5;
    private static final List<IContamination> CONTAMINATIONS = new CopyOnWriteArrayList<>();
    private static final List<Change> INTO = new CopyOnWriteArrayList<>();
    private static final List<Change> BACK = new CopyOnWriteArrayList<>();
    private static final int EVERY = 20;
    /* How many columns round each player are looked at a second, and how far out. */
    private static final int COLUMNS = 6;
    private static final int REACH = 48;
    /* One chance in so many that a recovered column comes back this time, so the land greens slowly. */
    private static final int COMES_BACK_ONE_IN = 4;

    private Withering() {
    }

    /** Declares a contamination of the land, from a mod's constructor. */
    public static void declare(final IContamination contamination) {
        CONTAMINATIONS.add(Objects.requireNonNull(contamination, "contamination"));
    }

    /**
     * {@code living} withers into {@code withered} where contamination is high: grass into dead grass. Both are asked
     * for only as the land withers, so a mod's blocks may be declared before they are registered.
     */
    public static void into(final Supplier<? extends Block> living, final Supplier<BlockState> withered) {
        INTO.add(new Change(living, withered));
    }

    /** {@code withered} comes back as {@code living} where contamination fell: dead grass back to grass. */
    public static void recovers(final Supplier<? extends Block> withered, final Supplier<BlockState> living) {
        BACK.add(new Change(withered, living));
    }

    /** Hands the game's bus what withers the land and stops crops, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % EVERY == 0) {
                second(level);
            }
        });
        NeoForge.EVENT_BUS.addListener(CropGrowEvent.Pre.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && HazardBalance.withering()
                    && contamination(level, event.getPos()) >= WITHERS_AT) {
                event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
            }
        });
    }

    /** How contaminated {@code pos} is in all, every mod's contamination added up. */
    public static double contamination(final ServerLevel level, final BlockPos pos) {
        double sum = 0;
        for (final IContamination contamination : CONTAMINATIONS) {
            sum += Math.max(0, contamination.at(level, pos));
        }
        return sum;
    }

    /**
     * Withers the top of the column at {@code pos}, or brings it back, as its contamination says: the plant standing
     * there and the ground under it. Whether anything changed.
     */
    public static boolean weigh(final ServerLevel level, final BlockPos pos, final RandomSource random) {
        final double contamination = contamination(level, pos);
        if (contamination >= WITHERS_AT) {
            // The ground first, so a dead bush is weighed on the withered ground it will stand on.
            final boolean ground = witherGround(level, pos.below()) || witherGround(level, pos);
            final boolean plant = witherPlant(level, pos);
            return plant || ground;
        }
        if (contamination < RECOVERS_BELOW && random.nextInt(COMES_BACK_ONE_IN) == 0) {
            return comeBack(level, pos.below()) || comeBack(level, pos);
        }
        return false;
    }

    private static void second(final ServerLevel level) {
        if (CONTAMINATIONS.isEmpty() || !HazardBalance.withering()) {
            return;
        }
        final RandomSource random = level.getRandom();
        for (final ServerPlayer player : level.players()) {
            for (int i = 0; i < COLUMNS; i++) {
                final int x = player.getBlockX() + random.nextInt(REACH * 2 + 1) - REACH;
                final int z = player.getBlockZ() + random.nextInt(REACH * 2 + 1) - REACH;
                // Only a chunk the game has whole: looking at one would otherwise load it.
                if (level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(x),
                        SectionPos.blockToSectionCoord(z)) == null) {
                    continue;
                }
                final int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                weigh(level, new BlockPos(x, top - 1, z), random);
            }
        }
    }

    private static boolean witherPlant(final ServerLevel level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.SMALL_FLOWERS) || state.is(BlockTags.SAPLINGS) || state.is(Blocks.SHORT_GRASS)
                || state.is(Blocks.FERN)) {
            level.setBlockAndUpdate(pos, deadBushOrAir(level, pos));
            return true;
        }
        if (state.getBlock() instanceof DoublePlantBlock && state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            final BlockPos lower = state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER
                    ? pos : pos.below();
            // The top half goes first, so taking the bottom away drops nothing.
            level.setBlock(lower.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlockAndUpdate(lower, deadBushOrAir(level, lower));
            return true;
        }
        return false;
    }

    private static boolean witherGround(final ServerLevel level, final BlockPos pos) {
        return change(level, pos, INTO);
    }

    private static boolean comeBack(final ServerLevel level, final BlockPos pos) {
        return change(level, pos, BACK);
    }

    private static boolean change(final ServerLevel level, final BlockPos pos, final List<Change> changes) {
        final BlockState state = level.getBlockState(pos);
        for (final Change change : changes) {
            if (state.is(change.from().get())) {
                level.setBlockAndUpdate(pos, change.to().get());
                return true;
            }
        }
        return false;
    }

    private static BlockState deadBushOrAir(final ServerLevel level, final BlockPos pos) {
        final BlockState bush = Blocks.DEAD_BUSH.defaultBlockState();
        return bush.canSurvive(level, pos) ? bush : Blocks.AIR.defaultBlockState();
    }

    /* A block that turns into another, as the land withers or comes back. */
    private record Change(Supplier<? extends Block> from, Supplier<BlockState> to) {
    }
}
