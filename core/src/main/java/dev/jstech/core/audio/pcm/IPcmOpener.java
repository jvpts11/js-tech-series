/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.pcm;

import java.io.IOException;

/**
 * How to open samples when they are about to be heard, and not before: a sound the player turned off, or one that
 * finds no free speaker, is never opened, so the file under it is never held.
 */
@FunctionalInterface
public interface IPcmOpener {

    /**
     * Opens the samples, from their start.
     *
     * @throws IOException when what they are read from cannot be read
     */
    IPcmSource open() throws IOException;
}
