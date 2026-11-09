/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import com.mojang.serialization.Codec;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.crafting.CraftingEras;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.hardware.PsuCondition;
import dev.jstech.computers.hardware.PsuConditions;
import dev.jstech.computers.os.DiskSystems;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.portable.PortableLink;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.PrintedDocuments;
import dev.jstech.computers.storage.DiskHealth;
import dev.jstech.computers.storage.DiskUsage;
import dev.jstech.computers.storage.ServerStorageContents;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.persistence.SaveLayout;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The data the mod's items carry: what a server stores and is built from, what a disk holds and boots, what a medium
 * carries, and the names a player gives them.
 *
 * <p>Every one is saved with the version of its layout beside its value ({@link SaveLayout#codec}), so a later change
 * to what one holds can bring an item saved before it up to date; a value saved before components had versions is
 * read as version 0, as it was written. What travels to the players is the value alone.
 */
public final class ComputingComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(JsComputers.MODID);

    /*
     * A Server item carries its state in its NBT: the items it stores, the hardware it is built from, and its network
     * node identity.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ServerStorageContents>>
            SERVER_STORAGE = saved("server_storage", ServerStorageContents.CODEC, ServerStorageContents.STREAM_CODEC);

    /*
     * A drive's stored items live in the save-wide volume store, not on the item: the item carries the
     * volume's id and a usage summary, so a drive holding thousands of types stays a tiny item.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>>
            DISK_VOLUME = saved("disk_volume", UUIDUtil.CODEC, UUIDUtil.STREAM_CODEC);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiskUsage>>
            DISK_USAGE = saved("disk_usage", DiskUsage.CODEC, DiskUsage.STREAM_CODEC);

    /*
     * What corruption did to a drive, beside its usage summary: what waits for the Defrag, the bad sectors and a death.
     * Absent on a drive nothing happened to.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiskHealth>>
            DISK_HEALTH = saved("disk_health", DiskHealth.CODEC, DiskHealth.STREAM_CODEC);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>>
            SERVER_HARDWARE = saved("server_hardware", ItemContainerContents.CODEC,
                    ItemContainerContents.STREAM_CODEC);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>>
            SERVER_NODE_UUID = saved("server_node_uuid", UUIDUtil.CODEC, UUIDUtil.STREAM_CODEC);

    /*
     * A RAID controller card carries its array configuration: the mode it runs and how many member
     * drives the array was formed with (so a missing member reads as degraded rather than smaller).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>>
            RAID_MODE = saved("raid_mode", Codec.STRING, ByteBufCodecs.STRING_UTF8);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            RAID_MEMBERS = saved("raid_members", Codec.INT, ByteBufCodecs.VAR_INT);

    /* The size of the volume a RAID controller's array was formed with, kept so a degraded array does not resize. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>>
            RAID_VOLUME = saved("raid_volume", Codec.LONG, ByteBufCodecs.VAR_LONG);

    /*
     * A rack server's software state (console history, installed programs, settings) persists WITH
     * the item, so it moves between racks with the machine. It travels to the players as the game sends a component
     * that names no form of its own: its saved form, read with the registries.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>>
            SERVER_CONSOLE = saved("server_console", CompoundTag.CODEC,
                    ByteBufCodecs.fromCodecWithRegistries(CompoundTag.CODEC));

    /*
     * The software a disk carries: installed programs and their versions, the desktop preferences, and
     * the shell history. It rides on the DISK, not on the computer, because that is what it is: moving
     * a system disk to another machine takes its programs along, and a fresh disk boots clean. Keeping
     * this on the block entity meant a newly installed system still believed the old one's programs
     * were present. It travels to the players as the server console does.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>>
            DISK_CONSOLE = saved("disk_console", CompoundTag.CODEC,
                    ByteBufCodecs.fromCodecWithRegistries(CompoundTag.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>>
            COMPUTER_NAME = saved("computer_name", Codec.STRING, ByteBufCodecs.STRING_UTF8);

    /*
     * How much of a (non-Server) computer disk's storage is public, as a per-mille 0..1000. The
     * component rides on the disk ItemStack so the split travels with the disk when it is pulled
     * and reinserted. An absent component reads as fully private (see DriveVolumes.publicPermille).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            DISK_PUBLIC_PERMILLE = saved("disk_public_permille", Codec.intRange(0, 1000), ByteBufCodecs.VAR_INT);

    /*
     * The media: components that together describe the content of a MediaItem. A medium carries exactly one kind
     * and the matching content component for that kind.
     */

    // Installer payload (OS_INSTALL / PROGRAM_INSTALL): the OS or program id.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>>
            MEDIA_PAYLOAD = saved("media_payload", ResourceLocation.CODEC, ResourceLocation.STREAM_CODEC);

    // Which disk of a system that comes on several this one is, counting from one; absent on the first or only one.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            MEDIA_PART = saved("media_part", Codec.intRange(1, 16), ByteBufCodecs.VAR_INT);

    /*
     * Which of the three content kinds this medium carries. Absent component → OS_INSTALL (safe
     * default that keeps legacy blank media behaving as installer media).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MediaKind>>
            MEDIA_KIND = saved("media_kind", StableCodecs.byName(MediaKind.class),
                    StableCodecs.byId(MediaKind.class, MediaKind.OS_INSTALL));

    // Data contents (DATA kind): a portable item/fluid storage snapshot.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ServerStorageContents>>
            MEDIA_DATA = saved("media_data", ServerStorageContents.CODEC, ServerStorageContents.STREAM_CODEC);

    // Capacity of a DATA medium in item-equivalents. Absent → MediaItem.DEFAULT_CAPACITY.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            MEDIA_CAPACITY = saved("media_capacity", Codec.INT, ByteBufCodecs.VAR_INT);

    /*
     * Disk filesystem components: files and the installed OS live on the disk's stack so
     * they travel with the disk when it is inserted or removed.
     */

    // The filesystem contents of a disk volume: path-keyed map of stored files.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilesystemContents>>
            FILESYSTEM = saved("filesystem", FilesystemContents.CODEC, FilesystemContents.STREAM_CODEC);

    /*
     * The systems a disk carries and which of them it boots, with what each of them remembers about having been
     * met. Present only on bootable disks; absent on plain data disks. It used to be one system written straight
     * onto the disk, so installing a second wrote over the first, and one mark for the disk rather than one per
     * system, which would have had a second system arrive already met.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiskSystems>>
            DISK_SYSTEMS = saved("disk_systems", DiskSystems.CODEC, DiskSystems.STREAM_CODEC);

    /*
     * A disk its system had in use when its machine went dark suddenly, as a real file system's dirty bit: the next
     * system to come up from it checks it first. It rides on the disk, so a disk moved to another machine is checked
     * there. Absent on a disk shut down cleanly.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>>
            DISK_UNCLEAN = saved("disk_unclean", Codec.BOOL, ByteBufCodecs.BOOL);

    /*
     * A user-chosen label for a disk or media volume, shown in This PC and the explorer drive tree and
     * editable there. Rides on the ItemStack so it travels with the disk/medium. Absent → the volume's
     * default name (e.g. "Local Disk" for a system disk, "Removable Drive" for a medium).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>>
            VOLUME_LABEL = saved("volume_label", Codec.STRING, ByteBufCodecs.STRING_UTF8);

    /* What a Printed Paper carries: the document a printer printed, read page by page as it came out. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PrintedDocument>>
            PRINTED_DOCUMENT = saved("printed_document", PrintedDocuments.CODEC, PrintedDocuments.STREAM_CODEC);

    /*
     * The bench recipes a Crafting Card's own ROM keeps: they travel with the card, so a card moved to another
     * Crafting Computer brings its recipes with it. Its era says how many there may be.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<CraftingPattern>>>
            RECIPE_ROM = saved("recipe_rom", CraftingPattern.CODEC.listOf(),
                    CraftingPattern.STREAM_CODEC.apply(ByteBufCodecs.list(CraftingEras.MAX_ROM_PER_CARD)));

    /*
     * How worn a power supply is, its own life and each unit's wear: it rides on the supply, so the wear goes with it
     * from machine to machine. Absent on a supply never turned on.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PsuCondition>>
            PSU_CONDITION = saved("psu_condition", PsuConditions.CODEC, PsuConditions.STREAM_CODEC);

    /*
     * A portable device's id, which its state on the server is kept by (its apps, its alerts): given the first time
     * it is carried, so every device is its own. Absent on one never carried, as the creative tab shows it.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>>
            PORTABLE_ID = saved("portable_id", UUIDUtil.CODEC, UUIDUtil.STREAM_CODEC);

    /* What a portable's battery holds, in millijoules: its bar under its icon. Absent on one never carried: full. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            PORTABLE_BATTERY = saved("portable_battery", Codec.INT, ByteBufCodecs.VAR_INT);

    /*
     * The game time a portable was last switched on, which its start is played from wherever it shows: in the hand,
     * held up. Absent while it is off, its battery flat or in its dock.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>>
            PORTABLE_ON_SINCE = saved("portable_on_since", Codec.LONG, ByteBufCodecs.VAR_LONG);

    /* The link a portable is on, while an access point reaches it: for its tooltip and its system. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<PortableLink>>
            PORTABLE_LINK = saved("portable_link", PortableLink.CODEC, PortableLink.STREAM_CODEC);

    private ComputingComponents() {
    }

    public static void register(final IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }

    /*
     * Declares the component {@code id}: saved by {@code codec} with the version of its own layout, named for the
     * component, and sent to the players by {@code stream}.
     */
    private static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> saved(
            final String id, final Codec<T> codec, final StreamCodec<? super RegistryFriendlyByteBuf, T> stream) {
        final SaveLayout layout = SaveLayout.of(JsComputers.MODID + ":" + id);
        return COMPONENTS.registerComponentType(id, builder -> builder.persistent(layout.codec(codec))
                .networkSynchronized(stream));
    }
}
