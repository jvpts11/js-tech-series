/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gateway.GatewayName;
import dev.jstech.computers.operation.payload.GatewayManagerActionPayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.Detail;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireComputer;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireGateway;
import dev.jstech.computers.operation.payload.GatewayManagerStatePayload.WireLog;
import dev.jstech.computers.operation.payload.RequestGatewayManagerPayload;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.CellGrid;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Gateway Manager: the host computer's front for the Network Gateways on its ports. The rail on the
 * left lists every Gateway linked to this computer, with Rename and Identify; the pane on the right shows
 * the selected one in five tabs. Status: this side and the ComputerCraft side as two cards, how CC names
 * the Gateway, its buffer and the last requests. Permissions: what CC may do through it. Computers: the
 * CC computers it knows. Shares: the folders CC sees. Log: the last forty things it did. Every click is an
 * action the server answers with a fresh state; nothing here decides anything on its own.
 */
public final class GatewayManagerApp implements IDesktopApp {

    public static final String TITLE = "Gateway Manager";
    private static final String NETWORK_PROGRAM = "Network";
    private static final String[] TABS = {"Status", "Permissions", "Computers", "Log"};
    private static final String[] CEILING_LABELS = {"LOW", "MEDIUM", "HIGH"};
    private static final String[] CAP_LABELS = {"4", "8", "16"};

    private static final int TAB_H = 13;
    private static final int RAIL_W = 104;
    private static final int ROW_H = 11;
    /** A rail row: the name, where the Gateway stands, how it is linked. */
    private static final int RAIL_ROW_H = ROW_H * 3;
    private static final int PAD = 4;
    private static final int GAP = 6;
    private static final int BTN_H = 12;
    private static final int CELL = 18;
    private static final int STATUS_H = 11;
    private static final int CARD_LINE_H = 9;
    private static final float CARD_SCALE = 0.75f;
    private static final float BIG_SCALE = 0.8f;
    private static final int CARD_H = CARD_LINE_H * 6 + PAD;
    /** A permission with choices takes two lines: the words, then the choices under them at the right. */
    private static final int KNOB_ROW_H = 26;
    /** A log entry takes two lines: when, who and how it went, then what was asked in full. */
    private static final int LOG_ROW_H = ROW_H * 2;
    private static final int CHOICE_W = 60;
    private static final int POPUP_W = 220;
    private static final int POPUP_H = 62;
    private static final int REFRESH_TICKS = 40;
    private static final int GREEN = 0xFF2A9D4A;
    private static final int AMBER = 0xFFB5781A;
    private static final int RED = 0xFFC0392B;
    private static final int JSC_EDGE = 0xFF316AC5;
    private static final int CC_EDGE = 0xFFE0563A;

    private static GatewayManagerApp active;

    private final BlockPos host;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private GatewayManagerStatePayload state;
    private long selected;
    private int tab;
    private int frames;
    private int lastX;
    private int lastY;
    private int lastW;
    private int lastH;
    private int lastMouseX;
    private int lastMouseY;
    /* The two cards' rectangles as last laid out, drawn by hand around their labels. */
    private int cardY;
    private int cardW;

    // components
    private final Panel root = new Panel();
    private final Label railHeader;
    private final Label emptyRail;
    private final ListView<WireGateway> rail;
    private final Button renameButton;
    private final Button identifyButton;
    private final TabStrip tabs;
    private final Label loadingLabel;
    private final Label placeholder;
    // Status
    private final Label jsTitle;
    private final Label jsBig;
    private final Label[] jsLines = new Label[4];
    private final Label ccTitle;
    private final Label ccBig;
    private final Label[] ccLines = new Label[4];
    private final Label namesLabel;
    private final Label bufferCaption;
    private final CellGrid bufferGrid;
    private final Button clearBuffer;
    private final Label recentCaption;
    private final ListView<WireLog> recentList;
    private final Label noRecent;
    private final Button openLog;
    private final Button openNetwork;
    // Permissions
    private final Checkbox readBox;
    private final Checkbox operationsBox;
    private final Label ceilingLabel;
    private final Label capLabel;
    private final Button[] ceilingChoice = new Button[3];
    private final Button[] capChoice = new Button[3];
    // Computers
    private final ColumnHeader computerColumns;
    private final ListView<WireComputer> computerList;
    private final Label noComputers;
    private final Button turnOn;
    private final Button reboot;
    private final Button shutdown;
    private final Button testEvent;
    // Log
    private final ColumnHeader logColumns;
    private final ListView<WireLog> logList;
    private final Label noLog;
    // Rename
    private final Popup renamePopup;
    private final Label renameHint;
    private final TextField renameField;
    private final Button renameApply;
    private final Button renameCancel;

    /** The rename dialog: Enter applies the name, as the button does. */
    private final class RenamePopup extends Popup {

