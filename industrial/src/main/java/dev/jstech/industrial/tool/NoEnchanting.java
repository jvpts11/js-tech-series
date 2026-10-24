/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.tool;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.JsIndustrial;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent;

/**
 * J's Industrial's tools and armour are not enchanted (industrial/docs/design/tools-and-armour.md): they are improved
 * with modules that go in and come out ({@link IndustrialModules}). Whatever is in {@link #TAG}, of this mod or any
 * other, the enchanting table offers nothing for and the anvil takes no enchantment onto, from a book or another item;
 * it is still renamed and repaired there. Vanilla's tools and armour keep their enchanting.
 */
@TextHolder
public final class NoEnchanting {

    public static final TextKey TAG_NAME = TextKey.of("tag.item.jsindustrial.not_enchantable", "Not Enchantable");
    /** What is improved by modules and never enchanted. */
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(
            JsIndustrial.MODID, "not_enchantable"));

    private NoEnchanting() {
    }

    /** Hands the game's bus what this answers, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(EnchantmentLevelSetEvent.class, event -> {
            if (refuses(event.getItem())) {
                event.setEnchantLevel(0);
            }
        });
        NeoForge.EVENT_BUS.addListener(AnvilUpdateEvent.class, event -> {
            if (refuses(event.getLeft()) && enchanted(event.getRight())) {
                event.setCanceled(true);
            }
        });
    }

    /** Whether {@code stack} is never enchanted. */
    public static boolean refuses(final ItemStack stack) {
        return !stack.isEmpty() && stack.is(TAG);
    }

    /* Whether a stack carries an enchantment it could give: an enchanted thing, or a book of them. */
    private static boolean enchanted(final ItemStack stack) {
        return !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
                || !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
    }
}
