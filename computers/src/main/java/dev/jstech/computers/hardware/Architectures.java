/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.vm.listing.Opcode;
import dev.jstech.core.tier.HardwareEra;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The architectures this mod brings, and the place a mod adds one of its own.
 *
 * <p>Each of the x86 chips runs what was built for the ones before it, as the real ones did, and none of them
 * runs what was built for the ones after. So a program written for the oldest machine of the line keeps working
 * on every machine that came later, and a Vintage computer stays a machine of its time: it runs the programs of
 * its own age and nothing newer, however slowly a newer one would have run them.
 *
 * <p>Architectures are added while the game is setting up and only read afterwards; when the mod grows a proper
 * registration event, in the API step, this is what moves behind it.
 */
public final class Architectures {

    /** The 16-bit x86 of the first machines, and the only thing they run. */
    public static final ArchitectureSpec X86_16 = own("x86_16", "x86-16", 16);

    /**
     * The 32-bit x86, which runs what was built for the 16-bit one as well as its own.
     *
     * <p>The real 386 ran what an 8086 ran, and that is the whole character of this family: a program built for
     * the oldest machine of the line keeps working on every machine after it, and never the other way about. It
     * is also what lets one program serve all three ages, since the smaller language builds for the oldest.
     */
    public static final ArchitectureSpec X86 = own("x86", "x86", 32, X86_16);

    /** The 64-bit x86, which runs what was built for the 32-bit one as well as its own. */
    public static final ArchitectureSpec X86_64 = own("x86_64", "x86-64", 64, X86);

    /**
     * The instructions an architecture brought that the ones before it did not have, by the architecture that
     * brought them.
     *
     * <p>Empty, and meant to be: the 32-bit and the 64-bit x86 have exactly the same instruction set today, and
     * whatever a later cycle adds goes into the 64-bit one alone, the way the real extensions did. An entry here
     * is what such a cycle writes, and until one does there is nothing a program can be written with that the
     * oldest of the two lacks.
     */
    private static final Map<Opcode, String> ADDED_IN = Map.of();

    private static final Map<String, ArchitectureSpec> KNOWN = new LinkedHashMap<>();

    static {
        add(X86_16);
        add(X86);
        add(X86_64);
    }

    private Architectures() {
    }

    /** Adds an architecture. An id that is already taken is refused rather than quietly replacing the other. */
    public static void add(final ArchitectureSpec architecture) {
        final ArchitectureSpec taken = KNOWN.putIfAbsent(architecture.id(), architecture);
        if (taken != null && !taken.equals(architecture)) {
            throw new IllegalStateException("the architecture id '" + architecture.id() + "' is already taken");
        }
    }

    /** The architecture of that id, or nothing when no mod has brought one under it. */
    public static Optional<ArchitectureSpec> byId(final String id) {
        return Optional.ofNullable(KNOWN.get(id));
    }

    /** Every architecture there is, oldest of the series first, then whatever a mod added after. */
    public static List<ArchitectureSpec> all() {
        return List.copyOf(KNOWN.values());
    }

    /**
     * The architecture a person meant, by its id or by the name it is written under.
     *
     * <p>A player types x86-64 rather than jsc:x86_64, and both are the same thing said twice, so both are taken.
     */
    public static Optional<ArchitectureSpec> find(final String idOrName) {
        final Optional<ArchitectureSpec> byId = byId(idOrName);
        if (byId.isPresent()) {
            return byId;
        }
        for (final ArchitectureSpec one : KNOWN.values()) {
            if (one.name().equalsIgnoreCase(idOrName)) {
                return Optional.of(one);
            }
        }
        return Optional.empty();
    }

    /** Whether {@code architecture} has that instruction: it brought it, or something it runs did. */
    public static boolean has(final ArchitectureSpec architecture, final Opcode opcode) {
        return has(architecture, opcode, ADDED_IN);
    }

    private static boolean has(final ArchitectureSpec architecture, final Opcode opcode,
                               final Map<Opcode, String> addedIn) {
        final String brought = addedIn.get(opcode);
        return brought == null || architecture.runs(brought);
    }

    /**
     * The oldest architecture that has every one of those instructions, starting from {@code baseline}.
     *
     * <p>A program should run on the oldest machine it could have run on, so this only ever moves up, and only
     * because the program reached for something the machine below did not have. What counts as a candidate is
     * what runs the baseline's programs: a newer chip of the same line will take them, and a chip of another
     * line is another machine entirely, however many bits it has.
     *
     * <p>Today it always answers the baseline, because nothing has been added to a later architecture yet. That
     * is the right answer rather than a missing one, and the day something is added this is where it is felt.
     */
    public static ArchitectureSpec oldestWith(final ArchitectureSpec baseline, final Set<Opcode> used) {
        return oldestWith(baseline, used, ADDED_IN);
    }

    /*
     * The same, against a table handed in. The one above reads the table this class keeps, which is empty until
     * a later cycle writes in it; this is how the choosing itself is put to the question in the meantime, with a
     * table that says something, rather than being taken on trust until the day it first matters.
     */
    static ArchitectureSpec oldestWith(final ArchitectureSpec baseline, final Set<Opcode> used,
                                       final Map<Opcode, String> addedIn) {
        ArchitectureSpec chosen = null;
        for (final ArchitectureSpec candidate : KNOWN.values()) {
            if (!candidate.runs(baseline) || !hasAll(candidate, used, addedIn)) {
                continue;
            }
            if (chosen == null || candidate.bits() < chosen.bits()) {
                chosen = candidate;
            }
        }
        return chosen == null ? baseline : chosen;
    }

    private static boolean hasAll(final ArchitectureSpec architecture, final Set<Opcode> used,
                                  final Map<Opcode, String> addedIn) {
        for (final Opcode opcode : used) {
            if (!has(architecture, opcode, addedIn)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The architecture this mod's own processors of that era are built on.
     *
     * <p>The eras beyond Standard have no hardware of their own yet, so their chips are x86-64 until it arrives;
     * every era is named here rather than left to a default, so that a new one cannot be added without this
     * question being answered.
     */
    public static ArchitectureSpec of(final HardwareEra era) {
        return switch (era) {
            case VINTAGE -> X86_16;
            case LEGACY -> X86;
            case STANDARD, ADVANCED, EXA, SINGULARITY -> X86_64;
        };
    }

    /*
     * One of this mod's own architectures, which runs its own programs and everything the architectures it
     * succeeds run. The mod id is a constant the compiler writes in here as the text itself, so naming it does not
     * drag the mod class, and Minecraft with it, into hardware that is tested without the game.
     */
    private static ArchitectureSpec own(final String path, final String name, final int bits,
                                        final ArchitectureSpec... succeeds) {
        final String id = JsComputers.MODID + ":" + path;
        final Set<String> runs = new HashSet<>();
        runs.add(id);
        for (final ArchitectureSpec older : succeeds) {
            runs.addAll(older.runs());
        }
        return new ArchitectureSpec(id, name, bits, runs);
    }
}
