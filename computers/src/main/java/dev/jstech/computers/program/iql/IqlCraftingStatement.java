/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.iql;

import dev.jstech.computers.program.iql.IqlLexer.Token;
import dev.jstech.computers.program.iql.IqlLexer.Type;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * One statement that sets a part of the crafting network: a Crafting Interface or a Crafting Input Router, named as
 * its window names it, and the one setting it changes, in the words its window shows under Software. Pure, so it is
 * tested without the game; the engine applies it to the part of that name on the network's crafting cable.
 *
 * <pre>
 * SET INTERFACE 'Kiln A' EXCLUSIVE ON | EXCLUSIVE OFF
 * SET INTERFACE 'Kiln A' MAX JOBS 4 | MAX JOBS AUTO
 * SET INTERFACE 'Kiln A' ROUTE 'Coarse dirt' INPUT gravel TO ROUTER 'North' | ROUTE 'Coarse dirt' INPUT gravel AUTO
 * PAUSE INTERFACE 'Kiln A' | RESUME INTERFACE 'Kiln A'
 * RENAME INTERFACE 'Kiln A' TO 'Kiln B' | RENAME ROUTER 'North' TO 'Gravel in'
 * SET ROUTER 'North' FILTER ONLY gravel | and every other setting SET BUS takes
 * </pre>
 *
 * @param part   which kind of part it names
 * @param name   the part's name
 * @param change what it sets
 */
public record IqlCraftingStatement(Part part, String name, Change change) {

    /** The kind of part a statement names. */
    public enum Part {
        INTERFACE, ROUTER
    }

    /** What a statement sets. */
    public sealed interface Change permits Exclusive, MaxJobs, Paused, Route, Rename, RouterSetting {
    }

    /** One recipe at a time, or several. */
    public record Exclusive(boolean oneAtATime) implements Change {
    }

    /** The most jobs at once, 0 for as many as come. */
    public record MaxJobs(int most) implements Change {
    }

    /** Paused, or running. */
    public record Paused(boolean paused) implements Change {
    }

    /**
     * The router an input of a pattern goes through, by name, or null for the one its filter picks.
     *
     * @param pattern the pattern's name, as the interface lists it
     * @param input   the input, by its id as typed
     * @param router  the router's name, or null to route it by the filters again
     */
    public record Route(String pattern, String input, @Nullable String router) implements Change {
    }

    /** A new name for the part. */
    public record Rename(String newName) implements Change {
    }

    /** One of the settings a bus has, set on a router. */
    public record RouterSetting(IqlBusStatement.Change setting) implements Change {
    }

    /** Whether {@code input} is one of these statements, which this reads rather than the action parser. */
    public static boolean isCrafting(final String input) {
        final List<Token> tokens = IqlLexer.lex(input == null ? "" : input);
        if (tokens.size() < 2) {
            return false;
        }
        final Token verb = tokens.get(0);
        final Token what = tokens.get(1);
        if (word(verb, "SET") || word(verb, "RENAME")) {
            return word(what, "INTERFACE") || word(what, "ROUTER");
        }
        return (word(verb, "PAUSE") || word(verb, "RESUME")) && word(what, "INTERFACE");
    }

    /** Reads one of these statements. */
    public static IqlCraftingStatement parse(final String input) {
        return new Reader(IqlLexer.lex(input)).statement();
    }

    private static boolean word(final Token token, final String keyword) {
        return token.type() == Type.WORD && token.text().equalsIgnoreCase(keyword);
    }

    /* Reads the tokens of one statement left to right. */
    private static final class Reader {

        private final List<Token> tokens;
        private int pos;

        Reader(final List<Token> tokens) {
            this.tokens = tokens;
        }

        IqlCraftingStatement statement() {
            final String verb = tokens.get(pos++).text().toUpperCase(Locale.ROOT);
            final Part part = word(tokens.get(pos++), "ROUTER") ? Part.ROUTER : Part.INTERFACE;
            final String name = name(verb, part);
            final Change change = switch (verb) {
                case "PAUSE" -> new Paused(true);
                case "RESUME" -> new Paused(false);
                case "RENAME" -> rename();
                default -> part == Part.ROUTER ? new RouterSetting(IqlBusStatement.setting(tokens.subList(pos,
                        tokens.size()))) : setting();
            };
            if (change instanceof RouterSetting) {
                pos = tokens.size();
            }
            if (pos < tokens.size()) {
                throw IqlError.of(IqlError.UNEXPECTED_TOKEN, tokens.get(pos).text());
            }
            return new IqlCraftingStatement(part, name, change);
        }

        private String name(final String verb, final Part part) {
            if (pos >= tokens.size()) {
                final String said = verb + (part == Part.ROUTER ? " ROUTER" : " INTERFACE");
                throw part == Part.ROUTER ? IqlError.of(IqlError.ROUTER_NEEDS_NAME, said)
                        : IqlError.of(IqlError.INTERFACE_NEEDS_NAME, said);
            }
            return tokens.get(pos++).text();
        }

        private Change rename() {
            if (!peek("TO")) {
                throw IqlError.of(IqlError.RENAME_NEEDS_TO);
            }
            pos++;
            if (pos >= tokens.size()) {
                throw IqlError.of(IqlError.RENAME_NEEDS_TO);
            }
            return new Rename(tokens.get(pos++).text());
        }

        private Change setting() {
            if (pos >= tokens.size()) {
                throw IqlError.of(IqlError.INTERFACE_NEEDS_SETTING);
            }
            final Token setting = tokens.get(pos++);
            return switch (setting.text().toUpperCase(Locale.ROOT)) {
                case "EXCLUSIVE" -> new Exclusive(onOrOff());
                case "MAX" -> {
                    keyword("JOBS");
                    if (peek("AUTO")) {
                        pos++;
                        yield new MaxJobs(0);
                    }
                    yield new MaxJobs((int) number());
                }
                case "ROUTE" -> route();
                default -> throw IqlError.of(IqlError.INTERFACE_UNKNOWN_SETTING, setting.text());
            };
        }

        /* ROUTE 'pattern' INPUT key TO ROUTER 'name', or ROUTE 'pattern' INPUT key AUTO. */
        private Change route() {
            final String pattern = next(IqlError.A_PATTERN).text();
            keyword("INPUT");
            final String input = next(IqlError.AN_INPUT).text();
            if (peek("AUTO")) {
                pos++;
                return new Route(pattern, input, null);
            }
            keyword("TO");
            keyword("ROUTER");
            return new Route(pattern, input, next(IqlError.A_ROUTER).text());
        }

        private boolean onOrOff() {
            if (peek("ON")) {
                pos++;
                return true;
            }
            keyword("OFF");
            return false;
        }

        private long number() {
            final Token token = next(IqlError.A_COUNT);
            if (token.type() != Type.NUMBER) {
                throw IqlError.of(IqlError.EXPECTED_GOT, IqlError.A_COUNT, token.text());
            }
            try {
                return Long.parseLong(token.text());
            } catch (final NumberFormatException e) {
                throw IqlError.of(IqlError.EXPECTED_GOT, IqlError.A_COUNT, token.text());
            }
        }

        private Token next(final Object what) {
            if (pos >= tokens.size()) {
                throw IqlError.of(IqlError.EXPECTED, what);
            }
            return tokens.get(pos++);
        }

        private void keyword(final String keyword) {
            if (!peek(keyword)) {
                throw IqlError.of(IqlError.EXPECTED, keyword);
            }
            pos++;
        }

        private boolean peek(final String keyword) {
            return pos < tokens.size() && word(tokens.get(pos), keyword);
        }
    }
}
