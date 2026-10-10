/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.sh;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.cli.ByNameProgram;
import dev.jstech.computers.program.cli.CliContext;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliOutput;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.CliTokenizer;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.core.text.Glob;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Running a whole line: the commands in it one after another, each handed what the one before printed, and the
 * last one's words put where the line said to put them.
 *
 * <p>The commands themselves know nothing of this. One writes lines and may read the ones it was handed; that
 * two of them were written with a pipe between them is the shell's business, which is exactly how it has always
 * been and why every command works in a pipeline without being taught to.
 */
@TextHolder
public final class ShRunner {

    /** How many lines one stage may hand to the next, so a pipeline cannot be made to fill a machine. */
    private static final int MOST_PIPED_LINES = 4096;

    private static final TextKey WHOLE_TERMINAL = TextKey.of("jsc.cli.sh.whole_terminal",
            "takes the whole terminal, so it cannot be piped or redirected");

    private ShRunner() {
    }

    /**
     * Puts in what the names stand for, and opens out the words with stars in them.
     *
     * <p>Each family in its own way, because they really were different: a Unix shell reads {@code $HOME} and
     * opens out {@code *.sgs} before the command ever sees it, and a DOS one reads {@code %HOME%} and hands
     * the star to the command, which is why {@code DEL *.TXT} was the command's own doing.
     */
    public static List<ShWord> expand(final ICliComputer computer, final List<CliTokenizer.Token> tokens) {
        /*
         * Only the DOS family writes a name between per cent signs. The network appliance says it the other
         * way, with the rest of its lowercase habits, so the test is for that family rather than for
         * anything that is not Unix.
         */
        final boolean dos = computer.shellFamily() == ShellFamily.DOS;
        /*
         * A DOS machine's own PATH is read from its disk, so building it costs a real seek; every other name
         * here is free. Asking for it only when a token could actually stand for it keeps a line with nothing
         * between per cent signs, such as "cls" or "ver", from touching the disk at all.
         */
        final boolean needsPath = dos && holdsPercent(tokens);
        final Map<String, String> named = namesOf(computer, needsPath);
        final List<ShWord> out = new ArrayList<>(tokens.size());
        // Each folder a star looks into is listed once a line, and only for a line that has a star in it.
        final Map<String, List<String>> listed = new LinkedHashMap<>();
        for (final CliTokenizer.Token token : tokens) {
            final ShWord typed = ShWord.typed(token);
            final String filled = ShWords.expand(token.text(), named, dos);
            if (typed.operator()) {
                // What a mark was is settled; only the name written against an arrow is put in.
                out.add(new ShWord(filled, true));
            } else if (dos || token.quoted() || !Glob.isPattern(filled, true)) {
                out.add(ShWord.plain(filled));
            } else {
                for (final String match : globbed(computer, filled, listed)) {
                    out.add(ShWord.plain(match));
                }
            }
        }
        return out;
    }

    /**
     * Runs a line that is more than one command, or that reads or writes a file.
     *
     * <p>Each stage is given a page of its own to write on. What it wrote is what the next one reads, and what
     * the last one wrote goes to the glass or into the file the line named.
     */
    public static CliShell.Response run(final CliShell shell, final ShLine line, final ICliComputer computer,
                                        final CliOutput out) {
        /*
         * A command that takes the whole terminal has nothing to hand along a pipe or into a file. Run here it
         * printed nothing and the terminal stayed where it was, and a redirection then emptied the file it named,
         * so it is refused before anything on the line runs.
         */
        for (final ShLine.Stage stage : line.stages()) {
            final ICliCommand command = shell.find(stage.word());
            if (command instanceof CliShell.IHandOver giving && command.available(computer)
                    && giving.fileOf(computer, stage.args()) != null) {
                out.error(CliTexts.SAID_BY.with(stage.word(), WHOLE_TERMINAL));
                return new CliShell.Response(out.lines(), false);
            }
        }
        List<String> feeding = new ArrayList<>();
        if (!line.from().isEmpty()) {
            final ICliComputer.FsResult read = computer.readFile(line.from());
            if (!read.ok()) {
                out.error(read.message());
                return new CliShell.Response(out.lines(), false);
            }
            feeding = linesOf(read.message().english());
        }
        List<CliLine> printed = List.of();
        ICliCommand last = null;
        for (int i = 0; i < line.stages().size(); i++) {
            final ShLine.Stage stage = line.stages().get(i);
            final ICliCommand command = shell.find(stage.word());
            last = command;
            if (command == null || !command.available(computer)) {
                out.error(CliTexts.NOT_FOUND.with(stage.word()));
                return new CliShell.Response(out.lines(), false);
            }
            final CliOutput page = new CliOutput(out.width());
            try {
                command.run(new CliContext(stage.args(), computer, page, shell, feeding));
            } catch (final RuntimeException unexpected) {
                out.error(CliTexts.FAILED.with(stage.word(), String.valueOf(unexpected.getMessage())));
                return new CliShell.Response(out.lines(), false);
            }
            printed = page.lines();
            feeding = textOf(printed);
        }
        if (line.writes()) {
            return written(computer, line, out, feeding);
        }
        for (final CliLine printedLine : printed) {
            out.line(printedLine);
        }
        // A line that ends in clearing the screen clears it, piped into or not, as it does on its own.
        return new CliShell.Response(out.lines(), last instanceof CliShell.IClearMarker);
    }

