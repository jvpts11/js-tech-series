/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.JsCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client, as they change: what a moving structure moving by its physics shows of itself going, its engines
 * burning and how hard, its faces glowing hot from the air they head into, and the trails it leaves where the air is
 * cold and thin enough to show one.
 *
 * @param entity   the structure's entity
 * @param engines  its engines that burn, and how hard
 * @param kelvins  how hot each face heading into the air is, in kelvin, by the way it looks: down, up, north, south,
 *                 west and east; nought for one not heated
 * @param contrail whether it leaves trails now
 * @param trails   the blocks its trails come from, places of its yard, packed
 */
public record StructureEffectsPayload(int entity, List<Engine> engines, List<Integer> kelvins, boolean contrail,
                                      List<Long> trails) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureEffectsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "structure_effects"));
    /** How many engines, and trails, are told at most. */
    public static final int MOST = 256;
    /** How finely an engine's push is told: in sixteenths. */
    public static final int STEPS = 16;
    /** Each face's heat when none is heated. */
    public static final List<Integer> NOTHING_HOT = List.of(0, 0, 0, 0, 0, 0);
    private static final int FACES = 6;

    public static final StreamCodec<RegistryFriendlyByteBuf, StructureEffectsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.entity());
                buffer.writeVarInt(payload.engines().size());
                for (final Engine engine : payload.engines()) {
                    buffer.writeLong(engine.pos());
                    buffer.writeByte(engine.strength());
                    buffer.writeByte(engine.look());
                }
                for (final int kelvin : payload.kelvins()) {
                    buffer.writeVarInt(kelvin);
                }
                buffer.writeBoolean(payload.contrail());
                buffer.writeVarInt(payload.trails().size());
                for (final long trail : payload.trails()) {
                    buffer.writeLong(trail);
                }
            },
            buffer -> {
                final int entity = buffer.readVarInt();
                final int engineCount = Math.min(buffer.readVarInt(), MOST);
                final List<Engine> engines = new ArrayList<>(engineCount);
                for (int i = 0; i < engineCount; i++) {
                    engines.add(new Engine(buffer.readLong(), buffer.readByte(), buffer.readByte()));
                }
                final List<Integer> kelvins = new ArrayList<>(FACES);
                for (int i = 0; i < FACES; i++) {
                    kelvins.add(buffer.readVarInt());
                }
                final boolean contrail = buffer.readBoolean();
                final int trailCount = Math.min(buffer.readVarInt(), MOST);
                final List<Long> trails = new ArrayList<>(trailCount);
                for (int i = 0; i < trailCount; i++) {
                    trails.add(buffer.readLong());
                }
                return new StructureEffectsPayload(entity, engines, kelvins, contrail, trails);
            });

    public StructureEffectsPayload {
        engines = List.copyOf(engines);
        kelvins = List.copyOf(kelvins);
        trails = List.copyOf(trails);
    }

    /** Whether it shows anything going: an engine burning, a face glowing or a trail. */
    public boolean any() {
        if (!engines.isEmpty() || contrail) {
            return true;
        }
        for (final int kelvin : kelvins) {
            if (kelvin > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public CustomPacketPayload.Type<StructureEffectsPayload> type() {
        return TYPE;
    }

    /**
     * An engine burning: its place in the yard, packed, how hard it pushes, in sixteenths of its full, and what it
     * looks like burning.
     *
     * @param pos      its place in the yard, packed
     * @param strength how hard it pushes, from 1 to 16
     * @param look     what it looks like: {@link #ROCKET}, {@link #JET} or {@link #PROPELLER}
     */
    public record Engine(long pos, byte strength, byte look) {

        /** A rocket's plume: flame and a long column of exhaust. */
        public static final byte ROCKET = 0;
        /** A jet's: a short blue flame, a haze of hot air. */
        public static final byte JET = 1;
        /** A propeller's: nothing burns. */
        public static final byte PROPELLER = 2;
    }
}
