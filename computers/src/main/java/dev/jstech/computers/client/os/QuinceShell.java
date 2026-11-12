/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.DiskTexts;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The shell of Quince's desktops (computers/docs/design/quince.md): the menu bar across the top with the fruit's menu,
 * the program in front's menus and, on Quince OS 9, the Application menu at its right listing what is open; and from
 * Quince OS X on the Dock along the foot, with the system's programs, what else is open, and the Trash at its end.
 *
 * <p>A menu pulled down takes the next click wherever it lands, as the real ones did: on an item it does it, anywhere
 * else it only closes.
 */
@TextHolder
final class QuinceShell {

    private final DesktopState desktop;
    /** The title whose menu is down, its index among the bar's titles, or -1. */
    private int open = -1;
    /** The bar's titles as last drawn, each {@code {left, right}}, the fruit first. */
    private List<int[]> titles = List.of();
    /** The menus as last drawn, in the bar's order. */
    private List<Menu> menus = List.of();
    /**
     * The machine's disks as the Special menu reads them, asked for only as that menu opens: the machine answers one
     * question of a file at a time, so asking in the background could take the answer a program is waiting for.
     */
    @Nullable
    private DiskToolClient disks;

    private static final TextKey ABOUT = TextKey.of("jsc.quince.menu.about", "About This Computer");
    private static final TextKey ABOUT_APP = TextKey.of("jsc.quince.menu.about_app", "About %s");
    private static final TextKey QUIT_APP = TextKey.of("jsc.quince.menu.quit_app", "Quit %s");
    private static final TextKey PREFERENCES = TextKey.of("jsc.quince.menu.preferences", "System Preferences...");
    private static final TextKey STORE = TextKey.of("jsc.quince.menu.store", "Quince Store...");
    private static final TextKey FORCE_QUIT = TextKey.of("jsc.quince.menu.force_quit", "Force Quit...");
    private static final TextKey RESTART = TextKey.of("jsc.quince.menu.restart", "Restart");
    private static final TextKey SHUT_DOWN = TextKey.of("jsc.quince.menu.shut_down", "Shut Down");
    private static final TextKey LOG_OUT = TextKey.of("jsc.quince.menu.log_out", "Log Out");
    private static final TextKey FILE = TextKey.of("jsc.quince.menu.file", "File");
    private static final TextKey EDIT = TextKey.of("jsc.quince.menu.edit", "Edit");
    private static final TextKey VIEW = TextKey.of("jsc.quince.menu.view", "View");
    private static final TextKey SPECIAL = TextKey.of("jsc.quince.menu.special", "Special");
    private static final TextKey GO = TextKey.of("jsc.quince.menu.go", "Go");
    private static final TextKey WINDOW = TextKey.of("jsc.quince.menu.window", "Window");
    private static final TextKey HELP = TextKey.of("jsc.quince.menu.help", "Help");
    private static final TextKey OPEN = TextKey.of("jsc.quince.menu.open", "Open");
    private static final TextKey NEW_WINDOW = TextKey.of("jsc.quince.menu.new_window", "New Seeker Window");
    private static final TextKey CLOSE_WINDOW = TextKey.of("jsc.quince.menu.close_window", "Close Window");
    private static final TextKey GET_INFO = TextKey.of("jsc.quince.menu.get_info", "Get Info");
    private static final TextKey UNDO = TextKey.of("jsc.quince.menu.undo", "Undo");
    private static final TextKey CUT = TextKey.of("jsc.quince.menu.cut", "Cut");
    private static final TextKey COPY = TextKey.of("jsc.quince.menu.copy", "Copy");
    private static final TextKey PASTE = TextKey.of("jsc.quince.menu.paste", "Paste");
    private static final TextKey SELECT_ALL = TextKey.of("jsc.quince.menu.select_all", "Select All");
    private static final TextKey CLEAN_UP = TextKey.of("jsc.quince.menu.clean_up", "Clean Up");
    private static final TextKey EMPTY_TRASH = TextKey.of("jsc.quince.menu.empty_trash", "Empty Trash");
    private static final TextKey ERASE_DISK = TextKey.of("jsc.quince.menu.erase_disk", "Erase Disk...");
    private static final TextKey SET_STARTUP = TextKey.of("jsc.quince.menu.set_startup", "Set Startup...");
    private static final TextKey APPLICATIONS = TextKey.of("jsc.quince.menu.applications", "Applications");
    private static final TextKey HOME = TextKey.of("jsc.quince.menu.home", "Home");
    private static final TextKey UTILITIES = TextKey.of("jsc.quince.menu.utilities", "Utilities");
    private static final TextKey MINIMIZE = TextKey.of("jsc.quince.menu.minimize", "Minimize");
    private static final TextKey ZOOM = TextKey.of("jsc.quince.menu.zoom", "Zoom");
    private static final TextKey SYSTEM_HELP = TextKey.of("jsc.quince.menu.system_help", "%s Help");
    private static final TextKey SEEKER = TextKey.of("jsc.quince.menu.seeker", "Seeker");

