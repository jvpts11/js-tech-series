/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.EntityEntry;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.team.Ownership;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The vehicles, by id: declared in code by their mods, and written by data packs as files under
 * {@code data/<namespace>/jstech/vehicles/}, a file winning over a declaration of its id, so a pack adds a vehicle with
 * no code. Every vehicle is the one entity {@code jscore:vehicle}, of whichever kind it was made, placed from the one
 * item {@code jscore:vehicle} that names its kind; its parts are the item {@code jscore:vehicle_part} naming theirs.
 * Both are listed with the game's tools and utilities, a stack for each kind and part. A vehicle's key is an item its
 * mod declares ({@link VehicleKeyItem}), named by its kind.
 */
public final class Vehicles {

    /** The entity every vehicle is. */
    public static final EntityEntry<VehicleEntity> ENTITY = CoreItems.CONTENT.entity("vehicle", VehicleEntity::new,
                    MobCategory.MISC).size(2, 1.5F).tracking(10, 2).holdsEnergy().named("Vehicle").register();
    /** Which vehicle a vehicle's item places. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<VehicleStack>> VEHICLE =
            CoreItems.CONTENT.component("vehicle", VehicleStack.CODEC, VehicleStack.STREAM_CODEC);
    /** Which part a part's item is. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<VehiclePartStack>> PART =
            CoreItems.CONTENT.component("vehicle_part", VehiclePartStack.CODEC, VehiclePartStack.STREAM_CODEC);
    /** Which vehicle a key opens. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<VehicleKey>> KEY =
            CoreItems.CONTENT.component("vehicle_key", VehicleKey.CODEC, VehicleKey.STREAM_CODEC);
    /** The item every vehicle is placed from. */
    public static final ItemEntry<VehicleItem> ITEM = CoreItems.CONTENT.item("vehicle",
            properties -> new VehicleItem(properties.stacksTo(1))).named("Vehicle").look(IItemLook.HANDMADE)
            .register();
    /** The item every vehicle part is while out of a vehicle. */
    public static final ItemEntry<VehiclePartItem> PART_ITEM = CoreItems.CONTENT.item("vehicle_part",
            properties -> new VehiclePartItem(properties.stacksTo(16))).named("Vehicle Part")
            .look(IItemLook.HANDMADE).register();
    /** Being run over by a vehicle. */
    public static final ResourceKey<DamageType> RUN_OVER = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "run_over"));

    private static final VehicleCatalogue<VehicleDef> CATALOGUE = new VehicleCatalogue<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "vehicles"), "jstech/vehicles", VehicleDef.CODEC);

    private Vehicles() {
    }

    /** Hands the mod's bus the vehicles' capabilities and their places in the creative menu, from the Core. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, Vehicles::registerCapabilities);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, Vehicles::onBuildContents);
    }

    /** Declares the vehicle {@code id}, as it is while no data pack says otherwise; and gives the id. */
    public static ResourceLocation declare(final ResourceLocation id, final VehicleDef def) {
        CATALOGUE.declare(id, def);
        return id;
    }

    /** The vehicle {@code id} on the side {@code level} is on, or none. */
    public static Optional<VehicleDef> get(final Level level, final ResourceLocation id) {
        return CATALOGUE.get(level, id);
    }

    /** The vehicle {@code id} wherever it is known, for what has no level to ask by. */
    public static Optional<VehicleDef> anywhere(final ResourceLocation id) {
        return CATALOGUE.anywhere(id);
    }

    /** Every vehicle on the side {@code level} is on, by id. */
    public static Map<ResourceLocation, VehicleDef> all(final Level level) {
        return CATALOGUE.all(level);
    }

    /** Every vehicle declared in code, by id. */
    public static Map<ResourceLocation, VehicleDef> declared() {
        return CATALOGUE.declared();
    }

    /**
     * Makes a new vehicle of the kind {@code id} at {@code at}, facing {@code yaw}, in the shape {@code variant}, owned
     * by {@code owner}; none when the kind is not known.
     */
    public static Optional<VehicleEntity> make(final ServerLevel level, final ResourceLocation id, final Vec3 at,
                                               final float yaw, final String variant,
                                               final @Nullable ServerPlayer owner) {
        final Optional<VehicleDef> def = get(level, id);
        if (def.isEmpty()) {
            return Optional.empty();
        }
        final VehicleEntity vehicle = new VehicleEntity(ENTITY.get(), level);
        vehicle.moveTo(at.x, at.y, at.z, yaw, 0);
        vehicle.make(id, def.get());
        if (!variant.isEmpty()) {
            vehicle.setVariant(variant);
        }
        if (owner != null) {
            vehicle.setOwnership(Ownership.of(owner.getUUID()));
        }
        level.addFreshEntity(vehicle);
        return Optional.of(vehicle);
    }

    /** A new vehicle of the kind {@code id}, as an item. */
    public static ItemStack item(final ResourceLocation id) {
        final ItemStack stack = new ItemStack(ITEM.get());
        stack.set(VEHICLE.get(), VehicleStack.of(id));
        return stack;
    }

    /** {@code vehicle} as an item, carrying all it is and holds. */
    public static ItemStack itemOf(final VehicleEntity vehicle) {
        final ItemStack stack = new ItemStack(ITEM.get());
        vehicle.defId().ifPresent(id -> stack.set(VEHICLE.get(), new VehicleStack(id, vehicle.variant(),
                Optional.of(vehicle.saveState()))));
        return stack;
    }

    /** The part {@code id}, worn to {@code health}, as an item. */
    public static ItemStack partItem(final ResourceLocation id, final float health) {
        final ItemStack stack = new ItemStack(PART_ITEM.get());
        stack.set(PART.get(), new VehiclePartStack(id, health));
        return stack;
    }

    /** A key cut for {@code vehicle}, of its keys' generation; none when its kind has no key. */
    public static ItemStack keyFor(final VehicleEntity vehicle) {
        final VehicleDef def = vehicle.def();
        if (def == null || def.key().isEmpty()) {
            return ItemStack.EMPTY;
        }
        final Item item = BuiltInRegistries.ITEM.get(def.key().get());
        if (!(item instanceof VehicleKeyItem)) {
            return ItemStack.EMPTY;
        }
        final ItemStack key = new ItemStack(item);
        key.set(KEY.get(), new VehicleKey(vehicle.keyId(), vehicle.keyGeneration(), def.name()));
        return key;
    }

    /** Being run over by {@code vehicle}, driven by {@code driver} when someone drives it. */
    public static DamageSource runOver(final ServerLevel level, final VehicleEntity vehicle,
                                       final @Nullable Entity driver) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(RUN_OVER), vehicle, driver);
    }

    private static void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerEntity(Capabilities.FluidHandler.ENTITY, ENTITY.get(),
                (vehicle, side) -> vehicle.holds().map(VehicleHolds::fluids).orElse(null));
        event.registerEntity(Capabilities.ItemHandler.ENTITY, ENTITY.get(),
                (vehicle, side) -> vehicle.holds().map(VehicleHolds::boot).orElse(null));
    }

    /* A stack of every vehicle and every part the game knows, with the game's tools and utilities. */
    private static void onBuildContents(final BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            return;
        }
        for (final ResourceLocation id : CATALOGUE.allOnClient().keySet()) {
            event.accept(item(id));
        }
        for (final ResourceLocation id : VehicleParts.allOnClient().keySet()) {
            event.accept(partItem(id, 1));
        }
    }
}
