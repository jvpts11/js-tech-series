/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import static dev.jstech.computers.client.FirmwareScreenTexts.of;
import static dev.jstech.computers.client.InstallerScreenTexts.AND_EVERY_FILE;
import static dev.jstech.computers.client.InstallerScreenTexts.ANSWER;
import static dev.jstech.computers.client.InstallerScreenTexts.CANNOT_UNDO;
import static dev.jstech.computers.client.InstallerScreenTexts.COLUMN_DISK;
import static dev.jstech.computers.client.InstallerScreenTexts.COLUMN_FREE;
import static dev.jstech.computers.client.InstallerScreenTexts.COLUMN_HOLDS;
import static dev.jstech.computers.client.InstallerScreenTexts.COLUMN_SIZE;
import static dev.jstech.computers.client.InstallerScreenTexts.COMPLETE;
import static dev.jstech.computers.client.InstallerScreenTexts.COMPUTER_NAME;
import static dev.jstech.computers.client.InstallerScreenTexts.COPYING;
import static dev.jstech.computers.client.InstallerScreenTexts.DISK;
import static dev.jstech.computers.client.InstallerScreenTexts.DISK_ANSWER;
import static dev.jstech.computers.client.InstallerScreenTexts.DISK_NARROW;
import static dev.jstech.computers.client.InstallerScreenTexts.DISK_WIDE;
import static dev.jstech.computers.client.InstallerScreenTexts.DONE;
import static dev.jstech.computers.client.InstallerScreenTexts.ERASED_FIRST;
import static dev.jstech.computers.client.InstallerScreenTexts.ERASE_ASK;
import static dev.jstech.computers.client.InstallerScreenTexts.ERASE_KEYS;
import static dev.jstech.computers.client.InstallerScreenTexts.EVERY_FILE;
import static dev.jstech.computers.client.InstallerScreenTexts.FOUND_DISK;
import static dev.jstech.computers.client.InstallerScreenTexts.FREE;
import static dev.jstech.computers.client.InstallerScreenTexts.GENERATION;
import static dev.jstech.computers.client.InstallerScreenTexts.HOLDS_FREE;
import static dev.jstech.computers.client.InstallerScreenTexts.HUB_FOOT;
import static dev.jstech.computers.client.InstallerScreenTexts.INSTALLATION_MEDIUM;
import static dev.jstech.computers.client.InstallerScreenTexts.INSTALLED;
import static dev.jstech.computers.client.InstallerScreenTexts.INSTALLING_ON;
import static dev.jstech.computers.client.InstallerScreenTexts.MEMORY;
import static dev.jstech.computers.client.InstallerScreenTexts.MIRROR_FIRST;
import static dev.jstech.computers.client.InstallerScreenTexts.MIRROR_SECOND;
import static dev.jstech.computers.client.InstallerScreenTexts.NAME_HELP_FIRST;
import static dev.jstech.computers.client.InstallerScreenTexts.NAME_HELP_SECOND;
import static dev.jstech.computers.client.InstallerScreenTexts.NEEDS;
import static dev.jstech.computers.client.InstallerScreenTexts.NEEDS_ON_DISK;
import static dev.jstech.computers.client.InstallerScreenTexts.NOTHING;
import static dev.jstech.computers.client.InstallerScreenTexts.NOT_SET;
import static dev.jstech.computers.client.InstallerScreenTexts.NO_DISK_SELECTED;
import static dev.jstech.computers.client.InstallerScreenTexts.NO_DISK_WITH_ROOM;
import static dev.jstech.computers.client.InstallerScreenTexts.NO_MIRROR;
import static dev.jstech.computers.client.InstallerScreenTexts.NO_ROOM_ANYWHERE;
import static dev.jstech.computers.client.InstallerScreenTexts.NO_ROOM_HERE;
import static dev.jstech.computers.client.InstallerScreenTexts.NO_SYSTEM;
import static dev.jstech.computers.client.InstallerScreenTexts.OK;
import static dev.jstech.computers.client.InstallerScreenTexts.PREPARES;
import static dev.jstech.computers.client.InstallerScreenTexts.PROCESSOR;
import static dev.jstech.computers.client.InstallerScreenTexts.READING;
import static dev.jstech.computers.client.InstallerScreenTexts.READING_MEDIUM;
import static dev.jstech.computers.client.InstallerScreenTexts.RESTARTS_WHEN_FINISHED;
import static dev.jstech.computers.client.InstallerScreenTexts.RESTART_TO_START;
import static dev.jstech.computers.client.InstallerScreenTexts.SECONDS_LEFT;
import static dev.jstech.computers.client.InstallerScreenTexts.SECONDS_LEFT_MEDIUM;
import static dev.jstech.computers.client.InstallerScreenTexts.SECONDS_LEFT_SENTENCE;
import static dev.jstech.computers.client.InstallerScreenTexts.TAKE_OUT_FIRST;
import static dev.jstech.computers.client.InstallerScreenTexts.TAKE_OUT_SECOND;
import static dev.jstech.computers.client.InstallerScreenTexts.TERMINAL_ONLY_ANSWER;
import static dev.jstech.computers.client.InstallerScreenTexts.TERMINAL_ONLY_CHOICE;
import static dev.jstech.computers.client.InstallerScreenTexts.THE_DRIVE;
import static dev.jstech.computers.client.InstallerScreenTexts.THE_INSTALLATION_MEDIUM;
import static dev.jstech.computers.client.InstallerScreenTexts.TO_QUIT;
import static dev.jstech.computers.client.InstallerScreenTexts.TO_SET_UP;
import static dev.jstech.computers.client.InstallerScreenTexts.WILL_BE_DELETED;

