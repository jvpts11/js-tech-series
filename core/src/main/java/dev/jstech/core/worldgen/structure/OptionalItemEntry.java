/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * The loot entry {@code jscore:optional_item}: an item named by its id, which another mod may or may not add; while
 * that mod is installed it is loot like any other, and while it is not, the entry gives nothing and the table still
 * reads. A structure's loot can so hold another mod's things without depending on the mod.
 *
 * <pre>{@code
 * { "type": "jscore:optional_item", "name": "mymod:titanium_ingot", "weight": 3,
 *   "functions": [{"function": "minecraft:set_count", "count": {"min": 1, "max": 4}}] }
 * }</pre>
 */
public final class OptionalItemEntry extends LootPoolSingletonContainer {

    private final ResourceLocation item;

    public static final MapCodec<OptionalItemEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("name").forGetter(entry -> entry.item))
            .and(singletonFields(instance))
            .apply(instance, OptionalItemEntry::new));

    private OptionalItemEntry(final ResourceLocation item, final int weight, final int quality,
                              final List<LootItemCondition> conditions, final List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.item = item;
    }

    /** An entry of the item {@code item}, for a loot table's data generation. */
    public static LootPoolSingletonContainer.Builder<?> of(final ResourceLocation item) {
        return simpleBuilder((weight, quality, conditions, functions) -> new OptionalItemEntry(item, weight, quality,
                conditions, functions));
    }

    /** The item's id. */
    public ResourceLocation item() {
        return this.item;
    }

    @Override
    public LootPoolEntryType getType() {
        return CoreStructures.OPTIONAL_ITEM.get();
    }

    @Override
    protected void createItemStack(final Consumer<ItemStack> stackConsumer, final LootContext lootContext) {
        final Item found = BuiltInRegistries.ITEM.get(this.item);
        if (found != Items.AIR) {
            stackConsumer.accept(new ItemStack(found));
        }
    }
}
