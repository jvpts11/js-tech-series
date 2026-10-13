/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.palette;

import java.util.function.Supplier;

/**
 * Colours worked out from palettes, kept until a palette changes. Made by {@link Palettes#derive}.
 *
 * <p>What it keeps is one value, the colours with the count of palette changes they were worked out at, so a
 * reader on another thread never sees colours paired with the wrong count. A change that lands while the colours
 * are being worked out leaves them marked with the count from before it, so the next ask works them out again.
 */
final class DerivedPalette<P> implements Supplier<P> {

    private final Supplier<P> working;
    private volatile Worked<P> last;

    DerivedPalette(final Supplier<P> working) {
        this.working = working;
    }

    @Override
    public P get() {
        final int now = Palettes.generation();
        Worked<P> seen = this.last;
        if (seen == null || seen.generation() != now) {
            seen = new Worked<>(now, this.working.get());
            this.last = seen;
        }
        return seen.colours();
    }

    private record Worked<P>(int generation, P colours) {
    }
}
