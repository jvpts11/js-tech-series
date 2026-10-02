/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import static dev.jstech.computers.registry.ComputingContent.CLUSTER;
import static dev.jstech.computers.registry.ComputingContent.DEVICES;
import static dev.jstech.computers.registry.ComputingContent.DISKS;
import static dev.jstech.computers.registry.ComputingContent.MACHINES;
import static dev.jstech.computers.registry.ComputingContent.MEDIA;
import static dev.jstech.computers.registry.ComputingContent.NETWORK;
import static dev.jstech.computers.registry.ComputingContent.PARTS;
import static dev.jstech.computers.registry.ComputingContent.RACKS;
import static dev.jstech.computers.registry.ComputingContent.RACK_EQUIPMENT;
import static dev.jstech.computers.registry.ComputingContent.SERVERS;

import dev.jstech.computers.advancement.JscTriggers;
import dev.jstech.computers.audio.ComputingAudioDevices;
import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.block.CaseStyle;
import dev.jstech.computers.block.ClusterManagementComputerBlock;
import dev.jstech.computers.block.CraftingComputerBlock;
import dev.jstech.computers.block.CraftingSwitchBlock;
import dev.jstech.computers.block.HbwInterfaceBlock;
import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.block.LegacyMonitorBlock;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.MainframePartBlock;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.block.PatternEncoderBlock;
import dev.jstech.computers.block.PeripheralCableBlock;
import dev.jstech.computers.block.PersonalComputerBlock;
import dev.jstech.computers.block.PersonalRouterBlock;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.ServerRackPartBlock;
import dev.jstech.computers.block.ServerRouterBlock;
import dev.jstech.computers.block.SpeakerBlock;
import dev.jstech.computers.block.SupercomputerRackBlock;
import dev.jstech.computers.block.TankBlock;
import dev.jstech.computers.block.VintageMonitorBlock;
import dev.jstech.computers.block.part.CablePartItem;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingSwitchBlockEntity;
import dev.jstech.computers.blockentity.HbwInterfaceBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.MainframePartBlockEntity;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.blockentity.NetworkGatewayBlockEntity;
import dev.jstech.computers.blockentity.PatternEncoderBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.PersonalRouterBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerRackPartBlockEntity;
import dev.jstech.computers.blockentity.ServerRouterBlockEntity;
import dev.jstech.computers.blockentity.SpeakerBlockEntity;
import dev.jstech.computers.blockentity.TankBlockEntity;
import dev.jstech.computers.hardware.ClusterInterfaceCardSpec;
import dev.jstech.computers.hardware.CpuSocketId;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.Microarchitectures;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.computers.hardware.PhiCoprocessorSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.item.CabinetBlockItem;
import dev.jstech.computers.item.ClusterInterfaceCardItem;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.CraftingCardItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.MainframeBlockItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.PhiCoprocessorItem;
import dev.jstech.computers.item.PsuItem;
import dev.jstech.computers.item.RackGadgetItem;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.computers.item.ServerCaseItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaBay;
import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaReaderBlock;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.CableBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLines;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.tier.IndustrialTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The catalogue of J's Computers: every block and item it adds, each declared once with everything said about it,
 * in the order its tab shows them, and the block entities its blocks make, beside the blocks that make them. The
 * data the items carry is in {@link ComputingComponents}, the menus in {@link ComputingMenus}, the tab and its
 * sections in {@link ComputingContent}, and the hardware of every era in {@link HardwareItems}.
 *
 * <p>A family of alike things (the cables, the computers of each era) shares a helper that says what they have in
 * common; each declaration then names its own.
 */
public final class ComputingModule {

    private static final ModContent CONTENT = ComputingContent.CONTENT;

    /** A body its block entity draws: the blocks show nothing but the particles a break scatters. */
    private static final IBlockLook MAINFRAME_BODY =
            IBlockLook.fixed(new IBlockModel.ParticleOnly("mainframe_cabinet", "block/mainframe_particle"));
    private static final IBlockLook RACK_BODY =
            IBlockLook.fixed(new IBlockModel.ParticleOnly("rack", "block/rack_particle"));
    private static final IBlockLook ENCODER_BODY =
            IBlockLook.fixed(new IBlockModel.ParticleOnly("pattern_encoder_body", "block/pattern_encoder_particle"));

    /**
     * How a rack cabinet sits in an item slot: 3 blocks tall is its longest side, and its model spans
     * x -1.5..0.5, y 0..3, z -0.5..1.5 blocks around the controller, so its middle moves by this much.
     */
    private static final CabinetBlockItem.Fit RACK_FIT = new CabinetBlockItem.Fit(48.0F, 0.5F, -1.5F, -0.5F);
    /** A rack's parts shown only when fitted or lit: the units seated in its rows and its light bars. */
    private static final List<String> RACK_FITTED = List.of("row_", "lightbar_");

    /**
     * How a device's body (an encoder, a drive) sits in an item slot: the body is one full block (x -0.5..0.5,
     * y 0..1, z -0.5..0.5 around its origin), so only the height needs re-centring.
     */
    private static final CabinetBlockItem.Fit DEVICE_FIT = new CabinetBlockItem.Fit(16.0F, 0.0F, -0.5F, 0.0F);
    /** A device's lamps, dark on its item, whatever the devices in the world show. */
    private static final List<String> DEVICE_LAMPS = List.of(MediaBay.POWER_LAMP, MediaBay.BUSY_LAMP);
    /** How thick the long distance line is, in pixels; every other data cable is four. */
    private static final int LONG_DISTANCE_PIXELS = 6;
    /** A small computer's lamps, dark on its item. */
    private static final List<String> COMPUTER_LAMPS =
            List.of(ComputingLooks.COMPUTER_POWER_LAMP, ComputingLooks.COMPUTER_DISK_LAMP);

    /*
     * Cables. Each data line has a cable of its own in each era, laid in the Core's cable block in the lane of its
     * line: access top left, backbone top middle, compute in the middle, crafting middle right. The long distance
     * line is thicker and never shares a block. The Crafting cable links a Crafting Switch to its computer, one cable
     * for every era.
     */

