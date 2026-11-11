/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.scan;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.jetbrains.annotations.Nullable;

/**
 * The flatbed scanners, one for each era, each the scanner of its day (computers/docs/design/peripherals.md): how long
 * each takes over a page, 8, 4, 2, 1 and half a second from the Vintage on. The seconds are estimates, to settle in
 * play.
 *
 * <p>Pure enum (no Minecraft imports) so the pace is unit-testable.
 */
public enum ScannerModel implements IStableName {

    PAKARD_SCANJOT("pakard_scanjot", HardwareEra.VINTAGE, 160),
    EPSILON_PERFECTA_1240U("epsilon_perfecta_1240u", HardwareEra.LEGACY, 80),
    PAKARD_SCANJOT_G4050("pakard_scanjot_g4050", HardwareEra.TRANSITION, 40),
    EPSILON_PERFECTA_V600("epsilon_perfecta_v600", HardwareEra.STANDARD, 20),
    EPSILON_PERFECTA_V39("epsilon_perfecta_v39", HardwareEra.ADVANCED, 10);

    private static final StableNames<ScannerModel> NAMES = StableNames.of(ScannerModel.class);
    /** Ticks in a second, for a page's pace in words. */
    private static final int TICKS_A_SECOND = 20;

    private final String serializedName;
    private final Era era;
    private final int pageTicks;

    ScannerModel(final String serializedName, final Era era, final int pageTicks) {
        this.serializedName = serializedName;
        this.era = era;
        this.pageTicks = pageTicks;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    public Era era() {
        return era;
    }

    /** How many ticks the scanner takes over a page. */
    public int pageTicks() {
        return pageTicks;
    }

    /** How many seconds a page takes, as its window says it. */
    public double pageSeconds() {
        return (double) pageTicks / TICKS_A_SECOND;
    }

    /** The scanner of {@code era}; an era after the Advanced has the Advanced's. */
    public static ScannerModel of(final Era era) {
        final Era parts = HardwareEra.withParts(era);
        for (final ScannerModel model : values()) {
            if (model.era == parts) {
                return model;
            }
        }
        return EPSILON_PERFECTA_V600;
    }

    /** The scanner a name stands for, or null for a name no scanner declares. */
    @Nullable
    public static ScannerModel find(@Nullable final String name) {
        return NAMES.find(name);
    }
}
