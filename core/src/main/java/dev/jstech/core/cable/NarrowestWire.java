/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.grid.LineSurvey;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/**
 * The narrowest wire of a part of a line, found as its wires are gone through: the one that carries least, which the
 * whole part moves no more than, as an instrument names it. A wire on a run too long carries nothing.
 */
public final class NarrowestWire {

    private long pos;
    private String cable = "";
    private long ceiling = Long.MAX_VALUE;

    /** Takes {@code wire}, standing at {@code place}, into account. */
    public void consider(@Nullable final GridPlace place, final CableParts.WireAt wire) {
        final long carries = wire.runTooLong() ? 0L : wire.type().throughput();
        if (place == null || carries >= this.ceiling) {
            return;
        }
        final CableType type = wire.type();
        this.pos = place.pos();
        this.cable = type.id() == null ? "" : type.id().toString();
        this.ceiling = carries;
    }

    /** The narrowest wire taken into account; none when no wire limits the part. */
    public Optional<LineSurvey.Narrowest> found() {
        return this.ceiling == Long.MAX_VALUE ? Optional.empty()
                : Optional.of(new LineSurvey.Narrowest(this.pos, this.cable, this.ceiling));
    }
}
