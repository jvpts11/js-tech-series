/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.DeviceManagerLayout;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.devices.DeviceMap;
import dev.jstech.computers.os.devices.DeviceRows;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.MenuBar;
import dev.jstech.core.client.gui.component.ScrollBar;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import static dev.jstech.computers.gui.layout.DeviceManagerLayout.ICON;
import static dev.jstech.computers.gui.layout.DeviceManagerLayout.INDENT;
import static dev.jstech.computers.gui.layout.DeviceManagerLayout.ROW_H;
import static dev.jstech.computers.gui.layout.DeviceManagerLayout.TOGGLE;

/**
 * The Device Manager of the Frames editions: the machine's hardware and every port it has, what is plugged into each
 * and which are free, and a device disabled and enabled again, each edition in the look of its day. Frames 95 keeps it
 * in System Properties, with the ways of viewing the devices as radio buttons and the actions as buttons under the
 * tree; Frames XP in its console window, with the views in the View menu and the actions on a right-click; Frames 11
 * with the views as a switch in its menu row.
 *
 * <p>Everything shown is the machine's, asked for when the window opens and after every change; a device the
 * computer disabled is crossed out.
 */
public final class DeviceManagerApp implements IDesktopApp {

    private final BlockPos host;
    private final Form form;
    private final Consumer<DeviceMap> listener = this::accept;
    /** The branches the player folded, kept across the machine's answers; the memory modules start folded. */
    private final Set<String> folded = new HashSet<>(Set.of(DeviceRows.MEMORY_KEY));
    private final MenuBar menuBar = new MenuBar(MENU_ITEM_W, ROW_H);
    private final ContextMenu context = new ContextMenu(MENU_ITEM_W, ROW_H);
    private final ScrollBar bar = new ScrollBar(this::mostScroll, () -> this.scroll, v -> this.scroll = v);
    private OsSkin skin = OsSkin.fallback();
    /** Whether a press began on the scrollbar, so the drag that follows moves it. */
    private boolean draggingBar;
    @Nullable
    private DeviceMap map;
    private List<DeviceRows.Row> rows = List.of();
    private DeviceRows.View view = DeviceRows.View.BY_PORT;
    /** The device the selection stands on, by its position, so it survives the rows being made again. */
    private long selectedDevice = DeviceRows.NO_DEVICE;
    /** The selected row's key, for a heading, which stands for no device. */
    private String selectedKey = "";
    private int scroll;
    /** The device whose Properties box is up, or none. */
    private long propertiesOf = DeviceRows.NO_DEVICE;
    /* The content rectangle of the last frame, so a click is read against exactly what was drawn. */
    private int left;
    private int top;
    private int width;
    private int height;
    private int mouseX;
    private int mouseY;

    private static final int MENU_ITEM_W = 90;
    private static final int TAB_PAD = 4;
    private static final int SCROLL_W = 5;

    /** The shape this window takes, decided by the desktop it opened on. */
    private enum Form { CLASSIC, CONSOLE, MODERN }

    public DeviceManagerApp(final BlockPos host, @Nullable final ResourceLocation desktopId) {
        this.host = host;
        this.form = formOf(desktopId);
        this.menuBar.add(GameText.resolve(DeviceManagerTexts.MENU_FILE), this::fileMenu)
                .add(GameText.resolve(DeviceManagerTexts.MENU_ACTION), this::actionMenu)
                .add(GameText.resolve(DeviceManagerTexts.MENU_VIEW), this::viewMenu)
                .add(GameText.resolve(DeviceManagerTexts.MENU_HELP), this::helpMenu);
        ClientDeviceMaps.listen(host, this.listener);
    }

    // what a test reads and clicks

    /** The rows as the tree reads them, in order, or nothing until the machine has answered. */
    public List<String> shownRows() {
        final List<String> out = new ArrayList<>();
        for (final DeviceRows.Row row : this.rows) {
            out.add(GameText.resolve(row.label()));
        }
        return out;
    }

    /** Whether the row reading {@code label} is crossed out as disabled. */
    public boolean rowDisabled(final String label) {
        final DeviceRows.Row row = rowReading(label);
        return row != null && row.state() == DeviceRows.State.DISABLED;
    }

