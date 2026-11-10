/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.program.DebugRequestPayload;
import dev.jstech.computers.operation.payload.program.DebugStatePayload;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The debugger as Virtual Studio draws it: Continue, the configuration and the steps on the toolbar; Locals and Watch
 * beside Call Stack, Breakpoints and Output in the dock; the Diagnostic Tools where the Solution Explorer was, with the
 * credits spent and on what; and the status bar in the debugger's colour, saying why the program stopped.
 *
 * <p>The studio hands this its window's parts and asks it to lay them out and draw them while a program is being
 * debugged; between debugging, only the configuration on the toolbar is this one's.
 */
@PaletteHolder
final class VirtualStudioDebug {

    private final StudioDebug debug;
    private final CodeWorkspace workspace;
    private final Supplier<OsSkin> skin;
    /** Where a file a listing names is on the disk, or null when the solution has no such file. */
    private final Function<String, String> pathOf;
    private final Button config;
    private final Button proceed;
    private final List<Button> steps;
    private final TabStrip leftTabs;
    private final TabStrip rightTabs;
    private final ListView<DebugStatePayload.Variable> locals;
    private final ListView<DebugStatePayload.Variable> watch;
    private final ListView<DebugStatePayload.Frame> stack;
    private final ListView<Map.Entry<String, Integer>> breakpoints;
    private final ListView<String> output;
    private final TextField input = new TextField(DebugRequestPayload.MOST_LINE);
    /** Whether the build is the Release one, which defines nothing for the debugger. */
    private boolean release;
    /** The stop the editor was last moved to, so a new stop moves it once and leaves the player to scroll after. */
    private String followed = "";

    private static final int TAB_H = 10;
    private static final int CAPTION_H = 9;
    private static final int ROW_H = 9;
    private static final int BUTTON_W = 13;
    private static final int LOCALS_TAB = 0;
    private static final int WATCH_TAB = 1;
    private static final int STACK_TAB = 0;
    private static final int BREAKPOINT_TAB = 1;
    private static final int OUTPUT_TAB = 2;
    /** The colours of debugging, {@code jsc:editor/studio_debug}: the status bar's ground and its text. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "editor/studio_debug",
            new Colours(0xFFCA5100, 0xFFFFFFFF));

    VirtualStudioDebug(final Panel root, final StudioDebug debug, final CodeWorkspace workspace,
                       final Supplier<OsSkin> skin, final Function<String, String> pathOf) {
        this.debug = debug;
        this.workspace = workspace;
        this.skin = skin;
        this.pathOf = pathOf;
        this.config = root.add(new Button(() -> GameText.resolve(this.release ? VirtualStudioTexts.RELEASE
                : DebugTexts.DEBUG) + " v", () -> this.release = !this.release).setLabelScale(0.75f));
        this.proceed = root.add(new Button(GameText.resolve(DebugTexts.CONTINUE), debug::resume).setPrimary(true)
                .setLabelScale(0.75f));
        this.steps = List.of(root.add(new Button("||", debug::pause)), root.add(new Button("■", debug::stop)),
                root.add(new Button("↺", debug::restart)), root.add(new Button("↓", debug::stepInto)),
                root.add(new Button("↷", debug::stepOver)), root.add(new Button("↑", debug::stepOut)));
        this.leftTabs = root.add(new TabStrip(List.of(GameText.resolve(DebugTexts.LOCALS),
                GameText.resolve(DebugTexts.WATCH))).fitToLabels(10).setUnderline(true));
        this.rightTabs = root.add(new TabStrip(List.of(GameText.resolve(DebugTexts.CALL_STACK),
                GameText.resolve(DebugTexts.BREAKPOINTS), GameText.resolve(DebugTexts.OUTPUT))).fitToLabels(10)
                .setUnderline(true));
        this.locals = root.add(new ListView<>(this::localRows, ROW_H, this::drawVariable));
        this.watch = root.add(new ListView<>(this::watchRows, ROW_H, this::drawVariable))
                .setOnClick((index, button, mx, my) -> this.debug.removeWatch(index));
        this.stack = root.add(new ListView<>(this::frames, ROW_H, this::drawFrame))
                .setOnClick((index, button, mx, my) -> this.showFrame(index));
        this.breakpoints = root.add(new ListView<>(debug::allBreakpoints, ROW_H, this::drawBreakpoint))
                .setOnClick((index, button, mx, my) -> this.showBreakpoint(index));
        this.output = root.add(new ListView<>(debug::output, ROW_H, this::drawOutput));
        this.input.setPlaceholder(GameText.resolve(DebugTexts.WAITING)).setOnCommit(line -> {
            this.debug.input(line);
            this.input.set("");
        });
        root.add(this.input);
    }

    /** Whether the build is the Release one; the Debug one, the default, is built for the debugger. */
    boolean release() {
        return this.release;
    }

