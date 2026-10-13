/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

/**
 * The colours one skin gives its windows, over the chrome its form draws: the ground, the text, the accent and what
 * a list picks out. Two skins of one form, KDE and GNOME on the flat form, differ here and nowhere else.
 *
 * @param titleText      what a window's title is written in
 * @param windowBg       a window's ground
 * @param windowBorder   a window's border
 * @param accent         selections, marks, the primary button, progress
 * @param text           primary text
 * @param dim            secondary text: captions, hints, placeholders
 * @param fieldBg        the ground of a text field
 * @param listSelect     the ground of a selected row
 * @param listSelectText what a selected row is written in
 * @param listHover      the ground of a hovered row
 */
public record SkinColours(int titleText, int windowBg, int windowBorder, int accent, int text, int dim, int fieldBg,
                          int listSelect, int listSelectText, int listHover) {
}
