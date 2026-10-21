/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * What each language of a mod lacks: every key any of its languages has, less the keys that language has, sorted.
 */
public final class TranslationGaps {

    private TranslationGaps() {
    }

    /** Every key of {@code languages}, each language's keys by its code. */
    public static Set<String> keys(final Map<String, Set<String>> languages) {
        final Set<String> all = new TreeSet<>();
        languages.values().forEach(all::addAll);
        return all;
    }

    /** Each language with the keys it lacks, by its code, sorted. */
    public static Map<String, List<String>> of(final Map<String, Set<String>> languages) {
        final Set<String> all = keys(languages);
        final Map<String, List<String>> out = new TreeMap<>();
        languages.forEach((code, has) -> out.put(code, all.stream().filter(key -> !has.contains(key)).toList()));
        return out;
    }
}
