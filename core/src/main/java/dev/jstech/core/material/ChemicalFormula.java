/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

/**
 * A chemical formula read as chemists write it: {@code CuFeS2}, {@code Cu2CO3(OH)2}, {@code CuSO4·5H2O}, groups in
 * round or square brackets, a hydrate's water after a dot, subscripts written as plain digits or as ₂. It knows how
 * many atoms of each element it holds, and with each element's atomic weight, its molar mass and the share of its
 * mass each element makes up: what a mineral is made of, from its formula alone.
 *
 * <pre>{@code
 * ChemicalFormula.parse("CuFeS2").massFraction("Cu", weights)   // 0.346
 * ChemicalFormula.parse("Cu2CO3(OH)2").subscripted()            // Cu₂CO₃(OH)₂
 * }</pre>
 *
 * <p>Pure: no game types.
 */
public final class ChemicalFormula {

    private final String written;
    private final Map<String, Integer> atoms;

    /* The dots a hydrate's water is written after. */
    private static final String HYDRATE_DOTS = "·.*•";
    private static final char FIRST_SUBSCRIPT = '₀';
    private static final char LAST_SUBSCRIPT = '₉';

    private ChemicalFormula(final String written, final Map<String, Integer> atoms) {
        this.written = written;
        this.atoms = Collections.unmodifiableMap(atoms);
    }

    /**
     * Reads {@code written}.
     *
     * @throws IllegalArgumentException when it is no formula: empty, a bracket left open, an element that is no
     *                                  symbol, a count that is not a whole number
     */
    public static ChemicalFormula parse(final String written) {
        Objects.requireNonNull(written, "written");
        final String plain = plainDigits(written.strip());
        if (plain.isEmpty()) {
            throw new IllegalArgumentException("a formula holds at least one element");
        }
        final Map<String, Integer> atoms = new LinkedHashMap<>();
        for (final String part : split(plain)) {
            final Reader reader = new Reader(part);
            final int times = reader.number(1);
            final Map<String, Integer> group = reader.group(')');
            if (!reader.done()) {
                throw new IllegalArgumentException("the formula " + written + " has " + reader.rest()
                        + " where it should end");
            }
            group.forEach((symbol, count) -> atoms.merge(symbol, Math.multiplyExact(count, times), Math::addExact));
        }
        return new ChemicalFormula(written.strip(), atoms);
    }

    /** {@code written} read, or none when it is no formula. */
    public static Optional<ChemicalFormula> read(final String written) {
        try {
            return Optional.of(parse(written));
        } catch (final IllegalArgumentException | ArithmeticException notOne) {
            return Optional.empty();
        }
    }

    /** The formula as it was written. */
    public String written() {
        return this.written;
    }

    /** How many atoms of each element it holds, by symbol, in the order they first appear. */
    public Map<String, Integer> atoms() {
        return this.atoms;
    }

    /** How many atoms of the element {@code symbol} it holds: 2 of S in CuFeS2. */
    public int count(final String symbol) {
        return this.atoms.getOrDefault(symbol, 0);
    }

    /**
     * Its molar mass, in grams a mole, from each element's atomic weight {@code weightOf} gives.
     *
     * @throws IllegalArgumentException when an element has no weight above none
     */
    public double molarMass(final ToDoubleFunction<String> weightOf) {
        double total = 0.0;
        for (final Map.Entry<String, Integer> atom : this.atoms.entrySet()) {
            total += weight(atom.getKey(), weightOf) * atom.getValue();
        }
        return total;
    }

    /** The share of its mass the element {@code symbol} makes up, from none to the whole. */
    public double massFraction(final String symbol, final ToDoubleFunction<String> weightOf) {
        final int count = count(symbol);
        return count == 0 ? 0.0 : weight(symbol, weightOf) * count / molarMass(weightOf);
    }

    /** The share of its mass each element makes up, in the order they first appear; together, the whole. */
    public Map<String, Double> massFractions(final ToDoubleFunction<String> weightOf) {
        final double total = molarMass(weightOf);
        final Map<String, Double> out = new LinkedHashMap<>();
        this.atoms.forEach((symbol, count) -> out.put(symbol, weight(symbol, weightOf) * count / total));
        return Collections.unmodifiableMap(out);
    }

