/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigKey;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A screen a setting is picked on when a word or a number is not enough to show what each value means: a style with
 * its preview, an item with its picture. A setting names its chooser with {@link ConfigKey#choosing}; the settings
 * screen shows the value as the chooser describes it and opens the chooser on a click.
 */
public interface IConfigChooser {

    /** The value as the settings screen shows it on the setting's button. */
    Component describe(ConfigKey<?> key, Object value);

    /**
     * The chooser's screen for {@code key}, going back to {@code parent} when done. What it picks goes into
     * {@code draft}, to be kept or dropped with the settings screen's other changes.
     */
    Screen open(Screen parent, ConfigDraft draft, ConfigKey<?> key);
}
