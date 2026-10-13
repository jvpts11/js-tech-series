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

/** The Cluster Manager window's own title, apart from what {@link ClusterManagerTexts} covers. */
@TextHolder
final class ClusterManagerAppTexts {

    static final TextKey TITLE = TextKey.of("jsc.cluster_manager_app.title", "Cluster Manager");

    private ClusterManagerAppTexts() {
    }
}
