/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import dev.jstech.core.client.motion.MotionClock;
import dev.jstech.core.motion.MotionKinds;
import dev.jstech.core.motion.MotionScope;
import dev.jstech.core.motion.MotionSpec;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.IntSupplier;

/**
 * A bar filled from the left by a percentage read every frame. On a system whose bars move
 * ({@link MotionKinds#PROGRESS_FILL}) the fill glides to a new amount over the system's time rather than jumping to it,
 * which is how a bar fed in steps (a share of a copy each time the machine says) still runs smoothly; a bar going
 * back, a new job starting over, goes back at once.
 */
public final class ProgressBar extends UiComponent {

    private final IntSupplier percent;
    /* Where the fill was when it last set off for a new amount, where it is going, and when it set off. */
    private double from;
    private double to = -1.0;
    private double setOffMs;

    /** @param percent the fill, 0 to 100, read each frame */
    public ProgressBar(final IntSupplier percent) {
        this.percent = percent;
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().panel(g, x(), y(), width(), height());
        final double shown = shown(Math.max(0, Math.min(100, percent.getAsInt())));
        final int filled = (int) Math.round((width() - 2) * shown / 100.0);
        if (filled > 0) {
            g.fill(x() + 1, y() + 1, x() + 1 + filled, bottom() - 1, ctx.skin().progressFill());
        }
    }

    /* The amount drawn this frame, on its way to {@code target}. */
    private double shown(final int target) {
        final MotionSpec spec = MotionScope.spec(MotionKinds.PROGRESS_FILL);
        final double now = MotionClock.now();
        if (to < 0.0 || !spec.moves() || MotionClock.reduced() || target < to) {
            from = target;
            to = target;
            return target;
        }
        if (target != to) {
            from = at(spec, now);
            to = target;
            setOffMs = now;
        }
        return at(spec, now);
    }

    private double at(final MotionSpec spec, final double now) {
        if (!spec.moves()) {
            return to;
        }
        final double elapsed = Math.max(0.0, Math.min(1.0, (now - setOffMs) / spec.duration()));
        return from + (to - from) * spec.easing().apply(elapsed);
    }
}
