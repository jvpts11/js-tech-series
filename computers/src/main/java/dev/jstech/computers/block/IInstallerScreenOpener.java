/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import org.jetbrains.annotations.Nullable;

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

        @Nullable
        private static IInstallerScreenOpener instance;

        private Holder() {
        }

        public static void set(final IInstallerScreenOpener opener) {
            instance = opener;
        }

        public static void open(final OpenInstallerPayload payload) {
            if (instance != null) {
                instance.open(payload);
            }
        }
    }
}
