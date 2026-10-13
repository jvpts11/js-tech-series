/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.TrashItem;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.TrashLayout;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Frames' Recycle Bin, as its explorer showed the bin: a pane down the left with the bin's tasks and a count of what
 * it holds, and a list beside it giving each file's name, the folder it was deleted from and its size.
 *
 * <p>On XP the pane is the blue task pane of that edition, with its white boxes and blue links; the other editions
 * draw it in their own panel.
 */
@PaletteHolder
final class TrashFramesLook implements ITrashLook {

    private final TrashApp app;
    private final ContextMenu menu = new ContextMenu(MENU_W, TrashLayout.ROW_H);
    private int left;
    private int top;
    private int width;
    private int height;
    private int lastClicked = -1;
    private long lastClickAt;

    private static final int MENU_W = 96;
    private static final long DOUBLE_CLICK_MS = 300L;
    private static final float SMALL = Texts.SMALL;
    /**
     * The XP look's own colours, {@code jsc:app/trash_xp}: its task pane from top to foot, the boxes on it, and a
     * link at rest and under the cursor.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/trash_xp",
            new Colours(0xFF7BA2E7, 0xFF6375D6, 0xFFFFFFFF, 0xFF215DC6, 0xFF428EFF));

    TrashFramesLook(final TrashApp app) {
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
        g.fill(x, y, x + w, y + h, skin.panelBg());
        drawPane(g, font, mouseX, mouseY);
        drawList(g, font, mouseX, mouseY);
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
        if (button == 0 && TrashLayout.task(0).holds(rx, ry)) {
            this.app.emptyAll();
            return;
        }
        if (button == 0 && TrashLayout.task(1).holds(rx, ry)) {
            this.app.restoreSelected();
            return;
        }
        if (rx < TrashLayout.PANE_W || ry < TrashLayout.rowY(0)) {
            return;
        }
        final int index = this.app.scroll() + (int) ((ry - TrashLayout.rowY(0)) / TrashLayout.ROW_H);
        final List<TrashItem> items = this.app.items();
        if (index < 0 || index >= items.size()) {
            this.app.clearSelection();
            return;
        }
        final TrashItem item = items.get(index);
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
        return Math.max(0, this.app.items().size() - TrashLayout.rowsShown(this.height));
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
        final int row = index - this.app.scroll();
        if (row < 0 || row >= TrashLayout.rowsShown(this.height)) {
            return null;
        }
        return new int[] {this.left + TrashLayout.nameX() + 10,
                this.top + TrashLayout.rowY(row) + TrashLayout.ROW_H / 2};
    }

    @Nullable
    @Override
    public int[] controlPoint(final String label) {
        final Rect r;
        if (label.equals(GameText.resolve(TrashTexts.EMPTY_RECYCLE_BIN))) {
            r = TrashLayout.task(0);
        } else if (label.equals(GameText.resolve(TrashTexts.RESTORE_ONE))
                || label.equals(GameText.resolve(TrashTexts.RESTORE_MANY))) {
            r = TrashLayout.task(1);
        } else {
            return null;
        }
        return new int[] {this.left + r.x() + r.w() / 2, this.top + r.y() + r.h() / 2};
    }

    private boolean xp() {
        return this.app.style() == PanelStyle.FRAMES_XP;
    }

    private void openMenu(final int mx, final int my) {
        final List<ContextMenu.Item> entries = new ArrayList<>();
        entries.add(new ContextMenu.Item(GameText.resolve(TrashTexts.RESTORE), true, this.app::restoreSelected));
        entries.add(ContextMenu.Item.separator());
        entries.add(new ContextMenu.Item(GameText.resolve(TrashTexts.DELETE), true, this.app::deleteSelected));
        entries.add(ContextMenu.Item.separator());
        final List<TrashItem> chosen = this.app.selection();
        entries.add(new ContextMenu.Item(GameText.resolve(TrashTexts.PROPERTIES), chosen.size() == 1,
                () -> this.app.showProperties(chosen.getFirst())));
        this.menu.open(entries, mx, my, this.left, this.top, this.width, this.height);
    }

    /** The pane down the left: the bin's tasks, then the count of what it holds. */
    private void drawPane(final GuiGraphics g, final Font font, final int mouseX, final int mouseY) {
        final OsSkin skin = this.app.skin();
        final int x = this.left;
        final int y = this.top;
        if (xp()) {
            g.fillGradient(x, y, x + TrashLayout.PANE_W, y + this.height, PALETTE.get().paneTop(),
                    PALETTE.get().paneFoot());
        } else {
            g.fill(x, y, x + TrashLayout.PANE_W, y + this.height, skin.listHover());
            g.fill(x + TrashLayout.PANE_W - 1, y, x + TrashLayout.PANE_W, y + this.height, skin.edge());
        }
        final List<TrashItem> items = this.app.items();
        final int picked = this.app.selection().size();
        final Rect tasks = TrashLayout.tasksBox();
        final int ground = box(g, font, tasks, GameText.resolve(TrashTexts.TASKS));
        task(g, font, 0, GameText.resolve(TrashTexts.EMPTY_RECYCLE_BIN), !items.isEmpty(), ground, mouseX, mouseY);
        task(g, font, 1, GameText.resolve(picked > 1 ? TrashTexts.RESTORE_MANY : TrashTexts.RESTORE_ONE), picked > 0,
                ground, mouseX, mouseY);
        final Rect details = TrashLayout.detailsBox();
        final int detailsGround = box(g, font, details, GameText.resolve(TrashTexts.DETAILS));
        TrashApp.write(g, font, TrashItem.summary(items), x + details.x() + 4,
                y + details.y() + TrashLayout.BOX_HEAD_H + 1, skin.text(), detailsGround, SMALL);
    }

