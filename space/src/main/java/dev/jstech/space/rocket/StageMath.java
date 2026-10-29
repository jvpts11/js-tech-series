/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import dev.jstech.space.orbit.OrbitScale;
import java.util.ArrayList;
import java.util.List;

/**
 * A rocket's stages from its parts, as real rockets are staged. The parts stand from the bottom up; a part that ends a
 * stage (a separation ring, a lander's descent stage) drops with the stage under it, and what is above it is the next
 * stage. Boosters hung at a stage's side burn with it and drop first, as the Space Shuttle's did: while they burn, the
 * core's engines burn too, and the stage's delta-v comes in two burns, the boosters' and the core's after them. Each
 * burn's delta-v is Tsiolkovsky's, the engines' specific impulse the thrust-weighted one of all that burn, everything
 * above it carried. The air's pressure on the nozzles takes thrust off as it rises, the same mass flowing: an engine's
 * thrust and impulse fall in a straight line from their vacuum ones to their sea-level ones at the Overworld's
 * sea-level pressure, and on past it in thicker air, so on Mars an engine is nearly as good as in a vacuum.
 */
public final class StageMath {

    /** The Overworld's air at sea level, in kilopascals, where engines give their sea-level thrust. */
    public static final double SEA_LEVEL_KILOPASCALS = 101.325;

    private StageMath() {
    }

    /** The share of the Overworld's sea-level pressure {@code kiloPascals} is: what the sums here call the air. */
    public static double air(final double kiloPascals) {
        return Math.max(0, kiloPascals) / SEA_LEVEL_KILOPASCALS;
    }

    /**
     * An engine's specific impulse in {@code air}, the share of the sea-level pressure round it, from its impulses at
     * sea level and in a vacuum; never below nothing, however thick the air.
     */
    public static double isp(final double seaLevelIsp, final double vacuumIsp, final double air) {
        return Math.max(0, vacuumIsp - (vacuumIsp - seaLevelIsp) * Math.max(0, air));
    }

