/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.List;

/**
 * Something in the world that makes running sounds while it is in a state to: a machine's fans while it is on, a
 * disk while it works, a rack while its servers run. It is asked on the client, a few times a second, what it wants
 * heard; it answers from the state it already has, and the director does the rest (starting, keeping, fading and
 * stopping the sounds, and choosing which to play when there are too many).
 *
 * <p>A block entity tells the director it is there when it loads on the client and that it is gone when it is removed
 * ({@code SoundDirector.track} and {@code untrack}); nothing else is needed.
 */
public interface IAudible {

    /** Where its sounds come from. */
    double audioX();

    double audioY();

    double audioZ();

    /** The running sounds it wants heard now; none when it is off. */
    List<LoopRequest> loops();
}
