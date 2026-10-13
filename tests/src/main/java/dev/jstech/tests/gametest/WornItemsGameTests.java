/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.core.integration.accessories.AccessoriesIntegration;
import dev.jstech.core.integration.curios.CuriosIntegration;
import dev.jstech.core.worn.WornItems;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What an entity wears, as the Core answers it: its armour with nothing installed, the slots of Curios or Accessories
 * when either is, and nothing at all for an empty slot.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WornItemsGameTests {

    private static final String ARENA = "empty";

    private WornItemsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void worn_countsTheArmourAndNoEmptySlot(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(WornItems.worn(player).isEmpty(), "a player wearing nothing wears nothing");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        final List<ItemStack> worn = WornItems.worn(player);
        helper.assertTrue(worn.size() == 2, "the helmet and the boots are worn; got " + worn);
        helper.assertTrue(WornItems.wears(player, stack -> stack.is(Items.IRON_HELMET)), "the helmet is found");
        helper.assertFalse(WornItems.wears(player, stack -> stack.is(Items.DIAMOND_HELMET)),
                "and nothing that is not worn");
        helper.assertTrue(WornItems.firstWorn(player, stack -> stack.is(Items.LEATHER_BOOTS)).isPresent(),
                "the first worn thing a test accepts is handed back");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void integrations_standDownWithoutTheirMods(final GameTestHelper helper) {
        helper.assertFalse(CuriosIntegration.isLoaded(), "the test run has no Curios");
        helper.assertFalse(AccessoriesIntegration.isLoaded(), "nor Accessories, so only armour is asked");
        helper.succeed();
    }
}
