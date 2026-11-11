/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The Mining Panel's words. */
@TextHolder
final class MiningPanelTexts {

    static final TextKey TITLE = TextKey.of("jsc.mining_panel.title", "Mining Panel");
    static final TextKey POOL = TextKey.of("jsc.mining_panel.tab.pool", "Pool");
    static final TextKey MINERS = TextKey.of("jsc.mining_panel.tab.miners", "Miners");
    static final TextKey BLOCKS = TextKey.of("jsc.mining_panel.tab.blocks", "Blocks");
    static final TextKey SETTINGS = TextKey.of("jsc.mining_panel.tab.settings", "Settings");
    static final TextKey NO_POOL = TextKey.of("jsc.mining_panel.no_pool", "No Mining Pool on this network");
    static final TextKey NO_POOL_HINT = TextKey.of("jsc.mining_panel.no_pool_hint",
            "A Mining Pool is a service a rack server runs; install it on one of this network's servers.");
    static final TextKey HEADER = TextKey.of("jsc.mining_panel.header", "%s on %s, fee %s, owner %s");
    static final TextKey HEADER_NO_OWNER = TextKey.of("jsc.mining_panel.header_no_owner", "%s on %s, fee %s");
    static final TextKey HASHRATE = TextKey.of("jsc.mining_panel.hashrate", "Hashrate");
    static final TextKey AN_HOUR = TextKey.of("jsc.mining_panel.an_hour", "TRC an hour");
    static final TextKey A_DAY = TextKey.of("jsc.mining_panel.a_day", "A day");
    static final TextKey A_WEEK = TextKey.of("jsc.mining_panel.a_week", "A week");
    static final TextKey BLOCKS_FOUND = TextKey.of("jsc.mining_panel.blocks_found", "Blocks found");
    static final TextKey LAST_HOUR = TextKey.of("jsc.mining_panel.last_hour", "The last hour");
    static final TextKey SHARES = TextKey.of("jsc.mining_panel.shares", "Shares of the round");
    static final TextKey YOU = TextKey.of("jsc.mining_panel.you", "%s (you)");
    static final TextKey NO_SHARES = TextKey.of("jsc.mining_panel.no_shares", "No work counted this round yet");
    static final TextKey MINER = TextKey.of("jsc.mining_panel.column.miner", "Miner");
    static final TextKey KIND = TextKey.of("jsc.mining_panel.column.kind", "Kind");
    static final TextKey PAYS = TextKey.of("jsc.mining_panel.column.pays", "Pays");
    static final TextKey SHARE = TextKey.of("jsc.mining_panel.column.share", "Share");
    static final TextKey BLOCK = TextKey.of("jsc.mining_panel.column.block", "Block");
    static final TextKey WHEN = TextKey.of("jsc.mining_panel.column.when", "When");
    static final TextKey FOUND_BY = TextKey.of("jsc.mining_panel.column.found_by", "Found by");
    static final TextKey REWARD = TextKey.of("jsc.mining_panel.column.reward", "Reward");
    static final TextKey ASKING = TextKey.of("jsc.mining_panel.asking", "asking");
    static final TextKey NO_MINERS = TextKey.of("jsc.mining_panel.no_miners", "No miner mines for this pool now");
    static final TextKey NO_BLOCKS = TextKey.of("jsc.mining_panel.no_blocks", "The pool has not found a block yet");
    static final TextKey EVERY = TextKey.of("jsc.mining_panel.every",
            "The pool finds a block about every %s; the difficulty adjusts in %s blocks.");
    static final TextKey RIG = TextKey.of("jsc.mining_panel.kind.rig", "%s rig, %s cards");
    static final TextKey ASIC = TextKey.of("jsc.mining_panel.kind.asic", "%s ASIC");
    static final TextKey PERSONAL_COMPUTER = TextKey.of("jsc.mining_panel.kind.personal_computer",
            "Personal Computer");
    static final TextKey MAINFRAME = TextKey.of("jsc.mining_panel.kind.mainframe", "Mainframe");
    static final TextKey SERVER = TextKey.of("jsc.mining_panel.kind.server", "Server");
    static final TextKey COMPUTER = TextKey.of("jsc.mining_panel.kind.computer", "Computer");
    static final TextKey TRANSITION = TextKey.of("jsc.mining_panel.era.transition", "Transition");
    static final TextKey STANDARD = TextKey.of("jsc.mining_panel.era.standard", "Standard");
    static final TextKey ADVANCED = TextKey.of("jsc.mining_panel.era.advanced", "Advanced");
    static final TextKey YOURS = TextKey.of("jsc.mining_panel.yours",
            "%s, on %s. You own it: these settings are yours alone.");
    static final TextKey NAME = TextKey.of("jsc.mining_panel.name", "Name");
    static final TextKey FEE = TextKey.of("jsc.mining_panel.fee", "Fee");
    static final TextKey FEE_TO = TextKey.of("jsc.mining_panel.fee_to", "Fee paid to");
    static final TextKey WHO = TextKey.of("jsc.mining_panel.who", "Who may join");
    static final TextKey ANY_MINER = TextKey.of("jsc.mining_panel.any_miner", "Any miner on the network");
    static final TextKey ACCEPTED = TextKey.of("jsc.mining_panel.accepted", "Miners I accept");
    static final TextKey ASKING_HEADING = TextKey.of("jsc.mining_panel.asking_heading", "Asking to join");
    static final TextKey NOBODY_ASKS = TextKey.of("jsc.mining_panel.nobody_asks", "Nobody is asking to join");
    static final TextKey ACCEPT = TextKey.of("jsc.mining_panel.accept", "Accept");
    static final TextKey REFUSE = TextKey.of("jsc.mining_panel.refuse", "Refuse");
    static final TextKey FEE_NOTE = TextKey.of("jsc.mining_panel.fee_note",
            "The fee comes off each block's reward before the shares are paid.");
    static final TextKey PERCENT = TextKey.of("jsc.mining_panel.percent", "%s%%");

    private MiningPanelTexts() {
    }
}
