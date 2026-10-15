/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.audio.SoundKey;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine sounds like, as its mod declares it: once as it starts working, all the while it works, and once as
 * it stops. Any of them may be none, and a machine that declares nothing is silent.
 *
 * @param start played once as it starts working
 * @param work  a loop, running while it works
 * @param stop  played once as it stops working
 */
public record MachineSounds(@Nullable SoundKey start, @Nullable SoundKey work, @Nullable SoundKey stop) {

    /** No sound at all. */
    public static final MachineSounds SILENT = new MachineSounds(null, null, null);
}
