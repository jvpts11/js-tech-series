/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.teracoin;

import dev.jstech.computers.client.os.CryptoWalletApp;
import dev.jstech.computers.machine.FileService;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.teracoin.LedgerEntry;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.WalletFiles;
import dev.jstech.computers.teracoin.WalletPrinting;
import dev.jstech.core.time.GameCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Crypto Wallet's requests, answered by the computer it runs on (computers/docs/design/teracoin.md): the wallets
 * the computer sees, a new wallet written to its disk, a send, and printing paid from a wallet. The computer reads the
 * wallet's key from its file for every request; the key never leaves the server.
 */
public final class WalletPayloads {

    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();
    /* The movements a wallet's history lists at most. */
    private static final int HISTORY = WalletStatePayload.MOST_MOVES;

    private WalletPayloads() {
    }

    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, WalletRequestPayload.TYPE, WalletRequestPayload.STREAM_CODEC,
                ComputerAccess.machine(WalletRequestPayload::hostPos), WalletPayloads::handle);
        registrar.playToClient(WalletStatePayload.TYPE, WalletStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> CryptoWalletApp.accept(payload)));
    }

    /**
     * The wallet at {@code path} on {@code host}, the computer at {@code hostPos}, or its first when the path holds
     * none, as its window shows it.
     */
    public static WalletStatePayload state(final ServerLevel level, final BlockPos hostPos, final IOsHost host,
                                           final FileService files, final String path, final int note) {
        final TeracoinChain chain = TeracoinState.of(level.getServer()).chain();
        final List<WalletFiles.Wallet> found = WalletFiles.all(files, host);
        Optional<WalletFiles.Wallet> shown = path.isEmpty() ? Optional.empty() : WalletFiles.read(files, path);
        if (shown.isEmpty() && !found.isEmpty()) {
            shown = Optional.of(found.get(0));
        }
        final List<WalletStatePayload.Wallet> wallets = new ArrayList<>();
        for (final WalletFiles.Wallet wallet : found) {
            wallets.add(new WalletStatePayload.Wallet(wallet.path(), wallet.file(), wallet.address(),
                    chain.balance(wallet.address())));
        }
        final String newFolder = WalletFiles.shown(host, WalletFiles.folder(host));
        final WalletStatePayload.Current current = shown.map(wallet -> new WalletStatePayload.Current(wallet.path(),
                        wallet.file(), WalletFiles.shown(host, WalletFiles.folderOf(wallet.path())), wallet.address(),
                        chain.balance(wallet.address()), chain.pendingIn(wallet.address()), newFolder,
                        WalletFiles.copyDrive(files), note))
                .orElse(new WalletStatePayload.Current("", "", "", "", 0L, 0L, newFolder, WalletFiles.copyDrive(files),
                        note));
        final List<WalletStatePayload.Move> moves = new ArrayList<>();
        if (current.any()) {
            for (final LedgerEntry entry : chain.history(current.address(), HISTORY)) {
                moves.add(move(chain, entry, current.address()));
            }
        }
        final long worldDays = level.getServer().overworld().getDayTime() / GameCalendar.DAY_TICKS;
        return new WalletStatePayload(hostPos,
                new WalletStatePayload.Chain(chain.height(), chain.difficulty(worldDays), chain.nextReward(),
                        chain.blocksToHalving()), current, wallets, moves,
                shown.map(wallet -> WalletPrinting.state(level, host, wallet)).orElse(WalletStatePayload.Print.NONE));
    }

    private static void handle(final WalletRequestPayload payload, final ServerPlayer player, final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost host) || host.services() == null
                || host.services().files() == null) {
            return;
        }
        final FileService files = host.services().files();
        final TeracoinState state = TeracoinState.of(level.getServer());
        String path = payload.wallet();
        int note = WalletStatePayload.NOTE_NONE;
        switch (payload.action()) {
            case WalletRequestPayload.CREATE -> {
                final String name = WalletFiles.cleanName(payload.text());
                if (name.isEmpty()) {
                    note = WalletStatePayload.NOTE_NO_NAME;
                    break;
                }
                final String folder = payload.other().isBlank() ? WalletFiles.folder(host) : payload.other();
                if (files.exists(folder.replace('\\', '/') + "/" + name + WalletFiles.EXTENSION)) {
                    note = WalletStatePayload.NOTE_NAME_TAKEN;
                    break;
                }
                final TeracoinChain.Keys keys = state.chain().createWallet(RANDOM);
                state.setDirty();
                final String written = WalletFiles.write(files, folder.replace('\\', '/'), name, keys,
                        payload.amount() == 1L ? WalletFiles.copyDrive(files) : 0);
                if (written == null) {
                    note = WalletStatePayload.NOTE_NOT_WRITTEN;
                } else {
                    path = written;
                    note = WalletStatePayload.NOTE_CREATED;
                }
            }
            case WalletRequestPayload.SEND -> note = send(level, files, state, payload);
            case WalletRequestPayload.PRINT, WalletRequestPayload.AUTO_PRINT ->
                    note = WalletPrinting.act(level, host, files, payload);
            default -> {
            }
        }
        PacketDistributor.sendToPlayer(player, state(level, payload.hostPos(), host, files, path, note));
    }

    private static int send(final ServerLevel level, final FileService files, final TeracoinState state,
                            final WalletRequestPayload payload) {
        final Optional<WalletFiles.Wallet> wallet = WalletFiles.read(files, payload.wallet());
        if (wallet.isEmpty()) {
            return WalletStatePayload.NOTE_NOT_YOURS;
        }
        final String to = payload.text().trim();
        if (!TeracoinAddress.valid(to)) {
            return WalletStatePayload.NOTE_BAD_ADDRESS;
        }
        final TeracoinChain.SendResult sent = state.chain().send(wallet.get().address(), wallet.get().key(), to,
                payload.amount(), payload.other().trim(), level.getServer().overworld().getGameTime(),
                level.getServer().overworld().getDayTime());
        state.setDirty();
        return switch (sent) {
            case SENT -> WalletStatePayload.NOTE_SENT;
            case NOT_YOURS -> WalletStatePayload.NOTE_NOT_YOURS;
            case BAD_ADDRESS -> WalletStatePayload.NOTE_BAD_ADDRESS;
            case NOTHING -> WalletStatePayload.NOTE_NOTHING;
            case TOO_MUCH -> WalletStatePayload.NOTE_TOO_MUCH;
        };
    }

    /* One movement as the wallet at {@code address} lists it. */
    private static WalletStatePayload.Move move(final TeracoinChain chain, final LedgerEntry entry,
                                                final String address) {
        final String other = switch (entry.kind()) {
            case SEND -> entry.from().equals(address) ? entry.to() : entry.from();
            case PAYOUT, FEE -> entry.label();
            case REWARD, PRINT -> "";
        };
        final String label = entry.kind() == LedgerEntry.Kind.PAYOUT || entry.kind() == LedgerEntry.Kind.FEE ? ""
                : entry.label();
        final int blocks = entry.pending() ? -1 : Math.max(1, chain.height() - entry.confirmedAt() + 1);
        return new WalletStatePayload.Move(entry.kind().id(), entry.dayTime(), entry.signedFor(address), other,
                label, entry.block(), blocks);
    }
}
