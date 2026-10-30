/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.descent;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.cruise.CruiseEvents;
import dev.jstech.space.cruise.CruiseFlight;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.PlannedTransfer;
import dev.jstech.space.launch.ClimbEffects;
import dev.jstech.space.rocket.RocketAscent;
import dev.jstech.space.rocket.RocketStaging;
import dev.jstech.space.rocket.StageMath;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SystemSpaces;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * A rocket leaving the ground of a world it landed on, with no pad: with a transfer set on its flight computer and its
 * window come, it lights its lowest stage not let go and climbs to the world's orbit, from where its transfer flies on.
 * A lunar lander's ascent stage leaves its descent stage on the ground as its launch platform, as the Apollo lunar
 * modules did: theirs are still on the Moon.
 */
public final class SurfaceLaunch {

    private SurfaceLaunch() {
    }

    /** A second of {@code body}, landed on a world, whose flight computer is {@code computer}. */
    public static void second(final ServerLevel level, final MovingStructure body,
                              final FlightComputerBlockEntity computer) {
        if (SystemSpaces.systemOf(level.dimension()).isPresent() || computer.descent() != DescentPhase.LANDED
                || body.motion() != null) {
            return;
        }
        final Optional<PlannedTransfer> plan = computer.executed();
        if (plan.isPresent() && level.getGameTime() >= plan.get().leaves() && !here(level, plan.get().to())) {
            liftOff(level, body, computer);
        }
    }

    /** {@code body} lifts off from the ground it stands on, leaving a lander's descent stage behind; whether it did. */
    public static boolean liftOff(final ServerLevel level, final MovingStructure body,
                                  final FlightComputerBlockEntity computer) {
        RocketStaging.leave(level, body, computer, RocketPart.Kind.LANDER_DESCENT);
        final List<StageMath.Burn> burns = RocketAscent.burns(level, body);
        final int gone = computer.separated();
        for (int burn = 0; burn < burns.size(); burn++) {
            if (burns.get(burn).stage() > gone) {
                body.move(new RocketAscent(burn, burns.get(burn).startMass(), 0, body.pose().y()));
                computer.liftOff(level.getGameTime());
                computer.descent(DescentPhase.NONE);
                ClimbEffects.burning(level, body);
                CruiseEvents.tell(level, body, DescentTexts.LIFT_OFF.of(DescentTexts.LIFTED_NAME.text(),
                        DescentTexts.LIFTED.with(CruiseFlight.nameOf(worldOf(level)))));
                return true;
            }
        }
        return false;
    }

    /* Whether {@code level} is the world {@code world}. */
    private static boolean here(final ServerLevel level, final ResourceLocation world) {
        return FlightComputerBlockEntity.here(level).map(SolarSystemWorlds::worldOf).filter(world::equals)
                .isPresent();
    }

    /* The world {@code level} is, by its id among the Solar System's; the Overworld for none. */
    private static ResourceLocation worldOf(final ServerLevel level) {
        return SolarSystemWorlds.worldOf(FlightComputerBlockEntity.here(level).orElse(SolarBody.OVERWORLD));
    }
}
