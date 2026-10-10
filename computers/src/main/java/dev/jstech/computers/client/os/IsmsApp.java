/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.IsmsLayout;
import dev.jstech.computers.operation.payload.IqlResultPayload;
import dev.jstech.computers.operation.payload.IsmsActionPayload;
import dev.jstech.computers.operation.payload.IsmsPlanPayload;
import dev.jstech.computers.operation.payload.IsmsSchemaPayload;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.RequestIsmsSchemaPayload;
import dev.jstech.computers.operation.payload.SaveFilePayload;
import dev.jstech.computers.printer.PrintLayout;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.iql.IqlScript;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.MenuBar;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.logic.TextDocument;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
 * The IQL Server Management Studio: the network administered from any computer on it, the way the database studio
 * administers a server. A menu bar where every item does something, a toolbar, the Object Explorer with a menu on
 * each node, query tabs that run their scripts a statement at a time against the real columns of each table, a status
 * bar with the engine, its version, the computer and the time a run took; and beside the queries the Object Explorer
 * Details, the Activity Monitor, the Template Explorer and the reference, with the Profiler in a window of its own.
 *
 * <p>It wears the look of the age of the computer it runs on: the Query Analyzer of 2000 on a Legacy one, the studio
 * of 2008, 2012 or 2022 after. Its scripts, results and settings are files on that computer's disk.
 *
 * <p>Every request carries the window's own number, so two studios open at once each get their own answers.
 */
public final class IsmsApp implements IDesktopApp, CodeFileReplies.IReader {

    private final BlockPos host;
    private final BlockPos monitor;
    private final int window;
    private final IsmsSkin skin = new IsmsSkin();
    private final MenuBar menuBar = new MenuBar(92, MenuBar.HEIGHT);
    private final ContextMenu context = new ContextMenu(110, 10);
    private final IsmsExplorer explorer = new IsmsExplorer();
    private final List<IsmsDocument> docs = new ArrayList<>();
    private final FileDialog files;
    private final PrintDialog print;
    private final EditorFindBar find;
    private final IsmsQueries queries = new IsmsQueries(this);
    private final IsmsDialogs dialogs = new IsmsDialogs(this);
    private final IsmsMenus menus = new IsmsMenus(this);
    private final ProcessList processes = new ProcessList();
    private final TextField search = new TextField(40);
    /** What holds the details' search field, and hands it the keyboard. */
    private final Panel searchPanel = new Panel();
    private final List<ToolButton> toolbar = new ArrayList<>();
    /** The files waiting to be read, the settings first; one is asked for at a time. */
    private final Deque<String> opening = new ArrayDeque<>();
    /** The saves sent and not yet answered, in the order they were sent. */
    private final Deque<PendingSave> saving = new ArrayDeque<>();
    private IsmsSettings settings = new IsmsSettings();
    @Nullable
    private IsmsSchemaPayload schema;
    private IsmsLook look = IsmsLook.STUDIO_2012;
    private Font font = Minecraft.getInstance().font;
    private int active;
    private int nextDoc = 1;
    private int queriesOpened;
    private int explorerW = IsmsLayout.EXPLORER_W;
    private int editorH = -1;
    private boolean explorerShown = true;
    private boolean propertiesShown;
    private boolean resultsShown = true;
    private Focus focus = Focus.CODE;
    private Drag dragging = Drag.NONE;
    private boolean started;
    private String noticeShownFor = "";
    private int frame;
    private int originX;
    private int originY;
    private int width = IsmsLayout.DEFAULT_W;
    private int height = IsmsLayout.DEFAULT_H;
    private int mouseX;
    private int mouseY;
    private long lastClickAt;
    private Object lastClicked = "";
    private int detailsSelected = -1;
    private int pageScroll;

    /** The program's id, whose window the desktop keeps with the machine. */
    public static final String KEY = Programs.ISMS.toString();
    private static final int TAB_ROOM = 18;
    private static final int DOUBLE_CLICK_MS = 400;
    private static final int ACTIVITY_EVERY = 20;
    private static final int MOST_COMPLETIONS = 12;
    private static final int MOST_SAVED_TEXT = 2000;
    private static final String COMMENT = "-- ";
    private static final String STATE_FILE = "file\t";
    private static final String STATE_QUERY = "query\t";
    private static final String SCRIPT_EXTENSION = "iql";
    private static final String RESULTS_EXTENSION = "csv";

    /** Every studio window open on this client, which the network's answers are routed among. */
    private static final List<IsmsApp> OPEN = new ArrayList<>();
    private static int nextWindow;

    public IsmsApp(final BlockPos host, final BlockPos monitor) {
        this.host = host;
        this.monitor = monitor;
        this.window = nextWindowNumber();
        this.files = new FileDialog(host, this);
        this.print = new PrintDialog(host, this);
        this.find = new EditorFindBar(() -> {
            final IsmsDocument doc = current();
            return doc == null ? null : doc.code.document();
        });
        this.processes.setOnPick((operation, button) -> pickOperation(operation));
        this.searchPanel.add(search);
        this.search.setOnEdit(() -> {
            final IsmsDocument doc = current();
            if (doc != null) {
                doc.search = search.edit();
            }
        });
        menuBar.add(GameText.resolve(IsmsTexts.FILE), menus::file)
                .add(GameText.resolve(IsmsTexts.EDIT), menus::edit)
                .add(GameText.resolve(IsmsTexts.VIEW), menus::view)
                .add(GameText.resolve(IsmsTexts.QUERY), menus::query)
                .add(GameText.resolve(IsmsTexts.TOOLS), menus::tools)
                .add(GameText.resolve(IsmsTexts.WINDOW), menus::window)
                .add(GameText.resolve(IsmsTexts.HELP), menus::help);
        OPEN.add(this);
    }

    /** A number for a studio or Profiler window, unique among the windows this client opened. */
    static int nextWindowNumber() {
        return ++nextWindow;
    }

    /** An answer to a statement, or to something asked beside one, for the window that asked. */
    public static void accept(final IqlResultPayload payload) {
        for (final IsmsApp app : List.copyOf(OPEN)) {
            if (app.window == payload.window()) {
                app.queries.answer(payload);
            }
        }
    }

    /** The network as the window that asked shows it. */
    public static void acceptSchema(final IsmsSchemaPayload payload) {
        for (final IsmsApp app : List.copyOf(OPEN)) {
            if (app.window == payload.window()) {
                app.applySchema(payload);
            }
        }
    }

    /** A craft's estimated plan, for the window that asked. */
    public static void acceptPlan(final IsmsPlanPayload payload) {
        for (final IsmsApp app : List.copyOf(OPEN)) {
            if (app.window == payload.window()) {
                app.queries.acceptPlan(payload);
            }
        }
    }

    /** The network's Operations in flight, which every open Activity Monitor shows. */
    public static void acceptActiveOps(final List<OperationRecord> operations, final int slotsUsed,
                                       final int slotsTotal) {
        for (final IsmsApp app : OPEN) {
            app.processes.show(operations, slotsUsed, slotsTotal);
        }
    }

    /** The studio window opened last, for a test to drive. */
    @Nullable
    public static IsmsApp latest() {
        return OPEN.isEmpty() ? null : OPEN.get(OPEN.size() - 1);
    }

    @Override
    public String title() {
        final IsmsDocument doc = current();
        final String network = schema == null ? "" : GameText.resolve(schema.network());
        return GameText.resolve(IsmsTexts.TITLE.with(doc == null ? "" : doc.title(), network, productName()));
    }

    @Override
    public int defaultWidth() {
        return IsmsLayout.DEFAULT_W;
    }

    @Override
    public int defaultHeight() {
        return IsmsLayout.DEFAULT_H;
    }

    @Override
    public int minWidth() {
        return IsmsLayout.MIN_W;
    }

    @Override
    public int minHeight() {
        return IsmsLayout.MIN_H;
    }

    @Override
    public void onClosed() {
        OPEN.remove(this);
        CodeFileReplies.forget(this);
    }

    @Override
    public void onRestored() {
        if (!OPEN.contains(this)) {
            OPEN.add(this);
        }
        askSchema();
    }

    @Override
    public String saveState() {
        final StringBuilder out = new StringBuilder();
        for (final IsmsDocument doc : docs) {
            if (!doc.isQuery()) {
                continue;
            }
            if (!doc.path.isEmpty() && !doc.dirty) {
                out.append(STATE_FILE).append(doc.path).append('\n');
            } else {
                final String text = doc.code.text();
                final String kept = text.length() > MOST_SAVED_TEXT ? text.substring(0, MOST_SAVED_TEXT) : text;
                out.append(STATE_QUERY).append(IsmsQueryEscaping.escape(kept)).append('\n');
            }
        }
        return out.toString();
    }

