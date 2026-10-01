/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Where a store keeps its ledger between one run of the server and the next: with the world, on a server, so the
 * ledger travels with the save as the recordings do.
 */
interface IMediaLedgerKeeper {

    /** The ledger as it was last kept, or null when it was never kept at all. */
    @Nullable
    List<MediaLedger.Entry> load();

    /** Keeps the ledger, every entry of it, in place of what was kept before. */
    void keep(List<MediaLedger.Entry> entries);
}
