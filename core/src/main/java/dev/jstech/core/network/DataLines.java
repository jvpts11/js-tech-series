/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.network;

import dev.jstech.core.connect.Connection;
import dev.jstech.core.tier.HardwareEra;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The data lines as cables and ports are made of them: each {@link DataLine}'s id, and what a port of a machine of an
 * era takes, which is the line up to that era's generation.
 */
public final class DataLines {

    private static final Map<DataLine, ResourceLocation> IDS = new EnumMap<>(DataLine.class);

    static {
        for (final DataLine line : DataLine.values()) {
            IDS.put(line, ResourceLocation.parse(line.lineId()));
        }
    }

    private DataLines() {
    }

    /** The id {@code line} is named by. */
    public static ResourceLocation id(final DataLine line) {
        return IDS.get(line);
    }

    /** The data line {@code line} names, or null when it names none. */
    public static @Nullable DataLine of(final @Nullable ResourceLocation line) {
        return line == null ? null : DataLine.ofLineId(line.toString());
    }

    /** Whether {@code line} is a data line. */
    public static boolean isData(final ResourceLocation line) {
        return of(line) != null;
    }

    /** What a cable of {@code link} offers the face it touches: its line, in its generation. */
    public static Connection of(final DataLink link) {
        return new Connection(id(link.line()), link.generation());
    }

    /** What a port of {@code line} on a machine of {@code era} takes: that era's cable and every earlier one. */
    public static Connection upTo(final HardwareEra era, final DataLine line) {
        return new Connection(id(line), line.generation(era));
    }

    /** What a port that takes each of {@code lines} on a machine of {@code era} takes. */
    public static Connection[] upTo(final HardwareEra era, final DataLine... lines) {
        final Connection[] out = new Connection[lines.length];
        for (int i = 0; i < lines.length; i++) {
            out[i] = upTo(era, lines[i]);
        }
        return out;
    }

    /** The link a connection of a data line is, or null when it is no data cable. */
    public static @Nullable DataLink linkOf(final Connection connection) {
        return DataLink.of(connection.line().toString(), connection.generation());
    }
}
