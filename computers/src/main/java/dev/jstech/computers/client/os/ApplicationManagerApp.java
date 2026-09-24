/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.CdeAppManagerLayout;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.os.CdeAppGroup;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * CDE's Application Manager: every program on the machine, sorted into groups, so that nobody has to know what
 * a program is called to find it. The first window shows the groups as folders, a double click on one opens it
 * in a window of its own, and a double click on a program there starts it.
 *
 * <p>A group is only drawn when it holds something, so a fresh system shows a few and the rest arrive with what
 * is installed. What a window shows is read from the desktop each time it is drawn, so a program installed
 * while it is open appears in it without being asked for.
 */
@PaletteHolder
final class ApplicationManagerApp implements IDesktopApp {

    /** What the first window is called, and what the name of a group's window begins with. */
    static final String KEY = "Application Manager";

    private static final String GROUP_MARK = " - ";

    /** The group this window has open, or null for the window of the groups themselves. */
    @Nullable
    private final CdeAppGroup group;
    private OsSkin skin;

    /** What is picked, and what the last click landed on and when, which is what tells a double click. */
    private int picked = -1;
    private int lastClicked = -1;
    private long lastClickAt;

    /* Where the content was last drawn, which is what a click is measured against. */
    private int left;
    private int top;
    private int width;
    private int height;

    private static final long DOUBLE_CLICK_MS = 300L;
    /** Its own colours, {@code jsc:app/application_manager}: a picked name, and the folders a group is drawn as. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/application_manager",
            new Colours(0xFF2E3A66, 0xFFFFFFFF, 0xFFFFE9A8, 0xFFF4C842, 0xFFFFF3C4, 0xFF9A7B16));

    /*
     * The window keeps no desktop of its own. A program's window outlives the screen it was opened on, so it asks
     * for the desktop that is up each time it needs one, and shows nothing while none is.
     */
    ApplicationManagerApp(@Nullable final CdeAppGroup group) {
        this.group = group;
    }

    /** The key of the window that has {@code group} open, or of the first window for null. */
    static String keyOf(@Nullable final CdeAppGroup group) {
        return group == null ? KEY : KEY + GROUP_MARK + group.label();
    }

    /** Whether a window so keyed is one of the Application Manager's. */
    static boolean owns(final String key) {
        return key.equals(KEY) || key.startsWith(KEY + GROUP_MARK);
    }

    /** The group a window so keyed has open, or null for the first window. */
    @Nullable
    static CdeAppGroup groupOf(final String key) {
        return key.startsWith(KEY + GROUP_MARK)
                ? CdeAppGroup.labelled(key.substring((KEY + GROUP_MARK).length())) : null;
    }

    /** The names under the icons this window shows, in the order it shows them, for a test to read. */
    List<String> names() {
        final List<String> out = new ArrayList<>();
        if (this.group == null) {
            for (final CdeAppGroup each : groups()) {
                out.add(each.label());
            }
        } else {
            for (final DesktopScreen.Launcher launcher : programs()) {
                out.add(launcher.label());
            }
        }
        return out;
    }

    /** The middle of the icon so named, in desktop pixels, or null when the window does not show it. */
    @Nullable
    int[] iconCentre(final String name) {
        final int index = names().indexOf(name);
        if (index < 0) {
            return null;
        }
        final Rect r = CdeAppManagerLayout.picture(index, this.group != null, this.width, this.height);
        return new int[] {this.left + r.x() + r.w() / 2, this.top + r.y() + r.h() / 2};
    }

    @Override
    public String title() {
        return keyOf(this.group);
    }

    @Override
    public int defaultWidth() {
        return this.group == null ? 292 : 300;
    }

    @Override
    public int defaultHeight() {
        return this.group == null ? 84 : 150;
    }

    @Override
    public int minWidth() {
        return 160;
    }

    @Override
    public int minHeight() {
        return 84;
    }

