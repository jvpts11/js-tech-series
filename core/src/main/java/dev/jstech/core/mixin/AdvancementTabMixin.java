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
import dev.jstech.core.client.advancement.WideBackgrounds;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws an advancement tab's background once, wide, where a mod declared a wide picture for its tile
 * ({@link WideBackgrounds}): the game's 16-pixel tiles are left out, and the picture is drawn behind the tree before
 * its lines and its advancements, sliding slower than the tree.
 */
@Mixin(AdvancementTab.class)
public abstract class AdvancementTabMixin {

    @Shadow
    @Final
    private DisplayInfo display;
    @Shadow
    private double scrollX;
    @Shadow
    private double scrollY;
    @Shadow
    private int minX;
    @Shadow
    private int minY;
    @Shadow
    private int maxX;
    @Shadow
    private int maxY;

    /* Each tile of the game's background, left out where the tile stands for a wide picture. */
    @WrapOperation(method = "drawContents", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V"))
    private void jscore$tileUnlessWide(final GuiGraphics graphics, final ResourceLocation tile, final int x,
                                      final int y, final float u, final float v, final int width, final int height,
                                      final int textureWidth, final int textureHeight,
                                      final Operation<Void> original) {
        if (WideBackgrounds.of(tile) == null) {
            original.call(graphics, tile, x, y, u, v, width, height, textureWidth, textureHeight);
        }
    }

    /* The wide picture, behind the tree's lines; the tree starts where the game centres it. */
    @Inject(method = "drawContents", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementWidget;drawConnectivity("
                    + "Lnet/minecraft/client/gui/GuiGraphics;IIZ)V"))
    private void jscore$wideBackground(final GuiGraphics graphics, final int x, final int y,
                                       final CallbackInfo callback) {
        this.display.getBackground().map(WideBackgrounds::of).ifPresent(wide -> wide.draw(graphics, this.scrollX,
                this.scrollY, WideBackgrounds.WINDOW_WIDTH / 2 - (this.maxX + this.minX) / 2,
                WideBackgrounds.WINDOW_HEIGHT / 2 - (this.maxY + this.minY) / 2));
    }
}
