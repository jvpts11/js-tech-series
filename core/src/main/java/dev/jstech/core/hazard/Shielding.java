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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Containers that shield what they hold: a data map of items ({@code data/<namespace>/data_maps/item/shielding.json})
 * a mod or a data pack fills with the density of a container's walls and how thick they are, a lead pig's eleven
 * thousand kilograms a cubic metre five centimetres thick. What a container holds, as a shulker box or a bundle holds
 * it, is still carried: its radiation reaches the carrier through the walls, weakened by their density as a block's
 * is ({@link RadiationKind#through}); a container the map says nothing of shields nothing.
 *
 * <pre>{@code
 * { "values": { "mymod:lead_container": { "density": 11340, "thickness": 0.05 } } }
 * }</pre>
 */
public final class Shielding {

    /** The walls of each container that shields what it holds. */
    public static final DataMapType<Item, Wall> SHIELDING = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "shielding"), Registries.ITEM, Wall.CODEC)
            .synced(Wall.CODEC, false).build();

    private Shielding() {
    }

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(SHIELDING));
    }

    /** The walls of {@code stack}, a container that shields what it holds; none for any other thing. */
    public static Optional<Wall> of(final ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : Optional.ofNullable(stack.getItemHolder().getData(SHIELDING));
    }

    /** What {@code stack} holds, as a shulker box or a bundle holds it; nothing for a thing that holds nothing. */
    public static List<ItemStack> contents(final ItemStack stack) {
        final List<ItemStack> held = new ArrayList<>();
        final ItemContainerContents box = stack.get(DataComponents.CONTAINER);
        if (box != null) {
            box.nonEmptyItems().forEach(held::add);
        }
        final BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            bundle.items().forEach(held::add);
        }
        return held;
    }

    /**
     * A container's walls.
     *
     * @param density   what they weigh, in kilograms a cubic metre: lead 11,340, steel 7,850
     * @param thickness how thick they are, in metres
     */
    public record Wall(double density, double thickness) {

        /** How walls are written in the data map. */
        public static final Codec<Wall> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.doubleRange(0.0, 50_000.0).fieldOf("density").forGetter(Wall::density),
                Codec.doubleRange(0.0, 10.0).fieldOf("thickness").forGetter(Wall::thickness))
                .apply(instance, Wall::new));
    }
}
