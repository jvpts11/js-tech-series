/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import org.jetbrains.annotations.Nullable;

/**
 * What a network of each era is built from, in that era's own machines, cables and boards: the Mainframe and its
 * board, the rack, its servers and their board, the routers, the cables of each line, the Personal Computer, its board
 * and monitor, and the systems the era runs. The parts that go on each board are picked by {@link EraParts}. From the
 * Standard on the backbone is fibre: it joins at an optical router, and the Mainframe and the rack take it through an
 * Optical Network Card and a Fibre Optic Interface.
 */
public enum EraKit {

    VINTAGE(HardwareEra.VINTAGE, ComputingModule.VINTAGE_MAINFRAME, HardwareItems.MOTHERBOARD_MTX_VINTAGE,
            ComputingModule.VINTAGE_SERVER_RACK, ComputingModule.VINTAGE_SERVER, HardwareItems.MOTHERBOARD_EEB_VINTAGE,
            ComputingModule.VINTAGE_ROUTER, ComputingModule.VINTAGE_ROUTER, ComputingModule.THIN_COAX_CABLE,
            ComputingModule.THICK_COAX_CABLE, ComputingModule.VINTAGE_PERSONAL_COMPUTER,
            HardwareItems.MOTHERBOARD_BABYAT_VINTAGE, ComputingModule.VINTAGE_MONITOR, null,
            ResourceLocation.fromNamespaceAndPath("jsc", "mc_dos")),
    LEGACY(HardwareEra.LEGACY, ComputingModule.LEGACY_MAINFRAME, HardwareItems.MOTHERBOARD_MTX_LEGACY,
            ComputingModule.LEGACY_SERVER_RACK, ComputingModule.LEGACY_SERVER,
            HardwareItems.MOTHERBOARD_EATX_LEGACY_604, ComputingModule.PERSONAL_ROUTER, ComputingModule.PERSONAL_ROUTER,
            ComputingModule.ETHERNET_CABLE, ComputingModule.HBW_CABLE, ComputingModule.LEGACY_PERSONAL_COMPUTER,
            HardwareItems.MOTHERBOARD_ATX_LEGACY_939, ComputingModule.LEGACY_MONITOR, null,
            ResourceLocation.fromNamespaceAndPath("jsc", "frames_xp")),
    TRANSITION(HardwareEra.TRANSITION, ComputingModule.TRANSITION_MAINFRAME, HardwareItems.MOTHERBOARD_MTX_T,
            ComputingModule.TRANSITION_SERVER_RACK, ComputingModule.TRANSITION_SERVER,
            HardwareItems.MOTHERBOARD_EEB_T_1366, ComputingModule.TRANSITION_ROUTER, ComputingModule.TRANSITION_ROUTER,
            ComputingModule.CAT5E_CABLE, ComputingModule.CX4_CABLE, ComputingModule.TRANSITION_PERSONAL_COMPUTER,
            HardwareItems.MOTHERBOARD_ATX_TRANSITION_775, ComputingModule.TRANSITION_MONITOR, null,
            ResourceLocation.fromNamespaceAndPath("jsc", "frames_7")),
    STANDARD(HardwareEra.STANDARD, ComputingModule.MAINFRAME, ComputingModule.MOTHERBOARD_MTX_S_2011,
            ComputingModule.SERVER_RACK, ComputingModule.SERVER, ComputingModule.MOTHERBOARD_EEB_S_2011,
            ComputingModule.OPTICAL_ROUTER, ComputingModule.STANDARD_ROUTER, ComputingModule.GIGABIT_CABLE,
            ComputingModule.FIBRE_CABLE, ComputingModule.PERSONAL_COMPUTER, HardwareItems.MOTHERBOARD_ATX_STANDARD_1155,
            ComputingModule.MONITOR, ComputingModule.FIBRE_OPTIC_INTERFACE,
            ResourceLocation.fromNamespaceAndPath("jsc", "frames_10")),
    ADVANCED(HardwareEra.ADVANCED, ComputingModule.ADVANCED_MAINFRAME, HardwareItems.MOTHERBOARD_MTX_A_SP3,
            ComputingModule.ADVANCED_SERVER_RACK, ComputingModule.ADVANCED_SERVER, HardwareItems.MOTHERBOARD_EEB_A_SP3,
            ComputingModule.ADVANCED_OPTICAL_ROUTER, ComputingModule.ADVANCED_ROUTER, ComputingModule.CAT6A_CABLE,
            ComputingModule.OM5_CABLE, ComputingModule.ADVANCED_PERSONAL_COMPUTER,
            HardwareItems.MOTHERBOARD_ATX_ADVANCED_1700, ComputingModule.COLOR_MONITOR,
            ComputingModule.ADVANCED_FIBRE_OPTIC_INTERFACE, ResourceLocation.fromNamespaceAndPath("jsc", "frames_11"));

