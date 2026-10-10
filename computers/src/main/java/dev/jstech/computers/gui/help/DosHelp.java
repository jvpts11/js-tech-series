/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.help;

import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.text.ITextLanguage;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The DOS family's HELP: a whole screen in the sixteen colours, the way the help of DOS 6 looked.
 *
 * <p>A grey menu bar with File and Search and the topic's title, a row of green buttons under it (Next, Back, Contents,
 * Index), the topic in white on blue under yellow headings, its links in green between angle brackets, and the keys
 * on a cyan line at the foot. Tab walks the buttons and the links, Enter follows the one picked, Alt with C, N, B or I
 * does what the buttons do, and Escape leaves.
 *
 * <p>What it shows is the manuals of the player's game, the same entries the binder prints, and the commands of the
 * machine beside them, each command's page the one its {@code /?} prints.
 *
 * <p>Pure: the keys come in as what they mean and the screen goes out as cells, so a test works it with no game.
 */
public final class DosHelp {

    private final String system;
    private final IHelpSource source;
    private final ITextLanguage language;
    /** The topics shown before this one, newest last, which Back walks. */
    private final List<HelpTarget> back = new ArrayList<>();
    private HelpTarget shown;
    private String title = "";
    private List<HelpLine> page = List.of();
    /** The width and the answers the page was laid out for, so it is laid out again when either changes. */
    private int laidFor = -1;
    private int laidWith = -1;
    private int answers;
    private int scroll;
    /** The button or link picked: the four buttons first, then the links of the page in their order; -1 for none. */
    private int focus = -1;
    /** The menu that is down, or -1, and its item picked. */
    private int menu = -1;
    private int menuItem;
    /** What is being typed into Find, or null when nothing is. */
    private StringBuilder asking;
    private String lastFind = "";
    private String message = "";
    private boolean leaving;
    private int bodyRows = DEFAULT_ROWS - 3;

    private static final int DEFAULT_ROWS = 25;
    private static final int MENU_ROW = 0;
    private static final int BUTTON_ROW = 1;
    private static final int BODY_TOP = 2;
    private static final int MENU_LEFT = 1;
    private static final int MENU_GAP = 2;
    private static final int BUTTON_GAP = 2;

    /** The four buttons, in the order Tab reaches them. */
    private static final int NEXT = 0;
    private static final int BACK = 1;
    private static final int CONTENTS = 2;
    private static final int INDEX = 3;
    private static final int BUTTONS = 4;

    /** The two menus and what each holds. */
    private static final int FILE_MENU = 0;
    private static final int SEARCH_MENU = 1;
    private static final List<List<TextKey>> MENU_ITEMS = List.of(List.of(HelpTexts.DOS_EXIT),
            List.of(HelpTexts.DOS_FIND, HelpTexts.DOS_REPEAT));

    private static final int BAR_INK = TextScreen.cga(TextScreen.BLACK);
    private static final int BAR = TextScreen.cga(TextScreen.GREY);
    private static final int GROUND = TextScreen.cga(TextScreen.BLUE);
    private static final int WHITE = TextScreen.cga(TextScreen.WHITE);
    private static final int GREY = TextScreen.cga(TextScreen.GREY);
    private static final int GREEN = TextScreen.cga(TextScreen.LIGHT_GREEN);
    private static final int YELLOW = TextScreen.cga(TextScreen.YELLOW);
    private static final int CYAN = TextScreen.cga(TextScreen.LIGHT_CYAN);
    private static final int RED = TextScreen.cga(TextScreen.LIGHT_RED);
    private static final int FOOT = TextScreen.cga(TextScreen.CYAN);
    private static final int SHADOW = TextScreen.cga(TextScreen.DARK_GREY);
    private static final int SHADOW_GROUND = TextScreen.cga(TextScreen.BLACK);

    /**
     * HELP opened on a topic: an entry, section or chapter of a manual, or a command, by what was typed after it, or
     * the contents when nothing was.
     *
     * @param system the system's name for the title bar, "MC-DOS" or "MC-NET"
     */
    public DosHelp(final String system, final String topic, final IHelpSource source,
                   final ITextLanguage language) {
        this.system = system;
        this.source = source;
        this.language = language;
        this.shown = this.opening(topic == null ? "" : topic.strip());
    }

    /** What the screen shows now. */
    public HelpTarget shown() {
        return this.shown;
    }

    /** The topic's title, as the title bar names it. */
    public String title() {
        return this.title;
    }

    /** The lines of the topic, laid out for the width last drawn at. */
    public List<HelpLine> page() {
        return this.page;
    }

