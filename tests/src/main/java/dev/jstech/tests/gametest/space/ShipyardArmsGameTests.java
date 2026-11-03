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
import dev.jstech.core.jstechseries.builder.Assembly;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.multiblock.plan.ArmMotion;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanArms;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.hangar.ItemPortBlockEntity;
import dev.jstech.space.hangar.ItemPortView;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
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
 * arms hold nothing left over. Its terminal is a machine, no computer: a data network's cable touching it does not
 * take it in.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipyardArmsGameTests {

    private static final String ARENA = "shipyard";
    private static final String BATCH = "jsspace_shipyard_arms";
    /* The pattern's lowest corner, the pit floor's west front corner, a ground of stone under it. */
    private static final BlockPos CORNER = new BlockPos(1, 2, 1);
    /* The item port, in the dock's own blocks. */
    private static final BlockPos PORT = dock(10, 0, 9);
    /*
     * A network in the booth, on its open floor before the console: the cable beside the terminal, the Mainframe
     * behind it; and how long the network is given to take in what its cable touches.
     */
    private static final BlockPos CABLE = dock(10, 4, 10);
    private static final BlockPos MAINFRAME = dock(10, 4, 11);
    private static final int SETTLE = 40;
    private static final int ARMS = 4;
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
        final int[] put = new int[1];
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
                    final Map.Entry<Item, Integer> one = needs.entrySet().iterator().next();
                    helper.assertTrue(insert(port, one.getKey(), one.getValue() + 1) == one.getValue(),
                            "as many of " + one.getKey() + " as the ship lacks go into the port's hold, and no more");
                    final ItemPortBlockEntity held = (ItemPortBlockEntity) level.getBlockEntity(helper.absolutePos(
                            PORT));
                    final ItemPortView view = held.view();
                    helper.assertTrue(view.ship().equals(SHIP.name()) && view.laid() == 0
                                    && view.blocks() == plan.plan().spots().size(),
                            "the port's screen shows the ship, none of its blocks laid yet: " + view);
                    helper.assertTrue(view.rows().stream().anyMatch(row -> row.item() == one.getKey()
                                    && row.have() == one.getValue() && row.enough()),
                            "and the block the port holds all of, as enough: " + view.rows());
                    put[0] = one.getValue();
                })
                // A tick on, the port has met its dock: what it holds, and what it passed on, count toward the ship.
                .thenIdle(1)
                .thenExecute(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    helper.assertTrue(dock.heldForBuild() == put[0], "the designer counts what the port holds "
                            + "toward the ship: " + dock.heldForBuild() + " of " + put[0]);
                })
                // The rest comes in as a pipe brings it, as much as the port's hold takes at a time.
                .thenWaitUntil(() -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal);
                    final IItemHandler port = level.getCapability(Capabilities.ItemHandler.BLOCK,
                            helper.absolutePos(PORT), Direction.EAST);
                    for (final Map.Entry<Item, Integer> need : dock.assembler().needs().entrySet()) {
                        insert(port, need.getKey(), need.getValue());
                    }
                    if (!dock.assembler().needs().isEmpty()) {
                        helper.fail("the port passes the blocks on to the arms: " + dock.assembler().needs().size()
                                + " kinds still to come");
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

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 1200)
    public static void terminal_isNoMemberOfTheNetworkItsCableTouches(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos terminal = TestShipyards.build(level, helper.absolutePos(CORNER));
        final MainframeBlockEntity[] mainframe = new MainframeBlockEntity[1];
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(level, terminal), "the dock forms"))
                .thenExecute(() -> mainframe[0] = network(helper))
                .thenWaitUntil(() -> helper.assertTrue(mainframe[0].networkUuid() != null, "the network is up"))
                .thenIdle(SETTLE)
                .thenExecute(() -> helper.assertTrue(NetworkSystem.get(level).membersCarryingOut(
                        mainframe[0].networkUuid(), Assembly.ASSEMBLE.id()).isEmpty(), "the shipyard's terminal, a"
                        + " machine and no computer, is no member of the network whose cable touches it"))
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

    /* A network in the booth: a Mainframe on its floor, and a cable between it and the terminal's console. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(MAINFRAME);
        TestCables.lay(helper, CABLE, ComputingModule.HBW_CABLE);
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
