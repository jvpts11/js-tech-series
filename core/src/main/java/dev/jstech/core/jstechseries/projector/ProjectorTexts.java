/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Structure Projector says: on its screen, above the multiblock it shows, and beside a ghost looked at. */
@TextHolder
public final class ProjectorTexts {

    public static final TextKey TITLE = TextKey.of("jscore.projector.title", "STRUCTURE PROJECTOR");
    public static final TextKey MULTIBLOCK = TextKey.of("jscore.projector.multiblock", "MULTIBLOCK");
    public static final TextKey IN_THE_WORLD = TextKey.of("jscore.projector.in_the_world", "IN THE WORLD");
    public static final TextKey PLACED = TextKey.of("jscore.projector.placed", "PLACED  %s/%s");
    public static final TextKey MISSING = TextKey.of("jscore.projector.missing", "MISSING  %s");
    public static final TextKey WRONG = TextKey.of("jscore.projector.wrong", "WRONG  %s");
    public static final TextKey NOTHING = TextKey.of("jscore.projector.nothing", "Choose a multiblock");
    public static final TextKey NOTHING_MISSING = TextKey.of("jscore.projector.nothing_missing", "Nothing missing");
    public static final TextKey TURN = TextKey.of("jscore.projector.turn", "TURN");
    public static final TextKey NORTH = TextKey.of("jscore.projector.north", "N");
    public static final TextKey EAST = TextKey.of("jscore.projector.east", "E");
    public static final TextKey SOUTH = TextKey.of("jscore.projector.south", "S");
    public static final TextKey WEST = TextKey.of("jscore.projector.west", "W");
    public static final TextKey MIRROR = TextKey.of("jscore.projector.mirror", "MIRROR");
    public static final TextKey MISSING_WORD = TextKey.of("jscore.projector.missing_word", "MISSING");
    public static final TextKey ONLY = TextKey.of("jscore.projector.only", "ONLY");
    public static final TextKey LAYER = TextKey.of("jscore.projector.layer", "LAYER");
    public static final TextKey OF = TextKey.of("jscore.projector.of", "OF %s");
    public static final TextKey ON = TextKey.of("jscore.projector.on", "ON");
    public static final TextKey OFF = TextKey.of("jscore.projector.off", "OFF");
    public static final TextKey SHOWING = TextKey.of("jscore.projector.showing", "SHOWING");
    public static final TextKey LAYER_OF = TextKey.of("jscore.projector.layer_of", "LAYER %s OF %s");
    public static final TextKey DRAG = TextKey.of("jscore.projector.drag", "drag to turn");
    public static final TextKey STATUS = TextKey.of("jscore.projector.status", "%s   %s x %s x %s   %s");
    public static final TextKey SIZE_LINE = TextKey.of("jscore.projector.size_line", "%s, %s x %s x %s");
    public static final TextKey IN_PLACE = TextKey.of("jscore.projector.in_place", "%s of %s in place");
    public static final TextKey MISSING_LINE = TextKey.of("jscore.projector.missing_line", "%s missing: %s");
    public static final TextKey WRONG_LINE = TextKey.of("jscore.projector.wrong_line", "%s wrong: %s for %s");
    public static final TextKey LOOK = TextKey.of("jscore.projector.look",
            "Look at a ghost to see what goes there");
    public static final TextKey GHOST_LAYER = TextKey.of("jscore.projector.ghost_layer", "layer %s of %s");
    public static final TextKey TERMINAL = TextKey.of("jscore.projector.terminal", "The terminal");
    public static final TextKey COUNTED = TextKey.of("jscore.projector.counted", "%s %s");
    public static final TextKey AND_MORE = TextKey.of("jscore.projector.and_more", "%s, %s");

    private ProjectorTexts() {
    }
}
