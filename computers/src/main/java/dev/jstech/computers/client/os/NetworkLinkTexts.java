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

/** The Network Manager's words for how its nodes are linked: the LINK column, the Map's legend, the hover card. */
@TextHolder
final class NetworkLinkTexts {

    static final TextKey LINK_COLUMN = TextKey.of("jsc.network_links.link_column", "LINK");
    static final TextKey SPEED = TextKey.of("jsc.network_links.speed", "%s · %s it/t");
    static final TextKey DOWN = TextKey.of("jsc.network_links.down", "%s · link down");
    static final TextKey NO_LINK = TextKey.of("jsc.network_links.no_link", "no link");
    static final TextKey PLACE = TextKey.of("jsc.network_links.place", "%s, %s, %s");
    static final TextKey BENDS = TextKey.of("jsc.network_links.bends",
            "%s, no link: its fibre bends at %s, and fibre only runs straight; an Optical Router there turns it.");
    static final TextKey TOO_LONG = TextKey.of("jsc.network_links.too_long",
            "%s, no link: a run of %s %s cables is longer than they reach (%s blocks).");
    static final TextKey MORE = TextKey.of("jsc.network_links.more", "+%s more");

    // the Map
    static final TextKey MAP_DOWN = TextKey.of("jsc.network_links.map_down", "link down");
    static final TextKey MAP_BENDS = TextKey.of("jsc.network_links.map_bends", "bends at %s");
    static final TextKey LEGEND = TextKey.of("jsc.network_links.legend", "LINES");
    static final TextKey LEGEND_ENTRY = TextKey.of("jsc.network_links.legend_entry", "%s, %s");
    static final TextKey OPTICAL_CARD = TextKey.of("jsc.network_links.optical_card", "Optical Network Card");
    static final TextKey OPTICAL_ROUTER = TextKey.of("jsc.network_links.optical_router", "Optical Router");
    static final TextKey ACCESS = TextKey.of("jsc.network_links.access", "access");
    static final TextKey BACKBONE = TextKey.of("jsc.network_links.backbone", "backbone");
    static final TextKey LONG_DISTANCE = TextKey.of("jsc.network_links.long_distance", "long distance");
    static final TextKey HIGH_COMPUTE = TextKey.of("jsc.network_links.high_compute", "high compute");

    // the hover card
    static final TextKey SECTION = TextKey.of("jsc.network_links.section", "NETWORK");
    static final TextKey CARD_LINK = TextKey.of("jsc.network_links.card_link", "%s, %s: %s it/t");
    static final TextKey CARD_NAMED = TextKey.of("jsc.network_links.card_named", "%s: %s it/t");
    static final TextKey RUN = TextKey.of("jsc.network_links.run", "Link up: a %s-block run of %s");
    static final TextKey THROUGH = TextKey.of("jsc.network_links.through", "through the Optical Router at %s");
    static final TextKey CARD_BENDS = TextKey.of("jsc.network_links.card_bends", "No link: its fibre bends at %s");
    static final TextKey CARD_TOO_LONG =
            TextKey.of("jsc.network_links.card_too_long", "No link: a run of %s cables, past its %s-block reach");

    // the Hardware tab
    static final TextKey LINKS = TextKey.of("jsc.network_links.links", "LINKS");
    static final TextKey OPTICAL_LINKS = TextKey.of("jsc.network_links.optical_links", "Optical links");
    static final TextKey OPTICAL_VALUE = TextKey.of("jsc.network_links.optical_value", "%s (%s up, %s down)");
    static final TextKey BACKBONE_ROW = TextKey.of("jsc.network_links.backbone_row", "Backbone");
    static final TextKey LINK_VALUE = TextKey.of("jsc.network_links.link_value", "%s, %s it/t");
    static final TextKey SLOWEST = TextKey.of("jsc.network_links.slowest", "Slowest link in use");
    static final TextKey SLOWEST_VALUE = TextKey.of("jsc.network_links.slowest_value", "%s, %s it/t (%s)");
    static final TextKey NONE = TextKey.of("jsc.network_links.none", "none");

    private NetworkLinkTexts() {
    }
}
