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
import dev.jstech.core.camera.CameraTexts;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.item.ItemTexts;
import dev.jstech.core.moving.ForceDiagramTexts;
import dev.jstech.core.notification.NotificationTexts;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * The Core's own keys: turning off the last sound heard, which the player's game does alone, changing the mode of
 * the item in the main hand, which the server does, opening an item's page in the manual, opening the history of
 * notifications, opening the diagram of forces of a moving structure, showing the physics of the structures near,
 * looking about freely, and opening the HUD editor. The first two and the last three have no key until the player
 * gives them one; the manual's is M, which every manual's tooltip names, the history's N, and the forces' K.
 */
@TextHolder
public final class CoreKeys {

    /** The heading the series' keys are listed under in the game's controls. */
    public static final TextKey CATEGORY = TextKey.of("key.categories.jstech", "J's Tech Series");

    public static final KeyAction TURN_OFF_LAST_SOUND = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "turn_off_last_sound"),
            AudioTexts.TURN_OFF_LAST_SOUND));

    /** Shows, in the world, the rays sound is heard by and a line to every sound playing; no key until given one. */
    public static final KeyAction SHOW_SOUND_DEBUG = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "show_sound_debug"), AudioTexts.SHOW_SOUND_DEBUG));

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

    /** Opens the history of notifications. N, until the player changes it. */
    public static final KeyAction NOTIFICATIONS = KeyActions.declare(KeyAction.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "notifications"),
                    NotificationTexts.HISTORY_KEY)
            .key(GLFW.GLFW_KEY_N));

    /**
     * Opens the diagram of forces of the moving structure the player stands on, sits in or looks at, and, held over a
     * block's tooltip, shows its physics. K, until the player changes it.
     */
    public static final KeyAction STRUCTURE_FORCES = KeyActions.declare(KeyAction.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_forces"), ForceDiagramTexts.KEY)
            .key(GLFW.GLFW_KEY_K));

    /** Shows, in the world, the physics of the moving structures near: no key until given one. */
    public static final KeyAction SHOW_STRUCTURE_DEBUG = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "show_structure_debug"),
            ForceDiagramTexts.DEBUG_KEY));

    /**
     * Held, turns the view without turning the player, as a head turns in a cockpit while the vehicle keeps its course;
     * let go, the view comes back. No key until given one.
     */
    public static final KeyAction FREE_LOOK = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "free_look"), CameraTexts.FREE_LOOK_KEY));

    /** What the key that opens the HUD editor is called. */
    public static final TextKey EDIT_HUD_KEY = TextKey.of("key.jscore.edit_hud", "Edit the HUD");
    /**
     * Opens the HUD editor over the game, which the settings screen also opens; no key until given one, since the
     * player seldom needs it.
     */
    public static final KeyAction EDIT_HUD = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "edit_hud"), EDIT_HUD_KEY));

    private CoreKeys() {
    }

    /** Declares the keys, before the player's game makes its key bindings. */
    public static void declare() {
        // Loading the class declares them.
    }
}
