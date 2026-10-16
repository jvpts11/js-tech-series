/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model.network;

import dev.jstech.core.JsCore;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The server telling the game of the player who ran a model command what to do: open the preview of a model, or read
 * the models again.
 *
 * @param preview whether it opens a preview; else it reads the models again
 * @param model   the model to preview, a declared one's name or a model file; the reload names none
 */
public record ModelToolPayload(boolean preview, ResourceLocation model) implements CustomPacketPayload {

    public static final Type<ModelToolPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "model_tool"));

    public static final StreamCodec<ByteBuf, ModelToolPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, ModelToolPayload::preview, ResourceLocation.STREAM_CODEC, ModelToolPayload::model,
            ModelToolPayload::new);

    public ModelToolPayload {
        Objects.requireNonNull(model, "model");
    }

    @Override
    public Type<ModelToolPayload> type() {
        return TYPE;
    }
}