    /** Whether a program is being debugged, which is when the debugger's panes take the window's places. */
    boolean shows() {
        return this.debug.active();
    }

    /** Hides or shows the debugger's own parts; the configuration shows whenever the toolbar does. */
    void setVisible(final boolean toolbar) {
        final boolean on = toolbar && this.shows();
        this.config.setVisible(toolbar);
        this.proceed.setVisible(on);
        this.steps.forEach(button -> button.setVisible(on));
        this.leftTabs.setVisible(on);
        this.rightTabs.setVisible(on);
        this.locals.setVisible(on && this.leftTabs.selected() == LOCALS_TAB);
        this.watch.setVisible(on && this.leftTabs.selected() == WATCH_TAB);
        this.stack.setVisible(on && this.rightTabs.selected() == STACK_TAB);
        this.breakpoints.setVisible(on && this.rightTabs.selected() == BREAKPOINT_TAB);
        this.output.setVisible(on && this.rightTabs.selected() == OUTPUT_TAB);
        this.input.setVisible(on && this.rightTabs.selected() == OUTPUT_TAB && this.waiting());
    }

    /**
     * Lays the toolbar out from {@code x}: Continue, where the studio's Start stands while debugging, then the
     * configuration, then while debugging the buttons of the steps.
     */
    void layoutToolbar(final int x, final int y, final int height) {
        this.proceed.setBounds(x, y, 34, height);
        this.config.setBounds(x + 39, y, 40, height);
        int at = x + 83;
        for (final Button step : this.steps) {
            step.setBounds(at, y, BUTTON_W, height);
            at += BUTTON_W + 1;
        }
    }

    /** Lays out the dock: Locals and Watch on the left half, Call Stack, Breakpoints and Output on the right. */
    void layoutDock(final int x, final int y, final int width, final int height) {
        final int half = width / 2;
        final int listTop = y + TAB_H + CAPTION_H;
        final int listH = Math.max(0, height - TAB_H - CAPTION_H);
        this.leftTabs.setBounds(x, y, half, TAB_H);
        this.locals.setBounds(x, listTop, half, listH);
        this.watch.setBounds(x, listTop, half, listH);
        this.rightTabs.setBounds(x + half, y, width - half, TAB_H);
        this.stack.setBounds(x + half, listTop, width - half, listH);
        this.breakpoints.setBounds(x + half, y + TAB_H, width - half, Math.max(0, height - TAB_H));
        final int inputH = this.waiting() ? 11 : 0;
        this.output.setBounds(x + half, y + TAB_H, width - half, Math.max(0, height - TAB_H - inputH));
        this.input.setBounds(x + half + 1, y + height - inputH, width - half - 2, inputH);
    }

    /** Draws the dock's column headings, under the tabs: a list of values has three, the stack two. */
    void drawDockHeadings(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        final OsSkin look = this.skin.get();
        final int half = width / 2;
        final int top = y + TAB_H;
        look.panel(g, x, top, half, CAPTION_H);
        Draw.text(g, font, GameText.resolve(DebugTexts.NAME), x + 3, top + 1, look.dim());
        Draw.text(g, font, GameText.resolve(DebugTexts.VALUE), x + half * 2 / 5, top + 1, look.dim());
        Draw.text(g, font, GameText.resolve(DebugTexts.TYPE), x + half * 3 / 4, top + 1, look.dim());
        if (this.rightTabs.selected() == STACK_TAB) {
            look.panel(g, x + half, top, width - half, CAPTION_H);
            Draw.text(g, font, GameText.resolve(DebugTexts.NAME), x + half + 3, top + 1, look.dim());
            Draw.text(g, font, GameText.resolve(DebugTexts.LINE), x + width - 24, top + 1, look.dim());
        }
    }

