/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The Network Interactor window's own title, apart from what {@link NetworkInteractorTexts} covers. */
@TextHolder
final class NetworkInteractorAppTexts {

    static final TextKey TITLE = TextKey.of("jsc.network_interactor_app.title", "Network Interactor");

    private NetworkInteractorAppTexts() {
    }
}
