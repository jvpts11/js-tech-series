/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.operation.payload.InstallerActionPayload;
import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import dev.jstech.computers.os.install.InstallerChrome;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.InstallerStyle;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * A system being installed, in its own installer's words.
 *
 * <p>The installer is the machine's: this screen holds the same one the machine does, built from what the machine
 * sent, and walks the same clock. It decides nothing. Every answer goes to the machine and comes back as the page
 * the machine is now on, so two players at two monitors are looking at one installation.
 *
 * <p>Leaving does not cancel: the copy carries on and the answers stay where they were. Quitting outright is only
 * offered while nothing has been written, which is the promise the pages that ask come before the pages that work
 * in order to keep.
 */
public final class InstallerScreen extends Screen {

    private static final int W = 340;
    private static final int H = 214;

    /** Where the body of the page starts under the title and the heading. */
    private static final int BODY_TOP = 40;

    /** The height of the bar of keys along the foot. */
    private static final int FOOT = 14;

    private final BlockPos computerPos;
    private final BlockPos monitorPos;

    private InstallerFlow flow;
    private int ticksDone;
    /** Which row of the page's list is under the cursor. */
    private int selection;
    /** The disk an erase has been offered for, kept here because the offer is the screen's and not the machine's. */
    private int erasePrompt = InstallerFlow.NO_DISK;
    /** The name being typed, which only reaches the machine when the page is left. */
    private String typed = "";

    public InstallerScreen(final OpenInstallerPayload payload) {
        super(Component.literal("Setup"));
        this.computerPos = payload.hostPos();
        this.monitorPos = payload.monitorPos();
        this.accept(payload);
    }

    /** Whether this screen is showing that machine, so a page for it updates instead of opening a second screen. */
    public boolean isFor(final BlockPos pos) {
        return this.computerPos.equals(pos);
    }

    /** The page the machine has moved to, with the work it has done behind it. */
    public void accept(final OpenInstallerPayload payload) {
        this.flow = payload.flow();
        this.ticksDone = payload.ticksDone();
        this.typed = this.flow.computerName();
        this.erasePrompt = InstallerFlow.NO_DISK;
        this.selection = switch (this.flow.page()) {
            case DISK, SETTINGS -> Math.max(0, indexOfSlot(this.flow.targetSlot()));
            case DESKTOP -> this.flow.desktopIndex() + 1;
            default -> 0;
        };
    }

