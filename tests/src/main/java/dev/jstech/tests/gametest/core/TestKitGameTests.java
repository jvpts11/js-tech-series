/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.multiblock.IMatchResult;
import dev.jstech.core.jstechseries.operation.OperationStatus;
import dev.jstech.core.jstechseries.operation.SelfTestOperationTask;
import dev.jstech.core.optional.OptionalMod;
import dev.jstech.core.team.Ownership;
import dev.jstech.testkit.gametest.Arenas;
import dev.jstech.testkit.gametest.Costs;
import dev.jstech.testkit.gametest.Expect;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.testkit.gametest.GameTestTimes;
import dev.jstech.testkit.gametest.OldSaves;
import dev.jstech.testkit.gametest.OptionalMods;
import dev.jstech.testkit.gametest.SaveRoundTrip;
import dev.jstech.testkit.gametest.StallMonitor;
import dev.jstech.testkit.gametest.TestChance;
import dev.jstech.testkit.gametest.TestTime;
import dev.jstech.testkit.kit.EnergyScenes;
import dev.jstech.testkit.kit.HazardScenes;
import dev.jstech.testkit.kit.MachineScenes;
import dev.jstech.testkit.kit.MenuScenes;
import dev.jstech.testkit.kit.MultiblockScenes;
import dev.jstech.testkit.kit.NetworkScenes;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestHazards;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestMultiblocks;
import dev.jstech.tests.TestNetworkMembers;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's test kit, used as a mod built on the Core uses it: arenas made in code, a round trip through the save,
 * ticks run at once, chance with a seed, assertions that explain, the cost of a tick and its limit, a mod seen absent,
 * old saves read back, the scenes of the network, its Operations, energy, machines, multiblocks, hazards and menus,
 * and the report of time and of a stall.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TestKitGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    private static final int SMELT_TICKS = 220;

    private TestKitGameTests() {
    }

    @GameTest(template = ARENA)
    public static void arenas_areMadeInCodeAndFoundAsTemplates(final GameTestHelper helper) {
        final ResourceLocation empty = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, ARENA);
        helper.assertTrue(Arenas.declared(empty), "the series' arena is declared in code");
        final Optional<StructureTemplate> template = helper.getLevel().getStructureManager().get(empty);
        helper.assertTrue(template.isPresent() && template.get().getSize().getX() == 9
                && template.get().getSize().getY() == 6, "and the game finds it as a template, 9 by 6 by 9");
        boolean refused = false;
        try {
            Arenas.declare(JsTests.MODID, ARENA, 3, 3, 3);
        } catch (final IllegalArgumentException expected) {
            refused = true;
        }
        helper.assertTrue(refused, "declared again with another size, it is refused");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void saveRoundTrip_readsABlockEntityBackAsItWasSaved(final GameTestHelper helper) {
        helper.setBlock(AT, Blocks.CHEST);
        final ChestBlockEntity chest = helper.getBlockEntity(AT);
        chest.setItem(4, new ItemStack(Items.EMERALD, 12));
        final SaveRoundTrip.Trip trip = SaveRoundTrip.of(helper, AT);
        trip.assertSame(helper, AT);
        final ChestBlockEntity read = trip.read(ChestBlockEntity.class);
        helper.assertTrue(read != chest && helper.getBlockEntity(AT) == read, "a new block entity stands in its place");
        helper.assertTrue(read.getItem(4).is(Items.EMERALD) && read.getItem(4).getCount() == 12, "with what it held");
        final CompoundTag before = trip.before().copy();
        before.putString("CustomName", "\"Box\"");
        helper.assertTrue(SaveRoundTrip.differences(before, trip.after()).getFirst().startsWith("CustomName:"),
                "a field that differs is named: " + SaveRoundTrip.differences(before, trip.after()));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void saveRoundTrip_aMemberReadBackJoinsItsNetworkAgain(final GameTestHelper helper) {
        final NetworkScenes.Network network = NetworkScenes.lay(helper, AT);
        helper.setBlock(AT.west(), TestNetworkMembers.REACTOR.get());
        helper.startSequence()
                .thenWaitUntil(() -> network.expectMembers(helper, 1))
                .thenExecute(() -> SaveRoundTrip.of(helper, AT.west()))
                .thenWaitUntil(() -> helper.assertTrue(network.members().size() == 1
                        && network.members().getFirst().pos() == helper.absolutePos(AT.west()).asLong(),
                        "read back, the reactor is on its network again"))
                .thenExecute(network::close)
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void time_runsABlockEntitysTicksAtOnce(final GameTestHelper helper) {
        helper.setBlock(AT, Blocks.FURNACE);
        final FurnaceBlockEntity furnace = helper.getBlockEntity(AT);
        furnace.setItem(0, new ItemStack(Items.RAW_IRON));
        furnace.setItem(1, new ItemStack(Items.COAL));
        helper.assertTrue(TestTime.advance(helper, AT, SMELT_TICKS) == SMELT_TICKS, "every tick ran");
        Expect.equal(helper, AT, "what the furnace made in one tick of the test", Items.IRON_INGOT,
                furnace.getItem(2).getItem());
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void chance_aSeedGivesTheSameRollsEveryTime(final GameTestHelper helper) {
        final int first = TestChance.seeded(helper.getLevel(), 42L, () -> rolls(helper.getLevel().random));
        final int again = TestChance.seeded(helper.getLevel(), 42L, () -> rolls(helper.getLevel().random));
        helper.assertTrue(first == again, "the level's random, seeded the same, rolls the same: " + first + ", "
                + again);
        helper.assertTrue(rolls(TestChance.source(7L)) == rolls(TestChance.source(7L)),
                "and a source of its own does too");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void expect_saysWhatWasExpectedFoundAndWhere(final GameTestHelper helper) {
        try {
            Expect.equal(helper, AT, "the kiln's output", 3, 2);
            throw new IllegalStateException("a wrong value passed");
        } catch (final GameTestAssertPosException failure) {
            helper.assertTrue(failure.getMessage().startsWith("the kiln's output: expected 3, found 2 at ")
                    && failure.getRelativePos().equals(AT), "it says what, both values and where: "
                    + failure.getMessage());
        }
        try {
            Expect.near(helper, AT, "the heat", 300.0, 310.0, 1.0);
            throw new IllegalStateException("a value too far passed");
        } catch (final GameTestAssertPosException failure) {
            helper.assertTrue(failure.getMessage().contains("off by 10.0"), "and how far off: "
                    + failure.getMessage());
        }
        Expect.near(helper, AT, "the heat", 300.0, 300.5, 1.0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void costs_measureATickAndFailPastTheirLimit(final GameTestHelper helper) {
        helper.setBlock(AT, Blocks.FURNACE);
        final double cost = Costs.below(helper, AT, 10_000.0);
        helper.assertTrue(cost > 0.0, "a furnace's tick costs something: " + cost);
        try {
            Costs.below(helper, AT, 0.0);
            throw new IllegalStateException("a tick passed a limit of nothing");
        } catch (final GameTestAssertPosException failure) {
            helper.assertTrue(failure.getMessage().contains("more than its limit"), "past its limit, it fails: "
                    + failure.getMessage());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void optionalMods_aModIsSeenAbsentWhileTheTestAsks(final GameTestHelper helper) {
        final OptionalMod jei = OptionalMod.of("jei");
        helper.assertTrue(jei.isLoaded(), "the run has JEI");
        helper.assertTrue(!OptionalMods.without("jei", jei::isLoaded), "a guard sees it absent while it is hidden");
        helper.assertTrue(jei.isLoaded(), "and installed again after");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void oldSaves_aPieceSavedIsReadBackThroughTheGamesUpgrades(final GameTestHelper helper) {
        helper.setBlock(AT, Blocks.CHEST);
        final ChestBlockEntity chest = helper.getBlockEntity(AT);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));
        final CompoundTag saved = OldSaves.capture(helper, AT, AT.east());
        helper.setBlock(AT, Blocks.AIR);
        final List<BlockEntity> placed = OldSaves.place(helper, saved, AT.above());
        helper.assertTrue(placed.size() == 1 && placed.getFirst() instanceof ChestBlockEntity box
                && box.getItem(0).is(Items.DIAMOND) && box.getItem(0).getCount() == 5,
                "the chest comes back with what it held: " + placed);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void oldSaves_everySampleOfAnEarlierVersionLoads(final GameTestHelper helper) {
        final List<OldSaves.Sample> samples = OldSaves.samples(JsTests.MODID, "old_saves");
        helper.assertTrue(!samples.isEmpty(), "the series keeps samples of its earlier versions");
        for (final OldSaves.Sample sample : samples) {
            final List<BlockEntity> placed = OldSaves.place(helper, sample, new BlockPos(1, 1, 1));
            helper.assertTrue(placed.stream().anyMatch(TestKitMachine.KitMachineBlockEntity.class::isInstance),
                    sample + " brings its machine back: " + placed);
            final TestKitMachine.KitMachineBlockEntity machine = placed.stream()
                    .filter(TestKitMachine.KitMachineBlockEntity.class::isInstance)
                    .map(TestKitMachine.KitMachineBlockEntity.class::cast).findFirst().orElseThrow();
            helper.assertTrue(machine.getInventory().getStackInSlot(0).is(Items.COBBLESTONE),
                    sample + "'s machine holds its input");
            for (final BlockEntity entity : placed) {
                helper.setBlock(helper.relativePos(entity.getBlockPos()), Blocks.AIR);
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void oldSaves_aSampleIsKeptOnlyWhenTheRunAsks(final GameTestHelper helper) {
        helper.setBlock(AT, TestKitMachine.BLOCK.get());
        final TestKitMachine.KitMachineBlockEntity machine = helper.getBlockEntity(AT);
        machine.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 8));
        helper.setBlock(AT.east(), Blocks.CHEST);
        final Optional<Path> kept = OldSaves.keep(helper, JsTests.MODID, "kit_machine", AT, AT.east());
        helper.assertTrue(kept.isPresent() == !System.getProperty(OldSaves.FOLDER_PROPERTY, "").isBlank(),
                "kept when the run names a folder, and only then: " + kept);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void networkScenes_giveARequestToAMemberAndFollowAnOperation(final GameTestHelper helper) {
        final NetworkScenes.Network network = NetworkScenes.lay(helper, AT, AT.east());
        helper.setBlock(AT.west(), TestNetworkMembers.ACCELERATOR.get());
        final NetworkScenes.Dispatch dispatch = NetworkScenes.dispatch(helper, 1);
        final UUID[] queued = new UUID[1];
        helper.startSequence()
                .thenWaitUntil(() -> network.expectMembers(helper, 1))
                .thenExecute(() -> Expect.equal(helper, AT, "a request given to the accelerator",
                        OperationStatus.COMPLETED, NetworkScenes.send(helper, network, TestNetworkMembers.ACCELERATE,
                                new TestNetworkMembers.AccelerateArgs())))
                .thenExecute(() -> queued[0] = dispatch.submit(new SelfTestOperationTask(1_000)))
                .thenWaitUntil(() -> dispatch.expect(helper, AT, queued[0], OperationStatus.COMPLETED))
                .thenExecute(() -> {
                    dispatch.close();
                    network.close();
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void machineScenes_runARecipeToItsEnd(final GameTestHelper helper) {
        helper.setBlock(AT, TestKitMachine.BLOCK.get());
        final TestKitMachine.KitMachineBlockEntity machine = helper.getBlockEntity(AT);
        final EnergyScenes.Gauge gauge = EnergyScenes.gauge(helper, AT);
        helper.assertTrue(EnergyScenes.fill(helper, AT, 100L) > 0L && gauge.gained() > 0L,
                "energy put in is read back through its port");
        MachineScenes.runRecipe(helper, machine, outputs -> Expect.equal(helper, AT, "what the machine made",
                Items.STONE, outputs.getFirst().getItem()), new ItemStack(Items.COBBLESTONE, 2));
    }

    @GameTest(template = ARENA)
    public static void multiblockScenes_buildAPatternAndSayWhereItFails(final GameTestHelper helper) {
        final Block part = TestMultiblocks.PART.get();
        MultiblockScenes.build(helper, TestMultiblocks.FURNACE, AT, Map.of('#', TestMultiblocks.CONTROLLER.get(),
                'P', part, 'I', part, 'O', part, 'E', part, 'G', part));
        final IMatchResult.Success formed = MultiblockScenes.expectForms(helper, TestMultiblocks.FURNACE, AT);
        helper.assertTrue(formed.ports().size() == 4, "built, it stands with its four ports");
        helper.setBlock(AT.north(), Blocks.STONE);
        try {
            MultiblockScenes.expectForms(helper, TestMultiblocks.FURNACE, AT);
            throw new IllegalStateException("a broken pattern stood");
        } catch (final GameTestAssertPosException failure) {
            helper.assertTrue(failure.getMessage().contains("finds minecraft:stone"), "it says what it found where: "
                    + failure.getMessage());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void hazardScenes_doseABodyAndCheckItsBand(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.join(helper, "dosed");
        try {
            HazardScenes.expectBand(helper, player, TestHazards.KIND, null);
            HazardScenes.dose(player, TestHazards.KIND, 8.0);
            HazardScenes.expectBand(helper, player, TestHazards.KIND, TestHazards.ITCHY);
            HazardScenes.dose(player, TestHazards.KIND, 15.0);
            HazardScenes.expectBand(helper, player, TestHazards.KIND, TestHazards.SORE);
            try {
                HazardScenes.expectBand(helper, player, TestHazards.KIND, TestHazards.ITCHY);
                throw new IllegalStateException("the wrong band passed");
            } catch (final GameTestAssertPosException failure) {
                helper.assertTrue(failure.getMessage().contains("found Sore"), "it says the band it is in: "
                        + failure.getMessage());
            }
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void menuScenes_openAMachineAndPressItsSwitchAsAPlayer(final GameTestHelper helper) {
        helper.setBlock(AT, TestKitMachine.BLOCK.get());
        final TestKitMachine.KitMachineBlockEntity machine = helper.getBlockEntity(AT);
        final ServerPlayer player = GameTestPlayers.connected(helper, "operator");
        try {
            machine.own(Ownership.of(player.getUUID()));
            player.teleportTo(helper.absoluteVec(AT.getCenter()).x, helper.absolutePos(AT).getY() + 1,
                    helper.absoluteVec(AT.getCenter()).z);
            MenuScenes.open(helper, player, AT, MachineMenu.class);
            helper.assertTrue(MenuScenes.press(player, MachineMenu.SWITCH) && !machine.control().on(),
                    "the switch pressed turns the machine off");
            MenuScenes.close(player);
            helper.assertTrue(player.containerMenu == player.inventoryMenu, "and the menu closes");
        } finally {
            GameTestPlayers.leave(player);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void reports_sayTheSlowestTestsAndWhereAStallStood(final GameTestHelper helper) {
        final List<String> lines = GameTestTimes.lines(List.of(new GameTestTimes.Timed("fast", 5L, true),
                new GameTestTimes.Timed("slow", 900L, false)));
        helper.assertTrue(lines.getFirst().startsWith("2 tests took 0.905 s") && lines.get(1).contains("slow")
                && lines.get(1).contains("FAILED"), "the report of time, the slowest first: " + lines);
        final String stall = StallMonitor.report(75L, Map.of("kiln_smelts", System.nanoTime()),
                Map.of(Thread.currentThread(), Thread.currentThread().getStackTrace()));
        helper.assertTrue(stall.contains("for 75 s") && stall.contains("kiln_smelts")
                && stall.contains(Thread.currentThread().getName()), "a stall says how long, what ran and where: "
                + stall);
        helper.succeed();
    }

    private static int rolls(final RandomSource random) {
        int sum = 0;
        for (int i = 0; i < 20; i++) {
            sum = sum * 31 + random.nextInt(1000);
        }
        return sum;
    }
}
