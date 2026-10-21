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
import net.minecraft.client.gui.GuiGraphics;

/**
 * The frames of one or more slots, in the style's slot piece: the game draws their items over them, at the places the
 * layout moved the menu's slots to.
 */
public final class SlotFrame extends UiComponent {

    private final int columns;
    private final int count;

    /** How big a slot is, its frame included. */
    public static final int SIZE = 18;

    /** The frame of one slot. */
    public SlotFrame() {
        this(1, 1);
    }

    /** The frames of {@code count} slots, {@code columns} to a row. */
    public SlotFrame(final int count, final int columns) {
        this.count = Math.max(1, count);
        this.columns = Math.max(1, columns);
    }

    @Override
    public boolean still() {
        return true;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        for (int i = 0; i < count; i++) {
            ctx.skin().slot(g, x() + (i % columns) * SIZE + 1, y() + (i / columns) * SIZE + 1);
        }
    }
}
