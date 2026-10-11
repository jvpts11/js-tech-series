/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.storage.ServerStorageContents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One drive a machine reads removable media in, as {@link MachineMedia} finds them: a reader on a cable, or a disk in
 * a Dock Station's tray. Whatever asks for a machine's media sees every kind of drive the same way: where it is, what
 * it holds, how a change to that is kept and how it gives its medium back.
 */
public sealed interface IMediaDrive {

    /** Where the drive is, as the explorers and the screens address it. */
    MediaVolume volume();

    /** What the machine reads in it: the medium, or nothing when it holds none or holds it where it cannot be read. */
    ItemStack medium();

    /** The kind of drive. */
    MediaDriveType type();

    /** Keeps a change made in place to what the drive holds, and shows it to the players near it. */
    void commit();

    /**
     * What the eject of a screen does: an optical drive opens its tray, the disc waiting on it for a hand; any other
     * hands its medium to the player, or drops it by the drive when the player has no room.
     */
    void eject(ServerLevel level, ServerPlayer player);

    /** The block the drive is in. */
    default BlockPos pos() {
        return BlockPos.of(volume().readerPos());
    }

    /** The kind of medium it holds, or null when it holds none or what it holds is not a medium. */
    @Nullable
    default MediaKind kind() {
        final ItemStack medium = medium();
        return medium.getItem() instanceof MediaItem ? MediaItem.kind(medium) : null;
    }

    /** The system or program the medium installs, or null when it installs none. */
    @Nullable
    default ResourceLocation payload() {
        final ItemStack medium = medium();
        return medium.getItem() instanceof MediaItem ? MediaItem.payload(medium) : null;
    }

    /** The format of the medium it holds, or null when it holds none or one with no format. */
    @Nullable
    default MediaFormat format() {
        return medium().getItem() instanceof FormattedMediaItem media ? media.format() : null;
    }

    /** What a data medium holds, empty for any other. */
    default ServerStorageContents data() {
        final ItemStack medium = medium();
        return medium.getItem() instanceof MediaItem ? MediaItem.data(medium) : ServerStorageContents.EMPTY;
    }

    /** Whether it holds a medium of that kind that names what it installs. */
    default boolean installs(final MediaKind kind) {
        return kind() == kind && payload() != null;
    }

    /**
     * A reader on a cable: a floppy drive, an optical drive, or a Dock Station's own port.
     *
     * @param reader the reader's block entity
     */
    record Reader(MediaReaderBlockEntity reader) implements IMediaDrive {

        @Override
        public MediaVolume volume() {
            return new MediaVolume(reader.getBlockPos().asLong(), MediaVolume.MEDIUM);
        }

        @Override
        public ItemStack medium() {
            return reader.readableMedium();
        }

        @Override
        public MediaDriveType type() {
            return reader.driveType();
        }

        @Override
        public void commit() {
            reader.setChanged();
            reader.fields().syncToClients();
        }

        @Override
        public void eject(final ServerLevel level, final ServerPlayer player) {
            if (reader.driveType().ejectButton() != null) {
                if (!reader.reading() && !reader.tray().isOpen()) {
                    reader.tray().press(level, reader.getBlockPos());
                }
                return;
            }
            give(level, player, pos(), reader.ejectMedia());
        }
    }

    /**
     * A disk in one of a Dock Station's trays, an external drive of its own.
     *
     * @param dock the dock
     * @param bay  its tray
     */
    record Docked(DockStationBlockEntity dock, int bay) implements IMediaDrive {

        @Override
        public MediaVolume volume() {
            return new MediaVolume(dock.getBlockPos().asLong(), bay);
        }

        @Override
        public ItemStack medium() {
            return dock.disk(bay);
        }

        @Override
        public MediaDriveType type() {
            return MediaDriveType.DOCK_STATION;
        }

        @Override
        public void commit() {
            dock.diskChanged();
        }

        @Override
        public void eject(final ServerLevel level, final ServerPlayer player) {
            give(level, player, pos(), dock.ejectDisk(bay));
        }
    }

    /** Puts what came out of a drive in the player's inventory, or by the drive when it has no room. */
    private static void give(final ServerLevel level, final ServerPlayer player, final BlockPos at,
                             final ItemStack ejected) {
        if (!ejected.isEmpty() && !player.addItem(ejected)) {
            Containers.dropItemStack(level, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, ejected);
        }
    }
}
