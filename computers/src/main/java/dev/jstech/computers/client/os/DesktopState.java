/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.audio.SoundOutput;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.gui.CdeStyle;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.computers.operation.payload.RequestDesktopFilesPayload;
import dev.jstech.computers.operation.payload.RequestDiskFilesPayload;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.SetupProgressPayload;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * One machine's desktop, whole, apart from whatever shows it: its windows, its panel and launchers, its icons, its
 * dialogs, how it looks and what the machine last said about it. It draws itself onto a {@link DesktopSurface}, so a
 * desktop can be drawn with no screen open, which is what a monitor showing it in the world needs.
 *
 * <p>The parts of the desktop are built around this and ask it for one another, through the readings below, each
 * named for what the caller wants rather than for the field it happens to come from.
 */
final class DesktopState {

    private final DesktopSurface surface;
    private final BlockPos host;
    private final BlockPos monitorPos;
    private final ResourceLocation osId;
    /** The desktop environment drawn: the OS's bundled one (Frames) or the Linux package installed. */
    private final ResourceLocation desktopId;
    /** The desktop environment's descriptor (chrome family, bundled apps, native names); null if unknown. */
    @Nullable
    private final DesktopEnvironmentDef chrome;
    /** The chrome family drawn (panel placement, launcher menu, window behaviour). */
    private final PanelStyle panel;
    /** The on-disk desktop folder (Users/Public/Desktop on the DOS family, home/player/Desktop on POSIX). */
    private final String desktopDir;
    private final DesktopTheme theme;
    /** How the owner chose this desktop should look, and the skin that dresses it. */
    private final DesktopPrefs prefs;
    /** The window manager: the windows, back to front, and the workspace that is up. */
    private final DesktopWindows wm = new DesktopWindows(this);
    /** The window manager's windows, back to front, which most of what the desktop does walks through. */
    private final List<DesktopWindow> windows = wm.all();
    /** The windows the machine has open, as it remembers them, and the programs' insides kept in this client. */
    private final WindowLayouts layouts;
    /** How the desktop opens things: programs, files, and the windows the machine's own programs have. */
    private final ProgramOpener opener;
    /** Where the desktop sits on the surface and how big it draws. */
    private final DesktopViewport view = new DesktopViewport(this);
    /** What this desktop can start, the programs installed on the machine, and the wallpaper's icons. */
    private final DesktopLaunchers catalogue;
    /** What the desktop tells the player: a dialog over everything, or a balloon over the notification area. */
    private final DesktopNotices notices = new DesktopNotices(this);
    /** The dialog that shuts the machine down, restarts it or logs off. */
    private final PowerDialog power = new PowerDialog(this);
    /** The launcher the panel opens: a Start menu, Kickoff, the Mint menu or the Activities overview. */
    private final StartMenus start = new StartMenus(this);
    /** The programs on the panel, their pins, where their buttons sit, and a program's own menu. */
    private final TaskbarModel taskbar = new TaskbarModel(this);
    /** The corner every panel reports the machine in: the network, the sound, the memory and the clock. */
    private final PanelTray tray = new PanelTray(this);
    /** The volume control the speaker in that corner opens, in the form this desktop gives it. */
    private final VolumePopup volumePopup = new VolumePopup(this);
    /** The bars the Linux desktops put their open windows on, and the period panel drawn out of relief. */
    private final LinuxPanels linuxPanels = new LinuxPanels(this);
    /** The Frames systems' two: the classic bottom taskbar, and Frames 11's centered band of icons. */
    private final FramesPanels framesPanels = new FramesPanels(this);
    /** CDE's Front Panel, which stands where the others have a bar, and the subpanel that rises out of it. */
    private final CdePanels cdePanels = new CdePanels(this);
    private final CdeLaunchers cdeLaunchers = new CdeLaunchers(this);
    /** The icons CDE stands put-away windows as on their workspace, which is all the task list it ever had. */
    private final CdeWindowIcons cdeWindowIcons = new CdeWindowIcons(this);
    /** The menu behind the button at the left of a Motif title bar: everything CDE lets be done to a window. */
    private final CdeWindowMenu cdeWindowMenu = new CdeWindowMenu(this);
    /** The flyout that lists one program's windows over its button on the panel. */
    private final TaskPopup taskPopup = new TaskPopup(this);
    /** The icons on the wallpaper: where each one sits, what it looks like, and which ones are picked. */
    private final DesktopIcons iconGrid = new DesktopIcons(this);
    /** Making, renaming and deleting the things that live on the desktop. */
    private final DeskFiles deskFiles = new DeskFiles(this);
    /** The trash: its icon, full or empty, and what deleting a thing on this desktop means. */
    private final DeskTrash trash = new DeskTrash(this);
    /** The machine's memory as the desktop weighs it, and the crash of a cooperative kernel run out of it. */
    private final DesktopMemory memory;
    /** The wallpaper's right-click menu, for whatever the cursor is on. */
    private final DeskMenu deskMenu = new DeskMenu(this);
    /** The panel's own menu, which a right click on the bar clear of its entries opens. */
    private final PanelMenu panelMenu = new PanelMenu(this);
    /** The player's inventory, laid over the window in front when its program has an inventory zone. */
    private final InventoryBand band = new InventoryBand(this);
    /** An icon or a file being dragged across the wallpaper, and the rubber band swept over it. */
    private final DesktopDrags drags = new DesktopDrags(this);
    /** Paints the desktop, back to front, each layer at a depth of its own. */
    private final DesktopPainter painter = new DesktopPainter(this);
    /** Files and folders living in the desktop folder ({@link SystemLayout#DESKTOP_DIR}), drawn as icons. */
    private final List<DiskFilesPayload.WireFile> desktopItems = new ArrayList<>();
    /** The media in the machine's drives, as its last listing said. */
    private final List<DiskFilesPayload.WireVolume> media = new ArrayList<>();
    /** The computer's name, synced from the server. */
    private String computerName = "";
    /** The pointer, desktop-local, as the frame being drawn has it, so a menu drawn late in it lights its row. */
    private int hoverX;
    private int hoverY;

