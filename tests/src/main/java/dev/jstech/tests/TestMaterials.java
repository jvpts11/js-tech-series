/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Materials;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The materials the tests use, declared as any mod declares its own: tin in every form a metal takes and as an ore in
 * three rocks, bronze as a gear, the game's own iron ingot, iron's ore in a rock the game has none in, and a material
 * of the test mod's own as a dust.
 */
@TextHolder
public final class TestMaterials {

    public static final TextKey TESTIUM_NAME = TextKey.of("jstests.material.testium", "Testium");

    /** A metal of the test mod's own. */
    public static final Material TESTIUM = Materials.material(
            ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "testium"), MaterialKind.METAL, TESTIUM_NAME)
            .colour(0x6FA8DC).declare();

    public static final Supplier<Item> TIN_PLATE = MaterialUses.use(JsTests.MODID, SeriesMaterials.TIN, Forms.PLATE);
    public static final Supplier<Item> BRONZE_GEAR = MaterialUses.use(JsTests.MODID, SeriesMaterials.BRONZE,
            Forms.GEAR);
    public static final Supplier<Item> IRON_INGOT = MaterialUses.use(JsTests.MODID, SeriesMaterials.IRON,
            Forms.INGOT);
    public static final Supplier<Item> TESTIUM_DUST = MaterialUses.use(JsTests.MODID, TESTIUM, Forms.DUST);

    static {
        // Tin in every form a metal takes but an ore, so each form's art is drawn somewhere.
        for (final Form form : Forms.all()) {
            if (form.shape() != Form.Shape.ORE && SeriesMaterials.TIN.fits(form)) {
                MaterialUses.use(JsTests.MODID, SeriesMaterials.TIN, form);
            }
        }
        MaterialUses.ore(JsTests.MODID, SeriesMaterials.TIN, HostRocks.STONE, HostRocks.DEEPSLATE, HostRocks.GRANITE);
        MaterialUses.ore(JsTests.MODID, SeriesMaterials.IRON, HostRocks.STONE, HostRocks.TUFF);
    }

    private TestMaterials() {
    }

    /** Declares the uses, from the test mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }
}
