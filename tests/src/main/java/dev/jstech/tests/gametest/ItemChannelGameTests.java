/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.storage.ItemChannel;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/** The item slots of a block face as a data channel: a simulated insert predicts what the real one places. */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ItemChannelGameTests {

    private static final String ARENA = "empty";

    private ItemChannelGameTests() {
    }

    /** A simulated insert reports the room of every slot, not just one stack, and the real insert then agrees. */
    @GameTest(template = ARENA)
    public static void insert_simulateReportsRoomOfEverySlot(final GameTestHelper helper) {
        final ItemStackHandler chest = new ItemStackHandler(4);
        final ItemChannel channel = new ItemChannel(chest);
        final StorageKey cobblestone = StorageKey.of(new ItemStack(Items.COBBLESTONE));
        helper.assertTrue(channel.insert(cobblestone, 200L, true) == 200L,
                "four empty slots have room for 200 cobblestone");
        helper.assertTrue(channel.insert(cobblestone, 1000L, true) == 256L,
                "the room is capped at the four stacks");
        helper.assertTrue(chest.getStackInSlot(0).isEmpty(), "simulating places nothing");
        helper.assertTrue(channel.insert(cobblestone, 200L, false) == 200L, "the real insert places the same");
        helper.succeed();
    }
}
