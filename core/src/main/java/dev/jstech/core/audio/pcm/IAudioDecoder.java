/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import java.io.IOException;
import java.io.InputStream;

/**
 * Turns one kind of sound file into samples, as they are read rather than all at once, so a long recording costs no
 * more memory than a short one. Each kind is registered once in {@link AudioDecoders}, by its file extension.
 */
public interface IAudioDecoder {

    /**
     * Opens the samples of an encoded file. The source owns the stream from here and closes it with itself.
     *
     * @throws IOException when the file is not of this kind, or is of it in a form this decoder does not read
     */
    IPcmSource open(InputStream encoded) throws IOException;
}
