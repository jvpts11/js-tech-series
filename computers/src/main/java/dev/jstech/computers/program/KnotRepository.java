/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * What a source repository keeps: a file's revisions, who wrote each of them, and what changed.
 *
 * <p>It sits under the package manager rather than beside it. A package is a finished thing one player
 * hands to another; this is the source while it is still being argued over, which is a different job and
 * the reason both exist.
 *
 * <p>Every revision is kept whole rather than as what changed from the one before it. That is the more
 * expensive of the two and it is chosen on purpose: a history bounded at sixty-four revisions of files
 * this size is a few kilobytes either way, and keeping them whole means a damaged revision costs one
 * revision rather than every one after it. What it costs is reported honestly, so the machine holding the
 * repository pays for it in disk space the player can see.
 *
 * <p>This class is pure and carries no Minecraft dependency.
 */
public final class KnotRepository {

    /** How many revisions one file keeps before the oldest start falling off the end. */
    public static final int MAX_REVISIONS = 64;

    /** How many files one repository holds. */
    public static final int MAX_FILES = 64;

    /** What one revision costs beyond its own text, for the author, the message and the time. */
    private static final int OVERHEAD_BYTES = 48;

    /** One saved state of a file. */
    public record Revision(int number, String author, String message, long at, String content) {

        public Revision {
            if (number < 1) {
                throw new IllegalArgumentException("revisions are numbered from one");
            }
            if (author == null || author.isBlank()) {
                throw new IllegalArgumentException("a revision has somebody who made it");
            }
            if (content == null) {
                throw new IllegalArgumentException("a revision has content, even if none");
            }
        }

        /** The name it is listed under. */
        public String label() {
            return "r" + number;
        }
    }

    /** One line of a comparison between two revisions. */
    public record DiffLine(Kind kind, String text) {

        /** Whether a line was added, taken away, or is only there for company. */
        public enum Kind { ADDED, REMOVED, CONTEXT }
    }

    private final String name;
    private final List<String> files = new ArrayList<>();
    private final List<List<Revision>> history = new ArrayList<>();
    private int nextNumber = 1;

