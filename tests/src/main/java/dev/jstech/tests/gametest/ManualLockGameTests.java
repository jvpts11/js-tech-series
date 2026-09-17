/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A hold a player put on the storage lasts until somebody lets it go, and closing a world is not that.
 *
 * <p>The hold itself cannot be saved: it points at which machines the things were on, and a world coming
 * back reads the network afresh. What is saved is what was held and of what, and the hold is taken again
 * against what is there then.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ManualLockGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;

    private ManualLockGameTests() {
    }

    @GameTest(template = ARENA)
    public static void manualLock_isWrittenDownWithTheMainframe(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork network =
                TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        network.seed(Items.IRON_INGOT, 128);
        final MainframeBlockEntity mainframe = network.mainframe();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final StorageKey iron = StorageKey.of(Items.IRON_INGOT);
                    final long held = mainframe.networkIndex().manualLock(iron, 64L, null);
                    helper.assertTrue(held > 0L, "nothing was held, so there is nothing to save");

                    final CompoundTag saved =
                            mainframe.saveWithoutMetadata(helper.getLevel().registryAccess());

                    helper.assertTrue(saved.contains("ManualLocks"),
                            "the Mainframe saved nothing about the hold a player put on the storage");
                })
                .thenSucceed();
    }

    /*
     * The other half: what was saved is taken again. The reservation is thrown away the way a reload throws
     * it away, and the holds are put back from what was kept, which is what a world coming back does.
     */
    @GameTest(template = ARENA)
    public static void manualLock_isTakenAgainAfterTheIndexIsReadAfresh(final GameTestHelper helper) {
        final TestWorldBuilder.CraftingNetwork network =
                TestWorldBuilder.forGameTest(helper).buildCraftingNetwork();
        network.seed(Items.IRON_INGOT, 128);
        final MainframeBlockEntity mainframe = network.mainframe();
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final StorageKey iron = StorageKey.of(Items.IRON_INGOT);
                    helper.assertTrue(mainframe.networkIndex().manualLock(iron, 64L, null) > 0L,
                            "nothing was held to begin with");
                    final var kept = mainframe.networkIndex().manualLockView();

                    mainframe.networkIndex().manualUnlockAll();
                    helper.assertFalse(mainframe.networkIndex().isManuallyLocked(iron),
                            "the hold was still there after being let go of");

                    helper.assertTrue(mainframe.networkIndex().restoreManualLocks(kept) == 1,
                            "the hold that was kept could not be taken again");
                    helper.assertTrue(mainframe.networkIndex().isManuallyLocked(iron),
                            "the hold did not come back");
                })
                .thenSucceed();
    }

    /*
     * A hold on a network holding nothing takes nothing, and taking nothing must leave nothing behind: an
     * empty holder would make every later Operation on that type wait for a hold on no storage at all.
     */
    @GameTest(template = ARENA)
    public static void manualLock_holdsNothingOnAnEmptyNetwork(final GameTestHelper helper) {
        final BlockPos at = new BlockPos(2, 2, 2);
        final MainframeBlockEntity mainframe = NetworkGameTests.placeRunningMainframe(helper, at);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final StorageKey iron = StorageKey.of(Items.IRON_INGOT);

                    helper.assertTrue(mainframe.networkIndex().manualLock(iron, 64L, null) == 0L,
                            "a hold on a network holding none of it held something");
                    helper.assertFalse(mainframe.networkIndex().isManuallyLocked(iron),
                            "a hold that took nothing was left behind as a hold");
                })
                .thenSucceed();
    }
}
