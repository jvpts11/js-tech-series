/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.EntityBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The coarse category the Network Interactor's category filter sorts data into: what the item is by the
 * tags the mods agree on ({@code c:ingots}, {@code c:ores}...), else by what kind of item it is (a block, a
 * machine, a tool, armour, food), with fluids and chemicals as categories of their own.
 */
@TextHolder
public final class ItemCategories {

    public static final String OTHER = "Other";

    /** The categories in the order the filter lists them. */
    public static final List<String> ALL = List.of("Ingots", "Nuggets", "Ores", "Raw materials", "Dusts", "Gems",
            "Plates", "Rods", "Blocks", "Machines", "Tools", "Armor", "Food", "Fluids", "Chemicals", OTHER);

    /** The common tag paths that name a category, and the category each names. */
    private static final String[][] TAGGED = {
            {"ingots", "Ingots"}, {"nuggets", "Nuggets"}, {"ores", "Ores"}, {"raw_materials", "Raw materials"},
            {"dusts", "Dusts"}, {"gems", "Gems"}, {"plates", "Plates"}, {"rods", "Rods"},
            {"storage_blocks", "Blocks"}, {"tools", "Tools"}, {"armors", "Armor"}, {"foods", "Food"}};

    /*
     * What each category reads as. The English name above stays the category's id, which a saved filter keeps,
     * so a player's filter survives a change of language.
     */
    private static final TextKey INGOTS = TextKey.of("jsc.item_category.ingots", "Ingots");
    private static final TextKey NUGGETS = TextKey.of("jsc.item_category.nuggets", "Nuggets");
    private static final TextKey ORES = TextKey.of("jsc.item_category.ores", "Ores");
    private static final TextKey RAW_MATERIALS = TextKey.of("jsc.item_category.raw_materials", "Raw materials");
    private static final TextKey DUSTS = TextKey.of("jsc.item_category.dusts", "Dusts");
    private static final TextKey GEMS = TextKey.of("jsc.item_category.gems", "Gems");
    private static final TextKey PLATES = TextKey.of("jsc.item_category.plates", "Plates");
    private static final TextKey RODS = TextKey.of("jsc.item_category.rods", "Rods");
    private static final TextKey BLOCKS = TextKey.of("jsc.item_category.blocks", "Blocks");
    private static final TextKey MACHINES = TextKey.of("jsc.item_category.machines", "Machines");
    private static final TextKey TOOLS = TextKey.of("jsc.item_category.tools", "Tools");
    private static final TextKey ARMOR = TextKey.of("jsc.item_category.armor", "Armor");
    private static final TextKey FOOD = TextKey.of("jsc.item_category.food", "Food");
    private static final TextKey FLUIDS = TextKey.of("jsc.item_category.fluids", "Fluids");
    private static final TextKey CHEMICALS = TextKey.of("jsc.item_category.chemicals", "Chemicals");
    private static final TextKey OTHER_LABEL = TextKey.of("jsc.item_category.other", "Other");
    private static final Map<String, TextKey> LABELS = labels(INGOTS, NUGGETS, ORES, RAW_MATERIALS, DUSTS, GEMS,
            PLATES, RODS, BLOCKS, MACHINES, TOOLS, ARMOR, FOOD, FLUIDS, CHEMICALS, OTHER_LABEL);

    private ItemCategories() {
    }

    /** What a category reads as in the player's language; one this list does not know reads as its id. */
    public static Text label(final String category) {
        final TextKey key = LABELS.get(category);
        return key == null ? Text.literal(category) : key.text();
    }

    public static String of(final StorageKey key) {
        if (key.isFluid()) {
            return "Fluids";
        }
        if (key.isChemical()) {
            return "Chemicals";
        }
        final ItemStack stack = key.stack(1);
        final String tagged = byTag(stack.getItemHolder().tags());
        if (tagged != null) {
            return tagged;
        }
        final Item item = stack.getItem();
        if (item instanceof BlockItem block) {
            return block.getBlock() instanceof EntityBlock ? "Machines" : "Blocks";
        }
        if (item instanceof DiggerItem || item instanceof SwordItem || item instanceof ShearsItem
                || item instanceof FishingRodItem || item instanceof FlintAndSteelItem || item instanceof BowItem
                || item instanceof CrossbowItem || item instanceof TridentItem) {
            return "Tools";
        }
        if (item instanceof ArmorItem) {
            return "Armor";
        }
        if (stack.has(DataComponents.FOOD)) {
            return "Food";
        }
        return OTHER;
    }

    private static String byTag(final Stream<TagKey<Item>> tags) {
        return tags.map(TagKey::location)
                .filter(id -> id.getNamespace().equals("c"))
                .map(ItemCategories::categoryOfTag)
                .filter(c -> c != null)
                .findFirst()
                .orElse(null);
    }

    private static String categoryOfTag(final ResourceLocation tag) {
        final String path = tag.getPath();
        for (final String[] entry : TAGGED) {
            if (path.equals(entry[0]) || path.startsWith(entry[0] + "/")) {
                return entry[1];
            }
        }
        return null;
    }

    /* Each label under the id it reads for, which is its own English. */
    private static Map<String, TextKey> labels(final TextKey... keys) {
        final Map<String, TextKey> out = new HashMap<>();
        for (final TextKey key : keys) {
            out.put(key.english(), key);
        }
        return Map.copyOf(out);
    }
}
