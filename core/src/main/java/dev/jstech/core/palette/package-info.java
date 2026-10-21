/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * Palettes: the colours a screen or a drawing paints with, each declared once in code as a record whose components are
 * its roles, and written to a file a resource pack can replace, so a pack recolours what a mod draws without touching
 * the mod.
 *
 * <p>A palette is declared with {@link Palettes#declare}, as a {@code static final} field of a class marked
 * {@link PaletteHolder}, under the mod's id and a path that says what it colours. What it gives back is a
 * {@link Palette}, read with {@link Palette#get} each time something paints and never kept in a field, so a change of
 * pack reaches every screen at once, an open one included. The data generation writes each palette's file under
 * {@code palettes/}, a line to a role as {@code #AARRGGBB}; a role a pack's file leaves out keeps its declared colour.
 * {@link PaletteRoles} reads a record as its roles and writes and reads its colours; {@link Palettes#derive} keeps
 * colours worked out from a palette until the palette changes; {@link PaletteHolders} loads the classes that declare
 * palettes, so every palette is known before it is written out or read back. A palette holder is a client class, never
 * loaded on a dedicated server.
 *
 * <p>How to declare one, from zero, is in {@code core/docs/TEXT_AND_COLOUR.md}. An example that compiles is J's
 * Industrial's machine screens
 * ({@code industrial/src/main/java/dev/jstech/industrial/client/MachineScreenSupport.java}), whose palette
 * {@code jsindustrial:machine/screen} a resource pack can recolour.
 */
@ApiStatus.Experimental
package dev.jstech.core.palette;

import org.jetbrains.annotations.ApiStatus;
