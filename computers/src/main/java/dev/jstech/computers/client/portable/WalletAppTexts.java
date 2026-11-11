/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of a portable's Crypto Wallet. */
@TextHolder
final class WalletAppTexts {

    static final TextKey NOT_CONNECTED = TextKey.of("jsc.mobile.wallet.not_connected",
            "Join a network to reach the chain");
    static final TextKey NO_WALLET = TextKey.of("jsc.mobile.wallet.no_wallet", "No wallet on this device yet");
    static final TextKey KEPT_HERE = TextKey.of("jsc.mobile.wallet.kept_here",
            "Its key is kept on the device: lose the device, lose the coins");
    static final TextKey MAKE = TextKey.of("jsc.mobile.wallet.make", "Make a wallet");
    static final TextKey BALANCE = TextKey.of("jsc.mobile.wallet.balance", "BALANCE");
    static final TextKey TRC = TextKey.of("jsc.mobile.wallet.trc", "TRC");
    static final TextKey SEND = TextKey.of("jsc.mobile.wallet.send", "Send");
    static final TextKey RECEIVE = TextKey.of("jsc.mobile.wallet.receive", "Receive");
    static final TextKey HISTORY = TextKey.of("jsc.mobile.wallet.history", "HISTORY");
    static final TextKey NOTHING_YET = TextKey.of("jsc.mobile.wallet.nothing_yet", "Nothing has moved yet");
    static final TextKey TO = TextKey.of("jsc.mobile.wallet.to", "to %s");
    static final TextKey FROM = TextKey.of("jsc.mobile.wallet.from", "from %s");
    static final TextKey POOL_PAYOUT = TextKey.of("jsc.mobile.wallet.pool_payout", "pool payout");
    static final TextKey BLOCK_REWARD = TextKey.of("jsc.mobile.wallet.block_reward", "block reward");
    static final TextKey PRINTED = TextKey.of("jsc.mobile.wallet.printed", "3D printer");
    static final TextKey PENDING_NEXT = TextKey.of("jsc.mobile.wallet.pending_next", "pending, next block");
    static final TextKey BLOCK = TextKey.of("jsc.mobile.wallet.block", "block %s");
    static final TextKey PAY_TO = TextKey.of("jsc.mobile.wallet.pay_to", "Pay to");
    static final TextKey ADDRESS_HINT = TextKey.of("jsc.mobile.wallet.address_hint", "trc1q...");
    static final TextKey AMOUNT = TextKey.of("jsc.mobile.wallet.amount", "Amount, in TRC");
    static final TextKey AVAILABLE = TextKey.of("jsc.mobile.wallet.available", "Available %s TRC");
    static final TextKey NO_FEE = TextKey.of("jsc.mobile.wallet.no_fee", "No fee; it waits for the next block");
    static final TextKey YOUR_ADDRESS = TextKey.of("jsc.mobile.wallet.your_address", "Your address");
    static final TextKey COPY = TextKey.of("jsc.mobile.wallet.copy", "Copy");
    static final TextKey BAD_ADDRESS = TextKey.of("jsc.mobile.wallet.bad_address", "That is not an address");
    static final TextKey TOO_MUCH = TextKey.of("jsc.mobile.wallet.too_much", "The wallet does not hold that much");
    static final TextKey NOTHING = TextKey.of("jsc.mobile.wallet.nothing", "Type an amount to send");
    static final TextKey NOT_SENT = TextKey.of("jsc.mobile.wallet.not_sent", "It was not sent");

    private WalletAppTexts() {
    }
}
