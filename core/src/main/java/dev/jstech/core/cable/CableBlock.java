/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.connect.Neighbours;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.util.BlockEntityTickers;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The Core's cable block, which every mod's cables are laid in: its shape is its wires, its junction box and its
 * parts, as its block entity holds them. Using it with a dye dyes the wire looked at; using it bare uses the part
 * looked at; breaking it takes out the wire or the part looked at and leaves the rest, and takes the block only with
 * its last piece.
 */
public final class CableBlock extends Block implements EntityBlock {

    public static final MapCodec<CableBlock> CODEC = simpleCodec(CableBlock::new);
    /* A block whose entity is not there yet has the shape of a wire's core. */
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);

    public CableBlock(final Properties properties) {
        super(properties);
    }

    /** Where a player's look runs within reach, from the eyes. */
    public static Vec3[] lookOf(final Player player) {
        final Vec3 start = player.getEyePosition();
        final Vec3 end = start.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + 1.0));
        return new Vec3[] {start, end};
    }

    /** What {@code player} aims at in the cable block at {@code pos}, or nothing when it is no cable block. */
    public static CableBlockEntity.Aim aimOf(final BlockGetter level, final BlockPos pos, final Player player) {
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return CableBlockEntity.Aim.NOTHING;
        }
        final Vec3[] look = lookOf(player);
        return cable.aim(look[0], look[1]);
    }

    @Override
    protected MapCodec<CableBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return level.getBlockEntity(pos) instanceof CableBlockEntity cable ? cable.voxelShape() : CORE;
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        final CableBlockEntity.Aim aim = aimOf(level, pos, player);
        if (aim.wire() == null || aim.wire().colour().filter(dye.getDyeColor()::equals).isPresent()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable
                && cable.dye(aim.wire().type(), dye.getDyeColor())) {
            level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            stack.consume(1, player);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        final Direction face = aimOf(level, pos, player).part();
        if (face == null || !(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        final IFacePart part = cable.getPart(face);
        if (part != null && player instanceof ServerPlayer server && part.use(server)) {
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(final BlockState state, final HitResult target, final LevelReader level,
                                       final BlockPos pos, final Player player) {
        final CableBlockEntity.Aim aim = aimOf(level, pos, player);
        if (aim.wire() != null) {
            return aim.wire().type().stack();
        }
        if (aim.part() != null && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            final IFacePart part = cable.getPart(aim.part());
            if (part != null) {
                return part.partItem();
            }
        }
        return ItemStack.EMPTY;
    }

    /* The block gives back every wire's cable and every part; its loot table gives nothing of its own. */
    @Override
    protected List<ItemStack> getDrops(final BlockState state, final LootParams.Builder params) {
        final BlockEntity entity = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return entity instanceof CableBlockEntity cable ? cable.drops() : List.of();
    }

    @Override
    protected void neighborChanged(final BlockState state, final Level level, final BlockPos pos,
                                   final Block neighbourBlock, final BlockPos neighbourPos,
                                   final boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbourBlock, neighbourPos, movedByPiston);
        final Direction face = Neighbours.faceTowards(pos, neighbourPos);
        if (face != null && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.fields().neighbourChanged(level, face);
        }
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos,
                            final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            // Never void what the parts are holding, on any way the block goes.
            cable.dropAllBuffers(server);
            cable.leaveGrids(server);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new CableBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                            final BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return BlockEntityTickers.create(type, CoreCables.BLOCK_ENTITY.get(), CableBlockEntity::serverTick);
    }
}
