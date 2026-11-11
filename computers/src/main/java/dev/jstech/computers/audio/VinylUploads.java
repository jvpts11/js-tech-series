/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.VinylDriveBlockEntity;
import dev.jstech.computers.blockentity.VinylTexts;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.core.audio.media.IMediaUploadHandler;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaReceipt;
import dev.jstech.core.audio.media.MediaUploads;
import dev.jstech.core.text.Text;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The sound of a disc a Vinyl RW Drive records, brought from the recording player's game, which holds the disc's sound
 * among its own (computers/docs/design/peripherals.md). Only a player at the computer brings it, only while that drive
 * records that song, and the drive keeps it until the song ends.
 */
public final class VinylUploads implements IMediaUploadHandler {

    /** What Soundfoundry brings a disc's sound for. */
    public static final String PURPOSE = JsComputers.MODID + ":vinyl";
    private static final char SEPARATOR = '\n';

    private VinylUploads() {
    }

    /** Takes the sounds brought for {@link #PURPOSE}; called once as the mod loads. */
    public static void register() {
        MediaUploads.handle(PURPOSE, new VinylUploads());
    }

    /** What the recording player's game sends with the disc's sound: the computer, the drive and the song. */
    public static String context(final BlockPos host, final BlockPos drive, final String song) {
        return Long.toString(host.asLong()) + SEPARATOR + drive.asLong() + SEPARATOR + song;
    }

    @Override
    @Nullable
    public Text refuse(final ServerPlayer player, final String context, final String name, final MediaId media) {
        final Target target = Target.parse(context).orElse(null);
        if (target == null || !ComputerAccess.machine(Target::host).admits(player, target)) {
            return MusicImports.NOT_AT_IT.text();
        }
        final VinylDriveBlockEntity drive = drive(player.serverLevel(), target);
        return drive != null && drive.work() == VinylDriveBlockEntity.Work.RECORDING
                && drive.song().equals(target.song()) ? null : VinylTexts.NOT_RECORDING.text();
    }

    @Override
    public MediaReceipt received(final ServerPlayer player, final String context, final String name,
                                 final MediaId media, final MediaInfo info) {
        final Target target = Target.parse(context).orElse(null);
        final VinylDriveBlockEntity drive = target == null ? null : drive(player.serverLevel(), target);
        return drive == null ? MediaReceipt.refused(VinylTexts.NOT_RECORDING.text())
                : drive.brought(player.serverLevel(), target.song(), name, media, info);
    }

    @Nullable
    private static VinylDriveBlockEntity drive(final ServerLevel level, final Target target) {
        return level.getBlockEntity(target.drive()) instanceof VinylDriveBlockEntity drive
                && target.host().equals(drive.ownerPos()) ? drive : null;
    }

    /** The computer, the drive and the song a sound was brought for, as Soundfoundry wrote them. */
    private record Target(BlockPos host, BlockPos drive, String song) {

        static Optional<Target> parse(final String context) {
            final String[] parts = context.split(String.valueOf(SEPARATOR), -1);
            if (parts.length != 3) {
                return Optional.empty();
            }
            try {
                return Optional.of(new Target(BlockPos.of(Long.parseLong(parts[0])),
                        BlockPos.of(Long.parseLong(parts[1])), parts[2]));
            } catch (final NumberFormatException malformed) {
                return Optional.empty();
            }
        }
    }
}
