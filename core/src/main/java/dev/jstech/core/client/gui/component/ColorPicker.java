/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.client.gui.logic.ColorMath;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * A colour: a button showing it and its {@code #RRGGBB}, which opens a picker over what is under it, a square of
 * saturation and value beside a bar of hues, a row of ready colours, and the colour's number typed straight in. The
 * colour is kept by its owner and read every frame; every change is handed to it at once.
 */
public final class ColorPicker extends UiComponent {

    private final IntSupplier colour;
    private final IntConsumer onChange;
    private final List<Integer> swatches;
    private boolean open;
    private double hue;
    private int dragging;
    private String typed = "";

    /** The picker's parts, in pixels. */
    public static final int SQUARE = 64;
    public static final int HUE_WIDTH = 8;
    public static final int SWATCH = 9;
    public static final int PAD = 4;
    public static final int PICKER_WIDTH = PAD + SQUARE + PAD + HUE_WIDTH + PAD;
    public static final int PICKER_HEIGHT = PAD + SQUARE + PAD + SWATCH + PAD + 10 + PAD;

    private static final int NONE = 0;
    private static final int SQUARE_DRAG = 1;
    private static final int HUE_DRAG = 2;
    private static final int HEX_LENGTH = 6;

    /**
     * @param colour   the colour now, opaque {@code 0xAARRGGBB}
     * @param onChange what is done with a new colour
     * @param swatches the ready colours under the square, up to eight
     */
    public ColorPicker(final IntSupplier colour, final IntConsumer onChange, final List<Integer> swatches) {
        this.colour = Objects.requireNonNull(colour, "colour");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.swatches = List.copyOf(swatches.subList(0, Math.min(8, swatches.size())));
    }

    /** Whether the picker is open. */
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().button(g, ctx.font(), x(), y(), width(), height(), "", hovered(ctx) || open, false, false,
                enabled());
        final int sw = height() - 8;
        ctx.skin().field(g, x() + 4, y() + 4, sw, sw, false);
        g.fill(x() + 5, y() + 5, x() + 3 + sw, y() + 3 + sw, colour.getAsInt());
        ctx.skin().drawText(g, ctx.font(), ColorMath.hex(colour.getAsInt()), x() + sw + 10, y() + (height() - 8) / 2,
                ctx.skin().text());
    }

    @Override
    public boolean overlayOpen() {
        return open;
    }

    @Override
    public boolean overlayContains(final double mx, final double my) {
        return contains(mx, my) || mx >= x() && mx < x() + PICKER_WIDTH && my >= top() && my < top() + PICKER_HEIGHT;
    }

    @Override
    public void renderOverlay(final GuiGraphics g, final UiContext ctx) {
        final int px = x();
        final int py = top();
        ctx.skin().listPopup(g, px, py, PICKER_WIDTH, PICKER_HEIGHT);
        final int sx = px + PAD;
        final int sy = py + PAD;
        // The square: saturation along it, value down it, for the hue chosen.
        for (int column = 0; column < SQUARE; column++) {
            final double saturation = column / (double) (SQUARE - 1);
            g.fillGradient(sx + column, sy, sx + column + 1, sy + SQUARE, ColorMath.hsv(hue, saturation, 1),
                    ColorMath.hsv(hue, saturation, 0));
        }
        final double[] hsv = ColorMath.toHsv(colour.getAsInt());
        final int markX = sx + (int) Math.round(hsv[1] * (SQUARE - 1));
        final int markY = sy + (int) Math.round((1 - hsv[2]) * (SQUARE - 1));
        Draw.outline(g, markX - 2, markY - 2, 5, 5, hsv[2] > 0.5 ? ctx.skin().fieldBg() : ctx.skin().text());
        // The bar of hues, red at the top round to red at the bottom.
        final int hx = sx + SQUARE + PAD;
        for (int row = 0; row < SQUARE; row++) {
            g.fill(hx, sy + row, hx + HUE_WIDTH, sy + row + 1, ColorMath.hsv(row / (double) SQUARE, 1, 1));
        }
        final int hueY = sy + (int) Math.round(hue * (SQUARE - 1));
        Draw.outline(g, hx - 1, hueY - 1, HUE_WIDTH + 2, 3, ctx.skin().text());
        // The ready colours.
        final int rowY = sy + SQUARE + PAD;
        for (int i = 0; i < swatches.size(); i++) {
            final int wx = sx + i * (SWATCH + 1);
            g.fill(wx, rowY, wx + SWATCH, rowY + SWATCH, swatches.get(i));
            Draw.outline(g, wx, rowY, SWATCH, SWATCH, ctx.skin().edge());
        }
        // The number, as typed or as it is.
        final String number = typed.isEmpty() ? ColorMath.hex(colour.getAsInt()) : "#" + typed;
        ctx.skin().drawText(g, ctx.font(), number, sx, rowY + SWATCH + PAD, ctx.skin().text());
    }

    @Override
    public void closeOverlay() {
        open = false;
        typed = "";
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (button != 0) {
            return false;
        }
        if (!open || contains(mx, my)) {
            open = !open;
            hue = ColorMath.toHsv(colour.getAsInt())[0];
            typed = "";
            return true;
        }
        final int sx = x() + PAD;
        final int sy = top() + PAD;
        if (mx >= sx && mx < sx + SQUARE && my >= sy && my < sy + SQUARE) {
            dragging = SQUARE_DRAG;
            pick(mx, my);
        } else if (mx >= sx + SQUARE + PAD && mx < sx + SQUARE + PAD + HUE_WIDTH && my >= sy && my < sy + SQUARE) {
            dragging = HUE_DRAG;
            pick(mx, my);
        } else {
            final int rowY = sy + SQUARE + PAD;
            final int index = (int) ((mx - sx) / (SWATCH + 1));
            if (my >= rowY && my < rowY + SWATCH && mx >= sx && index < swatches.size()) {
                onChange.accept(swatches.get(index));
                hue = ColorMath.toHsv(swatches.get(index))[0];
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(final double mx, final double my, final int button) {
        if (dragging != NONE) {
            pick(mx, my);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        dragging = NONE;
        return true;
    }

    @Override
    public boolean charTyped(final char c) {
        if (!open || Character.digit(c, 16) < 0 || typed.length() >= HEX_LENGTH) {
            return false;
        }
        typed += c;
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (!open) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_SPACE) {
                open = true;
                return true;
            }
            return false;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !typed.isEmpty()) {
            typed = typed.substring(0, typed.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER) {
            final OptionalInt read = ColorMath.parse(typed);
            read.ifPresent(value -> {
                onChange.accept(value);
                hue = ColorMath.toHsv(value)[0];
            });
            typed = "";
            return true;
        }
        return false;
    }

    private int top() {
        return bottom() + 1;
    }

    /* The colour under the pointer in the part being dragged, handed over at once. */
    private void pick(final double mx, final double my) {
        final int sx = x() + PAD;
        final int sy = top() + PAD;
        final double down = Math.max(0, Math.min(1, (my - sy) / (SQUARE - 1)));
        if (dragging == HUE_DRAG) {
            hue = Math.min(down, 0.999);
            final double[] hsv = ColorMath.toHsv(colour.getAsInt());
            onChange.accept(ColorMath.hsv(hue, hsv[1], hsv[2]));
        } else {
            final double along = Math.max(0, Math.min(1, (mx - sx) / (SQUARE - 1)));
            onChange.accept(ColorMath.hsv(hue, along, 1 - down));
        }
    }
}
