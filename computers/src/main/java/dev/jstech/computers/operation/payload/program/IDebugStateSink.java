/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.program;

import dev.jstech.computers.registry.ClientHook;

/**
 * Where a debugger's state goes when it reaches the player: the studio that is debugging, which only the client has.
 * The payload's handler hands it here, so the code it runs on a server never names a class of the client's.
 */
@FunctionalInterface
public interface IDebugStateSink {

    void accept(DebugStatePayload state);

    /** The sink the client set, if it set one. */
    final class Holder {

        private static final ClientHook<IDebugStateSink> HOOK = new ClientHook<>();

        private Holder() {
        }

        public static void set(final IDebugStateSink sink) {
            HOOK.set(sink);
        }

        public static void accept(final DebugStatePayload state) {
            HOOK.ifPresent(target -> target.accept(state));
        }
    }
}
