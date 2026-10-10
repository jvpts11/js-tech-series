/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.engine.prophet.ProphetEngine;
import dev.jstech.computers.engine.prophet.ProphetStates;
import dev.jstech.computers.gui.layout.ProphetConsoleLayout;
import dev.jstech.computers.operation.payload.ProphetActionPayload;
import dev.jstech.computers.operation.payload.ProphetConsolePayload;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Prophet Reactive Console, Prophet YourIQL's own tool. Its States tab lists the states the network is held to,
 * each with where it stands, its level against its band, the work on its way and the last thing done for it; under
 * the list, the selected state's graph (the band, the level as it went, and where the work on its way will take it)
 * and the Operations set going for it. The Subscriptions tab lists the watches, the Reactions tab what the engine did
 * lately, the Settings tab how often it looks, how much it asks for at once and whether it reacts at all. A statement
 * of YourIQL is written at the prompt and checked or applied.
 *
 * <p>Drawn in the system's own look, with the house's orange on the prompt and the status bar.
 */
public final class ProphetConsoleApp implements IDesktopApp {

    private final BlockPos host;
    private final int window;
    private final Panel root = new Panel();
    private final TabStrip tabs;
    private final TextField statement = new TextField(ProphetActionPayload.MAX_ARG);
    private final Button checkButton;
    private final Button applyButton;
    private final List<Hit> hits = new ArrayList<>();
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private ProphetConsolePayload data;
    private String selected = "";
    private Text status = Text.EMPTY;
    private boolean asking;
    private boolean askedOnce;
    private int frame;
    private int listScroll;
    private int width = ProphetConsoleLayout.DEFAULT_W;
    private int height = ProphetConsoleLayout.DEFAULT_H;

    public static final int TAB_STATES = 0;
    public static final int TAB_SUBSCRIPTIONS = 1;
    public static final int TAB_REACTIONS = 2;
    public static final int TAB_SETTINGS = 3;

    private static final int REFRESH_FRAMES = 40;
    private static final float SMALL = 0.75f;
    private static final int TICKS_PER_SECOND = 20;
    private static final String PROMPT = "YourIQL>";
    private static final String NOTHING = "-";
    private static final String SEPARATOR = "   ";
    private static final String SETTINGS_SEPARATOR = ";";

    /** Every console window open on this client, which answers are routed among by window number. */
    private static final List<ProphetConsoleApp> OPEN = new ArrayList<>();
    private static int windows;

    public ProphetConsoleApp(final BlockPos host, final BlockPos monitor) {
        this.host = host;
        this.window = ++windows;
        tabs = root.add(new TabStrip(List.of(GameText.resolve(ProphetConsoleTexts.STATES_TAB),
                GameText.resolve(ProphetConsoleTexts.SUBSCRIPTIONS_TAB),
                GameText.resolve(ProphetConsoleTexts.REACTIONS_TAB),
                GameText.resolve(ProphetConsoleTexts.SETTINGS_TAB))).setUnderline(true).fitToLabels(10)
                .setOnSelect(index -> listScroll = 0));
        root.add(statement);
        statement.setOnCommit(text -> apply());
        checkButton = root.add(new Button(GameText.resolve(ProphetConsoleTexts.CHECK), this::check));
        applyButton = root.add(new Button(GameText.resolve(ProphetConsoleTexts.APPLY), this::apply)
                .setPrimary(true));
        OPEN.add(this);
    }

    /** What the network's Prophet YourIQL says, for the window that asked. */
    public static void accept(final ProphetConsolePayload payload) {
        for (final ProphetConsoleApp app : OPEN) {
            if (app.window == payload.window()) {
                app.take(payload);
            }
        }
    }

    /** The console window opened last, for a test to drive. */
    @Nullable
    public static ProphetConsoleApp latest() {
        return OPEN.isEmpty() ? null : OPEN.get(OPEN.size() - 1);
    }

    @Override
    public String title() {
        return GameText.resolve(ProphetConsoleTexts.TITLE);
    }

    @Override
    public int defaultWidth() {
        return ProphetConsoleLayout.DEFAULT_W;
    }

    @Override
    public int defaultHeight() {
        return ProphetConsoleLayout.DEFAULT_H;
    }

    @Override
    public int minWidth() {
        return ProphetConsoleLayout.MIN_W;
    }

