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
import dev.jstech.core.gui.flex.Size;
import dev.jstech.core.gui.flex.TextMeasure;
import dev.jstech.core.gui.screen.CoreElements;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * A text of a screen's file, in one line or broken at its words to its box, drawn through the style: its font, its
 * colour for the text's tone, its manner.
 */
public final class TextView extends UiComponent {

    private final Supplier<String> text;
    private final IntSupplier colour;
    private final CoreElements.TextAlign align;
    private final TextMeasure measure;
    private boolean steady;
    /* The text last measured against its box, so a text that stays as it was is not measured again. */
    private @Nullable String measured;

    public TextView(final Supplier<String> text, final IntSupplier colour, final CoreElements.TextAlign align,
                    final TextMeasure measure) {
        this.text = Objects.requireNonNull(text, "text");
        this.colour = Objects.requireNonNull(colour, "colour");
        this.align = Objects.requireNonNull(align, "align");
        this.measure = Objects.requireNonNull(measure, "measure");
    }

    /** The text shown now. */
    public String text() {
        return text.get();
    }

    /** Says the text never changes (a translation key's words, a title), so its screen may draw it once. */
    public TextView steady() {
        steady = true;
        return this;
    }

    /**
     * Whether its text, changed since it was last asked, needs more room than its box: a bound value grown since the
     * screen was laid out, for the screen to lay itself out again rather than draw past the box.
     */
    public boolean outgrown() {
        final String now = text.get();
        if (now.equals(measured)) {
            return false;
        }
        measured = now;
        final Size needs = measure.of(now).measure(width());
        return needs.width() > width() || needs.height() > height();
    }

    @Override
    public boolean still() {
        return steady;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final List<String> lines = measure.lines(text.get(), width());
        final int lineHeight = Math.max(1, Math.round(measure.lineHeight() * measure.scale()));
        int at = y();
        for (final String line : lines) {
            final int tw = (int) Math.ceil(ctx.skin().textWidth(ctx.font(), line) * measure.scale());
            final int tx = switch (align) {
                case LEFT -> x();
                case CENTER -> x() + (width() - tw) / 2;
                case RIGHT -> x() + width() - tw;
            };
            if (measure.scale() == 1f) {
                ctx.skin().drawText(g, ctx.font(), line, tx, at, colour.getAsInt());
            } else {
                ctx.skin().drawText(g, ctx.font(), line, tx, at, colour.getAsInt(), measure.scale());
            }
            at += lineHeight;
        }
    }
}
