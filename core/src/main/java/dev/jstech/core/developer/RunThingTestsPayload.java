/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer;

import dev.jstech.core.JsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The studio's "Run its tests": the player asks the server to run, near them, the GameTests that name a thing.
 *
 * @param thing the thing's id
 */
public record RunThingTestsPayload(ResourceLocation thing) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RunThingTestsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "run_thing_tests"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RunThingTestsPayload> STREAM_CODEC =
            StreamCodec.composite(ResourceLocation.STREAM_CODEC, RunThingTestsPayload::thing,
                    RunThingTestsPayload::new);

    @Override
    public CustomPacketPayload.Type<RunThingTestsPayload> type() {
        return TYPE;
    }
}
