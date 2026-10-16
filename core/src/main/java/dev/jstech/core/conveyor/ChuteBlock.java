/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.util.DeclarationError;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A chute, of a kind a mod declares ({@link ConveyorType}): a tube items fall down through, each a gap behind the one
 * below it, chutes one above another a run, into the block under the last. Items come in from above, at its top, and
 * from its sides, halfway down; an item dropped into its open top falls in. Its kind's speed is how fast its items
 * fall; it moves by itself or on energy, never on a shaft.
 *
 * <pre>{@code
 * public static final BlockEntry<ChuteBlock> CHUTE = CONTENT.block("chute",
 *         properties -> new ChuteBlock(properties, MyTypes.CHUTE, () -> MyContent.CONVEYOR.get()))
 *         .named("Chute").look(IBlockLook.fixed(new IBlockModel.Handmade("chute"))).register();
 * }</pre>
 */
public class ChuteBlock extends ConveyorPieceBlock {

    private final MapCodec<ChuteBlock> codec;

    /* The tube's walls stand this far in from the block's sides, this thick, in pixels. */
    private static final double INSET = 2.0;
    private static final double WALL = 2.0;
    private static final float UPRIGHT = 90.0F;
    private static final VoxelShape OUTLINE = box(INSET, 0, INSET, PIXELS - INSET, PIXELS, PIXELS - INSET);
    private static final VoxelShape WALLS = Shapes.join(OUTLINE, box(INSET + WALL, 0, INSET + WALL,
            PIXELS - INSET - WALL, PIXELS, PIXELS - INSET - WALL), (outer, inner) -> outer && !inner);
    private static final ConveyorGeometry LINE = new ConveyorGeometry(new Vec3(MIDDLE, 1.0, MIDDLE),
            new Vec3(MIDDLE, 0.0, MIDDLE), Direction.NORTH, UPRIGHT);

    public ChuteBlock(final Properties properties, final ConveyorType type,
                      final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity) {
        super(properties, type, entity);
        if (type.drive() instanceof ConveyorDrive.Mechanical) {
            throw DeclarationError.of(null, "a chute's kind of conveyor", "moves on a shaft, and a chute has none",
                    "give the chute's kind a free drive or one on energy");
        }
        this.codec = simpleCodec(made -> new ChuteBlock(made, type, entity));
        registerDefaultState(this.stateDefinition.any().setValue(END, true));
    }

    @Override
    public ConveyorOpening entry(final BlockState state, final BlockPos pos) {
        return new ConveyorOpening(pos, Direction.DOWN);
    }

    @Override
    public ConveyorOpening exit(final BlockState state, final BlockPos pos) {
        return new ConveyorOpening(pos.below(), Direction.DOWN);
    }

    /* From above at its top, from its sides halfway down; nothing from below, where it gives. */
    @Override
    public double intakeAt(final BlockState state, @Nullable final Direction side) {
        if (side == Direction.DOWN) {
            return NOWHERE;
        }
        return side == Direction.UP ? START : MIDDLE;
    }

    @Override
    public ConveyorGeometry geometry(final BlockState state) {
        return LINE;
    }

    /* An item inside the tube is taken in where it is. */
    @Override
    public double landsAt(final BlockState state, final BlockPos pos, final Vec3 local) {
        return LINE.along(local);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(END);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return OUTLINE;
    }

    /* Its walls stop what touches them; its middle is open, so an item dropped in falls in. */
    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        return WALLS;
    }
}
