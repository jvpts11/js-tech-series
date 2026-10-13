/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalInt;

/**
 * Where a reader has jumped from in one manual, so Back returns there and Forward comes back again, as a browser's
 * buttons do. A jump is following a link, a tab, a line of the contents or of the search; turning the pages one by one
 * is reading on, and leaves no mark. Going somewhere new after going back forgets the way forward.
 *
 * <p>The places are spreads of the manual as it is laid out, and the history is the manual's own: each book keeps
 * its own way back.
 */
public final class GuideHistory {

    private final Deque<Integer> back = new ArrayDeque<>();
    private final Deque<Integer> forward = new ArrayDeque<>();

    /** How many places back a manual remembers; the oldest go first. */
    public static final int KEPT = 64;

    /** The reader jumps away from the spread {@code from}; the cover, which is no page, leaves no mark. */
    public void left(final int from) {
        if (from < 0 || this.back.peek() != null && this.back.peek() == from) {
            this.forward.clear();
            return;
        }
        this.back.push(from);
        this.forward.clear();
        while (this.back.size() > KEPT) {
            this.back.removeLast();
        }
    }

    /** The spread Back goes to, without going: what its button says it leads to. */
    public OptionalInt behind() {
        final Integer place = this.back.peek();
        return place == null ? OptionalInt.empty() : OptionalInt.of(place);
    }

    /** The spread Forward goes to, without going. */
    public OptionalInt ahead() {
        final Integer place = this.forward.peek();
        return place == null ? OptionalInt.empty() : OptionalInt.of(place);
    }

    /** Goes back from the spread {@code current}: where to show now, or nothing when there is no way back. */
    public OptionalInt back(final int current) {
        final Integer place = this.back.poll();
        if (place == null) {
            return OptionalInt.empty();
        }
        this.forward.push(current);
        return OptionalInt.of(place);
    }

    /** Goes forward again from the spread {@code current}: where to show now, or nothing. */
    public OptionalInt forward(final int current) {
        final Integer place = this.forward.poll();
        if (place == null) {
            return OptionalInt.empty();
        }
        this.back.push(current);
        return OptionalInt.of(place);
    }
}
