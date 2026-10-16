/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Structure Projector's block: a low dark body with its lens on top, which opens the projector's screen and shows
 * the chosen multiblock in front of itself.
 */
public final class StructureProjectorBlock extends DeviceBlock {

    private final MapCodec<StructureProjectorBlock> codec;

    private static final Device<StructureProjectorBlockEntity> DEVICE = Device.of(
            () -> StructureProjector.ENTITY.get()).opensMenu(StructureProjectorMenu::new);
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 10, 16), Block.box(3, 10, 3, 13, 11, 13));

    public StructureProjectorBlock(final Properties properties) {
        super(properties, DEVICE);
        this.codec = simpleCodec(StructureProjectorBlock::new);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }
}
