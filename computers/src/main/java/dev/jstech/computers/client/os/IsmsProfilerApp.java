/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.IsmsLayout;
import dev.jstech.computers.operation.payload.IsmsActionPayload;
import dev.jstech.computers.operation.payload.IsmsSchemaPayload;
import dev.jstech.computers.operation.payload.IsmsTracePayload;
import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.RequestIsmsSchemaPayload;
import dev.jstech.computers.operation.payload.SaveFilePayload;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.trace.TraceEvent;
import dev.jstech.computers.trace.TraceEventClass;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.MenuBar;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The IQL Server Profiler, opened from the studio's Tools in a window of its own: it records what the network's work
 * does as it happens, a row for each event. The statements that come through the network's door, the Operations taken
 * on and settled, the locks taken, the plans chosen and what the buses moved, each with who asked for it, on which
 * computer, how many items and how long it took. A row's detail shows below the grid. A trace is started, paused and
 * stopped, and saved as a file on the computer that opened it.
 */
public final class IsmsProfilerApp implements IDesktopApp, CodeFileReplies.IReader {

    private final BlockPos host;
    private final BlockPos monitor;
    private final int window;
    private final IsmsSkin skin = new IsmsSkin();
    private final MenuBar menuBar = new MenuBar(92, MenuBar.HEIGHT);
    private final ContextMenu context = new ContextMenu(110, 10);
    private final FileDialog files;
    private final List<Row> rows = new ArrayList<>();
    private final TextField finding = new TextField(40);
    private final Panel findPanel = new Panel();
    private final List<ToolButton> toolbar = new ArrayList<>();
    private State state = State.STOPPED;
    private int mask = TraceEventClass.Group.ALL;
    private int selected = -1;
    private int scroll;
    private boolean findOpen;
    private int traceNumber;
    private String network = "";
    private String path = "";
    private Text status = Text.EMPTY;
    private boolean askedSchema;
    private Font font = Minecraft.getInstance().font;
    private int originX;
    private int originY;
    private int width = IsmsLayout.PROFILER_W;
    private int height = IsmsLayout.PROFILER_H;
    private int mouseX;
    private int mouseY;
    /** Whether a save of this window's is waiting for its answer. */
    private boolean saving;
    @Nullable
    private String opening;

    /** The window's key, under which the desktop keeps it with the machine. */
    public static final String KEY = Programs.ISMS + "/profiler";
    private static final List<TraceEventClass.Group> GROUPS = List.of(TraceEventClass.Group.STATEMENTS,
            TraceEventClass.Group.OPERATIONS, TraceEventClass.Group.LOCKS, TraceEventClass.Group.PLANS,
            TraceEventClass.Group.BUSES);
    private static final List<TraceEventClass> CLASSES = List.of(TraceEventClass.STATEMENT_STARTING,
            TraceEventClass.STATEMENT_COMPLETED, TraceEventClass.OPERATION_CREATED, TraceEventClass.OPERATION_SETTLED,
            TraceEventClass.LOCK_ACQUIRED, TraceEventClass.PLAN_CHOSEN, TraceEventClass.BUS_MOVED);
    /** The share of the grid each column takes, in hundredths, left to right. */
    private static final int[] SHARES = {16, 32, 15, 11, 8, 8, 10};
    private static final int MOST_ROWS = 2000;
    private static final double TICKS_PER_SECOND = 20.0;
    private static final String TAB = "\t";
    private static final String DETAIL_SEPARATOR = " | ";
    private static final String EXTENSION = "log";

    /** Every Profiler window open on this client, which the network's events are routed among. */
    private static final List<IsmsProfilerApp> OPEN = new ArrayList<>();

    public IsmsProfilerApp(final BlockPos host, final BlockPos monitor) {
        this.host = host;
        this.monitor = monitor;
        this.window = IsmsApp.nextWindowNumber();
        this.files = new FileDialog(host, this);
        this.traceNumber = 1;
        findPanel.add(finding);
        finding.setOnCommit(text -> findNext());
        menuBar.add(GameText.resolve(IsmsTexts.FILE), this::fileMenu)
                .add(GameText.resolve(IsmsTexts.EDIT), this::editMenu)
                .add(GameText.resolve(IsmsTexts.VIEW), this::viewMenu)
                .add(GameText.resolve(IsmsProfilerTexts.REPLAY), this::replayMenu)
                .add(GameText.resolve(IsmsTexts.TOOLS), this::toolsMenu)
                .add(GameText.resolve(IsmsTexts.WINDOW), this::windowMenu)
                .add(GameText.resolve(IsmsTexts.HELP), this::helpMenu);
        OPEN.add(this);
    }

