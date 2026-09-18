/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.files;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.install.LiveInstallState;
import java.util.Optional;

/**
 * The files of a by-hand install, for an editor that asks the machine for one.
 *
 * <p>An editor reads and writes files by name and expects a disk behind the name. A machine running a live
 * medium has files that are on no disk: the medium's own, and those of the system being built, which only
 * become a disk's when the install is finished. They are named with a mark in front, and a name that carries
 * it is answered from the session instead, from wherever the session is standing, since that is what the name
 * was typed against.
 */
public final class LiveSessionFiles {

    private LiveSessionFiles() {
    }

    /** Whether that name is one of a session's files rather than a disk's. */
    public static boolean names(final String path) {
        return path.startsWith(LiveInstallState.FILE_SCHEME);
    }

    /** What the file holds, or nothing when the session has no such file or the machine has no session. */
    public static Optional<String> read(final IOsHost computer, final String path) {
        final LiveInstallState live = sessionOf(computer);
        return live == null ? Optional.empty() : Optional.ofNullable(live.fileAt(typed(path)));
    }

    /**
     * Writes the file, which is what the steps that come after will read.
     *
     * @return false when the machine has no session to write it into
     */
    public static boolean write(final IOsHost computer, final String path, final String content) {
        final LiveInstallState live = sessionOf(computer);
        if (live == null) {
            return false;
        }
        live.writeFileAt(typed(path), content);
        computer.setChanged();
        return true;
    }

    private static LiveInstallState sessionOf(final IOsHost computer) {
        final ComputerConsoleState console = computer.console();
        return console == null ? null : console.liveInstall();
    }

    private static String typed(final String path) {
        return path.substring(LiveInstallState.FILE_SCHEME.length());
    }
}