    /** Whether Exit was chosen, so the terminal is to be given back. */
    public boolean leaving() {
        return this.leaving;
    }

    /** What the foot of the screen says instead of the keys, or empty. */
    public String message() {
        return this.message;
    }

    /** The machine answered, so a page of a command, or the list of them, may be there now. */
    public void answered() {
        this.answers++;
    }

    /* What the keys do */

    /** The next topic: the entry printed after this one, or the next command. */
    public void next() {
        this.message = "";
        switch (this.shown.kind()) {
            case NODE -> this.reader(this.shown.manual()).flatMap(reader -> this.after(reader, this.shown.id()))
                    .ifPresent(this::go);
            case COMMAND -> {
                final List<HelpCommand> commands = this.source.commands();
                for (int i = 0; i + 1 < commands.size(); i++) {
                    if (commands.get(i).name().equalsIgnoreCase(this.shown.id())) {
                        this.go(HelpTarget.command(commands.get(i + 1).name()));
                        return;
                    }
                }
            }
            case CONTENTS, INDEX -> {
            }
        }
    }

    /** The topic shown before this one. */
    public void back() {
        this.message = "";
        if (!this.back.isEmpty()) {
            this.shown = this.back.removeLast();
            this.reset();
        }
    }

    /** The contents of the manual being read. */
    public void contents() {
        this.message = "";
        this.go(HelpTarget.contents(this.manualShown()));
    }

    /** The index of the manual being read. */
    public void index() {
        this.message = "";
        this.go(HelpTarget.index(this.manualShown()));
    }

    /** Picks the next button or link, or the one before it, going round. */
    public void focusNext(final boolean backwards) {
        final int count = BUTTONS + this.links().size();
        if (this.focus < 0) {
            this.focus = backwards ? count - 1 : (this.links().isEmpty() ? 0 : BUTTONS);
        } else {
            this.focus = Math.floorMod(this.focus + (backwards ? -1 : 1), count);
        }
        this.keepFocusInView();
    }

    /** Does what the picked button says, or follows the picked link. */
    public void follow() {
        if (this.focus < 0) {
            return;
        }
        if (this.focus < BUTTONS) {
            this.press(this.focus);
            return;
        }
        final List<Found> links = this.links();
        if (this.focus - BUTTONS < links.size()) {
            HelpTarget.read(links.get(this.focus - BUTTONS).span().link()).ifPresent(this::go);
        }
    }

    /** Moves the topic up or down that many lines. */
    public void scroll(final int lines) {
        this.scroll = Math.max(0, Math.min(this.scroll + lines, Math.max(0, this.page.size() - this.bodyRows)));
    }

    /** Moves it a screen. */
    public void page(final boolean down) {
        this.scroll(down ? this.bodyRows - 1 : 1 - this.bodyRows);
    }

    /** Puts a menu down, or takes it up when it is the one down. */
    public void openMenu(final int which) {
        this.menu = this.menu == which ? -1 : which;
        this.menuItem = 0;
    }

    /** Whether a menu is down. */
    public boolean menuOpen() {
        return this.menu >= 0;
    }

    /** Moves the picked item of the menu that is down. */
    public void menuMove(final int by) {
        if (this.menu >= 0) {
            this.menuItem = Math.floorMod(this.menuItem + by, MENU_ITEMS.get(this.menu).size());
        }
    }

    /** Moves to the menu beside the one down. */
    public void menuAcross(final int by) {
        if (this.menu >= 0) {
            this.menu = Math.floorMod(this.menu + by, MENU_ITEMS.size());
            this.menuItem = 0;
        }
    }

    /** Takes the menu that is down back up. */
    public void closeMenu() {
        this.menu = -1;
    }

    /** Does what the picked item of the menu that is down says. */
    public void pickMenuItem() {
        final int which = this.menu;
        final int item = this.menuItem;
        this.menu = -1;
        if (which == FILE_MENU) {
            this.leaving = true;
        } else if (which == SEARCH_MENU && item == 0) {
            this.startFind();
        } else if (which == SEARCH_MENU) {
            this.findAgain();
        }
    }

    /** Asks what to find, on the line at the foot. */
    public void startFind() {
        this.asking = new StringBuilder();
        this.message = "";
    }

    /** Whether Find is asking. */
    public boolean finding() {
        return this.asking != null;
    }

    /** A character typed while Find is asking. */
    public void typed(final char c) {
        if (this.asking != null && c >= ' ') {
            this.asking.append(c);
        }
    }

    /** Takes back the last character typed into Find. */
    public void erase() {
        if (this.asking != null && !this.asking.isEmpty()) {
            this.asking.setLength(this.asking.length() - 1);
        }
    }