    public static final CableEntry THIN_COAX_CABLE = dataCable("thin_coax_cable", DataLine.ACCESS,
            HardwareEra.VINTAGE, "thin_coax", "bnc").named("Thin Coaxial Cable").tab(NETWORK).register();
    public static final CableEntry ETHERNET_CABLE = dataCable("ethernet_cable", DataLine.ACCESS, HardwareEra.LEGACY,
            "ethernet", "rj45").named("Ethernet Cable").tab(NETWORK).register();
    public static final CableEntry CAT5E_CABLE = dataCable("cat5e_cable", DataLine.ACCESS, HardwareEra.TRANSITION,
            "cat5e", "rj45_boot").named("Cat 5e Cable").tab(NETWORK).register();
    public static final CableEntry GIGABIT_CABLE = dataCable("gigabit_cable", DataLine.ACCESS, HardwareEra.STANDARD,
            "gigabit", "rj45_snagless").named("Gigabit Ethernet Cable").tab(NETWORK).register();
    public static final CableEntry CAT6A_CABLE = dataCable("cat6a_cable", DataLine.ACCESS, HardwareEra.ADVANCED,
            "cat6a", "rj45_shielded").named("Cat 6a Cable").tab(NETWORK).register();
    public static final CableEntry THICK_COAX_CABLE = dataCable("thick_coax_cable", DataLine.BACKBONE,
            HardwareEra.VINTAGE, "thick_coax", "tap").named("Thick Coaxial Cable").tab(NETWORK).register();
    public static final CableEntry HBW_CABLE = dataCable("hbw_cable", DataLine.BACKBONE, HardwareEra.LEGACY, "hbw",
            "hbw").named("HBW Cable").tab(NETWORK).register();
    public static final CableEntry CX4_CABLE = dataCable("cx4_cable", DataLine.BACKBONE, HardwareEra.TRANSITION,
            "cx4", "cx4").named("10GBASE-CX4 Cable").tab(NETWORK).register();
    public static final CableEntry FIBRE_CABLE = dataCable("fibre_cable", DataLine.BACKBONE, HardwareEra.STANDARD,
            "fibre", "lc").named("Fibre Optic Cable").tab(NETWORK).register();
    public static final CableEntry OM5_CABLE = dataCable("om5_cable", DataLine.BACKBONE, HardwareEra.ADVANCED, "om5",
            "mpo").named("OM5 Fibre Cable").tab(NETWORK).register();
    public static final CableEntry TELEPHONE_LINE = dataCable("telephone_line", DataLine.LONG_DISTANCE,
            HardwareEra.VINTAGE, "telephone", "rj11").named("Telephone Line").tab(NETWORK).register();
    public static final CableEntry LEASED_LINE = dataCable("leased_line", DataLine.LONG_DISTANCE, HardwareEra.LEGACY,
            "leased", "rj48").named("Leased Line").tab(NETWORK).register();
    public static final CableEntry T3_LINE = dataCable("t3_line", DataLine.LONG_DISTANCE, HardwareEra.TRANSITION,
            "t3", "bnc_pair").named("T3 Line").tab(NETWORK).register();
    public static final CableEntry VLDC_CABLE = dataCable("vldc_cable", DataLine.LONG_DISTANCE, HardwareEra.STANDARD,
            "vldc", "vldc").named("VLDC Cable").tab(NETWORK).register();
    public static final CableEntry DARK_FIBRE_CABLE = dataCable("dark_fibre_cable", DataLine.LONG_DISTANCE,
            HardwareEra.ADVANCED, "dark_fibre", "sc_duplex").named("Dark Fibre Cable").tab(NETWORK).register();
    public static final CableEntry INFINIBAND_CABLE = dataCable("infiniband_cable", DataLine.HPC,
            HardwareEra.TRANSITION, "ib_qdr", "qsfp_bail").named("InfiniBand Cable").tab(CLUSTER).register();
    public static final CableEntry HPC_CABLE = dataCable("hpc_cable", DataLine.HPC, HardwareEra.STANDARD, "hpc",
            "qsfp").named("High Compute Cable").tab(CLUSTER).register();
    public static final CableEntry OSFP_CABLE = dataCable("osfp_cable", DataLine.HPC, HardwareEra.ADVANCED,
            "osfp_dac", "osfp").named("OSFP Cable").tab(CLUSTER).register();
    public static final CableEntry CRAFTING_CABLE = dataCable("crafting_cable", DataLine.CRAFTING,
            HardwareEra.VINTAGE, "crafting", "crafting").named("Crafting Cable").tab(CLUSTER).register();
    public static final BlockEntry<PeripheralCableBlock> PERIPHERAL_CABLE =
            cable("peripheral_cable", PeripheralCableBlock::new, NETWORK).named("Peripheral Cable").register();

    // Routers: the facing carries the port panel; the Server Router's back is its Mainframe uplink.

