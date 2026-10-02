/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.menu.ClusterManagementComputerMenu;
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.computers.menu.PersonalComputerMenu;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A small computer's left side, the panel that comes off: it comes on, sneaking and using the case with an empty hand
 * takes it off and puts it back, so does the button on each machine's assembly screen, and it stays as it was left
 * through a save and on every player's screen.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerCaseGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    private static final String SIDE_OFF = "SidePanelOff";

    private ComputerCaseGameTests() {
    }

    @GameTest(template = ARENA)
    public static void sidePanel_offIsSavedAndSentToTheClient(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = place(helper, WHERE,
                ComputingModule.TRANSITION_PERSONAL_COMPUTER.get(), PersonalComputerBlockEntity.class);
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        helper.assertFalse(pc.sidePanelOff(), "a case comes with its side on");

        pc.toggleSidePanel();
        helper.assertTrue(pc.getUpdateTag(registries).getBoolean(SIDE_OFF), "the client hears the side is off");
        final PersonalComputerBlockEntity loaded = new PersonalComputerBlockEntity(pc.getBlockPos(),
                pc.getBlockState());
        loaded.loadWithComponents(pc.saveWithFullMetadata(registries), registries);
        helper.assertTrue(loaded.sidePanelOff(), "and it is still off when the world is loaded again");

        pc.toggleSidePanel();
        helper.assertFalse(pc.getUpdateTag(registries).getBoolean(SIDE_OFF), "putting it back is heard too");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void sneakingWithAnEmptyHand_takesTheSideOffAndPutsItBack(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = place(helper, WHERE, ComputingModule.CRAFTING_COMPUTER.get(),
                CraftingComputerBlockEntity.class);
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        player.setShiftKeyDown(true);
        final BlockPos at = helper.absolutePos(WHERE);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.EAST, at, false);
        try {
            helper.useBlock(WHERE, player, hit);
            helper.assertTrue(computer.sidePanelOff(), "sneaking and using the case takes its side off");
            helper.useBlock(WHERE, player, hit);
            helper.assertFalse(computer.sidePanelOff(), "and again puts it back");
        } finally {
            player.setShiftKeyDown(false);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void assemblyButton_takesTheSideOffOnEveryMachine(final GameTestHelper helper) {
        final FakePlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
        final PersonalComputerBlockEntity pc = place(helper, new BlockPos(1, 2, 2),
                ComputingModule.HIGH_PERFORMANCE_PERSONAL_COMPUTER.get(), PersonalComputerBlockEntity.class);
        final CraftingComputerBlockEntity crafting = place(helper, new BlockPos(3, 2, 2),
                ComputingModule.VINTAGE_CRAFTING_COMPUTER.get(), CraftingComputerBlockEntity.class);
        final ClusterManagementComputerBlockEntity cmc = place(helper, new BlockPos(5, 2, 2),
                ComputingModule.ADVANCED_AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER.get(),
                ClusterManagementComputerBlockEntity.class);

        new PersonalComputerMenu(0, player.getInventory(), pc)
                .clickMenuButton(player, PersonalComputerMenu.BUTTON_SIDE_PANEL);
        new CraftingComputerMenu(0, player.getInventory(), crafting)
                .clickMenuButton(player, CraftingComputerMenu.BUTTON_SIDE_PANEL);
        new ClusterManagementComputerMenu(0, player.getInventory(), cmc)
                .clickMenuButton(player, ClusterManagementComputerMenu.BUTTON_SIDE_PANEL);
        for (final AbstractSmallComputerBlockEntity computer : new AbstractSmallComputerBlockEntity[] {pc, crafting,
                cmc}) {
            helper.assertTrue(computer.sidePanelOff(),
                    "the screen's button takes the side off the " + computer.getBlockState().getBlock());
        }
        helper.succeed();
    }

    private static <T extends AbstractSmallComputerBlockEntity> T place(final GameTestHelper helper,
                                                                      final BlockPos pos, final Block block,
                                                                      final Class<T> type) {
        helper.setBlock(pos, block);
        if (!type.isInstance(helper.getBlockEntity(pos))) {
            throw new IllegalStateException("no " + type.getSimpleName() + " at " + pos);
        }
        return type.cast(helper.getBlockEntity(pos));
    }
}
