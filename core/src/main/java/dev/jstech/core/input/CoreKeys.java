/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.input;

import dev.jstech.core.JsCore;
import dev.jstech.core.audio.AudioTexts;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.item.ItemTexts;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * The Core's own keys: turning off the last sound heard, which the player's game does alone, changing the mode of
 * the item in the main hand, which the server does, and opening an item's page in the manual. The first two have no
 * key until the player gives them one; the manual's is M, which every manual's tooltip names.
 */
@TextHolder
public final class CoreKeys {

    /** The heading the series' keys are listed under in the game's controls. */
    public static final TextKey CATEGORY = TextKey.of("key.categories.jstech", "J's Tech Series");

    public static final KeyAction TURN_OFF_LAST_SOUND = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "turn_off_last_sound"),
            AudioTexts.TURN_OFF_LAST_SOUND));

    public static final KeyAction CHANGE_ITEM_MODE = KeyActions.declare(KeyAction.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "item_mode"), ItemTexts.MODE_KEY)
            .onServer(ItemStates::cycleHeld));

    /**
     * Opens the manual at the page of the item under the pointer, or of the item in the main hand, held a moment so a
     * stray press does nothing. M, as the manuals' tooltips say, until the player changes it.
     */
    public static final KeyAction OPEN_IN_MANUAL = KeyActions.declare(KeyAction.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "open_in_manual"), GuideTexts.OPEN_KEY)
            .key(GLFW.GLFW_KEY_M));

    private CoreKeys() {
    }

    /** Declares the keys, before the player's game makes its key bindings. */
    public static void declare() {
        // Loading the class declares them.
    }
}
