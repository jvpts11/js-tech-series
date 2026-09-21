/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsCapability;

import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.UnixTree;
import java.util.List;

/**
 * The system folder skeleton a graphical desktop OS lays down on the system disk when it is
 * installed. Only a {@link OsCapability#FULL_DESKTOP} OS gets the full tree; terminal-only and
 * network OSes keep a bare filesystem.
 *
 * <p>This class is pure and carries no Minecraft dependency, so the layout can be unit-tested in
 * JUnit and reused by the install path in the binding layer. Parent directories are listed before
 * their children so a caller that materialises them in order never references a missing parent.
 */
public final class SystemLayout {

    private SystemLayout() {
    }

    /** The directory whose files are shown as icons on the desktop background. */
    public static final String DESKTOP_DIR = "Users/Public/Desktop";

    /** The ordered system directories a full desktop OS provides (parents before children). */
    /** The folder the system itself lives in, named after the system line rather than after somebody else's. */
    public static final String SYSTEM_DIR = "Frames";

    private static final List<String> DESKTOP_DIRECTORIES = List.of(
            "Program Files",
            "Program Files (x86)",
            SYSTEM_DIR,
            "Users",
            "Users/Public",
            "Users/Public/Desktop",
            "Users/Public/Documents"
    );

    /**
     * The directory a Linux or FreeBSD system shows as the desktop once a desktop environment is installed. What
     * asks on behalf of a particular system asks {@link #desktopDirFor(OsDef, KernelDef)}, since System V keeps
     * its people somewhere else.
     */
    public static final String POSIX_DESKTOP_DIR = UnixTree.HOME_AND_MEDIA.desktopPath();

    /**
     * Returns the system directories the given capability provisions on install.
     *
     * @param capability the capability tier of the OS being installed
     * @return the ordered directory paths to create, or an empty list for non-desktop OSes
     */
    public static List<String> directoriesFor(final OsCapability capability) {
        return capability == OsCapability.FULL_DESKTOP ? DESKTOP_DIRECTORIES : List.of();
    }

    /**
     * The desktop folder for an OS: the desktop under that system's own home on a POSIX kernel, the
     * Windows-style one otherwise.
     */
    public static String desktopDirFor(final OsDef os, final KernelDef kernel) {
        if (kernel == null || kernel.shellFamily() != ShellFamily.POSIX) {
            return DESKTOP_DIR;
        }
        return os == null ? POSIX_DESKTOP_DIR : UnixTree.of(os.platform()).desktopPath();
    }

    /**
     * Returns the system directories an OS provisions on install, by its kernel's shell family: a POSIX
     * kernel lays down its family's Unix tree regardless of capability (a terminal system still has a home and
     * /etc), a DOS kernel follows the capability rule above.
     */
    public static List<String> directoriesFor(final OsDef os,
                                              final KernelDef kernel) {
        if (kernel != null && kernel.shellFamily() == ShellFamily.POSIX) {
            return UnixTree.of(os.platform()).directories();
        }
        return directoriesFor(os.capability());
    }
}
