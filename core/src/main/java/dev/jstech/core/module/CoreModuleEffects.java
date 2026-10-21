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
import dev.jstech.core.tool.MiningShape;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * The effects every module's data may name: a change to one of the wearer's attributes, or of the hand holding the
 * tool the module is in, as the game's own attributes name it (its speed, armour, reach, how high it steps, how fast
 * it mines); one of the game's effects, given quietly for as long as a worn module works (night vision, water
 * breathing, fire resistance); and for a tool's module, an area or a vein it mines, and an enchantment it works as
 * though it had (silk touch, fortune).
 */
public final class CoreModuleEffects {

    /* The highest level of an enchantment a module gives, the game's own highest. */
    private static final int MOST_LEVEL = 255;

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
    public static final ModuleEffectType<Mining> MINING = ModuleEffects.declare(new ModuleEffectType<>(
            id("mining"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    MiningShape.CODEC.fieldOf("shape").forGetter(Mining::shape)).apply(instance, Mining::new))));
    public static final ModuleEffectType<Enchanted> ENCHANTMENT = ModuleEffects.declare(new ModuleEffectType<>(
            id("enchantment"), RecordCodecBuilder.mapCodec(instance -> instance.group(
                    ResourceKey.codec(Registries.ENCHANTMENT).fieldOf("enchantment")
                            .forGetter(Enchanted::enchantment),
                    Codec.intRange(1, MOST_LEVEL).optionalFieldOf("level", 1).forGetter(Enchanted::level))
                    .apply(instance, Enchanted::new))));

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

        @Override
        public void shape(final ModuleTool tool) {
            tool.attribute(attribute, amount, operation);
        }
    }

    /** Makes the tool it is in mine {@code shape} besides the block it breaks. */
    public record Mining(MiningShape shape) implements IModuleEffect {

        @Override
        public ModuleEffectType<Mining> type() {
            return MINING;
        }

        @Override
        public void shape(final ModuleTool tool) {
            tool.mines(shape);
        }
    }

    /**
     * Makes the tool it is in work as though enchanted with {@code enchantment} at {@code level}: silk touch, fortune,
     * efficiency, by the key the world's enchantments know them by.
     */
    public record Enchanted(ResourceKey<Enchantment> enchantment, int level) implements IModuleEffect {

        @Override
        public ModuleEffectType<Enchanted> type() {
            return ENCHANTMENT;
        }

        @Override
        public void shape(final ModuleTool tool) {
            tool.enchant(enchantment, level);
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
