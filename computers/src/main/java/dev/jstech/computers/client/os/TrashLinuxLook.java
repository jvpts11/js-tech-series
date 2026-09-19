/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.TrashItem;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.TrashLayout;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.UiContext;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

/**
 * The Trash of the Linux desktops, as their file managers show it: a bar over the view with the button that empties
 * it and a count of what it holds, the places down the left with the Trash picked among them, and the files as icons.
 *
 * <p>A file's menu says what its own desktop says: Dolphin restores to the former location, GNOME's Files restores
 * from the Trash, and Nemo simply restores.
 */
final class TrashLinuxLook implements ITrashLook {

    private final TrashApp app;
    private final ContextMenu menu = new ContextMenu(MENU_W, TrashLayout.ROW_H);
    private int left;
    private int top;
    private int width;
    private int height;
    private int lastClicked = -1;
    private long lastClickAt;

    private static final int MENU_W = 140;
    private static final long DOUBLE_CLICK_MS = 300L;
    private static final String EMPTY = "Empty Trash";
    private static final String[] PLACES = {"Home", "Desktop", "Trash"};
    private static final ResourceLocation FILES = ResourceLocation.fromNamespaceAndPath("jsc", "files");

    TrashLinuxLook(final TrashApp app) {
        this.app = app;
    }

    @Override
    public void render(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                       final int mouseX, final int mouseY) {
        this.left = x;
        this.top = y;
        this.width = w;
        this.height = h;
        final OsSkin skin = this.app.skin();
        g.fill(x, y, x + w, y + h, skin.windowBg());
        drawBar(g, font, mouseX, mouseY);
        drawPlaces(g, font, mouseX, mouseY);
        final Rect view = view();
        g.fill(view.x(), view.y(), view.x() + view.w(), view.y() + view.h(), skin.panelBg());
        g.fill(view.x(), view.y(), view.x() + 1, view.y() + view.h(), skin.edge());
        TrashIcons.render(g, font, this.app, view,
                new TrashIcons.Inks(skin.panelBg(), skin.text(), skin.listSelect(), skin.listRowText(true)),
                this.menu.isOpen() ? -1 : mouseX, mouseY);
        this.menu.render(g, new UiContext(skin, font, mouseX, mouseY, 0f));
    }

    @Override
    public void click(final double mouseX, final double mouseY, final int button) {
        if (this.menu.isOpen()) {
            this.menu.mouseClicked(mouseX, mouseY, button);
            return;
        }
        final double rx = mouseX - this.left;
        final double ry = mouseY - this.top;
        if (button == 0 && TrashLayout.emptyButton().holds(rx, ry)) {
            this.app.emptyAll();
            return;
        }
        for (int i = 0; i < TrashLayout.PLACES - 1; i++) {
            if (button == 0 && TrashLayout.place(i).holds(rx, ry)) {
                this.app.openPlace(i == 1);
                return;
            }
        }
        final Rect view = view();
        if (!view.holds(mouseX, mouseY)) {
            return;
        }
        final int index = TrashIcons.indexAt(this.app, view, mouseX, mouseY);
        if (index < 0) {
            this.app.clearSelection();
            return;
        }
        final TrashItem item = this.app.items().get(index);
        if (button == 1) {
            if (!this.app.isSelected(item)) {
                this.app.select(item, false);
            }
            openMenu((int) mouseX, (int) mouseY);
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
        return TrashIcons.scrollLimit(this.app, view());
    }

    @Override
    public boolean menuOpen() {
        return this.menu.isOpen();
    }

    @Override
    public List<String> menuLabels() {
        return TrashIcons.labels(this.menu);
    }

    @Nullable
    @Override
    public int[] menuPoint(final String label) {
        return TrashIcons.pointOf(this.menu, label);
    }

    @Nullable
    @Override
    public int[] itemCentre(final int index) {
        return TrashIcons.centre(this.app, view(), index);
    }

    @Nullable
    @Override
    public int[] controlPoint(final String label) {
        final Rect r;
        if (label.equals(EMPTY)) {
            r = TrashLayout.emptyButton();
        } else if (label.equals(PLACES[0])) {
            r = TrashLayout.place(0);
        } else if (label.equals(PLACES[1])) {
            r = TrashLayout.place(1);
        } else {
            return null;
        }
        return new int[] {this.left + r.x() + r.w() / 2, this.top + r.y() + r.h() / 2};
    }

    private Rect view() {
        final Rect r = TrashLayout.linuxView(this.width, this.height);
        return new Rect(this.left + r.x(), this.top + r.y(), r.w(), r.h());
    }

    private void openMenu(final int mx, final int my) {
        final String[] words = switch (this.app.style()) {
            case GNOME -> new String[] {"Restore From Trash", "Delete From Trash"};
            case CINNAMON -> new String[] {"Restore", "Delete Permanently"};
            default -> new String[] {"Restore to Former Location", "Delete"};
        };
        final List<ContextMenu.Item> entries = new ArrayList<>();
        entries.add(new ContextMenu.Item(words[0], true, this.app::restoreSelected));
        entries.add(new ContextMenu.Item(words[1], true, this.app::deleteSelected));
        entries.add(ContextMenu.Item.separator());
        final List<TrashItem> chosen = this.app.selection();
        entries.add(new ContextMenu.Item("Properties", chosen.size() == 1,
                () -> this.app.showProperties(chosen.getFirst())));
        this.menu.open(entries, mx, my, this.left, this.top, this.width, this.height);
    }

    /** The bar over the view: the button that empties the Trash, and what the Trash holds. */
    private void drawBar(final GuiGraphics g, final Font font, final int mouseX, final int mouseY) {
        final OsSkin skin = this.app.skin();
        g.fill(this.left, this.top + TrashLayout.BAR_H - 1, this.left + this.width, this.top + TrashLayout.BAR_H,
                skin.edge());
        final Rect b = TrashLayout.emptyButton();
        final int bx = this.left + b.x();
        final int by = this.top + b.y();
        final boolean any = !this.app.items().isEmpty();
        skin.button(g, font, bx, by, b.w(), b.h(), EMPTY, any && b.holds(mouseX - this.left, mouseY - this.top),
                false, false);
        if (!any) {
            Draw.disabled(g, bx, by, b.w(), b.h());
        }
        TrashApp.write(g, font, TrashItem.summary(this.app.items()), this.left + TrashLayout.summaryX(),
                this.top + 4, skin.dim(), skin.windowBg(), 1f);
    }

    /** The places down the left, with the Trash picked among them. */
    private void drawPlaces(final GuiGraphics g, final Font font, final int mouseX, final int mouseY) {
        final OsSkin skin = this.app.skin();
        final int x = this.left;
        final int y = this.top + TrashLayout.BAR_H;
        g.fill(x, y, x + TrashLayout.PLACES_W, this.top + this.height, skin.listHover());
        for (int i = 0; i < TrashLayout.PLACES; i++) {
            final Rect r = TrashLayout.place(i);
            final int px = this.left + r.x();
            final int py = this.top + r.y();
            final boolean here = i == TrashLayout.PLACES - 1;
            skin.listRow(g, px, py, r.w(), r.h(), !here && r.holds(mouseX - this.left, mouseY - this.top), here);
            ProgramIcons.draw(g, px + 3, py, TrashLayout.ICON_W, r.h(), here ? this.app.trashIcon() : FILES,
                    this.app.iconSet());
            TrashApp.write(g, font, PLACES[i], px + 3 + TrashLayout.ICON_W + 3, py + 3, skin.listRowText(here),
                    here ? skin.listSelect() : skin.listHover(), 1f);
        }
    }
}
