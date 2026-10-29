/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A suit's module, fitted to its piece in the inventory: carried and clicked with the right button onto the piece it
 * goes on, it is fitted to it and goes from the hand. Any other click moves it as any item.
 */
public final class SuitModuleItem extends Item {

    public SuitModuleItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideStackedOnOther(final ItemStack module, final Slot slot, final ClickAction action,
                                          final Player player) {
        if (action != ClickAction.SECONDARY || !slot.hasItem()) {
            return false;
        }
        if (LifeSupport.fit(player.level(), slot.getItem(), BuiltInRegistries.ITEM.getKey(this))) {
            module.shrink(1);
            slot.setChanged();
            player.playSound(SoundEvents.ARMOR_EQUIP_IRON.value());
            return true;
        }
        return false;
    }
}
