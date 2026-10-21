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
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A drop-down list: a button saying what is chosen, which opens the list of the others over everything under it, the
 * chosen line marked and the line under the pointer lit. A click on a line chooses it and closes the list; a click
 * outside, or Escape, closes it as it was. A long list scrolls.
 */
public final class Dropdown extends UiComponent {

    private final List<Supplier<String>> options;
    private final IntSupplier chosen;
    private final IntConsumer onChoose;
    private final Function<String, String> caption;
    private boolean open;
    private int scroll;
    private int hoveredRow = -1;

    /** How tall a line of the open list is. */
    public static final int ROW = 12;
    /** The most lines the open list shows at once. */
    public static final int MOST_ROWS = 8;

    /**
     * @param options  what can be chosen
     * @param chosen   which is chosen now
     * @param onChoose what is done when another is chosen
     * @param caption  what the button says, given what is chosen ({@code "Mode: " + chosen})
     */
    public Dropdown(final List<Supplier<String>> options, final IntSupplier chosen, final IntConsumer onChoose,
                    final Function<String, String> caption) {
        this.options = List.copyOf(options);
        this.chosen = Objects.requireNonNull(chosen, "chosen");
        this.onChoose = Objects.requireNonNull(onChoose, "onChoose");
        this.caption = Objects.requireNonNull(caption, "caption");
    }

    /** Whether the list is open. */
    public boolean isOpen() {
        return open;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public Component narration() {
        final int now = chosen.getAsInt();
        final String shown = now >= 0 && now < options.size() ? options.get(now).get() : "";
        return Narration.choice(spoken(), Component.literal(caption.apply(shown)));
    }

    @Override
    public Component narrationUsage() {
        return Narration.PRESS;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final int now = chosen.getAsInt();
        final String shown = now >= 0 && now < options.size() ? options.get(now).get() : "";
        ctx.skin().dropdown(g, ctx.font(), x(), y(), width(), height(), caption.apply(shown), open,
                hovered(ctx) || isFocused());
        if (!enabled()) {
            Draw.disabled(g, x(), y(), width(), height());
        }
    }

    @Override
    public boolean overlayOpen() {
        return open;
    }

    @Override
    public boolean overlayContains(final double mx, final double my) {
        return contains(mx, my) || mx >= x() && mx < right() && my >= listTop() && my < listTop() + listHeight();
    }

    @Override
    public void renderOverlay(final GuiGraphics g, final UiContext ctx) {
        final int top = listTop();
        ctx.skin().listPopup(g, x(), top, width(), listHeight());
        final int now = chosen.getAsInt();
        hoveredRow = -1;
        for (int row = 0; row < shownRows(); row++) {
            final int index = scroll + row;
            final int ry = top + 2 + row * ROW;
            final boolean over = ctx.over(x() + 1, ry - 1, width() - 2, ROW);
            if (over) {
                hoveredRow = index;
                g.fill(x() + 1, ry - 1, right() - 1, ry - 1 + ROW, ctx.skin().listHover());
            }
            ctx.skin().drawText(g, ctx.font(), options.get(index).get(), x() + 5, ry + 1,
                    ctx.skin().popupText(index == now));
        }
    }

    @Override
    public void closeOverlay() {
        open = false;
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (button != 0) {
            return false;
        }
        if (open && my >= listTop()) {
            final int row = (int) ((my - listTop() - 1) / ROW);
            final int index = scroll + row;
            if (row >= 0 && row < shownRows() && index < options.size()) {
                onChoose.accept(index);
            }
            open = false;
            return true;
        }
        open = !open;
        scroll = Math.max(0, Math.min(chosen.getAsInt(), options.size() - shownRows()));
        return true;
    }

    @Override
    public boolean mouseScrolled(final double mx, final double my, final double delta) {
        if (!open) {
            return false;
        }
        scroll = Math.max(0, Math.min(options.size() - shownRows(), scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final int now = chosen.getAsInt();
        return switch (key) {
            case GLFW.GLFW_KEY_DOWN -> choose(Math.min(options.size() - 1, now + 1));
            case GLFW.GLFW_KEY_UP -> choose(Math.max(0, now - 1));
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_SPACE -> {
                open = !open;
                yield true;
            }
            default -> false;
        };
    }

    private boolean choose(final int index) {
        onChoose.accept(index);
        return true;
    }

    private int listTop() {
        return bottom() + 1;
    }

    private int shownRows() {
        return Math.min(MOST_ROWS, options.size());
    }

    private int listHeight() {
        return 3 + shownRows() * ROW;
    }
}
