/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.signal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.team.CoreTeams;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * What a radio is tuned to: a numbered frequency of its owner's or of its owner's team, or a pair of its own. The same
 * tuning on two players' radios is two frequencies when it is private, one when they are on one team and it is the
 * team's.
 *
 * @param band   whose frequencies it is tuned among
 * @param number the frequency's number; 0 for a pair
 * @param pair   the pair it is one end of; none unless its band is a pair's
 */
public record Tuning(Band band, int number, @Nullable UUID pair) {

    /** A new radio's tuning: its owner's own frequency 1. */
    public static final Tuning DEFAULT = new Tuning(Band.PRIVATE, Frequency.LEAST, null);
    public static final Codec<Tuning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(Band.class).fieldOf("band").forGetter(Tuning::band),
            Codec.INT.optionalFieldOf("number", 0).forGetter(Tuning::number),
            UUIDUtil.CODEC.optionalFieldOf("pair").forGetter(tuning -> Optional.ofNullable(tuning.pair()))
    ).apply(instance, (band, number, pair) -> read(band, number, pair.orElse(null))));

    public Tuning {
        if (band == Band.PAIR ? pair == null || number != 0 : pair != null || number != Frequency.held(number)) {
            throw new IllegalArgumentException("not a tuning of the " + band.serializedName() + " band: " + number
                    + (pair == null ? "" : " of the pair " + pair));
        }
    }

    /** Tuned to frequency {@code number} of {@code band}, private or the team's, held to the numbers there are. */
    public static Tuning on(final Band band, final int number) {
        if (band == Band.PAIR) {
            throw new IllegalArgumentException("a pair is tuned by the pair, not by a number");
        }
        return new Tuning(band, Frequency.held(number), null);
    }

    /** One end of the pair {@code pair}. */
    public static Tuning paired(final UUID pair) {
        return new Tuning(Band.PAIR, 0, pair);
    }

    /** One end of a pair made now, which the other ends are given ({@link #paired}). */
    public static Tuning newPair() {
        return paired(UUID.randomUUID());
    }

    /** The frequency this tuning is on, for a radio of {@code owner}, asking the teams of {@code server}. */
    public Frequency frequency(final MinecraftServer server, final UUID owner) {
        return switch (this.band) {
            case PRIVATE -> new Frequency(Band.PRIVATE, owner.toString(), this.number);
            case TEAM -> new Frequency(Band.TEAM, CoreTeams.teamOf(server, owner), this.number);
            case PAIR -> new Frequency(Band.PAIR, String.valueOf(this.pair), 0);
        };
    }

    /* A tuning as saved, made whole: a pair's without its pair is a new radio's, a number out of range is held. */
    private static Tuning read(final Band band, final int number, @Nullable final UUID pair) {
        if (band == Band.PAIR) {
            return pair == null ? DEFAULT : paired(pair);
        }
        return on(band, number);
    }
}