    /** Stops asking. */
    public void cancelFind() {
        this.asking = null;
    }

    /** Looks for what was typed into Find. */
    public void submitFind() {
        if (this.asking == null) {
            return;
        }
        this.lastFind = this.asking.toString().strip();
        this.asking = null;
        this.findAgain();
    }

    /**
     * Looks for what was last asked for: a command of that name, or the next entry of the manual being read whose
     * title or text holds every word of it, after the one shown and going round.
     */
    public void findAgain() {
        this.message = "";
        if (this.lastFind.isEmpty()) {
            this.startFind();
            return;
        }
        for (final HelpCommand command : this.source.commands()) {
            if (command.name().equalsIgnoreCase(this.lastFind)) {
                this.go(HelpTarget.command(command.name()));
                return;
            }
        }
        final Optional<ManualReader> reader = this.reader(this.manualShown());
        if (reader.isEmpty()) {
            this.message = say(HelpTexts.DOS_NOT_FOUND);
            return;
        }
        final List<String> found = reader.get().search(this.lastFind);
        if (found.isEmpty()) {
            this.message = say(HelpTexts.DOS_NOT_FOUND);
            return;
        }
        final List<String> order = reader.get().entries();
        final String pick = HelpSearch.nextHit(order, found,
                this.shown.kind() == HelpTarget.Kind.NODE ? this.shown.id() : null);
        this.go(HelpTarget.node(reader.get().manualId(), pick));
    }

    /** A cell of the screen was clicked: a menu, a button, a link or an item of the menu that is down. */
    public void clicked(final int row, final int column) {
        if (this.menu >= 0) {
            final int[] box = this.menuBox(this.menu);
            final int item = row - box[1] - 1;
            if (column > box[0] && column < box[0] + box[2] - 1 && item >= 0
                    && item < MENU_ITEMS.get(this.menu).size()) {
                this.menuItem = item;
                this.pickMenuItem();
                return;
            }
        }
        if (row == MENU_ROW) {
            for (int i = 0; i < MENU_ITEMS.size(); i++) {
                final int at = this.menuX(i);
                if (column >= at && column < at + this.menuTitle(i).length()) {
                    this.openMenu(i);
                    return;
                }
            }
            this.menu = -1;
            return;
        }
        this.menu = -1;
        if (row == BUTTON_ROW) {
            for (int i = 0; i < BUTTONS; i++) {
                final int at = this.buttonX(i);
                if (column >= at && column < at + this.button(i).length()) {
                    this.focus = i;
                    this.press(i);
                    return;
                }
            }
            return;
        }
        final int line = this.scroll + row - BODY_TOP;
        final List<Found> links = this.links();
        for (int i = 0; i < links.size(); i++) {
            final Found link = links.get(i);
            if (link.line() == line && column >= link.column()
                    && column < link.column() + link.span().text().length()) {
                this.focus = BUTTONS + i;
                this.follow();
                return;
            }
        }
    }

    /** The screen, at that many cells: laid out again first when the width or the machine's answers changed. */
    public TextScreen paint(final int columns, final int rows) {
        this.bodyRows = Math.max(1, rows - 3);
        this.lay(columns);
        final TextScreen screen = new TextScreen(columns, rows, WHITE, GROUND);
        // The menu bar, with the topic's title in its middle.
        screen.fill(0, MENU_ROW, columns, 1, BAR_INK, BAR);
        for (int i = 0; i < MENU_ITEMS.size(); i++) {
            final boolean down = this.menu == i;
            screen.put(this.menuX(i), MENU_ROW, this.menuTitle(i), down ? BAR : BAR_INK, down ? BAR_INK : BAR);
        }
        final String heading = say(HelpTexts.DOS_TITLE, this.system, this.title);
        final int menusEnd = this.menuX(MENU_ITEMS.size() - 1) + this.menuTitle(MENU_ITEMS.size() - 1).length();
        screen.put(Math.max(menusEnd + MENU_GAP, (columns - heading.length()) / 2), MENU_ROW, heading, BAR_INK, BAR);
        // The buttons.
        for (int i = 0; i < BUTTONS; i++) {
            final boolean picked = this.focus == i;
            screen.put(this.buttonX(i), BUTTON_ROW, this.button(i), picked ? GROUND : GREEN, picked ? BAR : GROUND);
        }
        // The topic.
        final List<Found> links = this.links();
        final Found picked = this.focus >= BUTTONS && this.focus - BUTTONS < links.size()
                ? links.get(this.focus - BUTTONS) : null;
        for (int r = 0; r < this.bodyRows && this.scroll + r < this.page.size(); r++) {
            final int line = this.scroll + r;
            int column = 0;
            for (final HelpLine.Span span : this.page.get(line).spans()) {
                final boolean lit = picked != null && picked.line() == line && picked.column() == column;
                screen.put(column, BODY_TOP + r, span.text(), lit ? GROUND : ink(span.ink()), lit ? BAR : GROUND);
                column += span.text().length();
            }
        }
        // The foot: what Find asks, what was said, or the keys.
        screen.fill(0, rows - 1, columns, 1, BAR_INK, FOOT);
        final String foot = this.asking != null ? say(HelpTexts.DOS_FIND_PROMPT, this.asking + "_")
                : this.message.isEmpty() ? say(HelpTexts.DOS_KEYS) : this.message;
        screen.put(0, rows - 1, foot, BAR_INK, FOOT);
        if (this.menu >= 0) {
            this.drawMenu(screen);
        }
        return screen;
    }

