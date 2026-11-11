/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.core.moving.BlockAboard;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.StructureAutopilot;
import dev.jstech.core.moving.StructureReadings;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.signal.Reading;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The moving structure a machine stands on, as its programs fly it: J's Core's autopilot told where to go, and the
 * readings a flight sensor aboard would give, taken where the machine stands. A machine on the ground reads the place
 * it stands at, still, and has nothing to fly.
 *
 * <p>The readings are taken once a tick and kept for the rest of it, since a program that reads its altitude, speed
 * and heading one after another would otherwise look down and ahead three times over.
 */
public final class NavigationService {

    private final BlockEntity block;
    private final ServerLevel level;
    /* The readings of the tick they were taken in, or none yet. */
    private List<Reading> readings = List.of();
    private long readAt = Long.MIN_VALUE;

    /* How near the place a program names another structure's clamp is looked for, in blocks. */
    private static final double CLAMP_LOOK = 1.0;

    NavigationService(final BlockEntity block, final ServerLevel level) {
        this.block = block;
        this.level = level;
    }

    /** Whether the machine stands on one of J's Core's moving structures, which its programs can fly. */
    public boolean aboard() {
        return this.structure() != null;
    }

    /** Whether an autopilot flies the structure the machine stands on now. */
    public boolean piloted() {
        final MovingStructure structure = this.structure();
        return structure != null && structure.autopilot() != null;
    }

    /** Where in the world the machine is now: where its structure has it, or where it stands. */
    public Vec3 where() {
        return MovingStructures.inWorld(this.level, Vec3.atCenterOf(this.block.getBlockPos()));
    }

    /** The reading of that name a flight sensor would give where the machine stands, this tick. */
    public double reading(final TextKey name) {
        final long now = this.level.getGameTime();
        if (now != this.readAt) {
            this.readings = StructureReadings.of(this.level, this.block.getBlockPos());
            this.readAt = now;
        }
        for (final Reading reading : this.readings) {
            if (reading.name().equals(name)) {
                return reading.value();
            }
        }
        return 0;
    }

    /** Hands the structure the machine stands on to {@code pilot}, or takes its autopilot off for null. */
    public boolean fly(@Nullable final StructureAutopilot pilot) {
        final MovingStructure structure = this.structure();
        if (structure == null) {
            return false;
        }
        structure.fly(pilot);
        return true;
    }

    /**
     * Docks the structure the machine stands on to the clamp of another structure at the world's place {@code point},
     * no faster than {@code speed}: false when it stands on none, or no other structure has a clamp there.
     */
    public boolean dock(final Vec3 point, final double speed) {
        final MovingStructure own = this.structure();
        if (own == null) {
            return false;
        }
        for (final IStructureBody body : MovingStructures.near(this.level, AABB.ofSize(point, CLAMP_LOOK, CLAMP_LOOK,
                CLAMP_LOOK))) {
            if (body instanceof MovingStructure host && host != own) {
                final BlockPos clamp = BlockPos.containing(MovingStructures.toYard(host, point));
                if (MovingStructures.at(this.level, clamp) == host
                        && BlockAboard.at(this.level, clamp).clamp().isPresent()) {
                    own.fly(StructureAutopilot.dock(host.id(), clamp, speed));
                    return true;
                }
            }
        }
        return false;
    }

    /* The structure whose blocks hold the machine, or null when it stands in the world. */
    @Nullable
    private MovingStructure structure() {
        final BlockPos pos = this.block.getBlockPos();
        return YardLayout.isYardBlock(pos.getX()) && MovingStructures.at(this.level, pos) instanceof MovingStructure
                structure ? structure : null;
    }
}
