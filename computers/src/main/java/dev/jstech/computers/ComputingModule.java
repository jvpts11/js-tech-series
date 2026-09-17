/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import com.mojang.serialization.Codec;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.advancement.OsFirstBootTrigger;
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
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.menu.ClusterManagementComputerMenu;
import dev.jstech.computers.menu.CommandPromptMenu;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.CraftingComputerMenu;
import dev.jstech.computers.menu.CraftingSwitchMenu;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.menu.DosTerminalMenu;
import dev.jstech.computers.menu.ExportBusMenu;
import dev.jstech.computers.menu.ImportBusMenu;
import dev.jstech.computers.menu.InputBusMenu;
import dev.jstech.computers.menu.LinuxTtyMenu;
import dev.jstech.computers.menu.MainframeMenu;
import dev.jstech.computers.menu.NetworkGatewayMenu;
import dev.jstech.computers.menu.PatternEncoderMenu;
import dev.jstech.computers.menu.PersonalComputerMenu;
import dev.jstech.computers.menu.ReceivingBusMenu;
import dev.jstech.computers.menu.ServerAssemblyMenu;
import dev.jstech.computers.menu.ServerRackMenu;
import dev.jstech.computers.menu.ServerRouterMenu;
import dev.jstech.computers.os.boot.SystemWelcome;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaDriveType;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.os.media.MediaReaderBlock;
import dev.jstech.computers.os.media.MediaReaderBlockEntity;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.storage.DiskUsage;
import dev.jstech.computers.storage.ServerStorageContents;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.tier.IndustrialTier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

/**
 * Registration entry point for the Computing module: the data network's physical blocks (cables now; computers, routers and racks later).
 */
public final class ComputingModule {

    private ComputingModule() {
    }

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(JsComputers.MODID);

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(JsComputers.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JsComputers.MODID);

