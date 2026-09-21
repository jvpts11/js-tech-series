/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.listing;

import java.util.ArrayList;
import java.util.List;

/**
 * A whole compiled program: the types it holds and the class the runtime starts from.
 *
 * <p>The version at the head is the format's, not the program's. A runtime refuses a listing whose
 * major version is above the one it knows, because a listing from a later version may use
 * instructions it has never heard of, and guessing at those would be worse than saying so. It refuses
 * one too far below as well: the format changed since, and reading such a listing as today's would
 * run it wrongly without a word, where compiling its source again costs nothing. Between those two
 * lie the versions that still read correctly, and a listing of one of them runs untouched.
 *
 * <p>A program also names the architecture it was built for. The name is carried, not judged: which
 * machines will run it is a question about hardware, and the answer is worked out where the hardware
 * is.
 */
public final class AsmProgram {

    /** The version of the format this build writes; 3 names the architecture a program was built for. */
    public static final int VERSION = 3;

    /** The oldest version this build still reads correctly. Version 2 names every constructor {@code .ctor}. */
    public static final int OLDEST_VERSION = 2;

    /**
     * What a listing that names no architecture was built for. Every listing written before the name existed was
     * compiled for the 32-bit machines, which is where the language starts, so that is what one without a name is.
     */
    public static final String DEFAULT_ARCHITECTURE = "jsc:x86";

    private final int version;
    private final List<AsmType> types = new ArrayList<>();
    private String architecture = DEFAULT_ARCHITECTURE;
    private int architectureLine = 1;
    private String entryPoint;
    private Shape shape = Shape.SCRIPT;

    public AsmProgram() {
        this(VERSION);
    }

    public AsmProgram(final int version) {
        this.version = version;
    }

    /** The format version this listing was written in. */
    public int version() {
        return this.version;
    }

    /** The architecture this program was built for. */
    public String architecture() {
        return this.architecture;
    }

    /**
     * The line the architecture was named on, so that a machine refusing the program can point at it. A listing
     * that names none answers the head of the listing, which is the line that decides it.
     */
    public int architectureLine() {
        return this.architectureLine;
    }

    /** The types, in the order they were written. */
    public List<AsmType> types() {
        return List.copyOf(this.types);
    }

    /** The type of that name, or null. */
    public AsmType type(final String name) {
        for (final AsmType type : this.types) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    /** The class the runtime starts the program from, or null for a library. */
    public String entryPoint() {
        return this.entryPoint;
    }

    /** Whether this is a program that runs at a terminal or one that stays up. */
    public Shape shape() {
        return this.shape;
    }

    /**
     * Names the architecture this program was built for.
     *
     * @param namedOnLine the line it was named on when the name was read from text, and 1 for a program being built
     */
    public void setArchitecture(final String architecture, final int namedOnLine) {
        this.architecture = architecture;
        this.architectureLine = Math.max(1, namedOnLine);
    }

    /** Names the class the runtime starts from, and says which kind of program it is. */
    public void setEntryPoint(final String entryPoint, final Shape shape) {
        this.entryPoint = entryPoint;
        this.shape = shape;
    }

    /** Adds a type. */
    public void addType(final AsmType type) {
        this.types.add(type);
    }
}
