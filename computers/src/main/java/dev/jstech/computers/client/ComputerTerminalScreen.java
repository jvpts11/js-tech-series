/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.gui.layout.ComputerTerminalLayout;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload;
import dev.jstech.computers.operation.payload.PatternStudioStatePayload;
import dev.jstech.computers.operation.payload.LocalStorageSnapshotPayload;
import dev.jstech.computers.operation.payload.NetworkItemEntry;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.RequestServerBreakdownPayload;
import dev.jstech.computers.operation.payload.TerminalDiskPrivacyPayload;
import dev.jstech.computers.operation.payload.TerminalInsertPayload;
import dev.jstech.computers.operation.payload.TerminalLocalDepositPayload;
import dev.jstech.computers.operation.payload.TerminalMaintenancePayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;

import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

/**
 * Screen for the Monitor terminal: a left tab rail (icon over name) and a content area, drawn as a flat dark "computer OS" with square edges and a cyan accent, with the player inventory pinned along the bottom so every tab is usable.
 */
public class ComputerTerminalScreen extends AbstractComputerScreen<ComputerTerminalMenu> {

    /*
     * Flat palette (ARGB), read live from the render-bound OS theme so the Monitor terminal repaints in the host
     * computer's hardware-era skin. These mirror the former static constants one-for-one; refreshed via
     * syncPalette() at the top of each draw pass (renderBg/renderLabels) while the era theme is bound, so a
     * STANDARD-era host renders byte-identically to the old flat-dark constants.
     */
    private int OUTER;
    private int SCREEN;
    private int RAIL;
    private int PANEL;
    private int LINE;
    private int TRACK;
    private int SLOT_BG;
    private int SLOT_EDGE;
    private int ACCENT;
    private int ACCENT2;
    private int GREEN;
    private int AMBER;
    private int RED;
    private int TEXT;
    private int DIM;
    private int TAB_ON;
    private int TAB_LABEL_ON;
    private int HOVER;

    /** Refreshes the palette fields from the bound era theme. Called at the top of every draw pass. */
    private void syncPalette() {
        OUTER = JsTechTheme.outer();
        SCREEN = JsTechTheme.screen();
        RAIL = JsTechTheme.rail();
        PANEL = JsTechTheme.panel();
        LINE = JsTechTheme.line();
        TRACK = JsTechTheme.track();
        SLOT_BG = JsTechTheme.slotBg();
        SLOT_EDGE = JsTechTheme.slotEdge();
        ACCENT = JsTechTheme.accent();
        ACCENT2 = JsTechTheme.accent2();
        GREEN = JsTechTheme.green();
        AMBER = JsTechTheme.amber();
        RED = JsTechTheme.red();
        TEXT = JsTechTheme.text();
        DIM = JsTechTheme.dim();
        TAB_ON = JsTechTheme.tabOn();
        TAB_LABEL_ON = JsTechTheme.tabLabelOn();
        HOVER = JsTechTheme.hover();
    }

    /*
     * The rail's own measurements come from the layout model, which is what the click, the drawing, the
     * tooltips and the wheel all read, so none of them can work out a different answer from the others.
     */
    private static final int RAIL_X = ComputerTerminalLayout.RAIL_X;
    private static final int RAIL_W = ComputerTerminalLayout.RAIL_W;
    private static final int TAB_Y0 = ComputerTerminalLayout.TAB_Y0;
    private static final int TAB_H = ComputerTerminalLayout.TAB_H;
    private static final int BAR_H = ComputerTerminalLayout.BAR_H;
    private static final int CONTENT_X = ComputerTerminalLayout.CONTENT_X;
    private static final int CONTENT_W = ComputerTerminalLayout.CONTENT_W;

    // Network item grid (a virtual grid, not real slots; rendered from the snapshot).
    private static final int NET_X = ComputerTerminalLayout.GRID_X;
    private static final int NET_COLS = ComputerTerminalLayout.GRID_COLS;
    private static final int NET_ROWS = ComputerTerminalLayout.GRID_ROWS;
    private static final int NET_Y = ComputerTerminalLayout.GRID_Y;

    // Toolbar above the grid: what is searched for, which mod it belongs to, and the order.
    private static final int TOOLBAR_Y = ComputerTerminalLayout.TOOLBAR_Y;
    private static final int TOOLBAR_H = ComputerTerminalLayout.TOOLBAR_H;
    private static final int SORT_W = ComputerTerminalLayout.SORT_W;
    private static final int SORT_X = ComputerTerminalLayout.SORT_X;
    private static final int MOD_W = ComputerTerminalLayout.MOD_W;
    private static final int MOD_X = ComputerTerminalLayout.MOD_X;
    private static final int SEARCH_W = ComputerTerminalLayout.SEARCH_W;

    /*
     * Deposit button: the explicit "insert held items into the network" target, sitting on the rule that
     * separates the machine's own screen from the player's own rows.
     */
    private static final int DEPOSIT_X = ComputerTerminalLayout.DEPOSIT_X;
    private static final int DEPOSIT_Y = ComputerTerminalLayout.DEPOSIT_Y;
    private static final int DEPOSIT_W = ComputerTerminalLayout.DEPOSIT_W;
    private static final int DEPOSIT_H = ComputerTerminalLayout.DEPOSIT_H;

    /*
     * The headings, indexed by ComputerTerminalMenu.TAB_*. They are the rail's own words rather than the
     * mod's usual ones: "Upkeep" is what fits beside a heading list this narrow, and "Programs" is what
     * this system calls what is running, since its own verb for it is programs rather than processes.
     */
    private static final TextKey[] TAB_NAMES = {TerminalTexts.TAB_LOCAL, TerminalTexts.TAB_STORAGE,
            TerminalTexts.TAB_NETWORK, TerminalTexts.TAB_OPS, TerminalTexts.TAB_TASKS, TerminalTexts.TAB_UPKEEP,
            TerminalTexts.TAB_CRAFT, TerminalTexts.TAB_PROGRAMS, TerminalTexts.TAB_CONSOLE, TerminalTexts.TAB_PATTERNS};

    private int netScrollRow;
    int selectedOp;
    private int opScroll;
    int taskSubTab;

    @Nullable
    private EditBox searchBox;
    boolean sortByQuantity = true;
    /*
     * The grid's filtered and sorted view, kept between frames: it is asked for several times a frame and
     * re-sorting a big network's catalog each time cost the frame rate (see visibleItems).
     */
    private List<NetworkItemEntry> visibleCache = List.of();
    private List<NetworkItemEntry> visibleSource = List.of();
    private String visibleKey = "";

    /** Which mod's items the grid is showing, or the empty string for every one of them. */
    private String modFilter = "";

    /* The two buttons at the foot of the panel beside the grid, which the panel draws and the click reads. */
    /* The panel's two buttons, from the layout, which is where every reader of them now looks. */
    static final int PANE_BTN_Y = ComputerTerminalLayout.PANE_BTN_Y;
    static final int PANE_BTN_W = ComputerTerminalLayout.PANE_BTN_W;
    static final int PANE_BTN_H = ComputerTerminalLayout.PANE_BTN_H;
    static final int PANE_GET_X = ComputerTerminalLayout.PANE_GET_X;
    static final int PANE_CRAFT_X = ComputerTerminalLayout.PANE_CRAFT_X;

    /** The entry the panel beside the grid is describing, or null when nothing is picked out. */
    @Nullable
    private NetworkItemEntry selectedEntry;

    /*
     * Storage: the public/private slider band, one compact track per disk (a personal computer has two).
     * It lives in the panel beside the grid, where the detail of whatever is selected lives on the other
     * headings, so the grid keeps every one of its rows. For a host without a slider (a Server or a
     * Mainframe, whose whole store is the network's) the panel says so instead of showing a dead control.
     */
    private static final int SLIDER_TRACK0_DY = ComputerTerminalLayout.SLIDER_TRACK0_DY;
    private static final int SLIDER_ROW_PITCH = ComputerTerminalLayout.SLIDER_ROW_PITCH;
    private static final int SLIDER_TRACK_H = ComputerTerminalLayout.SLIDER_TRACK_H;
    private static final int SLIDER_TRACK_LX = ComputerTerminalLayout.SLIDER_TRACK_LX;
    private static final int SLIDER_HANDLE_W = ComputerTerminalLayout.SLIDER_HANDLE_W;
    // Snap step while dragging (50 per-mille = 5%); holding Shift drags at fine 1 per-mille. Tunable.
    private static final int SLIDER_STEP = 50;

    int draggingSliderDisk = -1;            // which disk's slider is being dragged, or -1 for none
    private int focusedSliderDisk = -1;    // which disk's slider has keyboard focus, or -1 for none
    // Optimistic per-disk values shown while dragging; overwritten by the authoritative sync each frame.
    private final int[] sliderPreview = new int[LocalStorageSnapshotPayload.MAX_DISKS];
    private final boolean[] sliderPreviewActive = new boolean[LocalStorageSnapshotPayload.MAX_DISKS];

    // Operations tab layout (content-relative); the tab and this hit test read the one number.
    private static final int OPS_ROWS = ComputerTerminalLayout.OPS_ROWS;

