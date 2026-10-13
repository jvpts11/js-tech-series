/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.operation.payload.OpenKvmPayload;
import dev.jstech.computers.registry.ClientHook;

/**
 * Opens the client-only KVM channel bar. Mirrors {@link IPostScreenOpener}: the payload handler lives
 * in common code, so the client registers the actual screen-opening lambda here during client setup
 * and the dedicated server never touches a screen class.
 */
@FunctionalInterface
public interface IKvmScreenOpener {

    /** Shows the switch's channel bar for the rack and monitor named in the payload. */
    void open(OpenKvmPayload payload);

    final class Holder {

        private static final ClientHook<IKvmScreenOpener> HOOK = new ClientHook<>();

        private Holder() {
        }

        public static void set(final IKvmScreenOpener opener) {
            HOOK.set(opener);
        }

        public static void open(final OpenKvmPayload payload) {
            HOOK.ifPresent(target -> target.open(payload));
        }
    }
}
