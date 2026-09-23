/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.program.cli.DosPath;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Where a machine's shells stand: the command line's current drive and the directory it keeps on each drive, and
 * where each terminal window's shell has gone.
 *
 * <p>Kept in memory, like the session of a real terminal: a machine that is loaded again starts at its boot drive's
 * root. Each drive remembers its own directory, so switching back to a drive returns to where it was left.
 */
public final class ShellLocations {

    private char drive = 'C';
    private final Map<Character, String> dirs = new LinkedHashMap<>();
    /**
     * Where each terminal window's shell is, by session number. Every terminal window is a shell of its own, so a
     * {@code cd} in one leaves the others where they were; a session lives as long as its window.
     */
    private final Map<Integer, Spot> sessions = new LinkedHashMap<>();

    /** Where one terminal window's shell is: its drive and directory. */
    public record Spot(char drive, String dir) {
    }

    /** The command line's current drive letter (upper-cased). */
    public char drive() {
        return this.drive;
    }

    /** The current directory of the current drive as a {@code '/'}-separated storage path; {@code ""} is the root. */
    public String dir() {
        return this.dirs.getOrDefault(this.drive, "");
    }

    /** Whether the command line has explicitly set a directory on the current drive (false: a fresh session). */
    public boolean moved() {
        return this.dirs.containsKey(this.drive);
    }

    /** Switches the current drive, which comes back to the directory it keeps. */
    public void setDrive(final char letter) {
        this.drive = Character.toUpperCase(letter);
    }

    /** Sets the current drive and the directory it keeps. */
    public void set(final char letter, final String dir) {
        this.drive = Character.toUpperCase(letter);
        this.dirs.put(this.drive, dir == null ? "" : dir);
    }

    /** Where that terminal window's shell is, or null when it has not moved from the command line's spot. */
    @Nullable
    public Spot session(final int session) {
        return this.sessions.get(session);
    }

    public void setSession(final int session, final char letter, final String dir) {
        this.sessions.put(session, new Spot(Character.toUpperCase(letter), dir == null ? "" : dir));
    }

    /**
     * Where a shell stands: a terminal window that has moved with {@code cd} is where it went, whatever the other
     * windows and the full-screen prompt are doing; one that has not is wherever the command line is.
     *
     * @param session the terminal window's session, or 0 for the machine's own prompt
     * @param home    where a fresh session starts, for a shell that starts at home rather than at the drive root;
     *                null for one that starts at the root. Once the player has changed directory the stored
     *                location wins, so {@code cd /} really lands on the root.
     */
    public DosPath.Location where(final int session, @Nullable final DosPath.Location home) {
        final Spot spot = session == 0 ? null : this.sessions.get(session);
        if (spot != null) {
            return new DosPath.Location(spot.drive(), segments(spot.dir()));
        }
        if (home != null && !this.moved() && this.drive == 'C') {
            return home;
        }
        return new DosPath.Location(this.drive, segments(this.dir()));
    }

    /** Moves a shell: a terminal window's, or the command line's for session 0. */
    public void moveTo(final int session, final DosPath.Location location) {
        if (session != 0) {
            this.setSession(session, location.drive(), location.storagePath());
        } else {
            this.set(location.drive(), location.storagePath());
        }
    }

    /** Back to the boot drive's root, every window included. */
    public void clear() {
        this.drive = 'C';
        this.dirs.clear();
        this.sessions.clear();
    }

    private static List<String> segments(final String dir) {
        return dir.isEmpty() ? List.of() : List.of(dir.split("/"));
    }
}
