/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.transit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.ItemFilters;
import java.util.Objects;

/**
 * A route of a sending station: what its filter lets through goes to the station of its address, as iron to station 3
 * and coal to station 7.
 *
 * @param filter  what goes this way
 * @param address the station it goes to
 */
public record CapsuleRoute(ItemFilter filter, int address) {

    /** How a route is saved and sent. */
    public static final Codec<CapsuleRoute> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemFilters.CODEC.fieldOf("filter").forGetter(CapsuleRoute::filter),
            Codec.INT.fieldOf("address").forGetter(CapsuleRoute::address)
    ).apply(instance, CapsuleRoute::new));

    public CapsuleRoute {
        Objects.requireNonNull(filter, "filter");
        if (address <= CapsuleStationBlockEntity.NO_ADDRESS) {
            throw new IllegalArgumentException("a route goes to a station's address, not " + address);
        }
    }
}
