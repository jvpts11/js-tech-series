/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import dev.jstech.computers.registry.ClientHook;


/**
 * Where a client keeps what the monitors near it show. The payload's handler is common code, so the client sets the
 * real keeper here during its setup and a dedicated server never touches a client class.
 */
@FunctionalInterface
public interface IMonitorPictureSink {

    void accept(MonitorPicturePayload payload);

    final class Holder {

        private static final ClientHook<IMonitorPictureSink> HOOK = new ClientHook<>();

        private Holder() {
        }

        public static void set(final IMonitorPictureSink sink) {
            HOOK.set(sink);
        }

        public static void accept(final MonitorPicturePayload payload) {
            HOOK.ifPresent(target -> target.accept(payload));
        }
    }
}
