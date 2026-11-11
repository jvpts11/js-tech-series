/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.core.id.IStableId;

/**
 * What a computer's miner is set to and how it stands (computers/docs/design/teracoin.md, "Mining"): the address it
 * pays, the pool it chose or that it mines alone, and whether it runs in the background. The miner also runs while its
 * program is open in front of a terminal; that is not saved, the tool that keeps it going is. Pure.
 */
public final class MinerSettings {

    private String address = "";
    private String pool = "";
    private boolean alone;
    private boolean background;
    /* Until when the miner in front of a terminal keeps it going; nothing saved. */
    private long toolUntil = -1L;
    /* What it last did, for the status and the tool. */
    private Status status = Status.OFF;
    private String poolShown = "";
    private String poolHost = "";
    private int feePermille;
    private long hashrate;
    private int sinceOffer;

    /** How long the tool keeps the miner going after its last tick. */
    public static final long TOOL_HOLD = 40L;
    /* The ticks between two offers to the chain: a second. */
    private static final int OFFER_EVERY = 20;

    public String address() {
        return address;
    }

    /** The pool chosen, by its name or its server's; empty for the network's first. */
    public String pool() {
        return pool;
    }

    /** Whether it mines alone, whatever pools the network has. */
    public boolean alone() {
        return alone;
    }

    /** Whether it runs in the background, the computer on. */
    public boolean background() {
        return background;
    }

    public void setAddress(final String chosen) {
        address = chosen.trim();
    }

    /** Mines for the pool {@code chosen}, or the network's first when it is empty. */
    public void setPool(final String chosen) {
        pool = chosen.trim();
        alone = false;
    }

    /** Mines alone. */
    public void setAlone() {
        pool = "";
        alone = true;
    }

    public void setBackground(final boolean on) {
        background = on;
    }

    /** The tool in front of a terminal ran at {@code now}: the miner keeps going a little after. */
    public void toolRan(final long now) {
        toolUntil = now + TOOL_HOLD;
    }

    /** The tool stopped. */
    public void toolStopped() {
        toolUntil = -1L;
    }

    /** Whether it runs at {@code now}: in the background, or in front of a terminal. */
    public boolean running(final long now) {
        return background || now <= toolUntil;
    }

    /** Counts a tick of the miner running; true once a second, when it offers its work. */
    public boolean secondDue() {
        sinceOffer++;
        if (sinceOffer < OFFER_EVERY) {
            return false;
        }
        sinceOffer = 0;
        return true;
    }

    /** What it did this second: mining, alone or for {@code poolName} on {@code host} at {@code fee}. */
    public void mined(final long hashes, final String poolName, final String host, final int fee) {
        status = poolName.isEmpty() ? Status.ALONE : Status.POOL;
        hashrate = hashes;
        poolShown = poolName;
        poolHost = host;
        feePermille = fee;
    }

    /** It could not mine this second, for {@code why}. */
    public void stalled(final Status why, final String poolName) {
        status = why;
        hashrate = 0L;
        poolShown = poolName;
        poolHost = "";
    }

    public Status status() {
        return status;
    }

    public String poolShown() {
        return poolShown;
    }

    public String poolHost() {
        return poolHost;
    }

    public int feePermille() {
        return feePermille;
    }

    public long hashrate() {
        return hashrate;
    }

    /** The settings as they are saved: address, pool, alone and background. */
    public Saved saved() {
        return new Saved(address, pool, alone, background);
    }

    public void restore(final Saved saved) {
        address = saved.address();
        pool = saved.pool();
        alone = saved.alone();
        background = saved.background();
        toolUntil = -1L;
        status = Status.OFF;
    }

    /** How the miner stands; an ASIC sends it to its window by its number. */
    public enum Status implements IStableId {
        /** It is not running. */
        OFF(0),
        /** It mines alone. */
        ALONE(1),
        /** It mines for a pool. */
        POOL(2),
        /** It has no address to pay. */
        NO_ADDRESS(3),
        /** The pool it chose is not on the network. */
        NO_POOL(4),
        /** The pool takes only the miners its owner accepts; this one asked and waits. */
        WAITING_FOR_OWNER(5),
        /** Its machine has nothing that hashes. */
        NOTHING_HASHES(6);

        private final int id;

        Status(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }
    }

    /** The settings as they are saved. */
    public record Saved(String address, String pool, boolean alone, boolean background) {
    }
}
