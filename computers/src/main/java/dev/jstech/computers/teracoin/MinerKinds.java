/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

/**
 * What kind of machine a miner is, as a pool keeps it and a panel shows it: a Mining Computer of its era and its cards,
 * an ASIC of its era, or another computer by its kind. Written as a short code so a panel says it in its own language:
 * {@code rig/standard/6}, {@code asic/advanced}, {@code machine/personal_computer}. Pure.
 */
public final class MinerKinds {

    /** A Mining Computer's code. */
    public static final String RIG = "rig";
    /** An ASIC's code. */
    public static final String ASIC = "asic";
    /** Any other computer's code. */
    public static final String MACHINE = "machine";

    private MinerKinds() {
    }

    /** A Mining Computer of {@code era} with {@code cards} cards on its risers. */
    public static String rig(final String era, final int cards) {
        return RIG + "/" + era + "/" + cards;
    }

    /** An ASIC of {@code era}. */
    public static String asic(final String era) {
        return ASIC + "/" + era;
    }

    /** Another computer, by the id of its kind. */
    public static String machine(final String kind) {
        return MACHINE + "/" + kind;
    }

    /** The parts of a code: its kind, then what it says of it. */
    public static String[] parts(final String code) {
        return code.split("/");
    }
}
