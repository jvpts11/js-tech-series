/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.camera;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.model.Easing;
import java.util.List;

/**
 * How a camera path is written in a resource pack's file: its keyframes, each with its time, its place as three
 * numbers, its turn, its roll, its field of view and its easing, by Blockbench's names; and whether it hides the HUD,
 * which it does unless said. Keyframes out of order read as an error, never as a path.
 */
public final class CameraPathCodecs {

    private static final Codec<List<Double>> POSITION = Codec.DOUBLE.listOf().comapFlatMap(
            position -> position.size() == 3 ? DataResult.success(position)
                    : DataResult.error(() -> "a keyframe's position is three numbers, x, y and z"),
            position -> position);
    private static final Codec<Easing> EASING = Codec.STRING.xmap(Easing::byName, Easing::serializedName);

    /** How a keyframe is written. */
    public static final Codec<CameraPath.Keyframe> KEYFRAME = RecordCodecBuilder.create(keyframe -> keyframe.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("time").forGetter(CameraPath.Keyframe::time),
            POSITION.fieldOf("position").forGetter(CameraPath.Keyframe::position),
            Codec.FLOAT.optionalFieldOf("yaw", 0.0F).forGetter(CameraPath.Keyframe::yaw),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(CameraPath.Keyframe::pitch),
            Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(CameraPath.Keyframe::roll),
            Codec.doubleRange(0.0, CameraPath.Keyframe.MOST_FOV).optionalFieldOf("fov", 0.0)
                    .forGetter(CameraPath.Keyframe::fov),
            EASING.optionalFieldOf("easing", Easing.LINEAR).forGetter(CameraPath.Keyframe::easing))
            .apply(keyframe, (time, position, yaw, pitch, roll, fov, easing) -> new CameraPath.Keyframe(time,
                    position.get(0), position.get(1), position.get(2), yaw, pitch, roll, fov, easing)));
    /** How a path is written. */
    public static final Codec<CameraPath> PATH = RecordCodecBuilder.<Written>create(path -> path.group(
            KEYFRAME.listOf().fieldOf("keyframes").forGetter(Written::keyframes),
            Codec.BOOL.optionalFieldOf("hide_hud", true).forGetter(Written::hideHud))
            .apply(path, Written::new)).comapFlatMap(CameraPathCodecs::checked,
                    path -> new Written(path.keyframes(), path.hideHud()));

    private CameraPathCodecs() {
    }

    private static DataResult<CameraPath> checked(final Written written) {
        if (!CameraPath.ordered(written.keyframes())) {
            return DataResult.error(() -> "a camera path has two keyframes or more, each later than the one before");
        }
        return DataResult.success(new CameraPath(written.keyframes(), written.hideHud()));
    }

    /* A path as it is written, before its keyframes are checked. */
    private record Written(List<CameraPath.Keyframe> keyframes, boolean hideHud) {
    }
}
