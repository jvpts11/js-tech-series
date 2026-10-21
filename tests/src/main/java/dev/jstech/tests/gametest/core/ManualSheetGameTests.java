/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.guide.ManualLecterns;
import dev.jstech.core.guide.TornSheets;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestGuide;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The manuals' things on the server: a torn sheet keeps which page it is a copy of through a save and a load, and a
 * manual used on an empty lectern is laid on it, the hand left empty, and Take gives it back, the lectern empty again.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ManualSheetGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos LECTERN = new BlockPos(1, 1, 1);

    private ManualSheetGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tornSheet_keepsItsPageThroughASaveAndALoad(final GameTestHelper helper) {
        final TornSheets.Sheet sheet = new TornSheets.Sheet(TestGuide.MANUAL, TestGuide.PRESS, 1, "1-4");
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final Tag saved = TornSheets.of(sheet).save(registries);
        final ItemStack read = ItemStack.parseOptional(registries, (CompoundTag) saved);
        helper.assertTrue(read.is(TornSheets.ITEM.get()), "a torn sheet reads back as one");
        helper.assertValueEqual(read.get(TornSheets.SHEET.get()), sheet, "the page it is a copy of");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void lectern_holdsAManualUsedOnItAndTakeGivesItBack(final GameTestHelper helper) {
        helper.setBlock(LECTERN, Blocks.LECTERN.defaultBlockState());
        final BlockPos at = helper.absolutePos(LECTERN);
        // A player in survival lays it down, so the manual leaves their hand, and takes it back.
        final FakePlayer reader = FakePlayerFactory.getMinecraft(helper.getLevel());
        reader.setGameMode(GameType.SURVIVAL);
        reader.getInventory().clearContent();
        reader.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TestGuide.MANUAL_ITEM.get()));
        final PlayerInteractEvent.RightClickBlock used = new PlayerInteractEvent.RightClickBlock(reader,
                InteractionHand.MAIN_HAND, at, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        ManualLecterns.onRightClick(used);
        helper.assertTrue(used.isCanceled(), "the lectern takes the manual in place of the game's use of it");
        helper.assertBlockProperty(LECTERN, LecternBlock.HAS_BOOK, true);
        helper.assertTrue(reader.getMainHandItem().isEmpty(), "the manual left the hand for the lectern");
        helper.assertTrue(ManualLecterns.takeOff(reader, helper.getLevel(), at), "Take takes the manual off");
        helper.assertBlockProperty(LECTERN, LecternBlock.HAS_BOOK, false);
        helper.assertTrue(reader.getInventory().contains(new ItemStack(TestGuide.MANUAL_ITEM.get())),
                "into the player's inventory");
        helper.assertFalse(ManualLecterns.takeOff(reader, helper.getLevel(), at), "an empty lectern gives nothing");
        reader.getInventory().clearContent();
        helper.succeed();
    }
}
