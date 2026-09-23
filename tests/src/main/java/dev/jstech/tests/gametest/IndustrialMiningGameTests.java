/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The machines mined as a survival player mines them: they need the right tool to come away, and a pickaxe is it.
 * Without the tag that says so, no tool was right and a machine broken in survival dropped nothing at all.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialMiningGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 4;

    private IndustrialMiningGameTests() {
    }

    @GameTest(template = ARENA)
    public static void machine_minedWithAPickaxe_dropsItself(final GameTestHelper helper) {
        final List<BlockEntry<?>> machines = List.of(IndustrialModule.MACERATOR, IndustrialModule.ELECTRIC_FURNACE,
                IndustrialModule.COMPRESSOR, IndustrialModule.COAL_GENERATOR);
        for (int i = 0; i < machines.size(); i++) {
            final BlockPos pos = new BlockPos(1 + 2 * i, 2, 2);
            helper.setBlock(pos, machines.get(i).get());
            mine(helper, pos, new ItemStack(Items.IRON_PICKAXE));
        }
        helper.runAfterDelay(SETTLE, () -> {
            for (int i = 0; i < machines.size(); i++) {
                helper.assertItemEntityPresent(machines.get(i).item(), new BlockPos(1 + 2 * i, 2, 2), 1.0);
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void machine_minedByHand_dropsNothing(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, IndustrialModule.MACERATOR.get());
        mine(helper, pos, ItemStack.EMPTY);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertBlockNotPresent(IndustrialModule.MACERATOR.get(), pos);
            helper.assertItemEntityNotPresent(IndustrialModule.MACERATOR.item(), pos, 3.0);
            helper.succeed();
        });
    }

    /** Breaks the block the way a survival player does, holding that. */
    private static void mine(final GameTestHelper helper, final BlockPos pos, final ItemStack held) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        try {
            player.gameMode.destroyBlock(helper.absolutePos(pos));
        } finally {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }
}