    /** What a trace a Profiler window began has seen, for that window. */
    public static void accept(final IsmsTracePayload payload) {
        for (final IsmsProfilerApp app : OPEN) {
            if (app.window == payload.window() && app.state == State.RUNNING) {
                payload.events().forEach(app::record);
            }
        }
    }

    /** The network's name, for the window that asked. */
    public static void acceptSchema(final IsmsSchemaPayload payload) {
        for (final IsmsProfilerApp app : OPEN) {
            if (app.window == payload.window()) {
                app.network = GameText.resolve(payload.network());
            }
        }
    }

    /** The Profiler window opened last, for a test to drive. */
    @Nullable
    public static IsmsProfilerApp latest() {
        return OPEN.isEmpty() ? null : OPEN.get(OPEN.size() - 1);
    }

    @Override
    public String title() {
        return GameText.resolve(IsmsProfilerTexts.TITLE.with(IsmsTexts.PROFILER.text(), traceName(), network));
    }

    @Override
    public int defaultWidth() {
        return IsmsLayout.PROFILER_W;
    }

    @Override
    public int defaultHeight() {
        return IsmsLayout.PROFILER_H;
    }

    @Override
    public int minWidth() {
        return IsmsLayout.PROFILER_MIN_W;
    }

    @Override
    public int minHeight() {
        return IsmsLayout.PROFILER_MIN_H;
    }

    @Override
    public void onClosed() {
        if (state != State.STOPPED) {
            send(new IsmsActionPayload(monitor, host, window, IsmsActionPayload.TRACE_STOP, "", 0));
        }
        OPEN.remove(this);
        CodeFileReplies.forget(this);
    }

