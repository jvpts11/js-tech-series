/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.core.id.StableNames;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** How Teracoin's chain and a Mining Pool are written into a save and read back. */
public final class TeracoinNbt {

    private static final StableNames<LedgerEntry.Kind> KINDS = StableNames.of(LedgerEntry.Kind.class);

    private TeracoinNbt() {
    }

    public static CompoundTag save(final TeracoinChain chain) {
        final TeracoinChain.Saved saved = chain.saved();
        final CompoundTag tag = new CompoundTag();
        tag.putInt("Height", saved.height());
        tag.putLong("Difficulty", saved.difficulty());
        tag.putLong("WindowStart", saved.windowStart());
        tag.putInt("HalvingFrom", saved.halvingFrom());
        tag.putLong("Issued", saved.issued());
        tag.putLong("Burned", saved.burned());
        tag.putLong("NextId", saved.nextId());
        final ListTag wallets = new ListTag();
        for (final Map.Entry<String, String> key : saved.keys().entrySet()) {
            final CompoundTag wallet = new CompoundTag();
            wallet.putString("Address", key.getKey());
            wallet.putString("Key", key.getValue());
            wallet.putLong("Balance", saved.balances().getOrDefault(key.getKey(), 0L));
            wallets.add(wallet);
        }
        for (final Map.Entry<String, Long> balance : saved.balances().entrySet()) {
            if (!saved.keys().containsKey(balance.getKey())) {
                final CompoundTag wallet = new CompoundTag();
                wallet.putString("Address", balance.getKey());
                wallet.putLong("Balance", balance.getValue());
                wallets.add(wallet);
            }
        }
        tag.put("Wallets", wallets);
        final ListTag ledger = new ListTag();
        for (final LedgerEntry entry : saved.ledger()) {
            ledger.add(entry(entry));
        }
        tag.put("Ledger", ledger);
        tag.put("Blocks", blocks(saved.blocks()));
        return tag;
    }

    public static void load(final TeracoinChain chain, final CompoundTag tag) {
        final Map<String, Long> balances = new HashMap<>();
        final Map<String, String> keys = new HashMap<>();
        for (final Tag one : tag.getList("Wallets", Tag.TAG_COMPOUND)) {
            final CompoundTag wallet = (CompoundTag) one;
            final String address = wallet.getString("Address");
            if (wallet.contains("Key")) {
                keys.put(address, wallet.getString("Key"));
            }
            if (wallet.getLong("Balance") != 0L) {
                balances.put(address, wallet.getLong("Balance"));
            }
        }
        final List<LedgerEntry> ledger = new ArrayList<>();
        for (final Tag one : tag.getList("Ledger", Tag.TAG_COMPOUND)) {
            ledger.add(entry((CompoundTag) one));
        }
        chain.restore(new TeracoinChain.Saved(tag.getInt("Height"),
                tag.contains("Difficulty") ? tag.getLong("Difficulty") : TeracoinRules.START_DIFFICULTY,
                tag.contains("WindowStart") ? tag.getLong("WindowStart") : -1L, tag.getInt("HalvingFrom"),
                tag.getLong("Issued"), tag.getLong("Burned"), tag.getLong("NextId"), balances, keys, ledger,
                blocks(tag.getList("Blocks", Tag.TAG_COMPOUND))));
    }

    public static CompoundTag save(final MiningPool pool) {
        final MiningPool.Saved saved = pool.saved();
        final CompoundTag tag = new CompoundTag();
        tag.putString("Name", saved.name());
        tag.putString("Owner", saved.owner());
        tag.putInt("Fee", saved.feePermille());
        tag.putBoolean("AcceptedOnly", saved.acceptedOnly());
        tag.putLong("RoundStart", saved.roundStart());
        final ListTag accepted = new ListTag();
        for (final String machine : saved.accepted()) {
            accepted.add(StringTag.valueOf(machine));
        }
        tag.put("Accepted", accepted);
        final ListTag requests = new ListTag();
        for (final MiningPool.Request request : saved.requests()) {
            final CompoundTag one = new CompoundTag();
            one.putString("Machine", request.machine());
            one.putString("Name", request.name());
            one.putString("Kind", request.kind());
            one.putString("Address", request.address());
            one.putLong("Hashrate", request.hashrate());
            requests.add(one);
        }
        tag.put("Requests", requests);
        final ListTag work = new ListTag();
        for (final Map.Entry<String, Long> entry : saved.work().entrySet()) {
            final CompoundTag one = new CompoundTag();
            one.putString("Address", entry.getKey());
            one.putLong("Hashes", entry.getValue());
            work.add(one);
        }
        tag.put("Work", work);
        tag.put("Found", blocks(saved.found()));
        tag.putInt("FoundCount", saved.foundCount());
        return tag;
    }

