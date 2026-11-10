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
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The debugger as Virtual Studio Code draws it: the Run and Debug view, with the variables, the names watched, the call
 * stack and the breakpoints; the bar of the steps floating over the editor; the Debug Console in the panel, with what
 * the program printed and where it stopped; and the status bar in the debugger's colour.
 */
@PaletteHolder
final class VsCodeDebug {

    private final StudioDebug debug;
    private final CodeWorkspace workspace;
    private final Supplier<OsSkin> skin;
    /** Where a file a listing names is on the disk, or null when the folder has no such file. */
    private final Function<String, String> pathOf;
    private final Runnable start;
    private final ListView<Row> view;
    private final List<Button> steps;
    private final ListView<String> console;
    private final TextField input = new TextField(DebugRequestPayload.MOST_LINE);
    /** The stop the editor was last moved to, so a new stop moves it once. */
    private String followed = "";

    private static final int ROW_H = 9;
    private static final int STEP_W = 11;
    /**
     * The colours of debugging, {@code jsc:editor/code_debug}: the status bar's ground and its text, and the Debug
     * Console's ground and text, dark whatever the theme, as the real one is.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "editor/code_debug",
            new Colours(0xFFCC6633, 0xFFFFFFFF, 0xFF181818, 0xFFCCCCCC));

    VsCodeDebug(final Panel root, final StudioDebug debug, final CodeWorkspace workspace,
                final Supplier<OsSkin> skin, final Function<String, String> pathOf, final Runnable start) {
        this.debug = debug;
        this.workspace = workspace;
        this.skin = skin;
        this.pathOf = pathOf;
        this.start = start;
        this.view = root.add(new ListView<>(this::rows, ROW_H, this::drawRow)).setOnClick(
                (index, button, mx, my) -> this.click(index));
        this.steps = List.of(root.add(new Button("▶", debug::resume)), root.add(new Button("↷",
                debug::stepOver)), root.add(new Button("↓", debug::stepInto)),
                root.add(new Button("↑", debug::stepOut)), root.add(new Button("↺", debug::restart)),
                root.add(new Button("■", debug::stop)));
        this.console = root.add(new ListView<>(debug::output, ROW_H, this::drawConsole));
        this.input.setPlaceholder(GameText.resolve(DebugTexts.WAITING)).setOnCommit(line -> {
            this.debug.input(line);
            this.input.set("");
        });
        root.add(this.input);
    }

    /** Whether a program is being debugged. */
    boolean shows() {
        return this.debug.active();
    }

    /** Lays the Run and Debug view out in the side bar, or hides it. */
    void layoutView(final boolean shown, final int x, final int y, final int width, final int height) {
        this.view.setVisible(shown);
        this.view.setBounds(x, y, width, height);
    }

    /** Lays the bar of the steps over the top right of the editor while debugging, or hides it. */
    void layoutSteps(final int right, final int y) {
        final boolean on = this.shows();
        int at = right - 3 - this.steps.size() * (STEP_W + 1);
        for (final Button step : this.steps) {
            step.setVisible(on);
            step.setBounds(at, y + 2, STEP_W, 9);
            at += STEP_W + 1;
        }
    }

    /** Hides the bar of the steps and the Debug Console, for a window showing no editor and no panel. */
    void hideEditorParts() {
        this.steps.forEach(step -> step.setVisible(false));
        this.console.setVisible(false);
        this.input.setVisible(false);
    }

    /** Lays the Debug Console out in the panel, or hides it; a program waiting for a line gets one to type in. */
    void layoutConsole(final boolean shown, final int x, final int y, final int width, final int height) {
        final DebugStatePayload state = this.debug.state();
        final boolean waiting = shown && state != null && state.live() && state.waiting();
        final int inputH = waiting ? 11 : 0;
        this.console.setVisible(shown);
        this.console.setBounds(x, y, width, Math.max(0, height - inputH));
        this.input.setVisible(waiting);
        this.input.setBounds(x + 1, y + height - inputH, width - 2, inputH);
    }

    /**
     * The status bar while debugging, in the debugger's colour: what is being debugged on the left; the credits it has
     * spent, where the caret is, the tab size and the language on the right.
     */
    void drawStatus(final GuiGraphics g, final Font font, final int x, final int y, final int width, final int height,
                    final List<String> right) {
        final int ink = PALETTE.get().statusText();
        g.fill(x, y, x + width, y + height, PALETTE.get().status());
        final String left = GameText.resolve(DebugTexts.STATUS_DEBUGGING.with(this.debug.name()));
        Draw.text(g, font, left, x + 3, y + 1, ink);
        int at = x + width - 3;
        final List<String> pieces = new ArrayList<>(right);
        pieces.add(GameText.resolve(DebugTexts.STATUS_CREDITS.with(this.debug.state().spent())));
        for (final String piece : pieces) {
            at -= font.width(piece);
            if (at < x + 10 + font.width(left)) {
                break;
            }
            Draw.text(g, font, piece, at, y + 1, ink);
            at -= 10;
        }
    }

    /** Marks the open file's breakpoints and stopped line, and opens the file of a new stop at its line. */
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

