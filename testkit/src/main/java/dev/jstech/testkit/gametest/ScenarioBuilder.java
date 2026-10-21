/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.ISpaceHolder;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.multipart.SpaceParts;
import dev.jstech.core.multipart.SpacePlacement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a test's scenario straight into a level, for a mod's GameTests and for tests driven from a player's game
 * alike: blocks set and machines placed as a player placing their items would, cables laid in the Core's cable
 * block, microblocks, parts and the game's small blocks put into block spaces, and a record of every block it wrote.
 * Positions handed to it are relative: a GameTest maps them through its arena, anything else through the origin it was
 * given.
 *
 * <p>A mod's own builder extends it with its machines' set-ups, as the series' test mod does.
 */
public class ScenarioBuilder {

    private final ServerLevel level;
    private final UnaryOperator<BlockPos> toAbsolute;
    /*
     * Every block this builder wrote. A GameTest builds into an empty arena, but the same scenario built in a real
     * world lands inside terrain: the caller needs to know which blocks are the scenario's own to clear the rock from
     * between them.
     */
    private final Set<Long> written = new HashSet<>();
    @Nullable
    private BoundingBox box;

    protected ScenarioBuilder(final ServerLevel level, final UnaryOperator<BlockPos> toAbsolute) {
        this.level = level;
        this.toAbsolute = toAbsolute;
    }

    /** A builder whose relative positions are the GameTest arena's, exactly as {@code helper.setBlock}'s are. */
    public static ScenarioBuilder forGameTest(final GameTestHelper helper) {
        return new ScenarioBuilder(helper.getLevel(), helper::absolutePos);
    }

    /** A builder whose relative positions are offsets from {@code origin}. */
    public static ScenarioBuilder at(final ServerLevel level, final BlockPos origin) {
        return new ScenarioBuilder(level, origin::offset);
    }

    /**
     * Lays a wire of {@code type} at {@code pos} of {@code level}, as a player laying it would: into the cable block
     * there, or into a new one, replacing whatever else stands there.
     *
     * @return whether the wire went in
     */
    public static boolean layCable(final Level level, final BlockPos pos, final CableType type) {
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity)) {
            level.setBlock(pos, CoreCables.BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        return Cables.lay(level, pos, type);
    }

    public ServerLevel level() {
        return level;
    }

    public BlockPos absolute(final BlockPos relative) {
        return toAbsolute.apply(relative);
    }

    /** Whether this builder placed the block at the given absolute position. */
    public boolean wrote(final BlockPos absolute) {
        return written.contains(absolute.asLong());
    }

    /** The box every block written so far fits in, or null when nothing has been placed. */
    @Nullable
    public BoundingBox writtenBox() {
        return box;
    }

    public void setBlock(final BlockPos relative, final Block block) {
        setBlock(relative, block.defaultBlockState());
    }

    public void setBlock(final BlockPos relative, final BlockState state) {
        // Neighbours updated and players told, as GameTestHelper.setBlock does.
        final BlockPos pos = absolute(relative);
        level.setBlock(pos, state, Block.UPDATE_ALL);
        note(pos);
    }

    /** Lays a wire of {@code cable} at {@code relative}: into the cable block there, or into a new one. */
    public void setBlock(final BlockPos relative, final CableEntry cable) {
        final BlockPos pos = absolute(relative);
        if (!layCable(level, pos, cable.get())) {
            throw new IllegalStateException("could not lay " + cable.id() + " at " + relative);
        }
        note(pos);
    }

    /**
     * Stands {@code block} in the space at {@code relative}, a cable's or one made where nothing stands, as a player
     * placing its microblock would.
     */
    public void placeMicroblock(final BlockPos relative, final Microblock block) {
        final BlockPos pos = absolute(relative);
        if (!SpacePlacement.place(level, new SpacePlacement.Placement(pos, block))) {
            throw new IllegalStateException("could not place " + block + " at " + relative);
        }
        note(pos);
    }

    /**
     * Mounts a new part of {@code type} on {@code face} of the space at {@code relative}, a cable's or one made where
     * nothing stands, and gives it back.
     */
    public <P extends IFacePart> P mountPart(final BlockPos relative, final Direction face, final PartType<P> type) {
        final BlockPos pos = absolute(relative);
        final P part = type.create();
        if (!SpacePlacement.mount(level, pos, face, part)) {
            throw new IllegalStateException("could not mount " + type.id() + " on the " + face + " face at "
                    + relative);
        }
        note(pos);
        return part;
    }

    /** Hangs the game's small block {@code state}, a torch or a lever, in the space at {@code relative}. */
    public void hangSmall(final BlockPos relative, final BlockState state) {
        final BlockPos pos = absolute(relative);
        if (!SpacePlacement.hang(level, pos, state)) {
            throw new IllegalStateException("could not hang " + state + " at " + relative);
        }
        note(pos);
    }

    /** The parts of the space at {@code relative}, a cable's or a space of parts alone. */
    public SpaceParts partsAt(final BlockPos relative) {
        if (!(level.getBlockEntity(absolute(relative)) instanceof ISpaceHolder holder)) {
            throw new IllegalStateException("no space of parts at " + relative);
        }
        return holder.parts();
    }

    /** Records a position as part of the scenario, growing the written box to hold it. */
    public void note(final BlockPos absolute) {
        written.add(absolute.asLong());
        final BoundingBox one = new BoundingBox(absolute);
        box = box == null ? one : BoundingBox.encapsulatingBoxes(List.of(box, one)).orElse(box);
    }

    /**
     * Clears the volume a multiblock is about to claim. In an empty arena this does nothing; in a world it is the
     * difference between a structure forming and a bare controller sitting in the rock, because a multiblock refuses
     * to raise its parts into occupied space.
     */
    public void clearFor(final Iterable<BlockPos> relativePositions) {
        for (final BlockPos relative : relativePositions) {
            final BlockPos pos = absolute(relative);
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            note(pos);
        }
    }

    /**
     * Places a block the way a player placing its item does: the block entity receives the item's default data
     * components. Many mods keep a machine's factory settings (its sides, its upgrades) in those components, so a raw
     * {@link #setBlock} would leave it with every face shut.
     */
    @Nullable
    public BlockEntity placeFromItem(final BlockPos relative, final Block block) {
        setBlock(relative, block.defaultBlockState());
        final BlockEntity entity = level.getBlockEntity(absolute(relative));
        if (entity != null) {
            entity.applyComponentsFromItemStack(new ItemStack(block));
            entity.setChanged();
        }
        return entity;
    }

    public BlockState getBlockState(final BlockPos relative) {
        return level.getBlockState(absolute(relative));
    }

    @Nullable
    public BlockEntity getBlockEntity(final BlockPos relative) {
        return level.getBlockEntity(absolute(relative));
    }

    /** The block entity at {@code relative}, or an {@link IllegalStateException} naming what was expected. */
    public <T extends BlockEntity> T blockEntity(final BlockPos relative, final Class<T> type) {
        final BlockEntity be = getBlockEntity(relative);
        if (type.isInstance(be)) {
            return type.cast(be);
        }
        throw new IllegalStateException("no " + type.getSimpleName() + " at " + relative);
    }
}
