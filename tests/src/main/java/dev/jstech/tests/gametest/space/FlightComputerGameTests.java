/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.flight.TransferPlan;
import dev.jstech.space.network.FarMapPayload;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.orbit.SolarSystemOrbits;
import dev.jstech.space.orbit.Transfers;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.universe.FarMaps;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Flight Computer: the transfer it plans, from the Overworld to Mars only in a window at its best and dearer out of
 * one, to the Moon whenever at the same cost; its keys, opening the transfer and the landing pages and no other,
 * choosing the world to go to, drawing the map nearer and wider, marking the spot to land on and clearing it, laying
 * the grid; and the landing map of Mars, drawn from its generator.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class FlightComputerGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_flight_computer";
    private static final BlockPos COMPUTER = new BlockPos(2, 2, 2);
    private static final double CLOSE = 1e-6;
    /* A window is a few degrees wide: within it the burn costs at most this much over its least. */
    private static final double WINDOW_SLACK = 1.2;

    private FlightComputerGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void plan_toMarsIsCheapestInItsWindowAndToTheMoonTheSameWhenever(final GameTestHelper helper) {
        long open = -1;
        long shut = -1;
        for (long tick = 0; tick < 2 * Transfers.synodicTicks(SolarSystemOrbits.EARTH, SolarSystemOrbits.MARS)
                && (open < 0 || shut < 0); tick += 600) {
            final boolean in = Transfers.inWindow(SolarSystemOrbits.EARTH, SolarSystemOrbits.MARS, tick);
            if (in && open < 0) {
                open = tick;
            } else if (!in && shut < 0) {
                shut = tick;
            }
        }
        helper.assertTrue(open >= 0 && shut >= 0, "a window and a time out of one are found");
        final TransferPlan best = TransferPlan.of(SolarBody.OVERWORLD, SolarBody.MARS, open).orElseThrow();
        helper.assertTrue(best.windowed() && best.inWindow() && best.toWindow() == 0,
                "in a window, it is open: " + best);
        helper.assertTrue(Math.abs(best.deltaVWindow() - Rocketry.LOW_ORBIT_TO_MARS_TRANSFER) < CLOSE
                && best.deltaVNow() < best.deltaVWindow() * WINDOW_SLACK,
                "in the window the burn costs about the transfer's least: " + best.deltaVNow());
        final TransferPlan later = TransferPlan.of(SolarBody.OVERWORLD, SolarBody.MARS, shut).orElseThrow();
        helper.assertTrue(!later.inWindow() && later.toWindow() > 0 && later.deltaVNow() > later.deltaVWindow(),
                "out of the window it waits for one, or costs more now: " + later);
        helper.assertTrue(later.aerobraking() && later.travel() == SolarSystemOrbits.MARS_TRIP_TICKS,
                "it brakes in Mars' air, eight minutes on: " + later);
        final TransferPlan moon = TransferPlan.of(SolarBody.OVERWORLD, SolarBody.MOON, shut).orElseThrow();
        helper.assertTrue(!moon.windowed() && moon.inWindow() && !moon.aerobraking()
                && Math.abs(moon.deltaVNow() - Rocketry.LOW_ORBIT_TO_MOON_ORBIT) < CLOSE,
                "to the airless Moon, no window, the same burn, by its engines: " + moon);
        helper.assertTrue(TransferPlan.of(SolarBody.MARS, SolarBody.MARS, 0).isEmpty(), "no transfer to itself");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void keys_openTheTransferAndTheLandingAndWorkEachPage(final GameTestHelper helper) {
        helper.setBlock(COMPUTER, RocketKitBlocks.FLIGHT_COMPUTER.get());
        final FlightComputerBlockEntity computer = helper.getBlockEntity(COMPUTER);
        helper.assertTrue(computer.page() == FlightPage.TRANSFER && computer.target().equals(SolarSystemWorlds.MARS),
                "it starts on the transfer to Mars");
        helper.assertTrue(computer.destinations().equals(List.of(SolarSystemWorlds.MOON, SolarSystemWorlds.MARS)),
                "from the Overworld it goes to the Moon or Mars: " + computer.destinations());
        helper.assertTrue(computer.plan().isPresent(), "it plans the transfer from where it stands");
        computer.press(FlightComputerBlockEntity.LEFT);
        helper.assertTrue(computer.target().equals(SolarSystemWorlds.MOON), "the first left key chooses the Moon");
        computer.press(FlightComputerBlockEntity.LEFT + 1);
        helper.assertTrue(computer.target().equals(SolarSystemWorlds.MARS), "the second, Mars");
        computer.press(FlightComputerBlockEntity.TOP);
        helper.assertTrue(computer.page() == FlightPage.TRANSFER, "the system's page does not open yet");
        computer.press(FlightComputerBlockEntity.TOP + 2);
        helper.assertTrue(computer.page() == FlightPage.LANDING, "the landing's key opens it");
        computer.press(FlightComputerBlockEntity.LEFT);
        helper.assertTrue(computer.zoom() == FlightComputerBlockEntity.NEAR, "zoomed in, the map is the nearer");
        computer.press(FlightComputerBlockEntity.LEFT + 1);
        helper.assertTrue(computer.zoom() == FlightComputerBlockEntity.WIDE, "zoomed out, the wider");
        computer.press(FlightComputerBlockEntity.LEFT + 2);
        helper.assertTrue(BlockPos.ZERO.equals(computer.mark()), "marked where the map looks, the world's middle");
        computer.markAt(1200, -800);
        helper.assertTrue(new BlockPos(1200, 0, -800).equals(computer.mark()), "a spot clicked is marked");
        computer.press(FlightComputerBlockEntity.RIGHT + 2);
        helper.assertTrue(computer.grid(), "the grid is laid");
        computer.press(FlightComputerBlockEntity.LEFT + 3);
        helper.assertTrue(computer.mark() == null, "the mark is cleared");
        computer.press(FlightComputerBlockEntity.TOP + 1);
        computer.press(FlightComputerBlockEntity.LEFT);
        helper.assertTrue(computer.page() == FlightPage.TRANSFER && computer.target().equals(SolarSystemWorlds.MOON),
                "back on the transfer, the Moon chosen");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 400)
    public static void landingMap_ofMarsIsDrawnFromItsGenerator(final GameTestHelper helper) {
        final ServerLevel mars = helper.getLevel().getServer().getLevel(SpaceWorlds.get(helper.getLevel(),
                SolarSystemWorlds.MARS).orElseThrow().dimension());
        helper.assertTrue(mars != null, "Mars is on the server");
        final FarMapPayload map = FarMaps.draw(mars, FlightComputerBlockEntity.WIDE, 0, 0);
        helper.assertTrue(map.blocks() == FlightComputerBlockEntity.WIDE_BLOCKS && !map.biomes().isEmpty()
                && map.pixels().length == map.size() * map.size()
                && map.biomes().stream().allMatch(biome -> biome.getPath().startsWith("mars")),
                "its wider map is drawn, of Mars' own ground: " + map.biomes());
        helper.succeed();
    }
}
