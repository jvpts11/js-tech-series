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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A length of an arm out from the umbilical tower to the rocket: a swing arm to a tank or an Umbilical Plate, or the
 * crew access arm to the capsule's hatch. Once its arm is formed, its end at the rocket draws the whole arm as the
 * approved launcher draws it, so the lengths show nothing of their own.
 */
public final class ArmBlock extends LauncherPieceBlock implements EntityBlock {

    private final MapCodec<ArmBlock> codec;

    /* The longest arm walked along. */
    private static final int LONGEST = 32;

    public ArmBlock(final BlockBehaviour.Properties properties, final Kind kind) {
        super(properties, kind);
        this.codec = simpleCodec(made -> new ArmBlock(made, kind));
    }

    /** Whether {@code state} is a formed arm's end at the rocket, which draws the arm. */
    public static boolean end(final BlockState state) {
        return state.getValue(FORMED) && state.getValue(ROLE) == Role.END;
    }

    /** Only the arm's end keeps anything: the arm it draws. */
    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return end(state) ? new ArmBlockEntity(pos, state) : null;
    }

    /** Its arm formed or came apart: as the arm's end it measures the arm, or forgets it. */
    @Override
    public void partFormed(final ServerLevel level, final BlockPos pos, final boolean formed) {
        super.partFormed(level, pos, formed);
        final BlockState now = level.getBlockState(pos);
        if (level.getBlockEntity(pos) instanceof ArmBlockEntity entity) {
            entity.arm(now.getBlock() == this && end(now) ? measure(level, pos, now.getValue(FACING)) : null);
        }
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return this.codec;
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
