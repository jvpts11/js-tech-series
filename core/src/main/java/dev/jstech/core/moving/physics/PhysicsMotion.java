/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving.physics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructurePose;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * Moves a structure by its physics: its weight and what holds it up, its engines, wings, wheels, balloons and the
 * rest, the wind, and what it meets ({@link StructurePhysics}). The rule never ends of itself: a structure falls,
 * floats, flies or rests by its physics until another rule is set. It holds how fast the structure goes and turns,
 * and its balloons' warm air, so a ship in flight flies on after a restart.
 *
 * <pre>{@code
 * MovingStructures.move(structure, PhysicsMotion.AT_REST);   // let it go: it falls, or floats, as it would
 * }</pre>
 *
 * @param vx   how fast its centre goes east, in metres a second
 * @param vy   how fast it goes up
 * @param vz   how fast it goes south
 * @param sx   how fast it turns about the world's east, in radians a second
 * @param sy   how fast it turns about the world's up
 * @param sz   how fast it turns about the world's south
 * @param warm the warm air of its balloons
 */
public record PhysicsMotion(double vx, double vy, double vz, double sx, double sy, double sz, List<WarmRoom> warm)
        implements IStructureMotion {

    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "physics");
    public static final MapCodec<PhysicsMotion> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("vx", 0.0).forGetter(PhysicsMotion::vx),
            Codec.DOUBLE.optionalFieldOf("vy", 0.0).forGetter(PhysicsMotion::vy),
            Codec.DOUBLE.optionalFieldOf("vz", 0.0).forGetter(PhysicsMotion::vz),
            Codec.DOUBLE.optionalFieldOf("sx", 0.0).forGetter(PhysicsMotion::sx),
            Codec.DOUBLE.optionalFieldOf("sy", 0.0).forGetter(PhysicsMotion::sy),
            Codec.DOUBLE.optionalFieldOf("sz", 0.0).forGetter(PhysicsMotion::sz),
            WarmRoom.CODEC.listOf().optionalFieldOf("warm", List.of()).forGetter(PhysicsMotion::warm)
    ).apply(instance, PhysicsMotion::new));
    /** A structure let go at rest. */
    public static final PhysicsMotion AT_REST = new PhysicsMotion(0, 0, 0, 0, 0, 0, List.of());

    public PhysicsMotion {
        warm = List.copyOf(warm);
    }

    /** A structure let go going {@code (vx, vy, vz)} metres a second, not turning. */
    public static PhysicsMotion going(final double vx, final double vy, final double vz) {
        return new PhysicsMotion(vx, vy, vz, 0, 0, 0, List.of());
    }

    @Override
    public Step step(final StructurePose now, final Context context) {
        final MovingStructure structure = context.structure();
        if (structure == null || !(context.world() instanceof ServerLevel level)) {
            return new Step(now, this);
        }
        return structure.physicsMade().step(level, this);
    }

    @Override
    public ResourceLocation type() {
        return TYPE;
    }

    @Override
    public boolean readsWorld() {
        return true;
    }

    /** Its balloons' warm air, by the room's id, in kelvin. */
    public Map<Long, Double> warmRooms() {
        final Map<Long, Double> rooms = new HashMap<>();
        for (final WarmRoom room : warm) {
            rooms.put(room.room(), room.kelvin());
        }
        return rooms;
    }

    /**
     * The warm air of a balloon aboard.
     *
     * @param room   the room's id
     * @param kelvin how warm its air is
     */
    public record WarmRoom(long room, double kelvin) {

        public static final Codec<WarmRoom> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.LONG.fieldOf("room").forGetter(WarmRoom::room),
                Codec.DOUBLE.fieldOf("kelvin").forGetter(WarmRoom::kelvin)
        ).apply(instance, WarmRoom::new));
    }
}
