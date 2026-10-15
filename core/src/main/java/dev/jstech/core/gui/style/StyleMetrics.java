/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.style;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * The spacing a style lays its screens out with, in pixels: what a screen's file does not set itself comes from here,
 * so a style with more room between its controls gives every screen that room, and the overlap check runs in it.
 *
 * @param gap           the room between two controls
 * @param padding       the room inside a window or a panel, around what it holds
 * @param rowHeight     the height of a row of a list
 * @param controlHeight the height of a button, a field or a slider
 * @param smallScale    the size small text is drawn at, the font's own being 1
 * @param titleGap      the room under a window's title
 */
public record StyleMetrics(int gap, int padding, int rowHeight, int controlHeight, float smallScale, int titleGap) {

    /** The spacing of the game's own screens. */
    public static final StyleMetrics GAME = new StyleMetrics(4, 7, 12, 20, 0.75f, 3);

    public StyleMetrics {
        if (gap < 0 || padding < 0 || rowHeight < 1 || controlHeight < 1 || titleGap < 0) {
            throw new IllegalArgumentException("a style's spacing cannot be negative or empty");
        }
        if (smallScale <= 0 || smallScale > 1) {
            throw new IllegalArgumentException("small text is drawn at more than nothing and at most the font's size");
        }
    }

    /** These metrics with whatever {@code over} sets in place of theirs. */
    public StyleMetrics with(final Partial over) {
        return new StyleMetrics(over.gap().orElse(gap), over.padding().orElse(padding),
                over.rowHeight().orElse(rowHeight), over.controlHeight().orElse(controlHeight),
                over.smallScale().orElse(smallScale), over.titleGap().orElse(titleGap));
    }

    /**
     * Spacing as a style file writes it: only what it sets, the rest taken from the style it builds on.
     *
     * @param gap           the room between two controls
     * @param padding       the room inside a window or a panel
     * @param rowHeight     the height of a row of a list
     * @param controlHeight the height of a button, a field or a slider
     * @param smallScale    the size small text is drawn at
     * @param titleGap      the room under a window's title
     */
    public record Partial(Optional<Integer> gap, Optional<Integer> padding, Optional<Integer> rowHeight,
                          Optional<Integer> controlHeight, Optional<Float> smallScale, Optional<Integer> titleGap) {

        /** Nothing set. */
        public static final Partial NONE = new Partial(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        public static final Codec<Partial> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 32).optionalFieldOf("gap").forGetter(Partial::gap),
                Codec.intRange(0, 32).optionalFieldOf("padding").forGetter(Partial::padding),
                Codec.intRange(1, 64).optionalFieldOf("row_height").forGetter(Partial::rowHeight),
                Codec.intRange(1, 64).optionalFieldOf("control_height").forGetter(Partial::controlHeight),
                Codec.floatRange(0.25f, 1f).optionalFieldOf("small_scale").forGetter(Partial::smallScale),
                Codec.intRange(0, 32).optionalFieldOf("title_gap").forGetter(Partial::titleGap)
        ).apply(i, Partial::new));
    }
}
