/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.teracoin.MiningPanelRequestPayload;
import dev.jstech.computers.operation.payload.teracoin.MiningPanelStatePayload;
import dev.jstech.computers.teracoin.MinerConsole;
import dev.jstech.computers.teracoin.MinerKinds;
import dev.jstech.computers.teracoin.MiningPool;
import dev.jstech.computers.teracoin.TeracoinAddress;
import dev.jstech.computers.teracoin.TeracoinHashrate;
import dev.jstech.computers.teracoin.TeracoinRules;
import dev.jstech.computers.teracoin.Trc;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Dropdown;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.RadioGroup;
import dev.jstech.core.client.gui.component.Slider;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.time.GameCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.client.os.TabbedWindowLook.FIELD_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.HEADER_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.PAD;
import static dev.jstech.computers.client.os.TabbedWindowLook.ROW_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.STATUS_H;
import static dev.jstech.computers.client.os.TabbedWindowLook.TAB_H;

/**
 * The Mining Panel, Blockwright's window on a network's Mining Pools (computers/docs/design/teracoin.md), as the
 * Messenger is the window of its service: the pool's hashrate, what it earns an hour, a day and a week, the blocks it
 * found, the last hour, each address's share of the round, its miners, its blocks with who found each, and, on the
 * pool's own server, its owner's settings: the fee, where it goes, who may join, and who asks to.
 */
@PaletteHolder
public final class MiningPanelApp implements IDesktopApp {

    private final BlockPos host;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private MiningPanelStatePayload state;
    private int tab;
    private int chosenPool;
    private int sentFee = -1;
    private long lastRequestMillis;
    private int lastMouseX;
    private int lastMouseY;

