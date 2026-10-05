/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import dev.jstech.core.guide.GuideIds;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.text.ITextLanguage;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The pages a help program at a terminal shows, laid out for the width of the glass: a manual's entry, a chapter or a
 * section with what it holds, a manual's contents and index, and a command's manual page.
 *
 * <p>Each of the three readers has its own voice. The DOS family's HELP indents its text under yellow headings and
 * writes a table on one line; info underlines a node's title and lists what is under it as a menu; man sets its
 * headings in capitals at the margin and its text seven columns in. The words are the manual's own in every voice.
 *
 * <p>Pure: no game here, so a test lays out a page and reads it back.
 */
public final class HelpPages {

    /** What every voice writes between two links on one line. */
    private static final String LINK_GAP = "  ";

    /** How wide a column of command names is at least, in DOS's contents. */
    private static final int LEAST_COMMAND_COLUMN = 12;

    /** The section manual entries are filed under at a Unix prompt: the one for everything that is not a command. */
    public static final String MAN_SECTION = "7";

    private HelpPages() {
    }

    /** The lines a recipe type makes at a terminal, worked out by whoever has the game to read them from. */
    @FunctionalInterface
    public interface IRecipeLines {

        /** One line per recipe of that type making that item, or of every item when none is named. */
        List<String> lines(String type, String output);
    }

    /** The three readers, each laying a page out its own way. */
    public enum Voice {
        DOS(1, 1, 3, 5, false),
        INFO(0, 0, 3, 6, false),
        MAN(0, 0, 7, 14, true);

        private final int titleIndent;
        private final int headingIndent;
        private final int bodyIndent;
        private final int termIndent;
        private final boolean capitals;

        Voice(final int titleIndent, final int headingIndent, final int bodyIndent, final int termIndent,
              final boolean capitals) {
            this.titleIndent = titleIndent;
            this.headingIndent = headingIndent;
            this.bodyIndent = bodyIndent;
            this.termIndent = termIndent;
            this.capitals = capitals;
        }
    }

    /** An entry of a manual, in that voice, for a glass that many columns wide. */
    public static List<HelpLine> article(final ManualReader reader, final ManualReader.Article article,
                                         final Voice voice, final int columns, final ITextLanguage language,
                                         final IRecipeLines recipes) {
        final List<HelpLine> lines = new ArrayList<>();
        final int width = Math.max(20, columns);
        title(lines, article.number() + " " + article.title(), voice, width);
        if (voice == Voice.MAN) {
            lines.addFirst(HelpLine.BLANK);
            lines.addFirst(manHeader(reader, article.id(), width));
            lines.add(HelpLine.of(0, say(HelpTexts.MAN_NAME, language), HelpLine.Ink.HEADING));
            lines.addAll(wrapped(manName(article.id()) + " - " + article.title(), voice.bodyIndent, width,
                    HelpLine.Ink.BODY));
            lines.add(HelpLine.BLANK);
        }
        for (final ManualReader.Piece piece : article.pieces()) {
            switch (piece) {
                case ManualReader.Piece.Heading heading -> {
                    if (voice == Voice.MAN && !lines.getLast().spans().isEmpty()) {
                        lines.add(HelpLine.BLANK);
                    }
                    lines.add(HelpLine.of(voice.headingIndent, voice.capitals
                            ? heading.text().toUpperCase(Locale.ROOT) : heading.text(), HelpLine.Ink.HEADING));
                }
                case ManualReader.Piece.Paragraph paragraph -> lines.addAll(wrapped(paragraph.text(),
                        voice.bodyIndent, width, switch (paragraph.tone()) {
                            case PLAIN -> HelpLine.Ink.BODY;
                            case NOTE -> HelpLine.Ink.NOTE;
                            case WARNING -> HelpLine.Ink.WARNING;
                        }));
                case ManualReader.Piece.Item item -> lines.addAll(hanging(item.marker() + " ", item.text(),
                        voice.bodyIndent, width));
                case ManualReader.Piece.Term term -> {
                    lines.addAll(wrapped(term.term(), voice.bodyIndent, width, HelpLine.Ink.TITLE));
                    lines.addAll(wrapped(term.text(), voice.termIndent, width, HelpLine.Ink.BODY));
                }
                case ManualReader.Piece.Table table -> table(lines, table, voice, width);
                case ManualReader.Piece.Picture picture -> {
                    if (!picture.caption().isEmpty()) {
                        lines.addAll(wrapped(picture.caption(), voice.bodyIndent, width, HelpLine.Ink.DIM));
                    }
                }
                case ManualReader.Piece.Recipes made -> {
                    final List<String> found = recipes.lines(made.type(), made.output());
                    if (found.isEmpty()) {
                        lines.addAll(wrapped(say(HelpTexts.NO_RECIPES, language), voice.bodyIndent, width,
                                HelpLine.Ink.DIM));
                    }
                    for (final String recipe : found) {
                        lines.addAll(wrapped(recipe, voice.bodyIndent, width, HelpLine.Ink.BODY));
                    }
                }
                case ManualReader.Piece.Links links -> links(lines, reader, links, voice, width, language);
            }
        }
        if (voice == Voice.MAN) {
            lines.add(HelpLine.BLANK);
            lines.add(spread(article.number(), "", manName(article.id()).toUpperCase(Locale.ROOT)
                    + "(" + MAN_SECTION + ")", width));
        }
        return lines;
    }

