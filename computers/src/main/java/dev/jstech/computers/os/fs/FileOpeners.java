/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Which programs can open a kind of file, and which of them does it by default.
 *
 * <p>A desktop that opens everything in the text editor is a desktop with one program in it. Saying
 * this out loud, once, is what lets a double-click reach the right program, what lets a player pick a
 * different one, and what lets the machine offer the kinds of file it can actually make.
 *
 * <p>What a machine really has is a separate question, answered by the caller: this says what could
 * open a file, and the caller keeps only what is installed. That way a machine with no editor on it
 * still says honestly that a source file is a thing an editor opens.
 */
public final class FileOpeners {

    /** The plain text editor every desktop ships, and the last resort for anything readable. */
    public static final String EDITOR = "editor";

    /**
     * The program runtime, which opens a compiled program by running it.
     *
     * <p>Running is what opening a compiled program means, so it comes first for one; the editors
     * that can read the listing follow, for a player who wants to look inside.
     */
    public static final String RUNTIME = "sigma";

    /**
     * The programs that can open each kind of file, best first.
     *
     * <p>Best means the one a player most likely wants: a source file opens in the editor written for
     * the language rather than in the one that treats it as text, though that one can still open it.
     */
    private static final Map<FileType, List<String>> BY_TYPE = new LinkedHashMap<>();

    static {
        final List<String> code = List.of("virtual_studio_code", "virtual_studio", "exposure", EDITOR);
        BY_TYPE.put(FileType.SGS, code);
        // The smaller language opens in the same editors: an old language is no reason for a worse one.
        BY_TYPE.put(FileType.SG, code);
        BY_TYPE.put(FileType.ASM, List.of(RUNTIME, "virtual_studio_code", "virtual_studio", "exposure", EDITOR));
        BY_TYPE.put(FileType.SLN, List.of("virtual_studio", EDITOR));
        BY_TYPE.put(FileType.SGSPROJ, List.of("virtual_studio", EDITOR));
        BY_TYPE.put(FileType.SGPROJ, List.of("virtual_studio", EDITOR));
        BY_TYPE.put(FileType.IQL, List.of("nms", EDITOR));
        BY_TYPE.put(FileType.CRAFT, List.of("crafting_manager"));
        /*
         * An archive opens in the archiver and a picture in the paint program, the way the file each of
         * them writes is the file it reads back. Leaving them unclaimed meant a double-click on one did
         * nothing at all and Open with offered no program for it, which looked like the file being broken.
         */
        BY_TYPE.put(FileType.ARK, List.of("ark"));
        BY_TYPE.put(FileType.PIX, List.of("paint"));
        BY_TYPE.put(FileType.TXT, List.of(EDITOR));
        BY_TYPE.put(FileType.CFG, List.of(EDITOR));
        BY_TYPE.put(FileType.CSV, List.of(EDITOR));
        BY_TYPE.put(FileType.CMD, List.of(EDITOR));
        BY_TYPE.put(FileType.LOG, List.of(EDITOR));
    }

    /**
     * The kinds of file a player can make from nothing, in the order a menu offers them.
     *
     * <p>Only the ones that mean anything empty: a pattern is written by the machine that encodes it and
     * a compiled program by the compiler, so neither belongs on a menu that creates a blank one.
     */
    private static final List<FileType> CREATABLE =
            List.of(FileType.TXT, FileType.SGS, FileType.SG, FileType.IQL, FileType.CFG, FileType.CSV, FileType.CMD);

    /**
     * The programs that open a file of any kind that is text, best first: the plain editor, which reads anything,
     * then the code editors. They are what Open with offers for a kind nobody claims, and the others it can offer
     * for a kind that is text. A program an addon brings that opens files adds itself with
     * {@link #registerAnyFileOpener}.
     */
    private static final List<String> ANY_FILE = new CopyOnWriteArrayList<>(
            List.of(EDITOR, "virtual_studio_code", "virtual_studio", "exposure"));

    private FileOpeners() {
    }

    /** The kinds of file a player can create, in the order to offer them. */
    public static List<FileType> creatable() {
        return CREATABLE;
    }

