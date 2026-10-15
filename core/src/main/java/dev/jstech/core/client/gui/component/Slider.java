/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.client.gui.logic.SliderMath;
import java.util.Objects;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

/**
 * A slider: dragged, clicked or stepped with the arrow keys, free or in steps, its value said on it. The value is a
 * fraction, 0 to 1, kept by its owner and read every frame; a slider in steps shows a tick under each.
 */
public final class Slider extends UiComponent {

    private final DoubleSupplier value;
    private final DoubleConsumer onChange;
    private final DoubleFunction<String> label;
    private final int steps;
    private boolean dragging;

    /** How tall the ticks of a slider in steps are, under its track. */
    private static final int TICK = 2;
    /** Every how many steps a tick is drawn longer. */
    private static final int MAJOR = 5;

    /**
     * @param value    the fraction now, 0 to 1
     * @param onChange what is done with a new fraction
     * @param label    what the slider says at a fraction
     * @param steps    how many places it stops at, its ends among them; under 2 for any fraction
     */
    public Slider(final DoubleSupplier value, final DoubleConsumer onChange, final DoubleFunction<String> label,
                  final int steps) {
        this.value = Objects.requireNonNull(value, "value");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
        this.label = Objects.requireNonNull(label, "label");
        this.steps = steps;
    }

    @Override
    public boolean focusable() {
        return true;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final double now = SliderMath.snap(value.getAsDouble(), steps);
        final int trackHeight = steps >= 2 ? height() - TICK - 1 : height();
        ctx.skin().slider(g, ctx.font(), x(), y(), width(), trackHeight, now, label.apply(now),
                hovered(ctx) || dragging || isFocused());
        if (steps >= 2) {
            final int room = width() - 8;
            for (int i = 0; i < steps; i++) {
                final int tx = x() + 4 + (int) Math.round(room * i / (double) (steps - 1));
                final int tall = i % MAJOR == 0 ? TICK + 1 : TICK;
                g.fill(tx, y() + trackHeight + 1, tx + 1, y() + trackHeight + tall, ctx.skin().dim());
            }
        }
        if (!enabled()) {
            Draw.disabled(g, x(), y(), width(), height());
        }
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (button != 0) {
            return false;
        }
        dragging = true;
        set(mx);
        return true;
    }

    @Override
    public boolean mouseDragged(final double mx, final double my, final int button) {
        if (dragging) {
            set(mx);
        }
        return dragging;
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        dragging = false;
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final double now = value.getAsDouble();
        final double next = switch (key) {
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_DOWN -> SliderMath.step(now, steps, -1);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP -> SliderMath.step(now, steps, 1);
            case GLFW.GLFW_KEY_HOME -> 0;
            case GLFW.GLFW_KEY_END -> 1;
            default -> -1;
        };
        if (next < 0) {
            return false;
        }
        onChange.accept(next);
        return true;
    }

    private void set(final double mx) {
        onChange.accept(SliderMath.snap(SliderMath.fraction(mx, x(), width(), 8), steps));
    }
}
