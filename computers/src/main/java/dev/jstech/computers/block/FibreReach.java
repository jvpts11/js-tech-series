/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import dev.jstech.core.tier.HardwareEra;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/**
 * How much of the fibre backbone a server rack's back takes, set by the newest Fibre Optic Interface mounted in it:
 * none without one, the Standard's Fibre Optic Cable with the Standard interface, and the Advanced's OM5 Fibre Cable
 * as well with the Advanced one.
 */
public enum FibreReach implements StringRepresentable {

    NONE("none", null),
    STANDARD("standard", HardwareEra.STANDARD),
    ADVANCED("advanced", HardwareEra.ADVANCED);

    private final String name;
    @Nullable
    private final HardwareEra newest;

    FibreReach(final String name, @Nullable final HardwareEra newest) {
        this.name = name;
        this.newest = newest;
    }

    /** The reach of an interface of {@code era}: the fibre of that era and of every era before it; none for null. */
    public static FibreReach upTo(@Nullable final HardwareEra era) {
        FibreReach reach = NONE;
        for (final FibreReach candidate : values()) {
            if (era != null && candidate.newest != null && candidate.newest.isAtMost(era)) {
                reach = candidate;
            }
        }
        return reach;
    }

    /** Whether a fibre cable of {@code era} goes in. */
    public boolean admits(final HardwareEra era) {
        return newest != null && era.isAtMost(newest);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
