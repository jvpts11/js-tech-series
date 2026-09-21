/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.sh;

import java.util.ArrayList;
import java.util.List;

/**
 * A typed line taken apart the way a shell takes one apart: the commands in it, what feeds the first and what
 * catches the last.
 *
 * <p>Until now a line was one command and its words. A shell is more than that, and always has been: a line is
 * a row of commands each handing its output to the next, with somewhere for the first to read from and
 * somewhere for the last to write to. That is what lets a player ask a question the mod never thought of,
 * which is the whole point of having a prompt at all.
 *
 * <p>Pure: it is given words and answers with words. What a redirection means to a disk, and what a name with
 * a star in it stands for, are the machine's to say, and are asked of it afterwards.
 *
 * @param stages  the commands, in order; one for a line with no pipe in it
 * @param from    the file the first command reads instead of the keyboard, or empty
 * @param into    the file the last command writes instead of the glass, or empty
 * @param append  whether writing adds to that file rather than replacing it
 */
public record ShLine(List<Stage> stages, String from, String into, boolean append) {

    /** One command of a line: the word that names it and the words after it. */
    public record Stage(String word, List<String> args) {

        public Stage {
            args = List.copyOf(args);
        }

        /** The whole stage as words again, which is what a shell hands to whatever runs it. */
        public List<String> tokens() {
            final List<String> out = new ArrayList<>(this.args.size() + 1);
            out.add(this.word);
            out.addAll(this.args);
            return out;
        }
    }

    public ShLine {
        stages = List.copyOf(stages);
        from = from == null ? "" : from;
        into = into == null ? "" : into;
    }

    /** A line with nothing on it, which is what an empty prompt is. */
    public static final ShLine NOTHING = new ShLine(List.of(), "", "", false);

    /**
     * Reads tokens the way a shell reads them.
     *
     * <p>A pipe ends a stage and starts the next. A redirection takes the word after it, whether it was
     * written against the arrow or apart from it, since both are typed at real shells every day.
     */
    public static ShLine of(final List<String> tokens) {
        final List<Stage> stages = new ArrayList<>();
        final List<String> words = new ArrayList<>();
        String from = "";
        String into = "";
        boolean append = false;
        for (int i = 0; i < tokens.size(); i++) {
            final String token = tokens.get(i);
            if (token.equals("|")) {
                addStage(stages, words);
                continue;
            }
            final Redirection redirection = redirectionOf(token);
            if (redirection == null) {
                words.add(token);
                continue;
            }
            final String named = redirection.name().isEmpty() && i + 1 < tokens.size()
                    ? tokens.get(++i) : redirection.name();
            if (redirection.reading()) {
                from = named;
            } else {
                into = named;
                append = redirection.append();
            }
        }
        addStage(stages, words);
        return new ShLine(stages, from, into, append);
    }

    /** Whether the line has anything to run at all. */
    public boolean isEmpty() {
        return this.stages.isEmpty();
    }

    /** Whether it is the plain one-command line a shell has always had. */
    public boolean isSimple() {
        return this.stages.size() == 1 && this.from.isEmpty() && this.into.isEmpty();
    }

    /** Whether anything at all is to be written to a file. */
    public boolean writes() {
        return !this.into.isEmpty();
    }

    private static void addStage(final List<Stage> stages, final List<String> words) {
        if (!words.isEmpty()) {
            stages.add(new Stage(words.get(0), new ArrayList<>(words.subList(1, words.size()))));
            words.clear();
        }
    }

    /** One arrow: which way it points, whether it adds, and the name written against it when there was one. */
    private record Redirection(boolean reading, boolean append, String name) {
    }

    private static Redirection redirectionOf(final String token) {
        if (token.startsWith(">>")) {
            return new Redirection(false, true, token.substring(2));
        }
        if (token.startsWith(">")) {
            return new Redirection(false, false, token.substring(1));
        }
        if (token.startsWith("<")) {
            return new Redirection(true, false, token.substring(1));
        }
        return null;
    }
}
