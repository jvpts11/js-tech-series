/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.core.jstechseries.SeriesModules;
import dev.jstech.core.jstechseries.tier.IndustrialTier;
import dev.jstech.core.module.Modules;
import dev.jstech.industrial.config.IndustrialServerConfig;
import dev.jstech.industrial.tool.IndustrialModules;
import dev.jstech.industrial.tool.NoEnchanting;
import dev.jstech.industrial.tool.ToolKinds;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestIndustrialGear;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * J's Industrial's tools and armour: their sockets by tier; a module one piece takes at most so many of; a module that
 * needs a piece or a seal, so a space module fits only a sealed suit; nothing of the mod enchanted at the table or the
 * anvil; and a kind of tool a world leaves out, out of the creative tab and its recipes gone.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialGearGameTests {

    private static final String ARENA = "empty";
    private static final int TABLE_LEVEL = 30;
    private static final int POWER = 15;
    private static final long COST = 5;

    private IndustrialGearGameTests() {
    }

    @GameTest(template = ARENA)
    public static void sockets_followTheTier(final GameTestHelper helper) {
        helper.assertTrue(IndustrialModules.sockets(IndustrialTier.T1) == 2
                && IndustrialModules.sockets(IndustrialTier.T3) == 2, "two up to the T3");
        helper.assertTrue(IndustrialModules.sockets(IndustrialTier.T4) == 3, "three at the T4");
        helper.assertTrue(IndustrialModules.sockets(IndustrialTier.T5) == 4
                && IndustrialModules.sockets(IndustrialTier.T6) == 4, "four at the T5 and T6");
        helper.assertTrue(IndustrialModules.sockets(IndustrialTier.T7) == 5
                && IndustrialModules.sockets(IndustrialTier.T9) == 5, "five from the T7");
        helper.assertTrue(TestIndustrialGear.OPEN_CHEST_DEF.kinds().size() == 2
                        && TestIndustrialGear.SEALED_CHEST_DEF.kinds().size() == 4
                        && TestIndustrialGear.SWORD_DEF.kinds().size() == 2,
                "an open frame of the T3 has two, a sealed suit of the T5 four, a tool of the T2 two");
        helper.assertTrue(TestIndustrialGear.SEALED_CHEST_DEF.traits().contains(SeriesModules.SEALED)
                && !TestIndustrialGear.OPEN_CHEST_DEF.traits().contains(SeriesModules.SEALED)
                && TestIndustrialGear.SEALED_HELMET_DEF.traits().contains(SeriesModules.HEAD),
                "each says its piece, and the closed suit that it is sealed");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void module_takesNoMoreThanItsMostInOnePiece(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ItemStack chest = new ItemStack(TestIndustrialGear.SEALED_CHEST.get());
        final ItemStack plates = new ItemStack(TestIndustrialGear.PLATE.get(), 3);
        helper.assertTrue(Modules.install(level, chest, 0, plates) && Modules.install(level, chest, 1, plates),
                "two plates go in");
        helper.assertFalse(Modules.install(level, chest, 2, plates), "a third does not, though a socket is free");
        helper.assertTrue(plates.getCount() == 1, "and stays in the hand");
        Modules.remove(level, chest, 0);
        helper.assertTrue(Modules.install(level, chest, 2, plates), "one taken out, another goes in");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void module_fitsOnlyTheHostItNeeds(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ItemStack open = new ItemStack(TestIndustrialGear.OPEN_CHEST.get());
        final ItemStack sealed = new ItemStack(TestIndustrialGear.SEALED_CHEST.get());
        final ItemStack helmet = new ItemStack(TestIndustrialGear.SEALED_HELMET.get());
        helper.assertFalse(Modules.install(level, open, 0, new ItemStack(TestIndustrialGear.SPACE.get())),
                "an open frame can't be sealed: the space module stays out");
        helper.assertTrue(Modules.install(level, sealed, 0, new ItemStack(TestIndustrialGear.SPACE.get())),
                "a sealed suit takes it");
        helper.assertFalse(Modules.install(level, sealed, 1, new ItemStack(TestIndustrialGear.FILTER.get())),
                "a filter is a head's");
        helper.assertTrue(Modules.install(level, helmet, 0, new ItemStack(TestIndustrialGear.FILTER.get())),
                "and goes in a helmet");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void noEnchanting_keepsTheTableAndTheAnvilOff(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ItemStack sword = new ItemStack(TestIndustrialGear.SWORD.get());
        final ItemStack vanilla = new ItemStack(Items.IRON_SWORD);
        helper.assertTrue(NoEnchanting.refuses(sword) && !NoEnchanting.refuses(vanilla), "the mod's sword, not the "
                + "game's");
        final EnchantmentLevelSetEvent table = NeoForge.EVENT_BUS.post(new EnchantmentLevelSetEvent(level,
                BlockPos.ZERO, 2, POWER, sword, TABLE_LEVEL));
        helper.assertTrue(table.getEnchantLevel() == 0, "the table offers it nothing");
        final EnchantmentLevelSetEvent game = NeoForge.EVENT_BUS.post(new EnchantmentLevelSetEvent(level,
                BlockPos.ZERO, 2, POWER, vanilla, TABLE_LEVEL));
        helper.assertTrue(game.getEnchantLevel() == TABLE_LEVEL, "and the game's sword its levels");
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        final ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS),
                1);
        book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        helper.assertTrue(NeoForge.EVENT_BUS.post(new AnvilUpdateEvent(sword, book, "", COST, player)).isCanceled(),
                "the anvil takes no book onto it");
        helper.assertFalse(NeoForge.EVENT_BUS.post(new AnvilUpdateEvent(sword, ItemStack.EMPTY, "Blade", COST,
                player)).isCanceled(), "though it still renames it");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void toolKind_leftOutLeavesTheTabAndItsRecipes(final GameTestHelper helper) {
        final ItemStack sword = new ItemStack(TestIndustrialGear.SWORD.get());
        helper.assertTrue(sword.is(ToolKinds.SWORDS.items()) && !ToolKinds.leftOut(sword)
                && ToolKinds.SWORDS.condition().test(ICondition.IContext.EMPTY), "while swords are made");
        IndustrialServerConfig.FILE.set(IndustrialServerConfig.SWORDS, false);
        try {
            helper.assertTrue(ToolKinds.leftOut(sword), "left out, a sword leaves the creative tab");
            helper.assertFalse(ToolKinds.SWORDS.condition().test(ICondition.IContext.EMPTY),
                    "and the condition its recipes carry fails");
            helper.assertFalse(ToolKinds.leftOut(new ItemStack(Items.IRON_SWORD)), "the game's sword stays");
        } finally {
            IndustrialServerConfig.FILE.set(IndustrialServerConfig.SWORDS, true);
        }
        helper.succeed();
    }
}