    DesktopState(final DesktopSurface surface, final BlockPos host, final BlockPos monitorPos,
                 final ResourceLocation osId, final ResourceLocation desktopId, final int ramTotalMb,
                 final int ramReservedMb) {
        this.surface = surface;
        this.host = host;
        this.monitorPos = monitorPos;
        this.osId = osId;
        this.desktopId = desktopId;
        this.memory = new DesktopMemory(this, osId, windows, ramTotalMb, ramReservedMb);
        this.layouts = new WindowLayouts(this, host);
        this.opener = new ProgramOpener(this, host, monitorPos, desktopId);
        this.chrome = OsRegistry.getDesktop(desktopId);
        this.catalogue = new DesktopLaunchers(this, host, monitorPos, desktopId, osId, chrome);
        // A desktop nobody registered is drawn as the first Frames edition, as its look is.
        this.panel = chrome != null ? chrome.panelStyle() : PanelStyle.FRAMES_95;
        final OsDef os = OsRegistry.getOs(osId);
        this.desktopDir = SystemLayout.desktopDirFor(os, os == null ? null : OsRegistry.getKernel(os.kernelId()));
        this.theme = DesktopTheme.forDesktop(desktopId);
        this.prefs = new DesktopPrefs(this, desktopId);
    }

    /**
     * Readies the desktop for its first frame and asks the machine for what it shows. The era's skin is resolved
     * first: the panel's placement follows the skin (a period desktop panels at the bottom), so waiting for the
     * machine's reply would draw one frame with the panel on the wrong edge and then jump.
     */
    void prepare() {
        // The speaker on the panel says whether the system is muted from the first frame it is drawn in.
        volumePopup.requestState();
        prefs.rebuildSkin();
        catalogue.build();
        /*
         * The windows that were open when this machine's monitor was last left are the machine's: they arrive from
         * the server with the desktop listing asked for here, and the layouts restore them.
         */
        requestDesktop();
    }

