/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

/**
 * One command a machine can run, as its help programs list it: the heading it is filed under, its name and its one
 * line, all in the player's language.
 */
public record HelpCommand(String group, String name, String summary) {
}