    /** A chapter or a section: its title, and what it holds, each a link. */
    public static List<HelpLine> node(final ManualReader reader, final String id, final Voice voice,
                                      final int columns, final ITextLanguage language) {
        final List<HelpLine> lines = new ArrayList<>();
        final int width = Math.max(20, columns);
        title(lines, reader.label(id), voice, width);
        final List<ManualReader.Node> children = reader.children(id);
        if (voice == Voice.INFO) {
            menu(lines, reader.manualId(), children, language);
            return lines;
        }
        for (final ManualReader.Node child : children) {
            lines.add(link(voice.bodyIndent, child.number() + " " + child.title(),
                    HelpTarget.node(reader.manualId(), child.id())));
        }
        return lines;
    }

    /**
     * DOS's contents: every chapter, section and entry of a manual, each a link and each set in under what it belongs
     * to, then the commands of the machine across the glass, then the other manuals.
     */
    public static List<HelpLine> contents(final ManualReader reader, final List<ManualReader> others,
                                          final List<HelpCommand> commands, final int columns,
                                          final ITextLanguage language) {
        final List<HelpLine> lines = new ArrayList<>();
        final int width = Math.max(20, columns);
        title(lines, reader.title(), Voice.DOS, width);
        for (final ManualReader.Node node : reader.tree()) {
            lines.add(link(Voice.DOS.bodyIndent + 2 * node.depth(), node.number() + " " + node.title(),
                    HelpTarget.node(reader.manualId(), node.id())));
        }
        lines.add(HelpLine.BLANK);
        lines.add(HelpLine.of(Voice.DOS.headingIndent, say(HelpTexts.COMMANDS, language), HelpLine.Ink.HEADING));
        if (commands.isEmpty()) {
            lines.add(HelpLine.of(Voice.DOS.bodyIndent, say(HelpTexts.NOTHING_YET, language), HelpLine.Ink.DIM));
        }
        int widest = LEAST_COMMAND_COLUMN;
        for (final HelpCommand command : commands) {
            widest = Math.max(widest, command.name().length() + 4);
        }
        final int across = Math.max(1, (width - Voice.DOS.bodyIndent - 1) / widest);
        for (int i = 0; i < commands.size(); i += across) {
            final List<HelpLine.Span> spans = new ArrayList<>();
            spans.add(new HelpLine.Span(" ".repeat(Voice.DOS.bodyIndent), HelpLine.Ink.BODY, ""));
            for (int j = i; j < Math.min(commands.size(), i + across); j++) {
                final String shown = "<" + commands.get(j).name() + ">";
                spans.add(new HelpLine.Span(shown, HelpLine.Ink.LINK,
                        HelpTarget.command(commands.get(j).name()).written()));
                spans.add(new HelpLine.Span(" ".repeat(Math.max(1, widest - shown.length())), HelpLine.Ink.BODY,
                        ""));
            }
            lines.add(new HelpLine(spans));
        }
        if (!others.isEmpty()) {
            lines.add(HelpLine.BLANK);
            lines.add(HelpLine.of(Voice.DOS.headingIndent, say(HelpTexts.OTHER_MANUALS, language),
                    HelpLine.Ink.HEADING));
            for (final ManualReader other : others) {
                lines.add(link(Voice.DOS.bodyIndent, other.title(), HelpTarget.contents(other.manualId())));
            }
        }
        return lines;
    }

