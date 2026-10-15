/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.EndRouter;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.inventory.IFilterSubject;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.ItemFilters;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How one end of a wire meets the block it plugs into, for a line that carries items, liquids or gases: its mode, its
 * priority among the ends a share could go to, the most it gives or takes a tick, its channel, and its filter, whose
 * rules' amounts say how much to keep in the block. Every setting has a default that leaves a mod's own design as it
 * is: a pipe from a machine's output to another's input just works.
 *
 * @param mode     how it meets the block
 * @param priority its priority: the highest fills first
 * @param rate     the most it gives or takes a tick; 0 for no limit but the line's
 * @param channel  its channel, from 0 to 15
 * @param filter   what it lets through, and how much of each to keep in the block
 */
public record CableEnd(EndMode mode, int priority, long rate, int channel, ItemFilter filter) {

    /** An end as it is laid: normal, every priority the same, no limit, the first channel, no filter. */
    public static final CableEnd DEFAULT = new CableEnd(EndMode.NORMAL, 0, 0L, 0, ItemFilter.EVERYTHING);
    /** The priorities an end may have. */
    public static final int LOWEST = -99;
    public static final int HIGHEST = 99;

    /** How an end is saved. */
    public static final Codec<CableEnd> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(EndMode.class).optionalFieldOf("mode", EndMode.NORMAL).forGetter(CableEnd::mode),
            Codec.intRange(LOWEST, HIGHEST).optionalFieldOf("priority", 0).forGetter(CableEnd::priority),
            Codec.LONG.validate(rate -> rate >= 0 ? DataResult.success(rate)
                    : DataResult.error(() -> "an end moves no less than nothing: " + rate))
                    .optionalFieldOf("rate", 0L).forGetter(CableEnd::rate),
            Codec.intRange(0, EndRouter.CHANNELS - 1).optionalFieldOf("channel", 0).forGetter(CableEnd::channel),
            ItemFilters.CODEC.optionalFieldOf("filter", ItemFilter.EVERYTHING).forGetter(CableEnd::filter)
    ).apply(instance, CableEnd::new));
    /** How an end is sent. */
    public static final StreamCodec<RegistryFriendlyByteBuf, CableEnd> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);

    public CableEnd {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(filter, "filter");
        if (priority < LOWEST || priority > HIGHEST) {
            throw new IllegalArgumentException("a priority runs from " + LOWEST + " to " + HIGHEST + ", not "
                    + priority);
        }
        if (rate < 0) {
            throw new IllegalArgumentException("an end moves no less than nothing: " + rate);
        }
        if (channel < 0 || channel >= EndRouter.CHANNELS) {
            throw new IllegalArgumentException("a line has channels 0 to " + (EndRouter.CHANNELS - 1) + ", not "
                    + channel);
        }
    }

    /** Whether it is set as it was laid. */
    public boolean isDefault() {
        return equals(DEFAULT);
    }

    /** The same end in {@code to}. */
    public CableEnd withMode(final EndMode to) {
        return new CableEnd(to, this.priority, this.rate, this.channel, this.filter);
    }

    /** The same end with the priority {@code to}. */
    public CableEnd withPriority(final int to) {
        return new CableEnd(this.mode, to, this.rate, this.channel, this.filter);
    }

    /** The same end moving at most {@code to} a tick. */
    public CableEnd withRate(final long to) {
        return new CableEnd(this.mode, this.priority, to, this.channel, this.filter);
    }

    /** The same end on the channel {@code to}. */
    public CableEnd withChannel(final int to) {
        return new CableEnd(this.mode, this.priority, this.rate, to, this.filter);
    }

    /** The same end with the filter {@code to}. */
    public CableEnd withFilter(final ItemFilter to) {
        return new CableEnd(this.mode, this.priority, this.rate, this.channel, to);
    }

    /** How much of {@code subject} it keeps in its block: the amount of the rule that names it, 0 for no limit. */
    public long keep(final IFilterSubject subject) {
        return this.filter.mode() == ItemFilter.Mode.ONLY ? this.filter.amountFor(subject) : 0L;
    }
}
