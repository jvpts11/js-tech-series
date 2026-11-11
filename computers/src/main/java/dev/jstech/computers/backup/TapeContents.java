/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a tape holds, carried on the tape itself: the label written on it, the backups written along it one after
 * another, and where its head stopped, which is where the next wind starts from. The backups' contents are kept in the
 * {@link BackupArchive}; the tape knows where on it each one lies and what it weighs, which is all a library needs to
 * wind to one.
 *
 * @param label what the player wrote on it, or empty for a blank one
 * @param spots the backups on it, in the order they were written
 * @param head  where its head is, in items from its start
 */
public record TapeContents(String label, List<Spot> spots, long head) {

    /** The longest label a tape takes. */
    public static final int MOST_LABEL = 24;
    /** The most backups one tape keeps along it. */
    public static final int MOST_SPOTS = 64;
    /** A tape nothing was ever written on. */
    public static final TapeContents BLANK = new TapeContents("", List.of(), 0L);

    public static final Codec<TapeContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("label", "").forGetter(TapeContents::label),
            Spot.CODEC.listOf().optionalFieldOf("spots", List.of()).forGetter(TapeContents::spots),
            Codec.LONG.optionalFieldOf("head", 0L).forGetter(TapeContents::head)
    ).apply(instance, TapeContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, TapeContents> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MOST_LABEL), TapeContents::label,
            Spot.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_SPOTS)), TapeContents::spots,
            ByteBufCodecs.VAR_LONG, TapeContents::head,
            TapeContents::new);

    public TapeContents {
        label = label.length() > MOST_LABEL ? label.substring(0, MOST_LABEL) : label;
        spots = List.copyOf(spots);
    }

    /** Where the written part of the tape ends, in items from its start: where the next backup is written. */
    public long used() {
        return spots.isEmpty() ? 0L : spots.getLast().start() + spots.getLast().size();
    }

    /** The tape with {@code spot} written after the last backup on it, its head left at its end. */
    public TapeContents written(final Spot spot) {
        final List<Spot> more = new ArrayList<>(spots);
        more.add(spot);
        return new TapeContents(label, more, spot.start() + spot.size());
    }

    /** The tape with its head at {@code at}, as a wind leaves it. */
    public TapeContents wound(final long at) {
        return new TapeContents(label, spots, at);
    }

    /** The tape with a new label on it. */
    public TapeContents labelled(final String name) {
        return new TapeContents(name, spots, head);
    }

    /** The tape wiped clean, its label kept and its head back at its start. */
    public TapeContents erased() {
        return new TapeContents(label, List.of(), 0L);
    }

    /** The spot of {@code backup} on this tape, or null when it is not on it. */
    public Spot spotOf(final UUID backup) {
        for (final Spot spot : spots) {
            if (spot.backup().equals(backup)) {
                return spot;
            }
        }
        return null;
    }

    /**
     * One backup written along the tape.
     *
     * @param backup the backup in the archive
     * @param start  where it begins, in items from the tape's start
     * @param size   how much of the tape it takes, in items as the tape's era weighs them
     * @param job    the job that made it
     * @param madeAt the world's clock when it was made
     * @param items  how many of the network's items it keeps
     */
    public record Spot(UUID backup, long start, long size, String job, long madeAt, long items) {

        /** The longest job name a spot carries. */
        public static final int MOST_JOB = 32;

        public static final Codec<Spot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUIDUtil.CODEC.fieldOf("backup").forGetter(Spot::backup),
                Codec.LONG.fieldOf("start").forGetter(Spot::start),
                Codec.LONG.fieldOf("size").forGetter(Spot::size),
                Codec.STRING.optionalFieldOf("job", "").forGetter(Spot::job),
                Codec.LONG.optionalFieldOf("made", 0L).forGetter(Spot::madeAt),
                Codec.LONG.optionalFieldOf("items", 0L).forGetter(Spot::items)
        ).apply(instance, Spot::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Spot> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, Spot::backup,
                ByteBufCodecs.VAR_LONG, Spot::start,
                ByteBufCodecs.VAR_LONG, Spot::size,
                ByteBufCodecs.stringUtf8(MOST_JOB), Spot::job,
                ByteBufCodecs.VAR_LONG, Spot::madeAt,
                ByteBufCodecs.VAR_LONG, Spot::items,
                Spot::new);

        public Spot {
            job = job.length() > MOST_JOB ? job.substring(0, MOST_JOB) : job;
        }
    }
}