    /** Sets or takes away the breakpoint on the caret's line of the open file. */
    void toggleAtCaret() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            this.debug.toggle(doc.path(), doc.area().document().cursorLine() + 1);
        }
    }

    /* The rows of the Run and Debug view: its sections, each with a heading, in the order Visual Studio Code has. */
    private List<Row> rows() {
        final List<Row> rows = new ArrayList<>();
        final DebugStatePayload state = this.debug.state();
        if (!this.shows()) {
            rows.add(Row.heading(GameText.resolve(DebugTexts.RUN_AND_DEBUG)));
            rows.add(new Row(Kind.START, "▶ " + GameText.resolve(DebugTexts.START_RUN_AND_DEBUG), "",
                    this.start));
        } else {
            rows.add(Row.heading(GameText.resolve(DebugTexts.RUN_AND_DEBUG) + " ▶ " + this.debug.name()));
            rows.add(Row.heading("v " + GameText.resolve(DebugTexts.VARIABLES)));
            rows.add(new Row(Kind.SUBHEADING, GameText.resolve(DebugTexts.LOCALS_SCOPE), "", null));
            for (final DebugStatePayload.Variable variable : state.locals()) {
                rows.add(new Row(Kind.VALUE, variable.name() + ":", variable.value(), null));
            }
            rows.add(Row.heading("v " + GameText.resolve(DebugTexts.WATCH)));
            final List<String> watched = this.debug.watches();
            for (int i = 0; i < watched.size(); i++) {
                final int index = i;
                final String value = i < state.watches().size() && !state.watches().get(i).type().isEmpty()
                        ? state.watches().get(i).value() : GameText.resolve(DebugTexts.NOT_FOUND);
                rows.add(new Row(Kind.VALUE, watched.get(i) + ":", value, () -> this.debug.removeWatch(index)));
            }
            rows.add(Row.heading("v " + GameText.resolve(DebugTexts.CALL_STACK)));
            for (final DebugStatePayload.Frame frame : state.stack()) {
                rows.add(new Row(Kind.ITEM, frame.method(), leaf(frame.file()) + " " + frame.line() + ":"
                        + frame.column(), () -> this.open(frame)));
            }
        }
        rows.add(Row.heading("v " + GameText.resolve(DebugTexts.BREAKPOINTS)));
        for (final Map.Entry<String, Integer> at : this.debug.allBreakpoints()) {
            rows.add(new Row(Kind.BREAKPOINT, leaf(at.getKey()), String.valueOf(at.getValue()),
                    () -> this.showBreakpoint(at)));
        }
        return rows;
    }

    private void click(final int index) {
        final List<Row> rows = this.rows();
        if (index >= 0 && index < rows.size() && rows.get(index).action() != null) {
            rows.get(index).action().run();
        }
    }

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

    private void showBreakpoint(final Map.Entry<String, Integer> at) {
        this.workspace.open(at.getKey());
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            doc.area().document().setCursor(Math.max(0, at.getValue() - 1), 0);
        }
    }

    private void drawRow(final GuiGraphics g, final UiContext ctx, final Row row, final int index, final int x,
                         final int y, final int width, final int height, final boolean hovered,
                         final boolean selected) {
        final Font font = ctx.font();
        final OsSkin look = this.skin.get();
        switch (row.kind()) {
            case HEADING -> {
                look.panel(g, x, y, width, height);
                Draw.text(g, font, font.plainSubstrByWidth(row.label(), width - 6), x + 3, y + 1, look.dim());
            }
            case START -> {
                ctx.skin().listRow(g, x, y, width, height, hovered, selected);
                Draw.text(g, font, font.plainSubstrByWidth(row.label(), width - 6), x + 6, y + 1, look.accent());
            }
            case BREAKPOINT -> {
                ctx.skin().listRow(g, x, y, width, height, hovered, selected);
                g.fill(x + 6, y + 3, x + 9, y + 6, PALETTE.get().status());
                Draw.text(g, font, font.plainSubstrByWidth(row.label(), width - 34), x + 12, y + 1, look.text());
                Draw.text(g, font, row.detail(), x + width - 4 - font.width(row.detail()), y + 1, look.dim());
            }
            default -> {
                ctx.skin().listRow(g, x, y, width, height, hovered, selected);
                final int indent = row.kind() == Kind.SUBHEADING ? 6 : 10;
                final String label = font.plainSubstrByWidth(row.label(), width / 2);
                Draw.text(g, font, label, x + indent, y + 1, row.kind() == Kind.VALUE ? look.accent() : look.text());
                final int detailX = x + indent + font.width(label) + 4;
                Draw.text(g, font, font.plainSubstrByWidth(row.detail(), Math.max(0, x + width - detailX - 2)),
                        detailX, y + 1, look.dim());
            }
        }
    }

    private void drawConsole(final GuiGraphics g, final UiContext ctx, final String line, final int index,
                             final int x, final int y, final int width, final int height, final boolean hovered,
                             final boolean selected) {
        g.fill(x, y, x + width, y + height, PALETTE.get().consoleGround());
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(line, width - 6), x + 3, y + 1,
                PALETTE.get().consoleText());
    }

    private static String leaf(final String file) {
        return file.substring(Math.max(file.lastIndexOf('/'), file.lastIndexOf('\\')) + 1);
    }

    /** What a row of the Run and Debug view is. */
    private enum Kind {
        HEADING, SUBHEADING, START, VALUE, ITEM, BREAKPOINT
    }

    /** One row of the view: what it is, its words, what stands on its right, and what a click on it does. */
    private record Row(Kind kind, String label, String detail, Runnable action) {

        static Row heading(final String label) {
            return new Row(Kind.HEADING, label, "", null);
        }
    }

    private record Colours(int status, int statusText, int consoleGround, int consoleText) {
    }
}
