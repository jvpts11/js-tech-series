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
 * the ship DOCK docks with, and its drift on its thrusters as it docks. Kept with its flight, so it flies on after a
 * restart.
 *
 * @param mode  the mode engaged
 * @param spot  where LAND sets it down: the middle of a landing platform's top
 * @param other the ship DOCK docks with
 * @param drift its drift on its thrusters, docking, in metres a second along the world
 */
public record ShipAutopilot(Mode mode, Optional<Vec3> spot, Optional<StructureUuid> other, Vec3 drift) {

    public static final Codec<ShipAutopilot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(Mode.class).fieldOf("mode").forGetter(ShipAutopilot::mode),
            Vec3.CODEC.optionalFieldOf("spot").forGetter(ShipAutopilot::spot),
            StructureUuid.CODEC.optionalFieldOf("other").forGetter(ShipAutopilot::other),
            Vec3.CODEC.optionalFieldOf("drift", Vec3.ZERO).forGetter(ShipAutopilot::drift)
    ).apply(instance, ShipAutopilot::new));

    /** LAND, onto {@code spot}. */
    public static ShipAutopilot land(final Vec3 spot) {
        return new ShipAutopilot(Mode.LAND, Optional.of(spot), Optional.empty(), Vec3.ZERO);
    }

    /** ORBIT. */
    public static ShipAutopilot orbit() {
        return new ShipAutopilot(Mode.ORBIT, Optional.empty(), Optional.empty(), Vec3.ZERO);
    }

    /** DOCK, with {@code other}. */
    public static ShipAutopilot dock(final StructureUuid other) {
        return new ShipAutopilot(Mode.DOCK, Optional.empty(), Optional.of(other), Vec3.ZERO);
    }

    /** It, drifting at {@code next}. */
    public ShipAutopilot drifting(final Vec3 next) {
        return new ShipAutopilot(this.mode, this.spot, this.other, next);
    }

    /** The autopilot's modes, as its HUD lists them. */
    public enum Mode implements IStableName {
        /** Down onto a landing platform. */
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
