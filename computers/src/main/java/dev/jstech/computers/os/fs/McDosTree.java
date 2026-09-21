/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What the root of an MC-DOS disk holds, and what its files say.
 *
 * <p>MC-DOS keeps itself at the root of the disk, as a DOS did: the system file, the command processor and the
 * two files it reads on the way up, with the rest of what it ships in a {@code DOS} directory. A program has a
 * directory of its own at the root, named the way that filesystem named things, and the search path in
 * {@code AUTOEXEC.BAT} lists exactly those directories, so the file says what is installed and changes when
 * that does.
 *
 * <p>Pure, with no Minecraft types in it, so the tree is unit-tested; the binding hands it what it found on
 * the machine.
 */
public final class McDosTree {

    /** The directory the system's own tools live in. */
    public static final String DOS = "DOS";

    public static final String AUTOEXEC = "AUTOEXEC.BAT";
    public static final String CONFIG = "CONFIG.SYS";
    public static final String COMMAND = "COMMAND.COM";
    public static final String MEMORY_DRIVER = DOS + "/HIMEM.SYS";
    public static final String README = DOS + "/README.TXT";

    private McDosTree() {
    }

    /**
     * What the tree is made from.
     *
     * @param systemName what the system is called
     * @param copyright  the line its maker signs it with
     * @param systemFile the file the system itself is, at the root
     * @param programs   the directory of each installed program, in the order they were installed
     */
    public record Facts(String systemName, String copyright, String systemFile, List<String> programs) {

        public Facts {
            programs = programs == null ? List.of() : List.copyOf(programs);
        }
    }

    /** The name a program's directory and its executable take: eight capitals at most, as that filesystem had. */
    public static String nameOf(final String command) {
        return InstallerLayout.dosName(command);
    }

    /** Every folder and file of the tree, folders before what is in them. */
    public static List<InstallerLayout.Entry> entries(final Facts facts) {
        final List<InstallerLayout.Entry> out = new ArrayList<>();
        out.add(new InstallerLayout.Entry(DOS, FileType.TXT, true));
        for (final String program : facts.programs()) {
            out.add(new InstallerLayout.Entry(program, FileType.TXT, true));
        }
        out.add(new InstallerLayout.Entry(AUTOEXEC, FileType.INI, false));
        out.add(new InstallerLayout.Entry(COMMAND, FileType.BIN, false));
        out.add(new InstallerLayout.Entry(CONFIG, FileType.INI, false));
        out.add(new InstallerLayout.Entry(facts.systemFile(), FileType.SYS, false));
        out.add(new InstallerLayout.Entry(MEMORY_DRIVER, FileType.SYS, false));
        out.add(new InstallerLayout.Entry(README, FileType.TXT, false));
        for (final String program : facts.programs()) {
            out.add(new InstallerLayout.Entry(program + "/" + program + ".EXE", FileType.EXE, false));
            out.add(new InstallerLayout.Entry(readmeOf(program), FileType.TXT, false));
        }
        return out;
    }

    /** Where a program's own notes are, inside its directory. */
    public static String readmeOf(final String program) {
        return program + "/README.TXT";
    }

    /** The text of one of the system's own files, or empty for one that is not text or is not the system's. */
    public static Optional<String> text(final Facts facts, final String path) {
        return switch (path) {
            case AUTOEXEC -> Optional.of(autoexec(facts));
            case CONFIG -> Optional.of("DEVICE=C:\\DOS\\HIMEM.SYS\nDOS=HIGH\nFILES=30\nBUFFERS=20\n");
            case README -> Optional.of(facts.systemName() + "\n" + facts.copyright() + "\n\n"
                    + "The system is " + facts.systemFile() + " and " + COMMAND + ", at the root of this disk.\n"
                    + CONFIG + " and " + AUTOEXEC + " are read when it starts.\n\n"
                    + "Each program you install gets a directory of its own at the root,\n"
                    + "and " + AUTOEXEC + " adds that directory to the PATH.\n\n"
                    + "Type HELP for the commands.\n");
            default -> Optional.empty();
        };
    }

    /** The startup file, whose search path is the system's tools and then every program's directory. */
    private static String autoexec(final Facts facts) {
        final StringBuilder path = new StringBuilder("C:\\").append(DOS);
        for (final String program : facts.programs()) {
            path.append(";C:\\").append(program);
        }
        return "@ECHO OFF\nPROMPT $P$G\nPATH " + path + "\n";
    }
}
