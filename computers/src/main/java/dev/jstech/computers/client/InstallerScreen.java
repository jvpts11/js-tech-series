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

import java.util.ArrayList;
import java.util.List;

/**
 * A system being installed, in its own installer's words and its own shape.
 *
 * <p>The installer is the machine's: this screen holds the same one the machine does, built from what the machine
 * sent, and walks the same clock. It decides nothing. Every answer goes to the machine and comes back as the page
 * the machine is now on, so two players at two monitors are looking at one installation.
 *
 * <p>Leaving does not cancel: the copy carries on and the answers stay where they were. Quitting outright is only
 * offered while nothing has been written, which is the promise the pages that ask come before the pages that work
 * in order to keep.
 *
 * <p>What the page looks like belongs to {@link InstallerFrames}; what it says belongs here.
 */
public final class InstallerScreen extends Screen {

    private static final int W = 340;
    private static final int H = 214;

    /** How far apart the rows of a list sit, which the mouse also has to know to find the one under it. */
    private static final int ROW = 11;

    /** Where the Size column ends, counted back from the right edge of the table. */
    private static final int SIZE_COLUMN = 120;

    /** The same for the Free column, which sits between Size and Holds. */
    private static final int FREE_COLUMN = 60;

    /** The clear space kept between a drive's name and whatever is written to the right of it. */
    private static final int COLUMN_GAP = 6;

    /** The wash laid over the button under the cursor: enough to read as lit, not enough to change its style. */
    private static final int HOVER_WASH = 0x30FFFFFF;

    /** How long the caret in a name field spends showing, and then hidden, in ticks. */
    private static final int CARET_TICKS = 10;

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
    /** Ticks since this screen opened, which is what turns the caret on and off. */
    private int blink;

    private int listTop;
    private int listLeft;
    private int listWidth;
    private int listRows;
    private int[] nextButton;
    private int[] backButton;
    private int[] cancelButton;
    private int[] eraseButton;

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

    /**
     * The page the installer is showing, by name, for a test that has to answer what it asks.
     *
     * <p>An installer does not ask everything up front: the questions come while the copy runs, at the
     * points the system reaches them, so anything driving one has to wait for a page rather than guess when
     * it will appear. Without this there is no way to tell from outside whether the installer is copying or
     * standing still waiting for somebody.
     */
    public String pageName() {
        return this.flow == null ? "" : this.flow.page().name();
    }

    /** The page the machine has moved to, with the work it has done behind it. */
    public void accept(final OpenInstallerPayload payload) {
        this.flow = payload.flow();
        this.ticksDone = payload.ticksDone();
        this.typed = this.flow.computerName();
        this.erasePrompt = InstallerFlow.NO_DISK;
        this.selection = switch (this.flow.page()) {
            case DISK, SETTINGS -> Math.max(0, this.indexOfSlot(this.flow.targetSlot()));
            case DESKTOP -> this.flow.desktopIndex() + 1;
            default -> 0;
        };
    }

