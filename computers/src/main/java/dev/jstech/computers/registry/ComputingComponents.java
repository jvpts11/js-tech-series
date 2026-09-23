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
import dev.jstech.computers.os.DiskSystems;
import dev.jstech.computers.os.fs.FilesystemContents;
import dev.jstech.computers.os.media.MediaKind;
import dev.jstech.computers.storage.DiskUsage;
import dev.jstech.computers.storage.ServerStorageContents;
import dev.jstech.core.id.StableCodecs;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The data the mod's items carry: what a server stores and is built from, what a disk holds and boots, what a medium
 * carries, and the names a player gives them.
 */
public final class ComputingComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(JsComputers.MODID);

    /*
     * A Server item carries its state in its NBT: the items it stores, the hardware it is built from, and its network
     * node identity.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ServerStorageContents>>
            SERVER_STORAGE = COMPONENTS.registerComponentType("server_storage", b -> b
                    .persistent(ServerStorageContents.CODEC)
                    .networkSynchronized(ServerStorageContents.STREAM_CODEC));

    /*
     * A drive's stored items live in the save-wide volume store, not on the item: the item carries the
     * volume's id and a usage summary, so a drive holding thousands of types stays a tiny item.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>>
            DISK_VOLUME = COMPONENTS.registerComponentType("disk_volume", b -> b
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiskUsage>>
            DISK_USAGE = COMPONENTS.registerComponentType("disk_usage", b -> b
                    .persistent(DiskUsage.CODEC)
                    .networkSynchronized(DiskUsage.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>>
            SERVER_HARDWARE = COMPONENTS.registerComponentType("server_hardware", b -> b
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>>
            SERVER_NODE_UUID = COMPONENTS.registerComponentType("server_node_uuid", b -> b
                    .persistent(UUIDUtil.CODEC)
                    .networkSynchronized(UUIDUtil.STREAM_CODEC));

    /*
     * A RAID Controller carries its array configuration: the mode it runs and how many member
     * drives the array was formed with (so a missing member reads as degraded rather than smaller).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>>
            RAID_MODE = COMPONENTS.registerComponentType("raid_mode", b -> b
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            RAID_MEMBERS = COMPONENTS.registerComponentType("raid_members", b -> b
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /*
     * A rack server's software state (console history, installed programs, settings) persists WITH
     * the item, so it moves between racks with the machine. Server-side only: never network-synced.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>>
            SERVER_CONSOLE = COMPONENTS.registerComponentType("server_console", b -> b
                    .persistent(CompoundTag.CODEC));

    /*
     * The software a disk carries: installed programs and their versions, the desktop preferences, and
     * the shell history. It rides on the DISK, not on the computer, because that is what it is: moving
     * a system disk to another machine takes its programs along, and a fresh disk boots clean. Keeping
     * this on the block entity meant a newly installed system still believed the old one's programs
     * were present. Server-side only: never network-synced.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>>
            DISK_CONSOLE = COMPONENTS.registerComponentType("disk_console", b -> b
                    .persistent(CompoundTag.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>>
            COMPUTER_NAME = COMPONENTS.registerComponentType("computer_name", b -> b
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    /*
     * How much of a (non-Server) computer disk's storage is public, as a per-mille 0..1000. The
     * component rides on the disk ItemStack so the split travels with the disk when it is pulled
     * and reinserted. An absent component reads as fully private (see DiskItem.publicPermille).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            DISK_PUBLIC_PERMILLE = COMPONENTS.registerComponentType("disk_public_permille", b -> b
                    .persistent(Codec.intRange(0, 1000))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /*
     * The media: components that together describe the content of a MediaItem. A medium carries exactly one kind
     * and the matching content component for that kind.
     */

    // Installer payload (OS_INSTALL / PROGRAM_INSTALL): the OS or program id.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>>
            MEDIA_PAYLOAD = COMPONENTS.registerComponentType("media_payload", b -> b
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC));

    /*
     * Which of the three content kinds this medium carries. Absent component → OS_INSTALL (safe
     * default that keeps legacy blank media behaving as installer media).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MediaKind>>
            MEDIA_KIND = COMPONENTS.registerComponentType("media_kind", b -> b
                    .persistent(StableCodecs.byName(MediaKind.class))
                    .networkSynchronized(StableCodecs.byId(MediaKind.class, MediaKind.OS_INSTALL)));

    // Data contents (DATA kind): a portable item/fluid storage snapshot.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ServerStorageContents>>
            MEDIA_DATA = COMPONENTS.registerComponentType("media_data", b -> b
                    .persistent(ServerStorageContents.CODEC)
                    .networkSynchronized(ServerStorageContents.STREAM_CODEC));

    // Capacity of a DATA medium in item-equivalents. Absent → MediaItem.DEFAULT_CAPACITY.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>
            MEDIA_CAPACITY = COMPONENTS.registerComponentType("media_capacity", b -> b
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    /*
     * Disk filesystem components: files and the installed OS live on the DiskItem stack so
     * they travel with the disk when it is inserted or removed.
     */

    // The filesystem contents of a disk volume: path-keyed map of stored files.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FilesystemContents>>
            FILESYSTEM = COMPONENTS.registerComponentType("filesystem", b -> b
                    .persistent(FilesystemContents.CODEC)
                    .networkSynchronized(FilesystemContents.STREAM_CODEC));

    /*
     * The systems a disk carries and which of them it boots, with what each of them remembers about having been
     * met. Present only on bootable disks; absent on plain data disks. It used to be one system written straight
     * onto the disk, so installing a second wrote over the first, and one mark for the disk rather than one per
     * system, which would have had a second system arrive already met.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DiskSystems>>
            DISK_SYSTEMS = COMPONENTS.registerComponentType("disk_systems", b -> b
                    .persistent(DiskSystems.CODEC)
                    .networkSynchronized(DiskSystems.STREAM_CODEC));

    /*
     * A user-chosen label for a disk or media volume, shown in This PC and the explorer drive tree and
     * editable there. Rides on the ItemStack so it travels with the disk/medium. Absent → the volume's
     * default name (e.g. "Local Disk" for a system disk, "Removable Drive" for a medium).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<String>>
            VOLUME_LABEL = COMPONENTS.registerComponentType("volume_label", b -> b
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8));

    private ComputingComponents() {
    }

    public static void register(final IEventBus modEventBus) {
        COMPONENTS.register(modEventBus);
    }
}
