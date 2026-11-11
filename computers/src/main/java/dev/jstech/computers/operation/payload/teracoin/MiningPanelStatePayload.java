/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.teracoin;

import dev.jstech.computers.JsComputers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A Mining Panel's whole state: the chain's line, the network's pools by name, and the pool looked at: its owner and
 * fee, whether the panel runs on its own server (where its settings are), its hashrate over the last hour, the blocks
 * it found, the round's shares, its miners and those asking to join.
 *
 * @param hostPos the computer the panel runs on
 * @param chain   the chain's line
 * @param pools   the network's pools, each as its name and its server's
 * @param chosen  which of them is looked at
 * @param pool    the pool looked at, or {@link Pool#NONE} when the network has none
 */
public record MiningPanelStatePayload(BlockPos hostPos, WalletStatePayload.Chain chain, List<String> pools,
                                      int chosen, Pool pool) implements CustomPacketPayload {

    public static final int MOST_POOLS = 8;
    public static final int MOST_ROWS = 32;
    private static final int MOST_TEXT = 128;

    public static final CustomPacketPayload.Type<MiningPanelStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mining_panel_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MiningPanelStatePayload> STREAM_CODEC =
            StreamCodec.of(MiningPanelStatePayload::write, MiningPanelStatePayload::read);

    public MiningPanelStatePayload {
        pools = List.copyOf(pools.size() > MOST_POOLS ? pools.subList(0, MOST_POOLS) : pools);
    }

    @Override
    public CustomPacketPayload.Type<MiningPanelStatePayload> type() {
        return TYPE;
    }

    private static String clip(final String text) {
        return text.length() > MOST_TEXT ? text.substring(0, MOST_TEXT) : text;
    }

    private static <T> List<T> most(final List<T> rows) {
        return rows.size() > MOST_ROWS ? rows.subList(0, MOST_ROWS) : rows;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final MiningPanelStatePayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos);
        buf.writeVarInt(payload.chain.height());
        buf.writeVarLong(payload.chain.difficulty());
        buf.writeVarLong(payload.chain.reward());
        buf.writeVarInt(payload.chain.toHalving());
        buf.writeVarInt(payload.pools.size());
        for (final String name : payload.pools) {
            buf.writeUtf(clip(name), MOST_TEXT);
        }
        buf.writeVarInt(payload.chosen);
        final Pool pool = payload.pool;
        buf.writeUtf(clip(pool.name()), MOST_TEXT);
        buf.writeUtf(clip(pool.host()), MOST_TEXT);
        buf.writeVarInt(pool.feePermille());
        buf.writeUtf(clip(pool.owner()), MOST_TEXT);
        buf.writeBoolean(pool.yours());
        buf.writeBoolean(pool.acceptedOnly());
        buf.writeVarLong(pool.hashrate());
        buf.writeVarInt(pool.foundCount());
        buf.writeVarLong(pool.roundTicks());
        buf.writeLongArray(pool.lastHour());
        final List<Share> shares = most(pool.shares());
        buf.writeVarInt(shares.size());
        for (final Share share : shares) {
            buf.writeUtf(clip(share.address()), MOST_TEXT);
            buf.writeVarInt(share.permille());
            buf.writeBoolean(share.yours());
        }
        final List<Miner> miners = most(pool.miners());
        buf.writeVarInt(miners.size());
        for (final Miner miner : miners) {
            buf.writeUtf(clip(miner.machine()), MOST_TEXT);
            buf.writeUtf(clip(miner.name()), MOST_TEXT);
            buf.writeUtf(clip(miner.kind()), MOST_TEXT);
            buf.writeUtf(clip(miner.address()), MOST_TEXT);
            buf.writeVarLong(miner.hashrate());
            buf.writeVarInt(miner.permille());
        }
        final List<Block> blocks = most(pool.blocks());
        buf.writeVarInt(blocks.size());
        for (final Block block : blocks) {
            buf.writeVarInt(block.height());
            buf.writeVarLong(block.dayTime());
            buf.writeUtf(clip(block.finder()), MOST_TEXT);
            buf.writeVarLong(block.reward());
        }
    }

    private static MiningPanelStatePayload read(final RegistryFriendlyByteBuf buf) {
        final BlockPos hostPos = BlockPos.STREAM_CODEC.decode(buf);
        final WalletStatePayload.Chain chain = new WalletStatePayload.Chain(buf.readVarInt(), buf.readVarLong(),
                buf.readVarLong(), buf.readVarInt());
        final int poolCount = Math.min(MOST_POOLS, buf.readVarInt());
        final List<String> pools = new ArrayList<>(poolCount);
        for (int i = 0; i < poolCount; i++) {
            pools.add(buf.readUtf(MOST_TEXT));
        }
        final int chosen = buf.readVarInt();
        final String name = buf.readUtf(MOST_TEXT);
        final String host = buf.readUtf(MOST_TEXT);
        final int fee = buf.readVarInt();
        final String owner = buf.readUtf(MOST_TEXT);
        final boolean yours = buf.readBoolean();
        final boolean acceptedOnly = buf.readBoolean();
        final long hashrate = buf.readVarLong();
        final int foundCount = buf.readVarInt();
        final long roundTicks = buf.readVarLong();
        final long[] lastHour = buf.readLongArray();
        final int shareCount = Math.min(MOST_ROWS, buf.readVarInt());
        final List<Share> shares = new ArrayList<>(shareCount);
        for (int i = 0; i < shareCount; i++) {
            shares.add(new Share(buf.readUtf(MOST_TEXT), buf.readVarInt(), buf.readBoolean()));
        }
        final int minerCount = Math.min(MOST_ROWS, buf.readVarInt());
        final List<Miner> miners = new ArrayList<>(minerCount);
        for (int i = 0; i < minerCount; i++) {
            miners.add(new Miner(buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT),
                    buf.readUtf(MOST_TEXT), buf.readVarLong(), buf.readVarInt()));
        }
        final int blockCount = Math.min(MOST_ROWS, buf.readVarInt());
        final List<Block> blocks = new ArrayList<>(blockCount);
        for (int i = 0; i < blockCount; i++) {
            blocks.add(new Block(buf.readVarInt(), buf.readVarLong(), buf.readUtf(MOST_TEXT), buf.readVarLong()));
        }
        return new MiningPanelStatePayload(hostPos, chain, pools, chosen, new Pool(name, host, fee, owner, yours,
                acceptedOnly, hashrate, foundCount, roundTicks, lastHour, shares, miners, blocks));
    }

    /**
     * The pool looked at. Its miners are those mining for it now, the ones asking to join among them when its owner
     * takes only those accepted (their share 0, their machine named so they can be accepted).
     *
     * @param name         its name
     * @param host         its server's name
     * @param feePermille  its fee, in thousandths of a block
     * @param owner        the address its fee is paid to, or empty
     * @param yours        whether the panel runs on its own server, where its settings are changed
     * @param acceptedOnly whether it takes only the miners its owner accepted
     * @param hashrate     its miners' hashrate together
     * @param foundCount   the blocks it has found
     * @param roundTicks   how long its round has run, in ticks, or -1
     * @param lastHour     its hashrate in each five minutes of the last hour, the oldest first
     * @param shares       each address's part of the round's work
     * @param miners       its miners, and those asking to join
     * @param blocks       the blocks it found, the newest first
     */
    public record Pool(String name, String host, int feePermille, String owner, boolean yours, boolean acceptedOnly,
                       long hashrate, int foundCount, long roundTicks, long[] lastHour, List<Share> shares,
                       List<Miner> miners, List<Block> blocks) {

        public static final Pool NONE = new Pool("", "", 0, "", false, false, 0L, 0, -1L, new long[0], List.of(),
                List.of(), List.of());

        public Pool {
            lastHour = lastHour.clone();
            shares = List.copyOf(shares);
            miners = List.copyOf(miners);
            blocks = List.copyOf(blocks);
        }

        /** Whether the network has a pool to look at. */
        public boolean any() {
            return !name.isEmpty() || !host.isEmpty();
        }

        @Override
        public long[] lastHour() {
            return lastHour.clone();
        }
    }

    /** An address's part of the round, in thousandths; {@code yours} when a wallet on the panel's computer has it. */
    public record Share(String address, int permille, boolean yours) {
    }

    /**
     * A miner of the pool: its machine and name, its kind, the address it pays, its hashrate and its part of the
     * pool's hashrate in thousandths; -1 for a miner asking to join.
     */
    public record Miner(String machine, String name, String kind, String address, long hashrate, int permille) {

        /** Whether it asks to join rather than mining. */
        public boolean asking() {
            return permille < 0;
        }
    }

    /** A block the pool found: its height, the world's time of day then, its finder and its reward. */
    public record Block(int height, long dayTime, String finder, long reward) {
    }
}
