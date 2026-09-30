/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.SoundOutput;
import dev.jstech.computers.client.MachineKeyboard;
import dev.jstech.computers.client.MonitorFrame;
import dev.jstech.computers.gui.CdeStyle;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.gui.TaskbarGroups;
import dev.jstech.computers.gui.layout.CdeExitLayout;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.gui.layout.CdeWindowIconLayout;
import dev.jstech.computers.gui.layout.VolumePopupLayout;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.RequestDiskFilesPayload;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.computers.operation.payload.RequestDesktopFilesPayload;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.operation.payload.SetupProgressPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.gui.layout.DesktopZ;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * The FULL_DESKTOP shell: a windowed desktop environment that a graphical OS (Frames 95 / XP / 11)
 * boots into. It renders inside a centred window (the monitor's screen) over the dimmed game, never
 * full-screen. The visual chrome is chosen per OS id; this is the shared window-manager engine all
 * three desktop OSes drive (one engine, distinct chrome + program gating).
 *
 * <p>First iteration: wallpaper, launcher icons, a taskbar with a start button and clock, a start
 * menu, and stackable program windows with a draggable title bar and a close box. Program content is
 * delegated to {@link IDesktopApp} instances. Visual polish is tuned in-game.
 */
public final class DesktopScreen extends CoreContainerScreen<DesktopMenu>
        implements MachineKeyboard.ITakesKeysFirst {

    private final BlockPos host;
    private final BlockPos monitorPos;
    private final ResourceLocation osId;
    private final DesktopTheme theme;
    /** How the owner chose this desktop should look, and the skin that dresses it. */
    private final DesktopPrefs prefs;
    /** The window manager: the windows, back to front, and the workspace that is up. */
    private final DesktopWindows wm = new DesktopWindows(this);
    /** The window manager's windows, back to front, which most of what the screen does walks through. */
    private final List<DesktopWindow> windows = wm.all();
    /** The windows the machine has open, as it remembers them, and the programs' insides kept in this client. */
    private final WindowLayouts layouts;
    /** How the desktop opens things: programs, files, and the windows the machine's own programs have. */
    private final ProgramOpener opener;
    /** Where the desktop sits on the game's screen and how big it draws. */
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

    /** Lets a running app request another program be opened on the desktop, by its key or its name. */
    public static void requestOpen(final String key) {
        DesktopRequests.open(new OpenRequest.Program(key));
    }

    /** Takes a window a Σ# program has open on the machine being looked at. */
    public static void acceptWindow(final UiWindowPayload payload) {
        DesktopRequests.window(payload);
    }

    /** Lets a running app end another program's window, the way a task manager does. */
    public static void requestClose(final String key) {
        DesktopRequests.close(key);
    }

    /**
     * Opens {@code dialog} as a window of its own over the window running {@code owner}: it is listed
     * with the owner on the panel, sits in front of it, and holds it until it is put away. The way an
     * Open or Save window belongs to the program that asked. Nothing happens when no desktop is up or
     * the owner has no window on it.
     */
    public static void openDialogFor(final IDesktopApp owner, final IDesktopApp dialog) {
        if (active != null) {
            active.wm.openDialog(owner, dialog);
        }
    }

    /** Closes the window running {@code app}, when it is up, the way its own Close button does. */
    public static void closeWindowFor(final IDesktopApp app) {
        if (active != null) {
            active.wm.closeOf(app);
        }
    }

    /** Puts away the window running {@code dialog}, when it is up. */
    public static void closeDialog(final IDesktopApp dialog) {
        if (active != null) {
            active.wm.closeOf(dialog);
        }
    }

    /**
     * Starts one of this machine's programs by its id, the way a shortcut to it does.
     *
     * <p>For a program that offers another as a way out of itself: a welcome pointing at This PC, and whatever
     * comes to want the same. Nothing happens when this machine has no such program, which is the honest answer
     * on a computer where it was never installed.
     */
    public static void openProgramById(final String path) {
        if (active != null) {
            active.opener.startProgram(path);
        }
    }

    /**
     * Brings the window of that key forward, or opens it the way a session coming back would, for a program that has a
     * second window of its own: Soundfoundry's sharing window, say. The key is the id its window factory is
     * registered under ({@link ProgramClient#register}), so the window comes back with the session like any other.
     */
    public static void openOrFocus(final String key) {
        if (active != null) {
            active.opener.openOrFocus(key);
        }
    }

    /**
     * The id of the wallpaper actually hanging on the desktop right now: the player's own choice when they made
     * one, else whatever the desktop ships with on the platform it runs on (FreeBSD's own picture there, the
     * usual one everywhere else).
     */
    public static String currentWallpaperId() {
        if (active == null) {
            return "";
        }
        return WallpaperPainter.styleFor(active.desktopId, active.platform(), active.prefs.wallpaper()).id();
    }

    /** Whether a window of that key is up on the desktop in front of the player. */
    public static boolean windowOpen(final String key) {
        if (active == null) {
            return false;
        }
        for (final DesktopWindow w : active.windows) {
            if (!w.dialog() && w.appKey().equals(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The name this desktop gives that program, or empty when this machine has no such program.
     *
     * <p>The name is the desktop's, not the program's: the same prompt is called one thing on one edition and
     * something else on another, and a button that offers it should say what this machine calls it.
     */
    public static String programLabel(final String path) {
        final Launcher launcher = active == null ? null : active.catalogue.byPath(path);
        return launcher == null ? "" : launcher.label();
    }

    /** Whether the computer at {@code pos} is on a data network, as its block entity tells the client. */
    public static boolean hostNetworked(final BlockPos pos) {
        final Level level = Minecraft.getInstance().level;
        return level != null
                && level.getBlockEntity(pos) instanceof IOsHost computer
                && computer.networkAttached();
    }

    /**
     * Opens this desktop's terminal and has it run that program, which is what double-clicking one does.
     *
     * <p>A program of the console kind needs a terminal to print into, so it is given one; the window is
     * whatever this desktop calls its terminal, because that is the one the machine has.
     */
    public static void requestRunAtTerminal(final String path) {
        DesktopRequests.open(new OpenRequest.RunAtTerminal(path));
    }

    /** Lets a running app open the explorer already navigated to {@code dir} (a drive, a folder). */
    public static void requestOpenFiles(final String dir) {
        DesktopRequests.open(new OpenRequest.FilesAt(dir));
    }

    /**
     * Lets a running app open a file in whatever program opens that kind by default.
     *
     * <p>Which program that is has one answer for the whole desktop, so the explorer asks here rather
     * than deciding for itself and disagreeing with a double-click on the desktop.
     */
    public static void requestOpenFile(final String path) {
        DesktopRequests.open(new OpenRequest.OpenFile("", path));
    }

    /** Lets a running app open a file in a program the player picked. */
    public static void requestOpenFileWith(final String programId, final String path) {
        DesktopRequests.open(new OpenRequest.OpenFile(programId, path));
    }

    /** Lets a running app ask the player which program opens a file, as Choose another program does. */
    public static void requestChooseOpener(final String path) {
        DesktopRequests.open(new OpenRequest.ChooseOpener(path));
    }

    /** The Open with chooser the open desktop is showing, or null when none is up. */
    @Nullable
    public static OpenWithPopup openWithChooser() {
        return active != null ? active.notices.chooser() : null;
    }

    /** The ids of the programs the open desktop's machine has, for a window offering what can open a file. */
    public static List<String> installedProgramIds() {
        return active == null ? List.of() : List.copyOf(active.catalogue.installed());
    }

    /** What a program is called, for a menu that offers it by id. */
    public static String openerName(final String programId) {
        return ProgramOpener.openerName(programId);
    }

    /**
     * Forgets every per-machine client cache: the programs' insides kept for the machines of the world the
     * player is leaving, and any open request that never found a desktop. Called on logout, so nothing of
     * one world lingers into the next.
     */
    public static void forgetClientState() {
        WindowLayouts.forgetAll();
        DesktopRequests.forgetAll();
    }

    /** The launcher labels the active desktop can open (built-in apps plus installed programs). */
    public static List<String> openableLabels() {
        return active != null ? active.launcherLabels() : List.of();
    }

    /**
     * Applies an accent/brightness change to the live desktop immediately, so a change in the Settings
     * app shows on the chrome without waiting for the next desktop refresh.
     *
     * @param accent     the accent override ({@code 0} = skin default)
     * @param brightness the screen brightness 0..100
     */
    public static void applyLivePrefs(final int accent, final int brightness, final boolean clock12h,
                                      final String wallpaper, final boolean taskbarCentered,
                                      final boolean darkMode, final int scale) {
        if (active != null) {
            active.view.setScalePercent(scale);
            active.prefs.apply(accent, brightness, clock12h, wallpaper, taskbarCentered, darkMode);
        }
    }

    /**
     * Raises the modal {@code .dat}-locked error dialog on the active desktop. Called from desktop
     * apps (e.g. the Files explorer) that detect a refused {@code .dat} action and need to surface
     * it; a no-op when no desktop is showing.
     */
    public static void showDatLockedError() {
        if (active != null) {
            active.notices.datLocked();
        }
    }

    /**
     * Hangs a picture on this desktop's wall, which is what the paint program's last button does.
     *
     * <p>The choice goes to the machine like any other setting, so it is remembered with the computer
     * rather than with the client looking at it, and every screen of that machine shows it.
     */
    public static void setWallpaperToPicture(final String path) {
        if (active == null || path == null || path.isEmpty()) {
            return;
        }
        final String choice = PixWallpaper.choiceFor(path);
        active.prefs.setWallpaper(choice);
        PacketDistributor.sendToServer(new SetSettingPayload(active.host, "wallpaper", choice));
    }

    /**
     * Raises the error for a refused action on an installer's own files: they are generated from the
     * medium's stamp, so there is nothing to rename, copy off, delete or overwrite.
     */
    public static void showInstallerLockedError() {
        if (active != null) {
            active.notices.showError(words(DesktopTexts.ERROR), words(DesktopTexts.INSTALLER_LOCKED));
        }
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }

    /** Raises that balloon on whichever desktop is looking at that machine, if one is. */
    public static void raise(final BlockPos host, final String title, final String body, final String opens) {
        if (active != null && active.host.equals(host)) {
            active.notices.showBalloon(title, body, opens);
        }
    }

    /** Local cursor cached each frame, so menus drawn later in the frame can highlight the hovered entry. */
    private int hoverX;
    private int hoverY;
    /** The key of the Workstation Info window, which a test reads. */
    private static final String WORKSTATION_INFO_KEY =
            WindowKeys.of(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "workstation_info"));

    /** The currently shown desktop is the one that receives desktop-folder listing replies. */
    @Nullable
    private static DesktopScreen active;


    /** Files and folders living in the desktop folder ({@link SystemLayout#DESKTOP_DIR}), drawn as icons. */
    private final List<DiskFilesPayload.WireFile> desktopItems = new ArrayList<>();

    /** The computer's name, synced from the server. */
    private String computerName = "";
    /** The media in the machine's drives, as its last listing said. */
    private final List<DiskFilesPayload.WireVolume> media = new ArrayList<>();

    /** The wallpaper's right-click menu, for whatever the cursor is on. */
    private final DeskMenu deskMenu = new DeskMenu(this);
    /** The panel's own menu, which a right click on the bar clear of its entries opens. */
    private final PanelMenu panelMenu = new PanelMenu(this);
    /** The player's inventory, laid over the window in front when its program has an inventory zone. */
    private final InventoryBand band = new InventoryBand(this);
    /** An icon or a file being dragged across the wallpaper, and the rubber band swept over it. */
    private final DesktopDrags drags = new DesktopDrags(this);
    /** Where the pointer and the keyboard go, layer by layer. */
    private final DesktopInput input = new DesktopInput(this);

    static final int TASKBAR_H = 24;

    /*
     * The work area (icons, windows, drops) is the desktop minus its panel. Every panel style but GNOME puts
     * the panel at the bottom; GNOME's top bar reserves the top TASKBAR_H pixels instead, so icons start and
     * windows clamp/maximize below it rather than sliding under it.
     */

    /**
     * Whether this desktop wears its period chrome. Derived from the skin's form, which the era already
     * decided, so the panel and the windows can never disagree about which decade they are in.
     */
    boolean periodPanel() {
        return prefs.skin().form() == OsSkin.Form.KDE2 || prefs.skin().form() == OsSkin.Form.GNOME1;
    }

    /**
     * The icon set to draw program icons from. A period desktop asks for its own artwork first and falls
     * back to that desktop's modern icons, so a Legacy KDE still looks like KDE even before period icons
     * are drawn for it.
     */
    private String iconSet() {
        return periodPanel()
                ? desktopId.getPath() + ProgramIcons.PERIOD_SUFFIX
                : desktopId.getPath();
    }

    /*
     * What a launcher drawn beside this screen is allowed to ask it. Each one is a plain reading of
     * something the screen already knows, named for what the caller wants rather than for the field it
     * happens to come from.
     */

    /** The icon set to draw programs with, which is the desktop's own and its era's. */
    String icons() {
        return iconSet();
    }

    Font textFont() {
        return font;
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
    public boolean trashFull() {
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

    /**
     * Asks the player a question over the whole desktop, running {@code yes} only when the answer is Yes: what
     * deletes a thing for good asks this first.
     */
    static void ask(final String title, final String message, final Runnable yes) {
        if (active != null) {
            active.notices.ask(title, message, yes);
        }
    }

    /** Tells the player something over the whole desktop, in a note they close with OK. */
    static void tell(final String title, final String message) {
        if (active != null) {
            active.notices.ask(title, message, null);
        }
    }

    /** The question or note up over the desktop, for a test to answer; null while none is. */
    @Nullable
    public QuestionPopup question() {
        return notices.question();
    }

    /** Where this desktop sits on the game's screen, how big it draws, and the work area its panel leaves. */
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
     * <p>A panel that writes in a pale colour is a dark band, so what sits on it has to be pale too. This is
     * the quick integer brightness rather than the proper contrast measure: it is deciding between two fixed
     * palettes, not checking whether text is readable.
     */
    boolean lightOn(final int textColor) {
        return luminance(textColor) > 140;
    }

    /** How bright an opaque colour reads, 0 to 255, for deciding what tone sits well on it. */
    private static int luminance(final int color) {
        return (((color >> 16) & 0xFF) * 30 + ((color >> 8) & 0xFF) * 59 + (color & 0xFF) * 11) / 100;
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
     * Puts another workspace up. Only what belongs there is drawn and answers the pointer from then on; the
     * rest stay exactly as they were left, and the machine is told, so the next person to look finds the same
     * workspace up with the same windows on it.
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
    public static void acceptVolumes(final DiskFilesPayload payload) {
        if (active == null) {
            return;
        }
        active.media.clear();
        for (final DiskFilesPayload.WireVolume volume : payload.volumes()) {
            if (volume.removable()) {
                active.media.add(volume);
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
        return networkAttached();
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

    /** Routes the machine's settings to the open desktop, whose panel shows its sound. */
    public static void acceptSettings(final SettingsSnapshotPayload payload) {
        if (active != null && active.host.equals(payload.hostPos())) {
            active.volumePopup.accept(payload.sound());
        }
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

    /** Where the pointer and the keyboard go, and the icon a click has picked. */
    DesktopInput input() {
        return input;
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
     * Whether what is kept on the desktop is laid out from the right edge. CDE did that, and here it also
     * leaves the top left to the icons of the windows that were put away.
     */
    boolean objectsStandRight() {
        return is(PanelStyle.CDE);
    }

    /**
     * Whether a launcher, a menu or a dialog is up. The popup gives way to all of them: it is the one
     * thing on the panel that opens by itself, so it must never sit over something the player asked for.
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

    /** The name this machine shows for whoever is at it. */
    String accountLabel() {
        return hostAccountLabel();
    }

    String shorten(final String text, final int max) {
        return trim(text, max);
    }

    void drawOutline(final GuiGraphics g, final int x, final int y, final int w, final int h, final int color) {
        outline(g, x, y, w, h, color);
    }

    /** The desktop that is up, for a window that outlived the screen it was opened on; null while none is. */
    @Nullable
    static DesktopScreen current() {
        return active;
    }

    /** The desktop's own name, which a launcher carries up its side band. */
    String deskName() {
        return desktopName();
    }

    /** The icon a window key or a launcher key is drawn with, for a panel entry or a row. */
    ResourceLocation programIdFor(final String key) {
        return iconOf(key);
    }

    /** Leaves the desktop without touching the machine, which is what logging off is. */
    void leaveDesktop() {
        onClose();
    }

    // Frames 11 taskbar: each centered item (Start + one per program) occupies this slot.
    static final int WIN11_SLOT = 22;
    static final int WIN11_ICON = 16;
    /** The width of the Frames XP Start pill, which the task buttons and its own hit-test both clear. */
    static final int XP_START_W = 58;

    /** The desktop environment drawn: the OS's bundled one (Frames) or the Linux package installed. */
    private final ResourceLocation desktopId;
    /** The desktop environment's descriptor (chrome family, bundled apps, native names); null if unknown. */
    @Nullable
    private final DesktopEnvironmentDef chrome;
    /** The chrome family drawn (panel placement, launcher menu, window behaviour). */
    private final PanelStyle panel;
    /** The on-disk desktop folder (Users/Public/Desktop on the DOS family, home/player/Desktop on POSIX). */
    private final String desktopDir;

    public DesktopScreen(final DesktopMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.host = menu.hostPos();
        this.monitorPos = menu.monitorPos();
        this.osId = menu.osId();
        this.desktopId = menu.desktopId();
        this.memory = new DesktopMemory(this, osId, windows, menu.ramTotalMb(), menu.ramReservedMb());
        this.layouts = new WindowLayouts(this, host);
        this.opener = new ProgramOpener(this, host, monitorPos, desktopId);
        this.chrome = OsRegistry.getDesktop(desktopId);
        this.catalogue = new DesktopLaunchers(this, host, monitorPos, desktopId, osId, chrome);
        // A desktop nobody registered is drawn as the first Frames edition, as its look is.
        this.panel = chrome != null ? chrome.panelStyle() : PanelStyle.FRAMES_95;
        final OsDef os =
                OsRegistry.getOs(osId);
        this.desktopDir = SystemLayout.desktopDirFor(os, os == null ? null
                : OsRegistry.getKernel(os.kernelId()));
        this.theme = DesktopTheme.forDesktop(desktopId);
        this.prefs = new DesktopPrefs(this, desktopId);
    }

    private boolean is(final PanelStyle style) {
        return panel == style;
    }

    /** A Linux desktop environment (bottom-panel KDE/Cinnamon or top-bar GNOME), as opposed to a Frames edition. */
    boolean linuxDesktop() {
        return is(PanelStyle.KDE)
                || is(PanelStyle.GNOME)
                || is(PanelStyle.CINNAMON);
    }

    /** The desktop environment's display name, for the Start band and menus. */
    private String desktopName() {
        return chrome != null ? chrome.displayName() : desktopId.getPath();
    }

    /** Reboots after a crash: the session is lost (windows and their saved state), back to an empty desktop. */
    private void reboot() {
        memory.recover();
        notices.dismissBalloon();
        windows.clear();
        layouts.forgetSession();
        start.close();
        notices.dismissPopup();
    }

    // inspection (client tests drive the desktop through the same hit areas the player clicks)

    public boolean isStartOpen() {
        return start.isOpen();
    }

    /** Screen position of the desktop's top-left corner: window and app geometry is relative to it. */
    public int desktopX() {
        return view.left();
    }

    public int desktopY() {
        return view.top();
    }

    /**
     * A rectangle an app drew, as it lands on the screen.
     *
     * <p>The desktop is drawn at a scale of its own, so a rectangle in an app's coordinates is not
     * where it appears: it moves with the glass and shrinks with it. Anything outside the desktop that
     * has to line up with something inside it asks here, rather than adding the corner and forgetting
     * the scale, which lands the shape low, right and too big.
     */
    public Rect2i onScreen(final int x, final int y, final int w, final int h) {
        return new Rect2i(view.screenX(x), view.screenY(y),
                (int) Math.round(w * view.scale()), (int) Math.round(h * view.scale()));
    }

    /** Whether the panel's own menu is open. */
    public boolean isPanelMenuOpen() {
        return panelMenu.isOpen();
    }

    /** Whether a program's menu, the one its panel entry opens, is open. */
    public boolean isTaskMenuOpen() {
        return taskbar.menu().isOpen();
    }

    /**
     * Screen position of the centre of the {@code index}-th panel entry, wherever this panel keeps its
     * entries: centred on Frames 11, from the left on every other panel.
     */
    public int[] taskButtonPoint(final int index) {
        final TaskStrip strip = taskbar.strip(view.width());
        return index >= 0 && index < strip.entries().size() ? taskEntryPoint(strip.entries().get(index).key()) : null;
    }

    /**
     * Screen position of the centre of the panel entry of the program {@code key}, or null when the
     * panel has none for it. On Frames XP a pinned program with no window is its quick launch icon.
     */
    public int[] taskEntryPoint(final String key) {
        final TaskStrip strip = taskbar.strip(view.width());
        final int index = TaskbarGroups.indexOf(strip.entries(), keyFor(key));
        if (index < 0) {
            return null;
        }
        final int y = view.screenY(view.panelOnTop() ? TASKBAR_H / 2 : view.height() - TASKBAR_H / 2);
        if (strip.w()[index] > 0) {
            return new int[] {view.screenX(strip.x()[index] + strip.w()[index] / 2), y};
        }
        final int quick = strip.quickIndexOf(index);
        return quick < 0 ? null
                : new int[] {view.screenX(strip.quickX() + quick * TaskStrip.QL_W + TaskStrip.QL_W / 2), y};
    }

    /** The programs the panel lists, in order, by what they read as: the pinned ones first, then every open one. */
    public List<String> taskEntryLabels() {
        final List<String> out = new ArrayList<>();
        for (final TaskbarGroups.Entry entry : taskbar.entries()) {
            out.add(nameOf(entry.key()));
        }
        return out;
    }

    /** The programs pinned to the panel, by the label the panel shows them under. */
    public List<String> pinnedLabels() {
        final List<String> out = new ArrayList<>();
        for (final String key : taskbar.pinnedKeys()) {
            out.add(nameOf(key));
        }
        return out;
    }

    /** Whether the panel's popup (the windows of one program) is up. */
    public boolean isTaskPopupOpen() {
        return taskPopup.isOpen();
    }

    /** The titles the panel's popup lists, in order, empty when it is not up. */
    public List<String> taskPopupTitles() {
        return taskPopup.titles();
    }

    /** The desktop-local centre of the popup's {@code index}-th card or row, where a test clicks it. */
    public int[] taskPopupItemPoint(final int index) {
        return taskPopup.itemPoint(index);
    }

    /** The desktop-local centre of the popup's close box for its {@code index}-th window. */
    public int[] taskPopupClosePoint(final int index) {
        return taskPopup.closePoint(index);
    }

    /** The labels of the open program menu, in order, empty when none is up. */
    public List<String> taskMenuLabels() {
        final List<String> out = new ArrayList<>();
        if (taskbar.menu().isOpen()) {
            for (final ContextMenu.Item item : taskbar.menu().items()) {
                out.add(item.label());
            }
        }
        return out;
    }

    /** The desktop-local centre of the program menu's entry {@code label}, or null when it is not there. */
    public int[] taskMenuPoint(final String label) {
        final List<String> labels = taskMenuLabels();
        final int index = labels.indexOf(label);
        return index < 0 ? null : taskbar.menu().itemCenter(index);
    }

    /** The titles of the dialog windows up on the desktop, front-most last. */
    public List<String> dialogTitles() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (w.dialog()) {
                out.add(w.app().title());
            }
        }
        return out;
    }

    /** The dialog window up over a window of the program {@code label}, or null. */
    public DesktopWindow dialogWindowFor(final String label) {
        final String key = keyFor(label);
        for (int i = windows.size() - 1; i >= 0; i--) {
            final DesktopWindow w = windows.get(i);
            if (w.dialog() && w.groupKey().equals(key)) {
                return w;
            }
        }
        return null;
    }

    /** Screen position of the centre of the panel menu's {@code label} entry, or null when it is not there. */
    @Nullable
    public int[] panelMenuPoint(final String label) {
        final int[] local = panelMenu.pointOf(label);
        return local == null ? null : new int[] {view.screenX(local[0]), view.screenY(local[1])};
    }

    /** Screen position of the middle of one of the Front Panel's controls, under the arrow at its head. */
    public int[] frontPanelPoint(final CdeFrontPanelLayout.Control control) {
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.control(control, view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2), view.screenY(r.y() + CdeFrontPanelLayout.ARROW_H + (r.h()
                - CdeFrontPanelLayout.ARROW_H) / 2)};
    }

    /** Screen position of the middle of the Front Panel's button for workspace {@code index}, from nought. */
    public int[] workspacePoint(final int index) {
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.workspace(index, view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2), view.screenY(r.y() + r.h() / 2)};
    }

    /** Screen position of a title-bar button (1 minimise, 2 maximise, 3 the way out) of the window so labelled. */
    public int[] windowButtonPoint(final String label, final int button) {
        final String key = keyFor(label);
        for (final DesktopWindow w : windows) {
            if (w.appKey().equals(key)) {
                final int[] at = w.buttonCentre(button);
                return new int[] {view.screenX(at[0]), view.screenY(at[1])};
            }
        }
        return new int[] {0, 0};
    }

    /** Screen position of the arrow at the head of a Front Panel control, which raises what is behind it. */
    public int[] frontPanelArrowPoint(final CdeFrontPanelLayout.Control control) {
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.control(control, view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2), view.screenY(r.y() + CdeFrontPanelLayout.ARROW_H / 2 + 1)};
    }

    /** The name the Front Panel is showing over the control the pointer rests on, or empty while it shows none. */
    public String frontPanelTip() {
        return cdePanels.shownTip();
    }

    /** What the subpanel standing on the Front Panel lists, top to bottom, or nothing when none is up. */
    public List<String> subpanelLabels() {
        return cdeLaunchers.labels();
    }

    /** Screen position of the line so labelled on the subpanel that is up, or null. */
    public int[] subpanelPoint(final String label) {
        final int[] at = cdeLaunchers.rowCentre(label, view.width(), view.height());
        return at == null ? null : screenPoint(at);
    }

    /** The names under the icons of the Application Manager window so titled, or nothing when it is not up. */
    public List<String> applicationManagerNames(final String windowTitle) {
        final DesktopWindow w = windowFor(windowTitle);
        return w != null && w.app() instanceof ApplicationManagerApp app ? app.names() : List.of();
    }

    /** Screen position of the icon so named in the Application Manager window so titled, or null. */
    public int[] applicationManagerPoint(final String windowTitle, final String name) {
        final DesktopWindow w = windowFor(windowTitle);
        final int[] at = w != null && w.app() instanceof ApplicationManagerApp app ? app.iconCentre(name) : null;
        return at == null ? null : screenPoint(at);
    }

    /** What the Workstation Info window shows, as {@code label=value}, or nothing while it is not up. */
    public List<String> workstationInfoFacts() {
        final DesktopWindow w = windowFor(WORKSTATION_INFO_KEY);
        return w != null && w.app() instanceof WorkstationInfoApp app ? app.shownFacts() : List.of();
    }

    /** CDE's look as the desktop is wearing it, as the machine would keep it. */
    public String wornCdeStyle() {
        return prefs.cdeStyle().encoded();
    }

    /** Screen position of a page on the Style Manager's strip, or null while the Style Manager is not up. */
    public int[] styleManagerPagePoint(final String page) {
        final StyleManagerApp manager = styleManager();
        final int[] at = manager == null ? null : manager.pageCentre(page);
        return at == null ? null : screenPoint(at);
    }

    /** Screen position of a palette on the Color page, or of a pattern on the Backdrop page, whichever is up. */
    public int[] stylePageRowPoint(final String name) {
        final StyleManagerApp manager = styleManager();
        if (manager == null) {
            return null;
        }
        int[] at = manager.colorPage() == null ? null : manager.colorPage().rowCentre(name);
        if (at == null && manager.backdropPage() != null) {
            at = manager.backdropPage().rowCentre(name);
        }
        return at == null ? null : screenPoint(at);
    }

    /** Screen position of a button of a Style Manager page: OK or Cancel on Color, Apply or Close on Backdrop. */
    public int[] stylePageButtonPoint(final boolean color, final int button) {
        final StyleManagerApp manager = styleManager();
        if (manager == null) {
            return null;
        }
        final int[] at = color
                ? manager.colorPage() == null ? null : manager.colorPage().buttonCentre(button)
                : manager.backdropPage() == null ? null : manager.backdropPage().buttonCentre(button);
        return at == null ? null : screenPoint(at);
    }

    /**
     * Screen position of a part of the Style Manager's Audio page: {@code mute}, {@code monitor}, {@code speakers},
     * {@code ok}, {@code cancel}, or {@code scale} at the volume {@code value}; null while the page is not up.
     */
    @Nullable
    public int[] styleAudioPoint(final String part, final int value) {
        final StyleManagerApp manager = styleManager();
        final CdeAudioPage page = manager == null ? null : manager.audioPage();
        final int[] at = page == null ? null : page.partCentre(part, value);
        return at == null ? null : screenPoint(at);
    }

    @Nullable
    private StyleManagerApp styleManager() {
        for (final DesktopWindow w : windows) {
            if (w.app() instanceof StyleManagerApp manager) {
                return manager;
            }
        }
        return null;
    }

    /** Screen position of the middle of EXIT on the Front Panel. */
    public int[] exitPoint() {
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.exit(view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2), view.screenY(r.y() + r.h() / 2)};
    }

    /** Whether the dialog that shuts the machine down or restarts it is up. */
    public boolean powerDialogOpen() {
        return power.isOpen();
    }

    /** Screen position of a button of CDE's Exit dialog, by the numbers {@link CdeExitLayout} gives them. */
    public int[] exitDialogPoint(final int button) {
        final CdeFrontPanelLayout.Rect r = CdeExitLayout.button(button, view.width(), view.height());
        return new int[] {view.screenX(r.x() + r.w() / 2), view.screenY(r.y() + r.h() / 2)};
    }

    /** What the window menu CDE has up lists, top to bottom, or nothing when none is up. */
    public List<String> windowMenuLabels() {
        return cdeWindowMenu.labels();
    }

    /** Screen position of the entry so labelled on the window menu that is up, or null. */
    public int[] windowMenuPoint(final String label) {
        final int[] at = cdeWindowMenu.entryCentre(label);
        return at == null ? null : new int[] {view.screenX(at[0]), view.screenY(at[1])};
    }

    /** Screen position of the box of workspace {@code index} on the Occupy Workspace dialog that is up, or null. */
    public int[] occupyBoxPoint(final int index) {
        final OccupyWorkspaceDialog dialog = occupyDialog();
        return dialog == null ? null : screenPoint(dialog.boxCentre(index));
    }

    /** Screen position of OK on the Occupy Workspace dialog that is up, or null. */
    public int[] occupyOkPoint() {
        final OccupyWorkspaceDialog dialog = occupyDialog();
        return dialog == null ? null : screenPoint(dialog.okCentre());
    }

    /** The workspaces the window so labelled is on, counted from nought. */
    public List<Integer> workspacesOf(final String label) {
        final List<Integer> out = new ArrayList<>();
        final String key = keyFor(label);
        for (final DesktopWindow w : windows) {
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
    public int[] putAwayIconPoint(final int index) {
        final CdeFrontPanelLayout.Rect tile = CdeWindowIconLayout.tile(index, view.width(), view.workAreaTop());
        return new int[] {view.screenX(tile.x() + tile.w() / 2), view.screenY(tile.y() + tile.h() / 2)};
    }

    /** Which workspace is up, counted from nought. */
    public int shownWorkspace() {
        return wm.workspace();
    }

    /** The labels of the program windows that are on show: open, not put away, on the workspace that is up. */
    public List<String> shownWindowLabels() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && !wm.away(w)) {
                out.add(nameOf(w.appKey()));
            }
        }
        return out;
    }

    /** What the title bars of the windows on show say. */
    public List<String> shownWindowTitles() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (!wm.away(w)) {
                out.add(titleOf(w));
            }
        }
        return out;
    }

    /** What a window is called on this desktop, on its title bar and wherever the panel lists it. */
    String titleOf(final DesktopWindow w) {
        return w.titleOn(chrome);
    }

    /** Screen position of a point on the panel clear of Start and of the task buttons: its empty stretch. */
    public int[] emptyPanelPoint() {
        return new int[] {view.screenX(taskbar.emptyX(view.width())),
                view.screenY(view.height() - TASKBAR_H / 2)};
    }

    /** Where the speaker on the panel is, on the screen. */
    public int[] speakerPoint() {
        final boolean top = view.panelOnTop();
        final int panelY = top ? 0 : view.height() - view.panelBand();
        return new int[] {view.screenX(tray.speakerX(view.width(), top) + 4), view.screenY(panelY + TASKBAR_H / 2)};
    }

    /**
     * Where a part of the open volume control is, on the screen: {@code track} at the volume {@code index},
     * {@code mute}, {@code chevron}, {@code output} number {@code index}, or {@code footer}; null when it has none.
     */
    @Nullable
    public int[] volumePoint(final String part, final int index) {
        final int[] p = volumePopup.pointOf(part, index);
        return p == null ? null : new int[] {view.screenX(p[0]), view.screenY(p[1])};
    }

    public boolean volumeControlOpen() {
        return volumePopup.controlOpen();
    }

    public boolean volumeMenuOpen() {
        return volumePopup.menuOpen();
    }

    /** The volume the panel shows, and whether it shows it muted. */
    public int volumeShown() {
        return volumePopup.volume();
    }

    public boolean mutedShown() {
        return volumePopup.muted();
    }

    /** The volume control this desktop opens, by the name of its look, or empty on a desktop with none. */
    public String volumeLook() {
        final VolumePopupLayout.Look look = volumePopup.look();
        return look == null ? "" : look.name();
    }

    /** The labels of the program windows this desktop has open (dialogs aside), back to front. */
    public List<String> openWindowLabels() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (!w.dialog()) {
                out.add(nameOf(w.appKey()));
            }
        }
        return out;
    }

    /**
     * What this desktop calls its terminal (Command Prompt, Megashell, Konsole...), for a program
     * offering to open one; empty when no desktop is up or it has none.
     */
    public static String terminalName() {
        return active == null ? "" : active.opener.terminalLabel();
    }

    public List<String> launcherLabels() {
        return catalogue.labels();
    }

    /** Whether {@code app} runs in the front (focused) window; what a recipe viewer's drop or transfer targets. */
    public boolean isFront(final IDesktopApp app) {
        final DesktopWindow front = wm.front();
        return front != null && front.app() == app;
    }

    /**
     * Every window of a program (its dialogs aside), front-most last, since a program may be open more than once.
     * The program is named by its window key or by the label it reads as here.
     */
    public List<DesktopWindow> windowsFor(final String label) {
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
    public DesktopWindow windowFor(final String label) {
        final String key = keyFor(label);
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && w.appKey().equals(key)) {
                return w;
            }
        }
        return null;
    }

    /**
     * The window key a request or a test names: a key as it is, or else the label a launcher or an open window
     * reads as on this desktop. A player who types a program's name at a shell asks this way; so do the tests,
     * which name what they click by what it says.
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
     * What a window key reads as on this desktop: the label of the launcher that opens it, else the name this
     * desktop gives its program, else what its window calls itself, else the key.
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
     * What a window key reads as on the desktop that is up, for a program listing the windows a machine has open
     * (a task manager) by the names the player knows them by; the key itself while no desktop is up.
     */
    public static String windowName(final String key) {
        return active == null ? key : active.nameOf(key);
    }

    /** Screen coordinates of the Start button's centre. */
    public int startButtonX() {
        return view.screenX(is(PanelStyle.FRAMES_11) ? 4 + WIN11_SLOT / 2 : 30);
    }

    public int startButtonY() {
        // GNOME's "Activities" launcher lives in the top bar; every other panel sits at the bottom.
        return view.screenY(view.panelOnTop() ? TASKBAR_H / 2 : view.height() - TASKBAR_H / 2);
    }

    /** Screen coordinates of the centre of the {@code index}-th Start menu entry (valid while it is open). */
    public int startMenuItemX() {
        return view.screenX(start.itemX(-1));
    }

    /**
     * Screen x of the centre of the {@code index}-th Start menu entry. Frames XP lays its entries in two
     * columns (programs left, places right), so the column depends on the entry.
     */
    public int startMenuItemX(final int index) {
        return view.screenX(start.itemX(index));
    }

    public int startMenuItemY(final int index) {
        return view.screenY(start.itemY(index));
    }

    /**
     * Outer bounds of the framed monitor window (the bezel plus its chin), in screen coordinates. Integrations
     * that place a side panel next to this screen (e.g. the JEI ingredient list) read this so the panel sits
     * beside the monitor rather than over it.
     */
    public MonitorFrameStyle.Geometry frameBounds() {
        // The frame wraps the glass as it is on the screen, whatever the desktop inside it is scaled to.
        return MonitorFrameStyle.forEra(prefs.era())
                .geometry(view.left(), view.top(), view.glassWidth(), view.glassHeight());
    }

    @Override
    protected void init() {
        /*
         * Size the container's image rect to the on-screen monitor glass, so leftPos/topPos centre exactly
         * where view.left()/view.top() place the desktop. The inventory/title labels the base would draw are pushed
         * off-screen, since the desktop draws its own chrome.
         */
        this.imageWidth = view.glassWidth();
        this.imageHeight = view.glassHeight();
        super.init();
        this.leftPos = view.left();
        this.topPos = view.top();
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;
        // The speaker on the panel says whether the system is muted from the first frame it is drawn in.
        volumePopup.requestState();

        /*
         * Resolve the era skin before the first frame. The panel's placement now follows the skin (a
         * period desktop panels at the bottom), so waiting for the desktop payload to arrive would draw
         * one frame with the panel on the wrong edge and then jump.
         */
        prefs.rebuildSkin();

        catalogue.build();

        /*
         * The windows that were open when this machine's monitor was last left are the machine's: they arrive
         * from the server with the desktop listing requested below, and the layouts restore them.
         */

        // Become the active desktop and fetch the desktop-folder listing for the background icons.
        active = this;
        requestDesktop();
    }

    /**
     * Restores the windows the machine has open, once, when the server hands them over. Later refreshes
     * of the desktop listing send the same payload again and must not open everything a second time.
     */
    public static void applyWindows(final DesktopWindowsPayload payload) {
        if (active != null && active.host.equals(payload.host())) {
            active.layouts.apply(payload);
        }
    }

    /** The platform the installed system stands on, Frames as the safe default for a machine with no system. */
    Platform platform() {
        final OsDef os = OsRegistry.getOs(osId);
        return os != null ? os.platform() : Platform.FRAMES;
    }

    /** The icon for a window key or a launcher key, for the panel and the menus; the generic one if none. */
    private ResourceLocation iconOf(final String key) {
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

    /** Requests the desktop folder's files so they can be drawn as background icons. */
    void requestDesktop() {
        PacketDistributor.sendToServer(new RequestDesktopFilesPayload(host));
    }

    /**
     * Refreshes the open desktop's server-derived state (installed programs included), so a program
     * installed or removed while the desktop is up gets its launcher without closing the monitor. Called
     * after any action that can change the installed set (a shell command, an install disc, Settings).
     */
    public static void refreshActive() {
        if (active != null) {
            active.requestDesktop();
        }
    }

    /**
     * A machine saying how the program it is setting up is going.
     *
     * <p>The desktop opens its Setup window on the first word and hands it every one after, so the
     * window is a view of the machine's job: two players at two monitors of one machine see the same
     * bar, and a desktop opened halfway through picks it up where it is.
     */
    public static void acceptSetup(final SetupProgressPayload payload) {
        if (active == null || !active.host.equals(payload.hostPos())) {
            return;
        }
        final DesktopWindow open = active.windowFor(SetupApp.KEY);
        final SetupApp app;
        if (open != null && open.app() instanceof SetupApp existing) {
            app = existing;
        } else {
            app = new SetupApp(active.host, active.desktopId.getPath());
            active.wm.open(SetupApp.KEY, app);
        }
        app.accept(payload);
        if (payload.state() == SetupProgressPayload.STATE_DONE) {
            // The launcher appears, or goes, the moment the job is done.
            active.requestDesktop();
            FilesApps.refreshAll();
        }
    }


    /** Routes a desktop-folder listing reply to the active desktop. */
    public static void acceptDesktop(final DesktopFilesPayload payload) {
        if (active == null) {
            return;
        }
        active.desktopItems.clear();
        active.desktopItems.addAll(payload.files());
        active.computerName = payload.computerName();
        active.view.setScalePercent(payload.prefs().scale());
        // The look the machine keeps goes in first, so the skin the choices rebuild is drawn from it.
        active.prefs.takeCdeStyle(CdeStyle.parse(payload.cdeStyle()));
        active.prefs.apply(payload.prefs().accent(), payload.prefs().brightness(), payload.prefs().clock12h(),
                payload.wallpaper(), payload.prefs().taskbarCentered(), payload.prefs().darkMode());
        active.taskbar.takePinned(payload.pinned());
        active.opener.takeDefaults(payload.defaultApps());
        active.iconGrid.pinnedCells().clear();
        for (final DesktopFilesPayload.WireIconCell cell : payload.iconCells()) {
            active.iconGrid.pinnedCells().put(cell.key(), cell.cell());
        }
        // A program installed or removed while the desktop is up gets or loses its launcher at once.
        final boolean programsChanged = active.catalogue.take(payload.programs(), payload.community());
        active.memory.takeSourceBuilt(payload.sourceBuilt());
        // The trash's picture changes with what is in it, and the icons are made again when it does.
        final boolean trashChanged = active.trash.setFull(payload.trashFull());
        if (programsChanged) {
            active.catalogue.build();
        } else if (trashChanged) {
            active.catalogue.rebuildIcons();
        }
        // Enter rename on a freshly created item once it appears in the listing.
        active.deskFiles.takePendingRename();
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        /*
         * renderBackground already draws the vanilla blur + dim gradient once; a second identical fill
         * would darken the world behind the desktop to near-black (the double-dim bug). One pass only.
         */
        renderBackground(g, mouseX, mouseY, partialTick);
        /*
         * The desktop paints everything itself instead of running the container's render pass, so it posts the
         * two container render events that pass would post: a recipe viewer draws its ingredient list beside the
         * monitor from them (its plain screen-render hook skips container screens on purpose).
         */
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Background(this, g, mouseX, mouseY));
        DesktopRequests.drain(this);
        layouts.pushIfChanged();
        /*
         * Keep the inventory slots glued to the focused Network Interactor window this frame (per-frame, so a
         * dragged window does not leave its slots a tick behind).
         */
        band.sync();
        final int sw = view.width();
        final int sh = view.height();
        final int ox = view.left();
        final int oy = view.top();
        final int lmx = (int) Math.floor(view.localX(mouseX));
        final int lmy = (int) Math.floor(view.localY(mouseY));
        // Cache the local cursor so the Start-menu draw (called deeper in this frame) can highlight the hovered row.
        this.hoverX = lmx;
        this.hoverY = lmy;
        taskPopup.update(lmx, lmy, sw, sh - view.panelBand());
        final HardwareEra eraNow = prefs.era();

        /*
         * The host computer's hardware-era monitor frame wraps the desktop glass, then translate so the desktop
         * draws in local (0,0)-(sw,sh) coordinates.
         */
        MonitorFrame.renderBody(g, ox, oy, view.glassWidth(), view.glassHeight(), eraNow, font);
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        // Everything on the desktop is drawn under its scale, so a smaller setting fits more on the glass.
        g.pose().scale((float) view.scale(), (float) view.scale(), 1);
        g.enableScissor(ox, oy, ox + view.glassWidth(), oy + view.glassHeight());

        if (is(PanelStyle.CDE)) {
            // CDE hangs no picture: each workspace wears a pattern of its own in the palette's backdrop colours.
            MotifChrome.backdrop(g, sw, sh, prefs.cdePalette(), prefs.cdeStyle().backdrop(wm.workspace()));
        } else {
            /*
             * A picture a player drew hangs in front of the built-in wallpapers, and falls back to them the
             * moment it cannot be found, so a deleted drawing never leaves the desktop with a blank wall.
             */
            PixWallpaper.want(host, prefs.wallpaper());
            if (!PixWallpaper.paint(g, sw, sh)) {
                WallpaperPainter.paint(g, sw, sh, desktopId, platform(), prefs.wallpaper(),
                        prefs.darkMode() && is(PanelStyle.FRAMES_11));
            }
        }

        // A cooperative OS that ran out of memory shows its crash screen, then reboots to an empty session.
        if (memory.crashing()) {
            if (memory.crashOver()) {
                reboot();
            } else {
                memory.renderCrash(g, sw, sh);
                g.disableScissor();
                g.pose().popPose();
                return;
            }
        }

        /*
         * Desktop icons: program launchers first, then the desktop folder's files and folders, laid
         * out in columns (top-down, then left-to-right) like a Windows desktop. Each icon's cell comes
         * from the free-positioning layout (a pinned cell, else the next auto-flow cell).
         */
        final int perCol = iconGrid.perColumn();
        /*
         * Each desktop layer draws at its own strictly-increasing Z (DesktopZ): the depth buffer keeps a back
         * layer behind a front one, so a back layer's batched text (an icon label) can never paint over a
         * front layer (an open window). Flushing the text batch between layers does not work: g.flush() is a
         * no-op outside a managed draw in 1.21.1, which is why the icon-label-over-window bug kept returning.
         *
         * Icons draw at DesktopZ.ICONS and the Start menu at DesktopZ.MENU, so the menu covers them via the
         * depth buffer: the icons behind it stay drawn (they must not vanish) and just sit under the panel.
         */
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.ICONS);
        iconGrid.render(g, lmx, lmy);
        // CDE stands a window that was put away on its workspace as an icon, having no panel to list it on.
        if (is(PanelStyle.CDE)) {
            cdeWindowIcons.render(g, wm.putAwayHere(), sw, view.workAreaTop(), prefs.cdePalette());
        }
        g.pose().popPose();

        renderWindows(g, lmx, lmy, partialTick, sw, sh);

        final int tbY = sh - view.panelBand();
        renderPanelLayer(g, tbY, sw, sh, lmx, lmy);
        renderMenus(g, tbY, lmx, lmy, partialTick);
        drags.render(g, sw, tbY, perCol);

        g.disableScissor();
        renderOverlays(g, sw, sh, lmx, lmy, partialTick);
        g.pose().popPose(); // close the (ox, oy) desktop-origin translate

        /*
         * The container pass posts its foreground event with the pose at the gui origin and the depth test off,
         * so a listener draws over the finished screen without fighting the desktop's layered depth.
         */
        RenderSystem.disableDepthTest();
        g.pose().pushPose();
        g.pose().translate(leftPos, topPos, 0);
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Foreground(this, g, mouseX, mouseY));
        g.pose().popPose();
        RenderSystem.enableDepthTest();
    }

    /*
     * The desktop paints its whole surface in the render() override above and does not call super.render(), so
     * the container's background pass is unused, and the inventory items and cursor are drawn by render() instead.
     */
    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
    }

    /**
     * The open windows, back to front, and the real container items of whichever one carries the player's
     * inventory.
     *
     * <p>Each window draws in a depth band of its own. An item is a model standing well in front of the pose
     * it is drawn at, so windows sharing one depth painted their items over one another.
     */
    private void renderWindows(final GuiGraphics g, final int lmx, final int lmy, final float partialTick,
                               final int sw, final int sh) {
        final DesktopWindow front = wm.front();
        for (int i = 0; i < windows.size(); i++) {
            final DesktopWindow w = windows.get(i);
            // A window on a workspace that is not up is not drawn at all, which is what makes them cost nothing.
            if (wm.away(w)) {
                continue;
            }
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.windowZ(i, windows.size()));
            w.setFocused(w == front);
            w.render(g, font, prefs.skin(), lmx, lmy, partialTick, sw, sh, view.panelReserve(), view.workAreaTop());
            g.pose().popPose();
        }
        /*
         * Real container-slot items for the focused Network Interactor window's inventory zone, over the
         * window the app already drew the slot backgrounds for.
         */
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.INVENTORY);
        hoveredSlot = band.render(g, lmx, lmy);
        g.pose().popPose();
    }

    /**
     * The panel this desktop wears, and the three things that sit just above it: a tray balloon, the figures
     * behind the notification area, and the popup listing one program's windows.
     */
    private void renderPanelLayer(final GuiGraphics g, final int tbY, final int sw, final int sh,
                                  final int lmx, final int lmy) {
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.TASKBAR);
        if (is(PanelStyle.CDE)) {
            // CDE has no bar at all: a slab of controls at the bottom centre, in its palette's relief.
            cdePanels.render(g, sw, sh, prefs.cdePalette());
        } else if (is(PanelStyle.FRAMES_11)) {
            // Frames 11 taskbar: dark bar, centered Start + app icons with an active indicator, clock right.
            framesPanels.renderModern(g, tbY, sw, lmx, lmy);
        } else if (periodPanel()) {
            renderPeriodPanel(g, tbY, sw, sh, lmx, lmy);
        } else if (is(PanelStyle.GNOME)) {
            renderGnomeTopBar(g, sw, lmx, lmy);
        } else if (linuxDesktop()) {
            renderLinuxPanel(g, tbY, sw, sh, lmx, lmy);
        } else {
            framesPanels.renderClassic(g, tbY, sw, sh, lmx, lmy);
        }
        g.pose().popPose();

        // A tray balloon sits above the panel and under the menus, so opening the launcher covers it.
        if (notices.balloonUp()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.TASKBAR + 10);
            notices.renderBalloon(g, tbY, sw);
            g.pose().popPose();
        }
        // The figures behind the notification area, while the cursor rests on it. CDE has no such area.
        if (!view.panelOnTop() && !is(PanelStyle.CDE)) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.TASKBAR + 8);
            drawTrayTip(g, tbY, sw);
            g.pose().popPose();
        }
        // The windows of one program, over the panel and the windows themselves.
        if (taskPopup.key() != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.MENU);
            taskPopup.render(g, tbY, sw, sh, lmx, lmy);
            g.pose().popPose();
        }
    }

    /**
     * The menus that share a height above the panel: the launcher, the panel's own, the desktop's, and the volume
     * control with its menu.
     */
    private void renderMenus(final GuiGraphics g, final int tbY, final int lmx, final int lmy,
                             final float partialTick) {
        // The name of a Front Panel control rides at the menus' height, so no window can stand over it.
        if (is(PanelStyle.CDE) && !menuOrDialogOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.MENU);
            cdePanels.renderTip(g, view.width(), view.height(), prefs.cdePalette());
            g.pose().popPose();
        }
        if (volumePopup.isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.MENU);
            volumePopup.render(g, new UiContext(prefs.skin(), font, lmx, lmy, partialTick), view.width(), tbY,
                    view.panelOnTop());
            g.pose().popPose();
        }
        if (!start.isOpen() && !deskMenu.isOpen() && !panelMenu.isOpen() && !cdeLaunchers.isOpen()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.MENU);
        start.render(g, tbY);
        // A subpanel of CDE's Front Panel is no launcher that comes and goes: it stays up until its arrow says so.
        cdeLaunchers.render(g, view.width(), view.height(), prefs.cdePalette());
        panelMenu.render(g, lmx, lmy);
        if (deskMenu.isOpen()) {
            deskMenu.render(g, new UiContext(prefs.skin(), font, lmx, lmy, partialTick));
        }
        g.pose().popPose();
    }

    /**
     * Everything that sits over the finished desktop, in the order it stacks: hover tooltips, the stack on
     * the cursor, the brightness dim, a program's own modal dialog, a dialog over the whole desktop, the
     * power dialog, and a program's menu from its panel entry.
     */
    private void renderOverlays(final GuiGraphics g, final int sw, final int sh, final int lmx, final int lmy,
                                final float partialTick) {
        /*
         * Hover tooltips draw at the base pose because the vanilla tooltip renderer translates +400 itself,
         * landing them at DesktopZ.TOOLTIP, above every window and the panel. The front window's app draws
         * its own hover hints; the inventory zone defers to the real slot's item tooltip.
         */
        final DesktopWindow tooltipWin = wm.front();
        if (tooltipWin != null) {
            tooltipWin.renderTooltip(g, font, lmx, lmy);
        }
        if (hoveredSlot != null && menu.getCarried().isEmpty() && hoveredSlot.hasItem()) {
            g.renderTooltip(font, hoveredSlot.getItem(), lmx, lmy);
        }

        // The carried (cursor) stack rides above the tooltip, at the mouse.
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.CURSOR);
        band.renderCarried(g, lmx, lmy);
        g.pose().popPose();

        // Brightness: a per-computer dim over the whole surface (100 = none, 0 = deeply dimmed).
        if (prefs.brightness() < 100) {
            final int alpha = Math.min(210, (100 - prefs.brightness()) * 21 / 10);
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP - 1);
            g.fill(0, 0, sw, sh, alpha << 24);
            g.pose().popPose();
        }

        /*
         * A focused app's modal dialog draws above every item icon and window, so the dialog and its own dim
         * cover and darken the icons instead of them piercing through at their blit depth.
         */
        final DesktopWindow modalWin = wm.front();
        if (modalWin != null && modalWin.app().modalActive()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            modalWin.app().renderModal(g, font,
                    modalWin.x() + 4, modalWin.y() + DesktopWindow.TITLE_H + 4,
                    modalWin.width() - 8, modalWin.height() - DesktopWindow.TITLE_H - 8, lmx, lmy);
            g.pose().popPose();
        }

        notices.renderPopup(g, prefs.skin(), lmx, lmy, sw, sh);
        // The power dialog rides at the same height: it is the one choice that ends the session.
        if (power.isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            power.render(g, sw, sh, lmx, lmy);
            g.pose().popPose();
        }
        // A program's own menu, from its panel entry, sits above the windows it acts on.
        if (taskbar.menu().isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            taskbar.menu().render(g, new UiContext(prefs.skin(), font, lmx, lmy, partialTick));
            g.pose().popPose();
        }
        // So does a window's own menu on CDE, which hangs from the button at the left of its title bar.
        if (cdeWindowMenu.isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            cdeWindowMenu.render(g, lmx, lmy, prefs.cdePalette());
            g.pose().popPose();
        }
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

    /**
     * Lets a running app hand the shell a job: lines typed at the terminal, one after the other. A line with a
     * newline in it is typed as the lines it holds.
     */
    public static void requestTypeAtTerminal(final List<String> lines) {
        DesktopRequests.open(new OpenRequest.TypeAtTerminal(List.of(String.join("\n", lines).split("\n"))));
    }

    /* What a test reads of the desktop's menu and its scale. */

    /** Whether the desktop's right-click menu is up. */
    public boolean deskMenuOpen() {
        return deskMenu.isOpen();
    }

    /** The desktop-local centre of the desk menu's item {@code index}, where a test clicks it. */
    public int[] deskMenuItemCenter(final int index) {
        return deskMenu.itemCenter(index);
    }

    /** The labels of the desk menu's items, in order, so a test finds one by name. */
    public List<String> deskMenuLabels() {
        return deskMenu.labels();
    }

    /** The desktop-local centre of item {@code index} of the menu open beside the desk menu, or null when none is. */
    public int[] deskSubmenuItemCenter(final int index) {
        return deskMenu.submenuItemCenter(index);
    }

    /** The names of the files and folders on the desktop, as their icons read. */
    public List<String> desktopItemNames() {
        final List<String> out = new ArrayList<>();
        for (final DiskFilesPayload.WireFile file : desktopItems) {
            out.add(FsPaths.fileName(file.path()));
        }
        return out;
    }

    /** The names under the wallpaper's icons ahead of the files, in their order, the trash first where it stands. */
    public List<String> deskIconLabels() {
        final List<String> out = new ArrayList<>();
        for (final Launcher l : catalogue.icons()) {
            out.add(l.label());
        }
        return out;
    }

    /** The desktop-local middle of the wallpaper icon so named, a program's, the trash's or a file's; or null. */
    @Nullable
    public int[] deskIconPoint(final String name) {
        final int icons = catalogue.icons().size();
        for (int slot = 0; slot < icons + desktopItems.size(); slot++) {
            final String label = slot < icons ? catalogue.icons().get(slot).label()
                    : FsPaths.fileName(desktopItems.get(slot - icons).path());
            if (label.equals(name)) {
                return iconGrid.centreOf(slot);
            }
        }
        return null;
    }

    /** The trash window that is up, or null while none is. */
    @Nullable
    public TrashApp trashWindow() {
        final DesktopWindow open = windowFor(WindowKeys.TRASH);
        return open != null && open.app() instanceof TrashApp app ? app : null;
    }

    /** The scale the desktop is drawn at, as a factor, which a test needs to land a click on a scaled desktop. */
    public double desktopScale() {
        return view.scale();
    }

    /** Asks for the Properties window of a file on the desktop, which the explorer knows how to show. */
    public static void requestFileProperties(final String path) {
        DesktopRequests.open(new OpenRequest.Properties(path));
    }

    /** Whether the host computer is on a data network right now, as its block entity tells the client. */
    private boolean networkAttached() {
        final Level level = Minecraft.getInstance().level;
        return level != null
                && level.getBlockEntity(host) instanceof IOsHost computer
                && computer.networkAttached();
    }

    private void drawTrayTip(final GuiGraphics g, final int panelY, final int sw) {
        tray.drawTip(g, panelY, sw);
    }

    /** Whether a desktop-local point is on the bottom panel's Start button. */
    boolean startButtonHit(final double mx, final double my, final int tbY) {
        return framesPanels.startButtonHit(mx, my, tbY);
    }

    // Linux desktop environments: panels

    /**
     * The KDE Plasma / Cinnamon bottom panel: a dark bar with the launcher button on the left, one task button
     * per open window (icon + title, accent underline when focused) at the same 88px pitch the classic taskbar
     * uses (so the shared click handling applies), and the clock and process meter on the right.
     */
    private void renderLinuxPanel(final GuiGraphics g, final int tbY, final int sw, final int sh,
                                  final int lmx, final int lmy) {
        linuxPanels.renderModern(g, tbY, sw, sh, lmx, lmy);
    }


    /**
     * The panel of a Legacy-era Unix desktop, at the bottom for both KDE and GNOME. It is drawn entirely
     * out of the skin's own primitives (a raised launcher stud, raised task buttons, a sunken clock)
     * so the panel is made of the same relief the windows are, instead of the flat modern band.
     */
    private void renderPeriodPanel(final GuiGraphics g, final int tbY, final int sw, final int sh,
                                   final int lmx, final int lmy) {
        linuxPanels.renderPeriod(g, tbY, sw, sh, lmx, lmy);
    }

    private void renderGnomeTopBar(final GuiGraphics g, final int sw, final int lmx, final int lmy) {
        linuxPanels.renderGnomeTopBar(g, sw, lmx, lmy);
    }

    /** A short account line for the Frames 11 Start footer: the computer's name, or a generic label. */
    private String hostAccountLabel() {
        return (computerName == null || computerName.isBlank()) ? words(DesktopTexts.LOCAL_ACCOUNT)
                : trim(computerName, 22);
    }

    /** A thin one-pixel rectangle outline used by the Frames 11 Start panel. */
    private static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h,
                               final int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /**
     * Tells the machine what the power dialog chose. It is going down, restarting or being left: the desktop closing
     * after this must not hand its windows back to a machine whose session has just ended.
     */
    void cyclePower(final int action) {
        layouts.powerCycling();
        PacketDistributor.sendToServer(new MachinePowerPayload(host, monitorPos, action));
    }

    /** A click goes down the desktop one layer at a time; only what nothing on it claimed reaches the container. */
    @Override
    public boolean mouseClicked(final double mouseXAbs, final double mouseYAbs, final int button) {
        return input.click(mouseXAbs, mouseYAbs, button) != DesktopInput.Click.CONTAINER
                || super.mouseClicked(view.slotX(mouseXAbs), view.slotY(mouseYAbs), button);
    }

    @Override
    public boolean mouseDragged(final double mouseXAbs, final double mouseYAbs, final int button,
                                final double dx, final double dy) {
        return input.drag(mouseXAbs, mouseYAbs, button) != DesktopInput.Click.CONTAINER
                || super.mouseDragged(view.slotX(mouseXAbs), view.slotY(mouseYAbs), button, dx, dy);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        return input.release(mouseX, mouseY, button) != DesktopInput.Click.CONTAINER
                || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        return input.typed(c) || super.charTyped(c, modifiers);
    }

    /** A key is the desktop's ahead of the rest of the game only while something on it is being typed at. */
    @Override
    public boolean keyFirst(final int key, final int scanCode, final int modifiers) {
        return input.keyFirst(key, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final DesktopInput.Click taken = input.key(key, scanCode, modifiers);
        return taken == DesktopInput.Click.CONTAINER ? super.keyPressed(key, scanCode, modifiers)
                : taken == DesktopInput.Click.TAKEN;
    }

    /** A key let go goes to the window in front, for a program that tells a press from a release. */
    @Override
    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        return input.keyReleased(key, scanCode, modifiers) || super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double dx, final double dy) {
        return input.scrolled(mouseX, mouseY, dy) || super.mouseScrolled(mouseX, mouseY, dx, dy);
    }

    /** The Occupy Workspace dialog that is up, front-most first, or null. */
    @Nullable
    private OccupyWorkspaceDialog occupyDialog() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (windows.get(i).app() instanceof OccupyWorkspaceDialog dialog) {
                return dialog;
            }
        }
        return null;
    }

    /** A desktop-local point as the screen position a click is given in. */
    private int[] screenPoint(final int[] local) {
        return new int[] {view.screenX(local[0]), view.screenY(local[1])};
    }

    /** Lays the inventory's slots over the window in front once a tick, ahead of the next frame. */
    @Override
    protected void containerTick() {
        super.containerTick();
        band.sync();
    }

    /** The first window one of the machine's Σ# programs has open on this desktop, or null. */
    @Nullable
    public SigmaWindowApp programWindow() {
        for (final DesktopWindow open : windows) {
            if (open.app() instanceof SigmaWindowApp app) {
                return app;
            }
        }
        return null;
    }

    private static String trim(final String s, final int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "...";
    }

    @Override
    public void removed() {
        /*
         * The explorers of this desktop stop listening: another machine's desktop may open next, and a
         * listing of its disk must not land in a window that was showing this one. They say they are
         * back when this desktop is restored.
         */
        FilesApps.forgetAll();
        TrashApp.forgetAll();
        ThisPcApp.forgetAll();
        /*
         * And the picture on the wall is let go with them: another machine's desktop may open next, and a
         * wallpaper chosen there under the same file name would otherwise be shown this one's drawing.
         */
        PixWallpaper.clear();
        // The layout goes back to the machine, and the programs' insides stay in this client.
        layouts.keep();
        if (active == this) {
            active = null;
        }
        // A screen that is gone is told nothing more about what it asked the machine for.
        CodeFileReplies.forget(deskFiles);
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /*
     * Ctrl+B is the game's narrator switch, and the game flips it before any screen sees the key unless
     * the screen's focused control is a text box taking input. Every key on the desktop is the desktop's
     * (C-b in Emacs above all), so the desktop always reports one: an invisible box that draws nothing,
     * keeps nothing and answers no key, there only to say that typing is spoken for.
     */
    @Nullable
    private KeySink keySink;

    @Override
    @Nullable
    public GuiEventListener getFocused() {
        if (this.font == null) {
            return super.getFocused();
        }
        if (this.keySink == null) {
            this.keySink = new KeySink(this.font);
        }
        return this.keySink;
    }

    private static final class KeySink extends EditBox {

        KeySink(final Font font) {
            super(font, 0, 0, 1, 1, Component.empty());
            this.setVisible(true);
            this.setEditable(true);
            this.setFocused(true);
        }

        @Override
        public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
            return false;
        }

        @Override
        public boolean charTyped(final char c, final int modifiers) {
            return false;
        }
    }
}
