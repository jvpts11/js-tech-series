/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.operation.payload.crash.OpenCrashPayload;
import dev.jstech.computers.registry.ClientHook;

/**
 * Hands the client-only crash screen the page a machine's system crashed onto. Mirrors {@link ISystemBootScreenOpener}:
 * the payload handler lives in common code, so the client registers the screen's side here during client setup and
 * the dedicated server never touches a screen class.
 */
@FunctionalInterface
public interface ICrashScreenOpener {

    /** Takes the page, for the session that is about to show it or for the one already showing it. */
    void open(OpenCrashPayload page);

    final class Holder {

        private static final ClientHook<ICrashScreenOpener> HOOK = new ClientHook<>();

        private Holder() {
        }

        public static void set(final ICrashScreenOpener opener) {
            HOOK.set(opener);
        }

        public static void open(final OpenCrashPayload page) {
            HOOK.ifPresent(target -> target.open(page));
        }
    }
}
