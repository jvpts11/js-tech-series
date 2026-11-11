/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.vinyl;

import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.SongFiles;
import dev.jstech.computers.audio.VinylDiscs;
import dev.jstech.computers.audio.VinylRecordings;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.computers.client.os.SoundfoundryVinyl;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.VinylActionPayload;
import dev.jstech.computers.operation.payload.VinylStatePayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.RecordingFile;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * Soundfoundry and the Vinyl RW Drive on its computer's cable (computers/docs/design/peripherals.md): the drive's state,
 * and Record, Stop, Cut and Eject, each answered with how the drive is afterwards.
 */
public final class VinylPayloads {

    private VinylPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, VinylActionPayload.TYPE, VinylActionPayload.STREAM_CODEC,
                ComputerAccess.machine(VinylActionPayload::hostPos), VinylPayloads::handle);
        registrar.playToClient(VinylStatePayload.TYPE, VinylStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((state, player) -> SoundfoundryVinyl.accept(state)));
    }

    /** The first Vinyl RW Drive on {@code host}'s cable, or null. */
    @Nullable
    public static VinylDriveBlockEntity driveOf(final ServerLevel level, final IOsHost host) {
        final List<Long> endpoints = new ArrayList<>(host.enabledEndpoints());
        Collections.sort(endpoints);
        for (final long at : endpoints) {
            if (Loaded.blockEntity(level, BlockPos.of(at)) instanceof VinylDriveBlockEntity drive) {
                return drive;
            }
        }
        return null;
    }

    private static void handle(final VinylActionPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost host)) {
            return;
        }
        final VinylDriveBlockEntity drive = driveOf(level, host);
        boolean upload = false;
        if (drive != null) {
            switch (payload.action()) {
                case VinylActionPayload.RECORD -> upload = drive.record(level).isPresent();
                case VinylActionPayload.STOP -> drive.stop(level);
                case VinylActionPayload.CUT -> cut(level, host, drive, payload.path());
                case VinylActionPayload.EJECT -> drive.eject(level);
                default -> {
                }
            }
        }
        PacketDistributor.sendToPlayer(player, state(level, host, payload.hostPos(), drive, upload));
    }

    /* Cuts the recording at {@code path} onto the blank, when it came from a disc. */
    private static void cut(final ServerLevel level, final IOsHost host, final VinylDriveBlockEntity drive,
                            final String path) {
        final RecordingFile file = SongFiles.read(level, host, path);
        final Optional<VinylRecordings.Entry> from = file == null ? Optional.empty()
                : VinylRecordings.of(level).of(file.media());
        drive.cut(level, from.map(VinylRecordings.Entry::song).orElse(null));
    }

    /** How the drive on {@code host}'s cable is, with the recordings in the system's Music/Vinyl folder. */
    public static VinylStatePayload state(final ServerLevel level, final IOsHost host, final BlockPos hostPos,
                                          @Nullable final VinylDriveBlockEntity drive, final boolean upload) {
        final List<VinylStatePayload.Recording> recordings = recordings(level, host);
        if (drive == null) {
            return new VinylStatePayload(hostPos, 0L, VinylStatePayload.EMPTY, "", 0, 0, 0, "", "", false, "",
                    Text.EMPTY, recordings);
        }
        final ItemStack record = drive.onPlatter();
        final int platter = record.isEmpty() ? VinylStatePayload.EMPTY
                : VinylDiscs.blank(record) ? VinylStatePayload.BLANK : VinylStatePayload.DISC;
        final String disc = VinylDiscs.songOf(record, level.registryAccess())
                .map(song -> song.value().description().getString()).orElse("");
        final int elapsed = drive.work() == VinylDriveBlockEntity.Work.NONE ? 0
                : (int) Math.min(drive.length(), level.getGameTime() - drive.started());
        final String folder = MusicImports.musicFolderOf(host) + "/" + VinylDriveBlockEntity.FOLDER;
        final String song = songName(level, drive.song());
        final String saving = drive.work() == VinylDriveBlockEntity.Work.RECORDING ? folder + "/" + song + ".ogg"
                : drive.lastSaved();
        final Text problem = drive.problem().words();
        return new VinylStatePayload(hostPos, drive.getBlockPos().asLong(), platter, clip(disc),
                drive.work().id(), elapsed, drive.length(), clip(song), clip(drive.song()), upload, clip(saving),
                problem, recordings);
    }

    /* The recordings in the system's Music/Vinyl folder, each with whether it came from a disc and when. */
    private static List<VinylStatePayload.Recording> recordings(final ServerLevel level, final IOsHost host) {
        final List<VinylStatePayload.Recording> out = new ArrayList<>();
        final String folder = MusicImports.musicFolderOf(host) + "/" + VinylDriveBlockEntity.FOLDER;
        final VinylRecordings book = VinylRecordings.of(level);
        for (final String path : SongFiles.under(level, host, folder)) {
            if (out.size() >= VinylStatePayload.MOST_RECORDINGS) {
                break;
            }
            final RecordingFile file = SongFiles.read(level, host, path);
            if (file == null || path.length() > VinylStatePayload.MOST_TEXT) {
                continue;
            }
            final Optional<VinylRecordings.Entry> from = book.of(file.media());
            final String name = FsPaths.fileName(path);
            final int dot = name.lastIndexOf('.');
            out.add(new VinylStatePayload.Recording(path, clip(dot > 0 ? name.substring(0, dot) : name),
                    from.isPresent(), from.map(VinylRecordings.Entry::day).orElse(0L), file.info().millis()));
        }
        return out;
    }

    /* A song's label, by its id; the id itself when the game does not know it. */
    private static String songName(final ServerLevel level, final String id) {
        final ResourceLocation key = id.isEmpty() ? null : ResourceLocation.tryParse(id);
        if (key == null) {
            return "";
        }
        final Optional<Holder.Reference<JukeboxSong>> song = level.registryAccess()
                .registryOrThrow(Registries.JUKEBOX_SONG).getHolder(ResourceKey.create(Registries.JUKEBOX_SONG, key));
        return song.map(found -> found.value().description().getString()).orElse(id);
    }

    private static String clip(final String text) {
        return text.length() > VinylStatePayload.MOST_TEXT ? text.substring(0, VinylStatePayload.MOST_TEXT) : text;
    }
}
