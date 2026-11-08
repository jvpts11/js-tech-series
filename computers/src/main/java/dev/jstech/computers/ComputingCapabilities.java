/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MainframePartBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRackPartBlockEntity;
import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import dev.jstech.computers.rack.RackThermalBudget;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.heat.ThermalBudgets;
import dev.jstech.core.signal.Signals;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Exposes the Computing module's block capabilities that differ by side, or that one block answers for another; an
 * inventory, energy store or tank a block offers on every side is declared as an exposed field of its block entity
 * instead.
 */
public final class ComputingCapabilities {

    private ComputingCapabilities() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, ComputingCapabilities::register);
    }

    public static void register(final RegisterCapabilitiesEvent event) {
        // The Gateway's buffer, for the chest, hopper or turtle on its sides, top or bottom.
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ComputingModule.NETWORK_GATEWAY_BE.get(),
                (be, side) -> be.bufferFor(side));
        // The Gateway's ComputerCraft face, only when there is a ComputerCraft to face.
        ComputerCraftIntegration.registerCapabilities(event);
        // A Redstone Interface says what it reads or emits to whatever reads the Core's signals.
        Signals.offer(event, ComputingModule.REDSTONE_INTERFACE_BE.get());
        // A rack's heat as a budget, for whatever reads machines' heat through the Core, asked of any of its blocks.
        ThermalBudgets.BUDGET.onBlockEntity(event, ComputingModule.SERVER_RACK_BE.get(),
                (rack, side) -> new RackThermalBudget(rack));
        ThermalBudgets.BUDGET.onBlockEntity(event, ComputingModule.SERVER_RACK_PART_BE.get(),
                (part, side) -> RackThermalBudget.ofPart(part));
        // What every computer draws its energy through, on every face; the Mainframe's other blocks take it for it.
        offerEnergy(event, ComputingModule.PERSONAL_COMPUTER_BE.get());
        offerEnergy(event, ComputingModule.SMALL_COMPUTER_BE.get());
        offerEnergy(event, ComputingModule.CRAFTING_COMPUTER_BE.get());
        offerEnergy(event, ComputingModule.CLUSTER_MANAGEMENT_COMPUTER_BE.get());
        offerEnergy(event, ComputingModule.MAINFRAME_BE.get());
        event.registerBlockEntity(EnergyPorts.PORT, ComputingModule.MAINFRAME_PART_BE.get(), (part, side) -> {
            final AbstractComputerBlockEntity mainframe = mainframeOf(part);
            return mainframe == null ? null : mainframe.energyPort();
        });
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ComputingModule.MAINFRAME_PART_BE.get(),
                (part, side) -> {
                    final AbstractComputerBlockEntity mainframe = mainframeOf(part);
                    return mainframe == null ? null : mainframe.feEnergy();
                });
        // A rack takes the energy of the machines in it, on any of its blocks.
        event.registerBlockEntity(EnergyPorts.PORT, ComputingModule.SERVER_RACK_BE.get(),
                (rack, side) -> rack.energyPort());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ComputingModule.SERVER_RACK_BE.get(),
                (rack, side) -> rack.feEnergy());
        event.registerBlockEntity(EnergyPorts.PORT, ComputingModule.SERVER_RACK_PART_BE.get(), (part, side) -> {
            final ServerRackBlockEntity rack = rackOf(part);
            return rack == null ? null : rack.energyPort();
        });
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ComputingModule.SERVER_RACK_PART_BE.get(),
                (part, side) -> {
                    final ServerRackBlockEntity rack = rackOf(part);
                    return rack == null ? null : rack.feEnergy();
                });
    }

    /**
     * Has every computer of {@code type} take energy through its power supply, on every face: J's Energy through J's
     * Core's energy port, and FE while the series takes it. A mod with a computer of its own calls it while the
     * capabilities are registered.
     */
    public static <B extends AbstractComputerBlockEntity> void offerEnergy(final RegisterCapabilitiesEvent event,
                                                                         final BlockEntityType<B> type) {
        event.registerBlockEntity(EnergyPorts.PORT, type, (computer, side) -> computer.energyPort());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (computer, side) -> computer.feEnergy());
    }

    /* The rack a block of it belongs to, or none while it is bound to no rack that is loaded. */
    @Nullable
    private static ServerRackBlockEntity rackOf(final ServerRackPartBlockEntity part) {
        final BlockPos origin = part.originPos();
        if (origin == null || part.getLevel() == null) {
            return null;
        }
        return Loaded.blockEntity(part.getLevel(), origin) instanceof ServerRackBlockEntity rack ? rack : null;
    }

    /* The Mainframe a block of it belongs to, or none while it is bound to no Mainframe that is loaded. */
    @Nullable
    private static AbstractComputerBlockEntity mainframeOf(final MainframePartBlockEntity part) {
        final BlockPos origin = part.originPos();
        if (origin == null || part.getLevel() == null) {
            return null;
        }
        return Loaded.blockEntity(part.getLevel(), origin) instanceof MainframeBlockEntity mainframe ? mainframe : null;
    }
}
