/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import dev.jstech.core.JsCore;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: the view the player is to see from now on, or none to go back to their own eyes.
 *
 * @param view   the view, or none
 * @param number which of the views the server started for the player this is, said back when it ends
 */
public record CameraViewPayload(Optional<CameraView> view, int number) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CameraViewPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "camera_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraViewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(CameraView.STREAM_CODEC).cast(), CameraViewPayload::view,
            ByteBufCodecs.VAR_INT.cast(), CameraViewPayload::number,
            CameraViewPayload::new);

    @Override
    public CustomPacketPayload.Type<CameraViewPayload> type() {
        return TYPE;
    }
}
