/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.air;

import dev.jstech.core.dimension.Climate;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;

/**
 * Air of its own a body breathes in place of the place's or its room's: a pressure suit's tanks, a diving helmet's
 * line, an oxygen mask. Declared with {@link Lungs#supply}.
 */
@FunctionalInterface
public interface IAirSupply {

    /**
     * The air {@code body} breathes from this supply now, its pressure and what it is made of; none where it breathes
     * the air round it. Asked often, every tick of a body that breathes: it reads, and spends nothing.
     */
    Optional<Climate> air(LivingEntity body);
}
