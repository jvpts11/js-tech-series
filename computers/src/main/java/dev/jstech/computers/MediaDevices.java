/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;
import static dev.jstech.computers.registry.ComputingContent.shelf;

import dev.jstech.computers.block.ExtendedCdDriveBlock;
import dev.jstech.computers.block.ScannerBlock;
import dev.jstech.computers.block.VinylDriveBlock;
import dev.jstech.computers.blockentity.ExtendedCdDriveBlockEntity;
import dev.jstech.computers.blockentity.ScannerBlockEntity;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.computers.item.CabinetBlockItem;
import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The devices that copy music, media and books (computers/docs/design/peripherals.md): the flatbed scanners of every
 * era, which read books, enchanted books, maps and printed sheets into files; the Vinyl RW Drives, which record a music
 * disc and cut a recording of one onto a blank vinyl; and the blank vinyl itself.
 */
public final class MediaDevices {

    public static final BlockEntry<ScannerBlock> VINTAGE_SCANNER =
            scanner("vintage_scanner", MapColor.TERRACOTTA_WHITE, HardwareEra.VINTAGE).named("Pakard ScanJot")
                    .register();
    public static final BlockEntry<ScannerBlock> LEGACY_SCANNER =
            scanner("legacy_scanner", MapColor.COLOR_LIGHT_GRAY, HardwareEra.LEGACY).named("Epsilon Perfecta 1240U")
                    .register();
    public static final BlockEntry<ScannerBlock> TRANSITION_SCANNER =
            scanner("transition_scanner", MapColor.COLOR_GRAY, HardwareEra.TRANSITION)
                    .named("Pakard ScanJot G4050").register();
    public static final BlockEntry<ScannerBlock> SCANNER =
            scanner("scanner", MapColor.COLOR_BLACK, HardwareEra.STANDARD).named("Epsilon Perfecta V600").register();
    public static final BlockEntry<ScannerBlock> ADVANCED_SCANNER =
            scanner("advanced_scanner", MapColor.COLOR_GRAY, HardwareEra.ADVANCED).named("Epsilon Perfecta V39")
                    .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScannerBlockEntity>> SCANNER_BE =
            CONTENT.blockEntity("scanner", ScannerBlockEntity::new, VINTAGE_SCANNER, LEGACY_SCANNER,
                    TRANSITION_SCANNER, SCANNER, ADVANCED_SCANNER);

    public static final BlockEntry<VinylDriveBlock> VINTAGE_VINYL_DRIVE =
            vinylDrive("vintage_vinyl_rw_drive", MapColor.WOOD, HardwareEra.VINTAGE)
                    .named("Vinyl RW Drive (Vintage)").register();
    public static final BlockEntry<VinylDriveBlock> LEGACY_VINYL_DRIVE =
            vinylDrive("legacy_vinyl_rw_drive", MapColor.COLOR_LIGHT_GRAY, HardwareEra.LEGACY)
                    .named("Vinyl RW Drive (Legacy)").register();
    public static final BlockEntry<VinylDriveBlock> TRANSITION_VINYL_DRIVE =
            vinylDrive("transition_vinyl_rw_drive", MapColor.COLOR_BLACK, HardwareEra.TRANSITION)
                    .named("Vinyl RW Drive (Transition)").register();
    public static final BlockEntry<VinylDriveBlock> VINYL_DRIVE =
            vinylDrive("vinyl_rw_drive", MapColor.COLOR_GRAY, HardwareEra.STANDARD).named("Vinyl RW Drive")
                    .register();
    public static final BlockEntry<VinylDriveBlock> ADVANCED_VINYL_DRIVE =
            vinylDrive("advanced_vinyl_rw_drive", MapColor.SNOW, HardwareEra.ADVANCED)
                    .named("Vinyl RW Drive (Advanced)").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VinylDriveBlockEntity>> VINYL_DRIVE_BE =
            CONTENT.blockEntity("vinyl_rw_drive", VinylDriveBlockEntity::new, VINTAGE_VINYL_DRIVE, LEGACY_VINYL_DRIVE,
                    TRANSITION_VINYL_DRIVE, VINYL_DRIVE, ADVANCED_VINYL_DRIVE);
    public static final BlockEntry<ExtendedCdDriveBlock> LEGACY_EXTENDED_CD_DRIVE =
            extendedCdDrive("legacy_extended_cd_drive", MapColor.COLOR_LIGHT_GRAY, HardwareEra.LEGACY)
                    .named("Extended CD Drive (Legacy)").register();
    public static final BlockEntry<ExtendedCdDriveBlock> TRANSITION_EXTENDED_CD_DRIVE =
            extendedCdDrive("transition_extended_cd_drive", MapColor.COLOR_BLACK, HardwareEra.TRANSITION)
                    .named("Extended CD Drive (Transition)").register();
    public static final BlockEntry<ExtendedCdDriveBlock> ADVANCED_EXTENDED_CD_DRIVE =
            extendedCdDrive("advanced_extended_cd_drive", MapColor.SNOW, HardwareEra.ADVANCED)
                    .named("Extended CD Drive (Advanced)").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ExtendedCdDriveBlockEntity>>
            EXTENDED_CD_DRIVE_BE = CONTENT.blockEntity("extended_cd_drive", ExtendedCdDriveBlockEntity::new,
                    LEGACY_EXTENDED_CD_DRIVE, TRANSITION_EXTENDED_CD_DRIVE, ADVANCED_EXTENDED_CD_DRIVE);
    /** The record a Vinyl RW Drive cuts a recording from a disc onto: it becomes that disc. */
    public static final ItemEntry<Item> BLANK_VINYL_RECORD = CONTENT.item("blank_vinyl_record",
                    properties -> new Item(properties.stacksTo(16))).named("Blank Vinyl Record")
            .tab(shelf(Shelf.PERIPHERALS, HardwareEra.VINTAGE)).register();

