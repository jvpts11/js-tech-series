/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.space.landing.ShipLandings;
import dev.jstech.space.landing.ShipLiftOffs;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SpaceClimbs;
import dev.jstech.space.universe.SystemLayout;
import dev.jstech.space.universe.SystemSpaces;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where a ship comes down, as its pilot may choose on its console in orbit: the places of the world below it a pilot
 * may come down onto ({@link ShipDestination}), and, once one is chosen, its next way down into that world coming
 * down over it, at the height every way down arrives at, the choice then spent. With none chosen, a ship comes down
 * under where it is.
 */
public final class ShipDestinations {

    /* The most landing pads the console offers, their numbers going by where they stand. */
    private static final int MOST_PADS = 20;
    private static final double HALF = 0.5;

    private ShipDestinations() {
    }

    /**
     * What the console {@code console} offers {@code player} to come down onto: the world its ship is over, in the
     * system's space, and its places; nothing away from the low orbit of a world.
     */
    public static Offer offeredTo(final ServerPlayer player, final AirReserveEntity console) {
        if (!(console.getLevel() instanceof ServerLevel space) || SystemSpaces.systemOf(space.dimension()).isEmpty()
                || !(MovingStructures.at(space, console.getBlockPos()) instanceof MovingStructure ship)) {
            return Offer.NONE;
        }
        final SolarBody body = SystemLayout.over(ship.pose().x(), ship.pose().z());
        final ServerLevel world = SpaceClimbs.worldBelow(space, ship.pose().x(), ship.pose().z()).orElse(null);
        if (body == null || world == null) {
            return Offer.NONE;
        }
        return new Offer(body.name(), places(player, world, console.dock()));
    }

    /**
     * The places of {@code world} {@code player} may come down onto: the dock their ship last left, {@code dock},
     * when it is in that world; the landing pads they may use there; their bed there; and where they last took off
     * from there.
     */
    public static List<ShipDestination> places(final ServerPlayer player, final ServerLevel world,
                                               final Optional<GlobalPos> dock) {
        final List<ShipDestination> out = new ArrayList<>();
        dock.filter(left -> left.dimension().equals(world.dimension())).ifPresent(left -> out.add(
                new ShipDestination(ShipDestination.Kind.DOCK, 0, world.dimension(), left.pos())));
        // A pad afloat on a moving structure goes where its structure goes: it is no place of the world.
        final List<BlockPos> pads = ShipLandings.padsUsableBy(player, world).stream().map(ShipLandings.Pad::middle)
                .filter(middle -> !YardLayout.isYardBlock(middle.getX()))
                .sorted(Comparator.comparingInt((BlockPos middle) -> middle.getX()).thenComparingInt(BlockPos::getZ))
                .limit(MOST_PADS).toList();
        for (int k = 0; k < pads.size(); k++) {
            out.add(new ShipDestination(ShipDestination.Kind.PAD, k + 1, world.dimension(), pads.get(k)));
        }
        final BlockPos bed = player.getRespawnPosition();
        if (bed != null && player.getRespawnDimension().equals(world.dimension())) {
            out.add(new ShipDestination(ShipDestination.Kind.BED, 0, world.dimension(), bed));
        }
        ShipLiftOffs.of(player, world.dimension()).ifPresent(place -> out.add(new ShipDestination(
                ShipDestination.Kind.LIFT_OFF, 0, world.dimension(), place)));
        return List.copyOf(out);
    }

    /**
     * Where {@code ship}, in the system's space {@code space}, comes down into {@code world}, arriving at
     * {@code under} with no place chosen: over the place its pilot chose in that world, the choice spent, or at
     * {@code under}.
     */
    public static Arrival arrival(final ServerLevel space, final MovingStructure ship, final ServerLevel world,
                                  final Vec3 under) {
        final AirReserveEntity console = consoleOf(space, ship);
        final ShipDestination chosen = console == null ? null : console.destination()
                .filter(place -> place.dimension().equals(world.dimension())).orElse(null);
        if (chosen == null) {
            return new Arrival(under, null, null);
        }
        console.destination(null);
        return new Arrival(new Vec3(chosen.at().getX() + HALF, under.y, chosen.at().getZ() + HALF), console, chosen);
    }

    /* The console of {@code ship} in {@code level}, where its yard's land is loaded; none for a ship with none. */
    private static @Nullable AirReserveEntity consoleOf(final ServerLevel level, final MovingStructure ship) {
        if (!(ship.motion() instanceof ShipFlight flight)) {
            return null;
        }
        final Optional<BlockPos> at = flight.consoleIn(ship);
        return at.isPresent() && level.isLoaded(at.get())
                && level.getBlockEntity(at.get()) instanceof AirReserveEntity console ? console : null;
    }

    /**
     * What a console offers to come down onto.
     *
     * @param body   the name of the sphere of the world below, or empty away from the low orbit of one
     * @param places the places of it a pilot may come down onto
     */
    public record Offer(String body, List<ShipDestination> places) {

        /** Nothing: away from the low orbit of a world. */
        public static final Offer NONE = new Offer("", List.of());

        public Offer {
            places = List.copyOf(places);
        }
    }

    /**
     * A ship's way down into a world.
     *
     * @param at      where it arrives
     * @param console the console whose choice it spends, or none
     * @param chosen  the place chosen it comes down over, or none
     */
    public record Arrival(Vec3 at, @Nullable AirReserveEntity console, @Nullable ShipDestination chosen) {

        /** It stayed up after all: the place chosen is kept for its next way down. */
        public void stayedUp() {
            if (this.console != null && this.chosen != null) {
                this.console.destination(this.chosen);
            }
        }
    }
}
