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

/**
 * What a partition is for (computers/docs/design/storage-devices.md): data, which a system lives on or keeps its files
 * and items on; the small EFI system partition the installers make under UEFI, where the boot entries live; or the
 * tiny boot partition FreeBSD keeps for its loader. Neither of the last two is ever a volume of the network. Pure.
 */
public enum PartitionRole implements IStableName {

    DATA("data"),
    EFI("efi"),
    BOOT("boot");

    private final String serializedName;

    private static final StableNames<PartitionRole> NAMES = StableNames.of(PartitionRole.class);

    PartitionRole(final String serializedName) {
        this.serializedName = serializedName;
    }

    /** The role called {@code name}; an unknown one reads as data. */
    public static PartitionRole byName(final String name) {
        return NAMES.byName(name, DATA);
    }

    @Override
    public String serializedName() {
        return this.serializedName;
    }
}
