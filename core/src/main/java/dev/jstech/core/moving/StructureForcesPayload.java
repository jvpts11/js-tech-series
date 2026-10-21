/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import dev.jstech.core.moving.physics.ForceSum;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Server to client, a few times a second to a player who looks at them: the forces on a moving structure, each kind
 * summed and where it acts on the whole, with its mass, its centre of mass, how fast it goes, the air round it and how
 * high above the ground it is. What the diagram of forces shows.
 *
 * @param entity   the structure's entity
 * @param mass     its whole mass, what it holds with it, in kilograms
 * @param centre   its centre of mass, in the world
 * @param velocity how fast its centre goes, in metres a second
 * @param air      the density of the air round it, in kilograms a cubic metre
 * @param ground   how high its centre is above the ground below it, in metres, up to {@link StructureReadings#LOOKS}
 * @param forces   each kind of force on it that gave something
 */
public record StructureForcesPayload(int entity, double mass, Vec3 centre, Vec3 velocity, double air, double ground,
                                     List<Force> forces) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureForcesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_forces"));
    /* The most forces told: one of each kind. */
    private static final int MOST = 16;

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureForcesPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entity());
                buffer.writeDouble(payload.mass());
                writeVec(buffer, payload.centre());
                writeVec(buffer, payload.velocity());
                buffer.writeDouble(payload.air());
                buffer.writeDouble(payload.ground());
                buffer.writeVarInt(payload.forces().size());
                for (final Force force : payload.forces()) {
                    buffer.writeByte(kindNumber(force.kind()));
                    writeVec(buffer, force.push());
                    writeVec(buffer, force.at());
                }
            },
            buffer -> {
                final int entity = buffer.readVarInt();
                final double mass = buffer.readDouble();
                final Vec3 centre = readVec(buffer);
                final Vec3 velocity = readVec(buffer);
                final double air = buffer.readDouble();
                final double ground = buffer.readDouble();
                final int count = Math.min(buffer.readVarInt(), MOST);
                final List<Force> forces = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    final ForceSum.ForceKind kind = kindOf(buffer.readByte());
                    final Vec3 push = readVec(buffer);
                    final Vec3 at = readVec(buffer);
                    if (kind != null) {
                        forces.add(new Force(kind, push, at));
                    }
                }
                return new StructureForcesPayload(entity, mass, centre, velocity, air, ground, forces);
            });

    public StructureForcesPayload {
        forces = List.copyOf(forces);
    }

    /** The sum of every force on it, in newtons. */
    public Vec3 sum() {
        Vec3 sum = Vec3.ZERO;
        for (final Force force : forces) {
            sum = sum.add(force.push());
        }
        return sum;
    }

    /** The force of {@code kind} on it, or nothing. */
    public Vec3 of(final ForceSum.ForceKind kind) {
        for (final Force force : forces) {
            if (force.kind() == kind) {
                return force.push();
            }
        }
        return Vec3.ZERO;
    }

    @Override
    public CustomPacketPayload.Type<StructureForcesPayload> type() {
        return TYPE;
    }

    private static void writeVec(final RegistryFriendlyByteBuf buffer, final Vec3 vec) {
        buffer.writeDouble(vec.x);
        buffer.writeDouble(vec.y);
        buffer.writeDouble(vec.z);
    }

    private static Vec3 readVec(final RegistryFriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    /* Each kind's number on the wire, kept as it is whatever the kinds' order. */
    private static int kindNumber(final ForceSum.ForceKind kind) {
        return switch (kind) {
            case GRAVITY -> 0;
            case THRUST -> 1;
            case LIFT -> 2;
            case DRAG -> 3;
            case BUOYANCY -> 4;
            case LEVITATION -> 5;
            case MAGNETISM -> 6;
            case WHEELS -> 7;
            case BALLOONS -> 8;
            case HELD -> 9;
        };
    }

    private static ForceSum.ForceKind kindOf(final int number) {
        for (final ForceSum.ForceKind kind : ForceSum.ForceKind.values()) {
            if (kindNumber(kind) == number) {
                return kind;
            }
        }
        return null;
    }

    /**
     * One kind of force on the structure, summed.
     *
     * @param kind its kind
     * @param push its sum, in newtons
     * @param at   where it acts on the whole, in the world
     */
    public record Force(ForceSum.ForceKind kind, Vec3 push, Vec3 at) {
    }
}