import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.InstallerActionPayload;
import dev.jstech.computers.operation.payload.OpenInstallerPayload;
import dev.jstech.computers.operation.payload.RequestFirmwareStatePayload;
import dev.jstech.computers.os.install.InstallerChrome;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
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
public final class InstallerScreen extends AbstractComputerScreen<MonitorSessionMenu> {

    private static final int W = MonitorGlass.WIDTH;
    private static final int H = MonitorGlass.HEIGHT;

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
    /** What the machine is made of, which one of these installers checks before it will start. */
    @Nullable
    private FirmwareStatePayload state;
    private int ticksDone;
    /** Which row of the page's list is under the cursor. */
    private int selection;
    /** The disk an erase has been offered for, kept here because the offer is the screen's and not the machine's. */
    private int erasePrompt = InstallerFlow.NO_DISK;
    /** The name being typed, which only reaches the machine when the page is left. */
    private String typed = "";
    /** Ticks since this screen opened, which is what turns the caret on and off. */
    private int blink;
    /** The button the mouse is down on, which is drawn pressed until it is let go of. */
    private InstallerFrames.Held held = InstallerFrames.Held.NONE;

    private int listTop;
    private int listLeft;
    private int listWidth;
    private int listRows;
    /** How tall a row of the list drawn last was, so a click lands on the one the player is looking at. */
    private int listRowHeight = ROW;
    private int[] nextButton;
    private int[] backButton;
    private int[] cancelButton;
    private int[] eraseButton;

    /** The page the machine last sent, kept until the session that shows it is built. */
    @Nullable
    private static OpenInstallerPayload pending;

    /** The screen currently open, so the machine's own description of itself finds it. */
    private static InstallerScreen active;

