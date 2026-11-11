/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.blockentity.ExtendedCdDriveBlockEntity;
import dev.jstech.computers.menu.ExtendedCdDriveMenu;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * An Extended CD Drive (computers/docs/design/peripherals.md): a duplicator tower two blocks tall, one reading tray and
 * seven writing trays, which copies on its own through its screen. Its lower block is the machine and draws the whole
 * tower; the upper one stands in its height, opens the same screen, and goes with the lower when either is broken.
 */
public class ExtendedCdDriveBlock extends DeviceBlock {

    private final Era era;

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final MapCodec<ExtendedCdDriveBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            EraCodecs.CODEC.fieldOf("era").forGetter(b -> b.era)
    ).apply(i, ExtendedCdDriveBlock::new));

    /** The tower's block entity, ticking its copies, with its screen as its menu. */
    private static final Device<ExtendedCdDriveBlockEntity> DEVICE =
            Device.of(() -> MediaDevices.EXTENDED_CD_DRIVE_BE.get())
                    .ticks(ExtendedCdDriveBlockEntity::serverTick)
                    .opensMenu(ExtendedCdDriveMenu::new);

    public ExtendedCdDriveBlock(final Properties properties, final Era era) {
        super(properties, DEVICE);
        this.era = era;
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    /** The era of the tower, which decides the discs it reads and writes. */
    public Era era() {
        return era;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    /* The tower needs the block above it free. */
    @Override
    @Nullable
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockPos above = context.getClickedPos().above();
        if (above.getY() >= context.getLevel().getMaxBuildHeight()
                || !context.getLevel().getBlockState(above).canBeReplaced(context)) {
            return null;
        }
        return super.getStateForPlacement(context).setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(final Level level, final BlockPos pos, final BlockState state,
                            @Nullable final LivingEntity placer, final ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /* Each half goes when the other does, so a broken tower drops once. */
    @Override
    protected BlockState updateShape(final BlockState state, final Direction direction, final BlockState neighbour,
                                     final LevelAccessor level, final BlockPos pos, final BlockPos neighbourPos) {
        final DoubleBlockHalf half = state.getValue(HALF);
        final Direction toOther = half == DoubleBlockHalf.LOWER ? Direction.UP : Direction.DOWN;
        if (direction == toOther && (!neighbour.is(this) || neighbour.getValue(HALF) == half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighbour, level, pos, neighbourPos);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? super.newBlockEntity(pos, state) : null;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state,
                                                                  final BlockEntityType<T> type) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? super.getTicker(level, state, type) : null;
    }

    /** The lower block draws the whole tower; the upper one draws nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? RenderShape.ENTITYBLOCK_ANIMATED
                : RenderShape.INVISIBLE;
    }

    /* The upper block opens the screen of the lower one. */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            final BlockState lower = level.getBlockState(pos.below());
            return lower.is(this) ? super.useWithoutItem(lower, level, pos.below(), player, hit)
                    : InteractionResult.PASS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