    /**
     * The Diagnostic Tools, where the Solution Explorer stands while debugging: the credits the program has spent,
     * split between the calls it made and the steps themselves, its memory and how long it has run, and what happened.
     */
    void drawDiagnostics(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                         final int height) {
        final OsSkin look = this.skin.get();
        final DebugStatePayload state = this.debug.state();
        look.panel(g, x, y, width, height);
        Draw.pushScissor(g, x, y, x + width, y + height);
        int row = y + 1;
        Draw.text(g, font, GameText.resolve(DebugTexts.DIAGNOSTIC_TOOLS), x + 3, row, look.dim());
        row += ROW_H + 3;
        row = this.figure(g, font, x + 3, row, width - 6, GameText.resolve(DebugTexts.CREDITS_SPENT),
                String.valueOf(state.spent()), look.text());
        for (final DebugStatePayload.Cost cost : state.calls()) {
            row = this.figure(g, font, x + 9, row, width - 12, cost.name(), String.valueOf(cost.credits()),
                    look.dim());
        }
        row = this.figure(g, font, x + 9, row, width - 12, GameText.resolve(DebugTexts.THE_STEPS),
                String.valueOf(state.steps()), look.dim());
        row = this.figure(g, font, x + 3, row, width - 6, GameText.resolve(DebugTexts.PROCESS_MEMORY),
                kilobytes(state.memory()), look.text());
        row = this.figure(g, font, x + 3, row, width - 6, GameText.resolve(DebugTexts.ELAPSED),
                GameText.resolve(DebugTexts.TICKS.with(state.elapsed())), look.text());
        row += 4;
        Draw.text(g, font, GameText.resolve(DebugTexts.EVENTS), x + 3, row, look.dim());
        row += ROW_H + 1;
        final List<String> events = this.debug.events();
        final int room = Math.max(0, (y + height - row) / ROW_H);
        for (final String event : events.subList(Math.max(0, events.size() - room), events.size())) {
            Draw.text(g, font, font.plainSubstrByWidth(event, width - 6), x + 3, row, look.text());
            row += ROW_H;
        }
        Draw.popScissor(g);
    }

    /**
     * The status bar while debugging, in the debugger's colour: why the program stopped, or that it runs, on the left;
     * what it has spent, where the caret is and the solution's name on the right.
     */
    void drawStatus(final GuiGraphics g, final Font font, final int x, final int y, final int width, final int height,
                    final String where, final String solution) {
        final DebugStatePayload state = this.debug.state();
        final int ink = PALETTE.get().statusText();
        g.fill(x, y, x + width, y + height, PALETTE.get().status());
        final String said = GameText.resolve(state.status() != DebugStatePayload.PAUSED ? DebugTexts.STATUS_RUNNING
                : "breakpoint".equals(state.reason()) ? DebugTexts.STATUS_BREAKPOINT : DebugTexts.STATUS_PAUSED);
        Draw.text(g, font, said, x + 3, y + 1, ink);
        int right = x + width - 3 - font.width(solution);
        Draw.text(g, font, solution, right, y + 1, ink);
        right -= 10 + font.width(where);
        Draw.text(g, font, where, right, y + 1, ink);
        final String credits = GameText.resolve(DebugTexts.STATUS_CREDITS.with(state.spent()));
        right -= 10 + font.width(credits);
        if (right > x + 8 + font.width(said)) {
            Draw.text(g, font, credits, right, y + 1, ink);
        }
    }

