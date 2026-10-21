/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.tooltip;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.tooltip.TooltipBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * A bar of a tooltip drawn: a black frame round the dark track, the full part in energy's red, a gas's grey, a
 * colour of the mod's or a fluid's own texture, and what it says beside it as the tooltip's text is drawn.
 */
final class ClientTooltipBar implements ClientTooltipComponent {

    private final TooltipBar bar;

    /** How wide and tall the bar is, the room between it and its words, and the height of a tooltip's line. */
    private static final int WIDE = 50;
    private static final int TALL = 7;
    private static final int GAP = 4;
    private static final int LINE = 10;
    /** The side of a fluid's texture, tiled along the bar. */
    private static final int TILE = 16;
    /** The colour text is drawn in when its own style gives none: the tooltip's white. */
    private static final int WHITE = -1;

    ClientTooltipBar(final TooltipBar bar) {
        this.bar = bar;
    }

    @Override
    public int getHeight() {
        return LINE;
    }

    @Override
    public int getWidth(final Font font) {
        return WIDE + GAP + font.width(bar.label());
    }

    @Override
    public void renderText(final Font font, final int x, final int y, final Matrix4f matrix,
                           final MultiBufferSource.BufferSource buffers) {
        font.drawInBatch(bar.label(), x + WIDE + GAP, y, WHITE, true, matrix, buffers, Font.DisplayMode.NORMAL, 0,
                LightTexture.FULL_BRIGHT);
    }

    @Override
    public void renderImage(final Font font, final int x, final int y, final GuiGraphics g) {
        final TooltipPalette.Colours colours = TooltipPalette.get();
        g.fill(x, y, x + WIDE, y + TALL, colours.frame());
        g.fill(x + 1, y + 1, x + WIDE - 1, y + TALL - 1, colours.track());
        final int full = Math.round((WIDE - 2) * bar.fill());
        if (full <= 0) {
            return;
        }
        switch (bar.paint()) {
            case TooltipBar.Paint.Energy ignored -> g.fill(x + 1, y + 1, x + 1 + full, y + TALL - 1,
                    colours.energy());
            case TooltipBar.Paint.Gas ignored -> g.fill(x + 1, y + 1, x + 1 + full, y + TALL - 1, colours.gas());
            case TooltipBar.Paint.Colour colour -> g.fill(x + 1, y + 1, x + 1 + full, y + TALL - 1,
                    FastColor.ARGB32.opaque(colour.rgb()));
            case TooltipBar.Paint.Fluid fluid -> fluid(g, fluid.fluid(), x + 1, y + 1, full);
        }
        g.fill(x + 1, y + 1, x + 1 + full, y + 2, colours.shine());
    }

    /* The fluid's still texture, tinted as the world tints it, tiled along the full part of the bar. */
    private static void fluid(final GuiGraphics g, final FluidStack fluid, final int x, final int y, final int full) {
        if (fluid.isEmpty()) {
            return;
        }
        final IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(fluid.getFluid());
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(look.getStillTexture(fluid));
        final int tint = look.getTintColor(fluid);
        final float alpha = FastColor.ARGB32.alpha(tint) == 0 ? 1.0F : FastColor.ARGB32.alpha(tint) / 255.0F;
        Draw.pushScissor(g, x, y, x + full, y + TALL - 2);
        try {
            for (int tx = x; tx < x + full; tx += TILE) {
                g.blit(tx, y - (TILE - TALL) / 2, 0, TILE, TILE, sprite, FastColor.ARGB32.red(tint) / 255.0F,
                        FastColor.ARGB32.green(tint) / 255.0F, FastColor.ARGB32.blue(tint) / 255.0F, alpha);
            }
        } finally {
            Draw.popScissor(g);
        }
    }
}
