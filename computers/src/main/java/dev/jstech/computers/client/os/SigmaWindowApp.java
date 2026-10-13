/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.api.client.ComponentRenderers;
import dev.jstech.computers.api.client.IComponentRenderer;
import dev.jstech.computers.gui.layout.UiLayout;
import dev.jstech.computers.operation.payload.UiEventPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.vm.program.UiWidgets;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A window a Σ# program opened, drawn by the machine's own system.
 *
 * <p>The program says what it wants (a row, a label, a table, a tree) and the desktop draws it the way
 * this system draws everything else, so the same program is a Frames 95 window on 95 and an XP one on XP.
 * Where each widget lands is worked out by {@link UiLayout} from what the letters of this font measure; how each is
 * drawn is {@link SigmaPainter}'s, and what the player does to each is {@link SigmaInput}'s.
 *
 * <p>What the player does goes back to the program as an event. A box being typed in keeps what is typed
 * here until it is sent, so a slow machine never eats a keystroke. A file dialog the program shows opens as the
 * system's own dialog over this window, and its answer goes back the same way.
 */
public final class SigmaWindowApp implements IDesktopApp {

    private final BlockPos host;
    private final int program;
    private final long window;
    private final SigmaUiState ui = new SigmaUiState();
    private final SigmaPainter painter;
    private final SigmaInput input;
    private final Map<Long, UiLayout.Rect> where = new HashMap<>();
    /** The scroll view each widget is seen through, by the widget, so it is drawn and clicked only inside it. */
    private final Map<Long, UiLayout.Rect> clips = new HashMap<>();
    /** The file dialogs this window has up for the program, by the dialog widget's number. */
    private final Map<Long, FileDialog> dialogs = new HashMap<>();
    private boolean closedByProgram;
    private final Map<Long, Boolean> answered = new HashMap<>();
    private UiWindowPayload state;
    /* The widgets of the state by their number, so walking up a widget's parents is not a scan at every step. */
    private Map<Long, UiWindowPayload.Widget> index;
    private OsSkin skin = OsSkin.fallback();
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    /** What every window a player's own program opens wears: it is no installed program, so it has none of its own. */
    private static final ResourceLocation ICON =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "sigma_program");

    public SigmaWindowApp(final BlockPos host, final UiWindowPayload opened) {
        this.host = host;
        this.program = opened.program();
        this.window = opened.window();
        this.state = opened;
        this.index = indexOf(opened);
        this.painter = new SigmaPainter(this.ui, host);
        this.input = new SigmaInput(this);
    }

    /** The key a desktop lists this window under, which names the program and the window. */
    public static String keyFor(final int program, final long window) {
        return "Σ#\0" + program + "\0" + window;
    }

    public String key() {
        return keyFor(this.program, this.window);
    }

    /** Takes the window as it now stands. */
    public void accept(final UiWindowPayload payload) {
        this.state = merged(payload);
        this.index = indexOf(this.state);
        // A box the program itself changed shows what the program says, not what was half typed into it.
        for (final UiWindowPayload.Widget widget : payload.widgets()) {
            final String typed = this.ui.typing.get(widget.id());
            final String held = "TextArea".equals(widget.kind()) ? widget.joined() : widget.text();
            if (typed != null && !"NumberBox".equals(widget.kind()) && !typed.equals(held)) {
                this.ui.typing.remove(widget.id());
            }
        }
        this.showDialogs();
    }

    /* A file dialog shown again, or for the first time, opens the system's own dialog over this window. */
    private void showDialogs() {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            final boolean save = "SaveFileDialog".equals(widget.kind());
            if (!save && !"OpenFileDialog".equals(widget.kind())
                    || widget.epoch() == this.ui.dialogsShown.getOrDefault(widget.id(), 0)) {
                continue;
            }
            this.ui.dialogsShown.put(widget.id(), widget.epoch());
            final FileDialog dialog = this.dialogs.computeIfAbsent(widget.id(), id -> new FileDialog(this.host, this));
            final long id = widget.id();
            this.answered.put(id, false);
            final Text title = widget.text().isEmpty() ? Text.literal(this.title()) : Text.literal(widget.text());
            final List<FileDialog.Filter> filters = filtersOf(widget.row(1));
            if (save) {
                dialog.saveAs(title, widget.row(0), widget.row(2), filters, path -> this.answer(id, path));
            } else {
                dialog.openFile(title, widget.row(0), filters, path -> this.answer(id, path));
            }
        }
    }

    private void answer(final long dialog, final String path) {
        this.answered.put(dialog, true);
        this.send("file", dialog, path, 0, 0);
    }

    /* A pattern such as "*.txt;*.md" as the kinds a dialog offers, then any file at all. */
    private static List<FileDialog.Filter> filtersOf(final String pattern) {
        final List<String> extensions = new ArrayList<>();
        for (final String one : pattern.split("[;,]")) {
            final String trimmed = one.trim();
            final int dot = trimmed.lastIndexOf('.');
            if (dot >= 0 && dot < trimmed.length() - 1 && !trimmed.endsWith("*")) {
                extensions.add(trimmed.substring(dot + 1));
            }
        }
        if (extensions.isEmpty()) {
            return List.of(FileDialog.Filter.ALL);
        }
        return List.of(FileDialog.Filter.of(Text.literal(GameText.resolve(SigmaWindowTexts.FILTER_NAMED
                .with(pattern.trim()))), extensions.toArray(String[]::new)), FileDialog.Filter.ALL);
    }

    /*
     * A canvas arrives carrying only what has been drawn on it since the last time, so that a program painting
     * every tick does not send its whole picture again: those strokes are added to the ones this screen has.
     */
    private UiWindowPayload merged(final UiWindowPayload payload) {
        final List<UiWindowPayload.Widget> widgets = payload.widgets();
        List<UiWindowPayload.Widget> joined = null;
        for (int i = 0; i < widgets.size(); i++) {
            final UiWindowPayload.Widget widget = widgets.get(i);
            if (!widget.appends()) {
                continue;
            }
            final List<UiWindowPayload.Stroke> had = strokesOf(widget.id());
            final List<UiWindowPayload.Stroke> all = new ArrayList<>(had.size() + widget.drawing().size());
            all.addAll(had);
            all.addAll(widget.drawing());
            if (joined == null) {
                joined = new ArrayList<>(widgets);
            }
            joined.set(i, widget.withStrokes(all));
        }
        return joined == null ? payload : payload.withWidgets(joined);
    }

    /** The strokes this screen already has for that widget. */
    private List<UiWindowPayload.Stroke> strokesOf(final long widget) {
        for (final UiWindowPayload.Widget had : this.state.widgets()) {
            if (had.id() == widget) {
                return had.drawing();
            }
        }
        return List.of();
    }

    /** The middle of the first widget of that kind, in desktop pixels, or null when it has none; for tests. */
    public int[] pointAt(final String kind) {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            final UiLayout.Rect rect = this.where.get(widget.id());
            if (rect != null && widget.kind().equals(kind)) {
                return new int[] {rect.x() + rect.w() / 2, rect.y() + rect.h() / 2};
            }
        }
        return null;
    }

    /** Where the first widget of that kind landed, as x, y, width and height, or null; for tests. */
    public int[] boundsOf(final String kind) {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            final UiLayout.Rect rect = this.where.get(widget.id());
            if (rect != null && widget.kind().equals(kind)) {
                return new int[] {rect.x(), rect.y(), rect.w(), rect.h()};
            }
        }
        return null;
    }

    /** What the first widget of that kind says, or empty when it has none; for tests. */
    public String said(final String kind) {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            if (widget.kind().equals(kind)) {
                return widget.text();
            }
        }
        return "";
    }

    /** How many rows the first list in the window holds; for tests. */
    public int rows() {
        return this.rowsOf("ListBox");
    }

    /** How many rows the first widget of that kind holds; for tests. */
    public int rowsOf(final String kind) {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            if (kind.equals(widget.kind())) {
                return widget.rows().size();
            }
        }
        return 0;
    }

    /** What the first widget of that kind has picked, counted from one; for tests. */
    public int pickedOf(final String kind) {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            if (kind.equals(widget.kind())) {
                return widget.number(4);
            }
        }
        return 0;
    }

    /** What the first widget of that kind holds as its value; for tests. */
    public int numberOf(final String kind) {
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            if (kind.equals(widget.kind())) {
                return widget.number(1);
            }
        }
        return 0;
    }

    /**
     * Where to click the entry of that place, counted from zero, in the first widget of that kind: a choice of a radio
     * group, a tab, a row of a list, a table or a tree as shown, a menu of a menu bar, an item of a picker, or a number
     * box's step up (0) and down (1); null when there is none. For tests.
     */
    public int[] entryPoint(final String kind, final int index) {
        final Font font = Minecraft.getInstance().font;
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            final UiLayout.Rect rect = this.where.get(widget.id());
            if (rect == null || !kind.equals(widget.kind())) {
                continue;
            }
            final int scroll = this.ui.scroll(widget.id());
            return switch (kind) {
                case "RadioGroup" -> middle(SigmaPainter.radioPlaces(font, widget, rect), index);
                case "TabView" -> middle(SigmaPainter.tabPlaces(font, widget, rect), index);
                case "ListBox", "TreeView" -> new int[] {rect.x() + rect.w() / 2,
                        rect.y() + 2 + (index - scroll) * SigmaPainter.ROW_H + SigmaPainter.ROW_H / 2};
                case "Table" -> new int[] {rect.x() + 10, rect.y() + SigmaPainter.HEADER_H + 3
                        + (index - scroll) * SigmaPainter.ROW_H + SigmaPainter.ROW_H / 2};
                case "MenuBar" -> {
                    final List<SigmaMenus.Title> titles = SigmaMenus.titles(font, widget, rect);
                    yield index < titles.size() ? new int[] {titles.get(index).x() + titles.get(index).w() / 2,
                            rect.y() + rect.h() / 2} : null;
                }
                case "ItemPicker" -> {
                    final int across = Math.max(1, rect.w() / SigmaPainter.SLOT);
                    yield new int[] {rect.x() + index % across * SigmaPainter.SLOT + SigmaPainter.SLOT / 2,
                            rect.y() + SigmaPainter.SEARCH_H + 2 + index / across * SigmaPainter.SLOT
                                    + SigmaPainter.SLOT / 2};
                }
                case "NumberBox" -> new int[] {rect.x() + rect.w() - SigmaGlyphs.ARROW_W / 2,
                        rect.y() + (index == 0 ? rect.h() / 4 : rect.h() * 3 / 4)};
                default -> null;
            };
        }
        return null;
    }

    /** Where to click the entry of that place, counted from zero, of the menu open over the window; for tests. */
    public int[] popupEntryPoint(final int index) {
        final SigmaMenus.Open menu = this.openMenu(Minecraft.getInstance().font);
        if (menu == null) {
            return null;
        }
        return new int[] {menu.rect().x() + 10,
                menu.rect().y() + 2 + (index - this.ui.popupScroll) * SigmaPainter.ROW_H + SigmaPainter.ROW_H / 2};
    }

    private static int[] middle(final List<UiLayout.Rect> places, final int index) {
        if (index < 0 || index >= places.size()) {
            return null;
        }
        final UiLayout.Rect place = places.get(index);
        return new int[] {place.x() + place.w() / 2, place.y() + place.h() / 2};
    }

    /** Whether a menu or a combo list is open over the window; for tests. */
    public boolean popupOpen() {
        return this.ui.popupOpen();
    }

    /** The file dialog this window has up for the program, or null; for tests. */
    public FileDialog openDialog() {
        for (final FileDialog dialog : this.dialogs.values()) {
            if (dialog.isOpen()) {
                return dialog;
            }
        }
        return null;
    }

    @Override
    public String title() {
        return this.state.title().isEmpty() ? GameText.resolve(SigmaWindowTexts.DEFAULT_TITLE) : this.state.title();
    }

    @Override
    public ResourceLocation iconId() {
        return ICON;
    }

    @Override
    public int defaultWidth() {
        return this.state.width() + 8;
    }

    @Override
    public int defaultHeight() {
        return this.state.height() + DesktopWindow.TITLE_H + 8;
    }

    @Override
    public int minWidth() {
        return Math.min(this.defaultWidth(), 120);
    }

    @Override
    public int minHeight() {
        return Math.min(this.defaultHeight(), 70);
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin == null ? OsSkin.fallback() : osSkin;
    }

    @Override
    public void onClosed() {
        for (final FileDialog dialog : this.dialogs.values()) {
            dialog.close();
        }
        // The player shutting the window is the program's to hear: its own OnClose runs, and it may end.
        // A window the program closed itself has nothing to be told, and answering would echo its own close.
        if (!this.closedByProgram) {
            this.send("close", 0L, "", 0, 0);
        }
    }

    /** Marks the window as taken away by the program, so closing it does not report back to that program. */
    void closedByProgram() {
        this.closedByProgram = true;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.contentX = x;
        this.contentY = y;
        this.contentW = width;
        this.contentH = height;
        this.input.pointer(mouseX, mouseY);
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        final List<UiLayout.Box> boxes = this.boxes(font);
        final List<UiLayout.Rect> places = UiLayout.lay(boxes, x, y, width, height, this.ui.scrolled);
        this.where.clear();
        for (final UiLayout.Rect rect : places) {
            this.where.put(rect.id(), rect);
        }
        this.noteScrollViews(boxes);
        // While a menu is open the widgets under it are not lit by the pointer going over them.
        final int litX = this.ui.popupOpen() ? Integer.MIN_VALUE : mouseX;
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            final UiLayout.Rect rect = this.where.get(widget.id());
            if (rect == null || !widget.shows() || !this.visible(widget)) {
                continue;
            }
            final UiLayout.Rect clip = this.clips.get(widget.id());
            if (clip != null) {
                Draw.pushScissor(g, clip.x(), clip.y(), clip.x() + clip.w(), clip.y() + clip.h());
            }
            this.painter.draw(g, font, this.skin, widget, rect, litX, mouseY);
            if (clip != null) {
                Draw.popScissor(g);
            }
        }
        this.drawPopup(g, font, mouseX, mouseY);
        this.noticeCancelledDialogs();
    }

    /** Whether a widget shows: when it and everything holding it shows, since a hidden row hides what is in it. */
    boolean visible(final UiWindowPayload.Widget widget) {
        UiWindowPayload.Widget at = widget;
        for (int depth = 0; at != null && depth < UiWidgets.MOST_WIDGETS; depth++) {
            if (!at.shows()) {
                return false;
            }
            at = at.parent() == 0 ? null : this.widgetOf(at.parent());
        }
        return true;
    }

    /*
     * Each scroll view: how far its widget can scroll, and the view as the clip of every widget inside it, so what is
     * scrolled out of it is neither drawn nor clicked.
     */
    private void noteScrollViews(final List<UiLayout.Box> boxes) {
        this.clips.clear();
        for (final UiLayout.Box box : boxes) {
            if (!"ScrollView".equals(box.kind())) {
                continue;
            }
            final UiLayout.Rect view = this.where.get(box.id());
            if (view == null) {
                continue;
            }
            this.ui.rooms.put(box.id(), UiLayout.scrollRoom(box, boxes, view.h()));
            final UiLayout.Rect inside = new UiLayout.Rect(box.id(), view.x() + 1, view.y() + 1,
                    view.w() - UiLayout.SCROLLBAR - 1, view.h() - 2);
            for (final UiWindowPayload.Widget widget : this.state.widgets()) {
                if (this.within(widget, box.id())) {
                    this.clips.merge(widget.id(), inside, SigmaWindowApp::overlap);
                }
            }
        }
    }

    private static UiLayout.Rect overlap(final UiLayout.Rect a, final UiLayout.Rect b) {
        final int x = Math.max(a.x(), b.x());
        final int y = Math.max(a.y(), b.y());
        final int right = Math.min(a.x() + a.w(), b.x() + b.w());
        final int bottom = Math.min(a.y() + a.h(), b.y() + b.h());
        return new UiLayout.Rect(a.id(), x, y, Math.max(0, right - x), Math.max(0, bottom - y));
    }

    /** Whether a widget lies inside the one of that number, anywhere down. */
    boolean within(final UiWindowPayload.Widget widget, final long holder) {
        UiWindowPayload.Widget at = widget;
        for (int depth = 0; at != null && at.parent() != 0 && depth < UiWidgets.MOST_WIDGETS; depth++) {
            if (at.parent() == holder) {
                return true;
            }
            at = this.widgetOf(at.parent());
        }
        return false;
    }

    private void drawPopup(final GuiGraphics g, final Font font, final int mouseX, final int mouseY) {
        final SigmaMenus.Open menu = this.openMenu(font);
        if (menu == null) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, 200);
        SigmaMenus.draw(g, font, this.skin, this.widgetOf(this.ui.popupOwner), menu, this.ui.popupScroll, mouseX,
                mouseY);
        g.pose().popPose();
    }

    /** The menu open over the window, laid out, or null when none is; closing it when what it belongs to is gone. */
    SigmaMenus.Open openMenu(final Font font) {
        if (!this.ui.popupOpen()) {
            return null;
        }
        final UiWindowPayload.Widget owner = this.widgetOf(this.ui.popupOwner);
        final UiLayout.Rect rect = owner == null ? null : this.where.get(
                this.ui.popup == SigmaUiState.PopupKind.CONTEXT ? owner.parent() : owner.id());
        if (owner == null || rect == null && this.ui.popup != SigmaUiState.PopupKind.CONTEXT) {
            this.ui.closePopup();
            return null;
        }
        return SigmaMenus.open(font, this.ui, owner, rect == null ? new UiLayout.Rect(0, 0, 0, 0, 0) : rect,
                this.contentX, this.contentY, this.contentW, this.contentH);
    }

    /* A dialog the player shut without choosing anything tells the program it was turned down. */
    private void noticeCancelledDialogs() {
        for (final Map.Entry<Long, FileDialog> entry : this.dialogs.entrySet()) {
            if (!entry.getValue().isOpen() && !this.answered.getOrDefault(entry.getKey(), true)) {
                this.answered.put(entry.getKey(), true);
                this.send("cancel", entry.getKey(), "", 0, 0);
            }
        }
    }

    /** What each widget is, and how big it is when nobody says otherwise, for the layout to work with. */
    private List<UiLayout.Box> boxes(final Font font) {
        final List<UiLayout.Box> boxes = new ArrayList<>();
        for (final UiWindowPayload.Widget widget : this.state.widgets()) {
            boxes.add(new UiLayout.Box(widget.id(), widget.parent(), widget.kind(), widget.weight(),
                    this.wideOf(font, widget), this.tallOf(widget), widget.width(), widget.height(),
                    widget.number(0), widget.x(), widget.y(), widget.number(4)));
        }
        return boxes;
    }

    private int wideOf(final Font font, final UiWindowPayload.Widget widget) {
        return switch (widget.kind()) {
            case "Label" -> font.width(widget.text());
            case "Button" -> font.width(widget.text()) + 12;
            case "TextBox" -> Math.max(60, font.width(this.ui.textOf(widget)) + 10);
            case "TextArea" -> 120;
            case "CheckBox" -> SigmaGlyphs.MARK + 4 + font.width(widget.text());
            case "ProgressBar", "NumberBox" -> 60;
            case "Slider", "ListBox", "TreeView" -> 90;
            case "RadioGroup" -> this.radiosWide(font, widget);
            case "ComboBox" -> this.widest(font, widget.rows()) + SigmaGlyphs.ARROW_W + 12;
            case "TabView" -> this.tabsWide(font, widget);
            case "GroupBox" -> font.width(widget.text()) + 20;
            case "ScrollView", "Image" -> 64;
            case "Table", "LogView", "OperationView" -> 160;
            case "MenuBar" -> this.menusWide(font, widget);
            case "StatusBar" -> 80;
            case "Chart" -> 100;
            case "ItemSlot" -> SigmaPainter.SLOT;
            case "ItemPicker" -> SigmaPainter.SLOT * 4;
            case "GenericComponent" -> this.rendererWide(widget, true);
            case "Canvas" -> 80;
            default -> 0;
        };
    }

    private int tallOf(final UiWindowPayload.Widget widget) {
        return switch (widget.kind()) {
            case "Label" -> 9;
            case "Button", "TextBox", "NumberBox", "Slider", "ComboBox" -> 16;
            case "CheckBox", "StatusBar", "MenuBar" -> 12;
            case "RadioGroup" -> widget.ticked() ? 12 : 12 * Math.max(1, widget.rows().size());
            case "TextArea", "Image", "LogView" -> 48;
            case "ProgressBar" -> 10;
            case "ListBox", "Canvas", "Table", "TreeView", "ScrollView" -> 60;
            case "TabView" -> UiLayout.TAB_H + 30;
            case "GroupBox" -> UiLayout.CAPTION_H + 12;
            case "Chart" -> 44;
            case "OperationView" -> 34;
            case "ItemSlot" -> SigmaPainter.SLOT;
            case "ItemPicker" -> SigmaPainter.SEARCH_H + 2 + SigmaPainter.SLOT * 2;
            case "GenericComponent" -> this.rendererWide(widget, false);
            default -> 0;
        };
    }

    private int rendererWide(final UiWindowPayload.Widget widget, final boolean across) {
        final IComponentRenderer renderer = ComponentRenderers.get(widget.text());
        if (renderer == null) {
            return across ? 80 : 40;
        }
        return across ? renderer.width() : renderer.height();
    }

    private int widest(final Font font, final List<String> said) {
        int most = 0;
        for (final String one : said) {
            most = Math.max(most, font.width(one));
        }
        return most;
    }

    private int radiosWide(final Font font, final UiWindowPayload.Widget widget) {
        if (!widget.ticked()) {
            return SigmaGlyphs.MARK + 3 + this.widest(font, widget.rows());
        }
        int total = 0;
        for (final String one : widget.rows()) {
            total += SigmaGlyphs.MARK + 13 + font.width(one);
        }
        return total;
    }

    private int tabsWide(final Font font, final UiWindowPayload.Widget widget) {
        int total = 0;
        for (final String one : widget.rows()) {
            total += font.width(one) + 14;
        }
        return total;
    }

    private int menusWide(final Font font, final UiWindowPayload.Widget widget) {
        int total = 4;
        final List<String> named = new ArrayList<>();
        for (final String menu : widget.details()) {
            if (!named.contains(menu)) {
                named.add(menu);
                total += font.width(menu) + 10;
            }
        }
        return total;
    }

    // what the player does, which the input works out

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        this.input.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        this.input.mouseDragged(mouseX, mouseY);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        this.input.mouseReleased();
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        return this.input.mouseScrolled(delta);
    }

    @Override
    public boolean charTyped(final char c) {
        return this.input.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return this.input.keyPressed(key, scanCode, modifiers);
    }

    /** Escape shuts an open menu before it would shut the window. */
    @Override
    public boolean wantsEscape() {
        return this.ui.popupOpen();
    }

    SigmaUiState ui() {
        return this.ui;
    }

    SigmaPainter painter() {
        return this.painter;
    }

    UiWindowPayload state() {
        return this.state;
    }

    /** Where a widget landed at the last frame, or null when it was not placed. */
    UiLayout.Rect rectOf(final long id) {
        return this.where.get(id);
    }

    /** The scroll view a widget is seen through, or null when it is in none. */
    UiLayout.Rect clipOf(final long id) {
        return this.clips.get(id);
    }

    UiWindowPayload.Widget widgetOf(final long id) {
        return id == 0 ? null : this.index.get(id);
    }

    /** The widget of a window with that number, or null; a scan, for the callers that hold no index. */
    static UiWindowPayload.Widget find(final UiWindowPayload window, final long id) {
        if (id == 0) {
            return null;
        }
        for (final UiWindowPayload.Widget widget : window.widgets()) {
            if (widget.id() == id) {
                return widget;
            }
        }
        return null;
    }

    private static Map<Long, UiWindowPayload.Widget> indexOf(final UiWindowPayload window) {
        final Map<Long, UiWindowPayload.Widget> byId = new HashMap<>();
        for (final UiWindowPayload.Widget widget : window.widgets()) {
            // The first widget of a number wins, as it does in a scan.
            byId.putIfAbsent(widget.id(), widget);
        }
        return byId;
    }

    /** Tells the program what the player did. */
    void send(final String kind, final long widget, final String said, final int number, final int second) {
        PacketDistributor.sendToServer(new UiEventPayload(this.host, this.program, this.window, widget, kind,
                said, number, second));
    }
}
