/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Where a stage of a rocket is set to go once it comes loose, as its flight computer marks it: a fall zone on the map,
 * best at sea, or a landing platform it comes back to and lands on, on land or afloat. A stage with neither set falls
 * anywhere in the world already made.
 */
public sealed interface StageFate permits StageFate.FallZone, StageFate.Platform {

    Codec<StageFate> CODEC = Codec.STRING.validate(kind -> FallZone.KIND.equals(kind) || Platform.KIND.equals(kind)
            ? DataResult.success(kind) : DataResult.error(() -> "no stage fate is called " + kind))
            .dispatch("kind", StageFate::kind, kind -> FallZone.KIND.equals(kind) ? FallZone.CODEC : Platform.CODEC);

    /** Its kind, as it is written. */
    String kind();

    /**
     * A circle of the map a stage falls into.
     *
     * @param dimension the world it is in
     * @param centre    its middle
     * @param radius    how far from its middle it reaches, in blocks
     */
    record FallZone(ResourceKey<Level> dimension, BlockPos centre, int radius) implements StageFate {

        public static final String KIND = "fall_zone";
        /** The widest a fall zone is drawn, in blocks. */
        public static final int MOST_RADIUS = 512;
        static final MapCodec<FallZone> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(FallZone::dimension),
                BlockPos.CODEC.fieldOf("centre").forGetter(FallZone::centre),
                Codec.intRange(1, MOST_RADIUS).fieldOf("radius").forGetter(FallZone::radius)
        ).apply(instance, FallZone::new));

        public FallZone {
            radius = Math.clamp(radius, 1, MOST_RADIUS);
        }

        @Override
        public String kind() {
            return KIND;
        }
    }

    /**
     * A landing platform a stage that can come back lands on.
     *
     * @param platform the platform's id
     */
    record Platform(UUID platform) implements StageFate {

        public static final String KIND = "platform";
        static final MapCodec<Platform> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                UUIDUtil.STRING_CODEC.fieldOf("platform").forGetter(Platform::platform)
        ).apply(instance, Platform::new));

        @Override
        public String kind() {
            return KIND;
        }
    }
}
