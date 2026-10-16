/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The item of a kind of part declared with the Core ({@code ModContent.part}): used on a cable or a space of parts, it
 * mounts the part on the face clicked; used on any other block, on the face of the space beside it toward that block,
 * a space made there where nothing stands, as a torch hangs on a wall.
 */
public final class PartItem extends Item {

    private final Supplier<? extends PartType<?>> type;

    private static final float VOLUME = 1.0F;
    private static final float PITCH = 0.9F;

    public PartItem(final Properties properties, final Supplier<? extends PartType<?>> type) {
        super(properties);
        this.type = type;
    }

    /** The kind of part it mounts. */
    public PartType<?> partType() {
        return this.type.get();
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos clicked = context.getClickedPos();
        final boolean onSpace = level.getBlockEntity(clicked) instanceof ISpaceHolder;
        final BlockPos pos = onSpace ? clicked : clicked.relative(context.getClickedFace());
        final Direction face = onSpace ? context.getClickedFace() : context.getClickedFace().getOpposite();
        if (!fits(level, pos, face)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!SpacePlacement.mount(level, pos, face, partType().create())) {
            return InteractionResult.FAIL;
        }
        level.playSound(null, pos, SoundType.METAL.getPlaceSound(), SoundSource.BLOCKS, VOLUME, PITCH);
        context.getItemInHand().consume(1, context.getPlayer());
        return InteractionResult.CONSUME;
    }

    /* Whether the part fits on {@code face} of the space at {@code pos}, or of one that would be made there. */
    private boolean fits(final Level level, final BlockPos pos, final Direction face) {
        if (level.getBlockEntity(pos) instanceof ISpaceHolder holder) {
            final int index = face.get3DDataValue();
            return holder.parts().fits(SpaceFit.Piece.device(SpaceSlots.face(index), partType().boxesOn(index)));
        }
        final BlockState there = level.getBlockState(pos);
        return there.canBeReplaced() && there.getFluidState().isEmpty();
    }
}
