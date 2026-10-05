/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.guide.GuideIds;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.text.ITextLanguage;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * info, the reader of the GNU manuals, on the Linux distributions: the manuals of the player's game as info files, and
 * the machine's commands beside them.
 *
 * <p>Every chapter, section and entry is a node. The top line names the node's neighbours (Next, Prev, Up), the node
 * fills the glass in grey on black, its menu lists what is under it, and the mode line under it names the file and
 * the node with how far down it is. n, p and u go to those neighbours, Tab and Enter follow a link, m goes to a menu
 * item by name, s searches, l goes back and q leaves. The directory node lists every manual, and the commands.
 *
 * <p>Pure: the keys come in as what they mean and the screen goes out as cells.
 */
public final class InfoHelp {

    private final IHelpSource source;
    private final ITextLanguage language;
    /** The nodes shown before this one, newest last, which l walks. */
    private final List<HelpTarget> last = new ArrayList<>();
    private HelpTarget shown;
    private boolean keys;
    private List<HelpLine> page = List.of();
    private int laidFor = -1;
    private int laidWith = -1;
    private int answers;
    private int scroll;
    private int focus = -1;
    private int bodyRows = 21;
    /** What a prompt in the echo area is asking for, and what has been typed into it; no prompt when null. */
    private TextKey prompt;
    private final StringBuilder typing = new StringBuilder();
    private String message;
    private boolean leaving;
    /** What info was opened on when nothing answered to it, until the machine has listed its commands. */
    private String unfound = "";

    private static final int HEADER_ROW = 0;
    private static final int BODY_TOP = 1;
    /** What info calls its directory node, and the top node of a file: names of nodes, the same in every language. */
    private static final String DIRECTORY_NODE = "(dir)Top";
    private static final String TOP_NODE = "Top";
    /** Where a link goes that leads to the directory, and to the list of the machine's commands. */
    private static final HelpTarget DIRECTORY = HelpTarget.contents("");
    private static final HelpTarget COMMANDS = HelpTarget.command("");

    private static final int INK = TextScreen.cga(TextScreen.GREY);
    private static final int BRIGHT = TextScreen.cga(TextScreen.WHITE);
    private static final int GROUND = TextScreen.cga(TextScreen.BLACK);
    private static final int LINK = TextScreen.cga(TextScreen.LIGHT_CYAN);
    private static final int WARNING = TextScreen.cga(TextScreen.LIGHT_RED);
    private static final int DIM = TextScreen.cga(TextScreen.DARK_GREY);

    /** info opened on a node by its name, or on the directory when none was given. */
    public InfoHelp(final String topic, final IHelpSource source, final ITextLanguage language) {
        this.source = source;
        this.language = language;
        this.message = say(HelpTexts.INFO_WELCOME);
        this.shown = DIRECTORY;
        final String asked = topic == null ? "" : topic.strip();
        if (!asked.isEmpty()) {
            final Optional<HelpTarget> found = this.named(asked);
            if (found.isPresent()) {
                this.shown = found.get();
            } else {
                // Perhaps a command, which the machine has not listed yet: asked again when it answers.
                this.unfound = asked;
                this.message = say(HelpTexts.INFO_NO_ITEM, asked, Text.literal(DIRECTORY_NODE));
            }
        }
    }

    /** What the glass shows now. */
    public HelpTarget shown() {
        return this.shown;
    }

    /** The node's lines, laid out for the width last drawn at. */
    public List<HelpLine> page() {
        return this.page;
    }

    /** Whether q was pressed, so the terminal is to be given back. */
    public boolean leaving() {
        return this.leaving;
    }

    /** What the echo area says. */
    public String message() {
        return this.message;
    }

    /** The machine answered, so the list of its commands or a page of one may be there now. */
    public void answered() {
        this.answers++;
        if (!this.unfound.isEmpty() && this.shown.equals(DIRECTORY) && this.last.isEmpty()) {
            final Optional<HelpTarget> found = this.named(this.unfound);
            if (found.isPresent()) {
                this.shown = found.get();
                this.message = "";
                this.reset();
            }
        }
        this.unfound = "";
    }

