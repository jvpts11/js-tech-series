/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multipart;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What a microblock is called after the block it is cut from: "Oak Planks Cover", "Stone Post (3/8)". */
@TextHolder
public final class MicroblockTexts {

    public static final TextKey COVER = TextKey.of("jscore.microblock.cover", "%s Cover");
    public static final TextKey PANEL = TextKey.of("jscore.microblock.panel", "%s Panel");
    public static final TextKey SLAB = TextKey.of("jscore.microblock.slab", "%s Slab");
    public static final TextKey FACE = TextKey.of("jscore.microblock.face", "%s Piece (%s/8)");
    public static final TextKey HOLLOW_COVER = TextKey.of("jscore.microblock.hollow_cover", "Hollow %s Cover");
    public static final TextKey HOLLOW_PANEL = TextKey.of("jscore.microblock.hollow_panel", "Hollow %s Panel");
    public static final TextKey HOLLOW_SLAB = TextKey.of("jscore.microblock.hollow_slab", "Hollow %s Slab");
    public static final TextKey HOLLOW = TextKey.of("jscore.microblock.hollow", "Hollow %s Piece (%s/8)");
    public static final TextKey FACADE = TextKey.of("jscore.microblock.facade", "%s Facade");
    public static final TextKey POST = TextKey.of("jscore.microblock.post", "%s Post (%s/8)");
    public static final TextKey CORNER = TextKey.of("jscore.microblock.corner", "%s Corner (%s/8)");
    public static final TextKey MICROBLOCK = TextKey.of("jscore.microblock.any", "Microblock");

    private MicroblockTexts() {
    }

    /** The key a microblock of {@code shape}, {@code size} eighths thick, is named by. */
    public static TextKey nameOf(final MicroShape shape, final int size) {
        return switch (shape) {
            case FACE -> size == MicroShape.COVER ? COVER : size == MicroShape.PANEL ? PANEL
                    : size == MicroShape.SLAB ? SLAB : FACE;
            case HOLLOW -> size == MicroShape.COVER ? HOLLOW_COVER : size == MicroShape.PANEL ? HOLLOW_PANEL
                    : size == MicroShape.SLAB ? HOLLOW_SLAB : HOLLOW;
            case FACADE -> FACADE;
            case POST -> POST;
            case CORNER -> CORNER;
        };
    }
}
