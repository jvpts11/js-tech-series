/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.system;

/** The Java that answers a call needing nothing but what the call hands over. */
@FunctionalInterface
public interface IPureFunction {

    /**
     * Answers one call, halting the program the way the runtime halts one when the call is wrong.
     *
     * @param context   the calling program's heap, for anything the call makes
     * @param target    what the call is made on, or null for a call on the type
     * @param arguments what the call hands over, in order; a place the call fills in holds nothing on the way in and
     *                  what the function put there on the way out, and a place it leaves empty is given what a
     *                  variable of that type starts with
     * @param line      the line of the listing making the call, for what the program is told when it goes wrong
     * @return the answer, or null for a call that gives nothing back
     */
    Object call(IPureContext context, Object target, Object[] arguments, int line);
}
