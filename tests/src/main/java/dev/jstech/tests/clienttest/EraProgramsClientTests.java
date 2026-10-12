/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.tests.testkit.EraKit;
import java.util.ArrayList;
import java.util.List;

/**
 * Each era's programs in a player's hands: at the era's Personal Computer, on its own network, every program its
 * desktop's Start menu lists opens in a window from there, and the window's close box closes it again.
 */
public final class EraProgramsClientTests {

    /** How long a program may take to put its window up. */
    private static final int WINDOW_WAIT = 80;

    private EraProgramsClientTests() {
    }

    @ClientTest(timeoutTicks = 6000)
    public static void legacy_everyProgramOnStartOpensAndCloses(final ClientTestContext ctx) {
        programs(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void transition_everyProgramOnStartOpensAndCloses(final ClientTestContext ctx) {
        programs(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void standard_everyProgramOnStartOpensAndCloses(final ClientTestContext ctx) {
        programs(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 6000)
    public static void advanced_everyProgramOnStartOpensAndCloses(final ClientTestContext ctx) {
        programs(ctx, EraKit.ADVANCED);
    }

    private static void programs(final ClientTestContext ctx, final EraKit kit) {
        final List<String> labels = new ArrayList<>();
        EraJourney.atTheDesk(ctx, kit)
                .then(EraJourney.SETTLE, () -> {
                    final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
                    // Whatever the desktop opened on its own, a welcome, is closed first, as a player would.
                    for (final String open : List.copyOf(desktop.openWindowLabels())) {
                        close(ctx, open);
                    }
                    labels.addAll(desktop.launcherLabels());
                })
                .then(EraJourney.SETTLE, () -> {
                    for (final String label : labels) {
                        opens(ctx, kit, label);
                    }
                });
    }

    /* Opens {@code label} from Start, waits for its window, looks at it and closes it with its close box. */
    private static void opens(final ClientTestContext ctx, final EraKit kit, final String label) {
        EraJourney.launch(ctx, kit, label)
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(label) != null, WINDOW_WAIT,
                        "the " + label + " window to open from Start")
                .thenScreenshot(EraJourney.SETTLE, kit.name().toLowerCase() + "-" + label.toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-"))
                .then(0, () -> close(ctx, label))
                .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(label) == null, WINDOW_WAIT,
                        "the " + label + " window to close from its close box");
    }

    private static void close(final ClientTestContext ctx, final String label) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(label);
        if (window != null) {
            ctx.clickDesktop(window.buttonCentre(DesktopWindow.BUTTON_CLOSE));
        }
    }
}
