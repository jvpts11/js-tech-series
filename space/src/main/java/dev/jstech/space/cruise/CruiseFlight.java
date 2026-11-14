/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.cruise;

import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.advancement.SpaceFirsts;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.exploration.WorldCoverage;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.PlannedTransfer;
import dev.jstech.space.orbit.OrbitObject;
import dev.jstech.space.orbit.OrbitObjects;
import dev.jstech.space.rocket.RocketBurns;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.space.universe.UniverseTexts;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

/**
 * A rocket's flight between worlds, as its flight computer flies it a second at a time: in the orbit of a world, with
 * a transfer set and its window come, it burns its transfer burn and sets out on its cruise to the world it goes to;
 * on the way the events of the cruise come, and when everyone aboard sleeps the cruise passes in their sleep; once it
 * rests in the other world's orbit it has arrived, and its crew is told.
 */
public final class CruiseFlight {

    /* How far round a rocket's box a body is aboard it, in blocks. */
    private static final double ABOARD = 3;
    private static final int TICKS_A_SECOND = 20;
    private static final int SECONDS_A_MINUTE = 60;
    /*
     * How much room is kept between two rockets resting in one orbit, in blocks; how many rings of spots round a
     * world's own spot one looks over for room, and how many spots the first ring holds.
     */
    private static final double APART = 8;
    private static final int RINGS = 8;
    private static final int FIRST_RING = 6;

    private CruiseFlight() {
    }

    /** A second of the flight of {@code body}, whose flight computer is {@code computer}, in its system's space. */
    public static void second(final ServerLevel level, final MovingStructure body,
                              final FlightComputerBlockEntity computer) {
        if (SystemSpaces.systemOf(level.dimension()).isEmpty()) {
            return;
        }
        if (body.motion() instanceof RocketCruise) {
            if (asleep(level, body)) {
                skip(level, body);
            } else if (SpaceServerConfig.cruiseEvents()) {
                CruiseEvents.second(level, body, computer);
            }
            return;
        }
        if (computer.cruising()) {
            arrive(level, body, computer);
            return;
        }
        photograph(level, body);
        computer.executed().ifPresent(plan -> setOut(level, body, computer, plan));
    }

    /**
     * Sets {@code body} out on its cruise as {@code plan} says, if its window has come, it rests in the orbit of
     * another world and its stages have the delta-v for its transfer and its capture: its transfer burn burnt, it
     * leaves its orbit; whether it set out.
     */
    public static boolean setOut(final ServerLevel level, final MovingStructure body,
                                 final FlightComputerBlockEntity computer, final PlannedTransfer plan) {
        final Optional<SolarBody> there = SolarSystemWorlds.bodyOf(plan.to());
        final SolarBody here = SystemLayout.over(body.pose().x(), body.pose().z());
        final OptionalDouble available = computer.available();
        if (level.getGameTime() < plan.leaves() || body.motion() != null || there.isEmpty() || here == null
                || here == there.get() || available.isEmpty()) {
            return false;
        }
        if (available.getAsDouble() < plan.needed()) {
            // Its window come and its delta-v short, the transfer is let go and its crew told, once.
            computer.dropPlan();
            CruiseEvents.tell(level, body, CruiseTexts.HELD.of(CruiseTexts.HELD_NAME.text(), CruiseTexts.SHORT.with(
                    nameOf(plan.to()), Math.round(available.getAsDouble()), Math.round(plan.needed()))));
            return false;
        }
        RocketBurns.burn(level, body, computer.separated(), plan.burn());
        final double[] spot = arrivalSpot(level, body, there.get(), plan.to());
        MovingStructures.move(body, RocketCruise.of(body.pose(), new double[] {spot[0], SystemLayout.ORBIT_Y,
                spot[1]}, level.getGameTime(), plan.travel(), plan.to()));
        OrbitObjects.forget(level.getServer(), body.id().value());
        computer.cruising(true);
        CruiseEvents.tell(level, body, CruiseTexts.DEPARTURE.of(CruiseTexts.DEPARTED_NAME.text(),
                CruiseTexts.DEPARTED.with(nameOf(plan.to()), duration(plan.travel()))));
        return true;
    }

    /*
     * Where in {@code there}'s low orbit {@code body} comes to rest at the end of its cruise to {@code world}: over the
     * world's middle, or, where rockets rest there or are bound there already, the nearest spot round it with room
     * for it, in rings a rocket's width apart, so no two rockets come to rest one inside the other.
     */
    private static double[] arrivalSpot(final ServerLevel level, final MovingStructure body, final SolarBody there,
                                        final ResourceLocation world) {
        final double[] middle = SystemLayout.orbitSpot(there, 0, 0);
        final double room = Math.max(body.worldBox().getXsize(), body.worldBox().getZsize()) + APART;
        final List<double[]> taken = new ArrayList<>();
        for (final IStructureBody other : MovingStructures.near(level, new AABB(middle[0], SystemLayout.ORBIT_Y,
                middle[1], middle[0], SystemLayout.ORBIT_Y, middle[1]).inflate(room * (RINGS + 1)))) {
            if (other instanceof MovingStructure resting && !resting.id().equals(body.id())) {
                taken.add(new double[] {resting.pose().x(), resting.pose().z(),
                        Math.max(resting.worldBox().getXsize(), resting.worldBox().getZsize())});
            }
        }
        for (final IStructureBody other : LevelMovingStructures.of(level).all()) {
            if (other instanceof MovingStructure bound && !bound.id().equals(body.id())
                    && bound.motion() instanceof RocketCruise cruise && cruise.to().equals(world)) {
                taken.add(new double[] {cruise.toX(), cruise.toZ(), Math.max(bound.worldBox().getXsize(),
                        bound.worldBox().getZsize())});
            }
        }
        for (int ring = 0; ring <= RINGS; ring++) {
            final int spots = ring == 0 ? 1 : FIRST_RING * ring;
            for (int k = 0; k < spots; k++) {
                final double angle = 2 * Math.PI * k / spots;
                final double[] spot = {middle[0] + ring * room * Math.cos(angle),
                    middle[1] + ring * room * Math.sin(angle)};
                if (free(spot, room, taken)) {
                    return spot;
                }
            }
        }
        return middle;
    }

