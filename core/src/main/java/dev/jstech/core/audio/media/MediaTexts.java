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
    public static final TextKey QUOTA_FULL = TextKey.of("jscore.media.quota_full",
            "You have brought %s MB of the %s MB this server keeps for each player");

    /* What the server's owner is told of its recordings. */
    public static final TextKey HELD = TextKey.of("jscore.media.held",
            "The server keeps %s recordings, %s MB in all");
    public static final TextKey PRUNED = TextKey.of("jscore.media.pruned",
            "Took out %s recordings nothing had used for %s days, %s MB in all");
    public static final TextKey PRUNE_FAILED = TextKey.of("jscore.media.prune_failed",
            "The recordings could not all be taken out: %s");

    private MediaTexts() {
    }
}
