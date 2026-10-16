/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.heat;

import dev.jstech.core.JsCore;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

/**
 * Seeing heat: a player who wears an item of {@link #SEES_HEAT} on the head (thermal goggles), or holds one (a thermal
 * camera), sees the world through a thermal view, everything the air's temperature dark and what is warmer or colder
 * than it in false colour, and reads beside the crosshair how hot what they look at is. What is seen is what holds
 * heat ({@link IHeatHolder}), a liquid as hot as its type is, when it is far from the air's, and a living thing at the
 * warmth of its body, the undead as cold as the air, anything hotter while it burns. A mod puts its own goggles in
 * the tag.
 */
public final class ThermalSight {

    /** The items that let a player see heat: worn on the head, or held in a hand. */
    public static final TagKey<Item> SEES_HEAT = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "sees_heat"));
    /** How warm a living thing is seen, in kelvin: a body's warmth. */
    public static final double BODY = 310.0;
    /** How hot a burning thing is seen, in kelvin: a wood fire's flames. */
    public static final double BURNING = 1_100.0;
    /**
     * How far from the air's temperature, in kelvin, a liquid's type must be for it to be seen: a type gives all of
     * its liquid one temperature, the game's water a nominal 300 K, so only a liquid far from the air's is shown, such
     * as lava, a molten metal or a liquefied gas.
     */
    public static final double LIQUID_APART = 50.0;

    private ThermalSight() {
    }

    /** Whether {@code player} sees heat: an item of the tag on the head or in a hand. */
    public static boolean sees(final Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(SEES_HEAT) || player.getMainHandItem().is(SEES_HEAT)
                || player.getOffhandItem().is(SEES_HEAT);
    }

    /** How hot the block at {@code pos} is seen, in kelvin: what it holds, or its liquid; none for the air's. */
    public static OptionalDouble of(final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof IHeatHolder holder) {
            final HeatBody body = holder.heatBody();
            if (body != null && body.known()) {
                return OptionalDouble.of(body.temperature());
            }
        }
        final FluidState fluid = level.getFluidState(pos);
        if (!fluid.isEmpty()) {
            final double kelvin = fluid.getFluidType().getTemperature(fluid, level, pos);
            if (Math.abs(kelvin - HeatClimate.ambient(level, pos)) >= LIQUID_APART) {
                return OptionalDouble.of(kelvin);
            }
        }
        return OptionalDouble.empty();
    }

    /**
     * How hot {@code entity} is seen, in kelvin: a living thing at a body's warmth, but the undead as cold as the air,
     * and anything burning hotter.
     */
    public static OptionalDouble of(final Entity entity) {
        if (entity.isOnFire()) {
            return OptionalDouble.of(BURNING);
        }
        return entity instanceof LivingEntity && !entity.getType().is(EntityTypeTags.UNDEAD)
                ? OptionalDouble.of(BODY) : OptionalDouble.empty();
    }
}
