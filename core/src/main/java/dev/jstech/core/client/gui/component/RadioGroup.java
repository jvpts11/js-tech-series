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
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Option buttons, one under the other: one of them chosen, kept by its owner and read every frame; a click on a row,
 * or the arrow keys, ask the owner to choose another.
 */
public final class RadioGroup extends UiComponent {

    private final List<Supplier<String>> options;
    private final IntSupplier chosen;
    private final IntConsumer onChoose;

    /** How tall a row is, and how big its button. */
    public static final int ROW = 13;
    public static final int BUTTON = 10;
    /** The room between a button and its label. */
    public static final int LABEL_GAP = 5;

    public RadioGroup(final List<Supplier<String>> options, final IntSupplier chosen, final IntConsumer onChoose) {
        this.options = List.copyOf(options);
        this.chosen = Objects.requireNonNull(chosen, "chosen");
        this.onChoose = Objects.requireNonNull(onChoose, "onChoose");
    }

    /** How tall the group is with all its rows. */
    public int neededHeight() {
        return options.size() * ROW;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final int now = chosen.getAsInt();
        for (int i = 0; i < options.size(); i++) {
            final int ry = y() + i * ROW;
            final boolean over = enabled() && ctx.over(x(), ry, width(), ROW);
            ctx.skin().radio(g, x(), ry, BUTTON, i == now, over);
            ctx.skin().drawText(g, ctx.font(), options.get(i).get(), x() + BUTTON + LABEL_GAP, ry + 1,
                    enabled() ? ctx.skin().text() : ctx.skin().dim());
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        final int row = (int) ((my - y()) / ROW);
        if (button != 0 || row < 0 || row >= options.size()) {
            return false;
        }
        onChoose.accept(row);
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final int now = chosen.getAsInt();
        if (key == GLFW.GLFW_KEY_DOWN && now + 1 < options.size()) {
            onChoose.accept(now + 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP && now > 0) {
            onChoose.accept(now - 1);
            return true;
        }
        return false;
    }

    @Override
    public Component narration() {
        final int now = chosen.getAsInt();
        return Narration.choice(spoken(), Component.literal(now >= 0 && now < options.size()
                ? options.get(now).get() : ""));
    }

    @Override
    public Component narrationUsage() {
        return Narration.CHOOSE;
    }
}