    private MediaDevices() {
    }

    public static void init() {
        // Referencing the class runs the declarations above.
    }

    /*
     * A scanner, whose body its block entity draws: the block shows nothing but the particles a break scatters. Its
     * item shows the scanner at rest, its lid closed, its lamps dark and nothing on the glass.
     */
    private static BlockBuilder<ScannerBlock> scanner(final String id, final MapColor color, final Era era) {
        final String model = ComputingLooks.scanner(era);
        return CONTENT.block(id, properties -> new ScannerBlock(properties, era))
                .properties(properties -> properties.mapColor(color).strength(1.0F).sound(SoundType.METAL)
                        .noOcclusion())
                .look(IBlockLook.fixed(new IBlockModel.ParticleOnly(id + "_body",
                        "block/scanner/" + model + "_particle")))
                .geo(ComputingLooks.SCANNER)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.SCANNER, model,
                        ComputingModule.DEVICE_FIT, ComputingLooks.SCANNER_AT_REST))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(shelf(Shelf.PERIPHERALS, era));
    }

    /* An Extended CD Drive, a tower two blocks tall drawn by its lower block's entity; its item shows it at rest. */
    private static BlockBuilder<ExtendedCdDriveBlock> extendedCdDrive(final String id, final MapColor color,
                                                                     final Era era) {
        final String model = ComputingLooks.extendedCdDrive(era);
        return CONTENT.block(id, properties -> new ExtendedCdDriveBlock(properties, era))
                .properties(properties -> properties.mapColor(color).strength(1.5F).sound(SoundType.METAL)
                        .noOcclusion())
                .look(IBlockLook.fixed(new IBlockModel.ParticleOnly(id + "_body",
                        "block/disc_tower/" + model + "_particle")))
                .geo(ComputingLooks.EXTENDED_CD_DRIVE)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.EXTENDED_CD_DRIVE,
                        model, ComputingModule.DEVICE_FIT, ComputingLooks.EXTENDED_CD_DRIVE_AT_REST))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(shelf(Shelf.PERIPHERALS, era));
    }

    /* A Vinyl RW Drive, drawn by its block entity; its item shows it at rest, nothing on its platter, its lamp dark. */
    private static BlockBuilder<VinylDriveBlock> vinylDrive(final String id, final MapColor color, final Era era) {
        final String model = ComputingLooks.vinylDrive(era);
        return CONTENT.block(id, properties -> new VinylDriveBlock(properties, era))
                .properties(properties -> properties.mapColor(color).strength(1.5F).sound(SoundType.WOOD)
                        .noOcclusion())
                .look(IBlockLook.fixed(new IBlockModel.ParticleOnly(id + "_body",
                        "block/vinyl/" + model + "_particle")))
                .geo(ComputingLooks.VINYL_DRIVE)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.VINYL_DRIVE, model,
                        ComputingModule.DEVICE_FIT, ComputingLooks.VINYL_DRIVE_AT_REST))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(shelf(Shelf.PERIPHERALS, era));
    }
}
