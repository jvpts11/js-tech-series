/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the screens declared in files and the chooser of styles say: the styles' names among them. */
@TextHolder
public final class InterfaceTexts {

    /** The names of the styles that come with the Core, as their files name them. */
    public static final TextKey STYLE_GAME = TextKey.of("jscore.style.game", "Game");
    public static final TextKey STYLE_JSTECH = TextKey.of("jscore.style.jstech", "J's Tech");
    public static final TextKey STYLE_HIGH_CONTRAST = TextKey.of("jscore.style.high_contrast", "High contrast");
    public static final TextKey STYLE_DEUTERANOPIA = TextKey.of("jscore.style.deuteranopia",
            "Deuteranopia (red and green)");
    public static final TextKey STYLE_PROTANOPIA = TextKey.of("jscore.style.protanopia", "Protanopia (red and green)");
    public static final TextKey STYLE_TRITANOPIA = TextKey.of("jscore.style.tritanopia",
            "Tritanopia (blue and yellow)");
    public static final TextKey STYLE_ERA_VINTAGE = TextKey.of("jscore.style.era.vintage", "Vintage era");
    public static final TextKey STYLE_ERA_LEGACY = TextKey.of("jscore.style.era.legacy", "Legacy era");
    public static final TextKey STYLE_ERA_TRANSITION = TextKey.of("jscore.style.era.transition", "Transition era");
    public static final TextKey STYLE_ERA_STANDARD = TextKey.of("jscore.style.era.standard", "Standard era");
    public static final TextKey STYLE_ERA_ADVANCED = TextKey.of("jscore.style.era.advanced", "Advanced era");

    /** What a screen whose file is missing shows in its place. */
    public static final TextKey MISSING_SCREEN = TextKey.of("jscore.screen.missing",
            "No resource pack has the screen %s.");

    /** The chooser of styles. */
    public static final TextKey CHOOSER_TITLE = TextKey.of("jscore.style_chooser.title", "Styles");
    public static final TextKey EVERY_SCREEN = TextKey.of("jscore.style_chooser.every_screen", "Every screen");
    public static final TextKey MODS_OWN = TextKey.of("jscore.style_chooser.mods_own", "Each mod's own");
    public static final TextKey FOR_EVERY_SCREEN = TextKey.of("jscore.style_chooser.for_every_screen",
            "The same as every screen");
    public static final TextKey SCREENS_OF = TextKey.of("jscore.style_chooser.screens_of", "%s's screens");
    public static final TextKey PREVIEW = TextKey.of("jscore.style_chooser.preview", "Preview");
    public static final TextKey DONE = TextKey.of("jscore.style_chooser.done", "Done");
    public static final TextKey CHOSEN_FOR_MODS = TextKey.of("jscore.style_chooser.chosen_for_mods",
            "%s for some mods");
    public static final TextKey SAMPLE_TITLE = TextKey.of("jscore.style_chooser.sample_title", "Crusher");
    public static final TextKey SAMPLE_TEXT = TextKey.of("jscore.style_chooser.sample_text", "Working: 42%%");
    public static final TextKey SAMPLE_DIM = TextKey.of("jscore.style_chooser.sample_dim", "Input from the left");
    public static final TextKey SAMPLE_BUTTON = TextKey.of("jscore.style_chooser.sample_button", "Start");
    public static final TextKey SAMPLE_CHECK = TextKey.of("jscore.style_chooser.sample_check", "Keep a stack");

    private InterfaceTexts() {
    }
}
