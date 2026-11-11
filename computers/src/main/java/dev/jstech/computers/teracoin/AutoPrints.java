/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A computer's Auto prints (computers/docs/design/teracoin.md, "The 3D Printer"): for each of its wallets, at most one
 * item, the 3D printer it prints on and how much the wallet keeps. When the wallet's balance passes what it keeps by
 * the price of one, one is printed, and so on, while the computer is on with the wallet's file in it. Pure.
 */
public final class AutoPrints {

    private final List<Auto> autos = new ArrayList<>();
    /* Ticks since it last looked, so it looks once a second, from the computer's own tick. */
    private int sinceLook;

    /** The most wallets of one computer that print by themselves. */
    public static final int MOST = 8;
    private static final int LOOK_EVERY = 20;

    /**
     * One wallet's Auto print.
     *
     * @param wallet      the path of the wallet's file
     * @param item        the item it prints, by its id
     * @param printer     where the printer stands
     * @param keep        what the wallet keeps, in units
     * @param lastDayTime the world's time it last printed, 0 before it ever did
     * @param lastCount   how many it last printed
     * @param lastBlock   the chain's height then
     */
    public record Auto(String wallet, String item, long printer, long keep, long lastDayTime, int lastCount,
                       int lastBlock) {

        /** The same, having just printed {@code count} at {@code dayTime}, the chain at {@code block}. */
        public Auto printed(final long dayTime, final int count, final int block) {
            return new Auto(wallet, item, printer, keep, dayTime, count, block);
        }
    }

    /** Every Auto print, in the order they were set. */
    public List<Auto> all() {
        return List.copyOf(autos);
    }

    /** The Auto print of the wallet at {@code wallet}, if it has one. */
    public Optional<Auto> of(final String wallet) {
        return autos.stream().filter(auto -> auto.wallet().equals(wallet)).findFirst();
    }

    /** Sets the Auto print of its wallet, in place of the one it had; whether there was room for it. */
    public boolean set(final Auto auto) {
        final int at = indexOf(auto.wallet());
        if (at >= 0) {
            autos.set(at, auto);
            return true;
        }
        if (autos.size() >= MOST) {
            return false;
        }
        autos.add(auto);
        return true;
    }

    /** Turns off the Auto print of the wallet at {@code wallet}. */
    public void clear(final String wallet) {
        final int at = indexOf(wallet);
        if (at >= 0) {
            autos.remove(at);
        }
    }

    /** Counts a tick of the computer; true once a second, when it looks at its wallets. */
    public boolean secondDue() {
        if (++sinceLook < LOOK_EVERY) {
            return false;
        }
        sinceLook = 0;
        return true;
    }

    /** Puts back what was saved, in place of what it holds. */
    public void restore(final List<Auto> saved) {
        autos.clear();
        for (final Auto auto : saved) {
            if (autos.size() < MOST) {
                autos.add(auto);
            }
        }
    }

    /**
     * How many to print now, from a wallet holding {@code balance}, keeping {@code keep}, at {@code price} each, with
     * {@code queued} copies of it already waiting unpaid in the printer: as many as the balance pays past what it keeps
     * and what waits, at most {@code most}.
     */
    public static int toPrint(final long balance, final long keep, final long price, final int queued,
                              final int most) {
        if (price <= 0L) {
            return 0;
        }
        final long spare = balance - keep - price * queued;
        return spare < price ? 0 : (int) Math.min(most, spare / price);
    }

    private int indexOf(final String wallet) {
        for (int i = 0; i < autos.size(); i++) {
            if (autos.get(i).wallet().equals(wallet)) {
                return i;
            }
        }
        return -1;
    }
}
