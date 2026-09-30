/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

/**
 * The available disk sizes.
 */
public enum DiskSize {

    GB_500("500g", "500G", 2_000L),
    TB_1("1t", "1T", 4_096L),
    TB_2("2t", "2T", 8_192L),
    TB_4("4t", "4T", 16_384L),
    TB_8("8t", "8T", 32_768L);

    private final String id;
    private final String displayName;
    private final long capacityItems;

    DiskSize(final String id, final String displayName, final long capacityItems) {
        this.id = id;
        this.displayName = displayName;
        this.capacityItems = capacityItems;
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
}