    @Override
    public void tick() {
        super.tick();
        this.blink++;
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
                this.quit();
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
                if (this.naming()) {
                    this.typed = this.typed.isEmpty() ? "" : this.typed.substring(0, this.typed.length() - 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_E -> {
                if (this.offerErase()) {
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
            final List<Integer> pages = this.hubPages();
            final int row = key - GLFW.GLFW_KEY_1;
            if (row < pages.size()) {
                this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                        InstallerActionPayload.ACTION_GO_TO, pages.get(row)));
            }
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public boolean charTyped(final char letter, final int modifiers) {
        if (this.erasePrompt != InstallerFlow.NO_DISK) {
            return true;
        }
        if (this.naming() && letter >= ' ' && letter != 127
                && this.typed.length() < InstallerFlow.MOST_NAME_LETTERS) {
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
        if (hit(this.nextButton, mouseX, mouseY)) {
            this.confirm();
            return true;
        }
        if (hit(this.backButton, mouseX, mouseY)) {
            this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                    InstallerActionPayload.ACTION_BACK));
            return true;
        }
        if (hit(this.cancelButton, mouseX, mouseY)) {
            this.quit();
            return true;
        }
        if (hit(this.eraseButton, mouseX, mouseY)) {
            this.offerErase();
            return true;
        }
        if (this.listRows > 0 && mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth) {
            final int row = (int) ((mouseY - this.listTop) / ROW);
            if (row >= 0 && row < this.listRows) {
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
        final int x = (this.width - W) / 2;
        final int y = (this.height - H) / 2;
        MonitorFrame.renderBody(g, x, y, W, H, null, font);

        this.listRows = 0;
        final InstallerFrames.Frame frame = InstallerFrames.paint(g, font, this.flow, this.ticksDone, x, y, W, H);
        this.nextButton = frame.next();
        this.backButton = frame.back();
        this.cancelButton = frame.cancel();
        this.eraseButton = frame.erase();

        /*
         * The button under the cursor says so. Marked here, over the rectangles every frame hands back, rather
         * than inside each of the five frames: one mark reaches all of them, and a frame drawn later gets it
         * for nothing. Nothing else about the button moves, because a button of these ages did not move.
         */
        this.markHovered(g, mouseX, mouseY);

        switch (this.flow.page()) {
            case DISK, SETTINGS -> this.drawDisks(g, frame);
            case NAME -> this.drawName(g, frame);
            case DESKTOP -> this.drawDesktops(g, frame);
            case HUB -> this.drawHub(g, frame);
            case COPY -> this.drawWork(g, frame);
            case DONE -> this.drawDone(g, frame);
            default -> this.drawWelcome(g, frame);
        }
        if (this.erasePrompt != InstallerFlow.NO_DISK) {
            this.drawEraseAsk(g, x, y, frame);
        }
    }

    /**
     * Lightens whichever button the cursor is over, so a page answers the mouse before it is clicked.
     *
     * <p>A thin wash rather than a redraw: the frames each draw their own buttons in their own age's style,
     * and this has to read as the same button lit up on all of them rather than as a sixth style.
     */
    private void markHovered(final GuiGraphics g, final int mouseX, final int mouseY) {
        for (final int[] box : new int[][]{this.nextButton, this.backButton, this.cancelButton, this.eraseButton}) {
            if (hit(box, mouseX, mouseY)) {
                g.fill(box[0], box[1], box[0] + box[2], box[1] + box[3], HOVER_WASH);
                return;
            }
        }
    }

    /** Whether the page under the cursor is one the player types a name on. */
    private boolean naming() {
        return this.flow.page() == InstallerPage.NAME || this.flow.page() == InstallerPage.SETTINGS;
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
        if (this.naming()) {
            this.send(InstallerActionPayload.named(this.computerPos, this.monitorPos, this.typed));
        }
        if (this.flow.page() == InstallerPage.HUB) {
            // On the list, the key opens the question under the cursor; the work is begun with its own letter.
            final List<Integer> pages = this.hubPages();
            if (this.selection >= 0 && this.selection < pages.size()) {
                this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                        InstallerActionPayload.ACTION_GO_TO, pages.get(this.selection)));
            }
            return;
        }
        this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                InstallerActionPayload.ACTION_NEXT));
    }

