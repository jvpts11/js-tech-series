/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Soundfoundry's state as a machine's save keeps it, under the one compound it is written to. Nothing is written for
 * a machine that never used it.
 */
final class SoundfoundryStorage {

    private static final String KEY = "Soundfoundry";
    private static final String DOWNLOADS = "Downloads";
    private static final StableNames<SongDownload.Status> STATUSES = StableNames.of(SongDownload.Status.class);
    private static final StableNames<DataTier> TIERS = StableNames.of(DataTier.class);

    private SoundfoundryStorage() {
    }

    static void save(final SoundfoundryState state, final CompoundTag tag) {
        if (state.size() == 0 && !state.shuffle() && !state.repeat()
                && state.volume() == SoundfoundryState.DEFAULT_VOLUME && state.balance() == 0
                && state.downloads().isEmpty()) {
            return;
        }
        final CompoundTag s = new CompoundTag();
        final ListTag songs = new ListTag();
        for (final String song : state.songs()) {
            songs.add(StringTag.valueOf(song));
        }
        s.put("Songs", songs);
        s.putInt("Current", state.current());
        s.putBoolean("Shuffle", state.shuffle());
        s.putBoolean("Repeat", state.repeat());
        s.putInt("Volume", state.volume());
        s.putInt("Balance", state.balance());
        if (!state.downloads().isEmpty()) {
            final ListTag downloads = new ListTag();
            for (final SongDownload download : state.downloads()) {
                downloads.add(save(download));
            }
            s.put(DOWNLOADS, downloads);
        }
        tag.put(KEY, s);
    }

    /** Puts the state back as the tag has it; a tag without one leaves it as a machine that never used it. */
    static void load(final SoundfoundryState state, final CompoundTag tag) {
        final CompoundTag s = tag.getCompound(KEY);
        state.clear();
        final List<String> songs = new ArrayList<>();
        for (final Tag song : s.getList("Songs", Tag.TAG_STRING)) {
            songs.add(song.getAsString());
        }
        state.add(songs);
        state.select(s.contains("Current") ? s.getInt("Current") : -1);
        state.setShuffle(s.getBoolean("Shuffle"));
        state.setRepeat(s.getBoolean("Repeat"));
        state.setVolume(s.contains("Volume") ? s.getInt("Volume") : SoundfoundryState.DEFAULT_VOLUME);
        state.setBalance(s.getInt("Balance"));
        state.forgetDownloads();
        for (final Tag download : s.getList(DOWNLOADS, Tag.TAG_COMPOUND)) {
            final SongDownload read = load((CompoundTag) download);
            if (read != null) {
                state.addDownload(read);
            }
        }
    }

    private static CompoundTag save(final SongDownload download) {
        final CompoundTag d = new CompoundTag();
        d.putString("Name", download.name());
        d.putString("Hash", download.media().hash());
        d.putString("Format", download.media().format());
        d.putLong("Bytes", download.media().bytes());
        d.putLong("Source", download.source());
        d.putString("Path", download.path());
        d.putString("From", download.from());
        d.putBoolean("Playlist", download.playlist());
        d.putLong("Done", download.done());
        d.putString("Status", download.status().serializedName());
        if (download.link() != null) {
            d.putString("Link", download.link().serializedName());
        }
        if (!download.trouble().isEmpty()) {
            d.putByteArray("Trouble", bytesOf(download.trouble()));
        }
        return d;
    }

    /* A download as the tag has it, or null for one that names no recording, which is left behind. */
    @Nullable
    private static SongDownload load(final CompoundTag d) {
        final MediaId media;
        try {
            media = new MediaId(d.getString("Hash"), d.getString("Format"), d.getLong("Bytes"));
        } catch (final IllegalArgumentException malformed) {
            return null;
        }
        final SongDownload download = new SongDownload(d.getString("Name"), media, d.getLong("Source"),
                d.getString("Path"), d.getString("From"), d.getBoolean("Playlist"));
        download.restore(d.getLong("Done"), STATUSES.byName(d.getString("Status"), SongDownload.Status.WAITING),
                TIERS.find(d.getString("Link")), d.contains("Trouble") ? textOf(d.getByteArray("Trouble")) : Text.EMPTY);
        return download;
    }

    /* Why a download failed is kept the way it travels to a screen, so it reads in each player's own language. */
    private static byte[] bytesOf(final Text text) {
        final ByteBuf buf = Unpooled.buffer();
        try {
            TextCodecs.STREAM_CODEC.encode(buf, text);
            final byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        } finally {
            buf.release();
        }
    }

    private static Text textOf(final byte[] bytes) {
        final ByteBuf buf = Unpooled.wrappedBuffer(bytes);
        try {
            return TextCodecs.STREAM_CODEC.decode(buf);
        } catch (final RuntimeException malformed) {
            return Text.EMPTY;
        } finally {
            buf.release();
        }
    }
}
