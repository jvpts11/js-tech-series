/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.UiWindowPayload;
import java.util.ArrayList;
import java.util.List;

/**
 * What other screens and programs have asked of the desktop since the last frame: things to open, windows a machine's
 * own programs have opened or closed, and windows the Task Manager has ended.
 *
 * <p>They arrive as requests rather than as calls because whoever asks is usually not on the render thread and often
 * is not a screen at all. The desktop that is up carries them out once at the top of a frame, which is what keeps a
 * window from being opened halfway through the frame that draws it.
 */
final class DesktopRequests {

    /** What was asked to be opened, in the order it was asked. */
    private static final List<OpenRequest> OPEN = new ArrayList<>();
    /**
     * The windows the machine says its Σ# programs have open. A program's window is the machine's, not this screen's:
     * it is opened when the machine first mentions it, redrawn whenever the machine sends it again, and taken away
     * when the machine says it is gone.
     */
    private static final List<UiWindowPayload> WINDOWS = new ArrayList<>();
    /** The programs whose windows were asked to end, by key or by name. */
    private static final List<String> CLOSE = new ArrayList<>();

    private DesktopRequests() {
    }

    static void open(final OpenRequest request) {
        OPEN.add(request);
    }

    static void window(final UiWindowPayload payload) {
        WINDOWS.add(payload);
    }

    static void close(final String key) {
        CLOSE.add(key);
    }

    /** Drops every request that never found a desktop, as the player leaves a world. */
    static void forgetAll() {
        OPEN.clear();
        WINDOWS.clear();
        CLOSE.clear();
    }

    /**
     * Carries out every request on {@code desktop}. Each list is taken before it is worked through, so a request made
     * while carrying one out waits for the next frame instead of changing the list underneath.
     */
    static void drain(final DesktopScreen desktop) {
        final ProgramOpener opener = desktop.opener();
        for (final UiWindowPayload payload : take(WINDOWS)) {
            opener.acceptProgramWindow(payload);
        }
        for (final OpenRequest request : take(OPEN)) {
            opener.open(request);
        }
        /*
         * A request to end a program's window closes the newest of that program, so ending a program that is open
         * more than once closes the one on top rather than the oldest copy of it.
         */
        for (final String asked : take(CLOSE)) {
            desktop.wm().closeNewest(desktop.keyFor(asked));
        }
    }

    private static <T> List<T> take(final List<T> pending) {
        if (pending.isEmpty()) {
            return List.of();
        }
        final List<T> now = List.copyOf(pending);
        pending.clear();
        return now;
    }
}
