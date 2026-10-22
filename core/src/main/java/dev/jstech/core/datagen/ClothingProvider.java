/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.JsCore;
import dev.jstech.core.hazard.Clothing;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.DataMapProvider;

/**
 * What the game's own armour does against the weather, near what each is made of: leather lets about six watts out a
 * square metre for each kelvin, about one clo, and keeps the wind out; mail lets almost all of it through and keeps
 * nothing out; plates of iron, gold, diamond and netherite hold a little in over their padding and keep the wind out;
 * a turtle shell keeps the water out too.
 */
public final class ClothingProvider extends DataMapProvider {

    private static final double LEATHER = 6.0;
    private static final double MAIL = 40.0;
    private static final double IRON = 25.0;
    private static final double GOLD = 30.0;
    private static final double DIAMOND = 25.0;
    private static final double NETHERITE = 20.0;
    private static final double SHELL = 20.0;
    private static final double ELYTRA = 30.0;

    public ClothingProvider(final PackOutput output, final CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    public String getName() {
        return JsCore.MODID + ":clothing";
    }

    @Override
    protected void gather() {
        final Builder<Clothing.Piece, Item> map = builder(Clothing.CLOTHING);
        set(map, List.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS),
                new Clothing.Piece(LEATHER, true, false));
        set(map, List.of(Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS,
                Items.CHAINMAIL_BOOTS), new Clothing.Piece(MAIL, false, false));
        set(map, List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS),
                new Clothing.Piece(IRON, true, false));
        set(map, List.of(Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS),
                new Clothing.Piece(GOLD, true, false));
        set(map, List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS),
                new Clothing.Piece(DIAMOND, true, false));
        set(map, List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS,
                Items.NETHERITE_BOOTS), new Clothing.Piece(NETHERITE, true, false));
        set(map, List.of(Items.TURTLE_HELMET), new Clothing.Piece(SHELL, true, true));
        set(map, List.of(Items.ELYTRA), new Clothing.Piece(ELYTRA, true, false));
    }

    private static void set(final Builder<Clothing.Piece, Item> map, final List<Item> items,
                            final Clothing.Piece piece) {
        for (final Item item : items) {
            map.add(item.builtInRegistryHolder(), piece, false);
        }
    }
}
