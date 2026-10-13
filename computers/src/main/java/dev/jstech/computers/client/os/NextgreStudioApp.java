/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.engine.nextgre.NextgreEngine;
import dev.jstech.computers.engine.nextgre.NextgrePlanView;
import dev.jstech.computers.gui.layout.NextgreStudioLayout;
import dev.jstech.computers.operation.payload.NextgreActionPayload;
import dev.jstech.computers.operation.payload.NextgreStudioPayload;
import dev.jstech.computers.os.edit.CodeRuns;
import dev.jstech.computers.os.edit.InkPalette;
import dev.jstech.computers.program.iql.IqlColouring;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Nextgre Planner Studio, NextgreIQL's own tool. On the Explain tab a statement's plan is shown as NextgreIQL
 * built it: each step a box, with the time reckoned for it against the time it took once it ran, the hints marked on
 * the boxes they changed, and beside the tree the plans the engine weighed with what each cost and why any was set
 * aside. Explain plans a statement; Explain Analyze runs it as well, and the boxes fill in as the craft goes.
 *
 * <p>The Statistics tab lists what the planner reckons with (the network it gathered, the times it measured, what
 * other mods measure); the Rules tab its rules, each switched on or off for this Mainframe, and the hints its dialect
 * takes; the History tab the plans it made lately, any of which opens on the Explain tab.
 *
 * <p>Drawn in the system's own look, with the house's blue on the boxes, the bars and the status bar.
 */
public final class NextgreStudioApp implements IDesktopApp {

    private final BlockPos host;
    private final BlockPos monitor;
    private final int window;
    private final Panel root = new Panel();
    private final TabStrip tabs;
    private final CodeArea editor = new CodeArea();
    private final Button explainButton;
    private final Button analyzeButton;
    private final Button gatherButton;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private NextgreStudioPayload data;
    @Nullable
    private NextgrePlanView plan;
    private List<int[]> boxes = List.of();
    private Text status = Text.EMPTY;
    private boolean asking;
    private boolean askedOnce;
    private int frame;
    private int scrollX;
    private int scrollY;
    private int listScroll;
    private int originX;
    private int originY;
    private int width = NextgreStudioLayout.DEFAULT_W;
    private int height = NextgreStudioLayout.DEFAULT_H;
    private double dragX;
    private double dragY;
    private boolean dragging;
    private final List<Hit> hits = new ArrayList<>();

    public static final int TAB_EXPLAIN = 0;
    public static final int TAB_STATISTICS = 1;
    public static final int TAB_RULES = 2;
    public static final int TAB_HISTORY = 3;

    private static final int REFRESH_FRAMES = 200;
    private static final int RUNNING_REFRESH_FRAMES = 30;
    private static final double TICKS_PER_SECOND = 20.0;
    private static final float SMALL = 0.75f;
    private static final String NO_TIME = "-";
    private static final String SEPARATOR = "   ";

    /** Every studio window open on this client, which answers are routed among by window number. */
    private static final List<NextgreStudioApp> OPEN = new ArrayList<>();
    private static int windows;

    public NextgreStudioApp(final BlockPos host, final BlockPos monitor) {
        this.host = host;
        this.monitor = monitor;
        this.window = ++windows;
        tabs = root.add(new TabStrip(List.of(GameText.resolve(NextgreStudioTexts.EXPLAIN_TAB),
                GameText.resolve(NextgreStudioTexts.STATISTICS_TAB), GameText.resolve(NextgreStudioTexts.RULES_TAB),
                GameText.resolve(NextgreStudioTexts.HISTORY_TAB))).setUnderline(true).fitToLabels(10)
                .setOnSelect(index -> listScroll = 0));
        editor.setPalette(InkPalette.LIGHT).setColouring(lines -> CodeRuns.byLine(lines, IqlColouring.spans(lines)));
        editor.setActive(true);
        root.add(editor);
        explainButton = root.add(new Button(GameText.resolve(NextgreStudioTexts.EXPLAIN), this::explain));
        analyzeButton = root.add(new Button(GameText.resolve(NextgreStudioTexts.EXPLAIN_ANALYZE), this::analyze)
                .setPrimary(true));
        gatherButton = root.add(new Button(GameText.resolve(NextgreStudioTexts.GATHER), this::gather));
        OPEN.add(this);
    }