    /** The system every era's Mainframe runs the network with. */
    public static final ResourceLocation NETWORK_OS = ResourceLocation.fromNamespaceAndPath("jsc", "mc_net");

    private final HardwareEra era;
    private final DeferredBlock<? extends Block> mainframe;
    private final DeferredItem<? extends Item> mainframeBoard;
    private final DeferredBlock<? extends Block> rack;
    private final DeferredItem<? extends Item> server;
    private final DeferredItem<? extends Item> serverBoard;
    private final DeferredBlock<? extends Block> junction;
    private final DeferredBlock<? extends Block> router;
    private final CableEntry access;
    private final CableEntry backbone;
    private final DeferredBlock<? extends Block> computer;
    private final DeferredItem<? extends Item> computerBoard;
    private final DeferredBlock<? extends Block> monitor;
    @Nullable
    private final DeferredItem<? extends Item> fibreInterface;
    private final ResourceLocation desktopOs;

    EraKit(final HardwareEra era, final DeferredBlock<? extends Block> mainframe,
           final DeferredItem<? extends Item> mainframeBoard, final DeferredBlock<? extends Block> rack,
           final DeferredItem<? extends Item> server, final DeferredItem<? extends Item> serverBoard,
           final DeferredBlock<? extends Block> junction, final DeferredBlock<? extends Block> router,
           final CableEntry access, final CableEntry backbone, final DeferredBlock<? extends Block> computer,
           final DeferredItem<? extends Item> computerBoard, final DeferredBlock<? extends Block> monitor,
           @Nullable final DeferredItem<? extends Item> fibreInterface, final ResourceLocation desktopOs) {
        this.era = era;
        this.mainframe = mainframe;
        this.mainframeBoard = mainframeBoard;
        this.rack = rack;
        this.server = server;
        this.serverBoard = serverBoard;
        this.junction = junction;
        this.router = router;
        this.access = access;
        this.backbone = backbone;
        this.computer = computer;
        this.computerBoard = computerBoard;
        this.monitor = monitor;
        this.fibreInterface = fibreInterface;
        this.desktopOs = desktopOs;
    }

    public HardwareEra era() {
        return era;
    }

    public Block mainframe() {
        return mainframe.get();
    }

    public Item mainframeBoard() {
        return mainframeBoard.get();
    }

    public Block rack() {
        return rack.get();
    }

    /** The era's 1U server. */
    public Item server() {
        return server.get();
    }

    public Item serverBoard() {
        return serverBoard.get();
    }

    /** Where the backbone meets: the era's router, or from the Standard on the optical router the fibre turns at. */
    public Block junction() {
        return junction.get();
    }

    /** The router that joins the access line to the backbone. */
    public Block router() {
        return router.get();
    }

    public CableEntry access() {
        return access;
    }

    public CableEntry backbone() {
        return backbone;
    }

    /** The era's Personal Computer. */
    public Block computer() {
        return computer.get();
    }

    public Item computerBoard() {
        return computerBoard.get();
    }

    public Block monitor() {
        return monitor.get();
    }

