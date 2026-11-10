/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import com.mojang.serialization.MapCodec;
import dev.jstech.computers.block.PartEras;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The hardware box (computers/docs/design/survival.md, "Abandoned buildings"): a carton found in the abandoned
 * buildings, printed with its era's machine, a computer's or a graphics card's; a loot block of its own, which holds
 * what it was packed with until a player opens it. Opened once it stays open, and a box the building's looters already
 * emptied stands open.
 */
public final class HardwareBoxBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty ERA = PartEras.property();
    public static final EnumProperty<HardwareBoxKind> KIND = EnumProperty.create("kind", HardwareBoxKind.class);
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final MapCodec<HardwareBoxBlock> CODEC = simpleCodec(HardwareBoxBlock::new);
    /* A computer's box, square; a graphics card's, long and low, its length across its front. */
    private static final VoxelShape COMPUTER = Block.box(2, 0, 2, 14, 10, 14);
    private static final VoxelShape CARD_ALONG_X = Block.box(1, 0, 3, 15, 6, 13);
    private static final VoxelShape CARD_ALONG_Z = Block.box(3, 0, 1, 13, 6, 15);

    public HardwareBoxBlock(final Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ERA, 0)
                .setValue(KIND, HardwareBoxKind.COMPUTER).setValue(OPEN, false));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new HardwareBoxBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        if (state.getValue(KIND) == HardwareBoxKind.COMPUTER) {
            return COMPUTER;
        }
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? CARD_ALONG_X : CARD_ALONG_Z;
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof HardwareBoxBlockEntity box) {
            player.openMenu(box);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState,
                            final boolean moved) {
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ERA, KIND, OPEN);
    }
}
