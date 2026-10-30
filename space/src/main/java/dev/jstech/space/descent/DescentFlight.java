/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.descent;

import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.multiblock.damage.DamageStamps;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.text.Text;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.cruise.CruiseEvents;
import dev.jstech.space.cruise.CruiseFlight;
import dev.jstech.space.exploration.WorldDiscovery;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.PlannedLanding;
import dev.jstech.space.orbit.OrbitObject;
import dev.jstech.space.orbit.OrbitObjects;
import dev.jstech.space.orbit.Rocketry;
import dev.jstech.space.rocket.RocketBurns;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import dev.jstech.space.universe.SphereMap;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * A rocket's coming down to a world, as its flight computer flies it a second at a time. Resting in the orbit of the
 * world its landing is set on, it begins, unless what it would meet is held against it: a crew whose team does not
 * know what kills there, no heat shield fit for the air, no legs or propellant to brake on them, each as the server's
 * rules of failure say. It comes down somewhere in its guidance's ellipse round the spot marked, its crew told; in the
 * world it glows through the air, its engines plume as they brake, and it stands on the ground, the dust rising, or
 * crashes where it could not slow. Burning up, with no heat shield in the air, it loses part of itself and its
 * parachutes with it.
 */
public final class DescentFlight {

    /* The effects of coming down: the air glowing at the shield, the engines braking, the dust at touchdown. */
    public static final ResourceLocation REENTRY = id("reentry");
    public static final ResourceLocation BRAKING = ResourceLocation.fromNamespaceAndPath("jscore", "rocket_plume");
    public static final ResourceLocation MOON_DUST = id("touchdown_moon");
    public static final ResourceLocation MARS_DUST = id("touchdown_mars");
    public static final ResourceLocation DUST = id("touchdown");
    /* How thick a world's air must be, in kilopascals, for parachutes alone to set a capsule down gently. */
    private static final double THICK = 30;
    /* How far the effects are seen, in blocks. */
    private static final int SEEN = 256;
    /* The blast of a crash and of burning up, and how many of its blocks each takes. */
    private static final float CRASH_BLAST = 4F;
    private static final float BURN_BLAST = 2F;
    private static final int CRASH_LOST = 3;
    private static final int BURN_LOST = 2;
    private static final double LOST_SIZE = 1.0;

    private DescentFlight() {
    }

    /** A second of the coming down of {@code body}, whose flight computer is {@code computer}. */
    public static void second(final ServerLevel level, final MovingStructure body,
                              final FlightComputerBlockEntity computer) {
        final DescentPhase phase = computer.descent();
        if (SystemSpaces.systemOf(level.dimension()).isPresent()) {
            if (!phase.descending() && body.motion() == null && !computer.cruising()) {
                computer.landing().ifPresent(landing -> begin(level, body, computer, landing));
            }
            return;
        }
        if (!phase.descending()) {
            return;
        }
        if (body.motion() instanceof RocketDescent descent) {
            follow(level, body, computer, descent);
        } else if (body.motion() == null) {
            touchDown(level, body, computer);
        }
    }

    /**
     * Begins {@code body}'s coming down to the landing {@code landing}, if it rests in that world's orbit and nothing
     * holds it; whether it began.
     */
    public static boolean begin(final ServerLevel level, final MovingStructure body,
                                final FlightComputerBlockEntity computer, final PlannedLanding landing) {
        final Optional<OrbitObject> orbit = OrbitObjects.get(level.getServer(), body.id().value())
                .filter(found -> found.kind() == OrbitObject.Kind.ROCKET && found.world().equals(landing.world()));
        final Optional<SolarBody> solar = SolarSystemWorlds.bodyOf(landing.world());
        final ServerLevel world = levelOf(level, landing.world());
        if (orbit.isEmpty() || solar.isEmpty() || world == null) {
            return false;
        }
        final Text name = CruiseFlight.nameOf(landing.world());
        for (final ServerPlayer player : CruiseFlight.aboard(level, body)) {
            if (!WorldDiscovery.canLandCrew(level.getServer(), CoreTeams.teamOf(level.getServer(),
                    player.getUUID()), landing.world())) {
                return hold(level, body, computer, DescentTexts.NO_ESSENTIALS.with(name));
            }
        }
        final int air = airOf(world);
        final boolean shield = computer.shield();
        if (air != RocketDescent.NO_AIR && !shield && !SpaceServerConfig.FILE.get(SpaceServerConfig.BURNING_UP)) {
            return hold(level, body, computer, DescentTexts.NO_SHIELD.with(name));
        }
        final boolean brakes = legs(level, computer) && computer.available().orElse(0)
                >= landingDeltaV(solar.get());
        if (air != RocketDescent.THICK_AIR && !brakes
                && !SpaceServerConfig.FILE.get(SpaceServerConfig.CRASH_LANDINGS)) {
            return hold(level, body, computer, DescentTexts.NO_LEGS.with(name));
        }
        final double width = Math.max(1, world.getWorldBorder().getSize());
        final double[] at = inEllipse(level.random, landing, computer.guidance(), width);
        final double[] spot = SystemLayout.orbitSpot(solar.get(), SphereMap.longitude(at[0], width),
                SphereMap.latitude(at[1], width));
        body.move(RocketDescent.of(landing.world(), spot, at[0], at[1], air, shield, brakes));
        computer.descent(DescentPhase.DEORBIT);
        CruiseEvents.tell(level, body, DescentTexts.DESCENT.of(DescentTexts.DESCENDING_NAME.text(),
                DescentTexts.DESCENDING.with(name)));
        return true;
    }

