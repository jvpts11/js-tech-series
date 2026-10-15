/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.machine.IUpgrade;
import dev.jstech.core.machine.UpgradeEffect;
import dev.jstech.core.machine.UpgradeItem;

/**
 * Upgrades of the test mod, one for each effect the Core's machines know: two recipes at once, twice the items, no
 * byproducts, half the fluid and matter, a doubled buffer, and silence; the first two fit in a machine only once.
 */
public final class TestUpgrades {

    /** Two recipes at once, for twice the energy; one fits in a machine. */
    public static final ItemEntry<UpgradeItem> PARALLEL = TestSounds.CONTENT.item("test_parallel_upgrade",
            properties -> new UpgradeItem(properties, UpgradeEffect.NONE.withParallel(1).withEnergy(2.0), 1))
            .named("Test Parallel Upgrade").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    /** Twice the items; one fits in a machine. */
    public static final ItemEntry<UpgradeItem> YIELD = TestSounds.CONTENT.item("test_yield_upgrade",
            properties -> new UpgradeItem(properties, UpgradeEffect.NONE.withYield(2.0), 1))
            .named("Test Yield Upgrade").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    /** No byproducts at all. */
    public static final ItemEntry<UpgradeItem> NO_BYPRODUCTS = TestSounds.CONTENT.item("test_no_byproducts_upgrade",
            properties -> new UpgradeItem(properties, UpgradeEffect.NONE.withByproducts(0.0), IUpgrade.ANY))
            .named("Test No Byproducts Upgrade").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    /** Half the fluid and matter a recipe spends. */
    public static final ItemEntry<UpgradeItem> THRIFT = TestSounds.CONTENT.item("test_thrift_upgrade",
            properties -> new UpgradeItem(properties, UpgradeEffect.NONE.withUse(0.5), IUpgrade.ANY))
            .named("Test Thrift Upgrade").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    /** Twice the energy stored, and silence. */
    public static final ItemEntry<UpgradeItem> BUFFER = TestSounds.CONTENT.item("test_buffer_upgrade",
            properties -> new UpgradeItem(properties, UpgradeEffect.NONE.withBuffer(2.0).withSound(0.0),
                    IUpgrade.ANY))
            .named("Test Buffer Upgrade").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();

    private TestUpgrades() {
    }

    /** Declares the upgrades, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }
}
