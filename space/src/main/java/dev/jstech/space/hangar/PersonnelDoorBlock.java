/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block of a personnel door of the Rocket Hangar, a door one block wide and two high: one in the control room's outer
 * wall, one from the control room onto the bay's floor. Used, it opens, swinging in against its hinge's side, the
 * door's other block with it; used again, it shuts.
 */
public final class PersonnelDoorBlock extends HorizontalDirectionalBlock {

    private final Map<Direction, VoxelShape> shut = new EnumMap<>(Direction.class);
    private final Map<Direction, VoxelShape> opened = new EnumMap<>(Direction.class);

    /** Whether the door stands open. */
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    private static final MapCodec<PersonnelDoorBlock> CODEC = simpleCodec(PersonnelDoorBlock::new);
    /* Its leaf as it faces north: across its wall's middle shut, against its hinge's side open. */
    private static final int[] SHUT = {0, 0, 6, 16, 16, 10};
    private static final int[] OPENED = {0, 0, 6, 3, 16, 16};
    /* How many blocks a door stands up from the one used, and down. */
    private static final int TALLEST = 2;

    public PersonnelDoorBlock(final BlockBehaviour.Properties properties) {
        super(properties.noOcclusion());
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            this.shut.put(way, HangarFittingBlock.turned(SHUT, way));
            this.opened.put(way, HangarFittingBlock.turned(OPENED, way));
        }
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return (state.getValue(OPEN) ? this.opened : this.shut).get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        final boolean open = !state.getValue(OPEN);
        swing(level, pos, open);
        for (final Direction way : new Direction[] {Direction.UP, Direction.DOWN}) {
            BlockPos at = pos.relative(way);
            for (int k = 0; k < TALLEST && level.getBlockState(at).is(this); k++) {
                swing(level, at, open);
                at = at.relative(way);
            }
        }
        level.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN);
    }

    /* Opens or shuts the door's block at {@code at}. */
    private static void swing(final Level level, final BlockPos at, final boolean open) {
        final BlockState state = level.getBlockState(at);
        if (state.getValue(OPEN) != open) {
            level.setBlock(at, state.setValue(OPEN, open), Block.UPDATE_ALL);
        }
    }
}