    @Override
    public void restoreState(final String state) {
        for (final String line : state.split("\n")) {
            if (line.startsWith(STATE_FILE)) {
                openPath(line.substring(STATE_FILE.length()));
            } else if (line.startsWith(STATE_QUERY)) {
                openNew("", IsmsQueryEscaping.unescape(line.substring(STATE_QUERY.length())));
            }
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
        this.frame++;
        look = IsmsSkin.lookOf(host);
        skin.wear(look);
        start();
        if (frame % ACTIVITY_EVERY == 0 && showing(IsmsDocument.Kind.ACTIVITY)) {
            action(IsmsActionPayload.ACTIVITY, "", 0);
        }
        final IsmsLook.Colours c = look.colours();
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, c.window());
        g.fillGradient(x, y, x + width, y + MenuBar.HEIGHT, c.menu(), c.menuTo());
        drawToolbar(g, c);
        final int statusY = y + IsmsLayout.statusY(height);
        int right = x;
        if (explorerShown) {
            explorerW = IsmsLayout.explorerW(width, explorerW);
            drawExplorer(g, c, x, y + IsmsLayout.BODY_Y, explorerW, statusY - y - IsmsLayout.BODY_Y);
            right = x + explorerW;
            g.fill(right, y + IsmsLayout.BODY_Y, right + IsmsLayout.VSPLIT_W, statusY,
                    dragging == Drag.EXPLORER ? c.select() : c.split());
            g.fill(right, y + IsmsLayout.BODY_Y, right + 1, statusY, c.splitEdge());
            g.fill(right + IsmsLayout.VSPLIT_W - 1, y + IsmsLayout.BODY_Y, right + IsmsLayout.VSPLIT_W, statusY,
                    c.splitEdge());
            right += IsmsLayout.VSPLIT_W;
        }
        drawDocuments(g, ctx, c, right, y + IsmsLayout.BODY_Y, x + width - right, statusY);
        drawStatus(g, c, x, statusY, width);
        menuBar.setBounds(x, y, width, MenuBar.HEIGHT);
        menuBar.setWindow(x, y, width, height);
        menuBar.render(g, ctx);
        context.render(g, ctx);
    }

    @Override
    public boolean modalActive() {
        return dialogs.isOpen();
    }

    @Override
    public void renderModal(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                            final int height, final int mouseX, final int mouseY) {
        dialogs.render(g, new UiContext(skin, font, mouseX, mouseY, 0f), x, y, width, height);
    }

    @Override
    public boolean wantsEscape() {
        return dialogs.isOpen() || context.isOpen() || menuBar.isOpen() || find.isOpen() || focus == Focus.SEARCH;
    }