    /** n: the node after this one at its level. */
    public void next() {
        this.neighbour(1).ifPresentOrElse(this::go, () -> this.message = say(HelpTexts.INFO_NO_NEXT));
    }

    /** p: the node before this one at its level. */
    public void previous() {
        this.neighbour(-1).ifPresentOrElse(this::go, () -> this.message = say(HelpTexts.INFO_NO_PREV));
    }

    /** u: the node this one is under. */
    public void up() {
        this.parent(this.shown).ifPresentOrElse(this::go, () -> this.message = say(HelpTexts.INFO_NO_UP));
    }

    /** t: the top node of the manual being read. */
    public void top() {
        if (this.shown.kind() == HelpTarget.Kind.COMMAND) {
            this.go(COMMANDS);
        } else if (!this.shown.manual().isEmpty()) {
            this.go(HelpTarget.contents(this.shown.manual()));
        }
    }

    /** d: the directory of every manual. */
    public void directory() {
        this.go(DIRECTORY);
    }

    /** l: the node shown before this one. */
    public void lastNode() {
        if (this.keys) {
            this.keys = false;
            this.reset();
            return;
        }
        if (this.last.isEmpty()) {
            this.message = say(HelpTexts.INFO_NO_LAST);
            return;
        }
        this.shown = this.last.removeLast();
        this.reset();
    }

    /** h: the keys info answers to. */
    public void showKeys() {
        this.keys = !this.keys;
        this.reset();
    }

    /** q: leaves. */
    public void quit() {
        this.leaving = true;
    }

    /** Moves the node up or down that many lines. */
    public void scroll(final int lines) {
        this.scroll = Math.max(0, Math.min(this.scroll + lines, Math.max(0, this.page.size() - this.bodyRows)));
    }

    /** Space and Backspace: a screen down or up. */
    public void page(final boolean down) {
        this.scroll(down ? this.bodyRows - 2 : 2 - this.bodyRows);
    }

    /** Tab: the next link of the node, or the one before it, going round. */
    public void focusNext(final boolean backwards) {
        final List<Found> links = this.links();
        if (links.isEmpty()) {
            return;
        }
        this.focus = this.focus < 0 ? (backwards ? links.size() - 1 : 0)
                : Math.floorMod(this.focus + (backwards ? -1 : 1), links.size());
        final int line = links.get(this.focus).line();
        if (line < this.scroll) {
            this.scroll = line;
        } else if (line >= this.scroll + this.bodyRows) {
            this.scroll = line - this.bodyRows + 1;
        }
    }

    /** Enter: follows the picked link. */
    public void follow() {
        final List<Found> links = this.links();
        if (this.focus >= 0 && this.focus < links.size()) {
            HelpTarget.read(links.get(this.focus).span().link()).ifPresent(this::go);
        }
    }

    /** m: asks for a menu item by name. */
    public void askMenuItem() {
        this.ask(HelpTexts.INFO_MENU_PROMPT);
    }

    /** s: asks what to search for. */
    public void askSearch() {
        this.ask(HelpTexts.INFO_SEARCH_PROMPT);
    }

    /** Whether the echo area is asking for something. */
    public boolean asking() {
        return this.prompt != null;
    }

    /** A character typed while the echo area asks. */
    public void typed(final char c) {
        if (this.prompt != null && c >= ' ') {
            this.typing.append(c);
        }
    }

    /** Takes back the last character typed. */
    public void erase() {
        if (!this.typing.isEmpty()) {
            this.typing.setLength(this.typing.length() - 1);
        }
    }

    /** Stops asking. */
    public void cancel() {
        this.prompt = null;
        this.message = "";
    }

    /** Enter while the echo area asks: goes to the menu item named, or searches for what was typed. */
    public void submit() {
        final TextKey asked = this.prompt;
        final String typed = this.typing.toString().strip();
        this.prompt = null;
        this.message = "";
        if (typed.isEmpty()) {
            return;
        }
        if (asked == HelpTexts.INFO_MENU_PROMPT) {
            this.menuItem(typed);
        } else {
            this.search(typed);
        }
    }

