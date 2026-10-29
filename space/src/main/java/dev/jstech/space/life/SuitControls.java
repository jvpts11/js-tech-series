/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

/**
 * The suit's display and control module, on its chest, as the A7L's: on the HUD while a suit's chest piece is worn,
 * stacked with the hazard devices, drawn as they are, and looked at up close on its key: the suit's pressure on a
 * needle, the time of air left, its state (sealed, oxygen low, holed), its oxygen and carbon dioxide on bars, four
 * warning lamps, and the purge, fan and light buttons. Its light, pressed held up, is kept on the suit.
 */
@TextHolder
public final class SuitControls {

    public static final TextKey SUIT_PSI = TextKey.of("jsspace.suit_controls.suit_psi", "SUIT PSI");
    public static final TextKey LEFT = TextKey.of("jsspace.suit_controls.left", "LEFT");
    public static final TextKey SEALED = TextKey.of("jsspace.suit_controls.sealed", "SUIT SEALED");
    public static final TextKey OXYGEN_LOW = TextKey.of("jsspace.suit_controls.oxygen_low", "O2 LOW");
    public static final TextKey LEAK = TextKey.of("jsspace.suit_controls.leak", "LEAK, PATCH IT");
    public static final TextKey OPEN = TextKey.of("jsspace.suit_controls.open", "SUIT OPEN");
    public static final TextKey OXYGEN = TextKey.of("jsspace.suit_controls.oxygen", "O2");
    public static final TextKey CARBON_DIOXIDE = TextKey.of("jsspace.suit_controls.carbon_dioxide", "CO2");
    public static final TextKey PSI = TextKey.of("jsspace.suit_controls.psi", "PSI");
    public static final TextKey FAN = TextKey.of("jsspace.suit_controls.fan", "FAN");
    public static final TextKey PURGE = TextKey.of("jsspace.suit_controls.purge", "PURGE");
    public static final TextKey LIGHT = TextKey.of("jsspace.suit_controls.light", "LIGHT");
    public static final TextKey KEY = TextKey.of("key.jsspace.suit_controls", "Look at the suit's controls");
    /** Looks at the suit's controls up close. O, until the player changes it. */
    public static final KeyAction LOOK = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "suit_controls"), KEY).key(GLFW.GLFW_KEY_O));

    private SuitControls() {
    }

    /** Declares the key, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Whether the face of the controls on {@code chest} is lit. */
    public static boolean lit(final ItemStack chest) {
        return chest.getOrDefault(SuitData.CONTROLS_LIGHT.get(), false);
    }

    /** Lights the face of the controls of the suit {@code player} wears, or puts it out; whether it wears one. */
    public static boolean light(final Player player, final boolean on) {
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (SuitAir.suitOf(player.level(), chest).isEmpty()) {
            return false;
        }
        chest.set(SuitData.CONTROLS_LIGHT.get(), on);
        return true;
    }
}
