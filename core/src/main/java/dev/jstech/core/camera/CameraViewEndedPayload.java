/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the view the server started has ended on the player's game, and how.
 *
 * @param ending how it ended, by its place among {@link CameraViewEvent.Ending}'s, as it is sent
 */
public record CameraViewEndedPayload(int ending) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CameraViewEndedPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "camera_view_ended"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraViewEndedPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.<RegistryFriendlyByteBuf>cast().map(CameraViewEndedPayload::new,
                    CameraViewEndedPayload::ending);

    /** The payload saying the view ended as {@code ending} says. */
    public static CameraViewEndedPayload of(final CameraViewEvent.Ending ending) {
        return new CameraViewEndedPayload(ending.code());
    }

    @Override
    public CustomPacketPayload.Type<CameraViewEndedPayload> type() {
        return TYPE;
    }
}
