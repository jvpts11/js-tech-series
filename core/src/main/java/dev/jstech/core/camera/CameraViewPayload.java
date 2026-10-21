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
 * @param view the view, or none
 */
public record CameraViewPayload(Optional<CameraView> view) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CameraViewPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "camera_view"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CameraViewPayload> STREAM_CODEC =
            ByteBufCodecs.optional(CameraView.STREAM_CODEC).<RegistryFriendlyByteBuf>cast()
                    .map(CameraViewPayload::new, CameraViewPayload::view);

    @Override
    public CustomPacketPayload.Type<CameraViewPayload> type() {
        return TYPE;
    }
}
