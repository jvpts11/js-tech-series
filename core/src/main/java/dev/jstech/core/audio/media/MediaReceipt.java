/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.text.Text;

/**
 * What became of a recording handed to whoever takes it: whether what it was brought for was done, and what the
 * player is told. The server keeping the bytes is not the same as the recording being put to use: the computer it was
 * for may have filled its disk while the bytes were on their way, and then the player is told it did not go in.
 *
 * @param accepted whether what the recording was brought for was done with it
 * @param message  what the player is told, or {@link Text#EMPTY}
 */
public record MediaReceipt(boolean accepted, Text message) {

    /** It was put to use. */
    public static MediaReceipt accepted(final Text message) {
        return new MediaReceipt(true, message);
    }

    /** The server has it, but it could not be put to use, for that reason. */
    public static MediaReceipt refused(final Text message) {
        return new MediaReceipt(false, message);
    }
}
