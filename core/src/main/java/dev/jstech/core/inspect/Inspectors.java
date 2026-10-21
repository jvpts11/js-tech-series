/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inspect;

import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.log.LogSystem;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;

/**
 * What the series' systems say of one place, as {@code /jstech inspect} reads it and the live inspection shows it
 * on a player's debug screen: the block and its block entity's fields as they are now, what it offers on each face,
 * and what each system holds there (a data network, a grid, a sealed room, a multiblock). Each kit adds its part from
 * its mod's constructor:
 *
 * <pre>{@code
 * Inspectors.provide(LOG, (level, pos, report) -> {
 *     if (level.getBlockEntity(pos) instanceof ReactorBlockEntity reactor) {
 *         report.line(TEXTS.CORE_HEAT, reactor.kelvin());
 *         report.under(TEXTS.RODS, rods -> reactor.rods().forEach(rod -> rods.line(TEXTS.ROD, rod.depth())));
 *     }
 * });
 * }</pre>
 *
 * <p>A part is read on the server's thread; one that fails says so, and the rest are read.
 */
public final class Inspectors {

    private static final Logger LOGGER = CoreLogs.DIAGNOSTICS.logger();
    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();
    /** The longest a value is written, so a long string in a block's fields does not swamp the rest. */
    private static final int LONGEST_VALUE = 120;
    private static final String CUT = "...";

    private Inspectors() {
    }

    /** What reads one system's part of a place. */
    @FunctionalInterface
    public interface IInspector {

        /** Writes what the system holds at {@code pos} of {@code level} into {@code report}, on the server's thread. */
        void inspect(ServerLevel level, BlockPos pos, Inspection report);
    }

    /** Adds {@code inspector}'s part, under {@code system}; a system may have several. */
    public static void provide(final LogSystem system, final IInspector inspector) {
        PROVIDERS.add(new Provider(Objects.requireNonNull(system, "system"),
                Objects.requireNonNull(inspector, "inspector")));
    }

    /** Every system's part of {@code pos}, in the order the parts were added, a system's parts together. */
    public static List<Part> read(final ServerLevel level, final BlockPos pos) {
        final List<Part> out = new ArrayList<>();
        for (final Provider provider : PROVIDERS) {
            final Inspection report = new Inspection(0);
            try {
                provider.inspector().inspect(level, pos, report);
            } catch (final RuntimeException failed) {
                LOGGER.warn("What {} holds at {} could not be read", provider.system(), pos, failed);
                report.line(InspectTexts.FAILED.text(), String.valueOf(failed));
            }
            if (report.lines.isEmpty()) {
                continue;
            }
            final Part last = out.isEmpty() ? null : out.getLast();
            if (last != null && last.system() == provider.system()) {
                final List<Line> joined = new ArrayList<>(last.lines());
                joined.addAll(report.lines);
                out.set(out.size() - 1, new Part(last.system(), joined));
            } else {
                out.add(new Part(provider.system(), report.lines));
            }
        }
        return out;
    }

    /** A value as an inspection writes it: a number to three places, anything else as it reads, cut when long. */
    public static String written(final Object value) {
        final String written = value instanceof Double || value instanceof Float
                ? String.format(Locale.ROOT, "%.3f", ((Number) value).doubleValue()) : String.valueOf(value);
        return written.length() > LONGEST_VALUE ? written.substring(0, LONGEST_VALUE) + CUT : written;
    }

    /* What {@code writes} says, on its own, so two parts can be told alike before either is reported. */
    static List<Line> linesOf(final Consumer<Inspection> writes) {
        final Inspection detached = new Inspection(0);
        writes.accept(detached);
        return List.copyOf(detached.lines);
    }

    /** What one system's part is written into: lines of a name and a value, and lines under them. */
    public static final class Inspection {

        private final int depth;
        private final List<Line> lines = new ArrayList<>();

        private Inspection(final int depth) {
            this.depth = depth;
        }

        /** A line naming something and saying its value. */
        public Inspection line(final Text name, final Object value) {
            this.lines.add(new Line(this.depth, name, written(value)));
            return this;
        }

        /** A line naming something, with no value of its own. */
        public Inspection line(final Text name) {
            this.lines.add(new Line(this.depth, name, ""));
            return this;
        }

        /** A line naming something, and the lines {@code inner} writes under it. */
        public Inspection under(final Text name, final Consumer<Inspection> inner) {
            this.lines.add(new Line(this.depth, name, ""));
            final Inspection nested = new Inspection(this.depth + 1);
            inner.accept(nested);
            this.lines.addAll(nested.lines);
            return this;
        }
    }

    /**
     * One system's part of a place.
     *
     * @param system the system
     * @param lines  what it says, in the order it said it
     */
    public record Part(LogSystem system, List<Line> lines) {

        public Part {
            lines = List.copyOf(lines);
        }
    }

    /**
     * One line of a part.
     *
     * @param depth how far under another line it is, 0 for none
     * @param name  what it names
     * @param value its value, empty for a line with lines under it
     */
    public record Line(int depth, Text name, String value) {
    }

    /* A part added, and the system it is under. */
    private record Provider(LogSystem system, IInspector inspector) {
    }
}
