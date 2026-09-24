/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.datacenter.LoadBalanceMode;
import dev.jstech.computers.operation.payload.ClusterManagerActionPayload;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload.Detail;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload.WireCluster;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload.WireCraft;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload.WireDest;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload.WireJob;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload.WireLane;
import dev.jstech.computers.operation.payload.ClusterManagerStatePayload.WireNode;
import dev.jstech.computers.operation.payload.ClusterMoveOutPayload;
import dev.jstech.computers.operation.payload.ClusterRenamePayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.RequestClusterManagerPayload;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.CellGrid;
import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.ProgressBar;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Cluster Manager: the Cluster Management Computer's front for every cluster on its network. Three
 * tabs (supercomputers, datacenters, AI), a list of clusters on the left and the selected one on the
 * right: its nodes with a power switch each, its craft queue or its inventory, the bulk install and
 * power actions, and the install job as it runs. Every click is an action the server answers with a
 * fresh state; nothing here decides anything on its own.
 *
 * <p>The tabs, the lists, the tables, the buttons and the three dialogs (a node, a move-out, a rename)
 * are components laid out every frame from the window's size; the job panel's lane overlay and the
 * warning bands are drawn by hand around them.
 */
public final class ClusterManagerApp implements IDesktopApp {

    private static final String[] LADDER = {"x8", "x16", "x32", "x64", "x128", "x256"};
    private static final String[] MODELS = {"5100", "7120", "7290", "9000"};
    private static final int[] PRESETS = {1, 16, 64, 256, -1};
    private static final int TAB_H = 13;
    private static final int LIST_W = 100;
    /** The dialogs' width: four buttons ("POWER OFF" the widest) side by side without clipping. */
    private static final int POPUP_W = 260;
    private static final int POPUP_H = 80;
    private static final int ROW_H = 11;
    private static final int PAD = 4;
    /** Clearance between two columns of a table, so neighbouring words never touch. */
    private static final int GAP = 6;
    private static final int BTN_H = 12;
    private static final int CELL = 18;
    private static final int SWITCH_W = 18;
    private static final int RENAME_W = 44;
    private static final int REFRESH_TICKS = 40;
    private static final int GREEN = 0xFF2A9D4A;
    private static final int AMBER = 0xFFB5781A;
    private static final int RED = 0xFFC0392B;
    private static final int WARN_BG = 0xFFFCE3A1;
    private static final int WARN_TEXT = 0xFF6B4E00;

    private static ClusterManagerApp active;

    private final BlockPos host;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private ClusterManagerStatePayload state;
    private int tab;          // 0 supercomputers, 1 datacenters, 2 AI
    private int selIndex = -1;
    private int subTab;       // supercomputers: 0 nodes, 1 map, 2 queue; datacenters: 0 servers, 1 inventory
    private int frames;
    private int lastX;
    private int lastY;
    private int lastW;
    private int lastH;
    private int lastMouseX;
    private int lastMouseY;

    // The dialogs' subjects while they are open.
    @Nullable
    private WireNode popupNode;
    @Nullable
    private NetworkItemEntry moveItem;
    private int moveQty = 64;
    private int moveDest;

    // components
    private final Panel root = new Panel();
    private final TabStrip tabs;
    private final Label loadingLabel;
    private final Label warnLabel;
    private final Label listHeader;
    private final Label emptyListLabel;
    private final ListView<WireCluster> clusterList;
    private final Label placeholder;
    private final Label nameLabel;
    private final Label pillLabel;
    private final Button renameButton;
    private final Label subLabel;
    private final Label jobLabel;
    private final ProgressBar jobBar;
    private final TabStrip scSubTabs;
    private final TabStrip dcSubTabs;
    private final ColumnHeader scNodeColumns;
    private final ColumnHeader dcNodeColumns;
    private final ListView<WireNode> nodeList;
    private final Label noNodesLabel;
    private final ColumnHeader mapColumns;
    private final ListView<Integer> slotList;
    private final Label pastLabel;
    private final ColumnHeader queueColumns;
    private final ListView<WireCraft> queueList;
    private final Label noQueueLabel;
    private final Label inventoryHint;
    private final CellGrid inventoryGrid;
    private final Label emptyInventoryLabel;
    private final Button systemAll;
    private final Button programAll;
    private final Button powerAll;
    private final Button cancelJob;
    private final Button balance;

    private final Popup nodePopup;
    private final Label nodeSystem;
    private final Label nodePrograms;
    private final Label nodeSlot;
    private final Button nodePower;
    private final Button nodeInstallSystem;
    private final Button nodeInstallProgram;
    private final Button nodeClose;

    private final Popup movePopup;
    private final Label moveQtyLabel;
    private final Button[] presetButtons = new Button[PRESETS.length];
    private final Button prevDest;
    private final Label destLabel;
    private final Button nextDest;
    private final Button moveConfirm;
    private final Button moveCancel;

    private final Popup renamePopup;
    private final Label renameHint;
    private final TextField renameField;
    private final Button renameApply;
    private final Button renameCancel;

    /** The rename dialog: Enter applies the name, as the button does. */
    private final class RenamePopup extends Popup {

