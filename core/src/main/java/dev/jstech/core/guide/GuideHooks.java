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
import net.minecraft.core.BlockPos;

/**
 * How code that runs on both sides opens a manual on a player's screen without naming a screen: a manual item's use
 * asks here, and the player's game, when it starts, says what answers. A dedicated server never sets it, and asking
 * there does nothing.
 */
public final class GuideHooks {

    private static volatile BiConsumer<String, String> opener = (manual, entry) -> {
    };
    private static volatile BiConsumer<String, BlockPos> lecternOpener = (manual, pos) -> {
    };

    private GuideHooks() {
    }

    /** Opens a manual on this game's screen, at an entry's page or at its cover when the entry is empty. */
    public static void open(final String manual, final String entry) {
        opener.accept(manual, entry == null ? "" : entry);
    }

    /** Opens a manual on this game's screen as it is read from the lectern at {@code pos}. */
    public static void openOnLectern(final String manual, final BlockPos pos) {
        lecternOpener.accept(manual, pos);
    }

    /** Says what opens manuals: the player's game does, once, as it starts. */
    public static void useOpener(final BiConsumer<String, String> what) {
        opener = Objects.requireNonNull(what, "what");
    }

    /** Says what opens manuals read from a lectern: the player's game does, once, as it starts. */
    public static void useLecternOpener(final BiConsumer<String, BlockPos> what) {
        lecternOpener = Objects.requireNonNull(what, "what");
    }
}