    /* Whether a rocket {@code room} wide, its room kept, rests at {@code spot} clear of each of {@code taken}. */
    private static boolean free(final double[] spot, final double room, final List<double[]> taken) {
        for (final double[] other : taken) {
            final double apart = (room + other[2] + APART) / 2;
            if ((spot[0] - other[0]) * (spot[0] - other[0]) + (spot[1] - other[1]) * (spot[1] - other[1])
                    < apart * apart) {
                return false;
            }
        }
        return true;
    }

    /** Has {@code body}'s cruise pass at once, as in its crew's sleep; whether it was cruising. */
    public static boolean skip(final ServerLevel level, final MovingStructure body) {
        if (!(body.motion() instanceof RocketCruise cruise)) {
            return false;
        }
        MovingStructures.move(body, cruise.skipped(level.getGameTime()));
        for (final ServerPlayer player : aboard(level, body)) {
            if (player.isSleeping()) {
                player.stopSleepInBed(false, true);
            }
        }
        CruiseEvents.tell(level, body, CruiseTexts.ARRIVAL.of(CruiseTexts.ARRIVED_NAME.text(),
                CruiseTexts.SLEPT.text()));
        return true;
    }

    /** The players aboard {@code body}: on it or in it. */
    public static List<ServerPlayer> aboard(final ServerLevel level, final MovingStructure body) {
        return level.getEntitiesOfClass(ServerPlayer.class, body.worldBox().inflate(ABOARD));
    }

    /** The world {@code world}'s name, as its crew is told it. */
    public static Text nameOf(final ResourceLocation world) {
        final TextKey name = world.equals(SolarSystemWorlds.MOON) ? UniverseTexts.MOON_WORLD
                : world.equals(SolarSystemWorlds.MARS) ? UniverseTexts.MARS_WORLD
                : world.equals(SolarSystemWorlds.OVERWORLD) ? UniverseTexts.OVERWORLD_WORLD : null;
        return name == null ? Text.literal(world.getPath()) : name.text();
    }

    /*
     * {@code body} came to rest at the end of its cruise, its plan flown: it is captured into the other world's orbit,
     * by its capture burn or by aerobraking where its plan meant to and its heat shield is whole, photographing the
     * ground under it on the way in. Short of the delta-v, it burns what it has and stays in a wide orbit of the world
     * rather than a low one, as a probe whose capture burn fell short: still the world's, its crew told so.
     */
    private static void arrive(final ServerLevel level, final MovingStructure body,
                               final FlightComputerBlockEntity computer) {
        final Optional<PlannedTransfer> flown = computer.executed();
        computer.arrived();
        final Optional<SolarBody> there = flown.flatMap(plan -> SolarSystemWorlds.bodyOf(plan.to()));
        if (flown.isEmpty() || there.isEmpty()) {
            return;
        }
        final PlannedTransfer plan = flown.get();
        final double capture = plan.captureBurn(computer.shield());
        final boolean captured = capture <= 0 || RocketBurns.burn(level, body, computer.separated(), capture);
        OrbitObjects.arrived(level, body, there.get());
        aboard(level, body).forEach(player -> SpaceFirsts.arrived(player, plan.to()));
        Rockets.get(level.getServer(), body.id()).ifPresent(rocket -> WorldCoverage.photographAt(level.getServer(),
                CoreTeams.teamOf(level.getServer(), rocket.ownership().owner()), plan.to(), level.getGameTime()));
        if (!captured) {
            CruiseEvents.tell(level, body, CruiseTexts.MISSED.of(CruiseTexts.MISSED_NAME.text(),
                    CruiseTexts.NOT_CAPTURED.with(nameOf(plan.to()))));
            return;
        }
        CruiseEvents.tell(level, body, CruiseTexts.ARRIVAL.of(CruiseTexts.ARRIVED_NAME.text(), (capture > 0
                ? CruiseTexts.ARRIVED : CruiseTexts.AEROBRAKED).with(nameOf(plan.to()))));
    }

    /*
     * {@code body} resting in a world's orbit: its crew photographs the ground under each pass, the world turning a
     * little under the orbit with every one, for its team's maps.
     */
    private static void photograph(final ServerLevel level, final MovingStructure body) {
        if (body.motion() != null) {
            return;
        }
        OrbitObjects.get(level.getServer(), body.id().value())
                .filter(found -> found.kind() == OrbitObject.Kind.ROCKET)
                .ifPresent(found -> WorldCoverage.photographAt(level.getServer(), CoreTeams.teamOf(level.getServer(),
                        found.ownership().owner()), found.world(), level.getGameTime()));
    }

    /* Whether everyone aboard {@code body} sleeps, and anyone is aboard. */
    private static boolean asleep(final ServerLevel level, final MovingStructure body) {
        final List<ServerPlayer> crew = aboard(level, body);
        return !crew.isEmpty() && crew.stream().allMatch(ServerPlayer::isSleepingLongEnough);
    }


    /* {@code ticks} as minutes and seconds, as a clock shows them. */
    private static Text duration(final long ticks) {
        final long seconds = ticks / TICKS_A_SECOND;
        return Text.literal(String.format("%d:%02d", seconds / SECONDS_A_MINUTE, seconds % SECONDS_A_MINUTE));
    }
}
