/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.grid.LineSurvey;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.ProbeLink;
import dev.jstech.core.jstechseries.probe.ProbeSessions;
import dev.jstech.core.mechanical.MechanicalPartBlockEntity;
import dev.jstech.core.mechanical.MechanicalReadings;
import dev.jstech.core.mechanical.RotationMath;
import dev.jstech.core.signal.ISignalOutput;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.signal.Signals;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMechanical;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Reading mechanical power: a torque meter measuring the torque through it, every part a sensor of its speed, and the
 * network probe linked to a part reading its network of shafts in watts, its sources and its loads.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MechanicalInstrumentsGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 60;
    /* What the meter carries at the balance: the load's 15 N·m and the shaft's 0.1 of friction beyond it. */
    private static final double CARRIED = TestMechanical.LOAD + 0.1;

    private MechanicalInstrumentsGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void torqueMeter_measuresTheTorqueThroughIt(final GameTestHelper helper) {
        line(helper);
        helper.runAfterDelay(SETTLE, () -> {
            final MechanicalPartBlockEntity meter = part(helper, new BlockPos(2, 2, 2));
            helper.assertTrue(Math.abs(Math.abs(meter.torqueThrough()) - CARRIED) < 0.3,
                    "it carries what the load and the shaft beyond it ask: " + meter.torqueThrough());
            final List<Reading> readings = meter.readings();
            helper.assertTrue(readings.size() == 2 && readings.get(1).name() == MechanicalReadings.TORQUE
                    && Math.abs(readings.get(1).value() - CARRIED) < 0.3, "and a sensor reads it: " + readings);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void part_isASensorOfItsSpeed(final GameTestHelper helper) {
        line(helper);
        helper.runAfterDelay(SETTLE, () -> {
            final BlockPos at = helper.absolutePos(new BlockPos(3, 2, 2));
            final ISignalOutput sensor = helper.getLevel().getCapability(Signals.OUTPUT, at, null);
            helper.assertTrue(sensor != null, "whatever reads sensors reads the shaft");
            final Reading speed = sensor.readings().getFirst();
            helper.assertTrue(speed.name() == MechanicalReadings.SPEED
                    && Math.abs(speed.value() - Math.abs(part(helper, new BlockPos(3, 2, 2)).rpm())) < 1e-6,
                    "its speed, as a tachometer: " + speed);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void probe_readsANetworkOfShaftsInWatts(final GameTestHelper helper) {
        line(helper);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack probe = new ItemStack(NetworkProbe.ITEM.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, probe);
        final BlockPos crank = helper.absolutePos(new BlockPos(1, 2, 2));
        probe.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(crank), Direction.UP, crank, false)));
        final ProbeLink link = player.getMainHandItem().get(NetworkProbe.LINK.get());
        helper.assertTrue(link != null && NetworkProbe.MECHANICAL.equals(link.cable()),
                "used on the crank, it links to its network of shafts: " + link);
        helper.runAfterDelay(SETTLE, () -> {
            final Optional<ProbeSessions.Read> read = ProbeSessions.read(helper.getLevel(), link);
            helper.assertTrue(read.isPresent() && read.get().cable() == null, "it reads the network");
            final LineSurvey survey = read.get().survey();
            final double rpm = part(helper, new BlockPos(1, 2, 2)).rpm();
            final double watts = RotationMath.power(CARRIED, rpm);
            helper.assertTrue(Math.abs(survey.moved() - watts) < 2.0,
                    "the power the crank gives: " + survey.moved() + " W, expected " + watts);
            helper.assertTrue(survey.cables() == 4, "four parts: " + survey.cables());
            helper.assertTrue(survey.members().stream().anyMatch(member -> member.role() == LineSurvey.Role.SOURCE)
                    && survey.members().stream().anyMatch(member -> member.role() == LineSurvey.Role.CONSUMER),
                    "the crank a source and the load a consumer");
            helper.succeed();
        });
    }

    /* A crank, a torque meter, a shaft and a load, along x. */
    private static void line(final GameTestHelper helper) {
        place(helper, new BlockPos(1, 2, 2), TestMechanical.CRANK.get());
        place(helper, new BlockPos(2, 2, 2), TestMechanical.TORQUE_METER.get());
        place(helper, new BlockPos(3, 2, 2), TestMechanical.SHAFT.get());
        place(helper, new BlockPos(4, 2, 2), TestMechanical.LOAD_BLOCK.get());
    }

    private static void place(final GameTestHelper helper, final BlockPos pos, final Block block) {
        helper.setBlock(pos, block.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
    }

    private static MechanicalPartBlockEntity part(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos);
    }
}
