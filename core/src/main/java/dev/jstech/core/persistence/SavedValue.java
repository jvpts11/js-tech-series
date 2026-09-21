/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.persistence;

import com.mojang.serialization.DataResult;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Reading something back that was written down, and saying so when it cannot be read.
 *
 * <p>A codec answers with either the value or an explanation, and the easy way to use it throws the
 * explanation away: ask for the value, and take nothing if there is none. Written that way a saved craft whose
 * pattern cannot be read comes back as nothing at all, the craft is quietly dropped, and the only trace is a
 * player wondering where it went. Nobody would ever learn why, because the one line that said why was
 * discarded at the point it was produced.
 *
 * <p>So every such read goes through here instead. The value still comes back as nothing when it cannot be
 * read, which is what the callers are written for, but the explanation reaches the log first, saying what was
 * being read and what was wrong with it.
 */
public final class SavedValue {

    private SavedValue() {
    }

    /**
     * What was read, or nothing with a line in the log naming what could not be read and why.
     *
     * @param result what the codec answered
     * @param log    the log of the mod doing the reading
     * @param what   what was being read, in words, so the line means something to whoever finds it
     */
    public static <T> Optional<T> read(final DataResult<T> result, final Logger log, final String what) {
        return result.resultOrPartial(error -> log.error("Could not read {}: {}", what, error));
    }

    /**
     * The same, giving back {@code fallback} where there was nothing to read.
     *
     * <p>For the caller that has somewhere to carry on from: a row that reads as a barrier rather than
     * stopping the whole list from being read.
     */
    public static <T> T readOr(final DataResult<T> result, final Logger log, final String what, final T fallback) {
        return read(result, log, what).orElse(fallback);
    }

    /**
     * What was written down, or nothing with a line in the log naming what could not be written.
     *
     * <p>The other direction, and the worse one to lose quietly: a value that cannot be written is gone at
     * the next save, and the first anybody hears of it is the thing not being there when the world comes back.
     *
     * @param result what the codec answered
     * @param log    the log of the mod doing the writing
     * @param what   what was being written, in words
     */
    public static <T> Optional<T> written(final DataResult<T> result, final Logger log, final String what) {
        return result.resultOrPartial(error -> log.error("Could not write {}: {}", what, error));
    }
}
