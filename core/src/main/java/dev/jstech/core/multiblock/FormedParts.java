/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.registry.CoreItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The blocks of a formed structure merged into its terminal's model, and given back: a structure that merges turns
 * each plain block of its shell into a {@link FormedPartBlock} that remembers it, and turns them back as it comes
 * apart. Only a block with no block entity merges; a port, a part that keeps something, the terminal itself stay as
 * they are, where they were.
 */
public final class FormedParts {

    /** The block a merged block stands as. */
    public static final BlockEntry<FormedPartBlock> BLOCK = CoreItems.CONTENT.block("formed_part",
                    FormedPartBlock::new)
            .properties(properties -> properties.mapColor(MapColor.METAL).strength(2.0F).noOcclusion()
                    .pushReaction(PushReaction.BLOCK).dynamicShape().isRedstoneConductor(FormedPartBlock::conducts))
            .named("Formed structure")
            .look(IBlockLook.fixed(new IBlockModel.ParticleOnly("formed_part", "minecraft:block/iron_block")))
            .drops(Drops.NONE)
            .register();
    /** Its block entity, which keeps what the block was. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FormedPartBlockEntity>> ENTITY =
            CoreItems.CONTENT.blockEntity("formed_part", FormedPartBlockEntity::new, BLOCK);

    /* Shown at each block as it merges or comes back. */
    private static final int PUFFS = 3;
    private static final double SPREAD = 0.3;
    private static final double SPEED = 0.02;

    private FormedParts() {
    }

    /** Declares the block, before the Core's content is registered. */
    public static void declare() {
        // Loading the class declares the block and its entity.
    }

    /** Whether the block {@code state} merges into a structure's model: a plain block, with no block entity. */
    public static boolean merges(final BlockState state) {
        return !state.isAir() && !state.hasBlockEntity() && state.getFluidState().isEmpty()
                && !(state.getBlock() instanceof FormedPartBlock);
    }

    /**
     * Merges into the model of the terminal at {@code terminal} every plain block at {@code places}, with a puff
     * of smoke at each.
     *
     * @return the places that merged
     */
    public static List<Long> merge(final ServerLevel level, final BlockPos terminal, final Collection<Long> places) {
        return merge(level, terminal, places, at -> Optional.empty());
    }

    /**
     * The same, each block taking the shape of the block of the formed model {@code shaped} says it is.
     *
     * @return the places that merged
     */
    public static List<Long> merge(final ServerLevel level, final BlockPos terminal, final Collection<Long> places,
                                   final Function<BlockPos, Optional<FormedPlace>> shaped) {
        final List<Long> out = new ArrayList<>();
        for (final long place : places) {
            final BlockPos at = MultiblockPatterns.decode(place);
            final BlockState was = level.getBlockState(at);
            if (!merges(was)) {
                continue;
            }
            // Its neighbours are not told: nothing round it changed, as the structure sees it.
            level.setBlock(at, BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(at) instanceof FormedPartBlockEntity part) {
                part.merged(was, terminal, shaped.apply(at).orElse(null));
                // Aboard a moving structure, its physics reads it again, now that it knows the block it was.
                final BlockState merged = level.getBlockState(at);
                MovingStructures.noteChange(level, at, merged, merged);
                out.add(place);
                puff(level, at);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Merges the block at {@code at} as a block a plan laid, for the plan's owner at {@code owner}: shaped as the block
     * {@code own} of its formed model, if it has one, and as the blocks {@code reaching} of the formed models beside
     * it that reach into it; it never looks for its owner. A block that does not merge stays as it is.
     *
     * @return whether it merged
     */
    public static boolean lay(final ServerLevel level, final BlockPos owner, final BlockPos at,
                              @Nullable final FormedPlace own, final List<FormedPlace> reaching) {
        final BlockState was = level.getBlockState(at);
        if (!merges(was)) {
            return false;
        }
        level.setBlock(at, BLOCK.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        if (!(level.getBlockEntity(at) instanceof FormedPartBlockEntity part)) {
            return false;
        }
        part.merged(was, owner, own);
        part.laid(reaching);
        final BlockState merged = level.getBlockState(at);
        MovingStructures.noteChange(level, at, merged, merged);
        puff(level, at);
        return true;
    }

    /** Tells the block a plan laid at {@code at} that the plan's owner is at {@code owner} now. */
    public static void ownedBy(final ServerLevel level, final BlockPos at, final BlockPos owner) {
        if (level.getBlockEntity(at) instanceof FormedPartBlockEntity part && part.planned()) {
            part.owned(owner);
        }
    }

    /**
     * Has the merged block at {@code at} draw its whole formed model itself from now on, wherever it goes, as a piece
     * no owner draws any more does, a rocket's spent stage falling on its own; whether one of a declared model stands
     * there to do it. One block of each such part is enough, and more draws the model again over itself.
     */
    public static boolean drawFrom(final ServerLevel level, final BlockPos at) {
        if (!(level.getBlockEntity(at) instanceof FormedPartBlockEntity part) || part.place() == null) {
            return false;
        }
        part.drawsPart(true);
        return true;
    }

    /** Tells the block a plan laid at {@code at} which blocks of the formed models beside it reach into it now. */
    public static void reach(final ServerLevel level, final BlockPos at, final List<FormedPlace> reaching) {
        if (level.getBlockEntity(at) instanceof FormedPartBlockEntity part && part.planned()) {
            part.reached(reaching);
        }
    }

    /** Gives back the blocks merged at {@code places}, each the block it was. */
    public static void restore(final ServerLevel level, final Collection<Long> places) {
        for (final long place : places) {
            restoreAt(level, MultiblockPatterns.decode(place));
        }
    }

    /** Gives back the block merged at {@code at}, if one is. */
    public static void restoreAt(final ServerLevel level, final BlockPos at) {
        if (level.getBlockEntity(at) instanceof FormedPartBlockEntity part
                && level.getBlockState(at).getBlock() instanceof FormedPartBlock) {
            final BlockState was = part.original();
            level.setBlock(at, was, Block.UPDATE_ALL);
            puff(level, at);
        }
    }

    private static void puff(final ServerLevel level, final BlockPos at) {
        level.sendParticles(ParticleTypes.POOF, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, PUFFS, SPREAD,
                SPREAD, SPREAD, SPEED);
    }
}
