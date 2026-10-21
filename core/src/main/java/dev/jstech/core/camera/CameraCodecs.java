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
import net.minecraft.world.phys.Vec3;

/* How the camera kit sends a place: three doubles, the game's own precision for where things are. */
final class CameraCodecs {

    static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.composite(ByteBufCodecs.DOUBLE, Vec3::x,
            ByteBufCodecs.DOUBLE, Vec3::y, ByteBufCodecs.DOUBLE, Vec3::z, Vec3::new);

    private CameraCodecs() {
    }
}
