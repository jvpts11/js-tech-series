/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.wireless;

import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import org.jetbrains.annotations.Nullable;

/**
 * The Wi-Fi of each era (computers/docs/design/wireless.md): the standard its access point and its cards speak, how
 * far an access point reaches, how fast it carries and what it draws, each slower than the access cable of its era.
 * The Vintage has no wireless. A link goes at the speed of the older of its two ends, as real Wi-Fi does.
 *
 * <p>Pure: no part of the game, so the rule of the older end is tested on its own.
 */
public enum WirelessStandard implements IStableName {

    LEGACY("legacy", HardwareEra.LEGACY, "802.11b/g", "802.11g", 24, 4, 10),
    TRANSITION("transition", HardwareEra.TRANSITION, "802.11n", "802.11n", 32, 16, 12),
    STANDARD("standard", HardwareEra.STANDARD, "802.11ac", "802.11ac", 48, 64, 15),
    ADVANCED("advanced", HardwareEra.ADVANCED, "Wi-Fi 6", "Wi-Fi 6", 64, 128, 20);

    private final String serializedName;
    private final Era era;
    private final String accessPointName;
    private final String cardName;
    private final int reachBlocks;
    private final int itemsPerTick;
    private final int watts;

    private static final StableNames<WirelessStandard> NAMES = StableNames.of(WirelessStandard.class);

    WirelessStandard(final String serializedName, final Era era, final String accessPointName, final String cardName,
                     final int reachBlocks, final int itemsPerTick, final int watts) {
        this.serializedName = serializedName;
        this.era = era;
        this.accessPointName = accessPointName;
        this.cardName = cardName;
        this.reachBlocks = reachBlocks;
        this.itemsPerTick = itemsPerTick;
        this.watts = watts;
    }

    /**
     * The Wi-Fi of {@code era}, or null for an era before it came: the Vintage has none, and an era after the Advanced
     * speaks the Advanced's until it has its own.
     */
    @Nullable
    public static WirelessStandard of(final Era era) {
        if (era.level() < HardwareEra.LEGACY.level()) {
            return null;
        }
        WirelessStandard found = LEGACY;
        for (final WirelessStandard standard : values()) {
            if (standard.era.level() <= era.level()) {
                found = standard;
            }
        }
        return found;
    }

    /** The older of two ends of a link, whose speed the link goes at. */
    public static WirelessStandard older(final WirelessStandard one, final WirelessStandard other) {
        return one.era.level() <= other.era.level() ? one : other;
    }

    /** The standard of that serialized name; an unknown one is the Legacy's. */
    public static WirelessStandard byName(final String name) {
        return NAMES.byName(name, LEGACY);
    }

    /** The era it came with. */
    public Era era() {
        return era;
    }

    /** What its access point says it speaks: 802.11b/g on the Legacy's. */
    public String accessPointName() {
        return accessPointName;
    }

    /** What a card of its era says it speaks: 802.11g on the Legacy's, the others as their access point. */
    public String cardName() {
        return cardName;
    }

    /** How far its access point reaches, in blocks. */
    public int reachBlocks() {
        return reachBlocks;
    }

    /** How many items a tick a link of this standard carries. */
    public int itemsPerTick() {
        return itemsPerTick;
    }

    /** What its access point draws, in watts. */
    public int watts() {
        return watts;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
