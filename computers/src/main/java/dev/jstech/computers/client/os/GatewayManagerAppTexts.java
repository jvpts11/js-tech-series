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

/** The Gateway Manager window's own title, apart from what {@link GatewayManagerTexts} covers. */
@TextHolder
final class GatewayManagerAppTexts {

    static final TextKey TITLE = TextKey.of("jsc.gateway_manager_app.title", "Gateway Manager");

    private GatewayManagerAppTexts() {
    }
}
