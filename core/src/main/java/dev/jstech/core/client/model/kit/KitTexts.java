/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the preview of a model. */
@TextHolder
final class KitTexts {

    /* The title, with the model's name: "Model preview: jsc:mainframe". */
    static final TextKey TITLE = TextKey.of("gui.jscore.model_preview.title", "Model preview: %s");
    /* When the models were last read: "reloaded 2 s ago". */
    static final TextKey RELOADED = TextKey.of("gui.jscore.model_preview.reloaded", "reloaded %s s ago");
    static final TextKey ANIMATIONS = TextKey.of("gui.jscore.model_preview.animations", "Animations");
    static final TextKey NO_ANIMATIONS = TextKey.of("gui.jscore.model_preview.no_animations",
            "This model has no animations");
    static final TextKey BONES = TextKey.of("gui.jscore.model_preview.bones", "Bones");
    static final TextKey LOCATORS = TextKey.of("gui.jscore.model_preview.locators", "Locators");
    static final TextKey GLOW = TextKey.of("gui.jscore.model_preview.glow", "Glow");
    static final TextKey WIRE = TextKey.of("gui.jscore.model_preview.wire", "Wire");
    static final TextKey HINT = TextKey.of("gui.jscore.model_preview.hint",
            "Drag: turn   Wheel: zoom   Shift: pan");
    /* An animation's button, to play it, with its name and how it ends: "> idle (loop)". */
    static final TextKey PLAY = TextKey.of("gui.jscore.model_preview.play", "> %s (%s)");
    /* The button of the animation that plays, to pause it: "|| working (loop)". */
    static final TextKey PAUSE = TextKey.of("gui.jscore.model_preview.pause", "|| %s (%s)");
    static final TextKey LOOP = TextKey.of("gui.jscore.model_preview.loop", "loop");
    static final TextKey ONCE = TextKey.of("gui.jscore.model_preview.once", "once");
    static final TextKey HOLD = TextKey.of("gui.jscore.model_preview.hold", "hold");
    /* Where the timeline is: "0.30 s of 1.20 s". */
    static final TextKey TIME = TextKey.of("gui.jscore.model_preview.time", "%s s of %s s");
    /* How fast it plays: "Speed: x1.0". */
    static final TextKey SPEED = TextKey.of("gui.jscore.model_preview.speed", "Speed: x%s");
    static final TextKey NOTHING_PLAYS = TextKey.of("gui.jscore.model_preview.nothing_plays", "at rest");
    /* A model whose file does not read: "This model does not read: the file is not there". */
    static final TextKey BROKEN = TextKey.of("gui.jscore.model_preview.broken", "This model does not read: %s");

    private KitTexts() {
    }
}
