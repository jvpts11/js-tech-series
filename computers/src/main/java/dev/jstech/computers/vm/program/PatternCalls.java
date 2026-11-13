/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.system.CallCost;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.SigmaCosts;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regex, a pattern matched over a text (computers/docs/design/sigma.md). Matching is priced by the characters the
 * pattern reads, a block at a time, since a pattern can read a text many times over; one that would read it more than
 * {@link #READS_PER_CHARACTER} times over, as a pattern that backtracks without end does, stops the program instead of
 * the server.
 */
@TextHolder
final class PatternCalls {

    private static final String OWNER = "Regex";
    private static final String TEXT = "string";
    /** How many times over a pattern may read its text, and a little more for a short one. */
    private static final long READS_PER_CHARACTER = 256L;
    private static final long READS_AT_LEAST = 65_536L;
    /* However long the text, one call reads no more than this, so it never holds the server's tick for long. */
    private static final long READS_AT_MOST = 16L * 1024L * 1024L;

    private static final TextKey NOT_A_PATTERN = TextKey.of("jsc.vm.pattern.not_a_pattern",
            "'%s' is not a pattern: %s");
    private static final TextKey READ_TOO_OFTEN = TextKey.of("jsc.vm.pattern.read_too_often",
            "the pattern '%s' read the text more than %s times over and was stopped");
    private static final TextKey TOO_DEEP = TextKey.of("jsc.vm.pattern.too_deep",
            "the pattern '%s' went too deep into the text and was stopped");

    private PatternCalls() {
    }

    static void bind(final Map<MemberId, ProcessCalls.Binding> bindings) {
        ProcessCalls.bind(bindings, OWNER, "IsMatch", null, (process, target, arguments, line) ->
                matched(process, arguments, line, Matcher::find), TEXT, TEXT);
        ProcessCalls.bind(bindings, OWNER, "Match", null, (process, target, arguments, line) ->
                matched(process, arguments, line, matcher -> {
                    final Values.ListValue groups = new Values.ListValue();
                    if (matcher.find()) {
                        for (int group = 0; group <= matcher.groupCount(); group++) {
                            final String found = matcher.group(group);
                            groups.items().add(process.heap().text(found == null ? "" : found, line));
                        }
                    }
                    return process.heap().allocate(groups, groups.bytes(), line);
                }), TEXT, TEXT);
        ProcessCalls.bind(bindings, OWNER, "Matches", null, (process, target, arguments, line) ->
                matched(process, arguments, line, matcher -> {
                    final Values.ListValue all = new Values.ListValue();
                    while (matcher.find()) {
                        all.items().add(process.heap().text(matcher.group(), line));
                    }
                    return process.heap().allocate(all, all.bytes(), line);
                }), TEXT, TEXT);
        ProcessCalls.bind(bindings, OWNER, "Replace", null, (process, target, arguments, line) ->
                matched(process, arguments, line, matcher -> {
                    final String replacement = Matcher.quoteReplacement((String) process.heap().alive(arguments[2],
                            line)).replace("\\$", "$");
                    // Built a match at a time, each asked to fit, so a long replacement stops the program in time.
                    final StringBuilder out = new StringBuilder();
                    while (matcher.find()) {
                        matcher.appendReplacement(out, replacement);
                        process.heap().fitsText(out.length(), line);
                    }
                    matcher.appendTail(out);
                    return process.heap().text(out.toString(), line);
                }), TEXT, TEXT, TEXT);
        ProcessCalls.bind(bindings, OWNER, "Split", null, (process, target, arguments, line) ->
                matched(process, arguments, line, matcher -> {
                    final Values.ListValue parts = new Values.ListValue();
                    int from = 0;
                    final String text = String.valueOf(arguments[0]);
                    while (matcher.find()) {
                        parts.items().add(process.heap().text(text.substring(from, matcher.start()), line));
                        from = matcher.end();
                    }
                    parts.items().add(process.heap().text(text.substring(from), line));
                    return process.heap().allocate(parts, parts.bytes(), line);
                }), TEXT, TEXT);
    }

    /*
     * Runs {@code work} on the pattern's matcher over the text, then charges the program for what the pattern read; a
     * pattern that is none, or that reads the text too often, stops the program.
     */
    private static Object matched(final Process process, final Object[] arguments, final int line,
                                  final Function<Matcher, Object> work) {
        // A text or a pattern that is null is nothing to read; written out it would be the word "null", and be matched.
        final String text = (String) process.heap().alive(arguments[0], line);
        final String written = (String) process.heap().alive(arguments[1], line);
        final Pattern pattern;
        try {
            pattern = Pattern.compile(written);
        } catch (final PatternSyntaxException wrong) {
            throw new Halt(Halt.Reason.BAD_CAST, line, NOT_A_PATTERN.with(written, wrong.getDescription()),
                    Halt.Thrown.ARGUMENT);
        } catch (final StackOverflowError deep) {
            // Compiling recurses too, as deep as the pattern nests its groups.
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, TOO_DEEP.with(written));
        }
        final CountedText counted = new CountedText(text, Math.min(READS_AT_MOST,
                Math.max(READS_AT_LEAST, READS_PER_CHARACTER * text.length())));
        try {
            return work.apply(pattern.matcher(counted));
        } catch (final CountedText.ReadTooOften stopped) {
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, READ_TOO_OFTEN.with(written, READS_PER_CHARACTER));
        } catch (final StackOverflowError deep) {
            // Java's matcher recurses for some patterns, and a long text would take the server's stack with it.
            throw new Halt(Halt.Reason.OUT_OF_RANGE, line, TOO_DEEP.with(written));
        } catch (final IllegalArgumentException | IndexOutOfBoundsException wrong) {
            // A replacement naming a group the pattern does not have.
            throw new Halt(Halt.Reason.BAD_CAST, line, NOT_A_PATTERN.with(written, wrong.getMessage()),
                    Halt.Thrown.ARGUMENT);
        } finally {
            process.charge(blocks(counted.reads()));
        }
    }

    /* What reading that many characters costs: a read's price for every block started. */
    private static int blocks(final long reads) {
        final long blocks = (reads + CallCost.BLOCK_BYTES - 1) / CallCost.BLOCK_BYTES;
        return (int) Math.min(Integer.MAX_VALUE, blocks * SigmaCosts.READ_PER_BLOCK);
    }

    /** A text that counts every character read of it, and stops a reader that reads too many. */
    private static final class CountedText implements CharSequence {

        private final String text;
        private final int from;
        private final int to;
        private final long[] read;
        private final long most;

        CountedText(final String text, final long most) {
            this(text, 0, text.length(), new long[1], most);
        }

        private CountedText(final String text, final int from, final int to, final long[] read, final long most) {
            this.text = text;
            this.from = from;
            this.to = to;
            this.read = read;
            this.most = most;
        }

        /** Thrown when the pattern read more than it may, unwinding the matcher. */
        static final class ReadTooOften extends RuntimeException {

            ReadTooOften() {
                super(null, null, false, false);
            }
        }

        long reads() {
            return this.read[0];
        }

        @Override
        public int length() {
            return this.to - this.from;
        }

        @Override
        public char charAt(final int index) {
            if (++this.read[0] > this.most) {
                throw new ReadTooOften();
            }
            return this.text.charAt(this.from + index);
        }

        @Override
        public CharSequence subSequence(final int start, final int end) {
            return new CountedText(this.text, this.from + start, this.from + end, this.read, this.most);
        }

        @Override
        public String toString() {
            return this.text.substring(this.from, this.to);
        }
    }
}
