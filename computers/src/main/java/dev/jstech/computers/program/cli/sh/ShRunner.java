/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.sh;

import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.cli.CliContext;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliOutput;
import dev.jstech.computers.program.cli.CliShell;
import dev.jstech.computers.program.cli.ICliCommand;
import dev.jstech.computers.program.cli.ICliComputer;
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
public final class ShRunner {

    /** How many lines one stage may hand to the next, so a pipeline cannot be made to fill a machine. */
    private static final int MOST_PIPED_LINES = 4096;

    private ShRunner() {
    }

    /**
     * Puts in what the names stand for, and opens out the words with stars in them.
     *
     * <p>Each family in its own way, because they really were different: a Unix shell reads {@code $HOME} and
     * opens out {@code *.sgs} before the command ever sees it, and a DOS one reads {@code %HOME%} and hands
     * the star to the command, which is why {@code DEL *.TXT} was the command's own doing.
     */
    public static List<String> expand(final ICliComputer computer, final List<String> tokens) {
        final boolean dos = computer.shellFamily() != ShellFamily.POSIX;
        final Map<String, String> named = namesOf(computer);
        final List<String> out = new ArrayList<>(tokens.size());
        List<String> names = null;
        for (final String token : tokens) {
            final String filled = ShWords.expand(token, named, dos);
            if (dos || (filled.indexOf('*') < 0 && filled.indexOf('?') < 0)) {
                out.add(filled);
                continue;
            }
            if (names == null) {
                // Asked for once per line, and only by a line that has a star in it.
                names = computer.fileNames();
            }
            out.addAll(ShWords.glob(filled, names));
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
        List<String> feeding = new ArrayList<>();
        if (!line.from().isEmpty()) {
            final ICliComputer.FsResult read = computer.readFile(line.from());
            if (!read.ok()) {
                out.error(read.message());
                return new CliShell.Response(out.lines(), false);
            }
            feeding = linesOf(read.message());
        }
        List<CliLine> printed = List.of();
        for (int i = 0; i < line.stages().size(); i++) {
            final ShLine.Stage stage = line.stages().get(i);
            final ICliCommand command = shell.find(stage.word());
            if (command == null || !command.available(computer)) {
                out.error("command not found: " + stage.word());
                return new CliShell.Response(out.lines(), false);
            }
            final CliOutput page = new CliOutput(out.width());
            try {
                command.run(new CliContext(stage.args(), computer, page, shell, feeding));
            } catch (final RuntimeException unexpected) {
                out.error("error running '" + stage.word() + "': " + unexpected.getMessage());
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
        return new CliShell.Response(out.lines(), false);
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

    /**
     * The names a shell knows: where it is, who it is and what it runs, and then whatever the player set.
     *
     * <p>What was set wins, because a shell that would not let you say what {@code HOME} means is a shell
     * arguing with the person typing at it.
     */
    private static Map<String, String> namesOf(final ICliComputer computer) {
        final Map<String, String> named = builtIn(computer);
        named.putAll(computer.shellVariables());
        return named;
    }

    /** The ones nobody set, which the machine answers for out of what it is. */
    private static Map<String, String> builtIn(final ICliComputer computer) {
        final Map<String, String> named = new LinkedHashMap<>();
        final boolean dos = computer.shellFamily() != ShellFamily.POSIX;
        named.put("HOSTNAME", computer.hostname());
        named.put("COMPUTERNAME", computer.hostname());
        named.put("USER", "player");
        named.put("USERNAME", "player");
        named.put("OS", computer.platform().label());
        if (dos) {
            named.put("CD", computer.prompt());
        } else {
            named.put("HOME", computer.tree().homePath());
            named.put("PWD", computer.currentLocation().storagePath());
        }
        return named;
    }

    /** What a command printed, as plain lines, which is what the next one is handed. */
    private static List<String> textOf(final List<CliLine> lines) {
        final List<String> text = new ArrayList<>(Math.min(lines.size(), MOST_PIPED_LINES));
        for (final CliLine line : lines) {
            if (text.size() >= MOST_PIPED_LINES) {
                break;
            }
            text.add(line.text());
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