    /**
     * Draws one frame of the desktop with the pointer at a desktop-local point. Returns false when a cooperative
     * kernel's crash screen is all there is to draw, until the machine reboots.
     */
    boolean paint(final GuiGraphics g, final int lmx, final int lmy, final float partialTick) {
        layouts.pushIfChanged();
        /*
         * Keep the inventory slots glued to the window in front this frame (per frame, so a dragged window does not
         * leave its slots a tick behind).
         */
        band.sync();
        this.hoverX = lmx;
        this.hoverY = lmy;
        return painter.paint(g, lmx, lmy, partialTick);
    }

    /** The inventory slot under the pointer as the last frame found it, or null. */
    @Nullable
    Slot hoveredSlot() {
        return painter.hovered();
    }

    /**
     * The desktop is put away. Its explorers stop listening and the picture on its wall is let go, since another
     * machine's desktop may come up next and must not be shown this one's listings or drawing; they come back when
     * this desktop is restored. The layout goes back to the machine, and the programs' insides stay in this client.
     */
    void putAway() {
        FilesApps.forgetAll();
        TrashApp.forgetAll();
        ThisPcApp.forgetAll();
        PixWallpaper.clear();
        layouts.keep();
        // A desktop that is gone is told nothing more about what it asked the machine for.
        CodeFileReplies.forget(deskFiles);
    }

    /** What the desktop is drawn on. */
    DesktopSurface surface() {
        return surface;
    }

    /** The container that carries the player's inventory into a window, or null when the surface has none. */
    @Nullable
    DesktopMenu container() {
        return surface.container();
    }

    /** The stack on the player's cursor, or the empty stack when there is none or no container to carry it. */
    ItemStack carried() {
        final DesktopMenu menu = surface.container();
        return menu == null ? ItemStack.EMPTY : menu.getCarried();
    }

    /**
     * Whether this desktop wears its period chrome. Derived from the skin's form, which the era already decided, so
     * the panel and the windows can never disagree about which decade they are in.
     */
    boolean periodPanel() {
        return prefs.skin().form() == OsSkin.Form.KDE2 || prefs.skin().form() == OsSkin.Form.GNOME1;
    }

    /**
     * The icon set to draw programs with. A period desktop asks for its own artwork first and falls back to that
     * desktop's modern icons, so a Legacy KDE still looks like KDE even before period icons are drawn for it.
     */
    String icons() {
        return periodPanel() ? desktopId.getPath() + ProgramIcons.PERIOD_SUFFIX : desktopId.getPath();
    }

    Font textFont() {
        return Minecraft.getInstance().font;
    }

    DesktopTheme themeColours() {
        return theme;
    }

    /** How the owner chose this desktop should look, the skin that dresses it, its clock and its era. */
    DesktopPrefs prefs() {
        return prefs;
    }

    /** Everything this desktop can start, in the order its menus list it. */
    List<Launcher> launcherList() {
        return catalogue.all();
    }

    /**
     * What the wallpaper wears as icons, before the files of the desktop folder: the trash first of all, on every
     * desktop but CDE, and then the programs. The menus list the programs alone, as the desktops' menus did.
     */
    List<Launcher> deskIcons() {
        return catalogue.icons();
    }

    /** The style this desktop is drawn in, which decides the look and the words of what opens on it. */
    PanelStyle panelStyle() {
        return panel;
    }

    /** Whether anything is in the trash, which is the picture its icon and its Front Panel control wear. */
    boolean trashFull() {
        return trash.full();
    }

    /** Whether the icon in that slot of the wallpaper is the trash's. */
    boolean isTrashIcon(final int slot) {
        final List<Launcher> icons = catalogue.icons();
        return slot >= 0 && slot < icons.size() && trash.is(icons.get(slot));
    }

    /** Opens the trash, or brings its window forward. */
    void openTrash() {
        trash.open();
    }

    /** Where this desktop sits on the surface, how big it draws, and the work area its panel leaves. */
    DesktopViewport view() {
        return view;
    }

    /** The launcher the panel opens, and whether it is open. */
    StartMenus start() {
        return start;
    }

    /** The wallpaper's right-click menu, which a program listed in a launcher also answers the right button with. */
    DeskMenu deskMenu() {
        return deskMenu;
    }

