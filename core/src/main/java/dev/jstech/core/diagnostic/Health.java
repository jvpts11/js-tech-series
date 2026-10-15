/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.log.LogSystem;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/**
 * The state of the series' systems, as {@code /jstech health} reads it, a crash report keeps it and the diagnostic
 * file writes it down: each system says how much of it there is (networks, grids, chunks loaded per owner, work
 * waiting) and warns of what is wrong. Each kit adds its part from its mod's constructor:
 *
 * <pre>{@code
 * Health.provide(LOG, (server, report) -> {
 *     report.line(TEXTS.SPREADING.with(fallout.size(server)));
 *     if (fallout.behind(server)) {
 *         report.warn(TEXTS.BEHIND);
 *     }
 * });
 * }</pre>
 *
 * <p>A part is read on the server's thread, and a part that fails is reported as failed rather than stopping the rest.
 */
public final class Health {

    private static final Logger LOGGER = CoreLogs.DIAGNOSTICS.logger();
    private static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private Health() {
    }

    /** What reads one system's part of the state. */
    @FunctionalInterface
    public interface IProvider {

        /** Writes the part into {@code report}, on the server's thread. */
        void report(MinecraftServer server, Report report);
    }

    /** Adds {@code provider}'s part, under {@code system}; a system may have several. */
    public static void provide(final LogSystem system, final IProvider provider) {
        PROVIDERS.add(new Provider(Objects.requireNonNull(system, "system"),
                Objects.requireNonNull(provider, "provider")));
    }

    /** Every system's part as it reads now, in the order the parts were added, a system's parts together. */
    public static List<Part> read(final MinecraftServer server) {
        final List<Part> out = new ArrayList<>();
        for (final Provider provider : PROVIDERS) {
            final Report report = new Report();
            try {
                provider.provider().report(server, report);
            } catch (final RuntimeException failed) {
                LOGGER.warn("The state of {} could not be read", provider.system(), failed);
                report.warn(DiagnosticTexts.HEALTH_FAILED.with(String.valueOf(failed)));
            }
            if (report.lines.isEmpty()) {
                continue;
            }
            final Part last = out.isEmpty() ? null : out.get(out.size() - 1);
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

    /** How many warnings the parts hold. */
    public static int warnings(final List<Part> parts) {
        int count = 0;
        for (final Part part : parts) {
            for (final Line line : part.lines()) {
                count += line.warning() ? 1 : 0;
            }
        }
        return count;
    }

    /** What one system's part is written into: lines that say how it is, and warnings of what is wrong. */
    public static final class Report {

        private final List<Line> lines = new ArrayList<>();

        private Report() {
        }

        /** A line that says how the system is. */
        public Report line(final Text text) {
            this.lines.add(new Line(text, false));
            return this;
        }

        /** A warning of something wrong, which {@code /jstech health} shows apart. */
        public Report warn(final Text text) {
            this.lines.add(new Line(text, true));
            return this;
        }
    }

    /**
     * One system's part of the state.
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
     * @param text    what it says
     * @param warning whether it warns of something wrong
     */
    public record Line(Text text, boolean warning) {
    }

    /* A part added, and the system it is under. */
    private record Provider(LogSystem system, IProvider provider) {
    }
}
