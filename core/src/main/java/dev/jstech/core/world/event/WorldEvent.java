/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.world.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * One happening of a world event, planned or under way, as its dimension keeps it.
 *
 * @param type     its kind's id
 * @param startsAt the game time it begins at
 * @param endsAt   the game time it ends at
 * @param centre   the place it happens round, for a kind that reaches only so far
 * @param warned   whether those carrying its instrument were warned
 * @param started  whether it has begun
 */
public record WorldEvent(ResourceLocation type, long startsAt, long endsAt, Optional<BlockPos> centre, boolean warned,
                         boolean started) {

    public static final Codec<WorldEvent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("type").forGetter(WorldEvent::type),
            Codec.LONG.fieldOf("starts_at").forGetter(WorldEvent::startsAt),
            Codec.LONG.fieldOf("ends_at").forGetter(WorldEvent::endsAt),
            BlockPos.CODEC.optionalFieldOf("centre").forGetter(WorldEvent::centre),
            Codec.BOOL.optionalFieldOf("warned", false).forGetter(WorldEvent::warned),
            Codec.BOOL.optionalFieldOf("started", false).forGetter(WorldEvent::started))
            .apply(instance, WorldEvent::new));

    /** Whether it is under way at {@code gameTime}. */
    public boolean activeAt(final long gameTime) {
        return gameTime >= this.startsAt && gameTime < this.endsAt;
    }

    /** Whether it reaches {@code pos}, reaching {@code radius} blocks round its place, or the whole dimension. */
    public boolean covers(final BlockPos pos, final int radius) {
        return this.centre.isEmpty() || this.centre.get().distSqr(pos) <= (double) radius * radius;
    }

    WorldEvent warn() {
        return new WorldEvent(this.type, this.startsAt, this.endsAt, this.centre, true, this.started);
    }

    WorldEvent start() {
        return new WorldEvent(this.type, this.startsAt, this.endsAt, this.centre, true, true);
    }

    WorldEvent endingAt(final long time) {
        return new WorldEvent(this.type, this.startsAt, time, this.centre, this.warned, this.started);
    }
}
