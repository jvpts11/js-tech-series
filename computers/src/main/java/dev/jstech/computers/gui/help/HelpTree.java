/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import dev.jstech.core.guide.ManualReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The tree a help window shows beside its page: every manual a book, its chapters and sections books inside it, its
 * entries pages, and the machine's commands a book of their own, filed under what each is for.
 *
 * <p>A book shows what is inside it only while it is open, and a row is known by a key that stays the same however
 * the tree is opened, so what was open and what was picked survive the tree being built again.
 *
 * <p>Pure: built from the manuals read as text and the commands as the machine listed them.
 */
public final class HelpTree {

    /** The key of the book of commands, which leads to the list of them. */
    public static final String COMMANDS_KEY = HelpTarget.command("").written();

    private HelpTree() {
    }

    /** What a row of the tree is drawn as. */
    public enum Icon {
        /** A book that is shut. */
        BOOK,
        /** A book that is open. */
        OPEN_BOOK,
        /** A page. */
        PAGE
    }

    /**
     * One row of the tree.
     *
     * @param key    what the row is known by
     * @param depth  how far it is set in
     * @param label  what it says
     * @param icon   what it is drawn as
     * @param target where it leads, or null for a heading of commands, which only opens
     */
    public record Row(String key, int depth, String label, Icon icon, HelpTarget target) {

        /** Whether the row opens and shuts. */
        public boolean book() {
            return this.icon != Icon.PAGE;
        }
    }

    /** The rows as they stand with those books open. */
    public static List<Row> rows(final List<ManualReader> manuals, final List<HelpCommand> commands,
                                 final String commandsTitle, final Set<String> open) {
        final List<Row> rows = new ArrayList<>();
        for (final ManualReader reader : manuals) {
            final HelpTarget top = HelpTarget.contents(reader.manualId());
            final boolean opened = open.contains(top.written());
            rows.add(new Row(top.written(), 0, reader.title(), opened ? Icon.OPEN_BOOK : Icon.BOOK, top));
            if (opened) {
                for (final String chapter : reader.chapters()) {
                    reader.node(chapter).ifPresent(node -> node(rows, reader, node, 1, open));
                }
            }
        }
        final boolean commandsOpen = open.contains(COMMANDS_KEY);
        rows.add(new Row(COMMANDS_KEY, 0, commandsTitle, commandsOpen ? Icon.OPEN_BOOK : Icon.BOOK,
                HelpTarget.command("")));
        if (commandsOpen) {
            final Map<String, List<HelpCommand>> groups = new LinkedHashMap<>();
            for (final HelpCommand command : commands) {
                groups.computeIfAbsent(command.group(), group -> new ArrayList<>()).add(command);
            }
            for (final Map.Entry<String, List<HelpCommand>> group : groups.entrySet()) {
                final String key = groupKey(group.getKey());
                final boolean groupOpen = open.contains(key);
                rows.add(new Row(key, 1, group.getKey(), groupOpen ? Icon.OPEN_BOOK : Icon.BOOK, null));
                if (groupOpen) {
                    for (final HelpCommand command : group.getValue()) {
                        final HelpTarget target = HelpTarget.command(command.name());
                        rows.add(new Row(target.written(), 2, command.name(), Icon.PAGE, target));
                    }
                }
            }
        }
        return rows;
    }

    /**
     * The keys of the books a page sits in, from the outside in, so opening a page opens what holds it: its manual,
     * its chapter and its section, or the book of commands and the command's group.
     */
    public static List<String> holding(final HelpTarget target, final List<ManualReader> manuals,
                                       final List<HelpCommand> commands) {
        final List<String> keys = new ArrayList<>();
        switch (target.kind()) {
            case NODE -> {
                keys.add(HelpTarget.contents(target.manual()).written());
                for (final ManualReader reader : manuals) {
                    if (!reader.manualId().equals(target.manual())) {
                        continue;
                    }
                    final List<String> above = new ArrayList<>();
                    String at = target.id();
                    while (reader.parent(at).isPresent()) {
                        at = reader.parent(at).get();
                        above.addFirst(HelpTarget.node(target.manual(), at).written());
                    }
                    keys.addAll(above);
                }
            }
            case COMMAND -> {
                keys.add(COMMANDS_KEY);
                for (final HelpCommand command : commands) {
                    if (command.name().equals(target.id())) {
                        keys.add(groupKey(command.group()));
                    }
                }
            }
            case CONTENTS, INDEX -> {
            }
        }
        return keys;
    }

    private static void node(final List<Row> rows, final ManualReader reader, final ManualReader.Node node,
                             final int depth, final Set<String> open) {
        final HelpTarget target = HelpTarget.node(reader.manualId(), node.id());
        final String label = node.number() + " " + node.title();
        if (node.kind() == ManualReader.Kind.ENTRY) {
            rows.add(new Row(target.written(), depth, label, Icon.PAGE, target));
            return;
        }
        final boolean opened = open.contains(target.written());
        rows.add(new Row(target.written(), depth, label, opened ? Icon.OPEN_BOOK : Icon.BOOK, target));
        if (opened) {
            for (final ManualReader.Node child : reader.children(node.id())) {
                node(rows, reader, child, depth + 1, open);
            }
        }
    }

    private static String groupKey(final String group) {
        return "group|" + group;
    }
}
