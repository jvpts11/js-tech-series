/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.jstech.computers.client.MachineKeyboard;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.operation.payload.SetupProgressPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The screen a player opens at a monitor to use a graphical system's desktop (Frames 95 / XP / 11, the Linux
 * desktops, CDE). It is drawn inside a centred window, the monitor's glass, over the dimmed game, never full-screen.
 *
 * <p>The desktop itself, its windows, panel, menus and what the machine said about it, is a {@link DesktopState},
 * which draws onto this screen and would draw the same onto any other surface. What is left here is what only an
 * open screen has: the pointer and the keyboard handed on to the desktop, the container that carries the player's
 * inventory, and the calls programs make to the desktop that is up.
 */
public final class DesktopScreen extends CoreContainerScreen<DesktopMenu>
        implements MachineKeyboard.ITakesKeysFirst, DesktopInspection, DesktopSurface {

    private final DesktopState state;
    /** Where the pointer and the keyboard go, layer by layer. */
    private final DesktopInput input;
    /*
     * Ctrl+B is the game's narrator switch, and the game flips it before any screen sees the key unless the screen's
     * focused control is a text box taking input. Every key on the desktop is the desktop's (C-b in Emacs above all),
     * so the desktop always reports one: an invisible box that draws nothing, keeps nothing and answers no key, there
     * only to say that typing is spoken for.
     */
    @Nullable
    private KeySink keySink;

    static final int TASKBAR_H = 24;
    // Frames 11 taskbar: each centered item (Start + one per program) occupies this slot.
    static final int WIN11_SLOT = 22;
    static final int WIN11_ICON = 16;
    /** The width of the Frames XP Start pill, which the task buttons and its own hit-test both clear. */
    static final int XP_START_W = 58;

    /** The desktop that is up, which the calls programs make to "the desktop" reach; null while none is. */
    @Nullable
    private static DesktopState active;

    public DesktopScreen(final DesktopMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.state = new DesktopState(this, menu.hostPos(), menu.monitorPos(), menu.osId(), menu.desktopId(),
                menu.ramTotalMb(), menu.ramReservedMb());
        this.input = new DesktopInput(this, state);
    }

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
     * Opens {@code dialog} as a window of its own over the window running {@code owner}: it is listed with the owner
     * on the panel, sits in front of it, and holds it until it is put away. The way an Open or Save window belongs to
     * the program that asked. Nothing happens when no desktop is up or the owner has no window on it.
     */
    public static void openDialogFor(final IDesktopApp owner, final IDesktopApp dialog) {
        if (active != null) {
            active.wm().openDialog(owner, dialog);
        }
    }

    /** Closes the window running {@code app}, when it is up, the way its own Close button does. */
    public static void closeWindowFor(final IDesktopApp app) {
        if (active != null) {
            active.wm().closeOf(app);
        }
    }

    /** Puts away the window running {@code dialog}, when it is up. */
    public static void closeDialog(final IDesktopApp dialog) {
        if (active != null) {
            active.wm().closeOf(dialog);
        }
    }

    /**
     * Starts one of this machine's programs by its id, the way a shortcut to it does.
     *
     * <p>For a program that offers another as a way out of itself: a welcome pointing at This PC, and whatever comes
     * to want the same. Nothing happens when this machine has no such program, which is the honest answer on a
     * computer where it was never installed.
     */
    public static void openProgramById(final String path) {
        if (active != null) {
            active.opener().startProgram(path);
        }
    }

    /**
     * Brings the window of that key forward, or opens it the way a session coming back would, for a program that has
     * a second window of its own: Soundfoundry's sharing window, say. The key is the id its window factory is
     * registered under ({@link ProgramClient#register}), so the window comes back with the session like any other.
     */
    public static void openOrFocus(final String key) {
        if (active != null) {
            active.opener().openOrFocus(key);
        }
    }

    /**
     * The id of the wallpaper actually hanging on the desktop right now: the player's own choice when they made one,
     * else whatever the desktop ships with on the platform it runs on.
     */
    public static String currentWallpaperId() {
        return active == null ? "" : active.wallpaperId();
    }

    /** Whether a window of that key is up on the desktop in front of the player. */
    public static boolean windowOpen(final String key) {
        return active != null && active.windowOpen(key);
    }

    /**
     * The name this desktop gives that program, or empty when this machine has no such program.
     *
     * <p>The name is the desktop's, not the program's: the same prompt is called one thing on one edition and
     * something else on another, and a button that offers it should say what this machine calls it.
     */
    public static String programLabel(final String path) {
        final Launcher launcher = active == null ? null : active.catalogue().byPath(path);
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
     * <p>A program of the console kind needs a terminal to print into, so it is given one; the window is whatever
     * this desktop calls its terminal, because that is the one the machine has.
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
     * <p>Which program that is has one answer for the whole desktop, so the explorer asks here rather than deciding
     * for itself and disagreeing with a double-click on the desktop.
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

    /**
     * Lets a running app hand the shell a job: lines typed at the terminal, one after the other. A line with a
     * newline in it is typed as the lines it holds.
     */
    public static void requestTypeAtTerminal(final List<String> lines) {
        DesktopRequests.open(new OpenRequest.TypeAtTerminal(List.of(String.join("\n", lines).split("\n"))));
    }

    /** Asks for the Properties window of a file on the desktop, which the explorer knows how to show. */
    public static void requestFileProperties(final String path) {
        DesktopRequests.open(new OpenRequest.Properties(path));
    }

    /** The Open with chooser the open desktop is showing, or null when none is up. */
    @Nullable
    public static OpenWithPopup openWithChooser() {
        return active != null ? active.notices().chooser() : null;
    }

    /** The ids of the programs the open desktop's machine has, for a window offering what can open a file. */
    public static List<String> installedProgramIds() {
        return active == null ? List.of() : List.copyOf(active.catalogue().installed());
    }

    /** What a program is called, for a menu that offers it by id. */
    public static String openerName(final String programId) {
        return ProgramOpener.openerName(programId);
    }

    /**
     * Forgets every per-machine client cache: the programs' insides kept for the machines of the world the player is
     * leaving, and any open request that never found a desktop. Called on logout, so nothing of one world lingers
     * into the next.
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
     * Applies an accent/brightness change to the live desktop immediately, so a change in the Settings app shows on
     * the chrome without waiting for the next desktop refresh.
     *
     * @param accent     the accent override ({@code 0} = skin default)
     * @param brightness the screen brightness 0..100
     */
    public static void applyLivePrefs(final int accent, final int brightness, final boolean clock12h,
                                      final String wallpaper, final boolean taskbarCentered,
                                      final boolean darkMode, final int scale) {
        if (active != null) {
            active.applyLivePrefs(accent, brightness, clock12h, wallpaper, taskbarCentered, darkMode, scale);
        }
    }

    /**
     * Raises the modal {@code .dat}-locked error dialog on the active desktop. Called from desktop apps (e.g. the
     * Files explorer) that detect a refused {@code .dat} action and need to surface it; a no-op when no desktop is
     * showing.
     */
    public static void showDatLockedError() {
        if (active != null) {
            active.notices().datLocked();
        }
    }

    /**
     * Raises the error for a refused action on an installer's own files: they are generated from the medium's stamp,
     * so there is nothing to rename, copy off, delete or overwrite.
     */
    public static void showInstallerLockedError() {
        if (active != null) {
            active.notices().showError(GameText.resolve(DesktopTexts.ERROR),
                    GameText.resolve(DesktopTexts.INSTALLER_LOCKED));
        }
    }

    /** Hangs a picture on this desktop's wall, which is what the paint program's last button does. */
    public static void setWallpaperToPicture(final String path) {
        if (active != null && path != null && !path.isEmpty()) {
            active.hangPicture(path);
        }
    }

    /** Raises that balloon on whichever desktop is looking at that machine, if one is. */
    public static void raise(final BlockPos host, final String title, final String body, final String opens) {
        if (active != null && active.host().equals(host)) {
            active.notices().showBalloon(title, body, opens);
        }
    }

    /**
     * What this desktop calls its terminal (Command Prompt, Megashell, Konsole...), for a program offering to open
     * one; empty when no desktop is up or it has none.
     */
    public static String terminalName() {
        return active == null ? "" : active.opener().terminalLabel();
    }

    /**
     * What a window key reads as on the desktop that is up, for a program listing the windows a machine has open (a
     * task manager) by the names the player knows them by; the key itself while no desktop is up.
     */
    public static String windowName(final String key) {
        return active == null ? key : active.nameOf(key);
    }

    /**
     * Refreshes the open desktop's server-derived state (installed programs included), so a program installed or
     * removed while the desktop is up gets its launcher without closing the monitor. Called after any action that can
     * change the installed set (a shell command, an install disc, Settings).
     */
    public static void refreshActive() {
        if (active != null) {
            active.requestDesktop();
        }
    }

    /** Takes the removable media a listing names, which are the machine's whichever folder was listed. */
    public static void acceptVolumes(final DiskFilesPayload payload) {
        if (active != null) {
            active.takeVolumes(payload);
        }
    }

    /** Routes the machine's settings to the open desktop, whose panel shows its sound. */
    public static void acceptSettings(final SettingsSnapshotPayload payload) {
        if (active != null && active.host().equals(payload.hostPos())) {
            active.volume().accept(payload.sound());
        }
    }

    /** Restores the windows the machine has open, when the server hands them over. */
    public static void applyWindows(final DesktopWindowsPayload payload) {
        if (active != null && active.host().equals(payload.host())) {
            active.takeWindows(payload);
        }
    }

    /** A machine saying how the program it is setting up is going, which its Setup window shows. */
    public static void acceptSetup(final SetupProgressPayload payload) {
        if (active != null && active.host().equals(payload.hostPos())) {
            active.takeSetup(payload);
        }
    }

    /** Routes a desktop-folder listing reply to the active desktop. */
    public static void acceptDesktop(final DesktopFilesPayload payload) {
        if (active != null) {
            active.takeDesktop(payload);
        }
    }

    /** Whether anything is in the trash, which is the picture its icon and its Front Panel control wear. */
    public boolean trashFull() {
        return state.trashFull();
    }

    /** A rectangle an app drew, as it lands on the screen, for anything outside the desktop that lines up with it. */
    public Rect2i onScreen(final int x, final int y, final int w, final int h) {
        return state.onScreen(x, y, w, h);
    }

    public List<String> launcherLabels() {
        return state.launcherLabels();
    }

    /** Whether {@code app} runs in the front (focused) window; what a recipe viewer's drop or transfer targets. */
    public boolean isFront(final IDesktopApp app) {
        return state.isFront(app);
    }

    /** Every window of a program (its dialogs aside), front-most last, named by its window key or its label. */
    public List<DesktopWindow> windowsFor(final String label) {
        return state.windowsFor(label);
    }

    /** The oldest window of a program, named by its window key or by the label it reads as here; or null. */
    @Nullable
    public DesktopWindow windowFor(final String label) {
        return state.windowFor(label);
    }

    /** Outer bounds of the framed monitor window (the bezel plus its chin), for a side panel placed beside it. */
    public MonitorFrameStyle.Geometry frameBounds() {
        return state.frameBounds();
    }

    @Override
    public int surfaceWidth() {
        return width;
    }

    @Override
    public int surfaceHeight() {
        return height;
    }

    @Override
    public DesktopMenu container() {
        return menu;
    }

    @Override
    public void leave() {
        onClose();
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        /*
         * renderBackground already draws the vanilla blur + dim gradient once; a second identical fill would darken
         * the world behind the desktop to near-black (the double-dim bug). One pass only.
         */
        renderBackground(g, mouseX, mouseY, partialTick);
        /*
         * The desktop paints everything itself instead of running the container's render pass, so it posts the two
         * container render events that pass would post: a recipe viewer draws its ingredient list beside the monitor
         * from them (its plain screen-render hook skips container screens on purpose).
         */
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Background(this, g, mouseX, mouseY));
        // What programs asked of "the desktop" is carried out by the one in front of the player.
        DesktopRequests.drain(state);
        final DesktopViewport view = state.view();
        if (!state.paint(g, (int) Math.floor(view.localX(mouseX)), (int) Math.floor(view.localY(mouseY)),
                partialTick)) {
            return; // the crash screen is all there is until the machine reboots
        }
        hoveredSlot = state.hoveredSlot();
        /*
         * The container pass posts its foreground event with the pose at the gui origin and the depth test off, so a
         * listener draws over the finished screen without fighting the desktop's layered depth.
         */
        RenderSystem.disableDepthTest();
        g.pose().pushPose();
        g.pose().translate(leftPos, topPos, 0);
        NeoForge.EVENT_BUS.post(new ContainerScreenEvent.Render.Foreground(this, g, mouseX, mouseY));
        g.pose().popPose();
        RenderSystem.enableDepthTest();
    }

    /** A click goes down the desktop one layer at a time; only what nothing on it claimed reaches the container. */
    @Override
    public boolean mouseClicked(final double mouseXAbs, final double mouseYAbs, final int button) {
        final DesktopViewport view = state.view();
        return input.click(mouseXAbs, mouseYAbs, button) != DesktopInput.Click.CONTAINER
                || super.mouseClicked(view.slotX(mouseXAbs), view.slotY(mouseYAbs), button);
    }

    @Override
    public boolean mouseDragged(final double mouseXAbs, final double mouseYAbs, final int button,
                                final double dx, final double dy) {
        final DesktopViewport view = state.view();
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

    @Override
    public void removed() {
        state.putAway();
        if (active == state) {
            active = null;
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

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

    /** The desktop this screen shows. */
    DesktopState state() {
        return state;
    }

    /** The desktop that is up, for a window that outlived the screen it was opened on; null while none is. */
    @Nullable
    static DesktopState current() {
        return active;
    }

    /**
     * Asks the player a question over the whole desktop, running {@code yes} only when the answer is Yes: what
     * deletes a thing for good asks this first.
     */
    static void ask(final String title, final String message, final Runnable yes) {
        if (active != null) {
            active.notices().ask(title, message, yes);
        }
    }

    /** Tells the player something over the whole desktop, in a note they close with OK. */
    static void tell(final String title, final String message) {
        if (active != null) {
            active.notices().ask(title, message, null);
        }
    }

    @Override
    protected void init() {
        /*
         * Size the container's image rect to the on-screen monitor glass, so leftPos/topPos centre exactly where the
         * desktop is placed. The inventory/title labels the base would draw are pushed off-screen, since the desktop
         * draws its own chrome.
         */
        final DesktopViewport view = state.view();
        this.imageWidth = view.glassWidth();
        this.imageHeight = view.glassHeight();
        super.init();
        this.leftPos = view.left();
        this.topPos = view.top();
        this.titleLabelX = -10000;
        this.inventoryLabelY = -10000;
        // Become the desktop that is up, since the machine's answers go to that one, then ready it and ask.
        active = state;
        state.prepare();
        state.askMachine();
    }

    /*
     * The desktop paints its whole surface in the render() override above and does not call super.render(), so the
     * container's background pass is unused, and the inventory items and cursor are drawn by render() instead.
     */
    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
    }

    /** Lays the inventory's slots over the window in front once a tick, ahead of the next frame. */
    @Override
    protected void containerTick() {
        super.containerTick();
        state.band().sync();
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
