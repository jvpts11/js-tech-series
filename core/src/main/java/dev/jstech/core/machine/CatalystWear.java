/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.JsCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * How a catalyst wears. A catalyst must be in the machine for its recipe to run and is not used up, but a real one
 * slowly loses its activity: a catalyst that can be damaged lasts as many recipes as its durability, losing one each
 * time a recipe runs with it, and once worn out becomes its spent form, which the data map
 * {@code jscore:spent_catalysts} of items names ({@code data/<namespace>/data_maps/item/spent_catalysts.json}), or is
 * gone when it names none. A catalyst that cannot be damaged, a die, never wears.
 *
 * <pre>{@code
 * { "values": { "mymod:iron_catalyst": "mymod:spent_iron_catalyst" } }
 * }</pre>
 */
public final class CatalystWear {

    /** What each catalyst becomes once worn out. */
    public static final DataMapType<Item, Item> SPENT = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "spent_catalysts"), Registries.ITEM,
            BuiltInRegistries.ITEM.byNameCodec()).synced(BuiltInRegistries.ITEM.byNameCodec(), false).build();

    private CatalystWear() {
    }

    /** Hands the mod's bus the data map, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(SPENT));
    }

    /** Whether {@code catalyst} wears as it works: whether it can be damaged. */
    public static boolean wears(final ItemStack catalyst) {
        return !catalyst.isEmpty() && catalyst.isDamageableItem();
    }

    /**
     * What {@code catalyst} is after {@code uses} more recipes: itself, worn by as many uses; its spent form once its
     * durability is gone; or nothing, when it has no spent form.
     */
    public static ItemStack worn(final ItemStack catalyst, final int uses) {
        if (!wears(catalyst) || uses <= 0) {
            return catalyst;
        }
        final int damage = (int) Math.min(Integer.MAX_VALUE, (long) catalyst.getDamageValue() + uses);
        if (damage < catalyst.getMaxDamage()) {
            final ItemStack out = catalyst.copy();
            out.setDamageValue(damage);
            return out;
        }
        final Item spent = catalyst.getItemHolder().getData(SPENT);
        return spent == null ? ItemStack.EMPTY : new ItemStack(spent);
    }
}
