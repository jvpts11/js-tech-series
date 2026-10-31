/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import com.mojang.serialization.Codec;
import dev.jstech.core.air.AirMix;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.RoomLiving;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.util.Loaded;
import dev.jstech.space.shipyard.ShipyardPlans;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A small ship's air reserve, kept by its cockpit's flight console: a small ship has no life support section, its
 * pressurised cabin topped up from this for its short trips, oxygen and nitrogen each back to the sea's pressure as
 * the crew breathe and the hull leaks, while the reserve lasts; it asks for its cabin to be found as a room, wherever
 * its ship goes. An oxygen bottle pours into it. Once the ship has left
 * its dock, the console keeps the ship too ({@link ShipAboard}), and draws its parts aboard; and watches its plan
 * there, so a block broken aboard leaves a hole its part holds round, and the right block put back mends it. It shows
 * the ship's flight as it flies ({@link ShipReadings}), and keeps its fusion reactor's deuterium, which the reactor
 * burns as the ship flies and a dock fills again.
 */
public final class AirReserveEntity extends SyncedBlockEntity {

    private final ValueField<Double> oxygen;
    private final ValueField<Double> nitrogen;
    private final ValueField<ShipAboard> ship;
    private final ValueField<List<Integer>> holes;
    private final ValueField<ShipReadings> flight;
    private final ValueField<Double> deuterium;
    /* The ship's plan, watched aboard once the ship is a structure; on the server. */
    private @Nullable PlannedBuild aboard;
    /* What its reactor has burnt since its load was last told, on the server: told a thousandth at a time. */
    private double burnt;
    /** The oxygen a full reserve holds, in moles: two hours of one crew member's breathing. */
    public static final double OXYGEN = 2 * 3600 * RoomLiving.PLAYER_BREATH;
    /** The nitrogen a full reserve holds, in moles, to make good what leaks. */
    public static final double NITROGEN = 270;
    /* The sea's air, in kilopascals of each gas. */
    private static final double OXYGEN_KPA = 21.2;
    private static final double NITROGEN_KPA = 79.1;
    private static final int TICKS_A_SECOND = 20;
    /* How finely its reactor's load is told its players, as a share of a full one. */
    private static final double LOAD_TOLD = 0.001;

    public AirReserveEntity(final BlockPos pos, final BlockState state) {
        super(ShipKitBlocks.AIR_RESERVE_ENTITY.get(), pos, state);
        this.oxygen = fields().value("Oxygen", Codec.DOUBLE, OXYGEN).save().toClient();
        this.nitrogen = fields().value("Nitrogen", Codec.DOUBLE, NITROGEN).save().toClient();
        this.ship = fields().nullable("Ship", ShipAboard.CODEC).save().toClient();
        this.holes = fields().value("Holes", Codec.INT.listOf(), List.<Integer>of()).toClient();
        this.flight = fields().value("Flight", ShipReadings.CODEC, ShipReadings.PARKED).toClient();
        this.deuterium = fields().value("Deuterium", Codec.DOUBLE, 1.0).save().toClient();
    }

    /** How much of a full load of deuterium its ship's fusion reactor has left, from nought to one. */
    public double deuterium() {
        final Double left = this.deuterium.get();
        return left == null ? 0 : left;
    }

    /** Its ship's reactor burns {@code share} of a full load of deuterium; whether it had any to burn. */
    public boolean burn(final double share) {
        final double left = deuterium();
        if (left <= 0) {
            return false;
        }
        this.burnt += share;
        if (this.burnt >= LOAD_TOLD || this.burnt >= left) {
            this.deuterium.set(Math.max(0, left - this.burnt));
            this.burnt = 0;
            setChanged();
        }
        return true;
    }

    /** What its ship's flight shows, as its players are told. */
    public ShipReadings flight() {
        final ShipReadings kept = this.flight.get();
        return kept == null ? ShipReadings.PARKED : kept;
    }

    /** Its ship flew a tick, showing {@code readings}: its players are told when they change. */
    public void flew(final ShipReadings readings) {
        if (!readings.equals(this.flight.get())) {
            this.flight.set(readings);
        }
    }

    /** The ship it is the console of, once it has left its dock. */
    public Optional<ShipAboard> ship() {
        return Optional.ofNullable(this.ship.get());
    }

    /** Keeps {@code kept}, the ship it is the console of, as the ship leaves its dock. */
    public void carry(final ShipAboard kept) {
        this.ship.set(kept);
        setChanged();
    }

    /** Lets its ship's plan go, as a dock takes the ship back onto the grid, and the dock fills its reactor again. */
    public void land() {
        stopWatching();
        this.ship.set(null);
        this.holes.set(List.of());
        this.deuterium.set(1.0);
        this.burnt = 0;
        setChanged();
    }

