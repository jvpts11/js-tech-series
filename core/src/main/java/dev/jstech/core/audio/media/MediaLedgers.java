/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.persistence.SaveFiles;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.ServerState;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * The ledger of a world's recordings, kept with the world as a state of the whole server: each recording by its name,
 * how many bytes it has, when it was last used and who brought it first.
 */
public final class MediaLedgers {

    private static final Codec<Line> LINE = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("hash").forGetter(Line::hash),
            Codec.STRING.fieldOf("format").forGetter(Line::format),
            Codec.LONG.fieldOf("bytes").forGetter(Line::bytes),
            Codec.LONG.fieldOf("last_used").forGetter(Line::lastUsed),
            UUIDUtil.STRING_CODEC.optionalFieldOf("brought_by").forGetter(Line::broughtBy)
    ).apply(instance, Line::new));
    private static final Codec<MediaLedger.Entry> ENTRY = LINE.flatXmap(MediaLedgers::entryOf,
            entry -> DataResult.success(lineOf(entry)));

    private static final ServerState<List<MediaLedger.Entry>> LEDGER = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "media_ledger"), ENTRY.listOf(), List.of())
            .fileName("jstech_media_ledger")
            .server();

    private MediaLedgers() {
    }

    /** Registers the ledger's state, from the Core's constructor. */
    public static void register() {
        CoreStates.register(LEDGER);
    }

    /** What keeps the ledger of {@code server}'s recordings, with its world. */
    static IMediaLedgerKeeper keeperOf(final MinecraftServer server) {
        return new IMediaLedgerKeeper() {
            @Override
            public @Nullable List<MediaLedger.Entry> load() {
                final List<MediaLedger.Entry> kept = LEDGER.get(server);
                final boolean saved = Files.isRegularFile(SaveFiles.file(server.overworld(), LEDGER.fileName()));
                return saved || !kept.isEmpty() ? kept : null;
            }

            @Override
            public void keep(final List<MediaLedger.Entry> entries) {
                LEDGER.set(server, List.copyOf(entries));
            }
        };
    }

    private static DataResult<MediaLedger.Entry> entryOf(final Line line) {
        try {
            return DataResult.success(new MediaLedger.Entry(new MediaId(line.hash(), line.format(), line.bytes()),
                    line.lastUsed(), line.broughtBy().orElse(null)));
        } catch (final IllegalArgumentException notARecording) {
            return DataResult.error(() -> "not a recording: " + notARecording.getMessage());
        }
    }

    private static Line lineOf(final MediaLedger.Entry entry) {
        return new Line(entry.media().hash(), entry.media().format(), entry.media().bytes(), entry.lastUsed(),
                Optional.ofNullable(entry.broughtBy()));
    }

    /**
     * One recording of the ledger, as the save writes it.
     *
     * @param hash      the recording's SHA-256
     * @param format    its kind of file
     * @param bytes     how many bytes it has
     * @param lastUsed  when anything last made use of it, in milliseconds since the epoch
     * @param broughtBy who brought it first, when anybody did
     */
    private record Line(String hash, String format, long bytes, long lastUsed, Optional<UUID> broughtBy) {
    }
}
