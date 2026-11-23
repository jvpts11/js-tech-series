/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialUses;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;

/**
 * The series' bronze, which the Core alloys by hand so any mod used alone can make it, and the tin it needs, which the
 * Core lays and smelts. Three copper ingots and one tin ingot make four of bronze at the crafting table, near real
 * bronze's share of tin (about 12%). Tin's ore lies in the Overworld's stone and deepslate in every world, and its raw
 * lumps and its ore smelt in the furnace and the blast furnace, as the game's own metals do.
 */
public final class SeriesBronze {

    public static final Supplier<Item> TIN_INGOT = MaterialUses.use(JsCore.MODID, SeriesMaterials.TIN, Forms.INGOT);
    public static final Supplier<Item> BRONZE_INGOT = MaterialUses.use(JsCore.MODID, SeriesMaterials.BRONZE,
            Forms.INGOT);
    /** The copper ingots that go with one tin ingot into bronze, and the bronze ingots they make. */
    public static final int COPPER_INGOTS = 3;
    public static final int ALLOYED = 4;
    /** The metals whose raw lumps and ore the Core smelts, so no other mod smelts them again. */
    public static final List<Material> SMELTED = List.of(SeriesMaterials.TIN);

    static {
        MaterialUses.ore(JsCore.MODID, SeriesMaterials.TIN, HostRocks.STONE, HostRocks.DEEPSLATE);
    }

    private SeriesBronze() {
    }

    /** Declares the tin and the bronze, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }
}
