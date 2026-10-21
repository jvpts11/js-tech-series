/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.ApiStatus;

/**
 * Changing what drops as data, with no code: three kinds of the game's modifiers of loot a data pack writes, each with
 * the game's own conditions of loot (the tool used, the dimension or the biome where it happens, the loot table it
 * changes), and, on the file, NeoForge's conditions (an installed mod):
 *
 * <pre>{@code
 * {"type": "jscore:add_drops", "conditions": [{"condition": "neoforge:loot_table_id",
 *   "loot_table_id": "minecraft:blocks/gravel"}], "items": [{"id": "minecraft:flint"}], "chance": 0.25}
 * {"type": "jscore:remove_drops", "conditions": [], "items": {"item": "minecraft:rotten_flesh"}}
 * {"type": "jscore:swap_drops", "conditions": [], "from": {"tag": "c:raw_materials/iron"},
 *   "to": "minecraft:iron_ingot"}
 * }</pre>
 *
 * Each goes in {@code data/<namespace>/loot_modifiers/} and is named in NeoForge's list of modifiers,
 * {@code data/neoforge/loot_modifiers/global_loot_modifiers.json}.
 */
public final class LootChanges {

    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS = DeferredRegister.create(
            NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, JsCore.MODID);

    /** Adds drops: {@code jscore:add_drops}. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AddDrops>> ADD =
            MODIFIERS.register("add_drops", () -> AddDrops.CODEC);
    /** Takes drops away: {@code jscore:remove_drops}. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<RemoveDrops>> REMOVE =
            MODIFIERS.register("remove_drops", () -> RemoveDrops.CODEC);
    /** Swaps one drop for another: {@code jscore:swap_drops}. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<SwapDrops>> SWAP =
            MODIFIERS.register("swap_drops", () -> SwapDrops.CODEC);

    private LootChanges() {
    }

    /** Registers the kinds of modifier, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void register(final IEventBus modBus) {
        MODIFIERS.register(modBus);
    }

    /** Adds {@code items} to what drops, each time or by {@code chance}. */
    public static final class AddDrops extends LootModifier {

        private final List<ItemStack> items;
        private final float chance;

        public static final MapCodec<AddDrops> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
                .and(ItemStack.CODEC.listOf().fieldOf("items").forGetter(add -> add.items))
                .and(Codec.floatRange(0.0f, 1.0f).optionalFieldOf("chance", 1.0f).forGetter(add -> add.chance))
                .apply(instance, AddDrops::new));

        public AddDrops(final LootItemCondition[] conditions, final List<ItemStack> items, final float chance) {
            super(conditions);
            this.items = List.copyOf(items);
            this.chance = chance;
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(final ObjectArrayList<ItemStack> generatedLoot,
                                                     final LootContext context) {
            if (this.chance >= 1.0f || context.getRandom().nextFloat() < this.chance) {
                for (final ItemStack item : this.items) {
                    generatedLoot.add(item.copy());
                }
            }
            return generatedLoot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return ADD.get();
        }
    }

    /** Takes away every drop {@code items} matches. */
    public static final class RemoveDrops extends LootModifier {

        private final Ingredient items;

        public static final MapCodec<RemoveDrops> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
                .and(Ingredient.CODEC.fieldOf("items").forGetter(remove -> remove.items))
                .apply(instance, RemoveDrops::new));

        public RemoveDrops(final LootItemCondition[] conditions, final Ingredient items) {
            super(conditions);
            this.items = items;
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(final ObjectArrayList<ItemStack> generatedLoot,
                                                     final LootContext context) {
            generatedLoot.removeIf(this.items);
            return generatedLoot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return REMOVE.get();
        }
    }

    /** Swaps every drop {@code from} matches for as many of {@code to}. */
    public static final class SwapDrops extends LootModifier {

        private final Ingredient from;
        private final Item to;

        public static final MapCodec<SwapDrops> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
                .and(Ingredient.CODEC.fieldOf("from").forGetter(swap -> swap.from))
                .and(BuiltInRegistries.ITEM.byNameCodec().fieldOf("to").forGetter(swap -> swap.to))
                .apply(instance, SwapDrops::new));

        public SwapDrops(final LootItemCondition[] conditions, final Ingredient from, final Item to) {
            super(conditions);
            this.from = from;
            this.to = to;
        }

        @Override
        protected ObjectArrayList<ItemStack> doApply(final ObjectArrayList<ItemStack> generatedLoot,
                                                     final LootContext context) {
            generatedLoot.replaceAll(stack -> this.from.test(stack) ? stack.transmuteCopy(this.to) : stack);
            return generatedLoot;
        }

        @Override
        public MapCodec<? extends IGlobalLootModifier> codec() {
            return SWAP.get();
        }
    }
}
