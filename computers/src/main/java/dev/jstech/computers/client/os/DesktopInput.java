/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.operation.payload.NiDepositPayload;
import dev.jstech.computers.operation.payload.NiShiftInsertPayload;
import dev.jstech.computers.operation.payload.workshop.WorkshopActionPayload;
import dev.jstech.computers.os.PanelStyle;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Where the pointer and the keyboard go on the desktop. A click travels down one layer at a time: whatever is modal
 * takes it first, then the panel, then the windows, then the wallpaper and its icons, and only what nothing claimed
 * reaches the container underneath. Each layer says whether it took the click, so the order they are tried in is the
 * whole of the routing and reads in one place. The keys go the same way: a dialog, the desktop's own menus and boxes,
 * an open launcher, and then the window in front.
 *
 * <p>What the screen itself should do with an event nothing here took (the container's own click, drag or key) is
 * answered with {@link Click#CONTAINER}, and the screen passes it on.
 */
final class DesktopInput {

    /** The screen the events arrive at, which a key nothing on the desktop took goes back to. */
    private final DesktopScreen screen;
    private final DesktopState desktop;
    /** The window being moved by its title bar, and where on it the pointer took hold; or null. */
    @Nullable
    private DesktopWindow dragging;
    private int dragOffsetX;
    private int dragOffsetY;
    /** The window being resized by an edge; or null. */
    @Nullable
    private DesktopWindow resizing;
    /** The window whose title-bar button is held down, drawn pushed in until the button is let go; or null. */
    @Nullable
    private DesktopWindow pressedButton;
    /** When the icon that is picked was clicked, which tells a double click. */
    private long pickedAt;

    /** How close two clicks on one icon must come to open it, in milliseconds. */
    private static final long DOUBLE_CLICK_MS = 300L;

    DesktopInput(final DesktopScreen screen, final DesktopState desktop) {
        this.screen = screen;
        this.desktop = desktop;
    }

    /** A button pressed at a point on the game's screen. */
    Click click(final double absX, final double absY, final int button) {
        if (desktop.memory().crashing()) {
            return Click.TAKEN; // the crash screen swallows input until the reboot completes
        }
        if (clickedOverlay(absX, absY, button)) {
            return Click.TAKEN;
        }
        final DesktopViewport view = desktop.view();
        final double mouseX = view.localX(absX);
        final double mouseY = view.localY(absY);
        final int tbY = view.height() - view.panelBand();
        if (clickedPanel(mouseX, mouseY, button, tbY)) {
            return Click.TAKEN;
        }
        final Click inWindow = clickedWindow(absX, absY, mouseX, mouseY, button);
        if (inWindow == Click.TAKEN) {
            return Click.TAKEN;
        }
        if (inWindow == Click.CONTAINER || clickedDesktop(mouseX, mouseY, button) == Click.CONTAINER) {
            return Click.CONTAINER;
        }
        return Click.TAKEN;
    }

    /** The pointer moved to a point on the game's screen with a button held. */
    Click drag(final double absX, final double absY, final int button) {
        if (desktop.notices().popupUp()) {
            return Click.TAKEN;
        }
        final DesktopViewport view = desktop.view();
        final double x = view.localX(absX);
        final double y = view.localY(absY);
        if (desktop.volume().mouseDragged(x, y)) {
            return Click.TAKEN;
        }
        if (dragging != null) {
            dragging.moveTo((int) x - dragOffsetX, (int) y - dragOffsetY, view.workAreaTop(), view.width(),
                    view.workAreaBottom());
            return Click.TAKEN;
        }
        if (resizing != null) {
            resizing.applyResize(x, y, view.workAreaTop(), view.width(), view.workAreaBottom());
            return Click.TAKEN;
        }
        // An icon dragged across the desktop, or the rubber band swept over it.
        if (desktop.drags().drag(x, y)) {
            return Click.TAKEN;
        }
        /*
         * While the window in front that carries the inventory holds a stack on the cursor, a drag is the
         * container's own spreading across slots, so it goes to the container, not the program.
         */
        final DesktopWindow w = desktop.wm().front();
        if (w != null && w.app() instanceof IInventoryBandApp && !desktop.carried().isEmpty()) {
            return Click.CONTAINER;
        }
        if (w != null) {
            w.app().mouseDragged(w, x, y, button);
            return Click.TAKEN;
        }
        return Click.CONTAINER;
    }

    /** A button let go at a point on the game's screen. */
    Click release(final double absX, final double absY, final int button) {
        final DesktopViewport view = desktop.view();
        final double x = view.localX(absX);
        final double y = view.localY(absY);
        desktop.volume().mouseReleased();
        if (desktop.notices().releasePopup(x, y, button)) {
            return Click.TAKEN;
        }
        // Letting go ends the sweep; whatever it covered stays selected.
        if (desktop.drags().endBand()) {
            return Click.TAKEN;
        }
        /*
         * A title-bar button pressed on the way down fires only when let go over that same button; dragging off it
         * takes it back. Either way it comes back out.
         */
        if (pressedButton != null) {
            releaseTitleButton(x, y);
            return Click.TAKEN;
        }
        final boolean iconDropped = desktop.drags().dropIcon(x, y);
        final boolean movingWindow = dragging != null || resizing != null;
        /*
         * A file dragged out of a file manager and let go outside it is the desktop's to place, before the file
         * manager sees the release, so its own drop inside its window does not also run.
         */
        if (!iconDropped && !movingWindow && desktop.drags().dropFromFileManager(x, y)) {
            return Click.CONTAINER;
        }
        // Otherwise the release goes to the program in front, for a drag and drop inside its window.
        if (!iconDropped && !movingWindow) {
            final DesktopWindow w = desktop.wm().front();
            if (w != null) {
                w.app().mouseReleased(w, x, y, button);
            }
        }
        /*
         * Edge snapping on the modern desktops: a window let go against an edge of the screen tiles to it, the top
         * edge filling the work area and a side edge filling that half.
         */
        if (dragging != null && (desktop.panelStyle() == PanelStyle.FRAMES_11 || desktop.linuxDesktop())) {
            snap(dragging, (int) x, (int) y);
        }
        dragging = null;
        resizing = null;
        return Click.CONTAINER;
    }

    /** A character typed; returns whether the desktop took it. */
    boolean typed(final char c) {
        if (desktop.notices().popupUp()) {
            return true;
        }
        final DeskFiles files = desktop.fileActions();
        // A desktop-icon rename captures typing before any window, until the name is as long as it may be.
        if (files.isRenaming() && c >= 32 && c != 127 && c != '/' && c != '\\' && files.type(c)) {
            return true;
        }
        // An open launcher's search box takes the typing; it is always focused while it is shown.
        if (desktop.start().type(c)) {
            return true;
        }
        final DesktopWindow w = desktop.wm().front();
        return w != null && w.app().charTyped(c);
    }

    /**
     * A key, asked before the rest of the game. The desktop takes it ahead of everything only when something on it
     * is being typed at: one of its own menus or boxes, or the program in the window in front. Over everything else
     * a key stays whoever's it was, which is what keeps a recipe viewer's keys working on the items a window shows.
     */
    boolean keyFirst(final int key, final int scanCode, final int modifiers) {
        final DesktopNotices notices = desktop.notices();
        final PowerDialog power = desktop.power();
        // Motif's keys for a window's menu come before the window's own program, as a window manager's do.
        if (desktop.panelStyle() == PanelStyle.CDE && !notices.popupUp() && !power.isOpen()
                && desktop.cdeWindowMenu().keyPressed(key, modifiers, desktop.wm().front())) {
            return true;
        }
        if (notices.popupUp() || power.isOpen() || desktop.deskMenu().isOpen() || desktop.taskbar().menu().isOpen()
                || desktop.fileActions().isRenaming() || desktop.start().isOpen()) {
            return screen.keyPressed(key, scanCode, modifiers);
        }
        final DesktopWindow w = desktop.wm().front();
        return w != null && (key != 256 || w.app().wantsEscape()) && w.app().keyPressed(key, scanCode, modifiers);
    }

    /** A key pressed. */
    Click key(final int key, final int scanCode, final int modifiers) {
        // A modal dialog swallows every key; Enter or Escape dismisses it, and nothing leaks behind it.
        if (desktop.notices().keyPopup(key, scanCode, modifiers)) {
            return Click.TAKEN;
        }
        final VolumePopup volume = desktop.volume();
        if (key == 256 && volume.isOpen()) {
            volume.close();
            return Click.TAKEN;
        }
        // The power dialog decides the fate of the whole machine, so it keeps the keyboard as it keeps the mouse.
        if (desktop.power().keyPressed(key)) {
            return Click.TAKEN;
        }
        // The desktop's menu and a program's are walked with the arrows and left with Escape, like any menu.
        final DeskMenu deskMenu = desktop.deskMenu();
        if (deskMenu.isOpen() && deskMenu.keyPressed(key, scanCode, modifiers)) {
            return Click.TAKEN;
        }
        if (desktop.taskbar().menu().isOpen() && desktop.taskbar().menu().keyPressed(key, scanCode, modifiers)) {
            return Click.TAKEN;
        }
        if (desktop.taskPopup().key() != null && key == 256) {
            desktop.taskPopup().dismiss();
            return Click.TAKEN;
        }
        // An icon's name being typed takes the keys first: Enter keeps it, Escape takes it back.
        final DeskFiles files = desktop.fileActions();
        if (files.isRenaming()) {
            switch (key) {
                case 257, 335 -> files.commitRename();
                case 256 -> files.cancelRename();
                case 259 -> files.backspace();
                default -> {
                    return Click.PASSED;
                }
            }
            return Click.TAKEN;
        }
        // An open launcher owns the keyboard ahead of the desktop, so Escape closes it rather than the desktop.
        if (desktop.start().keyPressed(key)) {
            return Click.TAKEN;
        }
        // The program in front has first refusal, except of Escape, which closes the desktop.
        final DesktopWindow w = desktop.wm().front();
        if (w != null && (key != 256 || w.app().wantsEscape()) && w.app().keyPressed(key, scanCode, modifiers)) {
            return Click.TAKEN;
        }
        // A container screen closes on the inventory key; the desktop must not, or 'E' would put the shell away.
        if (key == Minecraft.getInstance().options.keyInventory.getKey().getValue()) {
            return Click.TAKEN;
        }
        return Click.CONTAINER;
    }

    /** A key let go goes to the window in front, for a program that tells a press from a release. */
    boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        final DesktopWindow w = desktop.wm().front();
        return !desktop.notices().popupUp() && w != null && w.app().keyReleased(key, scanCode, modifiers);
    }

    /** The wheel turned at a point on the game's screen; returns whether the desktop took it. */
    boolean scrolled(final double absX, final double absY, final double dy) {
        if (desktop.notices().popupUp()) {
            return true;
        }
        // The wheel over the speaker, or over its open control, turns the volume a step a notch.
        final DesktopViewport view = desktop.view();
        final double x = view.localX(absX);
        final double y = view.localY(absY);
        final VolumePopup volume = desktop.volume();
        final int panelY = view.panelOnTop() ? 0 : view.height() - view.panelBand();
        if (dy != 0 && desktop.panelStyle() != PanelStyle.CDE && (volume.over(x, y)
                || desktop.tray().onSpeaker(x, y, view.width(), panelY, view.panelOnTop()))) {
            volume.nudge(dy > 0 ? 1 : -1);
            return true;
        }
        final DesktopWindow w = desktop.wm().front();
        return w != null && w.app().mouseScrolled(dy);
    }

    /**
     * Whatever is over everything else: the power dialog, an open program menu, the panel's popup, a modal dialog,
     * and a window holding a dialog of its own. Each of these is modal in its own way, so a click reaching one goes
     * no further down.
     */
    private boolean clickedOverlay(final double absX, final double absY, final int button) {
        final DesktopViewport view = desktop.view();
        final double x = view.localX(absX);
        final double y = view.localY(absY);
        // The power dialog decides the fate of the whole machine, so nothing behind it takes the click.
        if (desktop.power().click(x, y)) {
            return true;
        }
        // The volume control takes the next click like a menu: on it, it turns what it lands on; anywhere else it goes.
        final VolumePopup volume = desktop.volume();
        if (volume.isOpen()) {
            volume.mouseClicked(x, y, button);
            return true;
        }
        if (desktop.taskbar().menu().isOpen()) {
            desktop.taskbar().menu().mouseClicked(x, y, button);
            return true;
        }
        // A window's own menu on CDE takes the click too, unless it is on the very button the menu hangs from.
        final CdeWindowMenu windowMenu = desktop.cdeWindowMenu();
        if (windowMenu.isOpen() && windowMenu.clicked(x, y)) {
            return true;
        }
        final TaskPopup popup = desktop.taskPopup();
        if (popup.key() != null && popup.click(x, y, button)) {
            return true;
        }
        // A modal dialog swallows every click; only its OK button dismisses it, and a click beside it rings the bell.
        if (desktop.notices().clickPopup(x, y, button)) {
            return true;
        }
        // A program's own modal dialog isolates its window: the click goes to it and to nothing behind it.
        if (desktop.wm().focusModal()) {
            final DesktopWindow f = desktop.wm().front();
            if (f != null) {
                f.app().mouseClicked(f, x, y, button);
            }
            return true;
        }
        return false;
    }

    /**
     * The panel and everything that belongs to it: a balloon over it, its own menu, the speaker, the Start button and
     * an open launcher, and the row of program entries. The bar swallows any click that lands on it and misses all of
     * those, so nothing underneath ever reacts to a click on the panel.
     */
    private boolean clickedPanel(final double mouseX, final double mouseY, final int button, final int tbY) {
        final DesktopViewport view = desktop.view();
        // A balloon is dismissed by clicking it, and it swallows that click so nothing under it reacts.
        if (clickedBalloon(mouseX, mouseY, tbY, view.width())) {
            return true;
        }
        // The panel's menu takes the next click wherever it lands: on an entry it runs it, anywhere else it closes.
        final PanelMenu panelMenu = desktop.panelMenu();
        if (panelMenu.click(mouseX, mouseY)) {
            return true;
        }
        final boolean cde = desktop.panelStyle() == PanelStyle.CDE;
        // The speaker on the panel: the left button opens the volume control, the right one its menu.
        if (!cde && desktop.tray().onSpeaker(mouseX, mouseY, view.width(), view.panelOnTop() ? 0 : tbY,
                view.panelOnTop())) {
            if (button == 1) {
                desktop.volume().openMenu((int) mouseX, tbY, view.panelOnTop());
            } else if (button == 0) {
                desktop.volume().toggle();
            }
            return true;
        }
        /*
         * GNOME: the top bar's Activities corner toggles the overview, and the rest of the bar is inert. Only while
         * the bar is at the top: a period GNOME panels at the bottom, and swallowing clicks along the top edge there
         * ate the title bars of every window parked up there.
         */
        if (view.panelOnTop() && mouseY < DesktopScreen.TASKBAR_H) {
            if (mouseX < 64) {
                desktop.start().toggle();
            } else if (button == 1) {
                panelMenu.open((int) mouseX, 0);
            }
            return true;
        }
        /*
         * CDE: the Front Panel answers for itself, and so does the subpanel standing on it. There is no Start button
         * and no row of open programs, and the band either side of the slab is plain desktop.
         */
        if (cde) {
            // A click beside a subpanel leaves it up: only its own arrow puts it away again.
            if (button == 0 && desktop.cdeLaunchers().click(mouseX, mouseY, view.width(), view.height())) {
                return true;
            }
            // The right button on the slab opens the panel's own menu, where the Task Manager has always been.
            if (button == 1 && CdeFrontPanelLayout.panel(view.width(), view.height()).holds(mouseX, mouseY)) {
                panelMenu.open((int) mouseX, tbY);
                return true;
            }
            return desktop.cdePanels().click(mouseX, mouseY, view.width(), view.height());
        }
        // Frames 11 keeps Start with the centred group, so it has a hit test of its own.
        if (desktop.panelStyle() == PanelStyle.FRAMES_11 && mouseY >= tbY) {
            final int startX = desktop.taskbar().modernStartLeft(view.width());
            if (mouseX >= startX && mouseX < startX + DesktopScreen.WIN11_SLOT) {
                desktop.start().toggle();
                return true;
            }
        } else if (desktop.startButtonHit(mouseX, mouseY, tbY)) {
            desktop.start().toggle();
            return true;
        }
        // An open launcher takes a click on it; one anywhere else closes it and goes on below.
        if (desktop.start().click(mouseX, mouseY, button, tbY)) {
            return true;
        }
        /*
         * The panel's entries, one per program. A right click on the panel itself, clear of Start and of the entries,
         * opens the panel's own menu, the way every one of these desktops offers it. The rest of the bar is the bar
         * and swallows the click.
         */
        if (!view.panelOnTop() && mouseY >= tbY) {
            final TaskStrip strip = desktop.taskbar().strip(view.width());
            int idx = strip.indexAt(mouseX);
            int atX = idx >= 0 ? strip.x()[idx] : 0;
            if (idx < 0 && strip.quickCount() > 0) {
                idx = strip.quickEntryAt(mouseX);
                atX = idx >= 0 ? strip.quickX() + strip.quickIndexOf(idx) * TaskStrip.QL_W : 0;
            }
            if (idx >= 0) {
                desktop.taskbar().click(strip.entries().get(idx), atX, button, tbY);
                return true;
            }
            if (button == 1) {
                panelMenu.open((int) mouseX, tbY);
            }
            return true;
        }
        return false;
    }

    /** A click on a live balloon: it takes the click, and opens the program it offers when it offers one. */
    private boolean clickedBalloon(final double mx, final double my, final int tbY, final int sw) {
        final String opens = desktop.notices().clickBalloon(mx, my, tbY, sw);
        if (opens == null) {
            return false;
        }
        if (!opens.isEmpty()) {
            final IDesktopApp app = desktop.opener().factoryFor(opens);
            if (app != null) {
                desktop.wm().open(opens, app);
            }
        }
        return true;
    }

    /**
     * The open windows, front to back: a title-bar button, a resize edge, the bar itself, or the body. A window with
     * a dialog up takes nothing itself: a click on it brings the dialog forward, the way every desktop answers a
     * click on a program that is waiting for its own question.
     */
    private Click clickedWindow(final double absX, final double absY, final double mouseX, final double mouseY,
                                final int button) {
        final DesktopWindows wm = desktop.wm();
        for (int i = wm.all().size() - 1; i >= 0; i--) {
            final DesktopWindow w = wm.all().get(i);
            if (wm.away(w)) {
                continue;
            }
            final DesktopWindow held = wm.dialogOf(w);
            if (held != null && mouseX >= w.x() && mouseX <= w.x() + w.width()
                    && mouseY >= w.y() && mouseY <= w.y() + w.height()) {
                wm.focus(held);
                return Click.TAKEN;
            }
            final int titleButton = w.buttonAt(mouseX, mouseY);
            if (titleButton != 0) {
                // Pushed in now; it fires when let go over the same button, as a real button does.
                wm.bringToFront(i);
                w.setPressedButton(titleButton);
                pressedButton = w;
                return Click.TAKEN;
            }
            final int edge = w.resizeHitTest(mouseX, mouseY);
            if (edge != DesktopWindow.RESIZE_NONE) {
                wm.bringToFront(i);
                resizing = w;
                w.beginResize(edge, mouseX, mouseY);
                return Click.TAKEN;
            }
            if (w.titleBarHit(mouseX, mouseY)) {
                wm.bringToFront(i);
                dragging = w;
                dragOffsetX = (int) mouseX - w.x();
                dragOffsetY = (int) mouseY - w.y();
                return Click.TAKEN;
            }
            if (w.bodyHit(mouseX, mouseY)) {
                wm.bringToFront(i);
                return clickedWindowBody(w, absX, absY, mouseX, mouseY, button);
            }
        }
        return Click.PASSED;
    }

    /**
     * A click inside a window's body. Most of the time the program itself answers it, but a window carrying the
     * player's inventory lets the container drive instead, and the Network Interactor takes the clicks the container
     * would otherwise turn into a quick-move between slots.
     */
    private Click clickedWindowBody(final DesktopWindow w, final double absX, final double absY, final double mouseX,
                                    final double mouseY, final int button) {
        final InventoryBand band = desktop.band();
        // A shift-click on the inventory in the Workshop sends that whole stack to the station of the tab that is up.
        if (w.app() instanceof WorkshopApp workshop && Screen.hasShiftDown()) {
            final Slot slot = band.slotAt(absX, absY);
            if (slot != null && slot.hasItem()) {
                PacketDistributor.sendToServer(new WorkshopActionPayload(desktop.hostPos(), desktop.monitorPos(),
                        WorkshopActionPayload.SHIFT_INSERT, slot.getContainerSlot(), workshop.shiftInsertTab(), ""));
                return Click.TAKEN;
            }
        }
        // A click on a live inventory slot is a real container click: the container drives the cursor and the drag.
        if (w.app() instanceof IInventoryBandApp && !(w.app() instanceof NetworkInteractorApp)
                && !w.app().modalActive() && band.slotAt(absX, absY) != null) {
            return Click.CONTAINER;
        }
        if (w.app() instanceof NetworkInteractorApp ni) {
            final Click routed = clickedInteractor(ni, w, absX, absY, mouseX, mouseY, button);
            if (routed != Click.PASSED) {
                return routed;
            }
        }
        w.app().mouseClicked(w, mouseX, mouseY, button);
        return Click.TAKEN;
    }

    /**
     * The Network Interactor's own handling of a click on its window, which is where the desktop hands items between
     * the player and the network. It is here rather than in the program because the desktop, not the container, owns
     * the cursor while a window is open.
     */
    private Click clickedInteractor(final NetworkInteractorApp ni, final DesktopWindow w, final double absX,
                                    final double absY, final double mouseX, final double mouseY, final int button) {
        final InventoryBand band = desktop.band();
        /*
         * A shift-click on an inventory slot puts that whole stack into the network or the local storage, whichever
         * tab is up, instead of the container's quick-move between slots.
         */
        if (!ni.hasPopup() && Screen.hasShiftDown()) {
            final Slot slot = band.slotAt(absX, absY);
            final int target = ni.shiftInsertTarget();
            if (slot != null && slot.hasItem() && target >= 0) {
                PacketDistributor.sendToServer(new NiShiftInsertPayload(desktop.hostPos(), desktop.monitorPos(),
                        slot.getContainerSlot(), target));
                return Click.TAKEN;
            }
        }
        // While its request or storage dialog is open it is modal over the window, the inventory band included.
        if (!ni.hasPopup() && band.slotAt(absX, absY) != null) {
            return Click.CONTAINER;
        }
        /*
         * A held stack dropped on the item grid goes to the network or the local storage: the left button the whole
         * stack, the right one a single item or what a held container holds, and a held empty container right-clicked
         * on a fluid or chemical entry fills from it, so the entry under the cursor travels with a right click.
         */
        if (!ni.hasPopup() && !desktop.carried().isEmpty() && (button == 0 || button == 1)) {
            final double lx = mouseX - (w.x() + 4);
            final double ly = mouseY - (w.y() + 18);
            final int target = ni.cursorDepositTarget(lx, ly);
            if (target >= 0) {
                PacketDistributor.sendToServer(new NiDepositPayload(desktop.hostPos(), desktop.monitorPos(), target,
                        button == 0, button == 1 ? ni.cursorDepositEntry(lx, ly) : Optional.empty()));
                return Click.TAKEN;
            }
        }
        return Click.PASSED;
    }

    /**
     * The wallpaper and its icons, which is where a click lands when nothing above wanted it: the desktop's own menu,
     * an icon picked or opened, a drag armed, or a rubber band begun on bare wallpaper.
     */
    private Click clickedDesktop(final double mouseX, final double mouseY, final int button) {
        final DesktopViewport view = desktop.view();
        final DeskMenu deskMenu = desktop.deskMenu();
        // An open desktop menu takes the click first, and closes on it whatever it landed on.
        if (deskMenu.isOpen()) {
            deskMenu.mouseClicked(mouseX, mouseY, button);
            return Click.TAKEN;
        }
        // A click on the desktop keeps the name an icon was being given.
        if (desktop.fileActions().isRenaming()) {
            desktop.fileActions().commitRename();
        }
        if (desktop.panelStyle() == PanelStyle.CDE) {
            final CdeWindowIcons putAwayIcons = desktop.cdeWindowIcons();
            // A double click on the icon of a window that was put away brings that window back.
            if (button == 0 && putAwayIcons.clicked(mouseX, mouseY, desktop.wm().putAwayHere(), view.width(),
                    view.workAreaTop())) {
                return Click.TAKEN;
            }
            // The right button on such an icon raises the window's own menu, which is how it is closed from there.
            if (button == 1) {
                final DesktopWindow putAway = putAwayIcons.at(mouseX, mouseY, desktop.wm().putAwayHere(),
                        view.width(), view.workAreaTop());
                if (putAway != null) {
                    desktop.cdeWindowMenu().openFor(putAway, (int) mouseX, (int) mouseY);
                    return Click.TAKEN;
                }
            }
        }
        final DesktopIcons grid = desktop.iconGrid();
        final int slot = grid.slotAt(mouseX, mouseY, grid.perColumn());
        if (button == 1) {
            // The right button: the menu of whatever is under the cursor, or the wallpaper's own.
            grid.pick(slot);
            deskMenu.openFor(slot, (int) mouseX, (int) mouseY);
            return Click.TAKEN;
        }
        if (slot >= 0) {
            // One click picks an icon and a second one soon after opens it; either can start a drag.
            final long now = System.currentTimeMillis();
            final boolean twice = grid.picked() == slot && now - pickedAt < DOUBLE_CLICK_MS;
            grid.pick(slot);
            pickedAt = now;
            desktop.drags().armIcon(slot, mouseX, mouseY);
            if (twice) {
                desktop.openSlot(slot);
                grid.pick(-1);
            }
            return Click.TAKEN;
        }
        grid.pick(-1);
        grid.selection().clear();
        // A click on bare wallpaper with a stack held would have the container throw it into the world.
        if (!desktop.carried().isEmpty()) {
            return Click.TAKEN;
        }
        // Pressing on bare wallpaper starts a rubber band; a drag grows it from here.
        if (button == 0) {
            desktop.drags().startBand(mouseX, mouseY);
        }
        return Click.CONTAINER;
    }

    /** Lets go of a pressed title-bar button: over the same button it does what the button does. */
    private void releaseTitleButton(final double x, final double y) {
        final DesktopWindow pb = pressedButton;
        final int pressed = pb.pressedButton();
        pb.setPressedButton(0);
        pressedButton = null;
        if (pressed == DesktopWindow.BUTTON_NONE || pb.buttonAt(x, y) != pressed) {
            return;
        }
        if (pressed == DesktopWindow.BUTTON_CLOSE && desktop.panelStyle() == PanelStyle.CDE) {
            // Motif's button opens the window's menu, and closes the window on a double click.
            desktop.cdeWindowMenu().pressed(pb);
        } else if (pressed == DesktopWindow.BUTTON_CLOSE) {
            desktop.wm().close(pb);
        } else if (pressed == DesktopWindow.BUTTON_MINIMIZE) {
            pb.setMinimized(true);
        } else if (pressed == DesktopWindow.BUTTON_MAXIMIZE) {
            pb.toggleMaximize();
        }
    }

    /** Tiles a window let go against an edge of the screen: the top fills the work area, a side fills that half. */
    private void snap(final DesktopWindow w, final int x, final int y) {
        final DesktopViewport view = desktop.view();
        final int top = view.workAreaTop();
        final int workH = view.workAreaBottom() - top;
        final int halfW = view.width() / 2;
        if (y <= top + 4) {
            w.setMaximized(true);
        } else if (x <= 4) {
            w.snapTo(0, top, halfW, workH);
        } else if (x >= view.width() - 4) {
            w.snapTo(halfW, top, view.width() - halfW, workH);
        }
    }

    /**
     * What a layer did with an event: took it, left it for the next one (or, at the top, for nobody), or left it for
     * the container underneath.
     */
    enum Click {
        TAKEN,
        PASSED,
        CONTAINER
    }
}