    /** Whether the pointer is inside that rectangle, which is what decides a highlight. */
    boolean hoverIn(final int x, final int y, final int w, final int h) {
        return hoverX >= x && hoverX < x + w && hoverY >= y && hoverY < y + h;
    }

    /** Whether the pointer is below a line and between two columns, for a footer that has no fixed height. */
    boolean hoverBelowRight(final int top, final int left, final int right) {
        return hoverY >= top && hoverX >= left && hoverX < right;
    }

    /** Whether the pointer is past both edges, for a corner that runs to the end of the screen. */
    boolean hoverBeyond(final int minX, final int minY) {
        return hoverX >= minX && hoverY >= minY;
    }

    /**
     * Whether a light tone reads on a panel whose text is this colour.
     *
     * <p>A panel that writes in a pale colour is a dark band, so what sits on it has to be pale too. This is the
     * quick integer brightness rather than the proper contrast measure: it is deciding between two fixed palettes,
     * not checking whether text is readable.
     */
    boolean lightOn(final int textColor) {
        return luminance(textColor) > 140;
    }

    /** The window manager: the windows, back to front, and the workspace that is up. */
    DesktopWindows wm() {
        return wm;
    }

    /** Which of the desktop's workspaces is up, counted from nought. */
    int workspace() {
        return wm.workspace();
    }

    /**
     * Puts another workspace up. Only what belongs there is drawn and answers the pointer from then on; the rest
     * stay exactly as they were left, and the machine is told, so the next person to look finds the same workspace
     * up with the same windows on it.
     */
    void switchWorkspace(final int workspace) {
        if (!hasWorkspaces()) {
            return;
        }
        wm.setWorkspace(WorkspaceSet.clampIndex(workspace));
        cdeWindowMenu.close();
        start.close();
    }

    /** Whether this desktop has workspaces at all; one that does not keeps everything on the first. */
    boolean hasWorkspaces() {
        return is(PanelStyle.CDE);
    }

    /** The arrow at the head of a Front Panel control was pressed: its subpanel comes up, or goes back down. */
    void toggleSubpanel(final CdeFrontPanelLayout.Control control) {
        cdeLaunchers.toggle(control);
    }

    boolean subpanelOpen(final CdeFrontPanelLayout.Control control) {
        return cdeLaunchers.isOpen(control);
    }

    /** Where this system keeps what is on the desktop, and the home that folder stands in. */
    String desktopDirectory() {
        return desktopDir;
    }

    String homeDir() {
        final int slash = desktopDir.lastIndexOf('/');
        return slash <= 0 ? desktopDir : desktopDir.substring(0, slash);
    }

    /** Takes the removable media a listing names, which are the machine's whichever folder was listed. */
    void takeVolumes(final DiskFilesPayload payload) {
        media.clear();
        for (final DiskFilesPayload.WireVolume volume : payload.volumes()) {
            if (volume.removable()) {
                media.add(volume);
            }
        }
    }

    /** Asks the machine which media are in its drives, which a listing of its root says. */
    void askForMedia() {
        PacketDistributor.sendToServer(new RequestDiskFilesPayload(host, ""));
    }

    /** The media in the machine's drives as it last said, each opened in a file manager at its root. */
    List<DiskFilesPayload.WireVolume> media() {
        return media;
    }

    /** How the desktop opens things: programs, files, and the windows the machine's own programs have. */
    ProgramOpener opener() {
        return opener;
    }

    /** Whether the host computer is on a data network right now, as its block entity tells the client. */
    boolean onNetwork() {
        return DesktopScreen.hostNetworked(host);
    }

    /** The machine's memory as the desktop weighs it, for the meter and its tooltip. */
    DesktopMemory memory() {
        return memory;
    }

    /** What the desktop tells the player: its dialogs and its balloons. */
    DesktopNotices notices() {
        return notices;
    }

    /** The dialog that shuts the machine down, restarts it or logs off. */
    PowerDialog power() {
        return power;
    }

    /** The notification corner, which every panel draws at its right end. */
    PanelTray tray() {
        return tray;
    }

    /** The machine this desktop runs on. */
    BlockPos host() {
        return host;
    }

