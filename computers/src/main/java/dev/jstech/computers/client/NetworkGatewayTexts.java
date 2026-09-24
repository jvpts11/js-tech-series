/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What the Network Gateway's own screen says, kept apart from the screen so the language generator can read it on
 * a server too, where screens do not exist.
 */
@TextHolder
final class NetworkGatewayTexts {

    static final TextKey TITLE = TextKey.of("jsc.network_gateway.title", "NETWORK GATEWAY");
    /* The Gateway's name, the computer it serves, and how it reaches it. */
    static final TextKey LINKED = TextKey.of("jsc.network_gateway.linked", "%s on %s, %s");
    static final TextKey NOT_LINKED =
            TextKey.of("jsc.network_gateway.not_linked", "%s, not linked: plug the cable in the back");
    static final TextKey SEEN_FROM_CC = TextKey.of("jsc.network_gateway.seen_from_cc", "Seen from CC as %s");
    static final TextKey MANAGED = TextKey.of("jsc.network_gateway.managed", "Managed in Gateway Manager");
    static final TextKey ITEM_BUFFER = TextKey.of("jsc.network_gateway.item_buffer", "ITEM BUFFER");
    static final TextKey BUFFER_HINT = TextKey.of("jsc.network_gateway.buffer_hint", "pull lands here, push leaves");

    private NetworkGatewayTexts() {
    }
}
