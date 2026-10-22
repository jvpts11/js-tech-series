/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the studio's page of camera paths says. */
@TextHolder
public final class CameraPathTexts {

    public static final TextKey PATHS = TextKey.of("gui.jscore.studio.cameras.paths", "Camera paths");
    public static final TextKey A_NEW_PATH = TextKey.of("gui.jscore.studio.cameras.new", "+ A new path");
    public static final TextKey WRITTEN_HERE = TextKey.of("gui.jscore.studio.cameras.written_here", "written here");
    public static final TextKey A_NEW_ONE = TextKey.of("gui.jscore.studio.cameras.a_new_one", "a new path");
    /** What the name of a new path is written as, in its field before anything is. */
    public static final TextKey NAME_HINT = TextKey.of("gui.jscore.studio.cameras.name_hint", "mod:name");
    public static final TextKey KEYFRAMES = TextKey.of("gui.jscore.studio.cameras.keyframes", "Keyframes");
    public static final TextKey TIME = TextKey.of("gui.jscore.studio.cameras.time", "Time");
    public static final TextKey WHERE = TextKey.of("gui.jscore.studio.cameras.where", "Where");
    public static final TextKey LOOKING = TextKey.of("gui.jscore.studio.cameras.looking", "Looking");
    public static final TextKey FOV = TextKey.of("gui.jscore.studio.cameras.fov", "Fov");
    public static final TextKey ROLL = TextKey.of("gui.jscore.studio.cameras.roll", "Roll");
    public static final TextKey EASING = TextKey.of("gui.jscore.studio.cameras.easing", "Easing");
    /* Seconds. */
    public static final TextKey SECONDS = TextKey.of("gui.jscore.studio.cameras.seconds", "%s s");
    /** A field of view of 0, the player's own. */
    public static final TextKey OWN = TextKey.of("gui.jscore.studio.cameras.own", "own");
    public static final TextKey NONE_YET = TextKey.of("gui.jscore.studio.cameras.none_yet",
            "None yet: fly and record, or add one here");
    public static final TextKey FLY_AND_RECORD = TextKey.of("gui.jscore.studio.cameras.fly", "Fly and record");
    public static final TextKey STOP_RECORDING = TextKey.of("gui.jscore.studio.cameras.stop", "Stop recording");
    public static final TextKey ADD_HERE = TextKey.of("gui.jscore.studio.cameras.add_here", "Add here");
    public static final TextKey PLAY = TextKey.of("gui.jscore.studio.cameras.play", "Play");
    public static final TextKey SAVE_TO_FILE = TextKey.of("gui.jscore.studio.cameras.save", "Save to file");
    public static final TextKey HINT = TextKey.of("gui.jscore.studio.cameras.hint",
            "Each keyframe keeps where you stand and look. Play runs it as a scene; Save writes the file.");
    public static final TextKey EDIT_TIP = TextKey.of("gui.jscore.studio.cameras.edit_tip",
            "Scroll to change a time, a field of view or a roll; click an easing to change it, with shift back; "
                    + "Delete takes the keyframe away");
    public static final TextKey TWO_OR_MORE = TextKey.of("gui.jscore.studio.cameras.two_or_more",
            "A path is two keyframes or more");
    public static final TextKey NAME_FIRST = TextKey.of("gui.jscore.studio.cameras.name_first",
            "Write the path's name first, as mod:name");
    public static final TextKey SEEN_FROM_ABOVE = TextKey.of("gui.jscore.studio.cameras.above", "Seen from above");
    public static final TextKey ORIGIN = TextKey.of("gui.jscore.studio.cameras.origin", "origin");
    /* The key. */
    public static final TextKey RECORDING = TextKey.of("gui.jscore.studio.cameras.recording",
            "Recording: %s keeps where you are as a keyframe");
    /* Which keyframe. */
    public static final TextKey KEPT = TextKey.of("gui.jscore.studio.cameras.kept", "Keyframe %s kept");
    /* The easings: a curve's gentlest kind alone, and the others by the name of their curve. */
    public static final TextKey EASE_IN = TextKey.of("gui.jscore.studio.cameras.ease_in", "in");
    public static final TextKey EASE_OUT = TextKey.of("gui.jscore.studio.cameras.ease_out", "out");
    public static final TextKey EASE_IN_OUT = TextKey.of("gui.jscore.studio.cameras.ease_in_out", "in and out");
    public static final TextKey EASE_IN_OF = TextKey.of("gui.jscore.studio.cameras.ease_in_of", "in %s");
    public static final TextKey EASE_OUT_OF = TextKey.of("gui.jscore.studio.cameras.ease_out_of", "out %s");
    public static final TextKey EASE_IN_OUT_OF = TextKey.of("gui.jscore.studio.cameras.ease_in_out_of",
            "in and out %s");

    private CameraPathTexts() {
    }
}