    /* How it works */

    private static int ink(final HelpLine.Ink ink) {
        return switch (ink) {
            case TITLE, BODY -> WHITE;
            case RULE, DIM -> GREY;
            case HEADING -> YELLOW;
            case NOTE, TABLE -> CYAN;
            case WARNING -> RED;
            case LINK -> GREEN;
        };
    }

    private String say(final TextKey key, final Object... args) {
        return (args.length == 0 ? key.text() : key.with(args)).resolve(this.language);
    }

    /** Where HELP opens: the contents, or the topic typed, a command when no manual has one of that name. */
    private HelpTarget opening(final String topic) {
        final List<ManualReader> manuals = this.source.manuals();
        final String first = manuals.isEmpty() ? "" : manuals.getFirst().manualId();
        if (topic.isEmpty()) {
            return HelpTarget.contents(first);
        }
        for (final ManualReader reader : manuals) {
            final Optional<String> found = reader.find(topic);
            if (found.isPresent()) {
                return HelpTarget.node(reader.manualId(), found.get());
            }
        }
        return HelpTarget.command(topic.toLowerCase(Locale.ROOT));
    }

    private void go(final HelpTarget target) {
        this.message = "";
        if (!target.equals(this.shown)) {
            this.back.add(this.shown);
            this.shown = target;
        }
        this.reset();
    }

    private void reset() {
        this.scroll = 0;
        this.focus = -1;
        this.laidFor = -1;
    }

    private void press(final int button) {
        switch (button) {
            case NEXT -> this.next();
            case BACK -> this.back();
            case CONTENTS -> this.contents();
            default -> this.index();
        }
    }

    /** The manual being read: the one the topic is in, or the first for a command. */
    private String manualShown() {
        if (!this.shown.manual().isEmpty()) {
            return this.shown.manual();
        }
        final List<ManualReader> manuals = this.source.manuals();
        return manuals.isEmpty() ? "" : manuals.getFirst().manualId();
    }

    private Optional<ManualReader> reader(final String manual) {
        final List<ManualReader> manuals = this.source.manuals();
        for (final ManualReader reader : manuals) {
            if (reader.manualId().equals(manual)) {
                return Optional.of(reader);
            }
        }
        return manuals.isEmpty() ? Optional.empty() : Optional.of(manuals.getFirst());
    }

    /** The entry after a node: the next entry for an entry, the first one inside for a chapter or a section. */
    private Optional<HelpTarget> after(final ManualReader reader, final String id) {
        final Optional<ManualReader.Node> node = reader.node(id);
        if (node.isEmpty()) {
            return Optional.empty();
        }
        if (node.get().kind() == ManualReader.Kind.ENTRY) {
            return reader.next(id).map(next -> HelpTarget.node(reader.manualId(), next));
        }
        final String inside = node.get().number() + ".";
        for (final String entry : reader.entries()) {
            if (reader.node(entry).map(ManualReader.Node::number).orElse("").startsWith(inside)) {
                return Optional.of(HelpTarget.node(reader.manualId(), entry));
            }
        }
        return Optional.empty();
    }

