/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.PartRedstone;
import dev.jstech.core.multipart.PartStates;
import dev.jstech.core.multipart.SpaceSlots;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestToolkit;
import dev.jstech.tests.testkit.TestCables;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * What a part does on its own, face by face: it ticks only when it needs to, its light and its need of a tick are in
 * its block's state, it gives redstone out of its face and a comparator reading, wire beside a face of a kind that
 * deals in redstone joins it, it hears the redstone reaching its face, its kind's own room keeps others out, and a
 * menu on it stays open only while it is there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PartPowersGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos CABLE = new BlockPos(3, 2, 3);
    private static final int FULL = 15;

    private PartPowersGameTests() {
    }

    @GameTest(template = ARENA)
    public static void parts_tickOnlyWhenTheyNeedTo(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        helper.assertTrue(!ticking(helper), "a bare cable is never ticked");
        cable.addPart(Direction.NORTH, TestToolkit.LAMP_PART.get().create());
        helper.assertTrue(!ticking(helper), "nor one with a lamp, which needs no tick");
        cable.addPart(Direction.SOUTH, TestToolkit.PROBE_PART.get().create());
        helper.assertTrue(ticking(helper), "a part that needs its tick has it");
        cable.removePart(Direction.SOUTH);
        helper.assertTrue(!ticking(helper), "and without it the cable rests again");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void parts_giveLightThroughTheBlocksState(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        final TestToolkit.LampPart lamp = TestToolkit.LAMP_PART.get().create();
        cable.addPart(Direction.UP, lamp);
        final BlockPos pos = helper.absolutePos(CABLE);
        same(helper, 0, helper.getLevel().getBlockState(pos).getLightEmission(helper.getLevel(), pos), "unlit");
        lamp.set(true);
        same(helper, TestToolkit.LAMP_LIGHT, helper.getLevel().getBlockState(pos).getLightEmission(helper.getLevel(),
                pos), "the lit lamp's light");
        lamp.set(false);
        same(helper, 0, helper.getLevel().getBlockState(pos).getValue(PartStates.LIGHT), "put out");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void parts_giveRedstoneOutOfTheirFaceAndAReading(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        final TestToolkit.LampPart lamp = TestToolkit.LAMP_PART.get().create();
        cable.addPart(Direction.EAST, lamp);
        final BlockPos pos = helper.absolutePos(CABLE);
        // A comparator east of the cable, facing west, reads the cable.
        helper.setBlock(CABLE.east(), Blocks.COMPARATOR.defaultBlockState()
                .setValue(ComparatorBlock.FACING, Direction.WEST));
        lamp.set(true);

        same(helper, FULL, level.getSignal(pos, Direction.WEST), "the lamp's face gives the block east of it");
        same(helper, 0, level.getSignal(pos, Direction.EAST), "the west face gives nothing");
        same(helper, TestToolkit.LAMP_READING, level.getBlockState(pos).getAnalogOutputSignal(level, pos),
                "the comparator east reads the lamp");
        helper.assertTrue(PartRedstone.joins(level, pos, Direction.WEST), "wire east of the lamp joins it");
        helper.assertTrue(!PartRedstone.joins(level, pos, Direction.EAST), "wire west of the cable does not");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void parts_hearTheRedstoneReachingTheirFace(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        final TestToolkit.SensorPart sensor = TestToolkit.SENSOR_PART.get().create();
        cable.addPart(Direction.NORTH, sensor);
        helper.setBlock(CABLE.north(), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> helper.assertTrue(sensor.heard() == FULL,
                "the sensor hears the redstone block on its face, got " + sensor.heard()));
    }

    @GameTest(template = ARENA)
    public static void partKinds_takeTheirOwnRoom(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        helper.assertTrue(cable.parts().add(new Microblock(new Microblock.MicroblockPiece(MicroShape.FACE,
                MicroShape.PANEL, Blocks.STONE), SpaceSlots.face(Direction.DOWN.get3DDataValue()), SpaceSlots.Y)),
                "a panel on the floor");
        helper.assertTrue(!cable.addPart(Direction.NORTH, TestToolkit.PROBE_PART.get().create()),
                "a device's plate would reach into the panel");
        helper.assertTrue(cable.addPart(Direction.NORTH, TestToolkit.LAMP_PART.get().create()),
                "the lamp's own small box stands above it");
        helper.assertTrue(TestToolkit.LAMP_PART.get().boxesOn(Direction.DOWN.get3DDataValue()).getFirst()
                .equals(TestToolkit.LAMP_BOX.turnedTo(Direction.DOWN.get3DDataValue())), "turned to the floor");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void partMenus_stayOpenOnlyWhileThePartIsThere(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, SharedEnergy.INSULATED_COPPER_CABLE);
        final TestToolkit.LampPart lamp = TestToolkit.LAMP_PART.get().create();
        cable.addPart(Direction.UP, lamp);
        final BlockPos pos = helper.absolutePos(CABLE);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(pos.getX() + 0.5, pos.getY() - 1.0, pos.getZ() + 1.5);
        final Predicate<Player> valid = MenuValidity.part(helper.getLevel(), pos, Direction.UP, lamp);
        helper.assertTrue(valid.test(player), "open while the lamp is on the cable");
        cable.removePart(Direction.UP);
        helper.assertTrue(!valid.test(player), "closed once it is taken off");
        helper.succeed();
    }

    private static boolean ticking(final GameTestHelper helper) {
        final BlockState state = helper.getLevel().getBlockState(helper.absolutePos(CABLE));
        return state.getValue(PartStates.TICKING);
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
