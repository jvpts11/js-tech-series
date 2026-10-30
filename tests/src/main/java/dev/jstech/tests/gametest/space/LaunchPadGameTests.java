/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.space.launcher.LauncherBlocks;
import dev.jstech.space.launcher.LauncherPieceBlock;
import dev.jstech.space.pad.LaunchPads;
import dev.jstech.space.pad.PadBlocks;
import dev.jstech.space.pad.PadServicePortBlockEntity;
import dev.jstech.tests.JsTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The launch pad: its flame trench under the launcher's opening, a Flame Deflector below the ground spanning the trench
 * from one lined wall to the other, protects it; its service port, filled with water and its pumps' energy, holds a
 * deluge, poured at ignition; a pad with neither is blown apart at ignition; a thunderstorm holds the countdown; and
 * three Pad Pedestals in a column take their shape.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class LaunchPadGameTests {

    private static final String TALL = "tall";
    private static final String BATCH = "jsspace_pad";
    /* Where the rocket's axis meets the deck's top, six over the ground at y 4; the deflector two under the ground. */
    private static final BlockPos AXIS = new BlockPos(6, 10, 6);
    private static final BlockPos DEFLECTOR = new BlockPos(6, 2, 6);
    private static final BlockPos PORT = new BlockPos(2, 4, 2);
    private static final BlockPos CONCRETE = new BlockPos(6, 3, 6);

    private LaunchPadGameTests() {
    }

    @GameTest(template = TALL, batch = BATCH)
    public static void pad_isProtectedByItsLinedTrench(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        trench(helper);
        final BlockPos axis = helper.absolutePos(AXIS);
        helper.assertTrue(LaunchPads.protection(level, axis, Direction.EAST).trench(),
                "a deflector under the opening, spanning the trench between lined walls, is a flame trench");
        helper.assertFalse(LaunchPads.protection(level, axis, Direction.NORTH).trench(),
                "its walls are along the launcher's length, not across it");
        helper.setBlock(DEFLECTOR.south(2), Blocks.STONE);
        helper.assertFalse(LaunchPads.protection(level, axis, Direction.EAST).trench(),
                "a wall not lined is no flame trench");
        helper.succeed();
    }

    @GameTest(template = TALL, batch = BATCH)
    public static void pad_poursItsDelugeAtIgnition(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos axis = helper.absolutePos(AXIS);
        helper.setBlock(PORT, PadBlocks.PAD_SERVICE_PORT.get());
        helper.setBlock(CONCRETE, Blocks.STONE);
        final BlockPos port = helper.absolutePos(PORT);
        final IFluidHandler pipe = level.getCapability(Capabilities.FluidHandler.BLOCK, port, Direction.UP);
        final IEnergyStorage cable = level.getCapability(Capabilities.EnergyStorage.BLOCK, port, Direction.UP);
        helper.assertTrue(pipe != null && cable != null, "the port takes water by pipe and energy by cable");
        helper.assertTrue(pipe.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE) == 0,
                "only water");
        pipe.fill(new FluidStack(Fluids.WATER, PadServicePortBlockEntity.DELUGE_WATER),
                IFluidHandler.FluidAction.EXECUTE);
        helper.assertFalse(LaunchPads.protection(level, axis, Direction.EAST).deluge(),
                "water without its pumps' energy is no deluge");
        int given = 0;
        for (int tick = 0; tick < 100 && given < PadServicePortBlockEntity.DELUGE_ENERGY; tick++) {
            given += cable.receiveEnergy(PadServicePortBlockEntity.DELUGE_ENERGY, false);
        }
        helper.assertTrue(LaunchPads.protection(level, axis, Direction.EAST).deluge(), "both, it holds a deluge");
        final LaunchPads.Protection fired = LaunchPads.ignite(level, axis, Direction.EAST);
        helper.assertTrue(fired.deluge() && fired.safe(), "at ignition it pours it");
        helper.assertTrue(level.getBlockEntity(port) instanceof PadServicePortBlockEntity poured
                && poured.water() == 0 && poured.energy() == 0, "spending its water and its energy");
        helper.assertTrue(level.getBlockState(helper.absolutePos(CONCRETE)).is(Blocks.STONE),
                "and the pad takes no harm");
        helper.succeed();
    }

    @GameTest(template = TALL, batch = BATCH)
    public static void pad_withNeitherTrenchNorDelugeIsWreckedAtIgnition(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(CONCRETE, Blocks.STONE);
        final LaunchPads.Protection fired = LaunchPads.ignite(level, helper.absolutePos(AXIS), Direction.EAST);
        helper.assertFalse(fired.safe(), "a pad with neither trench nor deluge is not safe");
        helper.assertTrue(level.getBlockState(helper.absolutePos(CONCRETE)).isAir(),
                "and its ground is blown apart under the opening");
        helper.succeed();
    }

    @GameTest(template = TALL, batch = BATCH)
    public static void pad_holdsTheCountdownInAThunderstorm(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        level.setWeatherParameters(6000, 0, false, false);
        level.setThunderLevel(0.0F);
        level.setRainLevel(0.0F);
        helper.assertFalse(LaunchPads.stormHolds(level), "in fair weather the countdown goes on");
        // The storm set at its height at once, and cleared again in the same tick, so no other test sees it.
        level.setWeatherParameters(0, 6000, true, true);
        level.setRainLevel(1.0F);
        level.setThunderLevel(1.0F);
        final boolean held = LaunchPads.stormHolds(level);
        level.setWeatherParameters(6000, 0, false, false);
        level.setThunderLevel(0.0F);
        level.setRainLevel(0.0F);
        helper.assertTrue(held, "in a thunderstorm the panel holds it");
        helper.succeed();
    }

    @GameTest(template = TALL, batch = BATCH)
    public static void pad_pedestalsTakeTheirShapeThreeInAColumn(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos foot = new BlockPos(3, 2, 3);
        for (int y = 0; y < 3; y++) {
            helper.setBlock(foot.above(y), LauncherBlocks.PAD_PEDESTAL.get());
        }
        helper.assertTrue(role(level, helper.absolutePos(foot)) == LauncherPieceBlock.Role.END
                        && role(level, helper.absolutePos(foot.above())) == LauncherPieceBlock.Role.BODY
                        && role(level, helper.absolutePos(foot.above(2))) == LauncherPieceBlock.Role.TOP,
                "three in a column take their shape: foot, column, cap");
        helper.setBlock(foot.above(3), LauncherBlocks.PAD_PEDESTAL.get());
        helper.assertFalse(level.getBlockState(helper.absolutePos(foot)).getValue(LauncherPieceBlock.FORMED),
                "four are no pedestal");
        helper.succeed();
    }

    /* The flame trench under the opening: a row of three deflectors across it, two under the ground, lined walls. */
    private static void trench(final GameTestHelper helper) {
        for (int z = -1; z <= 1; z++) {
            helper.setBlock(DEFLECTOR.south(z), PadBlocks.FLAME_DEFLECTOR.get().defaultBlockState());
        }
        helper.setBlock(DEFLECTOR.south(2), PadBlocks.TRENCH_LINING.get());
        helper.setBlock(DEFLECTOR.north(2), PadBlocks.TRENCH_LINING.get());
    }

    private static LauncherPieceBlock.Role role(final ServerLevel level, final BlockPos at) {
        final BlockState state = level.getBlockState(at);
        return state.getValue(LauncherPieceBlock.FORMED) ? state.getValue(LauncherPieceBlock.ROLE) : null;
    }
}
