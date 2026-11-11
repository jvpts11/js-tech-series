/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.teracoin.MinerConsole;
import dev.jstech.computers.teracoin.MinerSettings;
import dev.jstech.computers.teracoin.MiningPool;
import dev.jstech.computers.teracoin.MiningPools;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.computers.teracoin.TeracoinChain;
import dev.jstech.computers.teracoin.WalletFiles;
import dev.jstech.computers.vm.program.Halt;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Teracoin as a program reaches it through its {@link TeracoinService}: a wallet's balance, address and sends, the
 * chain's height, difficulty and reward, the machine's own miner as its {@code miner} command sets it, and the Mining
 * Pools of its network. Amounts are whole hundred-millionths of a TRC, as the chain keeps them. A wallet file that
 * holds none, an address written wrong, an amount of nothing, or a label of more than a line stops the program; a
 * send the wallet cannot cover answers false.
 */
@TextHolder
final class TeracoinCalls {

    private static final String STRING = "string";
    private static final String WALLET = "Wallet";
    private static final String CHAIN = "Chain";
    private static final String MINER = "Miner";
    /** The longest label a send carries, as the Wallet program takes it. */
    private static final int LABEL_MOST = 128;

    private static final TextKey NO_WALLET = TextKey.of("jsc.service.teracoin.no_wallet",
            "'%s' holds no wallet");
    private static final TextKey NOT_AN_ADDRESS = TextKey.of("jsc.service.teracoin.not_an_address",
            "'%s' is not a Teracoin address");
    private static final TextKey NOT_AN_AMOUNT = TextKey.of("jsc.service.teracoin.not_an_amount",
            "%s is not an amount to send");
    private static final TextKey NOT_A_LABEL = TextKey.of("jsc.service.teracoin.not_a_label",
            "a label is one line of up to %s characters");

    private TeracoinCalls() {
    }

    static void bind(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        wallet(bindings);
        teracoin(bindings, CHAIN, "Height", (teracoin, call, target, arguments, line) -> teracoin.chain().height());
        teracoin(bindings, CHAIN, "Difficulty",
                (teracoin, call, target, arguments, line) -> teracoin.chain().difficulty(teracoin.worldDays()));
        teracoin(bindings, CHAIN, "Reward", (teracoin, call, target, arguments, line) -> teracoin.chain().nextReward());
        miner(bindings);
        teracoin(bindings, "Pool", "Names", (teracoin, call, target, arguments, line) -> {
            final Values.ListValue names = new Values.ListValue();
            names.items().addAll(teracoin.pools());
            return names;
        });
        teracoin(bindings, "Pool", "Of", (teracoin, call, target, arguments, line) -> {
            final ServerServices.Host host = teracoin.pool(String.valueOf(arguments[0]));
            return host == null ? null : poolOf(host, teracoin);
        }, STRING);
    }

