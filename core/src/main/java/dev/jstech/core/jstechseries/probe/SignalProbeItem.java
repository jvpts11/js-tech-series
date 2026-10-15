/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The signal probe in hand: used on a block it links to that block and reads its redstone, a wire's or any block's;
 * used in the air it is held up and its display read. While held and on it reads every tick, and hears the frequencies
 * where the player stands.
 */
public final class SignalProbeItem extends Item {

    public SignalProbeItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        final BlockPos pos = context.getClickedPos().immutable();
        context.getItemInHand().set(SignalProbe.LINK.get(), GlobalPos.of(server.dimension(), pos));
        player.displayClientMessage(GameText.component(SignalProbe.LINKED_TO.with(GameText.of(
                level.getBlockState(pos).getBlock().getName()), Text.literal(pos.getX() + ", " + pos.getY() + ", "
                + pos.getZ()))), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        if (level.isClientSide()) {
            SignalProbe.open(hand);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public void inventoryTick(final ItemStack stack, final Level level, final Entity entity, final int slot,
                              final boolean selected) {
        if (entity instanceof ServerPlayer player && (selected || player.getOffhandItem() == stack)) {
            SignalSessions.tick(player, stack);
        }
    }
}
