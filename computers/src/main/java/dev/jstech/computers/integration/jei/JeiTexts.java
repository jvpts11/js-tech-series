/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.jei;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the recipe viewer's transfer buttons tell a player, and the titles of its pages of a wafer's steps. Kept apart
 * from the handlers and the categories and free of the viewer's types, so the language generator can read it whether
 * or not the viewer is installed.
 */
@TextHolder
final class JeiTexts {

    static final TextKey OPEN_STUDIO =
            TextKey.of("jsc.jei.open_studio", "Open the Pattern Studio to transfer recipes");
    static final TextKey LITHOGRAPHY = TextKey.of("jsc.jei.wafer.lithography", "Lithography");
    static final TextKey ETCH = TextKey.of("jsc.jei.wafer.etch", "Plasma Etching");
    static final TextKey IMPLANT = TextKey.of("jsc.jei.wafer.implant", "Ion Implantation");
    static final TextKey DEPOSITION = TextKey.of("jsc.jei.wafer.deposition", "Deposition");
    static final TextKey PACKAGING = TextKey.of("jsc.jei.wafer.packaging", "Packaging");
    static final TextKey SECONDS = TextKey.of("jsc.jei.wafer.seconds", "%s S");

    private JeiTexts() {
    }
}