    /** The delta-v landing on {@code body} takes from its orbit, in metres a second, once its air has done its part. */
    public static double landingDeltaV(final SolarBody body) {
        return switch (body) {
            case MOON -> Rocketry.MOON_ORBIT_TO_SURFACE;
            case MARS -> Rocketry.MARS_LANDING;
            default -> 0;
        };
    }

    /** How much air {@code level} has to come down through: {@link RocketDescent#NO_AIR} and on. */
    public static int airOf(final Level level) {
        final double pressure = DimensionRulesData.of(level).pressure();
        return pressure >= THICK ? RocketDescent.THICK_AIR : pressure > 0 ? RocketDescent.THIN_AIR
                : RocketDescent.NO_AIR;
    }

    /*
     * Coming down through the world's sky: the computer tells the crew where it is; the air glows at the shield, and
     * with none fit for it the rocket burns up as it enters; the engines plume as they brake.
     */
    private static void follow(final ServerLevel level, final MovingStructure body,
                               final FlightComputerBlockEntity computer, final RocketDescent descent) {
        final DescentPhase was = computer.descent();
        final DescentPhase now = descent.phase();
        final Vec3 foot = new Vec3(body.pose().x(), body.pose().y(), body.pose().z());
        if (was == DescentPhase.DEORBIT && now == DescentPhase.ENTRY && descent.air() != RocketDescent.NO_AIR
                && !computer.shield()) {
            burnUp(level, body, computer, descent.world());
        }
        computer.descent(now);
        if (now == DescentPhase.ENTRY && descent.air() != RocketDescent.NO_AIR) {
            Effects.at(level, REENTRY, foot).facing(new Vec3(0, 1, 0)).following(body.entity()).reach(SEEN).send();
        } else if (now == DescentPhase.BRAKING) {
            Effects.at(level, BRAKING, foot).facing(new Vec3(0, -1, 0)).following(body.entity()).reach(SEEN)
                    .send();
        }
    }

    /*
     * Standing on the ground: where the air alone set a capsule with its parachutes down, or its engines braked it to
     * its legs, it landed, the propellant spent; else it crashed.
     */
    private static void touchDown(final ServerLevel level, final MovingStructure body,
                                  final FlightComputerBlockEntity computer) {
        final ResourceLocation world = worldOf(level);
        final Optional<SolarBody> solar = world == null ? Optional.empty() : SolarSystemWorlds.bodyOf(world);
        final boolean underChutes = airOf(level) == RocketDescent.THICK_AIR && computer.shield();
        final boolean braked = !underChutes && legs(level, computer) && RocketBurns.burn(level, body,
                computer.separated(), solar.map(DescentFlight::landingDeltaV).orElse(0.0));
        final Vec3 foot = new Vec3(body.pose().x(), body.pose().y(), body.pose().z());
        final Text name = world == null ? Text.literal(level.dimension().location().getPath())
                : CruiseFlight.nameOf(world);
        computer.landed();
        if (underChutes || braked) {
            Effects.at(level, solar.map(DescentFlight::dustOf).orElse(DUST), foot).reach(SEEN).send();
            CruiseEvents.tell(level, body, DescentTexts.LANDING.of(DescentTexts.LANDED_NAME.text(),
                    DescentTexts.LANDED.with(name)));
            return;
        }
        level.explode(null, foot.x, foot.y + 1, foot.z, CRASH_BLAST, Level.ExplosionInteraction.BLOCK);
        lose(level, computer, CRASH_LOST, true);
        CruiseEvents.tell(level, body, DescentTexts.CRASH.of(DescentTexts.CRASHED_NAME.text(),
                DescentTexts.CRASHED.with(name)));
    }

