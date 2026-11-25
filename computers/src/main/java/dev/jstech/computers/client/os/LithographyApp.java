/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.chain.machine.MaskWriting;
import dev.jstech.computers.lithography.LithographyLook;
import dev.jstech.computers.operation.payload.lithography.LithographyRequestPayload;
import dev.jstech.computers.operation.payload.lithography.LithographyStatePayload;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.MenuBar;
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
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The lithography program's window on the desktop systems (computers/docs/design/survival.md, "Masks"): the station
 * linked to its computer on the left, with what it is doing, its liquids and gases, what its computer holds for it and
 * what stands in its mask slot; on the right the masks it writes for this computer, a tab a kind of chip, and Write
 * Mask, which has it write the one picked on the Mask Blank in its slot; the writing's progress in the status bar.
 */
public final class LithographyApp implements IDesktopApp {

    private final BlockPos host;
    private final Panel root = new Panel();
    private final MenuBar menuBar = new MenuBar(MENU_ITEM_W, MenuBar.HEIGHT + 2);
    private final TabStrip tabs;
    private final ListView<LithographyLook.Pick> list;
    private final Button write;
    private OsSkin skin = OsSkin.fallback();
    private @Nullable LithographyStatePayload state;
    private int station;
    private int tab;
    private @Nullable ResourceLocation picked;
    private long lastRequestMillis;

    private static final int MENU_ITEM_W = 92;
    /* The window and where things stand in it, as the approved mock lays them out. */
    private static final int WIDTH = 490;
    private static final int HEIGHT = 250;
    private static final int PAD = 7;
    private static final int GROUP_W = 184;
    private static final int GROUP_TOP = 24;
    private static final int ROW_TOP = 11;
    private static final int ROW_PITCH = 16;
    private static final int LABEL_X = 8;
    private static final int VALUE_X = 79;
    private static final int MASKS_X = 200;
    private static final int TAB_H = 13;
    private static final int LIST_INSET = 7;
    private static final int LIST_TOP = 8;
    private static final int ROW_H = 14;
    private static final int BUTTON_W = 72;
    private static final int BUTTON_H = 15;
    private static final int BUTTON_FOOT = 21;
    private static final int STATUS_H = 16;
    private static final int BAR_W = 146;
    private static final long REFRESH_MILLIS = 1000L;
    private static final double PERMILLE = 1000.0;

    private static @Nullable LithographyApp active;

    /** The program on the computer at {@code host}. */
    public LithographyApp(final BlockPos host) {
        this.host = host;
        this.menuBar.add(GameText.resolve(LithographyTexts.MENU_FILE), this::fileMenu)
                .add(GameText.resolve(LithographyTexts.MENU_STATION), this::stationMenu)
                .add(GameText.resolve(LithographyTexts.MENU_HELP), this::helpMenu);
        this.tabs = root.add(new TabStrip(this::tabLabels).fitToLabels(PAD * 2).setOnSelect(index -> {
            tab = index;
            picked = null;
        }));
        this.list = root.add(new ListView<>(this::shownPicks, ROW_H, this::renderPick)
                .setOnClick((index, button, mx, my) -> pick(index)));
        // Write Mask for a station, Cut Mould for an Injection Moulder, as the machine shown is.
        this.write = root.add(new Button(() -> GameText.resolve(LithoWords.button(state == null
                ? LithographyLook.NONE : state.look())), this::writeMask));
        active = this;
        request(LithographyRequestPayload.LOOK, null);
    }

    /** Routes the computer's answer to the open window of that computer. */
    public static void accept(final LithographyStatePayload payload) {
        if (active != null && payload.hostPos().equals(active.host)) {
            active.state = payload;
            active.station = payload.look().shown();
        }
    }

    @Override
    public void onRestored() {
        active = this;
        request(LithographyRequestPayload.LOOK, null);
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
        return GameText.resolve(LithographyTexts.TITLE);
    }

    @Override
    public int defaultWidth() {
        return WIDTH;
    }

