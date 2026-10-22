/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * An item shown and not held, in a slot's frame or bare, read each frame from what the screen's code hands over; the
 * pointer on it shows the item's tooltip, as a slot's does.
 */
public final class ItemView extends UiComponent {

    private final Supplier<ItemStack> item;
    private final boolean frame;

    public ItemView(final Supplier<ItemStack> item, final boolean frame) {
        this.item = Objects.requireNonNull(item, "item");
        this.frame = frame;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        if (frame) {
            ctx.skin().slot(g, x() + 1, y() + 1);
        }
        final ItemStack now = item.get();
        if (!now.isEmpty()) {
            g.renderItem(now, x() + 1, y() + 1);
            g.renderItemDecorations(ctx.font(), now, x() + 1, y() + 1);
        }
    }

    @Override
    public List<Component> tooltip(final double mx, final double my) {
        final ItemStack now = item.get();
        return now.isEmpty() ? List.of() : Screen.getTooltipFromItem(Minecraft.getInstance(), now);
    }
}
