/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.hazard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * What a worn thing does against the weather: how much heat it lets out over the part of the body it covers, in watts
 * a square metre for each kelvin between the body and the place, and whether it keeps the wind and the water out. A
 * data map of items ({@code data/<namespace>/data_maps/item/clothing.json}) a mod or a data pack fills; a thing worn
 * that the map says nothing of keeps nothing in, as bare skin. A piece worn on the head covers a tenth of the body, on
 * the trunk and arms about a half, on the legs a third and on the feet the rest but the hands.
 *
 * <pre>{@code
 * { "values": { "mymod:fur_coat": { "lets_out": 1.2, "keeps_wind_out": true } } }
 * }</pre>
 */
public final class Clothing {

    /** What each worn thing does against the weather. */
    public static final DataMapType<Item, Piece> CLOTHING = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "clothing"), Registries.ITEM, Piece.CODEC)
            .synced(Piece.CODEC, false).build();
    /* The share of the body each slot's piece covers: the head, the trunk and arms, the legs, the feet. */
    private static final Map<EquipmentSlot, Double> COVERS = Map.of(EquipmentSlot.HEAD, 0.10,
            EquipmentSlot.CHEST, 0.48, EquipmentSlot.LEGS, 0.32, EquipmentSlot.FEET, 0.07);
    private static final double LEAST_LETS_OUT = 0.01;
    private static final double MOST_LETS_OUT = 1_000.0;

    private Clothing() {
    }

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(CLOTHING));
    }

    /** What {@code stack} does against the weather worn, if the map says. */
    public static Optional<Piece> of(final ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : Optional.ofNullable(stack.getItemHolder().getData(CLOTHING));
    }

    /** The share of the body the four worn pieces cover together; the hands and the face stay bare. */
    public static double covered() {
        double all = 0;
        for (final double share : COVERS.values()) {
            all += share;
        }
        return all;
    }

    /** What {@code body} wears against the weather: each piece by the share of the body it covers. */
    public static BodyHeat.Clothes worn(final LivingEntity body) {
        double resistance = 0;
        double windproof = 0;
        double waterproof = 0;
        for (final Map.Entry<EquipmentSlot, Double> slot : COVERS.entrySet()) {
            final Optional<Piece> piece = of(body.getItemBySlot(slot.getKey()));
            if (piece.isEmpty()) {
                continue;
            }
            resistance += slot.getValue() / piece.get().letsOut();
            windproof += piece.get().keepsWindOut() ? slot.getValue() : 0;
            waterproof += piece.get().keepsWaterOut() ? slot.getValue() : 0;
        }
        return new BodyHeat.Clothes(resistance, windproof, waterproof);
    }

    /**
     * What a worn thing does against the weather.
     *
     * @param letsOut       how much heat it lets out, in W/(m²K): the less, the warmer; a leather jacket about six,
     *                      a fur coat one
     * @param keepsWindOut  whether the wind does not press through it
     * @param keepsWaterOut whether it stays dry in water and rain
     */
    public record Piece(double letsOut, boolean keepsWindOut, boolean keepsWaterOut) {

        /** How a piece is written in the data map. */
        public static final Codec<Piece> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(LEAST_LETS_OUT, MOST_LETS_OUT).fieldOf("lets_out").forGetter(Piece::letsOut),
                Codec.BOOL.optionalFieldOf("keeps_wind_out", false).forGetter(Piece::keepsWindOut),
                Codec.BOOL.optionalFieldOf("keeps_water_out", false).forGetter(Piece::keepsWaterOut))
                .apply(instance, Piece::new));
    }
}
