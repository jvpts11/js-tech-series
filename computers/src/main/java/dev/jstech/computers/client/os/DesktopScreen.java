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
import dev.jstech.computers.client.monitor.MonitorTubes;
import dev.jstech.computers.client.theme.MonitorFrameStyle;
import dev.jstech.computers.config.ComputersClientConfig;
import dev.jstech.computers.menu.DesktopMenu;
import dev.jstech.computers.operation.payload.DesktopFilesPayload;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.client.live.TubeFilter;
import dev.jstech.core.client.recipeview.IKeepsViewersClear;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The screen a player opens at a monitor to use a graphical system's desktop (Frames 95 / XP / 11, the Linux
 * desktops, CDE). It is drawn inside a centred window, the monitor's glass, over the dimmed game, never full-screen.
 *
 * <p>The desktop itself, its windows, panel, menus and what the machine said about it, is a {@link DesktopState},
 * which draws onto this screen and would draw the same onto any other surface. What is left here is what only an
 * open screen has: the pointer and the keyboard handed on to the desktop, the container that carries the player's
 * inventory, and which desktop is up, the one the rest of the client reaches through {@link ActiveDesktop}.
 */
public final class DesktopScreen extends CoreContainerScreen<DesktopMenu>
        implements MachineKeyboard.ITakesKeysFirst, DesktopInspection, DesktopSurface, IKeepsViewersClear {

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
    /** Whether the game's own pointer is hidden over the glass, where the desktop draws its own. */
    private boolean osPointerHidden;

    static final int TASKBAR_H = 24;
    // Frames 11 taskbar: each centered item (Start + one per program) occupies this slot.
    static final int WIN11_SLOT = 22;
    static final int WIN11_ICON = 16;
    /** The width of the Frames XP Start pill, which the task buttons and its own hit-test both clear. */
    static final int XP_START_W = 58;

    /** The desktop that is up, the one the machine's answers and the programs' calls reach; null while none is. */
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

    /** The Open with chooser the open desktop is showing, or null when none is up. */
    @Nullable
    public static OpenWithPopup openWithChooser() {
        return active != null ? active.notices().chooser() : null;
    }

    /**
     * What this desktop calls its terminal (Command Prompt, Megashell, Konsole...), for a program offering to open
     * one; empty when no desktop is up or it has none.
     */
    public static String terminalName() {
        return active == null ? "" : active.opener().terminalLabel();
    }

    /** The launcher labels the active desktop can open (built-in apps plus installed programs). */
    public static List<String> openableLabels() {
        return active != null ? active.launcherLabels() : List.of();
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

    /*
     * The monitor body, so the recipe viewers' lists sit beside the monitor instead of over its bezel. The glass is
     * the container's own rectangle, which the viewers already keep clear.
     */
    @Override
    public List<Rect2i> areasKeptClear() {
        final MonitorFrameStyle.Geometry body = frameBounds();
        return List.of(new Rect2i(body.x(), body.y(), body.w(), body.h()));
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
        // Over the glass the desktop draws the pointer itself, so the game's own is hidden there and only there.
        final double lx = view.localX(mouseX);
        final double ly = view.localY(mouseY);
        showOsPointer(!(ownPointer() && lx >= 0 && ly >= 0 && lx < view.width() && ly < view.height()));
        // The desktop reaches the player through the monitor's tube, over the whole glass once it is drawn.
        TubeFilter.filterScreen(g, view.left(), view.top(), view.glassWidth(), view.glassHeight(),
                MonitorTubes.tubeAt(menu.monitorPos()));
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
        showOsPointer(true);
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

    /** The desktop draws the player's pointer over the glass unless the player switched that off. */
    @Override
    public boolean ownPointer() {
        return ComputersClientConfig.desktopCursors();
    }

    /** Whether the game's own pointer is hidden over the glass right now, which a test reads. */
    public boolean osPointerHidden() {
        return osPointerHidden;
    }

    /** What the desktop's pointer shows right now: arrow, busy, working or launch. */
    public String pointerState() {
        return state.pointers().state().name().toLowerCase(Locale.ROOT);
    }

    /* Shows or hides the game's own pointer, telling the window only when that changes. */
    private void showOsPointer(final boolean shown) {
        if (shown == !osPointerHidden) {
            return;
        }
        osPointerHidden = !shown;
        GLFW.glfwSetInputMode(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_CURSOR,
                shown ? GLFW.GLFW_CURSOR_NORMAL : GLFW.GLFW_CURSOR_HIDDEN);
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
