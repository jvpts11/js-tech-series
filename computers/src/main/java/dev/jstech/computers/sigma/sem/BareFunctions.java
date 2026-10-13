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
        /** A call on the first value, handed one fixed value: {@code rewind(f)} is {@code f.Seek(0)}. */
        ON_THE_FIRST_FIXED,
        /**
         * A call on the last value, handed the ones before it, as C puts the file last: {@code fputs(s, f)} is
         * {@code f.Write(s)}.
         */
        ON_THE_LAST,
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
        FORMATTED,
        /**
         * A format of one hole read while the program is compiled, and the library's read of one value into the
         * variable handed with it: {@code scanf("%d", out n)} is {@code Console.Scan(out n)}.
         */
        SCANNED,
        /**
         * A format read while the program is compiled, its text written to the file handed first:
         * {@code fprintf(f, "%d\n", n)} is {@code f.Write(} the text {@code )}.
         */
        FILE_PRINTED,
        /** scanf's one value, read from the file handed first: {@code fscanf(f, "%d", out n)} is {@code f.Scan(out n)}. */
        FILE_SCANNED,
        /** The text handed second, given to the variable handed first: {@code strcpy(out d, s)} is {@code d = s}. */
        COPIED_INTO,
        /**
         * The text handed second, joined onto what the variable handed first holds: {@code strcat(ref d, s)} is
         * {@code d = d + s}.
         */
        JOINED_ONTO
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

        /** The types of the values it takes, in order, leaving out a list that goes on and the out of a place. */
        public List<String> types() {
            final List<String> types = new ArrayList<>(this.takes.size());
            for (final String value : this.takes) {
                if (!MORE.equals(value)) {
                    final String type = value.startsWith(OUT) ? value.substring(OUT.length()) : value;
                    types.add(type.substring(0, type.indexOf(' ')));
                }
            }
            return types;
        }

        /** Whether the value at {@code index} is a place the call fills in, handed with out. */
        public boolean outward(final int index) {
            return this.takes.get(index).startsWith(OUT);
        }

        /** The name of the value at {@code index}, which a message about that value calls it by. */
        public String nameOf(final int index) {
            final String value = this.takes.get(index);
            return value.substring(value.lastIndexOf(' ') + 1);
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
    /** What a place the call fills in is written with. */
    private static final String OUT = "out ";
    /** Where the long way of a formatting call lives, which is text joined up and nothing called. */
    private static final String JOINED_TEXT = "string";
    /** The type the character tests and changes are asked of. */
    private static final String CHARACTER = "char";
    /** A file a program opened, which the calls on files are made on. */
    private static final String FILE = "FILE";
    /** The file a call on files is handed. */
    private static final String FILE_VALUE = "FILE f";
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
        same("getchar", "Console", "Read", form("int"));
        add(new Function("scanf", List.of(form("int", "string format", MORE)), Shape.SCANNED, "Console", "Scan", 0,
                OLD_NAMES, BOTH));
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
        // Files, as C's stdio works them, each one call on the file the program opened or on File.
        same("fopen", "File", "Open", form(FILE, "string path", "string mode"));
        first("fclose", "Close", form("void", FILE_VALUE));
        last("fgets", "ReadLine", form("bool", "out string line", FILE_VALUE));
        last("fputs", "Write", form("void", "string text", FILE_VALUE));
        add(new Function("fprintf", List.of(form("void", FILE_VALUE, "string format", MORE)), Shape.FILE_PRINTED,
                FILE, "Write", 0, OLD_NAMES, BOTH));
        add(new Function("fscanf", List.of(form("int", FILE_VALUE, "string format", MORE)), Shape.FILE_SCANNED,
                FILE, "Scan", 0, OLD_NAMES, BOTH));
        first("fgetc", "Read", form("int", FILE_VALUE));
        last("fputc", "Write", form("void", "char c", FILE_VALUE));
        add(new Function("feof", List.of(form("bool", FILE_VALUE)), Shape.READ_OFF_THE_FIRST, FILE, "AtEnd", 0,
                OLD_NAMES, BOTH));
        add(new Function("rewind", List.of(form("void", FILE_VALUE)), Shape.ON_THE_FIRST_FIXED, FILE, "Seek", 0,
                OLD_NAMES, BOTH));
        first("fseek", "Seek", form("void", FILE_VALUE, "int position"));
        add(new Function("ftell", List.of(form("int", FILE_VALUE)), Shape.READ_OFF_THE_FIRST, FILE, "Position", 0,
                OLD_NAMES, BOTH));
        same("remove", "File", "Delete", form("bool", "string path"));
        same("rename", "File", "Move", form("bool", "string from", "string to"));
        // In C's order, the place first: out for one that is only written, ref for one that is read first.
        add(new Function("strcpy", List.of(form("string", "out string dest", "string src")), Shape.COPIED_INTO,
                JOINED_TEXT, "", 0, OLD_NAMES, BOTH));
        add(new Function("strcat", List.of(form("string", "ref string dest", "string src")), Shape.JOINED_ONTO,
                JOINED_TEXT, "", 0, OLD_NAMES, BOTH));
        same("toupper", CHARACTER, "ToUpper", form("char", "char c"));
        same("tolower", CHARACTER, "ToLower", form("char", "char c"));
        same("isdigit", CHARACTER, "IsDigit", form("bool", "char c"));
        same("isalpha", CHARACTER, "IsLetter", form("bool", "char c"));
        same("isspace", CHARACTER, "IsWhiteSpace", form("bool", "char c"));
        // A bus of the network by its name, as its window and the IQL name it.
        same("bus", "Bus", "Named", form("Bus", "string name"));
        // A Redstone Interface of the machine by the name it answers to, as its window and the IQL name it.
        same("redstone", "Redstone", "Named", form("Redstone", "string name"));
        // A Crafting Interface and a Crafting Input Router of the crafting network, by the names their windows give.
        same("craftInterface", "CraftInterface", "Named", form("CraftInterface", "string name"));
        same("craftRouter", "CraftRouter", "Named", form("CraftRouter", "string name"));
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

    /** One that is a call on the file handed first. */
    private static void first(final String name, final String member, final Form form) {
        add(new Function(name, List.of(form), Shape.ON_THE_FIRST, FILE, member, 0, OLD_NAMES, BOTH));
    }

    /** One that is a call on the file handed last. */
    private static void last(final String name, final String member, final Form form) {
        add(new Function(name, List.of(form), Shape.ON_THE_LAST, FILE, member, 0, OLD_NAMES, BOTH));
    }

    private static void add(final Function function) {
        TABLE.put(function.name(), function);
    }

    private static Form form(final String gives, final String... takes) {
        return new Form(List.of(takes), gives);
    }
}