    /* The look of a pulled-down menu: a row's height, a rule's, and the room either side of an item's words. */
    private static final int ROW = 11;
    private static final int RULE = 5;
    private static final int PAD = 10;
    /** The Dock's programs, in its order; the Quince Store goes in before System Preferences from the Standard on. */
    private static final List<String> DOCK = List.of("files", "expedition", "qtunes", "editor", "preview",
            "media_player", "settings", "command_prompt");
    /* The folders the menus open, as the explorer names them: from the disk's root, which itself has no path. */
    private static final String DISK_ROOT = "";
    private static final String APPLICATIONS_DIR = "Applications";
    private static final String UTILITIES_DIR = "Applications/Utilities";

    QuinceShell(final DesktopState desktop) {
        this.desktop = desktop;
    }

    /** The face the desktop is drawn in now. */
    QuinceLook look() {
        final QuinceLook look = QuinceLook.of(desktop.desktopId(), desktop.prefs().era());
        return look == null ? QuinceLook.PULP : look;
    }

    /** Whether a menu is down. */
    boolean menuOpen() {
        return this.open >= 0;
    }

    /** Puts any menu away. */
    void close() {
        this.open = -1;
    }

    /** The middle of the bar's title at {@code index} as the last frame laid it out, the fruit's first, or null. */
    @Nullable
    int[] titlePoint(final int index) {
        if (index < 0 || index >= this.titles.size()) {
            return null;
        }
        final int[] span = this.titles.get(index);
        return new int[] {(span[0] + span[1]) / 2, QuinceBars.BAR_H / 2};
    }

    /** The middle of the item of that name in the menu that is down, or null when none is or it has none such. */
    @Nullable
    int[] itemPoint(final String label) {
        if (this.open < 0 || this.open >= this.menus.size() || this.open >= this.titles.size()) {
            return null;
        }
        final Menu menu = this.menus.get(this.open);
        final int[] box = box(desktop.textFont(), menu, this.titles.get(this.open)[0]);
        int y = box[1] + 2;
        for (final Item item : menu.items()) {
            final int h = item.rule() ? RULE : ROW;
            if (!item.rule() && item.label().equals(label)) {
                return new int[] {box[0] + box[2] / 2, y + h / 2};
            }
            y += h;
        }
        return null;
    }

