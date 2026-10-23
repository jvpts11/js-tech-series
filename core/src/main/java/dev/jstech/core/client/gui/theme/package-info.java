/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */

/**
 * What a screen is painted with, kept apart from any one look: an {@link EraPalette}, the colours of a window, its
 * panels, slots, text and the overlays; an {@link EraStyle}, which square overlays it draws (scanlines, a double bevel,
 * glass bands, an accent line under a header) and the scale of its small text; and an {@link EraTheme}, the two
 * together with the helpers a screen paints its window, slots, panels and text through. The palette is asked for each
 * time something is painted, never kept, so a resource pack's colours reach an open screen. These classes load only
 * on a player's game, never on a dedicated server.
 *
 * <p>A style declared in a file becomes a theme through {@code Styles.eraTheme}, so a screen painted through a theme
 * wears whatever style the player chose. The series' looks, one for each hardware era, are themes made of these
 * ({@code core/src/main/java/dev/jstech/core/jstechseries/theme/client/EraThemes.java}). How a screen binds a theme and
 * paints through it is in {@code core/docs/SCREENS.md}.
 */
@ApiStatus.Experimental
package dev.jstech.core.client.gui.theme;

import org.jetbrains.annotations.ApiStatus;