    /** A manual's index, every title and every word explained, each a link to where it is. */
    public static List<HelpLine> index(final ManualReader reader, final Voice voice, final int columns,
                                       final ITextLanguage language) {
        final List<HelpLine> lines = new ArrayList<>();
        title(lines, say(HelpTexts.INDEX, language) + ": " + reader.title(), voice, Math.max(20, columns));
        for (final ManualReader.IndexLine line : reader.index()) {
            lines.add(link(voice.bodyIndent, line.text(), HelpTarget.node(reader.manualId(), line.target())));
        }
        return lines;
    }

    /** A command's manual page as the machine sent it, under the command's name. */
    public static List<HelpLine> command(final String name, final List<String> page, final Voice voice,
                                         final int columns, final ITextLanguage language) {
        final List<HelpLine> lines = new ArrayList<>();
        final int width = Math.max(20, columns);
        title(lines, name.toUpperCase(Locale.ROOT), voice, width);
        if (page == null) {
            lines.add(HelpLine.of(voice.bodyIndent, say(HelpTexts.NOTHING_YET, language), HelpLine.Ink.DIM));
            return lines;
        }
        for (final String line : page) {
            int indent = 0;
            while (indent < line.length() && line.charAt(indent) == ' ') {
                indent++;
            }
            final List<HelpLine> wrapped = wrapped(line.substring(indent), voice.titleIndent + indent, width,
                    indent == 0 ? HelpLine.Ink.HEADING : HelpLine.Ink.BODY);
            lines.addAll(wrapped.isEmpty() ? List.of(HelpLine.BLANK) : wrapped);
        }
        return lines;
    }

    /** info's menu of what a node holds: "* Menu:", then a line for each, its label the link. */
    public static void menu(final List<HelpLine> lines, final String manual, final List<ManualReader.Node> children,
                            final ITextLanguage language) {
        if (children.isEmpty()) {
            return;
        }
        lines.add(HelpLine.BLANK);
        lines.add(HelpLine.of(0, say(HelpTexts.INFO_MENU, language), HelpLine.Ink.HEADING));
        lines.add(HelpLine.BLANK);
        for (final ManualReader.Node child : children) {
            lines.add(new HelpLine(List.of(new HelpLine.Span("* ", HelpLine.Ink.BODY, ""),
                    new HelpLine.Span(child.number() + " " + child.title(), HelpLine.Ink.LINK,
                            HelpTarget.node(manual, child.id()).written()),
                    new HelpLine.Span("::", HelpLine.Ink.BODY, ""))));
        }
    }

