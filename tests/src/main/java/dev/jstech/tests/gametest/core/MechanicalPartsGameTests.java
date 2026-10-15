/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMechanical;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The parts of the Core's kit of mechanical power in a world: a flywheel coasting after the power is cut, a clutch
 * letting go while powered, a brake holding the line, a speed controller turning its end faster by its signal, a
 * shear pin shearing under overload and a rated shaft breaking, a belt joining two pulleys, and mills driven by their
 * shafts, one working as fast as its shaft turns and one too heavy for its crank.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MechanicalPartsGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 40;

    private MechanicalPartsGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 260)
    public static void flywheel_keepsTurningAfterThePowerIsCut(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.X);
        place(helper, new BlockPos(3, 2, 2), TestMechanical.FLYWHEEL.get(), Direction.Axis.X);
        helper.runAfterDelay(120, () -> {
            final double spun = part(helper, new BlockPos(3, 2, 2)).rpm();
            helper.assertTrue(spun > 50.0, "the crank spins the flywheel up: " + spun);
            helper.setBlock(new BlockPos(1, 2, 2), Blocks.AIR);
            helper.runAfterDelay(20, () -> {
                final double coasting = part(helper, new BlockPos(3, 2, 2)).rpm();
                helper.assertTrue(coasting > 0.7 * spun, "and it coasts on after the crank is gone: " + coasting);
                helper.succeed();
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void clutch_letsGoWhilePowered(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.CLUTCH.get(), Direction.Axis.X);
        place(helper, new BlockPos(3, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.X);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(part(helper, new BlockPos(3, 2, 2)).rpm() > 50.0, "the clutch passes the turn");
            helper.setBlock(new BlockPos(2, 3, 2), Blocks.REDSTONE_BLOCK);
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(part(helper, new BlockPos(3, 2, 2)).rpm() == 0.0,
                        "powered, it lets go: " + part(helper, new BlockPos(3, 2, 2)).rpm());
                helper.assertTrue(part(helper, new BlockPos(1, 2, 2)).rpm() > 50.0, "while the crank turns on");
                helper.setBlock(new BlockPos(2, 3, 2), Blocks.AIR);
                helper.succeedWhen(() -> helper.assertTrue(part(helper, new BlockPos(3, 2, 2)).rpm() > 50.0,
                        "and takes hold again unpowered"));
            });
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void brake_holdsTheLineAsPowered(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.X);
        place(helper, new BlockPos(3, 2, 2), TestMechanical.BRAKE.get(), Direction.Axis.X);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(part(helper, new BlockPos(3, 2, 2)).rpm() > 50.0, "unpowered, the brake is free");
            helper.setBlock(new BlockPos(3, 3, 2), Blocks.REDSTONE_BLOCK);
            helper.succeedWhen(() -> helper.assertTrue(part(helper, new BlockPos(1, 2, 2)).rpm() == 0.0,
                    "powered, its 30 N·m hold the crank's 20: " + part(helper, new BlockPos(1, 2, 2)).rpm()));
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void controller_turnsItsFasterEndByItsSignal(final GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.REDSTONE_BLOCK);
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.CONTROLLER.get(), Direction.Axis.X);
        place(helper, new BlockPos(3, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.X);
        helper.runAfterDelay(SETTLE, () -> {
            final double in = part(helper, new BlockPos(2, 2, 2)).rpm();
            final double out = part(helper, new BlockPos(3, 2, 2)).rpm();
            helper.assertTrue(in > 1.0 && Math.abs(out / in - 2.0) < 0.01,
                    "at full signal its end turns twice as fast: " + out + " against " + in);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void shearPin_shearsUnderOverloadAndSparesTheRest(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SHEAR_PIN.get(), Direction.Axis.X);
        place(helper, new BlockPos(3, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.X);
        place(helper, new BlockPos(4, 2, 2), TestMechanical.LOAD_BLOCK.get(), Direction.Axis.X);
        helper.succeedWhen(() -> {
            helper.assertTrue(part(helper, new BlockPos(2, 2, 2)).sheared(),
                    "the load's 15 N·m shear the pin of 10");
            helper.assertTrue(part(helper, new BlockPos(4, 2, 2)).rpm() == 0.0, "the line beyond it stops");
            helper.assertTrue(part(helper, new BlockPos(1, 2, 2)).rpm() > 50.0, "and the crank runs free, unharmed");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void ratedShaft_breaksUnderOverload(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get(), Direction.Axis.X);
        place(helper, new BlockPos(2, 2, 2), TestMechanical.WEAK_SHAFT.get(), Direction.Axis.X);
        place(helper, new BlockPos(3, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.X);
        place(helper, new BlockPos(4, 2, 2), TestMechanical.LOAD_BLOCK.get(), Direction.Axis.X);
        helper.succeedWhen(() -> helper.assertBlockNotPresent(TestMechanical.WEAK_SHAFT.get(),
                new BlockPos(2, 2, 2)));
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void belt_joinsTwoPulleysAndTheSmallerTurnsFaster(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 1), TestMechanical.CRANK.get(), Direction.Axis.Z);
        place(helper, new BlockPos(1, 2, 2), TestMechanical.SMALL_PULLEY.get(), Direction.Axis.Z);
        place(helper, new BlockPos(4, 2, 2), TestMechanical.LARGE_PULLEY.get(), Direction.Axis.Z);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TestMechanical.BELT.get(), 2));
        use(helper, player, new BlockPos(1, 2, 2));
        use(helper, player, new BlockPos(4, 2, 2));
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "the belt is spent on the join");
        helper.runAfterDelay(SETTLE, () -> {
            final double small = part(helper, new BlockPos(1, 2, 2)).rpm();
            final double large = part(helper, new BlockPos(4, 2, 2)).rpm();
            helper.assertTrue(small > 1.0 && Math.abs(large / small - 0.5) < 0.01,
                    "the large pulley turns the same way, half as fast: " + large + " against " + small);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void mill_worksAsFastAsItsShaftTurns(final GameTestHelper helper) {
        final TestMechanical.MillBlockEntity mill = mill(helper, TestMechanical.MILL.get());
        helper.succeedWhen(() -> {
            helper.assertTrue(mill.getInventory().getStackInSlot(1).is(Items.GRAVEL), "the mill grinds gravel");
            final double rpm = Math.abs(part(helper, new BlockPos(2, 2, 2)).rpm());
            helper.assertTrue(rpm > TestMechanical.MILL_LEAST && rpm < TestMechanical.MILL_RATED,
                    "turning between its least and its rated speed: " + rpm);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void heavyMill_turnsTooSlowlyAndStops(final GameTestHelper helper) {
        final TestMechanical.MillBlockEntity mill = mill(helper, TestMechanical.HEAVY_MILL.get());
        helper.runAfterDelay(100, () -> {
            final double rpm = Math.abs(part(helper, new BlockPos(2, 2, 2)).rpm());
            helper.assertTrue(rpm > 1.0 && rpm < TestMechanical.MILL_LEAST,
                    "its 18 N·m slow the crank below the least it works at: " + rpm);
            helper.assertTrue(mill.status() == MachineStatus.TOO_SLOW, "so it stops: " + mill.status());
            helper.assertTrue(mill.getInventory().getStackInSlot(1).isEmpty(), "having made nothing");
            helper.succeed();
        });
    }

    /* A mill facing north, its back to a shaft and a crank south of it, given cobblestone. */
    private static TestMechanical.MillBlockEntity mill(final GameTestHelper helper, final Block block) {
        helper.setBlock(new BlockPos(2, 2, 1), block);
        final TestMechanical.MillBlockEntity mill = helper.getBlockEntity(new BlockPos(2, 2, 1));
        mill.getInventory().setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 4));
        place(helper, new BlockPos(2, 2, 2), TestMechanical.SHAFT.get(), Direction.Axis.Z);
        place(helper, new BlockPos(2, 2, 3), TestMechanical.CRANK.get(), Direction.Axis.Z);
        return mill;
    }

    /* The player uses what it holds on the top of the block at {@code pos}. */
    private static void use(final GameTestHelper helper, final Player player, final BlockPos pos) {
        final BlockPos at = helper.absolutePos(pos);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final Block block,
                              final Direction.Axis axis) {
        helper.setBlock(pos, block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis));
    }

    private static MechanicalPartBlockEntity part(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos);
    }
}
