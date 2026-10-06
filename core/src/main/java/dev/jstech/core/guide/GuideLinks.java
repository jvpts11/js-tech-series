/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links written inside a manual's sentences, so a text can lead to another page in its own words, as a guide does:
 * {@code see [Installing a system](jsc:installing)} reads "see Installing a system (1.4.1)", the words and the number
 * leading to that entry, and {@code ([](jsc:installing))} reads "(1.4.1)". A link may lead to an entry, a section or a
 * chapter, a chapter named by its mod's namespace alone.
 *
 * <p>The number is the one the target has in the manual being read, so the same sentence reads "1.4.1" in a mod's
 * own manual and "3.4.1" in one holding every chapter. A target that manual does not hold reads as its words alone and
 * leads nowhere. A translation keeps the brackets and the target and translates the words.
 */
public final class GuideLinks {

    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)]\\(([a-z0-9_.-]+(?::[a-z0-9_./-]+)?)\\)");

    private GuideLinks() {
    }

    /**
     * The sentence in runs of words, each run plain or one link's words and number.
     *
     * @param numbers the number a target has in the manual being read, or null when that manual does not hold it
     */
    public static List<Run> runs(final String sentence, final Function<String, String> numbers) {
        final List<Run> runs = new ArrayList<>();
        final Matcher matcher = LINK.matcher(sentence);
        int at = 0;
        while (matcher.find()) {
            if (matcher.start() > at) {
                runs.add(new Run(sentence.substring(at, matcher.start()), ""));
            }
            final String words = matcher.group(1).strip();
            final String target = matcher.group(2);
            final String number = numbers.apply(target);
            if (number == null) {
                runs.add(new Run(words.isEmpty() ? GuideIds.path(target) : words, ""));
            } else {
                runs.add(new Run(words.isEmpty() ? number : words + " (" + number + ")", target));
            }
            at = matcher.end();
        }
        if (at < sentence.length()) {
            runs.add(new Run(sentence.substring(at), ""));
        }
        return runs;
    }

    /** The sentence as plain words, each link written as its words and number. */
    public static String plain(final String sentence, final Function<String, String> numbers) {
        final StringBuilder out = new StringBuilder();
        for (final Run run : runs(sentence, numbers)) {
            out.append(run.text());
        }
        return out.toString();
    }

    /** What the sentence's links lead to, in the order they are written. */
    public static List<String> targets(final String sentence) {
        final List<String> targets = new ArrayList<>();
        final Matcher matcher = LINK.matcher(sentence);
        while (matcher.find()) {
            targets.add(matcher.group(2));
        }
        return targets;
    }

    /**
     * A run of a sentence.
     *
     * @param text   its words, as they are drawn
     * @param target where it leads, or empty for plain words
     */
    public record Run(String text, String target) {
    }
}
