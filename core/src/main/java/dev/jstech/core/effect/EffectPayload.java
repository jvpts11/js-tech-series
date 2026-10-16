/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import dev.jstech.core.JsCore;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The server firing an effect for the players near enough to see it: which, where and which way, aimed where if it
 * is a beam or an arc, in what colour and how big, following an entity or played on a block as its file anchors it.
 * Sent small: the effect's file stays on each player's game.
 *
 * @param effect the effect
 * @param at     where, or the block's corner for one played on a block
 * @param way    which way it faces
 * @param to     where a beam or an arc goes, if aimed
 * @param colour a colour laid over its own, {@code 0xAARRGGBB}; 0 for its own
 * @param scale  how big, 1 as made
 * @param follow the entity it follows, by its id; -1 for none
 * @param block  whether it is played on the block at {@code at}, where its file anchors it
 */
public record EffectPayload(ResourceLocation effect, Vec3 at, Vector3f way, Optional<Vec3> to, int colour,
                            float scale, int follow, boolean block) implements CustomPacketPayload {

    public static final Type<EffectPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "effect"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EffectPayload> STREAM_CODEC = StreamCodec.of(
            EffectPayload::write, EffectPayload::read);

    private static final int AIMED = 1;
    private static final int COLOURED = 2;
    private static final int SCALED = 4;
    private static final int FOLLOWS = 8;
    private static final int ON_BLOCK = 16;

    @Override
    public Type<EffectPayload> type() {
        return TYPE;
    }

    /* Only what differs from the plain is written, after a byte saying which. */
    private static void write(final RegistryFriendlyByteBuf buffer, final EffectPayload payload) {
        ResourceLocation.STREAM_CODEC.encode(buffer, payload.effect);
        buffer.writeDouble(payload.at.x);
        buffer.writeDouble(payload.at.y);
        buffer.writeDouble(payload.at.z);
        ByteBufCodecs.VECTOR3F.encode(buffer, payload.way);
        final int flags = (payload.to.isPresent() ? AIMED : 0) | (payload.colour != 0 ? COLOURED : 0)
                | (payload.scale != 1.0F ? SCALED : 0) | (payload.follow >= 0 ? FOLLOWS : 0)
                | (payload.block ? ON_BLOCK : 0);
        buffer.writeByte(flags);
        payload.to.ifPresent(to -> {
            buffer.writeDouble(to.x);
            buffer.writeDouble(to.y);
            buffer.writeDouble(to.z);
        });
        if (payload.colour != 0) {
            buffer.writeInt(payload.colour);
        }
        if (payload.scale != 1.0F) {
            buffer.writeFloat(payload.scale);
        }
        if (payload.follow >= 0) {
            buffer.writeVarInt(payload.follow);
        }
    }

    private static EffectPayload read(final RegistryFriendlyByteBuf buffer) {
        final ResourceLocation effect = ResourceLocation.STREAM_CODEC.decode(buffer);
        final Vec3 at = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        final Vector3f way = ByteBufCodecs.VECTOR3F.decode(buffer);
        final int flags = buffer.readByte();
        final Optional<Vec3> to = (flags & AIMED) != 0 ? Optional.of(new Vec3(buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble())) : Optional.empty();
        final int colour = (flags & COLOURED) != 0 ? buffer.readInt() : 0;
        final float scale = (flags & SCALED) != 0 ? buffer.readFloat() : 1.0F;
        final int follow = (flags & FOLLOWS) != 0 ? buffer.readVarInt() : -1;
        return new EffectPayload(effect, at, way, to, colour, scale, follow, (flags & ON_BLOCK) != 0);
    }
}