    /** The bar along the top and, from Quince OS X on, the Dock along the foot. */
    void render(final GuiGraphics g, final int sw, final int sh, final int lmx, final int lmy) {
        // The question of what to force quit stands while the machine asks it: closed, it comes back.
        if (!desktop.memoryAsk().isEmpty() && desktop.windowFor(QuinceMemoryApp.KEY) == null) {
            desktop.wm().open(QuinceMemoryApp.KEY, new QuinceMemoryApp(desktop));
        }
        // A change to a disk the Special menu asked for that the machine refused says why, as the disk tools do.
        if (this.disks != null && this.disks.refusal() != null) {
            desktop.notices().ask("", GameText.resolve(DiskTexts.reason(this.disks.refusal())), null);
            this.disks.clearRefusal();
        }
        final QuinceLook look = look();
        final Font font = desktop.textFont();
        this.menus = menusOf(look);
        final List<String> names = new ArrayList<>();
        for (final Menu menu : this.menus) {
            names.add(menu.title());
        }
        final ResourceLocation front = frontProgram();
        this.titles = QuinceBars.menuBar(g, font, look, 0, 0, sw, appName(), front, desktop.icons(), names.subList(look.lights() ? 2 : 1, names.size()),
                look == QuinceLook.ONE_BIT ? "" : QuinceBars.clock(), this.open);
        if (look.lights()) {
            final List<ResourceLocation> icons = new ArrayList<>();
            final List<Boolean> running = new ArrayList<>();
            dockItems(icons, running);
            QuinceBars.dock(g, look, 0, 0, sw, sh, icons, running, desktop.icons(),
                    QuinceBars.dockAt(look, 0, 0, sw, sh, icons.size(), lmx, lmy));
        }
    }

    /** The menu that is down, over everything on the desktop. */
    void renderMenu(final GuiGraphics g, final int lmx, final int lmy) {
        if (this.open < 0 || this.open >= this.menus.size() || this.open >= this.titles.size()) {
            return;
        }
        final QuinceLook look = look();
        final QuinceLook.Colours c = look.colours();
        final Font font = desktop.textFont();
        final Menu menu = this.menus.get(this.open);
        final int[] box = box(font, menu, this.titles.get(this.open)[0]);
        final int x = box[0];
        final int top = box[1];
        final int w = box[2];
        final int h = box[3];
        if (look == QuinceLook.PLATINUM) {
            g.fill(x + 2, top + 2, x + w + 2, top + h + 2, ChromeShapes.blend(0, c.shadow(), 0.4F));
            QuinceArt.bevel(g, x, top, x + w, top + h, c.menu(), c.light(), c.shade());
            ChromeShapes.outline(g, x, top, w, h, c.menuEdge());
        } else if (look == QuinceLook.ONE_BIT) {
            g.fill(x + 1, top + 1, x + w + 1, top + h + 1, c.shadow());
            g.fill(x, top, x + w, top + h, c.menu());
            ChromeShapes.outline(g, x, top, w, h, c.menuEdge());
        } else {
            QuinceArt.roundRect(g, x, top, x + w, top + h, Math.max(2, look.round()), c.menuEdge());
            QuinceArt.roundRect(g, x + 1, top + 1, x + w - 1, top + h - 1, Math.max(1, look.round() - 1), c.menu());
        }
        int y = top + 2;
        for (final Item item : menu.items()) {
            if (item.rule()) {
                if (look == QuinceLook.ONE_BIT) {
                    for (int xx = x + 1; xx < x + w - 1; xx += 2) {
                        g.fill(xx, y + 2, xx + 1, y + 3, c.dim());
                    }
                } else {
                    g.fill(x + 2, y + 2, x + w - 2, y + 3, c.shade());
                }
                y += RULE;
                continue;
            }
            final boolean hot = item.enabled() && lmx >= x && lmx < x + w && lmy >= y && lmy < y + ROW;
            if (hot) {
                g.fill(x + 1, y, x + w - 1, y + ROW, c.hot());
            }
            final int ink = hot ? c.hotInk() : c.ink();
            if (item.enabled()) {
                Draw.text(g, font, item.label(), x + PAD, y + 2, ink);
            } else if (look == QuinceLook.ONE_BIT) {
                QuinceArt.dimmed(g, font, item.label(), x + PAD, y + 2, c.ink(), c.menu());
            } else {
                Draw.text(g, font, item.label(), x + PAD, y + 2, c.dim());
            }
            y += ROW;
        }
    }

