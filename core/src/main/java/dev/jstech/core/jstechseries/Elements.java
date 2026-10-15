/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.material.OreLook;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * The 118 known elements as materials of the series' catalogue ({@code jscore:iron}), each with its symbol and atomic
 * number, a metal or a gas able to be liquid too, hydrogen and helium plasma as well. Their forms exist only when a mod
 * uses them. Iron, copper and gold keep the game's own things; iron's and tin's ores are drawn in their minerals' colour.
 */
public final class Elements {

    private static final Map<Element, Material> MATERIALS = new EnumMap<>(Element.class);

    static {
        for (final Element element : Element.values()) {
            MATERIALS.put(element, declare(element));
        }
    }

    private Elements() {
    }

    /** Declares the elements, from the Core's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The material of {@code element}. */
    public static Material of(final Element element) {
        return MATERIALS.get(element);
    }

    /** Every element's material, by element. */
    public static Map<Element, Material> all() {
        return Collections.unmodifiableMap(MATERIALS);
    }

    /** The element {@code material} is, or none for a material that is no element of the known table. */
    public static Optional<Element> element(final Material material) {
        for (final Map.Entry<Element, Material> entry : MATERIALS.entrySet()) {
            if (entry.getValue() == material) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private static Material declare(final Element element) {
        final Material.Builder builder = Materials.material(
                ResourceLocation.fromNamespaceAndPath(JsCore.MODID, element.path()), element.kind(),
                element.nameKey()).colour(element.colour())
                .with(MaterialProperties.SYMBOL, element.symbol())
                .with(MaterialProperties.ATOMIC_NUMBER, element.number());
        switch (element.kind()) {
            // A metal melts; a gas is liquefied (liquid nitrogen, liquid oxygen as a propellant).
            case METAL, GAS -> builder.also(MatterState.LIQUID);
            default -> {
            }
        }
        switch (element) {
            // Hydrogen's and helium's plasmas are what a fusion reactor holds.
            case HYDROGEN, HELIUM -> builder.also(MatterState.PLASMA);
            // Iron's ores are drawn in the rust of the game's own iron ore, not in the grey of the metal.
            case IRON -> builder.ore(OreLook.PLAIN.inColour(0xAF8E77))
                    .provided(Forms.INGOT, () -> Items.IRON_INGOT).provided(Forms.NUGGET, () -> Items.IRON_NUGGET)
                    .provided(Forms.BLOCK, () -> Items.IRON_BLOCK).provided(Forms.RAW, () -> Items.RAW_IRON)
                    .providedOre(HostRocks.STONE, () -> Blocks.IRON_ORE)
                    .providedOre(HostRocks.DEEPSLATE, () -> Blocks.DEEPSLATE_IRON_ORE);
            case COPPER -> builder.ore(OreLook.of(OreLook.Pattern.COPPER, 0x3A7663, 0.25, 0.55))
                    .provided(Forms.INGOT, () -> Items.COPPER_INGOT).provided(Forms.BLOCK, () -> Items.COPPER_BLOCK)
                    .provided(Forms.RAW, () -> Items.RAW_COPPER)
                    .providedOre(HostRocks.STONE, () -> Blocks.COPPER_ORE)
                    .providedOre(HostRocks.DEEPSLATE, () -> Blocks.DEEPSLATE_COPPER_ORE);
            case GOLD -> builder.ore(OreLook.of(OreLook.Pattern.GOLD))
                    .provided(Forms.INGOT, () -> Items.GOLD_INGOT).provided(Forms.NUGGET, () -> Items.GOLD_NUGGET)
                    .provided(Forms.BLOCK, () -> Items.GOLD_BLOCK).provided(Forms.RAW, () -> Items.RAW_GOLD)
                    .providedOre(HostRocks.STONE, () -> Blocks.GOLD_ORE)
                    .providedOre(HostRocks.DEEPSLATE, () -> Blocks.DEEPSLATE_GOLD_ORE)
                    .providedOre(HostRocks.NETHERRACK, () -> Blocks.NETHER_GOLD_ORE);
            // Tin's ore is drawn as its mineral, cassiterite, is: dark brown grains.
            case TIN -> builder.ore(OreLook.of(OreLook.Pattern.COAL).inColour(0x5A4434));
            default -> {
            }
        }
        return builder.declare();
    }
}
