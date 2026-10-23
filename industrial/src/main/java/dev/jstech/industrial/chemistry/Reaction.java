/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.chemistry;

import dev.jstech.core.material.ChemicalFormula;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A real chemical reaction, in its real proportions: each substance with its formula and its coefficient, on the side
 * it goes in or comes out, solid or fluid. The coefficients give the amounts: a fluid, a gas or a liquid, comes in
 * {@value #MILLIBUCKETS} mB for each, a solid in one dust for each, so N2 + 3 H2 → 2 NH3 takes 100 mB of nitrogen and
 * 300 of hydrogen and makes 200 of ammonia, which for gases is Avogadro's law. A reaction balances: every element has
 * as many atoms on each side, as a real equation does, and a reaction that does not is refused.
 *
 * <pre>{@code
 * Reaction ammonia = Reaction.builder()
 *         .in("jscore:nitrogen", "N2", 1, Phase.FLUID).in("jscore:hydrogen", "H2", 3, Phase.FLUID)
 *         .out("jscore:ammonia", "NH3", 2, Phase.FLUID).build();
 * ammonia.amount(ammonia.products().getFirst(), 1)   // 200
 * }</pre>
 *
 * <p>Pure: no game types. Each substance is named by its material's id, {@code "jscore:ammonia"}.
 *
 * @param reactants what goes in
 * @param products  what comes out
 */
public record Reaction(List<Term> reactants, List<Term> products) {

    /** The millibuckets a fluid comes in for each of its coefficient. */
    public static final int MILLIBUCKETS = 100;

    public Reaction {
        reactants = List.copyOf(reactants);
        products = List.copyOf(products);
        if (reactants.isEmpty() || products.isEmpty()) {
            throw new IllegalArgumentException("a reaction takes something and makes something");
        }
    }

    /** A reaction to declare, its substances one at a time. */
    public static Builder builder() {
        return new Builder();
    }

    /** Whether it balances: every element with as many atoms going in as coming out. */
    public boolean balanced() {
        return atoms(this.reactants).equals(atoms(this.products));
    }

    /**
     * How much of {@code term} a batch of {@code batches} takes or makes: its coefficient times the batches, in mB
     * for a fluid, in dusts for a solid.
     */
    public long amount(final Term term, final int batches) {
        if (batches < 1) {
            throw new IllegalArgumentException("a reaction runs one batch at least, not " + batches);
        }
        final long each = term.phase() == Phase.FLUID ? MILLIBUCKETS : 1L;
        return Math.multiplyExact(Math.multiplyExact((long) term.coefficient(), batches), each);
    }

    /** The equation as chemists write it: {@code N2 + 3 H2 → 2 NH3}. */
    public String equation() {
        return side(this.reactants) + " → " + side(this.products);
    }

    private static Map<String, Long> atoms(final List<Term> side) {
        final Map<String, Long> out = new HashMap<>();
        for (final Term term : side) {
            ChemicalFormula.parse(term.formula()).atoms().forEach((symbol, count) ->
                    out.merge(symbol, (long) count * term.coefficient(), Long::sum));
        }
        return out;
    }

    private static String side(final List<Term> terms) {
        final List<String> written = new ArrayList<>();
        for (final Term term : terms) {
            written.add((term.coefficient() == 1 ? "" : term.coefficient() + " ") + term.formula());
        }
        return String.join(" + ", written);
    }

    /** Whether a substance comes by the millibucket or by the dust. */
    public enum Phase {
        /** A solid, by the dust. */
        SOLID,
        /** A gas or a liquid, by the millibucket. */
        FLUID
    }

    /**
     * One substance of a reaction.
     *
     * @param material    its material's id: {@code "jscore:ammonia"}
     * @param formula     its formula as the equation writes it, the molecule's: {@code N2} for nitrogen gas
     * @param coefficient how many of it, at least one
     * @param phase       whether it comes by the dust or by the millibucket
     */
    public record Term(String material, String formula, int coefficient, Phase phase) {

        public Term {
            Objects.requireNonNull(material, "material");
            Objects.requireNonNull(phase, "phase");
            ChemicalFormula.parse(formula);
            if (coefficient < 1) {
                throw new IllegalArgumentException("a substance of a reaction counts one at least, not "
                        + coefficient);
            }
        }
    }

    /** How a reaction is declared. */
    public static final class Builder {

        private final List<Term> reactants = new ArrayList<>();
        private final List<Term> products = new ArrayList<>();

        private Builder() {
        }

        /** {@code coefficient} of the material {@code material}, of formula {@code formula}, goes in. */
        public Builder in(final String material, final String formula, final int coefficient, final Phase phase) {
            this.reactants.add(new Term(material, formula, coefficient, phase));
            return this;
        }

        /** {@code coefficient} of the material {@code material}, of formula {@code formula}, comes out. */
        public Builder out(final String material, final String formula, final int coefficient, final Phase phase) {
            this.products.add(new Term(material, formula, coefficient, phase));
            return this;
        }

        /**
         * The reaction.
         *
         * @throws IllegalArgumentException when it does not balance
         */
        public Reaction build() {
            final Reaction reaction = new Reaction(this.reactants, this.products);
            if (!reaction.balanced()) {
                throw new IllegalArgumentException("the reaction " + reaction.equation() + " does not balance");
            }
            return reaction;
        }
    }
}
