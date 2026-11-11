/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.text.TextKey;

/**
 * How a disk's partitions are written down (computers/docs/design/storage-devices.md): the Master Boot Record of the
 * BIOS machines, which keeps four partitions at most and sees at most 2 TB of a disk, as a real one does, or the GUID
 * Partition Table of the UEFI machines, with room for 128 and no such limit. Pure: no game types.
 */
public enum PartitionScheme implements IStableName {

    MBR("mbr", 4, 2_097_152L),
    GPT("gpt", 128, Long.MAX_VALUE);

    private final String serializedName;
    private final int mostPartitions;
    private final long mostMb;

    private static final StableNames<PartitionScheme> NAMES = StableNames.of(PartitionScheme.class);

    PartitionScheme(final String serializedName, final int mostPartitions, final long mostMb) {
        this.serializedName = serializedName;
        this.mostPartitions = mostPartitions;
        this.mostMb = mostMb;
    }

    /** The scheme called {@code name}; an unknown one reads as the Master Boot Record, the oldest. */
    public static PartitionScheme byName(final String name) {
        return NAMES.byName(name, MBR);
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }

    /** Its name as the tools print it ({@code GUID Partition Table}), in each player's language. */
    public TextKey displayName() {
        return this == GPT ? DiskTexts.GPT : DiskTexts.MBR;
    }

    /** The most partitions it keeps: four on the Master Boot Record. */
    public int mostPartitions() {
        return this.mostPartitions;
    }

    /** How much of a disk of {@code diskMb} megabytes it reaches: 2 TB at most on the Master Boot Record. */
    public long reachOf(final long diskMb) {
        return Math.min(Math.max(0L, diskMb), this.mostMb);
    }
}
