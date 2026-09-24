/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

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
@TextHolder
public record BootMenu(BootManager manager, Text title, List<Entry> entries, int defaultIndex,
                       int countdownTicks) {

    /** How many entries a menu may hold: more disks than any machine here has, plus the firmware. */
    public static final int MOST_ENTRIES = 12;

    /** A disk slot that is not a disk at all: the way into the firmware's own setup. */
    public static final int FIRMWARE = -1;

    /** Another that is no disk: starting the machine over, which the managers that offer it list among the rest. */
    public static final int RESTART = -2;

    /** Nothing to choose between. */
    public static final BootMenu NONE = new BootMenu(BootManager.NONE, Text.EMPTY, List.of(), 0, 0);

    /** The loader's count under its menu, with the seconds left put in, and what it says once a key stopped it. */
    public static final TextKey AUTOBOOT =
            TextKey.of("jsc.boot.menu.autoboot", "Autoboot in %s seconds. [Space] to pause");
    public static final TextKey AUTOBOOT_PAUSED = TextKey.of("jsc.boot.menu.autoboot_paused",
            "Autoboot paused. Press [Enter] to boot or a number to choose.");

    public BootMenu {
        manager = manager == null ? BootManager.NONE : manager;
        title = title == null ? Text.EMPTY : title;
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
     * <p>A disk carries several systems, so an entry has to say which of them it is: two entries can sit on the
     * same disk and booting one of them is not booting the other.
     *
     * @param label what the menu calls it
     * @param slot  the disk it sits on, or {@link #FIRMWARE} for the way into the setup
     * @param osId  the system on that disk, empty for the way into the setup
     */
    public record Entry(Text label, int slot, String osId) {

        public Entry {
            label = label == null ? Text.EMPTY : label;
            osId = osId == null ? "" : osId;
        }

        /** Whether this entry opens the firmware rather than booting anything. */
        public boolean isFirmware() {
            return this.slot == FIRMWARE;
        }

        /** Whether this entry starts the machine over rather than booting anything. */
        public boolean isRestart() {
            return this.slot == RESTART;
        }
    }

    /** Builds one up as the machine looks over its disks. */
    public static final class Builder {

        private final BootManager manager;
        private final List<Entry> entries = new ArrayList<>();
        private int defaultIndex;

        /** A menu of that manager's, which is who decides how it is drawn and what it is headed with. */
        public Builder(final BootManager manager) {
            this.manager = manager;
        }

        public Builder entry(final Text label, final int slot, final String osId) {
            this.entries.add(new Entry(label, slot, osId));
            return this;
        }

        /** The way into the setup, which boots nothing and so names no system. */
        public Builder firmware(final Text label) {
            this.entries.add(new Entry(label, FIRMWARE, ""));
            return this;
        }

        /** Starting the machine over, for a manager that lists it. */
        public Builder restart(final Text label) {
            this.entries.add(new Entry(label, RESTART, ""));
            return this;
        }

        /** Marks the entry added last as the one that boots when nobody chooses. */
        public Builder defaultsToLast() {
            this.defaultIndex = Math.max(0, this.entries.size() - 1);
            return this;
        }

        public BootMenu build(final int countdownTicks) {
            return new BootMenu(this.manager, this.manager.title(), this.entries, this.defaultIndex,
                    countdownTicks);
        }
    }
}
