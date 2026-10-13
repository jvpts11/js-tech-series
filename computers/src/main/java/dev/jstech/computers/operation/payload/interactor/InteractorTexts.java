/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.interactor;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Network Interactor's server side says about where things are kept, apart from the handlers that hand
 * it to windows: the language generator loads every class that declares sentences, on a server as well, where
 * windows do not exist.
 */
@TextHolder
final class InteractorTexts {

    /* A server by where its rack stands and which slot it is in. */
    static final TextKey SERVER_AT = TextKey.of("jsc.interactor.server_at", "Server %s, %s, %s #%s");
    static final TextKey PUBLISHED_PC = TextKey.of("jsc.interactor.published_pc", "Published PC");

    private InteractorTexts() {
    }
}