    /** The burns of the rocket of {@code pieces}, from the bottom up, in the order they fire. */
    public static List<Burn> burns(final List<Piece> pieces) {
        final List<List<Piece>> stages = new ArrayList<>();
        List<Piece> current = new ArrayList<>();
        for (final Piece piece : pieces) {
            current.add(piece);
            if (piece.endsStage()) {
                stages.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            stages.add(current);
        }
        final List<Burn> burns = new ArrayList<>();
        for (int k = 0; k < stages.size(); k++) {
            double above = 0;
            for (int j = k + 1; j < stages.size(); j++) {
                above += wet(stages.get(j));
            }
            burns.addAll(stage(k + 1, stages.get(k), above));
        }
        return List.copyOf(burns);
    }

    /**
     * The whole rocket's delta-v, its burns' summed, with every burn in {@code air}, the share of the sea-level
     * pressure: 0 in a vacuum, 1 at the Overworld's sea level.
     */
    public static double deltaV(final List<Burn> burns, final double air) {
        double sum = 0;
        for (final Burn burn : burns) {
            sum += deltaV(burn, air);
        }
        return sum;
    }

    /** The delta-v of {@code burn} in {@code air}, the share of the sea-level pressure. */
    public static double deltaV(final Burn burn, final double air) {
        return Math.max(0, burn.vacuumDeltaV() - (burn.vacuumDeltaV() - burn.seaLevelDeltaV()) * Math.max(0, air));
    }

    /**
     * How many times its weight the thrust of {@code burn} is at its start on a world pulling {@code pull} of the
     * Overworld's, in {@code air}, the share of the sea-level pressure.
     */
    public static double thrustToWeight(final Burn burn, final double pull, final double air) {
        final double thrust = Math.max(0, burn.thrust() - (burn.thrust() - burn.seaLevelThrust()) * Math.max(0, air));
        return burn.startMass() <= 0 || pull <= 0 ? 0
                : thrust / (burn.startMass() * OrbitScale.STANDARD_GRAVITY * pull);
    }

    /* The burns of the stage {@code number} of {@code pieces}, carrying {@code above} kilograms. */
    private static List<Burn> stage(final int number, final List<Piece> pieces, final double above) {
        double dry = 0;
        double propellant = 0;
        final Engines core = new Engines();
        final Engines boosters = new Engines();
        double boosterDry = 0;
        double boosterPropellant = 0;
        for (final Piece piece : pieces) {
            dry += piece.dry();
            propellant += piece.propellant();
            core.add(piece.thrust(), piece.seaLevelIsp(), piece.vacuumIsp());
            for (final Piece booster : piece.boosters()) {
                boosterDry += booster.dry();
                boosterPropellant += booster.propellant();
                boosters.add(booster.thrust(), booster.seaLevelIsp(), booster.vacuumIsp());
            }
        }
        final double start = above + dry + propellant + boosterDry + boosterPropellant;
        if (boosters.thrust <= 0 || boosterPropellant <= 0) {
            return List.of(burn(number, false, start, propellant, core));
        }
        // The boosters' burn: they and the core burn together until the boosters are empty.
        final double boosterTime = boosterPropellant / boosters.flow(false);
        final double coreBurned = core.thrust <= 0 ? 0 : Math.min(propellant, core.flow(false) * boosterTime);
        final Engines both = new Engines();
        both.merge(core);
        both.merge(boosters);
        final Burn first = burn(number, true, start, boosterPropellant + coreBurned, both);
        final double afterDrop = start - boosterPropellant - coreBurned - boosterDry;
        final Burn second = burn(number, false, afterDrop, propellant - coreBurned, core);
        return List.of(first, second);
    }

    private static Burn burn(final int number, final boolean boosters, final double start, final double burned,
                             final Engines engines) {
        final double end = start - burned;
        final boolean burns = engines.thrust > 0 && burned > 0 && end > 0;
        final double ratio = burns ? Math.log(start / end) : 0;
        return new Burn(number, boosters, start, end, engines.thrust, engines.seaLevelThrust(),
                engines.isp(false) * OrbitScale.STANDARD_GRAVITY * ratio,
                engines.isp(true) * OrbitScale.STANDARD_GRAVITY * ratio);
    }

    private static double wet(final List<Piece> pieces) {
        double mass = 0;
        for (final Piece piece : pieces) {
            mass += piece.dry() + piece.propellant();
            for (final Piece booster : piece.boosters()) {
                mass += booster.dry() + booster.propellant();
            }
        }
        return mass;
    }

    /**
     * A part of a rocket as the sums see it.
     *
     * @param dry         its mass empty, in kilograms
     * @param propellant  the propellant it holds, in kilograms
     * @param thrust      its engines' thrust in a vacuum, in newtons
     * @param seaLevelIsp its engines' specific impulse at sea level, in seconds
     * @param vacuumIsp   its engines' specific impulse in a vacuum, in seconds
     * @param endsStage   whether it ends the stage it is in
     * @param boosters    the boosters hung at its side, each summed into one
     */
    public record Piece(double dry, double propellant, double thrust, double seaLevelIsp, double vacuumIsp,
                        boolean endsStage, List<Piece> boosters) {

        public Piece {
            boosters = List.copyOf(boosters);
        }

        /** A part with no engine and no boosters. */
        public static Piece inert(final double dry, final double propellant, final boolean endsStage) {
            return new Piece(dry, propellant, 0, 0, 0, endsStage, List.of());
        }
    }

    /**
     * A burn of a stage.
     *
     * @param stage          the stage's number, from the bottom
     * @param boosters       whether its boosters burn in it
     * @param startMass      the rocket's mass as it starts, in kilograms
     * @param endMass        its mass as it ends, before anything drops
     * @param thrust         the thrust of what burns, in a vacuum, in newtons
     * @param seaLevelThrust the thrust of what burns at sea level
     * @param vacuumDeltaV   the speed it gives in a vacuum, in metres a second
     * @param seaLevelDeltaV the speed it gives at sea level
     */
    public record Burn(int stage, boolean boosters, double startMass, double endMass, double thrust,
                       double seaLevelThrust, double vacuumDeltaV, double seaLevelDeltaV) {
    }

    /* Engines burning together: their thrust, and their mass flows in a vacuum and at sea level. */
    private static final class Engines {

        private double thrust;
        private double vacuumFlow;
        private double seaLevelFlow;
        private double seaLevel;

        void add(final double newtons, final double seaLevelIsp, final double vacuumIsp) {
            if (newtons <= 0 || vacuumIsp <= 0) {
                return;
            }
            // The same mass flows anywhere; at sea level the air's pressure on the nozzle takes thrust off.
            final double flow = newtons / (vacuumIsp * OrbitScale.STANDARD_GRAVITY);
            this.thrust += newtons;
            this.vacuumFlow += flow;
            this.seaLevelFlow += flow;
            this.seaLevel += newtons * seaLevelIsp / vacuumIsp;
        }

        void merge(final Engines other) {
            this.thrust += other.thrust;
            this.vacuumFlow += other.vacuumFlow;
            this.seaLevelFlow += other.seaLevelFlow;
            this.seaLevel += other.seaLevel;
        }

        double flow(final boolean air) {
            return air ? this.seaLevelFlow : this.vacuumFlow;
        }

        double seaLevelThrust() {
            return this.seaLevel;
        }

        /* The thrust-weighted specific impulse of all of them, in seconds. */
        double isp(final boolean air) {
            final double flow = flow(air);
            return flow <= 0 ? 0 : (air ? this.seaLevel : this.thrust) / (flow * OrbitScale.STANDARD_GRAVITY);
        }
    }
}