    /** A cell was clicked: a link there is picked and followed. */
    public void clicked(final int row, final int column) {
        final int line = this.scroll + row - BODY_TOP - 1;
        final List<Found> links = this.links();
        for (int i = 0; i < links.size(); i++) {
            final Found link = links.get(i);
            if (link.line() == line && column >= link.column()
                    && column < link.column() + link.span().text().length()) {
                this.focus = i;
                this.follow();
                return;
            }
        }
    }

    /** The glass, at that many cells. */
    public TextScreen paint(final int columns, final int rows) {
        // The header row, a blank row under it, the node, the mode line and the echo area.
        this.bodyRows = Math.max(1, rows - 4);
        this.lay(columns);
        final TextScreen screen = new TextScreen(columns, rows, INK, GROUND);
        screen.fill(0, HEADER_ROW, columns, 1, GROUND, INK);
        screen.put(0, HEADER_ROW, this.header(), GROUND, INK);
        final List<Found> links = this.links();
        final Found picked = this.focus >= 0 && this.focus < links.size() ? links.get(this.focus) : null;
        for (int r = 0; r < this.bodyRows && this.scroll + r < this.page.size(); r++) {
            final int line = this.scroll + r;
            int column = 0;
            for (final HelpLine.Span span : this.page.get(line).spans()) {
                final boolean lit = picked != null && picked.line() == line && picked.column() == column;
                screen.put(column, BODY_TOP + 1 + r, span.text(), lit ? GROUND : ink(span.ink()), lit ? INK : GROUND);
                column += span.text().length();
            }
        }
        final String mode = "-----" + say(HelpTexts.INFO_MODE, this.file(), this.nodeName(), this.page.size(),
                this.where()) + "-----";
        screen.fill(0, rows - 2, columns, 1, GROUND, INK);
        screen.put(0, rows - 2, mode + "-".repeat(Math.max(0, columns - mode.length())), GROUND, INK);
        screen.put(0, rows - 1, this.prompt != null ? say(this.prompt, this.typing + "_") : this.message, INK,
                GROUND);
        return screen;
    }

    /* How it works */

    private static int ink(final HelpLine.Ink ink) {
        return switch (ink) {
            case TITLE, HEADING -> BRIGHT;
            case RULE, BODY, NOTE, TABLE -> INK;
            case WARNING -> WARNING;
            case LINK -> LINK;
            case DIM -> DIM;
        };
    }

    private String say(final TextKey key, final Object... args) {
        return (args.length == 0 ? key.text() : key.with(args)).resolve(this.language);
    }

    private void ask(final TextKey what) {
        this.prompt = what;
        this.typing.setLength(0);
    }

    private void go(final HelpTarget target) {
        if (!target.equals(this.shown) || this.keys) {
            this.last.add(this.shown);
            this.shown = target;
        }
        this.keys = false;
        this.message = "";
        this.reset();
    }

    private void reset() {
        this.scroll = 0;
        this.focus = -1;
        this.laidFor = -1;
    }

    /** A node by what was typed: a manual's title or file, "commands", a command, or an entry of any manual. */
    private Optional<HelpTarget> named(final String asked) {
        final String wanted = asked.toLowerCase(Locale.ROOT);
        for (final ManualReader reader : this.source.manuals()) {
            if (reader.title().equalsIgnoreCase(asked) || fileOf(reader.manualId()).equals(wanted)) {
                return Optional.of(HelpTarget.contents(reader.manualId()));
            }
        }
        if (wanted.equals("commands")) {
            return Optional.of(COMMANDS);
        }
        for (final ManualReader reader : this.source.manuals()) {
            final Optional<String> found = reader.find(asked);
            if (found.isPresent()) {
                return Optional.of(HelpTarget.node(reader.manualId(), found.get()));
            }
        }
        for (final HelpCommand command : this.source.commands()) {
            if (command.name().equalsIgnoreCase(asked)) {
                return Optional.of(HelpTarget.command(command.name()));
            }
        }
        return Optional.empty();
    }