    @Override
    public void mouseClicked(final DesktopWindow win, final double mx, final double my, final int button) {
        if (dialogs.isOpen()) {
            dialogs.mouseClicked(mx, my, button);
            return;
        }
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
            clickToolbar(lx, (int) mx, (int) my);
            return;
        }
        if (ly >= IsmsLayout.statusY(height) || ly < IsmsLayout.BODY_Y) {
            return;
        }
        if (explorerShown && Math.abs(lx - explorerW - IsmsLayout.VSPLIT_W / 2) <= 3) {
            dragging = Drag.EXPLORER;
            return;
        }
        if (explorerShown && lx < explorerW) {
            clickExplorer(lx, ly, (int) mx, (int) my, button);
            return;
        }
        clickDocuments(mx, my, lx, ly, button);
    }

    @Override
    public void mouseDragged(final DesktopWindow win, final double mx, final double my, final int button) {
        final int lx = (int) mx - originX;
        final int ly = (int) my - originY;
        switch (dragging) {
            case EXPLORER -> explorerW = IsmsLayout.explorerW(width, lx);
            case EDITOR -> editorH = IsmsLayout.editorH(height, ly - IsmsLayout.BODY_Y - IsmsLayout.TABS_H);
            case NONE -> {
                final IsmsDocument doc = current();
                if (doc != null && doc.isQuery() && focus == Focus.CODE) {
                    doc.code.mouseDragged(mx, my, button);
                }
            }
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow win, final double mx, final double my, final int button) {
        dragging = Drag.NONE;
        if (dialogs.isOpen()) {
            dialogs.mouseReleased(mx, my, button);
            return;
        }
        find.mouseReleased(mx, my, button);
        final IsmsDocument doc = current();
        if (doc != null && doc.isQuery()) {
            doc.code.mouseReleased(mx, my, button);
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final int lx = mouseX - originX;
        final int ly = mouseY - originY;
        final int step = delta > 0 ? -1 : 1;
        if (explorerShown && lx < explorerW) {
            explorer.scrollBy(step * 3, treeFit());
            return true;
        }
        final IsmsDocument doc = current();
        if (doc == null) {
            return false;
        }
        switch (doc.kind) {
            case QUERY -> {
                if (resultsShown && ly >= resultsTop()) {
                    doc.resultsScroll = Math.max(0, Math.min(Math.max(0, IsmsResultsView.lines(doc) - 1),
                            doc.resultsScroll + step * 2));
                } else {
                    doc.code.mouseScrolled(mouseX, mouseY, delta);
                }
            }
            case ACTIVITY -> processes.scrollBy(step);
            default -> pageScroll = Math.max(0, pageScroll + step * 2);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (dialogs.isOpen()) {
            return dialogs.keyPressed(key, scanCode, modifiers);
        }
        if (context.isOpen()) {
            return context.keyPressed(key, scanCode, modifiers);
        }
        if (menuBar.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        if (shortcut(key, modifiers)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (find.isOpen()) {
                find.close();
                focus = Focus.CODE;
            } else {
                focus = Focus.CODE;
            }
            return true;
        }
        if (find.isOpen() && find.hasFocus()) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                find.go();
                return true;
            }
            return find.keyPressed(key, scanCode, modifiers);
        }
        if (focus == Focus.SEARCH) {
            return searchPanel.keyPressed(key, scanCode, modifiers);
        }
        final IsmsDocument doc = current();
        return doc != null && doc.isQuery() && doc.code.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c) {
        if (dialogs.isOpen()) {
            return dialogs.charTyped(c);
        }
        if (context.isOpen() || menuBar.isOpen()) {
            return true;
        }
        if (find.isOpen() && find.hasFocus()) {
            return find.charTyped(c);
        }
        if (focus == Focus.SEARCH) {
            return searchPanel.charTyped(c);
        }
        final IsmsDocument doc = current();
        return doc != null && doc.isQuery() && doc.code.charTyped(c);
    }

    @Override
    public void onContent(final String path, final String content, final boolean exists) {
        final String asked = opening.poll();
        if (IsmsSettings.FILE.equals(path)) {
            settings = exists ? IsmsSettings.read(content) : new IsmsSettings();
            applySettings();
        } else if (asked != null && asked.equals(path)) {
            if (exists) {
                final IsmsDocument doc = openNew("", content);
                doc.path = path;
                doc.dirty = false;
                doc.status = IsmsTexts.OPENED.with(path);
                settings.opened(path);
                saveSettings();
            } else {
                say(IsmsTexts.COULD_NOT_OPEN.with(path), false);
            }
        }
        askNextFile();
    }

    @Override
    public void onContentTooLarge(final String path) {
        opening.poll();
        say(IsmsTexts.TOO_LARGE.with(path), false);
        askNextFile();
    }

    @Override
    public void onSaved(final boolean ok, final Text message) {
        final PendingSave done = saving.poll();
        if (done == null || done.quiet()) {
            return;
        }
        if (done.doc() != null && ok) {
            done.doc().path = done.path();
            done.doc().dirty = false;
            settings.opened(done.path());
            saveSettings();
        }
        if (done.after() != null && ok) {
            done.after().run();
        }
        say(message, ok);
    }

    // What the menus, the queries and the dialogs ask of the window.

    @Nullable
    IsmsDocument current() {
        return docs.isEmpty() ? null : docs.get(Math.max(0, Math.min(active, docs.size() - 1)));
    }

    @Nullable
    IsmsDocument document(final int id) {
        for (final IsmsDocument doc : docs) {
            if (doc.id == id) {
                return doc;
            }
        }
        return null;
    }

    List<IsmsDocument> documents() {
        return docs;
    }

    IsmsSettings settings() {
        return settings;
    }

    IsmsQueries queries() {
        return queries;
    }

    IsmsDialogs dialogs() {
        return dialogs;
    }

    IsmsLook look() {
        return look;
    }

    IsmsExplorer explorer() {
        return explorer;
    }

    Font font() {
        return font;
    }

    BlockPos host() {
        return host;
    }

    BlockPos monitor() {
        return monitor;
    }

    int window() {
        return window;
    }

    /** Whether the network runs a Midsoft IQL Server the studio can work with. */
    boolean connected() {
        return schema != null && schema.engine().compatible()
                && schema.engine().state() != IsmsSchemaPayload.EngineState.NOT_INSTALLED;
    }

    /** Why the studio is not connected, in a line. */
    Text connectionProblem() {
        return IsmsTexts.NO_ENGINE.text();
    }

    /** The studio's own name in its age: the Query Analyzer, or the Management Studio. */
    String productName() {
        return GameText.resolve(look.analyzer() ? IsmsTexts.ANALYZER : IsmsTexts.STUDIO);
    }

    void send(final CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    /** Asks the network for something beside a statement, the window's number with it. */
    void action(final int action, final String target, final int arg) {
        send(new IsmsActionPayload(monitor, host, window, action, target, arg));
    }

    /** Asks the network again for what the explorer shows. */
    void askSchema() {
        send(new RequestIsmsSchemaPayload(host, window));
    }

    void newQuery() {
        openNew("", "");
    }

    /** Opens a new query tab named after {@code name}'s query count, holding {@code text}. */
    IsmsDocument openNew(final String name, final String text) {
        queriesOpened++;
        final IsmsDocument doc = new IsmsDocument(nextDoc++, IsmsDocument.Kind.QUERY,
                GameText.resolve(IsmsTexts.QUERY_NAME.with(queriesOpened)));
        doc.mode = settings.results;
        doc.code.setText(text);
        doc.dirty = false;
        docs.add(doc);
        active = docs.size() - 1;
        focus = Focus.CODE;
        return doc;
    }

    /** Opens {@code text} in a new query and runs it. */
    void runNew(final String name, final String text) {
        queries.execute(openNew(name, text), false);
    }

    /** Opens the template called {@code name} in a new query. */
    void openTemplate(final TextKey name) {
        for (final IsmsTemplates.Template template : IsmsTemplates.ALL) {
            if (template.name() == name) {
                openNew("", template.text());
                return;
            }
        }
    }

    void chooseOpen() {
        files.openFile(IsmsTexts.OPEN_FILE.text(), "", List.of(FileDialog.Filter.of(IsmsTexts.SCRIPTS,
                SCRIPT_EXTENSION), FileDialog.Filter.ALL), this::openPath);
    }

    /** Reads {@code path} from the computer's disk into a new query. */
    void openPath(final String path) {
        if (path == null || path.isBlank()) {
            return;
        }
        opening.add(path);
        if (opening.size() == 1) {
            askNextFile();
        }
    }

    void closeDocument(@Nullable final IsmsDocument doc) {
        if (doc == null) {
            return;
        }
        if (doc.dirty && doc.isQuery()) {
            dialogs.ask(IsmsTexts.UNSAVED.with(doc.title()), () -> drop(doc));
            return;
        }
        drop(doc);
    }

    void closeAll() {
        for (final IsmsDocument doc : List.copyOf(docs)) {
            if (!doc.dirty || !doc.isQuery()) {
                drop(doc);
            }
        }
        final IsmsDocument left = current();
        if (left != null) {
            closeDocument(left);
        }
    }

    void save(@Nullable final IsmsDocument doc) {
        if (doc == null || !doc.isQuery()) {
            return;
        }
        if (doc.path.isEmpty()) {
            chooseSaveAs(doc);
            return;
        }
        write(doc.path, doc.code.text(), doc, null, false);
    }

    void chooseSaveAs(@Nullable final IsmsDocument doc) {
        if (doc == null || !doc.isQuery()) {
            return;
        }
        files.saveAs(IsmsTexts.SAVE_AS.text(), "", doc.suggestedFile(), List.of(FileDialog.Filter.of(
                IsmsTexts.SCRIPTS, SCRIPT_EXTENSION)), path -> write(withExtension(path, SCRIPT_EXTENSION),
                doc.code.text(), doc, null, false));
    }

    void saveAll() {
        for (final IsmsDocument doc : docs) {
            if (doc.isQuery() && doc.dirty && !doc.path.isEmpty()) {
                save(doc);
            }
        }
    }

    /** Writes the studio's choices to its settings file, without a word about it. */
    void saveSettings() {
        write(IsmsSettings.FILE, settings.write(), null, null, true);
    }

    /** Asks where to put {@code doc}'s results and writes them there, comma-separated. */
    void saveResults(final IsmsDocument doc) {
        if (doc.results.isEmpty()) {
            say(IsmsTexts.NO_RESULTS_TO_SAVE.text(), false);
            return;
        }
        final StringBuilder csv = new StringBuilder();
        for (final IsmsDocument.ResultSet set : doc.results) {
            if (!csv.isEmpty()) {
                csv.append("\n\n");
            }
            csv.append(IqlScript.csv(set.columns(), set.rows()));
        }
        final String base = doc.suggestedFile().replaceFirst("\\.iql$", "");
        files.saveAs(IsmsTexts.TO_FILE.text(), "", base + "." + RESULTS_EXTENSION, List.of(FileDialog.Filter.of(
                IsmsTexts.RESULT_FILES, RESULTS_EXTENSION)), path -> {
                    final String target = withExtension(path, RESULTS_EXTENSION);
                    write(target, csv.toString(), null, () -> doc.say(IsmsTexts.RESULTS_SAVED.with(target),
                            IsmsDocument.Tone.PLAIN), false);
                });
    }

    /** The query and its results, to the computer's printer, by way of the system's Print window. */
    void print() {
        final IsmsDocument doc = current();
        if (doc == null || !doc.isQuery()) {
            return;
        }
        final StringBuilder text = new StringBuilder(doc.code.text().strip());
        for (final IsmsDocument.ResultSet set : doc.results) {
            final List<List<String>> table = new ArrayList<>();
            table.add(set.columns());
            table.addAll(set.rows());
            text.append("\n\n").append(PrintLayout.table(table, true));
        }
        print.show(PrintDialog.Document.text(doc.title(), ActiveDesktop.windowName(KEY), text.toString()));
    }

    void exit() {
        ActiveDesktop.closeWindowFor(this);
    }

    /** Does what Ctrl and {@code letter} do in the editor: undo, redo, cut, copy, paste or select all. */
    void editKey(@Nullable final IsmsDocument doc, final char letter) {
        if (doc != null && doc.isQuery()) {
            doc.code.keyPressed(letter, 0, GLFW.GLFW_MOD_CONTROL);
            focus = Focus.CODE;
        }
    }

    /** Opens the find strip under the editor, to find and replace or to go to a line. */
    void find(final boolean goTo) {
        final IsmsDocument doc = current();
        if (doc != null && doc.isQuery()) {
            find.open(goTo ? EditorFindBar.Mode.GO_TO_LINE : EditorFindBar.Mode.FIND);
            focus = Focus.FIND;
        }
    }

    /** Comments the selected lines out with two dashes, or takes the dashes off again. */
    void comment(@Nullable final IsmsDocument doc) {
        if (doc != null && doc.isQuery()) {
            doc.code.document().toggleLinePrefix(COMMENT);
            doc.dirty = true;
        }
    }

    /** Writes what is selected in capitals, or in small letters. */
    void changeCase(@Nullable final IsmsDocument doc, final boolean upper) {
        if (doc == null || !doc.isQuery() || !doc.code.document().hasSelection()) {
            return;
        }
        final TextDocument text = doc.code.document();
        final TextDocument.Spot from = text.selectionStart();
        final TextDocument.Spot to = text.selectionEnd();
        final String selected = text.selectedText();
        text.deleteSelection();
        text.insertText(upper ? selected.toUpperCase(Locale.ROOT) : selected.toLowerCase(Locale.ROOT));
        text.select(from.line(), from.col(), to.line(), to.col());
        doc.dirty = true;
    }

    /** Lists the names that start with the word at the caret, for one to be put in its place. */
    void listMembers(@Nullable final IsmsDocument doc) {
        if (doc == null || !doc.isQuery()) {
            return;
        }
        final String prefix = wordAtCaret(doc);
        final List<String> matches = completions(prefix);
        if (matches.isEmpty()) {
            say(IsmsTexts.NO_MATCH.with(prefix), false);
            return;
        }
        final List<ContextMenu.Item> items = new ArrayList<>();
        for (final String match : matches) {
            items.add(new ContextMenu.Item(match, true, () -> complete(doc, prefix, match)));
        }
        final int[] caret = doc.code.caretPixel();
        context.open(items, caret[0], caret[1] + 10, originX, originY, width, height);
    }

    /** Finishes the word at the caret when only one name fits it, and lists them when more do. */
    void completeWord(@Nullable final IsmsDocument doc) {
        if (doc == null || !doc.isQuery()) {
            return;
        }
        final String prefix = wordAtCaret(doc);
        final List<String> matches = completions(prefix);
        if (matches.size() == 1) {
            complete(doc, prefix, matches.get(0));
        } else {
            listMembers(doc);
        }
    }

    void toggleExplorer() {
        explorerShown = !explorerShown;
    }

    void toggleProperties() {
        propertiesShown = !propertiesShown;
        explorerShown |= propertiesShown;
    }

    void showProperties() {
        propertiesShown = true;
        explorerShown = true;
    }

    void toggleResults() {
        resultsShown = !resultsShown;
    }

    void resultsTo(@Nullable final IsmsDocument doc, final IsmsSettings.Results mode) {
        if (doc != null) {
            doc.mode = mode;
        }
    }

    /** Opens one of the studio's own pages, or brings it forward when it is open. */
    void openPage(final IsmsDocument.Kind kind, final TextKey name) {
        for (int i = 0; i < docs.size(); i++) {
            if (docs.get(i).kind == kind) {
                active = i;
                return;
            }
        }
        docs.add(new IsmsDocument(nextDoc++, kind, GameText.resolve(name)));
        active = docs.size() - 1;
        pageScroll = 0;
    }

    void openDetails() {
        detailsSelected = -1;
        openPage(IsmsDocument.Kind.DETAILS, IsmsTexts.EXPLORER_DETAILS);
    }

    void openActivity() {
        openPage(IsmsDocument.Kind.ACTIVITY, IsmsTexts.ACTIVITY_MONITOR);
        action(IsmsActionPayload.ACTIVITY, "", 0);
    }

    void openProfiler() {
        ActiveDesktop.openOrFocus(IsmsProfilerApp.KEY);
    }

    void indexMaintenance() {
        final IsmsSchemaPayload s = schema;
        if (s == null || s.index() == null) {
            return;
        }
        final Text standing = IsmsTexts.INDEX_TEXT.with(IsmsExplorer.health(s.index().state()).text(),
                s.index().catalog(), s.index().servers());
        dialogs.indexMaintenance(standing, verb -> runNew("", IsmsTemplates.maintenance(verb)));
    }

    void about() {
        final IsmsSchemaPayload s = schema;
        final String network = s == null ? "" : GameText.resolve(s.network());
        final String hostName = s == null ? "" : s.host();
        dialogs.note(IsmsTexts.ABOUT.with(productName()), IsmsTexts.ABOUT_TEXT.with(productName(),
                ActiveDesktop.installedVersion(KEY), engineProduct(), network, hostName), false);
    }

    void activate(final int index) {
        if (index >= 0 && index < docs.size()) {
            active = index;
            pageScroll = 0;
        }
    }

    void stepTab(final int step) {
        if (!docs.isEmpty()) {
            activate(Math.floorMod(active + step, docs.size()));
        }
    }

    void resetLayout() {
        explorerW = IsmsLayout.EXPLORER_W;
        editorH = -1;
        explorerShown = true;
        resultsShown = true;
        propertiesShown = false;
    }

    /** Tells, in the tab in front, how many rows {@code table} holds. */
    void countRows(final String table) {
        final IsmsDocument doc = current() != null && current().isQuery() ? current() : openNew("", "");
        doc.say(IsmsTexts.HOLDS_ROWS.with(table, explorer.rowsOf(table)), IsmsDocument.Tone.PLAIN);
        doc.tab = IsmsDocument.ResultsTab.MESSAGES;
    }

    void copy(final String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
        say(IsmsTexts.COPIED.text(), true);
    }

    /** Opens the statement that makes the view or procedure of {@code node} again. */
    void scriptCreate(final IsmsExplorer.Node node) {
        final IsmsSchemaPayload.Saved saved = explorer.saved(node.kind, node.name);
        if (saved != null) {
            openNew(node.name, node.kind == IsmsExplorer.Kind.VIEW
                    ? IsmsTemplates.createView(saved.name(), saved.body())
                    : IsmsTemplates.createProcedure(saved.name(), saved.body()));
        }
    }

    /** Opens the statements that replace the procedure of {@code node} with what is edited. */
    void modify(final IsmsExplorer.Node node) {
        final IsmsSchemaPayload.Saved saved = explorer.saved(IsmsExplorer.Kind.PROCEDURE, node.name);
        if (saved != null) {
            openNew(node.name, IsmsTemplates.modifyProcedure(saved.name(), saved.body()));
        }
    }

    /** The word a results mode goes by on the toolbar's button. */
    static Text modeWord(final IsmsSettings.Results mode) {
        return switch (mode) {
            case GRID -> IsmsTexts.GRID.text();
            case TEXT -> IsmsTexts.TEXT.text();
            case FILE -> IsmsTexts.TO_A_FILE.text();
        };
    }

    // What a test asks of the window, in coordinates of the window's content.

    /** The middle of the {@code index}-th title of the menu bar. */
    public int[] menuTitleCenter(final int index) {
        return local(menuBar.titleCenter(index));
    }

    /** The middle of item {@code index} of the menu open, or of its submenu when one is open beside it. */
    public int[] menuItemCenter(final int index) {
        final ContextMenu open = menuBar.isOpen() ? menuBar.menu() : context;
        final ContextMenu shown = open.openSubmenu() != null ? open.openSubmenu() : open;
        return local(shown.itemCenter(index));
    }

    /** The labels of the menu open, from the menu bar or a context menu. */
    public List<String> openMenuLabels() {
        final ContextMenu open = menuBar.isOpen() ? menuBar.menu() : context;
        final List<String> out = new ArrayList<>();
        if (open.isOpen()) {
            open.items().forEach(item -> out.add(item.label()));
        }
        return out;
    }

    /** The middle of the toolbar button whose label is {@code word}, or null. */
    @Nullable
    public int[] toolbarCenter(final String word) {
        for (final ToolButton button : toolbar) {
            if (button.label().equals(word)) {
                return new int[] {button.x() + button.w() / 2, IsmsLayout.TOOLBAR_Y + IsmsLayout.TOOLBAR_H / 2};
            }
        }
        return null;
    }

    /** The middle of the explorer row of the node {@code key} names, revealing it first; null when not shown. */
    @Nullable
    public int[] nodeCenter(final String key) {
        final IsmsExplorer.Node node = explorer.node(key);
        if (node == null) {
            return null;
        }
        final int top = explorer.rowTop(node, treeFit());
        return top < 0 ? null : new int[] {explorerW / 2, IsmsLayout.BODY_Y + IsmsLayout.EX_HEAD_H + top + 5};
    }

    /** Opens every node above the node {@code key} names, so its row is drawn. */
    public void revealNode(final String key) {
        final IsmsExplorer.Node node = explorer.node(key);
        if (node == null) {
            return;
        }
        IsmsExplorer.Node at = node;
        while (at != null) {
            explorer.reveal(at);
            at = parentOf(at);
        }
    }

    /** The middle of the document tab {@code index}. */
    public int[] docTabCenter(final int index) {
        int x = (explorerShown ? explorerW + IsmsLayout.VSPLIT_W : 0);
        for (int i = 0; i < docs.size(); i++) {
            final int w = font.width(docs.get(i).title()) + TAB_ROOM;
            if (i == index) {
                return new int[] {x + w / 2 - 4, IsmsLayout.BODY_Y + IsmsLayout.TABS_H / 2};
            }
            x += w;
        }
        return new int[] {x, IsmsLayout.BODY_Y};
    }

    /** The middle of the results tab {@code index} of the query in front: Results, Messages, Plan. */
    public int[] resultsTabCenter(final int index) {
        final int x = explorerShown ? explorerW + IsmsLayout.VSPLIT_W : 0;
        return IsmsResultsView.tabCenter(font, x, resultsTop() - IsmsLayout.RES_TABS_H,
                IsmsResultsView.TABS.get(index));
    }

    /** Which results tab is in front, as its name: RESULTS, MESSAGES or PLAN. */
    public String resultsTab() {
        final IsmsDocument doc = current();
        return doc == null ? "" : doc.tab.name();
    }

    /** The middle of the editor of the query in front. */
    public int[] editorCenter() {
        final IsmsDocument doc = current();
        return doc == null ? new int[] {0, 0} : local(doc.code.center());
    }

    /** What the status bar says at its left. */
    public String statusText() {
        final IsmsDocument doc = current();
        return doc == null ? GameText.resolve(IsmsTexts.READY) : GameText.resolve(doc.status);
    }

    /** What the document in front is: QUERY, DETAILS, ACTIVITY, TEMPLATES, REFERENCE or SHORTCUTS. */
    public String documentKind() {
        final IsmsDocument doc = current();
        return doc == null ? "" : doc.kind.name();
    }

    /** How many documents are open. */
    public int documentCount() {
        return docs.size();
    }

    /** Puts {@code text} in the query in front's editor, as typing it would. */
    public void typeScript(final String text) {
        final IsmsDocument doc = current();
        if (doc != null && doc.isQuery()) {
            doc.code.setText(text);
            doc.dirty = true;
        }
    }

    /** Runs the query in front, as Execute does. */
    public void execute() {
        final IsmsDocument doc = current();
        if (doc != null) {
            queries.execute(doc, false);
        }
    }

    /** What the query in front's editor holds. */
    public String scriptText() {
        final IsmsDocument doc = current();
        return doc == null || !doc.isQuery() ? "" : doc.code.text();
    }

    /** The columns of each table the query in front read, in order. */
    public List<List<String>> resultColumns() {
        final IsmsDocument doc = current();
        final List<List<String>> out = new ArrayList<>();
        if (doc != null) {
            doc.results.forEach(set -> out.add(set.columns()));
        }
        return out;
    }

    /** The rows of each table the query in front read, in order. */
    public List<List<List<String>>> resultRows() {
        final IsmsDocument doc = current();
        final List<List<List<String>>> out = new ArrayList<>();
        if (doc != null) {
            doc.results.forEach(set -> out.add(set.rows()));
        }
        return out;
    }

    /** What the query in front's Messages pane says, line by line. */
    public List<String> messageLines() {
        final IsmsDocument doc = current();
        final List<String> out = new ArrayList<>();
        if (doc != null) {
            doc.messages.forEach(message -> out.add(message.text()));
        }
        return out;
    }

    /** The query in front's estimated plan, line by line. */
    public List<String> planLines() {
        final IsmsDocument doc = current();
        final List<String> out = new ArrayList<>();
        if (doc != null) {
            doc.plan.forEach(line -> out.add(GameText.resolve(line)));
        }
        return out;
    }

    /** How many words the query in front's editor underlines. */
    public int markCount() {
        final IsmsDocument doc = current();
        return doc == null ? 0 : doc.code.marks().size();
    }

    /** Where the query in front's results go: GRID, TEXT or FILE. */
    public String resultsMode() {
        final IsmsDocument doc = current();
        return doc == null ? "" : doc.mode.name();
    }

    /** Whether the query in front is running. */
    public boolean running() {
        final IsmsDocument doc = current();
        return doc != null && doc.running();
    }

    /** The file the query in front was opened from or saved to; empty while it has none. */
    public String documentPath() {
        final IsmsDocument doc = current();
        return doc == null ? "" : doc.path;
    }

    /** The system's file window the studio opens and saves through. */
    public FileDialog fileDialog() {
        return files;
    }

    /** Picks the node {@code key} names in the explorer, as a click on its row does. */
    public void selectNode(final String key) {
        explorer.select(explorer.node(key));
    }

    /** The columns Object Explorer Details shows now. */
    public List<String> detailColumns() {
        return details().columns();
    }

    /** The rows Object Explorer Details shows now. */
    public List<List<String>> detailRows() {
        return details().rows();
    }

    /** The open dialog's lines, for a test to read. */
    public List<String> dialogText() {
        return dialogs.text();
    }

    public boolean dialogOpen() {
        return dialogs.isOpen();
    }

    /** Presses the open dialog's main button. */
    public void pressDialog() {
        dialogs.pressPrimary();
    }

    /** The look the studio wears. */
    public String lookName() {
        return look.name();
    }

    /** The explorer's labels as drawn, for a test to read. */
    public List<String> explorerLabels() {
        final List<String> out = new ArrayList<>();
        collectLabels(explorer.root(), out);
        return out;
    }

    /** The schema last shown, for a test to read. */
    @Nullable
    public IsmsSchemaPayload schema() {
        return schema;
    }

    /** The middle of the Activity Monitor's row {@code index}. */
    public int[] activityRowCenter(final int index) {
        return local(processes.rowCenter(index));
    }

    /** What Object Explorer Details lists now. */
    IsmsExplorer.Details details() {
        final IsmsExplorer.Node node = explorer.selected() == null ? explorer.root() : explorer.selected();
        final IsmsDocument doc = current();
        return explorer.details(node, doc == null ? "" : doc.search);
    }

    private void start() {
        if (started) {
            return;
        }
        started = true;
        opening.addFirst(IsmsSettings.FILE);
        askNextFile();
        askSchema();
        if (docs.isEmpty()) {
            newQuery();
        }
    }

    private void askNextFile() {
        final String next = opening.peek();
        if (next != null) {
            CodeFileReplies.expectContent(this, next);
            send(new RequestFileContentPayload(host, next));
        }
    }

    private void applySettings() {
        for (final IsmsDocument doc : docs) {
            if (doc.isQuery() && !doc.running()) {
                doc.mode = settings.results;
            }
        }
        // A studio whose owner wants no query at the start closes the one it opened, while nothing is in it.
        if (!settings.queryAtStart && docs.size() == 1 && docs.get(0).isQuery() && docs.get(0).code.text().isEmpty()
                && docs.get(0).path.isEmpty()) {
            docs.clear();
        }
    }

    private void applySchema(final IsmsSchemaPayload payload) {
        this.schema = payload;
        explorer.rebuild(payload, look, worldClock());
        final IsmsSchemaPayload.Engine engine = payload.engine();
        final String state = engine.compatible() + engine.state().name() + engine.running();
        if (!connected() && !state.equals(noticeShownFor)) {
            noticeShownFor = state;
            final Text network = payload.network();
            final Text why = engine.running().isEmpty() ? IsmsTexts.NOT_INSTALLED.with(network)
                    : IsmsTexts.OTHER_ENGINE.with(network, engine.running());
            dialogs.note((look.analyzer() ? IsmsTexts.ANALYZER : IsmsTexts.STUDIO).text(),
                    IsmsTexts.NOTICE.with(IsmsTexts.NO_ENGINE.text(), why), true);
        } else if (connected()) {
            noticeShownFor = "";
        }
    }

    private void write(final String path, final String content, @Nullable final IsmsDocument doc,
                       @Nullable final Runnable after, final boolean quiet) {
        if (content.length() > SaveFilePayload.MAX_CONTENT) {
            say(IsmsTexts.TOO_LARGE.with(path), false);
            return;
        }
        CodeFileReplies.expectSaved(this);
        saving.add(new PendingSave(path, doc, after, quiet));
        send(new SaveFilePayload(host, path, content));
    }

    private void drop(final IsmsDocument doc) {
        if (doc.running()) {
            queries.cancel(doc);
        }
        docs.remove(doc);
        active = Math.max(0, Math.min(active, docs.size() - 1));
    }

    private void say(final Text text, final boolean ok) {
        final IsmsDocument doc = current();
        if (doc != null) {
            doc.status = text;
            doc.statusOk = ok;
        }
    }

    private boolean showing(final IsmsDocument.Kind kind) {
        final IsmsDocument doc = current();
        return doc != null && doc.kind == kind;
    }

    /* The time of day on the world's clock, which the explorer stamps a table's last change with. */
    private static String worldClock() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "";
        }
        final long t = (mc.level.getDayTime() + 6000L) % 24000L;
        return String.format(Locale.ROOT, "%02d:%02d", t / 1000L, t % 1000L * 60L / 1000L);
    }

    /** The engine as the status bar and the picker name it: its name and version. */
    private String engineProduct() {
        final IsmsSchemaPayload s = schema;
        if (s == null || s.engine().name().isEmpty()) {
            return "";
        }
        return GameText.resolve(IsmsTexts.PRODUCT.with(s.engine().name(), s.engine().version())).strip();
    }

    private int treeFit() {
        final int top = IsmsLayout.BODY_Y + IsmsLayout.EX_HEAD_H;
        final int bottom = IsmsLayout.statusY(height) - (propertiesShown ? IsmsLayout.PROPERTIES_H : 0);
        return Math.max(1, (bottom - top) / IsmsLayout.TREE_PITCH);
    }

    private int editorHeight() {
        if (editorH < 0) {
            editorH = IsmsLayout.defaultEditorH(height);
        }
        return resultsShown ? IsmsLayout.editorH(height, editorH) : IsmsLayout.documentH(height);
    }

    /** The top of the results pane, its tabs, in the window's content. */
    private int resultsTop() {
        return IsmsLayout.BODY_Y + IsmsLayout.TABS_H + editorHeight() + IsmsLayout.HSPLIT_H + IsmsLayout.RES_TABS_H;
    }

    private int[] local(final int[] abs) {
        return new int[] {abs[0] - originX, abs[1] - originY};
    }

    // Drawing.

    private void drawToolbar(final GuiGraphics g, final IsmsLook.Colours c) {
        final int y = originY + IsmsLayout.TOOLBAR_Y;
        g.fillGradient(originX, y, originX + width, y + IsmsLayout.TOOLBAR_H, c.toolbar(), c.toolbarTo());
        g.fill(originX, y + IsmsLayout.TOOLBAR_H - 1, originX + width, y + IsmsLayout.TOOLBAR_H, c.edge());
        layoutToolbar();
        final int by = y + (IsmsLayout.TOOLBAR_H - IsmsLayout.BUTTON_H) / 2;
        for (final ToolButton button : toolbar) {
            final int bx = originX + button.x();
            if (button.separator()) {
                g.fill(bx + IsmsLayout.TOOLBAR_SEPARATOR / 2, by, bx + IsmsLayout.TOOLBAR_SEPARATOR / 2 + 1,
                        by + IsmsLayout.BUTTON_H, c.edge());
                continue;
            }
            final boolean hovered = button.enabled() && mouseX >= bx && mouseX < bx + button.w() && mouseY >= by
                    && mouseY < by + IsmsLayout.BUTTON_H;
            final int face = button.picker() ? c.panel() : hovered ? c.select() : c.button();
            g.fill(bx, by, bx + button.w(), by + IsmsLayout.BUTTON_H, face);
            if (look.bevelled()) {
                if (button.picker()) {
                    skin.sunken(g, bx, by, button.w(), IsmsLayout.BUTTON_H);
                } else {
                    skin.raised(g, bx, by, button.w(), IsmsLayout.BUTTON_H);
                }
            } else {
                Draw.outline(g, bx, by, button.w(), IsmsLayout.BUTTON_H, hovered ? c.selectEdge() : c.buttonEdge());
            }
            int tx = bx + IsmsLayout.BUTTON_PAD / 2;
            if (button.icon() != Icon.NONE) {
                icon(g, button.icon(), tx, by + 3, button.enabled() ? c : null);
                tx += 8;
            }
            final int colour = !button.enabled() ? c.dim() : button.picker() ? c.accent() : c.text();
            Draw.text(g, font, Texts.clip(font, button.label(), bx + button.w() - tx - 3), tx, by + 2, colour, face);
        }
    }

    /* Where each toolbar button stands this frame; the picker takes what is left, and what does not fit drops. */
    private void layoutToolbar() {
        toolbar.clear();
        final IsmsDocument doc = current();
        final boolean query = doc != null && doc.isQuery();
        final boolean running = query && doc.running();
        final List<ToolButton> left = List.of(
                button(GameText.resolve(look.analyzer() ? IsmsTexts.NEW : IsmsTexts.NEW_QUERY), Icon.NONE, true,
                        this::newQuery),
                button(GameText.resolve(IsmsTexts.OPEN), Icon.NONE, true, this::chooseOpen),
                button(GameText.resolve(IsmsTexts.SAVE), Icon.NONE, query, () -> save(current())));
        final List<ToolButton> right = List.of(
                button(GameText.resolve(IsmsTexts.EXECUTE), Icon.RUN, query && !running && connected(),
                        () -> queries.execute(current(), false)),
                button(GameText.resolve(IsmsTexts.CANCEL), Icon.STOP, running, () -> queries.cancel(current())),
                button(GameText.resolve(IsmsTexts.PARSE), Icon.CHECK, query, () -> queries.parse(current())),
                button(GameText.resolve(IsmsTexts.PLAN), Icon.NONE, query, () -> queries.plan(current())),
                button(GameText.resolve(modeWord(query ? doc.mode : settings.results)), Icon.NONE, query,
                        () -> current().mode = current().mode.next()));
        int x = IsmsLayout.BUTTON_GAP + 1;
        for (final ToolButton b : left) {
            toolbar.add(b.at(x));
            x += b.w() + IsmsLayout.BUTTON_GAP;
        }
        toolbar.add(separator(x));
        x += IsmsLayout.TOOLBAR_SEPARATOR;
        int rightW = IsmsLayout.TOOLBAR_SEPARATOR;
        for (final ToolButton b : right) {
            rightW += b.w() + IsmsLayout.BUTTON_GAP;
        }
        final String picker = pickerLabel();
        final int pickerW = Math.min(font.width(picker) + IsmsLayout.BUTTON_PAD + 10, width - x - rightW
                - IsmsLayout.BUTTON_GAP);
        if (pickerW >= 40) {
            final int pickerX = x;
            toolbar.add(new ToolButton(picker, x, pickerW, true, false, true, Icon.NONE,
                    () -> context.open(menus.server(), originX + pickerX,
                            originY + IsmsLayout.BODY_Y, originX, originY, width, height)));
            x += pickerW + IsmsLayout.BUTTON_GAP;
            toolbar.add(separator(x));
            x += IsmsLayout.TOOLBAR_SEPARATOR;
        }
        for (final ToolButton b : right) {
            if (x + b.w() > width - 2) {
                break;
            }
            toolbar.add(b.at(x));
            x += b.w() + IsmsLayout.BUTTON_GAP;
        }
    }

    private String pickerLabel() {
        final IsmsSchemaPayload s = schema;
        if (s == null) {
            return GameText.resolve(IsmsTexts.NOT_CONNECTED);
        }
        final String product = engineProduct();
        return GameText.resolve(IsmsTexts.SERVER_PICK.with(s.network(), product.isEmpty() ? GameText.resolve(
                IsmsTexts.NOT_CONNECTED) : product, s.engine().state().word()));
    }

    private ToolButton button(final String label, final Icon icon, final boolean enabled, final Runnable action) {
        final int w = font.width(label) + IsmsLayout.BUTTON_PAD + (icon == Icon.NONE ? 0 : 8);
        return new ToolButton(label, 0, w, enabled, false, false, icon, action);
    }

    private static ToolButton separator(final int x) {
        return new ToolButton("", x, IsmsLayout.TOOLBAR_SEPARATOR, false, true, false, Icon.NONE, () -> { });
    }

    private void icon(final GuiGraphics g, final Icon icon, final int x, final int y,
                      @Nullable final IsmsLook.Colours lit) {
        final IsmsLook.Colours c = look.colours();
        final int colour = lit == null ? c.dim() : switch (icon) {
            case RUN -> c.run();
            case STOP -> c.text();
            default -> c.accent();
        };
        switch (icon) {
            case RUN -> {
                for (int i = 0; i < 3; i++) {
                    g.fill(x + i, y + i, x + i + 1, y + 6 - i, colour);
                }
            }
            case STOP -> g.fill(x, y + 1, x + 4, y + 5, colour);
            case CHECK -> {
                g.fill(x, y + 3, x + 1, y + 5, colour);
                g.fill(x + 1, y + 4, x + 2, y + 6, colour);
                for (int i = 0; i < 4; i++) {
                    g.fill(x + 2 + i, y + 4 - i, x + 3 + i, y + 5 - i, colour);
                }
            }
            case NONE -> {
                // A button of words alone.
            }
        }
    }

    private void drawExplorer(final GuiGraphics g, final IsmsLook.Colours c, final int x, final int y, final int w,
                              final int h) {
        g.fillGradient(x, y, x + w, y + IsmsLayout.EX_HEAD_H, c.head(), c.headTo());
        g.fill(x, y + IsmsLayout.EX_HEAD_H - 1, x + w, y + IsmsLayout.EX_HEAD_H, c.edge());
        final String title = GameText.resolve(look.analyzer() ? IsmsTexts.OBJECT_BROWSER : IsmsTexts.OBJECT_EXPLORER);
        Draw.text(g, font, Texts.clip(font, title, w - 26), x + 4, y + 2, c.headText(), c.head());
        // The refresh mark, a ring with a gap, and the collapse mark, a bar.
        final int rx = x + w - 20;
        Draw.outline(g, rx, y + 3, 5, 5, c.dim());
        g.fill(rx + 3, y + 3, rx + 5, y + 4, c.head());
        g.fill(x + w - 9, y + 5, x + w - 4, y + 6, c.dim());
        final int treeTop = y + IsmsLayout.EX_HEAD_H;
        final int treeH = h - IsmsLayout.EX_HEAD_H - (propertiesShown ? IsmsLayout.PROPERTIES_H : 0);
        explorer.render(g, font, c, x, treeTop, w, treeH, mouseX, mouseY);
        if (propertiesShown) {
            drawProperties(g, c, x, treeTop + treeH, w, IsmsLayout.PROPERTIES_H);
        }
    }

    private void drawProperties(final GuiGraphics g, final IsmsLook.Colours c, final int x, final int y, final int w,
                                final int h) {
        g.fill(x, y, x + w, y + h, c.panel());
        g.fill(x, y, x + w, y + IsmsLayout.EX_HEAD_H, c.head());
        g.fill(x, y, x + w, y + 1, c.edge());
        Draw.text(g, font, Texts.clip(font, GameText.resolve(IsmsTexts.PROPERTIES), w - 6), x + 4, y + 2,
                c.headText(), c.head());
        final IsmsExplorer.Node node = explorer.selected();
        if (node == null) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(IsmsTexts.NO_SELECTION), w - 6), x + 4,
                    y + IsmsLayout.EX_HEAD_H + 2, c.dim(), c.panel());
            return;
        }
        int ry = y + IsmsLayout.EX_HEAD_H + 2;
        for (final IsmsExplorer.Property property : explorer.properties(node)) {
            if (ry + 9 > y + h) {
                break;
            }
            final String name = GameText.resolve(property.name());
            Draw.text(g, font, Texts.clip(font, name, w / 2 - 6), x + 4, ry, c.dim(), c.panel());
            Draw.text(g, font, Texts.clip(font, property.value(), w / 2 - 4), x + w / 2, ry, c.text(), c.panel());
            ry += 9;
        }
    }

    private void drawDocuments(final GuiGraphics g, final UiContext ctx, final IsmsLook.Colours c, final int x,
                               final int y, final int w, final int statusY) {
        g.fillGradient(x, y, x + w, y + IsmsLayout.TABS_H, c.tabs(), c.tabsTo());
        g.fill(x, y + IsmsLayout.TABS_H - 1, x + w, y + IsmsLayout.TABS_H, c.edge());
        int tx = x;
        for (int i = 0; i < docs.size(); i++) {
            final String title = docs.get(i).title();
            final int tw = font.width(title) + TAB_ROOM;
            if (tx + tw > x + w) {
                break;
            }
            skin.tab(g, font, tx, y, tw, IsmsLayout.TABS_H, title, i == active);
            Draw.text(g, font, "x", tx + tw - 9, y + 2, c.dim(), i == active && c.tabActive() != 0 ? c.tabActive()
                    : c.tabs());
            tx += tw;
        }
        final IsmsDocument doc = current();
        final int top = y + IsmsLayout.TABS_H;
        if (doc == null) {
            g.fill(x, top, x + w, statusY, c.window());
            return;
        }
        switch (doc.kind) {
            case QUERY -> drawQuery(g, ctx, c, doc, x, top, w, statusY);
            case DETAILS -> drawDetails(g, ctx, c, doc, x, top, w, statusY);
            case ACTIVITY -> {
                g.fill(x, top, x + w, statusY, c.panel());
                processes.place(x + 2, top + 2, w - 4, statusY - top - 4);
                processes.render(g, ctx);
            }
            case TEMPLATES -> drawTemplates(g, c, x, top, w, statusY);
            case REFERENCE, SHORTCUTS -> drawReference(g, c, doc.kind, x, top, w, statusY);
        }
    }

    private void drawQuery(final GuiGraphics g, final UiContext ctx, final IsmsLook.Colours c, final IsmsDocument doc,
                           final int x, final int top, final int w, final int statusY) {
        final int editor = editorHeight();
        final int findH = find.isOpen() ? EditorFindBar.HEIGHT : 0;
        doc.code.setNumbered(look.lineNumbers() && settings.lineNumbers);
        doc.code.setActive(focus == Focus.CODE && !dialogs.isOpen() && !context.isOpen() && !menuBar.isOpen());
        doc.code.setBounds(x, top, w, editor - findH);
        doc.code.render(g, ctx);
        if (find.isOpen()) {
            g.fill(x, top + editor - findH, x + w, top + editor, c.window());
            find.render(g, ctx, font, x, top + editor - findH, w);
        }
        if (!resultsShown) {
            return;
        }
        final int splitY = top + editor;
        g.fill(x, splitY, x + w, splitY + IsmsLayout.HSPLIT_H, dragging == Drag.EDITOR ? c.select() : c.split());
        g.fill(x, splitY, x + w, splitY + 1, c.splitEdge());
        g.fill(x, splitY + IsmsLayout.HSPLIT_H - 1, x + w, splitY + IsmsLayout.HSPLIT_H, c.splitEdge());
        IsmsResultsView.render(g, font, c, doc, x, splitY + IsmsLayout.HSPLIT_H, w, statusY);
    }

    private void drawDetails(final GuiGraphics g, final UiContext ctx, final IsmsLook.Colours c,
                             final IsmsDocument doc, final int x, final int top, final int w, final int statusY) {
        g.fill(x, top, x + w, statusY, c.panel());
        final IsmsExplorer.Details details = details();
        Draw.text(g, font, Texts.clip(font, details.crumbs(), w - 8), x + 4, top + 2, c.text(), c.panel());
        g.fill(x, top + 11, x + w, top + 12, c.edge());
        search.setPlaceholder(GameText.resolve(IsmsTexts.SEARCH));
        search.setBounds(x + 3, top + 14, Math.min(160, w - 6), IsmsLayout.FIELD_H);
        searchPanel.setBounds(x + 3, top + 14, Math.min(160, w - 6), IsmsLayout.FIELD_H);
        searchPanel.focus(focus == Focus.SEARCH ? search : null);
        searchPanel.render(g, ctx);
        final int listTop = top + 14 + IsmsLayout.FIELD_H + 3;
        final int fit = Math.max(1, (statusY - listTop - 12) / IsmsLayout.ROW_H);
        IsmsResultsView.table(g, font, c, new IsmsDocument.ResultSet(details.columns(), details.rows()), x, listTop,
                w, fit, pageScroll, detailsSelected);
        Draw.text(g, font, GameText.resolve(IsmsTexts.ITEMS_SHOWN.with(details.rows().size())), x + 4, statusY - 10,
                c.dim(), c.panel());
    }

    private void drawTemplates(final GuiGraphics g, final IsmsLook.Colours c, final int x, final int top,
                               final int w, final int statusY) {
        g.fill(x, top, x + w, statusY, c.panel());
        Draw.text(g, font, Texts.clip(font, GameText.resolve(IsmsTexts.TEMPLATE_HINT), w - 8), x + 4, top + 3,
                c.dim(), c.panel());
        int ry = top + 15;
        for (int i = pageScroll; i < IsmsTemplates.ALL.size() && ry + 20 <= statusY; i++) {
            final IsmsTemplates.Template template = IsmsTemplates.ALL.get(i);
            final boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + 20;
            if (hovered) {
                g.fill(x, ry, x + w, ry + 20, c.select());
            }
            final int ground = hovered ? c.select() : c.panel();
            Draw.text(g, font, GameText.resolve(template.name()), x + 6, ry + 1, c.text(), ground);
            Draw.text(g, font, Texts.clip(font, template.text().split("\n")[0], w - 16), x + 12, ry + 10, c.dim(),
                    ground);
            ry += 20;
        }
    }

    private void drawReference(final GuiGraphics g, final IsmsLook.Colours c, final IsmsDocument.Kind kind,
                               final int x, final int top, final int w, final int statusY) {
        g.fill(x, top, x + w, statusY, c.panel());
        final List<String[]> lines = kind == IsmsDocument.Kind.SHORTCUTS ? shortcutLines() : referenceLines();
        int ry = top + 3;
        for (int i = Math.min(pageScroll, Math.max(0, lines.size() - 1)); i < lines.size() && ry + 9 <= statusY;
             i++) {
            final String[] line = lines.get(i);
            Draw.text(g, font, Texts.clip(font, line[0], w / 2 - 8), x + 6, ry, c.accent(), c.panel());
            Draw.text(g, font, Texts.clip(font, line[1], w / 2 - 6), x + w / 2, ry, c.text(), c.panel());
            ry += IsmsLayout.ROW_H;
        }
    }

    private static List<String[]> referenceLines() {
        final List<String[]> out = new ArrayList<>();
        for (final IsmsTemplates.Reference entry : IsmsTemplates.REFERENCE) {
            out.add(new String[] {entry.syntax(), GameText.resolve(entry.meaning())});
        }
        return out;
    }

    private List<String[]> shortcutLines() {
        final List<String[]> out = new ArrayList<>();
        for (final ContextMenu.Item item : menus.keyedItems()) {
            out.add(new String[] {item.label(), item.keys()});
        }
        return out;
    }

    private void drawStatus(final GuiGraphics g, final IsmsLook.Colours c, final int x, final int y, final int w) {
        skin.statusBar(g, x, y, w, IsmsLayout.STATUS_H);
        final IsmsDocument doc = current();
        final boolean ok = doc == null || doc.statusOk;
        g.fill(x + 4, y + 3, x + 9, y + 8, ok ? c.led() : c.bad());
        final List<String> left = new ArrayList<>();
        left.add(GameText.resolve(doc == null ? IsmsTexts.READY.text() : doc.status));
        final IsmsSchemaPayload s = schema;
        if (s != null) {
            left.add(GameText.resolve(s.network()));
            if (!engineProduct().isEmpty()) {
                left.add(engineProduct());
            }
            left.add(GameText.resolve(IsmsTexts.HOST.with(s.host())));
        }
        final List<String> right = new ArrayList<>();
        if (doc != null && doc.isQuery()) {
            right.add(GameText.resolve(IsmsTexts.ROWS.with(doc.rowsRead)));
            right.add(IsmsQueries.clock(doc.running() ? System.currentTimeMillis() - doc.run.startedAt
                    : doc.elapsedMillis));
        }
        int rx = x + w - 4;
        final List<int[]> taken = new ArrayList<>();
        for (int i = right.size() - 1; i >= 0; i--) {
            final int tw = font.width(right.get(i));
            rx -= tw;
            taken.add(new int[] {rx, tw});
            Draw.text(g, font, right.get(i), rx, y + 2, c.statusText(), c.status());
            rx -= 10;
        }
        int lx = x + 12;
        for (final String part : left) {
            final int room = rx - lx - 4;
            if (room <= 8) {
                break;
            }
            final String shown = Texts.clip(font, part, room);
            if (look.bevelled()) {
                skin.sunken(g, lx - 2, y + 1, font.width(shown) + 4, IsmsLayout.STATUS_H - 2);
            }
            Draw.text(g, font, shown, lx, y + 2, c.statusText(), c.status());
            lx += font.width(shown) + 10;
        }
        if (look.bevelled()) {
            for (final int[] part : taken) {
                skin.sunken(g, part[0] - 2, y + 1, part[1] + 4, IsmsLayout.STATUS_H - 2);
            }
        }
    }

    // Clicks.

    private void clickToolbar(final int lx, final int mx, final int my) {
        for (final ToolButton button : toolbar) {
            if (!button.separator() && button.enabled() && lx >= button.x() && lx < button.x() + button.w()) {
                button.action().run();
                return;
            }
        }
    }

    private void clickExplorer(final int lx, final int ly, final int mx, final int my, final int button) {
        if (ly < IsmsLayout.BODY_Y + IsmsLayout.EX_HEAD_H) {
            if (lx >= explorerW - 21 && lx < explorerW - 13) {
                askSchema();
            } else if (lx >= explorerW - 11) {
                explorer.collapseAll();
            }
            return;
        }
        final int treeTop = IsmsLayout.BODY_Y + IsmsLayout.EX_HEAD_H;
        final IsmsExplorer.Row row = explorer.rowAt(treeTop, ly, treeFit());
        if (row == null || ly >= treeTop + treeFit() * IsmsLayout.TREE_PITCH) {
            return;
        }
        final IsmsExplorer.Node node = row.node();
        explorer.select(node);
        if (button == 1) {
            context.open(menus.node(node), mx, my, originX, originY, width, height);
            return;
        }
        final int indent = 3 + row.depth() * IsmsLayout.INDENT;
        final boolean twisty = lx >= indent - 1 && lx < indent + 6;
        final boolean twice = doubleClick(node);
        if (twisty || twice && !node.children.isEmpty()) {
            explorer.toggle(node);
        } else if (twice && (node.kind == IsmsExplorer.Kind.TABLE || node.kind == IsmsExplorer.Kind.COLUMN
                || node.kind == IsmsExplorer.Kind.VIEW || node.kind == IsmsExplorer.Kind.PROCEDURE)) {
            final IsmsDocument doc = current();
            if (doc != null && doc.isQuery()) {
                doc.code.document().insertText(node.name);
                doc.dirty = true;
                focus = Focus.CODE;
            }
        }
    }

    private void clickDocuments(final double mx, final double my, final int lx, final int ly, final int button) {
        final int x0 = explorerShown ? explorerW + IsmsLayout.VSPLIT_W : 0;
        if (ly < IsmsLayout.BODY_Y + IsmsLayout.TABS_H) {
            int tx = x0;
            for (int i = 0; i < docs.size(); i++) {
                final int tw = font.width(docs.get(i).title()) + TAB_ROOM;
                if (lx >= tx && lx < tx + tw) {
                    if (lx >= tx + tw - 11 || button == 2) {
                        closeDocument(docs.get(i));
                    } else {
                        activate(i);
                    }
                    return;
                }
                tx += tw;
            }
            return;
        }
        final IsmsDocument doc = current();
        if (doc == null) {
            return;
        }
        final int top = IsmsLayout.BODY_Y + IsmsLayout.TABS_H;
        switch (doc.kind) {
            case QUERY -> clickQuery(doc, mx, my, ly, top, button);
            case DETAILS -> clickDetails(lx, ly, top, x0, button, mx, my);
            case ACTIVITY -> processes.mouseClicked(mx, my, button);
            case TEMPLATES -> {
                final int index = pageScroll + (ly - top - 15) / 20;
                if (ly >= top + 15 && index >= 0 && index < IsmsTemplates.ALL.size()) {
                    openNew("", IsmsTemplates.ALL.get(index).text());
                }
            }
            default -> {
                // The reference pages are read, not clicked.
            }
        }
    }

    private void clickQuery(final IsmsDocument doc, final double mx, final double my, final int ly, final int top,
                            final int button) {
        final int editor = editorHeight();
        if (resultsShown && Math.abs(ly - (top + editor + IsmsLayout.HSPLIT_H / 2)) <= 2) {
            dragging = Drag.EDITOR;
            return;
        }
        if (ly < top + editor) {
            if (find.isOpen() && ly >= top + editor - EditorFindBar.HEIGHT) {
                find.mouseClicked(mx, my, button);
                focus = Focus.FIND;
                return;
            }
            focus = Focus.CODE;
            if (button == 1) {
                context.open(editorMenu(doc), (int) mx, (int) my, originX, originY, width, height);
                return;
            }
            doc.code.mouseClicked(mx, my, button);
            return;
        }
        if (!resultsShown) {
            return;
        }
        final int x0 = explorerShown ? explorerW + IsmsLayout.VSPLIT_W : 0;
        final IsmsDocument.ResultsTab tab = IsmsResultsView.tabAt(font, originX + x0, originY + top + editor
                + IsmsLayout.HSPLIT_H, mx, my);
        if (tab != null) {
            doc.tab = tab;
            doc.resultsScroll = 0;
        }
    }

    private void clickDetails(final int lx, final int ly, final int top, final int x0, final int button,
                              final double mx, final double my) {
        if (ly >= top + 14 && ly < top + 14 + IsmsLayout.FIELD_H && lx < x0 + 163) {
            focus = Focus.SEARCH;
            searchPanel.mouseClicked(mx, my, button);
            return;
        }
        focus = Focus.CODE;
        final int listTop = top + 14 + IsmsLayout.FIELD_H + 3 + IsmsLayout.ROW_H;
        final int index = pageScroll + (ly - listTop) / IsmsLayout.ROW_H;
        final IsmsExplorer.Details details = details();
        if (ly < listTop || index < 0 || index >= details.rows().size()) {
            return;
        }
        detailsSelected = index;
        if (doubleClick("details" + index)) {
            final IsmsExplorer.Node at = explorer.selected() == null ? explorer.root() : explorer.selected();
            final IsmsExplorer.Node shown = at.children.isEmpty() ? parentOf(at) : at;
            if (shown != null) {
                final String name = details.rows().get(index).get(0);
                for (final IsmsExplorer.Node child : shown.children) {
                    if (child.label.equals(name)) {
                        explorer.select(child);
                        explorer.reveal(shown);
                        detailsSelected = -1;
                        pageScroll = 0;
                        return;
                    }
                }
            }
        }
    }

    private List<ContextMenu.Item> editorMenu(final IsmsDocument doc) {
        return List.of(
                ContextMenu.Item.keyed(GameText.resolve(IsmsTexts.EXECUTE), IsmsMenus.EXECUTE_KEYS, !doc.running(),
                        () -> queries.execute(doc, doc.code.document().hasSelection())),
                ContextMenu.Item.keyed(GameText.resolve(IsmsTexts.PARSE), IsmsMenus.PARSE_KEYS, true,
                        () -> queries.parse(doc)),
                ContextMenu.Item.keyed(GameText.resolve(IsmsTexts.ESTIMATED_PLAN), IsmsMenus.PLAN_KEYS, true,
                        () -> queries.plan(doc)),
                ContextMenu.Item.separator(),
                ContextMenu.Item.keyed(GameText.resolve(IsmsTexts.CUT), IsmsMenus.CUT_KEYS, true,
                        () -> editKey(doc, 'X')),
                ContextMenu.Item.keyed(GameText.resolve(IsmsTexts.COPY), IsmsMenus.COPY_KEYS, true,
                        () -> editKey(doc, 'C')),
                ContextMenu.Item.keyed(GameText.resolve(IsmsTexts.PASTE), IsmsMenus.PASTE_KEYS, true,
                        () -> editKey(doc, 'V')));
    }

    private void pickOperation(final OperationRecord operation) {
        context.open(menus.operation(operation), mouseX, mouseY, originX, originY, width, height);
    }

    @Nullable
    private IsmsExplorer.Node parentOf(final IsmsExplorer.Node node) {
        return findParent(explorer.root(), node);
    }

    @Nullable
    private static IsmsExplorer.Node findParent(final IsmsExplorer.Node at, final IsmsExplorer.Node node) {
        for (final IsmsExplorer.Node child : at.children) {
            if (child == node) {
                return at;
            }
            final IsmsExplorer.Node found = findParent(child, node);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static void collectLabels(final IsmsExplorer.Node node, final List<String> out) {
        out.add(node.note.isEmpty() ? node.label : node.label + " " + node.note);
        node.children.forEach(child -> collectLabels(child, out));
    }

    private boolean doubleClick(final Object target) {
        final long now = System.currentTimeMillis();
        final boolean twice = target.equals(lastClicked) && now - lastClickAt <= DOUBLE_CLICK_MS;
        lastClicked = twice ? "" : target;
        lastClickAt = now;
        return twice;
    }

    // Keys.

    private boolean shortcut(final int key, final int modifiers) {
        final boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        final boolean alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
        final IsmsDocument doc = current();
        if (alt && key == GLFW.GLFW_KEY_PAUSE) {
            if (doc != null) {
                queries.cancel(doc);
            }
            return true;
        }
        if (ctrl && shift) {
            return switch (key) {
                case GLFW.GLFW_KEY_S -> run(this::saveAll);
                case GLFW.GLFW_KEY_U -> run(() -> changeCase(doc, true));
                case GLFW.GLFW_KEY_F -> run(() -> resultsTo(doc, IsmsSettings.Results.FILE));
                case GLFW.GLFW_KEY_M -> run(() -> {
                    if (doc != null && doc.isQuery()) {
                        dialogs.templateValues(doc);
                    }
                });
                case GLFW.GLFW_KEY_TAB -> run(() -> stepTab(-1));
                default -> false;
            };
        }
        if (ctrl) {
            return switch (key) {
                case GLFW.GLFW_KEY_N -> run(this::newQuery);
                case GLFW.GLFW_KEY_O -> run(this::chooseOpen);
                case GLFW.GLFW_KEY_S -> run(() -> save(doc));
                case GLFW.GLFW_KEY_P -> run(this::print);
                case GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_H -> run(() -> find(false));
                case GLFW.GLFW_KEY_G -> run(() -> find(true));
                case GLFW.GLFW_KEY_K -> run(() -> comment(doc));
                case GLFW.GLFW_KEY_U -> run(() -> changeCase(doc, false));
                case GLFW.GLFW_KEY_J -> run(() -> listMembers(doc));
                case GLFW.GLFW_KEY_SPACE -> run(() -> completeWord(doc));
                case GLFW.GLFW_KEY_R -> run(this::toggleResults);
                case GLFW.GLFW_KEY_E -> run(() -> queryAction(doc, true));
                case GLFW.GLFW_KEY_L -> run(() -> {
                    if (doc != null) {
                        queries.plan(doc);
                    }
                });
                case GLFW.GLFW_KEY_D -> run(() -> resultsTo(doc, IsmsSettings.Results.GRID));
                case GLFW.GLFW_KEY_T -> run(() -> resultsTo(doc, IsmsSettings.Results.TEXT));
                case GLFW.GLFW_KEY_TAB -> run(() -> stepTab(1));
                case GLFW.GLFW_KEY_F4 -> run(() -> closeDocument(doc));
                case GLFW.GLFW_KEY_F5 -> run(() -> {
                    if (doc != null) {
                        queries.parse(doc);
                    }
                });
                default -> false;
            };
        }
        if (alt) {
            return false;
        }
        return switch (key) {
            case GLFW.GLFW_KEY_F1 -> run(() -> openPage(IsmsDocument.Kind.REFERENCE, IsmsTexts.IQL_REFERENCE));
            case GLFW.GLFW_KEY_F4 -> run(this::showProperties);
            case GLFW.GLFW_KEY_F5 -> run(() -> queryAction(doc, false));
            case GLFW.GLFW_KEY_F7 -> run(this::openDetails);
            case GLFW.GLFW_KEY_F8 -> run(this::toggleExplorer);
            default -> false;
        };
    }

    private void queryAction(@Nullable final IsmsDocument doc, final boolean selection) {
        if (doc != null) {
            queries.execute(doc, selection);
        }
    }

    private static boolean run(final Runnable action) {
        action.run();
        return true;
    }

    private String wordAtCaret(final IsmsDocument doc) {
        final TextDocument text = doc.code.document();
        final String line = text.line(text.cursorLine());
        int start = Math.min(text.cursorCol(), line.length());
        while (start > 0 && (Character.isLetterOrDigit(line.charAt(start - 1)) || line.charAt(start - 1) == '_')) {
            start--;
        }
        return line.substring(start, Math.min(text.cursorCol(), line.length()));
    }

    private List<String> completions(final String prefix) {
        final String lower = prefix.toLowerCase(Locale.ROOT);
        final List<String> out = new ArrayList<>();
        for (final String name : explorer.names()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(lower) && !out.contains(name)
                    && out.size() < MOST_COMPLETIONS) {
                out.add(name);
            }
        }
        return out;
    }

    private void complete(final IsmsDocument doc, final String prefix, final String match) {
        doc.code.document().insertText(match.substring(Math.min(prefix.length(), match.length())));
        doc.dirty = true;
        focus = Focus.CODE;
    }

    private static String withExtension(final String path, final String extension) {
        return path.toLowerCase(Locale.ROOT).endsWith("." + extension) ? path : path + "." + extension;
    }

    /** Who has the keyboard: the editor, the find strip, or the details' search. */
    private enum Focus { CODE, FIND, SEARCH }

    /** Which splitter the mouse is dragging. */
    private enum Drag { NONE, EXPLORER, EDITOR }

    /** The mark a toolbar button wears before its words. */
    private enum Icon { NONE, RUN, STOP, CHECK }

    /** A toolbar button as laid out this frame. */
    private record ToolButton(String label, int x, int w, boolean enabled, boolean separator, boolean picker,
                              Icon icon, Runnable action) {

        ToolButton at(final int value) {
            return new ToolButton(label, value, w, enabled, separator, picker, icon, action);
        }
    }

    /** A save sent and waiting for its answer: where, the tab it saves, what follows, and whether it says so. */
    private record PendingSave(String path, @Nullable IsmsDocument doc, @Nullable Runnable after, boolean quiet) {
    }
}