    /**
     * A click on the desktop: on an item of the menu that is down, on a title of the bar, or on the Dock. Answers
     * whether the shell took it; a menu that is down takes any click, and the bar takes every click along it.
     */
    boolean click(final double mx, final double my, final int button, final int sw, final int sh) {
        final QuinceLook look = look();
        if (this.open >= 0) {
            final int was = this.open;
            this.open = -1;
            if (was < this.menus.size() && was < this.titles.size()) {
                final Menu menu = this.menus.get(was);
                final int[] box = box(desktop.textFont(), menu, this.titles.get(was)[0]);
                if (mx >= box[0] && mx < box[0] + box[2] && my >= box[1] && my < box[1] + box[3]) {
                    final Item item = itemAt(menu, box[1], my);
                    if (item != null && item.enabled()) {
                        item.action().run();
                    }
                    return true;
                }
            }
            final int title = titleAt(mx, my);
            if (title >= 0 && title != was) {
                openMenu(title);
            }
            return true;
        }
        if (my < QuinceBars.BAR_H) {
            final int title = titleAt(mx, my);
            if (title >= 0) {
                desktop.closeMenus();
                openMenu(title);
            }
            return true;
        }
        if (look.lights()) {
            final List<ResourceLocation> icons = new ArrayList<>();
            dockItems(icons, new ArrayList<>());
            final int at = QuinceBars.dockAt(look, 0, 0, sw, sh, icons.size(), (int) mx, (int) my);
            if (at >= 0 && icons.get(at) != null) {
                launch(icons.get(at));
                return true;
            }
        }
        return false;
    }

    /** What is in the Dock now, and whether each one is running; a null is the divider before the Trash. */
    private void dockItems(final List<ResourceLocation> icons, final List<Boolean> running) {
        final List<String> shown = new ArrayList<>();
        for (final String path : DOCK) {
            if (path.equals("settings") && storeHere()) {
                shown.add("quince_store");
            }
            if (bundled(path)) {
                shown.add(path);
            }
        }
        // What else is open stands after the system's own, as the real Dock kept it.
        for (final DesktopWindow w : desktop.wm().all()) {
            final ProgramSpec spec = WindowKeys.program(w.appKey());
            if (!w.dialog() && spec != null && !shown.contains(spec.id().getPath())) {
                shown.add(spec.id().getPath());
            }
        }
        for (final String path : shown) {
            icons.add(program(path));
            running.add(runs(path));
        }
        icons.add(null);
        running.add(false);
        icons.add(program(desktop.trashFull() ? "trash_full" : "trash"));
        running.add(false);
    }

    /** Starts a program from the Dock, or brings its window forward when it is already open; the Trash opens. */
    private void launch(final ResourceLocation icon) {
        final String path = icon.getPath();
        if (path.startsWith("trash")) {
            desktop.openTrash();
            return;
        }
        open(path);
    }