    /** m: the item of this node's menu whose name starts with what was typed, or any node of that name. */
    private void menuItem(final String typed) {
        final String wanted = typed.toLowerCase(Locale.ROOT);
        for (final Found link : this.links()) {
            final String text = link.span().text().toLowerCase(Locale.ROOT);
            if (text.startsWith(wanted) || text.contains(" " + wanted)) {
                HelpTarget.read(link.span().link()).ifPresent(this::go);
                return;
            }
        }
        final Optional<HelpTarget> found = this.named(typed);
        if (found.isPresent()) {
            this.go(found.get());
        } else {
            this.message = say(HelpTexts.INFO_NO_ITEM, typed, this.nodeName());
        }
    }

    /** s: the next entry of the manual being read holding every word typed, after this node and going round. */
    private void search(final String typed) {
        final Optional<ManualReader> reader = this.reader(this.shown.manual());
        if (reader.isEmpty()) {
            this.message = say(HelpTexts.INFO_SEARCH_FAILED, typed);
            return;
        }
        final List<String> found = reader.get().search(typed);
        if (found.isEmpty()) {
            this.message = say(HelpTexts.INFO_SEARCH_FAILED, typed);
            return;
        }
        final List<String> order = reader.get().entries();
        final int here = this.shown.kind() == HelpTarget.Kind.NODE ? order.indexOf(this.shown.id()) : -1;
        String pick = found.getFirst();
        for (final String entry : found) {
            if (order.indexOf(entry) > here) {
                pick = entry;
                break;
            }
        }
        this.go(HelpTarget.node(reader.get().manualId(), pick));
    }

    private Optional<ManualReader> reader(final String manual) {
        for (final ManualReader reader : this.source.manuals()) {
            if (reader.manualId().equals(manual)) {
                return Optional.of(reader);
            }
        }
        return manual.isEmpty() && !this.source.manuals().isEmpty()
                ? Optional.of(this.source.manuals().getFirst()) : Optional.empty();
    }

    /** The info file a manual is: the last part of its id, its words joined by hyphens. */
    private static String fileOf(final String manual) {
        return GuideIds.path(manual).replace('_', '-');
    }

    /** The node a node is under: its section or chapter, a manual's top, or the directory. */
    private Optional<HelpTarget> parent(final HelpTarget node) {
        return switch (node.kind()) {
            case NODE -> {
                final Optional<ManualReader> reader = this.reader(node.manual());
                yield reader.isEmpty() ? Optional.empty() : Optional.of(reader.get().parent(node.id())
                        .map(id -> HelpTarget.node(node.manual(), id))
                        .orElse(HelpTarget.contents(node.manual())));
            }
            case CONTENTS -> node.manual().isEmpty() ? Optional.empty() : Optional.of(DIRECTORY);
            case COMMAND -> node.id().isEmpty() ? Optional.of(DIRECTORY) : Optional.of(COMMANDS);
            case INDEX -> Optional.of(HelpTarget.contents(node.manual()));
        };
    }

    /** The nodes at the same level as this one, in order: what n and p walk. */
    private List<HelpTarget> siblings(final HelpTarget node) {
        final List<HelpTarget> out = new ArrayList<>();
        if (node.kind() == HelpTarget.Kind.COMMAND && !node.id().isEmpty()) {
            for (final HelpCommand command : this.source.commands()) {
                out.add(HelpTarget.command(command.name()));
            }
            return out;
        }
        if (node.kind() != HelpTarget.Kind.NODE) {
            return out;
        }
        final Optional<ManualReader> reader = this.reader(node.manual());
        if (reader.isEmpty()) {
            return out;
        }
        final Optional<String> parent = reader.get().parent(node.id());
        final List<String> ids = parent.isPresent()
                ? reader.get().children(parent.get()).stream().map(ManualReader.Node::id).toList()
                : reader.get().chapters();
        for (final String id : ids) {
            out.add(HelpTarget.node(node.manual(), id));
        }
        return out;
    }

    private Optional<HelpTarget> neighbour(final int by) {
        final List<HelpTarget> siblings = this.siblings(this.shown);
        final int at = siblings.indexOf(this.shown);
        final int to = at + by;
        return at < 0 || to < 0 || to >= siblings.size() ? Optional.empty() : Optional.of(siblings.get(to));
    }