    /** How far down the Craft heading's catalogue has been scrolled. */
    int craftScroll;

    /**
     * The three questions this screen asks about a thing, each its own object.
     *
     * <p>The two that take a number share the field that number is typed into, because they are never up
     * at the same time and because a screen with two of them would be a screen with two places to look.
     */
    private final TerminalQuantityBox qtyBox =
            new TerminalQuantityBox(118, 14, 12, this::quantityTyped);
    private final TerminalRequestPopup request = new TerminalRequestPopup(this, menu, qtyBox);
    private final TerminalCraftPopup craft = new TerminalCraftPopup(this, menu, qtyBox);

    /** What one Operation is made of, shown when one of them is clicked into. */
    private final TerminalOperationPopup opPopup = new TerminalOperationPopup(this, menu);
    private static final int TASK_OP_ROWS = 4;

    // Maintenance tab layout (content-relative Y offsets from the content top cy).
    private static final int MNT_TILE_ROW1_Y = 32;
    private static final int MNT_TILE_ROW2_Y = 56;
    private static final int MNT_TILE_H = 22;
    private static final int MNT_ACTIONS_Y = 82;
    private static final int MNT_BTN_ROW1_Y = 94;    // ANALYZE | VACUUM
    private static final int MNT_BTN_REINDEX_Y = 112;
    private static final int MNT_BTN_DROP_Y = 130;
    private static final int MNT_BTN_H = 15;

    Text maintHint = Text.EMPTY;

    /** The one question this screen asks that cannot be taken back, and the only modal that is its own. */
    private final TerminalDropPopup drop = new TerminalDropPopup(this, menu);

    public ComputerTerminalScreen(final ComputerTerminalMenu menu, final Inventory inventory,
                                  final Component title) {
        super(menu, inventory, title);
        /*
         * The whole monitor glass, on every machine. This used to be a window narrower than the screen it
         * was drawn on, which grew by a band when the host was a Mainframe; the rail scrolls instead, so
         * nothing about the host changes the size of anything.
         */
        this.imageWidth = ComputerTerminalLayout.WIDTH;
        this.imageHeight = ComputerTerminalLayout.HEIGHT;
        // The terminal draws all of its own labels.
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;
    }

    @Override
    protected void init() {
        super.init();
        final EditBox box = new EditBox(font, leftPos + NET_X + 4, topPos + TOOLBAR_Y + 2,
                SEARCH_W - 8, TOOLBAR_H - 3, GameText.component(TerminalTexts.SEARCH));
        box.setBordered(false);
        /*
         * The fields are built outside a render pass, so color them from the resolved era theme (not the bound
         * static); containerTick keeps them in step when a board swap changes the host era.
         */
        box.setTextColor(theme.text());
        box.setMaxLength(48);
        box.setHint(GameText.component(TerminalTexts.SEARCH_HINT).withStyle(ChatFormatting.DARK_GRAY));
        box.setResponder(s -> netScrollRow = 0);
        addRenderableWidget(box);
        searchBox = box;

        /*
         * The field a quantity is typed into; hidden until one of the questions that takes one is up. It
         * is made here rather than with the other fields because a screen has no font until it is laid
         * out, and a box built with that null throws the moment anything measures a string in it.
         */
        qtyBox.ensure(font);
        qtyBox.setTextColor(theme.text());
        addRenderableWidget(qtyBox.widget());

        syncSearchBoxVisibility();

        /*
         * Indexed by ComputerTerminalMenu.TAB_*, so the array is in that order rather than the rail's:
         * the rail says which headings a machine offers, this says what each one of them draws.
         *
         * Built once and kept through a resize, because a heading can be holding something of the
         * machine's: the prompt's scrollback is the machine's console, and a window that changed size is
         * no reason to lose what was said at it.
         */
        if (tabs != null) {
            return;
        }
        tabs = new ITerminalTab[]{
            new LocalTerminalTab(this, menu),
            new StorageTerminalTab(this, menu),
            new NetworkTerminalTab(this, menu),
            new OpsTerminalTab(this, menu),
            new TasksTerminalTab(this, menu),
            new MaintenanceTerminalTab(this, menu),
            new CraftTerminalTab(this, menu),
            new ProcessesTerminalTab(this, menu),
            new ConsoleTerminalTab(this, menu),
            new PatternsTerminalTab(this, menu),
        };
    }

    private void syncSearchBoxVisibility() {
        if (searchBox == null) {
            return;
        }
        final boolean show = isGridTab() && !anyQuestionOpen();
        searchBox.visible = show;
        searchBox.active = show;
        searchBox.setY(topPos + TOOLBAR_Y + 2);
        if (!show) {
            searchBox.setFocused(false);
        }
        /*
         * The quantity field belongs to whichever question is up, and the two that take one draw it in
         * different places, so it is put where that question wants it before it is shown.
         */
        if (request.isOpen()) {
            request.placeField();
        } else if (craft.isOpen()) {
            craft.placeField();
        }
        qtyBox.show(request.isOpen() || craft.isOpen());
    }

    /** Whether any of the questions this screen asks is up, which is when nothing behind it is live. */
    private boolean anyQuestionOpen() {
        return request.isOpen() || craft.isOpen() || opPopup.isOpen() || drop.isOpen();
    }

    /** What was typed into the quantity field goes to whichever question asked for it. */
    private void quantityTyped(final long value) {
        if (craft.isOpen()) {
            craft.typed(value);
        } else if (request.isOpen()) {
            request.typed(value);
        }
    }

    /** Hands the keyboard to the quantity field, which is a thing the questions ask the screen to do. */
    void giveKeyboardTo(final TerminalQuantityBox box) {
        if (box.widget() != null) {
            setFocused(box.widget());
            box.setFocused(true);
        }
    }

    /**
     * The headings this machine offers, in rail order.
     *
     * <p>The rail is not a fixed list. It is built from what the host is and from whether the network can
     * craft at all: six on a personal computer with no crafting, ten on a Mainframe with it. Craft appears
     * when the network has a Crafting Computer on it; Patterns only on a machine that can be taught a recipe
     * itself; Tasks and Upkeep belong to the Mainframe, which is the machine that orchestrates. Console is
     * last on every one of them, because a machine always has a prompt.
     */
    private int[] railTabs() {
        final int shape = (menu.craftAvailable() ? 1 : 0)
                | (menu.patternsAvailable() ? 2 : 0)
                | (menu.mainframeHost() ? 4 : 0);
        if (railCache != null && railShape == shape) {
            return railCache;
        }
        final List<Integer> rail = new ArrayList<>(10);
        rail.add(ComputerTerminalMenu.TAB_LOCAL);
        rail.add(ComputerTerminalMenu.TAB_STORAGE);
        rail.add(ComputerTerminalMenu.TAB_NETWORK);
        if (menu.craftAvailable()) {
            rail.add(ComputerTerminalMenu.TAB_CRAFT);
        }
        if (menu.patternsAvailable()) {
            rail.add(ComputerTerminalMenu.TAB_PATTERNS);
        }
        rail.add(ComputerTerminalMenu.TAB_OPS);
        if (menu.mainframeHost()) {
            rail.add(ComputerTerminalMenu.TAB_TASKS);
            rail.add(ComputerTerminalMenu.TAB_MAINTENANCE);
        }
        rail.add(ComputerTerminalMenu.TAB_PROCESSES);
        rail.add(ComputerTerminalMenu.TAB_CONSOLE);
        final int[] out = new int[rail.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = rail.get(i);
        }
        railShape = shape;
        railCache = out;
        return out;
    }

    /*
     * The rail is asked for several times a frame (the drawing, the labels, the wheel, the click), and it
     * only ever changes when the host gains or loses one of the three optional headings. Hold the answer
     * and rebuild it when that shape changes, so a draw pass allocates nothing.
     */
    private int[] railCache;
    private int railShape = -1;

    // Per-tab rendering delegates; instantiated in init() once menu and screen geometry are ready.
    private ITerminalTab[] tabs;

    /**
     * The heading showing, or null when there is none to show.
     *
     * <p>Null-safe on purpose: input and the tick reach a screen from the moment it exists, which is before
     * the headings are built, and a heading can also be one the machine no longer offers.
     */
    @Nullable
    private ITerminalTab showing() {
        final int active = menu.activeTab();
        return tabs != null && active >= 0 && active < tabs.length ? tabs[active] : null;
    }

    // The rail now scrolls instead of shrinking, so every entry keeps its full height and its name.
    private int railScroll;

    private int railVisible() {
        return ComputerTerminalLayout.visibleRows(ComputerTerminalLayout.railHeight());
    }

    private int maxRailScroll() {
        return ComputerTerminalLayout.maxScroll(railTabs().length, railVisible());
    }

    private int contentW() {
        return CONTENT_W;
    }

    Font tabFont() {
        return font;
    }

    /**
     * Offers the Crafting Manager's state to the Patterns heading, when that is what asked for it.
     *
     * <p>The same two messages serve the desktop programs and this heading, because it is the same work
     * seen from a machine that draws no windows. Whichever of them is open takes the answer.
     */
    public static boolean acceptCraftManager(final CraftManagerStatePayload payload) {
        return PatternsTerminalTab.accept(payload);
    }

