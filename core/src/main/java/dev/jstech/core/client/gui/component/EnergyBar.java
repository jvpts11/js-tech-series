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
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * An energy store's gauge: a sunken well filled from the bottom in segments, from the look's darker energy colour to
 * its brighter one as it fills, the empty segments faint. The pointer resting on it says how much it holds.
 */
public final class EnergyBar extends UiComponent {

    private final DoubleSupplier fraction;
    private final Supplier<List<Component>> describe;

    /** How tall a segment is, its gap included. */
    private static final int SEGMENT = 4;

    /**
     * @param fraction how full it is, 0 to 1
     * @param describe what the pointer resting on it shows
     */
    public EnergyBar(final DoubleSupplier fraction, final Supplier<List<Component>> describe) {
        this.fraction = Objects.requireNonNull(fraction, "fraction");
        this.describe = Objects.requireNonNull(describe, "describe");
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        ctx.skin().field(g, x(), y(), width(), height(), false);
        final int inner = height() - 2;
        final int filled = (int) Math.round(inner * Math.max(0, Math.min(1, fraction.getAsDouble())));
        final int bright = ctx.skin().energyColour();
        final int dark = mix(bright, ctx.skin().fieldBg(), 0.45);
        for (int k = 0; k < inner; k++) {
            if (k % SEGMENT == SEGMENT - 1) {
                continue;
            }
            final int row = bottom() - 2 - k;
            final int colour = k < filled ? mix(dark, bright, k / (double) inner) : ctx.skin().edge();
            g.fill(x() + 1, row, right() - 1, row + 1, colour);
        }
    }

    @Override
    public List<Component> tooltip(final double mx, final double my) {
        return describe.get();
    }

    /* The colour {@code t} of the way from {@code from} to {@code to}, opaque. */
    private static int mix(final int from, final int to, final double t) {
        final double k = Math.max(0, Math.min(1, t));
        int out = 0xFF;
        for (int shift = 16; shift >= 0; shift -= 8) {
            final int a = from >> shift & 0xFF;
            final int b = to >> shift & 0xFF;
            out = out << 8 | (int) Math.round(a + (b - a) * k);
        }
        return out;
    }
}
