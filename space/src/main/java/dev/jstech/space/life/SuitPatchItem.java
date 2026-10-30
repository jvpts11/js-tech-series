/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import dev.jstech.core.multiblock.damage.DamageStamps;
import dev.jstech.core.multiblock.damage.FormedDamage;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A suit patch: used, it mends a hole of the suit worn, or of a suit clicked with it in the inventory, and is spent.
 * Put on a rocket's hull where a micrometeoroid holed it, it patches that hole too.
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

    /* Put on a hull a micrometeoroid holed, it patches the hole, and the air stops rushing out. */
    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        if (level.isClientSide() || FormedDamage.erase(level, context.getClickedPos(),
                DamageStamps.MICROMETEOROID) == 0) {
            return InteractionResult.PASS;
        }
        context.getItemInHand().consume(1, context.getPlayer());
        level.playSound(null, context.getClickedPos(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS);
        return InteractionResult.SUCCESS;
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