        RenamePopup() {
            super(GameText.resolve(ClusterManagerTexts.RENAME_TITLE), POPUP_W, POPUP_H);
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

    public ClusterManagerApp(final BlockPos host) {
        this.host = host;

        tabs = root.add(new TabStrip(words(ClusterManagerTexts.SUPERCOMPUTERS_TAB, ClusterManagerTexts.DATACENTERS_TAB,
                ClusterManagerTexts.AI_TAB)).fitToLabels(14).setOnSelect(this::selectTab));
        loadingLabel = root.add(new Label(GameText.resolve(ClusterManagerTexts.REACHING), Label.Tone.DIM));
        warnLabel = root.add(new Label(GameText.resolve(ClusterManagerTexts.CARD_REQUIRED)).setColor(WARN_TEXT));
        listHeader = root.add(new Label(this::listHeaderText, Label.Tone.DIM));
        emptyListLabel = root.add(new Label(() -> GameText.resolve(
                tab == 2 ? ClusterManagerTexts.NONE_YET : ClusterManagerTexts.NONE_ON_NETWORK), Label.Tone.DIM));
        clusterList = root.add(new ListView<WireCluster>(this::clustersOfTab, ROW_H * 2, this::renderClusterRow)
                .setOnClick(this::clusterClicked));
        placeholder = root.add(new Label(() -> GameText.resolve(
                tab == 2 ? ClusterManagerTexts.NO_AI_CLUSTERS : ClusterManagerTexts.SELECT_ONE), Label.Tone.DIM));
        nameLabel = root.add(new Label(() -> detail() == null ? "" : detail().name()));
        pillLabel = root.add(new Label(() -> GameText.resolve(detail() != null && detail().online()
                        ? ClusterManagerTexts.ONLINE : ClusterManagerTexts.OFFLINE))
                .setColor(() -> detail() != null && detail().online() ? GREEN : RED)
                .setAlign(Label.Align.RIGHT));
        renameButton = root.add(new Button(GameText.resolve(ClusterManagerTexts.RENAME), this::openRename));
        subLabel = root.add(new Label(() -> detail() == null ? "" : GameText.resolve(detail().sub()), Label.Tone.DIM));
        jobLabel = root.add(new Label(this::jobText).setColor(WARN_TEXT));
        jobBar = root.add(new ProgressBar(this::jobPercent));
        scSubTabs = root.add(new TabStrip(words(ClusterManagerTexts.NODES_TAB, ClusterManagerTexts.MAP_TAB,
                ClusterManagerTexts.QUEUE_TAB)).fitToLabels(10).setOnSelect(this::selectSubTab));
        dcSubTabs = root.add(new TabStrip(words(ClusterManagerTexts.SERVERS_TAB, ClusterManagerTexts.INVENTORY_TAB))
                .fitToLabels(10).setOnSelect(this::selectSubTab));
        scNodeColumns = root.add(new ColumnHeader(words(ClusterManagerTexts.RACK_UNIT_COLUMN,
                ClusterManagerTexts.NODE_COLUMN, ClusterManagerTexts.SYSTEM_COLUMN, ClusterManagerTexts.STATUS_COLUMN,
                ClusterManagerTexts.PHI_COLUMN)).setSortable(false));
        dcNodeColumns = root.add(new ColumnHeader(words(ClusterManagerTexts.RACK_UNIT_COLUMN,
                ClusterManagerTexts.NODE_COLUMN, ClusterManagerTexts.SYSTEM_COLUMN, ClusterManagerTexts.STATUS_COLUMN,
                ClusterManagerTexts.USED_COLUMN)).setSortable(false));
        nodeList = root.add(new ListView<WireNode>(this::nodes, ROW_H, this::renderNodeRow).setOnClick(this::nodeClicked));
        noNodesLabel = root.add(new Label(GameText.resolve(ClusterManagerTexts.NO_NODES), Label.Tone.DIM));
        mapColumns = root.add(new ColumnHeader(words(ClusterManagerTexts.SLOT_COLUMN, ClusterManagerTexts.CRAFTS_COLUMN,
                ClusterManagerTexts.NODE_COLUMN, ClusterManagerTexts.STATE_COLUMN)).setSortable(false));
        slotList = root.add(new ListView<Integer>(() -> List.of(0, 1, 2, 3, 4, 5), ROW_H, this::renderSlotRow));
        pastLabel = root.add(new Label(this::pastText).setColor(AMBER));
        queueColumns = root.add(new ColumnHeader(words(ClusterManagerTexts.OPERATION_COLUMN,
                ClusterManagerTexts.BY_COLUMN, ClusterManagerTexts.SLOTS_COLUMN)).setSortable(false));
        queueList = root.add(new ListView<WireCraft>(this::queue, ROW_H, this::renderQueueRow));
        noQueueLabel = root.add(new Label(GameText.resolve(ClusterManagerTexts.NO_CRAFTS), Label.Tone.DIM));
        inventoryHint = root.add(new Label(this::inventoryHintText, Label.Tone.DIM));
        inventoryGrid = root.add(new CellGrid(1, 1, 1, CELL)
                .setRenderer(this::renderInventoryCell)
                .setOnClick((index, button, shift) -> openMove(index)));
        emptyInventoryLabel = root.add(new Label(GameText.resolve(ClusterManagerTexts.SECTION_EMPTY), Label.Tone.DIM));
        systemAll = root.add(new Button(GameText.resolve(ClusterManagerTexts.SYSTEM_ALL),
                () -> act(ClusterManagerActionPayload.ACTION_INSTALL_SYSTEM_ALL)));
        programAll = root.add(new Button(GameText.resolve(ClusterManagerTexts.PROGRAM_ALL),
                () -> act(ClusterManagerActionPayload.ACTION_INSTALL_PROGRAM_ALL)));
        powerAll = root.add(new Button(
                () -> GameText.resolve(allOn() ? ClusterManagerTexts.ALL_OFF : ClusterManagerTexts.ALL_ON),
                () -> act(allOn() ? ClusterManagerActionPayload.ACTION_POWER_ALL_OFF
                        : ClusterManagerActionPayload.ACTION_POWER_ALL_ON)));
        cancelJob = root.add(new Button(GameText.resolve(ClusterManagerTexts.CANCEL_JOB),
                () -> act(ClusterManagerActionPayload.ACTION_CANCEL_JOB)));
        balance = root.add(new Button(() -> GameText.resolve(ClusterManagerTexts.BALANCE.with(balanceName())),
                () -> act(ClusterManagerActionPayload.ACTION_CYCLE_BALANCE)));

        nodePopup = new Popup(this::nodeTitle, POPUP_W, POPUP_H).setLayouter(this::layoutNodePopup);
        nodeSystem = nodePopup.add(new Label(() -> popupNode == null || popupNode.osLabel().isEmpty()
                ? GameText.resolve(ClusterManagerTexts.NO_SYSTEM_INSTALLED) : popupNode.osLabel()));
        nodePrograms = nodePopup.add(new Label(() -> popupNode == null || popupNode.programs().isEmpty()
                ? GameText.resolve(ClusterManagerTexts.NO_PROGRAMS) : popupNode.programs(), Label.Tone.DIM));
        nodeSlot = nodePopup.add(new Label(this::nodeSlotText, Label.Tone.DIM));
        nodePower = nodePopup.add(new Button(() -> GameText.resolve(popupNode != null && popupNode.bayOn()
                        ? ClusterManagerTexts.POWER_OFF : ClusterManagerTexts.POWER_ON),
                () -> nodeAction(ClusterManagerActionPayload.ACTION_TOGGLE_NODE)));
        nodeInstallSystem = nodePopup.add(new Button(GameText.resolve(ClusterManagerTexts.SYSTEM),
                () -> nodeAction(ClusterManagerActionPayload.ACTION_INSTALL_SYSTEM_NODE)));
        nodeInstallProgram = nodePopup.add(new Button(GameText.resolve(ClusterManagerTexts.PROGRAM),
                () -> nodeAction(ClusterManagerActionPayload.ACTION_INSTALL_PROGRAM_NODE)));
        nodeClose = nodePopup.add(new Button(GameText.resolve(ClusterManagerTexts.CLOSE), nodePopup::close));

        movePopup = new Popup(this::moveTitle, POPUP_W, POPUP_H).setLayouter(this::layoutMovePopup);
        moveQtyLabel = movePopup.add(new Label(() -> moveItem == null ? ""
                : GameText.resolve(ClusterManagerTexts.QUANTITY.with(moveItem.total())), Label.Tone.DIM));
        for (int i = 0; i < PRESETS.length; i++) {
            final int preset = PRESETS[i];
            presetButtons[i] = movePopup.add(new Button(
                    preset < 0 ? GameText.resolve(ClusterManagerTexts.MAX) : String.valueOf(preset),
                    () -> moveQty = preset));
        }
        prevDest = movePopup.add(new Button("<", () -> cycleDest(-1)));
        destLabel = movePopup.add(new Label(this::destText).setAlign(Label.Align.CENTER));
        nextDest = movePopup.add(new Button(">", () -> cycleDest(1)));
        moveConfirm = movePopup.add(new Button(GameText.resolve(ClusterManagerTexts.MOVE), this::confirmMove)
                .setPrimary(true));
        moveCancel = movePopup.add(new Button(GameText.resolve(ClusterManagerTexts.CANCEL), movePopup::close));

        renamePopup = new RenamePopup().setLayouter(this::layoutRenamePopup);
        renameHint = renamePopup.add(new Label(GameText.resolve(ClusterManagerTexts.RENAME_HINT), Label.Tone.DIM));
        renameField = renamePopup.add(new TextField(ClusterRenamePayload.MAX_NAME));
        renameApply = renamePopup.add(new Button(GameText.resolve(ClusterManagerTexts.APPLY), this::applyRename)
                .setPrimary(true));
        renameCancel = renamePopup.add(new Button(GameText.resolve(ClusterManagerTexts.CANCEL), renamePopup::close));

        active = this;
        request();
    }

    /** Routes a state from the server to the open window. */
    public static void accept(final ClusterManagerStatePayload payload) {
        if (active != null) {
            active.state = payload;
            if (payload.detail().kind() >= 0) {
                active.tab = payload.detail().kind();
                active.selIndex = payload.detail().index();
                active.tabs.setSelected(active.tab);
            }
        }
    }

    private void request() {
        PacketDistributor.sendToServer(new RequestClusterManagerPayload(host, tab, selIndex));
    }

    @Override
    public void onRestored() {
        active = this;
        request();
    }

    private void act(final int action) {
        PacketDistributor.sendToServer(ClusterManagerActionPayload.bulk(host, action, tab, selIndex));
    }

    private void actNode(final int action, final WireNode node) {
        PacketDistributor.sendToServer(new ClusterManagerActionPayload(host, action, tab, selIndex, node.rackPos(), node.row()));
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public String title() {
        return "Cluster Manager";
    }

    // Wide enough for the three footer actions to read unclipped beside the cluster list.
    @Override
    public int defaultWidth() {
        return 340;
    }

    @Override
    public int defaultHeight() {
        return 210;
    }

    @Override
    public int minWidth() {
        return 330;
    }

    @Override
    public int minHeight() {
        return 150;
    }

    // state readers

    @Nullable
    private Detail detail() {
        if (state == null) {
            return null;
        }
        final Detail d = state.detail();
        return d.kind() == tab && d.index() >= 0 ? d : null;
    }

    private List<WireCluster> clustersOfTab() {
        final List<WireCluster> out = new ArrayList<>();
        if (state != null) {
            for (final WireCluster c : state.clusters()) {
                if (c.kind() == tab) {
                    out.add(c);
                }
            }
        }
        return out;
    }

    private List<WireNode> nodes() {
        final Detail d = detail();
        return d == null ? List.of() : d.nodes();
    }

    private List<WireCraft> queue() {
        final Detail d = detail();
        return d == null ? List.of() : d.queue();
    }

    private List<NetworkItemEntry> items() {
        return state == null ? List.of() : state.items();
    }

    private boolean jobRunning() {
        if (state == null) {
            return false;
        }
        final WireJob job = state.job();
        return job.active() && job.clusterKind() == tab && job.clusterIndex() == selIndex;
    }

    private String listHeaderText() {
        final TextKey header = tab == 0 ? ClusterManagerTexts.SUPERCOMPUTERS
                : tab == 1 ? ClusterManagerTexts.SECTIONS : ClusterManagerTexts.AI_CLUSTERS;
        return GameText.resolve(header.with(clustersOfTab().size()));
    }

    private String jobText() {
        if (state == null) {
            return "";
        }
        final WireJob job = state.job();
        Text line = ClusterManagerTexts.INSTALLING_JOB.with(job.label(), job.done(), job.total());
        if (job.cancelled()) {
            line = ClusterManagerTexts.CANCELLING.with(line);
        }
        if (!job.lanes().isEmpty()) {
            final WireLane first = job.lanes().get(0);
            line = ClusterManagerTexts.WITH_LANE.with(line, first.name(), first.permille() / 10);
        }
        return GameText.resolve(line);
    }

    private int jobPercent() {
        if (state == null || state.job().total() == 0) {
            return 0;
        }
        return 100 * state.job().done() / state.job().total();
    }

    private String pastText() {
        final int past = Math.max(0, nodes().size() - LADDER.length);
        return past > 0 ? GameText.resolve(ClusterManagerTexts.PAST_THE_SLOTS.with(past)) : "";
    }

    private String inventoryHintText() {
        return GameText.resolve(ClusterManagerTexts.INVENTORY_HINT.with(items().size()));
    }

    private String nodeSlotText() {
        if (popupNode == null) {
            return "";
        }
        return GameText.resolve(popupNode.slotIndex() >= 0
                ? ClusterManagerTexts.CLUSTER_SLOT.with(popupNode.slotIndex() + 1)
                : (popupNode.bayOn() ? ClusterManagerTexts.BAY_ON_LINE : ClusterManagerTexts.BAY_OFF_LINE).text());
    }

    /* The words of a strip of tabs or of a table's columns, in the player's language. */
    private static List<String> words(final TextKey... keys) {
        final List<String> out = new ArrayList<>(keys.length);
        for (final TextKey key : keys) {
            out.add(GameText.resolve(key));
        }
        return out;
    }

    private boolean allOn() {
        final List<WireNode> nodes = nodes();
        if (nodes.isEmpty()) {
            return false;
        }
        for (final WireNode n : nodes) {
            if (!n.bayOn()) {
                return false;
            }
        }
        return true;
    }

    private Text balanceName() {
        final var mode = detail() == null ? LoadBalanceMode.ROUND_ROBIN
                : LoadBalanceMode.byId(detail().balance());
        return mode.text();
    }

    // selection

    private void selectTab(final int target) {
        if (target != tab) {
            tab = target;
            selIndex = -1;
            subTab = 0;
            resetScroll();
            request();
        }
    }

    private void selectSubTab(final int target) {
        subTab = target;
        resetScroll();
    }

    private void resetScroll() {
        nodeList.setScroll(0);
        queueList.setScroll(0);
        inventoryGrid.setScroll(0);
    }

    private void clusterClicked(final int index, final int button, final double mx, final double my) {
        final List<WireCluster> list = clustersOfTab();
        if (button == 0 && index >= 0 && index < list.size()) {
            selIndex = list.get(index).index();
            resetScroll();
            request();
        }
    }

    private void nodeClicked(final int index, final int button, final double mx, final double my) {
        final List<WireNode> nodes = nodes();
        if (button != 0 || index < 0 || index >= nodes.size()) {
            return;
        }
        final WireNode node = nodes.get(index);
        if (mx >= nodeList.right() - PAD - 16) {
            actNode(ClusterManagerActionPayload.ACTION_TOGGLE_NODE, node);
        } else {
            popupNode = node;
            nodePopup.open();
            nodePopup.placeIn(lastX, lastY, lastW, lastH);
        }
    }

    private void nodeAction(final int action) {
        if (popupNode != null) {
            actNode(action, popupNode);
        }
        nodePopup.close();
    }

    private void openMove(final int index) {
        final List<NetworkItemEntry> items = items();
        if (index < 0 || index >= items.size()) {
            return;
        }
        moveItem = items.get(index);
        moveQty = 64;
        moveDest = 0;
        movePopup.open();
        movePopup.placeIn(lastX, lastY, lastW, lastH);
    }

    private void cycleDest(final int step) {
        if (state == null || state.dests().isEmpty()) {
            return;
        }
        moveDest = Math.floorMod(moveDest + step, state.dests().size());
    }

    private String destText() {
        if (state == null || state.dests().isEmpty()) {
            return GameText.resolve(ClusterManagerTexts.NO_DESTINATION);
        }
        return GameText.resolve(
                ClusterManagerTexts.TO.with(state.dests().get(Math.min(moveDest, state.dests().size() - 1)).name()));
    }

    private void confirmMove() {
        if (moveItem != null && state != null && !state.dests().isEmpty()) {
            final WireDest dest = state.dests().get(Math.min(moveDest, state.dests().size() - 1));
            final long qty = moveQty < 0 ? moveItem.total() : Math.min(moveQty, moveItem.total());
            PacketDistributor.sendToServer(new ClusterMoveOutPayload(host, selIndex, moveItem.key(), qty, dest.pos()));
        }
        movePopup.close();
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
    }

    private void applyRename() {
        if (state != null) {
            PacketDistributor.sendToServer(new ClusterRenamePayload(host, tab, selIndex, renameField.edit().strip()));
        }
        renamePopup.close();
    }

    private String nodeTitle() {
        final WireNode n = popupNode;
        return n == null ? ""
                : GameText.resolve(ClusterManagerTexts.NODE_TITLE.with(n.name(), n.rackIndex(), n.row() + 1));
    }

    private String moveTitle() {
        return moveItem == null ? ""
                : GameText.resolve(ClusterManagerTexts.MOVE_OUT_TITLE.with(GameText.of(moveItem.key().displayName())));
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

        final int top = y + TAB_H;
        if (state != null) {
            if (!state.head().hasCard()) {
                g.fill(x + 1, top, x + width - 1, top + ROW_H + 2, WARN_BG);
            }
            // The cluster list's header band and its right edge.
            g.fill(x + LIST_W - 1, top, x + LIST_W, y + height, skin.edge());
            g.fill(x, top, x + LIST_W - 1, top + ROW_H, skin.panelBg());
            if (jobLabel.visible()) {
                g.fill(jobLabel.x() - PAD, jobLabel.y() - 2, jobLabel.right() + PAD, jobBar.bottom() + 2, WARN_BG);
            }
        }
        root.render(g, ctx);
        if (jobBar.visible() && state != null && !state.job().lanes().isEmpty()) {
            // Each lane's own progress rides on the bar past the finished part, so a slow node shows.
            final WireJob job = state.job();
            final int done = job.total() == 0 ? 0 : jobBar.width() * job.done() / job.total();
            final int laneW = jobBar.width() / Math.max(1, job.total());
            int lx = jobBar.x() + done;
            for (final WireLane lane : job.lanes()) {
                g.fill(lx, jobBar.y(), lx + laneW * lane.permille() / 1000, jobBar.bottom(), AMBER);
                lx += laneW;
            }
        }
    }

    /** Places every component from the content rectangle and the state; hides what the state has no use for. */
    private void layout(final Font font, final int x, final int y, final int width, final int height) {
        tabs.setBounds(x, y, width, TAB_H);
        tabs.setSelected(tab);
        final int top = y + TAB_H;
        final int bodyH = height - TAB_H;

        final boolean ready = state != null;
        loadingLabel.setVisible(!ready);
        loadingLabel.setBounds(x + PAD, top + PAD, width - PAD * 2, 8);
        warnLabel.setVisible(ready && !state.head().hasCard());
        warnLabel.setBounds(x + PAD, top + 2, width - PAD * 2, 8);

        listHeader.setVisible(ready);
        listHeader.setBounds(x + PAD, top + 2, LIST_W - PAD * 2, 8);
        final boolean noClusters = clustersOfTab().isEmpty();
        emptyListLabel.setVisible(ready && noClusters);
        emptyListLabel.setBounds(x + PAD, top + ROW_H + 2, LIST_W - PAD * 2, 8);
        clusterList.setVisible(ready && !noClusters);
        clusterList.setBounds(x, top + ROW_H, LIST_W - 1, bodyH - ROW_H);

        final Detail d = detail();
        final int dx = x + LIST_W;
        final int dw = width - LIST_W;
        placeholder.setVisible(ready && d == null);
        placeholder.setBounds(dx + PAD, top + PAD, dw - PAD * 2, 8);
        final boolean showDetail = ready && d != null;
        for (final var c : List.of(nameLabel, pillLabel, renameButton, subLabel)) {
            c.setVisible(showDetail);
        }
        final boolean running = showDetail && jobRunning();
        jobLabel.setVisible(running);
        jobBar.setVisible(running);
        scSubTabs.setVisible(showDetail && tab == 0);
        dcSubTabs.setVisible(showDetail && tab != 0);
        final boolean nodesTab = showDetail && subTab == 0;
        final boolean mapTab = showDetail && tab == 0 && subTab == 1;
        final boolean queueTab = showDetail && tab == 0 && subTab == 2;
        final boolean inventoryTab = showDetail && tab != 0 && subTab == 1;
        scNodeColumns.setVisible(nodesTab && tab == 0);
        dcNodeColumns.setVisible(nodesTab && tab != 0);
        nodeList.setVisible(nodesTab && !nodes().isEmpty());
        noNodesLabel.setVisible(nodesTab && nodes().isEmpty());
        mapColumns.setVisible(mapTab);
        slotList.setVisible(mapTab);
        pastLabel.setVisible(mapTab && !pastText().isEmpty());
        queueColumns.setVisible(queueTab);
        queueList.setVisible(queueTab && !queue().isEmpty());
        noQueueLabel.setVisible(queueTab && queue().isEmpty());
        inventoryHint.setVisible(inventoryTab);
        inventoryGrid.setVisible(inventoryTab && !items().isEmpty());
        emptyInventoryLabel.setVisible(inventoryTab && items().isEmpty());
        cancelJob.setVisible(running);
        balance.setVisible(showDetail && !running && tab == 1 && subTab == 1);
        final boolean bulk = showDetail && !running && !(tab == 1 && subTab == 1);
        systemAll.setVisible(bulk);
        programAll.setVisible(bulk);
        powerAll.setVisible(bulk);
        if (!showDetail) {
            return;
        }

        int dy = top + PAD;
        final int pillW = Math.max(font.width(GameText.resolve(ClusterManagerTexts.OFFLINE)),
                font.width(GameText.resolve(ClusterManagerTexts.ONLINE)));
        pillLabel.setBounds(dx + dw - PAD - pillW, dy, pillW, 8);
        renameButton.setBounds(dx + dw - PAD - pillW - 6 - RENAME_W, dy - 1, RENAME_W, 10);
        nameLabel.setBounds(dx + PAD, dy, renameButton.x() - 6 - (dx + PAD), 8);
        dy += ROW_H;
        subLabel.setBounds(dx + PAD, dy, dw - PAD * 2, 8);
        dy += ROW_H;
        if (running) {
            jobLabel.setBounds(dx + PAD * 2, dy + 2, dw - PAD * 4, 8);
            jobBar.setBounds(dx + PAD * 2, dy + ROW_H + 2, dw - PAD * 4, 4);
            dy += ROW_H * 2 + 8;
        }
        final TabStrip subTabs = tab == 0 ? scSubTabs : dcSubTabs;
        subTabs.setBounds(dx + PAD, dy, dw - PAD * 2, 12);
        subTabs.setSelected(subTab);
        dy += 14;
        final int bottom = top + bodyH - BTN_H - PAD * 2;
        final int by = top + bodyH - BTN_H - PAD;

        if (nodesTab) {
            /*
             * Columns are measured from both edges so nothing runs into its neighbour: the unit and the
             * metric take fixed room, the status takes what its longest word needs, and the name and the
             * system share the rest.
             */
            final int rightEdge = dx + dw - PAD - SWITCH_W;
            final int metricW = 32;
            final int statusW = font.width(GameText.resolve(ClusterManagerTexts.INSTALLING)) + 6;
            final int c0 = dx + PAD;
            final int c1 = c0 + font.width("R00 U0") + 8;
            final int c4 = rightEdge - metricW;
            final int c3 = c4 - statusW;
            final int c2 = c1 + Math.max(30, (c3 - c1 - GAP) / 2);
            final ColumnHeader columns = tab == 0 ? scNodeColumns : dcNodeColumns;
            columns.setBounds(dx, dy - 1, dw, ROW_H);
            columns.setColumnX(c0, c1, c2, c3, c4);
            nodeList.setBounds(dx, dy + ROW_H, dw, Math.max(ROW_H, bottom - (dy + ROW_H)));
            noNodesLabel.setBounds(c0, dy + ROW_H, dw - PAD * 2, 8);
        } else if (mapTab) {
            mapColumns.setBounds(dx, dy - 1, dw, ROW_H);
            mapColumns.setColumnX(dx + PAD, dx + PAD + 30, dx + PAD + 66, dx + dw - PAD - 60);
            final int listH = Math.min(LADDER.length * ROW_H, Math.max(ROW_H, bottom - (dy + ROW_H)));
            slotList.setBounds(dx, dy + ROW_H, dw, listH);
            pastLabel.setBounds(dx + PAD, dy + ROW_H + listH, dw - PAD * 2, 8);
            pastLabel.setVisible(pastLabel.visible() && pastLabel.bottom() <= bottom);
        } else if (queueTab) {
            queueColumns.setBounds(dx, dy - 1, dw, ROW_H);
            queueColumns.setColumnX(dx + PAD, dx + dw / 2, dx + dw - PAD - 60);
            queueList.setBounds(dx, dy + ROW_H, dw, Math.max(ROW_H, bottom - (dy + ROW_H)));
            noQueueLabel.setBounds(dx + PAD, dy + ROW_H, dw - PAD * 2, 8);
        } else if (inventoryTab) {
            inventoryHint.setBounds(dx + PAD, dy, dw - PAD * 2, 8);
            final int cols = Math.max(1, (dw - PAD * 2) / CELL);
            final int rows = Math.max(1, (bottom - (dy + ROW_H)) / CELL);
            inventoryGrid.setColumns(cols).setVisibleRows(rows).setTotalRows((items().size() + cols - 1) / cols)
                    .setCellCount(items().size()).place(dx + PAD, dy + ROW_H);
            emptyInventoryLabel.setBounds(dx + PAD, dy + ROW_H, dw - PAD * 2, 8);
        }

        // The footer: one wide button while a job runs or on the inventory, else the three bulk actions.
        final int gap = 3;
        final int footerX = dx + PAD;
        final int footerW = dw - PAD * 2;
        cancelJob.setBounds(footerX, by, footerW, BTN_H);
        balance.setBounds(footerX, by, footerW, BTN_H);
        final int bw = (footerW - gap * 2) / 3;
        systemAll.setBounds(footerX, by, bw, BTN_H);
        programAll.setBounds(footerX + bw + gap, by, bw, BTN_H);
        powerAll.setBounds(footerX + (bw + gap) * 2, by, bw, BTN_H);
        systemAll.setPrimary(!state.head().mediumSystem().isEmpty());
        programAll.setPrimary(!state.head().mediumProgram().isEmpty());
    }

    private void renderClusterRow(final GuiGraphics g, final UiContext ctx, final WireCluster c, final int index,
                                  final int x, final int y, final int w, final int h, final boolean hovered,
                                  final boolean selectedRow) {
        final boolean sel = c.index() == selIndex;
        if (sel || hovered) {
            g.fill(x, y, x + w, y + h, sel ? ctx.skin().fieldBg() : ctx.skin().panelBg());
        }
        if (sel) {
            g.fill(x, y, x + 2, y + h, ctx.skin().accent());
        }
        g.fill(x + 5, y + 3, x + 8, y + 6, c.reachable() ? (c.online() ? GREEN : RED) : ctx.skin().dim());
        g.drawString(ctx.font(), Texts.clip(ctx.font(), c.name(), w - 16), x + 11, y + 1, ctx.skin().text(), false);
        g.drawString(ctx.font(), Texts.clip(ctx.font(), GameText.resolve(c.sub()), w - 8), x + 5, y + ROW_H + 1,
                ctx.skin().dim(), false);
    }

    private void renderNodeRow(final GuiGraphics g, final UiContext ctx, final WireNode n, final int index, final int x,
                               final int y, final int w, final int h, final boolean hovered, final boolean selectedRow) {
        final ColumnHeader columns = tab == 0 ? scNodeColumns : dcNodeColumns;
        ctx.skin().listRow(g, x + 2, y, w - 4, h, hovered, false);
        final Font font = ctx.font();
        final int col = n.bayOn() ? ctx.skin().text() : ctx.skin().dim();
        final int c0 = columns.columnX(0);
        final int c1 = columns.columnX(1);
        final int c2 = columns.columnX(2);
        final int c3 = columns.columnX(3);
        final int c4 = columns.columnX(4);
        final int ty = y + 1;
        g.drawString(font, "R" + n.rackIndex() + " U" + (n.row() + 1), c0, ty, col, false);
        g.drawString(font, Texts.clip(font, n.name(), c2 - c1 - GAP), c1, ty, col, false);
        if (n.osLabel().isEmpty()) {
            g.drawString(font, "-", c2, ty, ctx.skin().dim(), false);
        } else {
            g.drawString(font, Texts.clip(font, n.osLabel(), c3 - c2 - GAP), c2, ty, col, false);
        }
        g.drawString(font, Texts.clip(font, stateLabel(n.state()), c4 - c3 - GAP), c3, ty, stateColor(n.state(), ctx), false);
        final String right = tab == 0 ? (n.phiModel() < 0 ? "-" : MODELS[Math.min(n.phiModel(), MODELS.length - 1)])
                : (n.total() <= 0 ? "-" : (100 * n.used() / Math.max(1, n.total())) + "%");
        g.drawString(font, Texts.clip(font, right, 32 - GAP), c4, ty, ctx.skin().accent(), false);
        // The bay switch at the row's right edge.
        final int sx = x + w - PAD - SWITCH_W + 2;
        g.fill(sx, ty, sx + 14, ty + 8, ctx.skin().fieldBg());
        g.fill(sx + (n.bayOn() ? 8 : 1), ty + 1, sx + (n.bayOn() ? 13 : 6), ty + 7, n.bayOn() ? GREEN : ctx.skin().dim());
    }

    /** The one word for what a machine is doing, matching the state the server sent. */
    private static String stateLabel(final int state) {
        return GameText.resolve(switch (state) {
            case ClusterManagerStatePayload.STATE_INCOMPLETE -> ClusterManagerTexts.INCOMPLETE;
            case ClusterManagerStatePayload.STATE_BAY_OFF -> ClusterManagerTexts.BAY_OFF;
            case ClusterManagerStatePayload.STATE_INSTALLING -> ClusterManagerTexts.INSTALLING;
            case ClusterManagerStatePayload.STATE_NO_COPROCESSOR -> ClusterManagerTexts.NO_PHI;
            case ClusterManagerStatePayload.STATE_UNDER_RATED -> ClusterManagerTexts.PHI_LOW;
            case ClusterManagerStatePayload.STATE_UNSLOTTED -> ClusterManagerTexts.INERT;
            case ClusterManagerStatePayload.STATE_NO_SYSTEM -> ClusterManagerTexts.NO_SYSTEM;
            default -> ClusterManagerTexts.ONLINE;
        });
    }

    private static int stateColor(final int state, final UiContext ctx) {
        return switch (state) {
            case ClusterManagerStatePayload.STATE_ONLINE -> GREEN;
            case ClusterManagerStatePayload.STATE_INCOMPLETE -> RED;
            case ClusterManagerStatePayload.STATE_INSTALLING -> ctx.skin().accent();
            case ClusterManagerStatePayload.STATE_BAY_OFF, ClusterManagerStatePayload.STATE_UNSLOTTED -> ctx.skin().dim();
            default -> AMBER;
        };
    }

    private void renderSlotRow(final GuiGraphics g, final UiContext ctx, final Integer slot, final int index, final int x,
                               final int y, final int w, final int h, final boolean hovered, final boolean selectedRow) {
        WireNode node = null;
        for (final WireNode n : nodes()) {
            if (n.slotIndex() == slot) {
                node = n;
            }
        }
        final Font font = ctx.font();
        final int ty = y + 1;
        g.drawString(font, String.valueOf(slot + 1), mapColumns.columnX(0), ty, ctx.skin().dim(), false);
        g.drawString(font, LADDER[slot], mapColumns.columnX(1), ty, ctx.skin().accent(), false);
        if (node == null) {
            g.drawString(font, GameText.resolve(ClusterManagerTexts.NO_NODE), mapColumns.columnX(2), ty,
                    ctx.skin().dim(), false);
            return;
        }
        g.drawString(font, Texts.clip(font, "R" + node.rackIndex() + " U" + (node.row() + 1) + " " + node.name(),
                mapColumns.columnX(3) - mapColumns.columnX(2) - GAP), mapColumns.columnX(2), ty, ctx.skin().text(), false);
        final String st = GameText.resolve(node.code() >= 16 ? ClusterManagerTexts.ONLINE
                : node.code() == 3 ? ClusterManagerTexts.BAY_OFF : node.code() == 2 ? ClusterManagerTexts.RATING_LOW
                : node.code() == 1 ? ClusterManagerTexts.NO_PHI_CARD : ClusterManagerTexts.SLOT_NO_NODE);
        g.drawString(font, st, mapColumns.columnX(3), ty, node.code() >= 16 ? GREEN : AMBER, false);
    }

    private void renderQueueRow(final GuiGraphics g, final UiContext ctx, final WireCraft c, final int index, final int x,
                                final int y, final int w, final int h, final boolean hovered, final boolean selectedRow) {
        final Font font = ctx.font();
        final int ty = y + 1;
        final int col = c.waiting() ? ctx.skin().dim() : ctx.skin().text();
        final int c0 = queueColumns.columnX(0);
        final int c1 = queueColumns.columnX(1);
        final int c2 = queueColumns.columnX(2);
        g.drawString(font, Texts.clip(font, GameText.resolve(ClusterManagerTexts.CRAFT.with(c.label())), c1 - c0 - GAP),
                c0, ty, col, false);
        g.drawString(font, Texts.clip(font, c.requester(), c2 - c1 - GAP), c1, ty, ctx.skin().dim(), false);
        g.drawString(font, c.waiting() ? GameText.resolve(ClusterManagerTexts.WAITING) : String.valueOf(c.slots()),
                c2, ty, c.waiting() ? AMBER : ctx.skin().accent(), false);
    }

    private void renderInventoryCell(final GuiGraphics g, final UiContext ctx, final int index, final int cx, final int cy,
                                     final int w, final int h, final boolean hovered) {
        final List<NetworkItemEntry> items = items();
        if (index < items.size()) {
            final NetworkItemEntry entry = items.get(index);
            DesktopItems.itemWithCount(g, ctx.font(), entry.key().stack(1), cx + 1, cy + 1, shortCount(entry.total()));
        }
    }

    // dialogs

    @Override
    public boolean modalActive() {
        return nodePopup.isOpen() || movePopup.isOpen() || renamePopup.isOpen();
    }

    @Override
    public void renderModal(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                            final int height, final int mouseX, final int mouseY) {
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, 0f);
        if (nodePopup.isOpen()) {
            nodePopup.renderIn(g, ctx, x, y, width, height);
        } else if (movePopup.isOpen()) {
            movePopup.renderIn(g, ctx, x, y, width, height);
        } else if (renamePopup.isOpen()) {
            renamePopup.renderIn(g, ctx, x, y, width, height);
        }
    }

    @Nullable
    private Popup openPopup() {
        if (nodePopup.isOpen()) {
            return nodePopup;
        }
        if (movePopup.isOpen()) {
            return movePopup;
        }
        return renamePopup.isOpen() ? renamePopup : null;
    }

    private void layoutNodePopup(final Popup p) {
        final int cy = p.contentTop();
        nodeSystem.setBounds(p.x() + 4, cy + 1, p.width() - 8, 8);
        nodePrograms.setBounds(p.x() + 4, cy + 12, p.width() - 8, 8);
        nodeSlot.setBounds(p.x() + 4, cy + 23, p.width() - 8, 8);
        layoutButtonRow(p, nodePower, nodeInstallSystem, nodeInstallProgram, nodeClose);
    }

    private void layoutMovePopup(final Popup p) {
        final int cy = p.contentTop();
        moveQtyLabel.setBounds(p.x() + 4, cy + 1, p.width() - 8, 8);
        final int cw = (p.width() - 8 - 2 * (PRESETS.length - 1)) / PRESETS.length;
        for (int i = 0; i < PRESETS.length; i++) {
            presetButtons[i].setBounds(p.x() + 4 + i * (cw + 2), cy + 12, cw, ROW_H);
            presetButtons[i].setPrimary(moveQty == PRESETS[i]);
        }
        final int ry = cy + 27;
        prevDest.setBounds(p.x() + 4, ry, 12, ROW_H);
        nextDest.setBounds(p.right() - 16, ry, 12, ROW_H);
        destLabel.setBounds(p.x() + 18, ry + 2, p.width() - 36, 8);
        layoutButtonRow(p, moveConfirm, moveCancel);
    }

    private void layoutRenamePopup(final Popup p) {
        final int cy = p.contentTop();
        renameHint.setBounds(p.x() + 4, cy + 1, p.width() - 8, 8);
        renameField.setBounds(p.x() + 4, cy + 14, p.width() - 8, ROW_H + 2);
        layoutButtonRow(p, renameApply, renameCancel);
    }

    /** Spreads the buttons across the dialog's bottom row, equally wide, two pixels apart. */
    private static void layoutButtonRow(final Popup p, final Button... buttons) {
        final int bw = (p.width() - 8 - 2 * (buttons.length - 1)) / buttons.length;
        final int by = p.bottom() - BTN_H - 4;
        int bx = p.x() + 4;
        for (final Button b : buttons) {
            b.setBounds(bx, by, bw, BTN_H);
            bx += bw + 2;
        }
    }

    // input

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup popup = openPopup();
        if (popup != null) {
            popup.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (button != 0) {
            return;
        }
        if (!root.mouseClicked(mouseX, mouseY, button) && inventoryGrid.visible() && inventoryGrid.contains(mouseX, mouseY)) {
            // An empty cell with a stack in hand deposits it into the section.
            act(ClusterManagerActionPayload.ACTION_DEPOSIT);
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup popup = openPopup();
        if (popup != null) {
            popup.mouseDragged(mouseX, mouseY, button);
        } else {
            root.mouseDragged(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup popup = openPopup();
        if (popup != null) {
            popup.mouseReleased(mouseX, mouseY, button);
        } else {
            root.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        if (openPopup() != null) {
            return true;
        }
        if (root.mouseScrolled(lastMouseX, lastMouseY, delta)) {
            return true;
        }
        // The wheel anywhere in the window moves whichever table the detail shows.
        final int step = delta > 0 ? -1 : 1;
        if (nodeList.visible()) {
            nodeList.setScroll(nodeList.scroll() + step);
        } else if (queueList.visible()) {
            queueList.setScroll(queueList.scroll() + step);
        } else if (inventoryGrid.visible()) {
            inventoryGrid.scrollBy(step);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final Popup popup = openPopup();
        return popup != null && popup.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c) {
        final Popup popup = openPopup();
        return popup != null && popup.charTyped(c);
    }

    // helpers

    private static String shortCount(final long n) {
        if (n >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", n / 1_000_000.0);
        }
        if (n >= 10_000L) {
            return (n / 1000) + "k";
        }
        if (n >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fk", n / 1000.0);
        }
        return String.valueOf(n);
    }
}
