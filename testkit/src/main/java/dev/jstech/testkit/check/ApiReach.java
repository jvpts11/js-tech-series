/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The names a promised source's public face says, each followed to where it comes from.
 *
 * <p>A name of the mod that is not promised itself is a promise nobody decided to make. Every promised source is read
 * this way, so one step from each follows the whole promise to its end: a promised type naming another holds that
 * one to the promise too, and that one is read in turn.
 */
final class ApiReach {

    private final String text;
    private final String pkg;
    private final List<ApiPromise> promises;
    private final Set<String> promisedTypes;
    private final Set<String> declaredTypes;
    private final List<String> elsewhere;
    private final Map<String, Set<String>> variablesOfType = new HashMap<>();

    /* A type's name as a signature writes it: capitalized, and not the inner part of a dotted name. */
    private static final Pattern NAME = Pattern.compile("(?<![\\w.])([A-Z][A-Za-z0-9_]*)\\b");
    /* The lowercase words a line opens with: its modifiers, and for a type the word for its kind. */
    private static final Pattern LEADING_WORDS = Pattern.compile("^(?:@?[a-z-]+\\s+)*@?[a-z]*");
    private static final Pattern PACKAGE = Pattern.compile("\\bpackage\\s+([\\w.]+)\\s*;");

    /**
     * A reader of one source's promises.
     *
     * @param declaredTypes every type the mods declare in a file of its own, by its whole name: what a name with no
     *                      import may be in the file's own package
     */
    ApiReach(final String text, final List<ApiPromise> promises, final Set<String> promisedTypes,
             final Set<String> declaredTypes, final List<String> elsewhere) {
        this.text = text;
        final Matcher pkgMatch = PACKAGE.matcher(text);
        this.pkg = pkgMatch.find() ? pkgMatch.group(1) : "";
        this.promises = promises;
        this.promisedTypes = promisedTypes;
        this.declaredTypes = declaredTypes;
        this.elsewhere = elsewhere;
        for (final ApiPromise promise : promises) {
            if (promise.isType()) {
                this.variablesOfType.put(promise.owner(), variablesDeclared(afterLeadingWords(promise.what())));
            }
        }
    }

    /** Each promised type of the source with a type of the mod it names and nobody promised, as "a > b". */
    Set<String> leaks() {
        final Set<String> leaked = new LinkedHashSet<>();
        for (final ApiPromise promise : this.promises) {
            final Set<String> variables = variablesAround(promise);
            for (final String name : namedBy(promise)) {
                final String unpromised = variables.contains(name) ? null : unpromised(name);
                if (unpromised != null) {
                    leaked.add(promise.owner() + " > " + unpromised);
                }
            }
        }
        return leaked;
    }

    /** The name with its last part taken off, or null when there is nothing left to take. */
    static String outerOf(final String name) {
        final int dot = name.lastIndexOf('.');
        return dot < 0 ? null : name.substring(0, dot);
    }

    /* The whole name the file imports that type as, or null when it imports no such type. */
    private static String importOf(final String type, final String text) {
        final Pattern imported = Pattern.compile("^\\s*import\\s+([A-Za-z0-9_.]+\\." + type + ");");
        for (final String line : text.split("\n")) {
            final Matcher match = imported.matcher(line);
            if (match.find()) {
                return match.group(1);
            }
        }
        return null;
    }

    /* The type names a promise's line says, leaving out the name of what it declares. */
    private static Set<String> namedBy(final ApiPromise promise) {
        String what = promise.what().replaceFirst("^" + ApiPromise.TYPE, "")
                .replaceFirst("^" + ApiPromise.DEPRECATED, "");
        if (!promise.isType()) {
            final int open = what.indexOf('(');
            if (open >= 0) {
                int start = open;
                while (start > 0 && Character.isJavaIdentifierPart(what.charAt(start - 1))) {
                    start--;
                }
                what = what.substring(0, start) + what.substring(open);
            } else if (what.contains(" ")) {
                what = what.substring(0, what.lastIndexOf(' '));
            } else {
                // An enum's constant: it names nothing but itself.
                return Set.of();
            }
        }
        final Set<String> found = new LinkedHashSet<>();
        final Matcher names = NAME.matcher(what);
        while (names.find()) {
            found.add(names.group(1));
        }
        return found;
    }

    /* The type variables in reach of a promise: its own method's, and those of every type around it. */
    private Set<String> variablesAround(final ApiPromise promise) {
        final Set<String> found = new HashSet<>();
        for (String owner = promise.owner(); owner != null; owner = outerOf(owner)) {
            found.addAll(this.variablesOfType.getOrDefault(owner, Set.of()));
        }
        if (!promise.isType()) {
            found.addAll(variablesDeclared(afterLeadingWords(promise.what())));
        }
        return found;
    }

    /* What follows a line's modifiers and its kind's word, where a declaration's type variables open. */
    private static String afterLeadingWords(final String what) {
        final String bare = what.replaceFirst("^" + ApiPromise.TYPE, "");
        final Matcher words = LEADING_WORDS.matcher(bare);
        return words.find() ? bare.substring(words.end()).strip() : what;
    }

    /* The type variables a declaration opens with, as in <K, V extends Comparable<V>>, or none. */
    private static Set<String> variablesDeclared(final String declaration) {
        if (!declaration.startsWith("<")) {
            return Set.of();
        }
        int depth = 0;
        int end = 0;
        for (int i = 0; i < declaration.length(); i++) {
            final char c = declaration.charAt(i);
            if (c == '<') {
                depth++;
            } else if (c == '>' && --depth == 0) {
                end = i;
                break;
            }
        }
        final Set<String> found = new HashSet<>();
        for (final String part : ApiSourceReader.topLevelParts(declaration.substring(1, end))) {
            final Matcher name = NAME.matcher(part);
            if (name.find()) {
                found.add(name.group(1));
            }
        }
        return found;
    }

    /*
     * Null when a signature may say that name, or the whole name it stands for when that is a type of the mod no one
     * has promised. What decides it is where that very type comes from, not what else the file imports.
     */
    private String unpromised(final String name) {
        final String imported = importOf(name, this.text);
        if (imported != null) {
            for (final String outside : this.elsewhere) {
                if (imported.startsWith(outside)) {
                    return null;
                }
            }
            return this.promisedTypes.contains(imported) ? null : imported;
        }
        if (Pattern.compile("\\b(class|interface|record|enum)\\s+" + name + "\\b").matcher(this.text).find()) {
            for (final ApiPromise promise : this.promises) {
                if (promise.isType() && (promise.owner().endsWith("." + name) || promise.owner().equals(name))) {
                    return null;
                }
            }
            return name + ", declared here but not visible";
        }
        // Named with no import and not declared here: a type of the file's own package, if it has one of that name.
        final String samePackage = this.pkg + "." + name;
        if (this.declaredTypes.contains(samePackage)) {
            return this.promisedTypes.contains(samePackage) ? null : samePackage;
        }
        /*
         * Or else java.lang's, or a type nested in one the type extends or implements, as a block's Properties are:
         * that supertype is named on the type's own line and followed from there.
         */
        return null;
    }
}
