/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import java.util.List;

/**
 * Who a program is and where it stands: the number the machine lists it under, the name it gave itself, what it was
 * started with, how many instructions it has run, and how it ended once it has.
 *
 * <p>It holds nothing of the program's own. The objects a program holds itself by are made on its heap by the
 * process, out of what this says.
 */
final class ProgramIdentity {

    private int machineId;
    private String name = "";
    private List<String> args = List.of();
    /** A long, because a program that stays up runs for as long as the world does. */
    private long spent;
    private boolean exited;
    private int exitCode;
    private boolean halted;
    private String message;

    /** The number the machine lists the program under, or 0 off any machine. */
    int machineId() {
        return this.machineId;
    }

    /** Takes the number the machine lists the program under. */
    void identify(final int id) {
        this.machineId = id;
    }

    /** The name the program gave itself, or empty when it gave none. */
    String name() {
        return this.name;
    }

    /** Names the program; a blank name is no name. */
    void rename(final String value) {
        this.name = value == null ? "" : value.strip();
    }

    /** What the program was started with. */
    List<String> args() {
        return this.args;
    }

    /** Takes what the program was started with. */
    void startWith(final List<String> arguments) {
        this.args = arguments == null ? List.of() : List.copyOf(arguments);
    }

    /** How many instructions it has run since it started. */
    long spent() {
        return this.spent;
    }

    /** Counts instructions it has run. */
    void spend(final int instructions) {
        this.spent += instructions;
    }

    /** Whether the program ended itself with {@code Program.Exit}. */
    boolean exited() {
        return this.exited;
    }

    /** How the program ended: what it said with {@code Program.Exit}, one for a halt, zero otherwise. */
    int exitCode() {
        return this.halted ? 1 : this.exitCode;
    }

    /** What the program said with {@code Program.Exit}, whatever else happened to it. */
    int givenExitCode() {
        return this.exitCode;
    }

    /** Takes the end the program gave itself. */
    void exit(final int code) {
        this.exited = true;
        this.exitCode = code;
    }

    /** Whether the program was halted. */
    boolean halted() {
        return this.halted;
    }

    /** What the program was told when it was halted, or null while it has not been. */
    String message() {
        return this.message;
    }

    /** Takes the halt that ended the program. */
    void halt(final String told) {
        this.halted = true;
        this.message = told;
    }

    /** Whether the program is over, by its own exit or a halt, so nothing more is handed to it. */
    boolean over() {
        return this.halted || this.exited;
    }

    /** Puts back everything but the name, as a snapshot wrote it. */
    void restore(final List<String> arguments, final int id, final long instructions, final boolean ended,
                 final int code, final boolean stopped, final String told) {
        this.startWith(arguments);
        this.machineId = id;
        this.spent = instructions;
        this.exited = ended;
        this.exitCode = code;
        this.halted = stopped;
        this.message = told;
    }
}
