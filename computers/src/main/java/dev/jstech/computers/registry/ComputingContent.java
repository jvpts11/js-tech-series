/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.OsBootstrap;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.media.InstallMedia;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.ModContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What J's Computers puts in the game is declared through {@link #CONTENT}, and shown in its one creative tab, whose
 * sections are declared here in the order the tab shows them. Every mod of the series carries its own tab.
 *
 * <p>Nothing here names a block or an item except lazily, so the catalogue classes that declare into these sections
 * can load in any order.
 */
public final class ComputingContent {

    public static final ModContent CONTENT = new ModContent(JsComputers.MODID);

    public static final ContentTab TAB = CONTENT.tab("computing", "J's Computers", () -> ComputingModule.MAINFRAME);

    /** The cables and routers a network is wired with. */
    public static final ContentTab.Section NETWORK = TAB.section();

    /** The machines: Mainframes, monitors, computers, and the node a supercomputer is built from. */
    public static final ContentTab.Section MACHINES = TAB.section();

    /** What ties a local cluster together: the fabric cables, the crafting switch and the uplink. */
    public static final ContentTab.Section CLUSTER = TAB.section();

    /** The devices at a desk: pattern encoders, drives and the gateway. */
    public static final ContentTab.Section DEVICES = TAB.section();

    /** Blank media, then an installer of every system and every installable program. */
    public static final ContentTab.Section MEDIA = TAB.section().alsoShowing(ComputingContent::installers);

    /** The cabinets, then the buses that mount on a cable. */
    public static final ContentTab.Section RACKS = TAB.section();

    /** Server cases and the servers assembled in them. */
    public static final ContentTab.Section SERVERS = TAB.section();

    /** What a rack carries besides servers: bay gadgets and rack units. */
    public static final ContentTab.Section RACK_EQUIPMENT = TAB.section();

    /** The parts of the first machines: boards, processors, memory, cards and a supply. */
    public static final ContentTab.Section PARTS = TAB.section();

    /** The disks of every tier and size. */
    public static final ContentTab.Section DISKS = TAB.section();

    /**
     * The hardware catalogue of every era, era by era. The order is asked for only when the tab is filled: reading it
     * now would load the catalogue, which declares into this section before it exists.
     */
    public static final ContentTab.Section CATALOGUE =
            TAB.section().sortedBy((one, other) -> HardwareItems.CREATIVE_ORDER.compare(one, other));

    private ComputingContent() {
    }

    /**
     * An installer on the medium of its generation, one per registered system and per installable program, straight
     * from the registries: Vintage on a floppy, Legacy on a CD, a Standard system on a bootable flash drive, a
     * Standard application on a DVD. Size never decides, so a small server daemon is not a floppy.
     */
    private static void installers(final CreativeModeTab.Output output) {
        for (final OsDef os : OsBootstrap.builtinOses()) {
            output.accept(stamped(InstallMedia.forSystem(os.minEra()), MediaKind.OS_INSTALL, os.id()));
        }
        for (final ProgramSpec program : OsBootstrap.builtinPrograms()) {
            if (program.installable()) {
                output.accept(stamped(InstallMedia.forProgram(program.era(), program.kind()),
                        MediaKind.PROGRAM_INSTALL, program.id()));
            }
        }
    }

    private static ItemStack stamped(final MediaFormat format, final MediaKind kind, final ResourceLocation payload) {
        final ItemStack disc = new ItemStack(mediumFor(format));
        MediaItem.setKind(disc, kind);
        MediaItem.setPayload(disc, payload);
        return disc;
    }

    /** The blank medium item of a physical format, to stamp an installer onto. */
    private static Item mediumFor(final MediaFormat format) {
        return switch (format) {
            case FLOPPY -> ComputingModule.FLOPPY_DISK.get();
            case CD -> ComputingModule.CD_ROM.get();
            case DVD -> ComputingModule.DVD_ROM.get();
            case USB -> ComputingModule.USB_FLASH_DRIVE.get();
        };
    }
}