    /** The menus the bar has on this face, in its order: the fruit's first. */
    private List<Menu> menusOf(final QuinceLook look) {
        final List<Menu> out = new ArrayList<>();
        out.add(new Menu("", fruit(look)));
        if (look.lights()) {
            final String app = appName();
            out.add(new Menu(app, List.of(Item.of(GameText.resolve(ABOUT_APP.with(app)), this::aboutFront),
                    Item.RULE, Item.of(GameText.resolve(QUIT_APP.with(app)), this::quitFront))));
        }
        out.add(new Menu(GameText.resolve(FILE), file(look)));
        out.add(new Menu(GameText.resolve(EDIT), edit()));
        out.add(new Menu(GameText.resolve(VIEW), List.of(Item.of(GameText.resolve(CLEAN_UP), this::cleanUp))));
        if (look.classic()) {
            out.add(new Menu(GameText.resolve(SPECIAL), special(look)));
        } else {
            out.add(new Menu(GameText.resolve(GO), List.of(
                    Item.of(GameText.resolve(APPLICATIONS), () -> desktop.opener().openFolder(APPLICATIONS_DIR)),
                    Item.of(GameText.resolve(HOME), () -> desktop.opener().openFolder(desktop.homeDir())),
                    Item.of(GameText.resolve(UTILITIES), () -> desktop.opener().openFolder(UTILITIES_DIR)))));
            out.add(new Menu(GameText.resolve(WINDOW), List.of(
                    Item.of(GameText.resolve(MINIMIZE), front() != null, this::minimizeFront),
                    Item.of(GameText.resolve(ZOOM), front() != null, this::zoomFront))));
        }
        if (look != QuinceLook.ONE_BIT) {
            out.add(new Menu(GameText.resolve(HELP), List.of(
                    Item.of(GameText.resolve(SYSTEM_HELP.with(systemName())), bundled("help_viewer"),
                            () -> open("help_viewer")))));
        }
        return out;
    }

    /** The fruit's menu: About This Computer, the programs it keeps, and from Quince OS X on the machine's own. */
    private List<Item> fruit(final QuinceLook look) {
        final List<Item> items = new ArrayList<>();
        items.add(Item.of(GameText.resolve(ABOUT), () -> open("this_pc")));
        items.add(Item.RULE);
        if (look.classic()) {
            for (final String path : List.of("calculator", "settings", "qtunes", "expedition")) {
                if (bundled(path)) {
                    items.add(Item.of(nameOf(path), () -> open(path)));
                }
            }
            if (look == QuinceLook.PLATINUM) {
                items.add(Item.RULE);
                items.add(Item.of(GameText.resolve(SEEKER), () -> open("files")));
            }
            return items;
        }
        items.add(Item.of(GameText.resolve(PREFERENCES), () -> open("settings")));
        if (storeHere()) {
            items.add(Item.of(GameText.resolve(STORE), () -> open("quince_store")));
        }
        items.add(Item.RULE);
        items.add(Item.of(GameText.resolve(FORCE_QUIT), () -> desktop.opener().openTaskManager()));
        items.add(Item.RULE);
        items.add(Item.of(GameText.resolve(RESTART), () -> desktop.cyclePower(MachinePowerPayload.ACTION_RESTART)));
        items.add(Item.of(GameText.resolve(SHUT_DOWN),
                () -> desktop.cyclePower(MachinePowerPayload.ACTION_SHUTDOWN)));
        items.add(Item.RULE);
        items.add(Item.of(GameText.resolve(LOG_OUT), () -> desktop.cyclePower(MachinePowerPayload.ACTION_LOG_OFF)));
        return items;
    }

    private List<Item> file(final QuinceLook look) {
        final DesktopWindow front = front();
        final List<Item> items = new ArrayList<>();
        if (look.lights()) {
            items.add(Item.of(GameText.resolve(NEW_WINDOW), () -> desktop.opener().openFolder(DISK_ROOT)));
        } else {
            final int picked = desktop.iconGrid().picked();
            items.add(Item.of(GameText.resolve(OPEN), picked >= 0, () -> desktop.openSlot(picked)));
        }
        items.add(Item.of(GameText.resolve(CLOSE_WINDOW), front != null, () -> desktop.wm().close(front)));
        items.add(Item.RULE);
        items.add(Item.of(GameText.resolve(GET_INFO), look.classic() && front != null
                && WindowKeys.program(front.appKey()) != null, this::getInfo));
        return items;
    }

