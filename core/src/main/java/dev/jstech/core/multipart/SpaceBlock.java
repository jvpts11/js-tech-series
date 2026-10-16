/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A block space of parts alone, with no cable through it: its shape is its parts; using it uses the device looked
 * at; breaking it takes out the part looked at and leaves the rest, the block going with its last part. A part takes
 * as long to break as the block it was cut from would.
 */
public final class SpaceBlock extends Block implements EntityBlock {

    public static final MapCodec<SpaceBlock> CODEC = simpleCodec(SpaceBlock::new);
    /* How long a device takes to break, as a cable's part does. */
    private static final float DEVICE_HARDNESS = 0.3F;
    /* The game's own rule: a block's hardness to a hand's mining speed, thirty to a block mined by hand. */
    private static final float BY_HAND = 30.0F;

    public SpaceBlock(final Properties properties) {
        super(properties);
    }

    /** The part {@code player} aims at in the space at {@code pos}, or none. */
    public static @Nullable SpaceParts.Hit aimOf(final BlockGetter level, final BlockPos pos, final Player player) {
        if (!(level.getBlockEntity(pos) instanceof ISpaceHolder holder)) {
            return null;
        }
        final Vec3 start = player.getEyePosition();
        final Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
        return holder.parts().aim(pos, start, end);
    }

    /**
     * What a strike on the space at {@code pos} takes out: the part aimed at, or none when the space holds fewer than
     * two parts, and then breaks whole as any block does.
     */
    public static @Nullable SpaceParts.Hit strikeAimOf(final BlockGetter level, final BlockPos pos,
                                                       final Player player) {
        if (!(level.getBlockEntity(pos) instanceof SpaceBlockEntity space) || space.parts().count() < 2) {
            return null;
        }
        return aimOf(level, pos, player);
    }

    /** The item of the part in {@code slot} of {@code parts}: the device's or the microblock's; none for none. */
    public static ItemStack itemOf(final SpaceParts parts, final int slot) {
        final Microblock block = parts.microblock(slot);
        if (block != null) {
            return CoreSpaces.stack(block.piece());
        }
        if (SpaceSlots.isFace(slot)) {
            final IFacePart device = parts.get(Direction.from3DDataValue(SpaceSlots.faceOf(slot)));
            if (device != null) {
                return device.partItem();
            }
        }
        return ItemStack.EMPTY;
    }

    /** Takes the space at {@code pos} away when no part is left in it. */
    public static void vacateIfEmpty(final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SpaceBlockEntity space && !space.parts().any()) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected MapCodec<SpaceBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return level.getBlockEntity(pos) instanceof SpaceBlockEntity space ? space.parts().shape() : Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        final SpaceParts.Hit aimed = aimOf(level, pos, player);
        if (aimed == null || !SpaceSlots.isFace(aimed.slot())
                || !(level.getBlockEntity(pos) instanceof SpaceBlockEntity space)) {
            return InteractionResult.PASS;
        }
        final IFacePart device = space.parts().get(Direction.from3DDataValue(SpaceSlots.faceOf(aimed.slot())));
        if (device == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return player instanceof ServerPlayer server && device.use(server) ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level,
                                       final BlockPos pos, final Player player) {
        final SpaceParts.Hit aimed = aimOf(level, pos, player);
        if (aimed == null || !(level.getBlockEntity(pos) instanceof SpaceBlockEntity space)) {
            return ItemStack.EMPTY;
        }
        return itemOf(space.parts(), aimed.slot());
    }

    /* Breaking a part takes as long as the block it was cut from; a device as long as a cable's part. */
    @Override
    protected float getDestroyProgress(final BlockState state, final Player player, final BlockGetter level,
                                       final BlockPos pos) {
        final SpaceParts.Hit aimed = aimOf(level, pos, player);
        final Microblock block = aimed == null || !(level.getBlockEntity(pos) instanceof SpaceBlockEntity space)
                ? null : space.parts().microblock(aimed.slot());
        if (block != null) {
            return block.piece().material().defaultBlockState().getDestroyProgress(player, level, pos);
        }
        return player.getDestroySpeed(state) / DEVICE_HARDNESS / BY_HAND;
    }

    /* The space gives back every part; its loot table gives nothing of its own. */
    @Override
    protected List<ItemStack> getDrops(final BlockState state, final LootParams.Builder params) {
        final BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return entity instanceof SpaceBlockEntity space ? space.parts().drops() : List.of();
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof SpaceBlockEntity space) {
            // Never void what the devices are holding, on any way the space goes.
            space.parts().dropContents(server);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new SpaceBlockEntity(pos, state);
    }
}
