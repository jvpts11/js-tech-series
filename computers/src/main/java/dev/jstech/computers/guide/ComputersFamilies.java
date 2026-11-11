/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.MediaDevices;
import dev.jstech.computers.block.AccessPointBlock;
import dev.jstech.computers.block.ChargingDockBlock;
import dev.jstech.computers.block.ClusterManagementComputerBlock;
import dev.jstech.computers.block.CraftingComputerBlock;
import dev.jstech.computers.block.ExtendedCdDriveBlock;
import dev.jstech.computers.block.HbwInterfaceBlock;
import dev.jstech.computers.block.HubBlock;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.block.NetworkGatewayBlock;
import dev.jstech.computers.block.PatternEncoderBlock;
import dev.jstech.computers.block.PersonalComputerBlock;
import dev.jstech.computers.block.PrinterBlock;
import dev.jstech.computers.block.QuinceComputerBlock;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.block.RepeaterBlock;
import dev.jstech.computers.block.RouterBlock;
import dev.jstech.computers.block.ScannerBlock;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.ServerRouterBlock;
import dev.jstech.computers.block.SpeakerBlock;
import dev.jstech.computers.block.SubwooferBlock;
import dev.jstech.computers.block.UpsBlock;
import dev.jstech.computers.block.VinylDriveBlock;
import dev.jstech.computers.block.WanGatewayBlock;
import dev.jstech.computers.block.WorkbenchBlock;
import dev.jstech.computers.block.part.CablePartItem;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.hardware.AnchorCardSpec;
import dev.jstech.computers.hardware.CaseFanSpec;
import dev.jstech.computers.hardware.ClusterInterfaceCardSpec;
import dev.jstech.computers.hardware.CoolerSpec;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.NetworkCardSpec;
import dev.jstech.computers.hardware.PhiCoprocessorSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.computers.hardware.StorageCardSpec;
import dev.jstech.computers.hardware.WanCardSpec;
import dev.jstech.computers.hardware.WifiCardSpec;
import dev.jstech.computers.hardware.WorkshopCardSpec;
import dev.jstech.computers.item.BayDriveItem;
import dev.jstech.computers.item.PrintedPaperItem;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.item.ServerCaseItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.item.TapeItem;
import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaReaderBlock;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.guide.GuideFamilies;
import dev.jstech.core.multipart.PartType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.ItemLike;

/**
 * The families of items the Computers' manuals are the pages of, each named after the entry that covers it: every
 * processor, every monitor, every rack server. An entry names its family in its header ({@code covers_family:
 * jsc:processors}), and the family is asked for as the manuals are read, so a part the mod adds later has its page
 * with nothing more to write. A family is known by what a thing is: the sheet of a part, the class of a block or an
 * item, the grid of a cable.
 */
final class ComputersFamilies {

    private ComputersFamilies() {
    }

    /** Registers every family, once, as the mod is built. */
    static void register() {
        family("motherboards", hardwareOf(MotherboardSpec.class));
        family("processors", hardwareOf(CpuSpec.class));
        family("memory", hardwareOf(RamSpec.class));
        family("graphics_cards", hardwareOf(GpuSpec.class));
        family("disks", hardwareOf(DiskSpec.class));
        family("power_supplies", hardwareOf(PsuSpec.class));
        family("cooling", hardwareOf(CoolerSpec.class, CaseFanSpec.class));
        family("sound_cards", hardwareOf(SoundCardSpec.class));
        family("expansion_cards", hardwareOf(WorkshopCardSpec.class, ClusterInterfaceCardSpec.class,
                NetworkCardSpec.class, AnchorCardSpec.class, PhiCoprocessorSpec.class, WanCardSpec.class));
        family("crafting_cards", hardwareOf(CraftingCardSpec.class));
        family("raid_arrays", hardwareOf(StorageCardSpec.class));
        family("case_front", itemsOf(BayDriveItem.class));
        family("upses", blocksOf(UpsBlock.class));
        family("building_chain_machines",
                () -> ChainMachineBlocks.shared().stream().map(block -> (ItemLike) block.get()).toList());
        family("electronics_workbench", blocksOf(WorkbenchBlock.class));
        family("chain_parts", () -> ChainParts.ERAS.stream()
                .flatMap(era -> ComputingContent.shelf(Shelf.CHAIN, era).items().stream())
                .filter(item -> !(item instanceof BlockItem)).map(item -> (ItemLike) item).toList());
        family("personal_computers", blocksOf(PersonalComputerBlock.class));
        family("quince_computers", blocksOf(QuinceComputerBlock.class));
        family("crafting_computers", blocksOf(CraftingComputerBlock.class));
        family("mainframes", blocksOf(MainframeBlock.class));
        family("wan_gateways", blocksOf(WanGatewayBlock.class));
        family("server_racks", blocksOf(ServerRackBlock.class), itemsOf(ServerItem.class, ServerCaseItem.class));
        family("rack_equipment", itemsOf(RackUnitItem.class));
        family("cluster_management_computers", blocksOf(ClusterManagementComputerBlock.class));
        family("hbw_interface", blocksOf(HbwInterfaceBlock.class));
        family("server_router", blocksOf(ServerRouterBlock.class));
        family("network_gateway", blocksOf(NetworkGatewayBlock.class));
        family("data_cables", cablesOn(GridKind.DATA, ComputingModule.CRAFTING_CABLE));
        family("routers", blocksOf(RouterBlock.class, RepeaterBlock.class));
        family("portables", itemsOf(PortableItem.class), blocksOf(ChargingDockBlock.class));
        family("wireless", blocksOf(AccessPointBlock.class), hardwareOf(WifiCardSpec.class));
        family("backups", itemsOf(TapeItem.class));
        family("monitors", blocksOf(MonitorBlock.class));
        family("peripheral_cables", cablesOn(GridKind.PERIPHERAL), blocksOf(HubBlock.class));
        family("drives", blocksOf(MediaReaderBlock.class));
        family("extended_cd_drives", blocksOf(ExtendedCdDriveBlock.class));
        family("vinyl_rw_drives", blocksOf(VinylDriveBlock.class),
                () -> List.of(MediaDevices.BLANK_VINYL_RECORD.get()));
        family("scanners", blocksOf(ScannerBlock.class));
        family("printers", blocksOf(PrinterBlock.class), itemsOf(PrintedPaperItem.class));
        family("speakers", blocksOf(SpeakerBlock.class, SubwooferBlock.class));
        family("redstone_interfaces", blocksOf(RedstoneInterfaceBlock.class));
        family("installing", itemsOf(FormattedMediaItem.class));
        family("pattern_encoders", blocksOf(PatternEncoderBlock.class));
        family("crafting_interfaces", partsOf(ComputingParts::isCrafting));
        family("buses", partsOf(kind -> ComputingParts.isImport(kind) || ComputingParts.isExport(kind)
                || ComputingParts.isExternal(kind)));
    }

