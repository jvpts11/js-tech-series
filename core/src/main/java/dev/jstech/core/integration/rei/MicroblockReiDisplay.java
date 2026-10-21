/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.rei;

import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCustomDisplay;
import net.minecraft.resources.ResourceLocation;

/**
 * A microblock rule as one of REI's crafting recipes, with the rule's own id: no recipe of the game stands behind it,
 * the rule being worked out by itself, so the id is given here rather than read from a recipe.
 */
final class MicroblockReiDisplay extends DefaultCustomDisplay {

    private final ResourceLocation id;

    MicroblockReiDisplay(final ResourceLocation id, final List<EntryIngredient> grid,
                         final List<EntryIngredient> made) {
        super(id, null, grid, made);
        this.id = id;
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(this.id);
    }
}
