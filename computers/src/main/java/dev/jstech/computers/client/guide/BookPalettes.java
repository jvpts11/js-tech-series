/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.guide;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.client.guide.GuidePalettes;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours of the books of the systems and the languages, each in the manner of the manuals of its systems' time:
 * white pages and Midsoft blue for the Frames Resource Kit, graphite and terminal green for the Linux Desk Reference,
 * kraft card and graph paper for Linux by Hand, red cloth and cream pages for the Daemon Handbook, purple for Learning
 * IQL, and black on white with a blue for the Book of Sigma. The band is each book's second colour, which every other
 * block of its thumb index is printed in; listings keep the colours the game's editors give code.
 */
@PaletteHolder
public final class BookPalettes {

    public static final Palette<GuidePalettes.Binder> FRAMES_RESOURCE_KIT = Palettes.declare(JsComputers.MODID,
            "guide/frames_resource_kit", new GuidePalettes.Binder(
                    0xFF1F4E9A, 0xFF2C63B8, 0xFFFBFBF7, 0xFFE9E9E2, 0xFF1C1F26, 0xFF69707C, 0xFF1F4E9A, 0xFF1F4E9A,
                    0xFFB9C2D0, 0xFFDFE6F2, 0xFFF3E6C8, 0xFF3A3D44, 0xFFFBFBF7, 0xFF1C1F26, 0xFF1F4E9A, 0xFF1C1F26,
                    0xFF8A4F12, 0xFF1F4E9A, 0xFF1C1F26, 0xFF3B78D1, 0xFFDFE6F2, 0xFF16376C, 0xFF1C1F26,
                    0xFFF0F3F5, 0xFFDFE6F2, 0xFF1F3F8A, 0xFF0F6E66, 0xFF9A3B16, 0xFF7A2D8A, 0xFF69707C, 0xFFCFE8CF,
                    0xFFE8B0B0, 0xFFC49A3A));

    public static final Palette<GuidePalettes.Binder> LINUX_DESK_REFERENCE = Palettes.declare(JsComputers.MODID,
            "guide/linux_desk_reference", new GuidePalettes.Binder(
                    0xFF2B2F36, 0xFF3D434D, 0xFFF4F4EF, 0xFFE3E3DB, 0xFF1F2328, 0xFF6B7075, 0xFF2E7D4F, 0xFF2E7D4F,
                    0xFFB8BCB4, 0xFFE0E6DC, 0xFFF3E6C8, 0xFF3A3D44, 0xFFF4F4EF, 0xFF1F2328, 0xFF2E7D4F, 0xFF1F2328,
                    0xFF8A4F12, 0xFF2E7D4F, 0xFF1F2328, 0xFF3F9A66, 0xFFE0E6DC, 0xFF1E2126, 0xFF1F2328,
                    0xFFECEEE9, 0xFFE0E6DC, 0xFF1F3F8A, 0xFF0F6E66, 0xFF9A3B16, 0xFF7A2D8A, 0xFF6B7075, 0xFFCFE8CF,
                    0xFFE8B0B0, 0xFFC49A3A));

    /**
     * A lab notebook's: what can go wrong is written on a yellow sticky note, and the buttons are a lighter kraft, so
     * their marks still read on them while they are pressed.
     */
    public static final Palette<GuidePalettes.Binder> LINUX_BY_HAND = Palettes.declare(JsComputers.MODID,
            "guide/linux_by_hand", new GuidePalettes.Binder(
                    0xFF9C7448, 0xFFC69C6A, 0xFFF7F4EA, 0xFFEBE6D6, 0xFF232836, 0xFF6C6A62, 0xFF1D3A73, 0xFF1D3A73,
                    0xFFB9B3A2, 0xFFE6E1D0, 0xFFFFF6B8, 0xFF3A3D44, 0xFFF7F4EA, 0xFF232836, 0xFF1D3A73, 0xFF232836,
                    0xFF8A4F12, 0xFF1D3A73, 0xFF232836, 0xFF3A5A9A, 0xFFE4EBF2, 0xFF6D5132, 0xFF232836,
                    0xFFF0ECE0, 0xFFE6E1D0, 0xFF1F3F8A, 0xFF0F6E66, 0xFF9A3B16, 0xFF7A2D8A, 0xFF6C6A62, 0xFFCFE8CF,
                    0xFFE8B0B0, 0xFFC49A3A));

