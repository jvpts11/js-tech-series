/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.teracoin.MinerConsole;
import dev.jstech.computers.teracoin.MiningPools;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.WalletFiles;
import dev.jstech.core.time.GameCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Teracoin as a machine's programs reach it (computers/docs/design/teracoin.md): the chain every world shares, the
 * wallets in the machine's files, the machine's own miner, and the Mining Pools of its network. A wallet is what its
 * file holds, so a program spends from one only by reading it, as the Wallet program does.
 */
public final class TeracoinService {

    private final IOsHost machine;
    private final ServerLevel level;
    /** Where the machine stands, which tells its miner from another off any network. */
    private final BlockPos at;
    private final FileService files;

    TeracoinService(final IOsHost machine, final ServerLevel level, final BlockPos at, final FileService files) {
        this.machine = machine;
        this.level = level;
        this.at = at;
        this.files = files;
    }

    /** The chain, as the whole server keeps it. */
    public TeracoinChain chain() {
        return this.state().chain();
    }

    /** How many days the world has lived, which the chain's difficulty follows. */
    public long worldDays() {
        return this.level.getServer().overworld().getDayTime() / GameCalendar.DAY_TICKS;
    }

    /** The chain's clock, which is the Overworld's, as the miners and the pools keep time by it. */
    public long now() {
        return this.level.getServer().overworld().getGameTime();
    }

    /** The wallet in the file at {@code path}, or none when the file holds none or cannot be read. */
    public Optional<WalletFiles.Wallet> wallet(final String path) {
        return WalletFiles.read(this.files, path);
    }

    /**
     * Sends {@code amount} from {@code wallet} to {@code to}, labelled {@code label}, as the Wallet program does: the
     * chain is saved with it, and a wallet that sends starts over the time it has held its coins.
     */
    public TeracoinChain.SendResult send(final WalletFiles.Wallet wallet, final String to, final long amount,
                                         final String label) {
        final TeracoinState state = this.state();
        final ServerLevel overworld = this.level.getServer().overworld();
        final TeracoinChain.SendResult sent = state.chain().send(wallet.address(), wallet.key(), to, amount, label,
                overworld.getGameTime(), overworld.getDayTime());
        if (sent == TeracoinChain.SendResult.SENT) {
            state.holdSince(wallet.address(), null);
        }
        state.setDirty();
        return sent;
    }

    /** The machine's miner, or null when it has no console to keep one in. */
    @Nullable
    public MinerConsole miner() {
        return this.machine.console() == null ? null : new MinerConsole(this.machine, this.level, this.at);
    }

    /** The names of the Mining Pools on the machine's network, in the order their servers are found. */
    public List<String> pools() {
        final List<String> names = new ArrayList<>();
        for (final ServerServices.Host host : MiningPools.on(this.level, this.machine.networkUuid())) {
            names.add(MiningPools.name(host));
        }
        return names;
    }

    /** The pool on the machine's network named {@code name}, or its first for an empty name; null when none. */
    @Nullable
    public ServerServices.Host pool(final String name) {
        return MiningPools.chosen(this.level, this.machine.networkUuid(), name);
    }

    private TeracoinState state() {
        return TeracoinState.of(this.level.getServer());
    }
}
