/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.place;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * A landmark: a unique, named place of a world, at a known place (Olympus Mons, the first city), which players
 * discover by coming near, as its file under {@code data/<namespace>/jstech/landmarks/} says. What stands there is
 * the world's: a landform, a structure placed at it, or nothing more than the name.
 *
 * <pre>{@code
 * { "name": "mymod.landmark.olympus_mons", "english": "Olympus Mons", "dimension": "mymod:mars",
 *   "x": 1200, "y": 140, "z": -800, "radius": 160 }
 * }</pre>
 *
 * @param name      its name's translation key
 * @param english   its name in English, for a pack that brings no language
 * @param dimension the dimension it is in
 * @param pos       where it is
 * @param radius    how near a player comes to discover it, in blocks
 * @param structure the structure that stands at it, if one does
 */
public record Landmark(String name, String english, ResourceKey<Level> dimension, BlockPos pos, int radius,
                       Optional<ResourceLocation> structure) {

    private static final int FARTHEST = 30_000_000;
    private static final int WIDEST = 4096;
    public static final Codec<Landmark> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Landmark::name),
            Codec.STRING.optionalFieldOf("english", "").forGetter(Landmark::english),
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Landmark::dimension),
            Codec.intRange(-FARTHEST, FARTHEST).fieldOf("x").forGetter(landmark -> landmark.pos().getX()),
            Codec.INT.optionalFieldOf("y", 64).forGetter(landmark -> landmark.pos().getY()),
            Codec.intRange(-FARTHEST, FARTHEST).fieldOf("z").forGetter(landmark -> landmark.pos().getZ()),
            Codec.intRange(1, WIDEST).optionalFieldOf("radius", 64).forGetter(Landmark::radius),
            ResourceLocation.CODEC.optionalFieldOf("structure").forGetter(Landmark::structure))
            .apply(instance, (name, english, dimension, x, y, z, radius, structure) -> new Landmark(name, english,
                    dimension, new BlockPos(x, y, z), radius, structure)));

    public Landmark {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(dimension, "dimension");
        pos = pos.immutable();
    }

    /** Its name, as a text to show. */
    public TextKey title() {
        return new TextKey(this.name, this.english.isEmpty() ? this.name : this.english);
    }

    /** Whether a player at {@code at} in {@code in} is near enough to discover it. */
    public boolean near(final ResourceKey<Level> in, final BlockPos at) {
        return in.equals(this.dimension) && at.distSqr(this.pos) <= (double) this.radius * this.radius;
    }
}
