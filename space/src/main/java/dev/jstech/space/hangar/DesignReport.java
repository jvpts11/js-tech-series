/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.text.Text;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.rocket.RocketPieces;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.StageMath;
import dev.jstech.space.universe.SolarBody;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * What a Rocket Hangar's design screen shows of a design, without becoming a simulator: each stage's delta-v and its
 * thrust against its weight on the world it leaves, the whole rocket's delta-v, how tall and how wide it stands, the
 * seats it has, where it can go and what each destination asks, and what it lacks. The first burn is summed in the air
 * of the world it leaves, every later one in a vacuum, as a rocket is above the air by the time its first stage drops.
 * A design that lacks what it needs to fly is not ready, and is not sent to the bay.
 *
 * @param stages       its stages, from the bottom up
 * @param deltaV       its delta-v, in metres a second
 * @param liftOff      its thrust against its weight as it leaves the ground; nothing for a rocket with no engine
 * @param height       how tall it stands, in blocks
 * @param width        how wide it stands, its boosters with it, in blocks
 * @param seats        the crew it seats
 * @param destinations where it can go from the world it leaves, and what each asks
 * @param checks       what it lacks, warns of, or has, the faults first
 */
public record DesignReport(List<Stage> stages, double deltaV, double liftOff, int height, int width, int seats,
                           List<DesignDestinations.Reach> destinations, List<Check> checks) {

    /**
     * The report of {@code stack}, of the parts {@code parts}, on a world pulling {@code pull} of the Overworld's with
     * {@code air} of its sea-level air, the body {@code from} where it is one of the Solar System's, in {@code bay}
     * where it is built in one.
     */
    public static DesignReport of(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                  final double pull, final double air, final Optional<SolarBody> from,
                                  final Optional<HangarBay> bay) {
        final List<StageMath.Burn> burns = RocketStages.burns(parts, stack).orElse(List.of());
        final List<List<Integer>> places = RocketStages.stages(parts, stack);
        final List<Stage> stages = new ArrayList<>();
        double total = 0;
        for (int k = 0; k < burns.size(); k++) {
            total += StageMath.deltaV(burns.get(k), k == 0 ? air : 0);
        }
        for (int s = 0; s < places.size(); s++) {
            stages.add(stage(parts, stack, s + 1, places.get(s), burns, pull, air));
        }
        final double liftOff = burns.isEmpty() ? 0 : StageMath.thrustToWeight(burns.get(0), pull, air);
        final int height = RocketStages.height(parts, stack);
        final int width = width(parts, stack);
        final int seats = seats(parts, stack);
        final List<DesignDestinations.Reach> destinations = from.map(DesignDestinations::from).orElse(List.of());
        final List<Check> checks = checks(parts, stack, stages, liftOff, height, width, seats, bay);
        return new DesignReport(List.copyOf(stages), total, liftOff, height, width, seats, destinations, checks);
    }

    /**
     * The report of {@code stack} designed on {@code level}, of the parts that world knows, leaving it with its pull
     * and its air, in {@code bay} where it is built in one.
     */
    public static DesignReport in(final Level level, final RocketStack stack, final Optional<HangarBay> bay) {
        final DimensionRules rules = DimensionRulesData.of(level);
        final DesignReport report = of(RocketParts.all(level), stack, rules.gravity(), StageMath.air(rules.pressure()),
                FlightComputerBlockEntity.here(level), bay);
        if (bay.isEmpty() || stack.sections().isEmpty()) {
            return report;
        }
        return report.withLauncher(RocketPlans.launcherSide(level, stack, bay.get()).isPresent());
    }

    /** The same report, saying last whether the rocket will stand on a mobile launcher in its bay, or on its floor. */
    public DesignReport withLauncher(final boolean onLauncher) {
        final List<Check> all = new ArrayList<>(this.checks);
        all.add(new Check(Severity.NOTE, onLauncher ? DesignTexts.LAUNCHER.text() : DesignTexts.NO_LAUNCHER.text()));
        return new DesignReport(this.stages, this.deltaV, this.liftOff, this.height, this.width, this.seats,
                this.destinations, List.copyOf(all));
    }

    /** Whether it has nothing it lacks to fly: no fault among its checks. */
    public boolean ready() {
        return this.checks.stream().noneMatch(check -> check.severity() == Severity.FAULT);
    }

    /* A stage as the screen lists it: its first engine, whether it holds propellant, its thrust and delta-v. */
    private static Stage stage(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack, final int number,
                               final List<Integer> places, final List<StageMath.Burn> burns, final double pull,
                               final double air) {
        ResourceLocation engine = null;
        boolean propellant = false;
        for (final int place : places) {
            final RocketStack.Section section = stack.sections().get(place);
            final List<RocketStack.Part> held = new ArrayList<>();
            held.add(section.part());
            section.boosters().forEach(held::addAll);
            for (final RocketStack.Part placed : held) {
                final RocketPart part = parts.get(placed.id());
                if (part == null) {
                    continue;
                }
                if (engine == null && part.engine().isPresent()) {
                    engine = placed.id();
                }
                propellant |= part.propellant() > 0;
            }
        }
        double deltaV = 0;
        double thrust = 0;
        double ratio = 0;
        boolean first = true;
        for (int k = 0; k < burns.size(); k++) {
            final StageMath.Burn burn = burns.get(k);
            if (burn.stage() != number) {
                continue;
            }
            final double round = k == 0 ? air : 0;
            deltaV += StageMath.deltaV(burn, round);
            if (first) {
                thrust = burn.thrust();
                ratio = StageMath.thrustToWeight(burn, pull, round);
                first = false;
            }
        }
        return new Stage(number, Optional.ofNullable(engine), propellant, thrust, ratio, deltaV);
    }

    /*
     * How wide the rocket stands, across whichever way it is wider: from the far side of a booster on one side to the
     * far side of the core, or of a booster across from it; a single booster widens it on its side alone.
     */
    private static int width(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack) {
        int west = 0;
        int east = 0;
        int north = 0;
        int south = 0;
        boolean known = false;
        for (final RocketPieces.Piece piece : RocketPieces.of(parts, stack)) {
            final RocketPart part = parts.get(piece.part().id());
            if (part == null) {
                continue;
            }
            // Its diameter is odd: it reaches half of it, rounded down, past its middle block on each side.
            final int half = Math.max(part.diameter(), part.topDiameter()) / 2;
            west = Math.min(west, piece.x() - half);
            east = Math.max(east, piece.x() + half);
            north = Math.min(north, piece.z() - half);
            south = Math.max(south, piece.z() + half);
            known = true;
        }
        return known ? Math.max(east - west, south - north) + 1 : 0;
    }

    private static int seats(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack) {
        int seats = 0;
        for (final RocketStack.Section section : stack.sections()) {
            final RocketPart part = parts.get(section.part().id());
            seats += part == null ? 0 : part.seats();
        }
        return seats;
    }

    /*
     * What the design lacks, warns of or has: no parts; a part that does not fit; a stack out of order, or boosters
     * where they do not fly; a stage with propellant and no engine, or an engine and nothing to burn; too weak to lift
     * off; taller than the hangar builds, or too wide to roll out through its door's slot; its crew, and the heat
     * shield a crew needs to come home through air; and the tanks, filled at the pad.
     */
    private static List<Check> checks(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                      final List<Stage> stages, final double liftOff, final int height,
                                      final int width, final int seats, final Optional<HangarBay> bay) {
        final List<Check> faults = new ArrayList<>();
        final List<Check> rest = new ArrayList<>();
        if (stack.sections().isEmpty()) {
            faults.add(new Check(Severity.FAULT, DesignTexts.NO_PARTS.text()));
            return List.copyOf(faults);
        }
        RocketStages.misfit(parts, stack).ifPresent(place -> faults.add(new Check(Severity.FAULT,
                DesignTexts.MISFIT.with(place + 1))));
        order(parts, stack, faults);
        boosters(parts, stack, faults);
        for (final Stage stage : stages) {
            if (stage.propellant() && stage.engine().isEmpty()) {
                faults.add(new Check(Severity.FAULT, DesignTexts.NO_ENGINE.with(stage.number())));
            } else if (stage.engine().isPresent() && !stage.propellant()) {
                faults.add(new Check(Severity.FAULT, DesignTexts.NO_PROPELLANT.with(stage.number())));
            }
        }
        if (stages.stream().anyMatch(stage -> stage.engine().isPresent()) && liftOff <= 1) {
            faults.add(new Check(Severity.FAULT, DesignTexts.STAYS_DOWN.text()));
        }
        if (bay.isPresent()) {
            if (height > HangarStructure.TALLEST_ROCKET) {
                faults.add(new Check(Severity.FAULT, DesignTexts.TOO_TALL.with(height,
                        HangarStructure.TALLEST_ROCKET)));
            }
            if (width > HangarStructure.WIDEST_ROCKET) {
                faults.add(new Check(Severity.FAULT, DesignTexts.TOO_WIDE.with(width,
                        HangarStructure.WIDEST_ROCKET)));
            }
        }
        boolean capsule = false;
        for (final RocketStack.Section section : stack.sections()) {
            final RocketPart part = parts.get(section.part().id());
            capsule |= part != null && part.kind() == RocketPart.Kind.CAPSULE;
        }
        if (seats == 0) {
            rest.add(new Check(Severity.WARNING, DesignTexts.NO_SEATS.text()));
        } else {
            rest.add(new Check(Severity.FINE, seats == 1 ? DesignTexts.SEAT_ONE.text()
                    : DesignTexts.SEATS.with(seats)));
            rest.add(capsule ? new Check(Severity.FINE, DesignTexts.SHIELD.text())
                    : new Check(Severity.WARNING, DesignTexts.NO_SHIELD.text()));
        }
        rest.add(new Check(Severity.NOTE, DesignTexts.TANKS.text()));
        faults.addAll(rest);
        return List.copyOf(faults);
    }

    /*
     * Whether the stack stands in an order that flies, each fault with its reason: each stage's engine at its foot,
     * as a stage burns from its foot up and drops what is under the next; one capsule at most, with nothing over it but
     * what belongs over a capsule; with no capsule, the lander on top, as it flies the rocket; the lander's ascent
     * stage standing on its descent stage; and a capsule or a lander, whose flight computer flies the rocket.
     */
    private static void order(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                              final List<Check> faults) {
        final List<List<Integer>> stages = RocketStages.stages(parts, stack);
        for (int s = 0; s < stages.size(); s++) {
            final List<Integer> stage = stages.get(s);
            for (int i = 1; i < stage.size(); i++) {
                final RocketPart part = parts.get(stack.sections().get(stage.get(i)).part().id());
                if (part != null && part.engine().isPresent()) {
                    faults.add(new Check(Severity.FAULT, DesignTexts.ENGINE_NOT_AT_FOOT.with(s + 1)));
                    break;
                }
            }
        }
        int capsule = -1;
        int ascent = -1;
        int capsules = 0;
        for (int k = 0; k < stack.sections().size(); k++) {
            final RocketPart part = parts.get(stack.sections().get(k).part().id());
            if (part == null) {
                continue;
            }
            if (part.kind() == RocketPart.Kind.CAPSULE) {
                capsules++;
                capsule = k;
            } else if (part.kind() == RocketPart.Kind.LANDER_ASCENT) {
                ascent = k;
                final RocketPart under = k == 0 ? null : parts.get(stack.sections().get(k - 1).part().id());
                if (under == null || under.kind() != RocketPart.Kind.LANDER_DESCENT) {
                    faults.add(new Check(Severity.FAULT, DesignTexts.ASCENT_ALONE.text()));
                }
            }
        }
        if (capsules > 1) {
            faults.add(new Check(Severity.FAULT, DesignTexts.CAPSULES.text()));
        }
        if (capsule >= 0) {
            if (!onlyOverACapsule(parts, stack, capsule)) {
                faults.add(new Check(Severity.FAULT, DesignTexts.CAPSULE_NOT_ON_TOP.text()));
            }
        } else if (ascent >= 0) {
            if (!onlyOverACapsule(parts, stack, ascent)) {
                faults.add(new Check(Severity.FAULT, DesignTexts.LANDER_NOT_ON_TOP.text()));
            }
        } else {
            faults.add(new Check(Severity.FAULT, DesignTexts.NOTHING_FLIES_IT.text()));
        }
    }

    /*
     * Whether its boosters hang where boosters fly, each fault with its reason: beside the first stage's engine alone,
     * their feet on the deck with the core's, as they burn with that stage; and each an engine at its foot with tanks
     * of its size over it, a column of the parts a rocket is built of.
     */
    private static void boosters(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                 final List<Check> faults) {
        final OptionalInt place = RocketStages.boosterPlace(parts, stack);
        boolean column = true;
        for (int k = 0; k < stack.sections().size(); k++) {
            final List<List<RocketStack.Part>> hung = stack.sections().get(k).boosters();
            if (!hung.isEmpty() && (place.isEmpty() || place.getAsInt() != k)) {
                faults.add(new Check(Severity.FAULT, DesignTexts.BOOSTERS_AT_ENGINE.text()));
            }
            for (final List<RocketStack.Part> booster : hung) {
                column &= column(parts, booster);
            }
        }
        if (!column) {
            faults.add(new Check(Severity.FAULT, DesignTexts.BOOSTER_COLUMN.text()));
        }
    }

    /* Whether {@code booster} is an engine at its foot with at least one tank of its diameter over it, and no more. */
    private static boolean column(final Map<ResourceLocation, RocketPart> parts,
                                  final List<RocketStack.Part> booster) {
        if (booster.size() < 2) {
            return false;
        }
        final RocketPart foot = parts.get(booster.get(0).id());
        if (foot == null || foot.kind() != RocketPart.Kind.ENGINE) {
            return false;
        }
        for (int s = 1; s < booster.size(); s++) {
            final RocketPart segment = parts.get(booster.get(s).id());
            if (segment == null || segment.kind() != RocketPart.Kind.TANK || segment.diameter() != foot.diameter()) {
                return false;
            }
        }
        return true;
    }

    /* Whether nothing stands over the place {@code below} but what belongs over a capsule: its escape tower, a port. */
    private static boolean onlyOverACapsule(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                            final int below) {
        for (int k = below + 1; k < stack.sections().size(); k++) {
            final RocketPart part = parts.get(stack.sections().get(k).part().id());
            if (part != null && part.kind() != RocketPart.Kind.ESCAPE_TOWER
                    && part.kind() != RocketPart.Kind.DOCKING_PORT) {
                return false;
            }
        }
        return true;
    }

    /** How a check reads: a fault keeps the design from the bay, a warning is worth knowing, the rest is so. */
    public enum Severity {
        FAULT,
        WARNING,
        FINE,
        NOTE
    }

    /**
     * A stage of the design.
     *
     * @param number     its number, from one at the bottom
     * @param engine     its first engine's part, if it has one
     * @param propellant whether it holds propellant
     * @param thrust     the thrust of its first burn, in a vacuum, in newtons
     * @param ratio      its first burn's thrust against its weight on the world it leaves
     * @param deltaV     its delta-v, in metres a second
     */
    public record Stage(int number, Optional<ResourceLocation> engine, boolean propellant, double thrust,
                        double ratio, double deltaV) {
    }

    /**
     * A thing the screen says of the design.
     *
     * @param severity how it reads
     * @param text     what it says
     */
    public record Check(Severity severity, Text text) {
    }
}
