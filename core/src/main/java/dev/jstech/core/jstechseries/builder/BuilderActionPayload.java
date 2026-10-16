/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.JsCore;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A key of the Multiblock Builder in a hand that asks the server to do something with the multiblock it is aimed at.
 *
 * @param mainHand whether it is in the main hand, else the other
 * @param action   what is asked
 */
public record BuilderActionPayload(boolean mainHand, Action action) implements CustomPacketPayload {

    public static final Type<BuilderActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "builder_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuilderActionPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, BuilderActionPayload::mainHand,
                    ByteBufCodecs.fromCodec(StableCodecs.byName(Action.class)), BuilderActionPayload::action,
                    BuilderActionPayload::new);

    @Override
    public Type<BuilderActionPayload> type() {
        return TYPE;
    }

    /** What a key asks. */
    public enum Action implements IStableName {
        /** OK: build the multiblock it is aimed at, as it is set. */
        BUILD("build"),
        /** HOLD: hold the building, or carry it on. */
        HOLD("hold"),
        /** Stop building. */
        STOP("stop");

        private final String serializedName;

        Action(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
