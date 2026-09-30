/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.TaskbarGroups;
import dev.jstech.computers.gui.layout.CdeExitLayout;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.gui.layout.CdeWindowIconLayout;
import dev.jstech.computers.gui.layout.VolumePopupLayout;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.core.client.gui.component.ContextMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What the client tests read of a desktop and where they click on it: the same hit areas the player clicks, found
 * by what they read as, in screen coordinates. The desktop screen answers these as its own public methods; they live
 * here, apart from how the desktop works, so reading the screen is not wading through them.
 */
interface DesktopInspection {

    /** Whether the launcher the panel opens is open. */
    default boolean isStartOpen() {
        return desktop().start().isOpen();
    }

    /** Screen position of the desktop's top-left corner: window and app geometry is relative to it. */
    default int desktopX() {
        return desktop().view().left();
    }

    default int desktopY() {
        return desktop().view().top();
    }

    /** The scale the desktop is drawn at, as a factor, which a test needs to land a click on a scaled desktop. */
    default double desktopScale() {
        return desktop().view().scale();
    }

    /** Whether the panel's own menu is open. */
    default boolean isPanelMenuOpen() {
        return desktop().panelMenu().isOpen();
    }

    /** Whether a program's menu, the one its panel entry opens, is open. */
    default boolean isTaskMenuOpen() {
        return desktop().taskbar().menu().isOpen();
    }

    /**
     * Screen position of the centre of the {@code index}-th panel entry, wherever this panel keeps its entries:
     * centred on Frames 11, from the left on every other panel.
     */
    @Nullable
    default int[] taskButtonPoint(final int index) {
        final TaskStrip strip = desktop().taskbar().strip(desktop().view().width());
        return index >= 0 && index < strip.entries().size() ? taskEntryPoint(strip.entries().get(index).key()) : null;
    }

    /**
     * Screen position of the centre of the panel entry of the program {@code key}, or null when the panel has none
     * for it. On Frames XP a pinned program with no window is its quick launch icon.
     */
    @Nullable
    default int[] taskEntryPoint(final String key) {
        final DesktopScreen desktop = desktop();
        final DesktopViewport view = desktop.view();
        final TaskStrip strip = desktop.taskbar().strip(view.width());
        final int index = TaskbarGroups.indexOf(strip.entries(), desktop.keyFor(key));
        if (index < 0) {
            return null;
        }
        final int half = DesktopScreen.TASKBAR_H / 2;
        final int y = view.screenY(view.panelOnTop() ? half : view.height() - half);
        if (strip.w()[index] > 0) {
            return new int[] {view.screenX(strip.x()[index] + strip.w()[index] / 2), y};
        }
        final int quick = strip.quickIndexOf(index);
        return quick < 0 ? null
                : new int[] {view.screenX(strip.quickX() + quick * TaskStrip.QL_W + TaskStrip.QL_W / 2), y};
    }

    /** The programs the panel lists, in order, by what they read as: the pinned ones first, then every open one. */
    default List<String> taskEntryLabels() {
        final List<String> out = new ArrayList<>();
        for (final TaskbarGroups.Entry entry : desktop().taskbar().entries()) {
            out.add(desktop().nameOf(entry.key()));
        }
        return out;
    }

    /** The programs pinned to the panel, by the label the panel shows them under. */
    default List<String> pinnedLabels() {
        final List<String> out = new ArrayList<>();
        for (final String key : desktop().taskbar().pinnedKeys()) {
            out.add(desktop().nameOf(key));
        }
        return out;
    }

    /** Whether the panel's popup (the windows of one program) is up. */
    default boolean isTaskPopupOpen() {
        return desktop().taskPopup().isOpen();
    }

    /** The titles the panel's popup lists, in order, empty when it is not up. */
    default List<String> taskPopupTitles() {
        return desktop().taskPopup().titles();
    }

    /** The desktop-local centre of the popup's {@code index}-th card or row, where a test clicks it. */
    default int[] taskPopupItemPoint(final int index) {
        return desktop().taskPopup().itemPoint(index);
    }

    /** The desktop-local centre of the popup's close box for its {@code index}-th window. */
    default int[] taskPopupClosePoint(final int index) {
        return desktop().taskPopup().closePoint(index);
    }

