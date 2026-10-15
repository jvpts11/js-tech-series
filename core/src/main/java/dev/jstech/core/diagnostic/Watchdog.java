/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.log.CoreLogs;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

/*
 * The watchdog: a thread of its own that looks at every thread running a section four times a second. A run that
 * has gone on longer than its section may is a stall: the first of each section is written to its system's log with
 * the stack of the thread it runs on, and the operators are told; every one is counted, and its length kept once it
 * ends, for /jstech health. The game's own watchdog stops a server whose tick never ends; this one says which part of
 * the series' work a long tick was in.
 */
final class Watchdog {

    private static final Logger LOGGER = CoreLogs.DIAGNOSTICS.logger();
    private static final long LOOK_EVERY_MILLIS = 250L;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private Watchdog() {
    }

    static void start() {
        if (STARTED.compareAndSet(false, true)) {
            Thread.ofPlatform().daemon().name("jscore-watchdog").priority(Thread.MIN_PRIORITY)
                    .start(Watchdog::watch);
        }
    }

    private static void watch() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(LOOK_EVERY_MILLIS);
                look(System.nanoTime());
            } catch (final InterruptedException stopped) {
                Thread.currentThread().interrupt();
            } catch (final RuntimeException failed) {
                LOGGER.error("The watchdog failed to look at the sections running; it goes on looking", failed);
            }
        }
    }

    /*
     * One look at every thread: the innermost run on it that has gone on too long is the stall, and the runs it is
     * inside are marked with it, since they are long by the same cause.
     */
    private static void look(final long now) {
        for (final Sections.Slot slot : Sections.slots()) {
            if (!slot.thread.isAlive()) {
                Sections.slots().remove(slot);
                continue;
            }
            for (Sections.Running at = slot.top; at != null; at = at.outer) {
                if (now - at.start < at.section.stallLimit()) {
                    continue;
                }
                if (!at.seen) {
                    for (Sections.Running inside = at; inside != null; inside = inside.outer) {
                        inside.seen = true;
                    }
                    stalled(slot, at, now - at.start);
                }
                break;
            }
        }
    }

    private static void stalled(final Sections.Slot slot, final Sections.Running running, final long nanos) {
        if (!running.section.stalledFirst()) {
            return;
        }
        final String millis = Long.toString(nanos / NANOS_PER_MILLI);
        final Stack stack = new Stack(slot.thread.getName());
        stack.setStackTrace(slot.thread.getStackTrace());
        final Logger logger = running.section.system().logger();
        logger.warn("{} of {} has run for {} ms on the thread {}; the first stall of a section is written down with "
                + "its stack, and the rest counted for /jstech health", running.section, running.section.system(),
                millis, slot.thread.getName(), stack);
        Sections.tellOperators(DiagnosticNotifications.STALLED,
                DiagnosticTexts.STALLED.with(running.section.toString(), millis));
    }

    /* The stack a stalled thread was on, written to the log as an exception's would be. */
    private static final class Stack extends Exception {

        private static final long serialVersionUID = 1L;

        private Stack(final String thread) {
            super("where " + thread + " was when it stalled", null, false, true);
        }
    }
}
