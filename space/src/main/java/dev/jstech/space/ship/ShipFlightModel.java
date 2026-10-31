/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.space.api.ShipPart;

/**
 * How a small ship flies, a tick at a time, in the spirit of No Man's Sky: it goes where its nose points, with no
 * drift, at a speed its pilot's throttle sets and its engines reach. In the air its air engines push and its wings
 * hold it up, more as the square of its speed and less in thinner air, and flying too slowly it sinks; in the thin air
 * of the climb and in space its bells push, and in space it flies free. Its weight counts: a heavier ship gathers speed
 * more slowly and needs more of it to stay up. Its boost, a store its reactor fills, takes it faster across the empty
 * stretches. The numbers are the game's, not a real ship's: its speeds are kept to what the world can be drawn and
 * loaded at.
 */
public final class ShipFlightModel {

    /** Seconds a tick. */
    public static final double TICK = 0.05;
    /** How hard the overworld pulls, in metres a second squared. */
    public static final double GRAVITY = 9.81;
    /** The speed a wing's figure is given at, in metres a second. */
    public static final double WING_SPEED = ShipPart.Figures.WING_SPEED;
    /* The fastest it goes at full throttle in the air and in space, in metres a second, and boosted. */
    private static final double CRUISE_AIR = 40.0;
    private static final double CRUISE_SPACE = 120.0;
    private static final double BOOSTED_AIR = 60.0;
    private static final double BOOSTED_SPACE = 300.0;
    /* How fast it sheds speed with less throttle, in metres a second squared: the air brakes it, space less. */
    private static final double SLOWING_AIR = 6.0;
    private static final double SLOWING_SPACE = 3.0;
    /* How fast the throttle opens and closes with its pilot's keys, and closes with nobody at the helm, a tick. */
    private static final double OPENS = 0.025;
    private static final double CLOSES = 0.04;
    /* How fast it turns and pitches, in degrees a second, and how far its nose goes up or down. */
    private static final double TURNS = 45.0;
    private static final double PITCHES = 30.0;
    private static final double MOST_PITCH = 60.0;
    /* The fastest it sinks, in metres a second. */
    private static final double MOST_SINK = 40.0;
    /* How long a full boost lasts, in seconds, and the energy it holds, in joules. */
    private static final double BOOST_SECONDS = 8.0;
    private static final double BOOST_ENERGY = 160.0e6;

    private ShipFlightModel() {
    }

    /**
     * The ship one tick on from {@code now}, its pilot holding {@code held}, in the air {@code air}.
     *
     * @param now   where its flight stands
     * @param held  what its pilot holds
     * @param air   the air round it, and its pull
     * @param ship  what it weighs and pushes
     * @param pitch how far its nose points up now, in degrees, down below nought
     */
    public static Tick step(final State now, final Held held, final Air air, final Performance ship,
                            final double pitch) {
        final double thick = Math.clamp(air.density(), 0.0, 1.0);
        final double push = push(ship, thick);
        final double throttle = held.piloted()
                ? Math.clamp(now.throttle() + held.forward() * OPENS - held.back() * CLOSES, 0.0, 1.0)
                : Math.max(0.0, now.throttle() - CLOSES);
        final boolean boosting = held.piloted() && held.boost() && air.empty() && now.charge() > 0 && push > 0;
        final double cruise = between(CRUISE_SPACE, CRUISE_AIR, thick);
        final double wanted = boosting ? between(BOOSTED_SPACE, BOOSTED_AIR, thick) : throttle * cruise;
        final double gathers = ship.mass() > 0 ? push / ship.mass() : 0;
        final double sheds = between(SLOWING_SPACE, SLOWING_AIR, thick);
        final double speed = now.speed() < wanted ? Math.min(wanted, now.speed() + gathers * TICK)
                : Math.max(wanted, now.speed() - sheds * TICK);
        final double pull = GRAVITY * Math.max(0.0, air.gravity());
        final double up = held(ship, speed, thick, pull);
        final double sink = up >= 1.0 ? Math.max(0.0, now.sink() - Math.max(pull, sheds) * TICK)
                : Math.min(MOST_SINK, now.sink() + pull * (1.0 - up) * TICK);
        final double charge = boosting ? Math.max(0.0, now.charge() - TICK / BOOST_SECONDS)
                : Math.min(1.0, now.charge() + Math.max(0.0, ship.power()) * TICK / BOOST_ENERGY);
        final double turn = (held.left() - held.right()) * TURNS * TICK;
        final double lifted = Math.clamp(pitch + (held.up() - held.down()) * PITCHES * TICK, -MOST_PITCH,
                MOST_PITCH) - pitch;
        return new Tick(new State(speed, sink, throttle, charge), turn, lifted, boosting, up < 1.0 && pull > 0,
                (1.0 - thick) * (push > 0 ? 1.0 : 0.0));
    }

