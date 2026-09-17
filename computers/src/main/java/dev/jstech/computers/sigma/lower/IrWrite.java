/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lower;

import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.sem.ITypeSymbol;

/**
 * A value going into a place, with every question about which value and which answer already settled.
 *
 * <p>Three shapes a player writes come to this one: {@code x = y}, {@code x += y} and {@code x++}. They differ
 * in what is put there and in which value the line is worth if anybody wants one, and in nothing else. Said
 * here once, whoever writes the assembly is left with a single thing to write instead of three that had each
 * worked the place out for themselves.
 *
 * <p>One more or one less is not a shape of its own: it is a combining assignment whose value is the number
 * one, written as a number of the kind the place holds so that nothing has to be converted on the way.
 *
 * @param place    where the value goes
 * @param operator {@code ASSIGN} to replace what is there, or what to combine the old value with
 * @param value    what is combined in, or what replaces it
 * @param type     what the place holds, which decides how the combining is done
 * @param answer   which value the line is worth, where anything wants one
 */
public record IrWrite(Place place, Operator operator, IExpr value, ITypeSymbol type, Answer answer) {

    /** Which value a write is worth, for whoever is using it as a value and not only for what it does. */
    public enum Answer {
        /** What was put there: what an assignment is worth. */
        WRITTEN,
        /** What was there before the change: what {@code x++} is worth. */
        BEFORE,
        /** What is there after it: what {@code ++x} is worth. */
        AFTER
    }
}