    @Override
    public int defaultHeight() {
        return HEIGHT;
    }

    @Override
    public int minWidth() {
        return WIDTH;
    }

    @Override
    public int minHeight() {
        return HEIGHT;
    }

    @Override
    public boolean waiting() {
        return state == null;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        if (Util.getMillis() - lastRequestMillis >= REFRESH_MILLIS) {
            request(LithographyRequestPayload.LOOK, null);
        }
        g.fill(x, y, x + width, y + height, skin.windowBg());
        final UiContext ui = new UiContext(skin, font, mouseX, mouseY, partialTick);
        final LithographyLook look = state == null ? LithographyLook.NONE : state.look();
        final boolean any = look.stations() > 0;
        tabs.setVisible(any);
        list.setVisible(any);
        write.setVisible(any);
        if (any) {
            renderStation(g, font, look, x, y, height);
            final int paneBottom = y + height - STATUS_H - PAD - 4;
            skin.panel(g, x + MASKS_X, y + GROUP_TOP + TAB_H, width - MASKS_X - PAD, paneBottom - (y + GROUP_TOP
                    + TAB_H));
            layoutMasks(look, x, y, width, paneBottom);
            Draw.text(g, font, GameText.resolve(LithoWords.neverUsedUp(look)), x + MASKS_X + LIST_INSET,
                    paneBottom - BUTTON_FOOT + 4, skin.dim());
        } else if (state != null) {
            final String none = GameText.resolve(LithographyTexts.NO_STATION);
            Draw.text(g, font, none, x + (width - font.width(none)) / 2, y + height / 2 - ROW_PITCH, skin.text());
            final String hint = Texts.clip(font, GameText.resolve(LithographyTexts.NO_STATION_HINT), width - PAD * 2);
            Draw.text(g, font, hint, x + (width - font.width(hint)) / 2, y + height / 2, skin.dim());
        }
        root.render(g, ui);
        renderStatus(g, font, look, x, y + height - STATUS_H, width);
        menuBar.setBounds(x, y, width, MenuBar.HEIGHT + 2);
        menuBar.setWindow(x, y, width, height);
        menuBar.render(g, ui);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (menuBar.isOpen() || menuBar.titleAt(mouseX, mouseY) >= 0) {
            menuBar.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (button == 0) {
            root.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return menuBar.isOpen() ? menuBar.keyPressed(key, scanCode, modifiers) : root.keyPressed(key, scanCode,
                modifiers);
    }

    // what the client tests read and click

    /** What the window shows, or nothing until the computer has answered. */
    public @Nullable LithographyLook look() {
        return state == null ? null : state.look();
    }

    /** What came of what it asked last: a refusal and why, or nothing. */
    public String said() {
        return state == null ? "" : GameText.resolve(state.said());
    }

    /** Shows the list of {@code kind}, its first mask picked. */
    public void showKind(final MaskWriting.Kind kind) {
        final int at = kinds().indexOf(kind.serializedName());
        if (at >= 0) {
            tab = at;
            tabs.setSelected(at);
            picked = null;
        }
    }

    /** Picks the mask the program calls {@code name} in the list shown, if it is there. */
    public void pickNamed(final String name) {
        final List<LithographyLook.Pick> shown = shownPicks();
        for (int i = 0; i < shown.size(); i++) {
            if (GameText.resolve(shown.get(i).name()).equals(name)) {
                pick(i);
            }
        }
    }

    /** Presses Write Mask. */
    public void writeMask() {
        final LithographyLook.Pick pick = currentPick();
        if (pick != null) {
            request(LithographyRequestPayload.WRITE, pick.mask());
        }
    }

    private void request(final int action, @Nullable final ResourceLocation mask) {
        lastRequestMillis = Util.getMillis();
        PacketDistributor.sendToServer(new LithographyRequestPayload(host, action, station,
                mask == null ? ResourceLocation.withDefaultNamespace("air") : mask));
    }

    /* The station's rows down the left, in its group: what it is, what it does, what it holds. */
    private void renderStation(final GuiGraphics g, final Font font, final LithographyLook look, final int x,
                               final int y, final int height) {
        final int gx = x + PAD;
        final int gy = y + GROUP_TOP;
        skin.panel(g, gx, gy, GROUP_W, height - STATUS_H - PAD - 4 - GROUP_TOP);
        final String group = GameText.resolve(LithographyTexts.GROUP_STATION);
        g.fill(gx + 6, gy - 4, gx + 10 + font.width(group), gy + 5, skin.windowBg());
        Draw.text(g, font, group, gx + 8, gy - 4, skin.accent());
        final List<String[]> rows = new ArrayList<>();
        rows.add(row(LithographyTexts.ROW_STATION, look.station()));
        rows.add(new String[] {GameText.resolve(LithographyTexts.ROW_LINKED),
            GameText.resolve(LithographyTexts.PERIPHERAL_PORT)});
        rows.add(row(LithographyTexts.ROW_STATUS, look.status()));
        rows.add(row(LithoWords.wafer(look), look.wafer()));
        if (look.exposing()) {
            rows.add(new String[] {GameText.resolve(LithoWords.layer(look)), String.format(Locale.ROOT,
                "%d%%  %s", look.layerPermille() / 10, GameText.resolve(LithographyTexts.SECONDS.with(
                look.layerSeconds())))});
        }
        for (final LithographyLook.Supply supply : look.supplies()) {
            rows.add(new String[] {GameText.resolve(supply.name()), GameText.resolve(LithographyTexts.MILLIBUCKETS
                .with(String.format(Locale.ROOT, "%,d", supply.amount())))});
        }
        rows.add(new String[] {GameText.resolve(LithoWords.process(look)), GameText.resolve(look.lithoMb() > 0
            ? LithographyTexts.HELD.with(look.lithoMb()) : LithographyTexts.NOT_RUNNING.text())});
        rows.add(row(LithoWords.slot(look), look.slot()));
        for (int i = 0; i < rows.size(); i++) {
            final int ry = gy + ROW_TOP + i * ROW_PITCH;
            Draw.text(g, font, Texts.clip(font, rows.get(i)[0], VALUE_X - LABEL_X - 2), gx + LABEL_X, ry,
                    skin.dim());
            Draw.text(g, font, Texts.clip(font, rows.get(i)[1], GROUP_W - VALUE_X - 4), gx + VALUE_X, ry,
                    skin.text());
        }
    }

    /*
     * The masks on the right: the kinds as tabs over their pane, the list of the one shown in it, Write Mask at its
     * foot; {@code bottom} is the pane's.
     */
    private void layoutMasks(final LithographyLook look, final int x, final int y, final int width, final int bottom) {
        final int mx = x + MASKS_X;
        final int mw = width - MASKS_X - PAD;
        final int top = y + GROUP_TOP;
        if (tab >= kinds().size()) {
            tab = 0;
        }
        tabs.setSelected(tab);
        tabs.setBounds(mx, top, mw, TAB_H);
        final int listTop = top + TAB_H + LIST_TOP;
        list.setBounds(mx + LIST_INSET, listTop, mw - LIST_INSET * 2, bottom - BUTTON_FOOT - 6 - listTop);
        final List<LithographyLook.Pick> shown = shownPicks();
        if (currentPick() == null && !shown.isEmpty()) {
            picked = shown.getFirst().mask();
        }
        list.setSelected(indexOfPicked(shown));
        write.setBounds(mx + mw - LIST_INSET - BUTTON_W, bottom - BUTTON_FOOT, BUTTON_W, BUTTON_H);
        write.setEnabled(!look.writes());
    }

    /* The status bar: what was refused, the writing and how far it is, or what Write Mask would do. */
    private void renderStatus(final GuiGraphics g, final Font font, final LithographyLook look, final int x,
                              final int y, final int width) {
        skin.statusBar(g, x, y, width, STATUS_H);
        final String said = said();
        final String line;
        if (!said.isEmpty()) {
            line = said;
        } else if (look.stations() == 0) {
            line = "";
        } else if (look.writes()) {
            line = GameText.resolve(LithoWords.making(look).with(look.writing()));
            skin.progress(g, x + width - BAR_W - PAD, y + 2, BAR_W, STATUS_H - 4,
                    look.writingPermille() / PERMILLE);
        } else if (look.blank()) {
            final LithographyLook.Pick pick = currentPick();
            line = pick == null ? "" : GameText.resolve(LithoWords.blankHint(look).with(pick.name()));
        } else if (look.masked()) {
            line = GameText.resolve(LithoWords.toolHint(look).with(look.slot()));
        } else {
            line = GameText.resolve(LithoWords.emptyHint(look));
        }
        final int room = width - PAD * 2 - (look.writes() ? BAR_W + PAD : 0);
        Draw.text(g, font, Texts.clip(font, line, room), x + PAD, y + 4, skin.text());
    }

    private void renderPick(final GuiGraphics g, final UiContext ctx, final LithographyLook.Pick pick,
                            final int index, final int x, final int y, final int w, final int h,
                            final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, w, h, hovered, selected);
        Draw.text(g, ctx.font(), Texts.clip(ctx.font(), GameText.resolve(pick.name()), w - 8), x + 4, y + 2,
                skin.listRowText(selected));
    }

    private void pick(final int index) {
        final List<LithographyLook.Pick> shown = shownPicks();
        if (index >= 0 && index < shown.size()) {
            picked = shown.get(index).mask();
        }
    }

    private @Nullable LithographyLook.Pick currentPick() {
        final List<LithographyLook.Pick> shown = shownPicks();
        final int at = indexOfPicked(shown);
        return at < 0 ? null : shown.get(at);
    }

    private int indexOfPicked(final List<LithographyLook.Pick> shown) {
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).mask().equals(picked)) {
                return i;
            }
        }
        return -1;
    }

    /* The masks of the kind shown. */
    private List<LithographyLook.Pick> shownPicks() {
        final List<String> kinds = kinds();
        if (state == null || kinds.isEmpty()) {
            return List.of();
        }
        final String kind = kinds.get(Math.min(tab, kinds.size() - 1));
        return state.look().picks().stream().filter(pick -> pick.kind().equals(kind)).toList();
    }

    /* The kinds the station writes masks of, in the order of the program's lists, as the picks come. */
    private List<String> kinds() {
        if (state == null) {
            return List.of();
        }
        return state.look().picks().stream().map(LithographyLook.Pick::kind).distinct().toList();
    }

    private List<String> tabLabels() {
        final List<String> out = new ArrayList<>();
        for (final String kind : kinds()) {
            out.add(GameText.resolve(LithoWords.kind(kind)));
        }
        return out;
    }

    private List<ContextMenu.Item> fileMenu() {
        return List.of(new ContextMenu.Item(GameText.resolve(LithographyTexts.EXIT), true,
                () -> ActiveDesktop.closeWindowFor(this)));
    }

    private List<ContextMenu.Item> stationMenu() {
        final int stations = state == null ? 0 : state.look().stations();
        final List<ContextMenu.Item> out = new ArrayList<>();
        for (int i = 0; i < stations; i++) {
            final int which = i;
            out.add(new ContextMenu.Item(GameText.resolve(LithographyTexts.STATION_N.with(i + 1)), true, () -> {
                station = which;
                picked = null;
                request(LithographyRequestPayload.LOOK, null);
            }));
        }
        return out;
    }

    private List<ContextMenu.Item> helpMenu() {
        return List.of(new ContextMenu.Item(GameText.resolve(LithographyTexts.ABOUT), false, () -> { }));
    }

    private static String[] row(final TextKey label, final Text value) {
        return new String[] {GameText.resolve(label), GameText.resolve(value)};
    }
}