    /**
     * How hard its engines push in air {@code thick} of the overworld's at sea level, in newtons: its air engines by
     * the air they breathe, its bells by what the air leaves them.
     */
    public static double push(final Performance ship, final double thick) {
        return ship.airThrust() * thick + ship.bellThrust() * (1.0 - thick);
    }

    /**
     * How much of its weight its wings hold up at {@code speed} in air {@code thick}, pulled at {@code pull} metres a
     * second squared: one, or more, holds all of it; with no pull it needs none.
     */
    public static double held(final Performance ship, final double speed, final double thick, final double pull) {
        final double weight = ship.mass() * pull;
        if (weight <= 0) {
            return 1.0;
        }
        final double lift = ship.wing() * thick * (speed / WING_SPEED) * (speed / WING_SPEED);
        return lift / weight;
    }

    /** The slowest it flies without sinking in air {@code thick}, pulled at {@code pull}; endless with no wings. */
    public static double stall(final Performance ship, final double thick, final double pull) {
        final double weight = ship.mass() * pull;
        if (weight <= 0) {
            return 0;
        }
        if (ship.wing() <= 0 || thick <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        return WING_SPEED * Math.sqrt(weight / (ship.wing() * thick));
    }

    /* {@code space} where there is no air, {@code air} at sea level's, straight between. */
    private static double between(final double space, final double air, final double thick) {
        return space + (air - space) * thick;
    }

    /**
     * What a ship weighs and pushes, from its parts' figures.
     *
     * @param mass       its mass, in kilograms
     * @param airThrust  what its air engines push, in newtons, in the air at sea level
     * @param bellThrust what its bells push, in newtons
     * @param wing       the weight its wings hold up at {@link #WING_SPEED} in the air at sea level, in newtons
     * @param power      the power its reactor makes beyond what its parts take, in watts
     */
    public record Performance(double mass, double airThrust, double bellThrust, double wing, double power) {

        /** A ship of nothing. */
        public static final Performance NONE = new Performance(0, 0, 0, 0, 0);
    }

    /**
     * Where a ship's flight stands.
     *
     * @param speed    how fast it goes along its nose, in metres a second
     * @param sink     how fast it sinks for want of lift, in metres a second
     * @param throttle how far its throttle is open, from nought to one
     * @param charge   how full its boost is, from nought to one
     */
    public record State(double speed, double sink, double throttle, double charge) {

        /** At rest, its boost full. */
        public static final State RESTING = new State(0, 0, 0, 1);
    }

    /**
     * What its pilot holds, each from nought to one: forward opens the throttle and back closes it, left and right
     * turn it, up and down pitch its nose.
     *
     * @param piloted whether a pilot is at its helm
     * @param boost   whether they hold the boost
     */
    public record Held(double forward, double back, double left, double right, double up, double down,
                       boolean boost, boolean piloted) {

        /** Nobody at the helm. */
        public static final Held NOBODY = new Held(0, 0, 0, 0, 0, 0, false, false);
    }

    /**
     * The air round a ship.
     *
     * @param density how thick it is, as a share of the overworld's at sea level
     * @param gravity how hard the world pulls, as a share of the overworld's
     * @param empty   whether the stretch round it is empty, for its boost
     */
    public record Air(double density, double gravity, boolean empty) {
    }

    /**
     * A tick of a ship's flight.
     *
     * @param state    where its flight stands after it
     * @param turn     how far it turned to its left, in degrees
     * @param pitch    how far its nose went up, in degrees, down below nought
     * @param boosting whether it boosted
     * @param stalled  whether it flew too slowly for its wings to hold it up
     * @param bells    how much of its push its bells gave, from nought to one
     */
    public record Tick(State state, double turn, double pitch, boolean boosting, boolean stalled, double bells) {
    }
}
