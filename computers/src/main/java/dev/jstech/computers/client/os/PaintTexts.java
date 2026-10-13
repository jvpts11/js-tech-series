/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What Paint says: its buttons, its tools, the file windows it opens and its status line. File names, sizes and
 * positions are data. Kept apart from the window so the language generator can read it on a server too, where
 * windows do not exist.
 */
@TextHolder
final class PaintTexts {

    // The toolbar.
    static final TextKey NEW = TextKey.of("jsc.paint.new", "New");
    static final TextKey OPEN = TextKey.of("jsc.paint.open", "Open");
    static final TextKey SAVE = TextKey.of("jsc.paint.save", "Save");
    static final TextKey UNDO = TextKey.of("jsc.paint.undo", "Undo");
    static final TextKey ZOOM = TextKey.of("jsc.paint.zoom", "%sx");
    static final TextKey WALLPAPER = TextKey.of("jsc.paint.wallpaper", "Wallpaper");

    // The tools.
    static final TextKey PENCIL = TextKey.of("jsc.paint.pencil", "Pencil");
    static final TextKey ERASER = TextKey.of("jsc.paint.eraser", "Eraser");
    static final TextKey FILL = TextKey.of("jsc.paint.fill", "Fill");
    static final TextKey DROPPER = TextKey.of("jsc.paint.dropper", "Dropper");
    static final TextKey LINE = TextKey.of("jsc.paint.line", "Line");
    static final TextKey RECTANGLE = TextKey.of("jsc.paint.rectangle", "Rectangle");
    static final TextKey ELLIPSE = TextKey.of("jsc.paint.ellipse", "Ellipse");

    // The file windows it opens.
    static final TextKey OPEN_PICTURE = TextKey.of("jsc.paint.open_picture", "Open picture");
    static final TextKey SAVE_PICTURE = TextKey.of("jsc.paint.save_picture", "Save picture");
    static final TextKey PICTURES = TextKey.of("jsc.paint.pictures", "Pictures");

    // The status line.
    static final TextKey POSITION = TextKey.of("jsc.paint.position", "x %s, y %s");
    static final TextKey SIZE = TextKey.of("jsc.paint.size", "%s x %s");
    static final TextKey NEW_PICTURE = TextKey.of("jsc.paint.new_picture", "New picture");
    static final TextKey NOTHING_TO_UNDO = TextKey.of("jsc.paint.nothing_to_undo", "Nothing to undo");
    static final TextKey NO_SUCH_PICTURE = TextKey.of("jsc.paint.no_such_picture", "No such picture");
    static final TextKey NOT_A_PICTURE = TextKey.of("jsc.paint.not_a_picture", "%s is not a picture");
    static final TextKey TOO_LARGE_TO_SAVE = TextKey.of("jsc.paint.too_large_to_save", "Picture too large to save");
    static final TextKey SAVING = TextKey.of("jsc.paint.saving", "Saving %s");
    static final TextKey TOO_LARGE_TO_OPEN =
            TextKey.of("jsc.paint.too_large_to_open", "That picture is too large to open here");
    static final TextKey SAVE_FIRST = TextKey.of("jsc.paint.save_first", "Save it first");
    static final TextKey NOW_WALLPAPER = TextKey.of("jsc.paint.now_wallpaper", "%s is now the wallpaper");

    private PaintTexts() {
    }
}
