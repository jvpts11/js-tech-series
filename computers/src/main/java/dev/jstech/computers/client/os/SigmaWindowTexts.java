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

/** What a Sigma program's own window says when the program itself has not. */
@TextHolder
final class SigmaWindowTexts {

    static final TextKey DEFAULT_TITLE = TextKey.of("jsc.sigma_window.default_title", "Window");
    /** How far an operation has gone: done, then the whole. */
    static final TextKey DONE_OF = TextKey.of("jsc.sigma_window.done_of", "%s of %s");
    /** In place of a component no mod on this game draws, naming its kind. */
    static final TextKey NEEDS = TextKey.of("jsc.sigma_window.needs", "Needs %s");
    /** In place of a component that reaches outside the game, which this player's settings keep off. */
    static final TextKey OFF_HERE = TextKey.of("jsc.sigma_window.off_here", "%s is off on this game");
    /** In place of a component whose renderer failed. */
    static final TextKey COULD_NOT_DRAW = TextKey.of("jsc.sigma_window.could_not_draw", "Could not draw %s");
    /** In place of a picture the machine has not found. */
    static final TextKey NO_PICTURE = TextKey.of("jsc.sigma_window.no_picture", "No picture");
    /** What an item picker's empty search line says. */
    static final TextKey SEARCH = TextKey.of("jsc.sigma_window.search", "Search");
    /** Some of what the program holds was left out, because the window was too big to send whole. */
    static final TextKey CUT = TextKey.of("jsc.sigma_window.cut", "(more than the window can show)");
    /** The kind of file a dialog filters by, named after its pattern. */
    static final TextKey FILTER_NAMED = TextKey.of("jsc.sigma_window.filter_named", "Files (%s)");
    /** A file dialog on a terminal: the line a path is typed in, and its two answers. */
    static final TextKey FILE_NAME = TextKey.of("jsc.sigma_window.file_name", "File name:");
    static final TextKey OK = TextKey.of("jsc.sigma_window.ok", "OK");
    static final TextKey CANCEL = TextKey.of("jsc.sigma_window.cancel", "Cancel");

    private SigmaWindowTexts() {
    }
}
