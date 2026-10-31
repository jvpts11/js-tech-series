/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.uuid.StructureUuid;
import java.util.Optional;
import net.minecraft.world.phys.Vec3;

/**
 * A ship's autopilot engaged ({@link ShipAutopilots}): which of its three modes, the landing spot LAND sets it down on,
 * the ship DOCK docks with, and its drift on its thrusters as it docks. LAND onto a runway, at a runway beacon, keeps
 * the way the runway runs and whether its stretch was found clear. Kept with its flight, so it flies on after a
 * restart.
 *
 * @param mode    the mode engaged
 * @param spot    where LAND sets it down: the middle of a ship landing pad's top, or the foot of a runway beacon
 * @param other   the ship DOCK docks with
 * @param drift   its drift on its thrusters, docking, in metres a second along the world
 * @param runway  landing on a runway, the way it runs from its beacon, in degrees east of north
 * @param cleared landing on a runway, whether its stretch was found long and free; till then it is looked at as the
 *                ship comes near and its land loads
 */
public record ShipAutopilot(Mode mode, Optional<Vec3> spot, Optional<StructureUuid> other, Vec3 drift,
                            Optional<Double> runway, boolean cleared) {

    public static final Codec<ShipAutopilot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(Mode.class).fieldOf("mode").forGetter(ShipAutopilot::mode),
            Vec3.CODEC.optionalFieldOf("spot").forGetter(ShipAutopilot::spot),
            StructureUuid.CODEC.optionalFieldOf("other").forGetter(ShipAutopilot::other),
            Vec3.CODEC.optionalFieldOf("drift", Vec3.ZERO).forGetter(ShipAutopilot::drift),
            Codec.DOUBLE.optionalFieldOf("runway").forGetter(ShipAutopilot::runway),
            Codec.BOOL.optionalFieldOf("cleared", false).forGetter(ShipAutopilot::cleared)
    ).apply(instance, ShipAutopilot::new));

    /** LAND, onto {@code spot}. */
    public static ShipAutopilot land(final Vec3 spot) {
        return new ShipAutopilot(Mode.LAND, Optional.of(spot), Optional.empty(), Vec3.ZERO, Optional.empty(), false);
    }

    /**
     * LAND onto the runway whose beacon's foot is {@code beacon}, the runway running {@code heading} degrees east of
     * north from it; {@code cleared} if its stretch was found long and free already.
     */
    public static ShipAutopilot runway(final Vec3 beacon, final double heading, final boolean cleared) {
        return new ShipAutopilot(Mode.LAND, Optional.of(beacon), Optional.empty(), Vec3.ZERO, Optional.of(heading),
                cleared);
    }

    /** ORBIT. */
    public static ShipAutopilot orbit() {
        return new ShipAutopilot(Mode.ORBIT, Optional.empty(), Optional.empty(), Vec3.ZERO, Optional.empty(), false);
    }

    /** DOCK, with {@code other}. */
    public static ShipAutopilot dock(final StructureUuid other) {
        return new ShipAutopilot(Mode.DOCK, Optional.empty(), Optional.of(other), Vec3.ZERO, Optional.empty(), false);
    }

    /** It, drifting at {@code next}. */
    public ShipAutopilot drifting(final Vec3 next) {
        return new ShipAutopilot(this.mode, this.spot, this.other, next, this.runway, this.cleared);
    }

    /** It, its runway's stretch found long and free. */
    public ShipAutopilot clearedRunway() {
        return new ShipAutopilot(this.mode, this.spot, this.other, this.drift, this.runway, true);
    }

    /** The autopilot's modes, as its HUD lists them. */
    public enum Mode implements IStableName {
        /** Down onto a ship landing pad, or onto a runway. */
        LAND("land"),
        /** Up over the line to space. */
        ORBIT("orbit"),
        /** Docked to another ship, collar to collar, in space. */
        DOCK("dock");

        private final String serializedName;

        Mode(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }
}