    /** What the network's NextgreIQL says, for the window that asked. */
    public static void accept(final NextgreStudioPayload payload) {
        for (final NextgreStudioApp app : OPEN) {
            if (app.window == payload.window()) {
                app.take(payload);
            }
        }
    }

    /** The studio window opened last, for a test to drive. */
    @Nullable
    public static NextgreStudioApp latest() {
        return OPEN.isEmpty() ? null : OPEN.get(OPEN.size() - 1);
    }

    @Override
    public String title() {
        return GameText.resolve(NextgreStudioTexts.TITLE);
    }

    @Override
    public int defaultWidth() {
        return NextgreStudioLayout.DEFAULT_W;
    }

    @Override
    public int defaultHeight() {
        return NextgreStudioLayout.DEFAULT_H;
    }

    @Override
    public int minWidth() {
        return NextgreStudioLayout.MIN_W;
    }

    @Override
    public int minHeight() {
        return NextgreStudioLayout.MIN_H;
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
    public String saveState() {
        return editor.text();
    }

    @Override
    public void restoreState(final String state) {
        editor.setText(state == null ? "" : state);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        originX = x;
        originY = y;
        width = w;
        height = h;
        if (!askedOnce) {
            askedOnce = true;
            refresh();
        }
        frame++;
        final boolean running = plan != null && plan.state() == NextgrePlanView.RUNNING;
        if (frame % (running ? RUNNING_REFRESH_FRAMES : REFRESH_FRAMES) == 0) {
            refresh();
        }
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        final NextgreStudioPalette.Colours c = NextgreStudioPalette.colours();
        g.fill(x, y, x + w, y + h, skin.windowBg());
        hits.clear();
        tabs.setBounds(x + NextgreStudioLayout.PAD, y + NextgreStudioLayout.TABS_Y, w - 2 * NextgreStudioLayout.PAD,
                NextgreStudioLayout.TABS_H);
        final boolean explaining = tabs.selected() == TAB_EXPLAIN;
        editor.setVisible(explaining);
        explainButton.setVisible(explaining);
        analyzeButton.setVisible(explaining);
        gatherButton.setVisible(tabs.selected() == TAB_STATISTICS);
        if (explaining) {
            editor.setBounds(x + NextgreStudioLayout.PAD, y + NextgreStudioLayout.QUERY_Y,
                    NextgreStudioLayout.queryW(w), NextgreStudioLayout.QUERY_H);
            final int bx = x + NextgreStudioLayout.buttonsX(w);
            explainButton.setBounds(bx, y + NextgreStudioLayout.QUERY_Y, NextgreStudioLayout.BUTTON_W,
                    NextgreStudioLayout.BUTTON_H);
            analyzeButton.setBounds(bx, y + NextgreStudioLayout.QUERY_Y + NextgreStudioLayout.BUTTON_H
                    + NextgreStudioLayout.BUTTON_GAP, NextgreStudioLayout.BUTTON_W, NextgreStudioLayout.BUTTON_H);
            drawTree(g, font, c, x + NextgreStudioLayout.PAD, y + NextgreStudioLayout.BODY_Y,
                    NextgreStudioLayout.treeW(w), NextgreStudioLayout.bodyH(h));
            drawSide(g, font, c, x + NextgreStudioLayout.sideX(w), y + NextgreStudioLayout.BODY_Y,
                    NextgreStudioLayout.SIDE_W, NextgreStudioLayout.bodyH(h));
        } else {
            drawList(g, font, c, x + NextgreStudioLayout.PAD, y + NextgreStudioLayout.LIST_Y,
                    w - 2 * NextgreStudioLayout.PAD, NextgreStudioLayout.listH(h) + NextgreStudioLayout.COLUMNS_H);
        }
        root.render(g, ctx);
        drawStatus(g, font, c, x, y + NextgreStudioLayout.statusY(h), w);
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
        if (tabs.selected() == TAB_EXPLAIN && inTree(mx, my)) {
            dragging = true;
            dragX = mx;
            dragY = my;
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow win, final double mx, final double my, final int button) {
        if (dragging) {
            scrollX = clampX(scrollX - (int) (mx - dragX));
            scrollY = clampY(scrollY - (int) (my - dragY));
            dragX = mx;
            dragY = my;
            return;
        }
        root.mouseDragged(mx, my, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow win, final double mx, final double my, final int button) {
        dragging = false;
        root.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final int step = delta > 0 ? -NextgreStudioLayout.ROW_H * 2 : NextgreStudioLayout.ROW_H * 2;
        if (tabs.selected() == TAB_EXPLAIN) {
            if (Screen.hasShiftDown()) {
                scrollX = clampX(scrollX + step);
            } else {
                scrollY = clampY(scrollY + step);
            }
        } else {
            listScroll = Math.max(0, listScroll + (delta > 0 ? -1 : 1));
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_F7) {
            if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
                analyze();
            } else {
                explain();
            }
            return true;
        }
        return tabs.selected() == TAB_EXPLAIN && editor.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c) {
        return tabs.selected() == TAB_EXPLAIN && editor.charTyped(c);
    }

    /** Plans the statement written, without running it. */
    public void explain() {
        ask(NextgreActionPayload.EXPLAIN, editor.text());
    }

    /** Plans the statement written and runs it, to see what each step takes. */
    public void analyze() {
        ask(NextgreActionPayload.EXPLAIN_ANALYZE, editor.text());
    }

    /** Has the statistics gathered afresh. */
    public void gather() {
        ask(NextgreActionPayload.ANALYZE, "");
    }

    /** Switches the rule {@code id} the other way. */
    public void toggleRule(final String id) {
        ask(NextgreActionPayload.TOGGLE_RULE, id);
    }

    /** Opens the plan numbered {@code id} on the Explain tab. */
    public void open(final int id) {
        tabs.setSelected(TAB_EXPLAIN);
        ask(NextgreActionPayload.OPEN, Integer.toString(id));
    }

    /** Writes {@code statement} into the statement's box, as typing it does. */
    public void typeStatement(final String statement) {
        editor.setText(statement);
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

    /** The middle of tab {@code index}, in screen coordinates, for a test to click. */
    public int[] tabCenter(final int index) {
        return tabs.tabCenter(index);
    }

    /** The middle of the Explain button, or of Explain Analyze when {@code analyzing}. */
    public int[] buttonCenter(final boolean analyzing) {
        return (analyzing ? analyzeButton : explainButton).center();
    }

    /** Whether a request is on its way and not answered yet. */
    public boolean asking() {
        return asking;
    }

    /** The titles of the boxes of the plan shown, top down. */
    public List<String> nodeTitles() {
        final List<String> out = new ArrayList<>();
        if (plan != null) {
            plan.nodes().forEach(node -> out.add(GameText.resolve(node.title())));
        }
        return out;
    }

    /** The hints marked on the boxes of the plan shown, all of them. */
    public List<String> nodeHints() {
        final List<String> out = new ArrayList<>();
        if (plan != null) {
            plan.nodes().forEach(node -> out.addAll(node.hints()));
        }
        return out;
    }

    /** The plans weighed, as the list beside the tree shows them. */
    public List<String> alternativeLines() {
        final List<String> out = new ArrayList<>();
        if (plan != null) {
            for (final NextgrePlanView.Alternative alt : plan.alternatives()) {
                out.add(GameText.resolve(alternativeText(alt)) + " " + GameText.resolve(alt.description()));
            }
        }
        return out;
    }

    /** Where the plan shown stands, one of {@link NextgrePlanView}'s states, or -1 when none is shown. */
    public int planState() {
        return plan == null ? -1 : plan.state();
    }

    /** Whether every box of the plan shown that runs has the time it took. */
    public boolean planMeasured() {
        if (plan == null) {
            return false;
        }
        for (final NextgrePlanView.Node node : plan.nodes()) {
            if (node.kind() == NextgrePlanView.ROOT && (node.actual() < 0 || !node.finished())) {
                return false;
            }
        }
        return true;
    }

    /** The lines of the Statistics tab, each name and value. */
    public List<String> statisticLines() {
        final List<String> out = new ArrayList<>();
        if (data != null) {
            data.statistics().forEach(row -> out.add(GameText.resolve(row.name()) + SEPARATOR
                    + GameText.resolve(row.value())));
        }
        return out;
    }

    /** The rules and hints of the Rules tab, each with whether it is on. */
    public List<String> ruleLines() {
        final List<String> out = new ArrayList<>();
        if (data != null) {
            data.rules().forEach(rule -> out.add(rule.id() + (rule.on() ? "=on" : "=off")));
        }
        return out;
    }

    /** The statements of the History tab, the newest first. */
    public List<String> historyLines() {
        final List<String> out = new ArrayList<>();
        if (data != null) {
            data.history().forEach(row -> out.add(row.statement()));
        }
        return out;
    }

    /** What the status bar says at its left. */
    public String statusText() {
        return GameText.resolve(statusLine());
    }

    private void take(final NextgreStudioPayload payload) {
        asking = false;
        data = payload;
        if (payload.plan().isPresent()) {
            final NextgrePlanView shown = payload.plan().get();
            final boolean fresh = plan == null || plan.id() != shown.id();
            plan = shown;
            final List<Integer> parents = new ArrayList<>();
            shown.nodes().forEach(node -> parents.add(node.parent()));
            boxes = NextgreStudioLayout.tree(parents);
            if (fresh) {
                scrollX = 0;
                scrollY = 0;
            }
        }
        if (!payload.message().isEmpty() || !payload.ok()) {
            status = payload.message();
        }
    }

    private void refresh() {
        send(NextgreActionPayload.REFRESH, plan == null ? "" : Integer.toString(plan.id()));
    }

    private void ask(final int action, final String arg) {
        asking = true;
        status = Text.EMPTY;
        send(action, arg);
    }

    private void send(final int action, final String arg) {
        PacketDistributor.sendToServer(new NextgreActionPayload(host, window, action, arg));
    }

    private Text statusLine() {
        if (asking) {
            return NextgreStudioTexts.ASKING.text();
        }
        if (!status.isEmpty()) {
            return status;
        }
        return plan == null ? NextgreStudioTexts.NO_PLAN.text() : plan.outcome();
    }

    private boolean inTree(final double mx, final double my) {
        final int tx = originX + NextgreStudioLayout.PAD;
        final int ty = originY + NextgreStudioLayout.BODY_Y;
        return mx >= tx && mx < tx + NextgreStudioLayout.treeW(width) && my >= ty
                && my < ty + NextgreStudioLayout.bodyH(height);
    }

    private int clampX(final int value) {
        final int[] size = NextgreStudioLayout.treeSize(boxes);
        return Math.max(0, Math.min(value, Math.max(0, size[0] - NextgreStudioLayout.treeW(width))));
    }

    private int clampY(final int value) {
        final int[] size = NextgreStudioLayout.treeSize(boxes);
        return Math.max(0, Math.min(value, Math.max(0, size[1] - NextgreStudioLayout.bodyH(height))));
    }

    // Drawing.

    private void drawTree(final GuiGraphics g, final Font font, final NextgreStudioPalette.Colours c, final int x,
                          final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, skin.fieldBg());
        Draw.outline(g, x, y, w, h, skin.edge());
        if (plan == null || plan.nodes().isEmpty()) {
            final Text said = plan == null ? NextgreStudioTexts.NO_PLAN.text() : plan.outcome();
            int ly = y + 6;
            for (final String line : wrap(font, GameText.resolve(said), w - 12)) {
                Draw.text(g, font, line, x + 6, ly, skin.dim(), skin.fieldBg());
                ly += 10;
            }
            return;
        }
        long most = 1L;
        for (final NextgrePlanView.Node node : plan.nodes()) {
            most = Math.max(most, Math.max(node.estimate(), node.actual()));
        }
        Draw.pushScissor(g, x + 1, y + 1, x + w - 1, y + h - 1);
        final int ox = x - scrollX;
        final int oy = y - scrollY;
        for (int i = 0; i < plan.nodes().size() && i < boxes.size(); i++) {
            final int parent = plan.nodes().get(i).parent();
            if (parent >= 0 && parent < boxes.size()) {
                final int[] from = boxes.get(parent);
                final int[] to = boxes.get(i);
                final int fx = ox + from[0] + NextgreStudioLayout.BOX_W / 2;
                final int fy = oy + from[1] + NextgreStudioLayout.BOX_H;
                final int tx = ox + to[0] + NextgreStudioLayout.BOX_W / 2;
                final int ty = oy + to[1];
                final int midY = fy + NextgreStudioLayout.LEVEL_GAP / 2;
                g.fill(fx, fy, fx + 1, midY + 1, c.line());
                g.fill(Math.min(fx, tx), midY, Math.max(fx, tx) + 1, midY + 1, c.line());
                g.fill(tx, midY, tx + 1, ty, c.line());
            }
        }
        for (int i = 0; i < plan.nodes().size() && i < boxes.size(); i++) {
            drawBox(g, font, c, plan.nodes().get(i), ox + boxes.get(i)[0], oy + boxes.get(i)[1], most);
        }
        Draw.popScissor(g);
    }

    private void drawBox(final GuiGraphics g, final Font font, final NextgreStudioPalette.Colours c,
                         final NextgrePlanView.Node node, final int x, final int y, final long most) {
        final int w = NextgreStudioLayout.BOX_W;
        final int h = NextgreStudioLayout.BOX_H;
        final boolean note = node.kind() == NextgrePlanView.NOTE;
        final int ground = note ? c.note() : c.box();
        g.fill(x, y, x + w, y + h, ground);
        final int edge = node.kind() == NextgrePlanView.MISSING ? c.bad()
                : !node.finished() && node.actual() >= 0 ? c.running() : c.accent();
        Draw.outline(g, x, y, w, h, edge);
        if (node.kind() == NextgrePlanView.ROOT) {
            Draw.outline(g, x + 1, y + 1, w - 2, h - 2, edge);
        }
        final int textColour = note ? c.noteText() : c.boxText();
        Draw.text(g, font, Texts.clip(font, GameText.resolve(node.title()), w - 6), x + 3, y + 3, textColour, ground);
        Draw.text(g, font, Texts.clip(font, GameText.resolve(node.detail()), (int) ((w - 6) / SMALL)), x + 3, y + 13,
                c.boxDim(), ground, SMALL);
        if (note) {
            return;
        }
        final int barW = w - 6;
        g.fill(x + 3, y + 21, x + 3 + barW, y + 25, c.track());
        if (node.estimate() > 0) {
            g.fill(x + 3, y + 21, x + 3 + (int) (barW * Math.min(1.0, node.estimate() / (double) most)), y + 23,
                    c.estimate());
        }
        if (node.actual() > 0) {
            g.fill(x + 3, y + 23, x + 3 + (int) (barW * Math.min(1.0, node.actual() / (double) most)), y + 25,
                    c.actual());
        }
        final String times = GameText.resolve(node.actual() < 0
                ? NextgreStudioTexts.EST_ONLY.with(seconds(node.estimate()))
                : NextgreStudioTexts.EST_ACTUAL.with(seconds(node.estimate()), node.finished()
                        ? seconds(node.actual()) : NextgreStudioTexts.SO_FAR.with(seconds(node.actual()))));
        Draw.text(g, font, Texts.clip(font, times, (int) ((w - 6) / SMALL)), x + 3, y + 27, c.boxDim(), ground,
                SMALL);
        int chipX = x + 3;
        for (final String hint : node.hints()) {
            final int chipW = (int) (font.width(hint) * SMALL) + 4;
            if (chipX + chipW > x + w - 3) {
                break;
            }
            g.fill(chipX, y + 34, chipX + chipW, y + 41, c.chip());
            Draw.text(g, font, hint, chipX + 2, y + 35, c.chipText(), c.chip(), SMALL);
            chipX += chipW + 2;
        }
    }

    private void drawSide(final GuiGraphics g, final Font font, final NextgreStudioPalette.Colours c, final int x,
                          final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, skin.fieldBg());
        Draw.outline(g, x, y, w, h, skin.edge());
        int ly = y + 3;
        Draw.text(g, font, GameText.resolve(NextgreStudioTexts.PLANS_WEIGHED), x + 4, ly, skin.dim(), skin.fieldBg(),
                SMALL);
        ly += 9;
        if (plan != null) {
            for (final NextgrePlanView.Alternative alt : plan.alternatives()) {
                if (ly + 20 > y + h) {
                    break;
                }
                final int ground = alt.chosen() ? c.chosen() : skin.fieldBg();
                if (alt.chosen()) {
                    g.fill(x + 1, ly - 1, x + w - 1, ly + 18, ground);
                }
                final int ink = !alt.setAside().isEmpty() ? skin.dim() : alt.chosen() ? c.boxText() : skin.text();
                Draw.text(g, font, Texts.clip(font, GameText.resolve(alternativeText(alt)), w - 8), x + 4, ly, ink,
                        ground);
                final Text why = alt.setAside().isEmpty() ? alt.description() : alt.setAside();
                Draw.text(g, font, Texts.clip(font, GameText.resolve(why), (int) ((w - 8) / SMALL)), x + 4, ly + 10,
                        alt.setAside().isEmpty() ? (alt.chosen() ? c.boxDim() : skin.dim()) : c.bad(), ground,
                        SMALL);
                ly += 21;
            }
        }
        if (data != null && ly + 20 <= y + h) {
            ly += 3;
            Draw.text(g, font, GameText.resolve(NextgreStudioTexts.STATISTICS_HEAD), x + 4, ly, skin.dim(),
                    skin.fieldBg(), SMALL);
            ly += 9;
            for (final NextgreEngine.StatRow row : data.statistics()) {
                if (ly + 8 > y + h) {
                    break;
                }
                final String line = GameText.resolve(row.name()) + ": " + GameText.resolve(row.value());
                Draw.text(g, font, Texts.clip(font, line, (int) ((w - 8) / SMALL)), x + 4, ly, skin.text(),
                        skin.fieldBg(), SMALL);
                ly += 8;
            }
        }
    }

    private void drawList(final GuiGraphics g, final Font font, final NextgreStudioPalette.Colours c, final int x,
                          final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + h, skin.fieldBg());
        Draw.outline(g, x, y, w, h, skin.edge());
        if (data == null) {
            return;
        }
        switch (tabs.selected()) {
            case TAB_STATISTICS -> drawStatistics(g, font, x, y, w, h);
            case TAB_RULES -> drawRules(g, font, c, x, y, w, h);
            default -> drawHistory(g, font, c, x, y, w, h);
        }
    }

