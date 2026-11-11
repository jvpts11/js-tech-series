/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

/**
 * A miner that hashed for the chain this second: the program on a computer or an ASIC. Pure.
 *
 * @param machine  the machine it runs on, as the world knows it, unique among the miners
 * @param name     the machine's name, as a pool and a panel show it
 * @param kind     what kind of machine it is, as {@link MinerKinds} writes it
 * @param address  the address it pays
 * @param hashrate its hashes a second
 * @param pool     the pool it mines for, or empty for a miner alone
 */
public record MinerOffer(String machine, String name, String kind, String address, long hashrate, String pool) {

    /** Whether it mines alone. */
    public boolean alone() {
        return pool.isEmpty();
    }
}
