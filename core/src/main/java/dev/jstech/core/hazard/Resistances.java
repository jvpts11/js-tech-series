/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.worn.WornItems;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * What share of each kind of harm a thing stops: a worn piece, by the data map {@code jscore:resistances} on items,
 * and a kind of creature, by the same map on entity types (a robot takes no radiation, a creature of ice suffers from
 * heat). The pieces worn add up, never past all of it; a creature's own resistance stops a share of what is left.
 * Damage of a kind is cut by what resists it; a hazard is protected from by the kind it names.
 */
public final class Resistances {

    /** A share of each kind, by the kind's id. */
    public static final Codec<Map<ResourceLocation, Float>> CODEC = Codec.unboundedMap(ResourceLocation.CODEC,
            Codec.floatRange(0F, 1F));
    /** What each item stops of each kind, worn. */
    public static final DataMapType<Item, Map<ResourceLocation, Float>> ITEMS = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "resistances"), Registries.ITEM, CODEC)
            .synced(CODEC, false).build();
    /** What each kind of creature stops of each kind, by itself. */
    public static final DataMapType<EntityType<?>, Map<ResourceLocation, Float>> ENTITIES = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "resistances"), Registries.ENTITY_TYPE, CODEC)
            .synced(CODEC, false).build();
    /* What else a worn stack stops besides its item's, by what is fitted to it, as mods declared. */
    private static final List<IStackResistance> SOURCES = new CopyOnWriteArrayList<>();

    private Resistances() {
    }

    /**
     * Declares {@code source} of what a worn stack stops besides its item's, by what is fitted to that very stack: a
     * suit's module.
     */
    public static void source(final IStackResistance source) {
        SOURCES.add(Objects.requireNonNull(source, "source"));
    }

    /** Hands the buses the data maps and what cuts damage, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> {
            event.register(ITEMS);
            event.register(ENTITIES);
        });
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, Resistances::onDamage);
    }

    /**
     * What share of {@code kind} {@code stack} stops, worn: its item's, and what is fitted to it, never past all of it.
     */
    public static double of(final ItemStack stack, final DamageKind kind) {
        if (stack.isEmpty()) {
            return 0;
        }
        final Map<ResourceLocation, Float> map = stack.getItemHolder().getData(ITEMS);
        double share = map == null ? 0 : map.getOrDefault(kind.id(), 0F);
        for (final IStackResistance source : SOURCES) {
            share += source.share(stack, kind);
        }
        return Math.min(1, share);
    }

    /** Each kind {@code stack} stops some of, and how much, in the order kinds were declared. */
    public static Map<DamageKind, Double> of(final ItemStack stack) {
        final Map<DamageKind, Double> out = new LinkedHashMap<>();
        if (stack.isEmpty()) {
            return out;
        }
        for (final DamageKind kind : DamageKinds.all()) {
            final double share = of(stack, kind);
            if (share > 0) {
                out.put(kind, share);
            }
        }
        return out;
    }

    /** What share of {@code kind} everything {@code entity} wears stops, added up and never past all of it. */
    public static double worn(final LivingEntity entity, final DamageKind kind) {
        double sum = 0;
        for (final ItemStack stack : WornItems.worn(entity)) {
            sum += of(stack, kind);
        }
        return Math.min(1, sum);
    }

    /** What share of {@code kind} {@code entity}'s kind stops by itself. */
    public static double own(final LivingEntity entity, final DamageKind kind) {
        final Map<ResourceLocation, Float> map = entity.getType().builtInRegistryHolder().getData(ENTITIES);
        return map == null ? 0 : map.getOrDefault(kind.id(), 0F);
    }

    /** What share of {@code kind} reaches {@code entity} no more: what it wears, then what it is. */
    public static double total(final LivingEntity entity, final DamageKind kind) {
        return 1 - (1 - worn(entity, kind)) * (1 - own(entity, kind));
    }

    /** Each kind what {@code entity} wears stops some of, and how much in all, for the body's screen. */
    public static Map<DamageKind, Double> worn(final LivingEntity entity) {
        final Map<DamageKind, Double> out = new LinkedHashMap<>();
        for (final DamageKind kind : DamageKinds.all()) {
            final double share = worn(entity, kind);
            if (share > 0) {
                out.put(kind, share);
            }
        }
        return out;
    }

    private static void onDamage(final LivingIncomingDamageEvent event) {
        final Optional<DamageKind> kind = DamageKinds.of(event.getSource());
        if (kind.isEmpty()) {
            return;
        }
        final double stopped = total(event.getEntity(), kind.get());
        if (stopped > 0) {
            event.setAmount((float) (event.getAmount() * (1 - stopped)));
        }
    }
}