    /** The places of its ship's plan missing a block in a formed part, by their numbers, as its players are told. */
    public List<Integer> holes() {
        final List<Integer> kept = this.holes.get();
        return kept == null ? List.of() : kept;
    }

    /** Loaded, on the server: it asks for its cabin to be found, as a room is only where a block asks for one. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel level) {
            SealedRooms.track(level, getBlockPos());
        }
    }

    /** Its chunk unloads: it stops watching its ship's plan, for when it loads again. */
    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        stopWatching();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        stopWatching();
        // Taken away, not unloaded: its cabin is no longer asked for.
        if (getLevel() instanceof ServerLevel level && level.isLoaded(getBlockPos())
                && !(level.getBlockEntity(getBlockPos()) instanceof AirReserveEntity)) {
            SealedRooms.untrack(level, getBlockPos());
        }
    }

    /* Moved with its ship, into its yard or another dimension's: where it stood, its cabin is no longer asked for. */
    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        if (move.from() instanceof ServerLevel left) {
            SealedRooms.untrack(left, was);
        }
    }

    /** The oxygen left in it, in moles. */
    public double oxygen() {
        return this.oxygen.get();
    }

    /** The nitrogen left in it, in moles. */
    public double nitrogen() {
        return this.nitrogen.get();
    }

    /** Pours up to {@code moles} of oxygen into it; how much went in, never past what it holds full. */
    public double pour(final double moles) {
        final double went = Math.max(0, Math.min(moles, OXYGEN - oxygen()));
        if (went > 0) {
            this.oxygen.set(oxygen() + went);
            setChanged();
        }
        return went;
    }

    /**
     * Once a second, on the server: tops up the cabin beside it from the reserve, and watches its ship's plan aboard
     * once its ship is a structure.
     */
    public void tick() {
        if (!(getLevel() instanceof ServerLevel level) || level.getGameTime() % TICKS_A_SECOND != 0) {
            return;
        }
        if (this.aboard == null) {
            watch(level);
        }
        final Optional<Room> cabin = SealedRooms.roomBeside(level, getBlockPos());
        if (cabin.isEmpty()) {
            return;
        }
        final double gaveOxygen = topUp(cabin.get(), AirMix.OXYGEN, OXYGEN_KPA, oxygen());
        final double gaveNitrogen = topUp(cabin.get(), AirMix.NITROGEN, NITROGEN_KPA, nitrogen());
        if (gaveOxygen > 0 || gaveNitrogen > 0) {
            this.oxygen.set(oxygen() - gaveOxygen);
            this.nitrogen.set(nitrogen() - gaveNitrogen);
            setChanged();
        }
    }

    /*
     * Watches its ship's plan where the ship's blocks stand in its structure, once they have come into it: each part
     * holds round a hole, and mends as the right block is put back.
     */
    private void watch(final ServerLevel level) {
        final ShipAboard kept = this.ship.get();
        if (kept == null || !YardLayout.isYardBlock(getBlockPos().getX())) {
            return;
        }
        final PlannedBuild plan = new PlannedBuild(ShipyardPlans.of(level, kept.design(), kept.orientation()),
                getBlockPos().offset(kept.nose())).holed();
        for (final PlanSpot spot : plan.plan().spots()) {
            if (!Loaded.at(level, plan.where(spot))) {
                return;
            }
        }
        this.aboard = plan;
        plan.start(level, getBlockPos(), this::told);
    }

    private void stopWatching() {
        final PlannedBuild was = this.aboard;
        this.aboard = null;
        if (was != null) {
            was.stop(false);
        }
    }

    /* Its ship's plan's counts changed: its players hear the holes, and its parts formed are kept. */
    private void told() {
        final PlannedBuild now = this.aboard;
        final ShipAboard kept = this.ship.get();
        if (now == null || kept == null) {
            return;
        }
        this.holes.set(ShipyardPlans.holesIn(now));
        if (!now.formedParts().equals(kept.formed())) {
            this.ship.set(new ShipAboard(kept.design(), kept.nose(), kept.turn(), now.formedParts()));
        }
        setChanged();
    }

    /* Gives {@code room} as much of {@code gas} as brings it back to {@code kilopascals}, out of {@code left}. */
    private static double topUp(final Room room, final ResourceLocation gas, final double kilopascals,
                                final double left) {
        final double wanted = GasMath.moles(kilopascals, room.gasVolume(), GasMath.TEMPERED) - room.moles(gas);
        final double given = Math.max(0, Math.min(wanted, left));
        if (given > 0) {
            room.add(gas, given);
        }
        return given;
    }
}