    /** Advancement criteria triggers: booting a computer into an OS (the Arch / Gentoo challenges use it). */
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, JsComputers.MODID);
    public static final DeferredHolder<CriterionTrigger<?>,
            OsFirstBootTrigger> OS_FIRST_BOOT =
            TRIGGERS.register("os_first_boot",
                    OsFirstBootTrigger::new);

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, JsComputers.MODID);

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(JsComputers.MODID);

    /*
     * Data components: a Server item carries its state in its NBT: the items it
     * stores, the hardware it is built from, and its network node identity.
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<
                    ServerStorageContents>>
            SERVER_STORAGE = COMPONENTS.registerComponentType("server_storage", b -> b
                    .persistent(ServerStorageContents.CODEC)
                    .networkSynchronized(
                            ServerStorageContents.STREAM_CODEC));

    /*
     * A drive's stored items live in the save-wide volume store, not on the item: the item carries the
     * volume's id and a usage summary, so a drive holding thousands of types stays a tiny item.
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<UUID>>
            DISK_VOLUME = COMPONENTS.registerComponentType("disk_volume", b -> b
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<
                    DiskUsage>>
            DISK_USAGE = COMPONENTS.registerComponentType("disk_usage", b -> b
                    .persistent(DiskUsage.CODEC)
                    .networkSynchronized(DiskUsage.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<ItemContainerContents>>
            SERVER_HARDWARE = COMPONENTS.registerComponentType("server_hardware", b -> b
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<UUID>>
            SERVER_NODE_UUID = COMPONENTS.registerComponentType("server_node_uuid", b -> b
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));

    /*
     * A RAID Controller carries its array configuration: the mode it runs and how many member
     * drives the array was formed with (so a missing member reads as degraded rather than smaller).
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<String>>
            RAID_MODE = COMPONENTS.registerComponentType("raid_mode", b -> b
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<Integer>>
            RAID_MEMBERS = COMPONENTS.registerComponentType("raid_members", b -> b
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /*
     * A rack server's software state (console history, installed programs, settings) persists WITH
     * the item, so it moves between racks with the machine. Server-side only: never network-synced.
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<CompoundTag>>
            SERVER_CONSOLE = COMPONENTS.registerComponentType("server_console", b -> b
                    .persistent(CompoundTag.CODEC));

    /*
     * The software a disk carries: installed programs and their versions, the desktop preferences, and
     * the shell history. It rides on the DISK, not on the computer, because that is what it is: moving
     * a system disk to another machine takes its programs along, and a fresh disk boots clean. Keeping
     * this on the block entity meant a newly installed system still believed the old one's programs
     * were present. Server-side only: never network-synced.
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<CompoundTag>>
            DISK_CONSOLE = COMPONENTS.registerComponentType("disk_console", b -> b
                    .persistent(CompoundTag.CODEC));

    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<String>>
            COMPUTER_NAME = COMPONENTS.registerComponentType("computer_name", b -> b
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /*
     * How much of a (non-Server) computer disk's storage is public, as a per-mille 0..1000. The
     * component rides on the disk ItemStack so the split travels with the disk when it is pulled
     * and reinserted. An absent component reads as fully private (see DiskItem.publicPermille).
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<Integer>>
            DISK_PUBLIC_PERMILLE = COMPONENTS.registerComponentType("disk_public_permille", b -> b
                    .persistent(Codec.intRange(0, 1000))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /*
     * OS media subsystem: components that together describe the content of a MediaItem.
     * A medium carries exactly one kind and the matching content component for that kind.
     */

    // Installer payload (OS_INSTALL / PROGRAM_INSTALL): the OS or program id.
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<ResourceLocation>>
            MEDIA_PAYLOAD = COMPONENTS.registerComponentType("media_payload", b -> b
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    /*
     * Which of the three content kinds this medium carries. Absent component → OS_INSTALL (safe
     * default that keeps legacy blank media behaving as installer media).
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<MediaKind>>
            MEDIA_KIND = COMPONENTS.registerComponentType("media_kind", b -> b
                    .persistent(StableCodecs.byName(MediaKind.class))
                    .networkSynchronized(StableCodecs.byId(MediaKind.class, MediaKind.OS_INSTALL)));

    // Data contents (DATA kind): a portable item/fluid storage snapshot.
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<
                    ServerStorageContents>>
            MEDIA_DATA = COMPONENTS.registerComponentType("media_data", b -> b
                    .persistent(ServerStorageContents.CODEC)
                    .networkSynchronized(
                            ServerStorageContents.STREAM_CODEC));

    // Capacity of a DATA medium in item-equivalents. Absent → MediaItem.DEFAULT_CAPACITY.
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<Integer>>
            MEDIA_CAPACITY = COMPONENTS.registerComponentType("media_capacity", b -> b
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /*
     * Disk filesystem components: files and the installed OS live on the DiskItem stack so
     * they travel with the disk when it is inserted or removed.
     */

    // The filesystem contents of a disk volume: path-keyed map of stored files.
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<
                    FilesystemContents>>
            FILESYSTEM = COMPONENTS.registerComponentType("filesystem", b -> b
                    .persistent(FilesystemContents.CODEC)
                    .networkSynchronized(
                            FilesystemContents.STREAM_CODEC));

    /*
     * The OS installed on a system disk: a ResourceLocation identifying the registered OsDef.
     * Present only on bootable disks; absent on plain data disks.
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<ResourceLocation>>
            SYSTEM_OS = COMPONENTS.registerComponentType("system_os", b -> b
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    /*
     * What the system on this disk remembers about being greeted. It rides on the disk rather than on the machine
     * so that erasing and installing again is a first meeting again, and so that a machine with two systems
     * greets each of them once.
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<
                    SystemWelcome>>
            SYSTEM_WELCOME = COMPONENTS.registerComponentType("system_welcome", b -> b
                    .persistent(SystemWelcome.CODEC)
                    .networkSynchronized(SystemWelcome.STREAM_CODEC));

    /*
     * A user-chosen label for a disk or media volume, shown in This PC and the explorer drive tree and
     * editable there. Rides on the ItemStack so it travels with the disk/medium. Absent → the volume's
     * default name (e.g. "Local Disk" for a system disk, "Removable Drive" for a medium).
     */
    public static final DeferredHolder<DataComponentType<?>,
            DataComponentType<String>>
            VOLUME_LABEL = COMPONENTS.registerComponentType("volume_label", b -> b
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private static BlockBehaviour.Properties cableProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(0.3F)
                .sound(SoundType.WOOL)
                .noOcclusion();
    }

    // Cables

    public static final DeferredBlock<DataCableBlock> ETHERNET_CABLE = BLOCKS.register(
            "ethernet_cable", () -> new DataCableBlock(cableProperties(), DataTier.T1_ETHERNET));

    public static final DeferredItem<BlockItem> ETHERNET_CABLE_ITEM = ITEMS.register(
            "ethernet_cable", () -> new BlockItem(ETHERNET_CABLE.get(), new Item.Properties()));

    public static final DeferredBlock<DataCableBlock> HBW_CABLE = BLOCKS.register(
            "hbw_cable", () -> new DataCableBlock(cableProperties(), DataTier.T2_HBW));

    public static final DeferredItem<BlockItem> HBW_CABLE_ITEM = ITEMS.register(
            "hbw_cable", () -> new BlockItem(HBW_CABLE.get(), new Item.Properties()));

    public static final DeferredBlock<DataCableBlock> HPC_CABLE = BLOCKS.register(
            "hpc_cable", () -> new DataCableBlock(cableProperties(), DataTier.HPC));

    public static final DeferredItem<BlockItem> HPC_CABLE_ITEM = ITEMS.register(
            "hpc_cable", () -> new BlockItem(HPC_CABLE.get(), new Item.Properties()));

    // Crafting cable: links a Crafting Switch to its Crafting Computer (a local machine cluster).
    public static final DeferredBlock<DataCableBlock> CRAFTING_CABLE = BLOCKS.register(
            "crafting_cable", () -> new DataCableBlock(cableProperties(), DataTier.CRAFTING));

    public static final DeferredItem<BlockItem> CRAFTING_CABLE_ITEM = ITEMS.register(
            "crafting_cable", () -> new BlockItem(CRAFTING_CABLE.get(), new Item.Properties()));

    // Crafting Switch: declares up to 5 adjacent machines, wired to a Crafting Computer over the crafting cable.
    public static final DeferredBlock<CraftingSwitchBlock> CRAFTING_SWITCH =
            BLOCKS.register("crafting_switch",
                    () -> new CraftingSwitchBlock(
                            BlockBehaviour.Properties.of().strength(1.5F)));

    public static final DeferredItem<BlockItem> CRAFTING_SWITCH_ITEM = ITEMS.register(
            "crafting_switch", () -> new BlockItem(CRAFTING_SWITCH.get(), new Item.Properties()));

    public static final DeferredBlock<PeripheralCableBlock> PERIPHERAL_CABLE =
            BLOCKS.register("peripheral_cable",
                    () -> new PeripheralCableBlock(cableProperties()));

    public static final DeferredItem<BlockItem> PERIPHERAL_CABLE_ITEM = ITEMS.register(
            "peripheral_cable", () -> new BlockItem(PERIPHERAL_CABLE.get(), new Item.Properties()));

    public static final DeferredBlock<MonitorBlock> MONITOR =
            BLOCKS.register("monitor", () -> new MonitorBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(1.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    public static final DeferredItem<BlockItem> MONITOR_ITEM = ITEMS.register(
            "monitor", () -> new BlockItem(MONITOR.get(), new Item.Properties()));

    public static final DeferredBlock<VintageMonitorBlock> VINTAGE_MONITOR =
            BLOCKS.register("vintage_monitor",
                    () -> new VintageMonitorBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(1.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> VINTAGE_MONITOR_ITEM = ITEMS.register(
            "vintage_monitor", () -> new BlockItem(VINTAGE_MONITOR.get(), new Item.Properties()));

    public static final DeferredBlock<LegacyMonitorBlock> LEGACY_MONITOR =
            BLOCKS.register("legacy_monitor",
                    () -> new LegacyMonitorBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_BLACK)
                                    .strength(1.0F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> LEGACY_MONITOR_ITEM = ITEMS.register(
            "legacy_monitor", () -> new BlockItem(LEGACY_MONITOR.get(), new Item.Properties()));

    // Block entities

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DataCableBlockEntity>> DATA_CABLE_BE =
            BLOCK_ENTITIES.register("data_cable",
                    () -> BlockEntityType.Builder.of(DataCableBlockEntity::new,
                            ETHERNET_CABLE.get(), HBW_CABLE.get(), HPC_CABLE.get(),
                            CRAFTING_CABLE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<CraftingSwitchBlockEntity>>
            CRAFTING_SWITCH_BE = BLOCK_ENTITIES.register("crafting_switch",
                    () -> BlockEntityType.Builder.of(
                            CraftingSwitchBlockEntity::new,
                            CRAFTING_SWITCH.get()).build(null));

    // Routers

    public static final DeferredBlock<PersonalRouterBlock> PERSONAL_ROUTER = BLOCKS.register(
            "personal_router", () -> new PersonalRouterBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_LIGHT_BLUE)
                    .strength(0.5F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> PERSONAL_ROUTER_ITEM = ITEMS.register(
            "personal_router", () -> new BlockItem(PERSONAL_ROUTER.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PersonalRouterBlockEntity>> PERSONAL_ROUTER_BE =
            BLOCK_ENTITIES.register("personal_router",
                    () -> BlockEntityType.Builder.of(PersonalRouterBlockEntity::new,
                            PERSONAL_ROUTER.get()).build(null));

    public static final DeferredBlock<ServerRouterBlock> SERVER_ROUTER =
            BLOCKS.register("server_router",
                    () -> new ServerRouterBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(0.6F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> SERVER_ROUTER_ITEM = ITEMS.register(
            "server_router", () -> new BlockItem(SERVER_ROUTER.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<ServerRouterBlockEntity>> SERVER_ROUTER_BE =
            BLOCK_ENTITIES.register("server_router",
                    () -> BlockEntityType.Builder.of(
                            ServerRouterBlockEntity::new,
                            SERVER_ROUTER.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<MonitorBlockEntity>> MONITOR_BE =
            BLOCK_ENTITIES.register("monitor",
                    () -> BlockEntityType.Builder.of(
                            MonitorBlockEntity::new,
                            MONITOR.get(), VINTAGE_MONITOR.get(), LEGACY_MONITOR.get()).build(null));

    public static final DeferredBlock<TankBlock> TANK =
            BLOCKS.register("tank", () -> new TankBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(0.6F)
                            .sound(SoundType.GLASS)
                            .noOcclusion()));

    public static final DeferredItem<BlockItem> TANK_ITEM = ITEMS.register(
            "tank", () -> new BlockItem(TANK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<TankBlockEntity>> TANK_BE =
            BLOCK_ENTITIES.register("tank",
                    () -> BlockEntityType.Builder.of(
                            TankBlockEntity::new,
                            TANK.get()).build(null));

    // Interaction buses: move items between the network and adjacent inventories

    public static final DeferredItem<CablePartItem> IMPORT_BUS_ITEM =
            ITEMS.register("import_bus", () -> new CablePartItem(
                    new Item.Properties(),
                    CablePartType.IMPORT));

    public static final DeferredItem<CablePartItem> EXPORT_BUS_ITEM =
            ITEMS.register("export_bus", () -> new CablePartItem(
                    new Item.Properties(),
                    CablePartType.EXPORT));

    public static final DeferredItem<CablePartItem> INPUT_BUS_ITEM =
            ITEMS.register("input_bus", () -> new CablePartItem(
                    new Item.Properties(),
                    CablePartType.INPUT));

    public static final DeferredItem<CablePartItem> RECEIVING_BUS_ITEM =
            ITEMS.register("receiving_bus", () -> new CablePartItem(
                    new Item.Properties(),
                    CablePartType.RECEIVING));

    public static final DeferredHolder<MenuType<?>,
            MenuType<ExportBusMenu>> EXPORT_BUS_MENU =
            MENUS.register("export_bus", () -> IMenuTypeExtension.create(
                    ExportBusMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>,
            MenuType<ImportBusMenu>> IMPORT_BUS_MENU =
            MENUS.register("import_bus", () -> IMenuTypeExtension.create(
                    ImportBusMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>,
            MenuType<CraftingSwitchMenu>> CRAFTING_SWITCH_MENU =
            MENUS.register("crafting_switch", () -> IMenuTypeExtension.create(
                    CraftingSwitchMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>,
            MenuType<InputBusMenu>> INPUT_BUS_MENU =
            MENUS.register("input_bus", () -> IMenuTypeExtension.create(
                    InputBusMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>,
            MenuType<ReceivingBusMenu>> RECEIVING_BUS_MENU =
            MENUS.register("receiving_bus", () -> IMenuTypeExtension.create(
                    ReceivingBusMenu::fromNetwork));

    // Server Rack: houses Server items as network nodes

    public static final DeferredBlock<ServerRackBlock> SERVER_RACK =
            BLOCKS.register("server_rack",
                    () -> new ServerRackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.5F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    /**
     * How a rack cabinet sits in an item slot: 3 blocks tall is its longest side, and its model spans
     * x -1.5..0.5, y 0..3, z -0.5..1.5 blocks around the controller, so its middle moves by this much.
     */
    private static final CabinetBlockItem.Fit RACK_FIT =
            new CabinetBlockItem.Fit(48.0F, 0.5F, -1.5F, -0.5F);

    public static final DeferredItem<BlockItem> SERVER_RACK_ITEM = ITEMS.register(
            "server_rack", () -> new CabinetBlockItem(
                    SERVER_RACK.get(), new Item.Properties(), "rack", "server_rack", "rack", RACK_FIT));

    /*
     * The supercomputer cabinet: the same foundation, but it seats only Supercomputer Nodes and its
     * rear port takes only the high-compute fabric.
     */
    public static final DeferredBlock<SupercomputerRackBlock>
            SUPERCOMPUTER_RACK = BLOCKS.register("supercomputer_rack",
                    () -> new SupercomputerRackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.5F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> SUPERCOMPUTER_RACK_ITEM = ITEMS.register(
            "supercomputer_rack", () -> new CabinetBlockItem(
                    SUPERCOMPUTER_RACK.get(), new Item.Properties(), "rack", "supercomputer_rack",
                    "rack", RACK_FIT));

    /*
     * The Server Rack of the earlier eras: the same cabinet in its decade's materials, seating only
     * servers of its own era or earlier.
     */
    public static final DeferredBlock<LegacyServerRackBlock>
            LEGACY_SERVER_RACK = BLOCKS.register("legacy_server_rack",
                    () -> new LegacyServerRackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.5F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> LEGACY_SERVER_RACK_ITEM = ITEMS.register(
            "legacy_server_rack", () -> new CabinetBlockItem(
                    LEGACY_SERVER_RACK.get(), new Item.Properties(), "rack", "legacy_server_rack",
                    "rack", RACK_FIT));

    public static final DeferredBlock<VintageServerRackBlock>
            VINTAGE_SERVER_RACK = BLOCKS.register("vintage_server_rack",
                    () -> new VintageServerRackBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.5F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> VINTAGE_SERVER_RACK_ITEM = ITEMS.register(
            "vintage_server_rack", () -> new CabinetBlockItem(
                    VINTAGE_SERVER_RACK.get(), new Item.Properties(), "rack", "vintage_server_rack",
                    "rack", RACK_FIT));

    public static final DeferredBlock<ServerRackPartBlock> SERVER_RACK_PART =
            BLOCKS.register("server_rack_part",
                    () -> new ServerRackPartBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.METAL)
                                    .strength(1.5F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerRackBlockEntity>> SERVER_RACK_BE =
            BLOCK_ENTITIES.register("server_rack",
                    () -> BlockEntityType.Builder.of(ServerRackBlockEntity::new,
                            SERVER_RACK.get(), SUPERCOMPUTER_RACK.get(), LEGACY_SERVER_RACK.get(),
                            VINTAGE_SERVER_RACK.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<ServerRackPartBlockEntity>> SERVER_RACK_PART_BE =
            BLOCK_ENTITIES.register("server_rack_part",
                    () -> BlockEntityType.Builder.of(
                            ServerRackPartBlockEntity::new,
                            SERVER_RACK_PART.get()).build(null));

    public static final DeferredHolder<MenuType<?>,
            MenuType<ServerRackMenu>> SERVER_RACK_MENU =
            MENUS.register("server_rack", () -> IMenuTypeExtension.create(
                    ServerRackMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>,
            MenuType<ServerAssemblyMenu>> SERVER_ASSEMBLY_MENU =
            MENUS.register("server_assembly", () -> IMenuTypeExtension.create(
                    ServerAssemblyMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>,
            MenuType<ComputerTerminalMenu>> COMPUTER_TERMINAL_MENU =
            MENUS.register("computer_terminal", () -> IMenuTypeExtension.create(
                    ComputerTerminalMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>,
            MenuType<CommandPromptMenu>> COMMAND_PROMPT_MENU =
            MENUS.register("command_prompt", () -> IMenuTypeExtension.create(
                    CommandPromptMenu::fromNetwork));

    /*
     * Each terminal platform opens its own screen: MC-DOS and the Linux TTY carry the Command Prompt's
     * data but are separate menu types, so the MC-NET window is never reused for another system's console.
     */
    public static final DeferredHolder<MenuType<?>,
            MenuType<DosTerminalMenu>> DOS_TERMINAL_MENU =
            MENUS.register("dos_terminal", () -> IMenuTypeExtension.create(
                    DosTerminalMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>,
            MenuType<LinuxTtyMenu>> LINUX_TTY_MENU =
            MENUS.register("linux_tty", () -> IMenuTypeExtension.create(
                    LinuxTtyMenu::fromNetwork));

    public static final DeferredHolder<MenuType<?>,
            MenuType<DesktopMenu>> DESKTOP_MENU =
            MENUS.register("desktop", () -> IMenuTypeExtension.create(
                    DesktopMenu::fromNetwork));

    // Hardware components (Standard era, minimal set to build a Mainframe)

    public static final DeferredItem<MotherboardItem> MOTHERBOARD_MTX_P = ITEMS.register(
            "motherboard_mtx_p", () -> new MotherboardItem(new Item.Properties(),
                    new MotherboardSpec(FormFactor.MTX,
                            HardwareEra.STANDARD, CpuSocketId.LGA_2011, 4,
                            Set.of(RamGeneration.DDR3), 8, PcieGeneration.PCIE_3_0, 6, 4, 8)));

    public static final DeferredItem<CpuItem> CPU_SERVO_2620 = ITEMS.register(
            "cpu_servo_2620", () -> new CpuItem(new Item.Properties(),
                    new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 6, 2000, 95, false)));

    public static final DeferredItem<CpuItem> CPU_SERVO_2690 = ITEMS.register(
            "cpu_servo_2690", () -> new CpuItem(new Item.Properties(),
                    new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 8, 2900, 135, false)));

    public static final DeferredItem<CpuItem> CPU_SERVO_2699 = ITEMS.register(
            "cpu_servo_2699", () -> new CpuItem(new Item.Properties(),
                    new CpuSpec(HardwareEra.STANDARD, CpuSocketId.LGA_2011, 18, 2300, 145, false)));

    public static final DeferredItem<RamItem> RAM_DDR3_8192 = ITEMS.register(
            "ram_ddr3_8192", () -> new RamItem(new Item.Properties(),
                    new RamSpec(HardwareEra.STANDARD, RamGeneration.DDR3, 2048, 15)));

    public static final DeferredItem<GpuItem> GPU_HD_7970 = ITEMS.register(
            "gpu_hd_7970", () -> new GpuItem(new Item.Properties(),
                    new GpuSpec(HardwareEra.STANDARD, PcieGeneration.PCIE_3_0, 2048, 3072, 250)));

    public static final DeferredItem<CraftingCardItem> CRAFTING_CARD_T2 = ITEMS.register(
            "crafting_card_t2", () -> new CraftingCardItem(new Item.Properties(),
                    new CraftingCardSpec(IndustrialTier.T2, PcieGeneration.PCIE_1_0, 0.05, 2, 75)));

    public static final DeferredItem<CraftingCardItem> CRAFTING_CARD_T3 = ITEMS.register(
            "crafting_card_t3", () -> new CraftingCardItem(new Item.Properties(),
                    new CraftingCardSpec(IndustrialTier.T3, PcieGeneration.PCIE_2_0, 0.1, 4, 100)));

    /*
     * The Cluster Interface Cards: exclusive to the Cluster Management Computer, one per era. Each era
     * reaches further and writes more nodes at once. Numbers are estimates.
     */
    public static final DeferredItem<ClusterInterfaceCardItem> SERIAL_CONSOLE_CARD =
            ITEMS.register("serial_console_card", () -> new ClusterInterfaceCardItem(
                    new Item.Properties(), new ClusterInterfaceCardSpec(
                            HardwareEra.VINTAGE, IndustrialTier.T2, PcieGeneration.PCI,
                            ClusterInterfaceCardSpec.Reach.DATACENTERS, 1, 10)));
    public static final DeferredItem<ClusterInterfaceCardItem> MANAGEMENT_NIC =
            ITEMS.register("management_nic", () -> new ClusterInterfaceCardItem(
                    new Item.Properties(), new ClusterInterfaceCardSpec(
                            HardwareEra.LEGACY, IndustrialTier.T3, PcieGeneration.PCIE_1_0,
                            ClusterInterfaceCardSpec.Reach.SUPERCOMPUTERS, 2, 20)));
    public static final DeferredItem<ClusterInterfaceCardItem> FABRIC_HOST_ADAPTER =
            ITEMS.register("fabric_host_adapter", () -> new ClusterInterfaceCardItem(
                    new Item.Properties(), new ClusterInterfaceCardSpec(
                            HardwareEra.STANDARD, IndustrialTier.T4, PcieGeneration.PCIE_3_0,
                            ClusterInterfaceCardSpec.Reach.ALL, 4, 35)));

    public static final DeferredItem<PsuItem> PSU_650G = ITEMS.register(
            "psu_650g", () -> new PsuItem(new Item.Properties(), new PsuSpec(650, 90)));

    /*
     * Pattern system: the Pattern Encoder burns .craft files onto removable media, one encoder per era:
     * the Standard one writes DVDs, CDs and USB sticks, the Legacy one CDs, the Vintage one floppies.
     */

    public static final DeferredBlock<PatternEncoderBlock> PATTERN_ENCODER =
            BLOCKS.register("pattern_encoder",
                    () -> new PatternEncoderBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(1.5F)
                                    .sound(SoundType.METAL)
                                    /*
                                     * The body is drawn by the block entity: without this a full cube would
                                     * block its own light and cull the faces of its neighbours.
                                     */
                                    .noOcclusion(), HardwareEra.STANDARD));

    /**
     * How an encoder body sits in an item slot: the body is one full block (x -0.5..0.5, y 0..1,
     * z -0.5..0.5 around its origin), so only the height needs re-centring.
     */
    private static final CabinetBlockItem.Fit ENCODER_FIT =
            new CabinetBlockItem.Fit(16.0F, 0.0F, -0.5F, 0.0F);

    public static final DeferredItem<BlockItem> PATTERN_ENCODER_ITEM = ITEMS.register(
            "pattern_encoder", () -> new CabinetBlockItem(
                    PATTERN_ENCODER.get(), new Item.Properties(), "pattern_encoder", "pattern_encoder",
                    "pattern_encoder", ENCODER_FIT));

    public static final DeferredBlock<PatternEncoderBlock> LEGACY_PATTERN_ENCODER =
            BLOCKS.register("legacy_pattern_encoder",
                    () -> new PatternEncoderBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_LIGHT_GRAY)
                                    .strength(1.5F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(), HardwareEra.LEGACY));

    public static final DeferredItem<BlockItem> LEGACY_PATTERN_ENCODER_ITEM = ITEMS.register(
            "legacy_pattern_encoder", () -> new CabinetBlockItem(
                    LEGACY_PATTERN_ENCODER.get(), new Item.Properties(), "pattern_encoder", "legacy_pattern_encoder",
                    "pattern_encoder", ENCODER_FIT));

    public static final DeferredBlock<PatternEncoderBlock> VINTAGE_PATTERN_ENCODER =
            BLOCKS.register("vintage_pattern_encoder",
                    () -> new PatternEncoderBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.TERRACOTTA_WHITE)
                                    .strength(1.5F)
                                    .sound(SoundType.METAL)
                                    .noOcclusion(), HardwareEra.VINTAGE));

    public static final DeferredItem<BlockItem> VINTAGE_PATTERN_ENCODER_ITEM = ITEMS.register(
            "vintage_pattern_encoder", () -> new CabinetBlockItem(
                    VINTAGE_PATTERN_ENCODER.get(), new Item.Properties(), "pattern_encoder", "vintage_pattern_encoder",
                    "pattern_encoder", ENCODER_FIT));

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<PatternEncoderBlockEntity>> PATTERN_ENCODER_BE =
            BLOCK_ENTITIES.register("pattern_encoder",
                    () -> BlockEntityType.Builder.of(
                            PatternEncoderBlockEntity::new,
                            PATTERN_ENCODER.get(), LEGACY_PATTERN_ENCODER.get(), VINTAGE_PATTERN_ENCODER.get()).build(null));

    public static final DeferredHolder<MenuType<?>,
            MenuType<PatternEncoderMenu>> PATTERN_ENCODER_MENU =
            MENUS.register("pattern_encoder", () -> IMenuTypeExtension.create(
                    PatternEncoderMenu::fromNetwork));

    /*
     * OS media subsystem: a peripheral block that holds one MediaItem and exposes the
     * installer payload or data contents so the firmware boot screen and transfer logic can read it.
     */

    // Media reader drives: one block per drive type, each linked to a computer via the Peripheral Cable.
    public static final DeferredBlock<MediaReaderBlock> FLOPPY_DRIVE = BLOCKS.register("floppy_drive",
            () -> new MediaReaderBlock(MediaDriveType.FLOPPY_DRIVE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(1.5F).sound(SoundType.METAL)));
    public static final DeferredBlock<MediaReaderBlock> CD_DRIVE = BLOCKS.register("cd_drive",
            () -> new MediaReaderBlock(MediaDriveType.CD_DRIVE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY).strength(1.5F).sound(SoundType.METAL)));
    public static final DeferredBlock<MediaReaderBlock> DVD_DRIVE = BLOCKS.register("dvd_drive",
            () -> new MediaReaderBlock(MediaDriveType.DVD_DRIVE, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(1.5F).sound(SoundType.METAL)));
    public static final DeferredBlock<MediaReaderBlock> DOCK_STATION = BLOCKS.register("dock_station",
            () -> new MediaReaderBlock(MediaDriveType.DOCK_STATION, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK).strength(1.5F).sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> FLOPPY_DRIVE_ITEM = ITEMS.register("floppy_drive",
            () -> new BlockItem(FLOPPY_DRIVE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> CD_DRIVE_ITEM = ITEMS.register("cd_drive",
            () -> new BlockItem(CD_DRIVE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DVD_DRIVE_ITEM = ITEMS.register("dvd_drive",
            () -> new BlockItem(DVD_DRIVE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DOCK_STATION_ITEM = ITEMS.register("dock_station",
            () -> new BlockItem(DOCK_STATION.get(), new Item.Properties()));

    // Typed physical media. The format is the item's identity; the content lives in components.
    public static final DeferredItem<FormattedMediaItem> FLOPPY_DISK = ITEMS.register("floppy_disk",
            () -> new FormattedMediaItem(new Item.Properties(), MediaFormat.FLOPPY, true));
    public static final DeferredItem<FormattedMediaItem> CD_ROM = ITEMS.register("cd_rom",
            () -> new FormattedMediaItem(new Item.Properties(), MediaFormat.CD, false));
    public static final DeferredItem<FormattedMediaItem> CD_RW = ITEMS.register("cd_rw",
            () -> new FormattedMediaItem(new Item.Properties(), MediaFormat.CD, true));
    public static final DeferredItem<FormattedMediaItem> DVD_ROM = ITEMS.register("dvd_rom",
            () -> new FormattedMediaItem(new Item.Properties(), MediaFormat.DVD, false));
    public static final DeferredItem<FormattedMediaItem> DVD_RW = ITEMS.register("dvd_rw",
            () -> new FormattedMediaItem(new Item.Properties(), MediaFormat.DVD, true));
    public static final DeferredItem<FormattedMediaItem> USB_FLASH_DRIVE = ITEMS.register("usb_flash_drive",
            () -> new FormattedMediaItem(new Item.Properties(), MediaFormat.USB, true));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MediaReaderBlockEntity>> MEDIA_READER_BE =
            BLOCK_ENTITIES.register("media_reader",
                    () -> BlockEntityType.Builder.of(MediaReaderBlockEntity::new,
                            FLOPPY_DRIVE.get(), CD_DRIVE.get(), DVD_DRIVE.get(), DOCK_STATION.get()).build(null));

    /*
     * The Network Gateway: a peripheral of one of our computers that is, on its other face, a ComputerCraft
     * peripheral, so the two families of computers reach each other through it. Managed from its host.
     */
    public static final DeferredBlock<NetworkGatewayBlock> NETWORK_GATEWAY =
            BLOCKS.register("network_gateway", () -> new NetworkGatewayBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.5F).sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> NETWORK_GATEWAY_ITEM = ITEMS.register("network_gateway",
            () -> new BlockItem(NETWORK_GATEWAY.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<NetworkGatewayBlockEntity>> NETWORK_GATEWAY_BE =
            BLOCK_ENTITIES.register("network_gateway",
                    () -> BlockEntityType.Builder.of(NetworkGatewayBlockEntity::new,
                            NETWORK_GATEWAY.get()).build(null));
    public static final DeferredHolder<MenuType<?>,
            MenuType<NetworkGatewayMenu>> NETWORK_GATEWAY_MENU =
            MENUS.register("network_gateway", () -> IMenuTypeExtension.create(
                    NetworkGatewayMenu::fromNetwork));

    public static final DeferredItem<MotherboardItem> MOTHERBOARD_EEB_P = ITEMS.register(
            "motherboard_eeb_p", () -> new MotherboardItem(new Item.Properties(),
                    new MotherboardSpec(FormFactor.EEB,
                            HardwareEra.STANDARD, CpuSocketId.LGA_2011, 2,
                            Set.of(RamGeneration.DDR3), 8, PcieGeneration.PCIE_3_0, 6, 6, 6)));

    /**
     * A registered disk item with its tier and size, for datagen and creative-tab iteration.
     */
    public record DiskEntry(StorageTier tier, DiskSize size, DeferredItem<DiskItem> item) {
        public String displayName() {
            return tier.productName() + " " + size.displayName();
        }
    }

    public static final List<DiskEntry> DISKS = registerDisks();

    private static List<DiskEntry> registerDisks() {
        final List<DiskEntry> disks = new ArrayList<>();
        for (final StorageTier tier : StorageTier.values()) {
            for (final DiskSize size : DiskSize.values()) {
                final String id = "disk_" + tier.name().toLowerCase(Locale.ROOT) + "_" + size.id();
                final DeferredItem<DiskItem> item = ITEMS.register(id, () -> new DiskItem(
                        new Item.Properties(),
                        new DiskSpec(tier, HardwareEra.STANDARD,
                                size.capacityItems(), tier.tdpWatts())));
                disks.add(new DiskEntry(tier, size, item));
            }
        }
        return List.copyOf(disks);
    }

    public static DiskItem disk(final StorageTier tier, final DiskSize size) {
        for (final DiskEntry entry : DISKS) {
            if (entry.tier() == tier && entry.size() == size) {
                return entry.item().get();
            }
        }
        throw new IllegalArgumentException("no registered disk for " + tier + " " + size);
    }

    // Server items

    public static final DeferredItem<ServerCaseItem> SERVER_CASE =
            ITEMS.register("server_case",
                    () -> new ServerCaseItem(new Item.Properties()));

    public static final DeferredItem<ServerItem> SERVER =
            ITEMS.register("server", () -> new ServerItem(
                    new Item.Properties()));

    /*
     * The servers of the earlier eras: a case of its era takes only boards of that era, and seats in a
     * cabinet of its era or later.
     */
    public static final DeferredItem<ServerCaseItem> LEGACY_SERVER_CASE =
            ITEMS.register("legacy_server_case",
                    () -> new ServerCaseItem(new Item.Properties()));

    public static final DeferredItem<ServerItem> LEGACY_SERVER =
            ITEMS.register("legacy_server", () -> new ServerItem(
                    new Item.Properties(),
                    RackChassis.LEGACY_SERVER));

    public static final DeferredItem<ServerCaseItem> VINTAGE_SERVER_CASE =
            ITEMS.register("vintage_server_case",
                    () -> new ServerCaseItem(new Item.Properties()));

    public static final DeferredItem<ServerItem> VINTAGE_SERVER =
            ITEMS.register("vintage_server", () -> new ServerItem(
                    new Item.Properties(),
                    RackChassis.VINTAGE_SERVER));

    public static final DeferredItem<ServerCaseItem> STORAGE_SERVER_CASE =
            ITEMS.register("storage_server_case",
                    () -> new ServerCaseItem(new Item.Properties()));

    public static final DeferredItem<ServerItem> STORAGE_SERVER =
            ITEMS.register("storage_server", () -> new ServerItem(
                    new Item.Properties(),
                    RackChassis.STORAGE_SERVER));

    // Rack units: equipment that serves the cabinet and spends the same rack-unit budget servers do.
    public static final DeferredItem<RackUnitItem> KVM_SWITCH =
            ITEMS.register("kvm_switch",
                    () -> new RackUnitItem(new Item.Properties(),
                            RackUnitItem.Kind.KVM_SWITCH));

    public static final DeferredItem<RackUnitItem> RACK_UPS =
            ITEMS.register("rack_ups",
                    () -> new RackUnitItem(new Item.Properties(),
                            RackUnitItem.Kind.RACK_UPS));

    public static final DeferredItem<RackUnitItem> COOLING_UNIT =
            ITEMS.register("cooling_unit",
                    () -> new RackUnitItem(new Item.Properties(),
                            RackUnitItem.Kind.COOLING_UNIT));

    /*
     * Bay gadgets: they occupy a gadget slot on the rack's front panel and serve the machine
     * mounted in that row.
     */
    public static final DeferredItem<RackGadgetItem> RAID_CONTROLLER =
            ITEMS.register("raid_controller",
                    () -> new RackGadgetItem(new Item.Properties(),
                            RackGadgetItem.Kind.RAID_CONTROLLER));

    public static final DeferredItem<RackGadgetItem> CACHE_CARD =
            ITEMS.register("cache_card",
                    () -> new RackGadgetItem(new Item.Properties(),
                            RackGadgetItem.Kind.CACHE_CARD));

    public static final DeferredItem<ServerCaseItem> COMPUTE_SERVER_CASE =
            ITEMS.register("compute_server_case",
                    () -> new ServerCaseItem(new Item.Properties()));

    public static final DeferredItem<ServerItem> COMPUTE_SERVER =
            ITEMS.register("compute_server", () -> new ServerItem(
                    new Item.Properties(),
                    RackChassis.COMPUTE_SERVER));

    public static ItemStack defaultServer() {
        final ItemStack stack = new ItemStack(SERVER.get());
        final NonNullList<ItemStack> hardware =
                NonNullList.withSize(
                        ServerHardwareHandler.SLOTS,
                        ItemStack.EMPTY);
        hardware.set(ServerHardwareHandler.MOBO,
                new ItemStack(MOTHERBOARD_EEB_P.get()));
        hardware.set(ServerHardwareHandler.CPU_START,
                new ItemStack(CPU_SERVO_2620.get()));
        hardware.set(ServerHardwareHandler.RAM_START,
                new ItemStack(RAM_DDR3_8192.get()));
        hardware.set(ServerHardwareHandler.PSU,
                new ItemStack(PSU_650G.get()));
        stack.set(SERVER_HARDWARE.get(),
                ItemContainerContents.fromItems(hardware));
        return stack;
    }

    /** A 2U storage server ready to run: the same board, CPU, RAM and supply as the default server. */
    public static ItemStack defaultStorageServer() {
        final ItemStack stack = new ItemStack(STORAGE_SERVER.get());
        final NonNullList<ItemStack> hardware =
                NonNullList.withSize(
                        ServerHardwareHandler.SLOTS,
                        ItemStack.EMPTY);
        hardware.set(ServerHardwareHandler.MOBO,
                new ItemStack(MOTHERBOARD_EEB_P.get()));
        hardware.set(ServerHardwareHandler.CPU_START,
                new ItemStack(CPU_SERVO_2620.get()));
        hardware.set(ServerHardwareHandler.RAM_START,
                new ItemStack(RAM_DDR3_8192.get()));
        hardware.set(ServerHardwareHandler.PSU,
                new ItemStack(PSU_650G.get()));
        stack.set(SERVER_HARDWARE.get(),
                ItemContainerContents.fromItems(hardware));
        return stack;
    }

    public static ItemStack cpulessServer() {
        final ItemStack stack = new ItemStack(SERVER.get());
        final NonNullList<ItemStack> hardware =
                NonNullList.withSize(
                        ServerHardwareHandler.SLOTS,
                        ItemStack.EMPTY);
        hardware.set(ServerHardwareHandler.MOBO,
                new ItemStack(MOTHERBOARD_EEB_P.get()));
        hardware.set(ServerHardwareHandler.RAM_START,
                new ItemStack(RAM_DDR3_8192.get()));
        hardware.set(ServerHardwareHandler.PSU,
                new ItemStack(PSU_650G.get()));
        stack.set(SERVER_HARDWARE.get(),
                ItemContainerContents.fromItems(hardware));
        return stack;
    }

    public static final DeferredItem<MotherboardItem> MOTHERBOARD_ATX_P = ITEMS.register(
            "motherboard_atx_p", () -> new MotherboardItem(new Item.Properties(),
                    new MotherboardSpec(FormFactor.ATX,
                            HardwareEra.STANDARD, CpuSocketId.AM3, 1,
                            Set.of(RamGeneration.DDR3), 4, PcieGeneration.PCIE_3_0, 4, 2, 4)));

    public static final DeferredItem<CpuItem> CPU_ASCENT_965 = ITEMS.register(
            "cpu_ascent_965", () -> new CpuItem(new Item.Properties(),
                    new CpuSpec(HardwareEra.STANDARD, CpuSocketId.AM3, 4, 3400, 125, false)));

    // Mainframe

    private static BlockBehaviour.Properties mainframeProperties() {
        /*
         * The cabinet is one GeckoLib model drawn by the controller, so the twelve blocks render
         * nothing themselves: without noOcclusion they would still cull their neighbours' faces and
         * block light, leaving a machine-shaped hole in the world around the model.
         */
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_GRAY)
                .strength(3.5F)
                .sound(SoundType.METAL)
                .noOcclusion()
                .requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<MainframeBlock> MAINFRAME = BLOCKS.register(
            "mainframe", () -> new MainframeBlock(mainframeProperties()));

    public static final DeferredItem<BlockItem> MAINFRAME_ITEM = ITEMS.register(
            "mainframe", () -> new MainframeBlockItem(
                    MAINFRAME.get(), new Item.Properties(), "mainframe"));

    /*
     * Earlier-era Mainframes: the same orchestrator and block entity, differing only by era, accepted
     * MTX board and skin. Same 3x2x2 multiblock geometry and shared parts.
     */
    public static final DeferredBlock<VintageMainframeBlock>
            VINTAGE_MAINFRAME = BLOCKS.register("vintage_mainframe",
                    () -> new VintageMainframeBlock(mainframeProperties()));

    public static final DeferredItem<BlockItem> VINTAGE_MAINFRAME_ITEM = ITEMS.register(
            "vintage_mainframe", () -> new MainframeBlockItem(
                    VINTAGE_MAINFRAME.get(), new Item.Properties(), "vintage_mainframe"));

    public static final DeferredBlock<LegacyMainframeBlock>
            LEGACY_MAINFRAME = BLOCKS.register("legacy_mainframe",
                    () -> new LegacyMainframeBlock(mainframeProperties()));

    public static final DeferredItem<BlockItem> LEGACY_MAINFRAME_ITEM = ITEMS.register(
            "legacy_mainframe", () -> new MainframeBlockItem(
                    LEGACY_MAINFRAME.get(), new Item.Properties(), "legacy_mainframe"));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MainframeBlockEntity>> MAINFRAME_BE =
            BLOCK_ENTITIES.register("mainframe",
                    () -> BlockEntityType.Builder.of(MainframeBlockEntity::new,
                            MAINFRAME.get(), VINTAGE_MAINFRAME.get(), LEGACY_MAINFRAME.get()).build(null));

    public static final DeferredBlock<MainframePartBlock> MAINFRAME_PART = BLOCKS.register(
            "mainframe_part", () -> new MainframePartBlock(mainframeProperties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MainframePartBlockEntity>> MAINFRAME_PART_BE =
            BLOCK_ENTITIES.register("mainframe_part",
                    () -> BlockEntityType.Builder.of(MainframePartBlockEntity::new, MAINFRAME_PART.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<MainframeMenu>> MAINFRAME_MENU =
            MENUS.register("mainframe", () -> IMenuTypeExtension.create(MainframeMenu::fromNetwork));

    // Personal Computer

    public static final DeferredBlock<PersonalComputerBlock> PERSONAL_COMPUTER = BLOCKS.register(
            "personal_computer", () -> new PersonalComputerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F)));

    public static final DeferredItem<BlockItem> PERSONAL_COMPUTER_ITEM = ITEMS.register(
            "personal_computer", () -> new BlockItem(PERSONAL_COMPUTER.get(), new Item.Properties()));

    /*
     * Earlier-era Personal Computers: the same machine and block entity, differing only by era, accepted
     * board and skin.
     */
    public static final DeferredBlock<VintagePersonalComputerBlock>
            VINTAGE_PERSONAL_COMPUTER = BLOCKS.register("vintage_personal_computer",
                    () -> new VintagePersonalComputerBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(2.0F)));

    public static final DeferredItem<BlockItem> VINTAGE_PERSONAL_COMPUTER_ITEM = ITEMS.register(
            "vintage_personal_computer",
            () -> new BlockItem(VINTAGE_PERSONAL_COMPUTER.get(), new Item.Properties()));

    public static final DeferredBlock<LegacyPersonalComputerBlock>
            LEGACY_PERSONAL_COMPUTER = BLOCKS.register("legacy_personal_computer",
                    () -> new LegacyPersonalComputerBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(2.0F)));

    public static final DeferredItem<BlockItem> LEGACY_PERSONAL_COMPUTER_ITEM = ITEMS.register(
            "legacy_personal_computer",
            () -> new BlockItem(LEGACY_PERSONAL_COMPUTER.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PersonalComputerBlockEntity>> PERSONAL_COMPUTER_BE =
            BLOCK_ENTITIES.register("personal_computer",
                    () -> BlockEntityType.Builder.of(PersonalComputerBlockEntity::new,
                            PERSONAL_COMPUTER.get(), VINTAGE_PERSONAL_COMPUTER.get(),
                            LEGACY_PERSONAL_COMPUTER.get()).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<PersonalComputerMenu>> PERSONAL_COMPUTER_MENU =
            MENUS.register("personal_computer", () -> IMenuTypeExtension.create(PersonalComputerMenu::fromNetwork));

    // Crafting Computer: an ATX computer that executes recipes once a Crafting Card is installed

    public static final DeferredBlock<CraftingComputerBlock> CRAFTING_COMPUTER = BLOCKS.register(
            "crafting_computer", () -> new CraftingComputerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GRAY)
                    .strength(2.0F)));

    public static final DeferredItem<BlockItem> CRAFTING_COMPUTER_ITEM = ITEMS.register(
            "crafting_computer", () -> new BlockItem(CRAFTING_COMPUTER.get(), new Item.Properties()));

    /*
     * Earlier-era Crafting Computers: the same machine and block entity, differing only by era, accepted
     * board and skin.
     */
    public static final DeferredBlock<VintageCraftingComputerBlock>
            VINTAGE_CRAFTING_COMPUTER = BLOCKS.register("vintage_crafting_computer",
                    () -> new VintageCraftingComputerBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(2.0F)));

    public static final DeferredItem<BlockItem> VINTAGE_CRAFTING_COMPUTER_ITEM = ITEMS.register(
            "vintage_crafting_computer",
            () -> new BlockItem(VINTAGE_CRAFTING_COMPUTER.get(), new Item.Properties()));

    public static final DeferredBlock<LegacyCraftingComputerBlock>
            LEGACY_CRAFTING_COMPUTER = BLOCKS.register("legacy_crafting_computer",
                    () -> new LegacyCraftingComputerBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(2.0F)));

    public static final DeferredItem<BlockItem> LEGACY_CRAFTING_COMPUTER_ITEM = ITEMS.register(
            "legacy_crafting_computer",
            () -> new BlockItem(LEGACY_CRAFTING_COMPUTER.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraftingComputerBlockEntity>> CRAFTING_COMPUTER_BE =
            BLOCK_ENTITIES.register("crafting_computer",
                    () -> BlockEntityType.Builder.of(CraftingComputerBlockEntity::new,
                            CRAFTING_COMPUTER.get(), VINTAGE_CRAFTING_COMPUTER.get(),
                            LEGACY_CRAFTING_COMPUTER.get()).build(null));

    public static final DeferredHolder<MenuType<?>,
            MenuType<CraftingComputerMenu>> CRAFTING_COMPUTER_MENU =
            MENUS.register("crafting_computer", () -> IMenuTypeExtension.create(
                    CraftingComputerMenu::fromNetwork));

    /*
     * Cluster Management Computer: a full computer that, with a Cluster Interface Card, drives every
     * supercomputer fabric and datacenter section on its network as one machine. Three eras.
     */
    public static final DeferredBlock<ClusterManagementComputerBlock>
            CLUSTER_MANAGEMENT_COMPUTER = BLOCKS.register("cluster_management_computer",
                    () -> new ClusterManagementComputerBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0F)));
    public static final DeferredItem<BlockItem> CLUSTER_MANAGEMENT_COMPUTER_ITEM = ITEMS.register(
            "cluster_management_computer",
            () -> new BlockItem(CLUSTER_MANAGEMENT_COMPUTER.get(), new Item.Properties()));
    public static final DeferredBlock<VintageClusterManagementComputerBlock>
            VINTAGE_CLUSTER_MANAGEMENT_COMPUTER = BLOCKS.register("vintage_cluster_management_computer",
                    () -> new VintageClusterManagementComputerBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0F)));
    public static final DeferredItem<BlockItem> VINTAGE_CLUSTER_MANAGEMENT_COMPUTER_ITEM = ITEMS.register(
            "vintage_cluster_management_computer",
            () -> new BlockItem(VINTAGE_CLUSTER_MANAGEMENT_COMPUTER.get(), new Item.Properties()));
    public static final DeferredBlock<LegacyClusterManagementComputerBlock>
            LEGACY_CLUSTER_MANAGEMENT_COMPUTER = BLOCKS.register("legacy_cluster_management_computer",
                    () -> new LegacyClusterManagementComputerBlock(
                            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(2.0F)));
    public static final DeferredItem<BlockItem> LEGACY_CLUSTER_MANAGEMENT_COMPUTER_ITEM = ITEMS.register(
            "legacy_cluster_management_computer",
            () -> new BlockItem(LEGACY_CLUSTER_MANAGEMENT_COMPUTER.get(), new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<ClusterManagementComputerBlockEntity>>
            CLUSTER_MANAGEMENT_COMPUTER_BE = BLOCK_ENTITIES.register("cluster_management_computer",
                    () -> BlockEntityType.Builder.of(
                            ClusterManagementComputerBlockEntity::new,
                            CLUSTER_MANAGEMENT_COMPUTER.get(), VINTAGE_CLUSTER_MANAGEMENT_COMPUTER.get(),
                            LEGACY_CLUSTER_MANAGEMENT_COMPUTER.get()).build(null));
    public static final DeferredHolder<MenuType<?>,
            MenuType<ClusterManagementComputerMenu>>
            CLUSTER_MANAGEMENT_COMPUTER_MENU = MENUS.register("cluster_management_computer",
                    () -> IMenuTypeExtension.create(
                            ClusterManagementComputerMenu::fromNetwork));

    /*
     * Supercomputer: the nodes are rack computers seated in Supercomputer Racks; the racks are tied
     * together by the high-compute fabric and uplinked to the data network by one HBW Interface.
     */

    public static final DeferredItem<ServerItem> SUPERCOMPUTER_NODE =
            ITEMS.register("supercomputer_node", () -> new ServerItem(
                    new Item.Properties(),
                    RackChassis.SUPERCOMPUTER_NODE));

    /** A node ready to run: board, CPU, RAM, supply, and the entry crafting co-processor in its slot. */
    public static ItemStack defaultSupercomputerNode() {
        final ItemStack stack = new ItemStack(SUPERCOMPUTER_NODE.get());
        final NonNullList<ItemStack> hardware =
                NonNullList.withSize(
                        ServerHardwareHandler.SLOTS,
                        ItemStack.EMPTY);
        hardware.set(ServerHardwareHandler.MOBO,
                new ItemStack(MOTHERBOARD_EEB_P.get()));
        hardware.set(ServerHardwareHandler.CPU_START,
                new ItemStack(CPU_SERVO_2620.get()));
        hardware.set(ServerHardwareHandler.RAM_START,
                new ItemStack(RAM_DDR3_8192.get()));
        hardware.set(ServerHardwareHandler.GPU_START,
                new ItemStack(PHI_5100.get()));
        hardware.set(ServerHardwareHandler.PSU,
                new ItemStack(PSU_650G.get()));
        stack.set(SERVER_HARDWARE.get(),
                ItemContainerContents.fromItems(hardware));
        return stack;
    }

    public static final DeferredBlock<HbwInterfaceBlock>
            HBW_INTERFACE = BLOCKS.register("hbw_interface",
                    () -> new HbwInterfaceBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.COLOR_GRAY)
                                    .strength(2.0F)
                                    .noOcclusion()));

    public static final DeferredItem<BlockItem> HBW_INTERFACE_ITEM = ITEMS.register(
            "hbw_interface", () -> new BlockItem(HBW_INTERFACE.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>,
            BlockEntityType<HbwInterfaceBlockEntity>>
            HBW_INTERFACE_BE = BLOCK_ENTITIES.register("hbw_interface",
                    () -> BlockEntityType.Builder.of(
                            HbwInterfaceBlockEntity::new,
                            HBW_INTERFACE.get()).build(null));

    public static final DeferredItem<PhiCoprocessorItem> PHI_5100 =
            ITEMS.register("phi_5100", () -> new PhiCoprocessorItem(
                    new Item.Properties(), new PhiCoprocessorSpec(
                            IndustrialTier.T3, 2, 60, 1050, 225)));

    public static final DeferredItem<PhiCoprocessorItem> PHI_7120 =
            ITEMS.register("phi_7120", () -> new PhiCoprocessorItem(
                    new Item.Properties(), new PhiCoprocessorSpec(
                            IndustrialTier.T4, 3, 61, 1240, 250)));

    public static final DeferredItem<PhiCoprocessorItem> PHI_7290 =
            ITEMS.register("phi_7290", () -> new PhiCoprocessorItem(
                    new Item.Properties(), new PhiCoprocessorSpec(
                            IndustrialTier.T4, 4, 72, 1500, 270)));

    public static final DeferredItem<PhiCoprocessorItem> PHI_9000 =
            ITEMS.register("phi_9000", () -> new PhiCoprocessorItem(
                    new Item.Properties(), new PhiCoprocessorSpec(
                            IndustrialTier.T5, 6, 96, 1800, 300)));

    public static final DeferredHolder<MenuType<?>,
            MenuType<ServerRouterMenu>> SERVER_ROUTER_MENU =
            MENUS.register("server_router", () -> IMenuTypeExtension.create(
                    ServerRouterMenu::fromNetwork));

    public static void register(final IEventBus modEventBus) {
        /*
         * Force the per-era hardware catalog to load so its items register onto ITEMS before the
         * DeferredRegister is handed to the mod event bus below.
         */
        HardwareItems.init();
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        MENUS.register(modEventBus);
        COMPONENTS.register(modEventBus);
        TRIGGERS.register(modEventBus);
    }
}
