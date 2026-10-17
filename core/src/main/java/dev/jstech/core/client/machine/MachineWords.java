/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Locale;

/**
 * The words the machine screen engraves for faces and what they do, in capitals as the steel is lettered; a machine
 * with a screen of its own letters its steel with {@link #upper} too.
 */
public final class MachineWords {

    private MachineWords() {
    }

    /** {@code text} as the steel is lettered. */
    public static String upper(final Text text) {
        return GameText.resolve(text).toUpperCase(Locale.ROOT);
    }

    /** The text of {@code key} as the steel is lettered. */
    public static String upper(final TextKey key) {
        return upper(key.text());
    }

    /** A face's name. */
    static TextKey face(final RelativeFace face) {
        return switch (face) {
            case FRONT -> MachineScreenTexts.FRONT;
            case BACK -> MachineScreenTexts.BACK;
            case LEFT -> MachineScreenTexts.LEFT;
            case RIGHT -> MachineScreenTexts.RIGHT;
            case TOP -> MachineScreenTexts.TOP;
            case BOTTOM -> MachineScreenTexts.BOTTOM;
        };
    }

    /** What a face does, in a word. */
    static TextKey mode(final FaceMode mode) {
        return switch (mode) {
            case IN -> MachineScreenTexts.MODE_IN;
            case OUT -> MachineScreenTexts.MODE_OUT;
            case BOTH -> MachineScreenTexts.MODE_BOTH;
            case NONE -> MachineScreenTexts.MODE_OFF;
        };
    }

    /** The colour of what a face does. */
    static int colour(final FaceMode mode) {
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        return switch (mode) {
            case IN -> c.faceIn();
            case OUT -> c.faceOut();
            case BOTH -> c.faceBoth();
            case NONE -> c.faceOff();
        };
    }
}
