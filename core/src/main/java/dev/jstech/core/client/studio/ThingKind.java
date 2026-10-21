/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The kinds of thing a mod puts in the game, as the studio's tree lists them under each mod. */
@TextHolder
public enum ThingKind {

    MACHINES(TextKey.of("gui.jscore.studio.kind.machines", "Machines")),
    BLOCKS(TextKey.of("gui.jscore.studio.kind.blocks", "Blocks")),
    ITEMS(TextKey.of("gui.jscore.studio.kind.items", "Items")),
    FLUIDS(TextKey.of("gui.jscore.studio.kind.fluids", "Fluids")),
    ENTITIES(TextKey.of("gui.jscore.studio.kind.entities", "Entities")),
    PARTS(TextKey.of("gui.jscore.studio.kind.parts", "Cables and parts")),
    RESEARCH(TextKey.of("gui.jscore.studio.kind.research", "Research"));

    private final TextKey name;

    ThingKind(final TextKey name) {
        this.name = name;
    }

    /** What the kind is called in the tree. */
    public TextKey title() {
        return name;
    }
}
