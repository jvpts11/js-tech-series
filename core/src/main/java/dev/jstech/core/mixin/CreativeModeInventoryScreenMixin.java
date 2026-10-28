/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.jstech.core.content.ContentTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws a creative tab's button with the picture its mod declared ({@link ContentTab#picture}) in place of its icon
 * item, where the item would stand: a mark that is no item of the mod.
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    /* A tab's picture's size, an item's. */
    private static final int SIZE = 16;

    @WrapOperation(method = "renderTabButton", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;II)V"))
    private void jscore$tabPicture(final GuiGraphics graphics, final ItemStack icon, final int x, final int y,
                                   final Operation<Void> original,
                                   @Local(argsOnly = true) final CreativeModeTab tab) {
        final ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        final ResourceLocation picture = id == null ? null : ContentTab.pictureOf(id);
        if (picture == null) {
            original.call(graphics, icon, x, y);
            return;
        }
        graphics.blit(picture, x, y, 0F, 0F, SIZE, SIZE, SIZE, SIZE);
    }
}
