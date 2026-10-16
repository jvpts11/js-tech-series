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
 * The server telling the players who see a model what one of its controllers does: plays an animation, plays it
 * again from its start, stops, or plays at another speed.
 *
 * @param target     what the model is drawn for
 * @param controller the controller, by name
 * @param action     what it does
 * @param animation  the animation it plays, by name; empty for a stop or a change of speed
 * @param transition how long it blends to it, in seconds
 * @param speed      how fast it plays, for a change of speed
 */
public record ModelAnimationPayload(ModelTarget target, String controller, Action action, String animation,
                                    float transition, float speed) implements CustomPacketPayload {

    /** The longest name of a controller or an animation a payload carries. */
    public static final int MAX_NAME = 256;

    public static final Type<ModelAnimationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "model_animation"));

    public static final StreamCodec<ByteBuf, ModelAnimationPayload> STREAM_CODEC = StreamCodec.of(
            ModelAnimationPayload::write, ModelAnimationPayload::read);

    private static final StreamCodec<ByteBuf, String> NAME = ByteBufCodecs.stringUtf8(MAX_NAME);

    public ModelAnimationPayload {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(controller, "controller");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(animation, "animation");
        // A name longer than the wire carries would fail as the payload is written, far from what made it.
        if (controller.length() > MAX_NAME || animation.length() > MAX_NAME) {
            throw new IllegalArgumentException("a controller's or an animation's name is at most " + MAX_NAME
                    + " characters");
        }
    }

    @Override
    public Type<ModelAnimationPayload> type() {
        return TYPE;
    }

    private static void write(final ByteBuf buffer, final ModelAnimationPayload payload) {
        ModelTarget.STREAM_CODEC.encode(buffer, payload.target);
        NAME.encode(buffer, payload.controller);
        buffer.writeByte(payload.action == Action.PLAY ? 0 : payload.action == Action.REPLAY ? 1
                : payload.action == Action.STOP ? 2 : 3);
        NAME.encode(buffer, payload.animation);
        buffer.writeFloat(payload.transition);
        buffer.writeFloat(payload.speed);
    }

    private static ModelAnimationPayload read(final ByteBuf buffer) {
        final ModelTarget target = ModelTarget.STREAM_CODEC.decode(buffer);
        final String controller = NAME.decode(buffer);
        final Action action = switch (buffer.readByte()) {
            case 0 -> Action.PLAY;
            case 1 -> Action.REPLAY;
            case 2 -> Action.STOP;
            default -> Action.SPEED;
        };
        return new ModelAnimationPayload(target, controller, action, NAME.decode(buffer), buffer.readFloat(),
                buffer.readFloat());
    }

    /** What a controller does. */
    public enum Action {
        /** Plays an animation, going on with it if it plays already. */
        PLAY,
        /** Plays an animation from its start. */
        REPLAY,
        /** Stops. */
        STOP,
        /** Plays at another speed. */
        SPEED
    }
}
