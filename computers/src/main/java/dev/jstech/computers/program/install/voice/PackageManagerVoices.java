/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.os.PackageManagerKind;

/**
 * What a package manager that ships built packages says before it installs or removes one, in its own words.
 *
 * <p>Each of them has a voice a player who has used the real one knows on sight, and the lines are that voice: what
 * was resolved, what will be installed or removed, how big it is, and where it comes from. They are one message, a
 * line at a time, and the bar the machine draws afterwards follows them.
 */
public final class PackageManagerVoices {

    private PackageManagerVoices() {
    }

    /**
     * Before the download starts.
     *
     * @param mb         how big the package is
     * @param mirrorHost the name the Mirror's machine goes by
     */
    public static String fetch(final PackageManagerKind manager, final String pkg, final String version,
                               final int mb, final String mirrorHost) {
        return switch (manager) {
            case APT -> String.join("\n",
                    "Reading package lists... Done",
                    "Building dependency tree... Done",
                    "The following NEW packages will be installed:",
                    "  " + pkg,
                    "Need to get " + mb + " MB of archives.",
                    "Get:1 mirror://" + mirrorHost + " stable/main " + pkg + " " + version + " [" + mb + " MB]");
            case DNF -> String.join("\n",
                    "Last metadata expiration check: 0:00:01 ago.",
                    "Dependencies resolved.",
                    "Installing:  " + pkg + "  x86_64  " + version + "  mirror  " + mb + " MB",
                    "Downloading Packages:");
            case PACMAN -> String.join("\n",
                    "resolving dependencies...",
                    "looking for conflicting packages...",
                    "Packages (1) " + pkg + "-" + version,
                    "Total Download Size: " + mb + ".00 MiB",
                    ":: Retrieving packages...");
            case PKG -> String.join("\n",
                    "The following 1 package(s) will be affected (of 0 checked):",
                    "",
                    "New packages to be INSTALLED:",
                    "        " + pkg + ": " + version,
                    "",
                    "Number of packages to be installed: 1",
                    "",
                    mb + " MiB to be downloaded.",
                    "[1/1] Fetching " + pkg + "-" + version + ".pkg from mirror://" + mirrorHost);
            default -> "Fetching " + pkg + " " + version + " from mirror://" + mirrorHost + " [" + mb + " MB]";
        };
    }

    /**
     * Before the package is taken off.
     *
     * @param mb how much room taking it off frees
     */
    public static String remove(final PackageManagerKind manager, final String pkg, final String version,
                                final int mb) {
        return switch (manager) {
            case APT -> String.join("\n",
                    "Reading package lists... Done",
                    "Building dependency tree... Done",
                    "The following packages will be REMOVED:",
                    "  " + pkg,
                    "After this operation, " + mb + " MB disk space will be freed.",
                    "Removing " + pkg + " (" + version + ") ...");
            case DNF -> String.join("\n",
                    "Dependencies resolved.",
                    "Removing:  " + pkg + "  x86_64  " + version,
                    "Running transaction");
            case PACMAN -> String.join("\n",
                    "checking dependencies...",
                    "Packages (1) " + pkg + "-" + version,
                    ":: Removing " + pkg + " ...");
            case PKG -> String.join("\n",
                    "Checking integrity... done (0 conflicting)",
                    "Deinstallation has been requested for the following 1 packages:",
                    "",
                    "Installed packages to be REMOVED:",
                    "        " + pkg + ": " + version,
                    "",
                    "The operation will free " + mb + " MiB.",
                    "[1/1] Deinstalling " + pkg + "-" + version + "...");
            default -> "Removing " + pkg + " ...";
        };
    }
}