    private static void wallet(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        teracoin(bindings, WALLET, "Balance",
                (teracoin, call, target, arguments, line) -> teracoin.chain().balance(address(arguments[0], line)),
                STRING);
        teracoin(bindings, WALLET, "Address", (teracoin, call, target, arguments, line) ->
                teracoin.wallet(String.valueOf(arguments[0])).map(WalletFiles.Wallet::address).orElse(""), STRING);
        teracoin(bindings, WALLET, "Send", (teracoin, call, target, arguments, line) -> {
            final String path = String.valueOf(arguments[0]);
            final Optional<WalletFiles.Wallet> wallet = teracoin.wallet(path);
            if (wallet.isEmpty()) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NO_WALLET.with(path), Halt.Thrown.ARGUMENT);
            }
            final String to = address(arguments[1], line);
            final long amount = ((Number) arguments[2]).longValue();
            if (amount <= 0) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_AN_AMOUNT.with(amount),
                        Halt.Thrown.ARGUMENT_OUT_OF_RANGE);
            }
            final String label = String.valueOf(arguments[3]);
            if (label.length() > LABEL_MOST || label.indexOf('\n') >= 0) {
                throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_A_LABEL.with(LABEL_MOST), Halt.Thrown.ARGUMENT);
            }
            return teracoin.send(wallet.get(), to, amount, label) == TeracoinChain.SendResult.SENT;
        }, STRING, STRING, "long", STRING);
    }

    /* The machine's miner, read and set as its miner command reads and sets it, and kept with the machine. */
    private static void miner(final Map<MemberId, MachineCalls.Binding<?>> bindings) {
        miner(bindings, "Installed", (miner, call, target, arguments, line) -> miner.installed());
        miner(bindings, "Status", (miner, call, target, arguments, line) ->
                miner.settings().status().name().toLowerCase(Locale.ROOT));
        miner(bindings, "Hashrate", (miner, call, target, arguments, line) -> miner.settings().hashrate());
        miner(bindings, "Pool", (miner, call, target, arguments, line) -> miner.settings().poolShown());
        miner(bindings, "Address", (miner, call, target, arguments, line) -> miner.settings().address());
        miner(bindings, "Background", (miner, call, target, arguments, line) -> miner.settings().background());
        miner(bindings, "SetAddress", (miner, call, target, arguments, line) -> {
            miner.settings().setAddress(address(arguments[0], line));
            miner.changed();
            return null;
        }, STRING);
        miner(bindings, "UsePool", (miner, call, target, arguments, line) -> {
            miner.settings().setPool(String.valueOf(arguments[0]));
            miner.changed();
            return null;
        }, STRING);
        miner(bindings, "MineAlone", (miner, call, target, arguments, line) -> {
            miner.settings().setAlone();
            miner.changed();
            return null;
        });
        miner(bindings, "Start", (miner, call, target, arguments, line) -> {
            if (!TeracoinAddress.valid(miner.settings().address())) {
                return false;
            }
            miner.settings().setBackground(true);
            miner.changed();
            return true;
        });
        miner(bindings, "Stop", (miner, call, target, arguments, line) -> {
            final MinerSettings settings = miner.settings();
            settings.setBackground(false);
            settings.toolStopped();
            miner.changed();
            return null;
        });
    }

    private static void teracoin(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String owner,
                                 final String name, final MachineCalls.IServiceFunction<TeracoinService> function,
                                 final String... parameters) {
        MachineCalls.bind(bindings, MachineServices::teracoin, owner, name, function, parameters);
    }

    private static void miner(final Map<MemberId, MachineCalls.Binding<?>> bindings, final String name,
                              final MachineCalls.IServiceFunction<MinerConsole> function,
                              final String... parameters) {
        MachineCalls.bind(bindings, services -> {
            final TeracoinService teracoin = services.teracoin();
            return teracoin == null ? null : teracoin.miner();
        }, MINER, name, function, parameters);
    }

    /* An address as a program writes it, in any letters, or the program stopped when it is not one. */
    private static String address(final Object written, final int line) {
        final String address = String.valueOf(written).trim().toLowerCase(Locale.ROOT);
        if (!TeracoinAddress.valid(address)) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, NOT_AN_ADDRESS.with(written), Halt.Thrown.ARGUMENT);
        }
        return address;
    }

    /* The pool on {@code host} as a program holds it, a picture taken when it was asked for. */
    private static Values.Obj poolOf(final ServerServices.Host host, final TeracoinService teracoin) {
        final MiningPool pool = MiningPools.state(host);
        final long now = teracoin.now();
        final Values.Obj made = new Values.Obj("Pool");
        made.set("Name", MiningPools.name(host));
        made.set("Host", host.hostname());
        made.set("FeePermille", pool.feePermille());
        made.set("Hashrate", pool.hashrate(now));
        made.set("Members", pool.members(now).size());
        made.set("Found", pool.foundCount());
        return made;
    }
}
