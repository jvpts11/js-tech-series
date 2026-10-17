/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * The effects a band of a hazard most often has: one of the game's effects held on the body, and harm of the hazard's
 * own kind, so its death message is the hazard's.
 */
public final class HazardEffects {

    /* How long an effect lasts after it is given, in ticks: a little over the second until it is given again. */
    private static final int HELD_TICKS = 50;

    private HazardEffects() {
    }

    /** One of the game's effects at {@code amplifier} (0 for level I), held on the body while it stands in the band. */
    public static IHazardEffect effect(final Holder<MobEffect> effect, final int amplifier) {
        return new IHazardEffect() {
            @Override
            public void second(final LivingEntity body, final HazardKind kind, final double level) {
                body.addEffect(new MobEffectInstance(effect, HELD_TICKS, amplifier, true, false, true));
            }
        };
    }

    /** Harm of the hazard's own kind, {@code perSecond} half hearts a second. */
    public static IHazardEffect damage(final float perSecond) {
        return new IHazardEffect() {
            @Override
            public void second(final LivingEntity body, final HazardKind kind, final double level) {
                body.hurt(kind.damageSource(body.level()), perSecond);
            }
        };
    }
}