    public InstallerScreen(final MonitorSessionMenu session, final Inventory inventory, final Component title) {
        super(session, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        this.computerPos = session.hostPos();
        this.monitorPos = session.monitorPos();
        if (pending != null) {
            this.accept(pending);
        }
    }

    /** The page the machine has reached, said before the session that shows it is opened. */
    public static void expect(final OpenInstallerPayload payload) {
        pending = payload;
    }

    /**
     * Routes the machine's own description of itself to the installer, which one of these reads out.
     *
     * <p>The same answer the self-test and the setup are sent: an installer that checks a machine before it
     * starts is asking the same questions the firmware asked a moment earlier, so it asks the same way.
     */
    public static void accept(final FirmwareStatePayload payload) {
        if (active != null && active.computerPos.equals(payload.hostPos())) {
            active.state = payload;
        }
    }

    @Override
    protected void init() {
        super.init();
        active = this;
        PacketDistributor.sendToServer(new RequestFirmwareStatePayload(this.computerPos));
    }

    @Override
    public void removed() {
        if (active == this) {
            active = null;
        }
        super.removed();
    }

    /** The machine's own generation, so the bezel is the monitor that machine would really have. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return this.getMenu().hardwareEra();
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
    protected void containerTick() {
        super.containerTick();
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
                /*
                 * Only where erasing is the thing E does. On a page with a name field E is a letter, and the
                 * one before this was reaching past it to the game's own inventory key, which closed the
                 * installer outright: a player naming a machine could not type an E without losing the page.
                 */
                if (!this.naming() && this.offerErase()) {
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
        /*
         * A page with a name field swallows everything else. The game closes a container screen on its own
         * inventory key, and that key is a letter: whichever letter a player has it bound to was the one
         * letter they could not put in a machine's name.
         */
        if (this.naming()) {
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
        /*
         * A button is pressed on the way down and acted on when it is let go, which is what a button does and
         * what lets it be drawn pushed in while it is held. Acting on the way down meant the pressed face was
         * never on the glass for a single frame: the page had already changed.
         */
        final InstallerFrames.Held pressed = this.buttonUnder(mouseX, mouseY);
        if (pressed != InstallerFrames.Held.NONE) {
            this.held = pressed;
            return true;
        }
        if (this.listRows > 0 && mouseX >= this.listLeft && mouseX < this.listLeft + this.listWidth) {
            final int row = (int) ((mouseY - this.listTop) / Math.max(1, this.listRowHeight));
            if (row >= 0 && row < this.listRows) {
                this.selection = row;
                this.chose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = this.leftPos;
        final int y = this.topPos;
        MonitorFrame.renderBody(g, x, y, W, H, screenEra(), font);

        this.listRows = 0;
        final InstallerFrames.Frame frame =
                InstallerFrames.paint(g, font, this.flow, this.ticksDone, x, y, W, H, this.held);
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

    /**
     * Letting the button go does what it says, as long as the mouse is still on it.
     *
     * <p>Sliding off a held button and letting go there cancels it, which is how every button anybody has ever
     * used behaves and the one way out of a press somebody did not mean.
     */
    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        final InstallerFrames.Held pressed = this.held;
        this.held = InstallerFrames.Held.NONE;
        if (pressed == InstallerFrames.Held.NONE) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        if (this.buttonUnder(mouseX, mouseY) != pressed) {
            return true;
        }
        switch (pressed) {
            case NEXT -> this.confirm();
            case BACK -> this.send(InstallerActionPayload.of(this.computerPos, this.monitorPos,
                    InstallerActionPayload.ACTION_BACK));
            case CANCEL -> this.quit();
            case ERASE -> this.offerErase();
            default -> {
            }
        }
        return true;
    }

    /** Which of the frame's buttons is under that point, or none. */
    private InstallerFrames.Held buttonUnder(final double mouseX, final double mouseY) {
        if (hit(this.nextButton, mouseX, mouseY)) {
            return InstallerFrames.Held.NEXT;
        }
        if (hit(this.backButton, mouseX, mouseY)) {
            return InstallerFrames.Held.BACK;
        }
        if (hit(this.cancelButton, mouseX, mouseY)) {
            return InstallerFrames.Held.CANCEL;
        }
        if (hit(this.eraseButton, mouseX, mouseY)) {
            return InstallerFrames.Held.ERASE;
        }
        return InstallerFrames.Held.NONE;
    }

    /** Remembers where a list was drawn, so a click lands on the row the player is looking at. */
    private void listAt(final int left, final int top, final int width, final int rows) {
        this.listLeft = left;
        this.listTop = top;
        this.listWidth = width;
        this.listRows = rows;
        this.listRowHeight = this.row();
    }

    /**
     * Whether this page is one of the ones drawn as plain text, which are written at the size a machine's own
     * output is written at.
     *
     * <p>The installers that ran in text ran in a terminal and fitted a screenful of it; the ones with a shape
     * of their own drew their words inside boxes they had already sized, so those stay as they are.
     */
    private boolean textMode() {
        return this.flow.chrome() == InstallerChrome.FULL_TEXT
                || this.flow.chrome() == InstallerChrome.BOXED_TEXT;
    }

    /** Writes one line of a page in whichever size that page is written at. */
    private void say(final GuiGraphics g, final String text, final int x, final int y, final int colour) {
        if (this.textMode()) {
            wall(g, text, x, y, colour);
        } else {
            g.drawString(font, text, x, y, colour, false);
        }
    }

    /** The same, ending at {@code rightEdge}. */
    private void sayRight(final GuiGraphics g, final String text, final int rightEdge, final int y,
                          final int colour) {
        this.say(g, text, rightEdge - this.width(text), y, colour);
    }

    /** How wide that line comes out in the size this page is written at. */
    private int width(final String text) {
        return this.textMode() ? wallWidth(text) : font.width(text);
    }

    /** How far apart this page's rows sit, which the mouse also has to know to find the one under it. */
    private int row() {
        return this.textMode() ? WALL_ROW + 2 : ROW;
    }

    /** A label cut to the room it has, measured in the size this page is written at. */
    private String fit(final String text, final int room) {
        return InstallerFrames.clip(font, text, this.textMode() ? TextWall.room(room) : room);
    }

    private void drawWelcome(final GuiGraphics g, final InstallerFrames.Frame f) {
        /*
         * One of them opens by checking the machine rather than by greeting it, and every line it checks is a
         * rule the game already keeps. A welcome with that installer's heading over it and no check under it
         * was the heading promising something the page never did.
         */
        if (this.flow.style() == InstallerStyle.FRAMES_95) {
            this.drawCheck(g, f);
            return;
        }
        final InstallerFrames.Paint p = f.paint();
        final InstallerFlow.Disk disk = this.flow.target();
        final int step = this.row();
        int ty = f.y();
        this.say(g, of(PREPARES.with(this.flow.systemName())), f.x(), ty, p.text());
        ty += step * 2;
        this.say(g, of(TO_SET_UP.with(this.flow.systemName())), f.x(), ty, p.text());
        ty += step;
        this.say(g, of(TO_QUIT), f.x(), ty, p.text());
        ty += step * 2;
        if (disk == null) {
            this.say(g, of(NO_ROOM_ANYWHERE), f.x(), ty, p.accent());
            return;
        }
        this.say(g, of(FOUND_DISK.with(this.flow.systemName())), f.x(), ty, p.dim());
        ty += step;
        // Cut to the page, since a drive names itself at whatever length its maker chose.
        this.say(g, this.fit(of(DISK_WIDE.with(disk.slot(), disk.label())), f.w()), f.x() + 8, ty, p.bright());
        ty += step;
        this.say(g, of(HOLDS_FREE.with(holds(disk), size(this.flow.freeOn(disk)))), f.x() + 8, ty, p.dim());
    }

    /**
     * The check one installer ran before it would start: the machine, line by line, each one ticked.
     *
     * <p>Every line is a rule the game keeps anyway, which is the point of showing them: the player watches
     * the installer satisfy itself about the generation, the processor, the memory, the room on the disk and
     * the medium in the drive, and knows what the refusal would have been about if one came.
     */
    private void drawCheck(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final FirmwareStatePayload.Machine machine = this.state == null ? null : this.state.machine();
        final InstallerFlow.Disk disk = this.flow.target();
        final String unknown = of(READING);
        final String noRoom = of(NO_DISK_WITH_ROOM);
        final String[][] rows = {
                {of(GENERATION), machine == null || machine.eraLabel().isEmpty() ? unknown : of(machine.eraLabel())},
                {of(PROCESSOR), machine == null || machine.cpuName().isEmpty() ? unknown : of(machine.cpuName())},
                {of(MEMORY), machine == null ? unknown : size(machine.ramMb())},
                {of(DISK.with(disk == null ? "-" : Integer.toString(disk.slot()))),
                        disk == null ? noRoom : of(FREE.with(size(this.flow.freeOn(disk))))},
                {of(INSTALLATION_MEDIUM), this.flow.systemName()},
        };
        final int step = this.row();
        int ty = f.y();
        for (final String[] line : rows) {
            // A row is ticked when it holds an answer: not the one still being read, not the refusal.
            final boolean good = !unknown.equals(line[1]) && !noRoom.equals(line[1]);
            final String label = line[0] + " ";
            this.say(g, label, f.x(), ty, p.text());
            /*
             * The dots between the question and the answer, which is how a check of that age tied the two
             * together across a screen that had no columns to line them up in.
             */
            final int answerAt = f.x() + f.w() - 20 - this.width(line[1]);
            this.leader(g, f.x() + this.width(label), answerAt - 3, ty, p.dim());
            this.say(g, line[1], answerAt, ty, p.text());
            if (good) {
                this.sayRight(g, of(OK), f.x() + f.w(), ty, p.accent());
            }
            ty += step;
        }
        ty += step;
        this.say(g, of(NEEDS_ON_DISK.with(this.flow.systemName(), size(this.flow.footprintMb()))),
                f.x(), ty, p.dim());
    }

    /** The row of dots between a question and its answer, drawn to fill exactly the gap between them. */
    private void leader(final GuiGraphics g, final int from, final int to, final int y, final int colour) {
        final int step = Math.max(1, this.width("."));
        final int dots = (to - from) / step;
        if (dots <= 1) {
            return;
        }
        this.say(g, ".".repeat(dots), from, y, colour);
    }

    private void drawDisks(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final boolean table = this.flow.chrome() == InstallerChrome.CARD;
        final int step = this.row();
        int ty = f.y();
        if (table) {
            g.drawString(font, of(COLUMN_DISK), f.x() + 4, ty, p.dim(), false);
            right(g, of(COLUMN_SIZE), f.x() + f.w() - SIZE_COLUMN, ty, p.dim());
            right(g, of(COLUMN_FREE), f.x() + f.w() - FREE_COLUMN, ty, p.dim());
            right(g, of(COLUMN_HOLDS), f.x() + f.w() - 4, ty, p.dim());
            g.fill(f.x(), ty + 9, f.x() + f.w(), ty + 10, 0xFFE3E5EE);
            ty += 13;
        }
        this.listAt(f.x(), ty, f.w(), this.flow.disks().size());
        for (int i = 0; i < this.flow.disks().size(); i++) {
            final InstallerFlow.Disk disk = this.flow.disks().get(i);
            final boolean here = i == this.selection;
            if (here) {
                g.fill(f.x(), ty - 1, f.x() + f.w(), ty + step - 1, p.select());
                if (table) {
                    g.fill(f.x(), ty - 1, f.x() + 2, ty + step - 1, p.accent());
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
                g.drawString(font, InstallerFrames.clip(font, of(DISK_NARROW.with(disk.slot(), disk.label())),
                        sizeLeft - COLUMN_GAP - (f.x() + 4)), f.x() + 4, ty, row, false);
                right(g, sizeText, f.x() + f.w() - SIZE_COLUMN, ty, faint);
                right(g, size(this.flow.freeOn(disk)), f.x() + f.w() - FREE_COLUMN, ty, faint);
                right(g, disk.hasSystem() ? disk.holds() : of(NOTHING), f.x() + f.w() - 4, ty, faint);
            } else {
                final String state = of(HOLDS_FREE.with(holds(disk), size(this.flow.freeOn(disk))));
                this.say(g, this.fit(of(DISK_WIDE.with(disk.slot(), disk.label())),
                        f.w() - this.width(state) - COLUMN_GAP), f.x(), ty, row);
                this.sayRight(g, state, f.x() + f.w(), ty, faint);
            }
            ty += step;
        }
        ty += step;
        this.say(g, of(NEEDS.with(this.flow.systemName(), size(this.flow.footprintMb()))), f.x(), ty, p.dim());
        final InstallerFlow.Disk disk = this.chosenDisk();
        if (disk != null && !this.flow.roomOn(disk)) {
            this.say(g, of(NO_ROOM_HERE), f.x(), ty + step, p.accent());
        } else if (disk != null && disk.hasSystem()) {
            this.say(g, of(ERASED_FIRST.with(disk.holds())), f.x(), ty + step, p.accent());
        }
        if (this.flow.page() == InstallerPage.SETTINGS) {
            final String label = of(COMPUTER_NAME) + "  ";
            final int room = f.w() - this.width(label);
            this.say(g, label + this.tailThatFits(room) + this.caret(), f.x(), ty + step * 2, p.bright());
        }
    }

    private void drawName(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final int step = this.row();
        int ty = f.y();
        this.say(g, of(NAME_HELP_FIRST), f.x(), ty, p.dim());
        this.say(g, of(NAME_HELP_SECOND), f.x(), ty + step, p.dim());
        ty += step * 3;
        this.say(g, of(COMPUTER_NAME), f.x(), ty, p.text());
        final int fx = f.x();
        final int fy = ty + step + 2;
        final int fw = Math.min(150, f.w());
        g.fill(fx, fy, fx + fw, fy + 13, 0xFFFFFFFF);
        g.fill(fx, fy + 12, fx + fw, fy + 13, p.accent());
        this.say(g, this.tailThatFits(fw - 6) + this.caret(), fx + 3, fy + 3, 0xFF202434);
    }

    /**
     * The end of what has been typed, as much of it as {@code room} pixels hold.
     *
     * <p>The end rather than the beginning, because the end is where the typing is happening: a name longer
     * than the field scrolls under the caret the way a text field does, instead of running out past the edge
     * of the box and over whatever is drawn beside it.
     */
    private String tailThatFits(final int room) {
        final int left = Math.max(0, room - this.width("_"));
        String tail = this.typed;
        while (!tail.isEmpty() && this.width(tail) > left) {
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
        final int step = this.row();
        int ty = f.y();
        if (this.flow.mirrorAnswers()) {
            this.say(g, of(MIRROR_FIRST.with(this.flow.mirrorHost())), f.x(), ty, p.dim());
            this.say(g, of(MIRROR_SECOND.with(this.flow.systemName())), f.x(), ty + step, p.dim());
        } else {
            this.say(g, of(NO_MIRROR), f.x(), ty, p.dim());
        }
        ty += step * 2 + 4;
        this.listAt(f.x(), ty, f.w(), this.flow.desktops().size() + 1);
        for (int i = 0; i <= this.flow.desktops().size(); i++) {
            final boolean here = i == this.selection;
            if (here) {
                g.fill(f.x(), ty - 1, f.x() + f.w(), ty + step - 1, p.select());
            }
            final int row = here ? p.selectText() : p.text();
            /*
             * The mark of what is chosen, written out rather than drawn: these installers ran in text, and a
             * filled bracket was the whole of what a chosen option looked like there.
             */
            final String mark = here ? "(X) " : "( ) ";
            if (i == 0) {
                this.say(g, mark + of(TERMINAL_ONLY_CHOICE), f.x() + 2, ty, row);
            } else {
                final InstallerFlow.Desktop desktop = this.flow.desktops().get(i - 1);
                this.say(g, mark + desktop.name(), f.x() + 2, ty, row);
                this.sayRight(g, size(desktop.sizeMb()), f.x() + f.w(), ty, here ? p.selectText() : p.dim());
            }
            ty += step;
        }
    }

    private void drawHub(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final int step = this.row();
        int ty = f.y();
        final List<Integer> pages = this.hubPages();
        this.listAt(f.x(), ty, f.w(), 0);
        for (int i = 0; i < pages.size(); i++) {
            final InstallerPage page = this.flow.style().stages().get(pages.get(i)).page();
            final boolean wanted = this.flow.wants(page);
            final boolean here = i == this.selection;
            if (here) {
                g.fill(f.x(), ty - 1, f.x() + f.w(), ty + step * 2 - 1, p.select());
            }
            this.say(g, (i + 1) + ") " + (wanted ? "[!]" : "[x]") + " "
                            + GameText.resolve(this.flow.style().heading(page, this.flow.systemName())),
                    f.x() + 2, ty, here ? p.selectText() : wanted ? p.accent() : p.text());
            this.say(g, this.answerFor(page), f.x() + 22, ty + step, here ? p.selectText() : p.dim());
            ty += step * 2 + 2;
        }
        this.say(g, of(HUB_FOOT), f.x(), ty + 4, p.dim());
    }

    /** What each question on the list has been answered with so far, in a few words. */
    private String answerFor(final InstallerPage page) {
        return switch (page) {
            case DISK, SETTINGS -> of(this.flow.target() == null ? NO_DISK_SELECTED.text()
                    : DISK_ANSWER.with(this.flow.target().slot(), this.flow.target().label()));
            case NAME -> of(this.flow.computerName().isBlank() ? NOT_SET.text()
                    : ANSWER.with(this.flow.computerName()));
            case DESKTOP -> of(this.flow.desktop() == null ? TERMINAL_ONLY_ANSWER.text()
                    : ANSWER.with(this.flow.desktop().name()));
            default -> "";
        };
    }

    /**
     * The copy, in the shape the installer doing it reported one.
     *
     * <p>These never looked alike. One put a single bar in a box and named the drive it was reading; another
     * listed every step and dotted its way across to a "done"; another had one gauge and a percentage on it;
     * the graphical ones kept their list down the side and said only what they were doing now. Drawing one
     * list and one bar for all of them made the most-watched minute of every installation the same minute.
     */
    private void drawWork(final GuiGraphics g, final InstallerFrames.Frame f) {
        switch (this.flow.style()) {
            case FRAMES_XP -> {
                if (this.flow.chrome() == InstallerChrome.SIDE_PANEL) {
                    this.drawWorkBeside(g, f);
                } else {
                    this.drawWorkBar(g, f);
                }
            }
            case MC_DOS, FRAMES_95, DEBIAN -> this.drawWorkBar(g, f);
            case FRAMES_11 -> this.drawWorkSteps(g, f);
            default -> this.drawWorkLog(g, f);
        }
    }

    /**
     * One bar and the drive being read: what the installers that reported a single figure showed.
     *
     * <p>A player watching one of these learns two things, which is what those screens gave them: how far it
     * has got, and that the medium is still being read, so taking it out now would stop the whole thing.
     */
    private void drawWorkBar(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final int step = this.row();
        final InstallerFlow.Disk disk = this.flow.target();
        final int percent = this.flow.permille(this.ticksDone) / 10;
        int ty = f.y();
        if (disk != null) {
            this.say(g, this.fit(of(COPYING.with(this.flow.systemName(), disk.slot(), disk.label())), f.w()),
                    f.x(), ty, p.text());
            ty += step * 2;
        }
        final InstallerFlow.Step running = this.flow.steps().get(this.flow.stepAt(this.ticksDone));
        this.say(g, GameText.resolve(running.label()), f.x(), ty, p.bright());
        ty += step + 4;
        /* The bar itself, sunk into the page the way those installers drew one. */
        final int barH = 9;
        g.fill(f.x() - 1, ty - 1, f.x() + f.w() + 1, ty + barH + 1, p.dim());
        g.fill(f.x(), ty, f.x() + f.w(), ty + barH, 0xFF000000);
        g.fill(f.x(), ty, f.x() + f.w() * this.flow.permille(this.ticksDone) / 1000, ty + barH, p.accent());
        ty += barH + 6;
        this.say(g, of(COMPLETE.with(percent)), f.x(), ty, p.text());
        this.sayRight(g, of(SECONDS_LEFT.with(this.secondsLeft())), f.x() + f.w(), ty, p.dim());
        ty += step;
        this.say(g, of(READING_MEDIUM.with(this.mediumName())), f.x(), ty, p.dim());
    }

    /**
     * Every step with dots running out to what came of it: how the installers that ran in a terminal reported.
     */
    private void drawWorkLog(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final int step = this.row();
        final int running = this.flow.stepAt(this.ticksDone);
        int ty = f.y();
        for (int i = 0; i < this.flow.steps().size(); i++) {
            final InstallerFlow.Step line = this.flow.steps().get(i);
            final boolean done = i < running;
            final String answer = done ? of(DONE) : i == running
                    ? this.flow.stepPermille(this.ticksDone) / 10 + "%" : "";
            final String label = this.fit(GameText.resolve(line.label()), f.w() - 60);
            this.say(g, label, f.x(), ty, done || i == running ? p.text() : p.dim());
            if (!answer.isEmpty()) {
                final int answerAt = f.x() + f.w() - this.width(answer);
                this.leader(g, f.x() + this.width(label) + 3, answerAt - 3, ty, p.dim());
                this.say(g, answer, answerAt, ty, done ? p.dim() : p.accent());
            }
            ty += step;
        }
        ty += step;
        this.say(g, of(SECONDS_LEFT_MEDIUM.with(this.secondsLeft())), f.x(), ty, p.dim());
    }

    /** The steps with a figure against each and one bar under them, as the newest installer shows them. */
    private void drawWorkSteps(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final int running = this.flow.stepAt(this.ticksDone);
        int ty = f.y();
        for (int i = 0; i < this.flow.steps().size(); i++) {
            final InstallerFlow.Step line = this.flow.steps().get(i);
            final boolean done = i < running;
            g.drawString(font, InstallerFrames.clip(font, GameText.resolve(line.label()), f.w() - 40), f.x(), ty,
                    done || i == running ? p.text() : p.dim(), false);
            if (done) {
                right(g, "100%", f.x() + f.w(), ty, p.dim());
            } else if (i == running) {
                right(g, this.flow.stepPermille(this.ticksDone) / 10 + "%", f.x() + f.w(), ty, p.accent());
            }
            ty += ROW;
        }
        ty += 8;
        g.fill(f.x(), ty, f.x() + f.w(), ty + 6, 0xFFE3E5EE);
        g.fill(f.x(), ty, f.x() + f.w() * this.flow.permille(this.ticksDone) / 1000, ty + 6, p.accent());
        /*
         * Two lines rather than one that runs off the card. The sentence is long, the card is not wide, and a
         * time that grows a digit made it longer still.
         */
        g.drawString(font, of(RESTARTS_WHEN_FINISHED), f.x(), ty + 12, p.dim(), false);
        g.drawString(font, of(SECONDS_LEFT_SENTENCE.with(this.secondsLeft())), f.x(), ty + 22, p.dim(), false);
    }

    /** The graphical phase, whose frame has already listed the steps down its own side. */
    private void drawWorkBeside(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final InstallerFlow.Step step = this.flow.steps().get(this.flow.stepAt(this.ticksDone));
        g.drawString(font, InstallerFrames.clip(font, GameText.resolve(step.label()), f.w()), f.x(), f.y(), p.text(),
                false);
        final InstallerFlow.Disk disk = this.flow.target();
        if (disk != null) {
            /*
             * Cut to the panel it is written in. A drive names itself at whatever length its maker chose,
             * and this line was drawn at full length whatever the room: on a long name it ran out past the
             * edge of the glass and off the monitor.
             */
            g.drawString(font, InstallerFrames.clip(font, of(INSTALLING_ON.with(disk.slot(), disk.label())), f.w()),
                    f.x(), f.y() + 12, p.dim(), false);
        }
    }

    /** How long the whole installation still has, in the seconds every one of these counts in. */
    private int secondsLeft() {
        return Math.max(0, (this.flow.ticksTotal() - this.ticksDone) / 20);
    }

    /**
     * The drive the system is being read from, by the name the firmware gave it, or the plain words for it.
     *
     * <p>Asked of the machine rather than assumed, because which drive it is is exactly what those installers
     * were telling the player: the one they must not open until this is over.
     */
    private String mediumName() {
        if (this.state != null) {
            for (final FirmwareStatePayload.Entry entry : this.state.entries()) {
                if (entry.kind() == FirmwareStatePayload.KIND_MEDIA && !entry.osId().isEmpty()) {
                    return of(THE_DRIVE.with(entry.device()));
                }
            }
        }
        return of(THE_INSTALLATION_MEDIUM);
    }

    private void drawDone(final GuiGraphics g, final InstallerFrames.Frame f) {
        final InstallerFrames.Paint p = f.paint();
        final int step = this.row();
        int ty = f.y();
        this.say(g, of(INSTALLED.with(this.flow.systemName())), f.x(), ty, p.bright());
        ty += step * 2;
        this.say(g, of(TAKE_OUT_FIRST), f.x(), ty, p.text());
        this.say(g, of(TAKE_OUT_SECOND), f.x(), ty + step, p.text());
        this.say(g, of(RESTART_TO_START.with(this.flow.systemName())), f.x(), ty + step * 3, p.accent());
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
        g.drawString(font, of(ERASE_ASK.with(disk.slot())), bx + 10, by + 10, 0xFF202434, false);
        g.drawString(font, of(disk.hasSystem() ? AND_EVERY_FILE.with(disk.holds()) : EVERY_FILE.text()),
                bx + 10, by + 26, 0xFF202434, false);
        g.drawString(font, of(WILL_BE_DELETED.with(InstallerFrames.clip(font, disk.label(), bw - 20))),
                bx + 10, by + 36, 0xFF202434, false);
        g.drawString(font, of(CANNOT_UNDO), bx + 10, by + 46, 0xFF6B7488, false);
        g.drawString(font, of(ERASE_KEYS), bx + 10, by + 60, 0xFFC42B1C, false);
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
        return disk.hasSystem() ? disk.holds() : of(NO_SYSTEM);
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
