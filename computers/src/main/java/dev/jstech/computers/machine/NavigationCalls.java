/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.moving.StructureAutopilot;
import dev.jstech.core.moving.StructureReadings;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.phys.Vec3;

/**
 * The moving structure a machine stands on, as a program flies it through its {@link NavigationService}: the
 * readings of a flight sensor where the machine stands, and J's Core's autopilot told to go to a place, hold a height,
 * follow a route or dock. Every command answers whether the machine stands on a structure to fly; a place that is not
 * one, a speed the autopilot does not take, or a route that is not written as one stops the program.
 */
@TextHolder
final class NavigationCalls {

    private static final String NAVIGATION = "Navigation";
    private static final String DOUBLE = "double";
    /** The slowest and fastest an autopilot is told to go, in metres a second, as J's Core keeps it. */
    private static final double SLOWEST = 0.01;
    private static final double FASTEST = 100_000;
    /** The most points a route a program writes has. */
    private static final int ROUTE_MOST = 256;
    private static final int AXES = 3;

    private static final TextKey NOT_A_SPEED = TextKey.of("jsc.service.navigation.not_a_speed",
            "%s is not a speed an autopilot takes: from 0.01 to 100000 metres a second");
    private static final TextKey NOT_A_PLACE = TextKey.of("jsc.service.navigation.not_a_place",
            "%s is not a place of the world");
    private static final TextKey NOT_A_ROUTE = TextKey.of("jsc.service.navigation.not_a_route",
            "a route is from 1 to %s points, each written as its x, y and z");
    private static final TextKey NO_ROUTE = TextKey.of("jsc.service.navigation.no_route", "the route is null");

    private NavigationCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        readings(bindings);
        navigation(bindings, "GoTo", (navigation, call, target, arguments, line) -> navigation.fly(
                StructureAutopilot.goTo(place(arguments, 0, line), speed(arguments[3], line))),
                DOUBLE, DOUBLE, DOUBLE, DOUBLE);
        navigation(bindings, "HoldAltitude", (navigation, call, target, arguments, line) -> navigation.fly(
                StructureAutopilot.holdAltitude(coordinate(arguments[0], line), speed(arguments[1], line))),
                DOUBLE, DOUBLE);
        navigation(bindings, "FollowRoute", (navigation, call, target, arguments, line) -> navigation.fly(
                StructureAutopilot.route(route(arguments[0], line), speed(arguments[1], line), flag(arguments[2]))),
                "List<double>", DOUBLE, "bool");
        navigation(bindings, "Dock", (navigation, call, target, arguments, line) ->
                navigation.dock(place(arguments, 0, line), speed(arguments[3], line)), DOUBLE, DOUBLE, DOUBLE, DOUBLE);
        navigation(bindings, "Release", (navigation, call, target, arguments, line) -> navigation.fly(null));
    }

    /* What the machine reads of where it stands and how it goes, each a glance. */
    private static void readings(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        navigation(bindings, "Aboard", (navigation, call, target, arguments, line) -> navigation.aboard());
        navigation(bindings, "Piloted", (navigation, call, target, arguments, line) -> navigation.piloted());
        navigation(bindings, "X", (navigation, call, target, arguments, line) -> navigation.where().x);
        navigation(bindings, "Z", (navigation, call, target, arguments, line) -> navigation.where().z);
        final Map<String, TextKey> read = Map.of("Altitude", StructureReadings.ALTITUDE, "Pressure",
                StructureReadings.PRESSURE, "Speed", StructureReadings.SPEED, "Climb", StructureReadings.CLIMB,
                "Pitch", StructureReadings.PITCH, "Roll", StructureReadings.ROLL, "Heading", StructureReadings.HEADING,
                "AboveGround", StructureReadings.GROUND, "ClearAhead", StructureReadings.AHEAD);
        read.forEach((name, key) -> navigation(bindings, name,
                (navigation, call, target, arguments, line) -> navigation.reading(key)));
    }

    private static void navigation(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                                   final MachineCalls.IServiceFunction<NavigationService> function,
                                   final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::navigation, NAVIGATION, name, function, parameters);
    }

    /* A bool as a program hands it over: the runtime keeps one as a number, nothing being false. */
    private static boolean flag(final Object written) {
        return written instanceof Number value ? value.longValue() != 0L : Boolean.TRUE.equals(written);
    }

    /* The place written by the three numbers from {@code first} on. */
    private static Vec3 place(final Object[] arguments, final int first, final int line) {
        return new Vec3(coordinate(arguments[first], line), coordinate(arguments[first + 1], line),
                coordinate(arguments[first + 2], line));
    }

    private static double coordinate(final Object written, final int line) {
        final double value = ((Number) written).doubleValue();
        if (!Double.isFinite(value)) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_PLACE.with(written),
                    Halt.Thrown.ARGUMENT_OUT_OF_RANGE);
        }
        return value;
    }

    private static double speed(final Object written, final int line) {
        final double value = ((Number) written).doubleValue();
        if (!(value >= SLOWEST && value <= FASTEST)) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_SPEED.with(written),
                    Halt.Thrown.ARGUMENT_OUT_OF_RANGE);
        }
        return value;
    }

    /* The points of a route written as a list of numbers, three to a point. */
    private static List<Vec3> route(final Object written, final int line) {
        if (!(written instanceof Values.ListValue list)) {
            throw new Halt(Halt.Reason.NO_OBJECT, line, NO_ROUTE.text());
        }
        final int count = list.size();
        if (count == 0 || count % AXES != 0 || count / AXES > ROUTE_MOST) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_ROUTE.with(ROUTE_MOST),
                    Halt.Thrown.ARGUMENT);
        }
        final List<Vec3> points = new ArrayList<>(count / AXES);
        for (int at = 0; at < count; at += AXES) {
            points.add(place(list.items().subList(at, at + AXES).toArray(), 0, line));
        }
        return points;
    }
}
