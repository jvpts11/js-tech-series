/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * One second of a module working for its wearer, what its effects are told: whom it works for, and the ways an effect
 * acts on them. What the module stops asking for goes: an attribute changed, the second after; an effect given, as it
 * wears off.
 */
public final class ModuleWearing {

    private final LivingEntity wearer;
    private final String socket;
    /* The attributes changed this second, by the id of their change. */
    private final Map<ResourceLocation, Holder<Attribute>> changed;
    private int count;

    /* An effect given lasts this long, in ticks, past the second it is given again in. */
    private static final int EFFECT_TICKS = 50;

    ModuleWearing(final LivingEntity wearer, final String socket,
                  final Map<ResourceLocation, Holder<Attribute>> changed) {
        this.wearer = wearer;
        this.socket = socket;
        this.changed = changed;
    }

    /** Whom the module works for. */
    public LivingEntity wearer() {
        return wearer;
    }

    /** Changes the wearer's {@code attribute} by {@code amount}, as {@code operation} adds it, this second. */
    public void modify(final Holder<Attribute> attribute, final double amount,
                       final AttributeModifier.Operation operation) {
        final AttributeInstance instance = wearer.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Modules.NAMESPACE, socket + "_" + count++);
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
        changed.put(id, attribute);
    }

    /** Gives the wearer {@code effect} at {@code amplifier}, quietly, for as long as the module keeps giving it. */
    public void effect(final Holder<MobEffect> effect, final int amplifier) {
        wearer.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, amplifier, true, false, true));
    }
}
