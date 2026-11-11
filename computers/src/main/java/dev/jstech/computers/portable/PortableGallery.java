/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.operation.payload.NetworkNodeInfo;
import dev.jstech.computers.operation.payload.files.FileAccess;
import dev.jstech.computers.operation.payload.network.NetworkPayloads;
import dev.jstech.computers.operation.payload.portable.PortableGalleryPayload;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.PictureScaling;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.core.audio.media.IMediaUploadHandler;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaProbe;
import dev.jstech.core.audio.media.MediaReceipt;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.uuid.NodeUuid;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A portable's camera and gallery on the server (computers/docs/design/wireless.md). The camera's picture comes as a
 * PNG at the resolution of the device's era, brought the way a song is and kept in the server's store; the gallery
 * keeps it, with a small copy to show, in the device's own storage, which it fills by its bytes. From the gallery a
 * picture goes to a personal computer of the network as a PNG file in its pictures folder, naming the picture the
 * store keeps, as a song's file does.
 */
@TextHolder
public final class PortableGallery implements IMediaUploadHandler {

    /** What a camera brings its pictures for. */
    public static final String PURPOSE = JsComputers.MODID + ":camera";

    static final TextKey NOT_IN_HAND = TextKey.of("jsc.portable.gallery.not_in_hand",
            "The picture is for a device in your hand");
    static final TextKey NO_CAMERA = TextKey.of("jsc.portable.gallery.no_camera", "This device has no camera");
    static final TextKey NOT_A_PICTURE = TextKey.of("jsc.portable.gallery.not_a_picture",
            "Not a picture this camera takes");
    static final TextKey FULL = TextKey.of("jsc.portable.gallery.full",
            "The gallery is full: delete some pictures first");
    static final TextKey NO_ROOM = TextKey.of("jsc.portable.gallery.no_room",
            "The device has no room for another picture");
    static final TextKey TAKEN = TextKey.of("jsc.portable.gallery.taken", "Picture taken");
    /* The picture's file, the computer. */
    static final TextKey SENT = TextKey.of("jsc.portable.gallery.sent", "%s sent to %s");
    static final TextKey NOT_SENT = TextKey.of("jsc.portable.gallery.not_sent", "%s could not be written on %s");
    static final TextKey NO_DISK = TextKey.of("jsc.portable.gallery.no_disk",
            "%s has no system disk with folders to keep it on");
    static final TextKey DISK_FULL = TextKey.of("jsc.portable.gallery.disk_full", "%s has no room for it");
    static final TextKey GONE = TextKey.of("jsc.portable.gallery.gone",
            "That computer is not on the network any more");

    /* The largest picture read to make a small copy of: past the Advanced camera's. */
    private static final long MOST_PIXELS = 4_096L * 4_096L;

    /** Sends {@code player} what the gallery of the device {@code device} shows, with {@code note} under it. */
    public static void view(final ServerPlayer player, final ServerLevel level, final UUID device,
                            final PortableState state, final Text note) {
        final List<PortableGalleryPayload.Picture> pictures = new ArrayList<>();
        for (final PortablePicture picture : state.pictures()) {
            pictures.add(PortableGalleryPayload.Picture.of(picture));
        }
        final List<PortableGalleryPayload.Target> targets = new ArrayList<>();
        final NetworkUuid network = networkOf(level, device);
        if (network != null) {
            for (final Map.Entry<NodeUuid, NetworkNodeInfo> pc
                    : NetworkPayloads.personalComputers(level, network).entrySet()) {
                targets.add(new PortableGalleryPayload.Target(pc.getKey().asString(), nameOf(pc.getKey(),
                        pc.getValue()), pc.getValue().link().wifi() != null ? PortableGalleryPayload.Target.WIFI
                        : pc.getValue().link().link()));
            }
        }
        PacketDistributor.sendToPlayer(player, new PortableGalleryPayload(device, pictures, targets, note));
    }

