/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.api;

import dev.jstech.core.data.DeclaredData;
import dev.jstech.space.JsSpace;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The pressure suits, by id: those declared in code, by J's Space or an add-on, and those a data pack's files say,
 * {@code data/<namespace>/jsspace/suits/}, a file winning over a declaration of its id. A suit is worn when its four
 * pieces are, each in its place.
 */
public final class SpaceSuits {

    private static final DeclaredData<SpaceSuit> SUITS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "suits"), "jsspace/suits", SpaceSuit.CODEC);

    private SpaceSuits() {
    }

    /** Declares the suit of {@code id}, as it is while no data pack says otherwise, while the game loads. */
    public static SpaceSuit declare(final ResourceLocation id, final SpaceSuit suit) {
        return SUITS.declare(id, suit);
    }

    /** Every suit on the side {@code level} is on, by id: a data pack's over the declared. */
    public static Map<ResourceLocation, SpaceSuit> all(final Level level) {
        return SUITS.all(level);
    }

    /** Every suit declared in code, by id. */
    public static Map<ResourceLocation, SpaceSuit> declared() {
        return SUITS.declared();
    }

    /** The suit {@code wearer} has on whole, its four pieces each in its place, if any. */
    public static Optional<SpaceSuit> worn(final LivingEntity wearer) {
        final ResourceLocation head = item(wearer, EquipmentSlot.HEAD);
        final ResourceLocation chest = item(wearer, EquipmentSlot.CHEST);
        final ResourceLocation legs = item(wearer, EquipmentSlot.LEGS);
        final ResourceLocation feet = item(wearer, EquipmentSlot.FEET);
        return all(wearer.level()).values().stream().filter(suit -> suit.helmet().equals(head)
                && suit.chest().equals(chest) && suit.legs().equals(legs) && suit.feet().equals(feet)).findFirst();
    }

    private static ResourceLocation item(final LivingEntity wearer, final EquipmentSlot slot) {
        return BuiltInRegistries.ITEM.getKey(wearer.getItemBySlot(slot).getItem());
    }
}
