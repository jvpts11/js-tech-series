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
 * What a style lays over its flat pieces, each sharp and square, in its own colours: a screen's scanlines, a raised
 * bevel, a halo round the accent, the two bands of glass, an accent line under a header. All off is a flat style.
 *
 * @param scanlines   faint lines every other pixel over a window
 * @param doubleBevel a lit and a shaded edge on raised surfaces, the other way round on sunken ones
 * @param glowAccent  a halo round what is filled with the accent
 * @param glassBands  headers, buttons and the chosen tab in two bands, the lighter over the darker
 * @param accentRule  the line under a header in the accent
 */
public record StyleFinish(boolean scanlines, boolean doubleBevel, boolean glowAccent, boolean glassBands,
                          boolean accentRule) {

    /** Nothing laid over. */
    public static final StyleFinish FLAT = new StyleFinish(false, false, false, false, false);

    /** This finish with whatever {@code over} sets in place of its own. */
    public StyleFinish with(final Partial over) {
        return new StyleFinish(over.scanlines().orElse(scanlines), over.doubleBevel().orElse(doubleBevel),
                over.glowAccent().orElse(glowAccent), over.glassBands().orElse(glassBands),
                over.accentRule().orElse(accentRule));
    }

    /**
     * A finish as a style file writes it: only what it sets.
     *
     * @param scanlines   faint lines over a window
     * @param doubleBevel a bevel on raised and sunken surfaces
     * @param glowAccent  a halo round the accent
     * @param glassBands  the two bands of glass
     * @param accentRule  the accent line under a header
     */
    public record Partial(Optional<Boolean> scanlines, Optional<Boolean> doubleBevel, Optional<Boolean> glowAccent,
                          Optional<Boolean> glassBands, Optional<Boolean> accentRule) {

        /** Nothing set. */
        public static final Partial NONE = new Partial(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        public static final Codec<Partial> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("scanlines").forGetter(Partial::scanlines),
                Codec.BOOL.optionalFieldOf("double_bevel").forGetter(Partial::doubleBevel),
                Codec.BOOL.optionalFieldOf("glow_accent").forGetter(Partial::glowAccent),
                Codec.BOOL.optionalFieldOf("glass_bands").forGetter(Partial::glassBands),
                Codec.BOOL.optionalFieldOf("accent_rule").forGetter(Partial::accentRule)
        ).apply(i, Partial::new));
    }
}
