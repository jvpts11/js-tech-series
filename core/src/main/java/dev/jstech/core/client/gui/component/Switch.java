/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * A switch with its label: on or off, kept by its owner and read every frame; a click or Space asks the owner to turn
 * it the other way.
 */
public final class Switch extends UiComponent {

    private final Supplier<String> label;
    private final BooleanSupplier on;
    private final Runnable onToggle;

    /** The switch's track, as the game's own size of things draws it. */
    public static final int TRACK_WIDTH = 22;
    public static final int TRACK_HEIGHT = 11;
    /** The room between the track and the label. */
    public static final int LABEL_GAP = 6;

    public Switch(final Supplier<String> label, final BooleanSupplier on, final Runnable onToggle) {
        this.label = Objects.requireNonNull(label, "label");
        this.on = Objects.requireNonNull(on, "on");
        this.onToggle = Objects.requireNonNull(onToggle, "onToggle");
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final int ty = y() + (height() - TRACK_HEIGHT) / 2;
        ctx.skin().switchControl(g, x(), ty, TRACK_WIDTH, TRACK_HEIGHT, on.getAsBoolean(), hovered(ctx) || isFocused());
        ctx.skin().drawText(g, ctx.font(), label.get(), x() + TRACK_WIDTH + LABEL_GAP, y() + (height() - 8) / 2,
                enabled() ? ctx.skin().text() : ctx.skin().dim());
        if (!enabled()) {
            Draw.disabled(g, x(), ty, TRACK_WIDTH, TRACK_HEIGHT);
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (button != 0) {
            return false;
        }
        onToggle.run();
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER) {
            onToggle.run();
            return true;
        }
        return false;
    }

    @Override
    public Component narration() {
        final String said = label.get();
        return Narration.toggle(spoken() != null ? spoken() : said.isEmpty() ? null : Component.literal(said),
                on.getAsBoolean());
    }

    @Override
    public Component narrationUsage() {
        return Narration.TOGGLE;
    }
}
