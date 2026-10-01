/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.grid;

import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.registry.CoreAttachments;
import net.minecraft.server.level.ServerLevel;

/** Where a dimension's grids are found: one of each kind, data's being the data network's own. */
public final class CoreGrids {

    private CoreGrids() {
    }

    /** The grid of {@code kind} in {@code level}. */
    public static Grid of(final ServerLevel level, final GridKind kind) {
        if (kind.carriesNetwork()) {
            return NetworkSystem.get(level).connectivity().grid();
        }
        return level.getData(CoreAttachments.GRIDS).of(kind);
    }
}
