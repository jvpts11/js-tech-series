/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

/**
 * The colours of a desktop's own chrome, the parts that are not a program's window: its panel, its launcher and
 * the captions of its icons.
 *
 * @param taskbar     the panel along the edge of the screen
 * @param taskbarEdge the line that parts the panel from the desktop
 * @param startButton the launcher button, and what a Linux launcher marks its lit entry with
 * @param startText   what is written on the panel
 * @param taskButton  a task's button on the panel
 * @param iconText    a desktop icon's caption
 * @param menuBg      the launcher menu's ground
 * @param menuText    what the launcher menu is written in
 * @param titleActive the band down the side of the launcher menu, and its lit entry
 */
public record DesktopColours(
        int taskbar, int taskbarEdge, int startButton, int startText, int taskButton,
        int iconText, int menuBg, int menuText, int titleActive) {
}
