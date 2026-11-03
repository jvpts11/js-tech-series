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
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.BuildPlan;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import dev.jstech.tests.testkit.space.TestHangars;
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
 * puts into its loading dock, taking only what the rocket still wants. The rocket's mobile launcher is laid by hand
 * first, as a player may, so the crane has only the rocket to build. Its terminal is a machine, no computer: a data
 * network's cable running over it does not take it in.
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
     * A network in the control room, a block over the floor's things: the Mainframe in the gap before the windows,
     * its cable east over the terminal; and how long the network is given to take in what its cable touches.
     */
    private static final BlockPos MAINFRAME = new BlockPos(39, 3, 42);
    private static final int CABLE_END = 41;
    private static final int SETTLE = 40;
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

    @GameTest(template = HANGAR, batch = BATCH, timeoutTicks = 600)
    public static void terminal_isNoMemberOfTheNetworkItsCableRunsOver(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos at = TestHangars.build(level, helper.absolutePos(TERMINAL), Orientation.NORTH);
        final MainframeBlockEntity mainframe = network(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(terminal(level, at).formed() && mainframe.networkUuid() != null,
                        "the hangar forms, and the network is up"))
                .thenIdle(SETTLE)
                .thenExecute(() -> helper.assertTrue(NetworkSystem.get(level).membersCarryingOut(
                        mainframe.networkUuid(), Assembly.ASSEMBLE.id()).isEmpty(), "the hangar's terminal, a machine"
                        + " and no computer, is no member of the network whose cable runs over it"))
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

    /* The network in the control room: the Mainframe, and its cable east over the terminal. */
    private static MainframeBlockEntity network(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(MAINFRAME);
        for (int x = MAINFRAME.getX() + 1; x <= CABLE_END; x++) {
            TestCables.lay(helper, new BlockPos(x, MAINFRAME.getY(), MAINFRAME.getZ()), ComputingModule.HBW_CABLE);
        }
        return mainframe;
    }
}
