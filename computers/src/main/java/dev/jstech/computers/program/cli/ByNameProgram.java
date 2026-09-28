/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.machine.MachineListing;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.fs.McDosTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jetbrains.annotations.Nullable;

/**
 * Running a listing by its own name at the prompt, exactly as MC-DOS finds a program without its extension and a
 * real shell finds one on its PATH.
 *
 * <p>A listing the machine runs itself needs no runtime installed beside it, so a Vintage machine, which can never
 * hold one, runs everything it compiles all the same: the shell only has to find the file the name stands for and
 * hand it over the same way an installed runtime's own {@code run} verb would have.
 */
public final class ByNameProgram {

    private ByNameProgram() {
    }

    /**
     * The PATH nobody set: MC-DOS's own tools directory and every installed program's own, exactly what
     * {@code AUTOEXEC.BAT} would have put on it; the well-known binary directories of a POSIX system's own tree.
     * Neither family ever had a PATH with nothing on it, so answering with an empty one would be answering for a
     * machine that never existed.
     */
    public static String defaultPath(final ICliComputer computer) {
        if (computer.platform() == Platform.MC_DOS) {
            return dosDefaultPath(computer);
        }
        if (computer.shellFamily() == ShellFamily.POSIX) {
            final StringBuilder path = new StringBuilder();
            for (final String dir : computer.tree().directories()) {
                if (dir.equals("bin") || dir.equals("usr/bin")) {
                    if (!path.isEmpty()) {
                        path.append(':');
                    }
                    path.append('/').append(dir);
                }
            }
            return path.toString();
        }
        return "";
    }

    /**
     * Whether an unknown word here is answered the way this family's own shell answers it (a real Unix shell
     * names the word itself, not a generic "command"), rather than this shell's usual wording.
     */
    static boolean ownsItsWording(final ICliComputer computer) {
        return computer.platform() == Platform.UNIX;
    }

    /**
     * Tries {@code word} as a listing found by name on this machine, the current directory or the PATH depending
     * on the family it speaks; null when nothing here answers to it, which leaves the shell to say the word is
     * unknown in its own way.
     *
     * <p>Declines outright on a machine with no board of its own to run a program on, so a word this shell cannot
     * run is reported unknown rather than answered with a runtime's own refusal.
     */
    @Nullable
    static CliShell.Response tryRun(final String word, final List<String> args, final ICliComputer computer,
                                    final CliOutput out) {
        if (!computer.hasFiles() || !computer.canRunPrograms()) {
            return null;
        }
        final String path = switch (computer.platform()) {
            case MC_DOS -> dosCandidate(word, computer);
            case UNIX -> posixCandidate(word, computer);
            default -> null;
        };
        if (path == null) {
            return null;
        }
        final ICliComputer.OpResult started = computer.startSigma(path, 0, args);
        if (started.ok()) {
            /*
             * A program that takes the terminal says nothing of its own; an empty line would sit under the
             * prompt for no reason, so only a message with something in it becomes a line on the glass.
             */
            if (!started.message().isEmpty()) {
                out.ok(started.message());
            }
        } else {
            out.error(started.message());
        }
        return new CliShell.Response(out.lines(), false);
    }

    /**
     * The directories a bare name is searched in: what the player set with {@code PATH} or {@code set PATH=}, or,
     * with nothing set, this system's own default.
     */
    static List<String> pathDirectories(final ICliComputer computer) {
        final String set = computer.shellVariables().get("PATH");
        final String path = set == null || set.isBlank() ? defaultPath(computer) : set;
        if (path.isBlank()) {
            return List.of();
        }
        final String separator = computer.shellFamily() == ShellFamily.DOS ? ";" : ":";
        final List<String> dirs = new ArrayList<>();
        for (final String dir : path.split(Pattern.quote(separator))) {
            if (!dir.isBlank()) {
                dirs.add(dir);
            }
        }
        return dirs;
    }

    /**
     * The current directory first, then every PATH directory, for the name MC-DOS would have looked for a
     * {@code .EXE} under: the extension is optional, the letters' case is never significant on that filesystem,
     * and the machine's own listings are the only thing a bare name at this prompt can ever mean.
     */
    @Nullable
    private static String dosCandidate(final String word, final ICliComputer computer) {
        final String named = withExtension(word);
        if (computer.readFile(named).ok()) {
            return named;
        }
        final String here = caseMatch(named, computer.fileNames());
        if (here != null) {
            return here;
        }
        for (final String dir : pathDirectories(computer)) {
            final String candidate = dir + "\\" + named;
            if (computer.readFile(candidate).ok()) {
                return candidate;
            }
            final String there = caseMatch(named, fileNamesIn(computer, dir));
            if (there != null) {
                return dir + "\\" + there;
            }
        }
        return null;
    }

    /** The file names (directories left out) directly inside {@code dir}, a DOS path. */
    private static List<String> fileNamesIn(final ICliComputer computer, final String dir) {
        final ICliComputer.FsResult listing = computer.listDisk(dir);
        if (!listing.ok()) {
            return List.of();
        }
        final List<String> names = new ArrayList<>();
        for (final ICliComputer.FsEntry entry : listing.entries()) {
            if (!entry.isDir()) {
                names.add(entry.name());
            }
        }
        return names;
    }

    /** The one of {@code names} that reads the same as {@code named} but for the letters' case, or null. */
    @Nullable
    private static String caseMatch(final String named, final List<String> names) {
        for (final String candidate : names) {
            if (!candidate.endsWith("/") && candidate.equalsIgnoreCase(named)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * MC-DOS's own PATH, read straight from the machine's own {@code AUTOEXEC.BAT}: the very file the system
     * writes the search path into whenever a program is installed, so a listing found this way is exactly what
     * {@code echo %PATH%} would have found too. A machine kept for a test, with no {@code AUTOEXEC.BAT} of its
     * own, falls back to the tools directory every MC-DOS ships with.
     */
    private static String dosDefaultPath(final ICliComputer computer) {
        final ICliComputer.FsResult autoexec = computer.readFile("C:\\" + McDosTree.AUTOEXEC);
        if (autoexec.ok()) {
            final String prefix = "PATH ";
            for (final String line : autoexec.message().english().split("\n")) {
                if (line.regionMatches(true, 0, prefix, 0, prefix.length())) {
                    return line.substring(prefix.length()).trim();
                }
            }
        }
        return "C:\\" + McDosTree.DOS;
    }

    /**
     * A name with a slash in it is a path, resolved exactly where it is written, whether that is {@code ./hello}
     * or an absolute one; a bare name is looked for only on the PATH, never in the current directory, since a real
     * shell never runs a program standing beside it unless it is asked to by that path.
     */
    @Nullable
    private static String posixCandidate(final String word, final ICliComputer computer) {
        final String named = withExtension(word);
        if (named.indexOf('/') >= 0) {
            return computer.readFile(named).ok() ? named : null;
        }
        for (final String dir : pathDirectories(computer)) {
            final String candidate = dir + "/" + named;
            if (computer.readFile(candidate).ok()) {
                return candidate;
            }
        }
        return null;
    }

    /** The name a word is looked for under: itself, when it already names the extension, or with it added. */
    private static String withExtension(final String word) {
        final String extension = "." + MachineListing.EXTENSION;
        return word.toLowerCase(Locale.ROOT).endsWith(extension) ? word : word + extension;
    }
}
