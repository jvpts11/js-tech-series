/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

/**
 * The available disk sizes, and what each holds at 256 MB an item. The sizes above 8 TB are hard disks only: the
 * helium drives of their years, when flash that large was not sold to put in a computer.
 */
public enum DiskSize {

    GB_500("500g", "500G", 2_000L, false),
    TB_1("1t", "1T", 4_096L, false),
    TB_2("2t", "2T", 8_192L, false),
    TB_4("4t", "4T", 16_384L, false),
    TB_8("8t", "8T", 32_768L, false),
    TB_12("12t", "12T", 49_152L, true),
    TB_16("16t", "16T", 65_536L, true),
    TB_20("20t", "20T", 81_920L, true),
    TB_24("24t", "24T", 98_304L, true);

    private final String id;
    private final String displayName;
    private final long capacityItems;
    private final boolean hardDiskOnly;

    DiskSize(final String id, final String displayName, final long capacityItems, final boolean hardDiskOnly) {
        this.id = id;
        this.displayName = displayName;
        this.capacityItems = capacityItems;
        this.hardDiskOnly = hardDiskOnly;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public long capacityItems() {
        return capacityItems;
    }

    /** Whether a disk of this size is made in that kind: every size is a hard disk, the smaller ones flash too. */
    public boolean comesAs(final StorageTier tier) {
        return !hardDiskOnly || tier == StorageTier.HDD;
    }
}
