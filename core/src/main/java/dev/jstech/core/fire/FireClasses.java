/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.fire;

import dev.jstech.core.JsCore;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.hazard.ItemHazards;
import dev.jstech.core.util.Loaded;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * The class of a fire, by the worst of what feeds it: the block it burns on and those round it. A data map of blocks
 * ({@code data/<namespace>/data_maps/block/fire_class.json}) says the class of a fuel; a liquid in the fluid tags
 * {@code jscore:flammable_liquids} or {@code jscore:cooking_oils} is of those; anything that holds energy is
 * electrical while it holds some; a block whose item reacts with water or catches fire in the air is a burning metal;
 * the rest are solids.
 *
 * <pre>{@code
 * { "values": { "mymod:fuel_tank": "liquids", "mymod:fryer": "cooking_oils", "#c:storage_blocks/sodium": "metals" } }
 * }</pre>
 */
public final class FireClasses {

    /** The class of fire each block feeds. */
    public static final DataMapType<Block, FireClass> FIRE_CLASS = DataMapType.builder(id("fire_class"),
            Registries.BLOCK, FireClass.CODEC).build();
    /** The liquids that burn as flammable liquids. */
    public static final TagKey<Fluid> FLAMMABLE_LIQUIDS = TagKey.create(Registries.FLUID, id("flammable_liquids"));
    /** The liquids that burn as cooking oils. */
    public static final TagKey<Fluid> COOKING_OILS = TagKey.create(Registries.FLUID, id("cooking_oils"));
    /* The places a fire feeds on: below it first, then round it and above. */
    private static final Direction[] FED_FROM = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST,
        Direction.WEST, Direction.UP};

    private FireClasses() {
    }

    /** Hands the mod's bus the data map, once, from the fire kit. */
    static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterDataMapTypesEvent.class, event -> event.register(FIRE_CLASS));
    }

    /** The class of the fire at {@code fire}: the worst of what it feeds on. */
    public static FireClass at(final Level level, final BlockPos fire) {
        FireClass worst = FireClass.SOLIDS;
        for (final Direction way : FED_FROM) {
            final BlockPos fuel = fire.relative(way);
            if (!Loaded.at(level, fuel)) {
                continue;
            }
            final FireClass kind = of(level, fuel, level.getBlockState(fuel), way.getOpposite());
            if (kind.worseThan(worst)) {
                worst = kind;
            }
        }
        return worst;
    }

    /** The class of fire the block {@code state} at {@code pos} feeds, touched from its {@code face}. */
    public static FireClass of(final Level level, final BlockPos pos, final BlockState state, final Direction face) {
        final FireClass mapped = state.getBlockHolder().getData(FIRE_CLASS);
        if (mapped != null) {
            return live(level, pos, face) ? worse(mapped, FireClass.ELECTRICAL) : mapped;
        }
        if (reactiveMetal(state)) {
            return FireClass.METALS;
        }
        if (live(level, pos, face)) {
            return FireClass.ELECTRICAL;
        }
        final FluidState fluid = state.getFluidState();
        if (fluid.is(COOKING_OILS)) {
            return FireClass.COOKING_OILS;
        }
        return fluid.is(FLAMMABLE_LIQUIDS) ? FireClass.LIQUIDS : FireClass.SOLIDS;
    }

    /* Whether the block at {@code pos} holds energy now, by the Core's ports or the common energy store. */
    private static boolean live(final Level level, final BlockPos pos, final Direction face) {
        final Optional<IEnergyPort> port = EnergyPorts.at(level, pos, face);
        if (port.isPresent() && port.get().stored() > 0) {
            return true;
        }
        final IEnergyStorage store = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, face);
        return store != null && store.getEnergyStored() > 0;
    }

    /* Whether the block's own item reacts with water or catches fire in the air: sodium, magnesium powder. */
    private static boolean reactiveMetal(final BlockState state) {
        if (state.isAir()) {
            return false;
        }
        final Map<ItemHazards.Kind, Double> hazards = ItemHazards.of(new ItemStack(state.getBlock().asItem()));
        return hazards.containsKey(ItemHazards.WATER_REACTIVE) || hazards.containsKey(ItemHazards.PYROPHORIC);
    }

    private static FireClass worse(final FireClass one, final FireClass other) {
        return one.worseThan(other) ? one : other;
    }

    private static ResourceLocation id(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsCore.MODID, path);
    }
}