    /** The formula with its counts written small, as in print: {@code Cu₂CO₃(OH)₂}; a hydrate's water stays large. */
    public String subscripted() {
        final StringBuilder out = new StringBuilder();
        boolean leading = true;
        for (int i = 0; i < this.written.length(); i++) {
            final char c = this.written.charAt(i);
            if (HYDRATE_DOTS.indexOf(c) >= 0) {
                out.append('·');
                leading = true;
            } else if (Character.isDigit(c) && c <= '9' && !leading) {
                out.append((char) (FIRST_SUBSCRIPT + (c - '0')));
            } else {
                out.append(c);
                if (!Character.isDigit(c)) {
                    leading = false;
                }
            }
        }
        return out.toString();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof ChemicalFormula formula && formula.atoms.equals(this.atoms);
    }

    @Override
    public int hashCode() {
        return this.atoms.hashCode();
    }

    @Override
    public String toString() {
        return this.written;
    }

    private static double weight(final String symbol, final ToDoubleFunction<String> weightOf) {
        final double weight = weightOf.applyAsDouble(symbol);
        if (!(weight > 0.0)) {
            throw new IllegalArgumentException("the element " + symbol + " has no atomic weight");
        }
        return weight;
    }

    /* The formula with ₂ written as 2. */
    private static String plainDigits(final String written) {
        final StringBuilder out = new StringBuilder(written.length());
        for (int i = 0; i < written.length(); i++) {
            final char c = written.charAt(i);
            out.append(c >= FIRST_SUBSCRIPT && c <= LAST_SUBSCRIPT ? (char) ('0' + (c - FIRST_SUBSCRIPT)) : c);
        }
        return out.toString();
    }

    /* The parts between a hydrate's dots: CuSO4 and 5H2O. */
    private static String[] split(final String plain) {
        final StringBuilder pattern = new StringBuilder("[");
        for (int i = 0; i < HYDRATE_DOTS.length(); i++) {
            pattern.append('\\').append(HYDRATE_DOTS.charAt(i));
        }
        final String[] parts = plain.split(pattern.append(']').toString(), -1);
        for (final String part : parts) {
            if (part.isBlank()) {
                throw new IllegalArgumentException("the formula " + plain + " has nothing beside a dot");
            }
        }
        return parts;
    }

    /* Reads one part of a formula, a character at a time. */
    private static final class Reader {

        private final String text;
        private int at;

        Reader(final String text) {
            this.text = text.strip();
        }

        boolean done() {
            return this.at >= this.text.length();
        }

        String rest() {
            return this.text.substring(this.at);
        }

        /* Elements and bracketed groups up to {@code closing} or the end, each with its count. */
        Map<String, Integer> group(final char closing) {
            final Map<String, Integer> out = new LinkedHashMap<>();
            while (!done() && this.text.charAt(this.at) != closing && this.text.charAt(this.at) != ']') {
                final char c = this.text.charAt(this.at);
                final Map<String, Integer> piece;
                if (c == '(' || c == '[') {
                    this.at++;
                    final char close = c == '(' ? ')' : ']';
                    piece = group(close);
                    if (done() || this.text.charAt(this.at) != close) {
                        throw new IllegalArgumentException("the formula " + this.text + " leaves a bracket open");
                    }
                    this.at++;
                } else if (Character.isUpperCase(c)) {
                    final int start = this.at++;
                    while (!done() && Character.isLowerCase(this.text.charAt(this.at))) {
                        this.at++;
                    }
                    piece = Map.of(this.text.substring(start, this.at), 1);
                } else {
                    throw new IllegalArgumentException("the formula " + this.text + " has " + c
                            + " where an element or a bracket should be");
                }
                final int times = number(1);
                piece.forEach((symbol, count) -> out.merge(symbol, Math.multiplyExact(count, times),
                        Math::addExact));
            }
            if (out.isEmpty()) {
                throw new IllegalArgumentException("the formula " + this.text + " has an empty group");
            }
            return out;
        }

        /* The whole number written here, or {@code otherwise} when none is. */
        int number(final int otherwise) {
            final int start = this.at;
            while (!done() && Character.isDigit(this.text.charAt(this.at)) && this.text.charAt(this.at) <= '9') {
                this.at++;
            }
            if (start == this.at) {
                return otherwise;
            }
            final int value = Integer.parseInt(this.text.substring(start, this.at));
            if (value <= 0) {
                throw new IllegalArgumentException("the formula " + this.text + " counts no atoms of something");
            }
            return value;
        }
    }
}
