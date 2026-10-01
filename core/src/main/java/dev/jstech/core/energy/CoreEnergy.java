/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * Energy as the Core keeps it: the registry of the units mods count energy in, FE among them, and the energy an item
 * holds. Machines, cables and items move FE, the game's own; a mod shows its players its own unit, turned from FE by
 * the unit's ratio.
 *
 * <p>An item that holds energy keeps it in the {@link #ENERGY} component and is given the game's energy capability by
 * {@link #holds}, from the mod's own capability registration:
 *
 * <pre>{@code
 * CoreEnergy.holds(event, BATTERY, 100_000L, 1_000, 1_000);
 * }</pre>
 */
@TextHolder
@EventBusSubscriber(modid = JsCore.MODID)
public final class CoreEnergy {

    /** The units' registry's key. */
    public static final ResourceKey<Registry<EnergyUnit>> UNITS_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "energy_unit"));
    /** Every unit energy is counted in. Synced, so a player's game shows the units the server knows. */
    public static final Registry<EnergyUnit> UNITS = new RegistryBuilder<>(UNITS_KEY).sync(true).create();

    private static final DeferredRegister<EnergyUnit> CORE_UNITS = DeferredRegister.create(UNITS_KEY, JsCore.MODID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, JsCore.MODID);
    private static final TextKey FE_NAME = TextKey.of("jscore.energy.fe", "Forge Energy");
    private static final TextKey FE_SYMBOL = TextKey.of("jscore.energy.fe.symbol", "FE");

    /** Forge Energy, the game's own unit, which every other is worth so many of. */
    public static final DeferredHolder<EnergyUnit, EnergyUnit> FE = CORE_UNITS.register("fe",
            () -> new EnergyUnit(FE_NAME, FE_SYMBOL, EnergyRatio.ONE));
    /** The FE an item holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> ENERGY =
            COMPONENTS.registerComponentType("energy", builder -> builder.persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG));

    private CoreEnergy() {
    }

    /** Hands the units and the item energy to the Core's event bus. */
    public static void register(final IEventBus modEventBus) {
        CORE_UNITS.register(modEventBus);
        COMPONENTS.register(modEventBus);
    }

    /**
     * Gives {@code items} the game's energy capability, holding up to {@code capacity} FE in their {@link #ENERGY}
     * component and taking and giving at most {@code maxReceive} and {@code maxExtract} FE at a time.
     */
    public static void holds(final RegisterCapabilitiesEvent event, final ItemLike items, final long capacity,
                             final int maxReceive, final int maxExtract) {
        event.registerItem(Capabilities.EnergyStorage.ITEM,
                (stack, context) -> new ItemEnergyStorage(stack, capacity, maxReceive, maxExtract), items);
    }

    /** How the energy grid of {@code level} moves energy, and what it moved in the last tick. */
    public static LevelEnergy of(final ServerLevel level) {
        return level.getData(CoreAttachments.ENERGY);
    }

    /** What the part of the energy grid the wire at {@code place} is in moved in the last tick; empty for none. */
    public static Optional<EnergyDistributionResult> lastTickAt(final ServerLevel level, final GridPlace place) {
        final OptionalLong number = CoreGrids.places(level).find(place);
        return number.isEmpty() ? Optional.empty() : of(level).lastTickOf(number.getAsLong());
    }

    @SubscribeEvent
    public static void onNewRegistry(final NewRegistryEvent event) {
        event.register(UNITS);
    }

    /* After the machines have ticked, the energy they made and want moves through the wires. */
    @SubscribeEvent
    public static void afterTick(final LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            of(level).tick(level);
        }
    }
}
