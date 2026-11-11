/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a Vinyl RW Drive says of its work, which Soundfoundry shows. */
@TextHolder
public final class VinylTexts {

    public static final TextKey NO_DISC = TextKey.of("jsc.vinyl.problem.no_disc",
            "Put a music disc on the drive to record it.");
    public static final TextKey NO_BLANK = TextKey.of("jsc.vinyl.problem.no_blank",
            "Put a blank vinyl in the drive to cut onto it.");
    public static final TextKey NOT_FROM_A_DISC = TextKey.of("jsc.vinyl.problem.not_from_a_disc",
            "Only a song that came from a disc goes onto a vinyl.");
    public static final TextKey BUSY = TextKey.of("jsc.vinyl.problem.busy", "The drive is already at work.");
    public static final TextKey NOT_SAVED = TextKey.of("jsc.vinyl.problem.not_saved",
            "The recording could not be kept on the computer's disk.");
    public static final TextKey NOT_RECORDING = TextKey.of("jsc.vinyl.problem.not_recording",
            "The drive is not recording that song.");
    public static final TextKey KEEPING = TextKey.of("jsc.vinyl.problem.keeping",
            "The recording is kept when the song ends.");

    private VinylTexts() {
    }
}
