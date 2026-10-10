/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.music;

import dev.jstech.computers.audio.MusicDownloads;
import dev.jstech.computers.audio.SongFiles;
import dev.jstech.computers.audio.SoundfoundryShare;
import dev.jstech.computers.audio.SoundfoundryTexts;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.client.audio.SoundfoundryShares;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.SoundfoundryShareActionPayload;
import dev.jstech.computers.operation.payload.SoundfoundryShareStatePayload;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SongDownload;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The payloads of Soundfoundry's sharing window: what a player does in it, carried out by the machine it runs on, and
 * how the machine's sharing then stands, sent back.
 *
 * <p>The window asks again every second while it is open, so a song coming in is seen to come in; what a search found
 * travels only in the answer to it, and the songs the machine shares only while the window shows them.
 */
public final class SoundfoundrySharePayloads {

    private SoundfoundrySharePayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, SoundfoundryShareActionPayload.TYPE,
                SoundfoundryShareActionPayload.STREAM_CODEC,
                ComputerAccess.machine(SoundfoundryShareActionPayload::hostPos), SoundfoundrySharePayloads::handle);
        registrar.playToClient(SoundfoundryShareStatePayload.TYPE, SoundfoundryShareStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((state, player) -> SoundfoundryShares.accept(state)));
    }

    /**
     * How sharing stands on that machine.
     *
     * @param found  what a search found, when this answers one
     * @param shared whether to name the songs the machine shares
     */
    public static SoundfoundryShareStatePayload stateOf(final ServerLevel level,
                                                        final AbstractComputerBlockEntity computer,
                                                        final Optional<List<SoundfoundryShare.Found>> found,
                                                        final boolean shared, final Text trouble) {
        final MusicDownloads downloads = computer.musicDownloads();
        final List<SoundfoundryShareStatePayload.Fetch> fetches = new ArrayList<>();
        for (final SongDownload download : computer.console().soundfoundry().downloads()) {
            fetches.add(new SoundfoundryShareStatePayload.Fetch(download.name(), download.bytes(), download.done(),
                    download.from(), SoundfoundryShareStatePayload.linkOf(download.link()),
                    download.status().serializedName(), downloads.millisLeft(download), download.trouble()));
        }
        final List<String> sharedSongs = SoundfoundryShare.sharedSongs(level, computer);
        final Optional<List<String>> names = shared ? Optional.of(sharedSongs.stream()
                .map(SongFiles::nameOf).toList()) : Optional.empty();
        return new SoundfoundryShareStatePayload(computer.getBlockPos(), found.map(SoundfoundrySharePayloads::listed),
                fetches, names, SoundfoundryShare.shownFolderOf(computer), sharedSongs.size(),
                SoundfoundryShare.peerCount(level, computer), SoundfoundryShare.storeOpen(computer),
                SoundfoundryShareStatePayload.linkOf(SoundfoundryShare.ownLink(level, computer)), trouble);
    }

    private static void handle(final SoundfoundryShareActionPayload payload, final ServerPlayer player,
                               final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof AbstractComputerBlockEntity computer)
                || !computer.console().isInstalled(Programs.SOUNDFOUNDRY.toString())) {
            return;
        }
        final SoundfoundryState state = computer.console().soundfoundry();
        Optional<List<SoundfoundryShare.Found>> found = Optional.empty();
        Text note = Text.EMPTY;
        switch (payload.action()) {
            case SoundfoundryShareActionPayload.SEARCH -> {
                found = Optional.of(SoundfoundryShare.search(level, computer, payload.text()));
                if (computer.networkUuid() == null) {
                    note = SoundfoundryTexts.NO_NETWORK.text();
                }
            }
            case SoundfoundryShareActionPayload.DOWNLOAD -> note = computer.musicDownloads().start(level,
                    payload.value(), payload.text(), payload.index() != 0);
            case SoundfoundryShareActionPayload.REMOVE -> {
                state.removeDownloads(new HashSet<>(payload.indexes()));
                computer.setChanged();
            }
            case SoundfoundryShareActionPayload.CLEAR -> {
                state.clearFinishedDownloads();
                computer.setChanged();
            }
            case SoundfoundryShareActionPayload.LOOK -> SoundfoundryShare.layDownSharedFolder(computer);
            default -> { }
        }
        final boolean shared = payload.action() == SoundfoundryShareActionPayload.LOOK && payload.index() != 0;
        PacketDistributor.sendToPlayer(player, stateOf(level, computer, found, shared, note));
    }

    private static List<SoundfoundryShareStatePayload.Found> listed(final List<SoundfoundryShare.Found> found) {
        final List<SoundfoundryShareStatePayload.Found> listed = new ArrayList<>(found.size());
        for (final SoundfoundryShare.Found one : found) {
            listed.add(new SoundfoundryShareStatePayload.Found(one.title(), one.artist(), one.media().bytes(),
                    one.from(), one.source(), one.path(), SoundfoundryShareStatePayload.linkOf(one.link())));
        }
        return listed;
    }
}
