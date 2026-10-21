/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.modpack;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the tools for those who build a modpack: what a reload says, and what the dumps say they wrote. */
@TextHolder
public final class ModpackTexts {

    public static final TextKey RELOADED = TextKey.of("jscore.modpack.reloaded",
            "Data reloaded: %s recipes and %s loot tables.");
    public static final TextKey UNIFIED = TextKey.of("jscore.modpack.unified",
            "Unification: %s items stand for %s others.");
    public static final TextKey NOTHING_WAITS = TextKey.of("jscore.modpack.nothing_waits",
            "Nothing waits for a restart.");
    public static final TextKey PACKS_WAIT = TextKey.of("jscore.modpack.packs_wait",
            "Content packs changed on disk: what they declare takes hold when the game starts again.");
    public static final TextKey SETTINGS_WAIT = TextKey.of("jscore.modpack.settings_wait",
            "Waiting for a restart in %s: %s");
    public static final TextKey DUMPED = TextKey.of("jscore.modpack.dumped", "Wrote %s to %s");
    public static final TextKey NOT_DUMPED = TextKey.of("jscore.modpack.not_dumped", "Could not write %s: %s");
    public static final TextKey REFERENCE_WRITTEN = TextKey.of("jscore.modpack.reference_written",
            "Wrote the reference, %s pages, to %s: open index.html, or put the folder online.");

    private ModpackTexts() {
    }
}
