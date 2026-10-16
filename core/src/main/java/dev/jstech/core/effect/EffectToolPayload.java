/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.effect;

import dev.jstech.core.JsCore;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The server telling the game of the player who ran an effect command what to do: open the effects editor on an
 * effect at the block they look at, open the card of a new effect there, or play an effect there.
 *
 * @param tool   what to do
 * @param effect the effect; a new one names none
 */
public record EffectToolPayload(Tool tool, ResourceLocation effect) implements CustomPacketPayload {

    public static final Type<EffectToolPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "effect_tool"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EffectToolPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(StableCodecs.byName(Tool.class)), EffectToolPayload::tool,
            ResourceLocation.STREAM_CODEC,
            EffectToolPayload::effect, EffectToolPayload::new);

    @Override
    public Type<EffectToolPayload> type() {
        return TYPE;
    }

    /** What an effect command does. */
    public enum Tool implements IStableName {
        EDIT("edit"),
        NEW("new"),
        PLAY("play");

        private final String serializedName;

        Tool(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
