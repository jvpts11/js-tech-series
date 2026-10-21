/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the developer's tools for screens say: the inspector, the editor and the new screen. */
@TextHolder
public final class DeveloperTexts {

    public static final TextKey INSPECTOR = TextKey.of("gui.jscore.inspector", "Inspector");
    public static final TextKey INSPECTOR_KEY = TextKey.of("gui.jscore.inspector.key", "F12");
    public static final TextKey LAYOUT = TextKey.of("gui.jscore.inspector.layout", "layout");
    public static final TextKey COMPONENT = TextKey.of("gui.jscore.inspector.component", "component");
    public static final TextKey PROBLEMS = TextKey.of("gui.jscore.inspector.problems", "Problems");
    public static final TextKey NO_PROBLEMS = TextKey.of("gui.jscore.inspector.no_problems", "None found");
    public static final TextKey GUI_SCALE = TextKey.of("gui.jscore.inspector.gui_scale", "GUI scale: %s");
    public static final TextKey GUI_SCALE_AUTO = TextKey.of("gui.jscore.inspector.gui_scale.auto", "Auto");
    public static final TextKey LANGUAGE = TextKey.of("gui.jscore.inspector.language", "Language: %s");
    public static final TextKey STATE = TextKey.of("gui.jscore.inspector.state", "State: %s");
    public static final TextKey AS_IT_IS = TextKey.of("gui.jscore.inspector.as_it_is", "as it is");
    /** The lines of the card of the component under the pointer. */
    public static final TextKey BOX = TextKey.of("gui.jscore.inspector.box", "box");
    public static final TextKey BOX_VALUE = TextKey.of("gui.jscore.inspector.box.value", "x %s  y %s  w %s  h %s");
    public static final TextKey BOUND = TextKey.of("gui.jscore.inspector.bound", "bound to");
    public static final TextKey BOUND_VALUE = TextKey.of("gui.jscore.inspector.bound.value", "%s, now %s");
    public static final TextKey DOES = TextKey.of("gui.jscore.inspector.does", "does");
    public static final TextKey STYLE = TextKey.of("gui.jscore.inspector.style", "style");
    public static final TextKey REDRAWS = TextKey.of("gui.jscore.inspector.redraws", "redraws");
    public static final TextKey NEVER = TextKey.of("gui.jscore.inspector.redraws.never", "never: drawn once");
    public static final TextKey WHEN = TextKey.of("gui.jscore.inspector.redraws.when", "when %s changes");
    public static final TextKey EVERY_FRAME = TextKey.of("gui.jscore.inspector.redraws.every_frame", "every frame");
    public static final TextKey FILE = TextKey.of("gui.jscore.inspector.file", "file");
    public static final TextKey FILE_VALUE = TextKey.of("gui.jscore.inspector.file.value", "%s, line %s");
    public static final TextKey COPIES = TextKey.of("gui.jscore.inspector.copies", "Ctrl+C copies the path");
    public static final TextKey CARD_LINE = TextKey.of("gui.jscore.inspector.card_line", "%s  %s");
    /** The problems. */
    public static final TextKey UNWIRED = TextKey.of("gui.jscore.inspector.unwired", "Nobody hands over %s");
    public static final TextKey NO_TEXT = TextKey.of("gui.jscore.inspector.no_text", "%s: %s has no %s text");
    public static final TextKey NO_NARRATION = TextKey.of("gui.jscore.inspector.no_narration",
            "%s: no narrator text");
    public static final TextKey OVERLAP = TextKey.of("gui.jscore.inspector.overlap", "%s overlap");
    public static final TextKey OUTSIDE = TextKey.of("gui.jscore.inspector.outside", "%s is past the window");

    /** The editor of screens. */
    public static final TextKey EDITING = TextKey.of("gui.jscore.screen_editor", "Editing %s");
    public static final TextKey UNDO = TextKey.of("gui.jscore.screen_editor.undo", "Undo");
    public static final TextKey REDO = TextKey.of("gui.jscore.screen_editor.redo", "Redo");
    public static final TextKey SNAP = TextKey.of("gui.jscore.screen_editor.snap", "Snap: %s px");
    public static final TextKey ADD = TextKey.of("gui.jscore.screen_editor.add", "Add...");
    public static final TextKey SAVE = TextKey.of("gui.jscore.screen_editor.save", "Save");
    public static final TextKey NAME = TextKey.of("gui.jscore.screen_editor.name", "Name");
    public static final TextKey BOUND_TO = TextKey.of("gui.jscore.screen_editor.bound_to", "Bound to");
    public static final TextKey STYLE_IS = TextKey.of("gui.jscore.screen_editor.style", "Style: %s");
    public static final TextKey DUPLICATE = TextKey.of("gui.jscore.screen_editor.duplicate", "Duplicate");
    public static final TextKey DELETE = TextKey.of("gui.jscore.screen_editor.delete", "Delete");
    public static final TextKey POSITION = TextKey.of("gui.jscore.screen_editor.position", "x %s  y %s    %s x %s");
    public static final TextKey ADD_HINT = TextKey.of("gui.jscore.screen_editor.add_hint",
            "Enter: add it at the pointer");
    public static final TextKey HINTS = TextKey.of("gui.jscore.screen_editor.hints",
            "Drag: move    Corners: resize    Alt: no snapping    Every change is written to the file");
    public static final TextKey NOT_TAKEN = TextKey.of("gui.jscore.screen_editor.not_taken", "Not taken: %s");
    public static final TextKey NO_SOURCE = TextKey.of("gui.jscore.screen_editor.no_source",
            "No source folder for %s in this run: the change holds until the game closes");

    /** The new screen from the block looked at. */
    public static final TextKey NEW_FOR = TextKey.of("gui.jscore.new_screen.for", "New screen for %s");
    public static final TextKey NEW = TextKey.of("gui.jscore.new_screen", "New screen");
    public static final TextKey LOOKED_AT = TextKey.of("gui.jscore.new_screen.looked_at", "for the block you look at");
    public static final TextKey BLOCK = TextKey.of("gui.jscore.new_screen.block", "Block");
    public static final TextKey FILE_FIELD = TextKey.of("gui.jscore.new_screen.file", "File");
    public static final TextKey START_FROM = TextKey.of("gui.jscore.new_screen.start_from", "Start from");
    public static final TextKey MACHINE = TextKey.of("gui.jscore.new_screen.machine", "Machine, inventory below");
    public static final TextKey NO_INVENTORY = TextKey.of("gui.jscore.new_screen.no_inventory", "No inventory");
    public static final TextKey TAB = TextKey.of("gui.jscore.new_screen.tab", "A tab of a screen");
    public static final TextKey HUD = TextKey.of("gui.jscore.new_screen.hud", "A HUD element");
    public static final TextKey COPY = TextKey.of("gui.jscore.new_screen.copy", "A copy of %s");
    public static final TextKey COPY_NONE = TextKey.of("gui.jscore.new_screen.copy.none", "A copy of a screen...");
    public static final TextKey CREATE = TextKey.of("gui.jscore.new_screen.create", "Create");
    public static final TextKey CREATE_HINT = TextKey.of("gui.jscore.new_screen.create_hint",
            "Create writes the file and opens it here, in the editor; the screen is then filled with Add");
    public static final TextKey EMPTY_FACE = TextKey.of("gui.jscore.new_screen.empty",
            "The face is empty. Add puts the first component here.");
    public static final TextKey NOT_WRITTEN = TextKey.of("gui.jscore.new_screen.not_written",
            "The file could not be written: %s");

    private DeveloperTexts() {
    }
}