    /* With no heat shield fit for the air, the rocket burns as it enters: its top blocks go, its parachutes too. */
    private static void burnUp(final ServerLevel level, final MovingStructure body,
                               final FlightComputerBlockEntity computer, final ResourceLocation world) {
        final AABB box = body.worldBox();
        level.explode(null, box.getCenter().x, box.maxY, box.getCenter().z, BURN_BLAST, true,
                Level.ExplosionInteraction.NONE);
        lose(level, computer, BURN_LOST, false);
        CruiseEvents.tell(level, body, DescentTexts.BURNING.of(DescentTexts.BURNT_NAME.text(),
                DescentTexts.BURNT.with(CruiseFlight.nameOf(world))));
    }

    /* The rocket loses {@code count} of its formed blocks: from its foot up when it {@code crashed}, else its top. */
    private static void lose(final ServerLevel level, final FlightComputerBlockEntity computer, final int count,
                             final boolean crashed) {
        final Comparator<BlockPos> up = Comparator.comparingInt(BlockPos::getY);
        final List<BlockPos> places = computer.formedPlaces(level).stream()
                .sorted(crashed ? up : up.reversed()).limit(count).toList();
        for (final BlockPos place : places) {
            FormedDamage.blame(level, place, crashed ? DamageStamps.IMPACT : DamageStamps.BURN, LOST_SIZE);
            level.setBlockAndUpdate(place, Blocks.AIR.defaultBlockState());
        }
    }

    /* A crew's landing held: told why, and the landing let go so it is not tried again till set anew. */
    private static boolean hold(final ServerLevel level, final MovingStructure body,
                                final FlightComputerBlockEntity computer, final Text why) {
        computer.landed();
        computer.descent(DescentPhase.NONE);
        CruiseEvents.tell(level, body, DescentTexts.HELD.of(DescentTexts.HELD_NAME.text(), why));
        return false;
    }

    /* Whether the rocket has legs to stand on: a stage's, or a lander's descent stage. */
    private static boolean legs(final ServerLevel level, final FlightComputerBlockEntity computer) {
        return computer.carries(level, RocketPart.Kind.LANDING_LEGS)
                || computer.carries(level, RocketPart.Kind.LANDER_DESCENT);
    }

    /*
     * Where in its guidance's ellipse round the spot marked of {@code landing} it comes down, in a world {@code width}
     * blocks wide: anywhere in it alike, never past the world's edges.
     */
    private static double[] inEllipse(final RandomSource random, final PlannedLanding landing, final int guidance,
                                      final double width) {
        final double reach = Math.sqrt(random.nextDouble());
        final double turn = random.nextDouble() * 2 * Math.PI;
        final double half = width / 2 - 1;
        final double x = landing.x() + reach * Math.cos(turn) * FlightComputerBlockEntity.ellipseLength(guidance) / 2;
        final double z = landing.z() + reach * Math.sin(turn) * FlightComputerBlockEntity.ellipseWidth(guidance) / 2;
        return new double[] {Math.clamp(x, -half, half), Math.clamp(z, -half / 2, half / 2)};
    }

    /* The dust a touchdown on {@code body} raises. */
    private static ResourceLocation dustOf(final SolarBody body) {
        return switch (body) {
            case MOON -> MOON_DUST;
            case MARS -> MARS_DUST;
            default -> DUST;
        };
    }

    /* The level of the world {@code world}, on {@code level}'s server; none if it is not there. */
    private static @Nullable ServerLevel levelOf(final ServerLevel level, final ResourceLocation world) {
        return SpaceWorlds.get(level, world).map(found -> level.getServer().getLevel(found.dimension()))
                .orElse(null);
    }

    /* The world {@code level} is, by its id among the worlds; none for a level that is not one. */
    private static @Nullable ResourceLocation worldOf(final Level level) {
        for (final Map.Entry<ResourceLocation, SpaceWorld> world : SpaceWorlds.all(level).entrySet()) {
            if (world.getValue().dimension().equals(level.dimension())) {
                return world.getKey();
            }
        }
        return null;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }
}
