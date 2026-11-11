/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.audio.VinylUploads;
import dev.jstech.computers.operation.payload.VinylActionPayload;
import dev.jstech.computers.operation.payload.VinylStatePayload;
import dev.jstech.core.client.audio.media.MediaUploader;
import dev.jstech.core.text.Text;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Soundfoundry's side of the Vinyl RW Drive on its computer's cable: the drive's state as the server last told it, the
 * actions sent to it, and, when a recording starts, the disc's sound brought from this game's own sounds, which is
 * the recording the drive keeps once the song ends (computers/docs/design/peripherals.md).
 */
public final class SoundfoundryVinyl {

    /* The state of each computer's drive, as the server last told it. */
    private static final Map<BlockPos, VinylStatePayload> STATES = new HashMap<>();
    /* When each computer's window last asked, so a window drawn every frame asks once a second. */
    private static final Map<BlockPos, Long> ASKED = new HashMap<>();
    private static final long LOOK_EVERY = 1000L;
    private static final Logger LOGGER = LogUtils.getLogger();
    /* What the bringing of a disc's sound has got to, for the window to show; nothing while none is under way. */
    @Nullable
    private static Text bringing;

    private SoundfoundryVinyl() {
    }

    /** The server's word on a drive: kept, and when it asks for the disc's sound, the sound is brought. */
    public static void accept(final VinylStatePayload state) {
        STATES.put(state.hostPos(), state);
        if (state.upload() && state.drive() != 0L) {
            bring(state);
        }
    }

    /** The drive on {@code host}'s cable as the server last told it, asking again once a second; or null. */
    @Nullable
    public static VinylStatePayload state(final BlockPos host) {
        final long now = System.currentTimeMillis();
        if (now - ASKED.getOrDefault(host, 0L) >= LOOK_EVERY) {
            ASKED.put(host, now);
            ask(host, VinylActionPayload.LOOK, "");
        }
        return STATES.get(host);
    }

    /** Sends {@code action} to the drive on {@code host}'s cable. */
    public static void ask(final BlockPos host, final int action, final String path) {
        PacketDistributor.sendToServer(new VinylActionPayload(host, action, path));
    }

    /** What bringing the disc's sound has got to, or null while nothing is being brought. */
    @Nullable
    public static Text bringing() {
        return bringing;
    }

    /* Reads the disc's sound from this game's own sounds and sends it, for the drive to keep when the song ends. */
    private static void bring(final VinylStatePayload state) {
        final Minecraft game = Minecraft.getInstance();
        final ResourceLocation id = ResourceLocation.tryParse(state.songId());
        if (game.level == null || id == null) {
            return;
        }
        final JukeboxSong song = game.level.registryAccess().registryOrThrow(Registries.JUKEBOX_SONG).get(id);
        if (song == null) {
            return;
        }
        final WeighedSoundEvents sounds = game.getSoundManager().getSoundEvent(song.soundEvent().value().getLocation());
        if (sounds == null) {
            return;
        }
        final Sound sound = sounds.getSound(RandomSource.create());
        final Optional<Resource> file = game.getResourceManager().getResource(sound.getPath());
        if (file.isEmpty()) {
            return;
        }
        final byte[] bytes;
        try (InputStream in = file.get().open()) {
            bytes = in.readAllBytes();
        } catch (final IOException unreadable) {
            LOGGER.warn("The sound of {} could not be read for a recording: {}", id, unreadable.getMessage());
            return;
        }
        bringing = SoundfoundryAppTexts.VINYL_BRINGING.text();
        MediaUploader.upload(bytes, state.song() + ".ogg", VinylUploads.PURPOSE,
                VinylUploads.context(state.hostPos(), BlockPos.of(state.drive()), state.songId()),
                new MediaUploader.IListener() {
                    @Override
                    public void progress(final long sent, final long total) {
                        bringing = SoundfoundryAppTexts.VINYL_BRINGING_PERCENT.with(total <= 0 ? 0
                                : sent * 100 / total);
                    }

                    @Override
                    public void finished(final boolean ok, final Text message) {
                        bringing = null;
                    }
                });
    }
}
