/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.core.jstechseries.builder.Assembly;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import dev.jstech.tests.testkit.space.TestHangars;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The Rocket Hangar's crane building the rocket sent to its bay by itself: from the blocks a chest, a hopper or a pipe
 * puts into its loading dock, taking only what the rocket still wants; and, on a network, set building by a request of
 * the network's language, the network bringing the blocks it stores and crafting the ones it lacks, until the rocket
 * is whole and the request is written down done. The rocket's mobile launcher is laid by hand first, as a player may,
 * so the crane has only the rocket to build.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CraneAssemblyGameTests {

    private static final String HANGAR = "rocket_hangar";
    private static final String BATCH = "jsspace_crane";
    /* The hangar's terminal in the arena's middle, the hangar standing with its door to the north. */
    private static final BlockPos TERMINAL = new BlockPos(40, 2, 42);
    /* A block of the loading dock, as the hangar's pattern draws it, in the control room's outer wall to the east. */
    private static final int[] DOCK = {27, 1, 6};
    /*
     * The network in the control room, in a line across the room a block over the floor's things: the Mainframe in
     * the gap before the windows, its cable east over the terminal, then the router, its Ethernet and a Crafting
     * Computer; the rack beside the Mainframe.
     */
    private static final BlockPos MAINFRAME = new BlockPos(39, 3, 42);
    private static final BlockPos RACK = new BlockPos(40, 3, 43);
    private static final BlockPos ROUTER = new BlockPos(44, 3, 42);
    private static final BlockPos ETHERNET = new BlockPos(45, 3, 42);
    private static final BlockPos CRAFTER = new BlockPos(46, 3, 42);
    private static final String PLAN = "NET";
    private static final int ROWS = 64;
    /* The rocket's three parts, the first of its plan; the launcher's after them. */
    private static final int PARTS = 3;

    private CraneAssemblyGameTests() {
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 1600)
    public static void crane_buildsTheRocketFromWhatComesInThroughTheLoadingDock(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = TestHangars.build(level, helper.absolutePos(TERMINAL), Orientation.NORTH);
        final BlockPos dock = TestHangars.at(at, Orientation.NORTH, DOCK[0], DOCK[1], DOCK[2]);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, at).formed(), "the hangar forms"))
                .thenExecute(() -> {
                    final HangarTerminalBlockEntity terminal = terminal(level, at);
                    final IItemHandler port = level.getCapability(Capabilities.ItemHandler.BLOCK, dock,
                            Direction.EAST);
                    helper.assertTrue(port != null, "the loading dock passes items to its terminal");
                    terminal.design(new RocketDesign("PORT", rocket()));
                    helper.assertTrue(terminal.confirm(), "a ready design is sent to the bay");
                    final PlannedBuild plan = terminal.build().orElseThrow();
                    layLauncher(level, plan);
                    final Map<Item, Integer> needs = terminal.crane().needs();
                    helper.assertTrue(needs.equals(counts(plan.plan(), true)), "its launcher laid, the crane waits"
                            + " for every block of the rocket: " + needs);
                    helper.assertTrue(insert(port, Items.STONE, 64) == 0,
                            "stone, which the rocket does not want, stays out");
                    for (final Map.Entry<Item, Integer> need : needs.entrySet()) {
                        helper.assertTrue(insert(port, need.getKey(), need.getValue() + 1) == need.getValue(),
                                "as many of " + need.getKey() + " as the rocket wants go in, and no more");
                    }
                    helper.assertTrue(terminal.crane().needs().isEmpty(), "the crane waits for nothing more");
                })
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, at).build().orElseThrow().complete(),
                        "the crane builds the whole rocket: " + terminal(level, at).build().orElseThrow().missing()
                                + " blocks to go"))
                .thenExecute(() -> {
                    final HangarTerminalBlockEntity terminal = terminal(level, at);
                    helper.assertTrue(terminal.formedParts().containsAll(List.of(0, 1, 2)), "every part formed as"
                            + " its last block went down: " + terminal.formedParts());
                    helper.assertTrue(terminal.crane().held().isEmpty(), "and the crane holds nothing left over");
                })
                .thenSucceed();
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 3000)
    public static void crane_isSetBuildingByTheNetworkWhichBringsAndMakesItsBlocks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = TestHangars.build(level, helper.absolutePos(TERMINAL), Orientation.NORTH);
        final MainframeBlockEntity mainframe = network(helper);
        // The network stores every block of the rocket but one kind of the tank's, which it crafts from stone.
        final BuildPlan laid = RocketPlans.of(level, rocket());
        final Item made = BuiltInRegistries.BLOCK.get(laid.spots().stream().filter(spot -> spot.part() == 1)
                .findFirst().orElseThrow().block()).asItem();
        final ServerRackBlockEntity rack = helper.getBlockEntity(RACK);
        for (final Map.Entry<Item, Integer> block : counts(laid, true).entrySet()) {
            rack.getServerStorage(0).insert(block.getKey() == made ? Items.STONE : block.getKey(), block.getValue());
        }
        final CraftingComputerBlockEntity crafter = helper.getBlockEntity(CRAFTER);
        final List<ItemStack> grid = new ArrayList<>();
        for (int cell = 0; cell < CraftingPattern.GRID_SIZE; cell++) {
            grid.add(cell == 0 ? new ItemStack(Items.STONE) : ItemStack.EMPTY);
        }
        helper.assertTrue(crafter.loadPattern(new CraftingPattern(grid, new ItemStack(made))),
                "the network learns to make the tank's block from stone");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, at).formed(), "the hangar forms"))
                .thenExecute(() -> {
                    final HangarTerminalBlockEntity terminal = terminal(level, at);
                    terminal.design(new RocketDesign(PLAN, rocket()));
                    terminal.save();
                    helper.assertTrue(terminal.plan().isEmpty(), "the plan is kept, not sent");
                })
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                        && NetworkSystem.get(level).membersCarryingOut(mainframe.networkUuid(),
                        Assembly.ASSEMBLE.id()).size() == 1, "the hangar's terminal is on the network"))
                .thenExecute(() -> {
                    final IqlEngine.Outcome given = mainframe.networkOperations().query(IqlEngine.viewOf(
                            new ServerCliComputer(mainframe, level)), "RUN " + Assembly.ASSEMBLE.id()
                            + " WITH plan = '" + PLAN + "'", ROWS);
                    helper.assertTrue(given.ok(), "the network sets the crane building: " + given.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, at).plan().map(RocketDesign::name).orElse("")
                        .equals(PLAN) && terminal(level, at).build().isPresent(),
                        "its plan of that name is sent to the bay"))
                .thenExecute(() -> layLauncher(level, terminal(level, at).build().orElseThrow()))
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, at).build().orElseThrow().complete(),
                        "the network brings and makes the blocks, and the crane builds the rocket: "
                                + terminal(level, at).build().orElseThrow().missing() + " blocks to go; waits for "
                                + terminal(level, at).crane().needs() + ", holds " + terminal(level, at).crane()
                                .held() + ", last operations " + mainframe.recentOperations().stream().limit(4)
                                .map(row -> row.kind() + "/" + row.key() + "/" + row.status() + "/" + row.moved()
                                        + "of" + row.requested()).toList()))
                .thenWaitUntil(() -> helper.assertTrue(mainframe.recentOperations().stream()
                        .anyMatch(row -> row.ofAnotherMod() && row.status() == OperationRecord.STATUS_COMPLETED),
                        "and the request is written down done"))
                .thenExecute(() -> helper.assertTrue(terminal(level, at).formedParts().containsAll(List.of(0, 1, 2)),
                        "the whole rocket formed"))
                .thenSucceed();
    }

    /* An engine, a tank and a capsule. */
    private static RocketStack rocket() {
        return RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 1), part("capsule_3", 1));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    private static HangarTerminalBlockEntity terminal(final ServerLevel level, final BlockPos at) {
        return (HangarTerminalBlockEntity) level.getBlockEntity(at);
    }

    /* Lays every block of the plan's mobile launcher by hand, as a player may: the parts after the rocket's. */
    private static void layLauncher(final ServerLevel level, final PlannedBuild plan) {
        for (final PlanSpot spot : plan.plan().spots()) {
            if (spot.part() >= PARTS) {
                level.setBlockAndUpdate(plan.where(spot), BuiltInRegistries.BLOCK.get(spot.block())
                        .defaultBlockState());
            }
        }
    }

    /*
     * How many of each block item the plan {@code plan} wants, the first wanted first: of the rocket's parts alone
     * where {@code rocketOnly}.
     */
    private static Map<Item, Integer> counts(final BuildPlan plan, final boolean rocketOnly) {
        final Map<Item, Integer> out = new LinkedHashMap<>();
        for (final PlanSpot spot : plan.spots()) {
            if (!rocketOnly || spot.part() < PARTS) {
                out.merge(BuiltInRegistries.BLOCK.get(spot.block()).asItem(), 1, Integer::sum);
            }
        }
        return out;
    }

    /* How many of {@code count} of {@code item} the handler takes, put in as a hopper would, a stack at a time. */
    private static int insert(final IItemHandler handler, final Item item, final int count) {
        int taken = 0;
        int left = count;
        final int most = new ItemStack(item).getMaxStackSize();
        while (left > 0) {
            final int offered = Math.min(left, most);
            final int took = offered - ItemHandlerHelper.insertItem(handler, new ItemStack(item, offered), false)
                    .getCount();
            taken += took;
            left -= offered;
            if (took < offered) {
                break;
            }
        }
        return taken;
    }

    /*
     * The network in the control room: the Mainframe, its cable east over the terminal to the router, its Ethernet
     * and a Crafting Computer; the rack of storage beside the Mainframe.
     */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        for (int x = MAINFRAME.getX() + 1; x < ROUTER.getX(); x++) {
            TestCables.lay(helper, new BlockPos(x, MAINFRAME.getY(), MAINFRAME.getZ()), ComputingModule.HBW_CABLE);
        }
        world.placeSeededRack(RACK, Direction.SOUTH);
        helper.setBlock(ROUTER, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, ETHERNET, ComputingModule.ETHERNET_CABLE);
        world.placeRunningCraftingComputer(CRAFTER);
        return mainframe;
    }
}