    @Override
    public int minHeight() {
        return ProphetConsoleLayout.MIN_H;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void onClosed() {
        OPEN.remove(this);
    }

    @Override
    public void onRestored() {
        if (!OPEN.contains(this)) {
            OPEN.add(this);
        }
        refresh();
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        width = w;
        height = h;
        if (!askedOnce) {
            askedOnce = true;
            refresh();
        }
        if (++frame % REFRESH_FRAMES == 0) {
            refresh();
        }
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        final ProphetConsolePalette.Colours c = ProphetConsolePalette.colours();
        g.fill(x, y, x + w, y + h, skin.windowBg());
        hits.clear();
        tabs.setBounds(x + ProphetConsoleLayout.PAD, y + ProphetConsoleLayout.TABS_Y,
                w - 2 * ProphetConsoleLayout.PAD, ProphetConsoleLayout.TABS_H);
        final int listX = x + ProphetConsoleLayout.PAD;
        final int listY = y + ProphetConsoleLayout.LIST_Y;
        final int listW = w - 2 * ProphetConsoleLayout.PAD;
        switch (tabs.selected()) {
            case TAB_STATES -> {
                drawStates(g, font, c, listX, listY, listW, ProphetConsoleLayout.listH(h));
                drawDetail(g, font, c, x, y + ProphetConsoleLayout.detailY(h), w);
            }
            case TAB_SUBSCRIPTIONS -> drawWatches(g, font, c, listX, listY, listW, ProphetConsoleLayout.pageH(h));
            case TAB_REACTIONS -> drawReactions(g, font, listX, listY, listW, ProphetConsoleLayout.pageH(h));
            default -> drawSettings(g, font, c, listX, listY, listW);
        }
        final int ey = y + ProphetConsoleLayout.editorY(h);
        g.fill(listX, ey, listX + ProphetConsoleLayout.PROMPT_W, ey + ProphetConsoleLayout.EDITOR_H,
                skin.fieldBg());
        Draw.text(g, font, PROMPT, listX + 2, ey + 3, c.prompt(), skin.fieldBg());
        statement.setBounds(x + ProphetConsoleLayout.statementX(), ey, ProphetConsoleLayout.statementW(w),
                ProphetConsoleLayout.EDITOR_H);
        checkButton.setBounds(x + ProphetConsoleLayout.checkX(w), ey, ProphetConsoleLayout.BUTTON_W,
                ProphetConsoleLayout.EDITOR_H);
        applyButton.setBounds(x + ProphetConsoleLayout.applyX(w), ey, ProphetConsoleLayout.BUTTON_W,
                ProphetConsoleLayout.EDITOR_H);
        root.render(g, ctx);
        drawStatus(g, font, c, x, y + ProphetConsoleLayout.statusY(h), w);
    }

    @Override
    public void mouseClicked(final DesktopWindow win, final double mx, final double my, final int button) {
        if (root.mouseClicked(mx, my, button)) {
            return;
        }
        for (final Hit hit : hits) {
            if (mx >= hit.x() && mx < hit.x() + hit.w() && my >= hit.y() && my < hit.y() + hit.h()) {
                hit.action().run();
                return;
            }
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        listScroll = Math.max(0, listScroll + (delta > 0 ? -1 : 1));
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        // Enter at the prompt applies the statement, through the field's own commit.
        return root.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    /** Applies the statement written at the prompt. */
    public void apply() {
        if (!statement.edit().isBlank()) {
            ask(ProphetActionPayload.APPLY, statement.edit().strip());
        }
    }

    /** Checks the statement written at the prompt, without applying it. */
    public void check() {
        ask(ProphetActionPayload.CHECK, statement.edit().strip());
    }

    /** Lets the state of the item {@code item} go. */
    public void forget(final String item) {
        ask(ProphetActionPayload.FORGET, item);
    }

    /** Lets watch number {@code number} go. */
    public void forgetWatch(final int number) {
        ask(ProphetActionPayload.FORGET, "WATCH " + number);
    }

    /** Sets how it looks and reacts. */
    public void configure(final int interval, final long batch, final boolean reacting) {
        ask(ProphetActionPayload.SETTINGS, interval + SETTINGS_SEPARATOR + batch + SETTINGS_SEPARATOR + reacting);
    }

    /** Writes {@code text} at the prompt, as typing it does. */
    public void typeStatement(final String text) {
        statement.set(text);
    }

    /** Brings tab {@code index} to the front, as a click on it does. */
    public void selectTab(final int index) {
        tabs.setSelected(index);
        listScroll = 0;
    }

    /** The tab in front. */
    public int tab() {
        return tabs.selected();
    }

    /** The middle of tab {@code index}, in desktop coordinates, for a test to click. */
    public int[] tabCenter(final int index) {
        return tabs.tabCenter(index);
    }

    /** The middle of the Apply button, or of Check when {@code checking}. */
    public int[] buttonCenter(final boolean checking) {
        return (checking ? checkButton : applyButton).center();
    }

    /** Picks the state of the item {@code item}, as a click on its row does. */
    public void select(final String item) {
        selected = item;
    }

    /** Whether a request is on its way and not answered yet. */
    public boolean asking() {
        return asking;
    }

    /** Each state as the list shows it: as written, where it stands, and what is on its way. */
    public List<String> stateLines() {
        final List<String> out = new ArrayList<>();
        if (data != null) {
            data.states().forEach(row -> out.add(row.written() + " | " + GameText.resolve(row.status()) + " | "
                    + row.held() + " | " + row.inFlight()));
        }
        return out;
    }

    /** Each watch as the Subscriptions tab shows it. */
    public List<String> watchLines() {
        final List<String> out = new ArrayList<>();
        if (data != null) {
            data.watches().forEach(row -> out.add(row.written() + (row.fired() ? " | fired" : " | armed")));
        }
        return out;
    }

    /** What the engine did lately, as the Reactions tab shows it. */
    public List<String> reactionLines() {
        final List<String> out = new ArrayList<>();
        if (data != null) {
            data.reactions().forEach(row -> out.add(GameText.resolve(row.what())));
        }
        return out;
    }

    /** How many levels the selected state's graph draws. */
    public int graphPoints() {
        final ProphetEngine.StateRow row = selectedRow();
        return row == null ? 0 : row.samples().size();
    }

    /** The settings as the console last heard them. */
    @Nullable
    public ProphetEngine.Settings settings() {
        return data == null ? null : data.settings();
    }

    /** What the status bar says at its left. */
    public String statusText() {
        return GameText.resolve(statusLine());
    }

    private void take(final ProphetConsolePayload payload) {
        asking = false;
        data = payload;
        if (!payload.message().isEmpty() || !payload.ok()) {
            status = payload.message();
        }
        if (selectedRow() == null && !payload.states().isEmpty()) {
            selected = payload.states().get(0).item();
        }
    }

    private void refresh() {
        send(ProphetActionPayload.REFRESH, "");
    }

    private void ask(final int action, final String arg) {
        asking = true;
        status = Text.EMPTY;
        send(action, arg);
    }

    private void send(final int action, final String arg) {
        PacketDistributor.sendToServer(new ProphetActionPayload(host, window, action, arg));
    }

    @Nullable
    private ProphetEngine.StateRow selectedRow() {
        if (data == null) {
            return null;
        }
        for (final ProphetEngine.StateRow row : data.states()) {
            if (row.item().equals(selected)) {
                return row;
            }
        }
        return null;
    }

    private Text statusLine() {
        if (asking) {
            return ProphetConsoleTexts.ASKING.text();
        }
        return status;
    }

    // Drawing.

    private void drawStates(final GuiGraphics g, final Font font, final ProphetConsolePalette.Colours c,
                            final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, skin.fieldBg());
        Draw.outline(g, x, y, w, h, skin.edge());
        final int[] xs = ProphetConsoleLayout.columns(x + 3, w - 6);
        columns(g, font, x, y, w, List.of(ProphetConsoleTexts.DESIRED_STATE, ProphetConsoleTexts.STATE,
                ProphetConsoleTexts.NOW, ProphetConsoleTexts.WORK, ProphetConsoleTexts.LAST_REACTION), xs);
        if (data == null) {
            return;
        }
        if (data.states().isEmpty()) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(ProphetConsoleTexts.NO_STATES), w - 8), x + 4,
                    y + ProphetConsoleLayout.COLUMNS_H + 3, skin.dim(), skin.fieldBg());
            return;
        }
        final int fit = Math.max(1, (h - ProphetConsoleLayout.COLUMNS_H) / ProphetConsoleLayout.ROW_H);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, data.states().size() - fit)));
        int ry = y + ProphetConsoleLayout.COLUMNS_H + 1;
        for (int i = listScroll; i < data.states().size() && i - listScroll < fit; i++) {
            final ProphetEngine.StateRow row = data.states().get(i);
            final boolean lit = row.item().equals(selected);
            final int ground = lit ? skin.listSelect() : skin.fieldBg();
            if (lit) {
                g.fill(x + 1, ry - 1, x + w - 1, ry + ProphetConsoleLayout.ROW_H - 1, ground);
            }
            Draw.text(g, font, Texts.clip(font, row.written(), xs[1] - xs[0] - 4), xs[0], ry + 1, skin.text(),
                    ground);
            final int chip = toneColour(c, row.tone());
            Draw.outline(g, xs[1], ry, xs[2] - xs[1] - 4, ProphetConsoleLayout.ROW_H - 2, chip);
            Draw.text(g, font, Texts.clip(font, GameText.resolve(row.status()), (int) ((xs[2] - xs[1] - 8) / SMALL)),
                    xs[1] + 2, ry + 2, chip, ground, SMALL);
            levelBar(g, font, c, row, xs[2], ry + 2, xs[3] - xs[2] - 4, ground);
            final String work = row.inFlight() > 0
                    ? GameText.resolve(ProphetConsoleTexts.IN_FLIGHT.with(row.inFlight())) : NOTHING;
            Draw.text(g, font, Texts.clip(font, work, (int) ((xs[4] - xs[3] - 4) / SMALL)), xs[3], ry + 2,
                    skin.text(), ground, SMALL);
            Draw.text(g, font, Texts.clip(font, GameText.resolve(row.last()), (int) ((x + w - xs[4] - 4) / SMALL)),
                    xs[4], ry + 2, skin.dim(), ground, SMALL);
            final String item = row.item();
            hits.add(new Hit(x + 1, ry - 1, w - 2, ProphetConsoleLayout.ROW_H, () -> selected = item));
            ry += ProphetConsoleLayout.ROW_H;
        }
    }

    /* A level against its band: the track, the band shaded on it, and a mark where the level stands. */
    private void levelBar(final GuiGraphics g, final Font font, final ProphetConsolePalette.Colours c,
                          final ProphetEngine.StateRow row, final int x, final int y, final int w, final int ground) {
        final int barW = Math.max(10, w - font.width("0000000"));
        final long top = ProphetConsoleLayout.graphTop(row.lower(), row.upper(), row.held() + row.inFlight());
        g.fill(x, y + 2, x + barW, y + 6, c.track());
        final int bandFrom = x + ProphetConsoleLayout.scaled(row.lower(), top, barW);
        final int bandTo = x + ProphetConsoleLayout.scaled(row.upper(), top, barW);
        g.fill(bandFrom, y + 2, Math.max(bandFrom + 1, bandTo), y + 6, c.band());
        g.fill(bandFrom, y + 1, bandFrom + 1, y + 7, c.bandEdge());
        final int mark = x + ProphetConsoleLayout.scaled(row.held(), top, barW);
        g.fill(mark, y, mark + 1, y + 8, c.marker());
        Draw.text(g, font, Long.toString(row.held()), x + barW + 3, y, skin.text(), ground, SMALL);
    }

    private void drawDetail(final GuiGraphics g, final Font font, final ProphetConsolePalette.Colours c,
                            final int x, final int y, final int w) {
        final ProphetEngine.StateRow row = selectedRow();
        final int left = x + ProphetConsoleLayout.PAD;
        if (row == null) {
            return;
        }
        Draw.text(g, font, Texts.clip(font, row.written(), w - 8), left, y + 1, skin.text(), skin.windowBg());
        final int gw = ProphetConsoleLayout.graphW(w);
        final int gy = y + 12;
        final int gh = ProphetConsoleLayout.DETAIL_H - 14;
        drawGraph(g, font, c, row, left, gy, gw, gh);
        final int ox = left + gw + ProphetConsoleLayout.PAD;
        final int ow = w - 3 * ProphetConsoleLayout.PAD - gw;
        g.fill(ox, gy, ox + ow, gy + gh, skin.fieldBg());
        Draw.outline(g, ox, gy, ow, gh, skin.edge());
        int ly = gy + 3;
        if (row.operations().isEmpty()) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(ProphetConsoleTexts.NO_OPERATIONS),
                    (int) ((ow - 6) / SMALL)), ox + 3, ly, skin.dim(), skin.fieldBg(), SMALL);
            ly += 9;
        }
        for (final Text line : row.operations()) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(line), (int) ((ow - 6) / SMALL)), ox + 3, ly,
                    skin.text(), skin.fieldBg(), SMALL);
            ly += 9;
        }
        if (row.inFlight() > 0 && ly + 8 < gy + gh) {
            final String counting = GameText.resolve(ProphetConsoleTexts.COUNTING.with(row.inFlight(), row.held(),
                    row.inFlight(), row.held() + row.inFlight()));
            Draw.text(g, font, Texts.clip(font, counting, (int) ((ow - 6) / SMALL)), ox + 3, ly, skin.dim(),
                    skin.fieldBg(), SMALL);
        }
    }

    /* The band, the level as it went, and the dashed line to where the work on its way takes it. */
    private void drawGraph(final GuiGraphics g, final Font font, final ProphetConsolePalette.Colours c,
                           final ProphetEngine.StateRow row, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, c.graph());
        Draw.outline(g, x, y, w, h, c.graphDim());
        final List<ProphetStates.Sample> samples = row.samples();
        long highest = row.held() + row.inFlight();
        for (final ProphetStates.Sample sample : samples) {
            highest = Math.max(highest, sample.held());
        }
        final long top = ProphetConsoleLayout.graphTop(row.lower(), row.upper(), highest);
        final int inner = h - 2;
        final int bandTop = y + 1 + ProphetConsoleLayout.levelY(Math.min(row.upper(), top), top, inner);
        final int bandBottom = y + 1 + ProphetConsoleLayout.levelY(row.lower(), top, inner);
        g.fill(x + 1, bandTop, x + w - 1, Math.max(bandTop + 1, bandBottom), c.band());
        g.fill(x + 1, bandBottom, x + w - 1, bandBottom + 1, c.bandEdge());
        Draw.text(g, font, Long.toString(row.lower()), x + 2, Math.max(y + 1, bandBottom - 7), c.graphDim(),
                c.graph(), SMALL);
        if (row.upper() != Long.MAX_VALUE && row.upper() < top) {
            g.fill(x + 1, bandTop, x + w - 1, bandTop + 1, c.bandEdge());
            Draw.text(g, font, Long.toString(row.upper()), x + 2, bandTop + 1, c.graphDim(), c.graph(), SMALL);
        }
        if (samples.isEmpty() || data == null) {
            return;
        }
        final long from = samples.get(0).at();
        final long span = Math.max(1L, data.now() - from);
        int px = -1;
        int py = -1;
        for (final ProphetStates.Sample sample : samples) {
            final int sx = x + 1 + (int) ((sample.at() - from) * (w - 3) / span);
            final int sy = y + 1 + ProphetConsoleLayout.levelY(sample.held(), top, inner);
            if (px >= 0) {
                line(g, px, py, sx, sy, c.level(), false);
            }
            px = sx;
            py = sy;
        }
        final int nowX = x + w - 2;
        final int nowY = y + 1 + ProphetConsoleLayout.levelY(row.held(), top, inner);
        line(g, px, py, nowX, nowY, c.level(), false);
        if (row.inFlight() > 0) {
            final int projectedY = y + 1 + ProphetConsoleLayout.levelY(row.held() + row.inFlight(), top, inner);
            line(g, nowX - 12, nowY, nowX, projectedY, c.level(), true);
            Draw.text(g, font, GameText.resolve(ProphetConsoleTexts.WITH_WORK), x + w / 2,
                    Math.max(y + 1, projectedY - 8), c.graphDim(), c.graph(), SMALL);
        }
    }

    /* A line of single pixels between two points, every other run of three left out when {@code dashed}. */
    private static void line(final GuiGraphics g, final int x0, final int y0, final int x1, final int y1,
                             final int colour, final boolean dashed) {
        final int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        for (int i = 0; i <= steps; i++) {
            if (dashed && (i / 3) % 2 == 1) {
                continue;
            }
            final int px = steps == 0 ? x0 : x0 + (x1 - x0) * i / steps;
            final int py = steps == 0 ? y0 : y0 + (y1 - y0) * i / steps;
            g.fill(px, py, px + 1, py + 1, colour);
        }
    }

    private void drawWatches(final GuiGraphics g, final Font font, final ProphetConsolePalette.Colours c,
                             final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, skin.fieldBg());
        Draw.outline(g, x, y, w, h, skin.edge());
        if (data == null) {
            return;
        }
        if (data.watches().isEmpty()) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(ProphetConsoleTexts.NO_WATCHES), w - 8), x + 4,
                    y + 4, skin.dim(), skin.fieldBg());
            return;
        }
        int ry = y + 3;
        for (int i = listScroll; i < data.watches().size() && ry + ProphetConsoleLayout.ROW_H <= y + h; i++) {
            final ProphetEngine.WatchRow row = data.watches().get(i);
            final String forget = GameText.resolve(ProphetConsoleTexts.FORGET);
            final int forgetW = font.width(forget) + 6;
            final String times = GameText.resolve(ProphetConsoleTexts.FIRED_TIMES.with(row.times()));
            final int timesW = (int) (font.width(times) * SMALL) + 6;
            final String number = "#" + row.number();
            Draw.text(g, font, number, x + 4, ry + 1, skin.dim(), skin.fieldBg());
            final int textX = x + 8 + font.width(number);
            Draw.text(g, font, Texts.clip(font, row.written(), w - (textX - x) - forgetW - timesW - 14), textX,
                    ry + 1, skin.text(), skin.fieldBg());
            Draw.text(g, font, times, x + w - forgetW - timesW - 4, ry + 2, row.fired() ? c.working() : c.waiting(),
                    skin.fieldBg(), SMALL);
            final int fx = x + w - forgetW - 3;
            Draw.outline(g, fx, ry, forgetW, ProphetConsoleLayout.ROW_H - 2, skin.edge());
            Draw.text(g, font, forget, fx + 3, ry + 1, skin.text(), skin.fieldBg());
            final int watchNumber = row.number();
            hits.add(new Hit(fx, ry, forgetW, ProphetConsoleLayout.ROW_H - 2, () -> forgetWatch(watchNumber)));
            ry += ProphetConsoleLayout.ROW_H;
        }
    }

    private void drawReactions(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                               final int h) {
        g.fill(x, y, x + w, y + h, skin.fieldBg());
        Draw.outline(g, x, y, w, h, skin.edge());
        if (data == null) {
            return;
        }
        if (data.reactions().isEmpty()) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(ProphetConsoleTexts.NO_REACTIONS), w - 8), x + 4,
                    y + 4, skin.dim(), skin.fieldBg());
            return;
        }
        final int agoW = 52;
        int ry = y + 3;
        for (int i = listScroll; i < data.reactions().size() && ry + 10 <= y + h; i++) {
            final ProphetEngine.ReactionRow row = data.reactions().get(i);
            Draw.text(g, font, GameText.resolve(ago(data.now() - row.at())), x + 4, ry, skin.dim(), skin.fieldBg());
            Draw.text(g, font, Texts.clip(font, GameText.resolve(row.what()), w - agoW - 8), x + agoW, ry,
                    skin.text(), skin.fieldBg());
            ry += 10;
        }
    }

    private void drawSettings(final GuiGraphics g, final Font font, final ProphetConsolePalette.Colours c,
                              final int x, final int y, final int w) {
        if (data == null) {
            return;
        }
        final ProphetEngine.Settings now = data.settings();
        int ry = y + 4;
        Draw.text(g, font, GameText.resolve(ProphetConsoleTexts.LOOK_EVERY), x + 2, ry, skin.text(),
                skin.windowBg());
        ry += 11;
        int cx = x + 2;
        for (final int ticks : List.of(20, 40, 100, 200)) {
            final String word = GameText.resolve(ProphetConsoleTexts.SECONDS.with(ticks / TICKS_PER_SECOND));
            cx = choice(g, font, c, cx, ry, word, ticks == now.interval(),
                    () -> configure(ticks, now.maxBatch(), now.reacting()));
        }
        ry += 18;
        Draw.text(g, font, GameText.resolve(ProphetConsoleTexts.AT_MOST), x + 2, ry, skin.text(), skin.windowBg());
        ry += 11;
        cx = x + 2;
        for (final long batch : List.of(64L, 256L, 1024L, 4096L)) {
            cx = choice(g, font, c, cx, ry, Long.toString(batch), batch == now.maxBatch(),
                    () -> configure(now.interval(), batch, now.reacting()));
        }
        ry += 18;
        Draw.text(g, font, GameText.resolve(ProphetConsoleTexts.REACT), x + 2, ry, skin.text(), skin.windowBg());
        ry += 11;
        cx = choice(g, font, c, x + 2, ry, GameText.resolve(ProphetConsoleTexts.REACTING), now.reacting(),
                () -> configure(now.interval(), now.maxBatch(), true));
        choice(g, font, c, cx, ry, GameText.resolve(ProphetConsoleTexts.PAUSED), !now.reacting(),
                () -> configure(now.interval(), now.maxBatch(), false));
    }

    /* One option of a setting: drawn picked or not, clickable; answers where the next one goes. */
    private int choice(final GuiGraphics g, final Font font, final ProphetConsolePalette.Colours c, final int x,
                       final int y, final String word, final boolean picked, final Runnable action) {
        final int w = font.width(word) + 10;
        final int ground = picked ? skin.listSelect() : skin.fieldBg();
        g.fill(x, y, x + w, y + 12, ground);
        Draw.outline(g, x, y, w, 12, picked ? c.working() : skin.edge());
        Draw.text(g, font, word, x + 5, y + 2, skin.text(), ground);
        hits.add(new Hit(x, y, w, 12, action));
        return x + w + 3;
    }

    private void columns(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                         final List<TextKey> names, final int[] xs) {
        g.fill(x + 1, y + 1, x + w - 1, y + ProphetConsoleLayout.COLUMNS_H, skin.panelBg());
        g.fill(x + 1, y + ProphetConsoleLayout.COLUMNS_H - 1, x + w - 1, y + ProphetConsoleLayout.COLUMNS_H,
                skin.edge());
        for (int i = 0; i < names.size(); i++) {
            final int right = i + 1 < xs.length ? xs[i + 1] : x + w;
            Draw.text(g, font, Texts.clip(font, GameText.resolve(names.get(i)), (int) ((right - xs[i] - 4) / SMALL)),
                    xs[i], y + 3, skin.dim(), skin.panelBg(), SMALL);
        }
    }

    private void drawStatus(final GuiGraphics g, final Font font, final ProphetConsolePalette.Colours c,
                            final int x, final int y, final int w) {
        g.fill(x, y, x + w, y + ProphetConsoleLayout.STATUS_H, c.status());
        final List<String> parts = new ArrayList<>();
        if (data != null) {
            parts.add(GameText.resolve(data.engine()));
            parts.add(GameText.resolve(data.network()));
            parts.add(GameText.resolve(ProphetConsoleTexts.STATES_COUNT.with(data.states().size())));
            parts.add(GameText.resolve(data.settings().reacting() ? ProphetConsoleTexts.REACTING_STATUS
                    : ProphetConsoleTexts.PAUSED_STATUS));
        }
        final String left = String.join(SEPARATOR, parts.stream().filter(s -> !s.isEmpty()).toList());
        Draw.text(g, font, Texts.clip(font, left, w / 2 + 40), x + 4, y + 2, c.statusText(), c.status());
        final String said = statusText();
        if (!said.isEmpty()) {
            final String clipped = Texts.clip(font, said, w / 2 - 50);
            Draw.text(g, font, clipped, x + w - 4 - font.width(clipped), y + 2, c.statusText(), c.status());
        }
    }

    private static int toneColour(final ProphetConsolePalette.Colours c, final byte tone) {
        return switch (tone) {
            case ProphetEngine.StateRow.TONE_GOOD -> c.holding();
            case ProphetEngine.StateRow.TONE_WORKING -> c.working();
            case ProphetEngine.StateRow.TONE_BAD -> c.trouble();
            default -> c.waiting();
        };
    }

    private static Text ago(final long ticks) {
        final long seconds = Math.max(0L, ticks) / TICKS_PER_SECOND;
        return seconds < 120 ? ProphetConsoleTexts.SECONDS_AGO.with(seconds)
                : ProphetConsoleTexts.MINUTES_AGO.with(seconds / 60);
    }

    /** Somewhere on the window that does something when clicked, as laid out this frame. */
    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
