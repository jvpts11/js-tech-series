/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/**
 * The recordings that must stay in the store however long nobody used them: those a mod still offers, such as a
 * server's music catalogue, and those playing right now. Clearing out the store leaves every one of them where it is.
 *
 * <p>A mod names its own when it loads, as a list asked for each time the store is cleared, since what it offers
 * changes as it runs.
 */
public final class MediaKeepers {

    private static final List<Supplier<Collection<MediaId>>> KEEPERS = new CopyOnWriteArrayList<>();

    static {
        register(MediaSessions::playingMedia);
    }

    private MediaKeepers() {
    }

    /** Says which recordings a mod needs kept, asked again each time the store is cleared. */
    public static void register(final Supplier<Collection<MediaId>> kept) {
        KEEPERS.add(kept);
    }

    /** Every recording some mod needs kept right now. */
    public static Set<MediaId> kept() {
        final Set<MediaId> kept = new HashSet<>();
        for (final Supplier<Collection<MediaId>> keeper : KEEPERS) {
            kept.addAll(keeper.get());
        }
        return kept;
    }
}