    private void quit() {
        if (this.flow.quittable()) {
            this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                    InstallerActionPayload.ACTION_QUIT));
        }
    }

    /** Puts the erase question up, if the page is one where a disk can be erased at all. */
    private boolean offerErase() {
        if (this.flow.page() == InstallerPage.DISK && this.chosenDisk() != null) {
            this.erasePrompt = this.chosenDisk().slot();
            return true;
        }
        return false;
    }

    private boolean eraseKey(final int key) {
        if (key == GLFW.GLFW_KEY_Y || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                    InstallerActionPayload.ACTION_ERASE, this.erasePrompt));
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
    private List<Integer> hubPages() {
        final List<Integer> pages = new ArrayList<>();
        final List<InstallerStyle.Stage> stages = this.flow.style().stages();
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

    /** Remembers where a list was drawn, so a click lands on the row the player is looking at. */
    private void listAt(final int left, final int top, final int width, final int rows) {
        this.listLeft = left;
        this.listTop = top;
        this.listWidth = width;
        this.listRows = rows;
    }

    private void drawWelcome(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final InstallerFlow.Disk disk = this.flow.target();
        int ty = f.y();
        g.drawString(font, "Setup prepares " + this.flow.systemName() + " to run on this computer.",
                f.x(), ty, p.text(), false);
        ty += 18;
        g.drawString(font, this.flow.systemName() + " needs " + size(this.flow.footprintMb()) + ".",
                f.x(), ty, p.dim(), false);
        ty += 16;
        if (disk == null) {
            g.drawString(font, "No disk in this machine has room for it.", f.x(), ty, p.accent(), false);
            return;
        }
        g.drawString(font, "Setup found a disk:", f.x(), ty, p.dim(), false);
        g.drawString(font, "Disk " + disk.slot() + "  " + disk.label(), f.x(), ty + 12, p.bright(), false);
        g.drawString(font, holds(disk) + ", " + size(this.flow.freeOn(disk)) + " free",
                f.x(), ty + 23, p.dim(), false);
    }

    private void drawDisks(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final boolean table = this.flow.chrome() == InstallerChrome.CARD;
        int ty = f.y();
        if (table) {
            g.drawString(font, "Disk", f.x() + 4, ty, p.dim(), false);
            right(g, "Size", f.x() + f.w() - SIZE_COLUMN, ty, p.dim());
            right(g, "Free", f.x() + f.w() - FREE_COLUMN, ty, p.dim());
            right(g, "Holds", f.x() + f.w() - 4, ty, p.dim());
            g.fill(f.x(), ty + 9, f.x() + f.w(), ty + 10, 0xFFE3E5EE);
            ty += 13;
        }
        this.listAt(f.x(), ty, f.w(), this.flow.disks().size());
        for (int i = 0; i < this.flow.disks().size(); i++) {
            final InstallerFlow.Disk disk = this.flow.disks().get(i);
            final boolean here = i == this.selection;
            if (here) {
                g.fill(f.x(), ty - 1, f.x() + f.w(), ty + ROW - 1, p.select());
                if (table) {
                    g.fill(f.x(), ty - 1, f.x() + 2, ty + ROW - 1, p.accent());
                }
            }
            final int row = here ? p.selectText() : p.text();
            final int faint = here ? p.selectText() : p.dim();
            /*
             * The name is cut to the room actually left beside whatever is written to its right, measured
             * rather than guessed. It used to be cut to a fixed width that took no account of how wide the
             * columns beside it had turned out, so a long drive name ran straight through the figures next
             * to it and the two were drawn on top of each other.
             */
            if (table) {
                final String sizeText = size(disk.sizeMb());
                final int sizeLeft = f.x() + f.w() - SIZE_COLUMN - font.width(sizeText);
                g.drawString(font, InstallerFrames.clip(font, "Disk " + disk.slot() + " " + disk.label(),
                        sizeLeft - COLUMN_GAP - (f.x() + 4)), f.x() + 4, ty, row, false);
                right(g, sizeText, f.x() + f.w() - SIZE_COLUMN, ty, faint);
                right(g, size(this.flow.freeOn(disk)), f.x() + f.w() - FREE_COLUMN, ty, faint);
                right(g, disk.hasSystem() ? disk.holds() : "Nothing", f.x() + f.w() - 4, ty, faint);
            } else {
                final String state = holds(disk) + ", " + size(this.flow.freeOn(disk)) + " free";
                g.drawString(font, InstallerFrames.clip(font, "Disk " + disk.slot() + "  " + disk.label(),
                        f.w() - font.width(state) - COLUMN_GAP), f.x(), ty, row, false);
                right(g, state, f.x() + f.w(), ty, faint);
            }
            ty += ROW;
        }
        ty += 6;
        g.drawString(font, this.flow.systemName() + " needs " + size(this.flow.footprintMb()) + ".",
                f.x(), ty, p.dim(), false);
        final InstallerFlow.Disk disk = this.chosenDisk();
        if (disk != null && !this.flow.roomOn(disk)) {
            g.drawString(font, "No room here. Erase this disk, or choose another.", f.x(), ty + 10, p.accent(),
                    false);
        } else if (disk != null && disk.hasSystem()) {
            g.drawString(font, disk.holds() + " on this disk is erased first.", f.x(), ty + 10, p.accent(), false);
        }
        if (this.flow.page() == InstallerPage.SETTINGS) {
            final String label = "Computer name:  ";
            final int room = f.w() - font.width(label);
            g.drawString(font, label + this.tailThatFits(room) + this.caret(), f.x(), ty + 22, p.bright(), false);
        }
    }

    private void drawName(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        int ty = f.y();
        g.drawString(font, "This name identifies the computer at the", f.x(), ty, p.dim(), false);
        g.drawString(font, "prompt and on the network.", f.x(), ty + 10, p.dim(), false);
        ty += 28;
        g.drawString(font, "Computer name:", f.x(), ty, p.text(), false);
        final int fx = f.x();
        final int fy = ty + 12;
        final int fw = Math.min(150, f.w());
        g.fill(fx, fy, fx + fw, fy + 13, 0xFFFFFFFF);
        g.fill(fx, fy + 12, fx + fw, fy + 13, p.accent());
        g.drawString(font, this.tailThatFits(fw - 6) + this.caret(), fx + 3, fy + 3, 0xFF202434, false);
    }

    /**
     * The end of what has been typed, as much of it as {@code room} pixels hold.
     *
     * <p>The end rather than the beginning, because the end is where the typing is happening: a name longer
     * than the field scrolls under the caret the way a text field does, instead of running out past the edge
     * of the box and over whatever is drawn beside it.
     */
    private String tailThatFits(final int room) {
        final int left = Math.max(0, room - font.width("_"));
        String tail = this.typed;
        while (!tail.isEmpty() && font.width(tail) > left) {
            tail = tail.substring(1);
        }
        return tail;
    }

    /** The caret, showing and hidden by turns, so a field waiting to be typed in looks like one. */
    private String caret() {
        return this.blink / CARET_TICKS % 2 == 0 ? "_" : " ";
    }

    private void drawDesktops(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        int ty = f.y();
        if (this.flow.mirrorAnswers()) {
            g.drawString(font, "The Mirror on " + this.flow.mirrorHost() + " answers.", f.x(), ty, p.dim(), false);
        } else {
            g.drawString(font, "No Mirror answers, so it comes up at its terminal.", f.x(), ty, p.dim(), false);
        }
        ty += 16;
        this.listAt(f.x(), ty, f.w(), this.flow.desktops().size() + 1);
        for (int i = 0; i <= this.flow.desktops().size(); i++) {
            final boolean here = i == this.selection;
            if (here) {
                g.fill(f.x(), ty - 1, f.x() + f.w(), ty + ROW - 1, p.select());
            }
            final int row = here ? p.selectText() : p.text();
            if (i == 0) {
                g.drawString(font, "None, the terminal only", f.x() + 2, ty, row, false);
            } else {
                final InstallerFlow.Desktop desktop = this.flow.desktops().get(i - 1);
                g.drawString(font, desktop.name(), f.x() + 2, ty, row, false);
                right(g, size(desktop.sizeMb()), f.x() + f.w(), ty, here ? p.selectText() : p.dim());
            }
            ty += ROW;
        }
    }

    private void drawHub(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        int ty = f.y();
        final List<Integer> pages = this.hubPages();
        this.listAt(f.x(), ty, f.w(), 0);
        for (int i = 0; i < pages.size(); i++) {
            final InstallerPage page = this.flow.style().stages().get(pages.get(i)).page();
            final boolean wanted = this.flow.wants(page);
            final boolean here = i == this.selection;
            if (here) {
                g.fill(f.x(), ty - 1, f.x() + f.w(), ty + 20, p.select());
            }
            g.drawString(font, (i + 1) + ") " + (wanted ? "[!]" : "[x]") + " "
                            + this.flow.style().heading(page, this.flow.systemName()),
                    f.x() + 2, ty, here ? p.selectText() : wanted ? p.accent() : p.text(), false);
            g.drawString(font, this.answerFor(page), f.x() + 22, ty + 10, here ? p.selectText() : p.dim(), false);
            ty += 22;
        }
        g.drawString(font, "A letter begins the installation once nothing is still wanted.", f.x(), ty + 4,
                p.dim(), false);
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

    private void drawWork(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        /*
         * The frame with the steps down its side has already listed them; repeating them in the middle of the
         * screen would be the same account twice, so there it only says what it is working on.
         */
        if (this.flow.chrome() == InstallerChrome.SIDE_PANEL) {
            final InstallerFlow.Step step = this.flow.steps().get(this.flow.stepAt(this.ticksDone));
            g.drawString(font, step.label(), f.x(), f.y(), p.text(), false);
            final InstallerFlow.Disk disk = this.flow.target();
            if (disk != null) {
                g.drawString(font, "Installing on Disk " + disk.slot() + ", " + disk.label(), f.x(), f.y() + 12,
                        p.dim(), false);
            }
            return;
        }
        final int running = this.flow.stepAt(this.ticksDone);
        int ty = f.y();
        for (int i = 0; i < this.flow.steps().size(); i++) {
            final InstallerFlow.Step step = this.flow.steps().get(i);
            final boolean done = i < running;
            g.drawString(font, InstallerFrames.clip(font, step.label(), f.w() - 40), f.x(), ty,
                    done || i == running ? p.text() : p.dim(), false);
            if (done) {
                right(g, "done", f.x() + f.w(), ty, p.dim());
            } else if (i == running) {
                right(g, this.flow.stepPermille(this.ticksDone) / 10 + "%", f.x() + f.w(), ty, p.accent());
            }
            ty += ROW;
        }
        ty += 8;
        g.fill(f.x(), ty, f.x() + f.w(), ty + 6, p.dim());
        g.fill(f.x(), ty, f.x() + f.w() * this.flow.permille(this.ticksDone) / 1000, ty + 6, p.accent());
        final int left = Math.max(0, (this.flow.ticksTotal() - this.ticksDone) / 20);
        g.drawString(font, "About " + left + " seconds left. Leave the medium in.", f.x(), ty + 12, p.dim(),
                false);
    }

    private void drawDone(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        int ty = f.y();
        g.drawString(font, this.flow.systemName() + " is installed.", f.x(), ty, p.bright(), false);
        ty += 18;
        g.drawString(font, "Take the installation medium out of the drive,", f.x(), ty, p.text(), false);
        g.drawString(font, "or the machine starts Setup again.", f.x(), ty + 10, p.text(), false);
        g.drawString(font, "Restart to start " + this.flow.systemName() + ".", f.x(), ty + 28, p.accent(), false);
    }

    private void drawEraseAsk(final GuiGraphics g, final int x, final int y, final InstallerFrames.Frame f) {
        final InstallerFlow.Disk disk = this.flow.diskAt(this.erasePrompt);
        if (disk == null) {
            this.erasePrompt = InstallerFlow.NO_DISK;
            return;
        }
        final int bw = 250;
        final int bh = 74;
        final int bx = x + (W - bw) / 2;
        final int by = y + (H - bh) / 2;
        g.fill(x, y, x + W, y + H, 0x99000000);
        g.fill(bx, by, bx + bw, by + bh, 0xFFFAFAFE);
        g.fill(bx, by, bx + bw, by + 1, 0xFFC42B1C);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, 0xFFC0C4D2);
        g.fill(bx, by, bx + 1, by + bh, 0xFFC0C4D2);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, 0xFFC0C4D2);
        g.drawString(font, "Erase Disk " + disk.slot() + "?", bx + 10, by + 10, 0xFF202434, false);
        g.drawString(font, (disk.hasSystem() ? disk.holds() + " and every file on" : "Every file on"),
                bx + 10, by + 26, 0xFF202434, false);
        g.drawString(font, InstallerFrames.clip(font, disk.label(), bw - 20) + " will be deleted.",
                bx + 10, by + 36, 0xFF202434, false);
        g.drawString(font, "This cannot be undone.", bx + 10, by + 46, 0xFF6B7488, false);
        g.drawString(font, "Y = erase        N = cancel", bx + 10, by + 60, 0xFFC42B1C, false);
    }

    private void right(final GuiGraphics g, final String text, final int rightEdge, final int y, final int colour) {
        g.drawString(font, text, rightEdge - font.width(text), y, colour, false);
    }

    private static boolean hit(final int[] rect, final double mouseX, final double mouseY) {
        return rect != null && mouseX >= rect[0] && mouseX < rect[0] + rect[2]
                && mouseY >= rect[1] && mouseY < rect[1] + rect[3];
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
}
