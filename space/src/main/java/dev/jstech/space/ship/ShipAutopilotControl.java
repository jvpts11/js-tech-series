/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.text.GameText;
import dev.jstech.space.JsSpace;
import dev.jstech.space.landing.Runways;
import dev.jstech.space.landing.ShipLandingTexts;
import dev.jstech.space.landing.ShipLandings;
import dev.jstech.space.universe.SystemSpaces;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * A ship's autopilot, changed at its helm: its key, J until the player changes it, takes it on to its next mode the
 * ship can fly, LAND, ORBIT and DOCK in turn, and off after the last: LAND to the nearest ship landing pad the pilot
 * may use, or, on wheels, the nearest runway whose stretch is clear; ORBIT from a world up to its space; DOCK in space
 * to the nearest ship with a collar. Taking the helm, holding any of its keys, lets the autopilot go.
 */
public final class ShipAutopilotControl {

    /** The key that changes the autopilot of the ship its player pilots. */
    public static final KeyAction KEY = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_autopilot"), ShipFlightTexts.AUTOPILOT_KEY)
            .key(GLFW.GLFW_KEY_J));
    /* How far a landing pad or a runway, and another ship to dock with, may be, in metres. */
    private static final double LAND_RANGE = 512;
    private static final double DOCK_RANGE = 200;
    /* The modes in the order the key goes through them. */
    private static final List<ShipAutopilot.Mode> ORDER = List.of(ShipAutopilot.Mode.LAND, ShipAutopilot.Mode.ORBIT,
            ShipAutopilot.Mode.DOCK);

    private ShipAutopilotControl() {
    }

    /** Declares the key, once, from the mod's constructor: loading the class declares it. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * {@code player} pressed the key: the ship whose helm they sit at goes on to its autopilot's next mode; docked to
     * another ship, it lets go of it ({@link ShipDocks#undock}).
     */
    public static void pressed(final ServerPlayer player) {
        if (!(player.getVehicle() instanceof StructureSeatEntity seat) || !seat.helm()
                || !(seat.structure() instanceof MovingStructure ship) || !(ship.motion() instanceof ShipFlight flight)
                || !(ship.level() instanceof ServerLevel level)) {
            return;
        }
        if (ShipDocks.undock(ship)) {
            return;
        }
        MovingStructures.move(ship, flight.engaged(next(player, level, ship, flight.autopilot())));
    }

    /** The mode after {@code now} that {@code ship}, flown by {@code pilot}, can fly; none after the last. */
    public static Optional<ShipAutopilot> next(final ServerPlayer pilot, final ServerLevel level,
                                               final MovingStructure ship, final Optional<ShipAutopilot> now) {
        for (int i = now.map(engaged -> ORDER.indexOf(engaged.mode()) + 1).orElse(0); i < ORDER.size(); i++) {
            final Optional<ShipAutopilot> offered = switch (ORDER.get(i)) {
                case LAND -> ship.motion() instanceof ShipFlight flight ? landing(pilot, level, ship, flight)
                        : Optional.empty();
                case ORBIT -> SystemSpaces.systemOf(level.dimension()).isEmpty() ? Optional.of(ShipAutopilot.orbit())
                        : Optional.empty();
                case DOCK -> docking(level, ship);
            };
            if (offered.isPresent()) {
                return offered;
            }
        }
        return Optional.empty();
    }

    /*
     * LAND onto the nearest of the ship landing pads {@code pilot} may use in the ship's world, another ship standing
     * on none, and, for a ship on wheels, the runways whose stretch is clear, within range. A runway nearer than where
     * it lands whose stretch is not clear, its pilot is told of.
     */
    private static Optional<ShipAutopilot> landing(final ServerPlayer pilot, final ServerLevel level,
                                                   final MovingStructure ship, final ShipFlight flight) {
        final Vec3 at = new Vec3(ship.pose().x(), ship.pose().y(), ship.pose().z());
        Optional<ShipAutopilot> best = Optional.empty();
        double nearest = LAND_RANGE;
        for (final ShipLandings.Pad pad : ShipLandings.padsUsableBy(pilot, level)) {
            final Vec3 spot = ShipLandings.padSpot(level, pad);
            if (spot.distanceTo(at) <= nearest && !ShipLandings.taken(level, spot, ship)) {
                nearest = spot.distanceTo(at);
                best = Optional.of(ShipAutopilot.land(spot));
            }
        }
        if (flight.ship().gear() != ShipFlightModel.Gear.WHEELS) {
            return best;
        }
        double blocked = Double.POSITIVE_INFINITY;
        for (final ShipLandings.Beacon beacon : ShipLandings.beacons(level)) {
            final double away = beacon.foot().distanceTo(at);
            if (away > nearest) {
                continue;
            }
            // Its stretch looked at now where its land is loaded, else as the ship comes near.
            final Optional<Boolean> clear = Runways.clear(level, beacon, ship);
            if (clear.isPresent() && !clear.get()) {
                blocked = Math.min(blocked, away);
                continue;
            }
            nearest = away;
            best = Optional.of(ShipAutopilot.runway(beacon.foot(), beacon.heading(), clear.isPresent()));
        }
        if (blocked < nearest) {
            pilot.displayClientMessage(GameText.component(ShipLandingTexts.RUNWAY_BLOCKED), true);
        }
        return best;
    }

    /* DOCK, in space, with the nearest other ship with a collar, its own having one too. */
    private static Optional<ShipAutopilot> docking(final ServerLevel level, final MovingStructure ship) {
        if (SystemSpaces.systemOf(level.dimension()).isEmpty() || ShipCollars.mouth(level, ship).isEmpty()) {
            return Optional.empty();
        }
        final Vec3 at = new Vec3(ship.pose().x(), ship.pose().y(), ship.pose().z());
        MovingStructure nearest = null;
        double best = DOCK_RANGE;
        for (final IStructureBody body : MovingStructures.near(level, new AABB(at, at).inflate(DOCK_RANGE))) {
            if (body == ship || !(body instanceof MovingStructure other) || !(other.motion() instanceof ShipFlight)) {
                continue;
            }
            final Optional<Vec3> mouth = ShipCollars.mouth(level, other);
            if (mouth.isPresent() && mouth.get().distanceTo(at) < best) {
                best = mouth.get().distanceTo(at);
                nearest = other;
            }
        }
        return nearest == null ? Optional.empty() : Optional.of(ShipAutopilot.dock(nearest.id()));
    }
}
