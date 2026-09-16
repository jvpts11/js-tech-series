/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import java.util.ArrayList;
import java.util.List;

/**
 * The menu a machine stops at on its way up: everything it could boot, and which of them it will boot if nobody
 * says otherwise.
 *
 * <p>Every entry is a disk that really carries a system, named the way the system that draws this menu names its
 * disks, plus a way into the firmware on the machines whose firmware can be reached that way. A machine with one
 * system still shows it, because the menu is also how a player finds out that the other disk is there.
 */
public record BootMenu(List<Entry> entries, int defaultIndex, int countdownTicks) {

    /** How many entries a menu may hold: more disks than any machine here has, plus the firmware. */
    public static final int MOST_ENTRIES = 12;

    /** A disk slot that is not a disk at all: the way into the firmware's own setup. */
    public static final int FIRMWARE = -1;

    /** Nothing to choose between. */
    public static final BootMenu NONE = new BootMenu(List.of(), 0, 0);

    public BootMenu {
        entries = entries == null ? List.of() : List.copyOf(entries.size() > MOST_ENTRIES
                ? entries.subList(0, MOST_ENTRIES) : entries);
        defaultIndex = entries.isEmpty() ? 0 : Math.max(0, Math.min(defaultIndex, entries.size() - 1));
        countdownTicks = Math.max(0, countdownTicks);
    }

    /** Whether there is a menu to show at all. */
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /** The seconds still on the clock, for the line that counts them down. */
    public int secondsLeft(final int ticksLeft) {
        return Math.max(0, (ticksLeft + 19) / 20);
    }

    /**
     * One thing the menu can boot.
     *
     * @param label what the menu calls it
     * @param slot  the disk it sits on, or {@link #FIRMWARE} for the way into the setup
     */
    public record Entry(String label, int slot) {

        public Entry {
            label = label == null ? "" : label;
        }

        /** Whether this entry opens the firmware rather than booting anything. */
        public boolean isFirmware() {
            return this.slot == FIRMWARE;
        }
    }

    /** Builds one up as the machine looks over its disks. */
    public static final class Builder {

        private final List<Entry> entries = new ArrayList<>();
        private int defaultIndex;

        public Builder entry(final String label, final int slot) {
            this.entries.add(new Entry(label, slot));
            return this;
        }

        /** Marks the entry added last as the one that boots when nobody chooses. */
        public Builder defaultsToLast() {
            this.defaultIndex = Math.max(0, this.entries.size() - 1);
            return this;
        }

        public BootMenu build(final int countdownTicks) {
            return new BootMenu(this.entries, this.defaultIndex, countdownTicks);
        }
    }
}
