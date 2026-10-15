/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A tank's gauge: the fluid's own texture, in its own tint, rising in a sunken well as far as the tank is full, with a
 * scale over it. The pointer resting on it says the fluid and how much of it there is.
 */
public final class TankGauge extends UiComponent {

    private final Supplier<FluidStack> fluid;
    private final LongSupplier capacity;
    private final Function<FluidStack, List<Component>> describe;

    private static final int TILE = 16;
    private static final int MARKS = 5;

    /**
     * @param fluid    what the tank holds
     * @param capacity how much it holds when full, in millibuckets
     * @param describe what the pointer resting on it shows
     */
    public TankGauge(final Supplier<FluidStack> fluid, final LongSupplier capacity,
                     final Function<FluidStack, List<Component>> describe) {
        this.fluid = Objects.requireNonNull(fluid, "fluid");
        this.capacity = Objects.requireNonNull(capacity, "capacity");
        this.describe = Objects.requireNonNull(describe, "describe");
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().field(g, x(), y(), width(), height(), false);
        final FluidStack held = fluid.get();
        final long most = capacity.getAsLong();
        final int inner = height() - 2;
        final int filled = held.isEmpty() || most <= 0 ? 0
                : (int) Math.max(1, Math.round(inner * Math.min(1.0, held.getAmount() / (double) most)));
        if (filled > 0) {
            final IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(held.getFluid());
            final ResourceLocation still = look.getStillTexture(held);
            final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(still);
            final int tint = look.getTintColor(held);
            final float r = (tint >> 16 & 0xFF) / 255f;
            final float gr = (tint >> 8 & 0xFF) / 255f;
            final float b = (tint & 0xFF) / 255f;
            final float a = (tint >>> 24) == 0 ? 1f : (tint >>> 24) / 255f;
            Draw.pushScissor(g, x() + 1, bottom() - 1 - filled, right() - 1, bottom() - 1);
            try {
                for (int ty = bottom() - 1 - TILE; ty > bottom() - 1 - filled - TILE; ty -= TILE) {
                    for (int tx = x() + 1; tx < right() - 1; tx += TILE) {
                        g.blit(tx, ty, 0, TILE, TILE, sprite, r, gr, b, a);
                    }
                }
            } finally {
                Draw.popScissor(g);
            }
        }
        for (int k = 1; k < MARKS; k++) {
            final int my = y() + 1 + inner * k / MARKS;
            g.fill(right() - (k % 2 == 0 ? 6 : 4), my, right() - 1, my + 1, ctx.skin().edge());
        }
    }

    @Override
    public List<Component> tooltip(final double mx, final double my) {
        return describe.apply(fluid.get());
    }
}
