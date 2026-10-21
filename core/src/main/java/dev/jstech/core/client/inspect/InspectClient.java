/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.inspect;

import dev.jstech.core.client.diagnostic.DiagnosticsClient;
import dev.jstech.core.inspect.InspectTexts;
import dev.jstech.core.inspect.InspectionPayload;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The place the player watches ({@code /jstech inspect watch}), as the server last read it, on the debug screen (F3):
 * a panel of what the systems say of it, and one of what the server sent the player in its last tick.
 */
public final class InspectClient {

    private static final String INDENT = "  ";
    private static final String BETWEEN = ": ";

    private static volatile InspectionPayload told = InspectionPayload.stopped();

    private InspectClient() {
    }

    /** Hands the game's bus what this answers, and the debug screen its panels, once, on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class,
                event -> told = InspectionPayload.stopped());
        DiagnosticsClient.panel(InspectTexts.PANEL, InspectClient::inspected);
        DiagnosticsClient.panel(InspectTexts.PAYLOADS_PANEL, () -> linesOf(told.sent()));
    }

    /** What the server read of the place the player watches. */
    public static void told(final InspectionPayload payload) {
        told = payload;
    }

    /** What the server last read, as it came. */
    public static InspectionPayload last() {
        return told;
    }

    /** The lines as the debug screen shows them: each under the one before it, by its depth. */
    public static List<String> linesOf(final List<InspectionPayload.Line> lines) {
        final List<String> out = new ArrayList<>(lines.size());
        for (final InspectionPayload.Line line : lines) {
            final String name = INDENT.repeat(line.depth()) + line.name().getString();
            out.add(line.value().isEmpty() ? name : name + BETWEEN + line.value());
        }
        return out;
    }

    private static List<String> inspected() {
        final InspectionPayload now = told;
        if (now.lines().isEmpty()) {
            return List.of();
        }
        final List<String> out = new ArrayList<>();
        out.add(now.pos().toShortString());
        out.addAll(linesOf(now.lines()));
        return out;
    }
}
