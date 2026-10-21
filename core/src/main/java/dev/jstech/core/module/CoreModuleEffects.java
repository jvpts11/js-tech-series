/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * The effects every module's data may name: a change to one of the wearer's attributes, as the game's own attributes
 * name it (its speed, armour, reach, how high it steps), and one of the game's effects, given quietly for as long as
 * the module works (night vision, water breathing, fire resistance).
 */
public final class CoreModuleEffects {

    public static final ModuleEffectType<AttributeChange> ATTRIBUTE = ModuleEffects.declare(new ModuleEffectType<>(
            id("attribute"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute")
                            .forGetter(AttributeChange::attribute),
                    Codec.DOUBLE.fieldOf("amount").forGetter(AttributeChange::amount),
                    AttributeModifier.Operation.CODEC.optionalFieldOf("operation",
                            AttributeModifier.Operation.ADD_VALUE).forGetter(AttributeChange::operation))
                    .apply(instance, AttributeChange::new))));
    public static final ModuleEffectType<GivenEffect> MOB_EFFECT = ModuleEffects.declare(new ModuleEffectType<>(
            id("mob_effect"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(GivenEffect::effect),
                    Codec.intRange(0, 255).optionalFieldOf("amplifier", 0).forGetter(GivenEffect::amplifier))
                    .apply(instance, GivenEffect::new))));

    private CoreModuleEffects() {
    }

    /** Declares the Core's effects, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }

    /** Changes the wearer's {@code attribute} by {@code amount}, as {@code operation} adds it. */
    public record AttributeChange(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation)
            implements IModuleEffect {

        @Override
        public ModuleEffectType<AttributeChange> type() {
            return ATTRIBUTE;
        }

        @Override
        public void apply(final ModuleWearing wearing) {
            wearing.modify(attribute, amount, operation);
        }
    }

    /** Gives the wearer {@code effect} at {@code amplifier}, 0 its first level. */
    public record GivenEffect(Holder<MobEffect> effect, int amplifier) implements IModuleEffect {

        @Override
        public ModuleEffectType<GivenEffect> type() {
            return MOB_EFFECT;
        }

        @Override
        public void apply(final ModuleWearing wearing) {
            wearing.effect(effect, amplifier);
        }
    }
}
