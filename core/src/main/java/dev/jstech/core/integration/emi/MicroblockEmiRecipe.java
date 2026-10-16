/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.emi;

import dev.emi.emi.api.recipe.EmiPatternCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.emi.emi.api.widget.SlotWidget;
import dev.jstech.core.JsCore;
import dev.jstech.core.multipart.CoreSpaces;
import dev.jstech.core.multipart.MicroblockShowcase;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One microblock rule in EMI's crafting recipes, one entry for every block and piece: each second its grid and what
 * it makes show another of the rule's frames ({@link MicroblockShowcase}), every place the same frame, as EMI shows
 * the game's own recipes with many forms.
 */
final class MicroblockEmiRecipe extends EmiPatternCraftingRecipe {

    private final List<MicroblockShowcase.Frame> frames;

    MicroblockEmiRecipe(final MicroblockShowcase.Rule rule) {
        super(inputsOf(MicroblockShowcase.frames(rule)), EmiStack.of(CoreSpaces.MICROBLOCK.get()),
                ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "/microblock/" + rule.id()));
        this.frames = MicroblockShowcase.frames(rule);
    }

    @Override
    public SlotWidget getInputWidget(final int slot, final int x, final int y) {
        return new GeneratedSlotWidget(random -> EmiStack.of(frameOf(random).grid().get(slot)), this.unique, x, y);
    }

    @Override
    public SlotWidget getOutputWidget(final int x, final int y) {
        return new GeneratedSlotWidget(random -> EmiStack.of(frameOf(random).output()), this.unique, x, y);
    }

    /* The frame shown this second: every place draws the same one, from the same seed. */
    private MicroblockShowcase.Frame frameOf(final Random random) {
        return this.frames.get(random.nextInt(this.frames.size()));
    }

    /* Everything the rule's frames hold, once each by item, so looking up any of them finds the rule. */
    private static List<EmiIngredient> inputsOf(final List<MicroblockShowcase.Frame> frames) {
        final Set<Item> items = new LinkedHashSet<>();
        for (final MicroblockShowcase.Frame frame : frames) {
            for (final ItemStack held : frame.grid()) {
                if (!held.isEmpty()) {
                    items.add(held.getItem());
                }
            }
        }
        final List<EmiIngredient> out = new ArrayList<>(items.size());
        for (final Item item : items) {
            out.add(EmiStack.of(item));
        }
        return out;
    }
}
