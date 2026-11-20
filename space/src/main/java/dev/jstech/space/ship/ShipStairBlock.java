/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A block of a way down let down by a landed ship, an airlock's boarding stair or a Ramp hold's ramp: its treads
 * within it, climbed as the game climbs steps, and a stair's rails beside them, which keep its climber to the middle
 * of its flights. It draws nothing, its door's model drawing the stair or the ramp, and it is aimed through, so the
 * land under it is what a click finds; the ship sets it down and takes it up again ({@link ShipStairs}).
 */
public final class ShipStairBlock extends Block implements EntityBlock {

    private static final int AXES = 3;

    public ShipStairBlock(final Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return new ShipStairEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        if (!(level.getBlockEntity(pos) instanceof ShipStairEntity stair) || stair.place() == null) {
            return Shapes.empty();
        }
        final FormedPlace place = stair.place();
        final List<double[]> boxes = new ArrayList<>(ShipStairs.treads(place));
        boxes.addAll(ShipStairs.rails(place));
        return shapeOf(boxes, place);
    }

    /**
     * The boxes {@code boxes} of a way down within its block standing as {@code place}, each its lowest corner then
     * its highest in the model's block, as one shape in the block's own space, turned as the block stands.
     */
    public static VoxelShape shapeOf(final List<double[]> boxes, final FormedPlace place) {
        VoxelShape out = Shapes.empty();
        final double[] low = new double[AXES];
        final double[] high = new double[AXES];
        for (final double[] box : boxes) {
            CellSpace.toWorld(new double[] {box[0], box[1], box[2]}, place.turn(), low);
            CellSpace.toWorld(new double[] {box[AXES], box[AXES + 1], box[AXES + 2]}, place.turn(), high);
            out = Shapes.joinUnoptimized(out, Shapes.box(Math.min(low[0], high[0]), Math.min(low[1], high[1]),
                    Math.min(low[2], high[2]), Math.max(low[0], high[0]), Math.max(low[1], high[1]),
                    Math.max(low[2], high[2])), BooleanOp.OR);
        }
        return out.optimize();
    }
}