    /** Puts what the last command printed into the file the line named, and says what came of that. */
    private static CliShell.Response written(final ICliComputer computer, final ShLine line, final CliOutput out,
                                             final List<String> text) {
        final String content = String.join("\n", text);
        final ICliComputer.FsResult wrote = line.append()
                ? computer.appendFile(line.into(), content)
                : computer.writeFile(line.into(), content);
        if (!wrote.ok()) {
            out.error(wrote.message());
        }
        return new CliShell.Response(out.lines(), false);
    }

    /** Whether any word on the line holds a per cent sign, the only way a DOS line ever names %PATH%. */
    private static boolean holdsPercent(final List<CliTokenizer.Token> tokens) {
        for (final CliTokenizer.Token token : tokens) {
            if (token.text().indexOf('%') >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * The names a word with a star in it opens out into, or the word itself when it matches nothing.
     *
     * <p>A star stands only in the last part of a path, as it does in most of what anybody types: {@code data/*.sgs}
     * is matched against what {@code data} holds, and each match keeps the folder in front of it.
     */
    private static List<String> globbed(final ICliComputer computer, final String word,
                                        final Map<String, List<String>> listed) {
        final int slash = word.lastIndexOf('/');
        final String folder = slash < 0 ? "" : word.substring(0, slash + 1);
        final String last = word.substring(slash + 1);
        if (Glob.isPattern(folder, true) || last.isEmpty()) {
            return List.of(word);
        }
        final List<String> names = listed.computeIfAbsent(folder,
                at -> at.isEmpty() ? computer.fileNames() : computer.fileNames(at));
        // A pattern that matches nothing comes back as itself, which with its folder in front is the word as typed.
        final List<String> matched = ShWords.glob(last, names);
        if (folder.isEmpty()) {
            return matched;
        }
        final List<String> whole = new ArrayList<>(matched.size());
        for (final String name : matched) {
            whole.add(folder + name);
        }
        return whole;
    }

    /**
     * The names a shell knows: where it is, who it is and what it runs, and then whatever the player set.
     *
     * <p>What was set wins, because a shell that would not let you say what {@code HOME} means is a shell
     * arguing with the person typing at it.
     */
    private static Map<String, String> namesOf(final ICliComputer computer, final boolean needsPath) {
        final Map<String, String> named = builtIn(computer, needsPath);
        named.putAll(computer.shellVariables());
        return named;
    }

    /** The ones nobody set, which the machine answers for out of what it is. */
    private static Map<String, String> builtIn(final ICliComputer computer, final boolean needsPath) {
        final Map<String, String> named = new LinkedHashMap<>();
        named.put("HOSTNAME", computer.hostname());
        named.put("COMPUTERNAME", computer.hostname());
        named.put("USER", "player");
        named.put("USERNAME", "player");
        named.put("OS", computer.platform().label());
        switch (computer.shellFamily()) {
            case DOS -> {
                named.put("CD", computer.prompt());
                if (needsPath) {
                    named.put("PATH", ByNameProgram.defaultPath(computer));
                }
            }
            case POSIX -> {
                named.put("HOME", computer.tree().homePath());
                named.put("PWD", computer.currentLocation().storagePath());
                /*
                 * Only the family a bare name is ever searched for on gets a PATH built this way; a FreeBSD or a
                 * Linux installs its programs where its own tree says, not under this bare "bin"/"usr/bin" guess.
                 * Nothing here reads a disk to build it, so it costs nothing to always answer for it.
                 */
                if (computer.platform() == Platform.UNIX) {
                    named.put("PATH", ByNameProgram.defaultPath(computer));
                }
            }
            /* A flat disk has no folder to be standing in and no home, so it answers for neither. */
            case NET -> { }
        }
        return named;
    }

    /**
     * What a command printed, as plain lines in the machine's language, which is what the next one is handed. A line
     * whose words break over several lines is several lines, as it is on the glass.
     */
    private static List<String> textOf(final List<CliLine> lines) {
        final List<String> text = new ArrayList<>(Math.min(lines.size(), MOST_PIPED_LINES));
        for (final CliLine line : lines) {
            for (final String one : line.text().split("\r?\n", -1)) {
                if (text.size() >= MOST_PIPED_LINES) {
                    return text;
                }
                text.add(one);
            }
        }
        return text;
    }

    private static List<String> linesOf(final String content) {
        final List<String> lines = new ArrayList<>();
        for (final String line : content.split("\r?\n", -1)) {
            if (lines.size() >= MOST_PIPED_LINES) {
                break;
            }
            lines.add(line);
        }
        return lines;
    }
}
