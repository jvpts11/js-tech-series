/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.cruise;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.PlannedTransfer;
import dev.jstech.space.orbit.OrbitObjects;
import dev.jstech.space.rocket.RocketBurns;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import dev.jstech.space.universe.UniverseTexts;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

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
        computer.executed().ifPresent(plan -> setOut(level, body, computer, plan));
    }

    /**
     * Sets {@code body} out on its cruise as {@code plan} says, if its window has come, it rests in the orbit of
     * another world and its stages have the delta-v: its transfer burn burnt, it leaves its orbit; whether it set out.
     */
    public static boolean setOut(final ServerLevel level, final MovingStructure body,
                                 final FlightComputerBlockEntity computer, final PlannedTransfer plan) {
        final Optional<SolarBody> there = SolarSystemWorlds.bodyOf(plan.to());
        final SolarBody here = SystemLayout.over(body.pose().x(), body.pose().z());
        final OptionalDouble available = computer.available();
        if (level.getGameTime() < plan.leaves() || body.motion() != null || there.isEmpty() || here == null
                || here == there.get() || available.isEmpty() || available.getAsDouble() < plan.burn()) {
            return false;
        }
        RocketBurns.burn(level, body, computer.separated(), plan.burn());
        final double[] spot = SystemLayout.orbitSpot(there.get(), 0, 0);
        MovingStructures.move(body, RocketCruise.of(body.pose(), new double[] {spot[0], SystemLayout.ORBIT_Y,
                spot[1]}, level.getGameTime(), plan.travel(), plan.to()));
        OrbitObjects.forget(level.getServer(), body.id().value());
        computer.cruising(true);
        CruiseEvents.tell(level, body, CruiseTexts.DEPARTURE.of(CruiseTexts.DEPARTED_NAME.text(),
                CruiseTexts.DEPARTED.with(nameOf(plan.to()), duration(plan.travel()))));
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

    /* {@code body} came to rest at the end of its cruise: it is in the other world's orbit, its plan flown. */
    private static void arrive(final ServerLevel level, final MovingStructure body,
                               final FlightComputerBlockEntity computer) {
        final ResourceLocation to = computer.executed().map(PlannedTransfer::to).orElse(null);
        computer.arrived();
        if (to != null) {
            CruiseEvents.tell(level, body, CruiseTexts.ARRIVAL.of(CruiseTexts.ARRIVED_NAME.text(),
                    CruiseTexts.ARRIVED.with(nameOf(to))));
        }
    }

    /* Whether everyone aboard {@code body} sleeps, and anyone is aboard. */
    private static boolean asleep(final ServerLevel level, final MovingStructure body) {
        final List<ServerPlayer> crew = aboard(level, body);
        return !crew.isEmpty() && crew.stream().allMatch(ServerPlayer::isSleepingLongEnough);
    }

    private static List<ServerPlayer> aboard(final ServerLevel level, final MovingStructure body) {
        return level.getEntitiesOfClass(ServerPlayer.class, body.worldBox().inflate(ABOARD));
    }

    /* The world {@code world}'s name. */
    private static Text nameOf(final ResourceLocation world) {
        final TextKey name = world.equals(SolarSystemWorlds.MOON) ? UniverseTexts.MOON_WORLD
                : world.equals(SolarSystemWorlds.MARS) ? UniverseTexts.MARS_WORLD
                : world.equals(SolarSystemWorlds.OVERWORLD) ? UniverseTexts.OVERWORLD_WORLD : null;
        return name == null ? Text.literal(world.getPath()) : name.text();
    }

    /* {@code ticks} as minutes and seconds, as a clock shows them. */
    private static Text duration(final long ticks) {
        final long seconds = ticks / TICKS_A_SECOND;
        return Text.literal(String.format("%d:%02d", seconds / SECONDS_A_MINUTE, seconds % SECONDS_A_MINUTE));
    }
}
