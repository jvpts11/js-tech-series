/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.os.UnixTree;
import java.util.List;
import java.util.Locale;

/**
 * The POSIX view over the computer's drives. Storage keeps the DOS model underneath (the system disk is
 * {@code C:}, further disks and media readers get the next letters), and this class maps a single rooted
 * tree onto it the way a Unix shell sees it:
 * <ul>
 *   <li>{@code /} is the root of the system disk, and {@code ~} is the user's home, which is where that system
 *       keeps it ({@code /home/player} on a Linux and FreeBSD, {@code /usr/player} on System V);</li>
 *   <li>every other drive is mounted by its letter under that system's mount point ({@code /media/d},
 *       {@code /mnt/d});</li>
 *   <li>relative paths, {@code .} and {@code ..} resolve against the current directory as usual.</li>
 * </ul>
 * Pure (no Minecraft types), so it is unit-tested; the command set converts every path argument through
 * {@link #toDos} and then reuses the same filesystem facade the DOS shell uses. The forms that name no tree are
 * the common one's, for what has no system to ask.
 */
public final class PosixPath {

    /** The drive whose root is {@code /}. */
    public static final char SYSTEM_DRIVE = 'C';
    /** The home directory segments under the root, on the common tree. */
    public static final List<String> HOME_SEGMENTS = UnixTree.HOME_AND_MEDIA.home();
    /** The mount point directory for the other drives, on the common tree. */
    public static final String MEDIA_ROOT = UnixTree.HOME_AND_MEDIA.mounts();

    private PosixPath() {
    }

    /** The home directory as a location on the system drive. */
    public static DosPath.Location home() {
        return home(UnixTree.HOME_AND_MEDIA);
    }

    /** The home directory of a system that keeps that tree. */
    public static DosPath.Location home(final UnixTree tree) {
        return new DosPath.Location(SYSTEM_DRIVE, tree.home());
    }

    public static String toDos(final String input) {
        return toDos(UnixTree.HOME_AND_MEDIA, input);
    }

    /**
     * Converts a POSIX path argument into the DOS-form string {@link DosPath#resolve} understands. Absolute
     * paths become drive-qualified ({@code /x} is {@code C:\x}, {@code /media/d/x} is {@code D:\x}), a
     * leading {@code ~} expands to the home directory, and relative paths pass through unchanged (they are
     * resolved against the current directory, {@code .} and {@code ..} included).
     */
    public static String toDos(final UnixTree tree, final String input) {
        final String in = input == null ? "" : input.trim();
        if (in.isEmpty()) {
            return "";
        }
        final String home = SYSTEM_DRIVE + ":\\" + String.join("\\", tree.home());
        if (in.equals("~")) {
            return home;
        }
        if (in.startsWith("~/")) {
            return home + "\\" + in.substring(2);
        }
        // Another machine's shared folder: the same path a DOS shell writes with backslashes.
        final NetPath net = NetPath.parse(in);
        if (net != null) {
            return net.display();
        }
        if (!in.startsWith("/")) {
            return in;
        }
        final String rest = in.replaceFirst("^/+", "");
        final String mountPrefix = tree.mounts() + "/";
        if (rest.startsWith(mountPrefix)) {
            final String afterMount = rest.substring(mountPrefix.length());
            final int slash = afterMount.indexOf('/');
            final String mount = slash < 0 ? afterMount : afterMount.substring(0, slash);
            if (mount.length() == 1 && Character.isLetter(mount.charAt(0))) {
                final char drive = Character.toUpperCase(mount.charAt(0));
                final String tail = slash < 0 ? "" : afterMount.substring(slash + 1);
                return drive + ":\\" + tail.replace('/', '\\');
            }
        }
        return SYSTEM_DRIVE + ":\\" + rest.replace('/', '\\');
    }

    public static String render(final DosPath.Location location) {
        return render(UnixTree.HOME_AND_MEDIA, location);
    }

    /** Renders a location as an absolute POSIX path ({@code /}, {@code /home/player/docs}, {@code /media/d/x}). */
    public static String render(final UnixTree tree, final DosPath.Location location) {
        final String joined = String.join("/", location.segments());
        if (location.drive() == SYSTEM_DRIVE) {
            return "/" + joined;
        }
        final String letter = Character.toString(location.drive()).toLowerCase(Locale.ROOT);
        final String mount = "/" + tree.mounts() + "/" + letter;
        return joined.isEmpty() ? mount : mount + "/" + joined;
    }

    public static String renderForPrompt(final DosPath.Location location) {
        return renderForPrompt(UnixTree.HOME_AND_MEDIA, location);
    }

    /** Renders a location for the prompt, abbreviating the home directory to {@code ~}. */
    public static String renderForPrompt(final UnixTree tree, final DosPath.Location location) {
        final List<String> home = tree.home();
        if (location.drive() == SYSTEM_DRIVE && startsWith(location.segments(), home)) {
            final List<String> rest = location.segments().subList(home.size(), location.segments().size());
            return rest.isEmpty() ? "~" : "~/" + String.join("/", rest);
        }
        return render(tree, location);
    }

    private static boolean startsWith(final List<String> segments, final List<String> home) {
        if (segments.size() < home.size()) {
            return false;
        }
        for (int i = 0; i < home.size(); i++) {
            if (!segments.get(i).equalsIgnoreCase(home.get(i))) {
                return false;
            }
        }
        return true;
    }
}
