/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.space.api.Propellants;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.flight.PlannedTransfer;
import dev.jstech.space.flight.TransferChoice;
import dev.jstech.space.flight.TransferPlan;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.SkeletonRocketParts;
import dev.jstech.space.rocket.TankLoad;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Planning a transfer on the Flight Computer's XFER page: leaving now costs the delta-v of now and a window that of the
 * window, each window later by the time between two; spending more than the least shortens the cruise; a gravity
 * assist spends less and takes longer; aerobraking where there is air and a heat shield saves the capture burn; to the
 * Moon there is no window and no assist. Its keys step the window, the delta-v spent, the assist and aerobraking, and
 * EXEC sets the plan to fly; it tells whether its rocket carries a heat shield and what delta-v it has.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TransferPlanningGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_transfer_planning";
    private static final BlockPos COMPUTER = new BlockPos(1, 2, 1);
    /* Half a turn of the Overworld's year: a moment out of the window. */
    private static final long TICK = 1_000;
    private static final double CLOSE = 1.0e-6;

    private TransferPlanningGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void plan_windowsSpendAssistAndAerobrakingShapeIt(final GameTestHelper helper) {
        final TransferPlan mars = TransferPlan.of(SolarBody.OVERWORLD, SolarBody.MARS, TICK).orElseThrow();
        final PlannedTransfer now = PlannedTransfer.of(mars, new TransferChoice(0, 0, false, true), true, TICK);
        final PlannedTransfer next = PlannedTransfer.of(mars, TransferChoice.CHEAPEST, true, TICK);
        final PlannedTransfer later = PlannedTransfer.of(mars, new TransferChoice(2, 0, false, true), true, TICK);
        helper.assertTrue(now.leaves() == TICK && Math.abs(now.burn() - mars.deltaVNow()) < CLOSE,
                "leaving now costs the delta-v of now");
        helper.assertTrue(next.leaves() == TICK + mars.toWindow() && Math.abs(next.burn() - mars.deltaVWindow())
                < CLOSE, "the next window costs the window's");
        helper.assertTrue(later.leaves() == next.leaves() + mars.between(),
                "each window later by the time between two");
        final PlannedTransfer faster = PlannedTransfer.of(mars, new TransferChoice(1, 2, false, true), true, TICK);
        helper.assertTrue(Math.abs(faster.burn() - next.burn() * TransferChoice.SPENDS[2]) < CLOSE
                && faster.travel() < next.travel(), "spending more shortens the cruise: " + faster.travel()
                + " against " + next.travel());
        final PlannedTransfer assisted = PlannedTransfer.of(mars, new TransferChoice(1, 0, true, true), true, TICK);
        helper.assertTrue(assisted.assisted() && assisted.burn() < next.burn() && assisted.travel() > next.travel(),
                "a gravity assist spends less and takes longer");
        helper.assertTrue(next.aerobraking() && next.capture() == 0, "aerobraking in Mars' air saves the capture");
        final PlannedTransfer bare = PlannedTransfer.of(mars, TransferChoice.CHEAPEST, false, TICK);
        helper.assertTrue(!bare.aerobraking() && bare.capture() == Rocketry.MARS_CAPTURE
                && bare.needed() == bare.burn() + Rocketry.MARS_CAPTURE, "with no heat shield it burns to capture");
        final TransferPlan moon = TransferPlan.of(SolarBody.OVERWORLD, SolarBody.MOON, TICK).orElseThrow();
        final PlannedTransfer toMoon = PlannedTransfer.of(moon, new TransferChoice(3, 0, true, true), true, TICK);
        helper.assertTrue(toMoon.leaves() == TICK && !toMoon.assisted() && !toMoon.aerobraking(),
                "to the airless Moon: no window, no assist, no aerobraking");
        helper.assertTrue(toMoon.to().equals(SolarSystemWorlds.worldOf(SolarBody.MOON)), "it goes to the Moon");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void computer_xferKeysPlanTheTransferAndExecSetsIt(final GameTestHelper helper) {
        helper.setBlock(COMPUTER, RocketKitBlocks.FLIGHT_COMPUTER.get());
        final FlightComputerBlockEntity computer = helper.getBlockEntity(COMPUTER);
        helper.assertTrue(!computer.shield() && computer.available().isEmpty(), "no rocket kept, nothing known");
        final RocketStack stack = RocketStack.of(new RocketStack.Part(SkeletonRocketParts.id("vacuum_engine_3"), 1),
                new RocketStack.Part(SkeletonRocketParts.id("tank_3"), 4),
                new RocketStack.Part(SkeletonRocketParts.id("capsule_3"), 1));
        computer.carry(new RocketDesign("PLANNED", stack), BlockPos.ZERO, List.of());
        Propellants.get(helper.getLevel(), SkeletonPropellants.METHALOX).ifPresent(methalox -> computer.load(1,
                TankLoad.filled(SkeletonPropellants.METHALOX, methalox, 23_200.0)));
        helper.assertTrue(computer.shield(), "its capsule's heat shield is known");
        helper.assertTrue(computer.available().isPresent(), "its delta-v is known: " + computer.available());
        computer.press(FlightComputerBlockEntity.TOP + FlightPage.KEYS.indexOf(FlightPage.TRANSFER));
        final int worlds = computer.destinations().size();
        computer.press(FlightComputerBlockEntity.LEFT + worlds + 1);
        computer.press(FlightComputerBlockEntity.RIGHT);
        computer.press(FlightComputerBlockEntity.RIGHT + 2);
        computer.press(FlightComputerBlockEntity.RIGHT + 3);
        final TransferChoice chosen = computer.choice();
        helper.assertTrue(chosen.window() == 2 && chosen.spend() == 1 && chosen.assist() && !chosen.aero(),
                "its keys step the window, the delta-v, the assist and aerobraking: " + chosen);
        helper.assertTrue(computer.executed().isEmpty(), "nothing set to fly yet");
        computer.press(FlightComputerBlockEntity.RIGHT + 4);
        final PlannedTransfer set = computer.executed().orElseThrow();
        helper.assertTrue(set.to().equals(computer.target()) && set.assisted() && !set.aerobraking(),
                "EXEC sets the plan to fly: " + set);
        computer.press(FlightComputerBlockEntity.LEFT);
        helper.assertTrue(computer.executed().isEmpty(), "another destination chosen, the plan set is let go");
        helper.succeed();
    }
}
