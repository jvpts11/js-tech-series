/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: how warm the player's core is and why, for the HUD and the body's screen. Sent once a second, when
 * anything changed.
 *
 * @param coreKelvin  the core's temperature
 * @param perMinute   how fast it changes, in kelvin a minute
 * @param airKelvin   the air's temperature where the player is
 * @param wind        the wind on them, in metres a second
 * @param sun         the sunlight on them, in watts a square metre
 * @param made        what their body makes, in watts
 * @param lost        what it loses, less what the place gives it, in watts
 * @param wet         whether what they wear is wet
 * @param letsOut     how much heat what they wear lets out, in W/(m²K); infinite for nothing worn
 * @param windproof   the share of the body whose clothes keep the wind out
 * @param waterproof  the share of the body whose clothes keep water out
 * @param acclimation how far they are used to a climate, in kelvin: below none to the cold, above to the heat
 */
public record BodyTemperaturePayload(float coreKelvin, float perMinute, float airKelvin, float wind, float sun,
                                     float made, float lost, boolean wet, float letsOut, float windproof,
                                     float waterproof, float acclimation) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BodyTemperaturePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "body_temperature"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BodyTemperaturePayload> STREAM_CODEC = StreamCodec.of(
            (buf, told) -> {
                buf.writeFloat(told.coreKelvin);
                buf.writeFloat(told.perMinute);
                buf.writeFloat(told.airKelvin);
                buf.writeFloat(told.wind);
                buf.writeFloat(told.sun);
                buf.writeFloat(told.made);
                buf.writeFloat(told.lost);
                buf.writeBoolean(told.wet);
                buf.writeFloat(told.letsOut);
                buf.writeFloat(told.windproof);
                buf.writeFloat(told.waterproof);
                buf.writeFloat(told.acclimation);
            },
            buf -> new BodyTemperaturePayload(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readBoolean(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat()));
    /** What a player's game holds before the server has said anything: a normal core, nothing known of the place. */
    public static final BodyTemperaturePayload NORMAL = new BodyTemperaturePayload((float) BodyHeat.NORMAL, 0,
            (float) BodyHeat.NORMAL, 0, 0, 0, 0, false, Float.POSITIVE_INFINITY, 0, 0, 0);

    @Override
    public CustomPacketPayload.Type<BodyTemperaturePayload> type() {
        return TYPE;
    }
}