    /** The middle of the row reading {@code label}, in desktop pixels, or null while it is not shown. */
    @Nullable
    public int[] rowCentre(final String label) {
        for (int i = 0; i < this.rows.size(); i++) {
            if (GameText.resolve(this.rows.get(i).label()).equals(label)) {
                final int shown = i - this.scroll;
                if (shown < 0 || shown >= DeviceManagerLayout.rowsShown(treeH())) {
                    return null;
                }
                return new int[] {treeX() + treeW() / 2, treeY() + 2 + shown * ROW_H + ROW_H / 2};
            }
        }
        return null;
    }

    /** The context menu, for a test to read and pick from. */
    public ContextMenu contextMenu() {
        return this.context;
    }

    /** The menu bar, for a test to open a menu of. */
    public MenuBar menuBar() {
        return this.menuBar;
    }

    /** The middle of the {@code index}-th button under Frames 95's tree, in desktop pixels. */
    public int[] buttonCentre(final int index) {
        return new int[] {this.left + DeviceManagerLayout.buttonX(index) + DeviceManagerLayout.BUTTON_W / 2,
                this.top + DeviceManagerLayout.BUTTONS_Y + DeviceManagerLayout.BUTTON_H / 2};
    }

    /** The view in front. */
    public DeviceRows.View view() {
        return this.view;
    }

    /** Whether the Properties box is up. */
    public boolean propertiesOpen() {
        return this.propertiesOf != DeviceRows.NO_DEVICE;
    }

    // window

    @Override
    public String title() {
        return GameText.resolve(this.form == Form.CLASSIC ? DeviceManagerTexts.SYSTEM_PROPERTIES
                : DeviceManagerTexts.TITLE);
    }

    /* The layout's sizes are the room inside the frame, so the window is that plus its frame and title bar. */
    @Override
    public int defaultWidth() {
        return DesktopWindow.windowWidthFor(switch (this.form) {
            case CLASSIC -> DeviceManagerLayout.W95;
            case CONSOLE -> DeviceManagerLayout.WXP;
            case MODERN -> DeviceManagerLayout.W11;
        });
    }

    @Override
    public int defaultHeight() {
        return DesktopWindow.windowHeightFor(switch (this.form) {
            case CLASSIC -> DeviceManagerLayout.H95;
            case CONSOLE -> DeviceManagerLayout.HXP;
            case MODERN -> DeviceManagerLayout.H11;
        });
    }

    /** Frames 95's System Properties is a dialog of one size; the others shrink only so far. */
    @Override
    public int minWidth() {
        return this.form == Form.CLASSIC ? defaultWidth() : defaultWidth() * 3 / 4;
    }