    /** How a node is named in a header and on the mode line. */
    private String label(final HelpTarget node) {
        return switch (node.kind()) {
            case NODE -> this.reader(node.manual()).map(reader -> reader.label(node.id())).orElse(node.id());
            case CONTENTS -> node.manual().isEmpty() ? DIRECTORY_NODE : TOP_NODE;
            case COMMAND -> node.id().isEmpty() ? say(HelpTexts.COMMANDS) : node.id();
            case INDEX -> say(HelpTexts.INDEX);
        };
    }

    /** The node's name on the mode line: an entry's title alone, the top of a manual or the directory as Top. */
    private String nodeName() {
        if (this.keys) {
            return say(HelpTexts.INFO_HELP_TITLE);
        }
        if (this.shown.kind() == HelpTarget.Kind.NODE) {
            return this.reader(this.shown.manual()).flatMap(reader -> reader.node(this.shown.id()))
                    .map(ManualReader.Node::title).orElse(this.shown.id());
        }
        return this.shown.kind() == HelpTarget.Kind.CONTENTS ? TOP_NODE : this.label(this.shown);
    }

    private String file() {
        if (this.shown.kind() == HelpTarget.Kind.COMMAND) {
            return "commands";
        }
        return this.shown.manual().isEmpty() ? "dir" : fileOf(this.shown.manual());
    }

    /** The top row: the node's neighbours and what it is under. */
    private String header() {
        if (this.keys) {
            return say(HelpTexts.INFO_HELP_TITLE);
        }
        final List<String> parts = new ArrayList<>();
        this.neighbour(1).ifPresent(next -> parts.add(say(HelpTexts.INFO_NEXT, this.label(next))));
        this.neighbour(-1).ifPresent(previous -> parts.add(say(HelpTexts.INFO_PREV, this.label(previous))));
        this.parent(this.shown).ifPresent(up -> parts.add(say(HelpTexts.INFO_UP, this.label(up))));
        return String.join(",  ", parts);
    }

    /** How far down the node the glass is: all of it, its top, its bottom, or a share. */
    private String where() {
        if (this.page.size() <= this.bodyRows) {
            return say(HelpTexts.INFO_ALL);
        }
        if (this.scroll == 0) {
            return say(HelpTexts.INFO_TOP);
        }
        if (this.scroll + this.bodyRows >= this.page.size()) {
            return say(HelpTexts.INFO_BOTTOM);
        }
        return this.scroll * 100 / this.page.size() + "%";
    }

    private void lay(final int columns) {
        if (this.laidFor == columns && this.laidWith == this.answers) {
            return;
        }
        this.laidFor = columns;
        this.laidWith = this.answers;
        if (this.keys) {
            this.page = new ArrayList<>(List.of(HelpLine.of(0, say(HelpTexts.INFO_HELP_TITLE), HelpLine.Ink.TITLE),
                    HelpLine.BLANK));
            for (final String line : HelpPages.wrap(say(HelpTexts.INFO_HELP_KEYS), columns - 1)) {
                this.page.add(HelpLine.of(0, line, HelpLine.Ink.BODY));
            }
            return;
        }
        this.page = switch (this.shown.kind()) {
            case NODE -> {
                final ManualReader reader = this.reader(this.shown.manual()).orElseThrow();
                yield reader.article(this.shown.id())
                        .map(article -> HelpPages.article(reader, article, HelpPages.Voice.INFO, columns,
                                this.language, this.source::recipeLines))
                        .orElseGet(() -> HelpPages.node(reader, this.shown.id(), HelpPages.Voice.INFO, columns,
                                this.language));
            }
            case CONTENTS -> this.shown.manual().isEmpty() ? this.directoryPage(columns) : this.topPage(columns);
            case COMMAND -> this.shown.id().isEmpty() ? this.commandsPage(columns)
                    : HelpPages.command(this.shown.id(), this.source.commandPage(this.shown.id()),
                    HelpPages.Voice.INFO, columns, this.language);
            case INDEX -> this.reader(this.shown.manual()).map(reader -> HelpPages.index(reader,
                    HelpPages.Voice.INFO, columns, this.language)).orElse(List.of());
        };
        this.scroll(0);
    }

