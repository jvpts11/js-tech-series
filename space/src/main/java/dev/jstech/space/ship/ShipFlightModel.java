/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.id.IStableName;
import dev.jstech.space.api.ShipPart;

/**
 * How a small ship flies, a tick at a time, in the spirit of No Man's Sky: it goes where its nose points, with no
 * drift, at a speed its pilot's throttle sets and its engines reach. In the air its air engines push and its wings
 * hold it up, more as the square of its speed and less in thinner air; in the thin air of the climb and in space its
 * bells push, and in space it flies free. Slower than its wings hold it, its air engines' nozzles turn down as the
 * Harrier's do, just far enough to hold what its wings do not, the rest of their thrust still pushing it on; where they
 * cannot hold it, in thin air or too heavy, it sinks. Slow, it hovers on them, rising and coming down straight, its
 * nose kept level; fast, its nose stays where its pilot points it, so it climbs out of any air on its bells.
 * Its weight counts: a heavier ship gathers speed more slowly and needs more of it to stay up. Its boost, a store its
 * reactor fills, takes it faster across the empty stretches. On the land, wheels roll it as on a runway, and feet hold
 * it where it stands. The numbers are the game's, not a real ship's: its speeds are kept to what the world can be
 * drawn and loaded at.
 */
public final class ShipFlightModel {

    /** Seconds a tick. */
    public static final double TICK = 0.05;
    /** How hard the overworld pulls, in metres a second squared. */
    public static final double GRAVITY = 9.81;
    /** The speed a wing's figure is given at, in metres a second. */
    public static final double WING_SPEED = ShipPart.Figures.WING_SPEED;
    /** How far its nozzles turn, in degrees: straight down. */
    public static final double DOWN = 90.0;
    /** How near the land its gear comes down, in blocks. */
    public static final double GEAR_HEIGHT = 10.0;
    /** The fastest it goes at full throttle in the overworld's air, in metres a second. */
    public static final double CRUISE_AIR = 40.0;
    /** How far its throttle opens a tick with its pilot's key held whole, and closes. */
    public static final double OPENS = 0.025;
    public static final double CLOSES = 0.04;
    /* The fastest it goes at full throttle in space, in metres a second, and boosted, in the air and in space. */
    private static final double CRUISE_SPACE = 120.0;
    private static final double BOOSTED_AIR = 60.0;
    private static final double BOOSTED_SPACE = 300.0;
    /* How fast it sheds speed with less throttle, in metres a second squared: the air brakes it, space less. */
    private static final double SLOWING_AIR = 6.0;
    private static final double SLOWING_SPACE = 3.0;
    /* How fast feet, or a hull with no gear, stop it on the land, in metres a second squared. */
    private static final double BRAKES = 12.0;
    /* How fast it turns and pitches, in degrees a second, and how far its nose goes up or down. */
    private static final double TURNS = 45.0;
    private static final double PITCHES = 30.0;
    private static final double MOST_PITCH = 60.0;
    /* The fastest it sinks, in metres a second. */
    private static final double MOST_SINK = 40.0;
    /*
     * The speed it hovers below, in metres a second. Hovering, the fastest it rises or comes down as its pilot holds,
     * and comes down with nobody at the helm, in metres a second; how fast it gathers that, in metres a second squared;
     * how fast its nozzles turn, in degrees a second.
     */
    private static final double HOVER_BELOW = 10.0;
    private static final double HOVER_RATE = 6.0;
    private static final double SETTLES = 2.0;
    private static final double HOVER_GATHERS = 4.0;
    private static final double NOZZLES_TURN = 90.0;
    /* How long a full boost lasts, in seconds, and the energy it holds, in joules. */
    private static final double BOOST_SECONDS = 8.0;
    private static final double BOOST_ENERGY = 160.0e6;
    /* Held up within this share of its weight is held up: nozzles turned just far enough hold it, rounding aside. */
    private static final double HELD_CLOSE = 1.0e-9;

    private ShipFlightModel() {
    }

