/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.energy.CoreEnergy;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.energy.EnergyRatio;
import dev.jstech.core.energy.EnergyUnit;
import dev.jstech.core.energy.IWrongClassRule;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * An energy of the test mod's own, declared through the Core's kit as any mod's is: counted in test joules, two of which
 * are worth one FE, with four classes of line from low to extra-high, the low class losing a thousandth of what crosses
 * each block, and the standard rule for a port of another class. Two of its lines are laid by vanilla items and drawn in a
 * jacket the series already has.
 */
@TextHolder
public final class TestEnergies {

    public static final TextKey VOLTS_NAME = TextKey.of("jstests.energy.volts", "Test Volts");
    public static final TextKey JOULE_NAME = TextKey.of("jstests.energy.joule", "Test Joule");
    public static final TextKey JOULE_SYMBOL = TextKey.of("jstests.energy.joule.symbol", "tJ");
    public static final TextKey WATT_SYMBOL = TextKey.of("jstests.energy.watt.symbol", "tW");
    public static final TextKey LOW_NAME = TextKey.of("jstests.energy.class.low", "Low");
    public static final TextKey LOW_RATING = TextKey.of("jstests.energy.class.low.rating", "100 V");
    public static final TextKey MEDIUM_NAME = TextKey.of("jstests.energy.class.medium", "Medium");
    public static final TextKey HIGH_NAME = TextKey.of("jstests.energy.class.high", "High");
    public static final TextKey EXTRA_NAME = TextKey.of("jstests.energy.class.extra", "Extra-high");

    /** The millionths each block of a low line loses. */
    public static final int LOW_LOSS = 1_000;
    public static final long LINE_RATE = 100_000L;
    /** The longest span of the hung line, in blocks. */
    public static final int HUNG_SPAN = 16;
    /** What touching a live bare wire of the low class does. */
    public static final float LOW_SHOCK = 4F;

    public static final DeferredRegister<EnergyUnit> UNITS = DeferredRegister.create(CoreEnergy.UNITS_KEY,
            JsTests.MODID);
    public static final DeferredRegister<CableType> CABLES = DeferredRegister.create(CoreCables.KEY, JsTests.MODID);
    /** Two test joules make one FE. */
    public static final DeferredHolder<EnergyUnit, EnergyUnit> JOULE = UNITS.register("test_joule",
            () -> new EnergyUnit(JOULE_NAME, JOULE_SYMBOL, new EnergyRatio(2L, 1L)));

    public static final EnergyClass LOW = EnergyClass.rated("low", LOW_NAME, LOW_RATING);
    public static final EnergyClass MEDIUM = EnergyClass.of("medium", MEDIUM_NAME);
    public static final EnergyClass HIGH = EnergyClass.of("high", HIGH_NAME);
    public static final EnergyClass EXTRA = EnergyClass.of("extra", EXTRA_NAME);
    public static final Energy VOLTS = Energies.builder(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                    "test_volts"), VOLTS_NAME).unit(JOULE).power(WATT_SYMBOL).classes(LOW, MEDIUM, HIGH, EXTRA)
            .loss(LOW, LOW_LOSS).wrongClass(IWrongClassRule.STANDARD).shock(LOW, LOW_SHOCK).declare();

    /** A line of the low class. */
    public static final DeferredHolder<CableType, CableType> LOW_LINE = CABLES.register("volts_low_line",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "volts_low_line")))
                    .grid(VOLTS.grid(LOW)).alone().carries(LINE_RATE, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/insulated_copper"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/plug/insulated_copper"))
                    .build(() -> Items.LIGHTNING_ROD));
    /** A line of the low class whose wire is also hung, across sixteen blocks at most. */
    public static final DeferredHolder<CableType, CableType> HUNG_LINE = CABLES.register("volts_hung_line",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "volts_hung_line")))
                    .grid(VOLTS.grid(LOW)).alone().carries(LINE_RATE, 0).hangs(HUNG_SPAN)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/insulated_copper"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/plug/insulated_copper"))
                    .build(() -> Items.STRING));
    /** A line of the high class. */
    public static final DeferredHolder<CableType, CableType> HIGH_LINE = CABLES.register("volts_high_line",
            () -> CableType.builder(Connection.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                            "volts_high_line")))
                    .grid(VOLTS.grid(HIGH)).alone().carries(LINE_RATE, 0)
                    .jacket(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/insulated_copper"))
                    .plug(ResourceLocation.fromNamespaceAndPath("jscore", "block/cable/plug/insulated_copper"))
                    .build(() -> Items.CHAIN));

    private TestEnergies() {
    }

    public static void register(final IEventBus modEventBus) {
        UNITS.register(modEventBus);
        CABLES.register(modEventBus);
    }
}