    /* Registers the family of the entry {@code entry}: the items of every one of {@code kinds}, in that order. */
    @SafeVarargs
    private static void family(final String entry, final Supplier<List<ItemLike>>... kinds) {
        GuideFamilies.register("jsc:" + entry, () -> {
            final List<String> ids = new ArrayList<>();
            for (final Supplier<List<ItemLike>> kind : kinds) {
                for (final ItemLike item : kind.get()) {
                    final String id = BuiltInRegistries.ITEM.getKey(item.asItem()).toString();
                    if (!ids.contains(id)) {
                        ids.add(id);
                    }
                }
            }
            return ids;
        });
    }

    /* Every item the mod registers that is of one of those kinds. */
    private static Supplier<List<ItemLike>> itemsOf(final Class<?>... kinds) {
        return () -> ComputingContent.CONTENT.declaredItems().stream().map(entry -> (ItemLike) entry.get())
                .filter(item -> isOf(item, kinds)).toList();
    }

    /* Every part whose sheet is of one of those kinds: a part is known by its entry in the hardware registry. */
    private static Supplier<List<ItemLike>> hardwareOf(final Class<?>... sheets) {
        return () -> ComputingContent.CONTENT.declaredItems().stream().map(entry -> (ItemLike) entry.get())
                .filter(item -> {
                    final HardwareRegistry.Part part = HardwareRegistry.part(item.asItem());
                    return part != null && isOf(part.spec(), sheets);
                }).toList();
    }

    /* The items of every block the mod registers that is of one of those kinds. */
    private static Supplier<List<ItemLike>> blocksOf(final Class<?>... kinds) {
        return () -> ComputingContent.CONTENT.declaredBlocks().stream().filter(BlockEntry::hasItem)
                .filter(entry -> isOf(entry.get(), kinds)).map(entry -> (ItemLike) entry.get()).toList();
    }

    /* The cable items of every cable the mod registers on {@code grid}, but those named. */
    private static Supplier<List<ItemLike>> cablesOn(final GridKind grid, final CableEntry... except) {
        final List<CableEntry> left = List.of(except);
        return () -> ComputingContent.CONTENT.declaredCables().stream()
                .filter(entry -> entry.get().grid() == grid && !left.contains(entry))
                .map(entry -> (ItemLike) entry).toList();
    }

    /* The items of every cable part the mod registers whose kind passes {@code kind}. */
    private static Supplier<List<ItemLike>> partsOf(final Predicate<PartType<?>> kind) {
        return () -> ComputingContent.CONTENT.declaredItems().stream()
                .filter(entry -> entry.get() instanceof CablePartItem part && kind.test(part.partType()))
                .map(entry -> (ItemLike) entry.get()).toList();
    }

    private static boolean isOf(final Object thing, final Class<?>... kinds) {
        for (final Class<?> kind : kinds) {
            if (kind.isInstance(thing)) {
                return true;
            }
        }
        return false;
    }
}
