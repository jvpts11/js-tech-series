/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * An amount too large to weigh weighs the most there is, and never less than nothing.
 *
 * <p>Every amount in the network is turned into a weight by multiplying it, and a large enough amount times
 * the weight of one comes back negative. What follows from a negative weight is not a number that reads
 * strangely: a request for it passes every check that it fits, putting it away adds room to a disk rather
 * than using it, and free space reads as more than the disk holds. None of it looks like arithmetic from the
 * outside; it looks like the storage inventing things.
 *
 * <p>An amount can arrive from a packet, so it can be any number at all. This asks for the largest there is.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SizeOverflowGameTests {

    private static final String ARENA = "empty";

    private SizeOverflowGameTests() {
    }

    @GameTest(template = ARENA)
    public static void weight_ofMoreItemsThanCouldExistIsStillAWeight(final GameTestHelper helper) {
        final StorageKey iron = StorageKey.of(Items.IRON_INGOT);
        helper.assertTrue(iron.weight(Long.MAX_VALUE) > 0L,
                "every item there could be weighed less than nothing");
        helper.assertTrue(iron.weight(Long.MAX_VALUE) == Long.MAX_VALUE,
                "and it should weigh as much as there is");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void weight_ofAnOrdinaryAmountIsUnchanged(final GameTestHelper helper) {
        final StorageKey iron = StorageKey.of(Items.IRON_INGOT);
        helper.assertTrue(iron.weight(64L) == 64L * StorageKey.MB_EQ_PER_ITEM,
                "an amount anybody would ask for must weigh exactly what it always did");
        helper.assertTrue(iron.weight(0L) == 0L, "and nothing must weigh nothing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void itemsFor_theLargestSizeThereIsStillCostsSomething(final GameTestHelper helper) {
        for (final HardwareEra era : HardwareEra.values()) {
            helper.assertTrue(era.itemsFor(Long.MAX_VALUE) > 0L,
                    "on a " + era + " disk the largest size there is cost nothing at all");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void itemsFor_anOrdinarySizeIsUnchanged(final GameTestHelper helper) {
        helper.assertTrue(HardwareEra.VINTAGE.itemsFor(4L) == 4L, "four megabytes on a vintage disk");
        helper.assertTrue(HardwareEra.LEGACY.itemsFor(17L) == 2L, "seventeen on a legacy disk rounds up to two");
        helper.assertTrue(HardwareEra.STANDARD.itemsFor(0L) == 0L, "nothing costs nothing");
        helper.succeed();
    }
}