    /** Sends the picture numbered {@code number} to the personal computer of node {@code node}, as a PNG file. */
    public static void send(final ServerPlayer player, final ServerLevel level, final UUID device,
                            final PortableState state, final int number, final String node) {
        final PortablePicture picture = state.picture(number);
        final NetworkUuid network = networkOf(level, device);
        if (picture == null || network == null) {
            return;
        }
        final IOsHost computer = computerOf(level, network, node);
        final NetworkNodeInfo info = computer == null ? null
                : NetworkPayloads.personalComputers(level, network).get(new NodeUuid(UUID.fromString(node)));
        final Text note = computer == null || info == null ? GONE.text()
                : write(level, player, computer, nameOf(new NodeUuid(UUID.fromString(node)), info), picture);
        view(player, level, device, state, note);
    }

    /** Deletes the picture numbered {@code number}, which gives its room back. */
    public static void delete(final ServerPlayer player, final ServerLevel level, final UUID device,
                              final int number) {
        final PortableDevices devices = PortableDevices.get(level.getServer());
        devices.deletePicture(device, number);
        final PortableState state = devices.find(device);
        if (state != null) {
            view(player, level, device, state, Text.EMPTY);
        }
    }

    @Override
    @Nullable
    public Text refuse(final ServerPlayer player, final String context, final String name, final MediaId media) {
        final UUID device = deviceOf(context);
        final ItemStack held = device == null ? ItemStack.EMPTY : Portables.held(player, device);
        if (!(held.getItem() instanceof PortableItem item)) {
            return NOT_IN_HAND.text();
        }
        if (item.model().cameraWidth() <= 0) {
            return NO_CAMERA.text();
        }
        if (!"png".equals(media.format())) {
            return NOT_A_PICTURE.text();
        }
        final PortableState state = PortableDevices.get(player.getServer()).find(device);
        if (state == null) {
            return NOT_IN_HAND.text();
        }
        if (state.pictures().size() >= PortableState.MOST_PICTURES) {
            return FULL.text();
        }
        return PortableStorage.pictureWeight(item.model(), media.bytes()) > state.free() ? NO_ROOM.text() : null;
    }

    /*
     * Not asked again whether the device is still in the hand: the picture was taken with it there, and a large one
     * takes a while to come. Whether it is of the camera's own size is asked here, where the file is whole; the small
     * copy is made off the game's thread, and the picture joins the gallery once it is.
     */
    @Override
    public MediaReceipt received(final ServerPlayer player, final String context, final String name,
                                 final MediaId media, final MediaInfo info) {
        final UUID device = deviceOf(context);
        final MediaStore store = MediaStore.current().orElse(null);
        final PortableState state = device == null ? null : PortableDevices.get(player.getServer()).find(device);
        if (state == null || store == null) {
            return MediaReceipt.refused(NOT_IN_HAND.text());
        }
        final byte[] head;
        try (InputStream in = store.open(media)) {
            head = in.readNBytes(MediaProbe.PNG_HEAD);
        } catch (final IOException unreadable) {
            return MediaReceipt.refused(NOT_A_PICTURE.text());
        }
        final Optional<MediaProbe.PictureSize> size = MediaProbe.pngSize(head);
        final PortableModel model = state.model();
        if (size.isEmpty() || size.get().width() != model.cameraWidth()
                || size.get().height() != model.cameraHeight()) {
            return MediaReceipt.refused(NOT_A_PICTURE.text());
        }
        final ServerLevel level = player.serverLevel();
        final long day = PortableClock.day(level.getDayTime());
        final Path file = store.path(media);
        CompletableFuture.supplyAsync(() -> thumbnail(file), Util.backgroundExecutor()).thenAcceptAsync(small -> {
            final PortableDevices devices = PortableDevices.get(level.getServer());
            final PortablePicture kept = devices.addPicture(device, media, model.cameraWidth(), model.cameraHeight(),
                    day, small);
            store.used(media);
            final PortableState now = devices.find(device);
            if (now != null && !player.hasDisconnected()) {
                view(player, level, device, now, kept == null ? NO_ROOM.text() : TAKEN.text());
            }
        }, level.getServer());
        return MediaReceipt.accepted(TAKEN.text());
    }