    /** The labels of the open program menu, in order, empty when none is up. */
    default List<String> taskMenuLabels() {
        final List<String> out = new ArrayList<>();
        final ContextMenu menu = desktop().taskbar().menu();
        if (menu.isOpen()) {
            for (final ContextMenu.Item item : menu.items()) {
                out.add(item.label());
            }
        }
        return out;
    }

    /** The desktop-local centre of the program menu's entry {@code label}, or null when it is not there. */
    @Nullable
    default int[] taskMenuPoint(final String label) {
        final int index = taskMenuLabels().indexOf(label);
        return index < 0 ? null : desktop().taskbar().menu().itemCenter(index);
    }

    /** The titles of the dialog windows up on the desktop, front-most last. */
    default List<String> dialogTitles() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : desktop().wm().all()) {
            if (w.dialog()) {
                out.add(w.app().title());
            }
        }
        return out;
    }

    /** The dialog window up over a window of the program {@code label}, or null. */
    @Nullable
    default DesktopWindow dialogWindowFor(final String label) {
        final String key = desktop().keyFor(label);
        final List<DesktopWindow> windows = desktop().wm().all();
        for (int i = windows.size() - 1; i >= 0; i--) {
            final DesktopWindow w = windows.get(i);
            if (w.dialog() && w.groupKey().equals(key)) {
                return w;
            }
        }
        return null;
    }

    /** The question or note up over the desktop, for a test to answer; null while none is. */
    @Nullable
    default QuestionPopup question() {
        return desktop().notices().question();
    }

    /** Screen position of the centre of the panel menu's {@code label} entry, or null when it is not there. */
    @Nullable
    default int[] panelMenuPoint(final String label) {
        final int[] local = desktop().panelMenu().pointOf(label);
        return local == null ? null : screen(local);
    }

    /** Screen position of the middle of one of the Front Panel's controls, under the arrow at its head. */
    default int[] frontPanelPoint(final CdeFrontPanelLayout.Control control) {
        final DesktopViewport view = desktop().view();
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.control(control, view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2),
                view.screenY(r.y() + CdeFrontPanelLayout.ARROW_H + (r.h() - CdeFrontPanelLayout.ARROW_H) / 2)};
    }

    /** Screen position of the middle of the Front Panel's button for workspace {@code index}, from nought. */
    default int[] workspacePoint(final int index) {
        final DesktopViewport view = desktop().view();
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.workspace(index, view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2), view.screenY(r.y() + r.h() / 2)};
    }

    /** Screen position of a title-bar button (1 minimise, 2 maximise, 3 the way out) of the window so labelled. */
    default int[] windowButtonPoint(final String label, final int button) {
        final String key = desktop().keyFor(label);
        for (final DesktopWindow w : desktop().wm().all()) {
            if (w.appKey().equals(key)) {
                return screen(w.buttonCentre(button));
            }
        }
        return new int[] {0, 0};
    }

    /** Screen position of the arrow at the head of a Front Panel control, which raises what is behind it. */
    default int[] frontPanelArrowPoint(final CdeFrontPanelLayout.Control control) {
        final DesktopViewport view = desktop().view();
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.control(control, view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2), view.screenY(r.y() + CdeFrontPanelLayout.ARROW_H / 2 + 1)};
    }

    /** The name the Front Panel is showing over the control the pointer rests on, or empty while it shows none. */
    default String frontPanelTip() {
        return desktop().cdePanels().shownTip();
    }

    /** What the subpanel standing on the Front Panel lists, top to bottom, or nothing when none is up. */
    default List<String> subpanelLabels() {
        return desktop().cdeLaunchers().labels();
    }

    /** Screen position of the line so labelled on the subpanel that is up, or null. */
    @Nullable
    default int[] subpanelPoint(final String label) {
        final DesktopViewport view = desktop().view();
        final int[] at = desktop().cdeLaunchers().rowCentre(label, view.width(), view.height());
        return at == null ? null : screen(at);
    }

    /** The names under the icons of the Application Manager window so titled, or nothing when it is not up. */
    default List<String> applicationManagerNames(final String windowTitle) {
        final DesktopWindow w = desktop().windowFor(windowTitle);
        return w != null && w.app() instanceof ApplicationManagerApp app ? app.names() : List.of();
    }

    /** Screen position of the icon so named in the Application Manager window so titled, or null. */
    @Nullable
    default int[] applicationManagerPoint(final String windowTitle, final String name) {
        final DesktopWindow w = desktop().windowFor(windowTitle);
        final int[] at = w != null && w.app() instanceof ApplicationManagerApp app ? app.iconCentre(name) : null;
        return at == null ? null : screen(at);
    }

    /** What the Workstation Info window shows, as {@code label=value}, or nothing while it is not up. */
    default List<String> workstationInfoFacts() {
        final DesktopWindow w = desktop().windowFor(
                WindowKeys.of(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "workstation_info")));
        return w != null && w.app() instanceof WorkstationInfoApp app ? app.shownFacts() : List.of();
    }

    /** CDE's look as the desktop is wearing it, as the machine would keep it. */
    default String wornCdeStyle() {
        return desktop().prefs().cdeStyle().encoded();
    }

    /** Screen position of a page on the Style Manager's strip, or null while the Style Manager is not up. */
    @Nullable
    default int[] styleManagerPagePoint(final String page) {
        final StyleManagerApp manager = styleManager();
        final int[] at = manager == null ? null : manager.pageCentre(page);
        return at == null ? null : screen(at);
    }

    /** Screen position of a palette on the Color page, or of a pattern on the Backdrop page, whichever is up. */
    @Nullable
    default int[] stylePageRowPoint(final String name) {
        final StyleManagerApp manager = styleManager();
        if (manager == null) {
            return null;
        }
        int[] at = manager.colorPage() == null ? null : manager.colorPage().rowCentre(name);
        if (at == null && manager.backdropPage() != null) {
            at = manager.backdropPage().rowCentre(name);
        }
        return at == null ? null : screen(at);
    }

    /** Screen position of a button of a Style Manager page: OK or Cancel on Color, Apply or Close on Backdrop. */
    @Nullable
    default int[] stylePageButtonPoint(final boolean color, final int button) {
        final StyleManagerApp manager = styleManager();
        if (manager == null) {
            return null;
        }
        final int[] at = color
                ? manager.colorPage() == null ? null : manager.colorPage().buttonCentre(button)
                : manager.backdropPage() == null ? null : manager.backdropPage().buttonCentre(button);
        return at == null ? null : screen(at);
    }

    /**
     * Screen position of a part of the Style Manager's Audio page: {@code mute}, {@code monitor}, {@code speakers},
     * {@code ok}, {@code cancel}, or {@code scale} at the volume {@code value}; null while the page is not up.
     */
    @Nullable
    default int[] styleAudioPoint(final String part, final int value) {
        final StyleManagerApp manager = styleManager();
        final CdeAudioPage page = manager == null ? null : manager.audioPage();
        final int[] at = page == null ? null : page.partCentre(part, value);
        return at == null ? null : screen(at);
    }

    /** Screen position of the middle of EXIT on the Front Panel. */
    default int[] exitPoint() {
        final DesktopViewport view = desktop().view();
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.exit(view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2), view.screenY(r.y() + r.h() / 2)};
    }

    /** Whether the dialog that shuts the machine down or restarts it is up. */
    default boolean powerDialogOpen() {
        return desktop().power().isOpen();
    }

    /** Screen position of a button of CDE's Exit dialog, by the numbers {@link CdeExitLayout} gives them. */
    default int[] exitDialogPoint(final int button) {
        final DesktopViewport view = desktop().view();
        final CdeFrontPanelLayout.Rect r = CdeExitLayout.button(button, view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2), view.screenY(r.y() + r.h() / 2)};
    }

    /** What the window menu CDE has up lists, top to bottom, or nothing when none is up. */
    default List<String> windowMenuLabels() {
        return desktop().cdeWindowMenu().labels();
    }

    /** Screen position of the entry so labelled on the window menu that is up, or null. */
    @Nullable
    default int[] windowMenuPoint(final String label) {
        final int[] at = desktop().cdeWindowMenu().entryCentre(label);
        return at == null ? null : screen(at);
    }

    /** Screen position of the box of workspace {@code index} on the Occupy Workspace dialog that is up, or null. */
    @Nullable
    default int[] occupyBoxPoint(final int index) {
        final OccupyWorkspaceDialog dialog = occupyDialog();
        return dialog == null ? null : screen(dialog.boxCentre(index));
    }

    /** Screen position of OK on the Occupy Workspace dialog that is up, or null. */
    @Nullable
    default int[] occupyOkPoint() {
        final OccupyWorkspaceDialog dialog = occupyDialog();
        return dialog == null ? null : screen(dialog.okCentre());
    }

    /** The workspaces the window so labelled is on, counted from nought. */
    default List<Integer> workspacesOf(final String label) {
        final List<Integer> out = new ArrayList<>();
        final String key = desktop().keyFor(label);
        for (final DesktopWindow w : desktop().wm().all()) {
            if (!w.dialog() && w.appKey().equals(key)) {
                for (int i = 0; i < WorkspaceSet.COUNT; i++) {
                    if (w.on(i)) {
                        out.add(i);
                    }
                }
                break;
            }
        }
        return out;
    }

    /** Screen position of the icon CDE stands the {@code index}-th put-away window of this workspace as. */
    default int[] putAwayIconPoint(final int index) {
        final DesktopViewport view = desktop().view();
        final CdeFrontPanelLayout.Rect tile = CdeWindowIconLayout.tile(index, view.width(), view.workAreaTop());
        return new int[] {view.screenX(tile.x() + tile.w() / 2), view.screenY(tile.y() + tile.h() / 2)};
    }

    /** Which workspace is up, counted from nought. */
    default int shownWorkspace() {
        return desktop().wm().workspace();
    }

    /** The labels of the program windows that are on show: open, not put away, on the workspace that is up. */
    default List<String> shownWindowLabels() {
        final DesktopWindows wm = desktop().wm();
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : wm.all()) {
            if (!w.dialog() && !wm.away(w)) {
                out.add(desktop().nameOf(w.appKey()));
            }
        }
        return out;
    }

    /** What the title bars of the windows on show say. */
    default List<String> shownWindowTitles() {
        final DesktopWindows wm = desktop().wm();
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : wm.all()) {
            if (!wm.away(w)) {
                out.add(desktop().titleOf(w));
            }
        }
        return out;
    }

    /** The labels of the program windows this desktop has open (dialogs aside), back to front. */
    default List<String> openWindowLabels() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : desktop().wm().all()) {
            if (!w.dialog()) {
                out.add(desktop().nameOf(w.appKey()));
            }
        }
        return out;
    }

    /** The first window one of the machine's Σ# programs has open on this desktop, or null. */
    @Nullable
    default SigmaWindowApp programWindow() {
        for (final DesktopWindow open : desktop().wm().all()) {
            if (open.app() instanceof SigmaWindowApp app) {
                return app;
            }
        }
        return null;
    }

    /** Screen position of a point on the panel clear of Start and of the task buttons: its empty stretch. */
    default int[] emptyPanelPoint() {
        final DesktopViewport view = desktop().view();
        return new int[] {view.screenX(desktop().taskbar().emptyX(view.width())),
                view.screenY(view.height() - DesktopScreen.TASKBAR_H / 2)};
    }

    /** Where the speaker on the panel is, on the screen. */
    default int[] speakerPoint() {
        final DesktopViewport view = desktop().view();
        final boolean top = view.panelOnTop();
        final int panelY = top ? 0 : view.height() - view.panelBand();
        return new int[] {view.screenX(desktop().tray().speakerX(view.width(), top) + 4),
                view.screenY(panelY + DesktopScreen.TASKBAR_H / 2)};
    }

    /**
     * Where a part of the open volume control is, on the screen: {@code track} at the volume {@code index},
     * {@code mute}, {@code chevron}, {@code output} number {@code index}, or {@code footer}; null when it has none.
     */
    @Nullable
    default int[] volumePoint(final String part, final int index) {
        final int[] p = desktop().volume().pointOf(part, index);
        return p == null ? null : screen(p);
    }

    default boolean volumeControlOpen() {
        return desktop().volume().controlOpen();
    }

    default boolean volumeMenuOpen() {
        return desktop().volume().menuOpen();
    }

    /** The volume the panel shows, and whether it shows it muted. */
    default int volumeShown() {
        return desktop().volume().volume();
    }

    default boolean mutedShown() {
        return desktop().volume().muted();
    }

    /** The volume control this desktop opens, by the name of its look, or empty on a desktop with none. */
    default String volumeLook() {
        final VolumePopupLayout.Look look = desktop().volume().look();
        return look == null ? "" : look.name();
    }

    /** Screen coordinates of the Start button's centre. */
    default int startButtonX() {
        final boolean modern = desktop().panelStyle() == PanelStyle.FRAMES_11;
        return desktop().view().screenX(modern ? 4 + DesktopScreen.WIN11_SLOT / 2 : 30);
    }

    default int startButtonY() {
        // GNOME's Activities corner lives in the top bar; every other panel sits at the bottom.
        final DesktopViewport view = desktop().view();
        final int half = DesktopScreen.TASKBAR_H / 2;
        return view.screenY(view.panelOnTop() ? half : view.height() - half);
    }

    /** Screen x of the middle of the launcher's entries, while it is open. */
    default int startMenuItemX() {
        return desktop().view().screenX(desktop().start().itemX(-1));
    }

    /**
     * Screen x of the centre of the {@code index}-th launcher entry. Frames XP lays its entries in two columns
     * (programs left, places right), so the column depends on the entry.
     */
    default int startMenuItemX(final int index) {
        return desktop().view().screenX(desktop().start().itemX(index));
    }

    default int startMenuItemY(final int index) {
        return desktop().view().screenY(desktop().start().itemY(index));
    }

    /** Whether the desktop's right-click menu is up. */
    default boolean deskMenuOpen() {
        return desktop().deskMenu().isOpen();
    }

    /** The desktop-local centre of the desk menu's item {@code index}, where a test clicks it. */
    default int[] deskMenuItemCenter(final int index) {
        return desktop().deskMenu().itemCenter(index);
    }

    /** The labels of the desk menu's items, in order, so a test finds one by name. */
    default List<String> deskMenuLabels() {
        return desktop().deskMenu().labels();
    }

    /** The desktop-local centre of item {@code index} of the menu open beside the desk menu, or null when none is. */
    @Nullable
    default int[] deskSubmenuItemCenter(final int index) {
        return desktop().deskMenu().submenuItemCenter(index);
    }

    /** The names of the files and folders on the desktop, as their icons read. */
    default List<String> desktopItemNames() {
        final List<String> out = new ArrayList<>();
        for (final DiskFilesPayload.WireFile file : desktop().deskFiles()) {
            out.add(FsPaths.fileName(file.path()));
        }
        return out;
    }

    /** The names under the wallpaper's icons ahead of the files, in their order, the trash first where it stands. */
    default List<String> deskIconLabels() {
        final List<String> out = new ArrayList<>();
        for (final Launcher l : desktop().deskIcons()) {
            out.add(l.label());
        }
        return out;
    }

    /** The desktop-local middle of the wallpaper icon so named, a program's, the trash's or a file's; or null. */
    @Nullable
    default int[] deskIconPoint(final String name) {
        final List<Launcher> icons = desktop().deskIcons();
        final List<DiskFilesPayload.WireFile> files = desktop().deskFiles();
        for (int slot = 0; slot < icons.size() + files.size(); slot++) {
            final String label = slot < icons.size() ? icons.get(slot).label()
                    : FsPaths.fileName(files.get(slot - icons.size()).path());
            if (label.equals(name)) {
                return desktop().iconGrid().centreOf(slot);
            }
        }
        return null;
    }

    /** The trash window that is up, or null while none is. */
    @Nullable
    default TrashApp trashWindow() {
        final DesktopWindow open = desktop().windowFor(WindowKeys.TRASH);
        return open != null && open.app() instanceof TrashApp app ? app : null;
    }

    /** The desktop these questions are asked of, which is the screen that answers them. */
    private DesktopScreen desktop() {
        return (DesktopScreen) this;
    }

    /** A desktop-local point as the screen position a click is given in. */
    private int[] screen(final int[] local) {
        final DesktopViewport view = desktop().view();
        return new int[] {view.screenX(local[0]), view.screenY(local[1])};
    }

    @Nullable
    private StyleManagerApp styleManager() {
        for (final DesktopWindow w : desktop().wm().all()) {
            if (w.app() instanceof StyleManagerApp manager) {
                return manager;
            }
        }
        return null;
    }

    /** The Occupy Workspace dialog that is up, front-most first, or null. */
    @Nullable
    private OccupyWorkspaceDialog occupyDialog() {
        final List<DesktopWindow> windows = desktop().wm().all();
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).app() instanceof OccupyWorkspaceDialog dialog) {
                return dialog;
            }
        }
        return null;
    }
}
