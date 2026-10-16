/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.conveyor.ConveyorBlock;
import dev.jstech.core.conveyor.ConveyorBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestConveyors;
import dev.jstech.tests.TestMechanical;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Core's conveyors: a run carries its items along and into a chest in front, the end of the run alone ticking;
 * items wait a gap apart at an end with nowhere to go, or fall off one that drops them; a belt hands its items into a
 * belt across its end; a belt a shaft turns moves only while it turns, and one on energy only while it has some; a
 * hopper and an item lying on the belt put items on; what stands on it is carried; and its items are kept in a save.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ConveyorGameTests {

    private static final String ARENA = "empty";
    private static final int Y = 2;
    private static final BlockPos FIRST = new BlockPos(1, Y, 2);
    private static final BlockPos SECOND = new BlockPos(2, Y, 2);
    private static final BlockPos THIRD = new BlockPos(3, Y, 2);
    private static final BlockPos AHEAD = new BlockPos(4, Y, 2);
    private static final double QUARTER = 0.25;
    private static final int STILL = 20;

    private ConveyorGameTests() {
    }

    @GameTest(template = ARENA)
    public static void belts_carryTheirItemsAlongTheRunIntoAChest(final GameTestHelper helper) {
        run(helper, TestConveyors.BELT.get(), FIRST, SECOND, THIRD);
        helper.setBlock(AHEAD, Blocks.CHEST);
        helper.assertTrue(!helper.getBlockState(FIRST).getValue(ConveyorBlock.END), "the first belt is no end");
        helper.assertTrue(helper.getBlockState(THIRD).getValue(ConveyorBlock.END), "the third ends the run");
        belt(helper, FIRST).put(new ItemStack(Items.DIAMOND, 3), 0.0);
        helper.succeedWhen(() -> helper.assertTrue(helper.getBlockEntity(AHEAD) instanceof ChestBlockEntity chest
                && chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount() == 3,
                "the diamonds carried into the chest"));
    }

    @GameTest(template = ARENA)
    public static void belts_holdWhatReachesTheirEndAGapApart(final GameTestHelper helper) {
        run(helper, TestConveyors.BELT.get(), FIRST);
        final ConveyorBlockEntity belt = belt(helper, FIRST);
        for (int k = 0; k < 4; k++) {
            belt.put(new ItemStack(Items.STONE), k * QUARTER);
        }
        helper.runAfterDelay(STILL, () -> {
            helper.assertTrue(belt.lane().size() == 4, "every stone still on the belt");
            helper.assertTrue(belt.lane().entries().getFirst().at() >= 1.0, "the first waits at the end");
            helper.assertTrue(Math.abs(belt.lane().entries().getLast().at() - QUARTER) < 1.0e-6,
                    "the last a gap behind the one ahead of it, three gaps back");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void droppingBelts_letWhatReachesTheirEndFallOff(final GameTestHelper helper) {
        run(helper, TestConveyors.DROPPING_BELT.get(), FIRST);
        belt(helper, FIRST).put(new ItemStack(Items.EMERALD), 0.5);
        helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.EMERALD, SECOND, 2.0));
    }

    @GameTest(template = ARENA)
    public static void belts_handTheirItemsOnIntoABeltAcrossTheirEnd(final GameTestHelper helper) {
        run(helper, TestConveyors.BELT.get(), FIRST);
        helper.setBlock(SECOND, TestConveyors.BELT.get().defaultBlockState().setValue(ConveyorBlock.FACING,
                Direction.SOUTH));
        belt(helper, FIRST).put(new ItemStack(Items.GOLD_INGOT), 0.5);
        helper.succeedWhen(() -> helper.assertTrue(!belt(helper, SECOND).lane().isEmpty(),
                "the ingot handed onto the belt across"));
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void turnedBelts_moveOnlyWhileAShaftTurnsThem(final GameTestHelper helper) {
        run(helper, TestConveyors.TURNED_BELT.get(), FIRST, SECOND);
        final ConveyorBlockEntity first = belt(helper, FIRST);
        first.put(new ItemStack(Items.IRON_INGOT), 0.0);
        helper.runAfterDelay(STILL, () -> {
            helper.assertTrue(first.lane().entries().getFirst().at() == 0.0, "no shaft, no moving");
            // A crank across the belt, its axis the belt's rollers', turns the whole run.
            helper.setBlock(FIRST.north(), TestMechanical.CRANK.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
            helper.succeedWhen(() -> helper.assertTrue(!belt(helper, SECOND).lane().isEmpty(),
                    "the crank turned the run and the ingot reached the second belt"));
        });
    }

    @GameTest(template = ARENA)
    public static void poweredBelts_runOnEnergyAnyBeltOfTheRunTakesIn(final GameTestHelper helper) {
        run(helper, TestConveyors.POWERED_BELT.get(), FIRST, SECOND);
        final ConveyorBlockEntity first = belt(helper, FIRST);
        first.put(new ItemStack(Items.COPPER_INGOT), 0.0);
        helper.runAfterDelay(STILL, () -> {
            helper.assertTrue(first.lane().entries().getFirst().at() == 0.0, "no energy, no moving");
            final ConveyorBlockEntity second = belt(helper, SECOND);
            second.energy().receiveEnergy(second.energy().getMaxEnergyStored(), false);
            helper.succeedWhen(() -> helper.assertTrue(!second.lane().isEmpty(),
                    "the energy the second belt took in ran the run"));
        });
    }

    @GameTest(template = ARENA)
    public static void belts_takeWhatAHopperGivesAndWhatLandsOnThem(final GameTestHelper helper) {
        run(helper, TestConveyors.BELT.get(), FIRST);
        run(helper, TestConveyors.BELT.get(), THIRD);
        helper.setBlock(FIRST.above(), Blocks.HOPPER);
        if (helper.getBlockEntity(FIRST.above()) instanceof HopperBlockEntity hopper) {
            hopper.setItem(0, new ItemStack(Items.APPLE));
        }
        helper.spawnItem(Items.BREAD, new Vec3(THIRD.getX() + 0.5, Y + 0.6, THIRD.getZ() + 0.5));
        helper.succeedWhen(() -> {
            helper.assertTrue(!belt(helper, FIRST).lane().isEmpty(), "the hopper's apple put on the belt");
            helper.assertTrue(!belt(helper, THIRD).lane().isEmpty(), "the bread that landed taken onto the belt");
            helper.assertItemEntityNotPresent(Items.BREAD);
        });
    }

    @GameTest(template = ARENA)
    public static void belts_carryWhatStandsOnThem(final GameTestHelper helper) {
        run(helper, TestConveyors.BELT.get(), FIRST, SECOND, THIRD);
        // A stone on the run keeps it moving, so it carries what stands on it at its speed.
        belt(helper, FIRST).put(new ItemStack(Items.STONE), 0.0);
        final ArmorStand stand = helper.spawn(EntityType.ARMOR_STAND, new Vec3(FIRST.getX() + 0.5,
                Y + 0.25, FIRST.getZ() + 0.5));
        final double from = stand.getX();
        helper.succeedWhen(() -> helper.assertTrue(stand.getX() > from + 0.5, "the stand carried east"));
    }

    @GameTest(template = ARENA)
    public static void beltItems_surviveAReload(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        run(helper, TestConveyors.BELT.get(), FIRST);
        final ConveyorBlockEntity belt = belt(helper, FIRST);
        belt.put(new ItemStack(Items.REDSTONE, 5), 0.5);
        final CompoundTag saved = belt.saveWithoutMetadata(registries);
        final ConveyorBlockEntity read = new ConveyorBlockEntity(TestConveyors.CONVEYOR.get(), belt.getBlockPos(),
                belt.getBlockState());
        read.loadWithComponents(saved, registries);
        helper.assertTrue(read.lane().size() == 1 && read.lane().entries().getFirst().item().getCount() == 5,
                "the redstone read back");
        helper.succeed();
    }

    /* Belts of {@code block} at {@code places}, each facing east, the way the run moves. */
    private static void run(final GameTestHelper helper, final Block block, final BlockPos... places) {
        for (final BlockPos place : places) {
            helper.setBlock(place, block.defaultBlockState().setValue(ConveyorBlock.FACING, Direction.EAST));
        }
    }

    private static ConveyorBlockEntity belt(final GameTestHelper helper, final BlockPos relative) {
        if (!(helper.getBlockEntity(relative) instanceof ConveyorBlockEntity belt)) {
            throw new GameTestAssertException("no conveyor at " + relative.toShortString());
        }
        return belt;
    }
}
