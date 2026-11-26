/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.multiblock.MultiblockPartBlockEntity;
import dev.jstech.core.multiblock.PortBuckets;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block of a chain machine that keeps something of its own, so it never merges into the formed model: a machine's
 * terminal and its entries. While its machine stands formed it draws nothing, the formed model drawing it as the
 * machine's own part.
 */
public class ChainFormedBlock extends DeviceBlock {

    /** Whether the machine it is part of stands formed, its formed model drawing it. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ChainFormedBlock(final Properties properties, final Device<?> device) {
        super(properties, device);
        registerDefaultState(defaultBlockState().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
    }

    /* A bucket on a liquid entry empties into it or fills from it, and never pours out beside the machine. */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MultiblockPartBlockEntity part) {
            final ItemInteractionResult bucket = PortBuckets.use(stack, part.port(), level, pos, player, hand,
                    hit.getDirection());
            if (bucket != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                return bucket;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /* Formed, it hides no face of its neighbours: the formed model stands in its place. */
    @Override
    protected VoxelShape getOcclusionShape(final BlockState state, final BlockGetter level, final BlockPos pos) {
        return state.getValue(FORMED) ? Shapes.empty() : super.getOcclusionShape(state, level, pos);
    }
}
