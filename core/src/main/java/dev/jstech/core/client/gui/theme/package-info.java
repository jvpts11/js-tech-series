/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * The era themes the series' block screens paint through, one for each hardware era: a palette of colours a resource
 * pack can change, and a style of square overlays the era draws (scanlines on a Vintage screen, a double bevel on a
 * Legacy one, glass bands, an accent line under a header), so the same screen code renders in the look of whatever era
 * its machine is. Square corners only, in every era. These classes load only on a player's game, never on a dedicated
 * server.
 *
 * <p>A screen takes its era's theme from {@link EraThemes#of}, or {@link EraThemes#ofNullable}, which gives the
 * Standard theme when there is no era. It binds the theme with {@link JsTechTheme#bind} at the start of its render,
 * draws through the {@link JsTechTheme} helpers (its window, its slots, its panels, its text) and unbinds it in a
 * {@code finally}; a style the player chose for J's Computers' screens, or for every screen, is worn in the era's
 * place. A theme is an {@link EraTheme}: an {@link EraPalette} asked for each time something is painted, never kept, so
 * a resource pack's colours reach an open screen, and an {@link EraStyle} saying which overlays it draws. Each era's
 * colours are the declared palette {@code jscore:era/<era>}.
 *
 * <p>How a screen binds a theme and paints through it is in {@code core/docs/SCREENS.md}. An example that compiles is
 * J's Computers' computer screens, which bind the theme of the computer's era
 * ({@code computers/src/main/java/dev/jstech/computers/client/AbstractComputerScreen.java}).
 */
@ApiStatus.Experimental
package dev.jstech.core.client.gui.theme;

import org.jetbrains.annotations.ApiStatus;
