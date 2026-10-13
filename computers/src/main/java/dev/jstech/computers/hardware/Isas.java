/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.vm.listing.Opcode;
import dev.jstech.core.tier.HardwareEra;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The instruction set architectures (ISAs) this mod brings, and the place a mod adds one of its own.
 *
 * <p>Each of the x86 chips runs what was built for the ones before it, as the real ones did, and none of them
 * runs what was built for the ones after. So a program written for the oldest machine of the line keeps working
 * on every machine that came later, and a Vintage computer stays a machine of its time: it runs the programs of
 * its own age and nothing newer, however slowly a newer one would have run them.
 *
 * <p>ISAs are added while the game is setting up and only read afterwards; when the mod grows a proper
 * registration event, in the API step, this is what moves behind it.
 */
public final class Isas {

    /** IA-16, the 16-bit x86 of the first machines, and the only thing they run. */
    public static final IsaSpec IA_16 = own("ia_16", "IA-16", 16);

    /**
     * The 32-bit x86, which runs what was built for the 16-bit one as well as its own.
     *
     * <p>The real 386 ran what an 8086 ran, and that is the whole character of this family: a program built for
     * the oldest machine of the line keeps working on every machine after it, and never the other way about. It
     * is also what lets one program serve all three ages, since the smaller language builds for the oldest.
     */
    public static final IsaSpec X86 = own("x86", "x86", 32, IA_16);

    /** The 64-bit x86, which runs what was built for the 32-bit one as well as its own. */
    public static final IsaSpec X86_64 = own("x86_64", "x86-64", 64, X86);

    /**
     * The instructions an ISA brought that the ones before it did not have, by the ISA that brought them.
     *
     * <p>Empty, and meant to be: the 32-bit and the 64-bit x86 have exactly the same instructions today, and
     * whatever a later cycle adds goes into the 64-bit one alone, the way the real extensions did. An entry here
     * is what such a cycle writes, and until one does there is nothing a program can be written with that the
     * oldest of the two lacks.
     */
    private static final Map<Opcode, String> ADDED_IN = Map.of();

    private static final Map<String, IsaSpec> KNOWN = new LinkedHashMap<>();

    static {
        add(IA_16);
        add(X86);
        add(X86_64);
    }

    private Isas() {
    }

    /** Adds an ISA. An id that is already taken is refused rather than quietly replacing the other. */
    public static void add(final IsaSpec isa) {
        final IsaSpec taken = KNOWN.putIfAbsent(isa.id(), isa);
        if (taken != null && !taken.equals(isa)) {
            throw new IllegalStateException("the ISA id '" + isa.id() + "' is already taken");
        }
    }

    /** The ISA of that id, or nothing when no mod has brought one under it. */
    public static Optional<IsaSpec> byId(final String id) {
        return Optional.ofNullable(KNOWN.get(id));
    }

    /** Every ISA there is, oldest of the series first, then whatever a mod added after. */
    public static List<IsaSpec> all() {
        return List.copyOf(KNOWN.values());
    }

    /**
     * The ISA a person meant, by its id or by the name it is written under.
     *
     * <p>A player types x86-64 rather than jsc:x86_64, and both are the same thing said twice, so both are taken.
     */
    public static Optional<IsaSpec> find(final String idOrName) {
        final Optional<IsaSpec> byId = byId(idOrName);
        if (byId.isPresent()) {
            return byId;
        }
        for (final IsaSpec one : KNOWN.values()) {
            if (one.name().equalsIgnoreCase(idOrName)) {
                return Optional.of(one);
            }
        }
        return Optional.empty();
    }

    /** Whether {@code isa} has that instruction: it brought it, or something it runs did. */
    public static boolean has(final IsaSpec isa, final Opcode opcode) {
        return has(isa, opcode, ADDED_IN);
    }

    private static boolean has(final IsaSpec isa, final Opcode opcode, final Map<Opcode, String> addedIn) {
        final String brought = addedIn.get(opcode);
        return brought == null || isa.runs(brought);
    }

    /**
     * The oldest ISA that has every one of those instructions, starting from {@code baseline}.
     *
     * <p>A program should run on the oldest machine it could have run on, so this only ever moves up, and only
     * because the program reached for something the machine below did not have. What counts as a candidate is
     * what runs the baseline's programs: a newer chip of the same line will take them, and a chip of another
     * line is another machine entirely, however many bits it has.
     *
     * <p>Today it always answers the baseline, because nothing has been added to a later ISA yet. That is the
     * right answer rather than a missing one, and the day something is added this is where it is felt.
     */
    public static IsaSpec oldestWith(final IsaSpec baseline, final Set<Opcode> used) {
        return oldestWith(baseline, used, ADDED_IN);
    }

    /*
     * The same, against a table handed in. The one above reads the table this class keeps, which is empty until
     * a later cycle writes in it; this is how the choosing itself is put to the question in the meantime, with a
     * table that says something, rather than being taken on trust until the day it first matters.
     */
    static IsaSpec oldestWith(final IsaSpec baseline, final Set<Opcode> used, final Map<Opcode, String> addedIn) {
        IsaSpec chosen = null;
        for (final IsaSpec candidate : KNOWN.values()) {
            if (!candidate.runs(baseline) || !hasAll(candidate, used, addedIn)) {
                continue;
            }
            if (chosen == null || candidate.bits() < chosen.bits()) {
                chosen = candidate;
            }
        }
        return chosen == null ? baseline : chosen;
    }

    private static boolean hasAll(final IsaSpec isa, final Set<Opcode> used, final Map<Opcode, String> addedIn) {
        for (final Opcode opcode : used) {
            if (!has(isa, opcode, addedIn)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The ISA this mod's own processors of that era are built on.
     *
     * <p>The eras from the Transition on are all x86-64 until an era brings an ISA of its own; every era is named
     * here rather than left to a default, so that a new one cannot be added without this question being answered.
     */
    public static IsaSpec of(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> IA_16;
            case LEGACY -> X86;
            case TRANSITION, STANDARD, ADVANCED, EXA, SINGULARITY -> X86_64;
        };
    }

    /**
     * The oldest machine a program of that language starts out built for, before what it turned out to use is
     * allowed to push it up.
     *
     * <p>The smaller language exists for the oldest machines of all, so its programs begin there and run on
     * everything after. The full one begins where its own library does, on the 32-bit machines.
     */
    public static IsaSpec oldestFor(final LanguageLevel level) {
        return level.full() ? X86 : IA_16;
    }

    /*
     * One of this mod's own ISAs, which runs its own programs and everything the ISAs it succeeds run. The mod id
     * is a constant the compiler writes in here as the text itself, so naming it does not drag the mod class, and
     * Minecraft with it, into hardware that is tested without the game.
     */
    private static IsaSpec own(final String path, final String name, final int bits, final IsaSpec... succeeds) {
        final String id = HardwareIds.own(path);
        final Set<String> runs = new HashSet<>();
        runs.add(id);
        for (final IsaSpec older : succeeds) {
            runs.addAll(older.runs());
        }
        return new IsaSpec(id, name, bits, runs);
    }
}
