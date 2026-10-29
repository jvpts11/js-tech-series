/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A block of a formed structure that has merged into its terminal's model: drawn as nothing, since the model draws
 * it, but solid as it was, as hard to break, as loud, as bright, and dropping what the block it was drops. A structure
 * of a declared formed model gives it the shape of its own part of the model ({@code FormedShapes}); any other, a
 * whole block. Breaking it breaks the structure, which gives every other block back. It looks for its terminal now
 * and then, and a block whose terminal has gone, broken by any means, turns back into what it was by itself; a block
 * a plan laid does not, its plan's owner giving it back itself.
 */
public final class FormedPartBlock extends Block implements EntityBlock {

    /** How often a merged block looks for its terminal, in ticks. */
    public static final int LOOKS_EVERY = 100;

    public FormedPartBlock(final Properties properties) {
        super(properties);
    }

    /** The block a merged block at {@code pos} was, or {@code state} for any other block. */
    public static BlockState originalAt(final BlockGetter level, final BlockPos pos, final BlockState state) {
        return state.getBlock() instanceof FormedPartBlock && level.getBlockEntity(pos)
                instanceof FormedPartBlockEntity part ? part.original() : state;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new FormedPartBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /*
     * It stands as its part of its structure's formed model, for a structure of a declared model: collided with,
     * outlined and aimed at as the model is drawn there; whole otherwise.
     */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return level.getBlockEntity(pos) instanceof FormedPartBlockEntity part ? part.shape() : Shapes.block();
    }

    @Override
    protected float getDestroyProgress(final BlockState state, final Player player, final BlockGetter level,
                                       final BlockPos pos) {
        final BlockState was = originalAt(level, pos, state);
        return was == state ? super.getDestroyProgress(state, player, level, pos)
                : was.getDestroyProgress(player, level, pos);
    }

    @Override
    public SoundType getSoundType(final BlockState state, final LevelReader level, final BlockPos pos,
                                  @Nullable final Entity entity) {
        final BlockState was = originalAt(level, pos, state);
        return was == state ? super.getSoundType(state, level, pos, entity) : was.getSoundType(level, pos, entity);
    }

    @Override
    public float getExplosionResistance(final BlockState state, final BlockGetter level, final BlockPos pos,
                                        final Explosion explosion) {
        final BlockState was = originalAt(level, pos, state);
        return was == state ? super.getExplosionResistance(state, level, pos, explosion)
                : was.getExplosionResistance(level, pos, explosion);
    }

    @Override
    public int getLightEmission(final BlockState state, final BlockGetter level, final BlockPos pos) {
        final BlockState was = originalAt(level, pos, state);
        return was == state ? super.getLightEmission(state, level, pos) : was.getLightEmission(level, pos);
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level,
                                       final BlockPos pos, final Player player) {
        final BlockState was = originalAt(level, pos, state);
        return was == state ? ItemStack.EMPTY : was.getCloneItemStack(target, level, pos, player);
    }

    /* What it drops is what the block it was drops, by that block's own loot. */
    @Override
    protected List<ItemStack> getDrops(final BlockState state, final LootParams.Builder params) {
        final BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (entity instanceof FormedPartBlockEntity part && !(part.original().getBlock() instanceof FormedPartBlock)) {
            return part.original().getDrops(params);
        }
        return List.of();
    }

    /*
     * A merged block standing on its terminal's front, clicked on that front, is a part of the terminal's face: the
     * click works the terminal as a click on the terminal itself does, so a multiblock's whole front opens its screen.
     */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof FormedPartBlockEntity part && part.terminal() != null) {
            final BlockPos terminal = part.terminal();
            if (level.getBlockEntity(terminal) instanceof IStructureTerminal owner) {
                final Optional<Direction> front = owner.formedOrientation().map(FormedPartBlock::frontOf);
                if (front.isPresent() && hit.getDirection() == front.get() && onFace(pos, terminal, front.get())) {
                    return level.getBlockState(terminal).useWithoutItem(level, player, hit.withPosition(terminal));
                }
            }
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState old,
                           final boolean movedByPiston) {
        super.onPlace(state, level, pos, old, movedByPiston);
        level.scheduleTick(pos, this, LOOKS_EVERY);
    }

    /* It looks for its terminal; one that does not hold it any more, broken or undone, gives it back. */
    @Override
    protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos,
                        final RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part) || part.planned()) {
            // A block a plan laid never looks: its plan's owner gives it back itself.
            return;
        }
        final BlockPos terminal = part.terminal();
        if (terminal != null && !(level.isLoaded(terminal) && level.shouldTickBlocksAt(terminal))) {
            // A terminal not running now is not taken for gone: its structure is only as it was saved.
            level.scheduleTick(pos, this, LOOKS_EVERY);
            return;
        }
        if (terminal == null || !(level.getBlockEntity(terminal) instanceof IStructureTerminal owner)
                || !owner.holdsPlace(pos)) {
            FormedParts.restoreAt(level, pos);
            return;
        }
        level.scheduleTick(pos, this, LOOKS_EVERY);
    }

    /* The way a structure standing as {@code orientation} faces: its drawing's north, where its terminal looks. */
    private static Direction frontOf(final Orientation orientation) {
        final int[] out = orientation.apply(0, 0, -1);
        return Direction.getNearest(out[0], out[1], out[2]);
    }

    /* Whether {@code pos} lies in the plane of the face of the terminal at {@code terminal} looking {@code front}. */
    private static boolean onFace(final BlockPos pos, final BlockPos terminal, final Direction front) {
        final BlockPos apart = pos.subtract(terminal);
        return apart.getX() * front.getStepX() + apart.getY() * front.getStepY() + apart.getZ() * front.getStepZ()
                == 0;
    }
}
