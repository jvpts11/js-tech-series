/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * What a player sees from somewhere other than their own eyes, while their body stays where it is: a scene along a
 * camera path, the view of an entity (a robot, a drone, a probe), or the view from a point (a periscope's top, a
 * camera's lens). The server starts one ({@link CameraViews}); sneaking ends it, a scene skipped.
 */
public sealed interface CameraView {

    /** How a view is sent to a player's game: its kind, then what it holds. */
    StreamCodec<ByteBuf, CameraView> STREAM_CODEC = ByteBufCodecs.BYTE.dispatch(CameraView::kind,
            CameraView::codecOf);

    /** Kinds of view, as they are sent. */
    byte SCENE = 0;
    byte WATCH = 1;
    byte POINT = 2;

    /** Its kind, as it is sent. */
    byte kind();

    /* How a view of {@code kind} is sent. */
    private static StreamCodec<ByteBuf, ? extends CameraView> codecOf(final byte kind) {
        if (kind == SCENE) {
            return Scene.STREAM_CODEC;
        }
        return kind == WATCH ? Watch.STREAM_CODEC : Point.STREAM_CODEC;
    }

    /**
     * A scene along the camera path {@code path}, played at {@code origin}, facing {@code yaw}.
     *
     * @param path   the camera path's id
     * @param origin where its places are measured from
     * @param yaw    which way its origin faces, in the game's degrees (0 south, 90 west)
     */
    record Scene(ResourceLocation path, Vec3 origin, float yaw) implements CameraView {

        static final StreamCodec<ByteBuf, Scene> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Scene::path, CameraCodecs.VEC3, Scene::origin, ByteBufCodecs.FLOAT,
                Scene::yaw, Scene::new);

        @Override
        public byte kind() {
            return SCENE;
        }
    }

    /**
     * What the entity {@code entity} sees, from its eyes, as they turn.
     *
     * @param entity the entity's id on the player's game
     */
    record Watch(int entity) implements CameraView {

        static final StreamCodec<ByteBuf, Watch> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Watch::new, Watch::entity);

        @Override
        public byte kind() {
            return WATCH;
        }
    }

    /**
     * What is seen from {@code place}, looking {@code yaw} and {@code pitch}; when {@code turns}, the player turns the
     * view as they would turn their head, as at a periscope.
     *
     * @param place where the view is from
     * @param yaw   which way it looks first, in the game's degrees
     * @param pitch how far down it looks first, in degrees
     * @param turns whether the player turns it
     */
    record Point(Vec3 place, float yaw, float pitch, boolean turns) implements CameraView {

        static final StreamCodec<ByteBuf, Point> STREAM_CODEC = StreamCodec.composite(
                CameraCodecs.VEC3, Point::place, ByteBufCodecs.FLOAT, Point::yaw, ByteBufCodecs.FLOAT, Point::pitch,
                ByteBufCodecs.BOOL, Point::turns, Point::new);

        @Override
        public byte kind() {
            return POINT;
        }
    }
}
