/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.blockentity.AccessPointBlockEntity;
import dev.jstech.computers.blockentity.ChargingDockBlockEntity;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.computers.chain.ChainBenches;
import dev.jstech.computers.blockentity.ServerRackPartBlockEntity;
import dev.jstech.computers.blockentity.ComputerThermalBudget;
import dev.jstech.computers.blockentity.UpsBlockEntity;
import dev.jstech.computers.integration.computercraft.ComputerCraftIntegration;
import dev.jstech.computers.rack.RackThermalBudget;
import dev.jstech.core.bigblock.BigBlockPartBlockEntity;
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
        offerEnergy(event, ComputingModule.WAN_GATEWAY_BE.get());
        offerEnergy(event, ComputingModule.MINING_COMPUTER_BE.get());
        offerHeat(event, ComputingModule.PERSONAL_COMPUTER_BE.get());
        offerHeat(event, ComputingModule.SMALL_COMPUTER_BE.get());
        offerHeat(event, ComputingModule.CRAFTING_COMPUTER_BE.get());
        offerHeat(event, ComputingModule.CLUSTER_MANAGEMENT_COMPUTER_BE.get());
        offerHeat(event, ComputingModule.MAINFRAME_BE.get());
        offerHeat(event, ComputingModule.WAN_GATEWAY_BE.get());
        offerThroughParts(event, ComputingModule.MAINFRAME_PART_BE.get());
        offerThroughParts(event, ComputingModule.WAN_GATEWAY_PART_BE.get());
        offerThroughParts(event, ComputingModule.MINING_COMPUTER_PART_BE.get());
        // A UPS takes the line only at its inlet, on its back and sides; the computers against it it feeds itself.
        event.registerBlockEntity(EnergyPorts.PORT, ComputingModule.UPS_BE.get(), UpsBlockEntity::energyPort);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ComputingModule.UPS_BE.get(),
                UpsBlockEntity::feEnergy);
        // An ASIC takes its energy on every face.
        event.registerBlockEntity(EnergyPorts.PORT, ComputingModule.ASIC_BE.get(), (asic, side) -> asic.energyPort());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ComputingModule.ASIC_BE.get(),
                (asic, side) -> asic.feEnergy());
        // An access point takes its energy on the face its cables come in by, and no other.
        event.registerBlockEntity(EnergyPorts.PORT, ComputingModule.ACCESS_POINT_BE.get(),
                AccessPointBlockEntity::energyPort);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ComputingModule.ACCESS_POINT_BE.get(),
                AccessPointBlockEntity::feEnergy);
        // A Mobile Device Charging Dock takes its energy behind it, and nowhere else.
        event.registerBlockEntity(EnergyPorts.PORT, PortableItems.CHARGING_DOCK_BE.get(),
                ChargingDockBlockEntity::energyPort);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PortableItems.CHARGING_DOCK_BE.get(),
                ChargingDockBlockEntity::feEnergy);
        // An Electronics Workbench takes its energy behind it, at the socket on its drawers, and nowhere else.
        event.registerBlockEntity(EnergyPorts.PORT, ChainBenches.BENCH_BE.get(), WorkbenchBlockEntity::energyPort);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ChainBenches.BENCH_BE.get(),
                WorkbenchBlockEntity::feEnergy);
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

    /**
     * Has every computer of {@code type} answer J's Core's thermal budget: its heat against what its case, coolers and
     * fans take away, and its temperature (computers/docs/design/temperature.md). A mod with a computer of its own
     * calls it while the capabilities are registered.
     */
    public static <B extends AbstractComputerBlockEntity> void offerHeat(final RegisterCapabilitiesEvent event,
                                                                       final BlockEntityType<B> type) {
        ThermalBudgets.BUDGET.onBlockEntity(event, type, (computer, side) -> ComputerThermalBudget.of(computer));
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

    /*
     * A cabinet's other blocks take its energy for it and answer for its heat, the Mainframe's and the gateway's alike,
     * on every face.
     */
    private static <P extends BigBlockPartBlockEntity> void offerThroughParts(final RegisterCapabilitiesEvent event,
                                                                             final BlockEntityType<P> parts) {
        ThermalBudgets.BUDGET.onBlockEntity(event, parts, (part, side) -> ComputerThermalBudget.of(cabinetOf(part)));
        event.registerBlockEntity(EnergyPorts.PORT, parts, (part, side) -> {
            final AbstractComputerBlockEntity cabinet = cabinetOf(part);
            return cabinet == null ? null : cabinet.energyPort();
        });
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, parts, (part, side) -> {
            final AbstractComputerBlockEntity cabinet = cabinetOf(part);
            return cabinet == null ? null : cabinet.feEnergy();
        });
    }

    /*
     * The computer a block of it belongs to, a cabinet's or a wide mining rig's, or none while it is bound to no
     * computer that is loaded.
     */
    @Nullable
    private static AbstractComputerBlockEntity cabinetOf(final BigBlockPartBlockEntity part) {
        final BlockPos origin = part.originPos();
        if (origin == null || part.getLevel() == null) {
            return null;
        }
        return Loaded.blockEntity(part.getLevel(), origin) instanceof AbstractComputerBlockEntity computer
                ? computer : null;
    }
}
