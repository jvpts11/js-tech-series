/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What Soundfoundry tells a player about the songs it cannot play or keep. */
@TextHolder
public final class SoundfoundryTexts {

    public static final TextKey NO_DEVICE = TextKey.of("jsc.soundfoundry.no_device",
            "This computer's sound plays no recordings");
    public static final TextKey MISSING = TextKey.of("jsc.soundfoundry.missing", "%s is not on the disk any more");
    public static final TextKey NOT_KEPT = TextKey.of("jsc.soundfoundry.not_kept",
            "%s names a song this server does not keep");
    public static final TextKey LIST_FULL = TextKey.of("jsc.soundfoundry.list_full",
            "The playlist holds at most %s songs");
    public static final TextKey NO_SONGS = TextKey.of("jsc.soundfoundry.no_songs", "There are no songs in %s");
    public static final TextKey NOT_A_LIST = TextKey.of("jsc.soundfoundry.not_a_list", "%s is not a playlist");
    public static final TextKey LIST_SAVED = TextKey.of("jsc.soundfoundry.list_saved", "The playlist is saved as %s");
    public static final TextKey LIST_NOT_SAVED = TextKey.of("jsc.soundfoundry.list_not_saved",
            "The playlist could not be saved as %s");
    public static final TextKey NO_NETWORK = TextKey.of("jsc.soundfoundry.no_network",
            "This computer is on no network");
    public static final TextKey NOT_THERE = TextKey.of("jsc.soundfoundry.not_there", "%s is not there any more");
    public static final TextKey ALREADY = TextKey.of("jsc.soundfoundry.already", "%s is already on its way");
    public static final TextKey TOO_MANY = TextKey.of("jsc.soundfoundry.too_many",
            "No more than %s downloads are listed: clear the finished ones first");
    public static final TextKey NOT_IN_CATALOG = TextKey.of("jsc.soundfoundry.not_in_catalog",
            "%s is not in the catalogue any more");
    public static final TextKey NOT_SHARED = TextKey.of("jsc.soundfoundry.not_shared",
            "%s is not shared by %s any more");

    private SoundfoundryTexts() {
    }
}
