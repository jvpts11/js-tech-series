/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The lunar suit, Apollo's A7L with its lunar visor assembly: a hard white helmet with its smoked visor in its frame,
 * the suit with its neck ring, bearings and convolutes, its chest module and hoses, and on its back the life-support
 * pack with its bottles; the trousers and the boots. Worn whole, it is a pressure suit for hours on a surface: half an
 * hour of air in its tanks, and the cold of the lunar night and the heat of its day. Its model is the Core's armour
 * kit's, each part following the wearer's limb; the gold visor, a module, is lowered over the helmet's own when
 * fitted.
 */
@TextHolder
public final class LunarSuit {

    /** The tab's row of what keeps a crew alive: the suits, then their modules. */
    public static final ContentTab.Section LIFE_SUPPORT = SpaceContent.TAB.section();
    public static final TextKey NAME = TextKey.of("jsspace.suit.lunar", "Lunar Suit");
    /** The suit's id among J's Space's suits. */
    public static final ResourceLocation ID = id("lunar");
    /** How many seconds of breathing its tanks hold, full: half an hour. */
    public static final int AIR = 1_800;
    /** The coldest and the hottest it stands, in degrees Celsius: the Moon's night and its noon, as the A7L did. */
    public static final double COLDEST = -160;
    public static final double HOTTEST = 125;
    /** The bone of its model that is the gold visor, lowered while the module is fitted to the helmet. */
    public static final String GOLD_VISOR_BONE = "goldVisor";

    /* What it is made of: cloth that stops as much as leather, and wears as iron does. */
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL,
            JsSpace.MODID);
    private static final int DURABILITY = 15;
    private static final int ENCHANTABILITY = 9;
    private static final DeferredHolder<ArmorMaterial, ArmorMaterial> MATERIAL = MATERIALS.register("lunar_suit",
            () -> new ArmorMaterial(Map.of(ArmorItem.Type.HELMET, 1, ArmorItem.Type.CHESTPLATE, 3,
                    ArmorItem.Type.LEGGINGS, 2, ArmorItem.Type.BOOTS, 1), ENCHANTABILITY,
                    SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY,
                    List.of(new ArmorMaterial.Layer(id("lunar_suit"))), 0, 0));

    /** The hard helmet, its smoked visor in its frame. */
    public static final ItemEntry<ArmorItem> HELMET = piece("lunar_helmet", ArmorItem.Type.HELMET)
            .named("Lunar Helmet").tab(LIFE_SUPPORT).register();
    /** The suit itself with the life-support pack on its back, which holds the tanks. */
    public static final ItemEntry<ArmorItem> SUIT = piece("lunar_suit", ArmorItem.Type.CHESTPLATE)
            .named("Lunar Suit").tab(LIFE_SUPPORT).register();
    /** The trousers, their convolutes at the knees and their pockets. */
    public static final ItemEntry<ArmorItem> TROUSERS = piece("lunar_trousers", ArmorItem.Type.LEGGINGS)
            .named("Lunar Trousers").tab(LIFE_SUPPORT).register();
    /** The lunar boots, worn over the suit's, their soles treaded. */
    public static final ItemEntry<ArmorItem> BOOTS = piece("lunar_boots", ArmorItem.Type.BOOTS)
            .named("Lunar Boots").tab(LIFE_SUPPORT).register();
    /** The suit's model, every piece's. */
    public static final DeclaredModel MODEL = SpaceContent.CONTENT.model("lunar_suit")
            .file("jsmodels/lunar_suit.gltf")
            .texture("textures/models/lunar_suit.png")
            .register();

    private LunarSuit() {
    }

    /** Registers what it is made of and declares it a suit, once, from the mod's constructor. */
    public static void declare(final IEventBus modEventBus) {
        MATERIALS.register(modEventBus);
        SpaceSuits.declare(ID, new SpaceSuit(NAME.key(), HELMET.getId(), SUIT.getId(), TROUSERS.getId(),
                BOOTS.getId(), AIR, COLDEST, HOTTEST));
    }

    private static ItemBuilder<ArmorItem> piece(final String id, final ArmorItem.Type type) {
        return SpaceContent.CONTENT.item(id, properties -> new ArmorItem(MATERIAL, type,
                properties.durability(type.getDurability(DURABILITY))));
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
