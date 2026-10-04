/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.computers.hardware.WorkshopCardSpec;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.os.devices.DeviceMaps;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.computers.workshop.WorkshopRates;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The personal-use cards and the Workshop that works them: where the cards seat, what each does with a player's items
 * (the game's own crafting, smelting, enchanting and anvil rules at the cards' price and pace), the furnace going on
 * with the window shut and its contents falling out of a broken computer, and the program kept to the Personal
 * Computer.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class WorkshopGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final BlockPos PC = new BlockPos(2, 2, 2);
    /** The card slots after the graphics card in the first. */
    private static final int FIRST_FREE_CARD_SLOT = PersonalComputerBlockEntity.GPU_SLOTS_START + 1;

    private WorkshopGameTests() {
    }

    /** The cards seat in a Personal Computer, on any slot from PCI on, and never in another kind of computer. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void cards_seatOnlyInAPersonalComputerFromThePciOn(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(PC);
        final CraftingComputerBlockEntity cc = world.placeRunningCraftingComputer(new BlockPos(5, 2, 2));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final ItemStack furnace = new ItemStack(ComputingModule.FURNACE_CARD.get());
                    helper.assertTrue(pc.isValidForSlot(FIRST_FREE_CARD_SLOT, furnace),
                            "a Personal Computer takes the Furnace Card");
                    helper.assertFalse(cc.isValidForSlot(CraftingComputerBlockEntity.PCIE_SLOTS_START + 2, furnace),
                            "a Crafting Computer has no program for it and refuses it");
                    final WorkshopCardSpec spec = new WorkshopCardSpec(WorkshopCard.FURNACE, 15);
                    helper.assertFalse(spec.fits(PcieGeneration.ISA), "no ISA board, the Vintage's, takes it");
                    helper.assertTrue(spec.fits(PcieGeneration.PCI) && spec.fits(PcieGeneration.AGP_8X)
                            && spec.fits(PcieGeneration.PCIE_3_0), "every bus from PCI on takes it");
                })
                .thenSucceed();
    }

    /** The program lives on a Personal Computer, from the Legacy on, on every windowed system but Frames 95. */
    @GameTest(template = ARENA)
    public static void program_isThePersonalComputersFromTheLegacyOn(final GameTestHelper helper) {
        final ProgramSpec spec = OsRegistry.getProgram(Programs.WORKSHOP);
        helper.assertTrue(spec != null, "the Workshop is a program");
        helper.assertTrue(spec.hostScope() == HostScope.PERSONAL_COMPUTER, "only on a Personal Computer");
        helper.assertTrue(spec.minEra() == HardwareEra.LEGACY, "from the Legacy on");
        helper.assertTrue(spec.minOsRank() == 2, "Frames XP or newer among the Frames");
        helper.succeed();
    }

    /** The grid crafts with the game's recipes, and Craft all refills it from the inventory until it runs out. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void crafting_craftsAllTheInventoryAllows(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.CRAFTING_TABLE);
                    final ServerPlayer player = player(helper);
                    final Workshop workshop = pc.workshop();
                    for (int i = 0; i < 3; i++) {
                        workshop.put(Workshop.GRID + i, new ItemStack(Items.IRON_INGOT));
                    }
                    workshop.put(Workshop.GRID + 4, new ItemStack(Items.STICK));
                    workshop.put(Workshop.GRID + 7, new ItemStack(Items.STICK));
                    player.getInventory().add(new ItemStack(Items.IRON_INGOT, 6));
                    player.getInventory().add(new ItemStack(Items.STICK, 4));
                    helper.assertTrue(workshop.craftResult(helper.getLevel()).is(Items.IRON_PICKAXE),
                            "the grid makes an iron pickaxe");
                    helper.assertTrue(workshop.moreFromInventory(player) == 2, "the inventory has two more in it");
                    helper.assertTrue(workshop.craft(player, true) == 3, "Craft all makes three");
                    helper.assertTrue(player.getInventory().countItem(Items.IRON_PICKAXE) == 3,
                            "the three pickaxes are in the inventory");
                    helper.assertTrue(player.getInventory().countItem(Items.IRON_INGOT) == 0
                            && workshop.slot(Workshop.GRID).isEmpty(), "every ingot was used");
                    leave(helper, player);
                })
                .thenSucceed();
    }

    /** The furnace smelts at the computer's pace with no fuel, and only while the computer is on. */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void furnace_smeltsWhileTheComputerIsOn(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(PC);
        final int perItem = WorkshopRates.ticksPerItem(200, WorkshopRates.furnaceSpeed(HardwareEra.STANDARD));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.FURNACE);
                    pc.workshop().put(Workshop.FURNACE_IN, new ItemStack(Items.RAW_IRON, 2));
                })
                .thenWaitUntil(() -> helper.assertTrue(pc.workshop().slot(Workshop.FURNACE_OUT).is(Items.IRON_INGOT)
                                && pc.workshop().slot(Workshop.FURNACE_OUT).getCount() == 2,
                        "two iron ingots at four times a furnace's pace; got "
                                + pc.workshop().slot(Workshop.FURNACE_OUT)))
                .thenExecute(() -> {
                    helper.assertTrue(pc.workshop().experienceEarned() > 0f, "and the experience they earned");
                    pc.workshop().put(Workshop.FURNACE_IN, new ItemStack(Items.RAW_IRON, 1));
                    pc.togglePower();
                })
                .thenExecuteAfter(perItem + 10, () -> helper.assertTrue(
                        pc.workshop().slot(Workshop.FURNACE_IN).getCount() == 1,
                        "with the computer off nothing smelts"))
                .thenSucceed();
    }

    /** What the furnace holds is the player's, so it falls out when the computer is broken. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void furnace_contentsFallOutOfABrokenComputer(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    pc.workshop().put(Workshop.FURNACE_IN, new ItemStack(Items.RAW_GOLD, 5));
                    helper.setBlock(PC, Blocks.AIR);
                })
                .thenExecuteAfter(2, () -> {
                    final List<ItemEntity> dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                            new AABB(helper.absolutePos(PC)).inflate(2));
                    helper.assertTrue(dropped.stream().anyMatch(item -> item.getItem().is(Items.RAW_GOLD)
                            && item.getItem().getCount() == 5), "the five raw gold fell out");
                })
                .thenSucceed();
    }

    /** The three offers of a full table, for one or two levels and no lapis. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void enchanting_takesFewerLevelsAndNoLapis(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.ENCHANTING);
                    final ServerPlayer player = player(helper);
                    player.setExperienceLevels(40);
                    pc.workshop().put(Workshop.ENCHANT_ITEM, new ItemStack(Items.DIAMOND_SWORD));
                    final List<Workshop.Offer> offers = pc.workshop().offers(player);
                    helper.assertTrue(offers.size() == 3 && offers.get(2).required() >= 20,
                            "three offers, the last a full table's; got " + offers);
                    helper.assertTrue(offers.get(0).levels() == 1 && offers.get(2).levels() == 2,
                            "they cost 1 and 2 levels");
                    helper.assertTrue(pc.workshop().enchant(player, 2), "the third offer is taken");
                    helper.assertTrue(pc.workshop().slot(Workshop.ENCHANT_ITEM).isEnchanted(),
                            "the sword is enchanted");
                    helper.assertTrue(player.experienceLevel == 38, "two levels, no lapis; level "
                            + player.experienceLevel);
                    leave(helper, player);
                })
                .thenSucceed();
    }

    /** The anvil's repair at two thirds of its levels, and the card never wears. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void anvil_repairsForTwoThirdsOfTheLevels(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(pc, WorkshopCard.ANVIL);
                    final ServerPlayer player = player(helper);
                    player.setExperienceLevels(30);
                    final ItemStack worn = new ItemStack(Items.DIAMOND_PICKAXE);
                    worn.setDamageValue(worn.getMaxDamage() - 100);
                    pc.workshop().put(Workshop.ANVIL_LEFT, worn);
                    pc.workshop().put(Workshop.ANVIL_RIGHT, new ItemStack(Items.DIAMOND, 3));
                    final Workshop.AnvilResult result = pc.workshop().anvil(player);
                    helper.assertTrue(!result.result().isEmpty() && result.anvilLevels() > 0,
                            "the diamonds repair it");
                    helper.assertTrue(result.levels() == WorkshopRates.anvilLevels(result.anvilLevels()),
                            "for two thirds of the anvil's levels");
                    helper.assertTrue(pc.workshop().takeAnvil(player), "it is taken");
                    helper.assertTrue(player.experienceLevel == 30 - result.levels(),
                            "the card's levels were paid; level " + player.experienceLevel);
                    helper.assertTrue(player.getInventory().countItem(Items.DIAMOND_PICKAXE) == 1,
                            "the repaired pickaxe is in the inventory");
                    helper.assertTrue(pc.workshop().slot(Workshop.ANVIL_LEFT).isEmpty()
                            && pc.workshop().slot(Workshop.ANVIL_RIGHT).getCount() < 3, "the inputs were used");
                    leave(helper, player);
                })
                .thenSucceed();
    }

    /** With no card in, the computer offers nothing; with them, the Device Manager draws each by its own picture. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void cards_areWhatTheComputerOffersAndShows(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(PC);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(pc.workshopCards() == 0, "no card, nothing offered");
                    install(pc, WorkshopCard.CRAFTING_TABLE);
                    install(pc, WorkshopCard.ANVIL);
                })
                .thenExecuteAfter(2, () -> {
                    helper.assertTrue(WorkshopCard.CRAFTING_TABLE.in(pc.workshopCards())
                            && WorkshopCard.ANVIL.in(pc.workshopCards())
                            && !WorkshopCard.FURNACE.in(pc.workshopCards()), "the two cards installed are offered");
                    final DeviceMap map = DeviceMaps.of(helper.getLevel(), pc);
                    helper.assertTrue(map.cardIcons().contains("crafting_table_card")
                            && map.cardIcons().contains("anvil_card"), "each card wears its picture; got "
                            + map.cardIcons());
                })
                .thenSucceed();
    }

    /* Puts {@code card} in the first free card slot of {@code pc}. */
    private static void install(final PersonalComputerBlockEntity pc, final WorkshopCard card) {
        final ItemStack stack = new ItemStack(switch (card) {
            case CRAFTING_TABLE -> ComputingModule.CRAFTING_TABLE_CARD.get();
            case FURNACE -> ComputingModule.FURNACE_CARD.get();
            case ENCHANTING -> ComputingModule.ENCHANTING_CARD.get();
            case ANVIL -> ComputingModule.ANVIL_CARD.get();
        });
        for (int slot = FIRST_FREE_CARD_SLOT; slot < PersonalComputerBlockEntity.GPU_SLOTS_START
                + PersonalComputerBlockEntity.GPU_SLOTS; slot++) {
            if (pc.getHardware().getStackInSlot(slot).isEmpty()) {
                pc.getHardware().setStackInSlot(slot, stack);
                return;
            }
        }
        throw new IllegalStateException("no free card slot");
    }

    /*
     * A player of its own for each test, a fake one: a mock player logged into the server would be sent the data the
     * mods sync on login, which a test connection cannot take.
     */
    private static ServerPlayer player(final GameTestHelper helper) {
        final ServerPlayer player = FakePlayerFactory.get(helper.getLevel(),
                new GameProfile(UUID.randomUUID(), "workshop-test"));
        player.getInventory().clearContent();
        player.setExperienceLevels(0);
        return player;
    }

    private static void leave(final GameTestHelper helper, final ServerPlayer player) {
        player.getInventory().clearContent();
    }
}
