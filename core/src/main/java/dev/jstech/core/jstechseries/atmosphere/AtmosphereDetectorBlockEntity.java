/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.atmosphere;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.moving.BlockMove;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.RoomAir;
import dev.jstech.core.room.RoomFace;
import dev.jstech.core.room.SealedRooms;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * An Atmosphere Detector: once a second it reads the air round it, a sealed room's beside it, or the place's in front
 * of it, and sends what it read to the players who see it. It asks for the room it stands in to be found.
 */
public final class AtmosphereDetectorBlockEntity extends SyncedBlockEntity {

    private final ValueField<AtmosphereReadings> readings = fields().value("Readings", AtmosphereReadings.CODEC,
            AtmosphereReadings.NONE).toClient();
    private double lastPressure = -1;

    private static final int EVERY = 20;

    public AtmosphereDetectorBlockEntity(final BlockPos pos, final BlockState state) {
        super(AtmosphereDetector.ENTITY.get(), pos, state);
    }

    /** What it read last. */
    public AtmosphereReadings readings() {
        final AtmosphereReadings read = this.readings.get();
        return read == null ? AtmosphereReadings.NONE : read;
    }

    /** Reads the air once a second, on the server. */
    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final AtmosphereDetectorBlockEntity detector) {
        if (level instanceof ServerLevel server && level.getGameTime() % EVERY == 0) {
            detector.read(server);
        }
    }

    /** Reads the air now: its room's, or the place's in front of it. */
    public void read(final ServerLevel level) {
        final Optional<Room> found = SealedRooms.roomBeside(level, this.worldPosition);
        final double pressure;
        final List<AtmosphereReadings.Gas> gases = new ArrayList<>();
        AtmosphereReadings read;
        if (found.isPresent()) {
            final Room room = found.get();
            final RoomAir air = room.air();
            pressure = air.pressure();
            for (final ResourceLocation gas : air.moles().keySet()) {
                gases.add(new AtmosphereReadings.Gas(gas, air.share(gas)));
            }
            final Set<BlockPos> leaking = new LinkedHashSet<>();
            for (final RoomFace face : room.leaks()) {
                if (face.area() >= GasMath.NOTICEABLE) {
                    leaking.add(face.block());
                }
            }
            final AABB box = room.bounds();
            final BlockPos corner = BlockPos.containing(box.minX, box.minY, box.minZ);
            final List<BlockPos> spots = new ArrayList<>();
            for (final BlockPos leak : leaking) {
                if (spots.size() < AtmosphereReadings.MOST_SPOTS) {
                    spots.add(leak.subtract(corner));
                }
            }
            read = new AtmosphereReadings(pressure, 0, sorted(gases), true, room.volume(), BlockPos.containing(
                    box.getXsize(), box.getYsize(), box.getZsize()), leaking.size(), spots);
        } else {
            final Climate climate = SealedRooms.placeClimate(level, Vec3.atCenterOf(this.worldPosition.relative(
                    facing())));
            pressure = climate.pressure();
            for (final Map.Entry<ResourceLocation, Double> gas : SealedRooms.airOf(climate).shares().entrySet()) {
                gases.add(new AtmosphereReadings.Gas(gas.getKey(), pressure > 0 ? gas.getValue() : 0));
            }
            read = new AtmosphereReadings(pressure, 0, sorted(gases), false, 0, BlockPos.ZERO, 0, List.of());
        }
        final double change = this.lastPressure < 0 ? 0 : pressure - this.lastPressure;
        this.lastPressure = pressure;
        this.readings.set(new AtmosphereReadings(read.pressure(), change, read.gases(), read.room(), read.volume(),
                read.size(), read.leaks(), read.spots()));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level instanceof ServerLevel server) {
            SealedRooms.track(server, this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // Taken away, not unloaded: the room it asked for is no longer asked for.
        if (this.level instanceof ServerLevel server && server.isLoaded(this.worldPosition)
                && !server.getBlockState(this.worldPosition).is(AtmosphereDetector.BLOCK.get())) {
            SealedRooms.untrack(server, this.worldPosition);
        }
    }

    /* Moved with a moving structure: the room it asked for where it stood is no longer asked for; it asks here. */
    @Override
    public void moved(final BlockPos was, final BlockMove move) {
        super.moved(was, move);
        if (move.from() instanceof ServerLevel left) {
            SealedRooms.untrack(left, was);
        }
    }

    private Direction facing() {
        return getBlockState().hasProperty(HorizontalDirectionalBlock.FACING)
                ? getBlockState().getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
    }

    /* The gases, the largest share first, as many as its display shows. */
    private static List<AtmosphereReadings.Gas> sorted(final List<AtmosphereReadings.Gas> gases) {
        return gases.stream().filter(gas -> gas.share() > 0)
                .sorted(Comparator.comparingDouble(AtmosphereReadings.Gas::share).reversed())
                .limit(AtmosphereReadings.MOST_GASES).toList();
    }
}
