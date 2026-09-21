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
 * <p>Pure, with no Minecraft types, so the path arithmetic built on it is unit-tested.
 *
 * @param home        the segments of the home directory under the root
 * @param mounts      the directory under the root where every other drive is mounted, by its letter
 * @param directories the tree the system lays down on its disk when it is installed, parents before children
 */
public record UnixTree(List<String> home, String mounts, List<String> directories) {

    /** The tree of the Linux distributions and of FreeBSD. */
    public static final UnixTree HOME_AND_MEDIA = new UnixTree(List.of("home", "player"), "media", List.of(
            "bin", "etc", "home", "home/player", "home/player/Desktop", "home/player/Documents", "media", "tmp",
            "usr", "usr/bin", "var"));

    /** System V's, from before either habit: people live under /usr and drives are mounted under /mnt. */
    public static final UnixTree SYSTEM_V = new UnixTree(List.of("usr", "player"), "mnt", List.of(
            "bin", "etc", "mnt", "tmp", "usr", "usr/bin", "usr/lib", "usr/player", "usr/player/Desktop",
            "usr/player/Documents"));

    public UnixTree {
        home = List.copyOf(home);
        directories = List.copyOf(directories);
    }

    /** The tree a system of that family keeps; the common one for a family that says nothing else. */
    public static UnixTree of(final Platform platform) {
        return platform == Platform.UNIX ? SYSTEM_V : HOME_AND_MEDIA;
    }

    /** The home directory as a path under the root, without a leading slash. */
    public String homePath() {
        return String.join("/", this.home);
    }

    /** The folder whose files a desktop shows as icons on its background. */
    public String desktopPath() {
        return homePath() + "/Desktop";
    }
}
