/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.hardware.CaseFanSpec;
import dev.jstech.computers.hardware.CoolerSpec;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The parts that cool a computer (computers/docs/design/temperature.md): a case fan of every era, which every part with
 * a fan takes too, the cooler a processor comes with in every era, and from the Standard the tower cooler and the
 * all-in-one water coolers, whose radiator takes two of the case's fan seats. Watts each takes away; estimates.
 */
public final class CoolingItems {

    public static final DeferredItem<Item> VINTAGE_CASE_FAN = PartItems.caseFan("case_fan_vintage",
            new CaseFanSpec(HardwareEra.VINTAGE, 80, 20)).named("Vintage Case Fan, 80 mm").register();
    public static final DeferredItem<Item> LEGACY_CASE_FAN = PartItems.caseFan("case_fan_legacy",
            new CaseFanSpec(HardwareEra.LEGACY, 80, 30)).named("Legacy Case Fan, 80 mm").register();
    public static final DeferredItem<Item> TRANSITION_CASE_FAN = PartItems.caseFan("case_fan_transition",
            new CaseFanSpec(HardwareEra.TRANSITION, 92, 40)).named("Transition Case Fan, 92 mm").register();
    public static final DeferredItem<Item> STANDARD_CASE_FAN = PartItems.caseFan("case_fan_standard",
            new CaseFanSpec(HardwareEra.STANDARD, 120, 50)).named("Standard Case Fan, 120 mm").register();
    public static final DeferredItem<Item> ADVANCED_CASE_FAN = PartItems.caseFan("case_fan_advanced",
            new CaseFanSpec(HardwareEra.ADVANCED, 120, 60)).named("Advanced Case Fan, 120 mm").register();

    public static final DeferredItem<Item> VINTAGE_STOCK_COOLER = PartItems.cooler("stock_cooler_vintage",
            new CoolerSpec(HardwareEra.VINTAGE, CoolerSpec.Kind.STOCK, 20)).named("Stock Cooler (Vintage)").register();
    public static final DeferredItem<Item> LEGACY_STOCK_COOLER = PartItems.cooler("stock_cooler_legacy",
            new CoolerSpec(HardwareEra.LEGACY, CoolerSpec.Kind.STOCK, 60)).named("Stock Cooler (Legacy)").register();
    public static final DeferredItem<Item> TRANSITION_STOCK_COOLER = PartItems.cooler("stock_cooler_transition",
            new CoolerSpec(HardwareEra.TRANSITION, CoolerSpec.Kind.STOCK, 100)).named("Stock Cooler (Transition)")
            .register();
    public static final DeferredItem<Item> STANDARD_STOCK_COOLER = PartItems.cooler("stock_cooler_standard",
            new CoolerSpec(HardwareEra.STANDARD, CoolerSpec.Kind.STOCK, 150)).named("Stock Cooler (Standard)")
            .register();
    public static final DeferredItem<Item> ADVANCED_STOCK_COOLER = PartItems.cooler("stock_cooler_advanced",
            new CoolerSpec(HardwareEra.ADVANCED, CoolerSpec.Kind.STOCK, 200)).named("Stock Cooler (Advanced)")
            .register();

    public static final DeferredItem<Item> STANDARD_TOWER_COOLER = PartItems.cooler("tower_cooler_standard",
            new CoolerSpec(HardwareEra.STANDARD, CoolerSpec.Kind.TOWER, 250)).named("Tower Cooler (Standard)")
            .register();
    public static final DeferredItem<Item> ADVANCED_TOWER_COOLER = PartItems.cooler("tower_cooler_advanced",
            new CoolerSpec(HardwareEra.ADVANCED, CoolerSpec.Kind.TOWER, 300)).named("Tower Cooler (Advanced)")
            .register();

    // The 360 mm radiator takes the same two fan seats as the 240, and takes away as much: the spec's numbers.
    public static final DeferredItem<Item> STANDARD_AQUALOOP_240 = PartItems.cooler("aqualoop_240_standard",
            new CoolerSpec(HardwareEra.STANDARD, CoolerSpec.Kind.WATER, 300)).named("MF AquaLoop 240 (Standard)")
            .register();
    public static final DeferredItem<Item> STANDARD_AQUALOOP_360 = PartItems.cooler("aqualoop_360_standard",
            new CoolerSpec(HardwareEra.STANDARD, CoolerSpec.Kind.WATER, 300)).named("MF AquaLoop 360 (Standard)")
            .register();
    public static final DeferredItem<Item> ADVANCED_AQUALOOP_240 = PartItems.cooler("aqualoop_240_advanced",
            new CoolerSpec(HardwareEra.ADVANCED, CoolerSpec.Kind.WATER, 400)).named("MF AquaLoop 240 (Advanced)")
            .register();
    public static final DeferredItem<Item> ADVANCED_AQUALOOP_360 = PartItems.cooler("aqualoop_360_advanced",
            new CoolerSpec(HardwareEra.ADVANCED, CoolerSpec.Kind.WATER, 400)).named("MF AquaLoop 360 (Advanced)")
            .register();

    private CoolingItems() {
    }

    /** Loads the class, which declares the items onto the mod's content; called before the content is registered. */
    public static void init() {
        // Referencing the class runs the declarations above.
    }
}
