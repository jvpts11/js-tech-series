/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Rocket Hangar's terminal block, a console of the control room's row, its screen the bay seen from above: once
 * its hangar stands whole it opens the Rocket Design; till then, the machine's screen, which says what the building
 * still lacks.
 */
public final class HangarTerminalBlock extends DeviceBlock {

    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public HangarTerminalBlock(final Properties properties, final Device<?> device) {
        super(properties, device);
        for (final Direction way : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (final int[] box : HangarConsoleBlock.BOXES) {
                shape = Shapes.or(shape, HangarFittingBlock.turned(box, way));
            }
            this.shapes.put(way, shape.optimize());
        }
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return this.shapes.get(state.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof HangarTerminalBlockEntity terminal && terminal.formed()) {
            if (!level.isClientSide() && player instanceof ServerPlayer designer) {
                RocketDesigner.open(designer, terminal, pos);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
