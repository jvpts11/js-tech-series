/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.TextKey;

/** The states a machine on the studio's bench is shown in, each as what its block entity is told it is doing. */
public enum BenchState {

    WORKING(StudioTexts.WORKING, "{Status:\"working\",Running:1b}"),
    IDLE(StudioTexts.IDLE, "{Status:\"waiting\",Running:0b}"),
    JAMMED(StudioTexts.JAMMED, "{Status:\"output_full\",Running:0b}");

    private final TextKey name;
    private final String data;

    BenchState(final TextKey name, final String data) {
        this.name = name;
        this.data = data;
    }

    /** What its button says. */
    public TextKey title() {
        return this.name;
    }

    /** What the machine's block entity is told, as the game writes a compound tag. */
    public String data() {
        return this.data;
    }
}
