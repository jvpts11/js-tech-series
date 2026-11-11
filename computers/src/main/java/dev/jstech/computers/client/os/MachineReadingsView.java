/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.hardware.MachineReadings;
import dev.jstech.computers.operation.payload.MachineReadingsPayload;
import dev.jstech.computers.operation.payload.RequestMachineReadingsPayload;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What a system monitor shows of its machine's power and temperature: it asks the server for the period it shows (the
 * last hour, day or 7 days) every couple of seconds, and keeps the last answer. The Task Manager and the System Monitor
 * each hold one.
 */
public final class MachineReadingsView {

    private final BlockPos host;
    private int period;
    private int frame;

    /* The periods' hours, in the order every picker lists them. */
    private static final int[] HOURS = {MachineReadings.HOUR, MachineReadings.DAY, MachineReadings.WEEK};
    private static final TextKey[] PERIOD_WORDS = {TaskManagerTexts.PERIOD_HOUR, TaskManagerTexts.PERIOD_DAY,
        TaskManagerTexts.PERIOD_WEEK};
    private static final int REFRESH_FRAMES = 40;
    private static final double JOULES_PER_KWH = 3_600_000.0;

    /* The last answer for each machine; a window reads only the one of its own machine and period. */
    private static final Map<BlockPos, MachineReadingsPayload> LATEST = new HashMap<>();

    MachineReadingsView(final BlockPos host) {
        this.host = host;
    }

    /** Keeps the server's answer for whichever window of that machine asked. */
    public static void accept(final MachineReadingsPayload payload) {
        LATEST.put(payload.hostPos(), payload);
    }

    /** Asks again now and then while the window is drawn. */
    void tick() {
        if (frame++ % REFRESH_FRAMES == 0) {
            request();
        }
    }

    /** The period shown: 0 the last hour, 1 the last day, 2 the last 7 days. */
    int period() {
        return period;
    }

    /** Shows {@code index} from now on, asking for it at once. */
    void choose(final int index) {
        period = Math.floorMod(index, HOURS.length);
        request();
    }

    /** The period after the one shown, as a click on the status bar moves it. */
    void next() {
        choose(period + 1);
    }

    /** The period shown in words, for a line of text: "last hour". */
    String periodWords() {
        return GameText.resolve(PERIOD_WORDS[period]);
    }

    /** The last answer for this machine and this period, or null before one came. */
    @Nullable
    MachineReadingsPayload latest() {
        final MachineReadingsPayload last = LATEST.get(host);
        return last != null && last.hours() == HOURS[period] ? last : null;
    }

    /** Joules as kilowatt-hours, with a decimal: "9.5 kWh". */
    static String kwh(final long joules) {
        return GameText.resolve(TaskManagerTexts.KWH.with(String.format(Locale.ROOT, "%.1f",
                joules / JOULES_PER_KWH)));
    }

    /** A series with its empty samples filled from the reading before them, for a line that does not drop to zero. */
    static int[] filled(final int[] series) {
        final int[] out = new int[series.length];
        int last = MachineReadings.NONE;
        for (final int value : series) {
            if (value != MachineReadings.NONE) {
                last = value;
                break;
            }
        }
        for (int i = 0; i < series.length; i++) {
            last = series[i] == MachineReadings.NONE ? last : series[i];
            out[i] = last == MachineReadings.NONE ? 0 : last;
        }
        return out;
    }

    private void request() {
        PacketDistributor.sendToServer(new RequestMachineReadingsPayload(host, HOURS[period]));
    }
}