    /**
     * The ship one tick on from {@code now}, its pilot holding {@code held}, in the air {@code air}.
     *
     * @param now   where its flight stands
     * @param held  what its pilot holds
     * @param air   the air round it, its pull, and the land under it
     * @param ship  what it weighs and pushes
     * @param pitch how far its nose points up now, in degrees, down below nought
     */
    public static Tick step(final State now, final Held held, final Air air, final Performance ship,
                            final double pitch) {
        final double thick = Math.clamp(air.density(), 0.0, 1.0);
        final double pull = GRAVITY * Math.max(0.0, air.gravity());
        final double weight = ship.mass() * pull;
        final double breathing = ship.airThrust() * thick;
        final double bells = ship.bellThrust() * (1.0 - thick);
        final double wings = held(ship, now.speed(), thick, pull);
        // Its wings falling short, its nozzles hold the rest; slow, it hovers on them, its pilot's up and down rising
        // and coming down straight, where fast they pitch its nose.
        final boolean wanting = pull > 0 && breathing > 0 && wings < 1.0;
        final boolean hovering = wanting && now.speed() < HOVER_BELOW;
        final double throttle = held.piloted()
                ? Math.clamp(now.throttle() + held.forward() * OPENS - held.back() * CLOSES, 0.0, 1.0)
                : Math.max(0.0, now.throttle() - CLOSES);
        final double wantedRise = !held.piloted() ? -SETTLES : hovering ? (held.up() - held.down()) * HOVER_RATE : 0;
        final double most = weight > 0 ? wings + breathing / weight : Double.POSITIVE_INFINITY;
        final double rise = !hovering ? toward(now.rise(), 0, HOVER_GATHERS * TICK) : wantedRise > now.rise()
                ? Math.min(wantedRise, now.rise() + Math.min(HOVER_GATHERS, Math.max(0, (most - 1.0) * pull)) * TICK)
                : Math.max(wantedRise, now.rise() - HOVER_GATHERS * TICK);
        final boolean resting = air.grounded() && rise <= 0;
        // What its nozzles hold up: what its wings do not, and what its rising asks; on the land, nothing.
        final double lifting = resting || !wanting ? 0
                : Math.max(0, weight * (1.0 - wings) + ship.mass() * (rise - now.rise()) / TICK);
        final double angle = Math.toDegrees(Math.asin(breathing > 0 ? Math.clamp(lifting / breathing, 0.0, 1.0)
                : 0.0));
        final double nozzles = toward(now.nozzles(), angle, NOZZLES_TURN * TICK);
        final double sin = Math.sin(Math.toRadians(nozzles));
        final double push = breathing * Math.cos(Math.toRadians(nozzles)) + bells;
        final boolean boosting = held.piloted() && held.boost() && air.empty() && now.charge() > 0 && push > 0;
        final double cruise = between(CRUISE_SPACE, CRUISE_AIR, thick);
        final boolean stood = air.grounded() && ship.gear() != Gear.WHEELS && rise <= 0;
        final double wanted = stood ? 0 : boosting ? between(BOOSTED_SPACE, BOOSTED_AIR, thick) : throttle * cruise;
        final double gathers = ship.mass() > 0 ? push / ship.mass() : 0;
        final double sheds = stood ? BRAKES : between(SLOWING_SPACE, SLOWING_AIR, thick);
        final double speed = now.speed() < wanted ? Math.min(wanted, now.speed() + gathers * TICK)
                : Math.max(wanted, now.speed() - sheds * TICK);
        final double up = weight > 0 ? held(ship, speed, thick, pull) + breathing * sin / weight + HELD_CLOSE : 1.0;
        final double sink = resting || up >= 1.0 ? Math.max(0.0, now.sink() - Math.max(pull, sheds) * TICK)
                : Math.min(MOST_SINK, now.sink() + pull * (1.0 - up) * TICK);
        final double charge = boosting ? Math.max(0.0, now.charge() - TICK / BOOST_SECONDS)
                : Math.min(1.0, now.charge() + Math.max(0.0, ship.power()) * TICK / BOOST_ENERGY);
        final double turn = (held.left() - held.right()) * TURNS * TICK;
        final double lifted = hovering ? toward(pitch, 0, PITCHES * TICK) - pitch
                : Math.clamp(pitch + (held.up() - held.down()) * PITCHES * TICK, -MOST_PITCH, MOST_PITCH) - pitch;
        final boolean gearDown = ship.gear() != Gear.NONE && (air.grounded() || hovering
                || air.height() < GEAR_HEIGHT);
        return new Tick(new State(speed, resting ? 0 : sink, throttle, charge, resting ? 0 : rise, nozzles),
                turn, lifted, boosting, up < 1.0 && pull > 0 && !air.grounded(), bells > 0 && push > 0
                ? bells / push : 0.0, gearDown, sin > 0 && breathing > 0 && !resting);
    }

