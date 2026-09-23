/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui;

/**
 * The colours of one of CDE's schemes: the handful its whole desktop is read from.
 *
 * <p>CDE never had a wallpaper picture and a separate window theme. It had a palette, and the frames, the
 * active title, the Front Panel and the backdrop were all drawn out of it, which is why changing one changed
 * the whole room at once. Everything CDE draws here asks a palette, so the same holds. Which schemes there are,
 * and what the Style Manager calls them, is {@link CdeScheme}.
 *
 * <p>Pure, with no Minecraft types, so how readable each palette is can be unit-tested.
 *
 * @param window    the grey every frame, panel and control is made of
 * @param light     the lit edge of a raised thing, top and left
 * @param shade     its shaded edge, bottom and right
 * @param active    the title of the window in front, and the workspace that is up
 * @param inset     the ground of a sunken well: a list, a file view, a text field
 * @param backdropA the backdrop's first colour
 * @param backdropB the backdrop's second colour, which its pattern is drawn in over the first
 * @param ink       the colour text is written in on the window grey
 * @param activeInk the colour an active title is written in
 */
public record CdePalette(int window, int light, int shade, int active, int inset, int backdropA, int backdropB,
                         int ink, int activeInk) {
}
