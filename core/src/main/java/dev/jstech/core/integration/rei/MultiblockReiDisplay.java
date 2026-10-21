/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.rei;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.recipeview.MultiblockEntry;
import dev.jstech.core.multiblock.MultiblockViewLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * One multiblock in REI, as JEI and EMI show it: its materials and its choice as what it takes, its controller as what
 * it makes, so looking up a block finds the multiblocks built of it.
 */
final class MultiblockReiDisplay implements Display {

    /** The multiblocks' category, the same id as JEI's and EMI's. */
    static final CategoryIdentifier<MultiblockReiDisplay> CATEGORY = CategoryIdentifier.of(JsCore.MODID,
            "multiblocks");

    private final MultiblockEntry entry;
    private final List<EntryIngredient> materials = new ArrayList<>();
    private final List<EntryIngredient> choices = new ArrayList<>();

    MultiblockReiDisplay(final MultiblockEntry entry) {
        this.entry = entry;
        for (final MultiblockEntry.Material material : entry.materials()) {
            this.materials.add(EntryIngredients.ofItemStacks(material.stacks().stream()
                    .map(stack -> stack.copyWithCount(material.count())).toList()));
        }
        entry.choice().ifPresent(choice -> {
            final List<ItemStack> stacks = choice.stacks();
            for (int index = 0; index < Math.min(stacks.size(), MultiblockViewLayout.MOST_CHOICES); index++) {
                // The last slot takes the rest of the choice in turn, so no block that will do goes unshown.
                this.choices.add(EntryIngredients.ofItemStacks(index == MultiblockViewLayout.MOST_CHOICES - 1
                        ? stacks.subList(index, stacks.size()) : List.of(stacks.get(index))));
            }
        });
    }

    /** The multiblock it shows. */
    MultiblockEntry entry() {
        return this.entry;
    }

    /** Its materials, one slot each. */
    List<EntryIngredient> materials() {
        return this.materials;
    }

    /** Its choice of blocks, a slot each. */
    List<EntryIngredient> choices() {
        return this.choices;
    }

    @Override
    public List<EntryIngredient> getInputEntries() {
        final List<EntryIngredient> out = new ArrayList<>(this.materials);
        out.addAll(this.choices);
        return out;
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return this.entry.controller().isEmpty() ? List.of() : List.of(EntryIngredients.of(this.entry.controller()));
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(this.entry.id());
    }
}