    /** Whether the system's sound is muted, which the speaker on the panel shows. */
    boolean soundMuted() {
        return volumePopup.muted();
    }

    /** What the panel's tip says on the speaker: the volume, or that the sound is muted. */
    String volumeTip() {
        return volumePopup.tip();
    }

    /** Where the system's sound goes, as the machine last said or the player last set. */
    SoundOutput soundOutput() {
        return volumePopup.output();
    }

    /** Sets the system's sound all at once, the way a dialog with an OK button does. */
    void applySound(final int volume, final boolean muted, final SoundOutput output) {
        volumePopup.apply(volume, muted, output);
    }

    /** Whether this desktop is drawn in that style, which decides what its panel and launcher look like. */
    boolean isPanel(final PanelStyle style) {
        return is(style);
    }

    /** The programs on the panel, their pins, where their buttons sit, and a program's own menu. */
    TaskbarModel taskbar() {
        return taskbar;
    }

    /** What this desktop can start, and the lookups the desktop makes of it. */
    DesktopLaunchers catalogue() {
        return catalogue;
    }

    /** The flyout that lists one program's windows over its button on the panel. */
    TaskPopup taskPopup() {
        return taskPopup;
    }

    /** The files and folders of the desktop folder, which are drawn as icons after the launchers. */
    List<DiskFilesPayload.WireFile> deskFiles() {
        return desktopItems;
    }

    /** Making, renaming and deleting the things that live on the desktop. */
    DeskFiles fileActions() {
        return deskFiles;
    }

    /** The trash as the desktop has it, whose icon has a menu of its own. */
    DeskTrash trash() {
        return trash;
    }

    /** The programs this machine has installed, by id path. */
    List<String> installedPrograms() {
        return catalogue.installed();
    }

    /** An icon or a file being dragged across the wallpaper, and the rubber band swept over it. */
    DesktopDrags drags() {
        return drags;
    }

    /** The player's inventory, laid over the window in front when its program has an inventory zone. */
    InventoryBand band() {
        return band;
    }

    /** The volume control the speaker on the panel opens. */
    VolumePopup volume() {
        return volumePopup;
    }

    /** The panel's own menu. */
    PanelMenu panelMenu() {
        return panelMenu;
    }

    /** CDE's Front Panel, the subpanels that rise out of it, a window's menu and the icons of windows put away. */
    CdePanels cdePanels() {
        return cdePanels;
    }

    CdeLaunchers cdeLaunchers() {
        return cdeLaunchers;
    }

    CdeWindowMenu cdeWindowMenu() {
        return cdeWindowMenu;
    }

    CdeWindowIcons cdeWindowIcons() {
        return cdeWindowIcons;
    }

    /** The monitor this desktop is shown on. */
    BlockPos monitorPos() {
        return monitorPos;
    }

    /** The desktop environment drawn: the system's own (Frames) or the Linux one installed. */
    ResourceLocation desktopId() {
        return desktopId;
    }

    /** The bars the Frames systems and the Linux desktops put their programs on. */
    FramesPanels framesPanels() {
        return framesPanels;
    }

    LinuxPanels linuxPanels() {
        return linuxPanels;
    }

    /** The icons on the wallpaper: where each one sits, what it looks like, and which ones are picked. */
    DesktopIcons iconGrid() {
        return iconGrid;
    }

    /** The desktop file being renamed in place and what has been typed so far, or -1 and empty. */
    int renamingIcon() {
        return deskFiles.renaming();
    }

    String renameText() {
        return deskFiles.typedName();
    }

    /** The computer this desktop belongs to, and the folder its icons come from. */
    BlockPos hostPos() {
        return host;
    }

    String deskDir() {
        return desktopDir;
    }

    /**
     * Whether what is kept on the desktop is laid out from the right edge. CDE did that, and here it also leaves the
     * top left to the icons of the windows that were put away.
     */
    boolean objectsStandRight() {
        return is(PanelStyle.CDE);
    }

