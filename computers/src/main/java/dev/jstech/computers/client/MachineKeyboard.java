/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Gives a machine's screen its keys before anything else in the game sees them.
 *
 * <p>A key pressed at a screen is offered to every mod first and to the screen last, so a mod that binds
 * Control and a letter to something of its own takes that key away from whatever is on the glass: an editor
 * whose Write Out is Control and O never hears it when a recipe viewer uses the same two keys to hide itself.
 * Somebody sitting at a computer is typing at the computer. So the screen is asked first, and only a key it
 * had no use for goes on to everybody else, which is what keeps another mod's keys working over the parts of
 * a desktop that are not taking text.
 */
public final class MachineKeyboard {

    private MachineKeyboard() {
    }

    /** Hands the game's bus what this answers, first of all, once, from the mod's client constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, ScreenEvent.KeyPressed.Pre.class,
                MachineKeyboard::pressed);
    }

    /** A screen that is a machine's glass, and is asked about every key before the rest of the game is. */
    public interface ITakesKeysFirst {

        /**
         * Takes the key if whatever is being typed at has a use for it.
         *
         * @return false to leave the key to the rest of the game, after which the screen is offered it again
         *         in the ordinary way, so a screen that answers false must not have done anything with it
         */
        boolean keyFirst(int key, int scanCode, int modifiers);
    }

    public static void pressed(final ScreenEvent.KeyPressed.Pre event) {
        if (event.getScreen() instanceof ITakesKeysFirst glass
                && glass.keyFirst(event.getKeyCode(), event.getScanCode(), event.getModifiers())) {
            // Used, so it ends here: nothing else hears it, and the game does not offer it to the screen again.
            event.setCanceled(true);
        }
    }
}
