/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Generic machine categories are dynamic, not a fixed set: a category is a recipe type id straight from the
 * recipe-type registry ({@code minecraft:smelting}, {@code mekanism:crushing}, ...), so every installed mod
 * contributes its own categories automatically. A processing pattern authored against a generic category stores
 * the machine id {@code generic:<recipeTypeId>} and matches any declared machine whose Crafting Switch face the
 * player tagged with that category, so the player declares "this face hosts a smelting machine" instead of the mod
 * guessing it from the block.
 */
@TextHolder
public final class MachineCategory {

    private static final TextKey MACHINE = TextKey.of("jsc.crafting.machine", "Machine");
    /* A recipe category as a machine: any machine that runs it. */
    private static final TextKey ANY = TextKey.of("jsc.crafting.any_machine", "Any %s");

    /** Namespace grouping the generic entries in the machine picker. */
    public static final String GENERIC_NAMESPACE = "generic";

    /** Prefix of the synthetic machine ids a generic pattern stores, e.g. {@code generic:minecraft:smelting}. */
    public static final String GENERIC_PREFIX = GENERIC_NAMESPACE + ":";

    /** The empty category, where an untagged face only matches by block id or face name. */
    public static final String NONE = "";

    private MachineCategory() {
    }

    /** Whether the given pattern machine id refers to a generic category rather than a concrete block. */
    public static boolean isGenericId(final String machineType) {
        return machineType != null && machineType.startsWith(GENERIC_PREFIX);
    }

    /** The machine id a pattern stores when authored against the given category. */
    public static String genericIdOf(final String categoryId) {
        return GENERIC_PREFIX + categoryId;
    }

    /** The category id inside a generic machine id ({@code generic:minecraft:smelting} → {@code minecraft:smelting}). */
    public static String categoryOf(final String genericMachineId) {
        return isGenericId(genericMachineId) ? genericMachineId.substring(GENERIC_PREFIX.length()) : NONE;
    }

    /**
     * A readable name for a machine id, the way a craft dialog lists it: a block id by its path, title-cased
     * with the underscores as spaces ({@code minecraft:blast_furnace} is "Blast Furnace"); a generic category
     * as "Any" and the category's own path ({@code generic:minecraft:smelting} is "Any Smelting").
     */
    public static String label(final String machineType) {
        return text(machineType).english();
    }

    /**
     * The same, as text: a machine that is a block goes by the block's own name, which each player reads in their
     * language; any other id goes by its title-cased path, which is data.
     */
    public static Text text(final String machineType) {
        if (machineType == null || machineType.isEmpty()) {
            return MACHINE.text();
        }
        if (isGenericId(machineType)) {
            return ANY.with(titleCase(pathOf(categoryOf(machineType))));
        }
        final ResourceLocation id = ResourceLocation.tryParse(machineType);
        if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
            return GameText.of(BuiltInRegistries.BLOCK.get(id).getName());
        }
        return Text.literal(titleCase(pathOf(machineType)));
    }

    private static String pathOf(final String id) {
        final int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    private static String titleCase(final String path) {
        final StringBuilder out = new StringBuilder(path.length());
        boolean start = true;
        for (final char c : path.toCharArray()) {
            if (c == '_' || c == '/' || c == '.') {
                out.append(' ');
                start = true;
            } else {
                out.append(start ? Character.toUpperCase(c) : c);
                start = false;
            }
        }
        return out.toString().trim();
    }

    /** Every category currently available: the installed recipe type ids, sorted. */
    public static List<String> categoryIds() {
        final List<String> ids = new ArrayList<>();
        for (final ResourceLocation id : BuiltInRegistries.RECIPE_TYPE.keySet()) {
            ids.add(id.toString());
        }
        ids.sort(Comparator.naturalOrder());
        return ids;
    }
}