    @Override
    public void onRestored() {
        if (!OPEN.contains(this)) {
            OPEN.add(this);
        }
        if (state == State.RUNNING) {
            start();
        }
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.font = font;
        this.originX = x;
        this.originY = y;
        this.width = width;
        this.height = height;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        if (!askedSchema) {
            askedSchema = true;
            send(new RequestIsmsSchemaPayload(host, window));
        }
        skin.wear(IsmsSkin.lookOf(host));
        final IsmsLook.Colours c = skin.look().colours();
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, c.window());
        g.fillGradient(x, y, x + width, y + MenuBar.HEIGHT, c.menu(), c.menuTo());
        drawToolbar(g, c);
        final int statusY = y + IsmsLayout.statusY(height);
        final int detailY = statusY - IsmsLayout.DETAIL_H;
        final int findH = findOpen ? IsmsLayout.FIELD_H + 4 : 0;
        drawGrid(g, c, x, y + IsmsLayout.BODY_Y, width, detailY - IsmsLayout.HSPLIT_H - findH - y
                - IsmsLayout.BODY_Y);
        if (findOpen) {
            final int fy = detailY - IsmsLayout.HSPLIT_H - findH;
            g.fill(x, fy, x + width, fy + findH, c.window());
            finding.setBounds(x + 4, fy + 2, Math.min(160, width - 8), IsmsLayout.FIELD_H);
            findPanel.setBounds(x + 4, fy + 2, Math.min(160, width - 8), IsmsLayout.FIELD_H);
            findPanel.focus(finding);
            findPanel.render(g, ctx);
        }
        g.fill(x, detailY - IsmsLayout.HSPLIT_H, x + width, detailY, c.split());
        g.fill(x, detailY - IsmsLayout.HSPLIT_H, x + width, detailY - IsmsLayout.HSPLIT_H + 1, c.splitEdge());
        drawDetail(g, c, x, detailY, width, IsmsLayout.DETAIL_H);
        drawStatus(g, c, x, statusY, width);
        menuBar.setBounds(x, y, width, MenuBar.HEIGHT);
        menuBar.setWindow(x, y, width, height);
        menuBar.render(g, ctx);
        context.render(g, ctx);
    }

    @Override
    public boolean wantsEscape() {
        return context.isOpen() || menuBar.isOpen() || findOpen;
    }

    @Override
    public void mouseClicked(final DesktopWindow win, final double mx, final double my, final int button) {
        if (context.isOpen()) {
            context.mouseClicked(mx, my, button);
            return;
        }
        if (menuBar.mouseClicked(mx, my, button)) {
            return;
        }
        final int lx = (int) mx - originX;
        final int ly = (int) my - originY;
        if (ly >= IsmsLayout.TOOLBAR_Y && ly < IsmsLayout.BODY_Y) {
            for (final ToolButton tool : toolbar) {
                if (tool.enabled() && lx >= tool.x() && lx < tool.x() + tool.w()) {
                    tool.action().run();
                    return;
                }
            }
            return;
        }
        if (findOpen && findPanel.mouseClicked(mx, my, button)) {
            return;
        }
        final int gridTop = IsmsLayout.BODY_Y + IsmsLayout.ROW_H;
        final int index = scroll + (ly - gridTop) / IsmsLayout.ROW_H;
        if (ly >= gridTop && ly < gridBottom() && index >= 0 && index < rows.size()) {
            selected = index;
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        scroll = Math.max(0, Math.min(Math.max(0, rows.size() - gridFit()), scroll + (delta > 0 ? -2 : 2)));
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (context.isOpen()) {
            return context.keyPressed(key, scanCode, modifiers);
        }
        if (menuBar.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        final boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && key == GLFW.GLFW_KEY_N) {
            newTrace();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_O) {
            chooseOpen();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_S) {
            save();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_F) {
            findOpen = true;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && findOpen) {
            findOpen = false;
            return true;
        }
        if (findOpen) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                findNext();
                return true;
            }
            return findPanel.keyPressed(key, scanCode, modifiers);
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            select(selected + (key == GLFW.GLFW_KEY_UP ? -1 : 1));
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(final char c) {
        return findOpen && findPanel.charTyped(c);
    }

    @Override
    public void onContent(final String file, final String content, final boolean exists) {
        if (opening == null || !opening.equals(file)) {
            return;
        }
        opening = null;
        if (!exists) {
            status = IsmsTexts.COULD_NOT_OPEN.with(file);
            return;
        }
        stop();
        rows.clear();
        for (final String line : content.split("\n")) {
            final Row row = parse(line);
            if (row != null) {
                rows.add(row);
            }
        }
        path = file;
        selected = rows.isEmpty() ? -1 : 0;
        status = IsmsTexts.OPENED.with(file);
    }

    @Override
    public void onSaved(final boolean ok, final Text message) {
        if (saving) {
            saving = false;
            status = ok ? IsmsProfilerTexts.SAVED.with(path) : message;
        }
    }

    /** Begins, or goes on with, the trace: the network tells this window what the picked groups do. */
    public void start() {
        state = State.RUNNING;
        send(new IsmsActionPayload(monitor, host, window, IsmsActionPayload.TRACE_START, "", mask));
    }

    /** Stops listening for now, keeping what was recorded. */
    public void pause() {
        if (state == State.RUNNING) {
            state = State.PAUSED;
            send(new IsmsActionPayload(monitor, host, window, IsmsActionPayload.TRACE_STOP, "", 0));
        }
    }

    /** Ends the trace. */
    public void stop() {
        if (state != State.STOPPED) {
            send(new IsmsActionPayload(monitor, host, window, IsmsActionPayload.TRACE_STOP, "", 0));
        }
        state = State.STOPPED;
    }

    /** The rows recorded, for a test to read: each event's class and what it says. */
    public List<String> rowTexts() {
        final List<String> out = new ArrayList<>();
        for (final Row row : rows) {
            out.add(row.event().kind().eventName() + " " + GameText.resolve(row.event().text()));
        }
        return out;
    }

    /** Whether the trace is running. */
    public boolean running() {
        return state == State.RUNNING;
    }

    /** What the detail pane says of the row picked, line by line. */
    public List<String> detailLines() {
        return selected < 0 || selected >= rows.size() ? List.of() : detail(rows.get(selected));
    }

    /** Picks row {@code index}, as a click on it does. */
    public void select(final int index) {
        if (!rows.isEmpty()) {
            selected = Math.max(0, Math.min(rows.size() - 1, index));
            if (selected < scroll) {
                scroll = selected;
            } else if (selected >= scroll + gridFit()) {
                scroll = selected - gridFit() + 1;
            }
        }
    }

    /** Picks which groups of events the trace records, as the Events picker does. */
    public void toggle(final TraceEventClass.Group group) {
        mask ^= group.bit();
        if (state == State.RUNNING) {
            start();
        }
    }

    /** The middle of the toolbar button whose label is {@code word}, or null. */
    @Nullable
    public int[] toolbarCenter(final String word) {
        for (final ToolButton tool : toolbar) {
            if (tool.label().equals(word)) {
                return new int[] {tool.x() + tool.w() / 2, IsmsLayout.TOOLBAR_Y + IsmsLayout.TOOLBAR_H / 2};
            }
        }
        return null;
    }

    /** Writes the trace to the file it came from, or asks where. */
    public void save() {
        if (path.isEmpty()) {
            chooseSaveAs();
            return;
        }
        write(path);
    }

    private void record(final TraceEvent event) {
        rows.add(new Row(event, worldClock()));
        while (rows.size() > MOST_ROWS) {
            rows.remove(0);
            selected--;
        }
        // The grid follows the newest row while the player is looking at the bottom of it.
        if (scroll + gridFit() >= rows.size() - 1) {
            scroll = Math.max(0, rows.size() - gridFit());
        }
    }

    private void newTrace() {
        stop();
        rows.clear();
        selected = -1;
        scroll = 0;
        path = "";
        traceNumber++;
        start();
    }

    private void clear() {
        rows.clear();
        selected = -1;
        scroll = 0;
    }

    private void chooseOpen() {
        files.openFile(IsmsTexts.OPEN_FILE.text(), "", List.of(FileDialog.Filter.of(IsmsTexts.TRACES, EXTENSION),
                FileDialog.Filter.ALL), file -> {
                    opening = file;
                    CodeFileReplies.expectContent(this, file);
                    send(new RequestFileContentPayload(host, file));
                });
    }

    private void chooseSaveAs() {
        files.saveAs(IsmsTexts.SAVE_AS.text(), "", traceName().replace(' ', '_') + "." + EXTENSION,
                List.of(FileDialog.Filter.of(IsmsTexts.TRACES, EXTENSION)), file -> {
                    path = file.toLowerCase(Locale.ROOT).endsWith("." + EXTENSION) ? file : file + "." + EXTENSION;
                    write(path);
                });
    }

    private void write(final String file) {
        final StringBuilder out = new StringBuilder();
        out.append(String.join(TAB, columnWords())).append('\n');
        for (final Row row : rows) {
            final List<String> cells = new ArrayList<>(cells(row));
            final List<String> detail = new ArrayList<>();
            row.event().detail().forEach(line -> detail.add(GameText.resolve(line)));
            cells.add(String.join(DETAIL_SEPARATOR, detail));
            out.append(String.join(TAB, cells).replace("\n", " ")).append('\n');
            if (out.length() > SaveFilePayload.MAX_CONTENT) {
                break;
            }
        }
        final String text = out.length() > SaveFilePayload.MAX_CONTENT ? out.substring(0, SaveFilePayload.MAX_CONTENT)
                : out.toString();
        saving = true;
        CodeFileReplies.expectSaved(this);
        send(new SaveFilePayload(host, file, text));
    }

    /* A line of a saved trace read back into a row, or null for the header or a line that is not one. */
    @Nullable
    private static Row parse(final String line) {
        final String[] cells = line.split(TAB, -1);
        if (cells.length < 7) {
            return null;
        }
        TraceEventClass kind = null;
        for (final TraceEventClass each : CLASSES) {
            if (each.eventName().equals(cells[0])) {
                kind = each;
            }
        }
        if (kind == null) {
            return null;
        }
        final List<Text> detail = new ArrayList<>();
        if (cells.length > 7 && !cells[7].isEmpty()) {
            for (final String part : cells[7].split(" \\| ")) {
                detail.add(Text.literal(part));
            }
        }
        return new Row(new TraceEvent(kind, Text.literal(cells[1]), cells[2], cells[3], number(cells[4]),
                number(cells[5]), 0L, detail), cells[6]);
    }

    private static long number(final String cell) {
        final String digits = cell.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? TraceEvent.NONE : Long.parseLong(digits);
    }

    private void findNext() {
        final String needle = finding.edit().toLowerCase(Locale.ROOT);
        if (needle.isEmpty()) {
            return;
        }
        for (int step = 1; step <= rows.size(); step++) {
            final int i = Math.floorMod(selected + step, rows.size());
            if (String.join(" ", cells(rows.get(i))).toLowerCase(Locale.ROOT).contains(needle)) {
                select(i);
                return;
            }
        }
        status = IsmsProfilerTexts.NOT_FOUND.with(finding.edit());
    }

    private void replaySelected() {
        if (selected < 0 || selected >= rows.size()) {
            return;
        }
        final TraceEvent event = rows.get(selected).event();
        if (event.kind() != TraceEventClass.STATEMENT_STARTING && event.kind() != TraceEventClass.STATEMENT_COMPLETED) {
            return;
        }
        ActiveDesktop.openOrFocus(IsmsApp.KEY);
        final IsmsApp studio = IsmsApp.latest();
        if (studio != null) {
            studio.runNew("", GameText.resolve(event.text()));
        }
    }

    private void send(final CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    private String traceName() {
        return path.isEmpty() ? GameText.resolve(IsmsProfilerTexts.UNTITLED.with(traceNumber))
                : path.substring(Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\')) + 1);
    }

    private int gridFit() {
        return Math.max(1, (gridBottom() - IsmsLayout.BODY_Y - IsmsLayout.ROW_H) / IsmsLayout.ROW_H);
    }

    private int gridBottom() {
        return IsmsLayout.statusY(height) - IsmsLayout.DETAIL_H - IsmsLayout.HSPLIT_H
                - (findOpen ? IsmsLayout.FIELD_H + 4 : 0);
    }

    private List<String> columnWords() {
        final List<String> out = new ArrayList<>();
        for (final TextKey key : List.of(IsmsProfilerTexts.EVENT_CLASS, IsmsProfilerTexts.TEXT_DATA,
                IsmsProfilerTexts.REQUESTER, IsmsProfilerTexts.COMPUTER, IsmsProfilerTexts.ITEMS,
                IsmsProfilerTexts.DURATION, IsmsProfilerTexts.START_TIME)) {
            out.add(GameText.resolve(key));
        }
        return out;
    }

    /* The cells of a row as the grid shows them, left to right. */
    private static List<String> cells(final Row row) {
        final TraceEvent e = row.event();
        final String items = e.items() < 0 ? "" : e.kind() == TraceEventClass.STATEMENT_COMPLETED
                ? GameText.resolve(IsmsTexts.ROWS.with(e.items())) : Long.toString(e.items());
        final String duration = e.duration() < 0 ? "" : GameText.resolve(IsmsProfilerTexts.TICKS.with(e.duration()));
        return List.of(e.kind().eventName(), GameText.resolve(e.text()), e.requester(), e.computer(), items, duration,
                row.time());
    }

    private static List<String> detail(final Row row) {
        final TraceEvent e = row.event();
        final List<String> out = new ArrayList<>();
        out.add(GameText.resolve(e.text()));
        if (!e.requester().isEmpty()) {
            out.add(GameText.resolve(IsmsProfilerTexts.ASKED_BY.with(e.requester(), e.computer())));
        }
        e.detail().forEach(line -> out.add(GameText.resolve(line)));
        if (e.duration() > 0) {
            out.add(GameText.resolve(IsmsProfilerTexts.TOOK.with(e.duration(), String.format(Locale.ROOT, "%.1f",
                    e.duration() / TICKS_PER_SECOND))));
        }
        return out;
    }

    private static String worldClock() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "";
        }
        final long t = (mc.level.getDayTime() + 6000L) % 24000L;
        final long seconds = t % 1000L * 3600L / 1000L;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", t / 1000L, seconds / 60L, seconds % 60L);
    }

    // Drawing.

    private void drawToolbar(final GuiGraphics g, final IsmsLook.Colours c) {
        final int y = originY + IsmsLayout.TOOLBAR_Y;
        g.fillGradient(originX, y, originX + width, y + IsmsLayout.TOOLBAR_H, c.toolbar(), c.toolbarTo());
        g.fill(originX, y + IsmsLayout.TOOLBAR_H - 1, originX + width, y + IsmsLayout.TOOLBAR_H, c.edge());
        toolbar.clear();
        int x = IsmsLayout.BUTTON_GAP + 1;
        for (final ToolButton tool : List.of(
                tool(IsmsProfilerTexts.NEW_TRACE, true, this::newTrace),
                tool(IsmsTexts.OPEN, true, this::chooseOpen),
                tool(IsmsTexts.SAVE, !rows.isEmpty(), this::save),
                tool(IsmsTexts.START, state != State.RUNNING, this::start),
                tool(IsmsProfilerTexts.PAUSE, state == State.RUNNING, this::pause),
                tool(IsmsTexts.STOP, state != State.STOPPED, this::stop),
                tool(IsmsProfilerTexts.CLEAR, !rows.isEmpty(), this::clear),
                tool(IsmsProfilerTexts.FIND, true, () -> findOpen = !findOpen),
                new ToolButton(GameText.resolve(IsmsProfilerTexts.EVENTS.with(groupWords())), 0,
                        font.width(GameText.resolve(IsmsProfilerTexts.EVENTS.with(groupWords())))
                                + IsmsLayout.BUTTON_PAD, true, this::openEvents))) {
            if (x + tool.w() > width - 2) {
                break;
            }
            final ToolButton placed = tool.at(x);
            toolbar.add(placed);
            final int bx = originX + x;
            final int by = y + (IsmsLayout.TOOLBAR_H - IsmsLayout.BUTTON_H) / 2;
            final boolean hovered = tool.enabled() && mouseX >= bx && mouseX < bx + tool.w() && mouseY >= by
                    && mouseY < by + IsmsLayout.BUTTON_H;
            g.fill(bx, by, bx + tool.w(), by + IsmsLayout.BUTTON_H, hovered ? c.select() : c.button());
            if (skin.look().bevelled()) {
                skin.raised(g, bx, by, tool.w(), IsmsLayout.BUTTON_H);
            } else {
                Draw.outline(g, bx, by, tool.w(), IsmsLayout.BUTTON_H, hovered ? c.selectEdge() : c.buttonEdge());
            }
            Draw.text(g, font, tool.label(), bx + IsmsLayout.BUTTON_PAD / 2, by + 2, tool.enabled() ? c.text()
                    : c.dim(), c.button());
            x += tool.w() + IsmsLayout.BUTTON_GAP;
        }
    }

    private ToolButton tool(final TextKey label, final boolean enabled, final Runnable action) {
        final String word = GameText.resolve(label);
        return new ToolButton(word, 0, font.width(word) + IsmsLayout.BUTTON_PAD, enabled, action);
    }

    private void drawGrid(final GuiGraphics g, final IsmsLook.Colours c, final int x, final int y, final int w,
                          final int h) {
        g.fill(x, y, x + w, y + h, c.panel());
        final int[] xs = new int[SHARES.length];
        int at = x;
        for (int i = 0; i < SHARES.length; i++) {
            xs[i] = at;
            at += w * SHARES[i] / 100;
        }
        g.fill(x, y, x + w, y + IsmsLayout.ROW_H, c.head());
        final List<String> words = columnWords();
        for (int i = 0; i < xs.length; i++) {
            final int right = i + 1 < xs.length ? xs[i + 1] : x + w;
            g.fill(right - 1, y, right, y + IsmsLayout.ROW_H, c.edge());
            Draw.text(g, font, Texts.clip(font, words.get(i), right - xs[i] - 6), xs[i] + 3, y + 1, c.headText(),
                    c.head());
        }
        if (rows.isEmpty()) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(IsmsProfilerTexts.NO_EVENTS), w - 8), x + 4,
                    y + IsmsLayout.ROW_H + 4, c.dim(), c.panel());
            return;
        }
        final int fit = Math.max(1, (h - IsmsLayout.ROW_H) / IsmsLayout.ROW_H);
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rows.size() - fit)));
        for (int r = scroll; r < rows.size() && r - scroll < fit; r++) {
            final int ry = y + IsmsLayout.ROW_H * (r - scroll + 1);
            final boolean lit = r == selected;
            final int ground = lit ? c.select() : c.panel();
            if (lit) {
                g.fill(x, ry, x + w, ry + IsmsLayout.ROW_H, ground);
            }
            final Row row = rows.get(r);
            final List<String> cells = cells(row);
            final int classColour = switch (row.event().kind()) {
                case OPERATION_SETTLED -> c.good();
                case LOCK_ACQUIRED -> c.warn();
                default -> c.accent();
            };
            for (int i = 0; i < cells.size(); i++) {
                final int right = i + 1 < xs.length ? xs[i + 1] : x + w;
                final int colour = i == 0 ? classColour : i == 1 ? c.accent() : i == 6 ? c.dim() : c.text();
                Draw.text(g, font, Texts.clip(font, cells.get(i), right - xs[i] - 6), xs[i] + 3, ry + 1, colour,
                        ground);
            }
        }
    }

    private void drawDetail(final GuiGraphics g, final IsmsLook.Colours c, final int x, final int y, final int w,
                            final int h) {
        g.fill(x, y, x + w, y + h, c.panel());
        int ry = y + 2;
        for (final String line : detailLines()) {
            if (ry + 9 > y + h) {
                break;
            }
            Draw.text(g, font, Texts.clip(font, line, w - 8), x + 4, ry, c.text(), c.panel());
            ry += 9;
        }
    }

    private void drawStatus(final GuiGraphics g, final IsmsLook.Colours c, final int x, final int y, final int w) {
        skin.statusBar(g, x, y, w, IsmsLayout.STATUS_H);
        g.fill(x + 4, y + 3, x + 9, y + 8, state == State.RUNNING ? c.led() : c.dim());
        final Text said = !status.isEmpty() ? status : switch (state) {
            case RUNNING -> IsmsProfilerTexts.RUNNING.text();
            case PAUSED -> IsmsProfilerTexts.PAUSED.text();
            case STOPPED -> IsmsProfilerTexts.STOPPED.text();
        };
        final String right = GameText.resolve(IsmsProfilerTexts.LINE.with(Math.max(1, selected + 1), 1)) + "   "
                + GameText.resolve(IsmsProfilerTexts.ROW_COUNT.with(rows.size()));
        Draw.text(g, font, right, x + w - 4 - font.width(right), y + 2, c.statusText(), c.status());
        Draw.text(g, font, Texts.clip(font, GameText.resolve(said), w - font.width(right) - 24), x + 12, y + 2,
                c.statusText(), c.status());
    }

    // Menus.

    private void openEvents() {
        final ToolButton picker = toolbar.get(toolbar.size() - 1);
        context.open(eventItems(), originX + picker.x(), originY + IsmsLayout.BODY_Y, originX, originY, width,
                height);
    }

    private List<ContextMenu.Item> eventItems() {
        final List<ContextMenu.Item> out = new ArrayList<>();
        for (final TraceEventClass.Group group : GROUPS) {
            final TextKey state = group.in(mask) ? IsmsProfilerTexts.GROUP_ON : IsmsProfilerTexts.GROUP_OFF;
            out.add(new ContextMenu.Item(GameText.resolve(state.with(groupWord(group))), true, () -> toggle(group)));
        }
        return out;
    }

    private String groupWords() {
        final List<String> on = new ArrayList<>();
        for (final TraceEventClass.Group group : GROUPS) {
            if (group.in(mask)) {
                on.add(GameText.resolve(groupWord(group)));
            }
        }
        return on.isEmpty() ? GameText.resolve(IsmsProfilerTexts.NO_GROUPS) : String.join(", ", on);
    }

    private static Text groupWord(final TraceEventClass.Group group) {
        return switch (group) {
            case STATEMENTS -> IsmsProfilerTexts.STATEMENTS.text();
            case OPERATIONS -> IsmsProfilerTexts.OPERATIONS.text();
            case LOCKS -> IsmsProfilerTexts.LOCKS.text();
            case PLANS -> IsmsProfilerTexts.PLANS.text();
            case BUSES -> IsmsProfilerTexts.BUSES.text();
        };
    }

    private List<ContextMenu.Item> fileMenu() {
        return List.of(
                keyed(IsmsProfilerTexts.NEW_TRACE, IsmsMenus.NEW_KEYS, true, this::newTrace),
                keyed(IsmsTexts.OPEN_FILE, IsmsMenus.OPEN_KEYS, true, this::chooseOpen),
                ContextMenu.Item.separator(),
                keyed(IsmsTexts.SAVE, IsmsMenus.SAVE_KEYS, !rows.isEmpty(), this::save),
                item(IsmsTexts.SAVE_AS, !rows.isEmpty(), this::chooseSaveAs),
                ContextMenu.Item.separator(),
                item(IsmsTexts.EXIT, true, () -> ActiveDesktop.closeWindowFor(this)));
    }

    private List<ContextMenu.Item> editMenu() {
        return List.of(
                keyed(IsmsTexts.COPY, IsmsMenus.COPY_KEYS, selected >= 0, () -> Minecraft.getInstance()
                        .keyboardHandler.setClipboard(String.join(TAB, cells(rows.get(selected))))),
                item(IsmsProfilerTexts.CLEAR_WINDOW, !rows.isEmpty(), this::clear),
                keyed(IsmsTexts.FIND, IsmsMenus.FIND_KEYS, true, () -> findOpen = true),
                item(IsmsProfilerTexts.FIND_NEXT, !finding.edit().isEmpty(), this::findNext));
    }

    private List<ContextMenu.Item> viewMenu() {
        return List.of(ContextMenu.Item.submenu(GameText.resolve(IsmsProfilerTexts.EVENTS_SELECTION), eventItems()));
    }

    private List<ContextMenu.Item> replayMenu() {
        final boolean statement = selected >= 0 && selected < rows.size()
                && (rows.get(selected).event().kind() == TraceEventClass.STATEMENT_STARTING
                || rows.get(selected).event().kind() == TraceEventClass.STATEMENT_COMPLETED);
        return List.of(item(IsmsProfilerTexts.REPLAY_STATEMENT, statement, this::replaySelected));
    }

    private List<ContextMenu.Item> toolsMenu() {
        return List.of(item(IsmsTexts.STUDIO, true, () -> ActiveDesktop.openOrFocus(IsmsApp.KEY)));
    }

    private List<ContextMenu.Item> windowMenu() {
        return List.of(item(IsmsTexts.CLOSE, true, () -> ActiveDesktop.closeWindowFor(this)));
    }

    private List<ContextMenu.Item> helpMenu() {
        return List.of(item(IsmsTexts.IQL_REFERENCE, true, () -> {
            ActiveDesktop.openOrFocus(IsmsApp.KEY);
            final IsmsApp studio = IsmsApp.latest();
            if (studio != null) {
                studio.openPage(IsmsDocument.Kind.REFERENCE, IsmsTexts.IQL_REFERENCE);
            }
        }));
    }

    private static ContextMenu.Item item(final TextKey label, final boolean enabled, final Runnable action) {
        return new ContextMenu.Item(GameText.resolve(label), enabled, action);
    }

    private static ContextMenu.Item keyed(final TextKey label, final String keys, final boolean enabled,
                                          final Runnable action) {
        return ContextMenu.Item.keyed(GameText.resolve(label), keys, enabled, action);
    }

    /** Where a trace stands. */
    private enum State { RUNNING, PAUSED, STOPPED }

    /** A recorded event, with the time on the world's clock it was seen at. */
    private record Row(TraceEvent event, String time) {
    }

    /** A toolbar button as laid out this frame. */
    private record ToolButton(String label, int x, int w, boolean enabled, Runnable action) {

        ToolButton at(final int value) {
            return new ToolButton(label, value, w, enabled, action);
        }
    }
}