    /** Whether the era's backbone is fibre, which the Mainframe and the rack take only through their adapters. */
    public boolean fibre() {
        return fibreInterface != null;
    }

    /** The Fibre Optic Interface a rack of the era takes the fibre through; null before the Standard. */
    @Nullable
    public Item fibreInterface() {
        return fibreInterface == null ? null : fibreInterface.get();
    }

    /** The desktop system a Personal Computer of the era runs. */
    public ResourceLocation desktopOs() {
        return desktopOs;
    }

    /** The era's 2U storage server. */
    public Item storageServer() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_STORAGE_SERVER.get();
            case LEGACY -> ComputingModule.LEGACY_STORAGE_SERVER.get();
            case TRANSITION -> ComputingModule.TRANSITION_STORAGE_SERVER.get();
            case STANDARD -> ComputingModule.STORAGE_SERVER.get();
            case ADVANCED -> ComputingModule.ADVANCED_STORAGE_SERVER.get();
        };
    }

    /** The era's 2U compute server. */
    public Item computeServer() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_COMPUTE_SERVER.get();
            case LEGACY -> ComputingModule.LEGACY_COMPUTE_SERVER.get();
            case TRANSITION -> ComputingModule.TRANSITION_COMPUTE_SERVER.get();
            case STANDARD -> ComputingModule.COMPUTE_SERVER.get();
            case ADVANCED -> ComputingModule.ADVANCED_COMPUTE_SERVER.get();
        };
    }

    /** The era's Import Bus, which pulls an inventory into the network. */
    public Item importBus() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_IMPORT_BUS_ITEM.get();
            case LEGACY -> ComputingModule.LEGACY_IMPORT_BUS_ITEM.get();
            case TRANSITION -> ComputingModule.TRANSITION_IMPORT_BUS_ITEM.get();
            case STANDARD -> ComputingModule.IMPORT_BUS_ITEM.get();
            case ADVANCED -> ComputingModule.ADVANCED_IMPORT_BUS_ITEM.get();
        };
    }

    /** The era's Export Bus, which fills an inventory from the network. */
    public Item exportBus() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_EXPORT_BUS_ITEM.get();
            case LEGACY -> ComputingModule.LEGACY_EXPORT_BUS_ITEM.get();
            case TRANSITION -> ComputingModule.TRANSITION_EXPORT_BUS_ITEM.get();
            case STANDARD -> ComputingModule.EXPORT_BUS_ITEM.get();
            case ADVANCED -> ComputingModule.ADVANCED_EXPORT_BUS_ITEM.get();
        };
    }

    /** The Vaultis Array of the era's slot: the RAID controller card. */
    public Item array() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VAULTIS_ARRAY_ISA.get();
            case LEGACY -> ComputingModule.VAULTIS_ARRAY_PCI.get();
            case TRANSITION -> ComputingModule.VAULTIS_ARRAY_PCIE2.get();
            case STANDARD -> ComputingModule.VAULTIS_ARRAY_PCIE3.get();
            case ADVANCED -> ComputingModule.VAULTIS_ARRAY_PCIE4.get();
        };
    }

    /** The Vaultis Cache of the era's slot: the read cache card. */
    public Item cache() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VAULTIS_CACHE_ISA.get();
            case LEGACY -> ComputingModule.VAULTIS_CACHE_PCI.get();
            case TRANSITION -> ComputingModule.VAULTIS_CACHE_PCIE2.get();
            case STANDARD -> ComputingModule.VAULTIS_CACHE_PCIE3.get();
            case ADVANCED -> ComputingModule.VAULTIS_CACHE_PCIE4.get();
        };
    }

    /** The era's KVM Switch, which lets one monitor address each machine in the rack. */
    public Item kvmSwitch() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_KVM_SWITCH.get();
            case LEGACY -> ComputingModule.LEGACY_KVM_SWITCH.get();
            case TRANSITION -> ComputingModule.TRANSITION_KVM_SWITCH.get();
            case STANDARD -> ComputingModule.KVM_SWITCH.get();
            case ADVANCED -> ComputingModule.ADVANCED_KVM_SWITCH.get();
        };
    }

    /** The era's Rack UPS. */
    public Item rackUps() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_RACK_UPS.get();
            case LEGACY -> ComputingModule.LEGACY_RACK_UPS.get();
            case TRANSITION -> ComputingModule.TRANSITION_RACK_UPS.get();
            case STANDARD -> ComputingModule.RACK_UPS.get();
            case ADVANCED -> ComputingModule.ADVANCED_RACK_UPS.get();
        };
    }

    /** The era's Cooling Unit. */
    public Item coolingUnit() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_COOLING_UNIT.get();
            case LEGACY -> ComputingModule.LEGACY_COOLING_UNIT.get();
            case TRANSITION -> ComputingModule.TRANSITION_COOLING_UNIT.get();
            case STANDARD -> ComputingModule.COOLING_UNIT.get();
            case ADVANCED -> ComputingModule.ADVANCED_COOLING_UNIT.get();
        };
    }

    /** The era's repeater, which renews the reach of the lines through it. */
    public Block repeater() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_REPEATER.get();
            case LEGACY -> ComputingModule.LEGACY_REPEATER.get();
            case TRANSITION -> ComputingModule.TRANSITION_REPEATER.get();
            case STANDARD -> ComputingModule.STANDARD_REPEATER.get();
            case ADVANCED -> ComputingModule.ADVANCED_REPEATER.get();
        };
    }

    /** The era's Crafting Computer. */
    public Block craftingComputer() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_CRAFTING_COMPUTER.get();
            case LEGACY -> ComputingModule.LEGACY_CRAFTING_COMPUTER.get();
            case TRANSITION -> ComputingModule.TRANSITION_CRAFTING_COMPUTER.get();
            case STANDARD -> ComputingModule.CRAFTING_COMPUTER.get();
            case ADVANCED -> ComputingModule.ADVANCED_CRAFTING_COMPUTER.get();
        };
    }

    /** The era's printer. */
    public Block printer() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_PRINTER.get();
            case LEGACY -> ComputingModule.LEGACY_PRINTER.get();
            case TRANSITION -> ComputingModule.TRANSITION_PRINTER.get();
            case STANDARD -> ComputingModule.PRINTER.get();
            case ADVANCED -> ComputingModule.ADVANCED_PRINTER.get();
        };
    }

    /** The era's Redstone Interface. */
    public Block redstoneInterface() {
        return switch (this) {
            case VINTAGE -> ComputingModule.VINTAGE_REDSTONE_INTERFACE.get();
            case LEGACY -> ComputingModule.LEGACY_REDSTONE_INTERFACE.get();
            case TRANSITION -> ComputingModule.TRANSITION_REDSTONE_INTERFACE.get();
            case STANDARD -> ComputingModule.STANDARD_REDSTONE_INTERFACE.get();
            case ADVANCED -> ComputingModule.ADVANCED_REDSTONE_INTERFACE.get();
        };
    }

    /** The era before this one, whose cables a device of this era still takes; null for the Vintage. */
    @Nullable
    public EraKit previous() {
        return switch (this) {
            case VINTAGE -> null;
            case LEGACY -> VINTAGE;
            case TRANSITION -> LEGACY;
            case STANDARD -> TRANSITION;
            case ADVANCED -> STANDARD;
        };
    }

    /** The optical drive a case of the era takes in a bay; null for the Vintage, whose bays hold no disc. */
    @Nullable
    public Item opticalBayDrive() {
        return switch (this) {
            case VINTAGE -> null;
            case LEGACY -> HardwareItems.INTERNAL_CD_DRIVE.get();
            case TRANSITION, STANDARD -> HardwareItems.INTERNAL_DVD_DRIVE.get();
            case ADVANCED -> HardwareItems.INTERNAL_BLU_RAY_DRIVE.get();
        };
    }
}
