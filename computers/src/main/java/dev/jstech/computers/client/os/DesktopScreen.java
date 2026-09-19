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
import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.MachineKeyboard;
import dev.jstech.computers.client.MonitorFrame;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.CdeStyle;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.gui.TaskbarGroups;
import dev.jstech.computers.gui.layout.CdeExitLayout;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.gui.layout.CdeWindowIconLayout;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.operation.payload.DesktopShellRunPayload;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.MachinePowerPayload;
import dev.jstech.computers.operation.payload.MoveFilePayload;
import dev.jstech.computers.operation.payload.NiDepositPayload;
import dev.jstech.computers.operation.payload.NiShiftInsertPayload;
import dev.jstech.computers.operation.payload.RequestDesktopFilesPayload;
import dev.jstech.computers.operation.payload.SetIconPositionPayload;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.SetupProgressPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.os.CdeAppGroup;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WorkspaceSet;
import dev.jstech.computers.os.fs.FileOpeners;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.gui.layout.DesktopZ;
import dev.jstech.core.tier.HardwareEra;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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
public final class DesktopScreen extends AbstractContainerScreen<DesktopMenu>
        implements MachineKeyboard.ITakesKeysFirst {

    private final BlockPos host;
    private final BlockPos monitorPos;
    private final ResourceLocation osId;
    private final DesktopTheme theme;
    private OsSkin skin;
    /** Per-computer accent override (0 = skin default), brightness, and clock format, from the settings. */
    private int desktopAccent;
    private int desktopBrightness = 100;
    private boolean desktopClock12h;
    /** Frames 11 taskbar layout: centered app strip (default) or left-aligned next to Start. */
    private boolean desktopTaskbarCentered = true;
    /** Frames 11 dark theme: darkens the window chrome (via the skin) and the Start menu. */
    private boolean desktopDarkMode;
    private final List<DesktopWindow> windows = new ArrayList<>();
    /** Which workspace is up, counted from nought; always the first on a desktop that has only one. */
    private int shownWorkspace;
    private final List<Launcher> launchers = new ArrayList<>();
    private final List<String> installedPrograms = new ArrayList<>();

    /** The Linux desktops' own ways of opening a program: Kickoff, the Mint menu, the Activities overview. */
    private final LinuxLaunchers linuxLaunchers = new LinuxLaunchers(this);
    /** The Frames systems' own: the classic Start menu, XP's two columns, Frames 11's floating panel. */
    private final FramesLaunchers framesLaunchers = new FramesLaunchers(this);
    /** The corner every panel reports the machine in: the network, the sound, the memory and the clock. */
    private final PanelTray tray = new PanelTray(this);
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

    /*
     * Per-OS memory model: the system, its desktop and its services hold their share of the machine's RAM
     * (reserved, from the server) and every open program holds its own, weighed under the running system by the
     * same rule the server applies. A cooperative kernel (Frames 95) is fragile and crashes when overloaded; a
     * preemptive one (XP/11) just refuses.
     */
    private int ramTotalMb;
    private int ramReservedMb;
    /** True while the cooperative OS is showing its crash screen; the desktop reboots to an empty session after. */
    private boolean crashing;
    private long crashUntil;

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
     * The name this desktop gives that program, or empty when this machine has no such program.
     *
     * <p>The name is the desktop's, not the program's: the same prompt is called one thing on one edition and
     * something else on another, and a button that offers it should say what this machine calls it.
     */
    public static String programLabel(final String path) {
        if (active == null) {
            return "";
        }
        for (final Launcher l : active.launchers) {
            if (l.programId().getPath().equals(path)) {
                return l.label();
            }
        }
        return "";
    }

    /** The programs pinned to the panel, by program id path, in the order the machine keeps them. */
    private final List<String> pinnedPrograms = new ArrayList<>();

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
        return active != null && active.popup instanceof OpenWithPopup chooser && chooser.isOpen() ? chooser : null;
    }

    /** The ids of the programs the open desktop's machine has, for a window offering what can open a file. */
    public static List<String> installedProgramIds() {
        return active == null ? List.of() : List.copyOf(active.installedPrograms);
    }

    /** What a program is called, for a menu that offers it by id. */
    public static String openerName(final String programId) {
        if (programId.equals(FileOpeners.EDITOR)) {
            return "Editor";
        }
        final ProgramSpec spec = Programs.get(
                ResourceLocation.fromNamespaceAndPath("jsc", programId));
        return spec == null ? programId : spec.displayName();
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
            active.desktopAccent = accent;
            active.desktopBrightness = brightness;
            active.desktopClock12h = clock12h;
            active.desktopWallpaper = wallpaper == null ? "" : wallpaper;
            active.desktopTaskbarCentered = taskbarCentered;
            active.desktopDarkMode = darkMode;
            active.desktopScale = scale;
            active.rebuildSkin();
        }
    }

    /**
     * How big the desktop draws everything, as a percentage of its designed size; 0 stands for 100.
     * Smaller fits more on the same glass, the way a display setting does on any desktop.
     */
    private int desktopScale;

    /** The percentage the desktop is drawn at when the machine has not been told another: the size that reads best. */
    public static final int DEFAULT_SCALE = 75;

    /** The scale as a factor: three quarters unless the setting says otherwise. */
    private double scale() {
        return (desktopScale <= 0 ? DEFAULT_SCALE : desktopScale) / 100.0;
    }

    /** The screen x of a desktop-local x, for a hook that hands a test a point to click. */
    private int sx(final int local) {
        return ox() + (int) Math.round(local * scale());
    }

    private int sy(final int local) {
        return oy() + (int) Math.round(local * scale());
    }

    /** The desktop-local x of an absolute screen x, allowing for where the glass is and how it is scaled. */
    private double lx(final double absX) {
        return (absX - ox()) / scale();
    }

    private double ly(final double absY) {
        return (absY - oy()) / scale();
    }

    /**
     * An absolute screen x moved so the container's own slot test, which adds {@code leftPos} to a
     * slot's desktop-local x, lands on the right slot under a scaled desktop.
     */
    private double vx(final double absX) {
        return ox() + lx(absX);
    }

    private double vy(final double absY) {
        return oy() + ly(absY);
    }

    /** Rebuilds the skin from the current accent + dark-mode prefs (dark applies only to the flat Frames 11). */
    private void rebuildSkin() {
        /*
         * The host's era picks the desktop's period look: a Linux desktop on Legacy hardware wears
         * its own era, instead of a modern flat theme on a machine from another decade.
         */
        // CDE is drawn out of the palette the machine keeps, which is a choice and not a fact of its era.
        OsSkin base = is(PanelStyle.CDE) ? OsSkin.motif(cdePalette()) : OsSkin.forDesktop(desktopId, era());
        if (desktopDarkMode) {
            base = base.darkVariant();
        }
        this.skin = base.withAccent(desktopAccent);
    }

    /**
     * Raises the modal {@code .dat}-locked error dialog on the active desktop. Called from desktop
     * apps (e.g. the Files explorer) that detect a refused {@code .dat} action and need to surface
     * it; a no-op when no desktop is showing.
     */
    public static void showDatLockedError() {
        if (active != null) {
            active.showError("Error", DAT_LOCKED_MESSAGE);
        }
    }

    /**
     * Raises the error for a refused action on an installer's own files: they are generated from the
     * medium's stamp, so there is nothing to rename, copy off, delete or overwrite.
     */
    public static void showInstallerLockedError() {
        if (active != null) {
            active.showError("Error", "This file is part of the installer and cannot be changed, copied or deleted."
                    + " Run the setup program to install it.");
        }
    }

    /** Opens a modal error dialog with the given title and message over this desktop. */
    void showError(final String title, final String message) {
        this.popup = new DesktopPopup(title, message, this.font);
    }

    /**
     * A notice from the system itself: it rises over the notification area and goes away on its own.
     *
     * @param opens the program a click on it opens, or empty when clicking it only puts it away
     */
    private record Balloon(String title, String body, long until, String opens) {
    }

    /** How long a balloon stays up before it fades away, in milliseconds. */
    private static final long BALLOON_MS = 9_000L;
    private static final int BALLOON_W = 152;
    @Nullable
    private Balloon balloon;

    /**
     * Raises a tray balloon. Unlike {@link #showError}, it takes nothing over: the machine is telling the
     * player something, not asking them to answer, so the desktop stays usable underneath it.
     */
    void showBalloon(final String title, final String body) {
        this.showBalloon(title, body, "");
    }

    /**
     * The same, for a notice that is also an invitation: clicking it opens the program it is about.
     *
     * <p>Which is how a machine of one edition said hello on its first start. It did not put a window in
     * front of anybody; it said one sentence from the corner and left the offer open for as long as the
     * sentence was up.
     */
    void showBalloon(final String title, final String body, final String opens) {
        this.balloon = new Balloon(title, body, System.currentTimeMillis() + BALLOON_MS, opens);
    }

    /** Raises that balloon on whichever desktop is looking at that machine, if one is. */
    public static void raise(final BlockPos host, final String title, final String body, final String opens) {
        if (active != null && active.host.equals(host)) {
            active.showBalloon(title, body, opens);
        }
    }

    private boolean startOpen;
    /** The Frames 11 Start search box: when non-empty, the pinned grid is replaced by a filtered result list. */
    private final StringBuilder startSearch = new StringBuilder();
    /** Local cursor cached each frame, so menus drawn later in the frame can highlight the hovered entry. */
    private int hoverX;
    private int hoverY;
    /** Programs pinned to the right "places" column of the Frames XP Start menu (drawn there, not on the left). */
    private static final Set<String> XP_PLACES =
            Set.of("This PC", "Files", "Settings", "Network");
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
     * The exact message shown whenever a player tries to copy, create, delete, rename, or otherwise
     * modify a {@code .dat} file by hand. A {@code .dat} is a read-only projection of the computer's
     * stored items, so the only sanctioned way to move those items is the Network Interactor.
     */
    static final String DAT_LOCKED_MESSAGE =
            "This file is impossible to modify, create, delete or change manually, "
                    + "use the network interactor for it.";

    /** The modal dialog currently shown over the desktop (an error, or Open with), or {@code null} when none. */
    @Nullable
    private Popup popup;

    /**
     * The program chosen with Always for each extension on this machine, as the desktop listing brings it. A choice
     * made here goes in at once, before the server has sent the listing again.
     */
    private final Map<String, String> defaultApps = new HashMap<>();

    /** Files and folders living in the desktop folder ({@link SystemLayout#DESKTOP_DIR}), drawn as icons. */
    private final List<DiskFilesPayload.WireFile> desktopItems = new ArrayList<>();

    /** The player's chosen wallpaper style ({@code ""} = OS default) and computer name, synced from the server. */
    private String desktopWallpaper = "";
    private String computerName = "";
    /** CDE's palette and the backdrop of each workspace, synced from the server and worn while one is chosen. */
    private CdeStyle cdeStyle = CdeStyle.DEFAULT;

    /** The desktop's right-click menu, the same component every program's menus are. */
    private final ContextMenu deskMenu =
            new ContextMenu(DESK_CTX_W, DESK_CTX_ITEM_H);
    /** Whether the panel's own menu is up, and where it was raised. */
    private boolean panelCtxOpen;
    private int panelCtxX;
    private int panelCtxY;

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
    private boolean periodPanel() {
        return skin.form() == OsSkin.Form.KDE2 || skin.form() == OsSkin.Form.GNOME1;
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

    OsSkin panelSkin() {
        return skin;
    }

    List<Launcher> launcherList() {
        return launchers;
    }

    int screenW() {
        return sw();
    }

    int screenH() {
        return sh();
    }

    /** Where an open launcher starts and how tall it is, which its own desktop decides. */
    int startMenuLeft() {
        return startMenuX();
    }

    int startMenuTall() {
        return startMenuHeight();
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

    /** The time this machine shows, in the format its settings ask for. */
    String clock() {
        return clockText();
    }

    /** The minute of the world's day counted from midnight, for a clock that has hands instead of figures. */
    int minuteOfDay() {
        final Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : (int) (((mc.level.getDayTime() % 24000L + 6000L) % 24000L) * 3L / 50L);
    }

    /** Which day of the world it is, counted from one, for a calendar page. */
    int dayOfWorld() {
        final Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 1 : (int) (mc.level.getDayTime() / 24000L % 9999L) + 1;
    }

    /** The palette a CDE desktop is drawn from. */
    CdePalette cdePalette() {
        return cdeStyle.colours();
    }

    /** CDE's look as this desktop is wearing it, which is what the Style Manager starts from. */
    CdeStyle cdeStyle() {
        return cdeStyle;
    }

    /**
     * Puts a look on the desktop at once, frames and panel and backdrop, without telling the machine: the Style
     * Manager shows a palette this way while it is being chosen, and puts the kept one back on Cancel.
     */
    void wearCdeStyle(final CdeStyle style) {
        cdeStyle = style == null ? CdeStyle.DEFAULT : style;
        rebuildSkin();
    }

    /** Puts a look on the desktop and has the machine keep it, so it is there for whoever looks next. */
    void keepCdeStyle(final CdeStyle style) {
        wearCdeStyle(style);
        PacketDistributor.sendToServer(new SetSettingPayload(host, "cdestyle", cdeStyle.encoded()));
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
        closeStart();
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

    /** Opens a file manager at that folder, under the name this desktop gives its file manager. */
    void openFolder(final String dir) {
        String label = "Files";
        for (final Launcher launcher : launchers) {
            if (launcher.programId() != null && launcher.programId().getPath().equals("files")) {
                label = launcher.label();
                break;
            }
        }
        openApp(label, new FilesApp(host, desktopId.getPath(), dir, monitorPos));
    }

    /** Whether the host computer is on a data network right now, as its block entity tells the client. */
    boolean onNetwork() {
        return networkAttached();
    }

    /** How much memory the machine is using and how much it has, for the meter and its tooltip. */
    int ramUsed() {
        return ramUsedMb();
    }

    int ramTotal() {
        return ramTotalMb;
    }

    String ramMeter() {
        return ramMeterText();
    }

    /** The notification corner, which every panel draws at its right end. */
    PanelTray tray() {
        return tray;
    }

    /** Whether this desktop is drawn in that style, which decides what its panel and launcher look like. */
    boolean isPanel(final PanelStyle style) {
        return is(style);
    }

    /** Whether the launcher is open, which lights its button on the panel. */
    boolean launcherOpen() {
        return startOpen;
    }

    /** Where each task button sits and how wide it is: the same measurement the clicks are tested against. */
    TaskStrip taskButtons(final int sw) {
        return taskStrip(sw);
    }

    /**
     * The left edge of the Start button on a panel that centres its contents, which is what makes the
     * whole [Start + open programs] group move together with the taskbar alignment setting.
     */
    int modernStartLeft(final int sw) {
        return win11StartX(sw);
    }

    /** The windows a program has open, back to front. */
    List<DesktopWindow> windowsOf(final String key) {
        return groupWindows(key);
    }

    /** The files and folders of the desktop folder, which are drawn as icons after the launchers. */
    List<DiskFilesPayload.WireFile> deskFiles() {
        return desktopItems;
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

    /** Says a file cannot be changed by hand, which is what a projection of stored items is. */
    void showLocked() {
        showError("Error", DAT_LOCKED_MESSAGE);
    }

    /** Whether this panel's popup shows the windows' live pictures rather than a list of their titles. */
    boolean popupShowsThumbnails() {
        return thumbnailPopups();
    }

    /** Whether this desktop's panel is the top bar rather than a bottom one. */
    boolean panelOnTop() {
        return topPanel();
    }

    /** The pixels a bottom panel takes, and the first row of the desktop under a top one. */
    int panelReserve() {
        return bottomReserve();
    }

    int workAreaTop() {
        return workTop();
    }

    int workAreaBottom() {
        return workBottom();
    }

    int workAreaWidth() {
        return sw();
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
        return startOpen || panelCtxOpen || taskMenu.isOpen() || cdeWindowMenu.isOpen() || popup != null
                || powerOpen || crashing;
    }

    /** The program whose windows the panel's popup is showing, or null while none is up. */
    @Nullable
    String openTaskPopup() {
        return taskPopup.key();
    }

    /** What a task button reads: the window's own title, or the program's name for a group of them. */
    String taskLabel(final TaskbarGroups.Entry entry) {
        return entryLabel(entry);
    }

    /** How many characters of a title fit in a button that wide. */
    int taskTitleRoom(final int w) {
        return taskTitleChars(w);
    }

    /** The little triangle that marks a button standing for several windows. */
    void drawStackCaret(final GuiGraphics g, final int x, final int y, final int color) {
        drawCaret(g, x, y, color);
    }

    /** What has been typed into an open launcher's search field, empty when nothing has. */
    String searchText() {
        return startSearch.toString();
    }

    /** The programs that match what was typed, or all of them when nothing was. */
    List<Launcher> searchedLaunchers() {
        return w11Filtered();
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

    void launchAt(final int index) {
        startChoose(index);
    }

    void launch(final Launcher launcher) {
        startChoose(launcher);
    }

    void closeLauncher() {
        closeStart();
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
        app.applySkin(skin);
        openApp(key, app);
    }

    /** The desktop that is up, for a window that outlived the screen it was opened on; null while none is. */
    @Nullable
    static DesktopScreen current() {
        return active;
    }

    void askToPowerOff() {
        openPowerDialog();
    }

    /** How wide an open launcher is, which its own desktop decides. */
    int startMenuWide() {
        return startMenuW();
    }

    /** Where an open launcher's top edge is, for the one desktop that floats it rather than sitting it on the bar. */
    int startMenuTop(final int tbY) {
        return startMenuY(tbY);
    }

    /** The desktop's own name, which a period launcher carries up its side band. */
    String deskName() {
        return desktopName();
    }

    /** The system's name, which the classic Start menu carries up its side band. */
    String osBand() {
        return osBandLabel();
    }

    /** The program a label belongs to, for a row that only has the label to go on. */
    ResourceLocation programIdFor(final String label) {
        return programIdForLabel(label);
    }

    /** The XP menu's two columns: the programs on the left, the system's own places on the right. */
    List<Launcher> xpLeft() {
        return xpLeftLaunchers();
    }

    List<Launcher> xpRight() {
        return xpRightLaunchers();
    }

    /** Where the XP left column's row {@code i} sits, which leaves the gap its separator needs. */
    int xpLeftRow(final int i) {
        return xpLeftRowY(i);
    }

    int xpAllRow() {
        return xpAllRowY();
    }

    int xpFooterOff(final int x, final int w) {
        return xpFooterOffX(x, w);
    }

    int xpFooterLog(final int x, final int w) {
        return xpFooterLogX(x, w);
    }

    /** Leaves the desktop without touching the machine, which is what logging off is. */
    void leaveDesktop() {
        onClose();
    }

    /** Opens the page listing everything installed on this machine, services included. */
    void openEverythingInstalled() {
        openAllPrograms();
    }

    /**
     * Whether the panel sits at the top. Only the modern GNOME shell does that: the GNOME of the Legacy
     * era put its panel at the bottom, and its top bar ("Activities") did not exist for another decade.
     */
    private boolean topPanel() {
        return is(PanelStyle.GNOME) && !periodPanel();
    }

    /** The first desktop-local row of the work area. */
    private int workTop() {
        return topPanel() ? TASKBAR_H : 0;
    }

    /** One past the last desktop-local row of the work area (the bottom panel's top, or the screen bottom). */
    private int workBottom() {
        return topPanel() ? sh() : sh() - panelBand();
    }

    /** The pixels reserved for a bottom panel (none under GNOME's top bar). */
    private int bottomReserve() {
        return topPanel() ? 0 : panelBand();
    }

    /**
     * How tall the band a panel stands in is. A taskbar is a taskbar's height on every desktop that has one;
     * CDE's Front Panel is a slab of pictures and stands taller, and windows keep out of its band the whole
     * width of the desktop although the slab itself is only as wide as what it holds.
     */
    private int panelBand() {
        return is(PanelStyle.CDE) ? CdeFrontPanelLayout.BAND_H : TASKBAR_H;
    }

    // Frames 11 taskbar: each centered item (Start + one per program) occupies this slot.
    static final int WIN11_SLOT = 22;
    static final int WIN11_ICON = 16;
    /** A pinned program with no window, on the panels that keep it in place as an icon (KDE, Cinnamon). */
    private static final int LAUNCHER_W = 22;
    /** The pitch of the Frames XP quick launch icons beside Start. */
    static final int QL_W = 16;
    static final int MENU_W = 130;
    static final int BAND_W = 22;
    static final int MENU_ITEM_H = 18;
    // Frames XP Start: a two-column panel (programs left, system "places" right) with a header and a footer band.
    static final int XP_MENU_W = 202;
    static final int XP_HEADER_H = 26;
    /** The orange band the Luna Start menu ran under its user header. */
    static final int XP_ORANGE_H = 2;
    static final int XP_FOOTER_H = 18;
    static final int XP_ROW_H = 16;
    static final int XP_LEFT_W = 120;
    /** The gap a separator sits in, between the pinned block and the rest of the left column. */
    static final int XP_SEP_H = 5;
    /** How many of the left column's entries are drawn as pinned (bold) at its top. */
    static final int XP_PINNED = 2;
    static final int XP_ALL_ROW_H = 15;
    /*
     * Frames 11 Start: a compact floating panel with a search box, a pinned-app grid, and a footer power button.
     * Kept small (5 columns, tight tiles) so even a Mainframe's full app set fits above the taskbar.
     */
    static final int W11_MENU_W = 172;
    static final int W11_COLS = 5;
    static final int W11_TILE_W = 32;
    static final int W11_TILE_H = 30;
    static final int W11_SEARCH_H = 14;
    static final int W11_FOOTER_H = 18;
    /*
     * The Linux launchers' own measurements live with the launchers, since that is what draws and hit-tests
     * them; the desktop only needs the few the shared geometry below is worked out from.
     */
    private static final int KDE_MENU_W = LinuxLaunchers.KDE_MENU_W;
    private static final int KDE_HEADER_H = LinuxLaunchers.KDE_HEADER_H;
    private static final int KDE_ROW_H = LinuxLaunchers.KDE_ROW_H;
    private static final int KDE_FOOTER_H = LinuxLaunchers.KDE_FOOTER_H;
    private static final int CIN_MENU_W = LinuxLaunchers.CIN_MENU_W;
    private static final int CIN_HEADER_H = LinuxLaunchers.CIN_HEADER_H;
    private static final int CIN_ROW_H = LinuxLaunchers.CIN_ROW_H;
    /** The width of the Frames XP Start pill, which the task buttons and its own hit-test both clear. */
    static final int XP_START_W = 58;
    private static final int DESK_CTX_W = 88;
    private static final int DESK_CTX_ITEM_H = 11;
    /**
     * The panel's own menu. Right-clicking a taskbar opens this on every desktop these imitate, and the Task
     * Manager is one entry on it rather than the click's whole meaning. A separator sits before that entry.
     */
    private static final String[] PANEL_CTX = {"Cascade Windows", "Show the Desktop", "-", "Task Manager"};
    private static final int PANEL_CTX_W = 104;

    /** A desktop/start-menu entry that opens an app when clicked. */
    /*
     * A launcher either opens a built-in app window (factory) or runs a custom action (e.g. open the
     * NMS, which is a server-side menu rather than a desktop window). Exactly one is non-null.
     */
    /** A desktop launcher: its display label, the program id (icon + identity), and the window factory. */
    /**
     * One thing on the desktop that can be started.
     *
     * <p>{@code runs} is the listing a player's own program starts at. Those have no window of their
     * own: like any console program they get a terminal and print into it, which is the same thing that
     * happens when one is opened in the file explorer.
     */
    record Launcher(String label, ResourceLocation programId,
                    Supplier<IDesktopApp> factory, String runs) {

        Launcher(final String label, final ResourceLocation programId,
                 final Supplier<IDesktopApp> factory) {
            this(label, programId, factory, "");
        }
    }

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
        this.ramTotalMb = menu.ramTotalMb();
        this.ramReservedMb = menu.ramReservedMb();
        this.chrome = OsRegistry.getDesktop(desktopId);
        this.panel = chrome != null ? chrome.panelStyle() : switch (desktopId.getPath()) {
            case "frames_xp" -> PanelStyle.FRAMES_XP;
            case "frames_11" -> PanelStyle.FRAMES_11;
            default -> PanelStyle.FRAMES_95;
        };
        final OsDef os =
                OsRegistry.getOs(osId);
        this.desktopDir = SystemLayout.desktopDirFor(os, os == null ? null
                : OsRegistry.getKernel(os.kernelId()));
        this.theme = DesktopTheme.forDesktop(desktopId);
        // A provisional skin: rebuildSkin() refines it with the host's era once the level is reachable.
        this.skin = OsSkin.forDesktop(desktopId);
    }

    private boolean is(final PanelStyle style) {
        return panel == style;
    }

    /** A Linux desktop environment (bottom-panel KDE/Cinnamon or top-bar GNOME), as opposed to a Frames edition. */
    private boolean linuxDesktop() {
        return is(PanelStyle.KDE)
                || is(PanelStyle.GNOME)
                || is(PanelStyle.CINNAMON);
    }

    /** The desktop environment's display name, for the Start band and menus. */
    private String desktopName() {
        return chrome != null ? chrome.displayName() : desktopId.getPath();
    }

    /** The megabytes a window opened under {@code key} holds: its program's weight under the running system. */
    private int windowRamMb(final String key) {
        final OsDef os = OsRegistry.getOs(osId);
        return os == null ? 0 : IOsHost.windowRamMb(key, os, chrome);
    }

    /** What the open windows hold together; a dialog is part of its program, not another copy of it. */
    private int windowsRamMb() {
        int sum = 0;
        for (final DesktopWindow w : windows) {
            if (!w.dialog()) {
                sum += windowRamMb(w.appKey());
            }
        }
        return sum;
    }

    /** Everything held right now: the system's share, its desktop and services, and the open windows. */
    private int ramUsedMb() {
        return ramReservedMb + windowsRamMb();
    }

    /** A cooperative kernel (Frames 95) has no memory protection: overloading it crashes the whole desktop. */
    private boolean isCooperative() {
        return osId.getPath().equals("frames_95");
    }

    /**
     * Whether a window of {@code key} may open now, that is whether its program's weight still fits in the
     * free RAM; otherwise a preemptive OS refuses with the figures and a cooperative one crashes.
     */
    private boolean allowOpen(final String key) {
        if (crashing) {
            return false;
        }
        final int need = windowRamMb(key);
        final int free = ramTotalMb - ramUsedMb();
        if (need <= free) {
            return true;
        }
        if (isCooperative()) {
            crashing = true;
            crashUntil = System.currentTimeMillis() + 4200;
        } else {
            /*
             * A refusal the machine can simply report: the desktop is still there, so a balloon says it the
             * way the notification area always did, instead of taking the screen over with a dialog.
             */
            showBalloon("Low on memory", "This computer is running out of RAM for programs. " + key
                    + " needs " + need + " MB and only " + Math.max(0, free) + " MB are free.");
        }
        return false;
    }

    /** Reboots after a crash: the session is lost (windows and their saved state), back to an empty desktop. */
    private void reboot() {
        crashing = false;
        balloon = null;
        windows.clear();
        SAVED_APPS.remove(host);
        startOpen = false;
        popup = null;
    }

    /** The cooperative-kernel crash screen: a classic blue fatal-error page, drawn in desktop-local coords. */
    private void renderCrash(final GuiGraphics g, final int sw, final int sh) {
        g.fill(0, 0, sw, sh, 0xFF0000AA);
        final int cy = sh / 3;
        final String head = " Frames ";
        final int hw = font.width(head) + 6;
        g.fill((sw - hw) / 2, cy - 2, (sw + hw) / 2, cy + 10, 0xFFAAAAAA);
        g.drawString(font, head, (sw - font.width(head)) / 2, cy, 0xFF0000AA, false);
        final String[] lines = {
            "A fatal exception has occurred.",
            "This computer ran out of memory with too many",
            "programs open, and the system became unstable.",
            "",
            "The cooperative kernel cannot recover.",
            "Rebooting...",
        };
        int ly = cy + 20;
        for (final String s : lines) {
            g.drawString(font, s, (sw - font.width(s)) / 2, ly, 0xFFFFFFFF, false);
            ly += 11;
        }
    }

    /*
     * The on-screen monitor "screen" rectangle: a centred window, not the whole game viewport. The extra slack
     * (vs the raw viewport) leaves room for the monitor frame drawn around the glass and its chin below it.
     */
    /** The glass's width on the screen, in screen pixels: what the frame wraps and the scissor clips. */
    private int pw() {
        return MonitorGlass.width(width);
    }

    private int ph() {
        return MonitorGlass.height(height);
    }

    /**
     * The desktop's width as the desktop sees it: the glass's, and more of it when the desktop is
     * drawn smaller. Everything laid out on the desktop uses this pair and is drawn under the scale.
     */
    private int sw() {
        return (int) Math.round(pw() / scale());
    }

    private int sh() {
        return (int) Math.round(ph() / scale());
    }

    private int ox() {
        return (width - pw()) / 2;
    }

    private int oy() {
        return (height - ph()) / 2;
    }

    // inspection (client tests drive the desktop through the same hit areas the player clicks)

    public boolean isStartOpen() {
        return startOpen;
    }

    /** Screen position of the desktop's top-left corner: window and app geometry is relative to it. */
    public int desktopX() {
        return ox();
    }

    public int desktopY() {
        return oy();
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
        return new Rect2i(sx(x), sy(y),
                (int) Math.round(w * scale()), (int) Math.round(h * scale()));
    }

    /** Whether the panel's own menu is open. */
    public boolean isPanelMenuOpen() {
        return panelCtxOpen;
    }

    /** Whether a program's menu, the one its panel entry opens, is open. */
    public boolean isTaskMenuOpen() {
        return taskMenu.isOpen();
    }

    /**
     * Screen position of the centre of the {@code index}-th panel entry, wherever this panel keeps its
     * entries: centred on Frames 11, from the left on every other panel.
     */
    public int[] taskButtonPoint(final int index) {
        final TaskStrip strip = taskStrip(sw());
        return index >= 0 && index < strip.entries().size() ? taskEntryPoint(strip.entries().get(index).key()) : null;
    }

    /**
     * Screen position of the centre of the panel entry of the program {@code key}, or null when the
     * panel has none for it. On Frames XP a pinned program with no window is its quick launch icon.
     */
    public int[] taskEntryPoint(final String key) {
        final TaskStrip strip = taskStrip(sw());
        final int index = TaskbarGroups.indexOf(strip.entries(), key);
        if (index < 0) {
            return null;
        }
        final int y = sy(topPanel() ? TASKBAR_H / 2 : sh() - TASKBAR_H / 2);
        if (strip.w()[index] > 0) {
            return new int[] {sx(strip.x()[index] + strip.w()[index] / 2), y};
        }
        final int quick = strip.quickIndexOf(index);
        return quick < 0 ? null : new int[] {sx(strip.quickX() + quick * QL_W + QL_W / 2), y};
    }

    /** The programs the panel lists, in order: the pinned ones first, then every open one. */
    public List<String> taskEntryLabels() {
        final List<String> out = new ArrayList<>();
        for (final TaskbarGroups.Entry entry : taskEntries()) {
            out.add(entry.key());
        }
        return out;
    }

    /** The programs pinned to the panel, by the label the panel shows them under. */
    public List<String> pinnedLabels() {
        return pinnedKeys();
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
        if (taskMenu.isOpen()) {
            for (final ContextMenu.Item item : taskMenu.items()) {
                out.add(item.label());
            }
        }
        return out;
    }

    /** The desktop-local centre of the program menu's entry {@code label}, or null when it is not there. */
    public int[] taskMenuPoint(final String label) {
        final List<String> labels = taskMenuLabels();
        final int index = labels.indexOf(label);
        return index < 0 ? null : taskMenu.itemCenter(index);
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
        for (int i = windows.size() - 1; i >= 0; i--) {
            final DesktopWindow w = windows.get(i);
            if (w.dialog() && w.groupKey().equals(label)) {
                return w;
            }
        }
        return null;
    }

    /** Screen position of the centre of the panel menu's {@code label} entry, or null when it is not there. */
    @Nullable
    public int[] panelMenuPoint(final String label) {
        if (!panelCtxOpen) {
            return null;
        }
        for (int i = 0; i < PANEL_CTX.length; i++) {
            if (PANEL_CTX[i].equals(label)) {
                return new int[] {sx(panelCtxX + PANEL_CTX_W / 2),
                        sy(panelCtxY + 1 + i * DESK_CTX_ITEM_H + DESK_CTX_ITEM_H / 2)};
            }
        }
        return null;
    }

    /** Screen position of the middle of one of the Front Panel's controls, under the arrow at its head. */
    public int[] frontPanelPoint(final CdeFrontPanelLayout.Control control) {
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.control(control, sw(), sh());
        return new int[] {sx(r.x() + r.w() / 2), sy(r.y() + CdeFrontPanelLayout.ARROW_H + (r.h()
                - CdeFrontPanelLayout.ARROW_H) / 2)};
    }

    /** Screen position of the middle of the Front Panel's button for workspace {@code index}, from nought. */
    public int[] workspacePoint(final int index) {
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.workspace(index, sw(), sh());
        return new int[] {sx(r.x() + r.w() / 2), sy(r.y() + r.h() / 2)};
    }

    /** Screen position of a title-bar button (1 minimise, 2 maximise, 3 the way out) of the window so labelled. */
    public int[] windowButtonPoint(final String label, final int button) {
        for (final DesktopWindow w : windows) {
            if (w.appKey().equals(label)) {
                final int[] at = w.buttonCentre(button);
                return new int[] {sx(at[0]), sy(at[1])};
            }
        }
        return new int[] {0, 0};
    }

    /** Screen position of the arrow at the head of a Front Panel control, which raises what is behind it. */
    public int[] frontPanelArrowPoint(final CdeFrontPanelLayout.Control control) {
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.control(control, sw(), sh());
        return new int[] {sx(r.x() + r.w() / 2), sy(r.y() + CdeFrontPanelLayout.ARROW_H / 2 + 1)};
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
        final int[] at = cdeLaunchers.rowCentre(label, sw(), sh());
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
        final DesktopWindow w = windowFor("Workstation Info");
        return w != null && w.app() instanceof WorkstationInfoApp app ? app.shownFacts() : List.of();
    }

    /** CDE's look as the desktop is wearing it, as the machine would keep it. */
    public String wornCdeStyle() {
        return cdeStyle.encoded();
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
        final CdeFrontPanelLayout.Rect r = CdeFrontPanelLayout.exit(sw(), sh());
        return new int[] {sx(r.x() + r.w() / 2), sy(r.y() + r.h() / 2)};
    }

    /** Whether the dialog that shuts the machine down or restarts it is up. */
    public boolean powerDialogOpen() {
        return powerOpen;
    }

    /** Screen position of a button of CDE's Exit dialog, by the numbers {@link CdeExitLayout} gives them. */
    public int[] exitDialogPoint(final int button) {
        final CdeFrontPanelLayout.Rect r = CdeExitLayout.button(button, sw(), sh());
        return new int[] {sx(r.x() + r.w() / 2), sy(r.y() + r.h() / 2)};
    }

    /** What the window menu CDE has up lists, top to bottom, or nothing when none is up. */
    public List<String> windowMenuLabels() {
        return cdeWindowMenu.labels();
    }

    /** Screen position of the entry so labelled on the window menu that is up, or null. */
    public int[] windowMenuPoint(final String label) {
        final int[] at = cdeWindowMenu.entryCentre(label);
        return at == null ? null : new int[] {sx(at[0]), sy(at[1])};
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
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && w.appKey().equals(label)) {
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
        final CdeFrontPanelLayout.Rect tile = CdeWindowIconLayout.tile(index, sw(), workTop());
        return new int[] {sx(tile.x() + tile.w() / 2), sy(tile.y() + tile.h() / 2)};
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
                out.add(w.appKey());
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
        return new int[] {sx(Math.max(TASK_X, taskStripRight(sw()) - 8)), sy(sh() - TASKBAR_H / 2)};
    }

    /** The labels of the program windows this desktop has open (dialogs aside), back to front. */
    public List<String> openWindowLabels() {
        final List<String> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (!w.dialog()) {
                out.add(w.appKey());
            }
        }
        return out;
    }

    /** The Start menu entries, top to bottom, as labelled for the player. */
    /** What this desktop calls its terminal, or an empty string when it has none installed. */
    private String terminalLabel() {
        for (final Launcher l : launchers) {
            if (Programs.COMMAND_PROMPT.equals(l.programId())) {
                return l.label();
            }
        }
        return "";
    }

    /**
     * What this desktop calls its terminal (Command Prompt, Megashell, Konsole...), for a program
     * offering to open one; empty when no desktop is up or it has none.
     */
    public static String terminalName() {
        return active == null ? "" : active.terminalLabel();
    }

    public List<String> launcherLabels() {
        final List<String> out = new ArrayList<>();
        for (final Launcher l : launchers) {
            out.add(l.label());
        }
        return out;
    }

    /** Whether {@code app} runs in the front (focused) window; what a recipe viewer's drop or transfer targets. */
    public boolean isFront(final IDesktopApp app) {
        final DesktopWindow front = frontWindow();
        return front != null && front.app() == app;
    }

    /** The open window hosting the program launched under {@code label}, or null. */
    @Nullable
    /** Every window of a program (its dialogs aside), front-most last, since a program may be open more than once. */
    public List<DesktopWindow> windowsFor(final String label) {
        final List<DesktopWindow> out = new ArrayList<>();
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && w.appKey().equals(label)) {
                out.add(w);
            }
        }
        return out;
    }

    public DesktopWindow windowFor(final String label) {
        for (final DesktopWindow w : windows) {
            if (!w.dialog() && w.appKey().equals(label)) {
                return w;
            }
        }
        return null;
    }

    /** Screen coordinates of the Start button's centre. */
    public int startButtonX() {
        return sx(is(PanelStyle.FRAMES_11) ? 4 + WIN11_SLOT / 2 : 30);
    }

    public int startButtonY() {
        // GNOME's "Activities" launcher lives in the top bar; every other panel sits at the bottom.
        return sy(topPanel() ? TASKBAR_H / 2 : sh() - TASKBAR_H / 2);
    }

    /** Screen coordinates of the centre of the {@code index}-th Start menu entry (valid while it is open). */
    public int startMenuItemX() {
        return sx(startMenuX() + BAND_W + 30);
    }

    /**
     * Screen x of the centre of the {@code index}-th Start menu entry. Frames XP lays its entries in two
     * columns (programs left, places right), so the column depends on the entry.
     */
    public int startMenuItemX(final int index) {
        if (panel == PanelStyle.FRAMES_XP
                && index >= 0 && index < launchers.size()) {
            final boolean place = XP_PLACES.contains(launchers.get(index).label());
            final int colX = startMenuX() + (place ? XP_LEFT_W + 3 : 3);
            final int colW = place ? XP_MENU_W - XP_LEFT_W - 6 : XP_LEFT_W - 6;
            return sx(colX + colW / 2);
        }
        return startMenuItemX();
    }

    public int startMenuItemY(final int index) {
        final int tbY = sh() - TASKBAR_H;
        if (panel == PanelStyle.FRAMES_XP
                && index >= 0 && index < launchers.size()) {
            final Launcher target = launchers.get(index);
            final boolean place = XP_PLACES.contains(target.label());
            final List<Launcher> column = place ? xpRightLaunchers() : xpLeftLaunchers();
            final int row = Math.max(0, column.indexOf(target));
            // The left column has a separator under its pinned block, so its rows are not a plain multiple.
            final int rowY = place ? row * XP_ROW_H : xpLeftRowY(row);
            return sy(tbY - startMenuHeight() + XP_HEADER_H + XP_ORANGE_H + 3 + rowY + XP_ROW_H / 2);
        }
        return sy(tbY - startMenuHeight() + 4 + index * MENU_ITEM_H + MENU_ITEM_H / 2);
    }

    /**
     * The host computer's hardware era, read from its block entity so the monitor frame matches the chassis.
     * Never null: a host that cannot name an era yet (a rack whose unit the client has not received) gets the
     * Standard frame, since the frame geometry is asked for every frame by the recipe viewer as well.
     */
    private HardwareEra era() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(host)
                instanceof IOsHost be) {
            final HardwareEra era = be.displayEra();
            if (era != null) {
                return era;
            }
        }
        return HardwareEra.STANDARD;
    }

    /** The in-game time of day as HH:MM for the taskbar clock (Minecraft dayTime 0 = 06:00). */
    private String clockText() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "";
        }
        final long t = mc.level.getDayTime() % 24000L;
        final int totalMin = (int) (((t + 6000L) % 24000L) * 3L / 50L);
        final int hour24 = totalMin / 60;
        final int minute = totalMin % 60;
        if (desktopClock12h) {
            final int h12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
            return String.format(Locale.ROOT, "%d:%02d %s", h12, minute, hour24 < 12 ? "AM" : "PM");
        }
        return String.format(Locale.ROOT, "%02d:%02d", hour24, minute);
    }

    /**
     * Outer bounds of the framed monitor window (the bezel plus its chin), in screen coordinates. Integrations
     * that place a side panel next to this screen (e.g. the JEI ingredient list) read this so the panel sits
     * beside the monitor rather than over it.
     */
    public MonitorFrameStyle.Geometry frameBounds() {
        // The frame wraps the glass as it is on the screen, whatever the desktop inside it is scaled to.
        return MonitorFrameStyle.forEra(era()).geometry(ox(), oy(), pw(), ph());
    }

    @Override
    protected void init() {
        /*
         * Size the container's image rect to the on-screen monitor glass, so leftPos/topPos centre exactly
         * where ox()/oy() place the desktop. The inventory/title labels the base would draw are pushed
         * off-screen, since the desktop draws its own chrome.
         */
        this.imageWidth = pw();
        this.imageHeight = ph();
        super.init();
        this.leftPos = ox();
        this.topPos = oy();
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;

        /*
         * Resolve the era skin before the first frame. The panel's placement now follows the skin (a
         * period desktop panels at the bottom), so waiting for the desktop payload to arrive would draw
         * one frame with the panel on the wrong edge and then jump.
         */
        rebuildSkin();

        buildLaunchers();

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
            app.applySkin(screen.skin);
            if (restored) {
                app.onRestored(); // a kept instance re-asks the server for what may have changed meanwhile
            }
            final DesktopWindow w = new DesktopWindow(app, ow.key(), ow.x(), ow.y(), ow.w(), ow.h());
            /*
             * Clamp into the current work area: the monitor may be a different size from the one the
             * layout was left on, and a title bar off-screen is a window nobody can reach.
             */
            w.moveTo(ow.x(), ow.y(), screen.workTop(), screen.sw(), screen.workBottom());
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

    /**
     * (Re)builds the launcher rail from the single program registry: every registered Frames app that opens a
     * window and is present on this computer. A pre-installed app is gated here by its host scope and OS rank
     * (e.g. the Network Manager on the Mainframe, Frames XP or newer); an installed app is gated on the server
     * (its id already sits in {@link #installedPrograms} only when it passed). No more per-program branches.
     */
    private void buildLaunchers() {
        launchers.clear();
        final boolean isMainframe = hostIs(
                MainframeBlockEntity.class);
        final boolean isCraftingComputer = hostIs(
                CraftingComputerBlockEntity.class);
        // A rack shows the desktop of the server mounted in it, so a rack host IS a server session.
        final boolean isServer = hostIs(
                ServerRackBlockEntity.class);
        final boolean isClusterManager = hostIs(
                ClusterManagementComputerBlockEntity.class);
        final int rank = OsRegistry.osVersionRank(osId);
        final OsDef os =
                OsRegistry.getOs(osId);
        final Platform platform =
                os != null ? os.platform() : Platform.FRAMES;
        for (final ProgramSpec spec
                : OsRegistry.programs()) {
            if (spec.kind() != ProgramKind.APP
                    || !spec.platforms().contains(platform)
                    || !ProgramClient.hasWindow(spec.id())) {
                continue;
            }
            if (spec.preinstalled()) {
                // Built-in apps are present when the desktop environment bundles them; gate by host and OS version.
                if (chrome != null && !chrome.bundles(spec.id())) {
                    continue;
                }
                if (!hostScopeAllows(spec.hostScope(), isMainframe, isCraftingComputer, isServer, isClusterManager)) {
                    continue;
                }
                if (rank != 0 && rank < spec.minOsRank()) {
                    continue;
                }
            } else if (!installedPrograms.contains(spec.id().getPath())) {
                continue; // installed apps: the server already gated them into installedPrograms
            }
            final ProgramClient.IDesktopAppFactory factory = ProgramClient.factory(spec.id());
            // Apps receive the desktop id (their skin/icon key); the Frames editions' id equals their OS id.
            launchers.add(new Launcher(launcherLabel(spec), spec.id(),
                    () -> factory.create(host, monitorPos, desktopId)));
        }
        /*
         * Then whatever the player installed from the Mirror. These are not the mod's programs and have
         * no window of their own: starting one gets it a terminal, exactly as opening it in the file
         * explorer would. The icon id is one the artwork can grow into; until it does they wear the
         * generic one, which is what ProgramIcons falls back to.
         */
        for (final CommunityLauncher one : communityPrograms) {
            launchers.add(new Launcher(one.name(),
                    ResourceLocation.fromNamespaceAndPath(
                            JsComputers.MODID, "sigma_" + one.icon()),
                    null, one.entry()));
        }
    }

    /** A player's own program on this desktop: what to call it, what to draw, and what to run. */
    private record CommunityLauncher(String name, String icon, String entry) {
    }

    private final List<CommunityLauncher> communityPrograms = new ArrayList<>();

    /** The program id for an open window's app key (its launcher label), for the taskbar icon; generic if none. */
    private ResourceLocation programIdForLabel(final String label) {
        for (final Launcher l : launchers) {
            if (l.label().equals(label)) {
                return l.programId();
            }
        }
        // A window whose program has no launcher (the Task Manager) still shows its own icon on the panel.
        final ProgramSpec spec = chrome == null ? null : chrome.programFor(label);
        return spec != null ? spec.id()
                : ResourceLocation.fromNamespaceAndPath("jsc", "generic");
    }

    /** Whether the linked host computer's block entity is (an instance of) {@code type}. */
    private boolean hostIs(final Class<?> type) {
        return Minecraft.getInstance().level != null
                && type.isInstance(Minecraft.getInstance().level.getBlockEntity(host));
    }

    /** Whether a program's host scope permits it on this computer. */
    private static boolean hostScopeAllows(final HostScope scope,
                                           final boolean isMainframe, final boolean isCraftingComputer,
                                           final boolean isServer, final boolean isClusterManager) {
        return switch (scope) {
            case ANY -> true;
            case MAINFRAME -> isMainframe;
            case CRAFTING_COMPUTER -> isCraftingComputer;
            case SERVER -> isServer;
            case CLUSTER_MANAGEMENT_COMPUTER -> isClusterManager;
        };
    }

    /**
     * The rail label for a program: the desktop environment's native name for it (Dolphin, Konsole, Nautilus...),
     * its own display name otherwise; the shell reads "Megashell" on Frames 11.
     */
    private String launcherLabel(final ProgramSpec spec) {
        if (chrome != null) {
            return chrome.launcherLabel(spec); // the rule the server resolves a window back to its program with
        }
        if (spec.id().getPath().equals("command_prompt")
                && is(PanelStyle.FRAMES_11)) {
            return "Megashell";
        }
        return spec.displayName();
    }

    /** Requests the desktop folder's files so they can be drawn as background icons. */
    private void requestDesktop() {
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
        active.desktopWallpaper = payload.wallpaper();
        active.cdeStyle = CdeStyle.parse(payload.cdeStyle());
        active.computerName = payload.computerName();
        active.desktopAccent = payload.prefs().accent();
        active.desktopBrightness = payload.prefs().brightness();
        active.desktopClock12h = payload.prefs().clock12h();
        active.desktopTaskbarCentered = payload.prefs().taskbarCentered();
        active.desktopDarkMode = payload.prefs().darkMode();
        active.desktopScale = payload.prefs().scale();
        active.pinnedPrograms.clear();
        active.pinnedPrograms.addAll(payload.pinned());
        active.defaultApps.clear();
        active.defaultApps.putAll(payload.defaultApps());
        active.rebuildSkin();
        active.iconGrid.pinnedCells().clear();
        for (final DesktopFilesPayload.WireIconCell cell : payload.iconCells()) {
            active.iconGrid.pinnedCells().put(cell.key(), cell.cell());
        }
        /*
         * Refresh the installed-program launchers whenever the installed set changes, so ANY installable
         * program (NMS, Minesweeper, Storage Insights, ...) gets its launcher the moment it is installed.
         */
        final Set<String> before = new HashSet<>(active.installedPrograms);
        final List<CommunityLauncher> theirsBefore = List.copyOf(active.communityPrograms);
        active.installedPrograms.clear();
        active.installedPrograms.addAll(payload.programs());
        active.communityPrograms.clear();
        for (final DesktopFilesPayload.WireCommunity one : payload.community()) {
            active.communityPrograms.add(new CommunityLauncher(one.name(), one.icon(), one.entry()));
        }
        if (!before.equals(new HashSet<>(active.installedPrograms))
                || !theirsBefore.equals(active.communityPrograms)) {
            active.buildLaunchers();
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
        syncInventorySlots();
        final int sw = sw();
        final int sh = sh();
        final int ox = ox();
        final int oy = oy();
        final int lmx = (int) Math.floor(lx(mouseX));
        final int lmy = (int) Math.floor(ly(mouseY));
        // Cache the local cursor so the Start-menu draw (called deeper in this frame) can highlight the hovered row.
        this.hoverX = lmx;
        this.hoverY = lmy;
        taskPopup.update(lmx, lmy, sw, sh - panelBand());
        final HardwareEra eraNow = era();

        /*
         * The host computer's hardware-era monitor frame wraps the desktop glass, then translate so the desktop
         * draws in local (0,0)-(sw,sh) coordinates.
         */
        MonitorFrame.renderBody(g, ox, oy, pw(), ph(), eraNow, font);
        g.pose().pushPose();
        g.pose().translate(ox, oy, 0);
        // Everything on the desktop is drawn under its scale, so a smaller setting fits more on the glass.
        g.pose().scale((float) scale(), (float) scale(), 1);
        g.enableScissor(ox, oy, ox + pw(), oy + ph());

        if (is(PanelStyle.CDE)) {
            // CDE hangs no picture: each workspace wears a pattern of its own in the palette's backdrop colours.
            MotifChrome.backdrop(g, sw, sh, cdePalette(), cdeStyle.backdrop(shownWorkspace));
        } else {
            WallpaperPainter.paint(g, sw, sh, desktopId, eraNow, desktopWallpaper);
        }

        // A cooperative OS that ran out of memory shows its crash screen, then reboots to an empty session.
        if (crashing) {
            if (System.currentTimeMillis() >= crashUntil) {
                reboot();
            } else {
                renderCrash(g, sw, sh);
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
            cdeWindowIcons.render(g, putAwayHere(), sw, workTop(), cdePalette());
        }
        g.pose().popPose();

        renderWindows(g, lmx, lmy, partialTick, sw, sh);

        final int tbY = sh - panelBand();
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
            w.render(g, font, skin, lmx, lmy, partialTick, sw, sh, bottomReserve(), workTop());
            g.pose().popPose();
        }
        /*
         * Real container-slot items for the focused Network Interactor window's inventory zone, over the
         * window the app already drew the slot backgrounds for.
         */
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.INVENTORY);
        renderInventoryItems(g, lmx, lmy, partialTick);
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
            cdePanels.render(g, sw, sh, cdePalette());
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
            framesPanels.renderClassic(g, tbY, sw, sh, lmx, lmy, desktopId.getPath());
        }
        g.pose().popPose();

        // A tray balloon sits above the panel and under the menus, so opening the launcher covers it.
        if (balloon != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.TASKBAR + 10);
            renderBalloon(g, tbY, sw);
            g.pose().popPose();
        }
        // The figures behind the notification area, while the cursor rests on it. CDE has no such area.
        if (!topPanel() && !is(PanelStyle.CDE)) {
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

    /** The three menus that share a height above the panel: the launcher, the panel's own, and the desktop's. */
    private void renderMenus(final GuiGraphics g, final int tbY, final int lmx, final int lmy,
                             final float partialTick) {
        // The name of a Front Panel control rides at the menus' height, so no window can stand over it.
        if (is(PanelStyle.CDE) && !menuOrDialogOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.MENU);
            cdePanels.renderTip(g, sw(), sh(), cdePalette());
            g.pose().popPose();
        }
        if (!startOpen && !deskMenu.isOpen() && !panelCtxOpen && !cdeLaunchers.isOpen()) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, DesktopZ.MENU);
        if (startOpen) {
            renderStartMenu(g, tbY);
        }
        // A subpanel of CDE's Front Panel is no launcher that comes and goes: it stays up until its arrow says so.
        cdeLaunchers.render(g, sw(), sh(), cdePalette());
        if (panelCtxOpen) {
            renderPanelContext(g, lmx, lmy);
        }
        if (deskMenu.isOpen()) {
            deskMenu.render(g, new UiContext(skin, font, lmx, lmy, partialTick));
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
        if (deskDragSlot < launchers.size() + desktopItems.size()) {
            final String label = deskDragSlot < launchers.size()
                    ? launchers.get(deskDragSlot).label()
                    : DesktopIcons.baseName(desktopItems.get(deskDragSlot - launchers.size()).path());
            final int gx = (int) deskDragX + 6;
            final int gy = (int) deskDragY + 2;
            g.fill(gx, gy, gx + font.width(label) + 6, gy + 12, 0xD0303848);
            g.drawString(font, label, gx + 3, gy + 2, 0xFFFFFFFF, false);
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
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], 0x334C84F0);
        g.fill(r[0], r[1], r[0] + r[2], r[1] + 1, 0xCC4C84F0);
        g.fill(r[0], r[1] + r[3] - 1, r[0] + r[2], r[1] + r[3], 0xCC4C84F0);
        g.fill(r[0], r[1], r[0] + 1, r[1] + r[3], 0xCC4C84F0);
        g.fill(r[0] + r[2] - 1, r[1], r[0] + r[2], r[1] + r[3], 0xCC4C84F0);
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
        renderCarried(g, lmx, lmy);
        g.pose().popPose();

        // Brightness: a per-computer dim over the whole surface (100 = none, 0 = deeply dimmed).
        if (desktopBrightness < 100) {
            final int alpha = Math.min(210, (100 - desktopBrightness) * 21 / 10);
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

        if (popup != null && !popup.isOpen()) {
            // Closed by its own choice rather than by a click the desktop saw, as Open with can be.
            popup = null;
        }
        if (popup != null) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            popup.renderIn(g, new UiContext(skin, font, lmx, lmy, 0f), 0, 0, sw, sh);
            g.pose().popPose();
        }
        // The power dialog rides at the same height: it is the one choice that ends the session.
        if (powerOpen) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            renderPowerDialog(g, sw, sh, lmx, lmy);
            g.pose().popPose();
        }
        // A program's own menu, from its panel entry, sits above the windows it acts on.
        if (taskMenu.isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            taskMenu.render(g, new UiContext(skin, font, lmx, lmy, partialTick));
            g.pose().popPose();
        }
        // So does a window's own menu on CDE, which hangs from the button at the left of its title bar.
        if (cdeWindowMenu.isOpen()) {
            g.pose().pushPose();
            g.pose().translate(0, 0, DesktopZ.POPUP);
            cdeWindowMenu.render(g, lmx, lmy, cdePalette());
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
            for (final String key : PENDING_CLOSE) {
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
            if (allowOpen("Files")) {
                openApp("Files", new FilesApp(host, desktopId.getPath(),
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
            if (allowOpen("Files")) {
                final FilesApp files = new FilesApp(host, desktopId.getPath(), desktopDir, monitorPos);
                files.showPropertiesFor(FsPaths.fileName(path));
                openApp("Files", files);
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
        final IDesktopApp app = factoryFor(key);
        if (app != null && allowOpen(key)) {
            openApp(key, app);
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
        final int total = launchers.size() + desktopItems.size();
        if (deskDragSlot < 0 || deskDragSlot >= total) {
            return;
        }
        final boolean isLauncher = deskDragSlot < launchers.size();
        final DiskFilesPayload.WireFile src =
                isLauncher ? null : desktopItems.get(deskDragSlot - launchers.size());

        // (1) Drop onto an open Files explorer window: move the file into the folder it is showing.
        final DesktopWindow explorer = explorerWindowAt(dx, dy);
        if (explorer != null && explorer.app() instanceof FilesApp files && src != null) {
            final String destDir = files.crossWindowDropDir(explorer, dx, dy);
            if (destDir != null && !samePathParent(src.path(), destDir)) {
                if (src.readOnly()) {
                    showError("Error", DAT_LOCKED_MESSAGE);
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
        if (target >= launchers.size() && target != deskDragSlot && src != null) {
            final DiskFilesPayload.WireFile dst = desktopItems.get(target - launchers.size());
            if (dst.directory()) {
                if (src.readOnly()) {
                    showError("Error", DAT_LOCKED_MESSAGE);
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
        if (dy >= workTop() && dy < workBottom() && overWallpaper(dx, dy)) {
            final int cell = iconGrid.cellAt(dx, dy, perCol);
            if (iconGrid.cellTaken(cell, deskDragSlot, perCol)) {
                return; // the target cell is occupied; leave the icon where it was
            }
            final String key = iconGrid.keyOf(deskDragSlot);
            iconGrid.pin(key, cell);
            PacketDistributor.sendToServer(new SetIconPositionPayload(host, key, cell));
        }
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
        if (dy >= workTop() && dy < workBottom() && overWallpaper(dx, dy)) {
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
            showError("Error", DAT_LOCKED_MESSAGE);
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
    private void openSlot(final int slot) {
        if (slot < launchers.size()) {
            runLauncher(launchers.get(slot));
            return;
        }
        final int di = slot - launchers.size();
        if (di < 0 || di >= desktopItems.size()) {
            return;
        }
        final DiskFilesPayload.WireFile f = desktopItems.get(di);
        if (f.directory()) {
            openApp("Files", new FilesApp(host, desktopId.getPath(), f.path(), monitorPos));
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
                FileOpeners.defaultFor(path, installedPrograms, defaultApps);
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
    private void chooseOpener(final String path) {
        final List<String> programs = FileOpeners.choices(path, installedPrograms);
        if (programs.isEmpty()) {
            showBalloon("Cannot open", "No program on this computer opens " + FsPaths.fileName(path));
            return;
        }
        final String extension = FileOpeners.extensionOf(path);
        final String current =
                FileOpeners.defaultFor(path, installedPrograms, defaultApps);
        final String opener = current.isEmpty() ? "" : openerName(current);
        popup = new OpenWithPopup(path, extension, programs, opener, skin.iconSet(), font, (program, always) -> {
            if (always) {
                defaultApps.put(extension, program);
                PacketDistributor.sendToServer(
                        new SetSettingPayload(host, "defaultapp:" + extension,
                                program));
            }
            openIn(program, path);
        });
    }

    /**
     * Opens a file in a named program, or says why it cannot be opened at all.
     *
     * <p>Which program a kind of file belongs to is one answer, kept in one place, so a double-click, a
     * pick from "Open with" and a run from the explorer all reach the same one.
     */
    private void openIn(final String programId, final String path) {
        if (programId.isEmpty()) {
            /*
             * Nothing here claims the kind, but a language an addon brought may: its compiled programs
             * have an extension of their own, and opening one of those means running it.
             */
            if (runsAsProgram(path)) {
                runAtTerminal(path);
                return;
            }
            showBalloon("Cannot open", "No program on this computer opens " + FsPaths.fileName(path));
            return;
        }
        if (programId.equals(FileOpeners.EDITOR)) {
            final EditorApp editor = new EditorApp(host);
            openApp("Editor", editor);
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
        final Launcher launcher = spec == null ? null : launcherFor(spec.id());
        if (launcher == null) {
            showBalloon("Cannot open", "No program on this computer opens " + FsPaths.fileName(path));
            return;
        }
        runLauncher(launcher);
        /*
         * The window exists once the launcher has run, so the file goes to it straight away. An app that
         * opens no files ignores this, which is what lets any program be picked without a special case.
         */
        final DesktopWindow opened = windowFor(launcher.label());
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
        final String terminal = terminalLabel();
        if (terminal.isEmpty()) {
            return null;
        }
        final DesktopWindow open = windowFor(terminal);
        if (open != null && open.app() instanceof ShellApp shell) {
            open.setMinimized(false);
            bringToFront(windows.indexOf(open));
            return shell;
        }
        final IDesktopApp made = factoryFor(terminal);
        if (made instanceof ShellApp shell && allowOpen(terminal)) {
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

    /** The launcher of a program by id, or null when this desktop does not offer it. */
    private Launcher launcherFor(final ResourceLocation id) {
        for (final Launcher launcher : launchers) {
            if (id.equals(launcher.programId())) {
                return launcher;
            }
        }
        return null;
    }

    /**
     * Offers the programs on this machine that can open the file, so the player picks one.
     *
     * <p>A menu rather than a dialog: it is the same question as the one that was just asked with the
     * right button, and the answer is one of a handful of names.
     */
    private List<ContextMenu.Item> openWithItems(final String path) {
        final List<ContextMenu.Item> entries = new ArrayList<>();
        for (final String programId
                : FileOpeners.available(path, installedPrograms)) {
            final ProgramSpec spec = Programs.get(
                    ResourceLocation.fromNamespaceAndPath("jsc", programId));
            final String label = spec == null ? programId : spec.displayName();
            entries.add(new ContextMenu.Item(label, true, () -> openIn(programId, path)));
        }
        if (!FileOpeners.choices(path, installedPrograms).isEmpty()) {
            if (!entries.isEmpty()) {
                entries.add(ContextMenu.Item.separator());
            }
            entries.add(new ContextMenu.Item("Choose another program...", true,
                    () -> chooseOpener(path)));
        }
        if (entries.isEmpty()) {
            entries.add(new ContextMenu.Item("No program opens this", false, () -> { }));
        }
        return entries;
    }

    /** What New offers on the desktop: a folder first, then a file of every kind the machine can create. */
    private List<ContextMenu.Item> newDeskItems() {
        final List<ContextMenu.Item> entries = new ArrayList<>();
        entries.add(new ContextMenu.Item("Folder", true, deskFiles::newFolder));
        entries.add(ContextMenu.Item.separator());
        for (final FileType type
                : FileOpeners.creatable()) {
            entries.add(new ContextMenu.Item(
                    FilesApp.typeLabel(type) + " (." + type.extension() + ")", true,
                    () -> deskFiles.newFile(type)));
        }
        return entries;
    }

    /** Opens the Settings window on one of its pages, the way a menu entry names a page rather than the program. */
    private void openSettingsPage(final int page) {
        if (allowOpen("Settings")) {
            openApp("Settings", new SettingsApp(host, monitorPos).showPage(page));
        }
    }

    /** Runs the launcher labelled {@code label}, when the desktop has one. */
    private void runLauncherCalled(final String label) {
        for (final Launcher launcher : launchers) {
            if (launcher.label().equals(label)) {
                runLauncher(launcher);
                return;
            }
        }
    }

    /**
     * The panel's menu, drawn in the desktop's own chrome: the bevelled plate on the older Frames, a light
     * rounded panel on the newer one and on the Linux desktops. A separator row is drawn as a rule.
     */
    private void renderPanelContext(final GuiGraphics g, final int hoverMx, final int hoverMy) {
        final int mx = panelCtxX;
        final int my = panelCtxY;
        final int mh = PANEL_CTX.length * DESK_CTX_ITEM_H + 2;
        final boolean light = luminance(skin.text()) > 140;
        final int bg = light ? 0xFF262B36 : 0xFFE8E8EC;
        final int fg = light ? 0xFFE7E9EF : 0xFF1A2230;
        g.fill(mx - 1, my - 1, mx + PANEL_CTX_W + 1, my + mh + 1, light ? 0xFF11151E : 0xFF000000);
        g.fill(mx, my, mx + PANEL_CTX_W, my + mh, bg);
        g.fill(mx, my, mx + PANEL_CTX_W, my + 1, light ? 0xFF3A4150 : 0xFFFFFFFF);
        final int hover = panelCtxItemAt(hoverMx, hoverMy);
        int iy = my + 1;
        for (int k = 0; k < PANEL_CTX.length; k++) {
            if ("-".equals(PANEL_CTX[k])) {
                g.fill(mx + 4, iy + DESK_CTX_ITEM_H / 2, mx + PANEL_CTX_W - 4,
                        iy + DESK_CTX_ITEM_H / 2 + 1, light ? 0xFF3A4150 : 0xFFB6BAC4);
            } else {
                if (k == hover) {
                    g.fill(mx + 1, iy, mx + PANEL_CTX_W - 1, iy + DESK_CTX_ITEM_H, skin.accent());
                }
                g.drawString(font, PANEL_CTX[k], mx + 4, iy + 2, k == hover ? 0xFFFFFFFF : fg, false);
            }
            iy += DESK_CTX_ITEM_H;
        }
    }

    /** The panel-menu entry under a desktop-local point, or {@code -1}; a separator never answers. */
    private int panelCtxItemAt(final double mx, final double my) {
        if (!panelCtxOpen || mx < panelCtxX || mx > panelCtxX + PANEL_CTX_W) {
            return -1;
        }
        final int rel = (int) Math.floor((my - (panelCtxY + 1)) / (double) DESK_CTX_ITEM_H);
        if (rel < 0 || rel >= PANEL_CTX.length || "-".equals(PANEL_CTX[rel])) {
            return -1;
        }
        return rel;
    }

    /** Raises the panel's menu at a point, clamped so it stays on the desktop. */
    private void openPanelMenu(final int atX, final int panelY) {
        final int mh = PANEL_CTX.length * DESK_CTX_ITEM_H + 2;
        panelCtxOpen = true;
        panelCtxX = Math.max(2, Math.min(sw() - PANEL_CTX_W - 2, atX));
        // Above a bottom panel, below a top one: the menu never covers the bar it came from.
        panelCtxY = topPanel() ? panelY + TASKBAR_H + 2 : panelY - mh - 2;
    }

    /** Runs a panel-menu entry. Every one of them does something: none is there for decoration. */
    private void runPanelMenu(final int index) {
        switch (PANEL_CTX[index]) {
            case "Cascade Windows" -> cascadeWindows();
            case "Show the Desktop" -> {
                for (final DesktopWindow w : windows) {
                    w.setMinimized(true);
                }
            }
            case "Task Manager" -> openTaskManager();
            default -> {
            }
        }
    }

    /** Steps the open windows down and to the right from the work area's corner, the way a cascade does. */
    private void cascadeWindows() {
        int step = 0;
        for (final DesktopWindow w : windows) {
            if (away(w)) {
                continue;
            }
            w.setMaximized(false);
            w.moveTo(16 + step * 12, workTop() + 10 + step * 12, workTop(), sw(), workBottom());
            step++;
        }
    }

    /**
     * Opens the menu for whatever the cursor is on: a program, a file or folder, or the wallpaper.
     *
     * <p>Each gets the entries that mean something for it, which is why a program no longer offers to
     * be renamed and the wallpaper no longer offers to be opened. The entries are the ones a desktop
     * has: a file opens, opens with a chosen program, is renamed, deleted or looked at; the wallpaper
     * makes new things, refreshes, and leads to the settings that dress it.
     */
    private void openDeskContext(final int slot, final int x, final int y) {
        final List<ContextMenu.Item> entries = new ArrayList<>();
        if (slot >= 0 && slot < launchers.size()) {
            final Launcher launcher = launchers.get(slot);
            entries.add(deskItem("Open", true, () -> runLauncher(launcher)));
            /*
             * Only what the machine could actually take off: the programs that ship with a system are
             * part of it, so offering to remove one would be offering something that then fails.
             */
            final ProgramSpec spec =
                    Programs.get(launcher.programId());
            if (pinsOnPanel() && launcher.factory() != null) {
                final boolean pinned = pinnedPrograms.contains(launcher.programId().getPath());
                entries.add(deskItem(pinned ? "Unpin from taskbar" : "Pin to taskbar", true,
                        () -> togglePin(launcher.label())));
            }
            if (spec != null && spec.installable()) {
                entries.add(ContextMenu.Item.separator());
                entries.add(deskItem("Uninstall", true, () -> uninstallLauncher(spec)));
            }
        } else if (slot >= launchers.size() && slot - launchers.size() < desktopItems.size()) {
            final int di = slot - launchers.size();
            final DiskFilesPayload.WireFile file = desktopItems.get(di);
            entries.add(deskItem("Open", true, () -> openSlot(launchers.size() + di)));
            if (!file.directory()) {
                entries.add(ContextMenu.Item.submenu("Open with", openWithItems(file.path())));
            }
            entries.add(ContextMenu.Item.separator());
            /*
             * A projection of what a drive holds is not a file anybody wrote, so it cannot be renamed or
             * deleted by hand; the filesystem refuses both, and a menu that offered them would be lying.
             */
            entries.add(deskItem("Rename", !file.readOnly(), () -> deskFiles.startRename(di)));
            entries.add(deskItem("Delete", !file.readOnly(), () -> deskFiles.delete(di)));
            entries.add(ContextMenu.Item.separator());
            entries.add(deskItem("Properties", true, () -> requestFileProperties(file.path())));
        } else {
            entries.add(ContextMenu.Item.submenu("New", newDeskItems()));
            entries.add(ContextMenu.Item.separator());
            entries.add(deskItem("Refresh", true, this::requestDesktop));
            entries.add(ContextMenu.Item.separator());
            entries.add(deskItem("Display settings", true, () -> openSettingsPage(SettingsApp.PAGE_DISPLAY)));
            entries.add(deskItem("Personalize", true, () -> openSettingsPage(SettingsApp.PAGE_PERSONALIZE)));
            entries.add(ContextMenu.Item.separator());
            entries.add(deskItem("Properties", true, () -> runLauncherCalled("This PC")));
        }
        deskMenu.open(entries, x, y, 0, 0, sw(), sh());
    }

    private static ContextMenu.Item deskItem(
            final String label, final boolean enabled, final Runnable action) {
        return new ContextMenu.Item(label, enabled, action);
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
        final List<String> out = new ArrayList<>();
        for (final ContextMenu.Item item : deskMenu.items()) {
            out.add(item.label());
        }
        return out;
    }

    /** The desktop-local centre of item {@code index} of the menu open beside the desk menu, or null when none is. */
    public int[] deskSubmenuItemCenter(final int index) {
        final ContextMenu sub = deskMenu.openSubmenu();
        return sub == null ? null : sub.itemCenter(index);
    }

    /** The names of the files and folders on the desktop, as their icons read. */
    public List<String> desktopItemNames() {
        final List<String> out = new ArrayList<>();
        for (final DiskFilesPayload.WireFile file : desktopItems) {
            out.add(FsPaths.fileName(file.path()));
        }
        return out;
    }

    /** The scale the desktop is drawn at, as a factor, which a test needs to land a click on a scaled desktop. */
    public double desktopScale() {
        return scale();
    }

    /** A queued request to show a file's Properties window: the explorer opens on its folder and shows it. */
    private static final String OPEN_PROPS = "Props\0";

    /** Asks for the Properties window of a file on the desktop, which the explorer knows how to show. */
    public static void requestFileProperties(final String path) {
        PENDING_OPEN.add(OPEN_PROPS + path);
    }

    /** Takes a program off this computer, the way {@code uninstall} at the prompt does. */
    private void uninstallLauncher(final ProgramSpec spec) {
        PacketDistributor.sendToServer(
                new DesktopShellRunPayload(host, "uninstall " + spec.commandName()));
    }

    /** The overall width of the Start menu panel, which differs per Frames version. */
    private int startMenuW() {
        if (periodPanel()) {
            return MENU_W; // the period launcher is a narrow list, whatever its desktop does today
        }
        return switch (panel) {
            case FRAMES_XP -> XP_MENU_W;
            case FRAMES_11 -> W11_MENU_W;
            case KDE -> KDE_MENU_W;
            case GNOME -> sw();
            case CINNAMON -> CIN_MENU_W;
            default -> MENU_W;
        };
    }

    private int startMenuHeight() {
        if (periodPanel()) {
            /*
             * A period launcher is a small program list, not a Plasma menu and not a full-screen
             * overview: it is sized by its own contents, like the classic launcher it is.
             */
            return Math.max(4, launchers.size()) * MENU_ITEM_H + 8;
        }
        switch (panel) {
            case KDE -> {
                return KDE_HEADER_H + Math.max(6, launchers.size()) * KDE_ROW_H + KDE_FOOTER_H + 8;
            }
            case GNOME -> {
                return sh() - TASKBAR_H; // the overview covers the whole desktop below the top bar
            }
            case CINNAMON -> {
                return CIN_HEADER_H + Math.max(5, launchers.size()) * CIN_ROW_H + 10;
            }
            default -> {
            }
        }
        return switch (desktopId.getPath()) {
            // Two columns: the taller of programs (left) and places (right) sets the body height.
            case "frames_xp" -> XP_HEADER_H + XP_ORANGE_H
                    + Math.max(xpLeftColumnH(), xpRightLaunchers().size() * XP_ROW_H)
                    + XP_FOOTER_H + 6;
            /*
             * A pinned grid sized to the full app set (so the panel does not resize as the search filters it).
             * Layout: 6 top pad + search + 5 + 9 (Pinned label) + rows + 5 + footer + 5 bottom pad.
             */
            case "frames_11" -> {
                final int gridRows = Math.max(1, (launchers.size() + W11_COLS - 1) / W11_COLS);
                yield 6 + W11_SEARCH_H + 5 + 9 + gridRows * W11_TILE_H + 5 + W11_FOOTER_H + 5;
            }
            default -> launchers.size() * MENU_ITEM_H + 6 + MENU_ITEM_H + 8;
        };
    }

    /**
     * The left edge of the app-icon strip on the Windows 11 taskbar: right after the Start button, so the whole
     * [Start + apps] group moves together with the taskbar alignment. Single source for render and hit-test.
     */
    private int win11AppsX(final int sw) {
        return win11StartX(sw) + WIN11_SLOT;
    }

    /**
     * The Frames 11 Start button's left edge. Centered mode places it as the leftmost of the centered
     * [Start + open apps] group (so the whole group is centered); left mode pins it to the corner. This is
     * why the taskbar-alignment setting actually moves the Start button.
     */
    private int win11StartX(final int sw) {
        if (!desktopTaskbarCentered) {
            return 4;
        }
        final int group = (taskEntries().size() + 1) * WIN11_SLOT;
        return Math.max(4, (sw - group) / 2);
    }

    /** The Start menu's left edge: left-pinned on 95/XP; on Frames 11 it opens over the Start button (clamped). */
    private int startMenuX() {
        if (is(PanelStyle.FRAMES_11)) {
            final int w = startMenuW();
            final int startCenter = win11StartX(sw()) + WIN11_SLOT / 2;
            return Math.max(4, Math.min(sw() - w - 4, startCenter - w / 2));
        }
        if (topPanel()) {
            return 0; // the Activities overview spans the desktop
        }
        return 4;
    }

    /** The Start menu's top edge for the given taskbar top: flush on 95/XP, floating with a gap on Frames 11. */
    private int startMenuY(final int tbY) {
        if (is(PanelStyle.FRAMES_11)) {
            return Math.max(2, tbY - startMenuHeight() - 6); // float above the taskbar, but never off the top
        }
        if (topPanel()) {
            return TASKBAR_H; // the overview hangs below the top bar
        }
        return tbY - startMenuHeight();
    }

    /** The launchers shown in the XP left "programs" column: everything that is not a fixed system place. */
    private List<Launcher> xpLeftLaunchers() {
        final List<Launcher> out = new ArrayList<>();
        for (final Launcher l : launchers) {
            if (!XP_PLACES.contains(l.label())) {
                out.add(l);
            }
        }
        return out;
    }

    /** The y of the {@code i}-th left-column row, measured from the top of the Start menu's body. */
    private int xpLeftRowY(final int i) {
        final int n = xpLeftLaunchers().size();
        final int pinned = Math.min(XP_PINNED, n);
        return i * XP_ROW_H + (i >= pinned && n > pinned ? XP_SEP_H : 0);
    }

    /** The y of the "All Programs" row, measured from the top of the Start menu's body. */
    private int xpAllRowY() {
        return xpLeftRowY(xpLeftLaunchers().size()) + XP_SEP_H;
    }

    /** How tall the left column runs: its rows, its separators, and the "All Programs" row under them. */
    private int xpLeftColumnH() {
        return xpAllRowY() + XP_ALL_ROW_H;
    }

    /** The left edge of the footer's "Turn Off Computer" entry, shared by the drawing and the hit-test. */
    private int xpFooterOffX(final int x, final int w) {
        return x + w - 6 - (font.width("Turn Off Computer") + 14);
    }

    /** The left edge of the footer's "Log Off" entry, immediately before the Turn Off one. */
    private int xpFooterLogX(final int x, final int w) {
        return xpFooterOffX(x, w) - 8 - (font.width("Log Off") + 14);
    }

    /** The launchers shown in the XP right "places" column: the fixed system entries, in launcher order. */
    private List<Launcher> xpRightLaunchers() {
        final List<Launcher> out = new ArrayList<>();
        for (final Launcher l : launchers) {
            if (XP_PLACES.contains(l.label())) {
                out.add(l);
            }
        }
        return out;
    }

    /** The launchers matching the Frames 11 Start search box (all of them when the box is empty). */
    private List<Launcher> w11Filtered() {
        final String q = startSearch.toString().toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            return launchers;
        }
        final List<Launcher> out = new ArrayList<>();
        for (final Launcher l : launchers) {
            if (l.label().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(l);
            }
        }
        return out;
    }

    /** A small downward caret: a program with several windows says so at the end of its button. */
    private static void drawCaret(final GuiGraphics g, final int x, final int y, final int color) {
        g.fill(x, y, x + 5, y + 1, color);
        g.fill(x + 1, y + 1, x + 4, y + 2, color);
        g.fill(x + 2, y + 2, x + 3, y + 3, color);
    }

    /** What a task button says: the window's title, or the count and the program when there are several. */
    private String entryLabel(final TaskbarGroups.Entry entry) {
        if (entry.windows() > 1) {
            return entry.windows() + " " + entry.key();
        }
        final List<DesktopWindow> mine = groupWindows(entry.key());
        return mine.isEmpty() ? entry.key() : titleOf(mine.get(mine.size() - 1));
    }

    /** The balloon's box in desktop-local coordinates, or null when none is up. Draw and hit-test share it. */
    @Nullable
    private int[] balloonRect(final int tbY, final int sw) {
        if (balloon == null) {
            return null;
        }
        final int lines = font.split(Component.literal(balloon.body()), BALLOON_W - 12).size();
        final int h = 15 + lines * 9 + 5;
        final int x = Math.max(4, sw - BALLOON_W - 6);
        return new int[] {x, tbY - h - 7, BALLOON_W, h};
    }

    /** The classic notification balloon: pale yellow, a blue "i", a title, a line or two, and a close box. */
    private void renderBalloon(final GuiGraphics g, final int tbY, final int sw) {
        if (balloon != null && System.currentTimeMillis() > balloon.until()) {
            balloon = null;
        }
        final int[] r = balloonRect(tbY, sw);
        if (r == null || balloon == null) {
            return;
        }
        final int x = r[0];
        final int y = r[1];
        final int w = r[2];
        final int h = r[3];
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        g.fill(x, y, x + w, y + h, 0xFFFFFFE1);
        /*
         * The tail, pointing down at the notification area it came from: a bordered wedge, drawn as an
         * outline first and the pale fill inset into it, so it carries the same 1px edge as the box.
         */
        final int tail = x + w - 42;
        for (int i = 0; i < 7; i++) {
            g.fill(tail + i - 1, y + h + i, tail + 14 - i, y + h + i + 1, 0xFF000000);
        }
        for (int i = 0; i < 6; i++) {
            g.fill(tail + i, y + h + i, tail + 12 - i, y + h + i + 1, 0xFFFFFFE1);
        }
        g.fill(tail, y + h - 1, tail + 12, y + h, 0xFFFFFFE1); // the tail opens into the balloon
        // The round blue "i" and the title beside it.
        g.fill(x + 6, y + 5, x + 14, y + 13, 0xFF1C53C9);
        g.fill(x + 7, y + 4, x + 13, y + 14, 0xFF1C53C9);
        g.fill(x + 9, y + 6, x + 11, y + 7, 0xFFFFFFFF);
        g.fill(x + 9, y + 8, x + 11, y + 12, 0xFFFFFFFF);
        g.drawString(font, Component.literal(balloon.title()).withStyle(ChatFormatting.BOLD),
                x + 18, y + 5, 0xFF000000, false);
        int ly = y + 16;
        for (final FormattedCharSequence line
                : font.split(Component.literal(balloon.body()), w - 12)) {
            g.drawString(font, line, x + 6, ly, 0xFF303030, false);
            ly += 9;
        }
        // The close box, the one part of a balloon anyone ever clicked.
        final int bx = x + w - 12;
        g.fill(bx, y + 4, bx + 8, y + 12, 0xFFE8E8CA);
        outline(g, bx, y + 4, 8, 8, 0xFF6A6A55);
        g.drawString(font, "x", bx + 2, y + 5, 0xFF303030, false);
    }

    /** A click on a live balloon: its close box dismisses it, and the rest of it absorbs the click. */
    private boolean balloonClick(final double mx, final double my, final int tbY, final int sw) {
        final int[] r = balloonRect(tbY, sw);
        if (r == null) {
            return false;
        }
        if (mx < r[0] || mx > r[0] + r[2] || my < r[1] || my > r[1] + r[3]) {
            return false;
        }
        /*
         * The close box only puts it away; anywhere else on a balloon that carries an offer takes it up, which
         * is what made those balloons worth clicking rather than worth dismissing.
         */
        final String opens = balloon == null ? "" : balloon.opens();
        final boolean onClose = mx >= r[0] + r[2] - 14;
        balloon = null;
        if (!opens.isEmpty() && !onClose) {
            final IDesktopApp app = factoryFor(opens);
            if (app != null) {
                openApp(opens, app);
            }
        }
        return true;
    }

    /** The memory meter's text, "used/total MB", shared by the panels so they can keep room for it. */
    private String ramMeterText() {
        return ramUsedMb() + "/" + ramTotalMb + " MB";
    }

    /*
     * The notification area, the same on every panel: whether the machine is on a network, a speaker, a
     * memory bar and the clock. It is deliberately narrow (a wordy meter here left no room for the task
     * buttons) and the figures behind the bar are one hover away.
     */
    /** The task strip: where it starts, the gap between buttons, and the width one may run to. */
    private static final int TASK_X = 64;
    private static final int TASK_GAP = 4;
    private static final int TASK_MIN_W = 44;
    private static final int TASK_MAX_W = 84;

    /**
     * Where every panel entry sits this frame, so the drawing and the hit tests share one reckoning.
     *
     * @param entries    the programs on the panel, in panel order
     * @param x          the left edge of each entry's button or cell
     * @param w          the width of each; zero for an entry the strip does not show (a Frames XP pin,
     *                   which lives on the quick launch instead)
     * @param quickX     where the Frames XP quick launch starts
     * @param quickCount how many icons the quick launch holds
     * @param right      where the strip must stop, clear of the notification area
     */
    record TaskStrip(List<TaskbarGroups.Entry> entries, int[] x, int[] w,
                     int quickX, int quickCount, int right) {

        /** The entry whose button or cell is under a desktop-local x, or -1. */
        int indexAt(final double mx) {
            for (int i = 0; i < entries.size(); i++) {
                if (w[i] > 0 && x[i] + w[i] <= right && mx >= x[i] && mx < x[i] + w[i]) {
                    return i;
                }
            }
            return -1;
        }

        /** The place of entry {@code index} on the quick launch (its rank among the pinned), or -1. */
        int quickIndexOf(final int index) {
            if (quickCount == 0 || !entries.get(index).pinned()) {
                return -1;
            }
            int j = 0;
            for (int i = 0; i < index; i++) {
                if (entries.get(i).pinned()) {
                    j++;
                }
            }
            return j;
        }

        /** The entry whose quick launch icon is under a desktop-local x, or -1. */
        int quickEntryAt(final double mx) {
            if (quickCount == 0 || mx < quickX || mx >= quickX + quickCount * QL_W) {
                return -1;
            }
            final int wanted = (int) ((mx - quickX) / QL_W);
            int j = 0;
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).pinned()) {
                    if (j == wanted) {
                        return i;
                    }
                    j++;
                }
            }
            return -1;
        }
    }

    /**
     * The strip shared out between the programs, never wider than is comfortable nor narrower than a name
     * can be read in. A taskbar that keeps one fixed width simply runs out of room, which is what hid the
     * open programs behind the notification area. Frames 11 gives every program one slot; Frames XP keeps
     * the pinned programs on a quick launch beside Start; KDE and Cinnamon keep a pinned program in place
     * as an icon until it opens; the older panels list only what is open.
     */
    private TaskStrip taskStrip(final int sw) {
        final List<TaskbarGroups.Entry> entries = taskEntries();
        final int n = entries.size();
        final int[] x = new int[n];
        final int[] w = new int[n];
        final int right = taskStripRight(sw);
        if (is(PanelStyle.FRAMES_11)) {
            final int appsX = win11AppsX(sw);
            for (int i = 0; i < n; i++) {
                x[i] = appsX + i * WIN11_SLOT;
                w[i] = WIN11_SLOT;
            }
            return new TaskStrip(entries, x, w, 0, 0, sw);
        }
        int left = TASK_X;
        int quickX = 0;
        int quickCount = 0;
        if (is(PanelStyle.FRAMES_XP)) {
            quickX = XP_START_W + 4;
            for (final TaskbarGroups.Entry entry : entries) {
                if (entry.pinned()) {
                    quickCount++;
                }
            }
            left = quickCount > 0 ? quickX + quickCount * QL_W + 6 : TASK_X;
        }
        final boolean launcherCells = linuxDesktop() && !periodPanel();
        int openCount = 0;
        int launcherCount = 0;
        for (final TaskbarGroups.Entry entry : entries) {
            if (entry.open()) {
                openCount++;
            } else if (launcherCells) {
                launcherCount++;
            }
        }
        final int strip = Math.max(0, right - left - launcherCount * LAUNCHER_W);
        final int btnW = openCount == 0 ? 0
                : Math.max(TASK_MIN_W, Math.min(TASK_MAX_W, strip / openCount - TASK_GAP));
        int cx = left;
        for (int i = 0; i < n; i++) {
            final TaskbarGroups.Entry entry = entries.get(i);
            if (entry.open()) {
                x[i] = cx;
                w[i] = btnW;
                cx += btnW + TASK_GAP;
            } else if (launcherCells) {
                x[i] = cx;
                w[i] = LAUNCHER_W;
                cx += LAUNCHER_W;
            }
        }
        return new TaskStrip(entries, x, w, quickX, quickCount, right);
    }

    /** The programs on the panel, in its order, grouped from the windows and the pins. */
    private List<TaskbarGroups.Entry> taskEntries() {
        final List<TaskbarGroups.Window> list = new ArrayList<>(windows.size());
        for (final DesktopWindow w : windows) {
            // A panel lists what is on the workspace that is up; the rest are met by going to theirs.
            if (w.on(shownWorkspace)) {
                list.add(new TaskbarGroups.Window(w.groupKey(), w.minimized(), w.serial()));
            }
        }
        final DesktopWindow front = frontWindow();
        return TaskbarGroups.group(list, pinnedKeys(), front == null ? null : front.groupKey());
    }

    /**
     * Whether this panel keeps pinned programs in view. The Frames 95 taskbar and the period panels
     * never had a place for them, and the GNOME top bar lists no programs at all.
     */
    private boolean pinsOnPanel() {
        if (periodPanel()) {
            return false;
        }
        return is(PanelStyle.FRAMES_11) || is(PanelStyle.FRAMES_XP)
                || is(PanelStyle.KDE) || is(PanelStyle.CINNAMON);
    }

    /** Whether the panel's popup shows the windows' live pictures (a modern panel) rather than their titles. */
    private boolean thumbnailPopups() {
        return !periodPanel() && (is(PanelStyle.FRAMES_11)
                || is(PanelStyle.KDE) || is(PanelStyle.CINNAMON));
    }

    /** The pinned programs by the label the panel lists them under; only those this desktop can start. */
    private List<String> pinnedKeys() {
        final List<String> out = new ArrayList<>();
        if (!pinsOnPanel()) {
            return out;
        }
        for (final String id : pinnedPrograms) {
            for (final Launcher launcher : launchers) {
                if (launcher.factory() != null && launcher.programId().getPath().equals(id)) {
                    out.add(launcher.label());
                    break;
                }
            }
        }
        return out;
    }

    /** The launcher a panel entry stands for, or null for a program with no launcher (the Task Manager). */
    @Nullable
    private Launcher pinnableLauncher(final String key) {
        for (final Launcher launcher : launchers) {
            if (launcher.factory() != null && launcher.label().equals(key)) {
                return launcher;
            }
        }
        return null;
    }

    /**
     * Pins a program to the panel or takes it off, and tells the machine, which keeps the list. The
     * panel changes at once rather than waiting for the machine's reply.
     */
    private void togglePin(final String key) {
        final Launcher launcher = pinnableLauncher(key);
        if (launcher == null) {
            return;
        }
        final String id = launcher.programId().getPath();
        final boolean pinned = pinnedPrograms.contains(id);
        if (pinned) {
            pinnedPrograms.remove(id);
        } else {
            pinnedPrograms.add(id);
        }
        PacketDistributor.sendToServer(new SetSettingPayload(
                host, pinned ? "unpin" : "pin", id));
    }

    /** How many characters of a title fit on a task button of {@code w} pixels, after its icon. */
    private static int taskTitleChars(final int w) {
        return Math.max(3, (w - 24) / 6);
    }

    /** Where a panel's task buttons must stop: clear of the notification area at its right end. */
    private int taskStripRight(final int sw) {
        return tray.taskStripRight(sw);
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

    private String osBandLabel() {
        return desktopName();
    }

    /**
     * Draws the Start menu. Each Frames version has its OWN layout, not just its own palette: 95 is the
     * classic side-band vertical list, XP is a two-column programs/places panel, and 11 is a centered
     * floating panel with a search box and a pinned-app grid.
     */
    private void renderStartMenu(final GuiGraphics g, final int tbY) {
        if (periodPanel()) {
            /*
             * A period desktop had a plain vertical launcher, not a modern Plasma menu and certainly not
             * the GNOME overview, which belongs to a shell released a decade later.
             */
            renderStartMenuPeriod(g, tbY);
            return;
        }
        switch (panel) {
            case FRAMES_XP -> renderStartMenuXp(g, tbY);
            case FRAMES_11 -> renderStartMenu11(g, tbY);
            case KDE -> renderStartMenuKde(g, tbY);
            case GNOME -> renderOverviewGnome(g);
            case CINNAMON -> renderStartMenuCinnamon(g, tbY);
            default -> renderStartMenu95(g, tbY);
        }
    }

    /**
     * The launcher of a Legacy-era Unix desktop: a raised panel with a coloured side band carrying the
     * desktop's name and one vertical list of programs. Drawn from the skin's own primitives, so it
     * carries the same relief as that skin's windows and panel instead of the modern flat chrome.
     */
    private void renderStartMenuPeriod(final GuiGraphics g, final int tbY) {
        framesLaunchers.renderPeriod(g, tbY);
    }

    /** Frames 95: the classic Start menu with a rotated OS-name side band and a single vertical program list. */
    private void renderStartMenu95(final GuiGraphics g, final int tbY) {
        framesLaunchers.render95(g, tbY);
    }

    /** Whether the open launcher has a live search box (Frames 11's Start, GNOME's Activities overview). */
    private boolean searchableStart() {
        /*
         * The period launcher is a plain program list with no search field, so it is not searchable
         * even though the modern GNOME shell it replaces is.
         */
        return is(PanelStyle.FRAMES_11)
                || (is(PanelStyle.GNOME) && !periodPanel());
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

    // Linux desktop environments: launchers, which LinuxLaunchers draws and hit-tests beside this screen

    private void renderStartMenuKde(final GuiGraphics g, final int tbY) {
        linuxLaunchers.renderKde(g, tbY);
    }

    private boolean handleStartClickKde(final int mx, final int my, final int tbY) {
        return linuxLaunchers.clickKde(mx, my, tbY);
    }

    private void renderOverviewGnome(final GuiGraphics g) {
        linuxLaunchers.renderGnomeOverview(g);
    }

    private boolean handleOverviewClickGnome(final int mx, final int my) {
        return linuxLaunchers.clickGnomeOverview(mx, my);
    }

    private void renderStartMenuCinnamon(final GuiGraphics g, final int tbY) {
        linuxLaunchers.renderCinnamon(g, tbY);
    }

    private boolean handleStartClickCinnamon(final int mx, final int my, final int tbY) {
        return linuxLaunchers.clickCinnamon(mx, my, tbY);
    }

    /** Frames XP: a two-column panel (programs on the left, system places on the right) with header/footer bands. */
    private void renderStartMenuXp(final GuiGraphics g, final int tbY) {
        framesLaunchers.renderXp(g, tbY);
    }

    /** Frames 11: a centered floating panel with a search box, a pinned-app grid, and a footer power button. */
    private void renderStartMenu11(final GuiGraphics g, final int tbY) {
        framesLaunchers.render11(g, tbY);
    }

    /** A short account line for the Frames 11 Start footer: the computer's name, or a generic label. */
    private String hostAccountLabel() {
        return (computerName == null || computerName.isBlank()) ? "Local account" : trim(computerName, 22);
    }

    /** A thin one-pixel rectangle outline used by the Frames 11 Start panel. */
    private static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h,
                               final int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Closes the Start menu and clears any Frames 11 search text so it reopens fresh. */
    private void closeStart() {
        startOpen = false;
        startSearch.setLength(0);
    }

    /*
     * Power
     *
     * Shutting down used to close the window and leave the machine running, so the computer stayed on
     * the network with everything still open. The three real choices now live in one dialog, and each
     * one reaches the machine.
     */

    private static final int POWER_W = 190;
    private static final int POWER_ROW_H = 20;
    private static final String[][] POWER_CHOICES = {
            {"Shut down", "the machine powers off"},
            {"Restart", "power-cycle, back at the POST"},
            {"Log off", "leave the screen, keep it running"},
    };

    private boolean powerOpen;
    // The desktop surface the dialog was centred on, so the click test lands where it was drawn.
    private int powerSurfaceW;
    private int powerSurfaceH;

    // The panel's entries: a program's menu, its windows, and the popup that lists them

    private static final int TASK_MENU_W = 118;

    /** A program's own menu, from its panel entry: the same component every menu on this desktop is. */
    private final ContextMenu taskMenu =
            new ContextMenu(TASK_MENU_W, DESK_CTX_ITEM_H);

    /** Opens a program's menu over its panel entry: what can be done with its windows and its pin. */
    private void openTaskMenu(final TaskbarGroups.Entry entry, final int atX, final int tbY) {
        final String key = entry.key();
        final List<ContextMenu.Item> items = new ArrayList<>();
        final boolean pinnable = pinsOnPanel() && pinnableLauncher(key) != null;
        if (entry.open()) {
            final boolean several = entry.windows() > 1;
            final boolean minimized = entry.state() == TaskbarGroups.State.MINIMIZED;
            items.add(deskItem(several ? "Restore all" : minimized ? "Restore" : "Bring to front", true,
                    () -> restoreGroup(key)));
            items.add(deskItem(several ? "Minimize all" : "Minimize", true, () -> minimizeGroup(key)));
            if (!several) {
                items.add(deskItem("Maximize", true, () -> {
                    restoreGroup(key);
                    final List<DesktopWindow> mine = groupWindows(key);
                    if (!mine.isEmpty()) {
                        mine.get(0).setMaximized(!mine.get(0).maximized());
                    }
                }));
            }
            items.add(deskItem("Minimize others", true, () -> minimizeOthers(key)));
            if (pinnable) {
                items.add(ContextMenu.Item.separator());
                items.add(deskItem(entry.pinned() ? "Unpin from taskbar" : "Pin to taskbar", true, () -> togglePin(key)));
            }
            items.add(ContextMenu.Item.separator());
            items.add(deskItem(several ? "Close all windows" : "Close", true, () -> closeGroup(key)));
        } else {
            items.add(deskItem("Open", true, () -> runLauncherCalled(key)));
            if (pinnable) {
                items.add(ContextMenu.Item.separator());
                items.add(deskItem("Unpin from taskbar", true, () -> togglePin(key)));
            }
        }
        taskPopup.dismiss();
        final int h = items.size() * DESK_CTX_ITEM_H + 2;
        // Above a bottom panel, below a top one: the menu never covers the entry it came from.
        final int y = topPanel() ? TASKBAR_H + 2 : tbY - h - 2;
        taskMenu.open(items, atX, y, 0, 0, sw(), sh());
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

    private void bringGroupToFront(final String key) {
        for (final DesktopWindow w : groupWindows(key)) {
            if (!w.dialog()) {
                bringWindowToFront(w);
            }
        }
    }

    private void restoreGroup(final String key) {
        for (final DesktopWindow w : groupWindows(key)) {
            w.setMinimized(false);
        }
        bringGroupToFront(key);
    }

    private void minimizeGroup(final String key) {
        for (final DesktopWindow w : groupWindows(key)) {
            w.setMinimized(true);
        }
    }

    private void minimizeOthers(final String key) {
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
        final int top = workTop();
        final int w = Math.max(dialog.minWidth(), Math.min(dialog.defaultWidth(), sw() - 8));
        final int h = Math.max(dialog.minHeight(), Math.min(dialog.defaultHeight(), workBottom() - top - 8));
        /*
         * Centred across the owner and hung just under its title bar, so the owner's name and edges stay
         * in view around the question it is asking, and the two read as two windows rather than one.
         */
        final int x = Math.max(0, Math.min(ownerWin.x() + (ownerWin.width() - w) / 2, sw() - w));
        final int y = Math.max(top, Math.min(ownerWin.y() + DesktopWindow.TITLE_H + 6, workBottom() - h));
        dialog.applySkin(skin);
        final DesktopWindow made = new DesktopWindow(dialog, ownerWin.appKey(), x, y, w, h);
        made.setOwner(ownerWin);
        made.setWorkspaces(ownerWin.workspaces());
        ownerWin.setMinimized(false);
        bringWindowToFront(ownerWin);
        windows.add(made);
    }

    /** A click on a panel entry: the program's menu, its window, or the popup listing several of them. */
    private void clickTaskEntry(final TaskbarGroups.Entry entry, final int atX,
                                final int button, final int tbY) {
        if (button == 1) {
            openTaskMenu(entry, atX, tbY);
            return;
        }
        if (button != 0) {
            return;
        }
        if (!entry.open()) {
            runLauncherCalled(entry.key());
            return;
        }
        if (entry.windows() == 1) {
            final List<DesktopWindow> mine = groupWindows(entry.key());
            if (entry.state() == TaskbarGroups.State.ACTIVE) {
                minimizeGroup(entry.key());
            } else if (mine.get(0).minimized()) {
                restoreGroup(entry.key());
            } else {
                bringGroupToFront(entry.key());
            }
            return;
        }
        // Several windows: the popup lists them, and stays until a click puts it away.
        taskPopup.openFor(entry.key());
    }

    private void openPowerDialog() {
        powerOpen = true;
    }

    private int powerHeight() {
        return 22 + POWER_CHOICES.length * POWER_ROW_H;
    }

    private int powerX() {
        return (powerSurfaceW - POWER_W) / 2;
    }

    private int powerY() {
        return (powerSurfaceH - powerHeight()) / 2;
    }

    /** Draws the power dialog over the desktop; returns false when it is not open. */
    private boolean renderPowerDialog(final GuiGraphics g, final int surfaceW, final int surfaceH,
                                      final int mouseX, final int mouseY) {
        if (!powerOpen) {
            return false;
        }
        powerSurfaceW = surfaceW;
        powerSurfaceH = surfaceH;
        // CDE asks its own way: how many programs are open, then Shut Down, Restart or Cancel.
        if (is(PanelStyle.CDE)) {
            CdeExitDialog.render(g, font, surfaceW, surfaceH, openPrograms(), cdePalette());
            return true;
        }
        final int x = powerX();
        final int y = powerY();
        g.fill(0, 0, surfaceW, surfaceH, 0x99000000);
        skin.windowShadow(g, x, y, POWER_W, powerHeight());
        skin.windowFrame(g, x, y, POWER_W, powerHeight());
        skin.titleBar(g, x, y, POWER_W, 14);
        g.drawString(font, "Power", x + 6, y + 3, skin.titleText(), false);
        for (int i = 0; i < POWER_CHOICES.length; i++) {
            final int rowY = y + 18 + i * POWER_ROW_H;
            final boolean hovered = mouseX >= x + 4 && mouseX < x + POWER_W - 4
                    && mouseY >= rowY && mouseY < rowY + POWER_ROW_H - 2;
            if (hovered) {
                g.fill(x + 4, rowY, x + POWER_W - 4, rowY + POWER_ROW_H - 2, skin.listHover());
            }
            g.drawString(font, POWER_CHOICES[i][0], x + 12, rowY + 2, skin.text(), false);
            g.drawString(font, POWER_CHOICES[i][1], x + 12, rowY + 11, skin.dim(), false);
        }
        return true;
    }

    /**
     * Handles a click while the power dialog is up; returns whether it consumed the click. The
     * coordinates come in absolute and are moved into the desktop's own space, where it was drawn.
     */
    private boolean clickPowerDialog(final double mouseXAbs, final double mouseYAbs) {
        if (!powerOpen) {
            return false;
        }
        final double mouseX = lx(mouseXAbs);
        final double mouseY = ly(mouseYAbs);
        if (is(PanelStyle.CDE)) {
            // A question with a Cancel of its own stays up until one of its buttons answers it.
            final int pressed = CdeExitLayout.buttonAt(mouseX, mouseY, powerSurfaceW, powerSurfaceH);
            if (pressed == CdeExitLayout.SHUT_DOWN || pressed == CdeExitLayout.RESTART) {
                sendPower(pressed == CdeExitLayout.SHUT_DOWN ? MachinePowerPayload.ACTION_SHUTDOWN
                        : MachinePowerPayload.ACTION_RESTART);
            } else if (pressed == CdeExitLayout.CANCEL) {
                powerOpen = false;
            }
            return true;
        }
        final int x = powerX();
        final int y = powerY();
        for (int i = 0; i < POWER_CHOICES.length; i++) {
            final int rowY = y + 18 + i * POWER_ROW_H;
            if (mouseX >= x + 4 && mouseX < x + POWER_W - 4
                    && mouseY >= rowY && mouseY < rowY + POWER_ROW_H - 2) {
                sendPower(i);
                return true;
            }
        }
        // A click anywhere else dismisses it: no accidental shutdowns.
        powerOpen = false;
        return true;
    }

    /**
     * Tells the machine what was chosen. It is going down, restarting or being left: the desktop closing after
     * this must not hand its windows back to a machine whose session has just ended.
     */
    private void sendPower(final int action) {
        powerCycling = true;
        PacketDistributor.sendToServer(new MachinePowerPayload(host, monitorPos, action));
        powerOpen = false;
    }

    /** How many programs are open, on every workspace; a dialog is a question a program asks, not a program. */
    private int openPrograms() {
        int open = 0;
        for (final DesktopWindow w : windows) {
            if (!w.dialog()) {
                open++;
            }
        }
        return open;
    }

    /** Toggles the Start menu open/closed, always opening with an empty search box. */
    private void toggleStart() {
        if (startOpen) {
            closeStart();
        } else {
            startOpen = true;
            startSearch.setLength(0);
        }
    }

    /**
     * Resolves a click inside the (version-specific) Start menu. Returns true when the click landed on the
     * panel (and was acted on or absorbed); false when it fell outside, so the caller closes the menu.
     */
    /*
     * Which button opened the Start menu entry, and where the cursor was. Every panel style lays its
     * entries out differently and each handler already does that arithmetic, so rather than a second
     * hit test that would have to match all of them, the handlers say which entry was hit and this
     * decides what to do with it.
     */
    private boolean startWithRightButton;
    private int startClickX;
    private int startClickY;

    /**
     * Starts a program from the Start menu, or opens its own menu when the right button asked.
     *
     * <p>Every panel style calls this instead of running the launcher itself, so a program listed
     * anywhere answers the right button the same way.
     */
    private void startChoose(final int index) {
        if (index >= 0 && index < launchers.size()) {
            startChoose(launchers.get(index));
        }
    }

    /** The same, for the panels that lay their entries out from a list of their own. */
    private void startChoose(final Launcher launcher) {
        if (this.startWithRightButton) {
            closeStart();
            openDeskContext(launchers.indexOf(launcher), this.startClickX, this.startClickY);
            return;
        }
        runLauncher(launcher);
    }

    private boolean handleStartMenuClick(final int mx, final int my, final int tbY) {
        if (periodPanel()) {
            return handleStartClickPeriod(mx, my, tbY);
        }
        return switch (panel) {
            case FRAMES_XP -> handleStartClickXp(mx, my, tbY);
            case FRAMES_11 -> handleStartClick11(mx, my, tbY);
            case KDE -> handleStartClickKde(mx, my, tbY);
            case GNOME -> handleOverviewClickGnome(mx, my);
            case CINNAMON -> handleStartClickCinnamon(mx, my, tbY);
            default -> handleStartClick95(mx, my, tbY);
        };
    }

    /**
     * Clicks in the period launcher. The rows are laid out from the same origin and pitch the renderer
     * uses, so what the player sees and what they hit are one list.
     */
    private boolean handleStartClickPeriod(final int mx, final int my, final int tbY) {
        return framesLaunchers.clickPeriod(mx, my, tbY);
    }

    private boolean handleStartClick95(final int mx, final int my, final int tbY) {
        return framesLaunchers.click95(mx, my, tbY);
    }

    private boolean handleStartClickXp(final int mx, final int my, final int tbY) {
        return framesLaunchers.clickXp(mx, my, tbY);
    }

    /** "All Programs": the page that lists everything installed on this machine, services included. */
    private void openAllPrograms() {
        for (final Launcher l : launchers) {
            if (l.programId().getPath().equals("settings")) {
                runLauncher(l);
                return;
            }
        }
    }

    private boolean handleStartClick11(final int mx, final int my, final int tbY) {
        return framesLaunchers.click11(mx, my, tbY);
    }

    /**
     * A click travels down the desktop one layer at a time: whatever is modal takes it first, then the
     * panel, then the windows, then the wallpaper and its icons, and only what nothing claimed reaches the
     * container underneath. Each layer below says whether it took the click, so the order they are tried in
     * is the whole of the routing and reads in one place.
     */
    @Override
    public boolean mouseClicked(final double mouseXAbs, final double mouseYAbs, final int button) {
        if (crashing) {
            return true; // the crash screen swallows input until the reboot completes
        }
        if (clickedOverlay(mouseXAbs, mouseYAbs, button)) {
            return true;
        }
        final double mouseX = lx(mouseXAbs);
        final double mouseY = ly(mouseYAbs);
        final int tbY = sh() - panelBand();
        if (clickedPanel(mouseX, mouseY, button, tbY)) {
            return true;
        }
        final Click inWindow = clickedWindow(mouseXAbs, mouseYAbs, mouseX, mouseY, button);
        if (inWindow == Click.TAKEN) {
            return true;
        }
        if (inWindow == Click.CONTAINER || clickedDesktop(mouseX, mouseY, button) == Click.CONTAINER) {
            return super.mouseClicked(vx(mouseXAbs), vy(mouseYAbs), button);
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
        if (clickPowerDialog(mouseXAbs, mouseYAbs)) {
            return true;
        }
        if (taskMenu.isOpen()) {
            taskMenu.mouseClicked(lx(mouseXAbs), ly(mouseYAbs), button);
            return true;
        }
        // A window's own menu on CDE takes the click too, unless it is on the very button the menu hangs from.
        if (cdeWindowMenu.isOpen() && cdeWindowMenu.clicked(lx(mouseXAbs), ly(mouseYAbs))) {
            return true;
        }
        if (taskPopup.key() != null && taskPopup.click(lx(mouseXAbs), ly(mouseYAbs), button)) {
            return true;
        }
        // A modal dialog swallows every click; only its OK button dismisses it.
        if (popup != null) {
            popup.mouseClicked(lx(mouseXAbs), ly(mouseYAbs), button);
            if (!popup.isOpen()) {
                popup = null;
            }
            return true;
        }
        /*
         * An app-level modal dialog isolates its window: route the click to it and to nothing behind it
         * (inventory slots, other windows, the taskbar), just like the desktop popup above.
         */
        if (focusModal()) {
            final DesktopWindow f = frontWindow();
            if (f != null) {
                f.app().mouseClicked(f, lx(mouseXAbs), ly(mouseYAbs), button);
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
        if (balloonClick(mouseX, mouseY, tbY, sw())) {
            return true;
        }

        /*
         * The panel's menu takes the next click wherever it lands: on an entry it runs it, anywhere else it
         * just closes, which is what a menu does.
         */
        if (panelCtxOpen) {
            final int entry = panelCtxItemAt(mouseX, mouseY);
            panelCtxOpen = false;
            if (entry >= 0) {
                runPanelMenu(entry);
            }
            return true;
        }

        /*
         * GNOME: the top bar's Activities corner toggles the overview; the rest of the bar is inert.
         * Only while the bar IS at the top: a period GNOME panels at the bottom, and swallowing clicks
         * along the top edge there ate the title bars of every window parked up there.
         */
        if (topPanel() && mouseY < TASKBAR_H) {
            if (mouseX < 64) {
                toggleStart();
            } else if (button == 1) {
                openPanelMenu((int) mouseX, 0);
            }
            return true;
        }
        /*
         * CDE: the Front Panel answers for itself, and so does the subpanel standing on it. There is no Start
         * button and no row of open programs to test, and the band either side of the slab is plain desktop.
         */
        if (is(PanelStyle.CDE)) {
            // A click beside a subpanel leaves it up: only its own arrow puts it away again.
            if (button == 0 && cdeLaunchers.click(mouseX, mouseY, sw(), sh())) {
                return true;
            }
            // The right button on the slab opens the panel's own menu, where the Task Manager has always been.
            if (button == 1 && CdeFrontPanelLayout.panel(sw(), sh()).holds(mouseX, mouseY)) {
                openPanelMenu((int) mouseX, tbY);
                return true;
            }
            return cdePanels.click(mouseX, mouseY, sw(), sh());
        }
        // Windows 11 keeps Start with the centered group, so it has its own hit test.
        if (is(PanelStyle.FRAMES_11) && mouseY >= tbY) {
            final int startX = win11StartX(sw());
            if (mouseX >= startX && mouseX < startX + WIN11_SLOT) {
                toggleStart();
                return true;
            }
        } else if (startButtonHit(mouseX, mouseY, tbY)) {
            toggleStart();
            return true;
        }
        if (startOpen) {
            /*
             * The right button asks about a program rather than starting it, the way it does on the
             * desktop itself. Before this, both buttons ran it, so there was no way to reach a
             * program's own menu from the one place every program is listed.
             */
            startWithRightButton = button == 1;
            startClickX = (int) mouseX;
            startClickY = (int) mouseY;
            final boolean handled = handleStartMenuClick((int) mouseX, (int) mouseY, tbY);
            startWithRightButton = false;
            if (handled) {
                return true;
            }
            startOpen = false;
        }

        /*
         * The panel's entries, one per program: the left button brings its window up or down, or lists
         * its windows when it has several; the right button opens the program's own menu, so a program
         * can be closed or pinned without first going to it. A right-click on the panel itself, clear of
         * Start and of the entries, opens the panel's own menu, the way every one of these desktops offers
         * it; the Task Manager is one entry on that menu. The rest of the bar is the bar and swallows the
         * click, so nothing under it reacts.
         */
        if (!topPanel() && mouseY >= tbY) {
            final TaskStrip strip = taskStrip(sw());
            int idx = strip.indexAt(mouseX);
            int atX = idx >= 0 ? strip.x()[idx] : 0;
            if (idx < 0 && strip.quickCount() > 0) {
                idx = strip.quickEntryAt(mouseX);
                atX = idx >= 0 ? strip.quickX() + strip.quickIndexOf(idx) * QL_W : 0;
            }
            if (idx >= 0) {
                clickTaskEntry(strip.entries().get(idx), atX, button, tbY);
                return true;
            }
            if (button == 1) {
                openPanelMenu((int) mouseX, tbY);
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
                && !w.app().modalActive() && slotUnderMouse(mouseXAbs, mouseYAbs) != null) {
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
            final Slot slot = slotUnderMouse(mouseXAbs, mouseYAbs);
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
        if (!ni.hasPopup() && slotUnderMouse(mouseXAbs, mouseYAbs) != null) {
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
                && cdeWindowIcons.clicked(mouseX, mouseY, putAwayHere(), sw(), workTop())) {
            return Click.TAKEN;
        }
        // The right button on such an icon raises the window's own menu, which is how it is closed from there.
        if (is(PanelStyle.CDE) && button == 1) {
            final DesktopWindow putAway = cdeWindowIcons.at(mouseX, mouseY, putAwayHere(), sw(), workTop());
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
            openDeskContext(slot, (int) mouseX, (int) mouseY);
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
        if (button == 0 && mouseY >= workTop() && mouseY < workBottom() && overWallpaper(mouseX, mouseY)) {
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
        if (popup != null) {
            return true;
        }
        if (dragging != null) {
            dragging.moveTo((int) (lx(mouseXAbs)) - dragOffsetX, (int) (ly(mouseYAbs)) - dragOffsetY,
                    workTop(), sw(), workBottom());
            return true;
        }
        if (resizing != null) {
            resizing.applyResize(lx(mouseXAbs), ly(mouseYAbs), workTop(), sw(), workBottom());
            return true;
        }
        // Dragging a desktop icon across the desktop, once the cursor has left the click dead zone.
        if (deskDragSlot >= 0) {
            deskDragX = lx(mouseXAbs);
            deskDragY = ly(mouseYAbs);
            if (!deskDragging
                    && (Math.abs(deskDragX - deskDragStartX) > DRAG_THRESHOLD
                        || Math.abs(deskDragY - deskDragStartY) > DRAG_THRESHOLD)) {
                deskDragging = true;
            }
            return true;
        }
        // Sweeping the wallpaper: extend the band and reselect what it now covers.
        if (bandActive) {
            bandX = lx(mouseXAbs);
            bandY = ly(mouseYAbs);
            iconGrid.selectWithin(bandRect());
            return true;
        }
        /*
         * No window drag/resize in progress. While the front Network Interactor holds a stack on the cursor,
         * a drag is the vanilla "spread across slots" gesture, so hand it to the container, not the app.
         */
        final DesktopWindow w = frontWindow();
        if (w != null && w.app() instanceof IInventoryBandApp && !menu.getCarried().isEmpty()) {
            return super.mouseDragged(vx(mouseXAbs), vy(mouseYAbs), button, dx, dy);
        }
        if (w != null) {
            w.app().mouseDragged(w, lx(mouseXAbs), ly(mouseYAbs), button);
            return true;
        }
        return super.mouseDragged(vx(mouseXAbs), vy(mouseYAbs), button, dx, dy);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (popup != null) {
            popup.mouseReleased(lx(mouseX), ly(mouseY), button);
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
            if (btn != DesktopWindow.BUTTON_NONE && pb.buttonAt(lx(mouseX), ly(mouseY)) == btn) {
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
            handleDeskDrop(lx(mouseX), ly(mouseY));
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
                && handleExplorerDropToDesktop(lx(mouseX), ly(mouseY))) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        /*
         * Route the release to the front window's app (for content drag-and-drop) unless this was a
         * desktop-icon drag, and only when no window move/resize is in progress.
         */
        if (!wasDeskDrag && dragging == null && resizing == null) {
            final DesktopWindow w = frontWindow();
            if (w != null) {
                w.app().mouseReleased(w, lx(mouseX), ly(mouseY), button);
            }
        }
        /*
         * Frames 11 edge snapping: releasing a dragged window against a screen edge tiles it (top = maximize,
         * left/right = that half). A modern-OS gesture the earlier editions do not have.
         */
        if (dragging != null && (is(PanelStyle.FRAMES_11) || linuxDesktop())) {
            final int lx = (int) (lx(mouseX));
            final int ly = (int) (ly(mouseY));
            final int top = workTop();
            final int workH = workBottom() - top;
            final int halfW = sw() / 2;
            if (ly <= top + 4) {
                dragging.setMaximized(true);
            } else if (lx <= 4) {
                dragging.snapTo(0, top, halfW, workH);
            } else if (lx >= sw() - 4) {
                dragging.snapTo(halfW, top, sw() - halfW, workH);
            }
        }
        dragging = null;
        resizing = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        if (popup != null) {
            return true;
        }
        // A desktop-icon rename captures typing before any window, until the name is as long as it may be.
        if (deskFiles.isRenaming() && c >= 32 && c != 127 && c != '/' && c != '\\' && deskFiles.type(c)) {
            return true;
        }
        // The Frames 11 Start search box captures typing while it is open (it is always focused when shown).
        if (startOpen && searchableStart() && c >= 32 && c != 127 && startSearch.length() < 24) {
            startSearch.append(c);
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
        if (is(PanelStyle.CDE) && popup == null && !powerOpen
                && cdeWindowMenu.keyPressed(key, modifiers, frontWindow())) {
            return true;
        }
        if (popup != null || powerOpen || deskMenu.isOpen() || taskMenu.isOpen() || deskFiles.isRenaming()
                || startOpen) {
            return keyPressed(key, scanCode, modifiers);
        }
        final DesktopWindow w = frontWindow();
        return w != null && (key != 256 || w.app().wantsEscape()) && w.app().keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        // A modal dialog swallows every key; Enter or Escape dismisses it, nothing leaks behind it.
        if (popup != null) {
            popup.keyPressed(key, scanCode, modifiers);
            if (!popup.isOpen()) {
                popup = null;
            }
            return true;
        }
        /*
         * The power dialog decides the fate of the whole machine, so it keeps the keyboard as it keeps the
         * mouse: Escape thinks again, and on CDE Enter takes the button that wears the ring, Shut Down.
         */
        if (powerOpen) {
            if (key == 256) {
                powerOpen = false;
            } else if (is(PanelStyle.CDE) && (key == 257 || key == 335)) {
                sendPower(MachinePowerPayload.ACTION_SHUTDOWN);
            }
            return true;
        }
        // The desktop's menu and a program's are walked with the arrows and left with Escape, like any menu.
        if (deskMenu.isOpen() && deskMenu.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        if (taskMenu.isOpen() && taskMenu.keyPressed(key, scanCode, modifiers)) {
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
        /*
         * While the Start menu is open it owns the keyboard: Escape closes it, and on Frames 11 the search box
         * takes Backspace (edit) and Enter (launch the top result). This runs before ESC reaches the desktop.
         */
        if (startOpen) {
            if (key == 256) { // Escape
                closeStart();
                return true;
            }
            if (searchableStart()) {
                if (key == 259) { // Backspace
                    if (startSearch.length() > 0) {
                        startSearch.deleteCharAt(startSearch.length() - 1);
                    }
                    return true;
                }
                if ((key == 257 || key == 335) && startSearch.length() > 0) { // Enter launches the first result
                    final List<Launcher> hits = w11Filtered();
                    if (!hits.isEmpty()) {
                        runLauncher(hits.get(0));
                        closeStart();
                    }
                    return true;
                }
                /*
                 * Swallow every other key so the open search box owns the keyboard: this stops a background
                 * window from eating letters and stops the inventory key from closing the desktop. charTyped
                 * is a separate GLFW event, so typed characters still reach the search box below.
                 */
                return true;
            }
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
        if (popup == null && w != null && w.app().keyReleased(key, scanCode, modifiers)) {
            return true;
        }
        return super.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double dx, final double dy) {
        if (popup != null) {
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
        return new int[] {sx(local[0]), sy(local[1])};
    }

    /** The topmost window that is on show, which receives keyboard and scroll input. */
    @Nullable
    private DesktopWindow frontWindow() {
        for (int i = windows.size() - 1; i >= 0; i--) {
            if (!away(windows.get(i))) {
                return windows.get(i);
            }
        }
        return null;
    }

    /** Whether the front (focused) window's app has a modal dialog open, in which case the desktop disables everything behind it. */
    private boolean focusModal() {
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

    /**
     * The front window only if it hosts a Network Interactor, the one window that shows the player's real
     * inventory slots. Returns {@code null} when the front window is another app or the desktop is bare, which
     * is exactly when the inventory slots must go inert.
     */
    @Nullable
    private DesktopWindow frontNetworkInteractorWindow() {
        final DesktopWindow w = frontWindow();
        return w != null && w.app() instanceof IInventoryBandApp ? w : null;
    }

    /**
     * Repositions the menu's 36 inventory slots over the focused Network Interactor window's inventory zone and
     * toggles them active, once per tick before the next render. When no Network Interactor is in front the
     * slots are switched off (not rendered, not hit-tested), so the inventory only appears inside that window.
     * The slot grid origin is kept relative to {@code leftPos}/{@code topPos}, the offset the container renders
     * and hit-tests slots at (since {@code leftPos == ox()} and {@code topPos == oy()}, that origin is just the
     * window-local position of the first inventory cell).
     */
    @Override
    protected void containerTick() {
        super.containerTick();
        syncInventorySlots();
    }

    /**
     * Positions the menu's 36 inventory slots over the focused Network Interactor window's inventory zone and
     * toggles them active. Run from {@link #containerTick()} and again at the top of {@link #render} so the
     * slots track a dragged/resized window per frame, not just per tick. When no Network Interactor is in front
     * the slots go inert (not rendered, not hit-tested), so the inventory only shows inside that window. The
     * grid origin is window-local, and since {@code leftPos == ox()} and {@code topPos == oy()}, that is exactly
     * the offset the container measures {@code slot.x}/{@code slot.y} from. The menu only rebuilds slots when
     * the origin actually changed, so this is cheap to call every frame.
     */
    private void syncInventorySlots() {
        final DesktopWindow w = frontNetworkInteractorWindow();
        if (w == null || !(w.app() instanceof IInventoryBandApp app)) {
            menu.setSlotsActive(false);
            return;
        }
        /*
         * Resolve the window's rectangle for this frame first, so slot positions never lag a frame behind a
         * drag, resize, or maximize (curX/curY are otherwise only refreshed when the window itself renders).
         */
        w.resolveGeometry(sw(), sh(), bottomReserve(), workTop());
        /*
         * The focused Network Interactor is the one that should receive network snapshots and console output,
         * so point the static routing at it whenever it is in front (matters when two windows are open).
         */
        app.markActive();
        menu.setSlotsActive(true);
        /*
         * The window-local top-left of the first inventory cell: past the window border + title bar to the app
         * content, then the app's own inventory-zone offset. The inventory is a fixed, framed band pinned just
         * above the footer; its Y uses the window's live content height (not the app's cached field) so the
         * cells line up with their backgrounds from the very first frame. The band is always fully visible (it
         * never scrolls and is never clipped) so every one of the 36 slots is always live.
         */
        final int contentHeight = w.height() - DesktopWindow.TITLE_H - 8;
        final int contentTop = w.y() + DesktopWindow.TITLE_H + 4;
        final int originX = w.x() + 4 + app.invCellContentX(0);
        final int originY = contentTop + app.invCellContentY(0, contentHeight);
        /*
         * The band's screen-space bounds span the rows the app shows: the band never scrolls and is never
         * clipped, but it can fold its top rows away, and a slot above the band's top goes inert.
         */
        final int bandTop = contentTop + app.invBandTop(contentHeight);
        final int bandBottom = contentTop + app.invBandBottom(contentHeight);
        menu.layoutInventory(originX, originY, bandTop, bandBottom);
    }

    /**
     * Draws the items held in the active inventory slots, plus the hover highlight, inside the desktop's
     * translated/scissored pass right after the windows, so the items sit over the front window's inventory
     * zone. Records {@link #hoveredSlot} so the carried-item and tooltip passes can use it. Coordinates are
     * desktop-local (the caller has already translated by ox()/oy()), which equals slot.x/slot.y here.
     */
    private void renderInventoryItems(final GuiGraphics g, final int lmx, final int lmy, final float partialTick) {
        hoveredSlot = null;
        if (!menu.slotsActive()) {
            return;
        }
        /*
         * A modal dialog in the focused app disables the inventory: still draw the items (the dialog's dim
         * darkens them) but give no hover highlight and no click target.
         */
        final boolean modal = focusModal();
        /*
         * lmx/lmy and the slot coordinates are both desktop-local (already inside the ox/oy translate).
         * Draw the items directly at the local slot coordinates: delegating to the inherited renderSlot would
         * add leftPos/topPos a second time (leftPos==ox()), double-offsetting the icons from their backgrounds.
         */
        for (final var slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            final ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                DesktopItems.item(g, stack, slot.x, slot.y);
                DesktopItems.count(g, font, stack, slot.x, slot.y);
            }
            if (!modal && lmx >= slot.x && lmx < slot.x + 16 && lmy >= slot.y && lmy < slot.y + 16) {
                hoveredSlot = slot;
                g.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x80FFFFFF);
            }
        }
    }

    /**
     * The active inventory slot under an absolute screen point, or {@code null}. Mirrors the container's own
     * hit-test ({@code slot.x + leftPos}, a 16x16 cell, active only), so a click there can be handed to the
     * vanilla container which expects the same geometry.
     */
    @Nullable
    private Slot slotUnderMouse(final double absX, final double absY) {
        if (!menu.slotsActive()) {
            return null;
        }
        for (final var slot : menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            final double mx = lx(absX);
            final double my = ly(absY);
            if (mx >= slot.x && mx < slot.x + 16 && my >= slot.y && my < slot.y + 16) {
                return slot;
            }
        }
        return null;
    }

    /** Draws the carried (cursor) stack at the mouse, above everything. Desktop-local coordinates. */
    private void renderCarried(final GuiGraphics g, final int lmx, final int lmy) {
        final ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            g.renderItem(carried, lmx - 8, lmy - 8);
            g.renderItemDecorations(font, carried, lmx - 8, lmy - 8);
        }
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
        final int top = workTop();
        final int workH = workBottom() - top;
        final int availW = sw() - 16;
        final int availH = workH - 16;
        final int w = availW >= app.minWidth() ? Math.min(app.defaultWidth(), availW) : availW;
        final int h = availH >= app.minHeight() ? Math.min(app.defaultHeight(), availH) : availH;
        final int x = Math.max(48, (sw() - w) / 2 + windows.size() * 12);
        final int y = Math.max(top + 6, top + (workH - h) / 2 + windows.size() * 12);
        final DesktopWindow opened = new DesktopWindow(app, key, x, y, w, h);
        // A program opens on the workspace that is up, which is where whoever started it is looking.
        opened.setWorkspaces(WorkspaceSet.only(shownWorkspace));
        windows.add(opened);
    }

    /** Opens that program's window on this desktop, if this machine has it at all. */
    private void startProgramById(final String path) {
        for (final Launcher l : launchers) {
            if (l.programId().getPath().equals(path) && l.factory() != null) {
                openApp(l.label(), l.factory().get());
                return;
            }
        }
    }

    /** Recreates a program from its launcher key, for restoring persisted windows. */
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
        for (final Launcher l : launchers) {
            if (l.label().equals(key) && l.factory() != null) {
                return l.factory().get();
            }
        }
        /*
         * A program the desktop shows no launcher for (the Task Manager) still opens, and still comes back
         * with the session, so it is looked up by the same label the panel calls it.
         */
        final ProgramSpec spec = chrome == null ? null : chrome.programFor(key);
        final ProgramClient.IDesktopAppFactory factory = spec == null ? null : ProgramClient.factory(spec.id());
        return factory == null ? null : factory.create(host, monitorPos, desktopId);
    }

    /**
     * Opens the Task Manager, or brings it forward when it is already up. It is the panel's own right-click
     * destination and has no launcher of its own, exactly as on the desktops this imitates.
     */
    private void openTaskManager() {
        final ProgramSpec spec = OsRegistry
                .getProgram(Programs.TASK_MANAGER);
        if (spec == null) {
            return;
        }
        final String key = chrome != null ? chrome.launcherLabel(spec) : spec.displayName();
        final DesktopWindow open = windowFor(key);
        if (open != null) {
            focusWindow(open);
            return;
        }
        final IDesktopApp app = factoryFor(key);
        if (app != null && allowOpen(key)) {
            app.applySkin(skin);
            openApp(key, app);
        }
    }

    /** Starts a launcher: a built-in app opens a window; an action-based one (e.g. the NMS) runs its action. */
    private void runLauncher(final Launcher l) {
        if (!l.runs().isEmpty()) {
            requestRunAtTerminal(l.runs());
            return;
        }
        if (allowOpen(l.label())) {
            openApp(l.label(), l.factory().get());
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