    /*
     * Writes the picture {@code sender} sent as a file on the computer called {@code computerName}, in its pictures
     * folder; says how.
     */
    private static Text write(final ServerLevel level, final ServerPlayer sender, final IOsHost computer,
                              final String computerName, final PortablePicture picture) {
        final ItemStack disk = computer.systemDisk();
        final FilesystemKind kind = FileAccess.filesystemKindOf(computer);
        if (disk.isEmpty() || kind != FilesystemKind.HIERARCHICAL) {
            return NO_DISK.with(computerName);
        }
        final OsDef os = computer.installedOs();
        final String dir = SystemLayout.picturesDirFor(os, os == null ? null : OsRegistry.getKernel(os.kernelId()));
        if (!DiskFilesystem.mkdirs(disk, dir, kind)) {
            return NOT_SENT.with(picture.fileName(), computerName);
        }
        final String content = new RecordingFile(picture.media(), MediaInfo.STILL).write();
        final String path = DiskFilesystem.uniquePath(disk, FsPaths.join(dir,
                RecordingFile.stemOf(picture.fileName(), "picture")), "." + FileType.PNG.extension(), content);
        final DiskFilesystem.WriteResult result = DiskFilesystem.write(disk, path, FileType.PNG, content,
                computer.systemDiskFreeWeight() + DiskFilesystem.weightOf(disk, path), kind, level.getGameTime());
        computer.setChanged();
        if (result == DiskFilesystem.WriteResult.OK) {
            MediaStore.current().ifPresent(store -> store.used(picture.media()));
            JscEvents.award(sender, JscEvents.PHOTO_SENT);
            return SENT.with(FsPaths.fileName(path), computerName);
        }
        return result == DiskFilesystem.WriteResult.DISK_FULL ? DISK_FULL.with(computerName)
                : NOT_SENT.with(picture.fileName(), computerName);
    }

    /* The small copy of a picture, a PNG, cut to four by three about its middle; none for one that does not read. */
    private static byte[] thumbnail(final Path picture) {
        try {
            final BufferedImage read = PictureScaling.decode(Files.readAllBytes(picture), MOST_PIXELS);
            if (read == null) {
                return new byte[0];
            }
            final int w = Math.min(read.getWidth(), read.getHeight() * 4 / 3);
            final int h = w * 3 / 4;
            final byte[] png = PictureScaling.png(PictureScaling.scale(read, (read.getWidth() - w) / 2,
                    (read.getHeight() - h) / 2, w, h, PortablePicture.THUMBNAIL_WIDTH,
                    PortablePicture.THUMBNAIL_HEIGHT));
            return png.length <= PortablePicture.MOST_THUMBNAIL ? png : new byte[0];
        } catch (final IOException | RuntimeException unreadable) {
            return new byte[0];
        }
    }

    /* The personal computer of node {@code node} on {@code network}, loaded, or null. */
    @Nullable
    private static IOsHost computerOf(final ServerLevel level, final NetworkUuid network, final String node) {
        for (final NetworkSystem.PersonalComputerNode pc : NetworkSystem.get(level).personalComputersOf(network)) {
            if (pc.nodeUuid().asString().equals(node)
                    && level.getBlockEntity(BlockPos.of(pc.pos())) instanceof IOsHost computer) {
                return computer;
            }
        }
        return null;
    }

    /* What a computer is called in the list: the name it was given, or PC and its short id. */
    private static String nameOf(final NodeUuid node, final NetworkNodeInfo info) {
        return info.name().isEmpty() ? "PC-" + ShortId.of(node.asString()) : info.name();
    }

    @Nullable
    private static NetworkUuid networkOf(final ServerLevel level, final UUID device) {
        final PortableLinks.Joined joined = PortableLinks.of(level, device);
        return joined == null ? null : joined.network();
    }

    /* The device a camera's context names: its id, as the camera wrote it. */
    @Nullable
    private static UUID deviceOf(final String context) {
        try {
            return UUID.fromString(context);
        } catch (final IllegalArgumentException malformed) {
            return null;
        }
    }
}
