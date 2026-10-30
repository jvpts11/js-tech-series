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
 * the crew breathe and the hull leaks, while the reserve lasts. An oxygen bottle pours into it. Once the ship has left
 * its dock, the console keeps the ship too ({@link ShipAboard}), and draws its parts aboard; and watches its plan
 * there, so a block broken aboard leaves a hole its part holds round, and the right block put back mends it.
 */
public final class AirReserveEntity extends SyncedBlockEntity {

    private final ValueField<Double> oxygen;
    private final ValueField<Double> nitrogen;
    private final ValueField<ShipAboard> ship;
    private final ValueField<List<Integer>> holes;
    /* The ship's plan, watched aboard once the ship is a structure; on the server. */
    private @Nullable PlannedBuild aboard;
    /** The oxygen a full reserve holds, in moles: two hours of one crew member's breathing. */
    public static final double OXYGEN = 2 * 3600 * RoomLiving.PLAYER_BREATH;
    /** The nitrogen a full reserve holds, in moles, to make good what leaks. */
    public static final double NITROGEN = 270;
    /* The sea's air, in kilopascals of each gas. */
    private static final double OXYGEN_KPA = 21.2;
    private static final double NITROGEN_KPA = 79.1;
    private static final int TICKS_A_SECOND = 20;

    public AirReserveEntity(final BlockPos pos, final BlockState state) {
        super(ShipKitBlocks.AIR_RESERVE_ENTITY.get(), pos, state);
        this.oxygen = fields().value("Oxygen", Codec.DOUBLE, OXYGEN).save().toClient();
        this.nitrogen = fields().value("Nitrogen", Codec.DOUBLE, NITROGEN).save().toClient();
        this.ship = fields().nullable("Ship", ShipAboard.CODEC).save().toClient();
        this.holes = fields().value("Holes", Codec.INT.listOf(), List.<Integer>of()).toClient();
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

    /** Lets its ship's plan go, as a dock takes the ship back onto the grid. */
    public void land() {
        stopWatching();
        this.ship.set(null);
        this.holes.set(List.of());
        setChanged();
    }

    /** The places of its ship's plan missing a block in a formed part, by their numbers, as its players are told. */
    public List<Integer> holes() {
        final List<Integer> kept = this.holes.get();
        return kept == null ? List.of() : kept;
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