    @Override
    public void tick() {
        super.tick();
        /*
         * The clock is the machine's and this only follows it: the work runs to the end of the page it is on and
         * waits there, exactly as the machine does, so the two never disagree about what is happening.
         */
        if (this.ticksDone < this.flow.ticksUnlocked()) {
            this.ticksDone++;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int modifiers) {
        if (this.erasePrompt != InstallerFlow.NO_DISK) {
            return this.eraseKey(key);
        }
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE -> {
                // Leaving the monitor is not leaving the installation: the machine keeps both.
                this.onClose();
                return true;
            }
            case GLFW.GLFW_KEY_F3 -> {
                if (this.flow.quittable()) {
                    this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                            InstallerActionPayload.ACTION_QUIT));
                }
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                this.confirm();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                this.move(-1);
                return true;
            }
            case GLFW.GLFW_KEY_DOWN -> {
                this.move(1);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (this.flow.page() == InstallerPage.NAME || this.flow.page() == InstallerPage.SETTINGS) {
                    this.typed = this.typed.isEmpty() ? "" : this.typed.substring(0, this.typed.length() - 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_E -> {
                if (this.flow.page() == InstallerPage.DISK && this.chosenDisk() != null) {
                    this.erasePrompt = this.chosenDisk().slot();
                    return true;
                }
            }
            case GLFW.GLFW_KEY_B -> {
                if (this.flow.page() == InstallerPage.HUB) {
                    // The one letter that starts the work on the installer that gathers its questions first.
                    this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                            InstallerActionPayload.ACTION_NEXT));
                    return true;
                }
            }
            default -> {
            }
        }
        if (this.flow.page() == InstallerPage.HUB && key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9) {
            this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                    InstallerActionPayload.ACTION_GO_TO, key - GLFW.GLFW_KEY_1 + 1));
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(final char letter, final int modifiers) {
        if (this.erasePrompt != InstallerFlow.NO_DISK) {
            return true;
        }
        final boolean naming = this.flow.page() == InstallerPage.NAME
                || this.flow.page() == InstallerPage.SETTINGS;
        if (naming && letter >= ' ' && letter != 127 && this.typed.length() < InstallerFlow.MOST_NAME_LETTERS) {
            this.typed = this.typed + letter;
            return true;
        }
        return super.charTyped(letter, modifiers);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (this.erasePrompt != InstallerFlow.NO_DISK) {
            return true;
        }
        final int rows = this.rowCount();
        if (rows > 0) {
            final int top = this.top() + BODY_TOP;
            final int row = (int) ((mouseY - top) / 11);
            if (mouseX >= this.left() && mouseX < this.left() + W && row >= 0 && row < rows) {
                this.selection = row;
                this.chose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final int x = this.left();
        final int y = this.top();
        final Look look = Look.of(this.flow.style());
        MonitorFrame.renderBody(g, x, y, W, H, null, font);
        g.fill(x, y, x + W, y + H, look.back());

        final boolean boxed = this.flow.chrome() == InstallerChrome.BOXED_TEXT;
        final int inner = boxed ? x + 10 : x;
        final int innerRight = boxed ? x + W - 10 : x + W;
        if (boxed) {
            g.fill(inner, y + 10, innerRight, y + H - FOOT - 6, look.panel());
        }

        final String title = this.flow.style().title(this.flow.systemName());
        g.drawString(font, title, inner + 8, y + 8, boxed ? look.panelText() : look.bright(), false);
        final String heading = this.flow.style().heading(this.flow.page(), this.flow.systemName());
        g.drawString(font, heading, inner + 8, y + 22, boxed ? look.panelText() : look.text(), false);

        switch (this.flow.page()) {
            case DISK, SETTINGS -> this.drawDisks(g, x, y, look, boxed);
            case NAME -> this.drawName(g, x, y, look, boxed);
            case DESKTOP -> this.drawDesktops(g, x, y, look, boxed);
            case HUB -> this.drawHub(g, x, y, look);
            case COPY -> this.drawWork(g, x, y, look, boxed);
            case DONE -> this.drawDone(g, x, y, look, boxed);
            default -> this.drawWelcome(g, x, y, look, boxed);
        }

        final String hint = this.flow.style().hint(this.flow.page());
        if (!hint.isEmpty()) {
            g.fill(x, y + H - FOOT, x + W, y + H, look.bar());
            g.drawString(font, hint, x + 6, y + H - FOOT + 3, look.barText(), false);
        }
        if (this.erasePrompt != InstallerFlow.NO_DISK) {
            this.drawEraseAsk(g, x, y, look);
        }
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    /** How many rows the page under the cursor has, so the arrows and the mouse agree about them. */
    private int rowCount() {
        return switch (this.flow.page()) {
            case DISK, SETTINGS -> this.flow.disks().size();
            case DESKTOP -> this.flow.desktops().size() + 1;
            case HUB -> this.hubPages().size();
            default -> 0;
        };
    }

    private void move(final int by) {
        final int rows = this.rowCount();
        if (rows <= 0) {
            return;
        }
        this.selection = Math.floorMod(this.selection + by, rows);
        this.chose();
    }

    /** Tells the machine what the cursor has landed on, for the pages where moving it is itself an answer. */
    private void chose() {
        switch (this.flow.page()) {
            case DISK, SETTINGS -> {
                final InstallerFlow.Disk disk = this.chosenDisk();
                if (disk != null) {
                    this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                            InstallerActionPayload.ACTION_SELECT_DISK, disk.slot()));
                }
            }
            case DESKTOP -> this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                    InstallerActionPayload.ACTION_DESKTOP, this.selection - 1));
            default -> {
            }
        }
    }

    /** The one action every page has: on to the next, or the restart that ends the last one. */
    private void confirm() {
        if (this.flow.page() == InstallerPage.DONE) {
            this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                    InstallerActionPayload.ACTION_REBOOT));
            return;
        }
        if (this.flow.page() == InstallerPage.NAME || this.flow.page() == InstallerPage.SETTINGS) {
            this.send(InstallerActionPayload.named(this.computerPos, this.monitorPos, this.typed));
        }
        if (this.flow.page() == InstallerPage.HUB) {
            // On the list, the key opens the question under the cursor; the work is begun with its own letter.
            final java.util.List<Integer> pages = this.hubPages();
            if (this.selection >= 0 && this.selection < pages.size()) {
                this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                        InstallerActionPayload.ACTION_GO_TO, pages.get(this.selection)));
            }
            return;
        }
        this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                InstallerActionPayload.ACTION_NEXT));
    }

    private boolean eraseKey(final int key) {
        if (key == GLFW.GLFW_KEY_Y || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                    InstallerActionPayload.ACTION_ERASE, this.erasePrompt));
            this.erasePrompt = InstallerFlow.NO_DISK;
            return true;
        }
        this.erasePrompt = InstallerFlow.NO_DISK;
        return true;
    }

    private InstallerFlow.Disk chosenDisk() {
        return this.selection >= 0 && this.selection < this.flow.disks().size()
                ? this.flow.disks().get(this.selection) : null;
    }

    private int indexOfSlot(final int slot) {
        for (int i = 0; i < this.flow.disks().size(); i++) {
            if (this.flow.disks().get(i).slot() == slot) {
                return i;
            }
        }
        return 0;
    }

    /** The pages the installer that gathers its questions lists, by their place in its order. */
    private java.util.List<Integer> hubPages() {
        final java.util.List<Integer> pages = new java.util.ArrayList<>();
        final java.util.List<InstallerStyle.Stage> stages = this.flow.style().stages();
        for (int i = 0; i < stages.size(); i++) {
            if (stages.get(i).asks() && stages.get(i).page() != InstallerPage.HUB) {
                pages.add(i);
            }
        }
        return pages;
    }

    private void send(final InstallerActionPayload payload) {
        PacketDistributor.sendToServer(payload);
    }

    private void drawWelcome(final GuiGraphics g, final int x, final int y, final Look look, final boolean boxed) {
        final int text = boxed ? look.panelText() : look.text();
        final InstallerFlow.Disk disk = this.flow.target();
        int ty = y + BODY_TOP;
        g.drawString(font, "Setup prepares " + this.flow.systemName() + " to run on this computer.",
                x + 16, ty, text, false);
        ty += 18;
        g.drawString(font, this.flow.systemName() + " needs " + size(this.flow.footprintMb()) + ".",
                x + 16, ty, look.dim(), false);
        ty += 16;
        if (disk == null) {
            g.drawString(font, "No disk in this machine has room for it.", x + 16, ty, look.accent(), false);
            return;
        }
        g.drawString(font, "Setup found a disk:", x + 16, ty, look.dim(), false);
        g.drawString(font, "Disk " + disk.slot() + "  " + disk.label(), x + 16, ty + 12, look.bright(), false);
        g.drawString(font, holds(disk) + ", " + size(this.flow.freeOn(disk)) + " free",
                x + 16, ty + 23, look.dim(), false);
    }

    private void drawDisks(final GuiGraphics g, final int x, final int y, final Look look, final boolean boxed) {
        final int text = boxed ? look.panelText() : look.text();
        int ty = y + BODY_TOP;
        for (int i = 0; i < this.flow.disks().size(); i++) {
            final InstallerFlow.Disk disk = this.flow.disks().get(i);
            final boolean here = i == this.selection;
            if (here) {
                g.fill(x + 12, ty - 1, x + W - 12, ty + 10, look.select());
            }
            final int row = here ? look.selectText() : text;
            g.drawString(font, "Disk " + disk.slot() + "  " + disk.label(), x + 16, ty, row, false);
            final String right = holds(disk) + ", " + size(this.flow.freeOn(disk)) + " free";
            g.drawString(font, right, x + W - 16 - font.width(right), ty, here ? look.selectText() : look.dim(),
                    false);
            ty += 11;
        }
        ty += 8;
        g.drawString(font, this.flow.systemName() + " needs " + size(this.flow.footprintMb()) + ".",
                x + 16, ty, look.dim(), false);
        final InstallerFlow.Disk disk = this.chosenDisk();
        if (disk != null && !this.flow.roomOn(disk)) {
            g.drawString(font, "There is no room on this disk. Erase it with E, or choose another.",
                    x + 16, ty + 11, look.accent(), false);
        } else if (disk != null && disk.hasSystem()) {
            g.drawString(font, disk.holds() + " on this disk is erased first.", x + 16, ty + 11, look.accent(),
                    false);
        }
        if (this.flow.page() == InstallerPage.SETTINGS) {
            g.drawString(font, "Computer name:  " + this.typed + "_", x + 16, ty + 24, look.bright(), false);
        }
    }

    private void drawName(final GuiGraphics g, final int x, final int y, final Look look, final boolean boxed) {
        final int text = boxed ? look.panelText() : look.text();
        int ty = y + BODY_TOP;
        g.drawString(font, "This name identifies the computer at the prompt and on the network.",
                x + 16, ty, look.dim(), false);
        ty += 20;
        g.drawString(font, "Computer name:", x + 16, ty, text, false);
        g.fill(x + 110, ty - 2, x + 260, ty + 11, look.select());
        g.drawString(font, this.typed + "_", x + 114, ty, look.selectText(), false);
    }

    private void drawDesktops(final GuiGraphics g, final int x, final int y, final Look look, final boolean boxed) {
        final int text = boxed ? look.panelText() : look.text();
        int ty = y + BODY_TOP;
        if (this.flow.mirrorAnswers()) {
            g.drawString(font, "The Mirror on " + this.flow.mirrorHost() + " answers.", x + 16, ty, look.dim(),
                    false);
        } else {
            g.drawString(font, "No Mirror answers, so " + this.flow.systemName()
                    + " comes up at its terminal.", x + 16, ty, look.dim(), false);
        }
        ty += 16;
        for (int i = 0; i <= this.flow.desktops().size(); i++) {
            final boolean here = i == this.selection;
            if (here) {
                g.fill(x + 12, ty - 1, x + W - 12, ty + 10, look.select());
            }
            final int row = here ? look.selectText() : text;
            if (i == 0) {
                g.drawString(font, "None, the terminal only", x + 16, ty, row, false);
            } else {
                final InstallerFlow.Desktop desktop = this.flow.desktops().get(i - 1);
                g.drawString(font, desktop.name(), x + 16, ty, row, false);
                final String right = size(desktop.sizeMb());
                g.drawString(font, right, x + W - 16 - font.width(right), ty,
                        here ? look.selectText() : look.dim(), false);
            }
            ty += 11;
        }
    }

    private void drawHub(final GuiGraphics g, final int x, final int y, final Look look) {
        int ty = y + BODY_TOP;
        final java.util.List<Integer> pages = this.hubPages();
        for (int i = 0; i < pages.size(); i++) {
            final InstallerPage page = this.flow.style().stages().get(pages.get(i)).page();
            final boolean wanted = this.flow.wants(page);
            final boolean here = i == this.selection;
            if (here) {
                g.fill(x + 12, ty - 1, x + W - 12, ty + 10, look.select());
            }
            final String mark = wanted ? "[!]" : "[x]";
            g.drawString(font, (i + 1) + ") " + mark + " "
                            + this.flow.style().heading(page, this.flow.systemName()),
                    x + 16, ty, here ? look.selectText() : wanted ? look.accent() : look.text(), false);
            g.drawString(font, this.answerFor(page), x + 40, ty + 10,
                    here ? look.selectText() : look.dim(), false);
            ty += 22;
        }
    }

    /** What each question on the list has been answered with so far, in a few words. */
    private String answerFor(final InstallerPage page) {
        return switch (page) {
            case DISK, SETTINGS -> this.flow.target() == null ? "(no disk selected)"
                    : "(Disk " + this.flow.target().slot() + ", " + this.flow.target().label() + ")";
            case NAME -> this.flow.computerName().isBlank() ? "(not set)" : "(" + this.flow.computerName() + ")";
            case DESKTOP -> this.flow.desktop() == null ? "(the terminal only)"
                    : "(" + this.flow.desktop().name() + ")";
            default -> "";
        };
    }

    private void drawWork(final GuiGraphics g, final int x, final int y, final Look look, final boolean boxed) {
        final int text = boxed ? look.panelText() : look.text();
        final int running = this.flow.stepAt(this.ticksDone);
        int ty = y + BODY_TOP;
        for (int i = 0; i < this.flow.steps().size(); i++) {
            final InstallerFlow.Step step = this.flow.steps().get(i);
            final boolean done = i < running;
            g.drawString(font, step.label(), x + 16, ty, done || i == running ? text : look.dim(), false);
            if (done) {
                g.drawString(font, "done", x + W - 16 - font.width("done"), ty, look.dim(), false);
            } else if (i == running) {
                final String pc = this.flow.stepPermille(this.ticksDone) / 10 + "%";
                g.drawString(font, pc, x + W - 16 - font.width(pc), ty, look.accent(), false);
            }
            ty += 11;
        }
        ty += 10;
        g.fill(x + 16, ty, x + W - 16, ty + 8, look.dim());
        g.fill(x + 16, ty, x + 16 + (W - 32) * this.flow.permille(this.ticksDone) / 1000, ty + 8, look.accent());
        final int left = Math.max(0, (this.flow.ticksTotal() - this.ticksDone) / 20);
        g.drawString(font, "About " + left + " seconds left. Do not take the medium out.",
                x + 16, ty + 14, look.dim(), false);
    }

    private void drawDone(final GuiGraphics g, final int x, final int y, final Look look, final boolean boxed) {
        final int text = boxed ? look.panelText() : look.text();
        int ty = y + BODY_TOP;
        g.drawString(font, this.flow.systemName() + " is installed.", x + 16, ty, look.bright(), false);
        ty += 18;
        g.drawString(font, "Take the installation medium out of the drive,", x + 16, ty, text, false);
        g.drawString(font, "or the machine starts Setup again.", x + 16, ty + 11, text, false);
        ty += 30;
        g.drawString(font, "Press ENTER to restart into " + this.flow.systemName() + ".", x + 16, ty,
                look.accent(), false);
    }

    private void drawEraseAsk(final GuiGraphics g, final int x, final int y, final Look look) {
        final InstallerFlow.Disk disk = this.flow.diskAt(this.erasePrompt);
        if (disk == null) {
            this.erasePrompt = InstallerFlow.NO_DISK;
            return;
        }
        final int bw = 280;
        final int bh = 76;
        final int bx = x + (W - bw) / 2;
        final int by = y + (H - bh) / 2;
        g.fill(x, y, x + W, y + H, 0x99000000);
        g.fill(bx, by, bx + bw, by + bh, look.panel());
        g.fill(bx, by, bx + bw, by + 1, look.accent());
        g.fill(bx, by + bh - 1, bx + bw, by + bh, look.accent());
        final int text = look.panel() == look.back() ? look.text() : look.panelText();
        g.drawString(font, "Erase Disk " + disk.slot() + "?", bx + 10, by + 10, text, false);
        g.drawString(font, (disk.hasSystem() ? disk.holds() + " and every file on " : "Every file on ")
                + disk.label(), bx + 10, by + 26, text, false);
        g.drawString(font, "will be deleted. This cannot be undone.", bx + 10, by + 37, text, false);
        g.drawString(font, "Y = erase        N = cancel", bx + 10, by + 56, look.accent(), false);
    }

    /** The system on a disk, or the plain words for one that carries none. */
    private static String holds(final InstallerFlow.Disk disk) {
        return disk.hasSystem() ? disk.holds() : "no system";
    }

    /** Megabytes as a person reads them: whole gigabytes where they are whole, megabytes otherwise. */
    private static String size(final int mb) {
        if (mb >= 1_048_576 && mb % 1_048_576 == 0) {
            return mb / 1_048_576 + " TB";
        }
        if (mb >= 1_024 && mb % 1_024 == 0) {
            return mb / 1_024 + " GB";
        }
        return mb + " MB";
    }

    /**
     * The colours of one installer.
     *
     * <p>The shapes the mock draws these in arrive with the rest of the chromes; what is here is each
     * installer's own ground and ink, so a machine already reads as the system it is putting on itself.
     */
    private record Look(int back, int text, int bright, int dim, int bar, int barText, int select, int selectText,
                        int accent, int panel, int panelText) {

        static Look of(final InstallerStyle style) {
            return switch (style) {
                case MC_DOS -> new Look(0xFF000000, 0xFF41D862, 0xFFA4FFBC, 0xFF1F8A3F, 0xFF41D862, 0xFF000000,
                        0xFF41D862, 0xFF000000, 0xFFA4FFBC, 0xFF000000, 0xFF41D862);
                case FRAMES_95, FRAMES_XP -> new Look(0xFF0000A8, 0xFFC0C0C0, 0xFFFFFFFF, 0xFF7B7BB8,
                        0xFFC0C0C0, 0xFF000000, 0xFFC0C0C0, 0xFF0000A8, 0xFFFFFF55, 0xFFC0C0C0, 0xFF000000);
                case FRAMES_11 -> new Look(0xFF0B1530, 0xFFE8EAF2, 0xFFFFFFFF, 0xFF9AA2B2, 0xFF3A6AE0,
                        0xFFFFFFFF, 0xFF3A6AE0, 0xFFFFFFFF, 0xFF7FA6FF, 0xFF0B1530, 0xFFE8EAF2);
                case UBUNTU -> new Look(0xFF111111, 0xFFE6E6E6, 0xFFFFFFFF, 0xFF8A8A8A, 0xFFE95420, 0xFFFFFFFF,
                        0xFFE95420, 0xFFFFFFFF, 0xFFE95420, 0xFF111111, 0xFFE6E6E6);
                case DEBIAN -> new Look(0xFF0000A8, 0xFF000000, 0xFF000000, 0xFF5A5A5A, 0xFF0000A8, 0xFFFFFFFF,
                        0xFFA80000, 0xFFFFFFFF, 0xFFA80000, 0xFFC0C0C0, 0xFF000000);
                case FEDORA -> new Look(0xFF000000, 0xFFE6E6E6, 0xFFFFFFFF, 0xFF8A8A8A, 0xFF1A1A1A, 0xFFE6E6E6,
                        0xFF294172, 0xFFFFFFFF, 0xFF5FE07A, 0xFF000000, 0xFFE6E6E6);
                case PLAIN -> new Look(0xFF10151B, 0xFFCDD6E2, 0xFF39D6C4, 0xFF7D8A9C, 0xFF19212B, 0xFFCDD6E2,
                        0xFF19212B, 0xFF39D6C4, 0xFF39D6C4, 0xFF10151B, 0xFFCDD6E2);
            };
        }
    }
}
