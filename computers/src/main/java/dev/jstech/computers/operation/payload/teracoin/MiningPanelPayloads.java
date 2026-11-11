/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.teracoin;

import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.client.os.MiningPanelApp;
import dev.jstech.computers.machine.FileService;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.teracoin.FoundBlock;
import dev.jstech.computers.teracoin.MiningPool;
import dev.jstech.computers.teracoin.MiningPools;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.TeracoinState;
import dev.jstech.computers.teracoin.WalletFiles;
import dev.jstech.core.time.GameCalendar;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * The Mining Panel's requests (computers/docs/design/teracoin.md, "Pools, the panel and the wallet"): it connects to
 * the network's Mining Pools as the Messenger connects to its service, and shows any of them. A pool's settings are
 * changed only from the panel on the pool's own server, which is how a pool is owned.
 */
public final class MiningPanelPayloads {

    private MiningPanelPayloads() {
    }

    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, MiningPanelRequestPayload.TYPE, MiningPanelRequestPayload.STREAM_CODEC,
                ComputerAccess.machine(MiningPanelRequestPayload::hostPos), MiningPanelPayloads::handle);
        registrar.playToClient(MiningPanelStatePayload.TYPE, MiningPanelStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> MiningPanelApp.accept(payload)));
    }

    private static void handle(final MiningPanelRequestPayload payload, final ServerPlayer player,
                               final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost host)) {
            return;
        }
        final List<ServerServices.Host> pools = MiningPools.on(level, host.networkUuid());
        final int chosen = pools.isEmpty() ? 0 : Math.max(0, Math.min(payload.pool(), pools.size() - 1));
        if (!pools.isEmpty() && payload.action() != MiningPanelRequestPayload.LOOK
                && yours(pools.get(chosen), payload.hostPos())) {
            final ServerServices.Host pool = pools.get(chosen);
            change(MiningPools.state(pool), payload);
            pool.changed();
        }
        PacketDistributor.sendToPlayer(player, state(level, host, payload.hostPos(), pools, chosen));
    }

    /* A pool's own server is the one place its settings are changed. */
    private static boolean yours(final ServerServices.Host pool, final BlockPos hostPos) {
        return pool.rack().getBlockPos().equals(hostPos) && pool.rack().shownRow() == pool.slot();
    }

    private static void change(final MiningPool pool, final MiningPanelRequestPayload payload) {
        switch (payload.action()) {
            case MiningPanelRequestPayload.SET_FEE -> pool.setFee((int) payload.value());
            case MiningPanelRequestPayload.SET_OWNER -> pool.setOwner(payload.text().trim());
            case MiningPanelRequestPayload.SET_ACCEPTED_ONLY -> pool.setAcceptedOnly(payload.value() == 1L);
            case MiningPanelRequestPayload.ACCEPT -> pool.accept(payload.text());
            case MiningPanelRequestPayload.REFUSE -> pool.refuse(payload.text());
            case MiningPanelRequestPayload.RENAME -> pool.rename(payload.text());
            default -> {
            }
        }
    }

    private static MiningPanelStatePayload state(final ServerLevel level, final IOsHost host, final BlockPos hostPos,
                                                 final List<ServerServices.Host> pools, final int chosen) {
        final TeracoinChain chain = TeracoinState.of(level.getServer()).chain();
        final long dayTime = level.getServer().overworld().getDayTime();
        final long now = level.getServer().overworld().getGameTime();
        final WalletStatePayload.Chain line = new WalletStatePayload.Chain(chain.height(),
                chain.difficulty(dayTime / GameCalendar.DAY_TICKS), chain.nextReward(), chain.blocksToHalving());
        final List<String> names = new ArrayList<>();
        for (final ServerServices.Host pool : pools) {
            names.add(MiningPools.name(pool));
        }
        if (pools.isEmpty()) {
            return new MiningPanelStatePayload(hostPos, line, names, 0, MiningPanelStatePayload.Pool.NONE);
        }
        final ServerServices.Host at = pools.get(chosen);
        final MiningPool pool = MiningPools.state(at);
        final Set<String> mine = walletsOf(host);
        final List<MiningPanelStatePayload.Share> shares = new ArrayList<>();
        for (final MiningPool.RoundShare share : pool.roundShares()) {
            shares.add(new MiningPanelStatePayload.Share(share.address(), share.permille(),
                    mine.contains(share.address())));
        }
        final long hashrate = pool.hashrate(now);
        final List<MiningPanelStatePayload.Miner> miners = new ArrayList<>();
        for (final MiningPool.Member member : pool.members(now)) {
            miners.add(new MiningPanelStatePayload.Miner(member.machine(), member.name(), member.kind(),
                    member.address(), member.hashrate(),
                    hashrate <= 0L ? 0 : (int) (member.hashrate() * 1000L / hashrate)));
        }
        for (final MiningPool.Request request : pool.requests()) {
            miners.add(new MiningPanelStatePayload.Miner(request.machine(), request.name(), request.kind(),
                    request.address(), request.hashrate(), -1));
        }
        final List<MiningPanelStatePayload.Block> blocks = new ArrayList<>();
        for (final FoundBlock block : pool.found()) {
            blocks.add(new MiningPanelStatePayload.Block(block.height(), block.dayTime(), block.finder(),
                    block.reward()));
        }
        return new MiningPanelStatePayload(hostPos, line, names, chosen, new MiningPanelStatePayload.Pool(
                MiningPools.name(at), at.name(), pool.feePermille(), pool.owner(), yours(at, hostPos),
                pool.acceptedOnly(), hashrate, pool.foundCount(), pool.roundStart() < 0L ? -1L
                : now - pool.roundStart(), pool.lastHour(now), shares, miners, blocks));
    }

    /* The addresses of the wallets on the panel's computer, so it can mark them as yours. */
    private static Set<String> walletsOf(final IOsHost host) {
        final Set<String> out = new HashSet<>();
        final FileService files = host.services() == null ? null : host.services().files();
        if (files != null) {
            for (final WalletFiles.Wallet wallet : WalletFiles.all(files, host)) {
                out.add(wallet.address());
            }
        }
        return out;
    }
}
