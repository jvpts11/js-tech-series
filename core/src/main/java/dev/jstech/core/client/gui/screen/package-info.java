/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The base screens for a mod that draws a screen in Java: one on a menu with slots, one with no menu, one of tabs, and
 * a dialog that asks to confirm. A screen declared in a file needs none of them written by hand; its screen is made for
 * it ({@code dev.jstech.core.client.gui.data}). These classes load only on a player's game, never on a dedicated
 * server.
 *
 * <p>A screen on a menu extends {@link CoreContainerScreen}: it draws the tooltips over everything, frames the menu's
 * own slots where the menu placed them, tests the pointer against rectangles in the screen's own space and presses the
 * menu's buttons, so no screen writes those again. A screen with no menu extends {@link CoreScreen} and draws in its
 * {@code renderContent}; it does not pause a game played alone, so a running machine keeps ticking while it is open. A
 * {@link TabbedScreen} hosts a row of {@link IGuiTab}s over the room they draw in, each told when it is chosen and when
 * another takes over, and a {@link ModalDialog} asks Confirm or Cancel over the screen it was opened from and hands
 * back the answer, Escape being Cancel. Their colours are a palette a resource pack can change,
 * {@code jscore:gui/screens}.
 *
 * <p>An example that compiles is J's Computers' Printer screen, a {@link CoreContainerScreen} on its menu that draws
 * from the layout its menu places the slots from
 * ({@code computers/src/main/java/dev/jstech/computers/client/PrinterScreen.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.client.gui.screen;

import org.jetbrains.annotations.ApiStatus;
