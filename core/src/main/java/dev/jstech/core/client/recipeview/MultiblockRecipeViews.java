/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.recipeview;

import dev.jstech.core.client.multiblock.GhostBlocks;
import dev.jstech.core.multiblock.IStructureShape;
import dev.jstech.core.multiblock.MultiblockViewLayout;
import dev.jstech.core.multiblock.MultiblockViewTexts;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * What both recipe viewers show of the multiblocks every mod has: each one the Structure Projector lists, laid out at
 * its least size, with its materials, its first choice and how it may stand. The bridges to JEI and to EMI read only
 * this, so the two show the same multiblocks the same way.
 */
public final class MultiblockRecipeViews {

    private MultiblockRecipeViews() {
    }

    /** Every multiblock that can be shown, in the order the projector lists them; one that lays out to nothing not. */
    public static List<MultiblockEntry> entries() {
        final List<MultiblockEntry> out = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, IStructureShape> shown : Projectables.all().entrySet()) {
            Projectables.preview(shown.getKey(), 0)
                    .ifPresent(preview -> out.add(entry(shown.getKey(), shown.getValue(), preview)));
        }
        return out;
    }

    /**
     * {@code shape}, called {@code id}, as both recipe viewers would show it, though no mod declared it yet: what the
     * studio's recorder shows of a pattern before it is written. None when it lays out to nothing.
     */
    public static Optional<MultiblockEntry> of(final ResourceLocation id, final IStructureShape shape) {
        return shape.preview(0).map(preview -> entry(id, shape, preview));
    }

    private static MultiblockEntry entry(final ResourceLocation id, final IStructureShape shape,
                                         final ShapePreview preview) {
        final List<MultiblockEntry.Material> materials = new ArrayList<>();
        preview.materials().forEach((first, count) -> {
            final List<ItemStack> stacks = itemsOf(GhostBlocks.blocksOf(first), count);
            if (!stacks.isEmpty()) {
                materials.add(new MultiblockEntry.Material(stacks, count, false));
            }
        });
        final List<ItemStack> controllers = itemsOf(GhostBlocks.blocksOf(preview.controllerBlock().orElse(null)), 1);
        final int room = MultiblockViewLayout.MOST_MATERIALS - (controllers.isEmpty() ? 0 : 1);
        final List<MultiblockEntry.Material> shown = new ArrayList<>(materials.subList(0,
                Math.min(room, materials.size())));
        if (!controllers.isEmpty()) {
            shown.add(new MultiblockEntry.Material(controllers, 1, true));
        }
        final Optional<MultiblockEntry.Choice> choice = preview.choices().stream()
                .map(each -> new MultiblockEntry.Choice(itemsOf(GhostBlocks.choicesOf(each.blocks(), each.tags()), 1),
                        each.places()))
                .filter(each -> each.stacks().size() > 1).findFirst();
        return new MultiblockEntry(id, preview, GameText.component(Projectables.name(id)), shown, choice,
                turns(shape), controllers.isEmpty() ? ItemStack.EMPTY : controllers.getFirst());
    }

    /* How it may stand, a line or three: its four turns always, and its mirror and lying down when it has them. */
    private static List<Component> turns(final IStructureShape shape) {
        if (shape.mirrors() && shape.liesDown()) {
            return List.of(line(MultiblockViewTexts.TURNS), line(MultiblockViewTexts.MIRRORS_AND),
                    line(MultiblockViewTexts.LIES_DOWN));
        }
        if (shape.mirrors()) {
            return List.of(line(MultiblockViewTexts.TURNS_AND), line(MultiblockViewTexts.MIRRORS));
        }
        if (shape.liesDown()) {
            return List.of(line(MultiblockViewTexts.TURNS_AND), line(MultiblockViewTexts.LIES_DOWN));
        }
        return List.of(line(MultiblockViewTexts.TURNS), line(MultiblockViewTexts.NO_MIRROR));
    }

    private static Component line(final TextKey key) {
        return GameText.component(key);
    }

    /* The items of {@code blocks}, {@code count} of each, blocks with no item of their own left out. */
    private static List<ItemStack> itemsOf(final List<Block> blocks, final int count) {
        final Set<Item> items = new LinkedHashSet<>();
        for (final Block block : blocks) {
            final Item item = block.asItem();
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        return items.stream().map(item -> new ItemStack(item, count)).toList();
    }
}