    @Override
    public void applySkin(final OsSkin skin) {
        this.skin = skin;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        this.left = x;
        this.top = y;
        this.width = w;
        this.height = h;
        if (this.skin == null) {
            return;
        }
        final boolean headed = this.group != null;
        if (headed) {
            final Rect head = CdeAppManagerLayout.head(w);
            this.skin.panel(g, x + head.x(), y + head.y(), head.w(), head.h());
            final int count = programs().size();
            final String says = this.group.label() + ", " + count + (count == 1 ? " program" : " programs");
            g.drawString(font, says, x + head.x() + 4, y + head.y() + 3, this.skin.text(), false);
        }
        final Rect well = CdeAppManagerLayout.well(headed, w, h);
        this.skin.panel(g, x + well.x(), y + well.y(), well.w(), well.h());
        final List<String> names = names();
        for (int i = 0; i < names.size(); i++) {
            // An icon the well has no room for waits until the window is made larger, rather than run over its rim.
            final Rect cell = CdeAppManagerLayout.cell(i, headed, w, h);
            if (cell.y() + cell.h() <= well.y() + well.h()) {
                drawIcon(g, font, i, names.get(i), headed);
            }
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (button != 0) {
            return;
        }
        final int at = CdeAppManagerLayout.indexAt(mouseX - this.left, mouseY - this.top, names().size(),
                this.group != null, this.width, this.height);
        final long now = System.currentTimeMillis();
        final boolean twice = at >= 0 && at == this.lastClicked && now - this.lastClickAt < DOUBLE_CLICK_MS;
        this.picked = at;
        this.lastClicked = twice ? -1 : at;
        this.lastClickAt = now;
        if (twice) {
            open(at);
        }
    }

    /** A group opens in a window of its own; a program starts. */
    private void open(final int index) {
        final DesktopScreen desktop = DesktopScreen.current();
        if (desktop == null) {
            return;
        }
        if (this.group == null) {
            desktop.openApplicationManager(groups().get(index));
        } else {
            desktop.launch(programs().get(index));
        }
    }

    private void drawIcon(final GuiGraphics g, final Font font, final int index, final String name,
                          final boolean headed) {
        final Rect cell = CdeAppManagerLayout.cell(index, headed, this.width, this.height);
        final Rect pic = CdeAppManagerLayout.picture(index, headed, this.width, this.height);
        final int px = this.left + pic.x();
        final int py = this.top + pic.y();
        final DesktopScreen desktop = DesktopScreen.current();
        if (this.group == null) {
            folder(g, px, py);
        } else if (desktop != null) {
            ProgramIcons.draw(g, px, py, pic.w(), pic.h() - 2, programs().get(index).programId(), desktop.icons());
        }
        final String shown = fit(font, name, cell.w() - 4);
        final int nameW = Texts.smallWidth(font, shown);
        final int nameX = this.left + cell.x() + (cell.w() - nameW) / 2;
        final int nameY = this.top + cell.y() + CdeAppManagerLayout.ICON + 2;
        if (index == this.picked) {
            g.fill(nameX - 2, nameY - 1, nameX + nameW + 2, nameY + CdeAppManagerLayout.NAME_H - 1,
                    PALETTE.get().pickedFill());
        }
        Texts.small(g, font, shown, nameX, nameY, index == this.picked ? PALETTE.get().pickedInk() : this.skin.text());
    }

    /** The groups that hold something on this machine, in the Application Manager's own order. */
    private List<CdeAppGroup> groups() {
        final boolean[] holds = new boolean[CdeAppGroup.values().length];
        for (final DesktopScreen.Launcher launcher : launchers()) {
            holds[CdeAppGroup.of(launcher.programId().getPath()).place()] = true;
        }
        final List<CdeAppGroup> out = new ArrayList<>();
        for (final CdeAppGroup each : CdeAppGroup.values()) {
            if (holds[each.place()]) {
                out.add(each);
            }
        }
        return out;
    }

    /** The programs of this window's group, in the order the desktop lists them. */
    private List<DesktopScreen.Launcher> programs() {
        final List<DesktopScreen.Launcher> out = new ArrayList<>();
        for (final DesktopScreen.Launcher launcher : launchers()) {
            if (CdeAppGroup.of(launcher.programId().getPath()) == this.group) {
                out.add(launcher);
            }
        }
        return out;
    }

    /** Every program of the desktop that is up, or none while no desktop is. */
    private static List<DesktopScreen.Launcher> launchers() {
        final DesktopScreen desktop = DesktopScreen.current();
        return desktop == null ? List.of() : desktop.launcherList();
    }

    /** A name in the small text, cut with an ellipsis when it is wider than its cell. */
    private static String fit(final Font font, final String name, final int room) {
        if (Texts.smallWidth(font, name) <= room) {
            return name;
        }
        final int units = Math.max(1, Texts.smallFits(room) - font.width("..."));
        return font.plainSubstrByWidth(name, units) + "...";
    }

    /** A folder the way the desktop draws one: a tab, a body, a line of light along its head. */
    private static void folder(final GuiGraphics g, final int x, final int y) {
        final Colours c = PALETTE.get();
        g.fill(x + 1, y + 1, x + 10, y + 4, c.folderTab());
        g.fill(x + 1, y + 4, x + 23, y + 20, c.folderBody());
        g.fill(x + 1, y + 4, x + 23, y + 6, c.folderLight());
        g.fill(x + 1, y + 1, x + 10, y + 2, c.folderEdge());
        g.fill(x + 1, y + 19, x + 23, y + 20, c.folderEdge());
        g.fill(x + 1, y + 1, x + 2, y + 20, c.folderEdge());
        g.fill(x + 22, y + 4, x + 23, y + 20, c.folderEdge());
    }

    /** A picked name's fill and ink, and a folder's tab, body, the light along its head and its edge. */
    private record Colours(int pickedFill, int pickedInk, int folderTab, int folderBody, int folderLight,
                           int folderEdge) {
    }
}
