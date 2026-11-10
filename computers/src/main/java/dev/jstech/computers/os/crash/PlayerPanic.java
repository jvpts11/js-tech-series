/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.crash;

import java.util.List;

/**
 * What the panic page of an operating system a player wrote shows (computers/docs/design/player-systems.md): the
 * message it went down with at the top, the error under it with its file and line and the calls it came through, the
 * system, its author, the process and the memory, and at the foot the reboot it asked for, all in its own colours with
 * its logo.
 *
 * @param system        the system, with its version: "Lantern OS 1.2"
 * @param author        who wrote it
 * @param message       what it passed to {@code Panic.Halt}, or what the machine says when it simply went down
 * @param trace         the error and the calls it came through, a line each; empty for a halt it asked for
 * @param pid           the process that went down
 * @param memoryMb      the memory it held
 * @param background    the colour behind the page, "#RRGGBB" or a name; empty for the mod's dark
 * @param text          the colour of its words; empty for the mod's light
 * @param logo          the picture at the top left, a path on its disk; empty for none
 * @param rebootSeconds how long the page stands before the machine starts again; negative when it stands until the
 *                      player restarts it
 */
public record PlayerPanic(String system, String author, String message, List<String> trace, int pid, int memoryMb,
                          String background, String text, String logo, int rebootSeconds) {

    /** The most lines of the error a page carries. */
    public static final int MOST_TRACE = 16;

    public PlayerPanic {
        trace = List.copyOf(trace.size() > MOST_TRACE ? trace.subList(0, MOST_TRACE) : trace);
    }

    /** Whether the page stands until the player restarts the machine, the system having asked for no reboot. */
    public boolean standsUntilRestarted() {
        return this.rebootSeconds < 0;
    }
}
