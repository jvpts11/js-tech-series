/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inventory;

import dev.jstech.core.id.IStableName;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Which items pass: a list of rules, and whether the list says the only items that pass or the only ones that do not.
 * A rule names an item exactly (the same item with the same components, so a worn sword is not a new one), the item
 * whatever its damage and components (fuzzy), or every item of a tag. An empty list lets everything pass, whichever its
 * mode, so a filter nobody set filters nothing.
 *
 * <p>Each rule may carry an amount, which the filter only keeps: a bus reads it as how many of the item to keep or to
 * move at most. The first rule that matches an item is the one that speaks for it.
 *
 * @param mode  whether the rules name what passes or what does not
 * @param rules the rules, in the order they are looked at
 */
public record ItemFilter(Mode mode, List<Rule> rules) {

    /** A filter that lets everything pass. */
    public static final ItemFilter EVERYTHING = new ItemFilter(Mode.ALL_BUT, List.of());

    public ItemFilter {
        Objects.requireNonNull(mode, "mode");
        rules = List.copyOf(rules);
    }

    /** A filter that lets pass only what {@code rules} name. */
    public static ItemFilter only(final Rule... rules) {
        return new ItemFilter(Mode.ONLY, List.of(rules));
    }

    /** A filter that lets pass everything but what {@code rules} name. */
    public static ItemFilter allBut(final Rule... rules) {
        return new ItemFilter(Mode.ALL_BUT, List.of(rules));
    }

    /** Whether {@code subject} passes. */
    public boolean allows(final IFilterSubject subject) {
        if (this.rules.isEmpty()) {
            return true;
        }
        final boolean named = firstMatch(subject).isPresent();
        return this.mode == Mode.ONLY ? named : !named;
    }

    /** The first rule that names {@code subject}. */
    public Optional<Rule> firstMatch(final IFilterSubject subject) {
        for (final Rule rule : this.rules) {
            if (rule.matches(subject)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    /** The amount the first rule that names {@code subject} carries; 0 when none names it or it carries none. */
    public long amountFor(final IFilterSubject subject) {
        return firstMatch(subject).map(Rule::amount).orElse(0L);
    }

    /** The same filter with {@code rule} added at the end. */
    public ItemFilter with(final Rule rule) {
        final List<Rule> more = new ArrayList<>(this.rules);
        more.add(Objects.requireNonNull(rule, "rule"));
        return new ItemFilter(this.mode, more);
    }

    /** Whether the rules name what passes or what does not. */
    public enum Mode implements IStableName {
        /** Only what the rules name passes. */
        ONLY("only"),
        /** Everything but what the rules name passes. */
        ALL_BUT("all_but");

        private final String serializedName;

        Mode(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }

    /** One rule of a filter: what it names, and the amount it carries. */
    public sealed interface Rule permits Exact, Fuzzy, Tag {

        /** The amount it carries; 0 for none. */
        long amount();

        /** Whether it names {@code subject}. */
        boolean matches(IFilterSubject subject);
    }

    /**
     * The same item as {@code pattern}, with the same components.
     *
     * @param pattern the item named
     * @param amount  the amount it carries
     */
    public record Exact(IFilterSubject pattern, long amount) implements Rule {

        public Exact {
            Objects.requireNonNull(pattern, "pattern");
            checkAmount(amount);
        }

        @Override
        public boolean matches(final IFilterSubject subject) {
            return this.pattern.sameAs(subject);
        }
    }

    /**
     * The item {@code item} whatever its damage and components.
     *
     * @param item   the item's id
     * @param amount the amount it carries
     */
    public record Fuzzy(String item, long amount) implements Rule {

        public Fuzzy {
            Objects.requireNonNull(item, "item");
            checkId(item);
            checkAmount(amount);
        }

        @Override
        public boolean matches(final IFilterSubject subject) {
            return this.item.equals(subject.itemId());
        }
    }

    /**
     * Every item of the tag {@code tag}.
     *
     * @param tag    the tag's id
     * @param amount the amount it carries
     */
    public record Tag(String tag, long amount) implements Rule {

        public Tag {
            Objects.requireNonNull(tag, "tag");
            checkId(tag);
            checkAmount(amount);
        }

        @Override
        public boolean matches(final IFilterSubject subject) {
            return subject.hasTag(this.tag);
        }
    }

    /* Rejects an id the game could not read back, so saving a rule never fails later on a bad string. */
    private static void checkId(final String id) {
        final int colon = id.indexOf(':');
        final String namespace = colon < 0 ? "minecraft" : id.substring(0, colon);
        final String path = colon < 0 ? id : id.substring(colon + 1);
        if (namespace.isEmpty() || !namespace.chars().allMatch(ItemFilter::isNamespaceChar)
                || path.isEmpty() || !path.chars().allMatch(ItemFilter::isPathChar)) {
            throw new IllegalArgumentException("not a valid id: " + id);
        }
    }

    private static boolean isNamespaceChar(final int c) {
        return c == '_' || c == '-' || c == '.' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9';
    }

    private static boolean isPathChar(final int c) {
        return isNamespaceChar(c) || c == '/';
    }

    private static void checkAmount(final long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("a rule carries no less than nothing: " + amount);
        }
    }
}
