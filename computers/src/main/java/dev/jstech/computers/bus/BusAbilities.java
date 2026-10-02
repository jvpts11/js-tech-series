/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import dev.jstech.core.tier.HardwareEra;

import java.util.EnumSet;
import java.util.Set;

/**
 * What a bus of an era can be set to do, and how fast it moves. The Vintage bus takes one kind at a time, with no
 * filter; the Legacy one filters and has a keep and a max; the Transition one sets them for each item it lists
 * instead; the Standard one has the Legacy's keep and max again, with a priority and conditions; the Advanced one
 * filters by tag and matches loosely as well. What an era can be set to do is what its window shows, so whatever
 * software sets on a bus can be set by hand too; and a bus never moves faster than the cable it sits on carries.
 *
 * @param era          the era of the bus
 * @param itemsPerTick how many items it moves a tick at most, before the cable's cap
 * @param features     what it can be set to do
 */
public record BusAbilities(HardwareEra era, int itemsPerTick, Set<BusFeature> features) {

    /** How many items a filter lists. */
    public static final int FILTER_SLOTS = 5;

    public BusAbilities {
        features = Set.copyOf(features);
    }

    /**
     * The abilities of a bus of {@code era}; the speeds are first estimates. The buses end at the Advanced, so a later
     * era is given what the Advanced bus can do.
     */
    public static BusAbilities of(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> new BusAbilities(era, 1, EnumSet.noneOf(BusFeature.class));
            case LEGACY -> new BusAbilities(era, 8, EnumSet.of(BusFeature.FILTER, BusFeature.QUANTITIES));
            case TRANSITION -> new BusAbilities(era, 16, EnumSet.of(BusFeature.FILTER, BusFeature.ITEM_QUANTITIES));
            case STANDARD -> new BusAbilities(era, 32, EnumSet.of(BusFeature.FILTER, BusFeature.QUANTITIES,
                    BusFeature.PRIORITY, BusFeature.CONDITIONS));
            case ADVANCED, EXA, SINGULARITY -> new BusAbilities(era, 64, EnumSet.of(BusFeature.FILTER,
                    BusFeature.QUANTITIES, BusFeature.PRIORITY, BusFeature.CONDITIONS, BusFeature.TAGS,
                    BusFeature.FUZZY));
        };
    }

    /**
     * The abilities of an External Storage Bus of {@code era}, which moves nothing itself: the network reaches the
     * inventory it faces as storage of its own. The Vintage one shows all of it; the Legacy and the Transition ones
     * what their filter lets through, read and written or only one of the two; the Standard one adds which storage
     * the network fills first; the Advanced one tags and the loose match.
     */
    public static BusAbilities external(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> new BusAbilities(era, 0, EnumSet.noneOf(BusFeature.class));
            case LEGACY, TRANSITION -> new BusAbilities(era, 0, EnumSet.of(BusFeature.FILTER, BusFeature.ACCESS));
            case STANDARD -> new BusAbilities(era, 0, EnumSet.of(BusFeature.FILTER, BusFeature.ACCESS,
                    BusFeature.PRIORITY));
            case ADVANCED, EXA, SINGULARITY -> new BusAbilities(era, 0, EnumSet.of(BusFeature.FILTER,
                    BusFeature.ACCESS, BusFeature.PRIORITY, BusFeature.TAGS, BusFeature.FUZZY));
        };
    }

    /** Whether the bus can be set to do {@code feature}. */
    public boolean can(final BusFeature feature) {
        return features.contains(feature);
    }

    /** How many items it moves a tick on a cable that carries {@code cable}: never more than the cable. */
    public long speedOn(final long cable) {
        return Math.max(0L, Math.min(itemsPerTick, cable));
    }
}
