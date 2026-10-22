/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEnd;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.EndRedstone;
import dev.jstech.core.fluid.FluidGrids;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.LineGroup;
import dev.jstech.core.grid.LineGroups;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.jstechseries.tools.SharedTools;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestCableTypes;
import dev.jstech.tests.TestFluidEnds;
import dev.jstech.tests.TestGroupMembers;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The ends of the Core's lines: an output's water goes first to the end of the highest priority, and to ends of one
 * priority in turns, the same share over time; an end set to work with a redstone signal rests without one, and one
 * set to work without a signal rests with one; an end turned off plugs into nothing; a filter lets through what it names and keeps no more in its
 * block than it says; an end moves no more than its rate; ends meet only on their channel; an end set to pull empties a
 * tank; the line of items shares the same way; a group line tells its machines the group they are in; a rubber pipe
 * bursts under lava and spills it; and the Basic Wrench goes round an end's modes.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CableEndsGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos OUTPUT = new BlockPos(1, 2, 2);
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));
    private static final BlockPos FAR = new BlockPos(5, 2, 2);
    private static final BlockPos SIDE = new BlockPos(3, 2, 3);
    private static final int WATER = 1_000;
    private static final int WAIT = 20;

    private CableEndsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void priority_fillsTheHighestFirst(final GameTestHelper helper) {
        final FluidTank output = fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        end(helper, RUN.get(1), Direction.SOUTH, TestCableTypes.PLAIN_PIPE, CableEnd.DEFAULT.withPriority(5));
        helper.succeedWhen(() -> {
            helper.assertTrue(output.isEmpty(), "the output empties");
            helper.assertTrue(tank(helper, SIDE).getFluidAmount() == WATER, "into the end of the highest priority");
            helper.assertTrue(tank(helper, FAR).isEmpty(), "and none into the other");
        });
    }

    @GameTest(template = ARENA)
    public static void samePriority_takesTurns(final GameTestHelper helper) {
        final FluidTank output = fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        helper.succeedWhen(() -> {
            helper.assertTrue(output.isEmpty(), "the output empties");
            final int side = tank(helper, SIDE).getFluidAmount();
            final int far = tank(helper, FAR).getFluidAmount();
            helper.assertTrue(side > 0 && far > 0, "each end of one priority gets its turn");
            helper.assertTrue(Math.abs(side - far) <= TestCableTypes.PLAIN_RATE,
                    "the same share over time, never more than a tick's worth apart: " + side + " and " + far);
            helper.assertTrue(side + far == WATER, "and none is lost");
        });
    }

    @GameTest(template = ARENA)
    public static void redstone_anEndThatWorksWithASignalRestsWithoutOne(final GameTestHelper helper) {
        final FluidTank output = fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        end(helper, RUN.get(0), Direction.WEST, TestCableTypes.PLAIN_PIPE,
                CableEnd.DEFAULT.withRedstone(EndRedstone.WITH_SIGNAL));
        helper.startSequence()
                .thenExecuteAfter(WAIT, () -> helper.assertTrue(output.getFluidAmount() == WATER,
                        "without a signal at its cable block the end rests: " + output.getFluidAmount()))
                .thenExecute(() -> helper.setBlock(RUN.get(0).above(), Blocks.REDSTONE_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(output.isEmpty(), "with one it works, and the output empties"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void redstone_anEndThatWorksWithoutASignalRestsWithOne(final GameTestHelper helper) {
        final FluidTank output = fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        helper.setBlock(RUN.get(0).above(), Blocks.REDSTONE_BLOCK);
        end(helper, RUN.get(0), Direction.WEST, TestCableTypes.PLAIN_PIPE,
                CableEnd.DEFAULT.withRedstone(EndRedstone.WITHOUT_SIGNAL));
        final CableBlockEntity first = TestCables.cable(helper, RUN.get(0));
        final CableEnd set = first.end(first.wire(TestCableTypes.PLAIN_PIPE.get()).slot(), Direction.WEST);
        final CableEnd saved = CableEnd.CODEC.parse(NbtOps.INSTANCE, CableEnd.CODEC.encodeStart(NbtOps.INSTANCE, set)
                .getOrThrow()).getOrThrow();
        helper.assertTrue(saved.redstone() == EndRedstone.WITHOUT_SIGNAL, "what it does with redstone is saved");
        helper.startSequence()
                .thenExecuteAfter(WAIT, () -> helper.assertTrue(output.getFluidAmount() == WATER,
                        "with a signal at its cable block the end rests: " + output.getFluidAmount()))
                .thenExecute(() -> helper.setBlock(RUN.get(0).above(), Blocks.AIR))
                .thenWaitUntil(() -> helper.assertTrue(output.isEmpty(), "without one it works, and the output empties"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void off_plugsIntoNothing(final GameTestHelper helper) {
        final FluidTank output = fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        end(helper, RUN.get(2), Direction.EAST, TestCableTypes.PLAIN_PIPE, CableEnd.DEFAULT.withMode(EndMode.OFF));
        final CableBlockEntity last = TestCables.cable(helper, RUN.get(2));
        helper.assertTrue((last.plugs(last.wire(TestCableTypes.PLAIN_PIPE.get()).slot())
                & 1 << Direction.EAST.get3DDataValue()) == 0, "an end turned off plugs into nothing");
        helper.succeedWhen(() -> {
            helper.assertTrue(output.isEmpty() && tank(helper, SIDE).getFluidAmount() == WATER,
                    "the water goes to the other end");
            helper.assertTrue(tank(helper, FAR).isEmpty(), "and none past the end turned off");
        });
    }

    @GameTest(template = ARENA)
    public static void filter_letsThroughOnlyWhatItNames(final GameTestHelper helper) {
        final FluidTank output = fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        end(helper, RUN.get(1), Direction.SOUTH, TestCableTypes.PLAIN_PIPE, CableEnd.DEFAULT.withFilter(
                ItemFilter.only(new ItemFilter.Fuzzy("minecraft:lava", 0))));
        helper.succeedWhen(() -> {
            helper.assertTrue(output.isEmpty() && tank(helper, FAR).getFluidAmount() == WATER,
                    "the water goes past an end that lets only lava through");
            helper.assertTrue(tank(helper, SIDE).isEmpty(), "which takes none of it");
        });
    }

    @GameTest(template = ARENA)
    public static void keep_fillsTheBlockNoFurtherThanTheFilterSays(final GameTestHelper helper) {
        final FluidTank output = fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        end(helper, RUN.get(1), Direction.SOUTH, TestCableTypes.PLAIN_PIPE, CableEnd.DEFAULT.withPriority(5)
                .withFilter(ItemFilter.only(new ItemFilter.Fuzzy("minecraft:water", 300))));
        helper.succeedWhen(() -> {
            helper.assertTrue(output.isEmpty(), "the output empties");
            helper.assertTrue(tank(helper, SIDE).getFluidAmount() == 300, "the end that keeps 300 stops there");
            helper.assertTrue(tank(helper, FAR).getFluidAmount() == WATER - 300, "the rest goes on");
        });
    }

    @GameTest(template = ARENA)
    public static void rate_movesNoMoreThroughAnEnd(final GameTestHelper helper) {
        fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        end(helper, RUN.get(1), Direction.SOUTH, TestCableTypes.PLAIN_PIPE, CableEnd.DEFAULT.withMode(EndMode.OFF));
        end(helper, RUN.get(2), Direction.EAST, TestCableTypes.PLAIN_PIPE, CableEnd.DEFAULT.withRate(10L));
        helper.succeedWhen(() -> {
            final OptionalLong moved = FluidGrids.movedLastTickAt(helper.getLevel(),
                    TestCables.cable(helper, RUN.get(0)).place(TestCableTypes.PLAIN_PIPE.get()));
            helper.assertTrue(moved.isPresent() && moved.getAsLong() == 10L,
                    "ten millibuckets a tick pass an end of rate ten: " + moved);
        });
    }

    @GameTest(template = ARENA)
    public static void channels_keepTheirEndsApart(final GameTestHelper helper) {
        final FluidTank output = fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        end(helper, RUN.get(0), Direction.WEST, TestCableTypes.PLAIN_PIPE, CableEnd.DEFAULT.withChannel(3));
        end(helper, RUN.get(2), Direction.EAST, TestCableTypes.PLAIN_PIPE, CableEnd.DEFAULT.withChannel(3));
        helper.succeedWhen(() -> {
            helper.assertTrue(output.isEmpty() && tank(helper, FAR).getFluidAmount() == WATER,
                    "the water goes to the end on its channel");
            helper.assertTrue(tank(helper, SIDE).isEmpty(), "and none to an end on another");
        });
    }

    @GameTest(template = ARENA)
    public static void pull_emptiesATankIntoAnother(final GameTestHelper helper) {
        helper.setBlock(OUTPUT, IndustrialModule.TANK.get());
        helper.setBlock(FAR, IndustrialModule.TANK.get());
        fill(helper, OUTPUT, 2_000);
        fill(helper, FAR, 100);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.PLAIN_PIPE);
        }
        helper.runAfterDelay(WAIT, () -> {
            helper.assertTrue(amount(helper, OUTPUT) == 2_000, "one tank never pours into another by itself");
            end(helper, RUN.get(0), Direction.WEST, TestCableTypes.PLAIN_PIPE, CableEnd.DEFAULT.withMode(
                    EndMode.PULL));
            helper.succeedWhen(() -> helper.assertTrue(amount(helper, OUTPUT) == 0 && amount(helper, FAR) == 2_100,
                    "but an end that pulls empties it into the other"));
        });
    }

    @GameTest(template = ARENA)
    public static void items_moveAlongTheLineOfItemsKeepingWhatTheFilterSays(final GameTestHelper helper) {
        helper.setBlock(OUTPUT, Blocks.BARREL);
        helper.setBlock(FAR, Blocks.BARREL);
        final BarrelBlockEntity from = helper.getBlockEntity(OUTPUT);
        final BarrelBlockEntity to = helper.getBlockEntity(FAR);
        from.setItem(0, new ItemStack(Items.COBBLESTONE, 32));
        from.setItem(1, new ItemStack(Items.DIRT, 8));
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.ITEM_LINE);
        }
        // One barrel never empties into another by itself: the line pulls from the first.
        end(helper, RUN.get(0), Direction.WEST, TestCableTypes.ITEM_LINE, CableEnd.DEFAULT.withMode(EndMode.PULL));
        end(helper, RUN.get(2), Direction.EAST, TestCableTypes.ITEM_LINE, CableEnd.DEFAULT.withFilter(
                ItemFilter.only(new ItemFilter.Fuzzy("minecraft:cobblestone", 20))));
        helper.succeedWhen(() -> {
            helper.assertTrue(count(to, Items.COBBLESTONE) == 20, "twenty cobblestones are kept in the barrel");
            helper.assertTrue(count(to, Items.DIRT) == 0 && count(from, Items.DIRT) == 8,
                    "and no dirt goes past the filter");
            helper.assertTrue(count(from, Items.COBBLESTONE) == 12, "none is lost");
        });
    }

    @GameTest(template = ARENA)
    public static void group_tellsItsMachinesTheGroupTheyAreIn(final GameTestHelper helper) {
        final BlockPos first = helper.absolutePos(OUTPUT);
        final BlockPos second = helper.absolutePos(FAR);
        TestGroupMembers.clear(helper.getLevel(), first);
        TestGroupMembers.clear(helper.getLevel(), second);
        helper.setBlock(OUTPUT, Blocks.LODESTONE);
        helper.setBlock(FAR, Blocks.LODESTONE);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.GROUP_LINE);
        }
        helper.runAfterDelay(2, () -> {
            final Optional<LineGroup> heard = TestGroupMembers.heard(helper.getLevel(), first);
            helper.assertTrue(heard.isPresent() && heard.get().has(first) && heard.get().has(second),
                    "both machines hear they are in one group");
            helper.assertTrue(LineGroups.of(helper.getLevel(), TestCableTypes.GROUPS).flatMap(groups ->
                    groups.groupOf(second)).equals(heard), "the line knows the group of each machine");
            TestCables.cable(helper, RUN.get(1)).take(TestCableTypes.GROUP_LINE.get());
            helper.runAfterDelay(2, () -> {
                helper.assertTrue(TestGroupMembers.dissolvedWith(first), "cut, the group dissolves");
                final Optional<LineGroup> now = TestGroupMembers.heard(helper.getLevel(), first);
                helper.assertTrue(now.isPresent() && now.get().has(first) && !now.get().has(second),
                        "and each side is a group of its own");
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void rubberPipe_burstsUnderLavaAndSpillsIt(final GameTestHelper helper) {
        TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(OUTPUT));
        TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT)).fill(new FluidStack(Fluids.LAVA, 5_000),
                IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(OUTPUT, Blocks.CHISELED_TUFF);
        helper.setBlock(FAR, Blocks.CHISELED_TUFF_BRICKS);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, TestCableTypes.RUBBER_PIPE);
        }
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockState(RUN.get(0)).is(Blocks.LAVA), "the pipe burst, spilling lava");
            helper.assertTrue(TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(FAR)).isEmpty(),
                    "and none of it got through");
        });
    }

    @GameTest(template = ARENA)
    public static void wrench_goesRoundAnEndsModes(final GameTestHelper helper) {
        fluidEnds(helper, TestCableTypes.PLAIN_PIPE);
        final BlockPos cable = helper.absolutePos(RUN.get(2));
        final FakePlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.fromString(
                "2f4c1e0a-6b7d-4c3e-9a1f-8e5d2c7b4a10"), "end_wrench"));
        // Two blocks west of the cable, eyes at its middle, looking east along the run.
        player.moveTo(cable.getX() - 1.5, cable.getY() + 0.5 - player.getEyeHeight(), cable.getZ() + 0.5, -90.0F,
                0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(SharedTools.WRENCH.get()));
        final Vec3 eastArm = new Vec3(cable.getX() + 0.85, cable.getY() + 0.5, cable.getZ() + 0.5);
        final UseOnContext use = new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(eastArm, Direction.WEST, cable, false));
        SharedTools.WRENCH.get().useOn(use);
        helper.assertTrue(TestCables.cable(helper, RUN.get(2)).end(TestCableTypes.PLAIN_PIPE.get(), Direction.EAST)
                .mode() == EndMode.PUSH, "the wrench sets the east end to push");
        SharedTools.WRENCH.get().useOn(use);
        SharedTools.WRENCH.get().useOn(use);
        helper.assertTrue(TestCables.cable(helper, RUN.get(2)).end(TestCableTypes.PLAIN_PIPE.get(), Direction.EAST)
                .mode() == EndMode.OFF, "then pull, then off");
        helper.succeed();
    }

    /* An output of a thousand millibuckets of water, a run of {@code pipe}, an input at its far end and one beside. */
    private static FluidTank fluidEnds(final GameTestHelper helper, final Supplier<CableType> pipe) {
        for (final BlockPos at : List.of(OUTPUT, FAR, SIDE)) {
            TestFluidEnds.clear(helper.getLevel(), helper.absolutePos(at));
        }
        final FluidTank output = TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(OUTPUT));
        output.fill(new FluidStack(Fluids.WATER, WATER), IFluidHandler.FluidAction.EXECUTE);
        helper.setBlock(OUTPUT, Blocks.CHISELED_TUFF);
        helper.setBlock(FAR, Blocks.CHISELED_TUFF_BRICKS);
        helper.setBlock(SIDE, Blocks.CHISELED_TUFF_BRICKS);
        for (final BlockPos at : RUN) {
            TestCables.lay(helper, at, pipe);
        }
        return output;
    }

    private static void end(final GameTestHelper helper, final BlockPos at, final Direction face,
                            final Supplier<CableType> type, final CableEnd end) {
        helper.assertTrue(TestCables.cable(helper, at).setEnd(type.get(), face, end), "the end is set");
    }

    private static FluidTank tank(final GameTestHelper helper, final BlockPos at) {
        return TestFluidEnds.tankAt(helper.getLevel(), helper.absolutePos(at));
    }

    private static void fill(final GameTestHelper helper, final BlockPos at, final int amount) {
        final IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(at), null);
        helper.assertTrue(tank != null, "the tank holds fluids");
        tank.fill(new FluidStack(Fluids.WATER, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    private static int amount(final GameTestHelper helper, final BlockPos at) {
        final IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(at), null);
        return tank == null ? -1 : tank.getFluidInTank(0).getAmount();
    }

    private static int count(final BarrelBlockEntity barrel, final Item item) {
        int found = 0;
        for (int slot = 0; slot < barrel.getContainerSize(); slot++) {
            if (barrel.getItem(slot).is(item)) {
                found += barrel.getItem(slot).getCount();
            }
        }
        return found;
    }
}
