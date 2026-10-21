/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * How a tool heats as it works and cools as it rests: each use adds its heat, and it gives it back to the air at a
 * steady rate. One that reaches its most overheats and does no more than a bare hand until it has cooled down to
 * nothing, as a drill run too long or a railgun fired too fast. The heat is reckoned from the world's time when it is
 * asked for, so a tool lying in a chest cools there with nothing counted each tick.
 *
 * @param most          the heat it overheats at; above nothing
 * @param perUse        the heat a use adds
 * @param coolPerSecond the heat it gives back each second
 */
public record ToolHeat(double most, double perUse, double coolPerSecond) {

    public ToolHeat {
        if (!(most > 0) || perUse < 0 || coolPerSecond < 0) {
            throw new IllegalArgumentException("a tool that heats overheats at some heat, and neither heats nor cools "
                    + "by less than nothing");
        }
    }

    /** How hot {@code stack} is now, from nothing to {@link #most()}. */
    public double heat(final Level level, final ItemStack stack) {
        final Heated heated = stack.get(Tools.HEAT.get());
        return heated == null ? 0.0 : heated.at(level.getGameTime(), this.coolPerSecond);
    }

    /** Whether {@code stack} has overheated and not yet cooled down to nothing. */
    public boolean overheated(final Level level, final ItemStack stack) {
        final Heated heated = stack.get(Tools.HEAT.get());
        return heated != null && heated.overheated() && heated.at(level.getGameTime(), this.coolPerSecond) > 0;
    }

    /** Adds a use's heat to {@code stack}; it overheats when that brings it to its most. */
    public void use(final Level level, final ItemStack stack) {
        final long now = level.getGameTime();
        final double heat = Math.min(this.most, heat(level, stack) + this.perUse);
        stack.set(Tools.HEAT.get(), new Heated(heat, now, overheated(level, stack) || heat >= this.most));
    }

    /**
     * The heat a tool held when it last changed, the world's time then, and whether it had overheated: what the tool
     * keeps, from which its heat now is reckoned.
     *
     * @param heat       its heat then
     * @param since      the world's time then, in ticks
     * @param overheated whether it had overheated
     */
    public record Heated(double heat, long since, boolean overheated) {

        public static final Codec<Heated> CODEC = RecordCodecBuilder.create(heated -> heated.group(
                Codec.DOUBLE.fieldOf("heat").forGetter(Heated::heat),
                Codec.LONG.fieldOf("since").forGetter(Heated::since),
                Codec.BOOL.optionalFieldOf("overheated", false).forGetter(Heated::overheated))
                .apply(heated, Heated::new));
        public static final StreamCodec<ByteBuf, Heated> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Heated::heat, ByteBufCodecs.VAR_LONG, Heated::since, ByteBufCodecs.BOOL,
                Heated::overheated, Heated::new);

        private static final double TICKS_PER_SECOND = 20.0;

        /** Its heat at the world's time {@code now}, cooling {@code coolPerSecond} a second since. */
        public double at(final long now, final double coolPerSecond) {
            return Math.max(0.0, this.heat - coolPerSecond * Math.max(0L, now - this.since) / TICKS_PER_SECOND);
        }
    }
}
