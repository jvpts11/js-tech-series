/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.menu.DesktopMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The player's own inventory as the desktop shows it: the menu's 36 real slots, laid over the inventory zone of the
 * window in front when that window's program has one, and switched off everywhere else, so the inventory appears only
 * inside that window. The slots are the container's, so a click on one is a real container click; the desktop draws
 * their items, lights the one under the pointer and carries the stack on the cursor.
 */
final class InventoryBand {

    private final DesktopScreen desktop;

    InventoryBand(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /**
     * Positions the slots over the front window's inventory zone and switches them on, or switches them off when the
     * window in front has none. Run every tick and again at the top of every frame, so the slots track a window being
     * dragged or resized frame by frame; the menu only rebuilds them when their origin actually moved, so this is
     * cheap to call that often.
     *
     * <p>The origin is window-local, and since the container draws and hit-tests a slot at the desktop's own corner
     * plus the slot's position, that is exactly the offset it measures the slots from.
     */
    void sync() {
        final DesktopMenu menu = desktop.getMenu();
        final DesktopWindow w = desktop.frontWindow();
        if (w == null || !(w.app() instanceof IInventoryBandApp app)) {
            menu.setSlotsActive(false);
            return;
        }
        final DesktopViewport view = desktop.view();
        /*
         * The window's rectangle for this frame comes first, so the slots never lag a frame behind a drag, a resize or
         * a maximize: its position is otherwise only brought up to date when the window itself draws.
         */
        w.resolveGeometry(view.width(), view.height(), view.panelReserve(), view.workAreaTop());
        // The window in front is the one that should receive the network's listings and console output.
        app.markActive();
        menu.setSlotsActive(true);
        /*
         * The window-local corner of the first cell: past the border and the title bar to the program's content,
         * then the program's own offset to its inventory zone. That zone is a fixed, framed band pinned just above
         * the footer, measured on the window's live content height rather than a height the program kept, so the
         * cells line up with their backgrounds from the very first frame.
         */
        final int contentHeight = w.height() - DesktopWindow.TITLE_H - 8;
        final int contentTop = w.y() + DesktopWindow.TITLE_H + 4;
        final int originX = w.x() + 4 + app.invCellContentX(0);
        final int originY = contentTop + app.invCellContentY(0, contentHeight);
        /*
         * The band spans the rows the program shows: it never scrolls and is never clipped, but it can fold its top
         * rows away, and a slot above its top goes inert.
         */
        final int bandTop = contentTop + app.invBandTop(contentHeight);
        final int bandBottom = contentTop + app.invBandBottom(contentHeight);
        menu.layoutInventory(originX, originY, bandTop, bandBottom);
    }

    /**
     * Draws the items in the live slots, with the highlight on the one under the pointer, in desktop-local coordinates
     * right after the windows, so they sit over the front window's inventory zone. Returns the slot under the
     * pointer, which the tooltip and the carried stack go by, or null.
     */
    @Nullable
    Slot render(final GuiGraphics g, final int lmx, final int lmy) {
        final DesktopMenu menu = desktop.getMenu();
        if (!menu.slotsActive()) {
            return null;
        }
        /*
         * A modal dialog in the front window's program disables the inventory: the items still draw, under the
         * dialog's dimming, but nothing lights up and nothing takes a click.
         */
        final boolean modal = desktop.focusModal();
        Slot hovered = null;
        /*
         * The items are drawn straight at the slots' positions, which are desktop-local like the pointer here: the
         * container's own slot drawing would add the desktop's corner a second time and set the items off their
         * backgrounds.
         */
        for (final Slot slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            final ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                DesktopItems.item(g, stack, slot.x, slot.y);
                DesktopItems.count(g, desktop.textFont(), stack, slot.x, slot.y);
            }
            if (!modal && over(slot, lmx, lmy)) {
                hovered = slot;
                g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, DesktopShellPalette.get().slotHover());
            }
        }
        return hovered;
    }

    /**
     * The live slot under a point on the game's screen, or null. It tests the same 16 by 16 cells the container does,
     * so a click found here can be handed to the container, which expects that geometry.
     */
    @Nullable
    Slot slotAt(final double absX, final double absY) {
        final DesktopMenu menu = desktop.getMenu();
        if (!menu.slotsActive()) {
            return null;
        }
        final double mx = desktop.view().localX(absX);
        final double my = desktop.view().localY(absY);
        for (final Slot slot : menu.slots) {
            if (slot.isActive() && over(slot, mx, my)) {
                return slot;
            }
        }
        return null;
    }

    /** Draws the stack on the cursor at the pointer, in desktop-local coordinates. */
    void renderCarried(final GuiGraphics g, final int lmx, final int lmy) {
        final ItemStack carried = desktop.getMenu().getCarried();
        if (!carried.isEmpty()) {
            g.renderItem(carried, lmx - 8, lmy - 8);
            g.renderItemDecorations(desktop.textFont(), carried, lmx - 8, lmy - 8);
        }
    }

    private static boolean over(final Slot slot, final double x, final double y) {
        return x >= slot.x && x < slot.x + 16 && y >= slot.y && y < slot.y + 16;
    }
}
