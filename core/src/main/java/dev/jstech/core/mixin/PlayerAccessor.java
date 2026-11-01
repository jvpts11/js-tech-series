/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * The item a player's last tick saw in their main hand, which the game keeps to itself and compares each tick to start
 * the swing over when the hand changes: a player made anew in a seamless passage between dimensions takes the old
 * one's, so the item they still hold does not read as a new one.
 */
@Mixin(Player.class)
public interface PlayerAccessor {

    /** The item in the main hand as the player's last tick saw it. */
    @Accessor("lastItemInMainHand")
    ItemStack jscore$lastItemInMainHand();

    /** Sets the item in the main hand as the player's last tick saw it. */
    @Accessor("lastItemInMainHand")
    void jscore$setLastItemInMainHand(ItemStack stack);
}
