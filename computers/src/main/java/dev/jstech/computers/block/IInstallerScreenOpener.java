/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import dev.jstech.computers.registry.ClientHook;

/**
 * Opens the client-only installer. Mirrors {@link IBootMenuScreenOpener}: the payload handler lives in common
 * code, so the client registers the screen-opening lambda here during client setup and the dedicated server never
 * touches a screen class.
 */
@FunctionalInterface
public interface IInstallerScreenOpener {

    /** Shows the installer the machine is in, on the page it has reached. */
    void open(OpenInstallerPayload payload);

    final class Holder {

        private static final ClientHook<IInstallerScreenOpener> HOOK = new ClientHook<>();

        private Holder() {
        }

        public static void set(final IInstallerScreenOpener opener) {
            HOOK.set(opener);
        }

        public static void open(final OpenInstallerPayload payload) {
            HOOK.ifPresent(target -> target.open(payload));
        }
    }
}
