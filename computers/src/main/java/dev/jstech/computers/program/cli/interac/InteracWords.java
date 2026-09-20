/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.interac;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What was typed after {@code interac}, taken apart: the verb, the words, and the options.
 *
 * <p>One program is typed at in three ways here, because the systems it runs on are typed at in three ways.
 * A Unix prompt says {@code interac get 42 cobblestone --to local}; the same player may write the verb as an
 * option, {@code interac --get 42 cobblestone}; and a DOS prompt says
 * {@code INTERAC GET 42 COBBLESTONE /LOCAL}, in any case at all. All three mean the same thing, so all three
 * are read into the same answer here and the program itself never asks which system it is on.
 *
 * <p>Pure, and the words are only words: nothing here knows what an item is or whether the network has one.
 *
 * @param verb    the word that says what to do, lowercase, or empty when none was given
 * @param words   everything after the verb that was not an option
 * @param options the options given, by name, each with its value or {@code ""} for one that is only given
 */
public record InteracWords(String verb, List<String> words, Map<String, String> options) {

    /** The options that take a value after them rather than being a flag on their own. */
    private static final List<String> TAKES_A_VALUE = List.of("to", "sort", "mod", "server");

    /** The DOS switches, each as the option it means: {@code /LOCAL} is {@code --to local}. */
    private static final Map<String, String[]> DOS_SWITCHES = Map.of(
            "local", new String[] {"to", "local"},
            "?", new String[] {"help", ""},
            "h", new String[] {"help", ""});

    public InteracWords {
        words = List.copyOf(words);
        options = Map.copyOf(options);
    }

    /** Reads the words a player typed after the program's own name. */
    public static InteracWords of(final List<String> args) {
        final List<String> words = new ArrayList<>();
        final Map<String, String> options = new LinkedHashMap<>();
        String verb = "";
        for (int i = 0; i < args.size(); i++) {
            final String raw = args.get(i);
            if (raw.startsWith("--") && raw.length() > 2) {
                i = option(raw.substring(2), args, i, options);
            } else if (raw.startsWith("/") && raw.length() > 1) {
                dosSwitch(raw.substring(1), options);
            } else if (verb.isEmpty() && words.isEmpty()) {
                verb = raw.toLowerCase(Locale.ROOT);
            } else {
                words.add(raw);
            }
        }
        /*
         * A verb written as an option is the verb: somebody who writes --get means get, and the options that
         * are left are the ones that really are options. Which names are verbs is the program's to say, so
         * anything that was not a verb simply stays an option and the program answers for it.
         */
        return new InteracWords(verb, words, options);
    }

    /**
     * The same, with the verb taken from an option when the word in front was not one.
     *
     * <p>{@code --get 42 cobblestone} says the verb the way a flag is written, so the 42 lands where the verb
     * would have been. Once the program says which names are verbs, the option is the verb and what was
     * standing in its place goes back to being a word.
     */
    public InteracWords withVerbFrom(final List<String> verbs) {
        if (verbs.contains(this.verb)) {
            return this;
        }
        for (final String name : this.options.keySet()) {
            if (!verbs.contains(name)) {
                continue;
            }
            final Map<String, String> left = new LinkedHashMap<>(this.options);
            final String value = left.remove(name);
            final List<String> words = new ArrayList<>();
            if (!value.isEmpty()) {
                words.add(value);
            }
            if (!this.verb.isEmpty()) {
                words.add(this.verb);
            }
            words.addAll(this.words);
            return new InteracWords(name, words, left);
        }
        return this;
    }

    /** Whether that option was given at all. */
    public boolean has(final String name) {
        return this.options.containsKey(name);
    }

    /** What that option was given as, or {@code fallback} when it was not given. */
    public String option(final String name, final String fallback) {
        final String value = this.options.get(name);
        return value == null || value.isEmpty() ? fallback : value.toLowerCase(Locale.ROOT);
    }

    /** The word at that place, or empty when there are fewer. */
    public String word(final int index) {
        return index >= 0 && index < this.words.size() ? this.words.get(index) : "";
    }

    public int wordCount() {
        return this.words.size();
    }

    /**
     * The quantity the words open with, or {@code -1} when they open with something else.
     *
     * <p>Written the way a person writes one: {@code 1000}, {@code 1,000} and {@code 1_000} are the same
     * number, since a listing shows the first of those and somebody will type it back.
     */
    public long quantity() {
        final String first = word(0).replace(",", "").replace("_", "");
        if (first.isEmpty()) {
            return -1L;
        }
        try {
            final long value = Long.parseLong(first);
            return value > 0L ? value : -1L;
        } catch (final NumberFormatException notANumber) {
            return -1L;
        }
    }

    /**
     * The thing the words name, as one piece of text: everything after the quantity when there is one, and
     * everything otherwise, with the quotes a player may have put around a name of several words taken off.
     */
    public String item() {
        final List<String> naming = quantity() < 0L ? this.words : this.words.subList(1, this.words.size());
        final String joined = String.join(" ", naming).trim();
        if (joined.length() > 1 && joined.startsWith("\"") && joined.endsWith("\"")) {
            return joined.substring(1, joined.length() - 1);
        }
        return joined;
    }

    /** Reads one {@code --option}, taking the word after it when that option is one that takes a value. */
    private static int option(final String written, final List<String> args, final int at,
                              final Map<String, String> options) {
        final int equals = written.indexOf('=');
        if (equals > 0) {
            options.put(written.substring(0, equals).toLowerCase(Locale.ROOT), written.substring(equals + 1));
            return at;
        }
        final String name = written.toLowerCase(Locale.ROOT);
        if (TAKES_A_VALUE.contains(name) && at + 1 < args.size() && !args.get(at + 1).startsWith("-")) {
            options.put(name, args.get(at + 1));
            return at + 1;
        }
        options.put(name, "");
        return at;
    }

    /** Reads one {@code /SWITCH}, which is how the DOS family writes an option, in any case. */
    private static void dosSwitch(final String written, final Map<String, String> options) {
        final String[] parts = written.toLowerCase(Locale.ROOT).split(":", 2);
        final String[] known = DOS_SWITCHES.get(parts[0]);
        if (known != null) {
            options.put(known[0], known[1]);
        } else if (parts[0].equals("s") && parts.length == 2) {
            // /S:COUNT is the DOS way of asking for a sort, and S is the letter the real ones used for it.
            options.put("sort", parts[1]);
        } else {
            options.put(parts[0], parts.length == 2 ? parts[1] : "");
        }
    }
}