    /** The Edit menu, which hands its keys to the program in front, as the menu's keys did. */
    private List<Item> edit() {
        final boolean any = front() != null;
        return List.of(Item.of(GameText.resolve(UNDO), any, () -> key(GLFW.GLFW_KEY_Z)), Item.RULE,
                Item.of(GameText.resolve(CUT), any, () -> key(GLFW.GLFW_KEY_X)),
                Item.of(GameText.resolve(COPY), any, () -> key(GLFW.GLFW_KEY_C)),
                Item.of(GameText.resolve(PASTE), any, () -> key(GLFW.GLFW_KEY_V)),
                Item.of(GameText.resolve(SELECT_ALL), any, () -> key(GLFW.GLFW_KEY_A)));
    }

    /** The classic systems' Special menu: the Trash, the disks, and the machine itself. */
    private List<Item> special(final QuinceLook look) {
        final List<Item> items = new ArrayList<>();
        items.add(Item.of(GameText.resolve(CLEAN_UP), this::cleanUp));
        items.add(Item.of(GameText.resolve(EMPTY_TRASH), desktop.trashFull(), () -> desktop.trash().emptyNow()));
        // Both work on the disks as the machine last reported them, which the menu asked for as it opened.
        final DiskReport report = this.disks == null ? DiskReport.NONE : this.disks.report();
        final Era era = desktop.prefs().era();
        items.add(Item.of(GameText.resolve(ERASE_DISK), QuinceDisksPopup.anyToErase(report, era),
                () -> desktop.notices().show(QuinceDisksPopup.erase(look, disks(), era))));
        items.add(Item.RULE);
        items.add(Item.of(GameText.resolve(SET_STARTUP), QuinceDisksPopup.anyToStartFrom(report),
                () -> desktop.notices().show(QuinceDisksPopup.startup(look, disks()))));
        items.add(Item.RULE);
        items.add(Item.of(GameText.resolve(RESTART), () -> desktop.cyclePower(MachinePowerPayload.ACTION_RESTART)));
        items.add(Item.of(GameText.resolve(SHUT_DOWN),
                () -> desktop.cyclePower(MachinePowerPayload.ACTION_SHUTDOWN)));
        return items;
    }

    /* Puts down the menu of the title {@code title}; the Special menu asks the machine about its disks as it opens. */
    private void openMenu(final int title) {
        this.open = title;
        if (title < this.menus.size() && this.menus.get(title).title().equals(GameText.resolve(SPECIAL))) {
            if (this.disks == null) {
                this.disks = new DiskToolClient(desktop.host());
            } else {
                this.disks.refresh();
            }
        }
    }

    /* What the Special menu last read of the disks, asked for now when it never was. */
    private DiskToolClient disks() {
        if (this.disks == null) {
            this.disks = new DiskToolClient(desktop.host());
        }
        return this.disks;
    }

    private void cleanUp() {
        desktop.iconGrid().unpinAll();
    }

    private void aboutFront() {
        open("this_pc");
    }

    private void quitFront() {
        final DesktopWindow front = front();
        if (front != null) {
            desktop.wm().closeGroup(front.groupKey());
        }
    }

    private void minimizeFront() {
        final DesktopWindow front = front();
        if (front != null) {
            desktop.wm().minimizeGroup(front.groupKey());
        }
    }

    private void zoomFront() {
        final DesktopWindow front = front();
        if (front != null) {
            front.toggleMaximize();
        }
    }

    /** The front program's memory block, in its own window. */
    private void getInfo() {
        final DesktopWindow front = front();
        final ProgramSpec spec = front == null ? null : WindowKeys.program(front.appKey());
        if (spec != null) {
            desktop.wm().open(QuinceInfoApp.KEY + spec.id().getPath(), new QuinceInfoApp(desktop, spec));
        }
    }

    /** Hands the program in front the key its menu item stands for, with the command key held. */
    private void key(final int key) {
        final DesktopWindow front = front();
        if (front != null) {
            front.app().keyPressed(key, 0, GLFW.GLFW_MOD_CONTROL);
        }
    }

