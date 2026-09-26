/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a player is told when a recording they bring is refused, or does not arrive. */
@TextHolder
public final class MediaTexts {

    public static final TextKey UPLOADS_CLOSED =
            TextKey.of("jscore.media.uploads_closed", "This server takes no recordings from players");
    public static final TextKey TOO_BIG =
            TextKey.of("jscore.media.too_big", "The recording is larger than the %s MB this server takes");
    public static final TextKey UNREADABLE =
            TextKey.of("jscore.media.unreadable", "Not a recording this server can read");
    public static final TextKey BROKEN = TextKey.of("jscore.media.broken", "The recording did not arrive whole");
    public static final TextKey NO_TAKER =
            TextKey.of("jscore.media.no_taker", "Nothing on this server takes recordings for that");
    public static final TextKey STORE_CLOSED =
            TextKey.of("jscore.media.store_closed", "The server keeps no recordings right now");
    public static final TextKey BUSY =
            TextKey.of("jscore.media.busy", "Wait for the recordings already on their way");

    private MediaTexts() {
    }
}