    /** Offers the Pattern Studio's state to the Patterns heading, when that is what asked for it. */
    public static boolean acceptPatternStudio(final PatternStudioStatePayload payload) {
        return PatternsTerminalTab.accept(payload);
    }

    /*
     * What the space is showing, for a test that drives it the way a player does. They answer about the
     * drawing rather than about the menu, because a heading drawn in one row and clicked in another is the
     * exact bug the layout model exists to prevent and a reading off the menu would never see it.
     */

    /** The heading this machine is showing, as a {@code ComputerTerminalMenu.TAB_*}. */
    public int activeHeading() {
        return menu.activeTab();
    }

    /** The headings this machine offers, named in English whatever the player reads, in rail order. */
    public List<String> headings() {
        final List<String> out = new ArrayList<>();
        for (final int tab : railTabs()) {
            out.add(TAB_NAMES[tab].english());
        }
        return out;
    }

    /** Where to click for a named heading, relative to the glass, or null when it is not offered. */
    @Nullable
    public int[] headingPoint(final String name) {
        final List<String> rail = headings();
        final int row = rail.indexOf(name) - railScroll;
        if (row < 0 || row >= railVisible()) {
            return null;
        }
        return new int[]{RAIL_X + RAIL_W / 2, ComputerTerminalLayout.rowY(row) + TAB_H / 2};
    }

    /** Whether the question about taking a thing out of the network is up, for a test to check. */
    public boolean requestOpen() {
        return request.isOpen();
    }

    /** What the prompt has printed on this machine, one line after another, for a test to read. */
    public String consoleText() {
        return tabs != null && tabs[ComputerTerminalMenu.TAB_CONSOLE] instanceof ConsoleTerminalTab console
                ? console.glassText() : "";
    }

    /** Where the glass starts, for a heading that draws in world coordinates of its own. */
    int left() {
        return leftPos;
    }

    int top() {
        return topPos;
    }

    /**
     * What the machine's system calls itself, which is what its console greets a player with.
     *
     * <p>Sent with the window. It was read off the host's own disks on the client instead, and the client
     * cannot read a disk it is not holding: the prompt came up on a machine that could not say what it was
     * running, so it greeted nobody at all.
     */
    String systemName() {
        return menu.systemName();
    }

    // Background

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        syncPalette();
        final int x = leftPos;
        final int y = topPos;

