/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.air;

import dev.jstech.core.dimension.Climate;
import dev.jstech.core.room.SealedRooms;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.world.entity.LivingEntity;

/**
 * What a body breathes: the air at its eyes, its sealed room's or the place's, unless a mod gives it air of its own
 * ({@link IAirSupply}), a suit's tanks or a mask's line. A body that breathes its own air takes nothing from the room
 * it stands in, and whether it can breathe, and whether the air poisons it, are read from that air. What the supply
 * spends, it spends itself.
 *
 * <pre>{@code
 * Lungs.supply(body -> wearsMask(body)
 *         ? Optional.of(new Climate(30, 20, Optional.of(PURE_OXYGEN), true)) : Optional.empty());
 * }</pre>
 */
public final class Lungs {

    private static final List<IAirSupply> SUPPLIES = new CopyOnWriteArrayList<>();

    private Lungs() {
    }

    /** Declares a supply of air, once, from a mod's constructor. */
    public static void supply(final IAirSupply supply) {
        SUPPLIES.add(supply);
    }

    /** The air of its own {@code body} breathes now, if a supply gives it any. */
    public static Optional<Climate> supplied(final LivingEntity body) {
        for (final IAirSupply supply : SUPPLIES) {
            final Optional<Climate> air = supply.air(body);
            if (air.isPresent()) {
                return air;
            }
        }
        return Optional.empty();
    }

    /** The air {@code body} breathes now: a supply's, else its room's or the place's at its eyes. */
    public static Climate breathed(final LivingEntity body) {
        return supplied(body).orElseGet(() -> SealedRooms.climateOf(body));
    }
}
