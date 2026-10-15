/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import java.util.UUID;
import net.minecraft.core.GlobalPos;
import org.jetbrains.annotations.Nullable;

/**
 * A radio on the air, as {@link SignalRadio} knows it: a transmitter that sends one value on its frequency, a receiver
 * that gives out what it hears, or a relay that sends again what it hears of its owner's team. A block entity that is
 * one joins the air while it is loaded and leaves it when it is not.
 */
public interface IRadio {

    /** What it does on the air. */
    Role role();

    /** What it is tuned to; a relay's is not asked. */
    Tuning tuning();

    /** Whose it is; none for a radio nobody placed, which is on the frequencies of nobody. */
    @Nullable
    UUID owner();

    /** How far it sends in open air, in blocks: a transmitter's and a relay's; a receiver's is not asked. */
    double power();

    /** The value a transmitter sends, 0 to 15. */
    default int value() {
        return 0;
    }

    /** Where a relay sends again what it hears, besides its own place, such as in another dimension; none for none. */
    @Nullable
    default GlobalPos partner() {
        return null;
    }

    /** A receiver hears a new reception; told only when it changes. */
    default void hear(final Reception reception) {
    }

    /** A transmitter is told it clashes with another on its frequency, or no longer does. */
    default void clash(final boolean clashing) {
    }

    /** What a radio does on the air. */
    enum Role {
        TRANSMITTER,
        RECEIVER,
        RELAY
    }
}
