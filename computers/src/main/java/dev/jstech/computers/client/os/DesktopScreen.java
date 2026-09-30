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
import dev.jstech.computers.operation.payload.MoveFilePayload;
import dev.jstech.computers.operation.payload.NiDepositPayload;
import dev.jstech.computers.operation.payload.NiShiftInsertPayload;
import dev.jstech.computers.operation.payload.RequestDesktopFilesPayload;
import dev.jstech.computers.operation.payload.SetIconPositionPayload;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.operation.payload.SetupProgressPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.os.CdeAppGroup;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.computers.os.fs.FileOpeners;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.gui.layout.DesktopZ;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
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
import net.minecraft.world.inventory.Slot;
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
    private final List<DesktopWindow> windows = new ArrayList<>();
    /** Where the desktop sits on the game's screen and how big it draws. */
    private final DesktopViewport view = new DesktopViewport(this);
    /** Which workspace is up, counted from nought; always the first on a desktop that has only one. */
    private int shownWorkspace;
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

    /**
     * Open windows kept per-computer across leaving and re-entering the Monitor in the same session.
     * Bounded (access-ordered, eldest evicted past the cap) so a long session that visits many computers
     * does not grow this map without limit.
     */
    private static final int MAX_SAVED_DESKTOPS = 16;

    /*
     * The live app instances kept per computer while its Monitor is left, so re-entering restores each
     * program's in-progress session (terminal scrollback, an unsaved query) instead of a fresh window.
     */
    private static final Map<BlockPos, Map<String, IDesktopApp>> SAVED_APPS =
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        final Map.Entry<BlockPos, Map<String, IDesktopApp>> eldest) {
                    return size() > MAX_SAVED_DESKTOPS;
                }
            };

    /** Apps a running window asked to launch (e.g. Files opening the Editor); drained by the active desktop. */
    private static final List<String> PENDING_OPEN = new ArrayList<>();

    /** Lets a running app request another program be opened on the desktop. */
    public static void requestOpen(final String key) {
        PENDING_OPEN.add(key);
    }

    /**
     * The windows the machine says its Σ# programs have open, waiting for the desktop to draw them.
     *
     * <p>A program's window is the machine's, not this screen's: it is opened when the machine first
     * mentions it, redrawn whenever the machine sends it again, and taken away when the machine says it is
     * gone or the player shuts it.
     */
    private static final List<UiWindowPayload> PENDING_UI =
            new ArrayList<>();

    /** Takes a window a Σ# program has open on the machine being looked at. */
    public static void acceptWindow(final UiWindowPayload payload) {
        PENDING_UI.add(payload);
    }

    /** Windows a running app asked to end (the Task Manager); drained by the active desktop. */
    private static final List<String> PENDING_CLOSE = new ArrayList<>();

    /** Lets a running app end another program's window, the way a task manager does. */
    public static void requestClose(final String key) {
        PENDING_CLOSE.add(key);
    }

    /**
     * Opens {@code dialog} as a window of its own over the window running {@code owner}: it is listed
     * with the owner on the panel, sits in front of it, and holds it until it is put away. The way an
     * Open or Save window belongs to the program that asked. Nothing happens when no desktop is up or
     * the owner has no window on it.
     */
    public static void openDialogFor(final IDesktopApp owner, final IDesktopApp dialog) {
        if (active != null) {
            active.openDialog(owner, dialog);
        }
    }

    /** Closes the window running {@code app}, when it is up, the way its own Close button does. */
    public static void closeWindowFor(final IDesktopApp app) {
        if (active != null) {
            active.closeWindowOf(app);
        }
    }

    /** Puts away the window running {@code dialog}, when it is up. */
    public static void closeDialog(final IDesktopApp dialog) {
        if (active != null) {
            active.closeWindowOf(dialog);
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
            active.startProgramById(path);
        }
    }

    /**
     * Brings the window of that key forward, or opens it the way a session coming back would, for a program that has a
     * second window of its own: Soundfoundry's sharing window, say. The key is the id its window factory is
     * registered under ({@link ProgramClient#register}), so the window comes back with the session like any other.
     */
    public static void openOrFocus(final String key) {
        if (active != null) {
            active.openOrFocusWindow(key);
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
     * A pending open of the explorer at a given folder, kept apart from a plain program key by a separator no
     * program id can contain.
     */
    private static final String OPEN_FILES_AT = "Files\0";

    /** A pending run of a compiled program, kept apart the same way. */
    private static final String RUN_AT_TERMINAL = "Terminal\0";

    /**
     * Opens this desktop's terminal and has it run that program, which is what double-clicking one does.
     *
     * <p>A program of the console kind needs a terminal to print into, so it is given one; the window is
     * whatever this desktop calls its terminal, because that is the one the machine has.
     */
    public static void requestRunAtTerminal(final String path) {
        PENDING_OPEN.add(RUN_AT_TERMINAL + path);
    }

    /** Lets a running app open the explorer already navigated to {@code dir} (a drive, a folder). */
    public static void requestOpenFiles(final String dir) {
        PENDING_OPEN.add(OPEN_FILES_AT + dir);
    }

    /** A queued request to open a file: the program to use (empty for the default), then the path. */
    private static final String OPEN_FILE = "File\0";

    /**
     * Lets a running app open a file in whatever program opens that kind by default.
     *
     * <p>Which program that is has one answer for the whole desktop, so the explorer asks here rather
     * than deciding for itself and disagreeing with a double-click on the desktop.
     */
    public static void requestOpenFile(final String path) {
        PENDING_OPEN.add(OPEN_FILE + "\0" + path);
    }

    /** Lets a running app open a file in a program the player picked. */
    public static void requestOpenFileWith(final String programId, final String path) {
        PENDING_OPEN.add(OPEN_FILE + programId + "\0" + path);
    }

    /** A queued request to ask the player which program opens a file, kept apart like the others. */
    private static final String CHOOSE_OPENER = "Choose\0";

    /** Lets a running app ask the player which program opens a file, as Choose another program does. */
    public static void requestChooseOpener(final String path) {
        PENDING_OPEN.add(CHOOSE_OPENER + path);
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
        if (programId.equals(FileOpeners.EDITOR)) {
            return words(DesktopTexts.EDITOR);
        }
        final ProgramSpec spec = Programs.get(
                ResourceLocation.fromNamespaceAndPath("jsc", programId));
        return spec == null ? programId : GameText.resolve(spec.name());
    }

    /**
     * Forgets every per-machine client cache: the programs' insides kept for the machines of the world the
     * player is leaving, and any open request that never found a desktop. Called on logout, so nothing of
     * one world lingers into the next.
     */
    public static void forgetClientState() {
        SAVED_APPS.clear();
        PENDING_OPEN.clear();
        PENDING_CLOSE.clear();
        PENDING_UI.clear();
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

    /* The notice for a file that no program on this machine opens. */
    private void cannotOpen(final String path) {
        notices.showBalloon(words(DesktopTexts.CANNOT_OPEN),
                GameText.resolve(DesktopTexts.NO_PROGRAM_OPENS.with(FsPaths.fileName(path))));
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
    /** The windows the desktop opens by itself, whatever this desktop calls the programs they belong to. */
    private static final String FILES_KEY = WindowKeys.of(Programs.FILES);
    private static final String SETTINGS_KEY = WindowKeys.of(Programs.SETTINGS);
    private static final String EDITOR_KEY =
            WindowKeys.of(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, FileOpeners.EDITOR));
    private static final String WORKSTATION_INFO_KEY =
            WindowKeys.of(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "workstation_info"));
    private int selectedIcon = -1;
    private long iconClickAt;
    @Nullable
    private DesktopWindow dragging;
    @Nullable
    private DesktopWindow resizing;
    // The window whose title-bar button is currently held down (pushed-in until release).
    @Nullable
    private DesktopWindow pressedBtnWindow;
    private int dragOffsetX;
    private int dragOffsetY;

    /** The currently shown desktop is the one that receives desktop-folder listing replies. */
    @Nullable
    private static DesktopScreen active;


    /**
     * The program chosen with Always for each extension on this machine, as the desktop listing brings it. A choice
     * made here goes in at once, before the server has sent the listing again.
     */
    private final Map<String, String> defaultApps = new HashMap<>();

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

    /*
     * Drag-and-drop of a desktop icon (a file/folder, or a program launcher), onto a folder, an open
     * explorer, or a free grid cell.
     * Rubber-band selection: dragging on empty wallpaper sweeps a rectangle and selects every icon
     * it touches. Every desktop does this, and without it there was no way to act on more than one
     * icon at a time.
     */
    private boolean bandActive;
    private double bandStartX;
    private double bandStartY;
    private double bandX;
    private double bandY;

    /** The band's rectangle in desktop coordinates: {x, y, w, h}. */
    private int[] bandRect() {
        final int bx = (int) Math.min(bandStartX, bandX);
        final int by = (int) Math.min(bandStartY, bandY);
        return new int[]{bx, by, (int) Math.abs(bandX - bandStartX), (int) Math.abs(bandY - bandStartY)};
    }

    private int deskDragSlot = -1; // global icon slot being dragged, or -1
    private boolean deskDragging;
    private double deskDragX;
    private double deskDragY;
    private double deskDragStartX;
    private double deskDragStartY;
    /** How far the cursor must travel from the press point before an icon click becomes a drag. */
    private static final double DRAG_THRESHOLD = 3.0;

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

    /** Opens that window, or brings it forward when one of that key is already up. */
    void openOnce(final String key, final Supplier<IDesktopApp> make) {
        final DesktopWindow open = windowFor(key);
        if (open != null) {
            focusWindow(open);
            return;
        }
        final IDesktopApp app = make.get();
        app.applySkin(prefs.skin());
        openApp(key, app);
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

    /** Which of the desktop's workspaces is up, counted from nought. */
    int workspace() {
        return shownWorkspace;
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
        shownWorkspace = WorkspaceSet.clampIndex(workspace);
        cdeWindowMenu.close();
        start.close();
    }

    /** Whether this desktop has workspaces at all; one that does not keeps everything on the first. */
    boolean hasWorkspaces() {
        return is(PanelStyle.CDE);
    }

    /** The windows put away on the workspace that is up, in the order they were opened, dialogs aside. */
    private List<DesktopWindow> putAwayHere() {
        final List<DesktopWindow> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (w.minimized() && !w.dialog() && w.owner() == null && w.on(shownWorkspace)) {
                out.add(w);
            }
        }
        /*
         * By when each was opened and not by how they are stacked, since bringing one back restacks the
         * list and the icons beside it must not jump about when that happens.
         */
        out.sort(Comparator.comparingInt(DesktopWindow::serial));
        return out;
    }

    /** Whether a window is out of sight: put away, or on a workspace that is not up. */
    private boolean away(final DesktopWindow w) {
        return w.minimized() || !w.on(shownWorkspace);
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

    /** Opens a file manager at that folder, as a window of the file manager this desktop has. */
    void openFolder(final String dir) {
        openApp(FILES_KEY, new FilesApp(host, desktopId.getPath(), dir, monitorPos));
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

    /** Opens the Settings window on its Sound page, where every link to the sound settings leads. */
    void openSoundSettings() {
        openSettingsPage(SettingsApp.PAGE_SOUND);
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

    /** The windows on the desktop, back to front. */
    List<DesktopWindow> windows() {
        return windows;
    }

    /** The flyout that lists one program's windows over its button on the panel. */
    TaskPopup taskPopup() {
        return taskPopup;
    }

    /** The windows a program has open, back to front. */
    List<DesktopWindow> windowsOf(final String key) {
        return groupWindows(key);
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

    /** The icon a click has picked, which shows its whole name, or -1 while none is picked. */
    int pickedIcon() {
        return selectedIcon;
    }

    /** The icon being dragged and where the cursor has taken it, for the drop outline and the ghost. */
    boolean draggingIcon() {
        return deskDragging;
    }

    int draggedIconSlot() {
        return deskDragSlot;
    }

    double iconDragX() {
        return deskDragX;
    }

    double iconDragY() {
        return deskDragY;
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

    /** Picks an icon, which is what a fresh file does so its name is ready to be typed over. */
    void pickIcon(final int slot) {
        selectedIcon = slot;
    }

    /**
     * Whether what is kept on the desktop is laid out from the right edge. CDE did that, and here it also
     * leaves the top left to the icons of the windows that were put away.
     */
    boolean objectsStandRight() {
        return is(PanelStyle.CDE);
    }

    /** Ending or bringing forward a window on the panel's behalf. */
    void closeOne(final DesktopWindow w) {
        closeWindow(w);
    }

    void focusOne(final DesktopWindow w) {
        focusWindow(w);
    }

    /** Sends a window behind every other, its dialogs with it and still in front of it. */
    void lowerOne(final DesktopWindow w) {
        final List<DesktopWindow> sent = new ArrayList<>();
        for (final DesktopWindow other : windows) {
            if (other == w || other.owner() == w) {
                sent.add(other);
            }
        }
        windows.removeAll(sent);
        sent.remove(w);
        windows.addAll(0, sent);
        windows.add(0, w);
    }

    /**
     * Says which workspaces a window is on, its dialogs with it. One taken off the workspace that is up simply
     * leaves it, as it did on CDE, and is found again on any workspace it is still on.
     */
    void occupy(final DesktopWindow w, final int workspaces) {
        for (final DesktopWindow other : windows) {
            if (other == w || other.owner() == w) {
                other.setWorkspaces(workspaces);
            }
        }
    }

    void closeAllOf(final String key) {
        closeGroup(key);
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

    /** Whether anything is open at all, which is what a workspace preview shows. */
    boolean anyWindowOpen() {
        return !windows.isEmpty();
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

    /**
     * Opens CDE's Application Manager on a group, or on the groups themselves for null, and brings the window
     * forward instead when it is already up: one window for each, however often it is asked for.
     */
    void openApplicationManager(@Nullable final CdeAppGroup group) {
        final String key = ApplicationManagerApp.keyOf(group);
        final DesktopWindow open = windowFor(key);
        if (open != null) {
            focusWindow(open);
            return;
        }
        final ApplicationManagerApp app = new ApplicationManagerApp(group);
        app.applySkin(prefs.skin());
        openApp(key, app);
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
        SAVED_APPS.remove(host);
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
        return shownWorkspace;
    }

    /** The labels of the program windows that are on show: open, not put away, on the workspace that is up. */
    public List<String> shownWindowLabels() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && !away(w)) {
                out.add(nameOf(w.appKey()));
            }
        }
        return out;
    }

    /** What the title bars of the windows on show say. */
    public List<String> shownWindowTitles() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (!away(w)) {
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

    /** What this desktop calls its terminal, or an empty string when it has none installed. */
    private String terminalLabel() {
        final Launcher terminal = catalogue.byProgram(Programs.COMMAND_PROMPT);
        return terminal == null ? "" : terminal.label();
    }

    /**
     * What this desktop calls its terminal (Command Prompt, Megashell, Konsole...), for a program
     * offering to open one; empty when no desktop is up or it has none.
     */
    public static String terminalName() {
        return active == null ? "" : active.terminalLabel();
    }

    public List<String> launcherLabels() {
        return catalogue.labels();
    }

    /** Whether {@code app} runs in the front (focused) window; what a recipe viewer's drop or transfer targets. */
    public boolean isFront(final IDesktopApp app) {
        final DesktopWindow front = frontWindow();
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
    private String keyFor(final String asked) {
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
         * Restore the windows that were open when this computer's Monitor was last left.
         * The open windows are the machine's, not this client's: they arrive from the server with the
         * desktop listing requested below, and are restored in applyWindows. The app instances kept per
         * computer (SAVED_APPS) are only the programs' insides (scrollback, an unsaved query) and are
         * reattached to the restored windows when they are still around.
         */

        // Become the active desktop and fetch the desktop-folder listing for the background icons.
        active = this;
        requestDesktop();
    }

    /**
     * Restores the windows the machine has open, once, when the server hands them over. Later refreshes
     * of the desktop listing send the same payload again and must not open everything a second time.
     */
    public static void applyWindows(
            final DesktopWindowsPayload payload) {
        final DesktopScreen screen = active;
        if (screen == null || !screen.host.equals(payload.host()) || screen.windowsRestored) {
            return;
        }
        screen.windowsRestored = true;
        if (!screen.windows.isEmpty()) {
            return; // the player already opened something before the layout arrived; keep theirs
        }
        // Only a desktop that has workspaces comes back on another than the first.
        screen.shownWorkspace = screen.hasWorkspaces() ? payload.workspace() : 0;
        final Map<String, IDesktopApp> savedApps = SAVED_APPS.get(screen.host);
        for (final OpenWindow ow : payload.toOpenWindows()) {
            IDesktopApp app = savedApps != null ? savedApps.get(ow.key()) : null;
            final boolean restored = app != null;
            if (app == null) {
                app = screen.factoryFor(ow.key());
            }
            if (app == null) {
                continue; // a program that is no longer installed simply does not come back
            }
            app.applySkin(screen.prefs.skin());
            if (restored) {
                app.onRestored(); // a kept instance re-asks the server for what may have changed meanwhile
            }
            final DesktopWindow w = new DesktopWindow(app, ow.key(), ow.x(), ow.y(), ow.w(), ow.h());
            /*
             * Clamp into the current work area: the monitor may be a different size from the one the
             * layout was left on, and a title bar off-screen is a window nobody can reach.
             */
            w.moveTo(ow.x(), ow.y(), screen.view.workAreaTop(), screen.view.width(), screen.view.workAreaBottom());
            w.setMinimized(ow.minimized());
            w.setMaximized(ow.maximized());
            w.setWorkspaces(screen.hasWorkspaces() ? ow.workspaces() : WorkspaceSet.only(0));
            screen.windows.add(w);
            /*
             * A kept instance still has everything it had; a fresh one, made because the game itself
             * was closed in between, is handed what the machine remembered it having open.
             */
            if (!restored && !ow.state().isEmpty()) {
                app.restoreState(ow.state());
            }
        }
    }

    /** Whether the machine's window layout has been applied to this desktop instance. */
    private boolean windowsRestored;

    /** The layout the machine was last told about, so only a real change is pushed to it. */
    private String pushedLayout = "";

    /**
     * Tells the machine which programs it has open, whenever that changes. Without this the machine only
     * learned its layout when the desktop closed, so anything reading its memory ledger (the Task Manager
     * above all) saw a computer running nothing while the player had five windows in front of them.
     */
    private void pushWindowsIfChanged() {
        if (!windowsRestored || powerCycling) {
            return;
        }
        final StringBuilder signature = new StringBuilder().append(shownWorkspace).append('|');
        for (final DesktopWindow w : windows) {
            if (!w.dialog()) {
                signature.append(w.appKey()).append(w.minimized() ? '-' : '+').append(w.workspaces()).append(';');
            }
        }
        final String now = signature.toString();
        if (now.equals(pushedLayout)) {
            return;
        }
        pushedLayout = now;
        PacketDistributor.sendToServer(
                DesktopWindowsPayload.of(host, snapshotWindows(), shownWorkspace));
    }

    /**
     * Set when this desktop is closing because the player shut the machine down or restarted it, so the
     * layout is NOT handed back to the machine on the way out: the server has just cleared it, and a
     * late arrival would resurrect windows on a machine that is off or rebooting.
     */
    private boolean powerCycling;

    /**
     * The current windows as the machine should remember them: floating bounds plus their state. A
     * dialog is a question in flight, not something a machine has open, so it is not remembered.
     */
    private List<OpenWindow> snapshotWindows() {
        final List<OpenWindow> out =
                new ArrayList<>(windows.size());
        for (final DesktopWindow w : windows) {
            // A Σ# program's window is the program's, not the desktop's: the machine says what it has.
            if (!w.dialog() && !(w.app() instanceof SigmaWindowApp)) {
                out.add(new OpenWindow(
                        w.appKey(), w.floatX(), w.floatY(), w.floatW(), w.floatH(), w.minimized(), w.maximized(),
                        w.app().saveState(), w.workspaces()));
            }
        }
        return out;
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
            active.openApp(SetupApp.KEY, app);
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
        active.defaultApps.clear();
        active.defaultApps.putAll(payload.defaultApps());
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
        takePendingRequests();
        pushWindowsIfChanged();
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
            MotifChrome.backdrop(g, sw, sh, prefs.cdePalette(), prefs.cdeStyle().backdrop(shownWorkspace));
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
            cdeWindowIcons.render(g, putAwayHere(), sw, view.workAreaTop(), prefs.cdePalette());
        }
        g.pose().popPose();

        renderWindows(g, lmx, lmy, partialTick, sw, sh);

        final int tbY = sh - view.panelBand();
        renderPanelLayer(g, tbY, sw, sh, lmx, lmy);
        renderMenus(g, tbY, lmx, lmy, partialTick);
        renderDragFeedback(g, sw, tbY, perCol);
        renderBand(g);

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
        final DesktopWindow front = frontWindow();
        for (int i = 0; i < windows.size(); i++) {
            final DesktopWindow w = windows.get(i);
            // A window on a workspace that is not up is not drawn at all, which is what makes them cost nothing.
            if (away(w)) {
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

    /** What an icon being dragged shows: the cell it would land on, and its name trailing the cursor. */
    private void renderDragFeedback(final GuiGraphics g, final int sw, final int tbY, final int perCol) {
        if (!deskDragging || deskDragSlot < 0) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.DRAG);
        /*
         * While dragging an icon to a free spot (not onto a folder), outline the grid cell it would snap to.
         * Suppressed over a folder (the green folder outline wins) or off the wallpaper, where the drop is a
         * no-op.
         */
        if (iconGrid.slotAt(deskDragX, deskDragY, perCol) < 0
                && deskDragX < sw && deskDragY < tbY && overWallpaper(deskDragX, deskDragY)) {
            iconGrid.drawDropCell(g, iconGrid.cellAt(deskDragX, deskDragY, perCol));
        }
        if (deskDragSlot < catalogue.icons().size() + desktopItems.size()) {
            final String label = deskDragSlot < catalogue.icons().size()
                    ? catalogue.icons().get(deskDragSlot).label()
                    : DesktopIcons.baseName(desktopItems.get(deskDragSlot - catalogue.icons().size()).path());
            final int gx = (int) deskDragX + 6;
            final int gy = (int) deskDragY + 2;
            final DesktopShellPalette.Colours c = DesktopShellPalette.get();
            g.fill(gx, gy, gx + font.width(label) + 6, gy + 12, c.ghostFill());
            g.drawString(font, label, gx + 3, gy + 2, c.ghostInk(), false);
        }
        g.pose().popPose();
    }

    /**
     * The rubber band, over the wallpaper and its icons: a translucent fill with a solid outline, the way
     * every desktop draws one.
     */
    private void renderBand(final GuiGraphics g) {
        if (!bandActive) {
            return;
        }
        final int[] r = bandRect();
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.ICONS + 1);
        final DesktopShellPalette.Colours c = DesktopShellPalette.get();
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], c.bandFill());
        g.fill(r[0], r[1], r[0] + r[2], r[1] + 1, c.bandEdge());
        g.fill(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], c.bandEdge());
        g.fill(r[0], r[1], r[0] + 1, r[1] + r[3], c.bandEdge());
        g.fill(r[0] + r[2] - 1, r[1], r[0] + r[2], r[1] + r[3], c.bandEdge());
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
        final DesktopWindow tooltipWin = frontWindow();
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
        final DesktopWindow modalWin = frontWindow();
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

    /**
     * Carries out what other screens and programs have asked of this desktop since the last frame: windows a
     * machine's own programs have opened or closed, files to open, things to type at the prompt, and windows
     * the Task Manager has ended.
     *
     * <p>They arrive as requests rather than as calls because whoever asks is usually not on the render
     * thread and often is not a screen at all. Draining them here, once at the top of a frame, is what keeps
     * a window from being opened halfway through the frame that draws it.
     */
    private void takePendingRequests() {
        // Draw whatever the machine says its programs have open, opening and closing as it says.
        if (!PENDING_UI.isEmpty()) {
            for (final var payload : PENDING_UI) {
                acceptProgramWindow(payload);
            }
            PENDING_UI.clear();
        }
        if (!PENDING_OPEN.isEmpty()) {
            for (final String key : PENDING_OPEN) {
                runOpenRequest(key);
            }
            PENDING_OPEN.clear();
        }
        /*
         * A request to end a window (the Task Manager) closes the newest of that program, so ending a
         * repeated program closes the one on top rather than the oldest copy of it.
         */
        if (!PENDING_CLOSE.isEmpty()) {
            for (final String asked : PENDING_CLOSE) {
                final String key = keyFor(asked);
                for (int i = windows.size() - 1; i >= 0; i--) {
                    final DesktopWindow w = windows.get(i);
                    if (!w.dialog() && w.appKey().equals(key)) {
                        closeWindow(w);
                        break;
                    }
                }
            }
            PENDING_CLOSE.clear();
        }
    }

    /**
     * One request to open something. Most are a program's name, but a few carry what to open it on: a folder
     * for the explorer, a file with or without the program to open it in, a line to run or to type at the
     * prompt, or a file whose Properties to show.
     */
    private void runOpenRequest(final String key) {
        if (key.startsWith(OPEN_FILES_AT)) {
            // This PC asked for a drive or a folder to be opened in the explorer.
            if (memory.allowOpen(FILES_KEY)) {
                openApp(FILES_KEY, new FilesApp(host, desktopId.getPath(),
                        key.substring(OPEN_FILES_AT.length()), monitorPos));
            }
            return;
        }
        if (key.startsWith(RUN_AT_TERMINAL)) {
            runAtTerminal(key.substring(RUN_AT_TERMINAL.length()));
            return;
        }
        if (key.startsWith(OPEN_PROPS)) {
            // A desktop icon's Properties: the explorer on the desktop's folder shows the window.
            final String path = key.substring(OPEN_PROPS.length());
            if (memory.allowOpen(FILES_KEY)) {
                final FilesApp files = new FilesApp(host, desktopId.getPath(), desktopDir, monitorPos);
                files.showPropertiesFor(FsPaths.fileName(path));
                openApp(FILES_KEY, files);
            }
            return;
        }
        if (key.startsWith(TYPE_AT_TERMINAL)) {
            typeAtTerminal(List.of(key.substring(TYPE_AT_TERMINAL.length()).split("\n")));
            return;
        }
        if (key.startsWith(OPEN_FILE)) {
            // A window asked for a file to be opened, in a program it named or in the default one.
            final String rest = key.substring(OPEN_FILE.length());
            final int split = rest.indexOf('\0');
            final String programId = rest.substring(0, split);
            final String path = rest.substring(split + 1);
            if (programId.isEmpty()) {
                openFile(path);
            } else {
                openIn(programId, path);
            }
            return;
        }
        if (key.startsWith(CHOOSE_OPENER)) {
            chooseOpener(key.substring(CHOOSE_OPENER.length()));
            return;
        }
        // A plain program: by its window key, or by the name a player typed for it at a shell.
        final String program = keyFor(key);
        final IDesktopApp app = factoryFor(program);
        if (app != null && memory.allowOpen(program)) {
            openApp(program, app);
        }
    }

    /**
     * Resolves a dropped desktop icon at desktop-local point ({@code dx},{@code dy}). In priority order:
     * dropping onto an open Files explorer moves the file/folder into the folder that window shows; dropping
     * onto a desktop folder moves it inside; and dropping on the bare wallpaper pins the icon to that grid
     * cell (free positioning) and persists the spot. {@code .dat} projections cannot be moved by hand, and any
     * move attempt raises the locked-file dialog instead, leaving the item where it is. Launchers have no
     * underlying file, so for them only the pin-to-cell path applies.
     */
    private void handleDeskDrop(final double dx, final double dy) {
        final int total = catalogue.icons().size() + desktopItems.size();
        if (deskDragSlot < 0 || deskDragSlot >= total) {
            return;
        }
        final boolean isLauncher = deskDragSlot < catalogue.icons().size();
        final DiskFilesPayload.WireFile src =
                isLauncher ? null : desktopItems.get(deskDragSlot - catalogue.icons().size());

        // (0) Dropped on the trash, its icon or CDE's control for it: the file is deleted.
        if (src != null && overTrash(dx, dy)) {
            if (src.readOnly()) {
                notices.datLocked();
            } else {
                clearMovedIconCell(src);
                DeskTrash.delete(host, List.of(src.path()));
            }
            return;
        }

        // (1) Drop onto an open Files explorer window: move the file into the folder it is showing.
        final DesktopWindow explorer = explorerWindowAt(dx, dy);
        if (explorer != null && explorer.app() instanceof FilesApp files && src != null) {
            final String destDir = files.crossWindowDropDir(explorer, dx, dy);
            if (destDir != null && !samePathParent(src.path(), destDir)) {
                if (src.readOnly()) {
                    notices.datLocked();
                } else {
                    PacketDistributor.sendToServer(new MoveFilePayload(host, src.path(), destDir));
                    clearMovedIconCell(src);
                    files.refresh();
                    requestDesktop();
                }
            }
            return;
        }

        // (2) Drop onto a desktop folder icon: move the file inside it.
        final int perCol = iconGrid.perColumn();
        final int target = iconGrid.slotAt(dx, dy, perCol);
        if (target >= catalogue.icons().size() && target != deskDragSlot && src != null) {
            final DiskFilesPayload.WireFile dst = desktopItems.get(target - catalogue.icons().size());
            if (dst.directory()) {
                if (src.readOnly()) {
                    notices.datLocked();
                } else {
                    PacketDistributor.sendToServer(new MoveFilePayload(host, src.path(), dst.path()));
                    clearMovedIconCell(src);
                    requestDesktop();
                }
                return;
            }
        }

        /*
         * (3) Drop on the bare wallpaper: pin the icon to the grid cell under the cursor and persist it,
         * unless that cell already holds another icon (so two icons never stack on the same spot).
         */
        if (dy >= view.workAreaTop() && dy < view.workAreaBottom() && overWallpaper(dx, dy)) {
            final int cell = iconGrid.cellAt(dx, dy, perCol);
            if (iconGrid.cellTaken(cell, deskDragSlot, perCol)) {
                return; // the target cell is occupied; leave the icon where it was
            }
            final String key = iconGrid.keyOf(deskDragSlot);
            iconGrid.pin(key, cell);
            PacketDistributor.sendToServer(new SetIconPositionPayload(host, key, cell));
        }
    }

    /** Whether a desktop point is on the trash: its icon on the wallpaper, or CDE's control for it on the panel. */
    private boolean overTrash(final double dx, final double dy) {
        if (is(PanelStyle.CDE)) {
            return CdeFrontPanelLayout.controlAt(dx, dy, view.width(), view.height())
                    == CdeFrontPanelLayout.Control.TRASH;
        }
        return isTrashIcon(iconGrid.slotAt(dx, dy, iconGrid.perColumn())) && overWallpaper(dx, dy);
    }

    /** Whether {@code destDir} is already the parent folder of {@code srcPath} (a no-op move). */
    private static boolean samePathParent(final String srcPath, final String destDir) {
        final int slash = srcPath.lastIndexOf('/');
        final String parent = slash < 0 ? "" : srcPath.substring(0, slash);
        return parent.equals(destDir);
    }

    /** Forgets a desktop icon's pinned cell once its file has left the desktop folder (moved away). */
    private void clearMovedIconCell(final DiskFilesPayload.WireFile src) {
        iconGrid.forget(src.path());
    }

    /**
     * Handles a file dragged out of the front Files explorer and released over the bare desktop or over a
     * different explorer window: it moves the file into the destination folder (the desktop folder, or the
     * other explorer's open folder). Returns {@code true} when it consumed the drop, so the origin explorer's
     * own in-window drop logic is skipped. A {@code .dat} cannot be moved this way; it raises the locked
     * dialog instead. Returns {@code false} when the front window is not a dragging explorer or the drop
     * lands back inside the origin window (let the app handle it).
     */
    private boolean handleExplorerDropToDesktop(final double dx, final double dy) {
        final DesktopWindow front = frontWindow();
        if (front == null || !(front.app() instanceof FilesApp origin) || !origin.isDragging()) {
            return false;
        }
        final DiskFilesPayload.WireFile dragged = origin.draggedFile();
        if (dragged == null) {
            return false;
        }
        // A drop landing inside the origin explorer is its own business (move into a subfolder, onto media).
        final boolean insideOrigin = dx >= front.x() && dx <= front.x() + front.width()
                && dy >= front.y() && dy <= front.y() + front.height();
        if (insideOrigin) {
            return false;
        }
        // Dropped on the trash: deleted, as a delete from the explorer's own menu would.
        if (overTrash(dx, dy)) {
            if (dragged.readOnly()) {
                notices.datLocked();
            } else {
                DeskTrash.delete(host, List.of(dragged.path()));
            }
            origin.cancelDrag();
            return true;
        }
        // Dropped onto a different explorer window: move into the folder that window shows.
        final DesktopWindow otherExplorer = explorerWindowAt(dx, dy);
        if (otherExplorer != null && otherExplorer != front
                && otherExplorer.app() instanceof FilesApp dest) {
            final String destDir = dest.crossWindowDropDir(otherExplorer, dx, dy);
            moveExplorerFile(origin, dest, dragged, destDir);
            origin.cancelDrag();
            return true;
        }
        // Dropped on the bare wallpaper: move it into the desktop folder.
        if (dy >= view.workAreaTop() && dy < view.workAreaBottom() && overWallpaper(dx, dy)) {
            moveExplorerFile(origin, null, dragged, desktopDir);
            origin.cancelDrag();
            return true;
        }
        return false;
    }

    /**
     * Emits the move of {@code dragged} (from {@code origin}) into {@code destDir}, refreshing the source
     * explorer, an optional destination explorer, and the desktop icons. A {@code .dat} or a no-op move
     * (already in that folder) does nothing but show the locked dialog where appropriate.
     */
    private void moveExplorerFile(final FilesApp origin, @Nullable final FilesApp dest,
                                  final DiskFilesPayload.WireFile dragged, final String destDir) {
        if (destDir == null || samePathParent(dragged.path(), destDir)) {
            return;
        }
        if (dragged.readOnly()) {
            notices.datLocked();
            return;
        }
        PacketDistributor.sendToServer(new MoveFilePayload(host, dragged.path(), destDir));
        origin.refresh();
        if (dest != null) {
            dest.refresh();
        }
        requestDesktop();
    }

    /** Opens an icon slot: a launcher starts its program; a desktop file/folder opens or navigates. */
    void openSlot(final int slot) {
        if (slot < catalogue.icons().size()) {
            final Launcher launcher = catalogue.icons().get(slot);
            if (trash.is(launcher)) {
                trash.open();
            } else {
                runLauncher(launcher);
            }
            return;
        }
        final int di = slot - catalogue.icons().size();
        if (di < 0 || di >= desktopItems.size()) {
            return;
        }
        final DiskFilesPayload.WireFile f = desktopItems.get(di);
        if (f.directory()) {
            openApp(FILES_KEY, new FilesApp(host, desktopId.getPath(), f.path(), monitorPos));
            return;
        }
        openFile(f.path());
    }

    /**
     * Opens a file the way a double-click does: in the program chosen with Always for its extension on this
     * computer, else in the one that opens its kind. A file of a kind nothing here knows, and that no language
     * runs, asks the player which program to use.
     */
    private void openFile(final String path) {
        final String program =
                FileOpeners.defaultFor(path, catalogue.installed(), defaultApps);
        if (program.isEmpty() && FileOpeners.isUnknownKind(path)
                && !runsAsProgram(path)) {
            chooseOpener(path);
            return;
        }
        openIn(program, path);
    }

    /** Whether a language on this computer runs files like this one, so opening it means running it. */
    private static boolean runsAsProgram(final String path) {
        final String extension = FileOpeners.extensionOf(path);
        return !extension.isEmpty() && JsCore.languages().runnerOf(extension) != null;
    }

    /**
     * Asks the player which program opens a file, with the Open with chooser over the whole desktop. Always makes
     * the choice this computer's program for the file's extension, written down on the machine.
     */
    void chooseOpener(final String path) {
        final List<String> programs = FileOpeners.choices(path, catalogue.installed());
        if (programs.isEmpty()) {
            cannotOpen(path);
            return;
        }
        final String extension = FileOpeners.extensionOf(path);
        final String current =
                FileOpeners.defaultFor(path, catalogue.installed(), defaultApps);
        final String opener = current.isEmpty() ? "" : openerName(current);
        final String iconSet = prefs.skin().iconSet();
        notices.show(new OpenWithPopup(path, extension, programs, opener, iconSet, font, (program, always) -> {
            if (always) {
                defaultApps.put(extension, program);
                PacketDistributor.sendToServer(
                        new SetSettingPayload(host, "defaultapp:" + extension,
                                program));
            }
            openIn(program, path);
        }));
    }

    /**
     * Opens a file in a named program, or says why it cannot be opened at all.
     *
     * <p>Which program a kind of file belongs to is one answer, kept in one place, so a double-click, a
     * pick from "Open with" and a run from the explorer all reach the same one.
     */
    void openIn(final String programId, final String path) {
        if (programId.isEmpty()) {
            /*
             * Nothing here claims the kind, but a language an addon brought may: its compiled programs
             * have an extension of their own, and opening one of those means running it.
             */
            if (runsAsProgram(path)) {
                runAtTerminal(path);
                return;
            }
            cannotOpen(path);
            return;
        }
        if (programId.equals(FileOpeners.EDITOR)) {
            final EditorApp editor = new EditorApp(host);
            openApp(EDITOR_KEY, editor);
            editor.openFile(path);
            return;
        }
        if (programId.equals(FileOpeners.RUNTIME)) {
            // A compiled program is run, not read: it gets this desktop's terminal and prints into it.
            runAtTerminal(path);
            return;
        }
        final ProgramSpec spec = Programs.get(
                ResourceLocation.fromNamespaceAndPath("jsc", programId));
        final Launcher launcher = spec == null ? null : catalogue.byProgram(spec.id());
        if (launcher == null) {
            cannotOpen(path);
            return;
        }
        runLauncher(launcher);
        /*
         * The window exists once the launcher has run, so the file goes to it straight away. An app that
         * opens no files ignores this, which is what lets any program be picked without a special case.
         */
        final DesktopWindow opened = windowFor(launcher.key());
        if (opened != null) {
            opened.app().openFile(path);
        }
    }

    /**
     * Runs a program at this desktop's terminal, whatever this desktop calls it, as if the command had
     * been typed there.
     */
    private void runAtTerminal(final String path) {
        final ShellApp shell = terminalApp();
        if (shell != null) {
            shell.runProgram(path);
        }
    }

    /** Types lines at this desktop's terminal, one after the other. */
    private void typeAtTerminal(final List<String> lines) {
        final ShellApp shell = terminalApp();
        if (shell != null) {
            shell.typeLines(lines);
        }
    }

    /**
     * This desktop's terminal window, brought forward, or opened when there is none: a program that
     * needs the prompt gets the one that is up rather than a second one beside it.
     */
    @Nullable
    private ShellApp terminalApp() {
        final Launcher launcher = catalogue.byProgram(Programs.COMMAND_PROMPT);
        if (launcher == null) {
            return null;
        }
        final String terminal = launcher.key();
        final DesktopWindow open = windowFor(terminal);
        if (open != null && open.app() instanceof ShellApp shell) {
            open.setMinimized(false);
            bringToFront(windows.indexOf(open));
            return shell;
        }
        final IDesktopApp made = factoryFor(terminal);
        if (made instanceof ShellApp shell && memory.allowOpen(terminal)) {
            openApp(terminal, shell);
            return shell;
        }
        return null;
    }

    /** A queued request to type lines at the terminal, the lines joined by newlines. */
    private static final String TYPE_AT_TERMINAL = "Type\0";

    /** Lets a running app hand the shell a job: lines typed at the terminal, one after the other. */
    public static void requestTypeAtTerminal(final List<String> lines) {
        PENDING_OPEN.add(TYPE_AT_TERMINAL + String.join("\n", lines));
    }

    /** Opens the Settings window on one of its pages, the way a menu entry names a page rather than the program. */
    void openSettingsPage(final int page) {
        if (memory.allowOpen(SETTINGS_KEY)) {
            openApp(SETTINGS_KEY, new SettingsApp(host, monitorPos).showPage(page));
        }
    }

    /** Runs the launcher known by {@code key}, when the desktop has one. */
    void runLauncherKeyed(final String key) {
        final Launcher launcher = catalogue.byKey(key);
        if (launcher != null) {
            runLauncher(launcher);
        }
    }

    /** Puts every window away, which is what Show desktop on the panel's menu does. */
    void showDesktop() {
        for (final DesktopWindow w : windows) {
            w.setMinimized(true);
        }
    }

    /** Steps the open windows down and to the right from the work area's corner, the way a cascade does. */
    void cascadeWindows() {
        int step = 0;
        for (final DesktopWindow w : windows) {
            if (away(w)) {
                continue;
            }
            w.setMaximized(false);
            w.moveTo(16 + step * 12, view.workAreaTop() + 10 + step * 12, view.workAreaTop(), view.width(),
                    view.workAreaBottom());
            step++;
        }
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

    /** A queued request to show a file's Properties window: the explorer opens on its folder and shows it. */
    private static final String OPEN_PROPS = "Props\0";

    /** Asks for the Properties window of a file on the desktop, which the explorer knows how to show. */
    public static void requestFileProperties(final String path) {
        PENDING_OPEN.add(OPEN_PROPS + path);
    }

    /** A click on a live balloon: it takes the click, and opens the program it offers when it offers one. */
    private boolean balloonClick(final double mx, final double my, final int tbY, final int sw) {
        final String opens = notices.clickBalloon(mx, my, tbY, sw);
        if (opens == null) {
            return false;
        }
        if (!opens.isEmpty()) {
            final IDesktopApp app = factoryFor(opens);
            if (app != null) {
                openApp(opens, app);
            }
        }
        return true;
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
    private boolean startButtonHit(final double mx, final double my, final int tbY) {
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

    /* The windows of one program */

    /** Every window listed under {@code key}, back to front, a program's own and its dialogs alike. */
    private List<DesktopWindow> groupWindows(final String key) {
        final List<DesktopWindow> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (w.groupKey().equals(key)) {
                out.add(w);
            }
        }
        return out;
    }

    /** The dialog up over {@code w}, or null: while there is one, {@code w} takes nothing itself. */
    @Nullable
    private DesktopWindow dialogOf(final DesktopWindow w) {
        for (final DesktopWindow other : windows) {
            if (other.owner() == w) {
                return other;
            }
        }
        return null;
    }

    /** Puts {@code w} in front, and its dialogs in front of it, in the order they were opened. */
    private void bringWindowToFront(final DesktopWindow w) {
        if (!windows.remove(w)) {
            return;
        }
        windows.add(w);
        final List<DesktopWindow> dialogs = new ArrayList<>();
        for (final DesktopWindow other : windows) {
            if (other.owner() == w) {
                dialogs.add(other);
            }
        }
        for (final DesktopWindow dialog : dialogs) {
            bringWindowToFront(dialog);
        }
    }

    /** Brings {@code w} up and forward: a dialog comes with the window it belongs to. */
    private void focusWindow(final DesktopWindow w) {
        final DesktopWindow root = w.owner() != null ? w.owner() : w;
        // A window asked for by name from another workspace takes the desktop there, as CDE did.
        if (!root.on(shownWorkspace)) {
            shownWorkspace = WorkspaceSet.first(root.workspaces());
        }
        for (final DesktopWindow other : groupWindows(root.groupKey())) {
            if (other == root || other.owner() == root) {
                other.setMinimized(false);
            }
        }
        bringWindowToFront(root);
        if (w != root) {
            bringWindowToFront(w);
        }
    }

    void bringGroupToFront(final String key) {
        for (final DesktopWindow w : groupWindows(key)) {
            if (!w.dialog()) {
                bringWindowToFront(w);
            }
        }
    }

    void restoreGroup(final String key) {
        for (final DesktopWindow w : groupWindows(key)) {
            w.setMinimized(false);
        }
        bringGroupToFront(key);
    }

    void minimizeGroup(final String key) {
        for (final DesktopWindow w : groupWindows(key)) {
            w.setMinimized(true);
        }
    }

    void minimizeOthers(final String key) {
        for (final DesktopWindow w : windows) {
            w.setMinimized(!w.groupKey().equals(key));
        }
        bringGroupToFront(key);
    }

    private void closeGroup(final String key) {
        final List<DesktopWindow> mine = groupWindows(key);
        for (int i = mine.size() - 1; i >= 0; i--) {
            if (windows.contains(mine.get(i))) {
                closeWindow(mine.get(i));
            }
        }
    }

    /** Ends {@code w}: its dialogs go first, since a window put away takes its questions with it. */
    private void closeWindow(final DesktopWindow w) {
        for (final DesktopWindow other : new ArrayList<>(windows)) {
            if (other.owner() == w) {
                closeWindow(other);
            }
        }
        if (windows.remove(w)) {
            w.app().onClosed();
        }
    }

    /** Ends the window running {@code app}, when there is one. */
    private void closeWindowOf(final IDesktopApp app) {
        for (final DesktopWindow w : new ArrayList<>(windows)) {
            if (w.app() == app) {
                closeWindow(w);
            }
        }
    }

    /**
     * Opens {@code dialog} as a window over the one running {@code ownerApp}, centred on it and in front
     * of it. The owner comes forward first, so the pair reads as one program that just asked something.
     */
    private void openDialog(final IDesktopApp ownerApp, final IDesktopApp dialog) {
        DesktopWindow ownerWin = null;
        for (final DesktopWindow w : windows) {
            if (w.app() == ownerApp) {
                ownerWin = w;
            } else if (w.app() == dialog) {
                focusWindow(w);
                return;
            }
        }
        if (ownerWin == null) {
            return;
        }
        final int top = view.workAreaTop();
        final int w = Math.max(dialog.minWidth(), Math.min(dialog.defaultWidth(), view.width() - 8));
        final int h = Math.max(dialog.minHeight(), Math.min(dialog.defaultHeight(), view.workAreaBottom() - top - 8));
        /*
         * Centred across the owner and hung just under its title bar, so the owner's name and edges stay
         * in view around the question it is asking, and the two read as two windows rather than one.
         */
        final int x = Math.max(0, Math.min(ownerWin.x() + (ownerWin.width() - w) / 2, view.width() - w));
        final int y = Math.max(top, Math.min(ownerWin.y() + DesktopWindow.TITLE_H + 6, view.workAreaBottom() - h));
        dialog.applySkin(prefs.skin());
        final DesktopWindow made = new DesktopWindow(dialog, ownerWin.appKey(), x, y, w, h);
        made.setOwner(ownerWin);
        made.setWorkspaces(ownerWin.workspaces());
        ownerWin.setMinimized(false);
        bringWindowToFront(ownerWin);
        windows.add(made);
    }

    /**
     * Tells the machine what the power dialog chose. It is going down, restarting or being left: the desktop closing
     * after this must not hand its windows back to a machine whose session has just ended.
     */
    void cyclePower(final int action) {
        powerCycling = true;
        PacketDistributor.sendToServer(new MachinePowerPayload(host, monitorPos, action));
    }

    /** How many programs are open, on every workspace; a dialog is a question a program asks, not a program. */
    int openPrograms() {
        int open = 0;
        for (final DesktopWindow w : windows) {
            if (!w.dialog()) {
                open++;
            }
        }
        return open;
    }

    /**
     * A click travels down the desktop one layer at a time: whatever is modal takes it first, then the
     * panel, then the windows, then the wallpaper and its icons, and only what nothing claimed reaches the
     * container underneath. Each layer below says whether it took the click, so the order they are tried in
     * is the whole of the routing and reads in one place.
     */
    @Override
    public boolean mouseClicked(final double mouseXAbs, final double mouseYAbs, final int button) {
        if (memory.crashing()) {
            return true; // the crash screen swallows input until the reboot completes
        }
        if (clickedOverlay(mouseXAbs, mouseYAbs, button)) {
            return true;
        }
        final double mouseX = view.localX(mouseXAbs);
        final double mouseY = view.localY(mouseYAbs);
        final int tbY = view.height() - view.panelBand();
        if (clickedPanel(mouseX, mouseY, button, tbY)) {
            return true;
        }
        final Click inWindow = clickedWindow(mouseXAbs, mouseYAbs, mouseX, mouseY, button);
        if (inWindow == Click.TAKEN) {
            return true;
        }
        if (inWindow == Click.CONTAINER || clickedDesktop(mouseX, mouseY, button) == Click.CONTAINER) {
            return super.mouseClicked(view.slotX(mouseXAbs), view.slotY(mouseYAbs), button);
        }
        return true;
    }

    /** What a layer did with a click: took it, left it for the next one, or handed it to the container. */
    private enum Click {
        TAKEN,
        PASSED,
        CONTAINER
    }

    /**
     * Whatever is over everything else: the power dialog, an open program menu, the panel's popup, a modal
     * dialog, and a window holding a dialog of its own. Each of these is modal in its own way, so a click
     * reaching one goes no further down.
     */
    private boolean clickedOverlay(final double mouseXAbs, final double mouseYAbs, final int button) {
        /*
         * The power dialog is modal: it decides the fate of the whole machine, so nothing behind it
         * takes the click. An open program menu takes the next click the same way, and the panel's
         * popup answers a click on it or is put away by one anywhere else.
         */
        if (power.click(view.localX(mouseXAbs), view.localY(mouseYAbs))) {
            return true;
        }
        // The volume control takes the next click like a menu: on it, it turns what it lands on; anywhere else it goes.
        if (volumePopup.isOpen()) {
            volumePopup.mouseClicked(view.localX(mouseXAbs), view.localY(mouseYAbs), button);
            return true;
        }
        if (taskbar.menu().isOpen()) {
            taskbar.menu().mouseClicked(view.localX(mouseXAbs), view.localY(mouseYAbs), button);
            return true;
        }
        // A window's own menu on CDE takes the click too, unless it is on the very button the menu hangs from.
        if (cdeWindowMenu.isOpen() && cdeWindowMenu.clicked(view.localX(mouseXAbs), view.localY(mouseYAbs))) {
            return true;
        }
        if (taskPopup.key() != null && taskPopup.click(view.localX(mouseXAbs), view.localY(mouseYAbs), button)) {
            return true;
        }
        // A modal dialog swallows every click; only its OK button dismisses it, and a click beside it rings the bell.
        if (notices.clickPopup(view.localX(mouseXAbs), view.localY(mouseYAbs), button)) {
            return true;
        }
        /*
         * An app-level modal dialog isolates its window: route the click to it and to nothing behind it
         * (inventory slots, other windows, the taskbar), just like the desktop popup above.
         */
        if (focusModal()) {
            final DesktopWindow f = frontWindow();
            if (f != null) {
                f.app().mouseClicked(f, view.localX(mouseXAbs), view.localY(mouseYAbs), button);
            }
            return true;
        }
        return false;
    }

    /**
     * The panel and everything that belongs to it: a balloon over it, its own menu, the Start button and an
     * open launcher, and the row of program entries. The bar swallows any click that lands on it and misses
     * all of those, so nothing underneath ever reacts to a click on the panel.
     */
    private boolean clickedPanel(final double mouseX, final double mouseY, final int button, final int tbY) {
        // A balloon is dismissed by clicking it, and it swallows that click so nothing under it reacts.
        if (balloonClick(mouseX, mouseY, tbY, view.width())) {
            return true;
        }

        /*
         * The panel's menu takes the next click wherever it lands: on an entry it runs it, anywhere else it
         * just closes, which is what a menu does.
         */
        if (panelMenu.click(mouseX, mouseY)) {
            return true;
        }

        // The speaker on the panel: the left button opens the volume control, the right one its menu.
        if (!is(PanelStyle.CDE)
                && tray.onSpeaker(mouseX, mouseY, view.width(), view.panelOnTop() ? 0 : tbY, view.panelOnTop())) {
            if (button == 1) {
                volumePopup.openMenu((int) mouseX, tbY, view.panelOnTop());
            } else if (button == 0) {
                volumePopup.toggle();
            }
            return true;
        }

        /*
         * GNOME: the top bar's Activities corner toggles the overview; the rest of the bar is inert.
         * Only while the bar IS at the top: a period GNOME panels at the bottom, and swallowing clicks
         * along the top edge there ate the title bars of every window parked up there.
         */
        if (view.panelOnTop() && mouseY < TASKBAR_H) {
            if (mouseX < 64) {
                start.toggle();
            } else if (button == 1) {
                panelMenu.open((int) mouseX, 0);
            }
            return true;
        }
        /*
         * CDE: the Front Panel answers for itself, and so does the subpanel standing on it. There is no Start
         * button and no row of open programs to test, and the band either side of the slab is plain desktop.
         */
        if (is(PanelStyle.CDE)) {
            // A click beside a subpanel leaves it up: only its own arrow puts it away again.
            if (button == 0 && cdeLaunchers.click(mouseX, mouseY, view.width(), view.height())) {
                return true;
            }
            // The right button on the slab opens the panel's own menu, where the Task Manager has always been.
            if (button == 1 && CdeFrontPanelLayout.panel(view.width(), view.height()).holds(mouseX, mouseY)) {
                panelMenu.open((int) mouseX, tbY);
                return true;
            }
            return cdePanels.click(mouseX, mouseY, view.width(), view.height());
        }
        // Windows 11 keeps Start with the centered group, so it has its own hit test.
        if (is(PanelStyle.FRAMES_11) && mouseY >= tbY) {
            final int startX = taskbar.modernStartLeft(view.width());
            if (mouseX >= startX && mouseX < startX + WIN11_SLOT) {
                start.toggle();
                return true;
            }
        } else if (startButtonHit(mouseX, mouseY, tbY)) {
            start.toggle();
            return true;
        }
        // An open launcher takes a click on it; one anywhere else closes it and goes on below.
        if (start.click(mouseX, mouseY, button, tbY)) {
            return true;
        }

        /*
         * The panel's entries, one per program: the left button brings its window up or down, or lists
         * its windows when it has several; the right button opens the program's own menu, so a program
         * can be closed or pinned without first going to it. A right-click on the panel itself, clear of
         * Start and of the entries, opens the panel's own menu, the way every one of these desktops offers
         * it; the Task Manager is one entry on that menu. The rest of the bar is the bar and swallows the
         * click, so nothing under it reacts.
         */
        if (!view.panelOnTop() && mouseY >= tbY) {
            final TaskStrip strip = taskbar.strip(view.width());
            int idx = strip.indexAt(mouseX);
            int atX = idx >= 0 ? strip.x()[idx] : 0;
            if (idx < 0 && strip.quickCount() > 0) {
                idx = strip.quickEntryAt(mouseX);
                atX = idx >= 0 ? strip.quickX() + strip.quickIndexOf(idx) * TaskStrip.QL_W : 0;
            }
            if (idx >= 0) {
                taskbar.click(strip.entries().get(idx), atX, button, tbY);
                return true;
            }
            if (button == 1) {
                panelMenu.open((int) mouseX, tbY);
            }
            return true;
        }
        return false;
    }

    /**
     * The open windows, front to back: a title-bar button, a resize edge, the bar itself, or the body. A
     * window with a dialog up takes nothing itself, and a click on the front window's inventory band is a
     * real container click rather than anything the desktop should answer.
     */
    private Click clickedWindow(final double mouseXAbs, final double mouseYAbs,
                                final double mouseX, final double mouseY, final int button) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            final DesktopWindow w = windows.get(i);
            if (away(w)) {
                continue;
            }
            /*
             * A window with a dialog up takes nothing itself: a click on it brings the dialog forward,
             * the way every desktop answers a click on a program that is waiting for its own question.
             */
            final DesktopWindow held = dialogOf(w);
            if (held != null && mouseX >= w.x() && mouseX <= w.x() + w.width()
                    && mouseY >= w.y() && mouseY <= w.y() + w.height()) {
                focusWindow(held);
                return Click.TAKEN;
            }
            final int titleBtn = w.buttonAt(mouseX, mouseY);
            if (titleBtn != 0) {
                /*
                 * Press the button now; the action fires on release over the same button, so the player
                 * sees the pushed-in feedback of a real click instead of the window reacting instantly.
                 */
                bringToFront(i);
                w.setPressedButton(titleBtn);
                pressedBtnWindow = w;
                return Click.TAKEN;
            }
            final int rdir = w.resizeHitTest(mouseX, mouseY);
            if (rdir != DesktopWindow.RESIZE_NONE) {
                bringToFront(i);
                resizing = w;
                w.beginResize(rdir, mouseX, mouseY);
                return Click.TAKEN;
            }
            if (w.titleBarHit(mouseX, mouseY)) {
                bringToFront(i);
                dragging = w;
                dragOffsetX = (int) mouseX - w.x();
                dragOffsetY = (int) mouseY - w.y();
                return Click.TAKEN;
            }
            if (w.bodyHit(mouseX, mouseY)) {
                bringToFront(i);
                return clickedWindowBody(w, mouseXAbs, mouseYAbs, mouseX, mouseY, button);
            }
        }
        return Click.PASSED;
    }

    /**
     * A click inside a window's body. Most of the time the program itself answers it, but a window carrying
     * the player's inventory has to let the container drive instead, and the Network Interactor takes the
     * ones the container would otherwise turn into a quick-move between slots.
     */
    private Click clickedWindowBody(final DesktopWindow w, final double mouseXAbs, final double mouseYAbs,
                                    final double mouseX, final double mouseY, final int button) {
        /*
         * A click landing on an active inventory slot (only the front inventory-band window has them) is
         * a real container click: let the vanilla container drive the cursor, drag, and shift-click.
         */
        if (w.app() instanceof IInventoryBandApp && !(w.app() instanceof NetworkInteractorApp)
                && !w.app().modalActive() && band.slotAt(mouseXAbs, mouseYAbs) != null) {
            return Click.CONTAINER;
        }
        if (w.app() instanceof NetworkInteractorApp ni) {
            final Click routed = clickedInteractor(ni, w, mouseXAbs, mouseYAbs, mouseX, mouseY, button);
            if (routed != Click.PASSED) {
                return routed;
            }
        }
        w.app().mouseClicked(w, mouseX, mouseY, button);
        return Click.TAKEN;
    }

    /**
     * The Network Interactor's own handling of a click on its window, which is where the desktop hands items
     * between the player and the network. It is here rather than in the program because the desktop, not the
     * container, owns the cursor while a window is open.
     */
    private Click clickedInteractor(final NetworkInteractorApp ni, final DesktopWindow w,
                                    final double mouseXAbs, final double mouseYAbs,
                                    final double mouseX, final double mouseY, final int button) {
        /*
         * Shift-click an inventory slot inserts that whole stack into the network (Network tab) or
         * local storage (Local tab), like MC-NET, instead of the vanilla quick-move between slots.
         */
        if (!ni.hasPopup() && hasShiftDown()) {
            final Slot slot = band.slotAt(mouseXAbs, mouseYAbs);
            final int target = ni.shiftInsertTarget();
            if (slot != null && slot.hasItem() && target >= 0) {
                PacketDistributor.sendToServer(
                        new NiShiftInsertPayload(host, monitorPos, slot.getContainerSlot(), target));
                return Click.TAKEN;
            }
        }
        /*
         * While the request/storage dialog is open it is modal over the window (even over the
         * inventory band) so the app gets the click instead of the vanilla container.
         */
        if (!ni.hasPopup() && band.slotAt(mouseXAbs, mouseYAbs) != null) {
            return Click.CONTAINER;
        }
        /*
         * A held stack dropped on the item grid goes to the network (Network tab) or local storage
         * (Storage tab): the desktop owns the cursor, so it routes the handoff here. Left = the
         * whole stack as items; right = one, or what a held container holds; and a held empty
         * container right-clicked on a fluid or chemical entry fills from it, so the entry under
         * the cursor travels with a right-click.
         */
        if (!ni.hasPopup() && !menu.getCarried().isEmpty() && (button == 0 || button == 1)) {
            final double lx = mouseX - (w.x() + 4);
            final double ly = mouseY - (w.y() + 18);
            final int target = ni.cursorDepositTarget(lx, ly);
            if (target >= 0) {
                PacketDistributor.sendToServer(
                        new NiDepositPayload(host, monitorPos, target, button == 0,
                                button == 1 ? ni.cursorDepositEntry(lx, ly) : Optional.empty()));
                return Click.TAKEN;
            }
        }
        return Click.PASSED;
    }

    /**
     * The wallpaper and its icons, which is where a click lands when nothing above wanted it: the desktop's
     * own menu, an icon picked or opened, a drag armed, or a rubber band begun on bare wallpaper.
     */
    private Click clickedDesktop(final double mouseX, final double mouseY, final int button) {
        // An open desktop context menu takes the click first, and closes on it whatever it landed on.
        if (deskMenu.isOpen()) {
            deskMenu.mouseClicked(mouseX, mouseY, button);
            return Click.TAKEN;
        }
        // A click on the desktop commits any in-progress icon rename.
        if (deskFiles.isRenaming()) {
            deskFiles.commitRename();
        }
        // On CDE a double click on the icon of a window that was put away brings that window back.
        if (is(PanelStyle.CDE) && button == 0
                && cdeWindowIcons.clicked(mouseX, mouseY, putAwayHere(), view.width(), view.workAreaTop())) {
            return Click.TAKEN;
        }
        // The right button on such an icon raises the window's own menu, which is how it is closed from there.
        if (is(PanelStyle.CDE) && button == 1) {
            final DesktopWindow putAway = cdeWindowIcons.at(mouseX, mouseY, putAwayHere(), view.width(),
                    view.workAreaTop());
            if (putAway != null) {
                cdeWindowMenu.openFor(putAway, (int) mouseX, (int) mouseY);
                return Click.TAKEN;
            }
        }

        final int perCol = iconGrid.perColumn();
        final int slot = iconGrid.slotAt(mouseX, mouseY, perCol);

        if (button == 1) {
            // Right-click: the menu of whatever is under the cursor, or the wallpaper's own.
            selectedIcon = slot;
            deskMenu.openFor(slot, (int) mouseX, (int) mouseY);
            return Click.TAKEN;
        }

        if (slot >= 0) {
            // Windows-style: single click selects an icon, a double click opens it.
            final long now = System.currentTimeMillis();
            final boolean dbl = selectedIcon == slot && now - iconClickAt < 300;
            selectedIcon = slot;
            iconClickAt = now;
            /*
             * Arm a drag of any desktop icon (a program launcher as well as a file or folder) so all of
             * them can be freely repositioned (the launcher drag only ever pins to a cell, never moves a file).
             * The drag does not actually begin until the cursor leaves a small dead zone, so a plain click (or
             * a double-click) never turns into an accidental reposition.
             */
            deskDragSlot = slot;
            deskDragging = false;
            deskDragStartX = mouseX;
            deskDragStartY = mouseY;
            if (dbl) {
                openSlot(slot);
                selectedIcon = -1;
            }
            return Click.TAKEN;
        }
        selectedIcon = -1;
        iconGrid.selection().clear();
        /*
         * A click on empty desktop while holding a stack would make the vanilla container throw the item to the
         * world (no slot under the cursor). Swallow it so nothing is ever dropped by clicking the wallpaper.
         */
        if (!menu.getCarried().isEmpty()) {
            return Click.TAKEN;
        }
        // Pressing on bare wallpaper starts a rubber band; the drag handler grows it from here.
        if (button == 0 && mouseY >= view.workAreaTop() && mouseY < view.workAreaBottom()
                && overWallpaper(mouseX, mouseY)) {
            bandActive = true;
            bandStartX = mouseX;
            bandStartY = mouseY;
            bandX = mouseX;
            bandY = mouseY;
        }
        return Click.CONTAINER;
    }

    @Override
    public boolean mouseDragged(final double mouseXAbs, final double mouseYAbs, final int button,
                                final double dx, final double dy) {
        if (notices.popupUp()) {
            return true;
        }
        if (volumePopup.mouseDragged(view.localX(mouseXAbs), view.localY(mouseYAbs))) {
            return true;
        }
        if (dragging != null) {
            dragging.moveTo((int) (view.localX(mouseXAbs)) - dragOffsetX, (int) (view.localY(mouseYAbs)) - dragOffsetY,
                    view.workAreaTop(), view.width(), view.workAreaBottom());
            return true;
        }
        if (resizing != null) {
            resizing.applyResize(view.localX(mouseXAbs), view.localY(mouseYAbs), view.workAreaTop(), view.width(),
                    view.workAreaBottom());
            return true;
        }
        // Dragging a desktop icon across the desktop, once the cursor has left the click dead zone.
        if (deskDragSlot >= 0) {
            deskDragX = view.localX(mouseXAbs);
            deskDragY = view.localY(mouseYAbs);
            if (!deskDragging
                    && (Math.abs(deskDragX - deskDragStartX) > DRAG_THRESHOLD
                        || Math.abs(deskDragY - deskDragStartY) > DRAG_THRESHOLD)) {
                deskDragging = true;
            }
            return true;
        }
        // Sweeping the wallpaper: extend the band and reselect what it now covers.
        if (bandActive) {
            bandX = view.localX(mouseXAbs);
            bandY = view.localY(mouseYAbs);
            iconGrid.selectWithin(bandRect());
            return true;
        }
        /*
         * No window drag/resize in progress. While the front Network Interactor holds a stack on the cursor,
         * a drag is the vanilla "spread across slots" gesture, so hand it to the container, not the app.
         */
        final DesktopWindow w = frontWindow();
        if (w != null && w.app() instanceof IInventoryBandApp && !menu.getCarried().isEmpty()) {
            return super.mouseDragged(view.slotX(mouseXAbs), view.slotY(mouseYAbs), button, dx, dy);
        }
        if (w != null) {
            w.app().mouseDragged(w, view.localX(mouseXAbs), view.localY(mouseYAbs), button);
            return true;
        }
        return super.mouseDragged(view.slotX(mouseXAbs), view.slotY(mouseYAbs), button, dx, dy);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        volumePopup.mouseReleased();
        if (notices.releasePopup(view.localX(mouseX), view.localY(mouseY), button)) {
            return true;
        }
        // Letting go ends the sweep; whatever it covered stays selected.
        if (bandActive) {
            bandActive = false;
            return true;
        }
        /*
         * A title-bar button was pressed on mousedown; fire its action only if released over the same
         * button (dragging off it cancels). Either way, clear the pushed-in state.
         */
        if (pressedBtnWindow != null) {
            final DesktopWindow pb = pressedBtnWindow;
            final int btn = pb.pressedButton();
            pb.setPressedButton(0);
            pressedBtnWindow = null;
            if (btn != DesktopWindow.BUTTON_NONE && pb.buttonAt(view.localX(mouseX), view.localY(mouseY)) == btn) {
                if (btn == DesktopWindow.BUTTON_CLOSE && is(PanelStyle.CDE)) {
                    // Motif's button opens the window's menu, and closes the window on a double click.
                    cdeWindowMenu.pressed(pb);
                } else if (btn == DesktopWindow.BUTTON_CLOSE) {
                    closeWindow(pb);
                } else if (btn == DesktopWindow.BUTTON_MINIMIZE) {
                    pb.setMinimized(true);
                } else if (btn == DesktopWindow.BUTTON_MAXIMIZE) {
                    pb.toggleMaximize();
                }
            }
            return true;
        }
        // A dragged desktop icon: handle the drop (move into a folder / open explorer, or pin to a cell).
        if (deskDragging && deskDragSlot >= 0) {
            handleDeskDrop(view.localX(mouseX), view.localY(mouseY));
        }
        final boolean wasDeskDrag = deskDragging;
        deskDragging = false;
        deskDragSlot = -1;
        /*
         * A file dragged out of a Files explorer and dropped on the bare desktop moves it into the desktop
         * folder. Handled here, before the app sees the release, so the explorer's own in-window drop logic
         * does not also fire. Anything else (a drop staying inside the window, or onto a removable medium)
         * falls through to the app below.
         */
        if (!wasDeskDrag && dragging == null && resizing == null
                && handleExplorerDropToDesktop(view.localX(mouseX), view.localY(mouseY))) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        /*
         * Route the release to the front window's app (for content drag-and-drop) unless this was a
         * desktop-icon drag, and only when no window move/resize is in progress.
         */
        if (!wasDeskDrag && dragging == null && resizing == null) {
            final DesktopWindow w = frontWindow();
            if (w != null) {
                w.app().mouseReleased(w, view.localX(mouseX), view.localY(mouseY), button);
            }
        }
        /*
         * Frames 11 edge snapping: releasing a dragged window against a screen edge tiles it (top = maximize,
         * left/right = that half). A modern-OS gesture the earlier editions do not have.
         */
        if (dragging != null && (is(PanelStyle.FRAMES_11) || linuxDesktop())) {
            final int lx = (int) (view.localX(mouseX));
            final int ly = (int) (view.localY(mouseY));
            final int top = view.workAreaTop();
            final int workH = view.workAreaBottom() - top;
            final int halfW = view.width() / 2;
            if (ly <= top + 4) {
                dragging.setMaximized(true);
            } else if (lx <= 4) {
                dragging.snapTo(0, top, halfW, workH);
            } else if (lx >= view.width() - 4) {
                dragging.snapTo(halfW, top, view.width() - halfW, workH);
            }
        }
        dragging = null;
        resizing = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        if (notices.popupUp()) {
            return true;
        }
        // A desktop-icon rename captures typing before any window, until the name is as long as it may be.
        if (deskFiles.isRenaming() && c >= 32 && c != 127 && c != '/' && c != '\\' && deskFiles.type(c)) {
            return true;
        }
        // An open launcher's search box takes the typing; it is always focused while it is shown.
        if (start.type(c)) {
            return true;
        }
        final DesktopWindow w = frontWindow();
        if (w != null && w.app().charTyped(c)) {
            return true;
        }
        return super.charTyped(c, modifiers);
    }

    /**
     * A desktop takes a key ahead of the rest of the game only when something on it is being typed at: one of
     * its own menus or boxes, or the program in the window in front. Over everything else a key stays whoever's
     * it was, which is what keeps a recipe viewer's keys working on the items a window shows.
     */
    @Override
    public boolean keyFirst(final int key, final int scanCode, final int modifiers) {
        // Motif's keys for a window's menu come before the window's own program, as a window manager's do.
        if (is(PanelStyle.CDE) && !notices.popupUp() && !power.isOpen()
                && cdeWindowMenu.keyPressed(key, modifiers, frontWindow())) {
            return true;
        }
        if (notices.popupUp() || power.isOpen() || deskMenu.isOpen() || taskbar.menu().isOpen()
                || deskFiles.isRenaming()
                || start.isOpen()) {
            return keyPressed(key, scanCode, modifiers);
        }
        final DesktopWindow w = frontWindow();
        return w != null && (key != 256 || w.app().wantsEscape()) && w.app().keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        // A modal dialog swallows every key; Enter or Escape dismisses it, nothing leaks behind it.
        if (notices.keyPopup(key, scanCode, modifiers)) {
            return true;
        }
        if (key == 256 && volumePopup.isOpen()) { // Escape
            volumePopup.close();
            return true;
        }
        // The power dialog decides the fate of the whole machine, so it keeps the keyboard as it keeps the mouse.
        if (power.keyPressed(key)) {
            return true;
        }
        // The desktop's menu and a program's are walked with the arrows and left with Escape, like any menu.
        if (deskMenu.isOpen() && deskMenu.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        if (taskbar.menu().isOpen() && taskbar.menu().keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        if (taskPopup.key() != null && key == 256) {
            taskPopup.dismiss();
            return true;
        }
        // An in-progress desktop-icon rename consumes keys first (Enter commits, Esc cancels).
        if (deskFiles.isRenaming()) {
            switch (key) {
                case 257, 335 -> deskFiles.commitRename();
                case 256 -> deskFiles.cancelRename();
                case 259 -> deskFiles.backspace();
                default -> {
                    return false;
                }
            }
            return true;
        }
        // An open launcher owns the keyboard ahead of the desktop, so Escape closes it rather than the desktop.
        if (start.keyPressed(key)) {
            return true;
        }
        // The front window's app gets first refusal on keys, except ESC which always closes the desktop.
        final DesktopWindow w = frontWindow();
        if (w != null && (key != 256 || w.app().wantsEscape()) && w.app().keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        /*
         * A container screen closes on the inventory key by default; the desktop must NOT, or pressing 'E'
         * would dismiss the whole shell. Swallow that key here.
         */
        if (key == Minecraft.getInstance().options.keyInventory.getKey().getValue()) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    /** A key let go goes to the window in front, for a program that tells a press from a release. */
    @Override
    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        final DesktopWindow w = frontWindow();
        if (!notices.popupUp() && w != null && w.app().keyReleased(key, scanCode, modifiers)) {
            return true;
        }
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double dx, final double dy) {
        if (notices.popupUp()) {
            return true;
        }
        // The wheel over the speaker, or over its open control, turns the volume a step a notch.
        final double lmx = view.localX(mouseX);
        final double lmy = view.localY(mouseY);
        if (dy != 0 && !is(PanelStyle.CDE) && (volumePopup.over(lmx, lmy)
                || tray.onSpeaker(lmx, lmy, view.width(), view.panelOnTop() ? 0 : view.height() - view.panelBand(),
                        view.panelOnTop()))) {
            volumePopup.nudge(dy > 0 ? 1 : -1);
            return true;
        }
        final DesktopWindow w = frontWindow();
        if (w != null && w.app().mouseScrolled(dy)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, dx, dy);
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

    /** The topmost window that is on show, which receives keyboard and scroll input. */
    @Nullable
    DesktopWindow frontWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (!away(windows.get(i))) {
                return windows.get(i);
            }
        }
        return null;
    }

    /** Whether the front window's program has a modal dialog open, which disables everything behind it. */
    boolean focusModal() {
        final DesktopWindow f = frontWindow();
        return f != null && f.app().modalActive();
    }

    /**
     * Whether a desktop-local point lands on the bare wallpaper, not over any open (non-minimized) window
     * body or title bar. Used so a free icon drop only snaps to a cell on the empty desktop, and so a
     * cross-window drag knows the cursor is on the desktop (not a window).
     */
    private boolean overWallpaper(final double mx, final double my) {
        for (final DesktopWindow w : windows) {
            if (!away(w) && mx >= w.x() && mx <= w.x() + w.width()
                    && my >= w.y() && my <= w.y() + w.height()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The topmost open Files-explorer window whose body is under a desktop-local point, or {@code null}.
     * Cross-window drag uses this to decide which open folder a dragged file should move into.
     */
    @Nullable
    private DesktopWindow explorerWindowAt(final double mx, final double my) {
        for (int i = windows.size() - 1; i >= 0; i--) {
            final DesktopWindow w = windows.get(i);
            if (away(w) || !(w.app() instanceof FilesApp)) {
                continue;
            }
            if (mx >= w.x() && mx <= w.x() + w.width() && my >= w.y() && my <= w.y() + w.height()) {
                return w;
            }
        }
        return null;
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

    /** Opens, redraws or takes away a window one of the machine's Σ# programs has. */
    private void acceptProgramWindow(final UiWindowPayload payload) {
        if (!payload.hostPos().equals(host)) {
            return;
        }
        final String key = SigmaWindowApp.keyFor(payload.program(), payload.window());
        for (final DesktopWindow open : windows) {
            if (open.appKey().equals(key) && open.app() instanceof SigmaWindowApp app) {
                if (payload.open()) {
                    app.accept(payload);
                } else {
                    // The program closed it: the window goes without telling the program again.
                    windows.remove(open);
                }
                return;
            }
        }
        if (payload.open()) {
            openApp(key, new SigmaWindowApp(host, payload));
        }
    }

    private void openApp(final String key, final IDesktopApp app) {
        /*
         * Open at the default size, clamped to the screen, but never below the app's minimum while the
         * screen still has room for it, so the content opens laid out (not collapsed) on a small monitor.
         */
        final int top = view.workAreaTop();
        final int workH = view.workAreaBottom() - top;
        final int availW = view.width() - 16;
        final int availH = workH - 16;
        final int w = availW >= app.minWidth() ? Math.min(app.defaultWidth(), availW) : availW;
        final int h = availH >= app.minHeight() ? Math.min(app.defaultHeight(), availH) : availH;
        /*
         * Each window opens a little further down and across than the last, but never past the edge of the work area
         * where it fits: a window that draws its own frame keeps its own size, which may be more than was clamped.
         */
        final int shownW = app.drawsOwnFrame() ? app.defaultWidth() : w;
        final int shownH = app.drawsOwnFrame() ? app.defaultHeight() : h;
        final int x = Math.max(0,
                Math.min(Math.max(48, (view.width() - w) / 2 + windows.size() * 12), view.width() - shownW));
        final int y = Math.max(top, Math.min(Math.max(top + 6, top + (workH - h) / 2 + windows.size() * 12),
                view.workAreaBottom() - shownH));
        final DesktopWindow opened = new DesktopWindow(app, key, x, y, w, h);
        // A program opens on the workspace that is up, which is where whoever started it is looking.
        opened.setWorkspaces(WorkspaceSet.only(shownWorkspace));
        windows.add(opened);
    }

    /** Opens that program's window on this desktop, if this machine has it at all. */
    private void startProgramById(final String path) {
        final Launcher launcher = catalogue.find(l -> l.programId().getPath().equals(path) && l.factory() != null);
        if (launcher != null) {
            openApp(launcher.key(), launcher.factory().get());
        }
    }

    /** Recreates a program from its window key ({@link WindowKeys}), for restoring persisted windows. */
    @Nullable
    private IDesktopApp factoryFor(final String key) {
        /*
         * The welcome has no launcher of its own: it is the system putting itself in front of somebody, not a
         * program anybody goes looking for, and it is the machine that asks for it by name.
         */
        if (WelcomeApp.KEY.equals(key)) {
            return new WelcomeApp(host);
        }
        // Nor has CDE's Application Manager, which is reached from the Front Panel, one window to a group.
        if (is(PanelStyle.CDE) && ApplicationManagerApp.owns(key)) {
            return new ApplicationManagerApp(ApplicationManagerApp.groupOf(key));
        }
        // Nor has the trash, which is a place of the desktop and no program.
        if (WindowKeys.TRASH.equals(key)) {
            return trash.window();
        }
        final Launcher launcher = catalogue.find(l -> l.key().equals(key) && l.factory() != null);
        if (launcher != null) {
            return launcher.factory().get();
        }
        /*
         * A program the desktop shows no launcher for (the Task Manager, or one a file opens in) still opens,
         * and still comes back with the session, by the id its window goes by.
         */
        final ResourceLocation id = ResourceLocation.tryParse(key);
        final ProgramClient.IDesktopAppFactory factory = id == null ? null : ProgramClient.factory(id);
        return factory == null ? null : factory.create(host, monitorPos, desktopId);
    }

    /**
     * Opens the Task Manager, or brings it forward when it is already up. It is the panel's own right-click
     * destination and has no launcher of its own, exactly as on the desktops this imitates.
     */
    void openTaskManager() {
        if (OsRegistry.getProgram(Programs.TASK_MANAGER) == null) {
            return;
        }
        final String key = WindowKeys.of(Programs.TASK_MANAGER);
        final DesktopWindow open = windowFor(key);
        if (open != null) {
            focusWindow(open);
            return;
        }
        final IDesktopApp app = factoryFor(key);
        if (app != null && memory.allowOpen(key)) {
            app.applySkin(prefs.skin());
            openApp(key, app);
        }
    }

    private void openOrFocusWindow(final String key) {
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && w.appKey().equals(key)) {
                focusWindow(w);
                return;
            }
        }
        final IDesktopApp app = factoryFor(key);
        if (app != null && memory.allowOpen(key)) {
            app.applySkin(prefs.skin());
            openApp(key, app);
        }
    }

    /** Starts a launcher: a built-in app opens a window; an action-based one (e.g. the NMS) runs its action. */
    void runLauncher(final Launcher l) {
        if (!l.runs().isEmpty()) {
            requestRunAtTerminal(l.runs());
            return;
        }
        if (memory.allowOpen(l.key())) {
            openApp(l.key(), l.factory().get());
        }
    }

    private void bringToFront(final int index) {
        if (index >= 0 && index < windows.size()) {
            bringWindowToFront(windows.get(index));
        }
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
        /*
         * The layout goes to the machine: the windows the player leaves behind are what the machine
         * has open, for whoever looks next and after the game is closed. Not when the desktop is closing
         * because the machine is going down; that layout belongs to a session that just ended, and the
         * server has already cleared it.
         */
        if (!powerCycling) {
            PacketDistributor.sendToServer(
                    DesktopWindowsPayload.of(
                            host, snapshotWindows(), shownWorkspace));
        }
        /*
         * The programs' insides stay in this client as a convenience, keyed by the same launcher keys the
         * machine's layout uses, so a restored window picks its session back up when it is still here.
         */
        final Map<String, IDesktopApp> apps = new LinkedHashMap<>();
        for (final DesktopWindow w : windows) {
            if (w.dialog()) {
                w.app().onClosed(); // a question left unanswered is not kept; the program is
            } else if (!(w.app() instanceof SigmaWindowApp)) {
                /*
                 * A Σ# program's window is not kept here either: the machine sends it again, as it
                 * stands, the moment anyone looks at that desktop.
                 */
                apps.put(w.appKey(), w.app());
            }
        }
        if (apps.isEmpty()) {
            SAVED_APPS.remove(host);
        } else {
            SAVED_APPS.put(host, apps);
        }
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
