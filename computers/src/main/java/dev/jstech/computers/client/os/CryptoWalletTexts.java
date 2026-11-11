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

/** The Crypto Wallet's words. */
@TextHolder
final class CryptoWalletTexts {

    static final TextKey TITLE = TextKey.of("jsc.crypto_wallet.title", "Crypto Wallet");
    static final TextKey TITLE_OF = TextKey.of("jsc.crypto_wallet.title_of", "Crypto Wallet: %s");
    static final TextKey OVERVIEW = TextKey.of("jsc.crypto_wallet.tab.overview", "Overview");
    static final TextKey SEND = TextKey.of("jsc.crypto_wallet.tab.send", "Send");
    static final TextKey RECEIVE = TextKey.of("jsc.crypto_wallet.tab.receive", "Receive");
    static final TextKey TRANSACTIONS = TextKey.of("jsc.crypto_wallet.tab.transactions", "Transactions");
    static final TextKey PRINT = TextKey.of("jsc.crypto_wallet.tab.print", "Print");
    static final TextKey WALLETS = TextKey.of("jsc.crypto_wallet.tab.wallets", "Wallets");
    static final TextKey BALANCE = TextKey.of("jsc.crypto_wallet.balance", "Balance");
    static final TextKey AMOUNT_TRC = TextKey.of("jsc.crypto_wallet.amount_trc", "%s TRC");
    static final TextKey PENDING = TextKey.of("jsc.crypto_wallet.pending", "Pending");
    static final TextKey CONFIRMS_NEXT = TextKey.of("jsc.crypto_wallet.confirms_next",
            "confirms with the next block");
    static final TextKey CHAIN_LINE = TextKey.of("jsc.crypto_wallet.chain_line",
            "Block %s, difficulty %s, reward %s TRC");
    static final TextKey HALVING_IN = TextKey.of("jsc.crypto_wallet.halving_in", "Halving in %s blocks");
    static final TextKey NO_HALVING = TextKey.of("jsc.crypto_wallet.no_halving", "No halving");
    static final TextKey WALLET_IN = TextKey.of("jsc.crypto_wallet.wallet_in", "%s, in %s");
    static final TextKey WALLET = TextKey.of("jsc.crypto_wallet.wallet", "Wallet");
    static final TextKey ADDRESS = TextKey.of("jsc.crypto_wallet.address", "Address");
    static final TextKey LATEST = TextKey.of("jsc.crypto_wallet.latest", "Latest");
    static final TextKey WHEN = TextKey.of("jsc.crypto_wallet.column.when", "When");
    static final TextKey WHAT = TextKey.of("jsc.crypto_wallet.column.what", "What");
    static final TextKey AMOUNT = TextKey.of("jsc.crypto_wallet.column.amount", "Amount");
    static final TextKey STATUS = TextKey.of("jsc.crypto_wallet.column.status", "Status");
    static final TextKey BLOCKS = TextKey.of("jsc.crypto_wallet.column.blocks", "Blocks");
    static final TextKey SENT_COLUMN = TextKey.of("jsc.crypto_wallet.column.sent", "Sent");
    static final TextKey TO_COLUMN = TextKey.of("jsc.crypto_wallet.column.to", "To");
    static final TextKey CONFIRMS = TextKey.of("jsc.crypto_wallet.column.confirms", "Confirms");
    static final TextKey FILE = TextKey.of("jsc.crypto_wallet.column.file", "File");
    static final TextKey WHEN_AT = TextKey.of("jsc.crypto_wallet.when_at", "Day %s, %s");
    static final TextKey PENDING_STATUS = TextKey.of("jsc.crypto_wallet.status.pending", "pending");
    static final TextKey CONFIRMED = TextKey.of("jsc.crypto_wallet.status.confirmed", "confirmed");
    static final TextKey WAITING = TextKey.of("jsc.crypto_wallet.status.waiting", "waiting");
    static final TextKey NEXT_BLOCK = TextKey.of("jsc.crypto_wallet.status.next_block", "next block");
    static final TextKey POOL_PAYOUT = TextKey.of("jsc.crypto_wallet.move.payout", "Pool payout, %s, block %s");
    static final TextKey POOL_FEE = TextKey.of("jsc.crypto_wallet.move.fee", "Pool fee, %s, block %s");
    static final TextKey BLOCK_REWARD = TextKey.of("jsc.crypto_wallet.move.reward", "Block reward, block %s");
    static final TextKey SENT_TO = TextKey.of("jsc.crypto_wallet.move.sent_to", "Sent to %s");
    static final TextKey SENT_TO_LABEL = TextKey.of("jsc.crypto_wallet.move.sent_to_label", "Sent to %s (%s)");
    static final TextKey RECEIVED_FROM = TextKey.of("jsc.crypto_wallet.move.received_from", "Received from %s");
    static final TextKey PRINTED = TextKey.of("jsc.crypto_wallet.move.printed", "3D Printer: %s");
    static final TextKey TO_LABEL = TextKey.of("jsc.crypto_wallet.to_label", "%s (%s)");
    static final TextKey PAY_TO = TextKey.of("jsc.crypto_wallet.pay_to", "Pay to");
    static final TextKey LABEL = TextKey.of("jsc.crypto_wallet.label", "Label");
    static final TextKey TRC = TextKey.of("jsc.crypto_wallet.trc", "TRC");
    static final TextKey AVAILABLE = TextKey.of("jsc.crypto_wallet.available", "Available %s TRC");
    static final TextKey NO_FEE = TextKey.of("jsc.crypto_wallet.no_fee",
            "No fee. The send waits until the next block confirms it, or 10 minutes when no block comes.");
    static final TextKey CLEAR = TextKey.of("jsc.crypto_wallet.clear", "Clear");
    static final TextKey WAITING_HEADING = TextKey.of("jsc.crypto_wallet.waiting_heading", "Waiting");
    static final TextKey YOUR_ADDRESS = TextKey.of("jsc.crypto_wallet.your_address", "Your address");
    static final TextKey COPY = TextKey.of("jsc.crypto_wallet.copy", "Copy");
    static final TextKey COPIED = TextKey.of("jsc.crypto_wallet.copied", "Copied");
    static final TextKey BELONGS = TextKey.of("jsc.crypto_wallet.belongs",
            "The address belongs to this wallet's file: take the file to another computer and the address goes "
                    + "with it.");
    static final TextKey TO_BE_PAID = TextKey.of("jsc.crypto_wallet.to_be_paid",
            "To be paid here, give it to who pays you, or set it as a miner's payout address (miner --address, or "
                    + "an ASIC's window).");
    static final TextKey ON_THIS_COMPUTER = TextKey.of("jsc.crypto_wallet.on_this_computer",
            "Wallets on this computer");
    static final TextKey SHOW = TextKey.of("jsc.crypto_wallet.show", "Show");
    static final TextKey ALL = TextKey.of("jsc.crypto_wallet.filter.all", "All");
    static final TextKey RECEIVED = TextKey.of("jsc.crypto_wallet.filter.received", "Received");
    static final TextKey SENT = TextKey.of("jsc.crypto_wallet.filter.sent", "Sent");
    static final TextKey PAYOUTS = TextKey.of("jsc.crypto_wallet.filter.payouts", "Pool payouts");
    static final TextKey PRINTED_FILTER = TextKey.of("jsc.crypto_wallet.filter.printed", "Printed");
    static final TextKey LINES_NOTE = TextKey.of("jsc.crypto_wallet.lines_note",
            "A line waits for one block; the blocks column counts the ones since.");
    static final TextKey NO_MOVES = TextKey.of("jsc.crypto_wallet.no_moves", "Nothing has moved yet.");
    static final TextKey NEW_WALLET = TextKey.of("jsc.crypto_wallet.new_wallet", "New wallet");
    static final TextKey OPEN = TextKey.of("jsc.crypto_wallet.open", "Open");
    static final TextKey NO_WALLET = TextKey.of("jsc.crypto_wallet.no_wallet", "No wallet yet");
    static final TextKey WALLET_IS = TextKey.of("jsc.crypto_wallet.wallet_is",
            "A wallet is a file that keeps the key of an address.");
    static final TextKey NAME = TextKey.of("jsc.crypto_wallet.name", "Name");
    static final TextKey SAVE_IN = TextKey.of("jsc.crypto_wallet.save_in", "Save in");
    static final TextKey COPY_TO = TextKey.of("jsc.crypto_wallet.copy_to", "Also save a copy on the USB stick (%s:)");
    static final TextKey KEY_WARNING = TextKey.of("jsc.crypto_wallet.key_warning",
            "The file is the key. Whoever has a copy can spend the coins; lose every copy and they are gone for "
                    + "good. Keep a copy somewhere safe.");
    static final TextKey CREATE = TextKey.of("jsc.crypto_wallet.create", "Create");
    static final TextKey CANCEL = TextKey.of("jsc.crypto_wallet.cancel", "Cancel");
    static final TextKey DEFAULT_NAME = TextKey.of("jsc.crypto_wallet.default_name", "main");
    static final TextKey NOTE_SENT = TextKey.of("jsc.crypto_wallet.note.sent", "Sent; it waits for the next block.");
    static final TextKey NOTE_NOT_YOURS = TextKey.of("jsc.crypto_wallet.note.not_yours",
            "This wallet's file no longer holds its key.");
    static final TextKey NOTE_BAD_ADDRESS = TextKey.of("jsc.crypto_wallet.note.bad_address",
            "That is not a Teracoin address: one starts with trc1q and has 35 letters.");
    static final TextKey NOTE_NOTHING = TextKey.of("jsc.crypto_wallet.note.nothing", "Type an amount to send.");
    static final TextKey NOTE_TOO_MUCH = TextKey.of("jsc.crypto_wallet.note.too_much",
            "The wallet does not hold that much.");
    static final TextKey NOTE_CREATED = TextKey.of("jsc.crypto_wallet.note.created", "The wallet is made.");
    static final TextKey NOTE_NAME_TAKEN = TextKey.of("jsc.crypto_wallet.note.name_taken",
            "There is a wallet of that name in that folder already.");
    static final TextKey NOTE_NOT_WRITTEN = TextKey.of("jsc.crypto_wallet.note.not_written",
            "The wallet could not be written: the folder is not there or the disk is full.");
    static final TextKey NOTE_NO_NAME = TextKey.of("jsc.crypto_wallet.note.no_name",
            "Give the wallet a name of letters and digits.");
    static final TextKey NOTE_PRINTING = TextKey.of("jsc.crypto_wallet.note.printing", "Sent to the printer.");
    static final TextKey NOTE_NOT_PRINTABLE = TextKey.of("jsc.crypto_wallet.note.not_printable",
            "That item is not on the server's list.");
    static final TextKey NOTE_NO_PRINTER = TextKey.of("jsc.crypto_wallet.note.no_printer",
            "No 3D printer is linked to this computer.");
    static final TextKey NOTE_AUTO_SET = TextKey.of("jsc.crypto_wallet.note.auto_set", "Auto print is on.");
    static final TextKey NOTE_AUTO_OFF = TextKey.of("jsc.crypto_wallet.note.auto_off", "Auto print is off.");
    static final TextKey NOTE_QUEUE_FULL = TextKey.of("jsc.crypto_wallet.note.queue_full",
            "The printer's queue is full.");
    static final TextKey PRINTER = TextKey.of("jsc.crypto_wallet.print.printer", "Printer");
    static final TextKey PRINTER_NTH = TextKey.of("jsc.crypto_wallet.print.printer_nth", "%s (%s)");
    static final TextKey ONE_OF_LINKED = TextKey.of("jsc.crypto_wallet.print.one_of_linked",
            "one of the printers linked to this computer");
    static final TextKey NONE_LINKED = TextKey.of("jsc.crypto_wallet.print.none_linked",
            "No 3D printer is linked to this computer.");
    static final TextKey WHAT_PRINTS = TextKey.of("jsc.crypto_wallet.print.what_prints",
            "What this server prints, and its price");
    static final TextKey PRINTS_NOTHING = TextKey.of("jsc.crypto_wallet.print.prints_nothing",
            "This server prints nothing.");
    static final TextKey EACH = TextKey.of("jsc.crypto_wallet.print.each", "%s TRC each");
    static final TextKey COPIES = TextKey.of("jsc.crypto_wallet.print.copies", "Copies");
    static final TextKey TOTAL = TextKey.of("jsc.crypto_wallet.print.total", "Total");
    static final TextKey TAKES = TextKey.of("jsc.crypto_wallet.print.takes", "Takes %s (%s s an item)");
    static final TextKey SECONDS = TextKey.of("jsc.crypto_wallet.print.seconds", "%s s");
    static final TextKey MINUTES = TextKey.of("jsc.crypto_wallet.print.minutes", "%s min");
    static final TextKey MINUTES_SECONDS = TextKey.of("jsc.crypto_wallet.print.minutes_seconds", "%s min %s s");
    static final TextKey PAID_AS_STARTS = TextKey.of("jsc.crypto_wallet.print.paid_as_starts",
            "Each item is paid as it starts: cancel, and the rest is not paid.");
    static final TextKey AUTO_PRINT = TextKey.of("jsc.crypto_wallet.print.auto", "Auto print");
    static final TextKey AUTO_PRINT_BUTTON = TextKey.of("jsc.crypto_wallet.print.auto_button", "Auto print...");
    static final TextKey AUTO_OFF = TextKey.of("jsc.crypto_wallet.print.auto_off", "Off");
    static final TextKey AUTO_ON = TextKey.of("jsc.crypto_wallet.print.auto_on", "On: %s on %s, keeping %s TRC");
    static final TextKey NOT_LINKED_NOW = TextKey.of("jsc.crypto_wallet.print.not_linked_now",
            "a printer no longer linked");
    static final TextKey BY_ITSELF = TextKey.of("jsc.crypto_wallet.print.by_itself",
            "Print from this wallet's balance by itself");
    static final TextKey ITEM = TextKey.of("jsc.crypto_wallet.print.item", "Item");
    static final TextKey PRICE_OPTION = TextKey.of("jsc.crypto_wallet.print.price_option", "%s, %s TRC");
    static final TextKey LINKED_OPTION = TextKey.of("jsc.crypto_wallet.print.linked_option",
            "%s, linked to this computer");
    static final TextKey KEEP_AT_LEAST = TextKey.of("jsc.crypto_wallet.print.keep_at_least", "Keep at least");
    static final TextKey AUTO_EXPLAINED = TextKey.of("jsc.crypto_wallet.print.auto_explained",
            "When the balance passes what it keeps by the price of one, one is printed. A payout that takes it to %s"
                    + " TRC prints %s of them and leaves %s.");
    static final TextKey AUTO_RUNS = TextKey.of("jsc.crypto_wallet.print.auto_runs",
            "It runs while this computer is on with the wallet's file in it.");
    static final TextKey AUTO_LAST = TextKey.of("jsc.crypto_wallet.print.auto_last",
            "Last: %s printed, day %s, %s (block %s)");
    static final TextKey AUTO_NEVER = TextKey.of("jsc.crypto_wallet.print.auto_never", "It has not printed yet.");
    static final TextKey KEEP_INVALID = TextKey.of("jsc.crypto_wallet.print.keep_invalid",
            "Type what the wallet keeps, in TRC.");
    static final TextKey OK = TextKey.of("jsc.crypto_wallet.ok", "OK");

    private CryptoWalletTexts() {
    }
}
