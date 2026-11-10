/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.StableIds;

/**
 * Which page a crash is drawn on: each edition of Frames has its own, as the real ones did, and every Unix kernel
 * writes its panic on the text console.
 */
public enum CrashStyle implements IStableId {

    /** Frames 95: the grey title over the fatal exception, the points under it, and the restart. */
    FRAMES_95(0),

    /** Frames XP: the Stop error screen, its stop code and technical information, the dump already done. */
    FRAMES_XP(1),

    /** Frames 7: the same screen, counting its dump of the memory to disk up to a hundred. */
    FRAMES_7(2),

    /** Frames 10: the sad face, the percentage collected, the code to scan and the stop code. */
    FRAMES_10(3),

    /** Frames 11: the same page, said to a device rather than a PC. */
    FRAMES_11(4),

    /** A Unix kernel's panic, written on the text console in the kernel's own words. */
    CONSOLE(5),

    /**
     * The bomb of the Quince System (computers/docs/design/quince.md): "Sorry, a system error occurred", the error's
     * ID, a Resume that never works and a Restart that loses whatever was open.
     */
    QUINCE_BOMB(6),

    /** The grey page of Quince OS X and quinceOS: you need to restart your computer, in four languages. */
    QUINCE_RESTART(7),

    /** Quince OS 9's bomb, in Platinum: the program that was running in quotes, the error's type, and Restart. */
    QUINCE_OS_9_BOMB(8),

    /**
     * OpenBSD's panic, which stops in the kernel debugger at {@code ddb>} and waits there, never rebooting by itself,
     * until the player types {@code boot reboot} (computers/docs/design/operating-systems.md).
     */
    DDB(9);

    private static final StableIds<CrashStyle> IDS = StableIds.of(CrashStyle.class);

    private final int id;

    CrashStyle(final int id) {
        this.id = id;
    }

    /** The style that declares {@code id}; an id none declares reads as the console. */
    public static CrashStyle byId(final int id) {
        return IDS.byId(id, CONSOLE);
    }

    /** Whether this is one of the Frames family's pages, which the hardware turns red. */
    public boolean frames() {
        return this != CONSOLE && this != DDB && !quince();
    }

    /** Whether this page stands until the player restarts the machine: Quince's, and OpenBSD's debugger. */
    public boolean standsUntilRestarted() {
        return quince() || this == DDB;
    }

    /** Whether this is one of Quince's pages, which stand until the player restarts the machine. */
    public boolean quince() {
        return this == QUINCE_BOMB || this == QUINCE_RESTART || this == QUINCE_OS_9_BOMB;
    }

    @Override
    public int id() {
        return this.id;
    }
}
