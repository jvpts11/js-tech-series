/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.unify;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.ApiStatus;

/**
 * What every loot table drops, as the item that stands for it ({@link Unification}): a block of another mod's tin ore
 * gives the raw tin the world's settings prefer. The Core's data adds it to the game's modifiers of loot, as
 * {@code jscore:unified_drops}; a modpack takes it out as it takes out any.
 */
public final class UnifiedDrops extends LootModifier {

    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> MODIFIERS = DeferredRegister.create(
            NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, JsCore.MODID);
    /** How it is written in a loot modifier's file. */
    public static final MapCodec<UnifiedDrops> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance)
            .apply(instance, UnifiedDrops::new));
    /** The modifier's kind: {@code jscore:unified_drops}. */
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<UnifiedDrops>> KIND =
            MODIFIERS.register("unified_drops", () -> CODEC);

    public UnifiedDrops(final LootItemCondition[] conditions) {
        super(conditions);
    }

    /** Registers the modifier's kind, and forgets the plan as each world stops, from the Core's constructor. */
    @ApiStatus.Internal
    public static void register(final IEventBus modBus) {
        MODIFIERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> Unification.forget());
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return KIND.get();
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(final ObjectArrayList<ItemStack> generatedLoot,
                                                 final LootContext context) {
        generatedLoot.replaceAll(Unification::unified);
        return generatedLoot;
    }
}
