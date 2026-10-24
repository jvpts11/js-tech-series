/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.id.IStableName;
import java.util.Objects;
import net.minecraft.core.BlockPos;

/**
 * Something a wire is not made for reaching it, for a tick: a fluid too hot or too cold for a pipe, one with a mark the
 * pipe is not made for (a corrosive fluid in a plain pipe), a gas coming at more pressure than the pipe holds, or what
 * it carried having worn it through. What befalls the wire is its cable's rule ({@link IBreachRule}): by default
 * nothing, and what it is not made for stays where it is.
 *
 * @param at       the cable block the wire is in
 * @param type     the wire's cable
 * @param reason   why it is not made for it
 * @param what     the id of what reached it
 * @param pressure the pressure it came at, in times the open air's; the open air's when pressure is not why
 */
public record LineBreach(BlockPos at, CableType type, Reason reason, String what, double pressure) {

    public LineBreach {
        at = at.immutable();
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(what, "what");
    }

    /** {@code what} reached the wire, at the open air's pressure. */
    public LineBreach(final BlockPos at, final CableType type, final Reason reason, final String what) {
        this(at, type, reason, what, 1);
    }

    /** Why a wire is not made for what reached it. */
    public enum Reason implements IStableName {
        /** Hotter than it stands: a pipe softens and bursts. */
        TOO_HOT("too_hot"),
        /** Colder than it stands: the metal turns brittle and cracks. */
        TOO_COLD("too_cold"),
        /** A mark it is not made for: a gas a pipe does not hold the pressure of, a corrosive fluid. */
        NOT_MADE_FOR("not_made_for"),
        /** More pressure than it holds: a pipe forced past what it stands bursts. */
        OVERPRESSURE("overpressure"),
        /**
         * Worn through by what it carried as long as it stands ({@code CableType.Builder.wornBy}): hydrogen
         * embrittled a steel pipe, a slurry abraded an unlined one.
         */
        WORN_OUT("worn_out");

        private final String serializedName;

        Reason(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