    @Override
    public int minHeight() {
        return this.form == Form.CLASSIC ? defaultHeight() : defaultHeight() * 3 / 4;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void onRestored() {
        ClientDeviceMaps.forget(this.listener);
        ClientDeviceMaps.listen(this.host, this.listener);
    }

    @Override
    public void onClosed() {
        ClientDeviceMaps.forget(this.listener);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mx, final int my, final float partialTick) {
        this.left = x;
        this.top = y;
        this.width = w;
        this.height = h;
        this.mouseX = mx;
        this.mouseY = my;
        g.fill(x, y, x + w, y + h, this.skin.windowBg());
        switch (this.form) {
            case CLASSIC -> renderClassic(g, font);
            case CONSOLE -> renderConsole(g, font);
            case MODERN -> renderModern(g, font);
        }
        final UiContext ui = new UiContext(this.skin, font, mx, my, partialTick);
        if (this.form != Form.CLASSIC) {
            // Frames 11's menu row stops short of the view switch at its right end.
            final boolean modern = this.form == Form.MODERN;
            this.menuBar.setBounds(x, y, modern ? switchX(font) - x - 4 : w,
                    modern ? DeviceManagerLayout.ROW11_H : MenuBar.HEIGHT);
            this.menuBar.setWindow(x, y, w, h);
            this.menuBar.render(g, ui);
        }
        this.context.render(g, ui);
        if (propertiesOpen()) {
            renderProperties(g, font);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mx, final double my, final int button) {
        if (propertiesOpen()) {
            final int px = this.left + DeviceManagerLayout.propsX(this.width);
            final int py = this.top + DeviceManagerLayout.propsY(this.height);
            if (inside(mx, my, px + DeviceManagerLayout.PROPS_W - 44, py + DeviceManagerLayout.PROPS_H - 17, 40,
                    DeviceManagerLayout.BUTTON_H)) {
                this.propertiesOf = DeviceRows.NO_DEVICE;
            }
            return;
        }
        if (this.context.isOpen()) {
            this.context.mouseClicked(mx, my, button);
            return;
        }
        if (this.form != Form.CLASSIC && (this.menuBar.isOpen() || this.menuBar.titleAt(mx, my) >= 0)) {
            this.menuBar.mouseClicked(mx, my, button);
            return;
        }
        if (this.form == Form.CLASSIC && clickedClassic(mx, my)) {
            return;
        }
        if (this.form == Form.MODERN && clickedSwitch(mx, my)) {
            return;
        }
        clickedTree(mx, my, button);
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mx, final double my, final int button) {
        if (this.draggingBar) {
            this.bar.mouseDragged(mx, my, button);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mx, final double my, final int button) {
        this.draggingBar = false;
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.scroll = Math.max(0, Math.min(mostScroll(), this.scroll - (int) Math.signum(delta)));
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.context.isOpen()) {
            return this.context.keyPressed(key, scanCode, modifiers);
        }
        if (this.menuBar.isOpen()) {
            return this.menuBar.keyPressed(key, scanCode, modifiers);
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && propertiesOpen()) {
            this.propertiesOf = DeviceRows.NO_DEVICE;
            return true;
        }
        return false;
    }

    // the machine's answers

    private void accept(final DeviceMap answer) {
        this.map = answer.withHost(answer.host().toUpperCase(Locale.ROOT));
        rebuild();
    }

    private void rebuild() {
        this.rows = this.map == null ? List.of() : DeviceRows.rows(this.map, this.view, this.folded);
        this.scroll = Math.min(this.scroll, mostScroll());
    }

    /** How far the tree scrolls: the rows past the ones it shows at once. */
    private int mostScroll() {
        return Math.max(0, this.rows.size() - DeviceManagerLayout.rowsShown(treeH()));
    }

    private void showView(final DeviceRows.View wanted) {
        this.view = wanted;
        this.scroll = 0;
        rebuild();
    }

    /** The device the selection stands on, or null when it stands on a heading or nothing. */
    @Nullable
    private DeviceMap.Device selectedDeviceOf() {
        return ClientDeviceMaps.device(this.map, this.selectedDevice);
    }

    private void toggleSelected() {
        final DeviceMap.Device device = selectedDeviceOf();
        if (device != null) {
            ClientDeviceMaps.setDisabled(this.host, device.pos(), !device.disabled());
        }
    }

    private void openProperties() {
        if (selectedDeviceOf() != null) {
            this.propertiesOf = this.selectedDevice;
        }
    }

    // Frames 95: System Properties

    private void renderClassic(final GuiGraphics g, final Font font) {
        final TextKey[] tabs = {DeviceManagerTexts.TAB_GENERAL, DeviceManagerTexts.TAB_DEVICE_MANAGER,
            DeviceManagerTexts.TAB_PROFILES, DeviceManagerTexts.TAB_PERFORMANCE};
        final int[] words = new int[tabs.length];
        int wordsTotal = 0;
        for (int i = 0; i < tabs.length; i++) {
            words[i] = Texts.smallWidth(font, GameText.resolve(tabs[i]));
            wordsTotal += words[i];
        }
        /*
         * One row of tabs, as the dialog had: words that outgrow it first take the room around them in, and only a
         * language whose words outgrow even that squeezes every tab alike and clips its words.
         */
        final int room = DeviceManagerLayout.PANE_W;
        final int gaps = tabs.length - 1;
        final int pad = Math.max(1, Math.min(TAB_PAD, (room - gaps - wordsTotal) / (2 * tabs.length)));
        final int total = wordsTotal + 2 * pad * tabs.length + gaps;
        final boolean squeezed = total > room;
        int tx = this.left + DeviceManagerLayout.TAB_X;
        for (int i = 0; i < tabs.length; i++) {
            final int natural = words[i] + 2 * pad;
            final int tw = squeezed ? natural * (room - gaps) / (total - gaps) : natural;
            final String full = GameText.resolve(tabs[i]);
            final String label = squeezed ? Texts.clip(font, full, Texts.smallFits(tw - 2 * pad)) : full;
            final boolean front = tabs[i] == DeviceManagerTexts.TAB_DEVICE_MANAGER;
            this.skin.tab(g, font, tx, this.top + DeviceManagerLayout.TAB_Y, tw, DeviceManagerLayout.TAB_H, "",
                    front);
            // The other pages of System Properties are not this window's; their tabs are there, and greyed.
            Texts.small(g, font, label, tx + pad, this.top + DeviceManagerLayout.TAB_Y + 3,
                    front ? this.skin.text() : this.skin.dim());
            tx += tw + 1;
        }
        this.skin.panel(g, this.left + DeviceManagerLayout.PANE_X, this.top + DeviceManagerLayout.PANE_Y,
                DeviceManagerLayout.PANE_W, DeviceManagerLayout.PANE_H);
        radio(g, font, DeviceManagerLayout.RADIO_X, DeviceManagerLayout.RADIO_Y, DeviceManagerTexts.RADIO_TYPE,
                DeviceRows.View.BY_TYPE);
        radio(g, font, DeviceManagerLayout.RADIO_X2, DeviceManagerLayout.RADIO_Y, DeviceManagerTexts.RADIO_CONNECTION,
                DeviceRows.View.BY_CONNECTION);
        radio(g, font, DeviceManagerLayout.RADIO_X, DeviceManagerLayout.RADIO_Y + DeviceManagerLayout.RADIO_ROW,
                DeviceManagerTexts.RADIO_PORT, DeviceRows.View.BY_PORT);
        renderTree(g, font);
        final DeviceMap.Device device = selectedDeviceOf();
        button(g, font, DeviceManagerLayout.buttonX(0), DeviceManagerLayout.BUTTONS_Y, DeviceManagerLayout.BUTTON_W,
                DeviceManagerTexts.PROPERTIES, device != null);
        button(g, font, DeviceManagerLayout.buttonX(1), DeviceManagerLayout.BUTTONS_Y, DeviceManagerLayout.BUTTON_W,
                DeviceManagerTexts.REFRESH, true);
        button(g, font, DeviceManagerLayout.buttonX(2), DeviceManagerLayout.BUTTONS_Y, DeviceManagerLayout.BUTTON_W,
                device != null && device.disabled() ? DeviceManagerTexts.ENABLE : DeviceManagerTexts.DISABLE,
                device != null);
        button(g, font, DeviceManagerLayout.buttonX(3), DeviceManagerLayout.BUTTONS_Y, DeviceManagerLayout.BUTTON_W,
                DeviceManagerTexts.PRINT, false);
        final int footX = DeviceManagerLayout.W95 - 2 * DeviceManagerLayout.FOOT_W - 8;
        button(g, font, footX, DeviceManagerLayout.FOOT_Y, DeviceManagerLayout.FOOT_W, DeviceManagerTexts.OK, true);
        button(g, font, footX + DeviceManagerLayout.FOOT_W + 4, DeviceManagerLayout.FOOT_Y,
                DeviceManagerLayout.FOOT_W, DeviceManagerTexts.CANCEL, true);
    }

    private void radio(final GuiGraphics g, final Font font, final int x, final int y, final TextKey label,
                       final DeviceRows.View of) {
        final int rx = this.left + x;
        final int ry = this.top + y;
        this.skin.field(g, rx, ry, 6, 6, false);
        if (this.view == of) {
            g.fill(rx + 2, ry + 2, rx + 4, ry + 4, this.skin.text());
        }
        Texts.small(g, font, GameText.resolve(label), rx + 8, ry, this.skin.text());
    }

    private boolean clickedClassic(final double mx, final double my) {
        final int rx = this.left + DeviceManagerLayout.RADIO_X;
        final int ry = this.top + DeviceManagerLayout.RADIO_Y;
        if (inside(mx, my, rx, ry - 1, DeviceManagerLayout.RADIO_X2 - DeviceManagerLayout.RADIO_X - 4,
                DeviceManagerLayout.RADIO_ROW)) {
            showView(DeviceRows.View.BY_TYPE);
            return true;
        }
        if (inside(mx, my, this.left + DeviceManagerLayout.RADIO_X2, ry - 1, 130, DeviceManagerLayout.RADIO_ROW)) {
            showView(DeviceRows.View.BY_CONNECTION);
            return true;
        }
        if (inside(mx, my, rx, ry + DeviceManagerLayout.RADIO_ROW - 1, 110, DeviceManagerLayout.RADIO_ROW)) {
            showView(DeviceRows.View.BY_PORT);
            return true;
        }
        for (int i = 0; i < 3; i++) {
            if (inside(mx, my, this.left + DeviceManagerLayout.buttonX(i), this.top + DeviceManagerLayout.BUTTONS_Y,
                    DeviceManagerLayout.BUTTON_W, DeviceManagerLayout.BUTTON_H)) {
                switch (i) {
                    case 0 -> openProperties();
                    case 1 -> ClientDeviceMaps.ask(this.host);
                    default -> toggleSelected();
                }
                return true;
            }
        }
        final int footX = this.left + DeviceManagerLayout.W95 - 2 * DeviceManagerLayout.FOOT_W - 8;
        if (inside(mx, my, footX, this.top + DeviceManagerLayout.FOOT_Y, 2 * DeviceManagerLayout.FOOT_W + 4,
                DeviceManagerLayout.FOOT_H)) {
            ActiveDesktop.closeWindowFor(this);
            return true;
        }
        return false;
    }

    // Frames XP: the console window

    private void renderConsole(final GuiGraphics g, final Font font) {
        renderTree(g, font);
        final int sy = this.top + this.height - DeviceManagerLayout.STATUS_H;
        this.skin.statusBar(g, this.left, sy, this.width, DeviceManagerLayout.STATUS_H);
        Texts.small(g, font, Texts.clip(font, status(), Texts.smallFits(this.width - 8)), this.left + 4, sy + 2,
                this.skin.text());
    }

    /** What the status bar says: what disabling the selected device did, or how many devices there are. */
    private String status() {
        final DeviceMap.Device device = selectedDeviceOf();
        if (device != null && device.disabled()) {
            return GameText.resolve(DeviceManagerTexts.STATUS_DISABLED.with(device.name()));
        }
        return GameText.resolve(DeviceManagerTexts.STATUS_COUNT.with(this.map == null ? 0 : this.map.devices().size()));
    }

    // Frames 11: the menu row and its view switch

    private void renderModern(final GuiGraphics g, final Font font) {
        renderTree(g, font);
        final TextKey[] names = segments();
        int sx = switchX(font);
        for (int i = 0; i < names.length; i++) {
            final String label = GameText.resolve(names[i]);
            final int sw = Texts.smallWidth(font, label) + DeviceManagerLayout.SEGMENT_PAD;
            final boolean on = viewOf(i) == this.view;
            if (on) {
                // The selection's ground with its own ink, which reads on every skin, and the accent under it.
                final int bottom = this.top + 1 + DeviceManagerLayout.SEGMENT_H;
                g.fill(sx, this.top + 1, sx + sw, bottom, this.skin.listSelect());
                g.fill(sx + 1, bottom - 2, sx + sw - 1, bottom - 1, this.skin.accent());
            }
            Draw.outline(g, sx, this.top + 1, sw, DeviceManagerLayout.SEGMENT_H, this.skin.edge());
            Texts.small(g, font, label, sx + DeviceManagerLayout.SEGMENT_PAD / 2, this.top + 3,
                    on ? this.skin.listRowText(true) : this.skin.text());
            sx += sw;
        }
    }

    private boolean clickedSwitch(final double mx, final double my) {
        if (my < this.top + 1 || my >= this.top + 1 + DeviceManagerLayout.SEGMENT_H) {
            return false;
        }
        final Font font = Minecraft.getInstance().font;
        int sx = switchX(font);
        final TextKey[] names = segments();
        for (int i = 0; i < names.length; i++) {
            final int sw = Texts.smallWidth(font, GameText.resolve(names[i])) + DeviceManagerLayout.SEGMENT_PAD;
            if (mx >= sx && mx < sx + sw) {
                showView(viewOf(i));
                return true;
            }
            sx += sw;
        }
        return false;
    }

    private int switchX(final Font font) {
        int total = 0;
        for (final TextKey name : segments()) {
            total += Texts.smallWidth(font, GameText.resolve(name)) + DeviceManagerLayout.SEGMENT_PAD;
        }
        return this.left + this.width - 4 - total;
    }

    private static TextKey[] segments() {
        return new TextKey[] {DeviceManagerTexts.SEGMENT_PORT, DeviceManagerTexts.SEGMENT_TYPE,
            DeviceManagerTexts.SEGMENT_CONNECTION};
    }

    private static DeviceRows.View viewOf(final int segment) {
        return switch (segment) {
            case 1 -> DeviceRows.View.BY_TYPE;
            case 2 -> DeviceRows.View.BY_CONNECTION;
            default -> DeviceRows.View.BY_PORT;
        };
    }

    // the tree, the same in every edition

    private int treeX() {
        return this.left + (this.form == Form.CLASSIC ? DeviceManagerLayout.TREE95_X : DeviceManagerLayout.TREE_MARGIN);
    }

    private int treeY() {
        return this.top + switch (this.form) {
            case CLASSIC -> DeviceManagerLayout.TREE95_Y;
            case CONSOLE -> DeviceManagerLayout.TREEXP_Y;
            case MODERN -> DeviceManagerLayout.TREE11_Y;
        };
    }

    private int treeW() {
        return this.form == Form.CLASSIC ? DeviceManagerLayout.TREE95_W
                : this.width - 2 * DeviceManagerLayout.TREE_MARGIN;
    }

    private int treeH() {
        return switch (this.form) {
            case CLASSIC -> DeviceManagerLayout.TREE95_H;
            case CONSOLE -> this.height - DeviceManagerLayout.TREEXP_Y - DeviceManagerLayout.STATUS_H
                    - DeviceManagerLayout.TREE_MARGIN;
            case MODERN -> this.height - DeviceManagerLayout.TREE11_Y - DeviceManagerLayout.TREE_MARGIN;
        };
    }

    private void renderTree(final GuiGraphics g, final Font font) {
        final int x = treeX();
        final int y = treeY();
        final int w = treeW();
        final int h = treeH();
        this.skin.field(g, x, y, w, h, false);
        if (this.map == null) {
            Texts.small(g, font, GameText.resolve(DeviceManagerTexts.READING), x + 4, y + 4, this.skin.dim());
            return;
        }
        final int shown = DeviceManagerLayout.rowsShown(h);
        final int rowW = rowWidth();
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        for (int i = 0; i < shown && this.scroll + i < this.rows.size(); i++) {
            renderRow(g, font, this.rows.get(this.scroll + i), x + 2, y + 2 + i * ROW_H, rowW);
        }
        g.disableScissor();
        this.bar.setBounds(x + w - 1 - SCROLL_W, y + 1, SCROLL_W, h - 2);
        this.bar.render(g, new UiContext(this.skin, font, this.mouseX, this.mouseY, 0f));
    }

    /** How wide a row is: the tree's inside, less the scrollbar while there is more than it shows. */
    private int rowWidth() {
        return treeW() - 4 - (mostScroll() > 0 ? SCROLL_W + 1 : 0);
    }

    private void renderRow(final GuiGraphics g, final Font font, final DeviceRows.Row row, final int x, final int y,
                           final int w) {
        final boolean chosen = isSelected(row);
        final boolean hovered = this.mouseX >= x && this.mouseX < x + w && this.mouseY >= y
                && this.mouseY < y + ROW_H && !this.context.isOpen() && !this.menuBar.isOpen();
        this.skin.listRow(g, x, y, w, ROW_H, hovered, chosen);
        int at = x + row.depth() * INDENT;
        if (row.expandable()) {
            final int ty = y + (ROW_H - TOGGLE) / 2;
            g.fill(at, ty, at + TOGGLE, ty + TOGGLE, this.skin.fieldBg());
            Draw.outline(g, at, ty, TOGGLE, TOGGLE, this.skin.dim());
            g.fill(at + 2, ty + 3, at + TOGGLE - 2, ty + 4, this.skin.text());
            if (this.folded.contains(row.key())) {
                g.fill(at + 3, ty + 2, at + 4, ty + TOGGLE - 2, this.skin.text());
            }
        }
        at += TOGGLE + 1;
        if (!row.icon().isEmpty()) {
            // The machine itself, at the root, wears This PC's own picture rather than a device's.
            final String set = row.icon().equals(DeviceRows.HOST_ICON) ? "program" : "device";
            final ResourceLocation icon = SkinSprites.find(set, row.icon(), row.icon(), this.skin.iconSet());
            if (SkinSprites.exists(icon)) {
                SkinSprites.draw(g, icon, at, y + 1, ICON, ICON, 16);
            }
        }
        at += ICON + 2;
        final String label = Texts.clip(font, GameText.resolve(row.label()), Texts.smallFits(x + w - at - 2));
        final int ink = chosen ? this.skin.listRowText(true)
                : row.state() == DeviceRows.State.NORMAL ? this.skin.text() : this.skin.dim();
        final int textX = row.state() == DeviceRows.State.SUMMARY ? x : at;
        Texts.small(g, font, label, textX, y + (ROW_H - DeviceManagerLayout.TEXT_H) / 2 + 1, ink);
        if (row.state() == DeviceRows.State.DISABLED) {
            final int lineY = y + ROW_H / 2;
            g.fill(at, lineY, at + Texts.smallWidth(font, label), lineY + 1, ink);
        }
    }

    private boolean isSelected(final DeviceRows.Row row) {
        return row.isDevice() ? row.pos() == this.selectedDevice
                : !row.key().isEmpty() && row.key().equals(this.selectedKey) && this.selectedDevice
                        == DeviceRows.NO_DEVICE;
    }

    private void clickedTree(final double mx, final double my, final int button) {
        if (this.bar.contains(mx, my) && this.bar.mouseClicked(mx, my, button)) {
            this.draggingBar = true;
            return;
        }
        final int x = treeX() + 2;
        final int y = treeY() + 2;
        if (mx < x || mx >= x + rowWidth() || my < y || my >= treeY() + treeH() - 2) {
            return;
        }
        final int index = this.scroll + (int) ((my - y) / ROW_H);
        if (index < 0 || index >= this.rows.size()) {
            return;
        }
        final DeviceRows.Row row = this.rows.get(index);
        final int toggleX = x + row.depth() * INDENT;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && row.expandable() && mx >= toggleX
                && mx < toggleX + TOGGLE + 1) {
            if (!this.folded.remove(row.key())) {
                this.folded.add(row.key());
            }
            rebuild();
            return;
        }
        this.selectedDevice = row.isDevice() ? row.pos() : DeviceRows.NO_DEVICE;
        this.selectedKey = row.key();
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && this.form != Form.CLASSIC && row.isDevice()) {
            this.context.open(actionItems(), (int) mx, (int) my, this.left, this.top, this.width, this.height);
        }
    }

