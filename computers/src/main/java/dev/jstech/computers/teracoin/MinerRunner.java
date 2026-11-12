/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.printer.Printers;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * A computer's miner at work (computers/docs/design/teracoin.md, "Mining"): while its program runs, in the background
 * or in front of a terminal, the computer on, it offers the chain its hashrate once a second, for the pool it chose or
 * alone. A pool that takes only the miners its owner accepts leaves it waiting, its request in the pool's settings.
 */
public final class MinerRunner {

    private MinerRunner() {
    }

    /**
     * A machine that mines, as the chain and the pools tell miners apart.
     *
     * @param machine the machine's key: its node, or where it stands
     * @param name    what it is called, as a pool's panel lists it
     * @param kind    what kind of miner it is ({@link MinerKinds})
     * @param hashes  what it hashes a second
     */
    public record Miner(String machine, String name, String kind, long hashes) {
    }

    /**
     * A tick of the computer {@code host}, standing at {@code at}; its cards sit on risers when {@code onRisers}. Its
     * miner offers its second's work when the second is due.
     */
    public static void tick(final IOsHost host, final ServerLevel level, final BlockPos at, final boolean onRisers) {
        final ComputerConsoleState console = host.console();
        if (console == null) {
            return;
        }
        final MinerSettings miner = console.miner();
        final long now = level.getGameTime();
        if (!miner.running(now) || !host.isRunning() || !console.isInstalled(Programs.MINER.toString())) {
            if (miner.status() != MinerSettings.Status.OFF) {
                miner.stalled(MinerSettings.Status.OFF, "");
            }
            return;
        }
        if (!miner.secondDue()) {
            return;
        }
        final ComputerBuild build = host.currentBuild();
        final long hashes = build == null ? 0L : TeracoinHashrate.of(build, onRisers);
        if (hashes <= 0L) {
            miner.stalled(MinerSettings.Status.NOTHING_HASHES, "");
            return;
        }
        offer(level, host.networkUuid(), at, miner, new Miner(machineKey(host, level, at), Printers.machineName(host),
                kindOf(host, build), hashes), found -> JscEvents.awardHost(host, JscEvents.BLOCK_FOUND, ""));
    }

    /**
     * Offers the chain {@code miner}'s second of work from the machine {@code who}, standing at {@code at} on
     * {@code network}: for the pool it chose (the network's first when it named none) or alone, the pool taking it
     * only once its owner accepts it when it takes only the miners it accepts. Its settings say how it stands after:
     * mining, or why not. A block its work finds is told to {@code found}; its rare item goes to its network's storage.
     */
    public static void offer(final ServerLevel level, @Nullable final NetworkUuid network, final BlockPos at,
                             final MinerSettings miner, final Miner who, final Consumer<FoundBlock> found) {
        if (!TeracoinAddress.valid(miner.address())) {
            miner.stalled(MinerSettings.Status.NO_ADDRESS, "");
            return;
        }
        ServerServices.Host pool = null;
        String poolName = "";
        if (!miner.alone()) {
            pool = MiningPools.chosen(level, network, miner.pool());
            if (pool == null && !miner.pool().isEmpty()) {
                miner.stalled(MinerSettings.Status.NO_POOL, miner.pool());
                return;
            }
            poolName = pool == null ? "" : MiningPools.name(pool);
        }
        final MinerOffer offer = new MinerOffer(who.machine(), who.name(), who.kind(), miner.address(), who.hashes(),
                poolName);
        if (pool != null && !MiningPools.state(pool).admits(offer)) {
            miner.stalled(MinerSettings.Status.WAITING_FOR_OWNER, poolName);
            pool.rack().setChanged();
            return;
        }
        if (pool != null && miner.status() != MinerSettings.Status.POOL) {
            JscEvents.awardOperatorAt(level, at, JscEvents.POOL_JOINED);
        }
        miner.mined(who.hashes(), poolName, pool == null ? "" : MiningPools.server(pool),
                pool == null ? 0 : MiningPools.state(pool).feePermille());
        TeracoinMining.offer(new TeracoinMining.Offer(offer, pool,
                item -> RareItems.deposit(level, network, at, item), found));
    }

    /** What kind of miner {@code host} is: a Mining Computer of its era and cards, or a computer of its kind. */
    public static String kindOf(final IOsHost host, final ComputerBuild build) {
        if (host instanceof AbstractComputerBlockEntity computer && computer.cardsOnRisers()) {
            return MinerKinds.rig(host.installedEra() == null ? "" : host.installedEra().path(), build.gpus().size());
        }
        return MinerKinds.machine(host.machine().id());
    }

    /**
     * The machine {@code host} as the miners tell each other apart: its node, or where it stands, in which world, since
     * two machines off any network may stand at the same place in two worlds.
     */
    public static String machineKey(final IOsHost host, @Nullable final ServerLevel level,
                                    @Nullable final BlockPos at) {
        if (host.nodeUuid() != null) {
            return host.nodeUuid().value().toString();
        }
        return level == null || at == null ? "" : level.dimension().location() + " " + at.toShortString();
    }
}
