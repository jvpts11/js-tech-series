/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.gui.screen;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Where in a screen's file each component is written: the line its object opens on, by its place in the file's tree,
 * the root first and each component's children in order ({@code []} the root, {@code [0, 2]} the third child of its
 * first). The developer's inspector and the editor of screens say "screens/crusher.json, line 58" with it.
 *
 * <p>Pure: no game types, so it is checked without the game.
 */
public final class ScreenFileLines {

    private static final String ROOT = "root";
    private static final String CHILDREN = "children";

    private ScreenFileLines() {
    }

    /** The line, counted from 1, each component of {@code text} opens on, by its place in the tree. */
    public static Map<List<Integer>, Integer> of(final String text) {
        final Map<List<Integer>, Integer> out = new HashMap<>();
        final Deque<Frame> open = new ArrayDeque<>();
        final StringBuilder word = new StringBuilder();
        int line = 1;
        boolean inString = false;
        boolean escaped = false;
        @Nullable String key = null;
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == '\n') {
                line++;
            }
            if (inString) {
                if (escaped) {
                    escaped = false;
                    word.append(c);
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                    key = word.toString();
                } else {
                    word.append(c);
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                word.setLength(0);
            } else if (c == '{') {
                final List<Integer> path = placeOf(open.peek(), key, open.size());
                if (path != null) {
                    out.put(path, line);
                }
                open.push(new Frame(false, false, path));
                key = null;
            } else if (c == '[') {
                final Frame parent = open.peek();
                final boolean children = parent != null && !parent.array && CHILDREN.equals(key)
                        && parent.path != null;
                open.push(new Frame(true, children, children ? parent.path : null));
                key = null;
            } else if (c == '}' || c == ']' || c == ',') {
                if (c != ',') {
                    open.poll();
                }
                key = null;
            }
        }
        return out;
    }

    /* The place in the tree of an object opening in {@code parent} after {@code key}, or none when it is no component. */
    @Nullable
    private static List<Integer> placeOf(@Nullable final Frame parent, @Nullable final String key, final int depth) {
        if (parent == null) {
            return null;
        }
        if (parent.array && parent.children) {
            final List<Integer> path = new ArrayList<>(parent.path);
            path.add(parent.count++);
            return List.copyOf(path);
        }
        return !parent.array && depth == 1 && ROOT.equals(key) ? List.of() : null;
    }

    /* An object or an array the reading is inside: whether it is a component's children, and that component's place. */
    private static final class Frame {

        private final boolean array;
        private final boolean children;
        @Nullable
        private final List<Integer> path;
        private int count;

        private Frame(final boolean array, final boolean children, @Nullable final List<Integer> path) {
            this.array = array;
            this.children = children;
            this.path = path;
        }
    }
}