    /** Lays the topic out for that width, when it was laid out for another or the machine has said more since. */
    private void lay(final int columns) {
        if (this.laidFor == columns && this.laidWith == this.answers) {
            return;
        }
        this.laidFor = columns;
        this.laidWith = this.answers;
        final List<HelpCommand> commands = this.source.commands();
        if (this.shown.kind() == HelpTarget.Kind.COMMAND && !commands.isEmpty()
                && commands.stream().noneMatch(command -> command.name().equalsIgnoreCase(this.shown.id()))) {
            // Neither a manual nor the machine has anything by that name: the contents, and the foot says so.
            this.message = say(HelpTexts.DOS_NOT_FOUND);
            this.shown = HelpTarget.contents(this.manualShown());
        }
        final Optional<ManualReader> reader = this.reader(this.shown.manual());
        switch (this.shown.kind()) {
            case NODE -> {
                final ManualReader read = reader.orElseThrow();
                this.title = read.label(this.shown.id());
                this.page = read.article(this.shown.id())
                        .map(article -> HelpPages.article(read, article, HelpPages.Voice.DOS, columns, this.language,
                                this.source::recipeLines))
                        .orElseGet(() -> HelpPages.node(read, this.shown.id(), HelpPages.Voice.DOS, columns,
                                this.language));
            }
            case COMMAND -> {
                this.title = this.shown.id().toUpperCase(Locale.ROOT);
                this.page = HelpPages.command(this.shown.id(), this.source.commandPage(this.shown.id()),
                        HelpPages.Voice.DOS, columns, this.language);
            }
            case CONTENTS -> {
                this.title = say(HelpTexts.CONTENTS);
                this.page = reader.map(read -> HelpPages.contents(read, this.others(read), commands, columns,
                        this.language)).orElse(List.of());
            }
            case INDEX -> {
                this.title = say(HelpTexts.INDEX);
                this.page = reader.map(read -> HelpPages.index(read, HelpPages.Voice.DOS, columns, this.language))
                        .orElse(List.of());
            }
        }
        this.scroll(0);
    }

    private List<ManualReader> others(final ManualReader reader) {
        final List<ManualReader> others = new ArrayList<>(this.source.manuals());
        others.remove(reader);
        return others;
    }

    /** Every link of the topic, where it stands, in the order Tab reaches them. */
    private List<Found> links() {
        final List<Found> out = new ArrayList<>();
        for (int line = 0; line < this.page.size(); line++) {
            for (final HelpLine.Placed placed : this.page.get(line).links()) {
                out.add(new Found(line, placed.column(), placed.span()));
            }
        }
        return out;
    }

    private void keepFocusInView() {
        if (this.focus < BUTTONS) {
            return;
        }
        final List<Found> links = this.links();
        if (this.focus - BUTTONS >= links.size()) {
            return;
        }
        final int line = links.get(this.focus - BUTTONS).line();
        if (line < this.scroll) {
            this.scroll = line;
        } else if (line >= this.scroll + this.bodyRows) {
            this.scroll = line - this.bodyRows + 1;
        }
    }

    private String button(final int which) {
        return "<" + say(switch (which) {
            case NEXT -> HelpTexts.DOS_NEXT;
            case BACK -> HelpTexts.DOS_BACK;
            case CONTENTS -> HelpTexts.CONTENTS;
            default -> HelpTexts.INDEX;
        }) + ">";
    }

    private int buttonX(final int which) {
        int x = MENU_LEFT;
        for (int i = 0; i < which; i++) {
            x += this.button(i).length() + BUTTON_GAP;
        }
        return x;
    }

    private String menuTitle(final int which) {
        return say(which == FILE_MENU ? HelpTexts.DOS_FILE : HelpTexts.DOS_SEARCH);
    }

    private int menuX(final int which) {
        int x = MENU_LEFT;
        for (int i = 0; i < which; i++) {
            x += this.menuTitle(i).length() + MENU_GAP;
        }
        return x;
    }

    /** Where the menu sits when it is down: its left column, its top row, its width and its height. */
    private int[] menuBox(final int which) {
        int wide = this.menuTitle(which).length();
        for (final TextKey item : MENU_ITEMS.get(which)) {
            wide = Math.max(wide, say(item).length());
        }
        return new int[] {this.menuX(which) - 1, MENU_ROW + 1, wide + 4, MENU_ITEMS.get(which).size() + 2};
    }

    private void drawMenu(final TextScreen screen) {
        final int[] box = this.menuBox(this.menu);
        screen.shadow(box[0], box[1], box[2], box[3], SHADOW, SHADOW_GROUND);
        screen.box(box[0], box[1], box[2], box[3], BAR_INK, BAR, false);
        final List<TextKey> items = MENU_ITEMS.get(this.menu);
        for (int i = 0; i < items.size(); i++) {
            final boolean picked = i == this.menuItem;
            final String label = " " + say(items.get(i));
            final String padded = label + " ".repeat(Math.max(0, box[2] - 2 - label.length()));
            screen.put(box[0] + 1, box[1] + 1 + i, padded, picked ? BAR : BAR_INK, picked ? BAR_INK : BAR);
        }
    }

    /** A link of the topic: its line, the column it starts at, and the piece that is the link. */
    private record Found(int line, int column, HelpLine.Span span) {
    }
}
