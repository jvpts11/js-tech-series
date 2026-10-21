/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.hud;

import dev.jstech.core.client.config.IConfigChooser;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.gui.HudSettings;
import dev.jstech.core.text.GameText;
import java.util.Map;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The HUD editor as the chooser of where the HUD's elements are, on the settings screen: the setting's row says how
 * many elements the player placed, and its button opens the editor over the game.
 */
public final class HudEditorChooser implements IConfigChooser {

    @Override
    public Component describe(final ConfigKey<?> key, final Object value) {
        final int placed = value instanceof Map<?, ?> map ? map.size() : 0;
        return placed == 0 ? GameText.component(HudTexts.AS_DECLARED)
                : GameText.component(HudTexts.PLACED.with(placed));
    }

    @Override
    public Screen open(final Screen parent, final ConfigDraft draft, final ConfigKey<?> key) {
        return new HudEditorScreen(parent, draft, HudSettings.PLACED, false);
    }
}