    /** A hardback's: what can go wrong stands on a deeper cream than the pages, so it shows on them. */
    public static final Palette<GuidePalettes.Binder> DAEMON_HANDBOOK = Palettes.declare(JsComputers.MODID,
            "guide/daemon_handbook", new GuidePalettes.Binder(
                    0xFF7A1818, 0xFF952424, 0xFFF1E9D2, 0xFFE2D7B9, 0xFF2A2220, 0xFF75695A, 0xFF8E1D1D, 0xFF8E1D1D,
                    0xFFC4B593, 0xFFE6DBBD, 0xFFEAD6AA, 0xFF3A3D44, 0xFFF1E9D2, 0xFF2A2220, 0xFF8E1D1D, 0xFF2A2220,
                    0xFF8A4F12, 0xFF8E1D1D, 0xFF2A2220, 0xFFB23A2A, 0xFFE6DBBD, 0xFF551111, 0xFF2A2220,
                    0xFFEDE3CA, 0xFFE6DBBD, 0xFF1F3F8A, 0xFF0F6E66, 0xFF9A3B16, 0xFF7A2D8A, 0xFF75695A, 0xFFCFE8CF,
                    0xFFE8B0B0, 0xFFC49A3A));

    public static final Palette<GuidePalettes.Binder> LEARNING_IQL = Palettes.declare(JsComputers.MODID,
            "guide/learning_iql", new GuidePalettes.Binder(
                    0xFF3F2466, 0xFF55337F, 0xFFFCFBFD, 0xFFEBE7F0, 0xFF21202A, 0xFF6D6878, 0xFF4B2A7A, 0xFF4B2A7A,
                    0xFFC3B8D4, 0xFFE9E2F3, 0xFFF3E6C8, 0xFF3A3D44, 0xFFFCFBFD, 0xFF21202A, 0xFF4B2A7A, 0xFF21202A,
                    0xFF8A4F12, 0xFF4B2A7A, 0xFF21202A, 0xFF7046A8, 0xFFE9E2F3, 0xFF2C1947, 0xFF21202A,
                    0xFFF3EFF9, 0xFFDDD2EC, 0xFF1F3F8A, 0xFF0F6E66, 0xFF9A3B16, 0xFF7A2D8A, 0xFF6D6878, 0xFFCFE8CF,
                    0xFFE8B0B0, 0xFFC49A3A));

    public static final Palette<GuidePalettes.Binder> BOOK_OF_SIGMA = Palettes.declare(JsComputers.MODID,
            "guide/book_of_sigma", new GuidePalettes.Binder(
                    0xFFE9E7E0, 0xFFD9D6CC, 0xFFFDFDFB, 0xFFECECE6, 0xFF141518, 0xFF666A70, 0xFF1D4FA3, 0xFF1D4FA3,
                    0xFFC3C6CC, 0xFFE8EDF6, 0xFFF3E6C8, 0xFF3A3D44, 0xFFFDFDFB, 0xFF141518, 0xFF1D4FA3, 0xFF141518,
                    0xFF8A4F12, 0xFF1D4FA3, 0xFF141518, 0xFF3A6FC4, 0xFFE8EDF6, 0xFFA3A29C, 0xFF141518,
                    0xFFF3F5F9, 0xFFDDE3EE, 0xFF1F3F8A, 0xFF0F6E66, 0xFF9A3B16, 0xFF7A2D8A, 0xFF666A70, 0xFFCFE8CF,
                    0xFFE8B0B0, 0xFFC49A3A));

    private BookPalettes() {
    }
}