    public KnotRepository(final String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("a repository has a name");
        }
        this.name = name.toLowerCase(Locale.ROOT);
    }

    public String name() {
        return name;
    }

    /** Every file this repository is keeping, in the order they were first committed. */
    public List<String> files() {
        return List.copyOf(files);
    }

    /**
     * Saves a file as it stands now.
     *
     * <p>Answers the revision made, or null when nothing had changed since the last one: committing the
     * same text twice makes a history nobody can read.
     */
    public Revision commit(final String file, final String author, final String message,
                           final String content, final long at) {
        if (file == null || file.isBlank() || content == null) {
            return null;
        }
        final int index = indexOf(file);
        if (index < 0 && files.size() >= MAX_FILES) {
            return null;
        }
        final List<Revision> revisions = index < 0 ? new ArrayList<>() : history.get(index);
        if (!revisions.isEmpty() && revisions.get(revisions.size() - 1).content().equals(content)) {
            return null;
        }
        final Revision revision = new Revision(nextNumber++, author,
                message == null || message.isBlank() ? "no message" : message, at, content);
        revisions.add(revision);
        while (revisions.size() > MAX_REVISIONS) {
            revisions.remove(0);
        }
        if (index < 0) {
            files.add(file);
            history.add(revisions);
        }
        return revision;
    }

    /** What a file looked like at its newest revision, or null when it is not in here. */
    public String head(final String file) {
        final List<Revision> revisions = revisionsOf(file);
        return revisions.isEmpty() ? null : revisions.get(revisions.size() - 1).content();
    }

    /** Every revision of a file, oldest first. */
    public List<Revision> revisionsOf(final String file) {
        final int index = indexOf(file);
        return index < 0 ? List.of() : List.copyOf(history.get(index));
    }

    /** Every revision of every file, newest first, which is what a history panel lists. */
    public List<Revision> recent(final int limit) {
        final List<Revision> out = new ArrayList<>();
        for (final List<Revision> revisions : history) {
            out.addAll(revisions);
        }
        out.sort((a, b) -> Integer.compare(b.number(), a.number()));
        return out.size() > limit ? List.copyOf(out.subList(0, limit)) : List.copyOf(out);
    }

    /** Which file a revision number belongs to, or null when no file has it. */
    public String fileOf(final int number) {
        for (int i = 0; i < files.size(); i++) {
            for (final Revision revision : history.get(i)) {
                if (revision.number() == number) {
                    return files.get(i);
                }
            }
        }
        return null;
    }

    /** One revision by its number, or null when there is no such one. */
    public Revision revision(final int number) {
        for (final List<Revision> revisions : history) {
            for (final Revision revision : revisions) {
                if (revision.number() == number) {
                    return revision;
                }
            }
        }
        return null;
    }

    /** What this repository weighs, which is what the service reports to the machine holding it. */
    public long bytes() {
        long total = 0L;
        for (final List<Revision> revisions : history) {
            for (final Revision revision : revisions) {
                total += revision.content().getBytes(StandardCharsets.UTF_8).length
                        + revision.message().getBytes(StandardCharsets.UTF_8).length + OVERHEAD_BYTES;
            }
        }
        return total;
    }

    /** Takes a whole history back, for a repository being read off a disk. */
    public void restore(final String file, final List<Revision> revisions) {
        if (file == null || revisions == null || revisions.isEmpty()) {
            return;
        }
        final int index = indexOf(file);
        final List<Revision> kept = new ArrayList<>(revisions);
        if (index < 0) {
            files.add(file);
            history.add(kept);
        } else {
            history.set(index, kept);
        }
        /*
         * Held below the largest a number holds, because a revision read back from a file somebody edited
         * could name one right at it, and the next number after that wraps round to a negative one, which
         * the next commit would then refuse by throwing rather than by answering.
         */
        for (final Revision revision : kept) {
            nextNumber = Math.max(nextNumber, Math.min(Integer.MAX_VALUE - 1, revision.number()) + 1);
        }
    }

    /**
     * What changed between two texts, line by line.
     *
     * <p>The simplest comparison that tells the truth: a line in both is context, a line only in the older
     * one went away, a line only in the newer one arrived. It is not the shortest possible list of
     * changes, and it does not pretend to be; it is the one a reader can follow.
     */
    public static List<DiffLine> diff(final String before, final String after) {
        final String[] old = (before == null ? "" : before).split("\n", -1);
        final String[] now = (after == null ? "" : after).split("\n", -1);
        final int[][] common = longestCommon(old, now);
        final List<DiffLine> out = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < old.length && j < now.length) {
            if (old[i].equals(now[j])) {
                out.add(new DiffLine(DiffLine.Kind.CONTEXT, old[i]));
                i++;
                j++;
            } else if (common[i + 1][j] >= common[i][j + 1]) {
                out.add(new DiffLine(DiffLine.Kind.REMOVED, old[i]));
                i++;
            } else {
                out.add(new DiffLine(DiffLine.Kind.ADDED, now[j]));
                j++;
            }
        }
        while (i < old.length) {
            out.add(new DiffLine(DiffLine.Kind.REMOVED, old[i++]));
        }
        while (j < now.length) {
            out.add(new DiffLine(DiffLine.Kind.ADDED, now[j++]));
        }
        return out;
    }

    /** The table behind the comparison: how many lines the two still have in common from each point on. */
    private static int[][] longestCommon(final String[] old, final String[] now) {
        final int[][] table = new int[old.length + 1][now.length + 1];
        for (int i = old.length - 1; i >= 0; i--) {
            for (int j = now.length - 1; j >= 0; j--) {
                table[i][j] = old[i].equals(now[j])
                        ? table[i + 1][j + 1] + 1
                        : Math.max(table[i + 1][j], table[i][j + 1]);
            }
        }
        return table;
    }

    private int indexOf(final String file) {
        return files.indexOf(file);
    }
}
