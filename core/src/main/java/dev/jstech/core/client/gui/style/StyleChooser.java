/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.style;

import dev.jstech.core.client.config.IConfigChooser;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.gui.screen.InterfaceTexts;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.text.GameText;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The chooser of the player's styles, for both of their settings: the style for everything and those by mod. */
public final class StyleChooser implements IConfigChooser {

    @Override
    public Component describe(final ConfigKey<?> key, final Object value) {
        if (value instanceof Map<?, ?> byMod) {
            return byMod.isEmpty() ? GameText.component(InterfaceTexts.FOR_EVERY_SCREEN)
                    : GameText.component(InterfaceTexts.CHOSEN_FOR_MODS.with(byMod.size()));
        }
        final String id = String.valueOf(value);
        if (id.isBlank()) {
            return GameText.component(InterfaceTexts.MODS_OWN);
        }
        return Optional.ofNullable(ResourceLocation.tryParse(id)).flatMap(Styles::get).map(Style::nameKey)
                .<Component>map(Component::translatable).orElseGet(() -> Component.literal(id));
    }

    @Override
    public Screen open(final Screen parent, final ConfigDraft draft, final ConfigKey<?> key) {
        return new StyleChooserScreen(parent, draft);
    }
}
