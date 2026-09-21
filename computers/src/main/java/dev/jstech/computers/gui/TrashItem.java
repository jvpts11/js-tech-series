/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

import dev.jstech.computers.os.fs.FsPaths;
import java.util.List;

/**
 * One thing in the trash as a trash window shows it: by the name it had, with the folder it was in written the way
 * the desktop writes a place, and its size.
 *
 * <p>Pure, with no Minecraft types, so what a window says of a deleted file is unit-tested.
 *
 * @param stored    the name it is kept under in the trash, which is how the window names it back to the machine
 * @param name      the name it had before it was deleted
 * @param place     the folder it was in, as the desktop writes a place
 * @param directory whether it is a folder
 * @param weight    the room it takes
 */
public record TrashItem(String stored, String name, String place, boolean directory, long weight) {

    /**
     * An item for a thing kept under {@code stored} that was at {@code original}, placed the Frames way with a drive
     * letter and backslashes, or the Unix way from the root.
     */
    public static TrashItem of(final String stored, final String original, final boolean directory,
                               final long weight, final boolean frames) {
        final String parent = FsPaths.parentDir(original);
        final String place = frames ? "C:\\" + parent.replace('/', '\\') : "/" + parent;
        return new TrashItem(stored, FsPaths.fileName(original), place, directory, weight);
    }

    /** What a list of things adds up to, the way the windows count them: {@code 3 items, 18 mB}. */
    public static String summary(final List<TrashItem> items) {
        long total = 0L;
        for (final TrashItem item : items) {
            total += item.weight();
        }
        return items.size() + (items.size() == 1 ? " item, " : " items, ") + total + " mB";
    }

    /** CDE counts what a window holds as objects, and says nothing of their size. */
    public static String objects(final List<TrashItem> items) {
        return items.size() + (items.size() == 1 ? " object" : " objects");
    }

    /** The extension of the name, without its dot, or empty for a folder or a name with none. */
    public String extension() {
        final int dot = this.name.lastIndexOf('.');
        return this.directory || dot <= 0 || dot == this.name.length() - 1 ? "" : this.name.substring(dot + 1);
    }

    /** Its size as the Size column writes it. */
    public String size() {
        return this.weight + " mB";
    }
}
