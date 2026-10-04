/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.trace;

import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Objects;

/**
 * One thing a trace saw happen: what kind, what it says, who asked for it and on which computer, how many items it
 * touched and how long it took where that is known, the game tick it happened on, and the lines its detail shows.
 *
 * @param items    how many items it touched, or -1 where it touched none
 * @param duration how many ticks it took, or -1 where it has no length
 * @param tick     the game tick it happened on
 */
public record TraceEvent(TraceEventClass kind, Text text, String requester, String computer, long items,
                         long duration, long tick, List<Text> detail) {

    public static final long NONE = -1L;

    public TraceEvent {
        Objects.requireNonNull(kind, "kind");
        text = text == null ? Text.EMPTY : text;
        requester = requester == null ? "" : requester;
        computer = computer == null ? "" : computer;
        detail = List.copyOf(detail);
    }
}
