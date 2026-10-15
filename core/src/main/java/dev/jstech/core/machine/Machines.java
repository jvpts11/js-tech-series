/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.content.ModContent;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.FaceMatterHandler;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.signal.Signals;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * What every machine of every mod offers and suffers alike, with nothing for its mod to register: its port of a class
 * (and an FE view of it, where its energy speaks FE), its tanks of each state of matter, its declared values, and its
 * readings for comparators and sensors; and the explosion that damages it rather than breaking it.
 */
public final class Machines {

    /** The menu every machine opens, behind its five tabs. */
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MENU = CoreItems.CONTENT.menu("machine",
            MachineMenu::new);
    private static final String NETWORK_VERSION = "1";

    private Machines() {
    }

    /** Declares the machines' menu, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, Machines::onRegisterCapabilities);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> ClientPayloads.onMenu(
                event.registrar(NETWORK_VERSION), MachineRecipePayload.TYPE, MachineRecipePayload.STREAM_CODEC,
                MachineMenu.class, (payload, menu, player, level) -> level.getRecipeManager().byKey(payload.recipe())
                        .filter(holder -> holder.value() instanceof ProcessingRecipe)
                        .ifPresent(holder -> MachineFill.fill(player, menu.machine(), processing(holder),
                                payload.all()))));
        NeoForge.EVENT_BUS.addListener(ExplosionEvent.Detonate.class, Machines::onExplosion);
    }

    public static void onRegisterCapabilities(final RegisterCapabilitiesEvent event) {
        for (final ModContent content : ModContent.all()) {
            for (final BlockEntityType<?> type : content.declaredBlockEntityTypes()) {
                register(event, type);
            }
        }
    }

    /** The machine at {@code pos}, if a machine is there. */
    public static Optional<MachineBlockEntity> at(final Level level, final BlockPos pos) {
        return level.getBlockEntity(pos) instanceof MachineBlockEntity machine ? Optional.of(machine)
                : Optional.empty();
    }

    /*
     * An explosion does not break a machine: it takes the machines out of what it breaks and damages them instead,
     * as only overvoltage, an explosion, sabotage or overheating damage a machine.
     */
    private static void onExplosion(final ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> {
            if (event.getLevel().getBlockEntity(pos) instanceof MachineBlockEntity machine) {
                machine.damage(MachineBlockEntity.EXPLOSION_DAMAGE);
                return true;
            }
            return false;
        });
    }

    /* A recipe already known to be a processing machine's. */
    @SuppressWarnings("unchecked")
    private static RecipeHolder<ProcessingRecipe> processing(final RecipeHolder<?> holder) {
        return (RecipeHolder<ProcessingRecipe>) holder;
    }

    @SuppressWarnings("unchecked")
    private static void register(final RegisterCapabilitiesEvent event, final BlockEntityType<?> type) {
        final BlockEntityType<BlockEntity> any = (BlockEntityType<BlockEntity>) type;
        event.registerBlockEntity(EnergyPorts.PORT, any, (entity, side) ->
                entity instanceof MachineBlockEntity machine && open(machine, MachineResource.ENERGY, side)
                        ? machine.power().portStore() : null);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, any, (entity, side) -> {
            if (!(entity instanceof MachineBlockEntity machine)) {
                return null;
            }
            final EnergyStore store = machine.power().portStore();
            return store != null && store.energy().acceptsFe()
                    ? machine.throughFace(Capabilities.EnergyStorage.BLOCK, side, store.asFe()) : null;
        });
        event.registerBlockEntity(IMachineParameters.CAPABILITY, any, (entity, side) ->
                entity instanceof MachineBlockEntity machine ? machine : null);
        event.registerBlockEntity(Signals.OUTPUT, any, (entity, side) ->
                entity instanceof MachineBlockEntity machine ? machine : null);
        for (final MatterState state : MatterState.values()) {
            final Optional<BlockCapability<IMatterHandler, @Nullable Direction>> capability =
                    MatterCapabilities.block(state);
            capability.ifPresent(found -> event.registerBlockEntity(found, any, (entity, side) ->
                    entity instanceof ProcessingMachineBlockEntity machine ? matterOn(machine, state, side) : null));
        }
    }

    /* Whether {@code side} of {@code machine} lets {@code resource} through at all; from inside, always. */
    private static boolean open(final MachineBlockEntity machine, final MachineResource resource,
                                final @Nullable Direction side) {
        return side == null || machine.sides().mode(resource, machine.getBlockState(), side) != FaceMode.NONE;
    }

    /* A machine's tanks of {@code state} as its {@code side} gives them out. */
    private static @Nullable IMatterHandler matterOn(final ProcessingMachineBlockEntity machine,
                                                     final MatterState state, final @Nullable Direction side) {
        final IMatterHandler whole = machine.matter().outside(state);
        final Optional<MachineResource> resource = MachineResource.of(state);
        if (whole == null || side == null || resource.isEmpty()) {
            return whole;
        }
        final FaceMode mode = machine.sides().mode(resource.get(), machine.getBlockState(), side);
        return switch (mode) {
            case NONE -> null;
            case BOTH -> whole;
            case IN, OUT -> new FaceMatterHandler(whole, mode, ItemFilter.EVERYTHING);
        };
    }
}
