/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.id.StableNames;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a machine did over its last ten minutes, as its screen's page of statistics shows it, carried only to whoever
 * has the screen open ({@link MachineMenu#history()}).
 *
 * @param made              what it made a minute, fifty readings twelve seconds apart, oldest first
 * @param power             what it drew a tick on the whole between the same readings
 * @param efficiency        how much of the last minute it worked, in percent
 * @param workedTicks       how many ticks of the last minute it worked
 * @param stoppedBy         what last stopped it in these ten minutes, by its status's name; empty when nothing did,
 *                          or only its player, by its switch or by redstone
 * @param stoppedSecondsAgo how many seconds ago that was
 */
public record MachineHistory(List<Integer> made, List<Long> power, int efficiency, int workedTicks,
                             String stoppedBy, int stoppedSecondsAgo) {

    /** Nothing yet: a machine just loaded, or the screen before the server has written it. */
    public static final MachineHistory NONE = new MachineHistory(List.of(), List.of(), 0, 0, "", 0);
    public static final StreamCodec<ByteBuf, MachineHistory> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), MachineHistory::made,
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), MachineHistory::power,
            ByteBufCodecs.VAR_INT, MachineHistory::efficiency,
            ByteBufCodecs.VAR_INT, MachineHistory::workedTicks,
            ByteBufCodecs.STRING_UTF8, MachineHistory::stoppedBy,
            ByteBufCodecs.VAR_INT, MachineHistory::stoppedSecondsAgo,
            MachineHistory::new);
    /** How many readings the page holds, and how far apart they are, in seconds. */
    public static final int READINGS = MachineStatistics.SAMPLES;
    public static final int READING_SECONDS = MachineStatistics.SAMPLE_SECONDS;
    private static final StableNames<MachineStatus> STATUSES = StableNames.of(MachineStatus.class);

    public MachineHistory {
        made = List.copyOf(made);
        power = List.copyOf(power);
    }

    /** The most it made a minute at any reading; nothing before its first. */
    public int mostMade() {
        int most = 0;
        for (final int each : made) {
            most = Math.max(most, each);
        }
        return most;
    }

    /** What it drew a tick at its last reading; nothing before its first. */
    public long lastPower() {
        return power.isEmpty() ? 0L : power.getLast();
    }

    /** What it drew a tick on the whole over every reading; nothing before its first. */
    public long averagePower() {
        long sum = 0L;
        for (final long each : power) {
            sum += each;
        }
        return power.isEmpty() ? 0L : sum / power.size();
    }

    /** What last stopped it in these ten minutes, other than its player. */
    public Optional<MachineStatus> stop() {
        return stoppedBy.isEmpty() ? Optional.empty() : Optional.ofNullable(STATUSES.find(stoppedBy));
    }

    /* What {@code statistics} hold at {@code now}. */
    static MachineHistory of(final MachineStatistics statistics, final long now) {
        return new MachineHistory(statistics.made(), statistics.power(), statistics.efficiency(),
                statistics.workedTicks(), statistics.stoppedBy(now), statistics.stoppedSecondsAgo(now));
    }
}