    /** Words broken into lines no wider than that, a word longer than a line cut where the line ends. */
    public static List<String> wrap(final String words, final int width) {
        final List<String> out = new ArrayList<>();
        final int room = Math.max(1, width);
        final StringBuilder line = new StringBuilder();
        for (final String word : words.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String rest = word;
            while (rest.length() > room) {
                if (!line.isEmpty()) {
                    out.add(line.toString());
                    line.setLength(0);
                }
                out.add(rest.substring(0, room));
                rest = rest.substring(room);
            }
            if (line.isEmpty()) {
                line.append(rest);
            } else if (line.length() + 1 + rest.length() <= room) {
                line.append(' ').append(rest);
            } else {
                out.add(line.toString());
                line.setLength(0);
                line.append(rest);
            }
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    /** The name an entry has at a Unix prompt: the last part of its id. */
    public static String manName(final String id) {
        return GuideIds.path(id);
    }

    private static String say(final TextKey key, final ITextLanguage language) {
        return key.text().resolve(language);
    }

    /** A page's title in that voice: set in and ruled across for DOS, underlined for info, nothing more for man. */
    private static void title(final List<HelpLine> lines, final String title, final Voice voice, final int width) {
        if (voice == Voice.MAN) {
            return;
        }
        if (voice == Voice.DOS) {
            // DOS's help left a row under its buttons before the topic's title.
            lines.add(HelpLine.BLANK);
        }
        lines.add(HelpLine.of(voice.titleIndent, title, HelpLine.Ink.TITLE));
        if (voice == Voice.DOS) {
            lines.add(HelpLine.of(voice.titleIndent, "─".repeat(Math.max(1, width - 2 * voice.titleIndent)),
                    HelpLine.Ink.RULE));
        } else {
            lines.add(HelpLine.of(0, "=".repeat(Math.min(width, title.length())), HelpLine.Ink.RULE));
            lines.add(HelpLine.BLANK);
        }
    }

    /** man's first line: the page's name and section at both ends, the manual's title between them. */
    private static HelpLine manHeader(final ManualReader reader, final String id, final int width) {
        final String name = manName(id).toUpperCase(Locale.ROOT) + "(" + MAN_SECTION + ")";
        return spread(name, reader.title(), name, width);
    }

    /** Three pieces of text on one line: at its start, in its middle and at its end, as far as they fit. */
    private static HelpLine spread(final String left, final String middle, final String right, final int width) {
        final StringBuilder line = new StringBuilder(left);
        final int middleAt = Math.max(line.length() + 1, (width - middle.length()) / 2);
        if (!middle.isEmpty() && middleAt + middle.length() + right.length() + 1 < width) {
            line.append(" ".repeat(middleAt - line.length())).append(middle);
        }
        final int rightAt = Math.max(line.length() + 1, width - right.length());
        if (rightAt + right.length() <= width) {
            line.append(" ".repeat(rightAt - line.length())).append(right);
        }
        // A glass too narrow for both ends keeps the start, as a terminal that narrow cuts the rest away.
        return HelpLine.of(0, line.length() > width ? line.substring(0, width) : line.toString(),
                HelpLine.Ink.TITLE);
    }

    private static void table(final List<HelpLine> lines, final ManualReader.Piece.Table table, final Voice voice,
                              final int width) {
        if (voice == Voice.DOS) {
            // DOS's help kept a table to one line of figures under the text, which is all a part's table holds.
            lines.add(HelpLine.BLANK);
            lines.addAll(wrapped(table.caption(), voice.headingIndent, width, HelpLine.Ink.TABLE));
            final List<String> cells = new ArrayList<>();
            for (final ManualReader.Row row : table.rows()) {
                cells.add(row.label() + " " + row.value());
            }
            final StringBuilder packed = new StringBuilder();
            for (final String cell : cells) {
                final String joint = packed.isEmpty() ? "" : "   ";
                if (!packed.isEmpty() && voice.headingIndent + packed.length() + joint.length() + cell.length()
                        > width - 1) {
                    lines.add(HelpLine.of(voice.headingIndent, packed.toString(), HelpLine.Ink.TABLE));
                    packed.setLength(0);
                    packed.append(cell);
                } else {
                    packed.append(joint).append(cell);
                }
            }
            if (!packed.isEmpty()) {
                lines.add(HelpLine.of(voice.headingIndent, packed.toString(), HelpLine.Ink.TABLE));
            }
            return;
        }
        lines.addAll(wrapped(table.caption(), voice.bodyIndent, width, HelpLine.Ink.TABLE));
        int widest = 0;
        for (final ManualReader.Row row : table.rows()) {
            widest = Math.max(widest, row.label().length());
        }
        for (final ManualReader.Row row : table.rows()) {
            final String label = row.label() + " ".repeat(widest - row.label().length() + 2);
            lines.addAll(hangingInk(label, row.value(), voice.bodyIndent + 2, width, HelpLine.Ink.TABLE));
        }
    }

    private static void links(final List<HelpLine> lines, final ManualReader reader,
                              final ManualReader.Piece.Links links, final Voice voice, final int width,
                              final ITextLanguage language) {
        final List<HelpLine.Span> pieces = new ArrayList<>();
        for (final ManualReader.Link link : links.links()) {
            final String target = HelpTarget.node(reader.manualId(), link.target()).written();
            pieces.add(new HelpLine.Span(switch (voice) {
                case DOS -> "<" + link.text() + ">";
                case INFO -> "*Note " + link.text() + "::";
                case MAN -> manName(link.target()) + "(" + MAN_SECTION + ")";
            }, HelpLine.Ink.LINK, target));
        }
        if (voice == Voice.MAN) {
            lines.add(HelpLine.BLANK);
            lines.add(HelpLine.of(0, say(HelpTexts.MAN_SEE_ALSO, language), HelpLine.Ink.HEADING));
            lines.addAll(packed(List.of(), pieces, voice.bodyIndent, width, ", "));
            return;
        }
        final String lead = voice == Voice.INFO ? say(HelpTexts.INFO_SEE, language) : links.lead();
        final int indent = voice == Voice.INFO ? voice.bodyIndent : voice.headingIndent;
        lines.addAll(packed(List.of(new HelpLine.Span(lead + " ", HelpLine.Ink.LINK, "")), pieces, indent, width,
                voice == Voice.INFO ? ", " : LINK_GAP));
    }

    /** Links laid along lines, as many to a line as fit, after what leads them in. */
    private static List<HelpLine> packed(final List<HelpLine.Span> lead, final List<HelpLine.Span> pieces,
                                         final int indent, final int width, final String gap) {
        final List<HelpLine> out = new ArrayList<>();
        final List<HelpLine.Span> line = new ArrayList<>();
        line.add(new HelpLine.Span(" ".repeat(indent), HelpLine.Ink.BODY, ""));
        line.addAll(lead);
        int used = indent;
        for (final HelpLine.Span span : lead) {
            used += span.text().length();
        }
        boolean first = true;
        for (final HelpLine.Span piece : pieces) {
            final int needed = (first ? 0 : gap.length()) + piece.text().length();
            // A link that does not fit goes to the next line, even the first after the words that lead it in.
            if ((!first || used > indent) && used + needed > width - 1) {
                out.add(new HelpLine(line));
                line.clear();
                line.add(new HelpLine.Span(" ".repeat(indent), HelpLine.Ink.BODY, ""));
                used = indent;
                first = true;
            }
            if (!first) {
                line.add(new HelpLine.Span(gap, HelpLine.Ink.BODY, ""));
                used += gap.length();
            }
            line.add(piece);
            used += piece.text().length();
            first = false;
        }
        out.add(new HelpLine(line));
        return out;
    }

    private static HelpLine link(final int indent, final String text, final HelpTarget target) {
        return new HelpLine(List.of(new HelpLine.Span(" ".repeat(indent), HelpLine.Ink.BODY, ""),
                new HelpLine.Span("<" + text + ">", HelpLine.Ink.LINK, target.written())));
    }

    private static List<HelpLine> wrapped(final String words, final int indent, final int width,
                                          final HelpLine.Ink ink) {
        final List<HelpLine> out = new ArrayList<>();
        for (final String line : wrap(words, width - indent - 1)) {
            out.add(HelpLine.of(indent, line, ink));
        }
        return out;
    }

    /** A marker, then words set in past it on every line, as a numbered step is. */
    private static List<HelpLine> hanging(final String marker, final String words, final int indent,
                                          final int width) {
        return hangingInk(marker, words, indent, width, HelpLine.Ink.BODY);
    }

    private static List<HelpLine> hangingInk(final String marker, final String words, final int indent,
                                             final int width, final HelpLine.Ink ink) {
        final List<HelpLine> out = new ArrayList<>();
        final List<String> wrapped = wrap(words, width - indent - marker.length() - 1);
        for (int i = 0; i < wrapped.size(); i++) {
            out.add(HelpLine.of(indent, (i == 0 ? marker : " ".repeat(marker.length())) + wrapped.get(i), ink));
        }
        if (wrapped.isEmpty()) {
            out.add(HelpLine.of(indent, marker, ink));
        }
        return out;
    }
}
