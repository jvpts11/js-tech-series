/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Items that hold something besides themselves, and the keys that work them: the modes an item switches between, and
 * the energy, fluid, gas and stacks it keeps in its components, each declared in one line with the item; keys that work
 * an item wherever its player carries it; and shaped recipes a setting turns on and off.
 *
 * <p>An item says what it holds where it is declared, on its {@link ItemBuilder} ({@code modes}, {@code holdsEnergy},
 * {@code holdsFluid}, {@code holdsGas}, {@code holdsItems}), and the Core does the rest through {@link ItemStates}: it
 * keeps each in a component that goes wherever the item goes, gives the item the game's capability for its energy, its
 * fluid and its stacks (an {@link ItemInventory}) or the Core's for its gas (an {@link ItemGasTank}), makes it stack
 * alone, and says what it holds in its tooltip. What a declared item holds is its {@link ItemState}, and each of its
 * modes an {@link ItemMode}, switched by the Core's mode key, by the mouse wheel while sneaking, or on the ring of
 * modes. {@link ItemActions} declares a key that finds the item in the hands, then in what is worn, then in the
 * inventory, and hands it to the action; an item that turns on and off keeps it in {@link ItemStates#ON}. A
 * {@link SwitchedShapedRecipe} is a shaped recipe that matches only while the setting it names is on, so a server takes
 * an item out of survival with no data pack.
 *
 * <p>How to declare one, from zero, is in {@code core/docs/CONTENT.md} and {@code core/docs/TOOLS.md}. Examples that
 * compile are J's Industrial's tank item ({@code industrial/src/main/java/dev/jstech/industrial/item/TankItem.java}),
 * which reads the fluid it carries from the Core's fluid component, and the test mod's items
 * ({@code tests/src/main/java/dev/jstech/tests/TestItems.java}), which hold modes, energy, a fluid and stacks.
 */
@ApiStatus.Experimental
package dev.jstech.core.item;

import dev.jstech.core.content.ItemBuilder;
import org.jetbrains.annotations.ApiStatus;
