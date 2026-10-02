/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
 * through a save and on every player's screen. And what is behind it: the parts in the machine, which every player
 * who sees it is told of.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerCaseGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos WHERE = new BlockPos(2, 2, 2);
    private static final String SIDE_OFF = "SidePanelOff";
    private static final String INSTALLED = "Installed";

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

    /**
     * The players who see a computer are told which item is in each of its slots, so they see each part inside it:
     * as it goes in, as it comes out, and when the machine is read from the save before anything changes.
     */
    @GameTest(template = ARENA)
    public static void installedParts_areToldToThePlayersWhoSeeTheMachine(final GameTestHelper helper) {
        final PersonalComputerBlockEntity pc = place(helper, WHERE, ComputingModule.PERSONAL_COMPUTER.get(),
                PersonalComputerBlockEntity.class);
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final Item board = HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get();
        final Item cpu = HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get();
        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(cpu));

        helper.assertValueEqual(pc.installedPart(PersonalComputerBlockEntity.CPU_SLOT), id(cpu), "the processor");
        helper.assertTrue(pc.installedPart(PersonalComputerBlockEntity.PSU_SLOT) == null, "an empty slot holds none");
        final ListTag told = pc.getUpdateTag(registries).getList(INSTALLED, Tag.TAG_STRING);
        helper.assertValueEqual(told.size(), PersonalComputerBlockEntity.HARDWARE_SLOTS, "the slots the client hears");
        helper.assertValueEqual(told.getString(PersonalComputerBlockEntity.MOTHERBOARD_SLOT), id(board).toString(),
                "the board the client hears");

        pc.getHardware().setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, ItemStack.EMPTY);
        helper.assertTrue(pc.installedPart(PersonalComputerBlockEntity.CPU_SLOT) == null,
                "a part taken out is gone from what the client hears");
        final PersonalComputerBlockEntity loaded = new PersonalComputerBlockEntity(pc.getBlockPos(),
                pc.getBlockState());
        loaded.loadWithComponents(pc.saveWithFullMetadata(registries), registries);
        helper.assertValueEqual(loaded.installedPart(PersonalComputerBlockEntity.MOTHERBOARD_SLOT), id(board),
                "the board of a machine read from the save");
        helper.succeed();
    }

    private static ResourceLocation id(final Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
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
