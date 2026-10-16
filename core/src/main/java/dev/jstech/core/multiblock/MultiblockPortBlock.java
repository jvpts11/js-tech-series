/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The block of a multiblock's port, which a mod registers once for each kind with its own look, and puts in
 * {@link MultiblockPorts#PORTS}: a dye used on it gives it that colour, and the same dye again takes it off.
 */
public class MultiblockPortBlock extends Block implements EntityBlock {

    private final Supplier<? extends BlockEntityType<? extends MultiblockPortBlockEntity>> type;

    public MultiblockPortBlock(final Properties properties,
                               final Supplier<? extends BlockEntityType<? extends MultiblockPortBlockEntity>> type) {
        super(properties);
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return this.type.get().create(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (!(stack.getItem() instanceof DyeItem dye)
                || !(level.getBlockEntity(pos) instanceof MultiblockPortBlockEntity port)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            final DyeColor colour = dye.getDyeColor();
            port.setColour(colour == port.colour() ? null : colour);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }
}