    public static void load(final MiningPool pool, final CompoundTag tag) {
        final List<String> accepted = new ArrayList<>();
        for (final Tag one : tag.getList("Accepted", Tag.TAG_STRING)) {
            accepted.add(one.getAsString());
        }
        final List<MiningPool.Request> requests = new ArrayList<>();
        for (final Tag one : tag.getList("Requests", Tag.TAG_COMPOUND)) {
            final CompoundTag request = (CompoundTag) one;
            requests.add(new MiningPool.Request(request.getString("Machine"), request.getString("Name"),
                    request.getString("Kind"), request.getString("Address"), request.getLong("Hashrate")));
        }
        final Map<String, Long> work = new LinkedHashMap<>();
        for (final Tag one : tag.getList("Work", Tag.TAG_COMPOUND)) {
            final CompoundTag entry = (CompoundTag) one;
            work.put(entry.getString("Address"), entry.getLong("Hashes"));
        }
        pool.restore(new MiningPool.Saved(tag.getString("Name"), tag.getString("Owner"),
                tag.contains("Fee") ? tag.getInt("Fee") : MiningPool.DEFAULT_FEE, tag.getBoolean("AcceptedOnly"),
                tag.contains("RoundStart") ? tag.getLong("RoundStart") : -1L, accepted, requests, work,
                blocks(tag.getList("Found", Tag.TAG_COMPOUND)), tag.getInt("FoundCount")));
    }

    /** Writes a computer's miner's settings into {@code tag}, when it has any. */
    public static void save(final MinerSettings miner, final CompoundTag tag) {
        final MinerSettings.Saved saved = miner.saved();
        if (saved.address().isEmpty() && saved.pool().isEmpty() && !saved.alone() && !saved.background()) {
            return;
        }
        final CompoundTag one = new CompoundTag();
        one.putString("Address", saved.address());
        one.putString("Pool", saved.pool());
        one.putBoolean("Alone", saved.alone());
        one.putBoolean("Background", saved.background());
        tag.put("Miner", one);
    }

    /** Writes a computer's wallets' Auto prints into {@code tag}, when it has any. */
    public static void save(final AutoPrints prints, final CompoundTag tag) {
        final List<AutoPrints.Auto> all = prints.all();
        if (all.isEmpty()) {
            return;
        }
        final ListTag list = new ListTag();
        for (final AutoPrints.Auto auto : all) {
            final CompoundTag one = new CompoundTag();
            one.putString("Wallet", auto.wallet());
            one.putString("Item", auto.item());
            one.putLong("Printer", auto.printer());
            one.putLong("Keep", auto.keep());
            one.putLong("LastDayTime", auto.lastDayTime());
            one.putInt("LastCount", auto.lastCount());
            one.putInt("LastBlock", auto.lastBlock());
            list.add(one);
        }
        tag.put("AutoPrints", list);
    }

    /** Reads a computer's wallets' Auto prints back from {@code tag}. */
    public static void load(final AutoPrints prints, final CompoundTag tag) {
        final List<AutoPrints.Auto> read = new ArrayList<>();
        final ListTag list = tag.getList("AutoPrints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag one = list.getCompound(i);
            read.add(new AutoPrints.Auto(one.getString("Wallet"), one.getString("Item"), one.getLong("Printer"),
                    one.getLong("Keep"), one.getLong("LastDayTime"), one.getInt("LastCount"),
                    one.getInt("LastBlock")));
        }
        prints.restore(read);
    }

    /** Reads a computer's miner's settings back from {@code tag}. */
    public static void load(final MinerSettings miner, final CompoundTag tag) {
        final CompoundTag one = tag.getCompound("Miner");
        miner.restore(new MinerSettings.Saved(one.getString("Address"), one.getString("Pool"),
                one.getBoolean("Alone"), one.getBoolean("Background")));
    }

    private static CompoundTag entry(final LedgerEntry entry) {
        final CompoundTag tag = new CompoundTag();
        tag.putLong("Id", entry.id());
        tag.putString("Kind", entry.kind().serializedName());
        tag.putString("From", entry.from());
        tag.putString("To", entry.to());
        tag.putLong("Amount", entry.amount());
        tag.putString("Label", entry.label());
        tag.putInt("Block", entry.block());
        tag.putLong("Tick", entry.tick());
        tag.putLong("DayTime", entry.dayTime());
        tag.putInt("Confirmed", entry.confirmedAt());
        return tag;
    }

    private static LedgerEntry entry(final CompoundTag tag) {
        return new LedgerEntry(tag.getLong("Id"), KINDS.byName(tag.getString("Kind"), LedgerEntry.Kind.SEND),
                tag.getString("From"), tag.getString("To"), tag.getLong("Amount"), tag.getString("Label"),
                tag.getInt("Block"), tag.getLong("Tick"), tag.getLong("DayTime"), tag.getInt("Confirmed"));
    }

    private static ListTag blocks(final List<FoundBlock> blocks) {
        final ListTag list = new ListTag();
        for (final FoundBlock block : blocks) {
            final CompoundTag tag = new CompoundTag();
            tag.putInt("Height", block.height());
            tag.putLong("Tick", block.tick());
            tag.putLong("DayTime", block.dayTime());
            tag.putLong("Reward", block.reward());
            tag.putLong("Difficulty", block.difficulty());
            tag.putString("Machine", block.machine());
            tag.putString("Finder", block.finder());
            tag.putString("Address", block.address());
            tag.putString("Pool", block.pool());
            tag.putBoolean("Rare", block.rare());
            list.add(tag);
        }
        return list;
    }

    private static List<FoundBlock> blocks(final ListTag list) {
        final List<FoundBlock> out = new ArrayList<>();
        for (final Tag one : list) {
            final CompoundTag tag = (CompoundTag) one;
            out.add(new FoundBlock(tag.getInt("Height"), tag.getLong("Tick"), tag.getLong("DayTime"),
                    tag.getLong("Reward"), tag.getLong("Difficulty"), tag.getString("Machine"),
                    tag.getString("Finder"), tag.getString("Address"), tag.getString("Pool"), tag.getBoolean("Rare")));
        }
        return out;
    }
}
