/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.edit;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What {@code /jstech edit} says. */
@TextHolder
public final class EditTexts {

    /** How many blocks, then the file's id. */
    public static final TextKey EXPORTED = TextKey.of("jscore.command.edit.exported",
            "Saved %s blocks as the structure file %s");
    public static final TextKey NOT_EXPORTED = TextKey.of("jscore.command.edit.not_exported",
            "The structure file %s could not be written");
    public static final TextKey NO_FILE = TextKey.of("jscore.command.edit.no_file", "No structure file is called %s");
    /** How many blocks, then the file's id. */
    public static final TextKey IMPORTING = TextKey.of("jscore.command.edit.importing",
            "Setting the %s blocks of %s; /jstech edit undo puts back what they replace");
    public static final TextKey AT_LIMIT = TextKey.of("jscore.command.edit.at_limit",
            "You have as many large edits under way as you may; wait for one to end");
    public static final TextKey UNDOING = TextKey.of("jscore.command.edit.undoing",
            "Putting back the %s blocks your last edit replaced");
    public static final TextKey NOTHING_TO_UNDO = TextKey.of("jscore.command.edit.nothing_to_undo",
            "You have no edit to undo");
    public static final TextKey STILL_UNDER_WAY = TextKey.of("jscore.command.edit.still_under_way",
            "Your last edit is still under way; undo it once it ends");

    private EditTexts() {
    }
}
