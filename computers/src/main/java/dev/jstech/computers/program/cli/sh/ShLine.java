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
 * @param badToken the mark or word a shell would report as unexpected when a pipe or an arrow has nothing on
 *                 one side ({@code newline} when the line ended there), or empty when the line is well formed
 */
public record ShLine(List<Stage> stages, String from, String into, boolean append, String badToken) {

    /** A line with nothing on it, which is what an empty prompt is. */
    public static final ShLine NOTHING = new ShLine(List.of(), "", "", false, "");

    /** What a shell calls the end of the line when it is the thing that came too soon. */
    private static final String NEWLINE = "newline";

    /** One command of a line: the word that names it and the words after it. */
    public record Stage(String word, List<String> args) {

        public Stage {
            args = List.copyOf(args);
        }
    }

    public ShLine {
        stages = List.copyOf(stages);
        from = from == null ? "" : from;
        into = into == null ? "" : into;
        badToken = badToken == null ? "" : badToken;
    }

    /**
     * Reads a line's words the way a shell reads them.
     *
     * <p>A pipe ends a stage and starts the next. A redirection takes the word after it, whether it was
     * written against the arrow or apart from it, since both are typed at real shells every day. Only a word that
     * was typed as one of the shell's marks is read as one; any other word is a command's, whatever it says.
     *
     * <p>A pipe with no command on one side and an arrow with no name after it are mistakes, and a shell says
     * so rather than guess what was meant; the line comes back with {@link #badToken()} set to the token a shell
     * would point at.
     */
    public static ShLine of(final List<ShWord> tokens) {
        final List<Stage> stages = new ArrayList<>();
        final List<String> words = new ArrayList<>();
        String from = "";
        String into = "";
        boolean append = false;
        boolean afterPipe = false;
        for (int i = 0; i < tokens.size(); i++) {
            final ShWord token = tokens.get(i);
            if (!token.operator()) {
                words.add(token.text());
                afterPipe = false;
                continue;
            }
            if (token.text().equals("|")) {
                if (words.isEmpty()) {
                    return malformed(stages, from, into, append, "|");
                }
                addStage(stages, words);
                afterPipe = true;
                continue;
            }
            final Redirection redirection = redirectionOf(token.text());
            if (redirection == null) {
                words.add(token.text());
                afterPipe = false;
                continue;
            }
            afterPipe = false;
            String named = redirection.name();
            if (named.isEmpty()) {
                if (i + 1 >= tokens.size()) {
                    return malformed(stages, from, into, append, NEWLINE);
                }
                final ShWord next = tokens.get(++i);
                if (next.operator() && (next.text().equals("|") || redirectionOf(next.text()) != null)) {
                    return malformed(stages, from, into, append, next.text());
                }
                named = next.text();
            }
            if (redirection.reading()) {
                from = named;
            } else {
                into = named;
                append = redirection.append();
            }
        }
        if (afterPipe) {
            return malformed(stages, from, into, append, NEWLINE);
        }
        addStage(stages, words);
        return new ShLine(stages, from, into, append, "");
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

    private static ShLine malformed(final List<Stage> stages, final String from, final String into,
                                    final boolean append, final String badToken) {
        return new ShLine(stages, from, into, append, badToken);
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
