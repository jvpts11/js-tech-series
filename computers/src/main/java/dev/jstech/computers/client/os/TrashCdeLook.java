/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.TrashItem;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.TrashLayout;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * CDE's Trash Can: a menu bar over a sunken well of objects and a line under it counting them.
 *
 * <p>CDE says Put Back and Shred for what the other desktops call restoring and deleting for good, and has no button
 * that empties the can: everything is selected and shredded, as it always was. Its menus list only what works.
 */
final class TrashCdeLook implements ITrashLook {

    private final TrashApp app;
    private final MotifMenu menu = new MotifMenu();
    /** The menu of the bar that is down, or -1 while the menu that is up came from a right-click, or none is. */
    private int openTitle = -1;
    private int left;
    private int top;
    private int width;
    private int height;
    private int lastClicked = -1;
    private long lastClickAt;

    private static final long DOUBLE_CLICK_MS = 300L;
    private static final String[] TITLES = {"File", "Selected", "View"};

    TrashCdeLook(final TrashApp app) {
        this.app = app;
    }

    @Override
    public void render(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                       final int mouseX, final int mouseY) {
        this.left = x;
        this.top = y;
        this.width = w;
        this.height = h;
        final CdePalette p = palette();
        g.fill(x, y, x + w, y + h, p.window());
        for (int i = 0; i < TrashLayout.MENUS; i++) {
            final Rect r = TrashLayout.menu(i);
            if (this.menu.isOpen() && this.openTitle == i) {
                MotifChrome.raised(g, x + r.x(), y + r.y(), r.w(), r.h(), p.window(), p);
            }
            final int tx = x + r.x() + (r.w() - font.width(TITLES[i])) / 2;
            TrashApp.write(g, font, TITLES[i], tx, y + r.y() + 2, p.ink(), p.window(), 1f);
            // The letter that opens the menu from the keyboard, underlined as Motif marked it.
            g.fill(tx, y + r.y() + 10, tx + font.width(TITLES[i].substring(0, 1)) - 1, y + r.y() + 11, p.ink());
        }
        final Rect well = well();
        MotifChrome.sunken(g, well.x(), well.y(), well.w(), well.h(), p.inset(), p);
        TrashIcons.render(g, font, this.app, well, new TrashIcons.Inks(p.inset(), p.ink(), p.active(), p.activeInk()),
                this.menu.isOpen() ? -1 : mouseX, mouseY);
        TrashApp.write(g, font, TrashItem.objects(this.app.items()), x + 6, y + TrashLayout.statusY(h), p.ink(),
                p.window(), 1f);
        this.menu.render(g, font, mouseX, mouseY, p);
    }

    @Override
    public void click(final double mouseX, final double mouseY, final int button) {
        if (this.menu.isOpen()) {
            final int title = titleAt(mouseX, mouseY);
            if (title >= 0 && title != this.openTitle) {
                openTitle(title, font());
                return;
            }
            this.menu.clicked(mouseX, mouseY);
            this.openTitle = -1;
            return;
        }
        final int title = titleAt(mouseX, mouseY);
        if (title >= 0 && button == 0) {
            openTitle(title, font());
            return;
        }
        final Rect well = well();
        if (!well.holds(mouseX, mouseY)) {
            return;
        }
        final int index = TrashIcons.indexAt(this.app, well, mouseX, mouseY);
        if (index < 0) {
            this.app.clearSelection();
            return;
        }
        final TrashItem item = this.app.items().get(index);
        if (button == 1) {
            if (!this.app.isSelected(item)) {
                this.app.select(item, false);
            }
            this.openTitle = -1;
            this.menu.open(selectedEntries(), (int) mouseX, (int) mouseY, font(), this.left + this.width,
                    this.top + this.height);
            return;
        }
        final long now = System.currentTimeMillis();
        final boolean twice = index == this.lastClicked && now - this.lastClickAt < DOUBLE_CLICK_MS;
        this.lastClicked = index;
        this.lastClickAt = now;
        this.app.select(item, Screen.hasControlDown());
        if (twice) {
            this.app.showProperties(item);
        }
    }

    @Override
    public int scrollLimit() {
        return TrashIcons.scrollLimit(this.app, well());
    }

    @Override
    public boolean menuOpen() {
        return this.menu.isOpen();
    }

    @Override
    public List<String> menuLabels() {
        final List<String> out = new ArrayList<>();
        for (final MotifMenu.Entry entry : this.menu.entries()) {
            if (!entry.isLine()) {
                out.add(entry.label());
            }
        }
        return out;
    }

    @Nullable
    @Override
    public int[] menuPoint(final String label) {
        final List<MotifMenu.Entry> entries = this.menu.entries();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).label().equals(label)) {
                return this.menu.entryCentre(i);
            }
        }
        return null;
    }

    @Nullable
    @Override
    public int[] itemCentre(final int index) {
        return TrashIcons.centre(this.app, well(), index);
    }

    @Nullable
    @Override
    public int[] controlPoint(final String label) {
        for (int i = 0; i < TITLES.length; i++) {
            if (TITLES[i].equals(label)) {
                final Rect r = TrashLayout.menu(i);
                return new int[] {this.left + r.x() + r.w() / 2, this.top + r.y() + r.h() / 2};
            }
        }
        return null;
    }

    private CdePalette palette() {
        final DesktopScreen desktop = DesktopScreen.current();
        return desktop == null ? CdePalette.DEFAULT : desktop.cdePalette();
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private Rect well() {
        final Rect r = TrashLayout.cdeWell(this.width, this.height);
        return new Rect(this.left + r.x(), this.top + r.y(), r.w(), r.h());
    }

    private int titleAt(final double mouseX, final double mouseY) {
        for (int i = 0; i < TrashLayout.MENUS; i++) {
            if (TrashLayout.menu(i).holds(mouseX - this.left, mouseY - this.top)) {
                return i;
            }
        }
        return -1;
    }

    /** Drops the menu of title {@code index} from the bar, just under its title. */
    private void openTitle(final int index, final Font font) {
        final Rect r = TrashLayout.menu(index);
        final List<MotifMenu.Entry> entries = switch (index) {
            case 0 -> List.of(
                    new MotifMenu.Entry("Select All", "", !this.app.items().isEmpty(), this.app::selectAll),
                    new MotifMenu.Entry("Deselect All", "", !this.app.selection().isEmpty(),
                            this.app::clearSelection),
                    MotifMenu.Entry.line(),
                    new MotifMenu.Entry("Close", "", true, this.app::close));
            case 1 -> selectedEntries();
            default -> List.of(
                    new MotifMenu.Entry("By Name", "", this.app.order() != TrashApp.Order.BY_NAME,
                            () -> this.app.orderBy(TrashApp.Order.BY_NAME)),
                    new MotifMenu.Entry("By Size", "", this.app.order() != TrashApp.Order.BY_SIZE,
                            () -> this.app.orderBy(TrashApp.Order.BY_SIZE)));
        };
        this.openTitle = index;
        this.menu.open(entries, this.left + r.x(), this.top + r.y() + r.h(), font, this.left + this.width,
                this.top + this.height);
    }

    /** What CDE does to the objects that are selected. */
    private List<MotifMenu.Entry> selectedEntries() {
        final boolean any = !this.app.selection().isEmpty();
        return List.of(new MotifMenu.Entry("Put Back", "", any, this.app::restoreSelected),
                new MotifMenu.Entry("Shred", "", any, this.app::deleteSelected));
    }
}
