/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** An ASIC's window's words, apart from the window so a server can read them without loading a screen. */
@TextHolder
final class AsicTexts {

    static final TextKey MAKER = TextKey.of("jsc.asic.maker", "BLOCKWRIGHT");
    static final TextKey MINING = TextKey.of("jsc.asic.mining", "MINING");
    static final TextKey NO_POWER = TextKey.of("jsc.asic.no_power", "NO POWER");
    static final TextKey NO_ADDRESS = TextKey.of("jsc.asic.no_address", "NO ADDRESS");
    static final TextKey NO_POOL = TextKey.of("jsc.asic.no_pool", "NO POOL");
    static final TextKey WAITING = TextKey.of("jsc.asic.waiting", "WAITING");
    static final TextKey IDLE = TextKey.of("jsc.asic.idle", "IDLE");
    static final TextKey PAYS_TO = TextKey.of("jsc.asic.pays_to", "PAYS TO");
    static final TextKey ADDRESS_HINT = TextKey.of("jsc.asic.address_hint", "trc1q... the address it pays");
    static final TextKey POOL = TextKey.of("jsc.asic.pool", "POOL");
    static final TextKey POOL_ON = TextKey.of("jsc.asic.pool_on", "%s, %s");
    static final TextKey FIRST_POOL = TextKey.of("jsc.asic.first_pool", "The network's pool");
    static final TextKey SOLO = TextKey.of("jsc.asic.solo", "Solo");
    static final TextKey SET = TextKey.of("jsc.asic.set", "SET");
    static final TextKey HASHBOARDS = TextKey.of("jsc.asic.hashboards", "HASHBOARDS");
    static final TextKey BOARD = TextKey.of("jsc.asic.board", "#%s");
    static final TextKey CELSIUS = TextKey.of("jsc.asic.celsius", "%s C");
    static final TextKey HASHRATE = TextKey.of("jsc.asic.hashrate", "HASHRATE");
    static final TextKey DRAW = TextKey.of("jsc.asic.draw", "DRAW");
    static final TextKey WATTS = TextKey.of("jsc.asic.watts", "%s W");
    static final TextKey FANS = TextKey.of("jsc.asic.fans", "FANS");
    static final TextKey RPM = TextKey.of("jsc.asic.rpm", "%s rpm");
    static final TextKey FOUND = TextKey.of("jsc.asic.found", "FOUND");
    static final TextKey ONE_BLOCK = TextKey.of("jsc.asic.one_block", "1 block");
    static final TextKey BLOCKS = TextKey.of("jsc.asic.blocks", "%s blocks");
    static final TextKey SYSTEM = TextKey.of("jsc.asic.system", "SYSTEM");
    static final TextKey NONE = TextKey.of("jsc.asic.none", "none");
    static final TextKey UPTIME = TextKey.of("jsc.asic.uptime", "UPTIME");
    static final TextKey MINUTES = TextKey.of("jsc.asic.minutes", "%s min");
    static final TextKey HOURS = TextKey.of("jsc.asic.hours", "%s h");
    static final TextKey DAYS = TextKey.of("jsc.asic.days", "%s days");

    private AsicTexts() {
    }
}