    // menus

    /** What can be done to the selected device, as the right-click and the Action menu offer it. */
    private List<ContextMenu.Item> actionItems() {
        final DeviceMap.Device device = selectedDeviceOf();
        final boolean on = device != null;
        return List.of(
                new ContextMenu.Item(GameText.resolve(DeviceManagerTexts.UPDATE_DRIVER), false, () -> { }),
                new ContextMenu.Item(GameText.resolve(on && device.disabled() ? DeviceManagerTexts.ENABLE
                        : DeviceManagerTexts.DISABLE), on, this::toggleSelected),
                new ContextMenu.Item(GameText.resolve(DeviceManagerTexts.UNINSTALL), false, () -> { }),
                ContextMenu.Item.separator(),
                new ContextMenu.Item(GameText.resolve(DeviceManagerTexts.PROPERTIES), on, this::openProperties));
    }

    private List<ContextMenu.Item> fileMenu() {
        return List.of(new ContextMenu.Item(GameText.resolve(DeviceManagerTexts.EXIT), true,
                () -> ActiveDesktop.closeWindowFor(this)));
    }

    private List<ContextMenu.Item> actionMenu() {
        final List<ContextMenu.Item> items = new ArrayList<>(actionItems());
        items.add(ContextMenu.Item.separator());
        items.add(new ContextMenu.Item(GameText.resolve(DeviceManagerTexts.REFRESH), true,
                () -> ClientDeviceMaps.ask(this.host)));
        return items;
    }