        RenamePopup() {
            super("RENAME GATEWAY", POPUP_W, POPUP_H);
        }

        @Override
        public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                applyRename();
                return true;
            }
            return super.keyPressed(key, scanCode, modifiers);
        }
    }

    public GatewayManagerApp(final BlockPos host) {
        this.host = host;

        railHeader = root.add(new Label(() -> "ON THIS COMPUTER · " + gateways().size(), Label.Tone.DIM).setScale(CARD_SCALE));
        emptyRail = root.add(new Label("none linked yet", Label.Tone.DIM).setScale(CARD_SCALE));
        rail = root.add(new ListView<WireGateway>(this::gateways, RAIL_ROW_H, this::renderRailRow).setOnClick(this::railClicked));
        renameButton = root.add(new Button("Rename", this::openRename));
        identifyButton = root.add(new Button("Identify", () -> act(GatewayManagerActionPayload.ACTION_IDENTIFY)));

        // Five labels fitted with six pixels each fill the pane at the smallest width without the last one falling off.
        tabs = root.add(new TabStrip(List.of(TABS)).fitToLabels(6).setOnSelect(this::selectTab));
        loadingLabel = root.add(new Label("Asking the computer ...", Label.Tone.DIM));
        placeholder = root.add(new Label("Select a Gateway on the left.", Label.Tone.DIM));

        jsTitle = root.add(new Label("THIS SIDE", Label.Tone.DIM).setScale(CARD_SCALE));
        jsBig = root.add(new Label(this::jsBigText).setScale(BIG_SCALE));
        jsLines[0] = root.add(new Label(() -> detail() == null ? "" : cap(detail().link()), Label.Tone.TEXT).setScale(CARD_SCALE));
        jsLines[1] = root.add(new Label(() -> detail() == null ? ""
                : detail().types() + plural(detail().types(), " type") + ", " + detail().servers()
                + plural(detail().servers(), " server")).setScale(CARD_SCALE));
        jsLines[2] = root.add(new Label(() -> detail() == null ? "" : detail().mainframeOnline() ? "Mainframe online" : "Mainframe offline")
                .setColor(() -> detail() != null && detail().mainframeOnline() ? skin.text() : AMBER).setScale(CARD_SCALE));
        jsLines[3] = root.add(new Label(() -> detail() == null ? ""
                : "Budget: " + (detail().budgetPermille() / 10) + "% of a tick", Label.Tone.DIM).setScale(CARD_SCALE));
        ccTitle = root.add(new Label("COMPUTERCRAFT SIDE", Label.Tone.DIM).setScale(CARD_SCALE));
        ccBig = root.add(new Label(this::ccBigText).setScale(BIG_SCALE));
        ccLines[0] = root.add(new Label(() -> detail() == null || !ccInstalled() ? ""
                : detail().wiredComputers() + plural(detail().wiredComputers(), " computer") + ", "
                + detail().wiredDevices() + plural(detail().wiredDevices(), " device")).setScale(CARD_SCALE));
        ccLines[1] = root.add(new Label(() -> detail() == null || !ccInstalled() ? ""
                : "Served: " + detail().calls() + plural(detail().calls(), " call")).setScale(CARD_SCALE));
        ccLines[2] = root.add(new Label(() -> detail() == null || !ccInstalled() ? ""
                : detail().operations() + plural(detail().operations(), " operation") + " this minute")
                .setScale(CARD_SCALE));
        ccLines[3] = root.add(new Label(() -> "").setScale(CARD_SCALE));
        namesLabel = root.add(new Label(this::namesText, Label.Tone.DIM).setScale(CARD_SCALE));
        bufferCaption = root.add(new Label(() -> "BUFFER " + bufferUsed() + " of " + GatewayManagerStatePayload.BUFFER_SLOTS,
                Label.Tone.DIM).setScale(CARD_SCALE));
        bufferGrid = root.add(new CellGrid(GatewayManagerStatePayload.BUFFER_SLOTS, 1, 1, CELL)
                .setWells(true).setCellCount(GatewayManagerStatePayload.BUFFER_SLOTS)
                .setRenderer(this::renderBufferCell).setTooltip(this::bufferTooltip));
        clearBuffer = root.add(new Button("Clear buffer to network", () -> act(GatewayManagerActionPayload.ACTION_CLEAR_BUFFER)));
        recentCaption = root.add(new Label("RECENT", Label.Tone.DIM).setScale(CARD_SCALE));
        recentList = root.add(new ListView<WireLog>(this::recent, LOG_ROW_H, this::renderLogRow));
        noRecent = root.add(new Label("nothing served yet", Label.Tone.DIM).setScale(CARD_SCALE));
        openLog = root.add(new Button("Open the log", () -> selectTab(3)));
        openNetwork = root.add(new Button("Open Network", () -> DesktopScreen.requestOpen(NETWORK_PROGRAM)));

        readBox = root.add(new Checkbox(() -> "Read the network: types, totals, servers, watches",
                () -> detail() != null && detail().read(),
                () -> act(GatewayManagerActionPayload.ACTION_SET_READ, detail() != null && detail().read() ? 0 : 1))
                .setLabelScale(CARD_SCALE));
        operationsBox = root.add(new Checkbox(() -> "Operations: pull, push, craft, cancel, run",
                () -> detail() != null && detail().operationsAllowed(),
                () -> act(GatewayManagerActionPayload.ACTION_SET_OPERATIONS, detail() != null && detail().operationsAllowed() ? 0 : 1))
                .setLabelScale(CARD_SCALE));
        ceilingLabel = root.add(new Label("Priority ceiling for CC requests").setScale(CARD_SCALE));
        capLabel = root.add(new Label("Calls per tick from CC, paid from this budget").setScale(CARD_SCALE));
        for (int i = 0; i < 3; i++) {
            final int index = i;
            ceilingChoice[i] = root.add(new Button(CEILING_LABELS[i], () -> act(GatewayManagerActionPayload.ACTION_SET_CEILING, index))
                    .setLabelScale(CARD_SCALE));
            capChoice[i] = root.add(new Button(CAP_LABELS[i], () -> act(GatewayManagerActionPayload.ACTION_SET_CAP, index))
                    .setLabelScale(CARD_SCALE));
        }

        computerColumns = root.add(new ColumnHeader(List.of("ID", "LABEL", "STATE", "AGENT", "LAST SEEN")).setSortable(false));
        computerList = root.add(new ListView<WireComputer>(this::computers, ROW_H, this::renderComputerRow));
        noComputers = root.add(new Label(this::noComputersText, Label.Tone.DIM).setScale(CARD_SCALE));
        turnOn = root.add(new Button("Turn on", () -> act(GatewayManagerActionPayload.ACTION_TURN_ON)));
        reboot = root.add(new Button("Reboot", () -> act(GatewayManagerActionPayload.ACTION_REBOOT)));
        shutdown = root.add(new Button("Shutdown", () -> act(GatewayManagerActionPayload.ACTION_SHUTDOWN)));
        testEvent = root.add(new Button("Test event", () -> act(GatewayManagerActionPayload.ACTION_TEST_EVENT)));

        logColumns = root.add(new ColumnHeader(List.of("WHEN", "WHO", "RESULT")).setSortable(false));
        logList = root.add(new ListView<WireLog>(this::log, LOG_ROW_H, this::renderLogRow));
        noLog = root.add(new Label("this Gateway has done nothing yet", Label.Tone.DIM).setScale(CARD_SCALE));

        renamePopup = new RenamePopup().setLayouter(this::layoutRenamePopup);
        renameHint = renamePopup.add(new Label("Letters, digits, dashes; empty goes back to the default.", Label.Tone.DIM)
                .setScale(CARD_SCALE));
        renameField = renamePopup.add(new TextField(GatewayName.MAX));
        renameApply = renamePopup.add(new Button("APPLY", this::applyRename).setPrimary(true));
        renameCancel = renamePopup.add(new Button("CANCEL", renamePopup::close));

        active = this;
        request();
    }

    /** Routes a state from the server to the open window. */
    public static void accept(final GatewayManagerStatePayload payload) {
        if (active == null) {
            return;
        }
        active.state = payload;
        if (payload.detail().present()) {
            active.selected = payload.detail().pos();
        } else if (!payload.gateways().isEmpty()) {
            /*
             * Nothing selected, or the selected Gateway is gone: the first one on the rail takes its place, so
             * the window always opens on something rather than on an empty pane.
             */
            active.selected = payload.gateways().get(0).pos();
            active.request();
        } else {
            active.selected = 0L;
        }
    }

    private void request() {
        PacketDistributor.sendToServer(new RequestGatewayManagerPayload(host, selected));
    }

    @Override
    public void onRestored() {
        active = this;
        request();
    }

    private void act(final int action) {
        act(action, 0);
    }

    private void act(final int action, final int value) {
        if (selected != 0L) {
            PacketDistributor.sendToServer(GatewayManagerActionPayload.valued(host, selected, action, value));
        }
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public String title() {
        return TITLE;
    }

    // Wide enough for the five tabs and the two cards to sit whole beside the rail.
    @Override
    public int defaultWidth() {
        return 350;
    }

    @Override
    public int defaultHeight() {
        return 240;
    }

    @Override
    public int minWidth() {
        return 340;
    }

    @Override
    public int minHeight() {
        return 180;
    }

    // state readers

    @Nullable
    private Detail detail() {
        return state != null && state.detail().present() && state.detail().pos() == selected ? state.detail() : null;
    }

    private boolean ccInstalled() {
        return state != null && state.head().ccInstalled();
    }

    private List<WireGateway> gateways() {
        return state == null ? List.of() : state.gateways();
    }

    private List<WireLog> recent() {
        return detail() == null ? List.of() : detail().recent();
    }

    private List<WireComputer> computers() {
        return detail() == null ? List.of() : detail().computers();
    }

    private List<WireLog> log() {
        return detail() == null ? List.of() : detail().log();
    }

    private int bufferUsed() {
        int used = 0;
        if (detail() != null) {
            for (final ItemStack stack : detail().buffer()) {
                if (!stack.isEmpty()) {
                    used++;
                }
            }
        }
        return used;
    }

    private String jsBigText() {
        if (detail() == null || state == null) {
            return "";
        }
        return detail().link().startsWith("not") ? "Not linked" : "Linked to " + state.head().hostName();
    }

    private String ccBigText() {
        if (state == null) {
            return "";
        }
        return ccInstalled() ? "CC: Tweaked " + state.head().ccVersion() : "CC: Tweaked is not installed";
    }

    private String namesText() {
        final Detail d = detail();
        if (d == null) {
            return "";
        }
        return "On CC: " + d.peripheralName() + " · rednet: "
                + (d.rednetId() < 0 ? "none yet" : String.valueOf(d.rednetId()));
    }

    private String noComputersText() {
        return ccInstalled() ? "no ComputerCraft computer is attached to this Gateway" : "CC: Tweaked is not installed";
    }

    private static String plural(final int n, final String word) {
        return n == 1 ? word : word + "s";
    }

    private static String cap(final String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // selection

    private void selectTab(final int target) {
        tab = Math.max(0, Math.min(TABS.length - 1, target));
        tabs.setSelected(tab);
        recentList.setScroll(0);
        computerList.setScroll(0);
        logList.setScroll(0);
    }

    private void railClicked(final int index, final int button, final double mx, final double my) {
        final List<WireGateway> list = gateways();
        if (button == 0 && index >= 0 && index < list.size()) {
            selected = list.get(index).pos();
            request();
        }
    }

    private void openRename() {
        final Detail d = detail();
        if (d == null) {
            return;
        }
        renameField.set(d.name());
        renamePopup.open();
        renamePopup.placeIn(lastX, lastY, lastW, lastH);
        renamePopup.focus(renameField);
        // The whole name is selected, so typing a new one replaces it instead of adding to it.
        renameField.selectAll();
    }

    private void applyRename() {
        if (selected != 0L) {
            String typed = renameField.edit().strip();
            if (typed.length() > GatewayManagerActionPayload.MAX_TEXT) {
                typed = typed.substring(0, GatewayManagerActionPayload.MAX_TEXT);
            }
            PacketDistributor.sendToServer(new GatewayManagerActionPayload(host, selected,
                    GatewayManagerActionPayload.ACTION_RENAME, 0, typed));
        }
        renamePopup.close();
    }

    // rendering

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        lastX = x;
        lastY = y;
        lastW = width;
        lastH = height;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        if (++frames % REFRESH_TICKS == 0) {
            request();
        }
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        layout(font, x, y, width, height);

        final int bodyBottom = y + height - STATUS_H;
        // The rail's header band and its right edge.
        g.fill(x, y, x + RAIL_W - 1, y + ROW_H, skin.panelBg());
        g.fill(x + RAIL_W - 1, y, x + RAIL_W, bodyBottom, skin.edge());
        if (tab == 0 && detail() != null) {
            // The two cards: a panel each, with the side's colour along the top edge.
            final int dx = x + RAIL_W + PAD;
            skin.panel(g, dx, cardY, cardW, CARD_H);
            g.fill(dx, cardY, dx + cardW, cardY + 2, JSC_EDGE);
            skin.panel(g, dx + cardW + PAD, cardY, cardW, CARD_H);
            g.fill(dx + cardW + PAD, cardY, dx + cardW * 2 + PAD, cardY + 2, CC_EDGE);
            final int lit = jsBig.y() + 2;
            g.fill(dx + PAD, lit, dx + PAD + 4, lit + 4, detail().link().startsWith("not") ? RED : GREEN);
            g.fill(dx + cardW + PAD * 2, lit, dx + cardW + PAD * 2 + 4, lit + 4,
                    !ccInstalled() ? RED : detail().ccOnline() ? GREEN : AMBER);
        }
        root.render(g, ctx);
        renderStatusBar(g, font, x, bodyBottom, width);
    }

    private void renderStatusBar(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        skin.statusBar(g, x, y, width, STATUS_H);
        if (state == null) {
            return;
        }
        final int count = state.gateways().size();
        final String fleet = count + plural(count, " gateway") + ", " + state.head().ccReachable() + " with CC reachable";
        final String load = state.head().callsThisMinute() + plural(state.head().callsThisMinute(), " call") + " this minute";
        final String selection = detail() == null ? "nothing selected" : detail().name() + " selected";
        final int ty = y + 2;
        g.fill(x + PAD, ty + 1, x + PAD + 4, ty + 5, state.head().ccReachable() > 0 ? GREEN : skin.dim());
        Texts.scaled(g, font, fleet, x + PAD + 7, ty, CARD_SCALE, skin.text());
        final int loadX = x + PAD + 7 + Texts.smallWidth(font, fleet) + GAP * 2;
        Texts.scaled(g, font, load, loadX, ty, CARD_SCALE, skin.dim());
        final int selW = (int) (font.width(selection) * CARD_SCALE);
        Texts.scaled(g, font, selection, x + width - PAD - selW, ty, CARD_SCALE, skin.dim());
    }

    /** Places every component from the content rectangle and the state; hides what the tab has no use for. */
    private void layout(final Font font, final int x, final int y, final int width, final int height) {
        final int bodyBottom = y + height - STATUS_H;
        final boolean ready = state != null;
        final Detail d = detail();

        railHeader.setBounds(x + PAD, y + 2, RAIL_W - PAD * 2, 8);
        final boolean noGateways = gateways().isEmpty();
        emptyRail.setVisible(ready && noGateways);
        emptyRail.setBounds(x + PAD, y + ROW_H + 2, RAIL_W - PAD * 2, 8);
        rail.setVisible(ready && !noGateways);
        rail.setBounds(x, y + ROW_H, RAIL_W - 1, bodyBottom - (y + ROW_H) - BTN_H - PAD * 2);
        final int railBtnW = (RAIL_W - 1 - PAD * 3) / 2;
        renameButton.setBounds(x + PAD, bodyBottom - BTN_H - PAD, railBtnW, BTN_H);
        identifyButton.setBounds(x + PAD * 2 + railBtnW, bodyBottom - BTN_H - PAD, railBtnW, BTN_H);
        renameButton.setEnabled(d != null);
        identifyButton.setEnabled(d != null);

        final int dx = x + RAIL_W;
        final int dw = width - RAIL_W;
        tabs.setBounds(dx, y, dw, TAB_H);
        tabs.setSelected(tab);
        final int top = y + TAB_H + 2;
        loadingLabel.setVisible(!ready);
        loadingLabel.setBounds(dx + PAD, top + PAD, dw - PAD * 2, 8);
        placeholder.setVisible(ready && d == null);
        placeholder.setBounds(dx + PAD, top + PAD, dw - PAD * 2, 8);

        final boolean status = d != null && tab == 0;
        final boolean permissions = d != null && tab == 1;
        final boolean computersTab = d != null && tab == 2;
        final boolean logTab = d != null && tab == 3;
        for (final UiComponent c : List.of(jsTitle, jsBig, jsLines[0], jsLines[1], jsLines[2], jsLines[3], ccTitle, ccBig,
                ccLines[0], ccLines[1], ccLines[2], ccLines[3], namesLabel, bufferCaption, bufferGrid, clearBuffer,
                recentCaption, recentList, noRecent, openLog, openNetwork)) {
            c.setVisible(status);
        }
        for (final UiComponent c : List.of(readBox, operationsBox, ceilingLabel, capLabel)) {
            c.setVisible(permissions);
        }
        for (int i = 0; i < 3; i++) {
            ceilingChoice[i].setVisible(permissions);
            capChoice[i].setVisible(permissions);
        }
        for (final UiComponent c : List.of(computerColumns, computerList, noComputers, turnOn, reboot, shutdown, testEvent)) {
            c.setVisible(computersTab);
        }
        for (final UiComponent c : List.of(logColumns, logList, noLog)) {
            c.setVisible(logTab);
        }
        if (d == null) {
            return;
        }

        final int px = dx + PAD;
        final int pw = dw - PAD * 2;
        final int bottom = bodyBottom - PAD;
        if (status) {
            cardY = top;
            cardW = (pw - PAD) / 2;
            layoutCard(px, cardY, cardW, jsTitle, jsBig, jsLines);
            layoutCard(px + cardW + PAD, cardY, cardW, ccTitle, ccBig, ccLines);
            int cy = cardY + CARD_H + 3;
            namesLabel.setBounds(px, cy, pw, 8);
            cy += ROW_H;
            final int clearW = font.width("Clear buffer to network") + 12;
            clearBuffer.setBounds(px + pw - clearW, cy - 2, clearW, BTN_H);
            clearBuffer.setEnabled(bufferUsed() > 0);
            bufferCaption.setBounds(px, cy, pw - clearW - GAP, 8);
            cy += BTN_H;
            bufferGrid.setColumns(GatewayManagerStatePayload.BUFFER_SLOTS).setVisibleRows(1).setTotalRows(1)
                    .setCellCount(GatewayManagerStatePayload.BUFFER_SLOTS).place(px, cy);
            cy += CELL + 3;
            recentCaption.setBounds(px, cy, pw, 8);
            cy += ROW_H - 2;
            final int by = bottom - BTN_H;
            // Whole entries only: a row cut in half reads as a bug.
            final int rows = Math.max(1, (by - PAD - cy) / LOG_ROW_H);
            recentList.setBounds(dx, cy, dw, rows * LOG_ROW_H);
            recentList.setVisible(!recent().isEmpty());
            noRecent.setVisible(recent().isEmpty());
            noRecent.setBounds(px, cy + 1, pw, 8);
            final int bw = (pw - PAD) / 2;
            openLog.setBounds(px, by, bw, BTN_H);
            openNetwork.setBounds(px + bw + PAD, by, bw, BTN_H);
            openLog.setVisible(openLog.y() >= recentList.y() + LOG_ROW_H);
            openNetwork.setVisible(openLog.visible());
        } else if (permissions) {
            final int rowH = 16;
            int ry = top + 2;
            readBox.setBounds(px, ry, pw, rowH);
            ry += rowH;
            operationsBox.setBounds(px, ry, pw, rowH);
            ry += rowH;
            layoutChoice(px, ry, pw, ceilingLabel, ceilingChoice, d.ceiling());
            ry += KNOB_ROW_H;
            layoutChoice(px, ry, pw, capLabel, capChoice, d.cap());
        } else if (computersTab) {
            final int c0 = px;
            final int c1 = c0 + font.width("000") + GAP;
            final int c4 = dx + dw - PAD - font.width("00 min ago") - 2;
            final int c3 = c4 - font.width("answering") - GAP;
            final int c2 = c3 - font.width("STATE") - GAP - 4;
            computerColumns.setBounds(dx, top, dw, ROW_H);
            computerColumns.setColumnX(c0, c1, c2, c3, c4);
            final int by = bottom - BTN_H;
            computerList.setBounds(dx, top + ROW_H, dw, Math.max(ROW_H, by - PAD - (top + ROW_H)));
            computerList.setVisible(!computers().isEmpty());
            noComputers.setVisible(computers().isEmpty());
            noComputers.setBounds(px, top + ROW_H + 1, pw, 8);
            final int testW = font.width("Test event") + 10;
            final int bw = (pw - testW - PAD * 3) / 3;
            turnOn.setBounds(px, by, bw, BTN_H);
            reboot.setBounds(px + bw + PAD, by, bw, BTN_H);
            shutdown.setBounds(px + (bw + PAD) * 2, by, bw, BTN_H);
            testEvent.setBounds(px + pw - testW, by, testW, BTN_H);
            /*
             * Turning a computer over there on and off is the ComputerCraft side's own doing, so the
             * buttons need nothing but the mod being there and a computer picked.
             */
            final boolean reachable = ccInstalled() && !d.computers().isEmpty();
            turnOn.setEnabled(reachable);
            reboot.setEnabled(reachable);
            shutdown.setEnabled(reachable);
            testEvent.setEnabled(ccInstalled());
        } else if (logTab) {
            logColumns.setBounds(dx, top, dw, ROW_H);
            logColumns.setColumnX(logWhenX(), logWhoX(), logResultX(font));
            final int rows = Math.max(1, (bottom - (top + ROW_H)) / LOG_ROW_H);
            logList.setBounds(dx, top + ROW_H, dw, rows * LOG_ROW_H);
            logList.setVisible(!log().isEmpty());
            noLog.setVisible(log().isEmpty());
            noLog.setBounds(px, top + ROW_H + 1, pw, 8);
        }
    }

    /* The log's columns, shared by the Log tab and the Status tab's recent list: when, who, and the result at the right. */

    private int logWhenX() {
        return lastX + RAIL_W + PAD;
    }

    private int logWhoX() {
        return logWhenX() + 44;
    }

    private int logResultX(final Font font) {
        // Room for the longest result the Gateway writes, "40 items in 12 operations", at the small font.
        return lastX + lastW - PAD - Math.round(font.width("40 items in 12 operations") * CARD_SCALE);
    }

    private static void layoutCard(final int cx, final int cy, final int cw, final Label title, final Label big,
                                   final Label[] lines) {
        int ly = cy + PAD;
        title.setBounds(cx + PAD, ly, cw - PAD * 2, 8);
        ly += CARD_LINE_H;
        big.setBounds(cx + PAD + 7, ly, cw - PAD * 2 - 7, 8);
        ly += CARD_LINE_H + 1;
        for (final Label line : lines) {
            line.setBounds(cx + PAD, ly, cw - PAD * 2, 8);
            ly += CARD_LINE_H;
        }
    }

    private static void layoutChoice(final int px, final int ry, final int pw, final Label label,
                                     final Button[] choice, final int chosen) {
        final int cw = CHOICE_W;
        final int groupW = cw * choice.length + 2 * (choice.length - 1);
        label.setBounds(px, ry + 1, pw, 8);
        int bx = px + pw - groupW;
        for (int i = 0; i < choice.length; i++) {
            choice[i].setBounds(bx, ry + 11, cw, BTN_H);
            choice[i].setPrimary(i == chosen);
            bx += cw + 2;
        }
    }

    private void renderRailRow(final GuiGraphics g, final UiContext ctx, final WireGateway gw, final int index,
                               final int x, final int y, final int w, final int h, final boolean hovered,
                               final boolean selectedRow) {
        final boolean sel = gw.pos() == selected;
        if (sel || hovered) {
            g.fill(x, y, x + w, y + h, sel ? ctx.skin().fieldBg() : ctx.skin().panelBg());
        }
        if (sel) {
            g.fill(x, y, x + 2, y + h, ctx.skin().accent());
        }
        final Font font = ctx.font();
        g.fill(x + w - 14, y + 3, x + w - 10, y + 7, gw.linked() ? GREEN : RED);
        g.fill(x + w - 8, y + 3, x + w - 4, y + 7, gw.ccLinked() ? GREEN : ctx.skin().dim());
        g.drawString(font, Texts.clip(font, gw.name(), w - 22), x + PAD, y + 2, ctx.skin().text(), false);
        // "at x, y, z · how it is linked": the place on one line, the link on the next.
        final int split = gw.where().indexOf(" · ");
        final String place = split < 0 ? gw.where() : gw.where().substring(0, split);
        final String link = split < 0 ? "" : gw.where().substring(split + 3);
        final int fits = (int) ((w - PAD * 2) / CARD_SCALE);
        Texts.scaled(g, font, Texts.clip(font, place, fits), x + PAD, y + ROW_H + 2, CARD_SCALE, ctx.skin().dim());
        Texts.scaled(g, font, Texts.clip(font, link, fits), x + PAD, y + ROW_H * 2 + 1, CARD_SCALE, ctx.skin().dim());
    }

    private void renderBufferCell(final GuiGraphics g, final UiContext ctx, final int index, final int cx, final int cy,
                                  final int w, final int h, final boolean hovered) {
        final Detail d = detail();
        if (d != null && index < d.buffer().size() && !d.buffer().get(index).isEmpty()) {
            final ItemStack stack = d.buffer().get(index);
            DesktopItems.itemWithCount(g, ctx.font(), stack, cx + 1, cy + 1, String.valueOf(stack.getCount()));
        }
    }

    private List<Component> bufferTooltip(final int index) {
        final Detail d = detail();
        if (d == null || index >= d.buffer().size() || d.buffer().get(index).isEmpty()) {
            return List.of();
        }
        final ItemStack stack = d.buffer().get(index);
        return List.of(Component.literal(stack.getCount() + " x " + stack.getHoverName().getString()));
    }

    /**
     * One log entry on two lines at the small font: when, who and how it went on the first, what was asked in
     * full on the second, so a long request is never cut to fit a column.
     */
    private void renderLogRow(final GuiGraphics g, final UiContext ctx, final WireLog row, final int index, final int x,
                              final int y, final int w, final int h, final boolean hovered, final boolean selectedRow) {
        ctx.skin().listRow(g, x + 2, y, w - 4, h, hovered, false);
        final Font font = ctx.font();
        final int c0 = logWhenX();
        final int c1 = logWhoX();
        final int c2 = logResultX(font);
        final int fits = (int) ((x + w - PAD - c0) / CARD_SCALE);
        Texts.scaled(g, font, row.when(), c0, y + 2, CARD_SCALE, ctx.skin().dim());
        Texts.scaled(g, font, Texts.clip(font, row.who(), (int) ((c2 - c1 - GAP) / CARD_SCALE)), c1, y + 2, CARD_SCALE,
                ctx.skin().text());
        Texts.scaled(g, font, Texts.clip(font, row.result(), (int) ((x + w - PAD - c2) / CARD_SCALE)), c2, y + 2,
                CARD_SCALE, toneColor(row.tone(), ctx));
        Texts.scaled(g, font, Texts.clip(font, row.what(), fits), c0, y + ROW_H + 1, CARD_SCALE, ctx.skin().text());
    }

    private static int toneColor(final int tone, final UiContext ctx) {
        return switch (tone) {
            case 1 -> AMBER;
            case 2 -> RED;
            default -> GREEN;
        };
    }

    private void renderComputerRow(final GuiGraphics g, final UiContext ctx, final WireComputer c, final int index,
                                   final int x, final int y, final int w, final int h, final boolean hovered,
                                   final boolean selectedRow) {
        ctx.skin().listRow(g, x + 2, y, w - 4, h, hovered, false);
        final Font font = ctx.font();
        final int ty = y + 1;
        g.drawString(font, String.valueOf(c.id()), computerColumns.columnX(0), ty, ctx.skin().text(), false);
        g.drawString(font, Texts.clip(font, c.label().isEmpty() ? "(no label)" : c.label(),
                computerColumns.columnX(2) - computerColumns.columnX(1) - GAP), computerColumns.columnX(1), ty,
                c.label().isEmpty() ? ctx.skin().dim() : ctx.skin().text(), false);
        final int sx = computerColumns.columnX(2);
        g.fill(sx, ty + 2, sx + 4, ty + 6, c.on() ? GREEN : RED);
        g.drawString(font, c.on() ? "on" : "off", sx + 6, ty, ctx.skin().text(), false);
        g.drawString(font, c.agent() ? "answering" : "none", computerColumns.columnX(3), ty,
                c.agent() ? GREEN : ctx.skin().dim(), false);
        g.drawString(font, c.lastSeen(), computerColumns.columnX(4), ty, ctx.skin().dim(), false);
    }

    // dialogs

    @Override
    public boolean modalActive() {
        return renamePopup.isOpen();
    }

    @Override
    public void renderModal(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                            final int height, final int mouseX, final int mouseY) {
        if (renamePopup.isOpen()) {
            renamePopup.renderIn(g, new UiContext(skin, font, mouseX, mouseY, 0f), x, y, width, height);
        }
    }

    private void layoutRenamePopup(final Popup p) {
        final int cy = p.contentTop();
        renameHint.setBounds(p.x() + 4, cy + 1, p.width() - 8, 8);
        renameField.setBounds(p.x() + 4, cy + 12, p.width() - 8, ROW_H + 2);
        final int bw = (p.width() - 8 - 2) / 2;
        final int by = p.bottom() - BTN_H - 4;
        renameApply.setBounds(p.x() + 4, by, bw, BTN_H);
        renameCancel.setBounds(p.x() + 4 + bw + 2, by, bw, BTN_H);
    }

    // input

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (renamePopup.isOpen()) {
            renamePopup.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (button == 0) {
            root.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (renamePopup.isOpen()) {
            renamePopup.mouseDragged(mouseX, mouseY, button);
        } else {
            root.mouseDragged(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (renamePopup.isOpen()) {
            renamePopup.mouseReleased(mouseX, mouseY, button);
        } else {
            root.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        if (renamePopup.isOpen()) {
            return true;
        }
        if (root.mouseScrolled(lastMouseX, lastMouseY, delta)) {
            return true;
        }
        final int step = delta > 0 ? -1 : 1;
        final ListView<?> table = tab == 2 ? computerList : tab == 3 ? logList : rail;
        table.setScroll(table.scroll() + step);
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return renamePopup.isOpen() && renamePopup.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c) {
        return renamePopup.isOpen() && renamePopup.charTyped(c);
    }

    @Override
    public void renderTooltip(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY) {
        final List<Component> lines = root.tooltip(mouseX, mouseY);
        if (!lines.isEmpty()) {
            g.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
    }

    // what the client tests read and click

    public boolean hasState() {
        return state != null;
    }

    public int tab() {
        return tab;
    }

    public String selectedName() {
        return detail() == null ? "" : detail().name();
    }

    public List<String> railNames() {
        final List<String> names = new ArrayList<>();
        for (final WireGateway g : gateways()) {
            names.add(g.name());
        }
        return names;
    }

    /** The selected Gateway's permissions as shown: read, operations, ceiling, cap. */
    public int[] permissionsShown() {
        final Detail d = detail();
        return d == null ? new int[0]
                : new int[]{d.read() ? 1 : 0, d.operationsAllowed() ? 1 : 0, d.ceiling(), d.cap()};
    }

    public List<String> logWhats() {
        final List<String> out = new ArrayList<>();
        for (final WireLog row : log()) {
            out.add(row.what());
        }
        return out;
    }

    public String statusLine() {
        return state == null ? "" : state.status();
    }

    public int bufferUsedShown() {
        return bufferUsed();
    }

    public int[] tabCenter(final int index) {
        return tabs.tabCenter(index);
    }

    public int[] railRowCenter(final int index) {
        return rail.rowCenter(index);
    }

    public int[] renameCenter() {
        return renameButton.center();
    }

    public int[] identifyCenter() {
        return identifyButton.center();
    }

    public int[] renameApplyCenter() {
        return renameApply.center();
    }

    public boolean renameOpen() {
        return renamePopup.isOpen();
    }

    public int[] readToggleCenter() {
        return readBox.center();
    }

    public int[] operationsToggleCenter() {
        return operationsBox.center();
    }

    public int[] ceilingChoiceCenter(final int index) {
        return ceilingChoice[index].center();
    }

    public int[] capChoiceCenter(final int index) {
        return capChoice[index].center();
    }

    public int[] clearBufferCenter() {
        return clearBuffer.center();
    }

    public int[] openLogCenter() {
        return openLog.center();
    }

    public int[] testEventCenter() {
        return testEvent.center();
    }
}
