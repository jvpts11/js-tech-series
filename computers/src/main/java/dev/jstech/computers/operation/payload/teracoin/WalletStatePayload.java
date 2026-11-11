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
 * A Crypto Wallet's whole state, as its computer answers every request: the chain's line, the wallet shown, the
 * computer's wallets, the wallet's movements, and its printing (the printers linked to the computer, the server's
 * prices and the wallet's Auto print). The key never travels: only the computer reads it from the file.
 *
 * @param hostPos the computer the wallet runs on
 * @param chain   the chain's line: its height, difficulty, reward and next halving
 * @param current the wallet shown and what the last request did
 * @param wallets the computer's wallets
 * @param moves   the wallet's movements, the newest first
 * @param print   what the wallet's Print tab shows
 */
public record WalletStatePayload(BlockPos hostPos, Chain chain, Current current, List<Wallet> wallets,
                                 List<Move> moves, Print print) implements CustomPacketPayload {

    /** Nothing to say. */
    public static final int NOTE_NONE = 0;
    public static final int NOTE_SENT = 1;
    public static final int NOTE_NOT_YOURS = 2;
    public static final int NOTE_BAD_ADDRESS = 3;
    public static final int NOTE_NOTHING = 4;
    public static final int NOTE_TOO_MUCH = 5;
    public static final int NOTE_CREATED = 6;
    public static final int NOTE_NAME_TAKEN = 7;
    public static final int NOTE_NOT_WRITTEN = 8;
    public static final int NOTE_NO_NAME = 9;
    public static final int NOTE_PRINTING = 10;
    public static final int NOTE_NOT_PRINTABLE = 11;
    public static final int NOTE_NO_PRINTER = 12;
    public static final int NOTE_AUTO_SET = 13;
    public static final int NOTE_AUTO_OFF = 14;
    public static final int NOTE_QUEUE_FULL = 15;
    /** The most of each list sent. */
    public static final int MOST_WALLETS = 16;
    public static final int MOST_MOVES = 64;
    public static final int MOST_PRINTERS = 16;
    public static final int MOST_PRICES = 64;
    /* The letters of a path, a name and an address at most. */
    private static final int MOST_PATH = WalletRequestPayload.MOST_PATH;
    private static final int MOST_TEXT = WalletRequestPayload.MOST_TEXT;

    public static final CustomPacketPayload.Type<WalletStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "wallet_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WalletStatePayload> STREAM_CODEC = StreamCodec.of(
            WalletStatePayload::write, WalletStatePayload::read);

    public WalletStatePayload {
        wallets = List.copyOf(wallets.size() > MOST_WALLETS ? wallets.subList(0, MOST_WALLETS) : wallets);
        moves = List.copyOf(moves.size() > MOST_MOVES ? moves.subList(0, MOST_MOVES) : moves);
    }

    @Override
    public CustomPacketPayload.Type<WalletStatePayload> type() {
        return TYPE;
    }

    private static String clip(final String text, final int most) {
        return text.length() > most ? text.substring(0, most) : text;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final WalletStatePayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos);
        final Chain chain = payload.chain;
        buf.writeVarInt(chain.height());
        buf.writeVarLong(chain.difficulty());
        buf.writeVarLong(chain.reward());
        buf.writeVarInt(chain.toHalving());
        final Current current = payload.current;
        buf.writeUtf(clip(current.path(), MOST_PATH), MOST_PATH);
        buf.writeUtf(clip(current.file(), MOST_TEXT), MOST_TEXT);
        buf.writeUtf(clip(current.folder(), MOST_PATH), MOST_PATH);
        buf.writeUtf(clip(current.address(), MOST_TEXT), MOST_TEXT);
        buf.writeVarLong(current.balance());
        buf.writeVarLong(current.pending());
        buf.writeUtf(clip(current.newFolder(), MOST_PATH), MOST_PATH);
        buf.writeChar(current.copyDrive());
        buf.writeVarInt(current.note());
        buf.writeVarInt(payload.wallets.size());
        for (final Wallet wallet : payload.wallets) {
            buf.writeUtf(clip(wallet.path(), MOST_PATH), MOST_PATH);
            buf.writeUtf(clip(wallet.file(), MOST_TEXT), MOST_TEXT);
            buf.writeUtf(clip(wallet.address(), MOST_TEXT), MOST_TEXT);
            buf.writeVarLong(wallet.balance());
        }
        buf.writeVarInt(payload.moves.size());
        for (final Move move : payload.moves) {
            buf.writeVarInt(move.kind());
            buf.writeVarLong(move.dayTime());
            buf.writeLong(move.amount());
            buf.writeUtf(clip(move.other(), MOST_TEXT), MOST_TEXT);
            buf.writeUtf(clip(move.label(), MOST_TEXT), MOST_TEXT);
            buf.writeVarInt(move.block());
            buf.writeVarInt(move.blocks() + 1);
        }
        final Print print = payload.print;
        final List<Printer> printers = print.printers().subList(0, Math.min(MOST_PRINTERS, print.printers().size()));
        buf.writeVarInt(printers.size());
        for (final Printer printer : printers) {
            buf.writeLong(printer.pos());
            buf.writeUtf(clip(printer.model(), MOST_TEXT), MOST_TEXT);
            buf.writeVarInt(printer.queued());
        }
        final List<Price> prices = print.prices().subList(0, Math.min(MOST_PRICES, print.prices().size()));
        buf.writeVarInt(prices.size());
        for (final Price price : prices) {
            buf.writeUtf(clip(price.item(), MOST_TEXT), MOST_TEXT);
            buf.writeVarLong(price.units());
        }
        final Auto auto = print.auto();
        buf.writeUtf(clip(auto.item(), MOST_TEXT), MOST_TEXT);
        buf.writeLong(auto.printer());
        buf.writeVarLong(auto.keep());
        buf.writeVarLong(auto.lastDayTime());
        buf.writeVarInt(auto.lastCount());
        buf.writeVarInt(auto.lastBlock());
    }

    private static WalletStatePayload read(final RegistryFriendlyByteBuf buf) {
        final BlockPos hostPos = BlockPos.STREAM_CODEC.decode(buf);
        final Chain chain = new Chain(buf.readVarInt(), buf.readVarLong(), buf.readVarLong(), buf.readVarInt());
        final Current current = new Current(buf.readUtf(MOST_PATH), buf.readUtf(MOST_TEXT), buf.readUtf(MOST_PATH),
                buf.readUtf(MOST_TEXT), buf.readVarLong(), buf.readVarLong(), buf.readUtf(MOST_PATH), buf.readChar(),
                buf.readVarInt());
        final int walletCount = Math.min(MOST_WALLETS, buf.readVarInt());
        final List<Wallet> wallets = new ArrayList<>(walletCount);
        for (int i = 0; i < walletCount; i++) {
            wallets.add(new Wallet(buf.readUtf(MOST_PATH), buf.readUtf(MOST_TEXT), buf.readUtf(MOST_TEXT),
                    buf.readVarLong()));
        }
        final int moveCount = Math.min(MOST_MOVES, buf.readVarInt());
        final List<Move> moves = new ArrayList<>(moveCount);
        for (int i = 0; i < moveCount; i++) {
            moves.add(new Move(buf.readVarInt(), buf.readVarLong(), buf.readLong(), buf.readUtf(MOST_TEXT),
                    buf.readUtf(MOST_TEXT), buf.readVarInt(), buf.readVarInt() - 1));
        }
        final int printerCount = Math.min(MOST_PRINTERS, buf.readVarInt());
        final List<Printer> printers = new ArrayList<>(printerCount);
        for (int i = 0; i < printerCount; i++) {
            printers.add(new Printer(buf.readLong(), buf.readUtf(MOST_TEXT), buf.readVarInt()));
        }
        final int priceCount = Math.min(MOST_PRICES, buf.readVarInt());
        final List<Price> prices = new ArrayList<>(priceCount);
        for (int i = 0; i < priceCount; i++) {
            prices.add(new Price(buf.readUtf(MOST_TEXT), buf.readVarLong()));
        }
        final Auto auto = new Auto(buf.readUtf(MOST_TEXT), buf.readLong(), buf.readVarLong(), buf.readVarLong(),
                buf.readVarInt(), buf.readVarInt());
        return new WalletStatePayload(hostPos, chain, current, wallets, moves, new Print(printers, prices, auto));
    }

    /** The chain's line at the window's foot. */
    public record Chain(int height, long difficulty, long reward, int toHalving) {
    }

    /**
     * The wallet shown, or none: its file's path, name and folder as the system writes it, its address, its balance
     * and what waits to reach it; where a new wallet goes, the drive a copy may go to (0 for none), and what the last
     * request did.
     */
    public record Current(String path, String file, String folder, String address, long balance, long pending,
                          String newFolder, char copyDrive, int note) {

        /** Whether a wallet is shown. */
        public boolean any() {
            return !address.isEmpty();
        }
    }

    /** One of the computer's wallets. */
    public record Wallet(String path, String file, String address, long balance) {
    }

    /**
     * One movement of the wallet shown, as its history lists it.
     *
     * @param kind    what moved the TRC, the id of the ledger's kind of entry
     * @param dayTime the world's time of day it was made
     * @param amount  what it did to the wallet: plus what came in, minus what went out
     * @param other   the address on its other side, or the pool that paid it
     * @param label   what the sender called it, or the item printed
     * @param block   the block it came from, or 0
     * @param blocks  the blocks since it was confirmed, from 1, or -1 while it waits
     */
    public record Move(int kind, long dayTime, long amount, String other, String label, int block, int blocks) {

        /** Whether it waits for a block. */
        public boolean waiting() {
            return blocks < 0;
        }
    }

    /** What the Print tab shows: the printers, the prices and the wallet's Auto print. */
    public record Print(List<Printer> printers, List<Price> prices, Auto auto) {

        /** Nothing to print with. */
        public static final Print NONE = new Print(List.of(), List.of(), Auto.OFF);

        public Print {
            printers = List.copyOf(printers);
            prices = List.copyOf(prices);
        }
    }

    /** A 3D printer linked to the computer: where it is, its model and the jobs waiting in it. */
    public record Printer(long pos, String model, int queued) {
    }

    /** An item the printers print, by its id, and its price. */
    public record Price(String item, long units) {
    }

    /**
     * A wallet's Auto print: the item, the printer and the balance kept, in units, and when it last printed, how many
     * and at which block of the chain (0 before it ever did); an empty item for none.
     */
    public record Auto(String item, long printer, long keep, long lastDayTime, int lastCount, int lastBlock) {

        public static final Auto OFF = new Auto("", 0L, 0L, 0L, 0, 0);

        /** Whether it is set. */
        public boolean on() {
            return !item.isEmpty();
        }
    }
}
