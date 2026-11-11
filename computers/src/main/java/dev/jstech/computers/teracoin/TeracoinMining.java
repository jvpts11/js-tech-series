/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.blockentity.ServerServices;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Teracoin's second (computers/docs/design/teracoin.md, "Blocks"): every miner that is on offers its hashrate during
 * the second, and once a second the chain weighs them together. A pool's miners add their second of work to their
 * pool's round; a block found pays a miner alone at once, or its pool's shares with the next block, and one in twenty
 * brings a rare item into the network storage of the machine whose work found it. A miner that is off offers nothing
 * and costs nothing.
 */
public final class TeracoinMining {

    /** The ticks between two weighings: a second. */
    public static final int SECOND = 20;
    /* The ticks in a world day. */
    private static final long DAY_TICKS = 24_000L;
    /* This second's miners, by the machine each runs on: a miner offering twice in a second counts once. */
    private static final Map<String, Offer> OFFERS = new LinkedHashMap<>();
    private static final RandomGenerator RANDOM = RandomGenerator.getDefault();

    private TeracoinMining() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, TeracoinMining::onServerTick);
        NeoForge.EVENT_BUS.addListener(ServerStoppingEvent.class, event -> OFFERS.clear());
    }

    /** A miner hashed this second: it is weighed at the second's end. */
    public static void offer(final Offer offer) {
        OFFERS.put(offer.data().machine(), offer);
    }

    /** Weighs the miners that offered since the last second, now: what the server's tick does once a second. */
    @Nullable
    public static FoundBlock weigh(final MinecraftServer server) {
        final TeracoinState state = TeracoinState.of(server);
        final TeracoinChain chain = state.chain();
        final long before = chain.version();
        final ServerLevel overworld = server.overworld();
        final long now = overworld.getGameTime();
        final long dayTime = overworld.getDayTime();
        final List<Offer> offers = new ArrayList<>(OFFERS.values());
        OFFERS.clear();
        final List<MinerOffer> miners = new ArrayList<>(offers.size());
        for (final Offer offer : offers) {
            miners.add(offer.data());
            if (offer.pool() != null) {
                MiningPools.state(offer.pool()).work(offer.data(), now);
                offer.pool().rack().setChanged();
            }
        }
        final FoundBlock found = chain.resolve(now, dayTime, dayTime / DAY_TICKS, miners, RANDOM);
        if (found != null) {
            pay(chain, found, offers);
        }
        if (chain.version() != before) {
            state.setDirty();
        }
        return found;
    }

    private static void onServerTick(final ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % SECOND == 0) {
            weigh(event.getServer());
        }
    }

    private static void pay(final TeracoinChain chain, final FoundBlock found, final List<Offer> offers) {
        Offer finder = null;
        for (final Offer offer : offers) {
            if (offer.data().machine().equals(found.machine())) {
                finder = offer;
                break;
            }
        }
        if (finder != null && finder.pool() != null) {
            final List<TeracoinChain.PoolShare> shares = MiningPools.state(finder.pool()).split(found);
            chain.creditPool(found, MiningPools.name(finder.pool()), shares);
            finder.pool().changed();
        } else {
            chain.creditAlone(found);
        }
        if (finder != null) {
            finder.found().accept(found);
            if (found.rare()) {
                final List<Item> rare = TeracoinSettings.rareItems();
                if (!rare.isEmpty()) {
                    finder.rare().accept(new ItemStack(rare.get(RANDOM.nextInt(rare.size()))));
                }
            }
        }
    }

    /**
     * A miner's second: what it offers the chain, the pool it mines for, where a rare item it finds goes, and what it
     * does when its work finds a block.
     *
     * @param data  its hashrate and the address it pays
     * @param pool  the pool it mines for, or null for a miner alone
     * @param rare  where a rare item its block brings goes
     * @param found what it does when its work finds a block
     */
    public record Offer(MinerOffer data, @Nullable ServerServices.Host pool, Consumer<ItemStack> rare,
                        Consumer<FoundBlock> found) {
    }
}
