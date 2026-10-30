/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.flight;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.space.rocket.StageFate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Where a stage of the rocket a flight computer keeps is set to go, as its STG page sets it before launch: a fall zone
 * round where the rocket stands at launch, so many blocks round, or a landing platform to come back to. A stage set to
 * neither falls anywhere the game has land loaded round a player.
 *
 * @param radius   the fall zone's radius, in blocks, for a stage set to fall in one
 * @param platform the platform it comes back to, for a stage set to land
 */
public record StageChoice(int radius, Optional<UUID> platform) {

    public static final Codec<StageChoice> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("radius", 0).forGetter(StageChoice::radius),
            UUIDUtil.STRING_CODEC.optionalFieldOf("platform").forGetter(StageChoice::platform)
    ).apply(instance, StageChoice::new));
    /** The radii a fall zone steps through, in blocks, and the one a zone is set with. */
    public static final List<Integer> RADII = List.of(64, 128, 250, 500);
    public static final int FIRST_RADIUS = 250;

    /** A fall zone {@code radius} blocks round where the rocket stands at launch. */
    public static StageChoice zone(final int radius) {
        return new StageChoice(radius, Optional.empty());
    }

    /** The platform {@code platform} to come back to. */
    public static StageChoice landing(final UUID platform) {
        return new StageChoice(0, Optional.of(platform));
    }

    /** Whether the stage comes back to a platform. */
    public boolean lands() {
        return this.platform.isPresent();
    }

    /** A fall zone of the next of {@link #RADII}, the smallest after the largest. */
    public StageChoice nextRadius() {
        final int at = RADII.indexOf(this.radius);
        return zone(RADII.get((at + 1) % RADII.size()));
    }

    /** As its rocket keeps it once it flies from {@code dimension}, standing at {@code at}. */
    public StageFate fate(final ResourceKey<Level> dimension, final BlockPos at) {
        return this.platform.<StageFate>map(StageFate.Platform::new)
                .orElseGet(() -> new StageFate.FallZone(dimension, at.immutable(), this.radius));
    }
}