    private final Panel root = new Panel();
    private final TabStrip tabs;
    private final Dropdown poolChoice;
    private final ColumnHeader minerColumns;
    private final ListView<MiningPanelStatePayload.Miner> minerList;
    private final ColumnHeader blockColumns;
    private final ListView<MiningPanelStatePayload.Block> blockList;
    private final TextField nameField;
    private final Slider feeSlider;
    private final TextField ownerField;
    private final RadioGroup who;
    private final ColumnHeader askingColumns;
    private final ListView<MiningPanelStatePayload.Miner> askingList;
    private final Button accept;
    private final Button refuse;

    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/mining_panel",
            new Colours(0xFFB07A14, 0xFF4CB782, 0xFFE8B33A, 0xFFE6E8EC));
    private static final TextKey[] TABS = {MiningPanelTexts.POOL, MiningPanelTexts.MINERS, MiningPanelTexts.BLOCKS,
            MiningPanelTexts.SETTINGS};
    private static final int POOL = 0;
    private static final int MINERS = 1;
    private static final int BLOCKS = 2;
    private static final int SETTINGS = 3;
    /* The Pool tab: the five figures as tiles, the last hour's chart beside the round's shares, the miners. */
    private static final int TILE_TOP = 20;
    private static final int TILE_H = 31;
    private static final int TILE_GAP = 5;
    private static final int TILES = 5;
    private static final int CHARTS_TOP = 57;
    private static final int CHART_H = 57;
    private static final int SHARE_PITCH = 23;
    private static final int POOL_TABLE_TOP = 135;
    private static final int POOL_ROWS = 4;
    /* The Settings tab: its fields after their labels, the fee's slider and the buttons that answer a miner. */
    private static final int LABEL_W = 72;
    private static final int SLIDER_W = 200;
    private static final int SLIDER_H = 11;
    private static final int FEE_STEPS = 6;
    private static final int BUTTON_W = 64;
    private static final long REFRESH_MILLIS = 2000L;
    private static final double SECONDS_PER_HOUR = 3_600.0;

    private static MiningPanelApp active;

    /** The panel of the computer at {@code host}. */
    public MiningPanelApp(final BlockPos host) {
        this.host = host;
        tabs = root.add(new TabStrip(this::tabLabels).fitToLabels(16).setOnSelect(index -> tab = index));
        poolChoice = root.add(new Dropdown(poolNames(), () -> chosenPool, index -> {
            chosenPool = index;
            request(MiningPanelRequestPayload.LOOK, "", 0L);
        }, words -> words));
        minerColumns = root.add(TabbedWindowLook.header(words(MiningPanelTexts.MINER, MiningPanelTexts.KIND,
                MiningPanelTexts.PAYS, MiningPanelTexts.HASHRATE, MiningPanelTexts.SHARE)));
        minerList = root.add(new ListView<>(this::miners, ROW_H, this::renderMiner));
        blockColumns = root.add(TabbedWindowLook.header(words(MiningPanelTexts.BLOCK, MiningPanelTexts.WHEN,
                MiningPanelTexts.FOUND_BY, MiningPanelTexts.REWARD)));
        blockList = root.add(new ListView<>(this::blocks, ROW_H, this::renderBlock));
        nameField = root.add(new TextField(MiningPool.NAME_MOST)
                .setOnCommit(name -> request(MiningPanelRequestPayload.RENAME, name, 0L)));
        feeSlider = root.add(new Slider(() -> state == null ? 0.0 : state.pool().feePermille()
                / (double) MiningPool.MOST_FEE, this::feeMoved, fraction -> "", FEE_STEPS));
        ownerField = root.add(new TextField(TeracoinAddress.LENGTH)
                .setOnCommit(address -> request(MiningPanelRequestPayload.SET_OWNER, address, 0L)));
        who = root.add(new RadioGroup(List.of(() -> GameText.resolve(MiningPanelTexts.ANY_MINER),
                () -> GameText.resolve(MiningPanelTexts.ACCEPTED)),
                () -> state != null && state.pool().acceptedOnly() ? 1 : 0,
                option -> request(MiningPanelRequestPayload.SET_ACCEPTED_ONLY, "", option)));
        askingColumns = root.add(TabbedWindowLook.header(words(MiningPanelTexts.MINER, MiningPanelTexts.KIND,
                MiningPanelTexts.PAYS, MiningPanelTexts.HASHRATE)));
        askingList = root.add(new ListView<>(this::asking, ROW_H, this::renderAsking)
                .setOnClick((index, button, mx, my) -> pickAsking(index)));
        accept = root.add(new Button(GameText.resolve(MiningPanelTexts.ACCEPT), () -> answer(true))
                .setPrimary(true));
        refuse = root.add(new Button(GameText.resolve(MiningPanelTexts.REFUSE), () -> answer(false)));
        active = this;
        request(MiningPanelRequestPayload.LOOK, "", 0L);
    }

    /** Routes the network's answer to the open panel of that computer. */
    public static void accept(final MiningPanelStatePayload payload) {
        if (active != null && payload.hostPos().equals(active.host)) {
            active.state = payload;
            active.chosenPool = payload.chosen();
            if (!active.nameField.isFocused()) {
                active.nameField.set(payload.pool().name());
            }
            if (!active.ownerField.isFocused()) {
                active.ownerField.set(payload.pool().owner());
            }
            if (!payload.pool().yours() && active.tab == SETTINGS) {
                active.tab = POOL;
            }
        }
    }

    @Override
    public void onRestored() {
        active = this;
        request(MiningPanelRequestPayload.LOOK, "", 0L);
    }

    @Override
    public void onClosed() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public String title() {
        return GameText.resolve(MiningPanelTexts.TITLE);
    }

    @Override
    public int defaultWidth() {
        return 493;
    }

    @Override
    public int defaultHeight() {
        return 313;
    }

    @Override
    public int minWidth() {
        return 400;
    }

    @Override
    public int minHeight() {
        return 280;
    }

    @Override
    public boolean waiting() {
        return state == null;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (Util.getMillis() - lastRequestMillis >= REFRESH_MILLIS) {
            request(MiningPanelRequestPayload.LOOK, "", 0L);
        }
        g.fill(x, y, x + width, y + height, skin.windowBg());
        final boolean any = state != null && state.pool().any();
        layout(font, x, y, width, height, any);
        if (!any) {
            final String none = GameText.resolve(MiningPanelTexts.NO_POOL);
            final int noneW = Math.round(font.width(none) * TabbedWindowLook.HEADING);
            TabbedWindowLook.heading(g, font, none, x + (width - noneW) / 2, y + height / 2 - 18, skin.text());
            final String hint = Texts.clip(font, GameText.resolve(MiningPanelTexts.NO_POOL_HINT), width - 2 * PAD);
            Draw.text(g, font, hint, x + (width - font.width(hint)) / 2, y + height / 2, skin.dim());
        } else {
            TabbedWindowLook.tabRow(g, skin, x, y, width);
            switch (tab) {
                case POOL -> renderPool(g, font, x, y, width, height);
                case MINERS, BLOCKS -> {
                    if ((tab == MINERS ? miners() : blocks()).isEmpty()) {
                        Draw.text(g, font, GameText.resolve(tab == MINERS ? MiningPanelTexts.NO_MINERS
                                : MiningPanelTexts.NO_BLOCKS), x + PAD + 5, y + TAB_H + 8 + HEADER_H + 3,
                                skin.dim());
                    }
                }
                case SETTINGS -> renderSettings(g, font, x, y, width, height);
                default -> {
                }
            }
        }
        root.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
        if (state != null) {
            TeracoinLook.statusBar(g, font, skin, x, y + height - STATUS_H, width, state.chain(), "");
        } else {
            skin.statusBar(g, x, y + height - STATUS_H, width, STATUS_H);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (button == 0) {
            root.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        return root.mouseScrolled(lastMouseX, lastMouseY, delta);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return root.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean takesTyping() {
        return IDesktopApp.typingIn(root);
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    // what the client tests read and click

    /** Whether the network has answered. */
    public boolean hasState() {
        return state != null;
    }

    /** The pool looked at, or {@link MiningPanelStatePayload.Pool#NONE}. */
    public MiningPanelStatePayload.Pool pool() {
        return state == null ? MiningPanelStatePayload.Pool.NONE : state.pool();
    }

    /** Shows the tab {@code index}. */
    public void showTab(final int index) {
        tab = index;
        tabs.setSelected(index);
    }

    private void request(final int action, final String text, final long value) {
        lastRequestMillis = Util.getMillis();
        PacketDistributor.sendToServer(new MiningPanelRequestPayload(host, action, chosenPool, text, value));
    }

    private void feeMoved(final double fraction) {
        final int fee = (int) Math.round(fraction * (FEE_STEPS - 1)) * (MiningPool.MOST_FEE / (FEE_STEPS - 1));
        if (fee != sentFee) {
            sentFee = fee;
            request(MiningPanelRequestPayload.SET_FEE, "", fee);
        }
    }

    private void pickAsking(final int index) {
        askingList.setSelected(index);
    }

    private void answer(final boolean yes) {
        final List<MiningPanelStatePayload.Miner> waiting = asking();
        final int chosen = askingList.selected();
        if (chosen >= 0 && chosen < waiting.size()) {
            request(yes ? MiningPanelRequestPayload.ACCEPT : MiningPanelRequestPayload.REFUSE,
                    waiting.get(chosen).machine(), 0L);
        }
    }

    private List<String> tabLabels() {
        final List<String> out = new ArrayList<>();
        final int shown = state != null && state.pool().yours() ? TABS.length : TABS.length - 1;
        for (int i = 0; i < shown; i++) {
            out.add(GameText.resolve(TABS[i]));
        }
        return out;
    }

    private List<Supplier<String>> poolNames() {
        final List<Supplier<String>> out = new ArrayList<>();
        for (int i = 0; i < MiningPanelStatePayload.MOST_POOLS; i++) {
            final int index = i;
            out.add(() -> state == null || index >= state.pools().size() ? "" : state.pools().get(index));
        }
        return out;
    }

    private List<MiningPanelStatePayload.Miner> miners() {
        final List<MiningPanelStatePayload.Miner> out = new ArrayList<>();
        if (state != null) {
            for (final MiningPanelStatePayload.Miner miner : state.pool().miners()) {
                if (!miner.asking()) {
                    out.add(miner);
                }
            }
        }
        return out;
    }

    private List<MiningPanelStatePayload.Miner> asking() {
        final List<MiningPanelStatePayload.Miner> out = new ArrayList<>();
        if (state != null) {
            for (final MiningPanelStatePayload.Miner miner : state.pool().miners()) {
                if (miner.asking()) {
                    out.add(miner);
                }
            }
        }
        return out;
    }

    private List<MiningPanelStatePayload.Block> blocks() {
        return state == null ? List.of() : state.pool().blocks();
    }

    private void layout(final Font font, final int x, final int y, final int width, final int height,
                        final boolean any) {
        final int px = x + PAD;
        final int pw = width - 2 * PAD;
        final int bottom = y + height - STATUS_H;
        final int top = y + TAB_H;
        TabbedWindowLook.placeTabs(tabs, x, y, width);
        tabs.setVisible(any);
        tabs.setSelected(tab);
        final boolean several = any && state.pools().size() > 1;
        poolChoice.setVisible(several);
        poolChoice.setBounds(x + width - 108, y + 1, 100, TAB_H - 2);
        final boolean pool = any && tab == POOL;
        final boolean all = any && tab == MINERS;
        final boolean blocksTab = any && tab == BLOCKS;
        final boolean settings = any && tab == SETTINGS;
        minerColumns.setVisible(pool || all);
        minerList.setVisible(pool || all);
        minerColumns.setColumnX(TabbedWindowLook.columns(px, pw, 11, 36, 57, 74));
        // The Pool tab's table sits under its tiles and charts; the Miners tab's table fills the tab.
        final int minerTop = pool ? top + POOL_TABLE_TOP : top + 8;
        minerColumns.setBounds(px, minerTop, pw, HEADER_H);
        final int minerRoom = bottom - 4 - (minerTop + HEADER_H);
        minerList.setBounds(px, minerTop + HEADER_H, pw, pool ? Math.min(POOL_ROWS * ROW_H, minerRoom - 13)
                : Math.max(ROW_H, minerRoom));
        blockColumns.setVisible(blocksTab);
        blockList.setVisible(blocksTab);
        if (blocksTab) {
            blockColumns.setBounds(px, top + 8, pw, HEADER_H);
            blockColumns.setColumnX(TabbedWindowLook.columns(px, pw, 18, 42, 75));
            blockList.setBounds(px, top + 8 + HEADER_H, pw, Math.max(ROW_H, bottom - 4 - (top + 8 + HEADER_H)));
        }
        nameField.setVisible(settings);
        feeSlider.setVisible(settings);
        ownerField.setVisible(settings);
        who.setVisible(settings);
        askingColumns.setVisible(settings);
        askingList.setVisible(settings);
        accept.setVisible(settings);
        refuse.setVisible(settings);
        if (settings) {
            final int fieldX = px + LABEL_W;
            nameField.setBounds(fieldX, top + 25, x + width - PAD - fieldX, FIELD_H);
            feeSlider.setBounds(fieldX, top + 46, SLIDER_W, SLIDER_H);
            ownerField.setBounds(fieldX, top + 78, x + width - PAD - fieldX, FIELD_H);
            who.setBounds(fieldX, top + 101, x + width - PAD - fieldX, who.neededHeight());
            final int listTop = who.bottom() + 18;
            askingColumns.setBounds(px, listTop, pw, HEADER_H);
            askingColumns.setColumnX(TabbedWindowLook.columns(px, pw, 13, 38, 62));
            refuse.setBounds(x + width - 11 - BUTTON_W, bottom - FIELD_H - 17, BUTTON_W, FIELD_H);
            accept.setBounds(refuse.x() - 8 - BUTTON_W, refuse.y(), BUTTON_W, FIELD_H);
            askingList.setBounds(px, listTop + HEADER_H, pw, Math.max(ROW_H, refuse.y() - 6 - (listTop + HEADER_H)));
        }
    }

    private void renderPool(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                            final int height) {
        final MiningPanelStatePayload.Pool pool = state.pool();
        final int px = x + PAD;
        final int pw = width - 2 * PAD;
        final String fee = percent(pool.feePermille());
        final String header = pool.owner().isEmpty()
                ? GameText.resolve(MiningPanelTexts.HEADER_NO_OWNER.with(pool.name(), pool.host(), fee))
                : GameText.resolve(MiningPanelTexts.HEADER.with(pool.name(), pool.host(), fee,
                        TeracoinAddress.shortForm(pool.owner())));
        final int top = y + TAB_H;
        Draw.text(g, font, Texts.clip(font, header, pw), px, top + 7, skin.dim());
        final double perHour = trcPerHour(pool.hashrate());
        final String[] labels = {GameText.resolve(MiningPanelTexts.HASHRATE),
                GameText.resolve(MiningPanelTexts.AN_HOUR), GameText.resolve(MiningPanelTexts.A_DAY),
                GameText.resolve(MiningPanelTexts.A_WEEK), GameText.resolve(MiningPanelTexts.BLOCKS_FOUND)};
        final String[] values = {TeracoinHashrate.format(pool.hashrate()),
                String.format(Locale.ROOT, "%,.1f", perHour), String.format(Locale.ROOT, "%,.0f", perHour * 24),
                String.format(Locale.ROOT, "%,.0f", perHour * 168),
                String.format(Locale.ROOT, "%,d", pool.foundCount())};
        final int tileW = (pw - (TILES - 1) * TILE_GAP) / TILES;
        final int tileY = top + TILE_TOP;
        for (int i = 0; i < TILES; i++) {
            final int tx = px + i * (tileW + TILE_GAP);
            g.fill(tx, tileY, tx + tileW, tileY + TILE_H, skin.panelBg());
            Draw.outline(g, tx, tileY, tileW, TILE_H, skin.edge());
            Draw.text(g, font, Texts.clip(font, labels[i], tileW - 10), tx + 5, tileY + 4, skin.dim());
            final String value = Texts.clip(font, values[i], (int) ((tileW - 10) / TabbedWindowLook.HEADING));
            TabbedWindowLook.heading(g, font, value, tx + 5, tileY + 15,
                    i >= 1 && i <= 3 ? colours().gold() : skin.text());
        }
        final int chartW = pw * 40 / 100;
        final int sharesX = px + pw * 43 / 100;
        Draw.text(g, font, GameText.resolve(MiningPanelTexts.LAST_HOUR), px, top + CHARTS_TOP, skin.text());
        Draw.text(g, font, GameText.resolve(MiningPanelTexts.SHARES), sharesX, top + CHARTS_TOP, skin.text());
        renderHour(g, px, top + CHARTS_TOP + 12, chartW, pool.lastHour());
        renderShares(g, font, sharesX, top + CHARTS_TOP + 15, px + pw - sharesX, pool.shares());
        final long seconds = pool.hashrate() <= 0L ? 0L : state.chain().difficulty() / pool.hashrate();
        if (seconds > 0L) {
            final String every = GameText.resolve(MiningPanelTexts.EVERY.with(
                    GameText.resolve(MinerConsole.duration(seconds)),
                    TeracoinRules.ADJUST_EVERY - state.chain().height() % TeracoinRules.ADJUST_EVERY));
            Draw.text(g, font, Texts.clip(font, every, pw), px + 1, y + height - STATUS_H - 13, skin.dim());
        }
        if (miners().isEmpty()) {
            Draw.text(g, font, GameText.resolve(MiningPanelTexts.NO_MINERS), minerList.x() + 5, minerList.y() + 3,
                    skin.dim());
        }
    }

    private void renderHour(final GuiGraphics g, final int x, final int y, final int w, final long[] hour) {
        g.fill(x, y, x + w, y + CHART_H, skin.fieldBg());
        Draw.outline(g, x, y, w, CHART_H, skin.edge());
        long most = 1L;
        for (final long slice : hour) {
            most = Math.max(most, slice);
        }
        final int slots = Math.max(1, hour.length);
        final int barW = Math.max(1, (w - 4) / slots - 1);
        for (int i = 0; i < hour.length; i++) {
            final int barH = (int) Math.round((CHART_H - 4) * (double) hour[i] / most);
            final int bx = x + 2 + i * (barW + 1);
            g.fill(bx, y + CHART_H - 2 - barH, bx + barW, y + CHART_H - 2, colours().bar());
        }
    }

    private void renderShares(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final List<MiningPanelStatePayload.Share> shares) {
        if (shares.isEmpty()) {
            Draw.text(g, font, GameText.resolve(MiningPanelTexts.NO_SHARES), x, y, skin.dim());
            return;
        }
        int row = y;
        for (int i = 0; i < Math.min(2, shares.size()); i++) {
            final MiningPanelStatePayload.Share share = shares.get(i);
            final String who = share.yours() ? GameText.resolve(MiningPanelTexts.YOU.with(
                    TeracoinAddress.shortForm(share.address()))) : TeracoinAddress.shortForm(share.address());
            final String part = String.format(Locale.ROOT, "%.1f%%", share.permille() / 10.0);
            Draw.text(g, font, Texts.clip(font, who, w - font.width(part) - 8), x, row, skin.text());
            Draw.text(g, font, part, x + w - font.width(part), row, skin.text());
            g.fill(x, row + 11, x + w, row + 16, colours().track());
            g.fill(x, row + 11, x + (int) ((long) w * share.permille() / 1000L), row + 16, colours().share());
            row += SHARE_PITCH;
        }
    }

    private void renderSettings(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                                final int height) {
        final MiningPanelStatePayload.Pool pool = state.pool();
        final int px = x + PAD + 1;
        Draw.text(g, font, Texts.clip(font, GameText.resolve(MiningPanelTexts.YOURS.with(pool.name(), pool.host())),
                width - 2 * PAD), px, y + TAB_H + 9, skin.dim());
        Draw.text(g, font, GameText.resolve(MiningPanelTexts.NAME), px, nameField.y() + 4, skin.text());
        Draw.text(g, font, GameText.resolve(MiningPanelTexts.FEE), px, feeSlider.y() + 2, skin.text());
        TabbedWindowLook.heading(g, font, percent(pool.feePermille()), feeSlider.right() + 13, feeSlider.y(),
                skin.text());
        final int steps = FEE_STEPS - 1;
        for (int i = 0; i <= steps; i++) {
            final String label = GameText.resolve(MiningPanelTexts.PERCENT.with(i));
            final int lx = feeSlider.x() + (feeSlider.width() - 4) * i / steps;
            Draw.text(g, font, label, lx - font.width(label) / 2 + 2, feeSlider.bottom() + 2, skin.dim());
        }
        Draw.text(g, font, GameText.resolve(MiningPanelTexts.FEE_TO), px, ownerField.y() + 4, skin.text());
        Draw.text(g, font, GameText.resolve(MiningPanelTexts.WHO), px, who.y() + 1, skin.text());
        TabbedWindowLook.heading(g, font, GameText.resolve(MiningPanelTexts.ASKING_HEADING), px,
                askingColumns.y() - 16, skin.text());
        if (asking().isEmpty()) {
            Draw.text(g, font, GameText.resolve(MiningPanelTexts.NOBODY_ASKS), askingList.x() + 5,
                    askingList.y() + 3, skin.dim());
        }
        Draw.text(g, font, Texts.clip(font, GameText.resolve(MiningPanelTexts.FEE_NOTE), width - 2 * PAD), px,
                y + height - STATUS_H - 13, skin.dim());
    }

    private void renderMiner(final GuiGraphics g, final UiContext ctx, final MiningPanelStatePayload.Miner miner,
                             final int index, final int x, final int y, final int w, final int h,
                             final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, w, h, hovered, selected);
        TabbedWindowLook.cells(g, ctx, x, y, w, TabbedWindowLook.columns(x, w, 11, 36, 57, 74), miner.name(),
                kindWords(miner.kind()), TeracoinAddress.shortForm(miner.address()),
                TeracoinHashrate.format(miner.hashrate()), miner.asking() ? GameText.resolve(MiningPanelTexts.ASKING)
                        : String.format(Locale.ROOT, "%.1f%%", miner.permille() / 10.0));
    }

    private void renderAsking(final GuiGraphics g, final UiContext ctx, final MiningPanelStatePayload.Miner miner,
                              final int index, final int x, final int y, final int w, final int h,
                              final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, w, h, hovered, selected);
        TabbedWindowLook.cells(g, ctx, x, y, w, TabbedWindowLook.columns(x, w, 13, 38, 62), miner.name(),
                kindWords(miner.kind()), TeracoinAddress.shortForm(miner.address()),
                TeracoinHashrate.format(miner.hashrate()));
    }

    private void renderBlock(final GuiGraphics g, final UiContext ctx, final MiningPanelStatePayload.Block block,
                             final int index, final int x, final int y, final int w, final int h,
                             final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, w, h, hovered, selected);
        TabbedWindowLook.cells(g, ctx, x, y, w, TabbedWindowLook.columns(x, w, 18, 42, 75),
                TabbedWindowLook.grouped(block.height()), GameText.resolve(CryptoWalletTexts.WHEN_AT.with(
                        GameCalendar.day(block.dayTime()) + 1, String.format(Locale.ROOT, "%02d:%02d",
                                GameCalendar.hourOf(block.dayTime()), GameCalendar.minuteOf(block.dayTime())))),
                block.finder(), Trc.format(block.reward(), 2));
    }

    /* What a pool earns an hour at {@code hashrate}: its share of the blocks an hour, times the reward. */
    private double trcPerHour(final long hashrate) {
        final long difficulty = Math.max(1L, state.chain().difficulty());
        return (double) hashrate / difficulty * SECONDS_PER_HOUR * state.chain().reward() / Trc.UNITS;
    }

    private static String kindWords(final String code) {
        final String[] parts = MinerKinds.parts(code);
        if (parts.length >= 3 && MinerKinds.RIG.equals(parts[0])) {
            return GameText.resolve(MiningPanelTexts.RIG.with(eraWords(parts[1]), parts[2]));
        }
        if (parts.length >= 2 && MinerKinds.ASIC.equals(parts[0])) {
            return GameText.resolve(MiningPanelTexts.ASIC.with(eraWords(parts[1])));
        }
        final String kind = parts.length >= 2 ? parts[1] : "";
        return GameText.resolve(switch (kind) {
            case "personal_computer" -> MiningPanelTexts.PERSONAL_COMPUTER;
            case "mainframe" -> MiningPanelTexts.MAINFRAME;
            case "server" -> MiningPanelTexts.SERVER;
            default -> MiningPanelTexts.COMPUTER;
        });
    }

    private static String eraWords(final String era) {
        return GameText.resolve(switch (era) {
            case "transition" -> MiningPanelTexts.TRANSITION;
            case "advanced" -> MiningPanelTexts.ADVANCED;
            default -> MiningPanelTexts.STANDARD;
        });
    }

    private static String percent(final int permille) {
        return permille % 10 == 0 ? permille / 10 + "%" : String.format(Locale.ROOT, "%.1f%%", permille / 10.0);
    }

    private static List<String> words(final TextKey... keys) {
        final List<String> out = new ArrayList<>();
        for (final TextKey key : keys) {
            out.add(GameText.resolve(key));
        }
        return out;
    }

    private static Colours colours() {
        return PALETTE.get();
    }

    /** The panel's own colours: Blockwright's gold, the last hour's bars, the shares' bars and their track. */
    private record Colours(int gold, int bar, int share, int track) {
    }
}
