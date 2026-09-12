/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

/**
 * What a path in the file explorer means.
 *
 * <p>The explorer shows more than one world in the same tree: the machine's own disk, a removable
 * medium in a reader, another computer's shared folder, and now the disk of a ComputerCraft computer on
 * the other side of a Gateway. Which of those a path names is decided by how it starts, and that
 * deciding is all here: it is the kind of thing that is easy to get subtly wrong in the middle of a
 * screen full of drawing, and the kind of thing that is easy to be sure of on its own.
 *
 * <p>Nothing here draws or knows anything about the game.
 */
public final class FilesPaths {

    /** Another machine of ours: {@code net:host/share/rest}. */
    public static final String NET = "net:";
    /** A removable medium in a reader: {@code media:<reader>/rest}. */
    public static final String MEDIA = "media:";
    /** A ComputerCraft computer across a Gateway: {@code cc:} for the computers, {@code cc:3/rest} below. */
    public static final String CC = "cc:";

    private FilesPaths() {
    }

    /** Whether the path names something on a ComputerCraft computer. */
    public static boolean isCc(final String path) {
        return path != null && path.startsWith(CC);
    }

    /**
     * Which computer over there a path names, or 0 for the list of computers itself.
     *
     * <p>Zero also answers for anything malformed, which lands the explorer on the list of computers
     * rather than asking a computer that does not exist.
     */
    public static int ccComputer(final String path) {
        if (!isCc(path)) {
            return 0;
        }
        final String rest = path.substring(CC.length());
        final int slash = rest.indexOf('/');
        final String number = slash < 0 ? rest : rest.substring(0, slash);
        if (number.isEmpty() || !number.chars().allMatch(Character::isDigit)) {
            return 0;
        }
        try {
            return Integer.parseInt(number);
        } catch (final NumberFormatException tooLong) {
            return 0;
        }
    }

    /** The folder on that computer, as that computer writes it: always starting at its root. */
    public static String ccPath(final String path) {
        if (!isCc(path)) {
            return "/";
        }
        final String rest = path.substring(CC.length());
        final int slash = rest.indexOf('/');
        if (slash < 0 || slash + 1 >= rest.length()) {
            return "/";
        }
        return "/" + rest.substring(slash + 1);
    }

    /** The explorer's path for that folder of that computer. */
    public static String ccOf(final int computer, final String there) {
        if (computer <= 0) {
            return CC;
        }
        final String bare = there == null ? "" : there.replace('\\', '/');
        final String trimmed = bare.startsWith("/") ? bare.substring(1) : bare;
        return trimmed.isEmpty() ? CC + computer : CC + computer + "/" + trimmed;
    }

    /** The name to show for a path over there: the computer, or the last part of the folder. */
    public static String ccName(final String path) {
        if (!isCc(path)) {
            return path;
        }
        final String rest = path.substring(CC.length());
        final int slash = rest.lastIndexOf('/');
        if (slash < 0) {
            return "Computer " + rest;
        }
        return rest.substring(slash + 1);
    }

    /** How deep a network path is: 0 for a host, 1 for a share, more below it; -1 for anything else. */
    public static int netDepth(final String path) {
        if (path == null || !path.startsWith(NET)) {
            return -1;
        }
        int depth = 0;
        for (int i = NET.length(); i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                depth++;
            }
        }
        return depth;
    }

    /** The last name in a network path: the host, the share, or the entry below. */
    public static String netName(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path.substring(NET.length());
    }

    /** The folder holding that one, in whichever of the worlds the path is in. */
    public static String parentOf(final String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        final int slash = path.lastIndexOf('/');
        if (slash < 0) {
            // The top of one of the other worlds goes to its own root, and the disk's top to the disk.
            return isCc(path) ? CC : path.startsWith(MEDIA) || path.startsWith(NET) ? "" : "";
        }
        final String above = path.substring(0, slash);
        return above.endsWith(":") ? above : above;
    }
}
