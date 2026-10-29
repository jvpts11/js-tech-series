/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A suit patch: used, it mends a hole of the suit worn, or of a suit clicked with it in the inventory, and is spent.
 */
public final class SuitPatchItem extends Item {

    public SuitPatchItem(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack patch = player.getItemInHand(hand);
        if (level.isClientSide() || !SuitAir.patch(player.getItemBySlot(EquipmentSlot.CHEST))) {
            return InteractionResultHolder.pass(patch);
        }
        patch.consume(1, player);
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value());
        return InteractionResultHolder.success(patch);
    }

    @Override
    public boolean overrideStackedOnOther(final ItemStack patch, final Slot slot, final ClickAction action,
                                          final Player player) {
        if (action != ClickAction.SECONDARY || !slot.hasItem() || !SuitAir.patch(slot.getItem())) {
            return false;
        }
        patch.shrink(1);
        slot.setChanged();
        player.playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value());
        return true;
    }
}
