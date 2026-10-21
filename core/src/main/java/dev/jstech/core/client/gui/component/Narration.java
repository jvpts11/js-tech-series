/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.component;

import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What the game's narrator says of the Core's components, in the game's own words for its widgets, so that each is
 * read as a player who uses the narrator already hears the game's buttons, check boxes, sliders and fields, in their
 * language.
 */
public final class Narration {

    /** How each kind is used while it has the keyboard, in the game's words. */
    public static final Component PRESS = Component.translatable("narration.button.usage.focused");
    public static final Component TOGGLE = Component.translatable("narration.checkbox.usage.focused");
    public static final Component SLIDE = Component.translatable("narration.slider.usage.focused");
    public static final Component CHOOSE = Component.translatable("narration.selection.usage");

    private Narration() {
    }

    /** A button called {@code name}: "Start button". */
    public static Component button(final Component name) {
        return Component.translatable("gui.narrate.button", name);
    }

    /** A check box called {@code name}, ticked or not: "Checkbox: Only changed: ON". */
    public static Component checkbox(final Component name, final boolean ticked) {
        return Component.translatable("narration.checkbox", CommonComponents.optionNameValue(name,
                CommonComponents.optionStatus(ticked)));
    }

    /** Something that turns on and off called {@code name}: "Pump: ON button". */
    public static Component toggle(final @Nullable Component name, final boolean on) {
        final Component status = CommonComponents.optionStatus(on);
        return button(name == null ? status : CommonComponents.optionNameValue(name, status));
    }

    /** A slider called {@code name} at {@code value}: "Speed: 40 slider". */
    public static Component slider(final @Nullable Component name, final Component value) {
        return Component.translatable("gui.narrate.slider", name == null ? value
                : CommonComponents.optionNameValue(name, value));
    }

    /** A field called {@code name} holding {@code value}: "Name edit box: Crusher". */
    public static Component field(final @Nullable Component name, final Component value) {
        return Component.translatable("gui.narrate.editBox", name == null ? Component.empty() : name, value);
    }

    /** A choice among some called {@code name}, at {@code chosen}: "Mode: Fast button". */
    public static Component choice(final @Nullable Component name, final Component chosen) {
        return button(name == null ? chosen : CommonComponents.optionNameValue(name, chosen));
    }
}
