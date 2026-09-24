/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.operation.payload.RunProgramPayload;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import java.util.ArrayDeque;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A terminal window for the desktop: the same CLI as the Command Prompt (dir, type, write, del, run,
 * iql, operation, ...) inside a Frames window. This is how a graphical OS, whose monitor never shows the
 * Command Prompt directly, still reaches the filesystem and the network from the keyboard.
 *
 * <p>The console itself is the machine's, and this window is one view of it. Everything about scrollback,
 * the prompt and talking to the shell lives in {@link ShellView}; what is left here is the window: what
 * it is called on this desktop, how big it opens, and where the keys go.
 */
public final class ShellApp implements IDesktopApp {

    /** What a window's frame takes round its content: the border, and the title bar above. */
    private static final int FRAME_W = 8;
    private static final int FRAME_H = DesktopWindow.TITLE_H + 8;

    /** The rows a terminal opens with. */
    private static final int ROWS = 24;

    private final ShellView view;
    private OsSkin skin = OsSkin.fallback();

    /** Where the pointer was last seen over the window, which is where a turn of the wheel happens. */
    private double pointerX;
    private double pointerY;

    /** The window title: the desktop environment's own terminal name (Konsole, Terminal, Megashell...). */
    private final String title;

    private final BlockPos host;

    /** Lines still to be typed here, one after the other as each finishes. */
    private final ArrayDeque<String> lines = new ArrayDeque<>();

    public ShellApp(final BlockPos host) {
        this(host, null);
    }

    public ShellApp(final BlockPos host, @Nullable final ResourceLocation desktopId) {
        final DesktopEnvironmentDef chrome = desktopId == null ? null : OsRegistry.getDesktop(desktopId);
        // The desktop says which family it stands on; a list kept here was what left the next desktop out.
        final boolean posix = chrome != null && chrome.panelStyle().unixLike();
        final ProgramSpec promptSpec = Programs.get(Programs.COMMAND_PROMPT);
        /*
         * Frames 11 ships its own modern shell ("Megashell"); every other desktop names the window after its
         * native terminal (Konsole on KDE, Terminal on GNOME/Cinnamon, Command Prompt on the older Frames).
         */
        this.title = chrome != null && promptSpec != null ? GameText.resolve(chrome.launcherLabel(promptSpec))
                : GameText.resolve(ShellAppTexts.TITLE);
        this.host = host;
        /*
         * A shell greets the player with the system it belongs to. On this family the desktop and the system are
         * the same thing by the same name, so the desktop is where the name comes from; a Unix terminal opens on
         * a bare prompt and greets nobody.
         */
        this.view = new ShellView(host, posix, chrome == null || posix ? "" : chrome.displayName()).asOwnWindow();
        this.view.setOnIdle(this::typeNext);
    }

    /**
     * Runs a compiled program here, as the explorer's double click on one does.
     *
     * <p>The window exists before anything the program prints can land in it, which is why the
     * desktop opens or raises the terminal first and only then hands it the program.
     */
    public void runProgram(final String path) {
        this.view.say(path, CliStyle.PROMPT);
        PacketDistributor.sendToServer(new RunProgramPayload(this.host, path, this.view.session()));
    }

    /**
     * Types lines here one after the other, the next only once the machine has answered the one
     * before, the way a studio hands the shell a job it cannot do itself.
     */
    public void typeLines(final List<String> typed) {
        this.lines.addAll(typed);
        if (!this.view.busy()) {
            typeNext();
        }
    }

    /** Whether an editor has taken the terminal over, which is what a test waits for after typing vim. */
    public boolean editing() {
        return this.view.editing();
    }

    /** The prompt as it stands, which says where the terminal is. */
    public String prompt() {
        return this.view.prompt();
    }

    /** The text of the file the editor holding the terminal has, or empty. */
    public String editorText() {
        return this.view.editorText();
    }

    /** What the editor's second buffer shows (what the compiler said), or empty. */
    public String editorLowerText() {
        return this.view.editorLowerText();
    }

    /** An editor that has the terminal uses Escape for its own modes, so it must not close the desktop. */
    @Override
    public boolean wantsEscape() {
        return this.view.editing();
    }

    /** Everything the terminal has printed so far, one line after another. */
    public String scrollbackText() {
        return this.view.scrollbackText();
    }

    private void typeNext() {
        final String next = this.lines.poll();
        if (next != null) {
            this.view.run(next);
        }
    }

    @Override
    public String title() {
        return this.title;
    }

    /* A terminal opens eighty columns by twenty-four rows, as terminals do; a desktop too small for that shrinks it. */
    @Override
    public int defaultWidth() {
        return ShellView.widthFor(TermBuffer.MONITOR_COLUMNS) + FRAME_W;
    }

    @Override
    public int defaultHeight() {
        return ShellView.heightFor(ROWS) + FRAME_H;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
        this.view.setSkin(osSkin);
    }

    @Override
    public void onClosed() {
        this.view.release();
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        this.view.setBounds(x, y, width, height);
        this.pointerX = mouseX;
        this.pointerY = mouseY;
        this.view.render(g, new UiContext(this.skin, font, mouseX, mouseY, partialTick));
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        this.view.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        this.view.mouseDragged(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        this.view.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        return this.view.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c) {
        return this.view.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return this.view.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        return this.view.mouseScrolled(this.pointerX, this.pointerY, delta);
    }
}
