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
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.gui.style.StylePiece;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphics;

/** A piece of the style drawn over a component's box: the ground of a panel or a well, a separator. */
public final class PieceView extends UiComponent {

    private final StylePiece piece;

    public PieceView(final StylePiece piece) {
        this.piece = Objects.requireNonNull(piece, "piece");
    }

    /** The piece drawn. */
    public StylePiece piece() {
        return piece;
    }

    @Override
    public boolean still() {
        return true;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        if (!(ctx.skin() instanceof StyleSkin style)) {
            ctx.skin().panel(g, x(), y(), width(), height());
            return;
        }
        if (piece == StylePiece.SEPARATOR) {
            style.separator(g, x(), y() + height() / 2, width());
        } else if (piece == StylePiece.HEADER) {
            style.header(g, x(), y(), width(), height());
        } else {
            style.piece(g, piece, x(), y(), width(), height());
        }
    }
}
