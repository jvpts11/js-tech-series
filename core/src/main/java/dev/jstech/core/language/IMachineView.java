/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.language;

/**
 * A program's view of the machine it runs on, as a language that runs its own files is given it.
 *
 * <p>Each program gets a view of its own when it starts and again when it comes back. Through it the program reads the
 * machine's clock, writes the lines a person reads, and learns how much memory it may hold. The machine keeps what the
 * program writes, so a terminal, a task manager or another program reads a program's output the same way whatever
 * language it is in.
 */
public interface IMachineView {

    /** The game tick the machine's world is on, or zero while the machine is in no world. */
    long tick();

    /** How far into the day the machine's world is, in ticks from 0 to 23,999. */
    long dayTime();

    /** How many whole days the machine's world has seen. */
    long day();

    /** Writes a line for a person to read; the machine keeps it within the limits it holds any program's output to. */
    void print(String line);

    /** How many bytes the program may hold at once. */
    long memoryQuota();
}
