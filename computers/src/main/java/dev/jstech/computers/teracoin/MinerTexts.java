/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What Blockwright's miner says at the prompt. */
@TextHolder
public final class MinerTexts {

    public static final TextKey SUMMARY = TextKey.of("jsc.miner.summary",
            "Mines Teracoin with this computer's processors and graphics cards");
    public static final TextKey USAGE = TextKey.of("jsc.miner.usage",
            "miner [--address ADDRESS] [--pool NAME | --alone] | status | start | stop");
    public static final TextKey DESCRIPTION = TextKey.of("jsc.miner.description",
            "Run alone, it mines in front of the terminal until Q is typed. It pays the address given, for the pool"
                    + " named, the network's first pool when none is named, or alone.");
    public static final TextKey DESCRIPTION_BACKGROUND = TextKey.of("jsc.miner.description_background",
            "miner start keeps it mining in the background while the computer is on; miner status says how it is"
                    + " doing and miner stop stops it. Settings given while it runs in the background take effect"
                    + " at once.");
    public static final TextKey OPTION_ADDRESS = TextKey.of("jsc.miner.option.address",
            "the address the miner pays, from a Crypto Wallet's Receive tab");
    public static final TextKey OPTION_POOL = TextKey.of("jsc.miner.option.pool",
            "mine for the pool of that name, or the server it runs on");
    public static final TextKey OPTION_ALONE = TextKey.of("jsc.miner.option.alone",
            "mine alone, whatever pools the network has");
    public static final TextKey EXAMPLE_FIRST = TextKey.of("jsc.miner.example.first",
            "keep mining in the background, paying the address --address set");
    public static final TextKey EXAMPLE_STATUS = TextKey.of("jsc.miner.example.status",
            "how the miner running in the background is doing");
    public static final TextKey BANNER = TextKey.of("jsc.miner.banner", "Blockwright miner %s, Teracoin");
    public static final TextKey ALONE_NO_POOL = TextKey.of("jsc.miner.alone_no_pool",
            "No pool on this network: mining alone, paying %s");
    public static final TextKey ALONE_CHOSEN = TextKey.of("jsc.miner.alone_chosen", "Mining alone, paying %s");
    public static final TextKey FOR_POOL = TextKey.of("jsc.miner.for_pool", "Mining for %s on %s, fee %s, paying %s");
    public static final TextKey CPU_ROW = TextKey.of("jsc.miner.cpu_row", "CPU   %s");
    public static final TextKey GPU_ROW = TextKey.of("jsc.miner.gpu_row", "GPU%s  slot %s");
    public static final TextKey GPU_RISER_ROW = TextKey.of("jsc.miner.gpu_riser_row", "GPU%s  riser %s");
    public static final TextKey CORES = TextKey.of("jsc.miner.cores", "%s (%s cores)");
    public static final TextKey BLOCK_LINE = TextKey.of("jsc.miner.block_line",
            "block %s, difficulty %s, reward %s TRC");
    public static final TextKey HASHING_WATTS = TextKey.of("jsc.miner.hashing_watts", "hashing %s, %s W");
    public static final TextKey HASHING = TextKey.of("jsc.miner.hashing", "hashing %s");
    public static final TextKey ALONE_EVERY = TextKey.of("jsc.miner.alone_every",
            "hashing %s: alone, a block about every %s");
    public static final TextKey POOL_HINT = TextKey.of("jsc.miner.pool_hint",
            "a pool pays a share of every block it finds instead");
    public static final TextKey FOUND = TextKey.of("jsc.miner.found", "this machine found block %s");
    public static final TextKey STAMP = TextKey.of("jsc.miner.stamp", "[%s] %s");
    public static final TextKey PRESS_Q = TextKey.of("jsc.miner.press_q", "Press Q to stop.");
    public static final TextKey STOPPED = TextKey.of("jsc.miner.stopped", "Miner stopped.");
    public static final TextKey NO_ADDRESS = TextKey.of("jsc.miner.no_address",
            "No payout address: miner --address trc1... sets one");
    public static final TextKey BAD_ADDRESS = TextKey.of("jsc.miner.bad_address", "%s is not a Teracoin address");
    public static final TextKey NO_POOL = TextKey.of("jsc.miner.no_pool", "No pool named %s on this network");
    public static final TextKey WAITING = TextKey.of("jsc.miner.waiting",
            "%s takes only the miners its owner accepts: this one asked to join and waits");
    public static final TextKey NOTHING_HASHES = TextKey.of("jsc.miner.nothing_hashes",
            "Nothing on this machine hashes");
    public static final TextKey ADDRESS_SET = TextKey.of("jsc.miner.address_set",
            "Payout address set; the next round pays it.");
    public static final TextKey POOL_SET = TextKey.of("jsc.miner.pool_set", "Mining for %s from now on.");
    public static final TextKey FIRST_POOL_SET = TextKey.of("jsc.miner.first_pool_set",
            "Mining for the network's first pool from now on.");
    public static final TextKey ALONE_SET = TextKey.of("jsc.miner.alone_set", "Mining alone from now on.");
    public static final TextKey STARTED = TextKey.of("jsc.miner.started",
            "The miner runs in the background: miner status shows it, miner stop stops it.");
    public static final TextKey BACKGROUND_STOPPED = TextKey.of("jsc.miner.background_stopped",
            "The miner stopped.");
    public static final TextKey NOT_RUNNING = TextKey.of("jsc.miner.not_running", "The miner is not running.");
    public static final TextKey CANNOT = TextKey.of("jsc.miner.cannot", "This machine cannot mine.");
    public static final TextKey LABEL_POOL = TextKey.of("jsc.miner.label.pool", "Pool");
    public static final TextKey LABEL_PAYS = TextKey.of("jsc.miner.label.pays", "Pays");
    public static final TextKey LABEL_CPU = TextKey.of("jsc.miner.label.cpu", "CPU");
    public static final TextKey LABEL_GPU = TextKey.of("jsc.miner.label.gpu", "GPU%s");
    public static final TextKey LABEL_GPUS = TextKey.of("jsc.miner.label.gpus", "GPU0-%s");
    public static final TextKey LABEL_TOTAL = TextKey.of("jsc.miner.label.total", "Total");
    public static final TextKey LABEL_ROUND = TextKey.of("jsc.miner.label.round", "Round");
    public static final TextKey LABEL_SHARE = TextKey.of("jsc.miner.label.share", "Share");
    public static final TextKey LABEL_PAID = TextKey.of("jsc.miner.label.paid", "Paid");
    public static final TextKey LABEL_EXPECTED = TextKey.of("jsc.miner.label.expected", "Expected");
    public static final TextKey STATUS_POOL = TextKey.of("jsc.miner.status.pool", "%s on %s, fee %s");
    public static final TextKey STATUS_ALONE = TextKey.of("jsc.miner.status.alone", "none: mining alone");
    public static final TextKey STATUS_EACH = TextKey.of("jsc.miner.status.each", "%s each, %s");
    public static final TextKey STATUS_TOTAL = TextKey.of("jsc.miner.status.total", "%s, %s W");
    public static final TextKey STATUS_ROUND = TextKey.of("jsc.miner.status.round",
            "%s since the pool's last block");
    public static final TextKey STATUS_SHARE = TextKey.of("jsc.miner.status.share",
            "%s of the pool's work this round");
    public static final TextKey STATUS_PAID_WAITING = TextKey.of("jsc.miner.status.paid_waiting",
            "%s TRC for block %s (waiting for a block)");
    public static final TextKey STATUS_PAID = TextKey.of("jsc.miner.status.paid", "%s TRC for block %s");
    public static final TextKey STATUS_EXPECTED = TextKey.of("jsc.miner.status.expected", "a block about every %s");
    public static final TextKey MINUTES = TextKey.of("jsc.miner.minutes", "%s minutes");
    public static final TextKey HOURS = TextKey.of("jsc.miner.hours", "%s hours");
    public static final TextKey DAYS = TextKey.of("jsc.miner.days", "%s days");

    private MinerTexts() {
    }
}