    private List<ContextMenu.Item> viewMenu() {
        return List.of(viewItem(DeviceManagerTexts.VIEW_TYPE, DeviceRows.View.BY_TYPE),
                viewItem(DeviceManagerTexts.VIEW_CONNECTION, DeviceRows.View.BY_CONNECTION),
                viewItem(DeviceManagerTexts.VIEW_PORT, DeviceRows.View.BY_PORT),
                ContextMenu.Item.separator(),
                new ContextMenu.Item(GameText.resolve(DeviceManagerTexts.SHOW_HIDDEN), false, () -> { }));
    }

    private ContextMenu.Item viewItem(final TextKey label, final DeviceRows.View of) {
        final String words = GameText.resolve(label);
        return new ContextMenu.Item(of == this.view ? GameText.resolve(DeviceManagerTexts.CHOSEN.with(words)) : words,
                true, () -> showView(of));
    }

    private List<ContextMenu.Item> helpMenu() {
        return List.of(new ContextMenu.Item(GameText.resolve(DeviceManagerTexts.ABOUT), false, () -> { }));
    }

    // the Properties box

    private void renderProperties(final GuiGraphics g, final Font font) {
        final DeviceMap.Device device = ClientDeviceMaps.device(this.map, this.propertiesOf);
        if (device == null) {
            this.propertiesOf = DeviceRows.NO_DEVICE;
            return;
        }
        final int x = this.left + DeviceManagerLayout.propsX(this.width);
        final int y = this.top + DeviceManagerLayout.propsY(this.height);
        g.fill(x - 1, y - 1, x + DeviceManagerLayout.PROPS_W + 1, y + DeviceManagerLayout.PROPS_H + 1,
                this.skin.edge());
        this.skin.panel(g, x, y, DeviceManagerLayout.PROPS_W, DeviceManagerLayout.PROPS_H);
        final int room = Texts.smallFits(DeviceManagerLayout.PROPS_W - 10);
        Texts.small(g, font, Texts.clip(font, GameText.resolve(DeviceManagerTexts.PROPS_TITLE.with(device.name())),
                room), x + 4, y + 3, this.skin.text());
        final DeviceMap.Port port = this.map.portOf(device.pos());
        if (port != null) {
            Texts.small(g, font, Texts.clip(font, GameText.resolve(DeviceManagerTexts.PROPS_LOCATION.with(port.name())),
                    room), x + 6, y + 14 + DeviceManagerLayout.PROPS_LINE, this.skin.text());
        }
        Texts.small(g, font, GameText.resolve(device.disabled() ? DeviceManagerTexts.PROPS_DISABLED
                : DeviceManagerTexts.PROPS_WORKING), x + 6, y + 14 + 2 * DeviceManagerLayout.PROPS_LINE,
                this.skin.text());
        button(g, font, DeviceManagerLayout.propsX(this.width) + DeviceManagerLayout.PROPS_W - 44,
                DeviceManagerLayout.propsY(this.height) + DeviceManagerLayout.PROPS_H - 17, 40, DeviceManagerTexts.OK,
                true);
    }

