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
import dev.jstech.computers.block.ClusterManagementComputerBlock;
import dev.jstech.computers.block.CraftingComputerBlock;
import dev.jstech.computers.block.CraftingSwitchBlock;
import dev.jstech.computers.block.DataCableBlock;
import dev.jstech.computers.block.HbwInterfaceBlock;
import dev.jstech.computers.block.LegacyClusterManagementComputerBlock;
import dev.jstech.computers.block.LegacyCraftingComputerBlock;
import dev.jstech.computers.block.LegacyMainframeBlock;
import dev.jstech.computers.block.LegacyMonitorBlock;
import dev.jstech.computers.block.LegacyPersonalComputerBlock;
import dev.jstech.computers.block.LegacyServerRackBlock;
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
import dev.jstech.computers.block.VintageClusterManagementComputerBlock;
import dev.jstech.computers.block.VintageCraftingComputerBlock;
import dev.jstech.computers.block.VintageMainframeBlock;
import dev.jstech.computers.block.VintageMonitorBlock;
import dev.jstech.computers.block.VintagePersonalComputerBlock;
import dev.jstech.computers.block.VintageServerRackBlock;
import dev.jstech.computers.block.part.CablePartItem;
import dev.jstech.computers.block.part.CablePartType;
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingSwitchBlockEntity;
import dev.jstech.computers.blockentity.DataCableBlockEntity;
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
import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaReaderBlock;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.tier.IndustrialTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
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

    /**
     * How an encoder body sits in an item slot: the body is one full block (x -0.5..0.5, y 0..1,
     * z -0.5..0.5 around its origin), so only the height needs re-centring.
     */
    private static final CabinetBlockItem.Fit ENCODER_FIT = new CabinetBlockItem.Fit(16.0F, 0.0F, -0.5F, 0.0F);

    // Cables. The data cables are one block entity; the Crafting cable links a Crafting Switch to its computer.

    public static final BlockEntry<DataCableBlock> ETHERNET_CABLE =
            cable("ethernet_cable", properties -> new DataCableBlock(properties, DataTier.T1_ETHERNET), NETWORK)
                    .named("Ethernet Cable").register();
    public static final BlockEntry<DataCableBlock> HBW_CABLE =
            cable("hbw_cable", properties -> new DataCableBlock(properties, DataTier.T2_HBW), NETWORK)
                    .named("HBW Cable").register();
    public static final BlockEntry<DataCableBlock> HPC_CABLE =
            cable("hpc_cable", properties -> new DataCableBlock(properties, DataTier.HPC), CLUSTER)
                    .named("High Compute Cable").register();
    public static final BlockEntry<DataCableBlock> CRAFTING_CABLE =
            cable("crafting_cable", properties -> new DataCableBlock(properties, DataTier.CRAFTING), CLUSTER)
                    .named("Crafting Cable").register();
    public static final BlockEntry<PeripheralCableBlock> PERIPHERAL_CABLE =
            cable("peripheral_cable", PeripheralCableBlock::new, NETWORK).named("Peripheral Cable").register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DataCableBlockEntity>> DATA_CABLE_BE =
            CONTENT.blockEntity("data_cable", DataCableBlockEntity::new,
                    ETHERNET_CABLE, HBW_CABLE, HPC_CABLE, CRAFTING_CABLE);

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
     * Mainframes: the same orchestrator and block entity in three eras, differing by era, accepted board and skin. A
     * 3x2x2 cabinet drawn as one model by the controller; the controller hands its item over itself when broken,
     * and the part blocks of the structure are taken down without drops.
     */

    public static final BlockEntry<MainframeBlock> MAINFRAME =
            mainframe("mainframe", MainframeBlock::new).named("Mainframe").register();
    public static final BlockEntry<VintageMainframeBlock> VINTAGE_MAINFRAME =
            mainframe("vintage_mainframe", VintageMainframeBlock::new).named("Vintage Mainframe").register();
    public static final BlockEntry<LegacyMainframeBlock> LEGACY_MAINFRAME =
            mainframe("legacy_mainframe", LegacyMainframeBlock::new).named("Legacy Mainframe").register();
    public static final BlockEntry<MainframePartBlock> MAINFRAME_PART =
            CONTENT.block("mainframe_part", MainframePartBlock::new).properties(ComputingModule::mainframeProperties)
                    .named("Mainframe").look(MAINFRAME_BODY).tag(BlockTags.MINEABLE_WITH_PICKAXE).register();

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MainframeBlockEntity>> MAINFRAME_BE =
            CONTENT.blockEntity("mainframe", MainframeBlockEntity::new, MAINFRAME, VINTAGE_MAINFRAME, LEGACY_MAINFRAME);
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

    // The computers, each in three eras: the same machine and block entity, differing by era, board and skin.

    public static final BlockEntry<PersonalComputerBlock> PERSONAL_COMPUTER =
            computer("personal_computer", PersonalComputerBlock::new).named("Personal Computer").register();
    public static final BlockEntry<VintagePersonalComputerBlock> VINTAGE_PERSONAL_COMPUTER =
            computer("vintage_personal_computer", VintagePersonalComputerBlock::new)
                    .named("Vintage Personal Computer").register();
    public static final BlockEntry<LegacyPersonalComputerBlock> LEGACY_PERSONAL_COMPUTER =
            computer("legacy_personal_computer", LegacyPersonalComputerBlock::new)
                    .named("Legacy Personal Computer").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PersonalComputerBlockEntity>>
            PERSONAL_COMPUTER_BE = CONTENT.blockEntity("personal_computer", PersonalComputerBlockEntity::new,
                    PERSONAL_COMPUTER, VINTAGE_PERSONAL_COMPUTER, LEGACY_PERSONAL_COMPUTER);

    // Crafting Computer: an ATX computer that executes recipes once a Crafting Card is installed.
    public static final BlockEntry<CraftingComputerBlock> CRAFTING_COMPUTER =
            computer("crafting_computer", CraftingComputerBlock::new).named("Crafting Computer").register();
    public static final BlockEntry<VintageCraftingComputerBlock> VINTAGE_CRAFTING_COMPUTER =
            computer("vintage_crafting_computer", VintageCraftingComputerBlock::new)
                    .named("Vintage Crafting Computer").register();
    public static final BlockEntry<LegacyCraftingComputerBlock> LEGACY_CRAFTING_COMPUTER =
            computer("legacy_crafting_computer", LegacyCraftingComputerBlock::new)
                    .named("Legacy Crafting Computer").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingComputerBlockEntity>>
            CRAFTING_COMPUTER_BE = CONTENT.blockEntity("crafting_computer", CraftingComputerBlockEntity::new,
                    CRAFTING_COMPUTER, VINTAGE_CRAFTING_COMPUTER, LEGACY_CRAFTING_COMPUTER);

    /*
     * Cluster Management Computer: a full computer that, with a Cluster Interface Card, drives every
     * supercomputer fabric and datacenter section on its network as one machine.
     */
    public static final BlockEntry<ClusterManagementComputerBlock> CLUSTER_MANAGEMENT_COMPUTER =
            computer("cluster_management_computer", ClusterManagementComputerBlock::new)
                    .named("Cluster Management Computer").register();
    public static final BlockEntry<VintageClusterManagementComputerBlock> VINTAGE_CLUSTER_MANAGEMENT_COMPUTER =
            computer("vintage_cluster_management_computer", VintageClusterManagementComputerBlock::new)
                    .named("Vintage Cluster Management Computer").register();
    public static final BlockEntry<LegacyClusterManagementComputerBlock> LEGACY_CLUSTER_MANAGEMENT_COMPUTER =
            computer("legacy_cluster_management_computer", LegacyClusterManagementComputerBlock::new)
                    .named("Legacy Cluster Management Computer").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ClusterManagementComputerBlockEntity>>
            CLUSTER_MANAGEMENT_COMPUTER_BE = CONTENT.blockEntity("cluster_management_computer",
                    ClusterManagementComputerBlockEntity::new, CLUSTER_MANAGEMENT_COMPUTER,
                    VINTAGE_CLUSTER_MANAGEMENT_COMPUTER, LEGACY_CLUSTER_MANAGEMENT_COMPUTER);

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
     * Media drives: one block per drive type, each linked to a computer over the Peripheral Cable. A loaded drive
     * lights its front; the Dock Station is a low hub on the desk, drawn by hand, with the stick standing in it.
     */

    public static final BlockEntry<MediaReaderBlock> FLOPPY_DRIVE =
            drive("floppy_drive", MediaDriveType.FLOPPY_DRIVE, MapColor.COLOR_GRAY).named("Floppy Drive").register();
    public static final BlockEntry<MediaReaderBlock> CD_DRIVE =
            drive("cd_drive", MediaDriveType.CD_DRIVE, MapColor.COLOR_GRAY).named("CD Drive").register();
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
    // The flash drive is a model made by hand in three dimensions, not a flat sprite.
    public static final ItemEntry<FormattedMediaItem> USB_FLASH_DRIVE =
            medium("usb_flash_drive", MediaFormat.USB, true).look(IItemLook.HANDMADE).named("USB Flash Drive")
                    .register();

    /*
     * The Server Racks, one per era, and the Supercomputer Rack, which seats only Supercomputer Nodes and whose rear
     * port takes only the high-compute fabric. A cabinet takes servers of its own era or earlier; it is drawn as one
     * model by its controller, which hands its item over itself when broken.
     */

    public static final BlockEntry<VintageServerRackBlock> VINTAGE_SERVER_RACK =
            rack("vintage_server_rack", VintageServerRackBlock::new).named("Vintage Server Rack").register();
    public static final BlockEntry<LegacyServerRackBlock> LEGACY_SERVER_RACK =
            rack("legacy_server_rack", LegacyServerRackBlock::new).named("Legacy Server Rack").register();
    public static final BlockEntry<ServerRackBlock> SERVER_RACK =
            rack("server_rack", ServerRackBlock::new).named("Server Rack").register();
    public static final BlockEntry<SupercomputerRackBlock> SUPERCOMPUTER_RACK =
            rack("supercomputer_rack", SupercomputerRackBlock::new).named("Supercomputer Rack").register();
    public static final BlockEntry<ServerRackPartBlock> SERVER_RACK_PART =
            CONTENT.block("server_rack_part", ServerRackPartBlock::new).properties(ComputingModule::rackProperties)
                    .named("Server Rack").look(RACK_BODY).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerRackBlockEntity>> SERVER_RACK_BE =
            CONTENT.blockEntity("server_rack", ServerRackBlockEntity::new,
                    SERVER_RACK, SUPERCOMPUTER_RACK, LEGACY_SERVER_RACK, VINTAGE_SERVER_RACK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerRackPartBlockEntity>>
            SERVER_RACK_PART_BE = CONTENT.blockEntity("server_rack_part", ServerRackPartBlockEntity::new,
                    SERVER_RACK_PART);

    // Interaction buses: parts mounted on a cable's face that move items between the network and an inventory.

    public static final ItemEntry<CablePartItem> IMPORT_BUS_ITEM =
            bus("import_bus", CablePartType.IMPORT, "block/import_bus_part").named("Import Bus").register();
    public static final ItemEntry<CablePartItem> EXPORT_BUS_ITEM =
            bus("export_bus", CablePartType.EXPORT, "block/export_bus_part").named("Export Bus").register();
    public static final ItemEntry<CablePartItem> INPUT_BUS_ITEM =
            bus("input_bus", CablePartType.INPUT, "block/export_bus_part").named("Crafting Input Bus").register();
    public static final ItemEntry<CablePartItem> RECEIVING_BUS_ITEM =
            bus("receiving_bus", CablePartType.RECEIVING, "block/import_bus_part").named("Crafting Receiving Bus")
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
    public static final ItemEntry<ServerCaseItem> SERVER_CASE =
            serverCase("server_case").named("Server Case").register();
    public static final ItemEntry<ServerItem> SERVER =
            server("server", RackChassis.SERVER, SERVERS).named("Server").register();
    public static final ItemEntry<ServerCaseItem> STORAGE_SERVER_CASE =
            serverCase("storage_server_case").named("Storage Server Case").register();
    public static final ItemEntry<ServerItem> STORAGE_SERVER =
            server("storage_server", RackChassis.STORAGE_SERVER, SERVERS).named("Storage Server").register();
    public static final ItemEntry<ServerCaseItem> COMPUTE_SERVER_CASE =
            serverCase("compute_server_case").named("Compute Server Case").register();
    public static final ItemEntry<ServerItem> COMPUTE_SERVER =
            server("compute_server", RackChassis.COMPUTE_SERVER, SERVERS).named("Compute Server").register();
    // The supercomputer's node is a rack computer too, shown with the machines.
    public static final ItemEntry<ServerItem> SUPERCOMPUTER_NODE =
            server("supercomputer_node", RackChassis.SUPERCOMPUTER_NODE, MACHINES).named("Supercomputer Node")
                    .register();

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

    // The parts of the first machines, Standard era; the rest of every era is in HardwareItems.

    public static final ItemEntry<MotherboardItem> MOTHERBOARD_MTX_P = part("motherboard_mtx_p",
            properties -> new MotherboardItem(properties, new MotherboardSpec(FormFactor.MTX, HardwareEra.STANDARD,
                    CpuSocketId.LGA_2011, 4, Set.of(RamGeneration.DDR3), 8, PcieGeneration.PCIE_3_0, 6, 4, 8)))
            .named("MTX-P Motherboard").register();
    public static final ItemEntry<MotherboardItem> MOTHERBOARD_ATX_P = part("motherboard_atx_p",
            properties -> new MotherboardItem(properties, new MotherboardSpec(FormFactor.ATX, HardwareEra.STANDARD,
                    CpuSocketId.AM3, 1, Set.of(RamGeneration.DDR3), 4, PcieGeneration.PCIE_3_0, 4, 2, 4)))
            .named("ATX-P Motherboard").register();
    public static final ItemEntry<MotherboardItem> MOTHERBOARD_EEB_P = part("motherboard_eeb_p",
            properties -> new MotherboardItem(properties, new MotherboardSpec(FormFactor.EEB, HardwareEra.STANDARD,
                    CpuSocketId.LGA_2011, 2, Set.of(RamGeneration.DDR3), 8, PcieGeneration.PCIE_3_0, 6, 6, 6)))
            .named("EEB-P Server Board").register();
    public static final ItemEntry<CpuItem> CPU_SERVO_2620 = part("cpu_servo_2620", properties -> new CpuItem(
            properties, new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 6, 2000, 95, false)))
            .named("Integra Servo 2620").register();
    public static final ItemEntry<CpuItem> CPU_SERVO_2690 = part("cpu_servo_2690", properties -> new CpuItem(
            properties, new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 8, 2900, 135, false)))
            .named("Integra Servo 2690").register();
    public static final ItemEntry<CpuItem> CPU_SERVO_2699 = part("cpu_servo_2699", properties -> new CpuItem(
            properties, new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 18, 2300, 145, false)))
            .named("Integra Servo 2699").register();
    public static final ItemEntry<CpuItem> CPU_ASCENT_965 = part("cpu_ascent_965", properties -> new CpuItem(
            properties, new CpuSpec(HardwareEra.STANDARD, CpuSocketId.AM3, 4, 3400, 125, false)))
            .named("Velocion Ascent X4 965").register();
    public static final ItemEntry<RamItem> RAM_DDR3_8192 = part("ram_ddr3_8192", properties -> new RamItem(
            properties, new RamSpec(HardwareEra.STANDARD, RamGeneration.DDR3, 2048, 15)))
            .named("Stratix DDR3-8192").register();
    public static final ItemEntry<GpuItem> GPU_HD_7970 = part("gpu_hd_7970", properties -> new GpuItem(
            properties, new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, 2048, 3072, 250)))
            .named("Pyrix Radiance HD 7970").register();
    public static final ItemEntry<CraftingCardItem> CRAFTING_CARD_T2 = part("crafting_card_t2",
            properties -> new CraftingCardItem(properties,
                    new CraftingCardSpec(IndustrialTier.T2, PcieGeneration.PCIE_1_0, 0.05, 2, 75)))
            .named("Forge Logic Crafting Card").register();
    public static final ItemEntry<CraftingCardItem> CRAFTING_CARD_T3 = part("crafting_card_t3",
            properties -> new CraftingCardItem(properties,
                    new CraftingCardSpec(IndustrialTier.T3, PcieGeneration.PCIE_2_0, 0.1, 4, 100)))
            .named("Forge Logic Crafting Card T3").register();
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
                final String id = "disk_" + tier.name().toLowerCase(Locale.ROOT) + "_" + size.id();
                disks.add(new DiskEntry(tier, size, CONTENT.item(id, properties -> new DiskItem(properties,
                                new DiskSpec(tier, HardwareEra.STANDARD, size.capacityItems(), tier.tdpWatts())))
                        .named(tier.productName() + " " + size.displayName()).tab(DISKS).register()));
            }
        }
        return List.copyOf(disks);
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

    /** A cable: a core, an arm toward each side it connects to, and the core as its item. */
    private static <B extends Block> BlockBuilder<B> cable(final String id,
                                                          final Function<BlockBehaviour.Properties, B> factory,
                                                          final ContentTab.Section section) {
        return CONTENT.block(id, factory).properties(ComputingModule::cableProperties)
                .look(IBlockLook.pipe("block/" + id, "block/cable_core", "block/cable_arm"))
                .item().itemLook(IItemLook.parent("block/" + id + "_core")).tab(section);
    }

    private static <B extends Block> BlockBuilder<B> mainframe(final String id,
                                                              final Function<BlockBehaviour.Properties, B> factory) {
        return CONTENT.block(id, factory).properties(ComputingModule::mainframeProperties).look(MAINFRAME_BODY)
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

    private static <B extends Block> BlockBuilder<B> computer(final String id,
                                                             final Function<BlockBehaviour.Properties, B> factory) {
        return CONTENT.block(id, factory)
                .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(2.0F))
                .look(IBlockLook::orientable).item().tab(MACHINES);
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
                .look(ENCODER_BODY)
                .item((block, properties) -> new CabinetBlockItem(block, properties, "pattern_encoder", id,
                        "pattern_encoder", ENCODER_FIT))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(DEVICES);
    }

    /** A drive with its casing on the sides and top, and its front lit while it holds a medium. */
    private static BlockBuilder<MediaReaderBlock> drive(final String id, final MediaDriveType type,
                                                        final MapColor color) {
        final String casing = "block/" + id + "_casing";
        return CONTENT.block(id, properties -> new MediaReaderBlock(type, properties))
                .properties(properties -> properties.mapColor(color).strength(1.5F).sound(SoundType.METAL))
                .look(IBlockLook.facing(IBlockModel.orientable(id, casing, "block/" + id + "_front", casing))
                        .whileOn(MediaReaderBlock.LOADED,
                                IBlockModel.orientable(id + "_active", casing, "block/" + id + "_active", casing)))
                .item().tab(DEVICES);
    }

    /** A speaker: its grille in front, its sockets behind, set down facing whoever places it. */
    private static BlockBuilder<SpeakerBlock> speaker(final String id, final String textures, final HardwareEra era,
                                                      final MapColor color) {
        final String face = "block/" + textures + "_";
        return CONTENT.block(id, properties -> new SpeakerBlock(properties, era))
                .properties(properties -> properties.mapColor(color).strength(1.0F).sound(SoundType.WOOD))
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

    private static <B extends Block> BlockBuilder<B> rack(final String id,
                                                         final Function<BlockBehaviour.Properties, B> factory) {
        return CONTENT.block(id, factory).properties(ComputingModule::rackProperties).look(RACK_BODY)
                .item((block, properties) -> new CabinetBlockItem(block, properties, "rack", id, "rack", RACK_FIT))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).drops(Drops.NONE).tab(RACKS);
    }

    private static ItemBuilder<FormattedMediaItem> medium(final String id, final MediaFormat format,
                                                          final boolean writable) {
        return CONTENT.item(id, properties -> new FormattedMediaItem(properties, format, writable)).tab(MEDIA);
    }

    /** A bus's item shows the part it mounts as. */
    private static ItemBuilder<CablePartItem> bus(final String id, final CablePartType type, final String model) {
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
