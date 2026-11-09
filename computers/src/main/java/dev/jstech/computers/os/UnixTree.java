/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import java.util.List;

/**
 * Where a system met at a Unix prompt keeps the player's home and mounts the other drives.
 *
 * <p>The families agree on one rooted tree and on little else about it. A Linux and FreeBSD put the home under
 * {@code /home} and mount what is plugged in under {@code /media}; System V, which is older than both habits,
 * keeps people under {@code /usr} and mounts under {@code /mnt}. The storage underneath is the same for all of
 * them, so this is only ever a way of naming it, and everything that writes or reads such a path asks here.
 *
 * <p>Quince's systems keep trees of their own (computers/docs/design/quince.md): the classic ones a System Folder and
 * the Desktop Folder at the root of the disk, Quince OS X and quinceOS the Applications, the Library, the System and
 * the Users, with every other disk under /Volumes.
 *
 * <p>Pure, with no Minecraft types, so the path arithmetic built on it is unit-tested.
 *
 * @param home        the segments of the home directory under the root
 * @param mounts      the directory under the root where every other drive is mounted, by its letter
 * @param desktop     the segments of the folder whose files a desktop shows on its background
 * @param directories the tree the system lays down on its disk when it is installed, parents before children
 */
public record UnixTree(List<String> home, String mounts, List<String> desktop, List<String> directories) {

    /** The tree of the Linux distributions and of FreeBSD. */
    public static final UnixTree HOME_AND_MEDIA = new UnixTree(List.of("home", "player"), "media",
            List.of("home", "player", "Desktop"), List.of("bin", "etc", "home", "home/player", "home/player/Desktop",
            "home/player/Documents", "media", "tmp", "usr", "usr/bin", "var"));

    /** System V's, from before either habit: people live under /usr and drives are mounted under /mnt. */
    public static final UnixTree SYSTEM_V = new UnixTree(List.of("usr", "player"), "mnt",
            List.of("usr", "player", "Desktop"), List.of("bin", "etc", "mnt", "tmp", "usr", "usr/bin", "usr/lib",
            "usr/player", "usr/player/Desktop", "usr/player/Documents"));

    /** The Quince System's: the System Folder and the Desktop Folder, where its documents live too. */
    public static final UnixTree QUINCE_SYSTEM = new UnixTree(List.of("Desktop Folder"), "Volumes",
            List.of("Desktop Folder"), List.of("Desktop Folder", "System Folder", "Volumes"));

    /** Quince OS 9's: the System Folder with its control panels and extensions, the Applications and the Documents. */
    public static final UnixTree QUINCE_OS_9 = new UnixTree(List.of("Documents"), "Volumes",
            List.of("Desktop Folder"), List.of("Applications", "Desktop Folder", "Documents", "System Folder",
            "System Folder/Control Panels", "System Folder/Extensions", "Volumes"));

    /** Quince OS X's and quinceOS's: the Applications, the Library, the System and the Users. */
    public static final UnixTree QUINCE_X = new UnixTree(List.of("Users", "player"), "Volumes",
            List.of("Users", "player", "Desktop"), List.of("Applications", "Applications/Utilities", "Library",
            "System", "System/Library", "Users", "Users/player", "Users/player/Desktop", "Users/player/Documents",
            "Users/player/Music", "Volumes", "bin", "etc", "tmp", "usr", "usr/bin", "usr/local"));

    public UnixTree {
        home = List.copyOf(home);
        desktop = List.copyOf(desktop);
        directories = List.copyOf(directories);
    }

    /** The tree a system of that family keeps; the common one for a family that says nothing else. */
    public static UnixTree of(final Platform platform) {
        return of(platform, 0);
    }

    /**
     * The tree a system of that family keeps at {@code familyRank} in it: Quince's own by the age of the system, the
     * family's one tree for the others. A Quince system of no rank keeps the later systems' tree, which is the only one
     * a prompt is ever met on.
     */
    public static UnixTree of(final Platform platform, final int familyRank) {
        return switch (platform) {
            case UNIX -> SYSTEM_V;
            case QUINCE -> switch (familyRank) {
                case QuinceFamily.QUINCE_SYSTEM -> QUINCE_SYSTEM;
                case QuinceFamily.OS_9 -> QUINCE_OS_9;
                default -> QUINCE_X;
            };
            default -> HOME_AND_MEDIA;
        };
    }

    /** The home directory as a path under the root, without a leading slash. */
    public String homePath() {
        return String.join("/", this.home);
    }

    /** The folder whose files a desktop shows as icons on its background. */
    public String desktopPath() {
        return String.join("/", this.desktop);
    }

    /** The folder songs are kept in, made the first time one is brought. */
    public String musicPath() {
        return homePath() + "/Music";
    }
}
