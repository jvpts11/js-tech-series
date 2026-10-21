/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Menus declared once: their slots in named groups placed from the screen's layout, where a shift-click in each group
 * sends the stack, the buttons they answer, the values their screen shows, and while they stay open. The base does the
 * rest: the shift-click, the buttons, the validity check and the sending of the values.
 *
 * <p>A menu extends {@link CoreMenu}. Its slots are added in groups, each a {@link SlotGroup}, the player's inventory
 * as {@link PlayerSlots}, and a route from one group to others says where a shift-click in it sends a stack. A number
 * the server works out is a {@link MenuValue}; a value of any kind a codec writes, a name, a list or a record of the
 * machine's state, is a {@link SyncedValue}, bound by its name to the components of a screen's file, and all of them
 * travel in one payload of the Core's ({@link MenuSync}), so a mod writes none. {@link MenuValidity} gives the tests of
 * while a menu stays open: a menu opened on a block stays open while that very block entity stands there and the player
 * can reach it, which holds for every variant of the block that makes the same block entity. {@link MenuOpening} finds,
 * on the client, the block entity a menu was opened on.
 *
 * <p>How to declare one, from zero, is in {@code core/docs/CONTENT.md}, and how a screen's file binds to its values in
 * {@code core/docs/SCREENS.md}. An example that compiles is J's Example's logbook
 * ({@code example/src/main/java/dev/jstech/example/screen/LogbookMenu.java}): the energy made on the server and how
 * many times its player opened it, both worked out by the server, and a button that starts the count over.
 */
@ApiStatus.Experimental
package dev.jstech.core.menu;

import org.jetbrains.annotations.ApiStatus;
