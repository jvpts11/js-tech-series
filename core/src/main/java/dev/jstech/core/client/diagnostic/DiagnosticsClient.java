/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.diagnostic;

import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.client.GameLocale;
import dev.jstech.core.diagnostic.DiagnosticTexts;
import dev.jstech.core.diagnostic.Diagnostics;
import dev.jstech.core.diagnostic.Timings;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * The series on the game's debug screen (F3), in its right-hand column where the game shows what it has inside: a
 * panel for each part of the series that has something to show, under its name, and the pieces of work timed, the
 * longest on average first. A part adds its panel from its client code:
 *
 * <pre>{@code
 * DiagnosticsClient.panel(SoundMixerTexts.TITLE, AudioDebugLines::lines);
 * }</pre>
 */
public final class DiagnosticsClient {

    /** How many of the pieces of work timed are shown, the longest first. */
    public static final int TIMINGS_SHOWN = 8;

    private static final Logger LOGGER = CoreLogs.DIAGNOSTICS.logger();
    private static final List<Panel> PANELS = new CopyOnWriteArrayList<>();
    private static final double NANOS_PER_MILLI = 1_000_000.0;

    /* The panels already reported as broken, so a fault that repeats every frame is said once. */
    private static final Set<Panel> REPORTED = ConcurrentHashMap.newKeySet();

    private DiagnosticsClient() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(CustomizeGuiOverlayEvent.DebugText.class, DiagnosticsClient::onDebugText);
    }

    /** Adds a panel called {@code title}, whose lines are read each frame the debug screen is open. */
    public static void panel(final TextKey title, final Supplier<List<String>> lines) {
        PANELS.add(new Panel(Objects.requireNonNull(title, "title"), Objects.requireNonNull(lines, "lines")));
    }

    /** Every line the series adds to the debug screen, as it reads now. */
    public static List<String> lines() {
        final List<String> out = new ArrayList<>();
        for (final Panel panel : PANELS) {
            final List<String> own = linesOf(panel);
            if (own.isEmpty()) {
                continue;
            }
            out.add("");
            out.add(ChatFormatting.UNDERLINE + GameText.resolve(panel.title()));
            out.addAll(own);
        }
        final List<Timings.Summary> timed = Diagnostics.timings().summaries();
        if (!timed.isEmpty()) {
            out.add("");
            out.add(ChatFormatting.UNDERLINE + GameText.resolve(DiagnosticTexts.TIMINGS));
            for (final Timings.Summary summary : timed.subList(0, Math.min(TIMINGS_SHOWN, timed.size()))) {
                out.add(GameText.resolve(DiagnosticTexts.TIMING.with(summary.section(), millis(summary.averageNanos()),
                        millis(summary.maxNanos()))));
            }
        }
        return out;
    }

    public static void onDebugText(final CustomizeGuiOverlayEvent.DebugText event) {
        event.getRight().addAll(lines());
    }

    /* One panel's lines; a panel that fails or gives none shows nothing and leaves the others be. */
    private static List<String> linesOf(final Panel panel) {
        try {
            final List<String> own = panel.lines().get();
            return own == null ? List.of() : own;
        } catch (final RuntimeException broken) {
            if (REPORTED.add(panel)) {
                LOGGER.warn("A panel of the debug screen failed and is left out", broken);
            }
            return List.of();
        }
    }

    private static String millis(final long nanos) {
        return String.format(GameLocale.locale(), "%.3f", nanos / NANOS_PER_MILLI);
    }

    /* A part of the series' lines, under its name. */
    private record Panel(TextKey title, Supplier<List<String>> lines) {
    }
}
