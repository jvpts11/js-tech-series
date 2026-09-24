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

/**
 * What Storage Insights says, kept apart from the window so the language generator can read it on a server too,
 * where windows do not exist.
 */
@TextHolder
final class StorageInsightsTexts {

    static final TextKey TITLE = TextKey.of("jsc.storage_insights.title", "Storage Insights");
    static final TextKey READING = TextKey.of("jsc.storage_insights.reading", "Reading network...");
    static final TextKey SEARCH_ITEM = TextKey.of("jsc.storage_insights.search_item", "search item...");
    static final TextKey TYPES_TILE = TextKey.of("jsc.storage_insights.types_tile", "TYPES %s");
    static final TextKey TOTAL_TILE = TextKey.of("jsc.storage_insights.total_tile", "TOTAL %s");
    static final TextKey LOW_TILE = TextKey.of("jsc.storage_insights.low_tile", "LOW %s");
    static final TextKey TOP_ITEMS = TextKey.of("jsc.storage_insights.top_items", "TOP ITEMS");
    static final TextKey LOADING = TextKey.of("jsc.storage_insights.loading", "loading...");
    static final TextKey NO_MATCH = TextKey.of("jsc.storage_insights.no_match", "no match");
    static final TextKey LOW_STOCK = TextKey.of("jsc.storage_insights.low_stock", "LOW STOCK");
    static final TextKey ALL_STOCKED = TextKey.of("jsc.storage_insights.all_stocked", "all stocked");
    static final TextKey THRESHOLD = TextKey.of("jsc.storage_insights.threshold", "Threshold");
    static final TextKey BY_SERVER = TextKey.of("jsc.storage_insights.by_server", "BY SERVER");
    static final TextKey BACK = TextKey.of("jsc.storage_insights.back", "< Back");
    static final TextKey LOADING_ITEM = TextKey.of("jsc.storage_insights.loading_item", "Loading item...");
    static final TextKey TOTAL = TextKey.of("jsc.storage_insights.total", "%s total");
    static final TextKey STORED_IN = TextKey.of("jsc.storage_insights.stored_in", "STORED IN");
    static final TextKey USED_TO_MAKE = TextKey.of("jsc.storage_insights.used_to_make", "USED TO MAKE");
    static final TextKey NOTHING_ON_NETWORK =
            TextKey.of("jsc.storage_insights.nothing_on_network", "nothing on the network");
    static final TextKey BUSES = TextKey.of("jsc.storage_insights.buses", "BUSES");
    static final TextKey NOT_ON_A_BUS = TextKey.of("jsc.storage_insights.not_on_a_bus", "not on any bus filter");

    private StorageInsightsTexts() {
    }
}
