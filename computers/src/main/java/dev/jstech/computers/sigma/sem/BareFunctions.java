/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaVersions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The calls written with no type in front of them, the way the languages of the old machines wrote theirs:
 * {@code printf}, and the old names for what the library already does.
 *
 * <p>One table, and everything that has to know them reads it: the checker, the editors that offer them, the
 * strip under an editor's list that prices them, and the manual that lists them. Each says its name, the ways it
 * is written, what it is written down as, the languages that have it and the version it came in.
 *
 * <p>None of them is anything new while a program runs. Each comes down to a call the library already has, the
 * one a player would have written the long way, and the listing is the one the long way gives, to the byte. A
 * method or a variable of the program's own under the same name is the program's, and is what gets called.
 */
public final class BareFunctions {

    /**
     * How a call of one of them comes down to the long way of writing it.
     *
     * <p>The long way is always a single call or a single value of the library, which is what lets a listing come
     * out the same: nothing is added around it, and the values the player wrote are handed over as they were.
     */
    public enum Shape {
        /** The library's own call, handed the same values: {@code puts(s)} is {@code Console.PrintLine(s)}. */
        SAME_VALUES,
        /** A call on the first value, handed the rest: {@code strstr(h, n)} is {@code h.IndexOf(n)}. */
        ON_THE_FIRST,
        /** A value read off the first: {@code strlen(s)} is {@code s.Length}. */
        READ_OFF_THE_FIRST,
        /**
         * The library's own call, handed the same values and then one fixed value: {@code rand()} is
         * {@code Random.Next(32768)}, and {@code atoi(s)} is {@code Convert.ToInt(s, 0)}.
         */
        FIXED_VALUE,
        /** A format read while the program is compiled, its text handed to the console: {@code printf}. */
        PRINTED,
        /** The same format, with the text as the value instead of printed: {@code sprintf}. */
        FORMATTED
    }

    /**
     * One way of writing a call of it, as the manual and the editors show it.
     *
     * @param takes the values it takes, in order, each its type and its name ({@code "int n"}); {@code "..."}
     *              closes a list that goes on
     * @param gives the type of what it gives back
     */
    public record Form(List<String> takes, String gives) {

        public Form {
            takes = List.copyOf(takes);
        }

        /** The types of the values it takes, in order, leaving out a list that goes on. */
        public List<String> types() {
            final List<String> types = new ArrayList<>(this.takes.size());
            for (final String value : this.takes) {
                if (!MORE.equals(value)) {
                    types.add(value.substring(0, value.indexOf(' ')));
                }
            }
            return types;
        }

        /** The name of the value at {@code index}, which a message about that value calls it by. */
        public String nameOf(final int index) {
            final String value = this.takes.get(index);
            return value.substring(value.indexOf(' ') + 1);
        }

        /** The form written out under {@code name}: {@code abs(int n) : int}. */
        public String written(final String name) {
            return name + "(" + String.join(", ", this.takes) + ") : " + this.gives;
        }
    }

    /**
     * One of them.
     *
     * @param name      what it is called, which is how a program writes it
     * @param forms     the ways it is written, each checked as a version of one method
     * @param shape     how a call of it comes down to the long way
     * @param owner     the type of the library the long way is on
     * @param member    the call or the value of that type the long way names
     * @param fixed     the value the long way is handed, for {@link Shape#FIXED_VALUE}, and nothing otherwise
     * @param since     the version of the language it came in
     * @param languages the languages that have it
     */
    public record Function(String name, List<Form> forms, Shape shape, String owner, String member, int fixed,
                           int since, Set<LanguageLevel> languages) {

        public Function {
            forms = List.copyOf(forms);
            languages = Set.copyOf(languages);
        }

        /** Whether a program in {@code level} can call it at all, whatever version it is held to. */
        public boolean in(final LanguageLevel level) {
            return this.languages.contains(level);
        }
    }

