/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import dev.jstech.core.JsCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The kinds the Core gives every mod's structures: the structure type {@code jscore:placed}, the placements
 * {@code jscore:spread} and {@code jscore:fixed}, the piece of a settlement's plan, the processor
 * {@code jscore:weathering}, and the loot entry {@code jscore:optional_item}.
 */
public final class CoreStructures {

    private static final DeferredRegister<StructureType<?>> TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE,
            JsCore.MODID);
    private static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS = DeferredRegister.create(
            Registries.STRUCTURE_PLACEMENT, JsCore.MODID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(
            Registries.STRUCTURE_PIECE, JsCore.MODID);
    private static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(
            Registries.STRUCTURE_PROCESSOR, JsCore.MODID);
    private static final DeferredRegister<LootPoolEntryType> LOOT = DeferredRegister.create(
            Registries.LOOT_POOL_ENTRY_TYPE, JsCore.MODID);

    /** {@code jscore:placed}: see {@link PlacedStructure}. */
    public static final DeferredHolder<StructureType<?>, StructureType<PlacedStructure>> PLACED = TYPES.register(
            "placed", () -> () -> PlacedStructure.CODEC);
    /** {@code jscore:spread}: see {@link SpreadPlacement}. */
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<SpreadPlacement>> SPREAD =
            PLACEMENTS.register("spread", () -> () -> SpreadPlacement.CODEC);
    /** {@code jscore:fixed}: see {@link FixedPlacement}. */
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<FixedPlacement>> FIXED =
            PLACEMENTS.register("fixed", () -> () -> FixedPlacement.CODEC);
    /** {@code jscore:settlement_plan}: see {@link SettlementPlan}. */
    public static final DeferredHolder<StructurePieceType, StructurePieceType> SETTLEMENT_PLAN = PIECES.register(
            "settlement_plan", () -> SettlementPlan::new);
    /** {@code jscore:weathering}: see {@link Weathering}. */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<Weathering.Processor>>
            WEATHERING = PROCESSORS.register("weathering", () -> () -> Weathering.Processor.CODEC);
    /** {@code jscore:optional_item}: see {@link OptionalItemEntry}. */
    public static final DeferredHolder<LootPoolEntryType, LootPoolEntryType> OPTIONAL_ITEM = LOOT.register(
            "optional_item", () -> new LootPoolEntryType(OptionalItemEntry.CODEC));

    private CoreStructures() {
    }

    /** Registers the kinds, and grows the settlements, from the Core's constructor. */
    public static void register(final IEventBus modEventBus) {
        TYPES.register(modEventBus);
        PLACEMENTS.register(modEventBus);
        PIECES.register(modEventBus);
        PROCESSORS.register(modEventBus);
        LOOT.register(modEventBus);
        Settlements.listen();
    }
}
