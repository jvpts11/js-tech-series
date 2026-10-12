/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

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
}