    // pieces

    /** A button in the skin's own shape at a place relative to the window, greyed when it cannot act. */
    private void button(final GuiGraphics g, final Font font, final int rx, final int ry, final int w,
                        final TextKey key, final boolean enabled) {
        final int x = this.left + rx;
        final int y = this.top + ry;
        final boolean hot = enabled && inside(this.mouseX, this.mouseY, x, y, w, DeviceManagerLayout.BUTTON_H);
        this.skin.button(g, font, x, y, w, DeviceManagerLayout.BUTTON_H, "", hot, false, false);
        final String label = GameText.resolve(key);
        Texts.small(g, font, label, x + (w - Texts.smallWidth(font, label)) / 2,
                y + (DeviceManagerLayout.BUTTON_H - DeviceManagerLayout.TEXT_H) / 2, enabled ? this.skin.text()
                        : this.skin.dim());
    }

    @Nullable
    private DeviceRows.Row rowReading(final String label) {
        for (final DeviceRows.Row row : this.rows) {
            if (GameText.resolve(row.label()).equals(label)) {
                return row;
            }
        }
        return null;
    }

    private static boolean inside(final double mx, final double my, final int x, final int y, final int w,
                                  final int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** The shape the window takes on that desktop, by the family of chrome the desktop declares. */
    private static Form formOf(@Nullable final ResourceLocation desktopId) {
        final DesktopEnvironmentDef desktop = desktopId == null ? null : OsRegistry.getDesktop(desktopId);
        if (desktop == null) {
            return Form.CONSOLE;
        }
        return switch (desktop.panelStyle()) {
            case FRAMES_95 -> Form.CLASSIC;
            case FRAMES_11 -> Form.MODERN;
            default -> Form.CONSOLE;
        };
    }
}