    /** The directory: what it is, and a menu line for every manual and one for the commands. */
    private List<HelpLine> directoryPage(final int columns) {
        final List<HelpLine> lines = new ArrayList<>();
        for (final String line : HelpPages.wrap(say(HelpTexts.INFO_DIR), columns - 1)) {
            lines.add(HelpLine.of(0, line, HelpLine.Ink.BODY));
        }
        for (final String line : HelpPages.wrap(say(HelpTexts.INFO_DIR_HINT), columns - 1)) {
            lines.add(HelpLine.of(0, line, HelpLine.Ink.BODY));
        }
        lines.add(HelpLine.BLANK);
        lines.add(HelpLine.of(0, say(HelpTexts.INFO_MENU), HelpLine.Ink.HEADING));
        lines.add(HelpLine.BLANK);
        for (final ManualReader reader : this.source.manuals()) {
            lines.add(menuLine(reader.title(), HelpTarget.contents(reader.manualId()),
                    "(" + fileOf(reader.manualId()) + ")."));
        }
        lines.add(menuLine(say(HelpTexts.COMMANDS), COMMANDS, "(commands)."));
        return lines;
    }

    /** A manual's top node: its title, and a menu line for every chapter. */
    private List<HelpLine> topPage(final int columns) {
        final ManualReader reader = this.reader(this.shown.manual()).orElseThrow();
        final List<HelpLine> lines = new ArrayList<>();
        lines.add(HelpLine.of(0, reader.title(), HelpLine.Ink.TITLE));
        lines.add(HelpLine.of(0, "*".repeat(Math.min(columns, reader.title().length())), HelpLine.Ink.RULE));
        final List<ManualReader.Node> chapters = new ArrayList<>();
        for (final String chapter : reader.chapters()) {
            reader.node(chapter).ifPresent(chapters::add);
        }
        HelpPages.menu(lines, reader.manualId(), chapters, this.language);
        return lines;
    }

    /** The commands' top node: a menu line for every command, with what it is for. */
    private List<HelpLine> commandsPage(final int columns) {
        final List<HelpLine> lines = new ArrayList<>();
        lines.add(HelpLine.of(0, say(HelpTexts.COMMANDS), HelpLine.Ink.TITLE));
        lines.add(HelpLine.of(0, "*".repeat(say(HelpTexts.COMMANDS).length()), HelpLine.Ink.RULE));
        lines.add(HelpLine.BLANK);
        lines.add(HelpLine.of(0, say(HelpTexts.INFO_MENU), HelpLine.Ink.HEADING));
        lines.add(HelpLine.BLANK);
        if (this.source.commands().isEmpty()) {
            lines.add(HelpLine.of(0, say(HelpTexts.NOTHING_YET), HelpLine.Ink.DIM));
        }
        int widest = 0;
        for (final HelpCommand command : this.source.commands()) {
            widest = Math.max(widest, command.name().length());
        }
        for (final HelpCommand command : this.source.commands()) {
            final String summary = command.summary();
            final int room = Math.max(0, columns - widest - 8);
            lines.add(new HelpLine(List.of(new HelpLine.Span("* ", HelpLine.Ink.BODY, ""),
                    new HelpLine.Span(command.name(), HelpLine.Ink.LINK, HelpTarget.command(command.name())
                            .written()),
                    new HelpLine.Span("::" + " ".repeat(widest - command.name().length() + 2)
                            + (summary.length() > room ? summary.substring(0, room) : summary),
                            HelpLine.Ink.BODY, ""))));
        }
        return lines;
    }

    private static HelpLine menuLine(final String name, final HelpTarget target, final String after) {
        return new HelpLine(List.of(new HelpLine.Span("* ", HelpLine.Ink.BODY, ""),
                new HelpLine.Span(name, HelpLine.Ink.LINK, target.written()),
                new HelpLine.Span(": " + after, HelpLine.Ink.BODY, "")));
    }

    private List<Found> links() {
        final List<Found> out = new ArrayList<>();
        for (int line = 0; line < this.page.size(); line++) {
            for (final HelpLine.Placed placed : this.page.get(line).links()) {
                out.add(new Found(line, placed.column(), placed.span()));
            }
        }
        return out;
    }

    /** A link of the node: its line, the column it starts at, and the piece that is the link. */
    private record Found(int line, int column, HelpLine.Span span) {
    }
}
