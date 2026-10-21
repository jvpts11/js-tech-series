/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VehicleDriveTest {

    private static final VehicleDrive.Engine DIESEL = new VehicleDrive.Engine(90_000, 800, 4500, 0.3, false, 1);
    private static final VehicleDrive.Engine MOTOR = new VehicleDrive.Engine(60_000, 0, 9000, 0.9, true, 1);
    private static final VehicleDrive.Gearbox FIVE = new VehicleDrive.Gearbox(List.of(3.6, 2.1, 1.4, 1.0, 0.8), 3.4,
            4.1);
    private static final double RADIUS = 0.34;
    private static final double TICK = 0.05;
    private static final double AIR = 20;

    private VehicleDrive drive;

    @BeforeEach
    void makeDrive() {
        drive = new VehicleDrive();
    }

    @Test
    void step_pushesAheadFromAStandstillWithItsClutchSlipping() {
        final VehicleDrive.Result result = drive.step(DIESEL, FIVE, RADIUS, ahead(1), 0, AIR, TICK);

        assertTrue(result.force() > 1000, "it pulls away: " + result.force());
        assertTrue(result.rpm() > DIESEL.idleRpm(), "above its idle as the clutch slips: " + result.rpm());
        assertEquals(1, result.gear());
    }

    @Test
    void step_changesUpAsTheSpeedRises() {
        int gear = 0;
        for (int speed = 1; speed <= 40; speed++) {
            for (int i = 0; i < 20; i++) {
                gear = drive.step(DIESEL, FIVE, RADIUS, ahead(1), speed, AIR, TICK).gear();
            }
        }

        assertEquals(5, gear, "in its top gear at 40 metres a second");
    }

    @Test
    void step_changesDownAsItSlows() {
        for (int i = 0; i < 40; i++) {
            drive.step(DIESEL, FIVE, RADIUS, ahead(1), 30, AIR, TICK);
        }
        int gear = 5;
        for (int i = 0; i < 200; i++) {
            gear = drive.step(DIESEL, FIVE, RADIUS, ahead(0.2), 3, AIR, TICK).gear();
        }

        assertEquals(1, gear, "back in first at walking pace");
    }

    @Test
    void step_goesIntoReverseWhenAskedAtAStandstill() {
        final VehicleDrive.Result result = drive.step(DIESEL, FIVE, RADIUS,
                new VehicleDrive.Controls(1, true, true, 0, true), 0, AIR, TICK);

        assertEquals(-1, result.gear());
        assertTrue(result.force() < 0, "it pushes backwards: " + result.force());
    }

    @Test
    void step_givesNoPushAtTheEnginesLimit() {
        final double atLimit = DIESEL.maxRpm() / FIVE.ratio(5) / FIVE.finalDrive() / 60 * 2 * Math.PI * RADIUS;
        for (int i = 0; i < 40; i++) {
            drive.step(DIESEL, FIVE, RADIUS, ahead(1), atLimit * 1.2, AIR, TICK);
        }

        assertEquals(0, drive.step(DIESEL, FIVE, RADIUS, ahead(1), atLimit * 1.2, AIR, TICK).force());
    }

    @Test
    void step_burnsWhileRunningAndNothingWhenOff() {
        final double idling = drive.step(DIESEL, FIVE, RADIUS, ahead(0), 0, AIR, TICK).joules();
        final double working = drive.step(DIESEL, FIVE, RADIUS, ahead(1), 10, AIR, TICK).joules();
        final double off = drive.step(DIESEL, FIVE, RADIUS, new VehicleDrive.Controls(1, false, true, 0, false), 10,
                AIR, TICK).joules();

        assertTrue(idling > 0, "an idling engine burns to turn itself over");
        assertTrue(working > idling * 5, "working, far more: " + working + " against " + idling);
        assertEquals(0, off);
    }

    @Test
    void step_letsAnElectricMotorPullHardestFromAStandstill() {
        final VehicleDrive electric = new VehicleDrive();
        final double still = electric.step(MOTOR, VehicleDrive.Gearbox.SINGLE, RADIUS, ahead(1), 0, AIR, TICK)
                .force();
        final double fast = electric.step(MOTOR, VehicleDrive.Gearbox.SINGLE, RADIUS, ahead(1), 30, AIR, TICK)
                .force();

        assertTrue(still > fast, "full force still, less at speed: " + still + " and " + fast);
        assertEquals(0, electric.step(MOTOR, VehicleDrive.Gearbox.SINGLE, RADIUS, ahead(0), 0, AIR, TICK).joules(),
                "an electric motor draws nothing to idle");
    }

    @Test
    void step_warmsTheEngineToItsWorkingHeat() {
        for (int i = 0; i < 20 * 600; i++) {
            drive.step(DIESEL, FIVE, RADIUS, ahead(0.3), 10, AIR, TICK);
        }

        assertEquals(AIR + 70, drive.temperature(AIR), 2);
    }

    @Test
    void step_changesAManualGearboxOnlyWhenTold() {
        final VehicleDrive.Controls hold = new VehicleDrive.Controls(1, false, false, 0, true);
        for (int i = 0; i < 40; i++) {
            drive.step(DIESEL, FIVE, RADIUS, hold, 25, AIR, TICK);
        }
        assertEquals(1, drive.gear(), "a manual gearbox stays where it is");

        drive.step(DIESEL, FIVE, RADIUS, new VehicleDrive.Controls(1, false, false, 1, true), 25, AIR, TICK);

        assertEquals(2, drive.gear());
    }

    @Test
    void engine_refusesATopSpeedBelowItsIdle() {
        assertThrows(IllegalArgumentException.class, () -> new VehicleDrive.Engine(1000, 3000, 2000, 0.3, false, 1));
    }

    private static VehicleDrive.Controls ahead(final double throttle) {
        return new VehicleDrive.Controls(throttle, false, true, 0, true);
    }
}