    /** What closes a list of values that goes on. */
    public static final String MORE = "...";
    /** Where the long way of a formatting call lives, which is text joined up and nothing called. */
    private static final String JOINED_TEXT = "string";
    /** The type the character tests and changes are asked of. */
    private static final String CHARACTER = "char";
    /** What an old program's rand gives back is under this, as it was on the machines it comes from. */
    private static final int RAND_LIMIT = 32768;
    private static final Set<LanguageLevel> BOTH = Set.of(LanguageLevel.SIGMA, LanguageLevel.SIGMA_SHARP);
    /** The version the old names came in; printf came with the languages themselves. */
    private static final int OLD_NAMES = 2;

    private static final Map<String, Function> TABLE = new LinkedHashMap<>();

    static {
        add(new Function("printf", List.of(form("void", "string format", MORE)), Shape.PRINTED, "Console", "Print",
                0, SigmaVersions.FIRST, BOTH));
        add(new Function("sprintf", List.of(form("string", "string format", MORE)), Shape.FORMATTED, JOINED_TEXT,
                "", 0, OLD_NAMES, BOTH));
        same("puts", "Console", "PrintLine", form("void", "string text"));
        same("putchar", "Console", "Print", form("void", "char c"));
        same("gets", "Console", "ReadLine", form("string"));
        same("exit", "Program", "Exit", form("void", "int status"));
        same("abs", "Math", "Abs", form("int", "int n"), form("double", "double x"));
        same("sqrt", "Math", "Sqrt", form("double", "double x"));
        same("pow", "Math", "Pow", form("double", "double x", "double y"));
        same("floor", "Math", "Floor", form("double", "double x"));
        same("min", "Math", "Min", form("int", "int a", "int b"), form("double", "double a", "double b"));
        same("max", "Math", "Max", form("int", "int a", "int b"), form("double", "double a", "double b"));
        // Text that is not a number is 0, as it was: the value handed to fall back on.
        add(new Function("atoi", List.of(form("int", "string text")), Shape.FIXED_VALUE, "Convert", "ToInt", 0,
                OLD_NAMES, BOTH));
        same("atof", "Convert", "ToDouble", form("double", "string text"));
        same("itoa", "Convert", "ToString", form("string", "int value"), form("string", "int value", "int base"));
        same("srand", "Random", "Seed", form("void", "long seed"));
        add(new Function("rand", List.of(form("int")), Shape.FIXED_VALUE, "Random", "Next", RAND_LIMIT,
                OLD_NAMES, BOTH));
        add(new Function("strlen", List.of(form("int", "string text")), Shape.READ_OFF_THE_FIRST, "string",
                "Length", 0, OLD_NAMES, BOTH));
        add(new Function("strstr", List.of(form("int", "string haystack", "string needle")), Shape.ON_THE_FIRST,
                "string", "IndexOf", 0, OLD_NAMES, BOTH));
        same("strcmp", "string", "Compare", form("int", "string a", "string b"));
        same("toupper", CHARACTER, "ToUpper", form("char", "char c"));
        same("tolower", CHARACTER, "ToLower", form("char", "char c"));
        same("isdigit", CHARACTER, "IsDigit", form("bool", "char c"));
        same("isalpha", CHARACTER, "IsLetter", form("bool", "char c"));
        same("isspace", CHARACTER, "IsWhiteSpace", form("bool", "char c"));
    }

    private BareFunctions() {
    }

    /** The one of them called {@code name}, or null when none is. */
    public static Function named(final String name) {
        return TABLE.get(name);
    }

    /** Every one of them, in the order the manual lists them. */
    public static List<Function> all() {
        return List.copyOf(TABLE.values());
    }

    /** Those whose names begin with {@code prefix}, ignoring case, in the order the manual lists them. */
    public static List<Function> startingWith(final String prefix) {
        final List<Function> found = new ArrayList<>();
        for (final Function function : TABLE.values()) {
            if (function.name().regionMatches(true, 0, prefix, 0, prefix.length())) {
                found.add(function);
            }
        }
        return found;
    }

    private static void same(final String name, final String owner, final String member, final Form... forms) {
        add(new Function(name, List.of(forms), Shape.SAME_VALUES, owner, member, 0, OLD_NAMES, BOTH));
    }

    private static void add(final Function function) {
        TABLE.put(function.name(), function);
    }

    private static Form form(final String gives, final String... takes) {
        return new Form(List.of(takes), gives);
    }
}