    /**
     * Whether a launcher, a menu or a dialog is up. The popup gives way to all of them: it is the one thing on the
     * panel that opens by itself, so it must never sit over something the player asked for.
     */
    boolean menuOrDialogOpen() {
        return start.isOpen() || panelMenu.isOpen() || taskbar.menu().isOpen() || cdeWindowMenu.isOpen()
                || notices.popupUp() || power.isOpen() || memory.crashing();
    }

    /** The program whose windows the panel's popup is showing, or null while none is up. */
    @Nullable
    String openTaskPopup() {
        return taskPopup.key();
    }

    /** The name this machine shows for whoever is at it: the computer's name, or a generic label. */
    String accountLabel() {
        return computerName == null || computerName.isBlank() ? GameText.resolve(DesktopTexts.LOCAL_ACCOUNT)
                : shorten(computerName, 22);
    }

    String shorten(final String text, final int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "...";
    }

    /** A thin one-pixel rectangle outline, as the Frames 11 Start panel draws its boxes. */
    void drawOutline(final GuiGraphics g, final int x, final int y, final int w, final int h, final int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** The desktop's own name, which a launcher carries up its side band. */
    String deskName() {
        return chrome != null ? chrome.displayName() : desktopId.getPath();
    }

    /** The icon a window key or a launcher key is drawn with, for a panel entry or a row; the generic one if none. */
    ResourceLocation programIdFor(final String key) {
        final Launcher launcher = catalogue.byKey(key);
        if (launcher != null) {
            return launcher.programId();
        }
        if (WindowKeys.TRASH.equals(key)) {
            return trash.icon();
        }
        // A window whose program has no launcher (the Task Manager) still shows its own icon on the panel.
        final ProgramSpec spec = WindowKeys.program(key);
        if (spec != null) {
            return spec.id();
        }
        // A window no program answers to (a setup, the welcome, a player's own program) wears the one it asks for.
        for (final DesktopWindow w : windows) {
            if (w.appKey().equals(key) && w.app().iconId() != null) {
                return w.app().iconId();
            }
        }
        return ResourceLocation.fromNamespaceAndPath("jsc", "generic");
    }

    /** Leaves the desktop without touching the machine, which is what logging off is. */
    void leaveDesktop() {
        surface.leave();
    }

    /** A Linux desktop environment (bottom-panel KDE/Cinnamon or top-bar GNOME), as opposed to a Frames edition. */
    boolean linuxDesktop() {
        return is(PanelStyle.KDE) || is(PanelStyle.GNOME) || is(PanelStyle.CINNAMON);
    }

    /** Reboots after a crash: the session is lost (windows and their saved state), back to an empty desktop. */
    void reboot() {
        memory.recover();
        notices.dismissBalloon();
        windows.clear();
        layouts.forgetSession();
        start.close();
        notices.dismissPopup();
    }

    /**
     * A rectangle an app drew, as it lands on the screen.
     *
     * <p>The desktop is drawn at a scale of its own, so a rectangle in an app's coordinates is not where it appears:
     * it moves with the glass and shrinks with it. Anything outside the desktop that has to line up with something
     * inside it asks here, rather than adding the corner and forgetting the scale, which lands the shape low, right
     * and too big.
     */
    Rect2i onScreen(final int x, final int y, final int w, final int h) {
        return new Rect2i(view.screenX(x), view.screenY(y),
                (int) Math.round(w * view.scale()), (int) Math.round(h * view.scale()));
    }

    /** What a window is called on this desktop, on its title bar and wherever the panel lists it. */
    String titleOf(final DesktopWindow w) {
        return w.titleOn(chrome);
    }

    List<String> launcherLabels() {
        return catalogue.labels();
    }

    /** Whether {@code app} runs in the front (focused) window; what a recipe viewer's drop or transfer targets. */
    boolean isFront(final IDesktopApp app) {
        final DesktopWindow front = wm.front();
        return front != null && front.app() == app;
    }

    /**
     * Every window of a program (its dialogs aside), front-most last, since a program may be open more than once.
     * The program is named by its window key or by the label it reads as here.
     */
    List<DesktopWindow> windowsFor(final String label) {
        final List<DesktopWindow> out = new ArrayList<>();
        final String key = keyFor(label);
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && w.appKey().equals(key)) {
                out.add(w);
            }
        }
        return out;
    }

    /** The oldest window of a program, named by its window key or by the label it reads as here; or null. */
    @Nullable
    DesktopWindow windowFor(final String label) {
        final String key = keyFor(label);
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && w.appKey().equals(key)) {
                return w;
            }
        }
        return null;
    }

    /** Whether a window of that key is up on this desktop, its dialogs aside. */
    boolean windowOpen(final String key) {
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && w.appKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The window key a request or a test names: a key as it is, or else the label a launcher or an open window reads
     * as on this desktop. A player who types a program's name at a shell asks this way; so do the tests, which name
     * what they click by what it says.
     */
    String keyFor(final String asked) {
        if (catalogue.byKey(asked) != null) {
            return asked;
        }
        for (final DesktopWindow w : windows) {
            if (w.appKey().equals(asked)) {
                return asked;
            }
        }
        final Launcher labelled = catalogue.byLabel(asked);
        if (labelled != null) {
            return labelled.key();
        }
        for (final DesktopWindow w : windows) {
            if (nameOf(w.appKey()).equals(asked)) {
                return w.appKey();
            }
        }
        return asked;
    }

    /**
     * What a window key reads as on this desktop: the label of the launcher that opens it, else the name this desktop
     * gives its program, else what its window calls itself, else the key.
     */
    String nameOf(final String key) {
        final Launcher launcher = catalogue.byKey(key);
        if (launcher != null) {
            return launcher.label();
        }
        if (WindowKeys.TRASH.equals(key)) {
            return trash.title();
        }
        final ProgramSpec spec = WindowKeys.program(key);
        if (spec != null) {
            return catalogue.labelOf(spec);
        }
        for (final DesktopWindow w : windows) {
            if (w.appKey().equals(key)) {
                return w.app().title();
            }
        }
        return key;
    }

    /**
     * Outer bounds of the framed monitor window (the bezel plus its chin), in screen coordinates. Integrations that
     * place a side panel next to the desktop (the JEI ingredient list) read this so the panel sits beside the monitor
     * rather than over it.
     */
    MonitorFrameStyle.Geometry frameBounds() {
        // The frame wraps the glass as it is on the screen, whatever the desktop inside it is scaled to.
        return MonitorFrameStyle.forEra(prefs.era())
                .geometry(view.left(), view.top(), view.glassWidth(), view.glassHeight());
    }

    /** The platform the installed system stands on, Frames as the safe default for a machine with no system. */
    Platform platform() {
        final OsDef os = OsRegistry.getOs(osId);
        return os != null ? os.platform() : Platform.FRAMES;
    }

    /**
     * The id of the wallpaper actually hanging on the desktop: the player's own choice when they made one, else
     * whatever the desktop ships with on the platform it runs on (FreeBSD's own picture there, the usual one
     * everywhere else).
     */
    String wallpaperId() {
        return WallpaperPainter.styleFor(desktopId, platform(), prefs.wallpaper()).id();
    }

    /**
     * Hangs a picture on this desktop's wall. The choice goes to the machine like any other setting, so it is
     * remembered with the computer rather than with the client looking at it, and every screen of that machine shows
     * it.
     */
    void hangPicture(final String path) {
        final String choice = PixWallpaper.choiceFor(path);
        prefs.setWallpaper(choice);
        PacketDistributor.sendToServer(new SetSettingPayload(host, "wallpaper", choice));
    }

    /** Applies a change made in the Settings program at once, without waiting for the next desktop listing. */
    void applyLivePrefs(final int accent, final int brightness, final boolean clock12h, final String wallpaper,
                        final boolean taskbarCentered, final boolean darkMode, final int scale) {
        view.setScalePercent(scale);
        prefs.apply(accent, brightness, clock12h, wallpaper, taskbarCentered, darkMode);
    }

    /** Requests the desktop folder's files so they can be drawn as background icons. */
    void requestDesktop() {
        PacketDistributor.sendToServer(new RequestDesktopFilesPayload(host));
    }

    /**
     * Restores the windows the machine has open, once, when the server hands them over. Later refreshes of the
     * desktop listing send the same payload again and must not open everything a second time.
     */
    void takeWindows(final DesktopWindowsPayload payload) {
        layouts.apply(payload);
    }

    /**
     * A machine saying how the program it is setting up is going. The desktop opens its Setup window on the first
     * word and hands it every one after, so the window is a view of the machine's job: two players at two monitors
     * of one machine see the same bar, and a desktop opened halfway through picks it up where it is.
     */
    void takeSetup(final SetupProgressPayload payload) {
        final DesktopWindow open = windowFor(SetupApp.KEY);
        final SetupApp app;
        if (open != null && open.app() instanceof SetupApp existing) {
            app = existing;
        } else {
            app = new SetupApp(host, desktopId.getPath());
            wm.open(SetupApp.KEY, app);
        }
        app.accept(payload);
        if (payload.state() == SetupProgressPayload.STATE_DONE) {
            // The launcher appears, or goes, the moment the job is done.
            requestDesktop();
            FilesApps.refreshAll();
        }
    }

    /** Takes a listing of the desktop folder, and with it how the machine says the desktop should look. */
    void takeDesktop(final DesktopFilesPayload payload) {
        desktopItems.clear();
        desktopItems.addAll(payload.files());
        computerName = payload.computerName();
        view.setScalePercent(payload.prefs().scale());
        // The look the machine keeps goes in first, so the skin the choices rebuild is drawn from it.
        prefs.takeCdeStyle(CdeStyle.parse(payload.cdeStyle()));
        prefs.apply(payload.prefs().accent(), payload.prefs().brightness(), payload.prefs().clock12h(),
                payload.wallpaper(), payload.prefs().taskbarCentered(), payload.prefs().darkMode());
        taskbar.takePinned(payload.pinned());
        opener.takeDefaults(payload.defaultApps());
        iconGrid.pinnedCells().clear();
        for (final DesktopFilesPayload.WireIconCell cell : payload.iconCells()) {
            iconGrid.pinnedCells().put(cell.key(), cell.cell());
        }
        // A program installed or removed while the desktop is up gets or loses its launcher at once.
        final boolean programsChanged = catalogue.take(payload.programs(), payload.community());
        memory.takeSourceBuilt(payload.sourceBuilt());
        // The trash's picture changes with what is in it, and the icons are made again when it does.
        final boolean trashChanged = trash.setFull(payload.trashFull());
        if (programsChanged) {
            catalogue.build();
        } else if (trashChanged) {
            catalogue.rebuildIcons();
        }
        // Enter rename on a freshly created item once it appears in the listing.
        deskFiles.takePendingRename();
    }

    /** Opens an icon slot: a launcher starts its program; a desktop file/folder opens or navigates. */
    void openSlot(final int slot) {
        if (slot < catalogue.icons().size()) {
            final Launcher launcher = catalogue.icons().get(slot);
            if (trash.is(launcher)) {
                trash.open();
            } else {
                opener.run(launcher);
            }
            return;
        }
        final int di = slot - catalogue.icons().size();
        if (di < 0 || di >= desktopItems.size()) {
            return;
        }
        final DiskFilesPayload.WireFile f = desktopItems.get(di);
        if (f.directory()) {
            opener.openFolder(f.path());
            return;
        }
        opener.openFile(f.path());
    }

    /** Whether a desktop-local point is on the bottom panel's Start button. */
    boolean startButtonHit(final double mx, final double my, final int tbY) {
        return framesPanels.startButtonHit(mx, my, tbY);
    }

    /**
     * Tells the machine what the power dialog chose. It is going down, restarting or being left: the desktop closing
     * after this must not hand its windows back to a machine whose session has just ended.
     */
    void cyclePower(final int action) {
        layouts.powerCycling();
        PacketDistributor.sendToServer(new MachinePowerPayload(host, monitorPos, action));
    }

    private boolean is(final PanelStyle style) {
        return panel == style;
    }

    /** How bright an opaque colour reads, 0 to 255, for deciding what tone sits well on it. */
    private static int luminance(final int color) {
        return (((color >> 16) & 0xFF) * 30 + ((color >> 8) & 0xFF) * 59 + (color & 0xFF) * 11) / 100;
    }
}
