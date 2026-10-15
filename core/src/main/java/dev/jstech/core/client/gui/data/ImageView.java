/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.data;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** A sprite of the game's or a resource pack's GUI atlas, drawn over its box. */
public final class ImageView extends UiComponent {

    private final ResourceLocation sprite;

    public ImageView(final ResourceLocation sprite) {
        this.sprite = Objects.requireNonNull(sprite, "sprite");
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        Draw.blended(() -> g.blitSprite(sprite, x(), y(), width(), height()));
    }
}
