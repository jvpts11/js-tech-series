/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.mixin;

import dev.jstech.core.advancement.AdvancementIcons;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws a stack that carries a picture ({@link AdvancementIcons}) as that picture, 16 pixels square at the height the
 * game draws items at, in place of its item: an advancement's icon on its tab, in its tree, in its tooltip and in its
 * toast all come through here.
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {

    /* The height the game lifts an item to over the slot it stands in. */
    private static final float ITEM_HEIGHT = 150.0F;
    private static final int SIZE = 16;

    @Inject(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/world/item/ItemStack;IIII)V", at = @At("HEAD"), cancellable = true)
    private void jscore$picture(final LivingEntity entity, final Level level, final ItemStack stack, final int x,
                                final int y, final int seed, final int guiOffset, final CallbackInfo callback) {
        final ResourceLocation picture = AdvancementIcons.pictureOf(stack);
        if (picture != null) {
            final GuiGraphics graphics = (GuiGraphics) (Object) this;
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, ITEM_HEIGHT);
            graphics.blit(picture, x, y, 0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE);
            graphics.pose().popPose();
            callback.cancel();
        }
    }
}