        // The host computer's hardware-era monitor frame wraps the whole terminal window.
        MonitorFrame.renderBody(g, x, y, imageWidth, imageHeight, screenEra(), font);

        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, OUTER);
        g.fill(x, y, x + imageWidth, y + imageHeight, SCREEN);

        // The status bar across the top: the same reading whatever heading is showing.
        g.fill(x, y, x + imageWidth, y + BAR_H, RAIL);
        g.fill(x, y + BAR_H - 1, x + imageWidth, y + BAR_H, LINE);

        /*
         * The heading rail: a scrolling list of names, so every entry keeps its word even when a machine
         * offers more headings than fit at once.
         */
        final int railH = ComputerTerminalLayout.railHeight();
        g.fill(x + RAIL_X, y + BAR_H, x + RAIL_X + RAIL_W, y + imageHeight, RAIL);
        g.fill(x + RAIL_X + RAIL_W, y + BAR_H, x + RAIL_X + RAIL_W + 1, y + imageHeight, LINE);
        final int[] rail = railTabs();
        railScroll = ComputerTerminalLayout.clampScroll(railScroll, rail.length, railVisible());
        final int visible = railVisible();
        for (int row = 0; row < visible && railScroll + row < rail.length; row++) {
            final int i = railScroll + row;
            final int tab = rail[i];
            final int tx = x + RAIL_X;
            final int ty = y + TAB_Y0 + row * TAB_H;
            if (tab == menu.activeTab()) {
                g.fill(tx, ty, tx + RAIL_W, ty + TAB_H, TAB_ON);
                g.fill(tx, ty, tx + 2, ty + TAB_H, ACCENT);
            }
        }
        // Scroll hints: a small up/down chevron when there is more rail above or below.
        if (railScroll > 0) {
            g.fill(x + RAIL_X + RAIL_W / 2 - 2, y + TAB_Y0 - 1, x + RAIL_X + RAIL_W / 2 + 2, y + TAB_Y0, ACCENT);
        }
        if (railScroll < maxRailScroll()) {
            final int by = y + TAB_Y0 + railH - 1;
            g.fill(x + RAIL_X + RAIL_W / 2 - 2, by, x + RAIL_X + RAIL_W / 2 + 2, by + 1, ACCENT);
        }

        final int cx = x + CONTENT_X;
        final int cy = y;
        final int cw = contentW();
        final ITerminalTab heading = showing();
        if (heading != null) {
            heading.renderTabBg(g, x, y, cx, cy, cw, mouseX, mouseY, partialTick);
        }

        // The rule that separates the machine's own screen from the player's own rows.
        g.fill(x + RAIL_X + RAIL_W, y + ComputerTerminalLayout.INV_LINE_Y,
                x + imageWidth, y + ComputerTerminalLayout.INV_LINE_Y + 1, LINE);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                slotBg(g, x + ComputerTerminalMenu.INV_X + col * 18, y + menu.invY() + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            slotBg(g, x + ComputerTerminalMenu.INV_X + col * 18, y + menu.hotbarY());
        }
    }

    void inlineTrack(final GuiGraphics g, final int x, final int y, final int w,
                             final double f, final int color) {
        final int tx = x + 36;
        final int tw = w - 36 - 44;
        g.fill(tx, y + 1, tx + tw, y + 8, TRACK);
        g.fill(tx, y + 1, tx + tw, y + 2, LINE);
        final int fw = (int) Math.round((tw - 2) * Math.max(0, Math.min(1, f)));
        if (fw > 0) {
            g.fill(tx + 1, y + 2, tx + 1 + fw, y + 7, color);
        }
    }

    // The value shown for a disk's slider: the optimistic preview while dragging, else the synced value.
    int sliderValue(final int disk) {
        if (disk >= 0 && disk < sliderPreviewActive.length && sliderPreviewActive[disk]) {
            return sliderPreview[disk];
        }
        return menu.diskPermille(disk);
    }

    /*
     * Track geometry, panel-relative to absolute. The tracks live in the panel beside the grid, which is
     * the one place on this screen where the detail of what is selected belongs, and it is where the
     * Storage heading draws them too: one source for the drawing and for the drag.
     */
    private int sliderTrackX() {
        return leftPos + ComputerTerminalLayout.PANE_X + SLIDER_TRACK_LX;
    }

    private int sliderTrackW() {
        return ComputerTerminalLayout.PANE_W - SLIDER_TRACK_LX * 2;
    }

    private int sliderTrackY(final int disk) {
        return topPos + ComputerTerminalLayout.PANE_Y + SLIDER_TRACK0_DY + disk * SLIDER_ROW_PITCH;
    }

    /** The disk whose slider track the cursor is over (within a small vertical tolerance), or -1. */
    private int sliderDiskAt(final double mx, final double my) {
        if (menu.activeTab() != ComputerTerminalMenu.TAB_STORAGE || !menu.storageHasSlider()) {
            return -1;
        }
        final int tx = sliderTrackX();
        final int tw = sliderTrackW();
        if (mx < tx - 1 || mx > tx + tw + 1) {
            return -1;
        }
        for (int d = 0; d < menu.diskCount(); d++) {
            if (menu.diskCapacityWeight(d) <= 0L) {
                continue; // an empty disk slot has no draggable handle
            }
            final int ty = sliderTrackY(d);
            if (my >= ty - 3 && my <= ty + SLIDER_TRACK_H + 3) {
                return d;
            }
        }
        return -1;
    }

    /** Maps a cursor X to a per-mille for the given disk, snapped to the step grid unless Shift is held. */
    private int sliderPermilleAt(final int disk, final double mx, final boolean fine) {
        final int tx = sliderTrackX();
        final int span = Math.max(1, sliderTrackW() - SLIDER_HANDLE_W);
        final double frac = Math.max(0.0, Math.min(1.0, (mx - tx) / span));
        int permille = (int) Math.round(frac * 1000.0);
        if (!fine) {
            permille = Math.round(permille / (float) SLIDER_STEP) * SLIDER_STEP;
        }
        return Math.max(0, Math.min(1000, permille));
    }

    private void setSliderPreview(final int disk, final int permille) {
        if (disk >= 0 && disk < sliderPreview.length) {
            sliderPreview[disk] = permille;
            sliderPreviewActive[disk] = true;
        }
    }

    private void sendSliderValue(final int disk, final int permille) {
        PacketDistributor.sendToServer(new TerminalDiskPrivacyPayload(
                menu.monitorPos(), menu.hostPos(), disk, permille));
    }

    /*
     * The item grid and its toolbar, drawn the same way for the network's store and the machine's own,
     * so the two headings are pixel-identical and a player never has to learn one of them twice.
     */
    void gridBg(final GuiGraphics g, final int x, final int y, final int dy, final int rows) {
        final int tbx = x + NET_X;
        final int tby = y + TOOLBAR_Y + dy;
        g.fill(tbx, tby, tbx + SEARCH_W, tby + TOOLBAR_H, TRACK);
        g.fill(tbx, tby, tbx + SEARCH_W, tby + 1, LINE);
        final int mbx = x + MOD_X;
        g.fill(mbx, tby, mbx + MOD_W, tby + TOOLBAR_H, PANEL);
        g.fill(mbx, tby, mbx + MOD_W, tby + 1, modFilter.isEmpty() ? LINE : ACCENT);
        final int sbx = x + SORT_X;
        g.fill(sbx, tby, sbx + SORT_W, tby + TOOLBAR_H, PANEL);
        g.fill(sbx, tby, sbx + SORT_W, tby + 1, LINE);

        final List<NetworkItemEntry> items = visibleItems();
        final int start = clampScroll(items.size(), rows) * NET_COLS;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < NET_COLS; col++) {
                final int sx = x + NET_X + col * 18;
                final int sy = y + NET_Y + dy + row * 18;
                slotBg(g, sx, sy);
                final int idx = start + row * NET_COLS + col;
                if (idx < items.size()) {
                    final NetworkItemEntry e = items.get(idx);
                    drawDataIcon(g, e.key(), e.total(), sx, sy);
                    if (selectedEntry != null && selectedEntry.key().equals(e.key())) {
                        outline(g, sx, sy, ACCENT);
                    }
                }
            }
        }
        depositBar(g, x, y, dy);
    }

    /** A one-pixel ring round a cell, which is how the grid says which one the panel is describing. */
    private void outline(final GuiGraphics g, final int x, final int y, final int color) {
        g.fill(x - 1, y - 1, x + 17, y, color);
        g.fill(x - 1, y + 16, x + 17, y + 17, color);
        g.fill(x - 1, y, x, y + 16, color);
        g.fill(x + 16, y, x + 17, y + 16, color);
    }

    /** The panel beside the grid: a flat ground with a line across its top, as every panel here has. */
    void paneBg(final GuiGraphics g, final int x, final int y) {
        final int px = x + ComputerTerminalLayout.PANE_X;
        final int py = y + ComputerTerminalLayout.PANE_Y;
        g.fill(px, py, px + ComputerTerminalLayout.PANE_W, py + ComputerTerminalLayout.PANE_H, PANEL);
        g.fill(px, py, px + ComputerTerminalLayout.PANE_W, py + 1, LINE);
    }

    /** What the panel beside the grid is describing, which a click on a cell picks out. */
    @Nullable
    NetworkItemEntry selectedEntry() {
        return selectedEntry;
    }

    /**
     * Picks a thing out and asks the network where it is.
     *
     * <p>The rows of which server holds how much are the machine's answer, not a guess from the snapshot,
     * so the panel is the same answer the prompt gives, which is the whole point of it being there.
     */
    private void selectEntry(final NetworkItemEntry entry) {
        selectedEntry = entry;
        menu.setServerBreakdown(List.of());
        PacketDistributor.sendToServer(new RequestServerBreakdownPayload(
                menu.monitorPos(), menu.hostPos(), entry.key()));
    }

    /**
     * The pattern that makes the selected thing, when the network has one loaded, else null.
     *
     * <p>The answer is held onto, because the panel asks for it twice a frame while the catalogue only
     * changes when a fresh one arrives from the machine. Without that, every frame walked the whole
     * catalogue and built a key for each row in it.
     */
    @Nullable
    CraftCatalogPayload.Entry craftFor(@Nullable final NetworkItemEntry entry) {
        if (entry == null) {
            return null;
        }
        final List<CraftCatalogPayload.Entry> catalog = menu.craftCatalog();
        if (entry == craftForEntry && catalog == craftForCatalog) {
            return craftForResult;
        }
        CraftCatalogPayload.Entry found = null;
        for (final CraftCatalogPayload.Entry candidate : catalog) {
            if (StorageKey.of(candidate.result()).equals(entry.key())) {
                found = candidate;
                break;
            }
        }
        craftForEntry = entry;
        craftForCatalog = catalog;
        craftForResult = found;
        return found;
    }

    @Nullable
    private NetworkItemEntry craftForEntry;
    @Nullable
    private List<CraftCatalogPayload.Entry> craftForCatalog;
    @Nullable
    private CraftCatalogPayload.Entry craftForResult;

    /** The two things the panel lets a player do with what is picked out: ask for it, or have it made. */
    private boolean clickedPane(final double mouseX, final double mouseY) {
        if (selectedEntry == null) {
            return false;
        }
        if (inRect(mouseX, mouseY, leftPos + PANE_GET_X, topPos + PANE_BTN_Y, PANE_BTN_W, PANE_BTN_H)) {
            request.openFromNetwork(selectedEntry);
            syncSearchBoxVisibility();
            return true;
        }
        final CraftCatalogPayload.Entry pattern = craftFor(selectedEntry);
        if (pattern != null
                && inRect(mouseX, mouseY, leftPos + PANE_CRAFT_X, topPos + PANE_BTN_Y, PANE_BTN_W, PANE_BTN_H)) {
            openCraftPopup(pattern);
            return true;
        }
        return false;
    }

    void slotBg(final GuiGraphics g, final int x, final int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        g.fill(x, y, x + 16, y + 16, SLOT_BG);
    }

    static double frac(final int a, final int b) {
        return b <= 0 ? 0 : Math.min(1.0, (double) a / b);
    }

    void track(final GuiGraphics g, final int x, final int y, final int w,
                       final double f, final int color) {
        g.fill(x, y, x + w, y + 8, TRACK);
        g.fill(x, y, x + w, y + 1, LINE);
        final int fw = (int) Math.round((w - 2) * Math.max(0, Math.min(1, f)));
        if (fw > 0) {
            g.fill(x + 1, y + 1, x + 1 + fw, y + 7, color);
        }
    }

    // Labels / text

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        syncPalette();
        final int cx = CONTENT_X;
        final int cy = 0;
        final int cw = contentW();

        // The name of each heading, for the scrolling window of rail entries.
        final int[] rail = railTabs();
        final int visible = railVisible();
        for (int row = 0; row < visible && railScroll + row < rail.length; row++) {
            final int i = railScroll + row;
            final int ty = TAB_Y0 + row * TAB_H;
            g.drawString(font, GameText.resolve(TAB_NAMES[rail[i]]), RAIL_X + 8, ty + 5,
                    rail[i] == menu.activeTab() ? TAB_LABEL_ON : DIM, false);
        }

        renderStatusBar(g);

        final ITerminalTab heading = showing();
        if (heading != null) {
            heading.renderTabLabels(g, cx, cy, cw);
        } else {
            placeholder(g, cx, cy, GameText.resolve(TerminalTexts.NOT_AVAILABLE));
        }

        // The player's own rows, named so the two halves of the glass are never confused for one another.
        g.drawString(font, GameText.resolve(TerminalTexts.YOUR_INVENTORY), CONTENT_X,
                ComputerTerminalLayout.INV_LABEL_Y, DIM, false);
    }

    /**
     * The bar across the top: what this machine is, which network it is on and what that network holds.
     *
     * <p>The same reading whatever heading is showing, because it answers the question a player has every
     * time they look at the screen and should never have to change headings to ask.
     */
    private void renderStatusBar(final GuiGraphics g) {
        g.drawString(font, this.title, 6, 4, ACCENT, false);
        final int netState = menu.networkLinkState();
        int at = 6 + font.width(this.title) + 10;
        if (netState == 2) {
            g.drawString(font, GameText.resolve(TerminalTexts.NETWORK_CONFLICT), at, 4, RED, false);
        } else if (netState == 1) {
            final String servers = GameText.resolve(
                    (menu.serverCount() == 1 ? AssemblyTexts.ONE_SERVER : AssemblyTexts.SERVERS).with(menu.serverCount()));
            g.drawString(font, servers, at, 4, DIM, false);
            at += font.width(servers) + 10;
            final String held = GameText.resolve(TerminalTexts.HELD.with(fmt(menu.networkStorageUsed())));
            g.drawString(font, held, at, 4, TEXT, false);
        } else {
            g.drawString(font, GameText.resolve(TerminalTexts.NO_NETWORK), at, 4, DIM, false);
        }
        /*
         * What the machine itself is doing, at the far end: a build that will not run, a machine that is
         * switched off, and how many Operations the network has in flight.
         */
        final String state;
        final int stateColor;
        if (!menu.buildValid()) {
            state = GameText.resolve(TerminalTexts.BUILD_INVALID);
            stateColor = RED;
        } else if (!menu.running()) {
            state = GameText.resolve(TerminalTexts.HALTED);
            stateColor = AMBER;
        } else {
            final int live = menu.activeOps().size();
            state = GameText.resolve(live == 0 ? TerminalTexts.IDLE.text()
                    : (live == 1 ? TerminalTexts.ONE_OP : TerminalTexts.OPS).with(live));
            stateColor = live == 0 ? DIM : GREEN;
        }
        g.drawString(font, state, imageWidth - font.width(state) - 6, 4, stateColor, false);
    }

    // Craft tab: catalog grid + running/recent panels + request popup

    /* Mirrors of the Craft heading's own catalogue geometry, which this hit test reads. */
    private static final int CRAFT_COLS = ComputerTerminalLayout.GRID_COLS;
    private static final int CRAFT_ROWS = ComputerTerminalLayout.GRID_ROWS - 2;
    private static final int CRAFT_GRID_Y = ComputerTerminalLayout.GRID_Y;

    @Nullable
    private CraftCatalogPayload.Entry
            craftEntryAt(final int mouseX, final int mouseY) {
        final var catalog = menu.craftCatalog();
        final int gx = leftPos + NET_X;
        final int gy = topPos + CRAFT_GRID_Y;
        if (mouseX < gx || mouseX >= gx + CRAFT_COLS * 18 || mouseY < gy || mouseY >= gy + CRAFT_ROWS * 18) {
            return null;
        }
        final int col = (mouseX - gx) / 18;
        final int row = (mouseY - gy) / 18;
        final int index = (row + craftScroll) * CRAFT_COLS + col;
        return index < catalog.size() ? catalog.get(index) : null;
    }

    private void openCraftPopup(final CraftCatalogPayload.Entry entry) {
        craft.open(entry);
        syncSearchBoxVisibility();
    }


    private void placeholder(final GuiGraphics g, final int cx, final int cy, final String text) {
        g.drawString(font, text, cx + 6, cy + 28, DIM, false);
    }

    // Network tab: a virtual item grid drawn from the snapshot

    /** The number of grid rows, which is the same on every heading that draws one. */
    private int gridRows() {
        return NET_ROWS;
    }

    /** Which mod an entry comes from, which is the namespace of whatever is behind it. */
    private static String modOf(final NetworkItemEntry entry) {
        final ResourceLocation id = entry.key().registryId();
        return id == null ? "" : id.getNamespace();
    }

    /**
     * The mods whose items the network holds, in the order they are named, with the empty string first
     * for "every one of them". What the Mod button steps through.
     */
    List<String> modsPresent() {
        final List<NetworkItemEntry> source = menu.activeTab() == ComputerTerminalMenu.TAB_STORAGE
                ? menu.localItems() : menu.networkItems();
        final List<String> out = new ArrayList<>();
        out.add("");
        for (final NetworkItemEntry e : source) {
            final String mod = modOf(e);
            if (!mod.isEmpty() && !out.contains(mod)) {
                out.add(mod);
            }
        }
        out.subList(1, out.size()).sort(String::compareToIgnoreCase);
        return out;
    }

    /** Which mod's items are being shown, or the empty string for every one of them. */
    String modFilter() {
        return modFilter;
    }

    /** Steps the Mod button on, wrapping back to every mod at the end of the list. */
    private void cycleModFilter(final int step) {
        final List<String> mods = modsPresent();
        final int at = Math.max(0, mods.indexOf(modFilter));
        modFilter = mods.get(Math.floorMod(at + step, mods.size()));
        netScrollRow = 0;
    }

    List<NetworkItemEntry> visibleItems() {
        final String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        final List<NetworkItemEntry> source = menu.activeTab() == ComputerTerminalMenu.TAB_STORAGE
                ? menu.localItems() : menu.networkItems();
        // A snapshot replaces the menu's list object, so the list itself tells a fresh snapshot from the last one.
        final String key = menu.activeTab() + "|" + sortByQuantity + "|" + modFilter + "|" + q;
        if (source == visibleSource && key.equals(visibleKey)) {
            return visibleCache;
        }
        final List<NetworkItemEntry> out = new ArrayList<>();
        for (final NetworkItemEntry e : source) {
            if (!modFilter.isEmpty() && !modFilter.equals(modOf(e))) {
                continue;
            }
            if (q.isEmpty()
                    || e.name().getString().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(e);
            }
        }
        if (sortByQuantity) {
            out.sort((a, b) -> Long.compare(b.total(), a.total()));
        } else {
            out.sort((a, b) -> a.name().getString()
                    .compareToIgnoreCase(b.name().getString()));
        }
        visibleCache = out;
        visibleSource = source;
        visibleKey = key;
        return out;
    }

    private void depositBar(final GuiGraphics g, final int x, final int y, final int yShift) {
        final int dx = x + DEPOSIT_X;
        final int dy = y + DEPOSIT_Y + yShift;
        final boolean holding = !menu.getCarried().isEmpty();
        /*
         * A fill with a line across its top, the way every other button on this screen is drawn. It had a
         * border around it, which cost a pixel on each side of a button that only has sixteen to live in
         * between the rule and the player's own rows, and so it sat on the rule.
         */
        g.fill(dx, dy, dx + DEPOSIT_W, dy + DEPOSIT_H, holding ? 0xFF123038 : TRACK);
        g.fill(dx, dy, dx + DEPOSIT_W, dy + 1, holding ? ACCENT : LINE);
        // Down-arrow glyph (deposit into the network), sized to sit inside the button with a pixel to spare.
        final int gx = dx + 4;
        final int gy = dy + 2;
        final int gc = holding ? ACCENT : DIM;
        g.fill(gx + 2, gy, gx + 4, gy + 4, gc);
        g.fill(gx, gy + 3, gx + 6, gy + 4, gc);
        g.fill(gx + 1, gy + 4, gx + 5, gy + 5, gc);
        g.fill(gx + 2, gy + 5, gx + 4, gy + 6, gc);
    }

    private int clampScroll(final int count, final int visibleRows) {
        final int rows = (count + NET_COLS - 1) / NET_COLS;
        final int max = Math.max(0, rows - visibleRows);
        netScrollRow = Math.max(0, Math.min(max, netScrollRow));
        return netScrollRow;
    }

    @Nullable
    private NetworkItemEntry networkItemAt(final int mx, final int my) {
        final int relX = mx - (leftPos + NET_X);
        final int relY = my - (topPos + NET_Y);
        if (relX < 0 || relY < 0 || relX % 18 > 16 || relY % 18 > 16) {
            return null;
        }
        final int col = relX / 18;
        final int row = relY / 18;
        if (col >= NET_COLS || row >= gridRows()) {
            return null;
        }
        final List<NetworkItemEntry> items = visibleItems();
        final int idx = (netScrollRow + row) * NET_COLS + col;
        return idx >= 0 && idx < items.size() ? items.get(idx) : null;
    }

    // Operations tab: recent network Operations + provenance detail

    void moveRow(final GuiGraphics g, final int cx, final int my, final OperationRecord.MoveRow mv) {
        /*
         * Either end can be a "host (program)" label, so the parts are measured instead of sitting in fixed
         * columns: the origin takes up to half the row, the arrow follows it, and the quantity and the
         * destination get what is left.
         */
        final int left = cx + 6;
        final int right = cx + ComputerTerminalLayout.POPUP_W - 6;
        final String from = font.plainSubstrByWidth(mv.from(), (right - left) / 2 - 6);
        g.drawString(font, from, left, my, DIM, false);
        final int arrowX = left + font.width(from) + 4;
        g.drawString(font, ">", arrowX, my, ACCENT, false);
        final int tailX = arrowX + font.width(">") + 4;
        g.drawString(font, font.plainSubstrByWidth(fmt(mv.qty()) + " " + mv.to(), right - tailX),
                tailX, my, TEXT, false);
    }

    int statusColor(final byte status) {
        return switch (status) {
            case OperationRecord.STATUS_COMPLETED -> GREEN;
            case OperationRecord.STATUS_PARTIAL, OperationRecord.STATUS_WAITING -> AMBER;
            case OperationRecord.STATUS_PROCESSING -> ACCENT2;
            case OperationRecord.STATUS_PENDING -> DIM;
            default -> RED;
        };
    }

    static String statusLabel(final byte status) {
        return GameText.resolve(switch (status) {
            case OperationRecord.STATUS_COMPLETED -> TerminalTexts.STATUS_COMPLETED;
            case OperationRecord.STATUS_PARTIAL -> TerminalTexts.STATUS_PARTIAL;
            case OperationRecord.STATUS_PROCESSING -> TerminalTexts.STATUS_PROCESSING;
            case OperationRecord.STATUS_WAITING -> TerminalTexts.STATUS_WAITING;
            case OperationRecord.STATUS_RESOURCE_LOCKED -> TerminalTexts.STATUS_RESOURCE_LOCKED;
            case OperationRecord.STATUS_PENDING -> TerminalTexts.STATUS_PENDING;
            default -> TerminalTexts.STATUS_FAILED;
        });
    }

    static String opTypeLabel(final byte type) {
        return switch (type) {
            case OperationRecord.TYPE_INSERT -> "INSERT";
            case OperationRecord.TYPE_DELETE -> "DELETE";
            case OperationRecord.TYPE_MOVE -> "MOVE";
            case OperationRecord.TYPE_ANALYZE -> "ANALYZE";
            case OperationRecord.TYPE_REINDEX -> "REINDEX";
            case OperationRecord.TYPE_VACUUM -> "VACUUM";
            case OperationRecord.TYPE_DROP -> "DROP";
            case OperationRecord.TYPE_CRAFT -> "CRAFT";
            default -> "SELECT";
        };
    }

    /*
     * Tasks tab (Mainframe only): the network's Operations in flight. Not to be confused with the desktop's
     * Task Manager, which is about one machine; this one is about the network the Mainframe orchestrates.
     */

    // Maintenance tab (Mainframe-only): index stats + ANALYZE / VACUUM / REINDEX / DROP

    private int maintButtonAt(final int mx, final int my) {
        final int cx = leftPos + CONTENT_X;
        final int cy = topPos;
        final int cw = contentW();
        final int halfW = (cw - 4) / 2;
        if (inRect(mx, my, cx, cy + MNT_BTN_ROW1_Y, halfW, MNT_BTN_H)) {
            return 0;
        }
        if (inRect(mx, my, cx + halfW + 4, cy + MNT_BTN_ROW1_Y, halfW, MNT_BTN_H)) {
            return 1;
        }
        if (inRect(mx, my, cx, cy + MNT_BTN_REINDEX_Y, cw, MNT_BTN_H)) {
            return 2;
        }
        if (inRect(mx, my, cx, cy + MNT_BTN_DROP_Y, cw, MNT_BTN_H)) {
            return 3;
        }
        return -1;
    }

    private void openDrop() {
        drop.open();
        syncSearchBoxVisibility();
    }

    private void sendMaintenance(final int action) {
        PacketDistributor.sendToServer(
                new TerminalMaintenancePayload(menu.monitorPos(), menu.hostPos(), action));
    }

    private int taskSubTabAt(final int mx, final int my) {
        final int barY = topPos + 24;
        if (my < barY || my >= barY + 14) {
            return -1;
        }
        final int sw = contentW() / 3;
        final int rel = mx - (leftPos + CONTENT_X);
        if (rel < 0) {
            return -1;
        }
        final int sub = rel / sw;
        return sub >= 0 && sub < 3 ? sub : -1;
    }

    int clampOpScroll(final int size) {
        final int max = Math.max(0, size - OPS_ROWS);
        opScroll = Math.max(0, Math.min(max, opScroll));
        return opScroll;
    }

    private int opsRowAt(final int mx, final int my) {
        if (mx < leftPos + CONTENT_X || mx >= leftPos + imageWidth - 6) {
            return -1;
        }
        final int rel = my - (topPos + 32);
        if (rel < 0) {
            return -1;
        }
        final int row = rel / 12;
        if (row < 0 || row >= OPS_ROWS) {
            return -1;
        }
        final int idx = opScroll + row;
        return idx < menu.operationsLog().size() ? idx : -1;
    }

    void tile(final GuiGraphics g, final int x, final int y, final String key,
                      final String value, final String unit) {
        g.drawString(font, key, x + 4, y + 4, DIM, false);
        g.drawString(font, value, x + 4, y + 14, TEXT, false);
        if (!unit.isEmpty()) {
            g.drawString(font, unit, x + 5 + font.width(value), y + 16, DIM, false);
        }
    }

    void barLabel(final GuiGraphics g, final int x, final int y, final int w,
                          final String label, final String value) {
        g.drawString(font, label, x, y, TEXT, false);
        g.drawString(font, value, x + w - font.width(value), y, DIM, false);
    }

    // Interaction

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        // The DROP popup is modal: ESC closes it; every other key is swallowed.
        if (drop.isOpen()) {
            if (key == 256) {
                drop.close();
                syncSearchBoxVisibility();
            }
            return true;
        }
        /*
         * The two questions that take a number are modal: ESC closes, Enter says go, typing goes to the
         * field, and every other key is swallowed so the inventory key never closes the screen mid-edit.
         */
        if (craft.isOpen()) {
            if (key == 256) {
                craft.close();
                syncSearchBoxVisibility();
            } else if (key == 257 || key == 335) {
                craft.submitIfPossible();
                syncSearchBoxVisibility();
            } else if (qtyBox.focused()) {
                qtyBox.keyPressed(key, scan, mods);
            }
            return true;
        }
        if (request.isOpen()) {
            if (key == 256) {
                request.close();
                syncSearchBoxVisibility();
            } else if (key == 257 || key == 335) {
                request.submit();
                syncSearchBoxVisibility();
            } else if (qtyBox.focused()) {
                qtyBox.keyPressed(key, scan, mods);
            }
            return true;
        }
        /*
         * A heading that is typed into takes every key before the screen's own shortcuts do: the prompt
         * is a heading here now, and a terminal that let the inventory key close the screen mid-line
         * would be a terminal nobody could type an 'e' at.
         */
        final ITerminalTab heading = showing();
        if (heading != null && heading.onKeyPressed(key, scan, mods)) {
            return true;
        }
        /*
         * A focused privacy slider takes the arrow / Home / End keys: arrows nudge by the snap step
         * (Shift = fine 1 per-mille), Home/End jump to fully private / fully public. Each key commits.
         */
        if (menu.activeTab() == ComputerTerminalMenu.TAB_STORAGE && menu.storageHasSlider()
                && focusedSliderDisk >= 0 && focusedSliderDisk < menu.diskCount()
                && (searchBox == null || !searchBox.isFocused())) {
            final int disk = focusedSliderDisk;
            final int stepKey = (mods & 0x0001) != 0 ? 1 : SLIDER_STEP; // GLFW_MOD_SHIFT = 1
            Integer next = null;
            switch (key) {
                case 263 -> next = sliderValue(disk) - stepKey; // left arrow
                case 262 -> next = sliderValue(disk) + stepKey; // right arrow
                case 268 -> next = 0;                           // Home
                case 269 -> next = 1000;                        // End
                default -> { /* not a slider key */ }
            }
            if (next != null) {
                final int permille = Math.max(0, Math.min(1000, next));
                setSliderPreview(disk, permille);
                sendSliderValue(disk, permille);
                return true;
            }
        }
        /*
         * While the search field has focus, route typing to it; ESC unfocuses it; never let a letter
         * key fall through and close the GUI.
         */
        if (searchBox != null && searchBox.isFocused()) {
            if (key == 256) {
                searchBox.setFocused(false);
                setFocused(null);
                return true;
            }
            searchBox.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(final char c, final int mods) {
        if ((request.isOpen() || craft.isOpen()) && qtyBox.focused()) {
            return qtyBox.charTyped(c, mods);
        }
        if (searchBox != null && searchBox.isFocused()) {
            return searchBox.charTyped(c, mods);
        }
        final ITerminalTab heading = showing();
        if (!anyQuestionOpen() && heading != null && heading.onCharTyped(c, mods)) {
            return true;
        }
        return super.charTyped(c, mods);
    }

    @Override
    public boolean keyReleased(final int key, final int scan, final int mods) {
        final ITerminalTab heading = showing();
        if (heading != null && heading.onKeyReleased(key, scan, mods)) {
            return true;
        }
        return super.keyReleased(key, scan, mods);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        /*
         * An editor at the prompt uses Escape for its own modes, and a terminal in the middle of one is
         * the last place a stray Escape should shut the whole machine's screen.
         */
        final ITerminalTab heading = showing();
        return heading == null || !heading.wantsEscape();
    }

    @Override
    public void removed() {
        for (final ITerminalTab tab : tabs == null ? new ITerminalTab[0] : tabs) {
            if (tab != null) {
                tab.onRemoved();
            }
        }
        super.removed();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (drop.isOpen()) {
            final boolean was = drop.isOpen();
            final boolean taken = drop.clicked(mouseX, mouseY, button);
            if (was && !drop.isOpen()) {
                syncSearchBoxVisibility();
            }
            return taken;
        }
        if (craft.isOpen()) {
            final boolean taken = craft.clicked(mouseX, mouseY, button);
            if (!craft.isOpen()) {
                syncSearchBoxVisibility();
            }
            return taken;
        }
        if (request.isOpen()) {
            final boolean taken = request.clicked(mouseX, mouseY, button);
            if (!request.isOpen()) {
                syncSearchBoxVisibility();
            }
            return taken;
        }
        if (opPopup.isOpen()) {
            final boolean taken = opPopup.clicked(mouseX, mouseY, button);
            if (!opPopup.isOpen()) {
                syncSearchBoxVisibility();
            }
            return taken;
        }
        /*
         * Let the active content tab claim the press (e.g. the Processes tab's action buttons), after any
         * modal popup above has had its chance but before the rail/grid handlers below.
         */
        final ITerminalTab heading = showing();
        if (heading != null && heading.onMouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        /*
         * Privacy slider (Storage tab): a press on a disk's track starts a drag and jumps the value to
         * the cursor. Handled before the deposit/grid handlers so a slider drag never deposits a stack.
         */
        if (button == 0) {
            final int disk = sliderDiskAt(mouseX, mouseY);
            if (disk >= 0) {
                draggingSliderDisk = disk;
                focusedSliderDisk = disk;
                setSliderPreview(disk, sliderPermilleAt(disk, mouseX, hasShiftDown()));
                return true;
            }
        }
        /*
         * Clicking the search field selects it for typing; clicking elsewhere deselects it. Container
         * screens don't reliably route focus to widgets, so do it explicitly.
         */
        if (searchBox != null && searchBox.visible) {
            if (searchBox.isMouseOver(mouseX, mouseY)) {
                setFocused(searchBox);
                searchBox.setFocused(true);
                return searchBox.mouseClicked(mouseX, mouseY, button);
            }
            searchBox.setFocused(false);
        }
        if (clickedWithHeldStack(mouseX, mouseY, button)) {
            return true;
        }
        /*
         * Storage tab: clicking an item with an empty cursor opens the actions popup, where the player
         * sets a quantity and sends it to their inventory or up into the network.
         */
        if (menu.activeTab() == ComputerTerminalMenu.TAB_STORAGE && menu.getCarried().isEmpty()
                && button == 0) {
            final NetworkItemEntry e = networkItemAt((int) mouseX, (int) mouseY);
            if (e != null) {
                request.openFromStorage(e);
                syncSearchBoxVisibility();
                return true;
            }
        }
        if (button == 0 && (clickedRail(mouseX, mouseY) || clickedTabContent(mouseX, mouseY))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Handing a held stack to the machine: clicking the grid or the deposit bar puts it into the network
     * (Network tab) or the computer's own storage (Storage tab).
     *
     * <p>Left puts the whole stack in as items and right puts in one, or what a held container holds. A held
     * empty container right-clicked on a fluid or chemical entry fills from it, so which entry the cursor was
     * over travels with a right-click and not with a left one.
     */
    private boolean clickedWithHeldStack(final double mouseX, final double mouseY, final int button) {
        if (!isGridTab() || menu.getCarried().isEmpty() || (button != 0 && button != 1)
                || (!overDepositBar(mouseX, mouseY) && !overNetworkGrid(mouseX, mouseY))) {
            return false;
        }
        final Optional<StorageKey> entry = button == 1
                ? Optional.ofNullable(networkItemAt((int) mouseX, (int) mouseY)).map(NetworkItemEntry::key)
                : Optional.empty();
        if (menu.activeTab() == ComputerTerminalMenu.TAB_STORAGE) {
            PacketDistributor.sendToServer(new TerminalLocalDepositPayload(menu.monitorPos(), menu.hostPos(),
                    button == 1 ? TerminalLocalDepositPayload.CURSOR_ONE : TerminalLocalDepositPayload.CURSOR,
                    entry));
        } else {
            PacketDistributor.sendToServer(new TerminalInsertPayload(menu.monitorPos(), menu.hostPos(),
                    button == 1 ? TerminalInsertPayload.CURSOR_ONE : TerminalInsertPayload.CURSOR, entry));
        }
        return true;
    }

    /**
     * The rail down the left side, which is how the space changes what it is showing.
     *
     * <p>Every entry on it switches the content beside it, the prompt included. It used to be that clicking
     * the prompt threw a separate window over the screen instead, which is a thing a screen that is the
     * whole machine has no business doing to itself.
     */
    private boolean clickedRail(final double mouseX, final double mouseY) {
        final int[] rail = railTabs();
        final int row = ComputerTerminalLayout.rowAt(mouseX - leftPos, mouseY - topPos, railVisible());
        if (row < 0 || railScroll + row >= rail.length) {
            return false;
        }
        final int tab = rail[railScroll + row];
        if (tab != menu.activeTab()) {
            menu.setActiveTab(tab);
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, tab);
            }
            syncSearchBoxVisibility();
        }
        return true;
    }

    /**
     * A left click on whichever tab is showing: a recipe to request, an item to ask the network for, a
     * logged or running operation to look into, or one of the maintenance actions.
     */
    private boolean clickedTabContent(final double mouseX, final double mouseY) {
        // The Craft tab: clicking a catalog entry opens the request popup.
        if (menu.activeTab() == ComputerTerminalMenu.TAB_CRAFT && menu.getCarried().isEmpty()) {
            final var entry = craftEntryAt((int) mouseX, (int) mouseY);
            if (entry != null) {
                openCraftPopup(entry);
                return true;
            }
        }
        if (isGridTab()) {
            if (inRect(mouseX, mouseY, leftPos + SORT_X, topPos + TOOLBAR_Y,
                    SORT_W, TOOLBAR_H)) {
                sortByQuantity = !sortByQuantity;
                netScrollRow = 0;
                return true;
            }
            if (inRect(mouseX, mouseY, leftPos + MOD_X, topPos + TOOLBAR_Y, MOD_W, TOOLBAR_H)) {
                cycleModFilter(hasShiftDown() ? -1 : 1);
                return true;
            }
            /*
             * A click on the network's grid picks the thing out rather than asking for it: the panel beside
             * the grid then says everything the machine knows about it, and the asking is done there. The
             * machine's own store keeps its popup, because what is done to a local stack is taking or giving
             * it and there is nothing else to say about it.
             */
            if (menu.activeTab() == ComputerTerminalMenu.TAB_NETWORK) {
                final NetworkItemEntry e = networkItemAt((int) mouseX, (int) mouseY);
                if (e != null) {
                    selectEntry(e);
                    return true;
                }
                if (clickedPane(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        if (menu.activeTab() == ComputerTerminalMenu.TAB_OPS) {
            final int row = opsRowAt((int) mouseX, (int) mouseY);
            if (row >= 0) {
                selectedOp = row;
                openOpPopup(menu.operationsLog().get(row)); // click a logged op -> SubOperations popup
                return true;
            }
        }
        if (menu.activeTab() == ComputerTerminalMenu.TAB_TASKS) {
            final int sub = taskSubTabAt((int) mouseX, (int) mouseY);
            if (sub >= 0) {
                taskSubTab = sub;
                return true;
            }
            final int opRow = taskOpRowAt((int) mouseX, (int) mouseY);
            if (opRow >= 0) {
                openOpPopup(menu.activeOps().get(opRow)); // click an in-flight op -> SubOperations popup
                return true;
            }
        }
        return menu.activeTab() == ComputerTerminalMenu.TAB_MAINTENANCE
                && clickedMaintenance(mouseX, mouseY);
    }

    /** The maintenance actions, each of which is logged as an Operation the way a database records one. */
    private boolean clickedMaintenance(final double mouseX, final double mouseY) {
        switch (maintButtonAt((int) mouseX, (int) mouseY)) {
            case 0 -> {
                sendMaintenance(TerminalMaintenancePayload.ACTION_ANALYZE);
                maintHint = TerminalTexts.LOGGED.with("ANALYZE");
                return true;
            }
            case 1 -> {
                sendMaintenance(TerminalMaintenancePayload.ACTION_VACUUM);
                maintHint = TerminalTexts.LOGGED.with("VACUUM");
                return true;
            }
            case 2 -> {
                sendMaintenance(TerminalMaintenancePayload.ACTION_REINDEX);
                maintHint = TerminalTexts.LOGGED.with("REINDEX");
                return true;
            }
            case 3 -> {
                openDrop();
                return true;
            }
            default -> {
                return false; // clicked empty space
            }
        }
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button,
                                final double dragX, final double dragY) {
        // A live slider drag updates the optimistic preview every frame; nothing is sent until release.
        if (draggingSliderDisk >= 0 && button == 0) {
            setSliderPreview(draggingSliderDisk, sliderPermilleAt(draggingSliderDisk, mouseX, hasShiftDown()));
            return true;
        }
        final ITerminalTab heading = showing();
        if (heading != null && heading.onMouseDragged(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        // Releasing a slider drag commits the value once (one packet per drag, not per pixel).
        if (draggingSliderDisk >= 0 && button == 0) {
            final int disk = draggingSliderDisk;
            final int permille = sliderPermilleAt(disk, mouseX, hasShiftDown());
            setSliderPreview(disk, permille);
            sendSliderValue(disk, permille);
            draggingSliderDisk = -1;
            return true;
        }
        final ITerminalTab heading = showing();
        if (heading != null && heading.onMouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /* Ticks to wait for the menu's synchronised data before trusting it to shape the rail. */
    private static final int SETTLE_TICKS = 5;

    private int ticksOpen;

    @Override
    protected void containerTick() {
        super.containerTick();
        // Keep the text fields in step with the host era resolved by the base, so a board swap recolors them.
        if (searchBox != null) {
            searchBox.setTextColor(theme.text());
        }
        qtyBox.setTextColor(theme.text());
        /*
         * Drop a slider's optimistic preview once the authoritative sync has caught up to it (or while
         * it is not being dragged and the server reports a different, clamped value), so a rejected
         * value visibly corrects and later syncs drive the display.
         */
        for (int d = 0; d < sliderPreviewActive.length; d++) {
            if (sliderPreviewActive[d] && draggingSliderDisk != d
                    && d < menu.diskCount() && menu.diskPermille(d) == sliderPreview[d]) {
                sliderPreviewActive[d] = false;
            }
        }
        /*
         * A heading can stop being offered while somebody is looking at it: pull the Crafting Card and
         * Patterns is gone, unplug the network and Craft is. The rail is what the machine offers, so the
         * screen falls back to the network rather than drawing a heading that is no longer on it.
         */
        final int active = menu.activeTab();
        boolean onRail = false;
        for (final int tab : railTabs()) {
            onRail |= tab == active;
        }
        /*
         * Hold that judgement for the first few ticks. The rail's shape is read from the synchronised menu
         * data, which arrives a tick or two after the window does, so a screen reopened on Craft or Patterns
         * would otherwise be thrown back to the network before the machine had a chance to say it offers it.
         */
        if (ticksOpen < SETTLE_TICKS) {
            ticksOpen++;
            onRail = true;
        }
        if (!onRail) {
            menu.setActiveTab(ComputerTerminalMenu.TAB_NETWORK);
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId,
                        ComputerTerminalMenu.TAB_NETWORK);
            }
            syncSearchBoxVisibility();
        }
        craft.tick();
        final ITerminalTab heading = showing();
        if (heading != null) {
            heading.onContainerTick();
        }
    }

    @Override
    protected void slotClicked(final Slot slot, final int slotId, final int button, final ClickType type) {
        /*
         * On the Network/Storage tabs, shift-clicking an inventory stack deposits it into the network
         * or local storage respectively, instead of a (no-op) quick-move.
         */
        if (isGridTab() && type == ClickType.QUICK_MOVE
                && slot != null && slot.hasItem() && slot.index >= menu.storageSlotCount()) {
            if (menu.activeTab() == ComputerTerminalMenu.TAB_STORAGE) {
                PacketDistributor.sendToServer(new TerminalLocalDepositPayload(
                        menu.monitorPos(), menu.hostPos(), slot.index, Optional.empty()));
            } else {
                PacketDistributor.sendToServer(new TerminalInsertPayload(
                        menu.monitorPos(), menu.hostPos(), slot.index, Optional.empty()));
            }
            return;
        }
        super.slotClicked(slot, slotId, button, type);
    }

    private boolean overNetworkGrid(final double mx, final double my) {
        final int gy = topPos + NET_Y;
        return mx >= leftPos + NET_X && mx < leftPos + NET_X + NET_COLS * 18
                && my >= gy && my < gy + gridRows() * 18;
    }

    private boolean isGridTab() {
        return menu.activeTab() == ComputerTerminalMenu.TAB_NETWORK
                || menu.activeTab() == ComputerTerminalMenu.TAB_STORAGE;
    }

    private boolean overDepositBar(final double mx, final double my) {
        final int dx = leftPos + DEPOSIT_X;
        final int dy = topPos + DEPOSIT_Y;
        return mx >= dx && mx < dx + DEPOSIT_W && my >= dy && my < dy + DEPOSIT_H;
    }

    private void openOpPopup(final OperationRecord op) {
        opPopup.open(op);
        syncSearchBoxVisibility();
    }

    private int taskOpRowAt(final int mx, final int my) {
        if (menu.activeTab() != ComputerTerminalMenu.TAB_TASKS || taskSubTab != 0) {
            return -1;
        }
        if (mx < leftPos + CONTENT_X || mx >= leftPos + imageWidth - 6) {
            return -1;
        }
        final int rel = my - (topPos + 90);
        if (rel < 0) {
            return -1;
        }
        final int row = rel / 14;
        return row >= 0 && row < Math.min(TASK_OP_ROWS, menu.activeOps().size()) ? row : -1;
    }


    private static boolean inRect(final double mx, final double my, final int x, final int y,
                                  final int w, final int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }


    void drawDataIcon(final GuiGraphics g, final StorageKey key, final long count,
                              final int x, final int y) {
        if (!key.isItem()) {
            if (key.isChemical()) {
                ChemicalSprite.draw(g, key, x, y);
            } else {
                FluidSprite.draw(g, key.fluidPrototype(), x, y);
            }
            if (count >= 0L) {
                final String c = fmt(count);
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.drawString(font, c, x + 17 - font.width(c), y + 9, 0xFFFFFFFF, true);
                g.pose().popPose();
            }
        } else {
            g.renderItem(key.stack(1), x, y);
            if (count >= 0L) {
                g.renderItemDecorations(font, key.stack(1), x, y, fmt(count));
            }
        }
    }

    static String fmt(final long n) {
        if (n < 10_000) {
            return String.format("%,d", n);
        }
        if (n < 1_000_000) {
            return String.format("%.1fk", n / 1_000.0);
        }
        return String.format("%.1fM", n / 1_000_000.0);
    }

    @Override
    public boolean mouseScrolled(final double mx, final double my, final double dx, final double dy) {
        // Scrolling over the tab rail moves the rail when it holds more entries than fit.
        if (dy != 0 && mx >= leftPos + RAIL_X && mx < leftPos + RAIL_X + RAIL_W && maxRailScroll() > 0) {
            railScroll = ComputerTerminalLayout.clampScroll(
                    railScroll - (int) Math.signum(dy), railTabs().length, railVisible());
            return true;
        }
        if (drop.scrolled(dy)) {
            return true;
        }
        if (opPopup.scrolled(dy)) {
            return true;
        }
        if (!craft.isOpen() && menu.activeTab() == ComputerTerminalMenu.TAB_CRAFT && dy != 0) {
            craftScroll = Math.max(0, craftScroll - (int) Math.signum(dy));
            return true;
        }
        if (isGridTab() && dy != 0) {
            final int rows = (visibleItems().size() + NET_COLS - 1) / NET_COLS;
            final int max = Math.max(0, rows - NET_ROWS);
            netScrollRow = Math.max(0, Math.min(max, netScrollRow - (int) Math.signum(dy)));
            return true;
        }
        if (menu.activeTab() == ComputerTerminalMenu.TAB_OPS && dy != 0) {
            final int max = Math.max(0, menu.operationsLog().size() - OPS_ROWS);
            opScroll = Math.max(0, Math.min(max, opScroll - (int) Math.signum(dy)));
            return true;
        }
        final ITerminalTab heading = showing();
        if (heading != null && heading.onMouseScrolled(mx, my, dy)) {
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        syncSearchBoxVisibility();
        /*
         * The base render() binds the era skin, draws the screen, and renders the slot tooltip; the popups and
         * custom hover tooltips below draw afterward using the palette fields refreshed during renderBg/Labels.
         */
        super.render(g, mouseX, mouseY, partialTick);
        if (drop.isOpen()) {
            drop.render(g, mouseX, mouseY);
        } else if (craft.isOpen()) {
            craft.render(g, mouseX, mouseY);
        } else if (request.isOpen()) {
            request.render(g, mouseX, mouseY, partialTick);
        } else if (opPopup.isOpen()) {
            opPopup.render(g);
        } else if (isGridTab()) {
            final boolean local = menu.activeTab() == ComputerTerminalMenu.TAB_STORAGE;
            final TextKey where = local ? TerminalTexts.IN_STORAGE : TerminalTexts.ON_THE_NETWORK;
            renderNetworkHover(g, mouseX, mouseY);
            if (overDepositBar(mouseX, mouseY)) {
                g.renderComponentTooltip(font, List.of(
                        GameText.component(local ? TerminalTexts.DEPOSIT_LOCAL : TerminalTexts.DEPOSIT_NETWORK),
                        GameText.component(TerminalTexts.DEPOSIT_CLICK).withStyle(ChatFormatting.GRAY),
                        GameText.component(TerminalTexts.DEPOSIT_RIGHT_CLICK).withStyle(ChatFormatting.GRAY),
                        GameText.component(TerminalTexts.DEPOSIT_SHIFT_CLICK).withStyle(ChatFormatting.GRAY)),
                        mouseX, mouseY);
                return;
            }
            final NetworkItemEntry e = networkItemAt(mouseX, mouseY);
            if (e != null) {
                final List<Component> lines = new ArrayList<>();
                lines.add(e.name());
                lines.add(GameText.component(where.with(String.format("%,d", e.total())))
                        .withStyle(ChatFormatting.GRAY));
                if (local) {
                    lines.add(GameText.component(TerminalTexts.TAKE_CLICK).withStyle(ChatFormatting.DARK_GRAY));
                    lines.add(GameText.component(TerminalTexts.TAKE_SHIFT_CLICK).withStyle(ChatFormatting.DARK_GRAY));
                }
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
    }

    private void renderNetworkHover(final GuiGraphics g, final int mx, final int my) {
        final int gx = leftPos + NET_X;
        /*
         * Match the grid's per-tab vertical shift and row count so the highlight tracks the cell the
         * tooltip hit-test (networkItemAt) reports -- on the Storage tab the slider band pushes both down.
         */
        final int gy = topPos + NET_Y;
        final int relX = mx - gx;
        final int relY = my - gy;
        if (relX < 0 || relY < 0 || relX % 18 > 15 || relY % 18 > 15) {
            return;
        }
        final int col = relX / 18;
        final int row = relY / 18;
        if (col >= NET_COLS || row >= gridRows()) {
            return;
        }
        final int sx = gx + col * 18;
        final int sy = gy + row * 18;
        // Items render above flat fills, so push the highlight forward to sit over them.
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        g.fill(sx, sy, sx + 16, sy + 16, 0x80FFFFFF);
        g.pose().popPose();
    }

    @Override
    protected HardwareEra screenEra() {
        /*
         * Read the host computer's era straight from its block entity on the client, so the very first frame
         * already wears the right era skin. Relying only on the synced era slot lagged one tick and flashed
         * the default era when the GUI opened. Fall back to the synced value if the host isn't client-loaded.
         */
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(menu.hostPos())
                instanceof AbstractComputerBlockEntity host) {
            return host.displayEra();
        }
        return menu.hardwareEra();
    }
}
