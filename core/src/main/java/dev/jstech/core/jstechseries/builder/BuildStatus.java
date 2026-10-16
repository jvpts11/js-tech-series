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
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * How a Multiblock Builder's building goes, as the server tells the player holding it: what it is doing, how many
 * blocks it has placed, and when it stopped, why.
 *
 * @param controller the controller of the multiblock it builds
 * @param phase      what it is doing
 * @param placed     how many blocks it has placed since it began
 * @param reason     why it stopped, or {@link Reason#NONE}
 * @param block      the block the reason names (the one it ran out of), by id, or empty
 */
public record BuildStatus(BlockPos controller, Phase phase, int placed, Reason reason, String block)
        implements CustomPacketPayload {

    /** Nothing being built. */
    public static final BuildStatus IDLE = new BuildStatus(BlockPos.ZERO, Phase.IDLE, 0, Reason.NONE, "");
    public static final Type<BuildStatus> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "build_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuildStatus> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, BuildStatus::controller,
            ByteBufCodecs.fromCodec(StableCodecs.byName(Phase.class)), BuildStatus::phase,
            ByteBufCodecs.VAR_INT, BuildStatus::placed,
            ByteBufCodecs.fromCodec(StableCodecs.byName(Reason.class)), BuildStatus::reason,
            ByteBufCodecs.STRING_UTF8, BuildStatus::block,
            BuildStatus::new);

    @Override
    public Type<BuildStatus> type() {
        return TYPE;
    }

    /** What a builder is doing. */
    public enum Phase implements IStableName {
        /** Nothing. */
        IDLE("idle"),
        /** Placing block after block. */
        BUILDING("building"),
        /** Held by its HOLD key, to carry on. */
        HELD("held"),
        /** Done: nothing is missing. */
        BUILT("built"),
        /** Stopped before it was done, for its reason. */
        STOPPED("stopped");

        private final String serializedName;

        Phase(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** Why a builder stopped before it was done. */
    public enum Reason implements IStableName {
        /** It did not stop, or was stopped by the player. */
        NONE("none", BuilderTexts.STOPPED),
        /** The player may not build multiblocks. */
        NOT_ALLOWED("not_allowed", BuilderTexts.NOT_ALLOWED),
        /** The controller forms no multiblock the builder knows, or is gone. */
        UNKNOWN("unknown", BuilderTexts.UNKNOWN),
        /** The player walked too far from the controller. */
        TOO_FAR("too_far", BuilderTexts.TOO_FAR),
        /** A place is protected from the player. */
        PROTECTED("protected", BuilderTexts.PROTECTED),
        /** Something stands where a block goes. */
        BLOCKED("blocked", BuilderTexts.BLOCKED),
        /** The source has none of a block left. */
        RAN_OUT("ran_out", BuilderTexts.RAN_OUT),
        /** Nothing is missing, but places hold other blocks, which it does not break. */
        WRONG_LEFT("wrong_left", BuilderTexts.WRONG_LEFT);

        private final String serializedName;
        private final TextKey says;

        Reason(final String serializedName, final TextKey says) {
            this.serializedName = serializedName;
            this.says = says;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** What the builder's display says of it. */
        public TextKey says() {
            return this.says;
        }
    }
}
