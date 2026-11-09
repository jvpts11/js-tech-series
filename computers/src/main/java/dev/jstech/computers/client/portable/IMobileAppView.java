/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * The inside of one of the portables' apps (computers/docs/design/wireless.md): what it draws on its page, with its
 * family's kit, and what it asks the server for while it is open, so what it shows stays fresh.
 */
public sealed interface IMobileAppView permits NetworkAppView, AlertsAppView, MessengerAppView, CameraAppView,
        GalleryAppView, MapsAppView, ProspectingAppView {

    /** Draws the app on {@code page}. */
    void paint(AppPage page);

    /** What it asks the server for now, or null when it asks for nothing. */
    @Nullable
    PortableActionPayload ask(UUID device, MobileAppState state);

    /** How often it asks while it is open, in ticks. */
    int askEvery();
}