    /**
     * How hard its engines push in air {@code thick} of the overworld's at sea level, its nozzles turned back, in
     * newtons: its air engines by the air they breathe, its bells by what the air leaves them.
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

    /* {@code from} moved toward {@code to} by no more than {@code by}. */
    private static double toward(final double from, final double to, final double by) {
        return from < to ? Math.min(to, from + by) : Math.max(to, from - by);
    }

    /** What a ship stands on, on the land. */
    public enum Gear implements IStableName {
        /** Nothing: its hull rests on the land and does not roll. */
        NONE("none"),
        /** Wheels: it rolls on the land and takes a runway. */
        WHEELS("wheels"),
        /** Feet: it stands on rough land where it comes down, and does not roll. */
        LEGS("legs");

        private final String serializedName;

        Gear(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }

    /**
     * What a ship weighs and pushes, from its parts' figures.
     *
     * @param mass       its mass, in kilograms
     * @param airThrust  what its air engines push or lift, in newtons, in the air at sea level
     * @param bellThrust what its bells push, in newtons
     * @param wing       the weight its wings hold up at {@link #WING_SPEED} in the air at sea level, in newtons
     * @param power      the power its reactor makes beyond what its parts take, in watts
     * @param gear       what it stands on
     */
    public record Performance(double mass, double airThrust, double bellThrust, double wing, double power,
                              Gear gear) {

        /** A ship of nothing. */
        public static final Performance NONE = new Performance(0, 0, 0, 0, 0, Gear.NONE);
    }

    /**
     * Where a ship's flight stands.
     *
     * @param speed    how fast it goes along its nose, in metres a second
     * @param sink     how fast it sinks for want of lift, in metres a second
     * @param throttle how far its throttle is open, from nought to one
     * @param charge   how full its boost is, from nought to one
     * @param rise     how fast it rises on its nozzles, in metres a second, coming down below nought
     * @param nozzles  how far its air engines' nozzles are turned down, in degrees
     */
    public record State(double speed, double sink, double throttle, double charge, double rise, double nozzles) {

        /** At rest, its boost full, its nozzles back. */
        public static final State RESTING = new State(0, 0, 0, 1, 0, 0);
    }

    /**
     * What its pilot holds, each from nought to one: forward opens the throttle and back closes it, left and right
     * turn it, up and down pitch its nose, or, hovering, take it up and down.
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
     * The air round a ship, and the land under it.
     *
     * @param density  how thick it is, as a share of the overworld's at sea level
     * @param gravity  how hard the world pulls, as a share of the overworld's
     * @param empty    whether the stretch round it is empty, for its boost
     * @param grounded whether it stood on the land the tick before
     * @param height   how high it is over the land, in blocks
     */
    public record Air(double density, double gravity, boolean empty, boolean grounded, double height) {
    }

    /**
     * A tick of a ship's flight.
     *
     * @param state    where its flight stands after it
     * @param turn     how far it turned to its left, in degrees
     * @param pitch    how far its nose went up, in degrees, down below nought
     * @param boosting whether it boosted
     * @param stalled  whether it flew too slowly for its wings and nozzles to hold it up
     * @param bells    how much of its push its bells gave, from nought to one
     * @param gearDown whether its gear is down
     * @param lifting  whether its nozzles hold it up, blowing down
     */
    public record Tick(State state, double turn, double pitch, boolean boosting, boolean stalled, double bells,
                       boolean gearDown, boolean lifting) {
    }
}