    /** Starts the program at that path, or brings it forward when it is open. */
    private void open(final String path) {
        final Launcher launcher = desktop.catalogue().find(l -> l.programId() != null
                && l.programId().getPath().equals(path));
        if (launcher == null) {
            return;
        }
        for (final DesktopWindow w : desktop.wm().all()) {
            if (!w.dialog() && w.appKey().equals(launcher.key())) {
                desktop.wm().focus(w);
                return;
            }
        }
        desktop.opener().run(launcher);
    }

    /** The title under a point of the bar, or -1. */
    private int titleAt(final double mx, final double my) {
        if (my < 0 || my >= QuinceBars.BAR_H) {
            return -1;
        }
        for (int i = 0; i < this.titles.size(); i++) {
            final int[] span = this.titles.get(i);
            if (mx >= span[0] && mx < span[1]) {
                return i;
            }
        }
        return -1;
    }

    /** Where a menu hangs from its title at {@code left}: its box, {@code {x, y, w, h}}. */
    private static int[] box(final Font font, final Menu menu, final int left) {
        int w = 0;
        int h = 4;
        for (final Item item : menu.items()) {
            if (item.rule()) {
                h += RULE;
            } else {
                w = Math.max(w, font.width(item.label()));
                h += ROW;
            }
        }
        return new int[] {Math.max(0, left), QuinceBars.BAR_H - 1, w + 2 * PAD + 4, h};
    }

    @Nullable
    private static Item itemAt(final Menu menu, final int top, final double my) {
        int y = top + 2;
        for (final Item item : menu.items()) {
            final int h = item.rule() ? RULE : ROW;
            if (my >= y && my < y + h) {
                return item.rule() ? null : item;
            }
            y += h;
        }
        return null;
    }

    @Nullable
    private DesktopWindow front() {
        final DesktopWindow front = desktop.wm().front();
        return front != null && !desktop.wm().away(front) ? front : null;
    }

    /** The program in front's icon, for Quince OS 9's Application menu; the Seeker's with nothing open. */
    private ResourceLocation frontProgram() {
        final DesktopWindow front = front();
        final ProgramSpec spec = front == null ? null : WindowKeys.program(front.appKey());
        return spec != null ? spec.id() : program("files");
    }

    /** The program in front's name, the Seeker's with nothing open, as the bar writes it. */
    private String appName() {
        final DesktopWindow front = front();
        final ProgramSpec spec = front == null ? null : WindowKeys.program(front.appKey());
        return spec != null ? nameOf(spec.id().getPath()) : nameOf("files");
    }

    private String nameOf(final String path) {
        return desktop.nameOf(WindowKeys.of(program(path)));
    }

    private String systemName() {
        return desktop.platform().label();
    }

    /** Whether this desktop bundles the program at that path. */
    private boolean bundled(final String path) {
        return desktop.catalogue().find(l -> l.programId() != null && l.programId().getPath().equals(path)) != null;
    }

    /** Whether the Quince Store is here: it came with Quince OS X from the Standard on. */
    private boolean storeHere() {
        return bundled("quince_store") && desktop.prefs().era() != null
                && !desktop.prefs().era().isAtMost(HardwareEra.TRANSITION);
    }

    private boolean runs(final String path) {
        for (final DesktopWindow w : desktop.wm().all()) {
            final ProgramSpec spec = WindowKeys.program(w.appKey());
            if (!w.dialog() && spec != null && spec.id().getPath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    private static ResourceLocation program(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }

    /** One menu of the bar: its title and its items. */
    private record Menu(String title, List<Item> items) {
    }

    /**
     * One item of a menu: what it says, whether it can be chosen, and what it does; a rule between groups is an item
     * of its own.
     */
    private record Item(String label, boolean enabled, Runnable action, boolean rule) {

        static final Item RULE = new Item("", false, () -> { }, true);

        static Item of(final String label, final Runnable action) {
            return new Item(label, true, action, false);
        }

        static Item of(final String label, final boolean enabled, final Runnable action) {
            return new Item(label, enabled, action, false);
        }
    }
}