    private void drawStatistics(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                final int h) {
        final int valueX = x + w * 45 / 100;
        columns(g, font, x, y, w, List.of(NextgreStudioTexts.STATISTIC, NextgreStudioTexts.VALUE),
                new int[] {x + 4, valueX});
        gatherButton.setBounds(x + w - NextgreStudioLayout.BUTTON_W - 18 - 2, y + 1,
                NextgreStudioLayout.BUTTON_W + 18, NextgreStudioLayout.COLUMNS_H - 2);
        final List<NextgreEngine.StatRow> rows = data.statistics();
        final int fit = Math.max(1, (h - NextgreStudioLayout.COLUMNS_H) / NextgreStudioLayout.ROW_H);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, rows.size() - fit)));
        int ry = y + NextgreStudioLayout.COLUMNS_H + 1;
        for (int i = listScroll; i < rows.size() && i - listScroll < fit; i++) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(rows.get(i).name()), valueX - x - 8), x + 4, ry,
                    skin.text(), skin.fieldBg());
            Draw.text(g, font, Texts.clip(font, GameText.resolve(rows.get(i).value()), x + w - valueX - 4), valueX,
                    ry, skin.text(), skin.fieldBg());
            ry += NextgreStudioLayout.ROW_H;
        }
    }

    private void drawRules(final GuiGraphics g, final Font font, final NextgreStudioPalette.Colours c, final int x,
                           final int y, final int w, final int h) {
        final List<NextgreEngine.RuleRow> rows = data.rules();
        final List<Object> lines = new ArrayList<>();
        byte kind = -1;
        for (final NextgreEngine.RuleRow rule : rows) {
            if (rule.kind() != kind) {
                kind = rule.kind();
                lines.add(heading(kind));
            }
            lines.add(rule);
        }
        final int fit = Math.max(1, (h - 4) / NextgreStudioLayout.RULE_H);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, lines.size() - fit)));
        int ry = y + 3;
        for (int i = listScroll; i < lines.size() && ry + 10 <= y + h; i++) {
            if (lines.get(i) instanceof TextKey heading) {
                Draw.text(g, font, GameText.resolve(heading), x + 4, ry + 2, skin.dim(), skin.fieldBg(), SMALL);
                ry += 11;
                continue;
            }
            final NextgreEngine.RuleRow rule = (NextgreEngine.RuleRow) lines.get(i);
            int nameX = x + 4;
            if (rule.switchable()) {
                g.fill(x + 4, ry, x + 12, ry + 8, c.box());
                Draw.outline(g, x + 4, ry, 8, 8, skin.edge());
                if (rule.on()) {
                    g.fill(x + 6, ry + 2, x + 10, ry + 6, c.accent());
                }
                hits.add(new Hit(x + 2, ry - 1, w - 4, NextgreStudioLayout.RULE_H - 2, () -> toggleRule(rule.id())));
                nameX = x + 16;
            }
            Draw.text(g, font, Texts.clip(font, GameText.resolve(rule.name()), x + w - nameX - 4), nameX, ry,
                    rule.on() ? skin.text() : skin.dim(), skin.fieldBg());
            Draw.text(g, font, Texts.clip(font, GameText.resolve(rule.tells()), (int) ((x + w - nameX - 4) / SMALL)),
                    nameX, ry + 10, skin.dim(), skin.fieldBg(), SMALL);
            ry += NextgreStudioLayout.RULE_H;
        }
    }

    private void drawHistory(final GuiGraphics g, final Font font, final NextgreStudioPalette.Colours c, final int x,
                             final int y, final int w, final int h) {
        final int[] xs = {x + 4, x + 30, x + w * 62 / 100, x + w * 74 / 100, x + w * 86 / 100};
        columns(g, font, x, y, w, List.of(NextgreStudioTexts.PLAN_NUMBER, NextgreStudioTexts.STATEMENT,
                NextgreStudioTexts.COST, NextgreStudioTexts.TOOK, NextgreStudioTexts.STATE), xs);
        final List<NextgreStudioPayload.HistoryRow> rows = data.history();
        if (rows.isEmpty()) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(NextgreStudioTexts.NO_HISTORY), w - 8), x + 4,
                    y + NextgreStudioLayout.COLUMNS_H + 3, skin.dim(), skin.fieldBg());
            return;
        }
        final int fit = Math.max(1, (h - NextgreStudioLayout.COLUMNS_H) / NextgreStudioLayout.ROW_H);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, rows.size() - fit)));
        int ry = y + NextgreStudioLayout.COLUMNS_H + 1;
        for (int i = listScroll; i < rows.size() && i - listScroll < fit; i++) {
            final NextgreStudioPayload.HistoryRow row = rows.get(i);
            final boolean shown = plan != null && plan.id() == row.id();
            final int ground = shown ? c.chosen() : skin.fieldBg();
            if (shown) {
                g.fill(x + 1, ry - 1, x + w - 1, ry + NextgreStudioLayout.ROW_H - 1, ground);
            }
            final int ink = shown ? c.boxText() : skin.text();
            final List<String> cells = List.of(Integer.toString(row.id()), row.statement(),
                    row.cost() < 0 ? NO_TIME : GameText.resolve(seconds(row.cost())),
                    row.executionTicks() < 0 ? NO_TIME : GameText.resolve(seconds(row.executionTicks())),
                    GameText.resolve(stateWord(row.state())));
            for (int k = 0; k < cells.size(); k++) {
                final int right = k + 1 < xs.length ? xs[k + 1] : x + w;
                final int colour = k == 4 ? stateColour(c, row.state(), shown) : ink;
                Draw.text(g, font, Texts.clip(font, cells.get(k), right - xs[k] - 4), xs[k], ry, colour, ground);
            }
            hits.add(new Hit(x + 1, ry - 1, w - 2, NextgreStudioLayout.ROW_H, () -> open(row.id())));
            ry += NextgreStudioLayout.ROW_H;
        }
    }

    private void columns(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                         final List<TextKey> names, final int[] xs) {
        g.fill(x + 1, y + 1, x + w - 1, y + NextgreStudioLayout.COLUMNS_H, skin.panelBg());
        g.fill(x + 1, y + NextgreStudioLayout.COLUMNS_H - 1, x + w - 1, y + NextgreStudioLayout.COLUMNS_H,
                skin.edge());
        for (int i = 0; i < names.size(); i++) {
            final int right = i + 1 < xs.length ? xs[i + 1] : x + w;
            Draw.text(g, font, Texts.clip(font, GameText.resolve(names.get(i)), right - xs[i] - 4), xs[i], y + 2,
                    skin.dim(), skin.panelBg());
        }
    }

    private void drawStatus(final GuiGraphics g, final Font font, final NextgreStudioPalette.Colours c, final int x,
                            final int y, final int w) {
        g.fill(x, y, x + w, y + NextgreStudioLayout.STATUS_H, c.status());
        final List<String> right = new ArrayList<>();
        if (data != null) {
            right.add(GameText.resolve(data.engine()));
            right.add(GameText.resolve(data.network()));
        }
        if (plan != null) {
            right.add(GameText.resolve(NextgreStudioTexts.PLANNING.with(String.format(Locale.ROOT, "%.1f",
                    plan.planningMicros() / 1000.0))));
            right.add(plan.state() == NextgrePlanView.RUNNING ? GameText.resolve(NextgreStudioTexts.EXECUTING)
                    : plan.executionTicks() >= 0
                    ? GameText.resolve(NextgreStudioTexts.EXECUTION.with(seconds(plan.executionTicks()))) : "");
        }
        final String rightText = String.join(SEPARATOR, right.stream().filter(s -> !s.isEmpty()).toList());
        final int rightW = font.width(rightText);
        Draw.text(g, font, rightText, x + w - 4 - rightW, y + 2, c.statusText(), c.status());
        Draw.text(g, font, Texts.clip(font, statusText(), w - rightW - 16), x + 4, y + 2, c.statusText(), c.status());
    }

    private static TextKey heading(final byte kind) {
        return switch (kind) {
            case NextgreEngine.RuleRow.OWN_RULE -> NextgreStudioTexts.OWN_RULES;
            case NextgreEngine.RuleRow.ADDED_RULE -> NextgreStudioTexts.ADDED_RULES;
            case NextgreEngine.RuleRow.OWN_HINT -> NextgreStudioTexts.OWN_HINTS;
            default -> NextgreStudioTexts.ADDED_HINTS;
        };
    }

    private static Text alternativeText(final NextgrePlanView.Alternative alt) {
        return (alt.chosen() ? NextgreStudioTexts.ALTERNATIVE_CHOSEN : NextgreStudioTexts.ALTERNATIVE)
                .with(alt.number(), alt.cost());
    }

    /* {@code text} broken into lines no wider than {@code width}, at the spaces between its words. */
    private static List<String> wrap(final Font font, final String text, final int width) {
        final List<String> out = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : text.split(" ")) {
            if (!line.isEmpty() && font.width(line + " " + word) > width) {
                out.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    private static Text seconds(final long ticks) {
        if (ticks < 0) {
            return Text.literal(NO_TIME);
        }
        final double secs = ticks / TICKS_PER_SECOND;
        return NextgreStudioTexts.SECONDS.with(secs < 10.0 ? String.format(Locale.ROOT, "%.1f", secs)
                : Long.toString(Math.round(secs)));
    }

    private static TextKey stateWord(final byte state) {
        return switch (state) {
            case NextgrePlanView.RUNNING -> NextgreStudioTexts.RUNNING;
            case NextgrePlanView.DONE -> NextgreStudioTexts.DONE;
            case NextgrePlanView.FAILED -> NextgreStudioTexts.SHORT;
            default -> NextgreStudioTexts.PLANNED;
        };
    }

    private int stateColour(final NextgreStudioPalette.Colours c, final byte state, final boolean shown) {
        return switch (state) {
            case NextgrePlanView.RUNNING -> c.running();
            case NextgrePlanView.DONE -> c.good();
            case NextgrePlanView.FAILED -> c.bad();
            default -> shown ? c.boxDim() : skin.dim();
        };
    }

    /** Somewhere on the window that does something when clicked, as laid out this frame. */
    private record Hit(int x, int y, int w, int h, Runnable action) {
    }
}