    /** A box of the pane with its heading, answering the colour its links are written on. */
    private int box(final GuiGraphics g, final Font font, final Rect r, final String heading) {
        final OsSkin skin = this.app.skin();
        final int x = this.left + r.x();
        final int y = this.top + r.y();
        final int ground = xp() ? PALETTE.get().box() : skin.panelBg();
        if (xp()) {
            g.fill(x, y, x + r.w(), y + r.h(), PALETTE.get().box());
        } else {
            skin.panel(g, x, y, r.w(), r.h());
        }
        TrashApp.write(g, font, heading, x + 4, y + 3, xp() ? PALETTE.get().link() : skin.text(), ground,
                SMALL);
        g.fill(x + 3, y + TrashLayout.BOX_HEAD_H - 2, x + r.w() - 3, y + TrashLayout.BOX_HEAD_H - 1, skin.edge());
        return ground;
    }

    private void task(final GuiGraphics g, final Font font, final int index, final String label,
                      final boolean enabled, final int ground, final int mouseX, final int mouseY) {
        final OsSkin skin = this.app.skin();
        final Rect r = TrashLayout.task(index);
        final boolean hover = enabled && r.holds(mouseX - this.left, mouseY - this.top);
        final int ink = !enabled ? skin.dim()
                : xp() ? (hover ? PALETTE.get().linkHover() : PALETTE.get().link()) : skin.accent();
        final int x = this.left + r.x();
        final int y = this.top + r.y() + 2;
        TrashApp.write(g, font, label, x, y, ink, ground, SMALL);
        if (hover) {
            g.fill(x, y + 8, x + Texts.smallWidth(font, label), y + 9, ink);
        }
    }

    /** The list: its column headings, then a row for each thing in the bin. */
    private void drawList(final GuiGraphics g, final Font font, final int mouseX, final int mouseY) {
        final OsSkin skin = this.app.skin();
        final Rect header = TrashLayout.header(this.width);
        final int hx = this.left + header.x();
        final int hy = this.top + header.y();
        g.fill(hx, hy, hx + header.w(), hy + header.h(), skin.windowBg());
        g.fill(hx, hy + header.h() - 1, hx + header.w(), hy + header.h(), skin.edge());
        TrashApp.write(g, font, TrashTexts.NAME_COLUMN.text(), this.left + TrashLayout.nameX(), hy + 2, skin.text(),
                skin.windowBg(), 1f);
        TrashApp.write(g, font, TrashTexts.PLACE_COLUMN.text(), this.left + TrashLayout.placeX(this.width), hy + 2,
                skin.text(), skin.windowBg(), 1f);
        final String size = GameText.resolve(TrashTexts.SIZE_COLUMN);
        TrashApp.write(g, font, size, this.left + TrashLayout.sizeRight(this.width) - font.width(size), hy + 2,
                skin.text(), skin.windowBg(), 1f);
        final List<TrashItem> items = this.app.items();
        final int shown = TrashLayout.rowsShown(this.height);
        final int rowW = this.width - TrashLayout.PANE_W;
        for (int row = 0; row < shown && this.app.scroll() + row < items.size(); row++) {
            final TrashItem item = items.get(this.app.scroll() + row);
            final int ry = this.top + TrashLayout.rowY(row);
            final boolean picked = this.app.isSelected(item);
            final boolean hover = mouseX >= hx && mouseX < hx + rowW && mouseY >= ry
                    && mouseY < ry + TrashLayout.ROW_H && !this.menu.isOpen();
            skin.listRow(g, hx, ry, rowW, TrashLayout.ROW_H, hover, picked);
            final int ground = picked ? skin.listSelect() : skin.panelBg();
            ProgramIcons.draw(g, hx + 3, ry, TrashLayout.ICON_W, TrashLayout.ROW_H, this.app.iconOf(item),
                    this.app.iconSet());
            final int ink = skin.listRowText(picked);
            final int nameRoom = TrashLayout.placeX(this.width) - TrashLayout.nameX() - 4;
            final int placeRoom = TrashLayout.sizeRight(this.width) - TrashLayout.SIZE_COL_W
                    - TrashLayout.placeX(this.width);
            TrashApp.write(g, font, Texts.clip(font, item.name(), nameRoom), this.left + TrashLayout.nameX(), ry + 2,
                    ink, ground, 1f);
            TrashApp.write(g, font, Texts.clip(font, item.place(), placeRoom),
                    this.left + TrashLayout.placeX(this.width), ry + 2, ink, ground, 1f);
            final String weight = GameText.resolve(item.size());
            TrashApp.write(g, font, weight, this.left + TrashLayout.sizeRight(this.width) - font.width(weight), ry + 2,
                    ink, ground, 1f);
        }
    }

    /** The XP look's colours, as the palette above names them. */
    private record Colours(int paneTop, int paneFoot, int box, int link, int linkHover) {
    }
}