    public static final BlockEntry<PersonalRouterBlock> PERSONAL_ROUTER =
            CONTENT.block("personal_router", PersonalRouterBlock::new)
                    .properties(properties -> properties.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.5F)
                            .sound(SoundType.METAL).noOcclusion())
                    .named("Personal Router").look(IBlockLook::orientable).item().tab(NETWORK).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PersonalRouterBlockEntity>>
            PERSONAL_ROUTER_BE =
            CONTENT.blockEntity("personal_router", PersonalRouterBlockEntity::new, PERSONAL_ROUTER);

    public static final BlockEntry<ServerRouterBlock> SERVER_ROUTER =
            CONTENT.block("server_router", ServerRouterBlock::new)
            .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(0.6F).sound(SoundType.METAL)
                    .noOcclusion())
            .named("Server Router")
            .look(IBlockLook.facing(new IBlockModel.SixFaces("server_router", "block/server_router_top",
                    "block/server_router_top", "block/server_router_front", "block/server_router_back",
                    "block/server_router_side", "block/server_router_side", "block/server_router_side")))
            .item().tab(NETWORK).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerRouterBlockEntity>> SERVER_ROUTER_BE =
            CONTENT.blockEntity("server_router", ServerRouterBlockEntity::new, SERVER_ROUTER);

    /*
     * Mainframes: the same orchestrator and block entity in every era, differing by era, accepted board, cabinet and
     * skin. A 3x2x2 cabinet drawn as one model by the controller; the controller hands its item over itself when
     * broken, and the part blocks of the structure are taken down without drops.
     */

    public static final BlockEntry<MainframeBlock> MAINFRAME =
            mainframe("mainframe", HardwareEra.STANDARD).named("Mainframe").register();
    public static final BlockEntry<MainframeBlock> VINTAGE_MAINFRAME =
            mainframe("vintage_mainframe", HardwareEra.VINTAGE).named("Vintage Mainframe").register();
    public static final BlockEntry<MainframeBlock> LEGACY_MAINFRAME =
            mainframe("legacy_mainframe", HardwareEra.LEGACY).named("Legacy Mainframe").register();
    public static final BlockEntry<MainframeBlock> TRANSITION_MAINFRAME =
            mainframe("transition_mainframe", HardwareEra.TRANSITION).named("Transition Mainframe").register();
    public static final BlockEntry<MainframeBlock> ADVANCED_MAINFRAME =
            mainframe("advanced_mainframe", HardwareEra.ADVANCED).named("Advanced Mainframe").register();
    public static final BlockEntry<MainframePartBlock> MAINFRAME_PART =
            CONTENT.block("mainframe_part", MainframePartBlock::new).properties(ComputingModule::mainframeProperties)
                    .named("Mainframe").look(MAINFRAME_BODY).tag(BlockTags.MINEABLE_WITH_PICKAXE).register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MainframeBlockEntity>> MAINFRAME_BE =
            CONTENT.blockEntity("mainframe", MainframeBlockEntity::new, MAINFRAME, VINTAGE_MAINFRAME, LEGACY_MAINFRAME,
                    TRANSITION_MAINFRAME, ADVANCED_MAINFRAME);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MainframePartBlockEntity>>
            MAINFRAME_PART_BE = CONTENT.blockEntity("mainframe_part", MainframePartBlockEntity::new, MAINFRAME_PART);

    // Monitors: a screen meant to be looked at, so its front faces the player who placed it.

    public static final BlockEntry<MonitorBlock> MONITOR =
            monitor("monitor", MonitorBlock::new).named("Monitor").register();
    public static final BlockEntry<VintageMonitorBlock> VINTAGE_MONITOR =
            monitor("vintage_monitor", VintageMonitorBlock::new).named("Vintage Monitor").register();
    public static final BlockEntry<LegacyMonitorBlock> LEGACY_MONITOR =
            monitor("legacy_monitor", LegacyMonitorBlock::new).named("Legacy Monitor").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MonitorBlockEntity>> MONITOR_BE =
            CONTENT.blockEntity("monitor", MonitorBlockEntity::new, MONITOR, VINTAGE_MONITOR, LEGACY_MONITOR);

    /* Tank: glass walls in a metal casing frame, so it reads as a containment vessel rather than a solid block. */
    public static final BlockEntry<TankBlock> TANK = CONTENT.block("tank", TankBlock::new)
            .properties(properties -> properties.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6F)
                    .sound(SoundType.GLASS).noOcclusion())
            .named("Tank")
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("tank", "minecraft:block/glass",
                    "block/mainframe_side", "block/mainframe_side", "cutout")))
            .item().tab(MACHINES).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_BE =
            CONTENT.blockEntity("tank", TankBlockEntity::new, TANK);

    /*
     * The small computers. Each machine comes in the tower of each age up to the Transition, and from the Standard
     * age on in three cases, a block each: Neutral (plain id, as the case of the age with nothing added), High
     * Performance and Aesthetic. The case is its look alone; the machine, its block entity and what it takes are
     * the same in all of them.
     */

    public static final BlockEntry<PersonalComputerBlock> PERSONAL_COMPUTER =
            personalComputer("personal_computer", HardwareEra.STANDARD, CaseStyle.NEUTRAL)
                    .named("Personal Computer").register();
    public static final BlockEntry<PersonalComputerBlock> HIGH_PERFORMANCE_PERSONAL_COMPUTER =
            personalComputer("high_performance_personal_computer", HardwareEra.STANDARD, CaseStyle.HIGH_PERFORMANCE)
                    .named("High Performance Personal Computer").register();
    public static final BlockEntry<PersonalComputerBlock> AESTHETIC_PERSONAL_COMPUTER =
            personalComputer("aesthetic_personal_computer", HardwareEra.STANDARD, CaseStyle.AESTHETIC)
                    .named("Aesthetic Personal Computer").register();
    public static final BlockEntry<PersonalComputerBlock> VINTAGE_PERSONAL_COMPUTER =
            personalComputer("vintage_personal_computer", HardwareEra.VINTAGE, CaseStyle.SOLE)
                    .named("Vintage Personal Computer").register();
    public static final BlockEntry<PersonalComputerBlock> LEGACY_PERSONAL_COMPUTER =
            personalComputer("legacy_personal_computer", HardwareEra.LEGACY, CaseStyle.SOLE)
                    .named("Legacy Personal Computer").register();
    public static final BlockEntry<PersonalComputerBlock> TRANSITION_PERSONAL_COMPUTER =
            personalComputer("transition_personal_computer", HardwareEra.TRANSITION, CaseStyle.SOLE)
                    .named("Transition Personal Computer").register();
    public static final BlockEntry<PersonalComputerBlock> ADVANCED_PERSONAL_COMPUTER =
            personalComputer("advanced_personal_computer", HardwareEra.ADVANCED, CaseStyle.NEUTRAL)
                    .named("Advanced Personal Computer").register();
    public static final BlockEntry<PersonalComputerBlock> ADVANCED_HIGH_PERFORMANCE_PERSONAL_COMPUTER =
            personalComputer("advanced_high_performance_personal_computer", HardwareEra.ADVANCED,
                    CaseStyle.HIGH_PERFORMANCE).named("Advanced High Performance Personal Computer").register();
    public static final BlockEntry<PersonalComputerBlock> ADVANCED_AESTHETIC_PERSONAL_COMPUTER =
            personalComputer("advanced_aesthetic_personal_computer", HardwareEra.ADVANCED, CaseStyle.AESTHETIC)
                    .named("Advanced Aesthetic Personal Computer").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PersonalComputerBlockEntity>>
            PERSONAL_COMPUTER_BE = CONTENT.blockEntity("personal_computer", PersonalComputerBlockEntity::new,
                    PERSONAL_COMPUTER, HIGH_PERFORMANCE_PERSONAL_COMPUTER, AESTHETIC_PERSONAL_COMPUTER,
                    VINTAGE_PERSONAL_COMPUTER, LEGACY_PERSONAL_COMPUTER, TRANSITION_PERSONAL_COMPUTER,
                    ADVANCED_PERSONAL_COMPUTER, ADVANCED_HIGH_PERFORMANCE_PERSONAL_COMPUTER,
                    ADVANCED_AESTHETIC_PERSONAL_COMPUTER);

    // Crafting Computer: an ATX computer that executes recipes once a Crafting Card is installed.
    public static final BlockEntry<CraftingComputerBlock> CRAFTING_COMPUTER =
            craftingComputer("crafting_computer", HardwareEra.STANDARD, CaseStyle.NEUTRAL)
                    .named("Crafting Computer").register();
    public static final BlockEntry<CraftingComputerBlock> HIGH_PERFORMANCE_CRAFTING_COMPUTER =
            craftingComputer("high_performance_crafting_computer", HardwareEra.STANDARD, CaseStyle.HIGH_PERFORMANCE)
                    .named("High Performance Crafting Computer").register();
    public static final BlockEntry<CraftingComputerBlock> AESTHETIC_CRAFTING_COMPUTER =
            craftingComputer("aesthetic_crafting_computer", HardwareEra.STANDARD, CaseStyle.AESTHETIC)
                    .named("Aesthetic Crafting Computer").register();
    public static final BlockEntry<CraftingComputerBlock> VINTAGE_CRAFTING_COMPUTER =
            craftingComputer("vintage_crafting_computer", HardwareEra.VINTAGE, CaseStyle.SOLE)
                    .named("Vintage Crafting Computer").register();
    public static final BlockEntry<CraftingComputerBlock> LEGACY_CRAFTING_COMPUTER =
            craftingComputer("legacy_crafting_computer", HardwareEra.LEGACY, CaseStyle.SOLE)
                    .named("Legacy Crafting Computer").register();
    public static final BlockEntry<CraftingComputerBlock> TRANSITION_CRAFTING_COMPUTER =
            craftingComputer("transition_crafting_computer", HardwareEra.TRANSITION, CaseStyle.SOLE)
                    .named("Transition Crafting Computer").register();
    public static final BlockEntry<CraftingComputerBlock> ADVANCED_CRAFTING_COMPUTER =
            craftingComputer("advanced_crafting_computer", HardwareEra.ADVANCED, CaseStyle.NEUTRAL)
                    .named("Advanced Crafting Computer").register();
    public static final BlockEntry<CraftingComputerBlock> ADVANCED_HIGH_PERFORMANCE_CRAFTING_COMPUTER =
            craftingComputer("advanced_high_performance_crafting_computer", HardwareEra.ADVANCED,
                    CaseStyle.HIGH_PERFORMANCE).named("Advanced High Performance Crafting Computer").register();
    public static final BlockEntry<CraftingComputerBlock> ADVANCED_AESTHETIC_CRAFTING_COMPUTER =
            craftingComputer("advanced_aesthetic_crafting_computer", HardwareEra.ADVANCED, CaseStyle.AESTHETIC)
                    .named("Advanced Aesthetic Crafting Computer").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingComputerBlockEntity>>
            CRAFTING_COMPUTER_BE = CONTENT.blockEntity("crafting_computer", CraftingComputerBlockEntity::new,
                    CRAFTING_COMPUTER, HIGH_PERFORMANCE_CRAFTING_COMPUTER, AESTHETIC_CRAFTING_COMPUTER,
                    VINTAGE_CRAFTING_COMPUTER, LEGACY_CRAFTING_COMPUTER, TRANSITION_CRAFTING_COMPUTER,
                    ADVANCED_CRAFTING_COMPUTER, ADVANCED_HIGH_PERFORMANCE_CRAFTING_COMPUTER,
                    ADVANCED_AESTHETIC_CRAFTING_COMPUTER);

    /*
     * Cluster Management Computer: a full computer that, with a Cluster Interface Card, drives every
     * supercomputer fabric and datacenter section on its network as one machine.
     */
    public static final BlockEntry<ClusterManagementComputerBlock> CLUSTER_MANAGEMENT_COMPUTER =
            clusterManagementComputer("cluster_management_computer", HardwareEra.STANDARD, CaseStyle.NEUTRAL)
                    .named("Cluster Management Computer").register();
    public static final BlockEntry<ClusterManagementComputerBlock> HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER =
            clusterManagementComputer("high_performance_cluster_management_computer", HardwareEra.STANDARD,
                    CaseStyle.HIGH_PERFORMANCE).named("High Performance Cluster Management Computer").register();
    public static final BlockEntry<ClusterManagementComputerBlock> AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER =
            clusterManagementComputer("aesthetic_cluster_management_computer", HardwareEra.STANDARD,
                    CaseStyle.AESTHETIC).named("Aesthetic Cluster Management Computer").register();
    public static final BlockEntry<ClusterManagementComputerBlock> VINTAGE_CLUSTER_MANAGEMENT_COMPUTER =
            clusterManagementComputer("vintage_cluster_management_computer", HardwareEra.VINTAGE, CaseStyle.SOLE)
                    .named("Vintage Cluster Management Computer").register();
    public static final BlockEntry<ClusterManagementComputerBlock> LEGACY_CLUSTER_MANAGEMENT_COMPUTER =
            clusterManagementComputer("legacy_cluster_management_computer", HardwareEra.LEGACY, CaseStyle.SOLE)
                    .named("Legacy Cluster Management Computer").register();
    public static final BlockEntry<ClusterManagementComputerBlock> TRANSITION_CLUSTER_MANAGEMENT_COMPUTER =
            clusterManagementComputer("transition_cluster_management_computer", HardwareEra.TRANSITION,
                    CaseStyle.SOLE).named("Transition Cluster Management Computer").register();
    public static final BlockEntry<ClusterManagementComputerBlock> ADVANCED_CLUSTER_MANAGEMENT_COMPUTER =
            clusterManagementComputer("advanced_cluster_management_computer", HardwareEra.ADVANCED,
                    CaseStyle.NEUTRAL).named("Advanced Cluster Management Computer").register();
    public static final BlockEntry<ClusterManagementComputerBlock>
            ADVANCED_HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER = clusterManagementComputer(
                    "advanced_high_performance_cluster_management_computer", HardwareEra.ADVANCED,
                    CaseStyle.HIGH_PERFORMANCE).named("Advanced High Performance Cluster Management Computer")
                    .register();
    public static final BlockEntry<ClusterManagementComputerBlock> ADVANCED_AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER =
            clusterManagementComputer("advanced_aesthetic_cluster_management_computer", HardwareEra.ADVANCED,
                    CaseStyle.AESTHETIC).named("Advanced Aesthetic Cluster Management Computer").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClusterManagementComputerBlockEntity>>
            CLUSTER_MANAGEMENT_COMPUTER_BE = CONTENT.blockEntity("cluster_management_computer",
                    ClusterManagementComputerBlockEntity::new, CLUSTER_MANAGEMENT_COMPUTER,
                    HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER, AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER,
                    VINTAGE_CLUSTER_MANAGEMENT_COMPUTER, LEGACY_CLUSTER_MANAGEMENT_COMPUTER,
                    TRANSITION_CLUSTER_MANAGEMENT_COMPUTER, ADVANCED_CLUSTER_MANAGEMENT_COMPUTER,
                    ADVANCED_HIGH_PERFORMANCE_CLUSTER_MANAGEMENT_COMPUTER,
                    ADVANCED_AESTHETIC_CLUSTER_MANAGEMENT_COMPUTER);

    // Crafting Switch: declares up to 5 adjacent machines, wired to a Crafting Computer over the crafting cable.
    public static final BlockEntry<CraftingSwitchBlock> CRAFTING_SWITCH =
            CONTENT.block("crafting_switch", CraftingSwitchBlock::new)
                    .properties(properties -> properties.strength(1.5F))
                    .named("Crafting Switch").look(IBlockLook::cubeAll).item().tab(CLUSTER).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingSwitchBlockEntity>>
            CRAFTING_SWITCH_BE =
            CONTENT.blockEntity("crafting_switch", CraftingSwitchBlockEntity::new, CRAFTING_SWITCH);

    /*
     * The HBW Interface: a supercomputer's racks are tied together by the high-compute fabric and uplinked to the
     * data network by one of these.
     */
    public static final BlockEntry<HbwInterfaceBlock> HBW_INTERFACE =
            CONTENT.block("hbw_interface", HbwInterfaceBlock::new)
            .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.0F).noOcclusion())
            .named("HBW Interface").look(IBlockLook::column).item().tab(CLUSTER).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HbwInterfaceBlockEntity>> HBW_INTERFACE_BE =
            CONTENT.blockEntity("hbw_interface", HbwInterfaceBlockEntity::new, HBW_INTERFACE);

    /*
     * Pattern Encoders burn .craft files onto removable media, one per era: the Standard one writes DVDs, CDs and USB
     * sticks, the Legacy one CDs, the Vintage one floppies. The body is drawn by the block entity.
     */

    public static final BlockEntry<PatternEncoderBlock> PATTERN_ENCODER =
            encoder("pattern_encoder", MapColor.COLOR_GRAY, HardwareEra.STANDARD).named("Pattern Encoder").register();
    public static final BlockEntry<PatternEncoderBlock> LEGACY_PATTERN_ENCODER =
            encoder("legacy_pattern_encoder", MapColor.COLOR_LIGHT_GRAY, HardwareEra.LEGACY)
                    .named("Legacy Pattern Encoder").register();
    public static final BlockEntry<PatternEncoderBlock> VINTAGE_PATTERN_ENCODER =
            encoder("vintage_pattern_encoder", MapColor.TERRACOTTA_WHITE, HardwareEra.VINTAGE)
                    .named("Vintage Pattern Encoder").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PatternEncoderBlockEntity>>
            PATTERN_ENCODER_BE = CONTENT.blockEntity("pattern_encoder", PatternEncoderBlockEntity::new,
                    PATTERN_ENCODER, LEGACY_PATTERN_ENCODER, VINTAGE_PATTERN_ENCODER);

    /*
     * Media drives: one block per drive type, each linked to a computer over the Peripheral Cable. A drive is drawn
     * by its block entity as the drive of its day, the medium in it the player's own; the Dock Station is a low hub
     * on the desk, drawn by hand, with the stick standing in it.
     */

    public static final BlockEntry<MediaReaderBlock> FLOPPY_DRIVE =
            drive("floppy_drive", MediaDriveType.FLOPPY_DRIVE, MapColor.TERRACOTTA_WHITE).named("Floppy Drive")
                    .register();
    public static final BlockEntry<MediaReaderBlock> CD_DRIVE =
            drive("cd_drive", MediaDriveType.CD_DRIVE, MapColor.COLOR_LIGHT_GRAY).named("CD Drive").register();
    public static final BlockEntry<MediaReaderBlock> DVD_DRIVE =
            drive("dvd_drive", MediaDriveType.DVD_DRIVE, MapColor.COLOR_BLACK).named("DVD Drive").register();
    public static final BlockEntry<MediaReaderBlock> DOCK_STATION =
            CONTENT.block("dock_station", properties -> new MediaReaderBlock(MediaDriveType.DOCK_STATION, properties))
                    .properties(properties -> properties.mapColor(MapColor.COLOR_BLACK).strength(1.5F)
                            .sound(SoundType.METAL))
                    .named("Dock Station")
                    .look(IBlockLook.facing(new IBlockModel.Handmade("dock_station"))
                            .whileOn(MediaReaderBlock.LOADED, new IBlockModel.Handmade("dock_station_docked")))
                    .item().tab(DEVICES).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MediaReaderBlockEntity>> MEDIA_READER_BE =
            CONTENT.blockEntity("media_reader", MediaReaderBlockEntity::new,
                    FLOPPY_DRIVE, CD_DRIVE, DVD_DRIVE, DOCK_STATION);

    /* The speakers, which carry a computer's sound out beside its monitor: a Legacy model and a Standard one. */
    public static final BlockEntry<SpeakerBlock> LEGACY_SPEAKER =
            speaker("legacy_speaker", "speaker_legacy", HardwareEra.LEGACY, MapColor.COLOR_LIGHT_GRAY)
                    .named("Artisan ToneWorks").register();
    public static final BlockEntry<SpeakerBlock> SPEAKER =
            speaker("speaker", "speaker_standard", HardwareEra.STANDARD, MapColor.COLOR_BLACK)
                    .named("Artisan Cobble").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpeakerBlockEntity>> SPEAKER_BE =
            CONTENT.blockEntity("speaker", SpeakerBlockEntity::new, LEGACY_SPEAKER, SPEAKER);

    /*
     * The Network Gateway: a peripheral of one of our computers that is, on its other face, a ComputerCraft
     * peripheral, so the two families of computers reach each other through it. Its front lights for a linked
     * Gateway and for one blinking to be found.
     */
    public static final BlockEntry<NetworkGatewayBlock> NETWORK_GATEWAY =
            CONTENT.block("network_gateway", NetworkGatewayBlock::new)
                    .properties(properties -> properties.mapColor(MapColor.COLOR_BLACK).strength(1.5F)
                            .sound(SoundType.METAL))
                    .named("Network Gateway")
                    .look(IBlockLook.facing(gateway("network_gateway", "block/network_gateway_front"))
                            .whileOn(NetworkGatewayBlock.LIT,
                                    gateway("network_gateway_lit", "block/network_gateway_front_lit")))
                    .item().tab(DEVICES).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NetworkGatewayBlockEntity>>
            NETWORK_GATEWAY_BE =
            CONTENT.blockEntity("network_gateway", NetworkGatewayBlockEntity::new, NETWORK_GATEWAY);

    // Typed physical media: the format is the item's identity, the content lives in components.

    public static final ItemEntry<FormattedMediaItem> FLOPPY_DISK =
            medium("floppy_disk", MediaFormat.FLOPPY, true).named("Floppy Disk").register();
    public static final ItemEntry<FormattedMediaItem> CD_ROM =
            medium("cd_rom", MediaFormat.CD, false).named("CD-ROM").register();
    public static final ItemEntry<FormattedMediaItem> CD_RW =
            medium("cd_rw", MediaFormat.CD, true).named("CD-RW").register();
    public static final ItemEntry<FormattedMediaItem> DVD_ROM =
            medium("dvd_rom", MediaFormat.DVD, false).named("DVD-ROM").register();
    public static final ItemEntry<FormattedMediaItem> DVD_RW =
            medium("dvd_rw", MediaFormat.DVD, true).named("DVD-RW").register();
    public static final ItemEntry<FormattedMediaItem> BD_ROM =
            medium("bd_rom", MediaFormat.BLU_RAY, false).named("BD-ROM").register();
    public static final ItemEntry<FormattedMediaItem> BD_RE =
            medium("bd_re", MediaFormat.BLU_RAY, true).named("BD-RE").register();
    // The flash drive is a model made by hand in three dimensions, not a flat sprite.
    public static final ItemEntry<FormattedMediaItem> USB_FLASH_DRIVE =
            medium("usb_flash_drive", MediaFormat.USB, true).look(IItemLook.HANDMADE).named("USB Flash Drive")
                    .register();

    /*
     * The Server Racks, one per era, and the Supercomputer Racks of the Standard and the Advanced, which seat only
     * Supercomputer Nodes and whose rear port takes only the high-compute fabric. A cabinet takes servers of its own
     * era or earlier; it is drawn as one model by its controller, which hands its item over itself when broken.
     */

    public static final BlockEntry<ServerRackBlock> VINTAGE_SERVER_RACK =
            serverRack("vintage_server_rack", HardwareEra.VINTAGE).named("Vintage Server Rack").register();
    public static final BlockEntry<ServerRackBlock> LEGACY_SERVER_RACK =
            serverRack("legacy_server_rack", HardwareEra.LEGACY).named("Legacy Server Rack").register();
    public static final BlockEntry<ServerRackBlock> TRANSITION_SERVER_RACK =
            serverRack("transition_server_rack", HardwareEra.TRANSITION).named("Transition Server Rack").register();
    public static final BlockEntry<ServerRackBlock> SERVER_RACK =
            serverRack("server_rack", HardwareEra.STANDARD).named("Server Rack").register();
    public static final BlockEntry<ServerRackBlock> ADVANCED_SERVER_RACK =
            serverRack("advanced_server_rack", HardwareEra.ADVANCED).named("Advanced Server Rack").register();
    public static final BlockEntry<SupercomputerRackBlock> SUPERCOMPUTER_RACK =
            supercomputerRack("supercomputer_rack", HardwareEra.STANDARD).named("Supercomputer Rack").register();
    public static final BlockEntry<SupercomputerRackBlock> ADVANCED_SUPERCOMPUTER_RACK =
            supercomputerRack("advanced_supercomputer_rack", HardwareEra.ADVANCED)
                    .named("Advanced Supercomputer Rack").register();
    public static final BlockEntry<ServerRackPartBlock> SERVER_RACK_PART =
            CONTENT.block("server_rack_part", ServerRackPartBlock::new).properties(ComputingModule::rackProperties)
                    .named("Server Rack").look(RACK_BODY).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerRackBlockEntity>> SERVER_RACK_BE =
            CONTENT.blockEntity("server_rack", ServerRackBlockEntity::new,
                    SERVER_RACK, SUPERCOMPUTER_RACK, LEGACY_SERVER_RACK, VINTAGE_SERVER_RACK, TRANSITION_SERVER_RACK,
                    ADVANCED_SERVER_RACK, ADVANCED_SUPERCOMPUTER_RACK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerRackPartBlockEntity>>
            SERVER_RACK_PART_BE = CONTENT.blockEntity("server_rack_part", ServerRackPartBlockEntity::new,
                    SERVER_RACK_PART);

    // Interaction buses: parts mounted on a cable's face that move items between the network and an inventory.

    public static final ItemEntry<CablePartItem> IMPORT_BUS_ITEM =
            bus("import_bus", ComputingParts.IMPORT, "block/import_bus_part").named("Import Bus").register();
    public static final ItemEntry<CablePartItem> EXPORT_BUS_ITEM =
            bus("export_bus", ComputingParts.EXPORT, "block/export_bus_part").named("Export Bus").register();
    public static final ItemEntry<CablePartItem> INPUT_BUS_ITEM =
            bus("input_bus", ComputingParts.INPUT, "block/export_bus_part").named("Crafting Input Bus").register();
    public static final ItemEntry<CablePartItem> RECEIVING_BUS_ITEM =
            bus("receiving_bus", ComputingParts.RECEIVING, "block/import_bus_part").named("Crafting Receiving Bus")
                    .register();

    /*
     * Servers, and the cases they are assembled in. A case of an era takes only boards of that era, and its server
     * seats in a cabinet of its era or later.
     */

    public static final ItemEntry<ServerCaseItem> VINTAGE_SERVER_CASE =
            serverCase("vintage_server_case").named("Vintage Server Case").register();
    public static final ItemEntry<ServerItem> VINTAGE_SERVER =
            server("vintage_server", RackChassis.VINTAGE_SERVER, SERVERS).named("Vintage Server").register();
    public static final ItemEntry<ServerCaseItem> LEGACY_SERVER_CASE =
            serverCase("legacy_server_case").named("Legacy Server Case").register();
    public static final ItemEntry<ServerItem> LEGACY_SERVER =
            server("legacy_server", RackChassis.LEGACY_SERVER, SERVERS).named("Legacy Server").register();
    public static final ItemEntry<ServerCaseItem> TRANSITION_SERVER_CASE =
            serverCase("transition_server_case").named("Transition Server Case").register();
    public static final ItemEntry<ServerItem> TRANSITION_SERVER =
            server("transition_server", RackChassis.TRANSITION_SERVER, SERVERS).named("Transition Server").register();
    public static final ItemEntry<ServerCaseItem> SERVER_CASE =
            serverCase("server_case").named("Server Case").register();
    public static final ItemEntry<ServerItem> SERVER =
            server("server", RackChassis.SERVER, SERVERS).named("Server").register();
    public static final ItemEntry<ServerCaseItem> ADVANCED_SERVER_CASE =
            serverCase("advanced_server_case").named("Advanced Server Case").register();
    public static final ItemEntry<ServerItem> ADVANCED_SERVER =
            server("advanced_server", RackChassis.ADVANCED_SERVER, SERVERS).named("Advanced Server").register();
    public static final ItemEntry<ServerCaseItem> STORAGE_SERVER_CASE =
            serverCase("storage_server_case").named("Storage Server Case").register();
    public static final ItemEntry<ServerItem> STORAGE_SERVER =
            server("storage_server", RackChassis.STORAGE_SERVER, SERVERS).named("Storage Server").register();
    public static final ItemEntry<ServerCaseItem> COMPUTE_SERVER_CASE =
            serverCase("compute_server_case").named("Compute Server Case").register();
    public static final ItemEntry<ServerItem> COMPUTE_SERVER =
            server("compute_server", RackChassis.COMPUTE_SERVER, SERVERS).named("Compute Server").register();
    // The supercomputer's nodes are rack computers too, shown with the machines.
    public static final ItemEntry<ServerItem> SUPERCOMPUTER_NODE =
            server("supercomputer_node", RackChassis.SUPERCOMPUTER_NODE, MACHINES).named("Supercomputer Node")
                    .register();
    public static final ItemEntry<ServerItem> ADVANCED_SUPERCOMPUTER_NODE =
            server("advanced_supercomputer_node", RackChassis.ADVANCED_SUPERCOMPUTER_NODE, MACHINES)
                    .named("Advanced Supercomputer Node").register();

    // Rack equipment: bay gadgets serve the machine in their row; rack units spend the cabinet's unit budget.

    public static final ItemEntry<RackGadgetItem> RAID_CONTROLLER =
            rackGadget("raid_controller", RackGadgetItem.Kind.RAID_CONTROLLER).named("RAID Controller").register();
    public static final ItemEntry<RackGadgetItem> CACHE_CARD =
            rackGadget("cache_card", RackGadgetItem.Kind.CACHE_CARD).named("Cache Card").register();
    public static final ItemEntry<RackUnitItem> KVM_SWITCH =
            rackUnit("kvm_switch", RackUnitItem.Kind.KVM_SWITCH).named("KVM Switch").register();
    public static final ItemEntry<RackUnitItem> RACK_UPS =
            rackUnit("rack_ups", RackUnitItem.Kind.RACK_UPS).named("Rack UPS").register();
    public static final ItemEntry<RackUnitItem> COOLING_UNIT =
            rackUnit("cooling_unit", RackUnitItem.Kind.COOLING_UNIT).named("Cooling Unit").register();

    /*
     * The parts of the first machines, Standard era; the rest of every era is in HardwareItems. The Mainframe's
     * four-way board and the two-way server board take the Servo on LGA 2011, and their memory slots follow the
     * boards of their kind before them; a machine counts only as many as its own case holds.
     */

    public static final ItemEntry<MotherboardItem> MOTHERBOARD_MTX_S_2011 = part("motherboard_mtx_s_2011",
            properties -> new MotherboardItem(properties, new MotherboardSpec(FormFactor.MTX, HardwareEra.STANDARD,
                    CpuSocketId.LGA_2011, 4, Set.of(RamGeneration.DDR3), 48, PcieGeneration.PCIE_3_0, 8, 6, 8)))
            .named("MF MTX-S Motherboard (4x LGA 2011)").register();
    public static final ItemEntry<MotherboardItem> MOTHERBOARD_EEB_S_2011 = part("motherboard_eeb_s_2011",
            properties -> new MotherboardItem(properties, new MotherboardSpec(FormFactor.EEB, HardwareEra.STANDARD,
                    CpuSocketId.LGA_2011, 2, Set.of(RamGeneration.DDR3), 16, PcieGeneration.PCIE_3_0, 6, 6, 6)))
            .named("MF EEB-S Server Board (2x LGA 2011)").register();
    public static final ItemEntry<CpuItem> CPU_SERVO_2620 = part("cpu_integra_servo_2620", properties -> new CpuItem(
            properties, new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 6, 2000, 95, false)
                    .on(Microarchitectures.SANDY_BRIDGE, "").withSmt()))
            .named("Integra Servo 2620").register();
    public static final ItemEntry<CpuItem> CPU_SERVO_2690 = part("cpu_integra_servo_2690", properties -> new CpuItem(
            properties, new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 8, 2900, 135, false)
                    .on(Microarchitectures.SANDY_BRIDGE, "").withSmt()))
            .named("Integra Servo 2690").register();
    /* The top of the LGA 2011 Servos on DDR3; the eighteen-core chips came with the next socket and DDR4. */
    public static final ItemEntry<CpuItem> CPU_SERVO_2697_V2 = part("cpu_integra_servo_2697_v2",
            properties -> new CpuItem(properties, new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 12, 2700,
                    130, false).on(Microarchitectures.IVY_BRIDGE, "").withSmt()))
            .named("Integra Servo 2697 v2").register();
    public static final ItemEntry<RamItem> RAM_DDR3_8192 = part("ram_ddr3_8192", properties -> new RamItem(
            properties, new RamSpec(HardwareEra.STANDARD, RamGeneration.DDR3, 2048, 15)))
            .named("Stratix Layer DDR3-8192").register();
    public static final ItemEntry<GpuItem> GPU_HD_7970 = part("gpu_radiance_hd_7970", properties -> new GpuItem(
            properties, new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, 2048, 3072, 250)
                    .on(Microarchitectures.GCN, "Tahiti", 925)))
            .named("Velocion Radiance HD 7970").register();
    public static final ItemEntry<CraftingCardItem> CRAFTING_CARD_T2 = part("crafting_card_t2",
            properties -> new CraftingCardItem(properties,
                    new CraftingCardSpec(IndustrialTier.T2, PcieGeneration.PCIE_1_0, 0.05, 2, 75)))
            .named("Forge Logic Crafting Card").register();
    public static final ItemEntry<CraftingCardItem> CRAFTING_CARD_T3 = part("crafting_card_t3",
            properties -> new CraftingCardItem(properties,
                    new CraftingCardSpec(IndustrialTier.T3, PcieGeneration.PCIE_2_0, 0.1, 4, 100)))
            .named("Forge Logic Crafting Card T3").register();
    // The card of the Advanced boards, twice the tier before it again.
    public static final ItemEntry<CraftingCardItem> CRAFTING_CARD_T4 = part("crafting_card_t4",
            properties -> new CraftingCardItem(properties,
                    new CraftingCardSpec(IndustrialTier.T4, PcieGeneration.PCIE_4_0, 0.2, 8, 125)))
            .named("Forge Logic Crafting Card T4").register();
    /*
     * The Cluster Interface Cards: exclusive to the Cluster Management Computer, one per era. Each era
     * reaches further and writes more nodes at once. Numbers are estimates.
     */
    public static final ItemEntry<ClusterInterfaceCardItem> SERIAL_CONSOLE_CARD = part("serial_console_card",
            properties -> new ClusterInterfaceCardItem(properties, new ClusterInterfaceCardSpec(HardwareEra.VINTAGE,
                    IndustrialTier.T2, PcieGeneration.PCI, ClusterInterfaceCardSpec.Reach.DATACENTERS, 1, 10)))
            .named("Serial Console Card").register();
    public static final ItemEntry<ClusterInterfaceCardItem> MANAGEMENT_NIC = part("management_nic",
            properties -> new ClusterInterfaceCardItem(properties, new ClusterInterfaceCardSpec(HardwareEra.LEGACY,
                    IndustrialTier.T3, PcieGeneration.PCIE_1_0, ClusterInterfaceCardSpec.Reach.SUPERCOMPUTERS, 2, 20)))
            .named("Management NIC").register();
    public static final ItemEntry<ClusterInterfaceCardItem> FABRIC_HOST_ADAPTER = part("fabric_host_adapter",
            properties -> new ClusterInterfaceCardItem(properties, new ClusterInterfaceCardSpec(HardwareEra.STANDARD,
                    IndustrialTier.T4, PcieGeneration.PCIE_3_0, ClusterInterfaceCardSpec.Reach.ALL, 4, 35)))
            .named("Fabric Host Adapter").register();
    public static final ItemEntry<ClusterInterfaceCardItem> FABRIC_DPU = part("fabric_dpu",
            properties -> new ClusterInterfaceCardItem(properties, new ClusterInterfaceCardSpec(HardwareEra.ADVANCED,
                    IndustrialTier.T5, PcieGeneration.PCIE_4_0, ClusterInterfaceCardSpec.Reach.ALL, 8, 75)))
            .named("Fabric DPU").register();
    public static final ItemEntry<PhiCoprocessorItem> PHI_5100 =
            phi("phi_5100", new PhiCoprocessorSpec(IndustrialTier.T3, 2, 60, 1050, 225))
                    .named("Integra Phi 5100 Co-processor").register();
    public static final ItemEntry<PhiCoprocessorItem> PHI_7120 =
            phi("phi_7120", new PhiCoprocessorSpec(IndustrialTier.T4, 3, 61, 1240, 250))
                    .named("Integra Phi 7120 Co-processor").register();
    public static final ItemEntry<PhiCoprocessorItem> PHI_7290 =
            phi("phi_7290", new PhiCoprocessorSpec(IndustrialTier.T4, 4, 72, 1500, 270))
                    .named("Integra Phi 7290 Co-processor").register();
    public static final ItemEntry<PhiCoprocessorItem> PHI_9000 =
            phi("phi_9000", new PhiCoprocessorSpec(IndustrialTier.T5, 6, 96, 1800, 300))
                    .named("Integra Phi 9000 Co-processor").register();
    public static final ItemEntry<PsuItem> PSU_650G =
            part("psu_650g", properties -> new PsuItem(properties, new PsuSpec(650, 90)))
                    .named("MF PowerGold 650G").register();

    /** Every disk of every tier and size, in that order. */
    public static final List<DiskEntry> DISKS_BY_SIZE = registerDisks();

    private ComputingModule() {
    }

    /** The registered disk of that tier and size. */
    public static DiskItem disk(final StorageTier tier, final DiskSize size) {
        for (final DiskEntry entry : DISKS_BY_SIZE) {
            if (entry.tier() == tier && entry.size() == size) {
                return entry.item().get();
            }
        }
        throw new IllegalArgumentException("no registered disk for " + tier + " " + size);
    }

    public static void register(final IEventBus modEventBus) {
        /*
         * Load the per-era hardware catalogue so its items are declared before the content is handed to the mod
         * event bus below.
         */
        HardwareItems.init();
        ComputingSounds.init();
        ComputingAudioDevices.init();
        CONTENT.register(modEventBus);
        ComputingComponents.register(modEventBus);
        ComputingMenus.register(modEventBus);
        JscTriggers.register(modEventBus);
    }

    /** A registered disk with its tier and size. */
    public record DiskEntry(StorageTier tier, DiskSize size, DeferredItem<DiskItem> item) {
    }

    private static List<DiskEntry> registerDisks() {
        final List<DiskEntry> disks = new ArrayList<>();
        for (final StorageTier tier : StorageTier.values()) {
            for (final DiskSize size : DiskSize.values()) {
                if (!size.comesAs(tier)) {
                    continue;
                }
                final String id = "disk_" + tier.name().toLowerCase(Locale.ROOT) + "_" + size.id();
                disks.add(new DiskEntry(tier, size, CONTENT.item(id, properties -> new DiskItem(properties,
                                new DiskSpec(tier, diskEra(tier, size), size.capacityItems(), tier.tdpWatts())))
                        .named(tier.productName() + " " + size.displayName()).tab(DISKS).register()));
            }
        }
        return List.copyOf(disks);
    }

    /*
     * Each disk belongs to the years it sold in: the hard disks up to 2 TB to the Transition and the helium ones above
     * 8 TB to the Advanced, the SATA SSD of 8 TB and the NVMe drives from 4 TB to the Advanced as well, and the rest of
     * the grid to the Standard.
     */
    private static HardwareEra diskEra(final StorageTier tier, final DiskSize size) {
        final long items = size.capacityItems();
        final long lastStandard = switch (tier) {
            case HDD -> DiskSize.TB_8.capacityItems();
            case SSD -> DiskSize.TB_4.capacityItems();
            case NVME -> DiskSize.TB_2.capacityItems();
        };
        if (tier == StorageTier.HDD && items <= DiskSize.TB_2.capacityItems()) {
            return HardwareEra.TRANSITION;
        }
        return items <= lastStandard ? HardwareEra.STANDARD : HardwareEra.ADVANCED;
    }

    private static BlockBehaviour.Properties cableProperties(final BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.COLOR_GRAY).strength(0.3F).sound(SoundType.WOOL).noOcclusion();
    }

    /*
     * The cabinet is one model drawn by the controller, so the twelve blocks render nothing themselves: without
     * noOcclusion they would still cull their neighbours' faces and block light, leaving a machine-shaped hole in
     * the world around the model.
     */
    private static BlockBehaviour.Properties mainframeProperties(final BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.COLOR_GRAY).strength(3.5F).sound(SoundType.METAL).noOcclusion()
                .requiresCorrectToolForDrops();
    }

    private static BlockBehaviour.Properties rackProperties(final BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL).noOcclusion();
    }

    /*
     * The data cable of {@code line} in {@code era}, carrying what that link carries, in the jacket and with the plug
     * of the same names. Each line has a lane of its own, the same in every era; the long distance line has none and
     * is thicker, since a block that holds it holds nothing else.
     */
    private static CableBuilder dataCable(final String id, final DataLine line, final HardwareEra era,
                                          final String jacket, final String plug) {
        final DataLink link = new DataLink(line, era);
        final CableType.Builder builder = CableType.builder(DataLines.of(link))
                .carries(link.throughput(), link.range())
                .jacket(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "block/cable/" + jacket))
                .plug(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "block/cable/plug/" + plug));
        if (link.straight()) {
            builder.runsStraight();
        }
        if (link.betweenTwoEnds()) {
            builder.alone().thickness(LONG_DISTANCE_PIXELS).joinsAtMost(DataLink.ENDS);
        } else {
            builder.lane(switch (line) {
                case ACCESS -> Lane.TOP_LEFT;
                case BACKBONE -> Lane.TOP;
                case HPC -> Lane.MIDDLE;
                default -> Lane.RIGHT;
            });
        }
        return CONTENT.cable(id, builder);
    }

    /** A cable: a core, an arm toward each side it connects to, and the core as its item. */
    private static <B extends Block> BlockBuilder<B> cable(final String id,
                                                          final Function<BlockBehaviour.Properties, B> factory,
                                                          final ContentTab.Section section) {
        return CONTENT.block(id, factory).properties(ComputingModule::cableProperties)
                .look(IBlockLook.pipe("block/" + id, "block/cable_core", "block/cable_arm"))
                .item().itemLook(IItemLook.parent("block/" + id + "_core")).tab(section);
    }

    private static BlockBuilder<MainframeBlock> mainframe(final String id, final HardwareEra era) {
        return CONTENT.block(id, properties -> new MainframeBlock(properties, era))
                .properties(ComputingModule::mainframeProperties).look(MAINFRAME_BODY)
                .geo(ComputingLooks.MAINFRAME)
                .item((block, properties) -> new MainframeBlockItem(block, properties, id))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).drops(Drops.NONE).tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .tab(MACHINES);
    }

    /** A screen: lit while its computer shows something, its front toward the player who placed it. */
    private static <B extends Block> BlockBuilder<B> monitor(final String id,
                                                            final Function<BlockBehaviour.Properties, B> factory) {
        final String side = "block/" + id + "_side";
        return CONTENT.block(id, factory)
                .properties(properties -> properties.mapColor(MapColor.COLOR_BLACK).strength(1.0F)
                        .sound(SoundType.METAL).noOcclusion())
                .look(IBlockLook.facing(IBlockModel.orientable(id, side, "block/" + id + "_front", side))
                        .frontAgainstFacing()
                        .whileOn(MonitorBlock.LIT, IBlockModel.orientable(id + "_on", side, "block/" + id + "_front_on",
                                side)))
                .item().tab(MACHINES);
    }

    private static BlockBuilder<PersonalComputerBlock> personalComputer(final String id, final HardwareEra era,
                                                                        final CaseStyle style) {
        return computer(id, era, style, properties -> new PersonalComputerBlock(properties, era, style));
    }

    private static BlockBuilder<CraftingComputerBlock> craftingComputer(final String id, final HardwareEra era,
                                                                        final CaseStyle style) {
        return computer(id, era, style, properties -> new CraftingComputerBlock(properties, era, style));
    }

    private static BlockBuilder<ClusterManagementComputerBlock> clusterManagementComputer(
            final String id, final HardwareEra era, final CaseStyle style) {
        return computer(id, era, style, properties -> new ClusterManagementComputerBlock(properties, era, style));
    }

    /**
     * A small computer, whose case its block entity draws: the block shows nothing but the particles a break scatters,
     * and without noOcclusion the full cube would block its own light and cull the faces of its neighbours. Its item
     * shows the same case, switched off.
     */
    private static <B extends Block & IComputerCase> BlockBuilder<B> computer(
            final String id, final HardwareEra era, final CaseStyle style,
            final Function<BlockBehaviour.Properties, B> factory) {
        return CONTENT.block(id, factory)
                .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.0F).noOcclusion())
                .look(IBlockLook.fixed(new IBlockModel.ParticleOnly(id + "_body",
                        "block/computer/" + style.caseName(era) + "_particle")))
                .geo(ComputingLooks.COMPUTER)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.COMPUTER,
                        block.caseModel(), DEVICE_FIT, COMPUTER_LAMPS))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(MACHINES);
    }

    private static BlockBuilder<PatternEncoderBlock> encoder(final String id, final MapColor color,
                                                             final HardwareEra era) {
        /*
         * The body is drawn by the block entity: without noOcclusion a full cube would block its own light and cull
         * the faces of its neighbours.
         */
        return CONTENT.block(id, properties -> new PatternEncoderBlock(properties, era))
                .properties(properties -> properties.mapColor(color).strength(1.5F).sound(SoundType.METAL)
                        .noOcclusion())
                .look(ENCODER_BODY).geo(ComputingLooks.PATTERN_ENCODER)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.PATTERN_ENCODER,
                        id, DEVICE_FIT, DEVICE_LAMPS))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(DEVICES);
    }

    /**
     * A drive, whose body its block entity draws: the block shows nothing but the particles a break scatters, and
     * without noOcclusion the full cube would block its own light and cull the faces of its neighbours.
     */
    private static BlockBuilder<MediaReaderBlock> drive(final String id, final MediaDriveType type,
                                                        final MapColor color) {
        return CONTENT.block(id, properties -> new MediaReaderBlock(type, properties))
                .properties(properties -> properties.mapColor(color).strength(1.5F).sound(SoundType.METAL)
                        .noOcclusion())
                .look(IBlockLook.fixed(new IBlockModel.ParticleOnly(id + "_body", "block/" + id + "_particle")))
                .geo(ComputingLooks.MEDIA_DRIVE)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.MEDIA_DRIVE, id,
                        DEVICE_FIT, DEVICE_LAMPS))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(DEVICES);
    }

    /** A speaker: its grille in front, its sockets behind, set down facing whoever places it. */
    private static BlockBuilder<SpeakerBlock> speaker(final String id, final String textures, final HardwareEra era,
                                                      final MapColor color) {
        final String face = "block/" + textures + "_";
        return CONTENT.block(id, properties -> new SpeakerBlock(properties, era))
                .properties(properties -> properties.mapColor(color).strength(1.0F).sound(SoundType.METAL))
                .look(IBlockLook.facing(new IBlockModel.SixFaces(id, face + "top", face + "top", face + "front",
                        face + "back", face + "side", face + "side", face + "side")))
                .item().tab(DEVICES);
    }

    /** The gateway's box: the modem socket on the front, the cable socket behind, louvres and hatches. */
    private static IBlockModel gateway(final String name, final String front) {
        return new IBlockModel.SixFaces(name, "block/network_gateway_top", "block/network_gateway_top", front,
                "block/network_gateway_back", "block/network_gateway_side", "block/network_gateway_side",
                "block/network_gateway_side");
    }

    private static BlockBuilder<ServerRackBlock> serverRack(final String id, final HardwareEra era) {
        return rack(id, properties -> new ServerRackBlock(properties, era));
    }

    private static BlockBuilder<SupercomputerRackBlock> supercomputerRack(final String id, final HardwareEra era) {
        return rack(id, properties -> new SupercomputerRackBlock(properties, era));
    }

    private static <B extends Block> BlockBuilder<B> rack(final String id,
                                                         final Function<BlockBehaviour.Properties, B> factory) {
        return CONTENT.block(id, factory).properties(ComputingModule::rackProperties).look(RACK_BODY)
                .geo(ComputingLooks.RACK)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.RACK, id, RACK_FIT,
                        RACK_FITTED))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).drops(Drops.NONE).tab(RACKS);
    }

    private static ItemBuilder<FormattedMediaItem> medium(final String id, final MediaFormat format,
                                                          final boolean writable) {
        return CONTENT.item(id, properties -> new FormattedMediaItem(properties, format, writable)).tab(MEDIA);
    }

    /** A bus's item shows the part it mounts as. */
    private static ItemBuilder<CablePartItem> bus(final String id, final Supplier<? extends PartType<?>> type,
                                                  final String model) {
        return CONTENT.item(id, properties -> new CablePartItem(properties, type))
                .look(IItemLook.parent(model)).tab(RACKS);
    }

    private static ItemBuilder<ServerCaseItem> serverCase(final String id) {
        return CONTENT.item(id, ServerCaseItem::new).tab(SERVERS);
    }

    private static ItemBuilder<ServerItem> server(final String id, final RackChassis chassis,
                                                  final ContentTab.Section section) {
        return CONTENT.item(id, properties -> new ServerItem(properties, chassis)).tab(section);
    }

    private static ItemBuilder<RackGadgetItem> rackGadget(final String id, final RackGadgetItem.Kind kind) {
        return CONTENT.item(id, properties -> new RackGadgetItem(properties, kind)).tab(RACK_EQUIPMENT);
    }

    private static ItemBuilder<RackUnitItem> rackUnit(final String id, final RackUnitItem.Kind kind) {
        return CONTENT.item(id, properties -> new RackUnitItem(properties, kind)).tab(RACK_EQUIPMENT);
    }

    private static <I extends Item> ItemBuilder<I> part(final String id, final Function<Item.Properties, I> factory) {
        return CONTENT.item(id, factory).tab(PARTS);
    }

    private static ItemBuilder<PhiCoprocessorItem> phi(final String id, final PhiCoprocessorSpec spec) {
        return part(id, properties -> new PhiCoprocessorItem(properties, spec));
    }
}
