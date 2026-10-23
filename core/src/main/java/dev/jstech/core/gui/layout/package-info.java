/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Layouts a test can check: the positions of a screen's parts written as plain Java with no game types, so the faults a
 * compiler cannot see (a button over a label, text running past the edge, a slot frame a pixel away from its slot) are
 * caught by a unit test. These classes live outside {@code client} because both sides read them: the menu that places
 * the real slots and the screen that draws their frames read the same layout.
 *
 * <p>A mod writes a layout class for each screen, with its sizes as constants and a {@code layout()} that builds a
 * {@link GuiLayout}: its solid boxes and slots, which may touch but never overlap, its lines of text, checked only
 * against the edges, and the player's inventory. The menu places its slots from {@link GuiLayout#slotAt}, the screen
 * draws their frames from the same calls, and a test asserts {@link GuiLayout#isClean} at the smallest size the screen
 * can have and with the longest text it can show. {@link DesktopZ} keeps the depth of every layer a desktop draws, back
 * to front, and {@link WindowGeometry} the rectangle a desktop window is drawn in, never below its smallest size. The
 * layouts of the Core's own screens (its settings screen, the manual, the notification history, the Sound Mixer, the
 * developer's tools) are here too, each checked by a test of its own.
 *
 * <p>How to write a layout class and its test, from zero, is in {@code core/docs/SCREENS.md}, and how a menu reads it
 * in {@code core/docs/CONTENT.md}. An example that compiles is J's Computers' Printer: its layout
 * ({@code computers/src/main/java/dev/jstech/computers/gui/layout/PrinterLayout.java}), which its menu and its screen
 * both read, and its test ({@code computers/src/test/java/dev/jstech/computers/gui/layout/PrinterLayoutTest.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.gui.layout;

import org.jetbrains.annotations.ApiStatus;
