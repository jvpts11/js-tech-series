/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launcher;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A length of an arm out from the umbilical tower to the rocket: a swing arm to a tank or an Umbilical Plate, or the
 * crew access arm to the capsule's hatch. Once its arm is formed, its end at the rocket draws the whole arm as the
 * approved launcher draws it, so the lengths show nothing of their own. While the launch count has its arm swung away
 * from the rocket, its blocks give way, as the arm is drawn turned clear of them.
 */
public final class ArmBlock extends LauncherPieceBlock implements EntityBlock {

    private final MapCodec<ArmBlock> codec;

    /** Whether its arm is swung away from the rocket by the launch count, its blocks giving way. */
    public static final BooleanProperty AWAY = BooleanProperty.create("away");
    /* The longest arm walked along. */
    private static final int LONGEST = 32;

    public ArmBlock(final BlockBehaviour.Properties properties, final Kind kind) {
        super(properties, kind);
        this.codec = simpleCodec(made -> new ArmBlock(made, kind));
        registerDefaultState(defaultBlockState().setValue(AWAY, false));
    }

    /** Whether {@code state} is a formed arm's end at the rocket, which draws the arm. */
    public static boolean end(final BlockState state) {
        return state.getValue(FORMED) && state.getValue(ROLE) == Role.END;
    }

    /**
     * The arm whose end is at {@code end} swung away from its rocket, or back to it: each of its blocks gives way while
     * it is away, and stands again once it is back.
     */
    public static void swing(final ServerLevel level, final BlockPos end, final boolean away) {
        if (!(level.getBlockEntity(end) instanceof ArmBlockEntity entity) || entity.arm().isEmpty()) {
            return;
        }
        final ArmBlockEntity.Arm arm = entity.arm().get();
        for (int k = 0; k < arm.length(); k++) {
            final BlockPos at = end.relative(arm.out().getOpposite(), k);
            final BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof ArmBlock && state.getValue(AWAY) != away) {
                level.setBlock(at, state.setValue(AWAY, away), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Only the arm's end keeps anything: the arm it draws. */
    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return end(state) ? new ArmBlockEntity(pos, state) : null;
    }

    /**
     * Its arm formed or came apart: as the arm's end it measures the arm, or forgets it; come apart, a length that was
     * swung away stands again.
     */
    @Override
    public void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed) {
        super.partFormed(level, pos, formed);
        BlockState now = level.getBlockState(pos);
        if (!formed && now.getBlock() == this && now.getValue(AWAY)) {
            now = now.setValue(AWAY, false);
            level.setBlock(pos, now, Block.UPDATE_CLIENTS);
        }
        if (level.getBlockEntity(pos) instanceof ArmBlockEntity entity) {
            entity.arm(now.getBlock() == this && end(now) ? measure(level, pos, now.getValue(FACING)) : null);
        }
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return state.getValue(AWAY) ? Shapes.empty() : super.getShape(state, level, pos, context);
    }

    @Override
    protected VoxelShape getCollisionShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                           final CollisionContext context) {
        return state.getValue(AWAY) ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AWAY);
    }

    /* The arm whose end is at {@code end}, running out toward {@code out}: how many lengths it has to its tower. */
    private ArmBlockEntity.Arm measure(final BlockGetter level, final BlockPos end, final Direction out) {
        int length = 1;
        while (length < LONGEST && kindOf(level, end.relative(out.getOpposite(), length)) == kind()) {
            length++;
        }
        return new ArmBlockEntity.Arm(out, length);
    }
}