    /**
     * Marks the open file's breakpoints and stopped line in the editor, and when the program has stopped somewhere new
     * opens the file it stopped in at that line, as a debugger brings the stop into view.
     */
    void follow() {
        final DebugStatePayload state = this.debug.state();
        if (this.debug.paused() && !state.stack().isEmpty()) {
            final DebugStatePayload.Frame top = state.stack().getFirst();
            final String stop = top.file() + ":" + top.line() + ":" + top.column() + ":" + state.spent();
            if (!stop.equals(this.followed)) {
                this.followed = stop;
                this.open(top);
            }
        }
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            doc.area().setBreakpoints(this.debug.breakpointsIn(doc.path()))
                    .setStoppedAt(this.debug.stoppedLineIn(doc.path()))
                    .setOnMarginClick(line -> this.debug.toggle(doc.path(), line));
        }
    }

    /** The keys of the debugger, as both studios have them; says whether one was taken. */
    boolean keyPressed(final int key, final int modifiers, final Runnable start, final Runnable startWithout) {
        return this.debug.keyPressed(key, modifiers, start, startWithout, this::toggleAtCaret);
    }

    /** Sets or takes away the breakpoint on the caret's line of the open file. */
    void toggleAtCaret() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            this.debug.toggle(doc.path(), doc.area().document().cursorLine() + 1);
        }
    }

    /** Shows the Output pane, where a program waiting for a line is typed at. */
    void showOutput() {
        this.rightTabs.setSelected(OUTPUT_TAB);
    }

    private boolean waiting() {
        final DebugStatePayload state = this.debug.state();
        return state != null && state.live() && state.waiting();
    }

    private List<DebugStatePayload.Variable> localRows() {
        final DebugStatePayload state = this.debug.state();
        return state == null ? List.of() : state.locals();
    }

    private List<DebugStatePayload.Variable> watchRows() {
        final DebugStatePayload state = this.debug.state();
        if (state != null && state.watches().size() == this.debug.watches().size()) {
            return state.watches();
        }
        return this.debug.watches().stream().map(name -> new DebugStatePayload.Variable(name, "", "")).toList();
    }

    private List<DebugStatePayload.Frame> frames() {
        final DebugStatePayload state = this.debug.state();
        return state == null ? List.of() : state.stack();
    }

    /* Opens the file a call is in, at its line and column. */
    private void open(final DebugStatePayload.Frame frame) {
        final String path = this.pathOf.apply(frame.file());
        if (path == null) {
            return;
        }
        this.workspace.open(path);
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null && StudioDebug.sameFile(doc.path(), frame.file())) {
            doc.area().document().setCursor(Math.max(0, frame.line() - 1), Math.max(0, frame.column() - 1));
        }
    }

    private void showFrame(final int index) {
        final List<DebugStatePayload.Frame> all = this.frames();
        if (index >= 0 && index < all.size()) {
            this.open(all.get(index));
        }
    }

    private void showBreakpoint(final int index) {
        final List<Map.Entry<String, Integer>> all = this.debug.allBreakpoints();
        if (index < 0 || index >= all.size()) {
            return;
        }
        this.workspace.open(all.get(index).getKey());
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            doc.area().document().setCursor(Math.max(0, all.get(index).getValue() - 1), 0);
        }
    }

    private int figure(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                       final String label, final String value, final int ink) {
        Draw.text(g, font, font.plainSubstrByWidth(label, width - font.width(value) - 6), x, y, ink);
        Draw.text(g, font, value, x + width - font.width(value), y, this.skin.get().text());
        return y + ROW_H;
    }

    private void drawVariable(final GuiGraphics g, final UiContext ctx, final DebugStatePayload.Variable variable,
                              final int index, final int x, final int y, final int width, final int height,
                              final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, width, height, hovered, selected);
        final Font font = ctx.font();
        final int valueX = x + width * 2 / 5;
        final int typeX = x + width * 3 / 4;
        final int ink = ctx.skin().listRowText(selected);
        Draw.text(g, font, font.plainSubstrByWidth(variable.name(), valueX - x - 6), x + 3, y + 1, ink);
        final String value = variable.type().isEmpty() && variable.value().isEmpty()
                ? GameText.resolve(DebugTexts.NOT_FOUND) : variable.value();
        Draw.text(g, font, font.plainSubstrByWidth(value, typeX - valueX - 4), valueX, y + 1, ink);
        Draw.text(g, font, font.plainSubstrByWidth(variable.type(), x + width - typeX - 2), typeX, y + 1,
                ctx.skin().dim());
    }

    private void drawFrame(final GuiGraphics g, final UiContext ctx, final DebugStatePayload.Frame frame,
                           final int index, final int x, final int y, final int width, final int height,
                           final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, width, height, hovered, selected);
        final Font font = ctx.font();
        Draw.text(g, font, font.plainSubstrByWidth(frame.method() + "()", width - 30), x + 3, y + 1,
                ctx.skin().listRowText(selected));
        Draw.text(g, font, String.valueOf(frame.line()), x + width - 24, y + 1, ctx.skin().dim());
    }

    private void drawBreakpoint(final GuiGraphics g, final UiContext ctx, final Map.Entry<String, Integer> at,
                                final int index, final int x, final int y, final int width, final int height,
                                final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, width, height, hovered, selected);
        final String file = at.getKey().substring(Math.max(at.getKey().lastIndexOf('/'),
                at.getKey().lastIndexOf('\\')) + 1);
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(file + "  " + at.getValue(), width - 6), x + 3,
                y + 1, ctx.skin().listRowText(selected));
    }

    private void drawOutput(final GuiGraphics g, final UiContext ctx, final String line, final int index,
                            final int x, final int y, final int width, final int height, final boolean hovered,
                            final boolean selected) {
        ctx.skin().listRow(g, x, y, width, height, hovered, selected);
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(line, width - 6), x + 3, y + 1, ctx.skin().text());
    }

    private static String kilobytes(final long bytes) {
        return UnitFormatter.forCurrentLocale().compact(bytes, Unit.BYTES);
    }

    private record Colours(int status, int statusText) {
    }
}
