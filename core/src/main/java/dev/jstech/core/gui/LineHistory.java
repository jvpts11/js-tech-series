/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The lines typed at a command line, and where the arrow keys have walked to among them.
 *
 * <p>Up goes to older lines and stops at the oldest. Down comes back, and one step past the newest is the empty
 * line the walk started from. A line typed twice running is kept once, as a shell keeps it.
 */
public final class LineHistory {

    private final List<String> lines = new ArrayList<>();

    /** Where the walk is, or {@link #NOWHERE} when the arrow keys have not been used since the last line. */
    private int at = NOWHERE;

    private static final int NOWHERE = -1;

    /** Replaces what is kept, for a command line that is handed the history its machine remembers. */
    public void replaceWith(final List<String> kept) {
        lines.clear();
        lines.addAll(kept);
        at = NOWHERE;
    }

    /** Keeps a line that was just entered, unless it is empty or the one before it again, and ends the walk. */
    public void add(final String line) {
        at = NOWHERE;
        if (!line.isEmpty() && (lines.isEmpty() || !lines.get(lines.size() - 1).equals(line))) {
            lines.add(line);
        }
    }

    /** Ends the walk without keeping anything, for a line that was entered and is not one to bring back. */
    public void rest() {
        at = NOWHERE;
    }

    /**
     * One step of the arrow keys.
     *
     * @param direction negative for an older line, positive for a newer one
     * @return what the command line should now hold, or nothing when there is no history to walk
     */
    public Optional<String> recall(final int direction) {
        if (lines.isEmpty()) {
            return Optional.empty();
        }
        if (at == NOWHERE) {
            at = lines.size();
        }
        at = Math.max(0, Math.min(lines.size(), at + Integer.signum(direction)));
        if (at >= lines.size()) {
            at = NOWHERE;
            return Optional.of("");
        }
        return Optional.of(lines.get(at));
    }
}
