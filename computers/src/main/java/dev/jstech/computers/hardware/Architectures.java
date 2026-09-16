/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.JsComputers;
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
 * <p>x86-64 runs x86 as the real one does, so a program built for the older chips keeps running on the newer
 * ones and not the other way about. The 16-bit x86 of the earliest machines runs only its own, which is what
 * makes a Vintage computer a machine of its time rather than a slower modern one.
 *
 * <p>Architectures are added while the game is setting up and only read afterwards; when the mod grows a proper
 * registration event, in the API step, this is what moves behind it.
 */
public final class Architectures {

    /** The 16-bit x86 of the first machines: its own programs only, since the language needs a later era. */
    public static final ArchitectureSpec X86_16 = own("x86_16", "x86-16", 16);

    /** The 32-bit x86, where the language starts. It is a new instruction set, not a wider version of the first. */
    public static final ArchitectureSpec X86 = own("x86", "x86", 32);

    /** The 64-bit x86, which runs what was built for the 32-bit one as well as its own. */
    public static final ArchitectureSpec X86_64 = own("x86_64", "x86-64", 64, X86);

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
