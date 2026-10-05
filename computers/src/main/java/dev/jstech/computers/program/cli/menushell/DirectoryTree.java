/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli.menushell;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The tree of folders a DOS menu shell draws: the root, and under each folder that is open the folders in it, each
 * row with the lines that join it to its parent and a mark saying whether it has folders of its own to open. Built
 * from every folder of the disk written whole, as the machine lists them.
 */
public final class DirectoryTree {

    private final String root;
    private final List<String> folders;
    private final Set<String> open = new HashSet<>();

    /** A tree under {@code root} ({@code C:\}) of {@code folders}, each written whole; the root starts open. */
    public DirectoryTree(final String root, final List<String> folders) {
        this.root = root;
        this.folders = List.copyOf(folders);
        this.open.add(key(root));
    }

    /**
     * The same tree over {@code newFolders}, the disk read again: what was open stays open, as the shell keeps its
     * tree as it was when the disk is refreshed.
     */
    public DirectoryTree rebuilt(final List<String> newFolders) {
        final DirectoryTree again = new DirectoryTree(root, newFolders);
        again.open.addAll(open);
        return again;
    }

    /** The root, written whole. */
    public String root() {
        return root;
    }

    /** The rows shown, from the root down, each folder under its parent when the parent is open. */
    public List<Row> rows() {
        final List<Row> out = new ArrayList<>();
        out.add(new Row(root, "", hasChildren(root), isOpen(root), 0));
        addChildren(out, root, "", 1);
        return out;
    }

    /** Opens {@code folder} one level: its folders are shown. */
    public void expand(final String folder) {
        open.add(key(folder));
    }

    /** Opens {@code folder} and every folder under it. */
    public void expandBranch(final String folder) {
        open.add(key(folder));
        for (final String each : folders) {
            if (under(each, folder)) {
                open.add(key(each));
            }
        }
    }

    /** Opens every folder there is. */
    public void expandAll() {
        expandBranch(root);
    }

    /** Closes {@code folder}, and every folder under it with it. */
    public void collapse(final String folder) {
        open.remove(key(folder));
        for (final String each : folders) {
            if (under(each, folder)) {
                open.remove(key(each));
            }
        }
    }

    /** Opens every folder on the way down to {@code folder}, so it is shown. */
    public void reveal(final String folder) {
        String parent = parentOf(folder);
        while (parent != null) {
            open.add(key(parent));
            parent = parentOf(parent);
        }
    }

    /** Whether {@code folder} has folders of its own. */
    public boolean hasChildren(final String folder) {
        for (final String each : folders) {
            if (isChild(each, folder)) {
                return true;
            }
        }
        return false;
    }

    /** Whether {@code folder} is open. */
    public boolean isOpen(final String folder) {
        return open.contains(key(folder));
    }

    private void addChildren(final List<Row> out, final String parent, final String lead, final int depth) {
        if (!isOpen(parent)) {
            return;
        }
        final List<String> children = new ArrayList<>();
        for (final String each : folders) {
            if (isChild(each, parent)) {
                children.add(each);
            }
        }
        for (int i = 0; i < children.size(); i++) {
            final String child = children.get(i);
            final boolean last = i == children.size() - 1;
            out.add(new Row(child, lead + (last ? "└─" : "├─"), hasChildren(child), isOpen(child), depth));
            addChildren(out, child, lead + (last ? "  " : "│ "), depth + 1);
        }
    }

    /* Whether {@code folder} sits right inside {@code parent}. */
    private boolean isChild(final String folder, final String parent) {
        final String p = parentOf(folder);
        return p != null && key(p).equals(key(parent));
    }

    /* Whether {@code folder} sits anywhere under {@code parent}. */
    private boolean under(final String folder, final String parent) {
        String p = parentOf(folder);
        while (p != null) {
            if (key(p).equals(key(parent))) {
                return true;
            }
            p = parentOf(p);
        }
        return false;
    }

    /* The folder {@code folder} is in, or null for the root. */
    private String parentOf(final String folder) {
        if (key(folder).equals(key(root))) {
            return null;
        }
        final String trimmed = folder.endsWith("\\") ? folder.substring(0, folder.length() - 1) : folder;
        final int slash = trimmed.lastIndexOf('\\');
        if (slash < 0) {
            return null;
        }
        final String parent = trimmed.substring(0, slash);
        // C:\DOS is in C:\, written with its backslash.
        return parent.endsWith(":") ? parent + "\\" : parent;
    }

    private static String key(final String folder) {
        final String upper = folder.toUpperCase(Locale.ROOT);
        return upper.endsWith("\\") && !upper.endsWith(":\\") ? upper.substring(0, upper.length() - 1) : upper;
    }

    /**
     * One row of the tree.
     *
     * @param folder      the folder, written whole
     * @param lead        the lines that join it to its parent, drawn before its mark
     * @param hasChildren whether it has folders of its own
     * @param open        whether those are shown
     * @param depth       how far down from the root it is
     */
    public record Row(String folder, String lead, boolean hasChildren, boolean open, int depth) {

        /** Its own name, the root written whole. */
        public String name() {
            final String trimmed = folder.endsWith("\\") && !folder.endsWith(":\\")
                    ? folder.substring(0, folder.length() - 1) : folder;
            final int slash = trimmed.lastIndexOf('\\');
            return slash < 0 || trimmed.endsWith(":\\") ? trimmed : trimmed.substring(slash + 1);
        }
    }
}
