/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.core.jstechseries.builder.Assembly;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.ArmMotion;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanArms;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardPlans;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
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
 * The Small Shipyard's robot arms: four on their rails along the bay's edges once the dock forms; what the ship sent
 * to the dock wants goes in through its item port, and no more; each arm takes a block, swings it over and sets it
 * down only once its swing is over, then welds it; every part of the ship forms as its last block goes down, and the
 * arms hold nothing left over. On a network, a request to build a plan the dock keeps by name sends it to the dock,
 * the network brings the blocks through a data cable in at the booth's gland, and the request is written down done.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipyardArmsGameTests {

    private static final String ARENA = "shipyard";
    private static final String NETWORKED = "shipyard_network";
    private static final String BATCH = "jsspace_shipyard_arms";
    /* The pattern's lowest corner, the pit floor's west front corner, a ground of stone under it. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* The item port, in the dock's own blocks; and the booth's gland beside the terminal. */
    private static final BlockPos PORT = dock(10, 0, 9);
    private static final BlockPos GLAND = dock(11, 4, 10);
    /* The network east of the booth: the Mainframe, and its rack of storage, its rear on the cable. */
    private static final BlockPos MAINFRAME = dock(15, 4, 10);
    private static final BlockPos RACK = dock(14, 4, 11);
    private static final int ARMS = 4;
    private static final String PLAN = "NET";
    private static final int ROWS = 64;
    /* A small ship ready to fly that fits the dock. */
    private static final ShipDesign SHIP = new ShipDesign("ARMS", List.of(part("orbiter_cockpit"), part("airlock"),
            part("quarters"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
            part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));

    private ShipyardArmsGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 24000)
    public static void arms_buildTheShipFromWhatComesInThroughThePort(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final BlockPos[] first = new BlockPos[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    helper.assertTrue(dock.arms().arms().size() == ARMS, "four arms stand on their rails once the"
                            + " dock forms: " + dock.arms().arms().size());
                    final IItemHandler port = level.getCapability(Capabilities.ItemHandler.BLOCK,
                            helper.absolutePos(PORT), Direction.EAST);
                    helper.assertTrue(port != null, "the dock's item port passes items to its arms");
                    dock.design(SHIP);
                    helper.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    final Map<Item, Integer> needs = dock.assembler().needs();
                    helper.assertTrue(needs.equals(counts(plan.plan())), "the arms wait for every block of the ship");
                    helper.assertTrue(insert(port, Items.STONE, 64) == 0, "stone, which the ship does not want,"
                            + " stays out");
                    for (final Map.Entry<Item, Integer> need : needs.entrySet()) {
                        helper.assertTrue(insert(port, need.getKey(), need.getValue() + 1) == need.getValue(),
                                "as many of " + need.getKey() + " as the ship wants go in, and no more");
                    }
                })
                .thenWaitUntil(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    final Optional<PlanArms.Job> job = dock.arms().job(0).or(() -> dock.arms().job(1))
                            .or(() -> dock.arms().job(2)).or(() -> dock.arms().job(3));
                    helper.assertTrue(job.isPresent(), "an arm takes a block");
                    first[0] = job.get().at();
                    helper.assertTrue(!job.get().set() && level.getBlockState(first[0]).isAir(), "its block is"
                            + " not down while the arm swings it");
                })
                .thenIdle(ArmMotion.SET + 1)
                .thenExecute(() -> helper.assertTrue(!level.getBlockState(first[0]).isAir(), "once its swing is"
                        + " over, the arm has set its block down"))
                .thenWaitUntil(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    final PlannedBuild plan = dock.build().orElseThrow();
                    if (!plan.complete()) {
                        helper.fail("the arms build the whole ship: " + plan.missing() + " blocks to go, "
                                + left(helper, dock, plan));
                    }
                })
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    helper.assertTrue(dock.built(), "every part formed as its last block went down: "
                            + dock.formedParts());
                    helper.assertTrue(dock.assembler().held().isEmpty(), "and the arms hold nothing left over");
                })
                .thenSucceed();
    }

    @GameTest(template = NETWORKED, batch = BATCH, timeoutTicks = 24000)
    public static void arms_areSetBuildingByTheNetworkWhichBringsTheBlocks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final MainframeBlockEntity[] mainframe = new MainframeBlockEntity[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> {
                    mainframe[0] = network(helper);
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    dock.design(new ShipDesign(PLAN, SHIP.spine(), SHIP.hung()));
                    helper.assertTrue(dock.save() && dock.plan().isEmpty(), "the plan is kept, not sent");
                    final BuildPlan laid = ShipyardPlans.of(level, dock.design(), Orientation.NORTH);
                    final ServerRackBlockEntity rack = helper.getBlockEntity(RACK);
                    for (final Map.Entry<Item, Integer> block : counts(laid).entrySet()) {
                        rack.getServerStorage(0).insert(block.getKey(), block.getValue());
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(mainframe[0].networkUuid() != null
                        && NetworkSystem.get(level).membersCarryingOut(mainframe[0].networkUuid(),
                        Assembly.ASSEMBLE.id()).size() == 1, "the shipyard's terminal is on the network, by the"
                        + " cable in at the booth's gland"))
                .thenExecute(() -> {
                    final IqlEngine.Outcome given = mainframe[0].networkOperations().query(IqlEngine.viewOf(
                            new ServerCliComputer(mainframe[0], level)), "RUN " + Assembly.ASSEMBLE.id()
                            + " WITH plan = '" + PLAN + "'", ROWS);
                    helper.assertTrue(given.ok(), "the network sets the arms building: " + given.message());
                })
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, terminal).plan().map(ShipDesign::name)
                        .orElse("").equals(PLAN) && terminal(level, terminal).build().isPresent(), "its plan of"
                        + " that name is sent to the dock"))
                .thenWaitUntil(() -> {
                    final ShipyardTerminalBlockEntity dock = terminal(level, terminal);
                    final PlannedBuild plan = dock.build().orElseThrow();
                    if (!plan.complete()) {
                        helper.fail("the network brings the blocks, and the arms build the ship: " + plan.missing()
                                + " blocks to go; waiting " + dock.assembler().waiting() + ", holds "
                                + dock.assembler().held() + ", last operations " + mainframe[0].recentOperations()
                                .stream().limit(4).map(row -> row.kind() + "/" + row.key() + "/" + row.status() + "/"
                                + row.moved() + "of" + row.requested()).toList());
                    }
                })
                .thenWaitUntil(() -> helper.assertTrue(mainframe[0].recentOperations().stream()
                        .anyMatch(row -> row.ofAnotherMod() && row.status() == OperationRecord.STATUS_COMPLETED),
                        "and the request is written down done"))
                .thenSucceed();
    }

    /* What is left to lay, in the dock's own blocks: how many no arm reaches, and the first few of them. */
    private static String left(final GameTestHelper helper, final ShipyardTerminalBlockEntity dock,
                               final PlannedBuild plan) {
        int unreached = 0;
        final StringBuilder some = new StringBuilder();
        for (int i = 0; i < plan.plan().spots().size(); i++) {
            if (plan.present(i)) {
                continue;
            }
            final BlockPos at = plan.where(plan.plan().spots().get(i));
            final double[] place = {at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5};
            final boolean reached = dock.arms().arms().stream().anyMatch(arm -> arm.rig().reaches(place));
            if (!reached) {
                unreached++;
            }
            if (some.length() < 300) {
                some.append(helper.relativePos(at).subtract(CORNER).offset(-11, -3, -13)).append(reached ? "" : "!")
                        .append(level(helper, at)).append(' ');
            }
        }
        return unreached + " out of reach; " + some + "held " + dock.assembler().held().size() + " kinds";
    }

    /* What stands at {@code at}, by its block's path. */
    private static String level(final GameTestHelper helper, final BlockPos at) {
        return "=" + BuiltInRegistries.BLOCK.getKey(helper.getLevel().getBlockState(at).getBlock()).getPath();
    }

    private static boolean formed(final ServerLevel level, final BlockPos terminal) {
        return level.getBlockEntity(terminal) instanceof ShipyardTerminalBlockEntity dock && dock.formed();
    }

    private static ShipyardTerminalBlockEntity terminal(final ServerLevel level, final BlockPos terminal) {
        return (ShipyardTerminalBlockEntity) level.getBlockEntity(terminal);
    }

    /* The network east of the booth: the Mainframe and its rack, and a cable in to the terminal by the gland. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final MainframeBlockEntity mainframe = world.placeRunningMainframe(MAINFRAME);
        for (int x = GLAND.getX(); x < MAINFRAME.getX(); x++) {
            TestCables.lay(helper, new BlockPos(x, GLAND.getY(), GLAND.getZ()), ComputingModule.HBW_CABLE);
        }
        world.placeSeededRack(RACK, Direction.SOUTH);
        return mainframe;
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

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    /* A place of the dock in its own blocks, across from the bay's middle, up from the ground, along from the front. */
    private static BlockPos dock(final int x, final int y, final int z) {
        return CORNER.offset(x + 11, y + 3, z + 13);
    }
}
