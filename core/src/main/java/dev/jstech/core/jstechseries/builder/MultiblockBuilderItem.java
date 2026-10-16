/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.machine.MachineBlockEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The Multiblock Builder as an item: used on a multiblock's controller it is aimed at it and held up, before the
 * controller opens its own screen; used on anything else, or in the air, it is held up as it is, to read or to set.
 */
public final class MultiblockBuilderItem extends Item {

    public MultiblockBuilderItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    /* Before the block it is used on answers: a controller's own screen would open otherwise. */
    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof MachineBlockEntity machine)
                || machine.structure() == null) {
            return InteractionResult.PASS;
        }
        stack.set(MultiblockBuilder.AIM.get(), GlobalPos.of(level.dimension(), context.getClickedPos().immutable()));
        if (level.isClientSide()) {
            MultiblockBuilder.open(context.getHand());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            MultiblockBuilder.open(context.getHand());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (level.isClientSide()) {
            MultiblockBuilder.open(hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
