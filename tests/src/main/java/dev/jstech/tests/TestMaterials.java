/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.jstechseries.SharedCatalogue;
import dev.jstech.core.jstechseries.SimplePath;
import dev.jstech.core.material.CatalogueItems;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.material.MineralFamilies;
import dev.jstech.core.material.OreLook;
import dev.jstech.core.matter.MatterBridges;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/**
 * The materials the tests use, declared as any mod declares its own: tin in every form a metal takes and as an ore in
 * three rocks, bronze as a gear, the game's own iron ingot, iron's ore in a rock the game has none in, a material of
 * the test mod's own as a dust, a radioactive mineral of the test mod's own as an ore, a gas the test gas fluid is, and
 * a slurry.
 */
@TextHolder
public final class TestMaterials {

    public static final TextKey TESTIUM_NAME = TextKey.of("jstests.material.testium", "Testium");
    public static final TextKey TESTITE_NAME = TextKey.of("jstests.material.testite", "Testite");
    public static final TextKey TESTOGEN_NAME = TextKey.of("jstests.material.testogen", "Testogen");
    public static final TextKey TEST_MUD_NAME = TextKey.of("jstests.material.test_mud", "Test Mud");

    /** A metal of the test mod's own. */
    public static final Material TESTIUM = Materials.material(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "testium"), MaterialKind.METAL, TESTIUM_NAME)
            .colour(0x6FA8DC).declare();
    /** A mineral of uranium and thorium, an oxide, whose veins cross stone and granite. */
    public static final Material TESTITE = Materials.material(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "testite"), MaterialKind.MINERAL, TESTITE_NAME)
            .colour(0x353735).mineral(MineralFamilies.OXIDE, HostRocks.STONE, HostRocks.GRANITE)
            .contains(Elements.of(Element.URANIUM), 0.7).contains(Elements.of(Element.THORIUM), 0.1)
            .ore(OreLook.of(OreLook.Pattern.LAPIS)).declare();
    /** A gas of the test mod's own, which the test gas fluid is, as another mod's gas kept as a fluid would be. */
    public static final Material TESTOGEN = Materials.material(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "testogen"), MaterialKind.GAS, TESTOGEN_NAME)
            .colour(0xD0D0E0).declare();
    /** A slurry of the test mod's own. */
    public static final Material TEST_MUD = Materials.material(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_mud"), MaterialKind.MINERAL, TEST_MUD_NAME)
            .colour(0x5C4A3A).at(MatterState.SLURRY).declare();

    public static final Supplier<Item> TIN_PLATE = MaterialUses.use(JsTests.MODID, SeriesMaterials.TIN, Forms.PLATE);
    public static final Supplier<Item> BRONZE_GEAR = MaterialUses.use(JsTests.MODID, SeriesMaterials.BRONZE,
            Forms.GEAR);
    public static final Supplier<Item> IRON_INGOT = MaterialUses.use(JsTests.MODID, SeriesMaterials.IRON,
            Forms.INGOT);
    public static final Supplier<Item> TESTIUM_DUST = MaterialUses.use(JsTests.MODID, TESTIUM, Forms.DUST);

    public static final TextKey GADGET_NAME = TextKey.of("item.jstests.test_gadget", "Test Gadget");
    /** A thing of the shared catalogue of the test mod's own, drawn with a picture the game has. */
    public static final CatalogueItems.Entry GADGET = CatalogueItems.declare(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_gadget"), GADGET_NAME,
            ResourceLocation.withDefaultNamespace("item/comparator"));
    public static final Supplier<Item> GADGET_ITEM = CatalogueItems.use(JsTests.MODID, GADGET);
    /** Tin molten, at its melting point, and gasoline, at room temperature. */
    public static final Supplier<Fluid> MOLTEN_TIN = MaterialLiquids.use(JsTests.MODID, SeriesMaterials.TIN, 505);
    public static final Supplier<Fluid> GASOLINE = MaterialLiquids.use(JsTests.MODID, SharedCatalogue.GASOLINE,
            MaterialLiquids.ROOM);
    /** Whether the test mod's simpler path is closed where J's Industrial is installed; off unless a test says. */
    public static final boolean[] INDUSTRIAL_CHAIN_ONLY = {false};

    static {
        // Tin in every form a metal takes but an ore, so each form's art is drawn somewhere.
        for (final Form form : Forms.all()) {
            if (form.shape() != Form.Shape.ORE && SeriesMaterials.TIN.fits(form)) {
                MaterialUses.use(JsTests.MODID, SeriesMaterials.TIN, form);
            }
        }
        MaterialUses.ore(JsTests.MODID, SeriesMaterials.TIN, HostRocks.STONE, HostRocks.DEEPSLATE, HostRocks.GRANITE);
        MaterialUses.ore(JsTests.MODID, SeriesMaterials.IRON, HostRocks.STONE, HostRocks.TUFF);
        MaterialUses.ore(JsTests.MODID, TESTITE, HostRocks.STONE, HostRocks.GRANITE);
        SimplePath.rule(JsTests.MODID, () -> INDUSTRIAL_CHAIN_ONLY[0]);
        MatterBridges.fluid(TESTOGEN, ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_gas"));
    }

    private TestMaterials() {
    }

    /** Declares the uses, from the test mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }
}
