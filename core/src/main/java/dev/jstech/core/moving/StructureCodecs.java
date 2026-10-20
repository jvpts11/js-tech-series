/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How the kit's pure values are written to a save and sent: kept apart from them, so the values stay free of the game
 * and are tested on their own.
 */
public final class StructureCodecs {

    /** A pose in a save: its place, and its rotation as a quaternion. */
    public static final Codec<StructurePose> POSE = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("x").forGetter(StructurePose::x),
            Codec.DOUBLE.fieldOf("y").forGetter(StructurePose::y),
            Codec.DOUBLE.fieldOf("z").forGetter(StructurePose::z),
            Codec.DOUBLE.fieldOf("qx").forGetter(StructurePose::qx),
            Codec.DOUBLE.fieldOf("qy").forGetter(StructurePose::qy),
            Codec.DOUBLE.fieldOf("qz").forGetter(StructurePose::qz),
            Codec.DOUBLE.fieldOf("qw").forGetter(StructurePose::qw)
    ).apply(instance, StructurePose::new));

    /** A pose sent to a player: the place to the full, the rotation in floats, which is more than a game can show. */
    public static final StreamCodec<ByteBuf, StructurePose> POSE_STREAM = StreamCodec.of(
            (buffer, pose) -> {
                buffer.writeDouble(pose.x());
                buffer.writeDouble(pose.y());
                buffer.writeDouble(pose.z());
                buffer.writeFloat((float) pose.qx());
                buffer.writeFloat((float) pose.qy());
                buffer.writeFloat((float) pose.qz());
                buffer.writeFloat((float) pose.qw());
            },
            buffer -> new StructurePose(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat()));

    /** A small direction or speed sent to a player, three floats. */
    public static final StreamCodec<ByteBuf, float[]> VECTOR_STREAM = StreamCodec.of(
            (buffer, vector) -> {
                buffer.writeFloat(vector[0]);
                buffer.writeFloat(vector[1]);
                buffer.writeFloat(vector[2]);
            },
            buffer -> new float[] {buffer.readFloat(), buffer.readFloat(), buffer.readFloat()});

    /** A place in the yard sent to a player, three doubles: the yard is far enough out that floats would not do. */
    public static final StreamCodec<ByteBuf, double[]> PLACE_STREAM = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, place -> place[0],
            ByteBufCodecs.DOUBLE, place -> place[1],
            ByteBufCodecs.DOUBLE, place -> place[2],
            (x, y, z) -> new double[] {x, y, z});

    private StructureCodecs() {
    }
}