    /**
     * Every program that could open {@code path}, best first, whether or not the machine has it.
     *
     * <p>A kind the machines know but give no program opens in nothing rather than falling back to the text
     * editor. That is not a gap: a {@code .dat} is a read-only view of what a drive is holding and a {@code .exe}
     * is a program, and handing either to an editor would offer to edit something that cannot be edited. A kind
     * the machines do not know at all is text, so everything that opens any file can open it.
     */
    public static List<String> forPath(final String path) {
        final FileType type = typeOf(path);
        if (type == null) {
            return List.of();
        }
        return type == FileType.OTHER ? List.copyOf(ANY_FILE) : BY_TYPE.getOrDefault(type, List.of());
    }

    /**
     * Every program the player can pick for {@code path} on a machine that has {@code installed}, best first: the
     * ones for its kind, then, for a kind that is text, the rest of the ones that open any file. It is what Choose
     * another program offers, so a text file can go to a code editor as well as to the plain one.
     */
    public static List<String> choices(final String path, final List<String> installed) {
        final List<String> out = new ArrayList<>(available(path, installed));
        final FileType type = typeOf(path);
        if (type != null && type.userEditable()) {
            for (final String program : ANY_FILE) {
                if (!out.contains(program) && (program.equals(EDITOR) || installed.contains(program))) {
                    out.add(program);
                }
            }
        }
        return out;
    }

    /** Adds a program that opens a file of any kind to the ones Open with offers; nothing when it is there already. */
    public static void registerAnyFileOpener(final String programId) {
        if (programId != null && !programId.isBlank() && !ANY_FILE.contains(programId)) {
            ANY_FILE.add(programId);
        }
    }

    /**
     * The programs that can open {@code path} on a machine that has {@code installed}, best first.
     *
     * <p>The plain editor is always there, so a readable file is never one a machine cannot open; a file
     * whose only opener is missing comes back empty, which is the honest answer and the one that lets a
     * caller say so instead of opening the wrong thing.
     */
    public static List<String> available(final String path, final List<String> installed) {
        final List<String> out = new ArrayList<>();
        for (final String program : forPath(path)) {
            if (program.equals(EDITOR) || installed.contains(program)) {
                out.add(program);
            }
        }
        return out;
    }

    /**
     * The program that opens {@code path} by default on such a machine, or empty when none can. A kind the
     * machines do not know has no default: the player is asked which program to use.
     */
    public static String defaultFor(final String path, final List<String> installed) {
        if (isUnknownKind(path)) {
            return "";
        }
        final List<String> can = available(path, installed);
        return can.isEmpty() ? "" : can.getFirst();
    }

    /**
     * The program that opens {@code path} on such a machine, given {@code chosen}, the program picked with Always for
     * each extension: that one while the machine can still open the file with it, otherwise the default for the
     * file's kind.
     */
    public static String defaultFor(final String path, final List<String> installed,
                                    final Map<String, String> chosen) {
        final String picked = chosen == null ? "" : programOf(chosen.get(extensionOf(path)));
        if (!picked.isEmpty() && choices(path, installed).contains(picked)) {
            return picked;
        }
        return defaultFor(path, installed);
    }

    /** Whether {@code path} is a file of a kind the machines do not know, which a double-click asks about. */
    public static boolean isUnknownKind(final String path) {
        return typeOf(path) == FileType.OTHER;
    }

    /** The extension of a path's file name, in lower case and without the dot; empty when it has none. */
    public static String extensionOf(final String path) {
        if (path == null) {
            return "";
        }
        final String name = path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1);
        final int dot = name.lastIndexOf('.');
        return dot < 0 || dot == name.length() - 1 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** A program id as the desktop names programs, from one written down with or without the series' namespace. */
    private static String programOf(final String saved) {
        if (saved == null) {
            return "";
        }
        return saved.startsWith("jsc:") ? saved.substring("jsc:".length()) : saved;
    }

    /** The type of a path, or null when there is no path at all. */
    private static FileType typeOf(final String path) {
        return path == null ? null : FileType.of(extensionOf(path));
    }
}
