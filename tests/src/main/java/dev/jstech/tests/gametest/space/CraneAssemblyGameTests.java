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
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketPlans;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The Rocket Hangar's crane building the rocket sent to its bay by itself: from the blocks a chest, a hopper or a pipe
 * puts into an item port in its wall, taking only what the rocket still wants; and, on a network, set building by a
 * request of the network's language, the network bringing the blocks it stores and crafting the ones it lacks, until
 * the rocket is whole and the request is written down done.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CraneAssemblyGameTests {

    private static final String HANGAR = "tall";
    private static final String BATCH = "jsspace_crane";
    /* The hangar's bay, from (2, 2, 2) to (8, 13, 8), its hoist at 12; the rocket stands on (5, 2, 5). */
    private static final int LOW = 2;
    private static final int HIGH_XZ = 8;
    private static final int TOP = 13;
    private static final BlockPos TERMINAL = new BlockPos(5, 3, 9);
    private static final BlockPos DOOR = new BlockPos(1, 2, 5);
    private static final BlockPos HOIST = new BlockPos(5, 12, 5);
    private static final BlockPos PORT = new BlockPos(1, 4, 4);
    /* The network south of the hangar, its cable behind the terminal. */
    private static final BlockPos MAINFRAME = new BlockPos(2, 3, 10);
    private static final BlockPos RACK = new BlockPos(3, 3, 11);
    private static final BlockPos ROUTER = new BlockPos(6, 3, 10);
    private static final BlockPos ETHERNET = new BlockPos(7, 3, 10);
    private static final BlockPos CRAFTER = new BlockPos(8, 3, 10);
    private static final String PLAN = "NET";
    private static final int ROWS = 64;

    private CraneAssemblyGameTests() {
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 1600)
    public static void crane_buildsTheRocketFromWhatComesInThroughAPort(final GameTestHelper helper) {
        build(helper);
        helper.setBlock(PORT, HangarBlocks.ITEM_PORT.get());
        final HangarTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        terminal.look();
        final IItemHandler port = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,
                helper.absolutePos(PORT), Direction.WEST);
        helper.assertTrue(port != null, "the port in the hangar's wall passes items to its terminal");
        terminal.design(new RocketDesign("PORT", rocket()));
        helper.assertTrue(terminal.confirm(), "a ready design is sent to the bay");
        final PlannedBuild plan = terminal.build().orElseThrow();
        final Map<Item, Integer> needs = terminal.crane().needs();
        helper.assertTrue(needs.equals(counts(plan.plan())), "the crane waits for every block of the rocket: "
                + needs);
        helper.assertTrue(insert(port, Items.STONE, 64) == 0, "stone, which the rocket does not want, stays out");
        for (final Map.Entry<Item, Integer> need : needs.entrySet()) {
            helper.assertTrue(insert(port, need.getKey(), need.getValue() + 1) == need.getValue(),
                    "as many of " + need.getKey() + " as the rocket wants go in, and no more");
        }
        helper.assertTrue(terminal.crane().needs().isEmpty(), "the crane waits for nothing more");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(plan.complete(), "the crane builds the whole rocket: "
                        + plan.missing() + " blocks to go"))
                .thenExecute(() -> {
                    helper.assertTrue(terminal.formedParts().equals(List.of(0, 1, 2)), "every part formed as its"
                            + " last block went down: " + terminal.formedParts());
                    helper.assertTrue(terminal.crane().held().isEmpty(), "and the crane holds nothing left over");
                    terminal.undo();
                })
                .thenSucceed();
    }

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 3000)
    public static void crane_isSetBuildingByTheNetworkWhichBringsAndMakesItsBlocks(final GameTestHelper helper) {
        build(helper);
        final MainframeBlockEntity mainframe = network(helper);
        final HangarTerminalBlockEntity terminal = helper.getBlockEntity(TERMINAL);
        terminal.look();
        terminal.design(new RocketDesign(PLAN, rocket()));
        terminal.save();
        helper.assertTrue(terminal.plan().isEmpty(), "the plan is kept, not sent");
        // The network stores every block of the rocket but one kind of the tank's, which it crafts from stone.
        final BuildPlan laid = RocketPlans.of(helper.getLevel(), rocket());
        final Item made = BuiltInRegistries.BLOCK.get(laid.spots().stream().filter(spot -> spot.part() == 1)
                .findFirst().orElseThrow().block()).asItem();
        final ServerRackBlockEntity rack = helper.getBlockEntity(RACK);
        final Map<Item, Integer> blocks = counts(laid);
        for (final Map.Entry<Item, Integer> block : blocks.entrySet()) {
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
                .thenWaitUntil(() -> helper.assertTrue(mainframe.networkUuid() != null
                        && NetworkSystem.get(helper.getLevel()).membersCarryingOut(mainframe.networkUuid(),
                        Assembly.ASSEMBLE.id()).size() == 1, "the hangar's terminal is on the network"))
                .thenExecute(() -> {
                    final IqlEngine.Outcome given = mainframe.networkOperations().query(IqlEngine.viewOf(
                            new ServerCliComputer(mainframe, helper.getLevel())), "RUN " + Assembly.ASSEMBLE.id()
                            + " WITH plan = '" + PLAN + "'", ROWS);
                    helper.assertTrue(given.ok(), "the network sets the crane building: " + given.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(terminal.plan().map(RocketDesign::name).orElse("")
                        .equals(PLAN) && terminal.build().isPresent(), "its plan of that name is sent to the bay"))
                .thenWaitUntil(() -> helper.assertTrue(terminal.build().orElseThrow().complete(),
                        "the network brings and makes the blocks, and the crane builds the rocket: "
                                + terminal.build().orElseThrow().missing() + " blocks to go"))
                .thenWaitUntil(() -> helper.assertTrue(mainframe.recentOperations().stream()
                        .anyMatch(row -> row.ofAnotherMod() && row.status() == OperationRecord.STATUS_COMPLETED),
                        "and the request is written down done"))
                .thenExecute(() -> {
                    helper.assertTrue(terminal.formedParts().equals(List.of(0, 1, 2)), "the whole rocket formed");
                    terminal.undo();
                })
                .thenSucceed();
    }

    /* An engine, a tank and a capsule. */
    private static RocketStack rocket() {
        return RocketStack.of(part("sea_level_engine_3", 1), part("tank_3", 1), part("capsule_3", 1));
    }

    private static RocketStack.Part part(final String path, final int layers) {
        return new RocketStack.Part(SkeletonRocketParts.id(path), layers);
    }

    /* How many of each block item the plan {@code plan} wants, the first wanted first. */
    private static Map<Item, Integer> counts(final BuildPlan plan) {
        final Map<Item, Integer> out = new LinkedHashMap<>();
        for (final PlanSpot spot : plan.spots()) {
            out.merge(BuiltInRegistries.BLOCK.get(spot.block()).asItem(), 1, Integer::sum);
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

    /* The network south of the hangar: the Mainframe, the rack of storage, the router and a Crafting Computer. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        for (int x = MAINFRAME.getX() + 1; x <= TERMINAL.getX(); x++) {
            TestCables.lay(helper, new BlockPos(x, MAINFRAME.getY(), MAINFRAME.getZ()), ComputingModule.HBW_CABLE);
        }
        world.placeSeededRack(RACK, Direction.SOUTH);
        helper.setBlock(ROUTER, ComputingModule.PERSONAL_ROUTER.get());
        TestCables.lay(helper, ETHERNET, ComputingModule.ETHERNET_CABLE);
        world.placeRunningCraftingComputer(CRAFTER);
        return mainframe;
    }

    /* The hangar of stone, its terminal in the south wall looking north, its crane, its door. */
    private static void build(final GameTestHelper helper) {
        for (int x = LOW - 1; x <= HIGH_XZ + 1; x++) {
            for (int z = LOW - 1; z <= HIGH_XZ + 1; z++) {
                for (int y = LOW - 1; y <= TOP + 1; y++) {
                    final boolean shell = x < LOW || x > HIGH_XZ || z < LOW || z > HIGH_XZ || y < LOW || y > TOP;
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        helper.setBlock(TERMINAL, HangarBlocks.TERMINAL.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        for (int x = LOW; x <= HIGH_XZ; x++) {
            helper.setBlock(new BlockPos(x, TOP, LOW), HangarBlocks.CRANE_RAIL.get());
            helper.setBlock(new BlockPos(x, TOP, HIGH_XZ), HangarBlocks.CRANE_RAIL.get());
        }
        for (int z = LOW + 1; z < HIGH_XZ; z++) {
            helper.setBlock(new BlockPos(5, TOP, z), HangarBlocks.CRANE_BRIDGE.get());
        }
        helper.setBlock(HOIST, HangarBlocks.CRANE_HOIST.get());
        helper.setBlock(DOOR, HangarBlocks.BAY_DOOR.get());
    }
}
