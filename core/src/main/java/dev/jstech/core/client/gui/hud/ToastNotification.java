/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.gui.hud;

/**
 * Bridge that displays a {@link ToastData} via the vanilla toast system.
 */
public final class ToastNotification {

    private ToastNotification() {
    }

    // Package-private until a caller exists. Showing a toast is deferred on purpose: the game's toast API changes
    // between versions, so the bridge waits until it is needed and can be written against the version in use.
    static void show(final ToastData data) {
        throw new UnsupportedOperationException("ToastNotification.show is not implemented yet");
    }
}