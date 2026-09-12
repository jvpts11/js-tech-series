/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import java.util.List;

/**
 * The peripherals a Lua program on one of our machines can reach.
 *
 * <p>A program written for a ComputerCraft computer expects a {@code peripheral} table, and on one of
 * our machines what is on the other side of a Gateway is exactly that: the things on the ComputerCraft
 * network the Gateway sits on. So the same program, unchanged, finds the same peripherals whichever
 * kind of computer it happens to be running on.
 *
 * <p>A machine with no Gateway has none, which is a fair answer and not an error: it is what a
 * ComputerCraft computer with nothing attached says too.
 */
public interface ILuaPeripherals {

    /** A machine with nothing of the sort within reach. */
    ILuaPeripherals NONE = new ILuaPeripherals() {
    };

    /** What is within reach, by the names that side knows them by. */
    default List<String> names() {
        return List.of();
    }

    /** What that one is, or null when there is no such thing within reach. */
    default String typeOf(final String name) {
        return null;
    }

    /** What that one answers to; empty when there is no such thing. */
    default List<String> methodsOf(final String name) {
        return List.of();
    }

    /**
     * Calls one of those methods and gives back what it said.
     *
     * @throws dev.jstech.computers.cannon.run.Halt when there is no such peripheral or method, or the
     *                                              call was refused
     */
    default Object call(final String name, final String method, final List<Object> arguments) {
        throw new dev.jstech.computers.cannon.run.Halt(
                dev.jstech.computers.cannon.run.Halt.Reason.NO_SUCH_MEMBER, 0, "No peripheral attached");
    }
}
