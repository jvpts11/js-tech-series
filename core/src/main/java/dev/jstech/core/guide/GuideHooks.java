/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * How code that runs on both sides opens a manual on a player's screen without naming a screen: a manual item's use
 * asks here, and the player's game, when it starts, says what answers. A dedicated server never sets it, and asking
 * there does nothing.
 */
public final class GuideHooks {

    private static volatile BiConsumer<String, String> opener = (manual, entry) -> {
    };

    private GuideHooks() {
    }

    /** Opens a manual on this game's screen, at an entry's page or at its cover when the entry is empty. */
    public static void open(final String manual, final String entry) {
        opener.accept(manual, entry == null ? "" : entry);
    }

    /** Says what opens manuals: the player's game does, once, as it starts. */
    public static void useOpener(final BiConsumer<String, String> what) {
        opener = Objects.requireNonNull(what, "what");
    }
}
