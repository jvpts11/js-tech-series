/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import dev.jstech.computers.chain.ChipClasses;
import dev.jstech.computers.chain.ChipItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * A chip's class on a player's game: the model with the dot of paint on its package while the chip has a class, and
 * the dot's paint and shade in its class's colours.
 */
public final class ChipClassesClient {

    private ChipClassesClient() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterColorHandlersEvent.Item.class, event -> event.register(ChipClassesClient::tint,
                chips()));
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
            for (final Item chip : chips()) {
                ItemProperties.register(chip, ChipClasses.PROPERTY,
                        (stack, level, entity, seed) -> ChipClasses.of(stack) > 0 ? 1F : 0F);
            }
        }));
    }

    /* The model's first layer is the chip as it is; the second the dot's paint, the third its shade. */
    private static int tint(final ItemStack stack, final int layer) {
        return ChipClasses.paint(ChipClasses.of(stack), layer);
    }

    private static Item[] chips() {
        return BuiltInRegistries.ITEM.stream().filter(item -> item instanceof ChipItem).toArray(Item[]::new);
    }
}
