/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

/**
 * When a vehicle's lights shine: the headlights dipped or full, the tail lights with any lights on, the brake lights
 * as it brakes, the indicators blinking as they are switched, a little under once a second, and the light inside as it
 * is switched. A broken light never shines. Pure: no part of the game.
 */
public final class VehicleLights {

    /* An indicator is lit for this many ticks, then dark for as many. */
    private static final int BLINK = 8;
    /* A tail light shines this much of its brightness; braking, all of it. */
    private static final float TAIL_SHARE = 0.4F;

    private VehicleLights() {
    }

    /** Whether the indicators are in their lit half of a blink at {@code tick}. */
    public static boolean blinkOn(final long tick) {
        return (tick / BLINK) % 2 == 0;
    }

    /**
     * How brightly a light of {@code role} shines, from nought to 1, with the vehicle's switches as {@code switches},
     * the light worn to {@code health}, and the indicators' blink lit or not.
     */
    public static float shine(final VehicleLightRole role, final VehicleSwitches switches, final float health,
                              final boolean blinkLit) {
        if (health <= 0) {
            return 0;
        }
        final boolean on = switch (role) {
            case HEAD -> switches.lights().headlights();
            case TAIL -> switches.lights() != VehicleSwitches.Lights.OFF;
            case BRAKE -> switches.braking();
            case INDICATOR_LEFT -> switches.indicators().left() && blinkLit;
            case INDICATOR_RIGHT -> switches.indicators().right() && blinkLit;
            case INSIDE -> switches.insideLight();
        };
        if (!on) {
            return 0;
        }
        if (role == VehicleLightRole.TAIL) {
            return TAIL_SHARE;
        }
        if (role == VehicleLightRole.HEAD && switches.lights() != VehicleSwitches.Lights.MAIN) {
            return 1 - TAIL_SHARE;
        }
        return 1;
    }
}
