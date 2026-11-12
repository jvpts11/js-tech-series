/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.disk.DiskCommand;
import dev.jstech.computers.os.disk.DiskReport;
import dev.jstech.computers.os.disk.PartitionScheme;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The classic Quince systems' Special menu at work on the disks (computers/docs/design/quince.md): Erase Disk lists the
 * volumes the system reads, the one it starts from shown and never chosen, and asks before it erases the one picked,
 * formatting it again in its own file system; Set Startup lists the volumes with a system on them and has the machine
 * start from the one picked the next time it starts. Both read the machine's disks as its disk tools do, and are drawn
 * in the face's own dialog, list and buttons.
 */
@TextHolder
final class QuinceDisksPopup extends Popup {

    private final QuinceLook look;
    private final DiskToolClient disks;
    private final boolean erasing;
    private final List<Volume> volumes;
    /** The row picked, or -1 when none can be. */
    private int picked;
    /** The first row in view, when there are more than the list shows. */
    private int top;
    /** The volume asked about before it is erased, once Erase... is pressed; null while the list is up. */
    @Nullable
    private Volume asking;

    static final TextKey ERASE_WHICH = TextKey.of("jsc.quince.disks.erase_which", "Erase which disk?");
    static final TextKey START_FROM = TextKey.of("jsc.quince.disks.start_from", "Start up from:");
    static final TextKey STARTUP_DISK = TextKey.of("jsc.quince.disks.startup_disk", "startup disk");
    static final TextKey UNTITLED = TextKey.of("jsc.quince.disks.untitled", "untitled");
    static final TextKey NEXT_RESTART = TextKey.of("jsc.quince.disks.next_restart",
            "Takes effect the next time you restart.");
    static final TextKey CANCEL = TextKey.of("jsc.quince.disks.cancel", "Cancel");
    static final TextKey ERASE_ASK = TextKey.of("jsc.quince.disks.erase_ask", "Erase...");
    static final TextKey ERASE = TextKey.of("jsc.quince.disks.erase", "Erase");
    static final TextKey OK = TextKey.of("jsc.quince.disks.ok", "OK");
    static final TextKey COMPLETELY = TextKey.of("jsc.quince.disks.completely",
            "Completely erase disk named \"%s\"? Everything on it will be lost.");

    /* The dialogs as the mock lays them on the glass: their sizes, and where the list and the buttons stand. */
    private static final int WIDTH = 272;
    private static final int ASK_WIDTH = 244;
    private static final int ASK_HEIGHT = 80;
    private static final int INSET = 14;
    private static final int HEADING_Y = 12;
    private static final int LIST_TOP = 28;
    private static final int ROW_H = 20;
    private static final int MOST_ROWS = 6;
    private static final int ICON_X = 4;
    private static final int ICON_Y = 4;
    private static final int NAME_X = 24;
    private static final int TEXT_Y = 6;
    private static final int UNDER_LIST = 26;
    private static final int UNDER_NOTE = 30;
    private static final int NOTE_UP = 42;
    private static final int BUTTONS_UP = 24;
    private static final int BUTTON_W = 56;
    private static final int ACTION_RIGHT = 70;
    private static final int CANCEL_RIGHT = 138;
    private static final int ASK_BUTTON_W = 50;
    private static final int ASK_ACTION_RIGHT = 66;
    private static final int ASK_CANCEL_RIGHT = 128;
    private static final int ASK_TEXT_X = 42;
    private static final int ASK_TEXT_W = 180;
    private static final int LINE_H = 11;
    private static final int CLASSIC_BUTTON_H = 13;
    private static final int PLATINUM_BUTTON_H = 14;

    private QuinceDisksPopup(final QuinceLook look, final DiskToolClient disks, final boolean erasing,
                             final List<Volume> volumes) {
        super("", WIDTH, 0);
        this.look = look;
        this.disks = disks;
        this.erasing = erasing;
        this.volumes = List.copyOf(volumes);
        this.picked = firstPicked();
        setPreferredSize(WIDTH, LIST_TOP + shown() * ROW_H + 2 + (erasing ? UNDER_LIST : UNDER_NOTE) + BUTTONS_UP);
        // A classic dialog stands over the desktop as it is, nothing dimmed behind it.
        setDim(0);
        setCloseOnOutsideClick(false);
        open();
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        final Font font = ctx.font();
        final QuinceLook.Colours c = this.look.colours();
        QuinceArt.dialog(g, this.look, x(), y(), right(), bottom());
        if (this.asking != null) {
            renderAsk(g, font, c);
            return;
        }
        Draw.text(g, font, GameText.resolve(this.erasing ? ERASE_WHICH : START_FROM), x() + INSET, y() + HEADING_Y,
                c.ink());
        final int lx = x() + INSET;
        final int ly = y() + LIST_TOP;
        final int lw = width() - 2 * INSET;
        g.fill(lx, ly, lx + lw, ly + shown() * ROW_H + 2, c.light());
        ChromeShapes.outline(g, lx, ly, lw, shown() * ROW_H + 2, c.shade());
        for (int k = 0; k < shown(); k++) {
            final int index = this.top + k;
            final Volume volume = this.volumes.get(index);
            final int ry = ly + 1 + k * ROW_H;
            final boolean selected = index == this.picked;
            if (selected) {
                g.fill(lx + 1, ry, lx + lw - 1, ry + ROW_H, c.select());
            }
            final int ink = selected ? c.selectInk() : volume.choosable() ? c.ink() : c.dim();
            QuinceArt.sprite(g, QuinceArt.HD, lx + ICON_X, ry + ICON_Y - 2, ink);
            Draw.text(g, font, volume.name(), lx + NAME_X, ry + TEXT_Y, ink);
            Draw.text(g, font, volume.detail(), lx + lw - 6 - font.width(volume.detail()), ry + TEXT_Y, ink);
        }
        if (!this.erasing) {
            Draw.text(g, font, GameText.resolve(NEXT_RESTART), x() + INSET, bottom() - NOTE_UP, c.dim());
        }
        button(g, font, right() - CANCEL_RIGHT, BUTTON_W, GameText.resolve(CANCEL), false, true);
        button(g, font, right() - ACTION_RIGHT, BUTTON_W, GameText.resolve(this.erasing ? ERASE_ASK : OK), true,
                this.picked >= 0);
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (width() == 0 || height() == 0) {
            return true;
        }
        if (this.asking != null) {
            if (onButton(mx, my, right() - ASK_ACTION_RIGHT, ASK_BUTTON_W)) {
                erase(this.asking);
            } else if (onButton(mx, my, right() - ASK_CANCEL_RIGHT, ASK_BUTTON_W)) {
                close();
            }
            return true;
        }
        final int lx = x() + INSET;
        final int ly = y() + LIST_TOP + 1;
        if (mx >= lx && mx < right() - INSET && my >= ly && my < ly + shown() * ROW_H) {
            final int index = this.top + (int) ((my - ly) / ROW_H);
            if (index < this.volumes.size() && this.volumes.get(index).choosable()) {
                this.picked = index;
            }
        } else if (onButton(mx, my, right() - ACTION_RIGHT, BUTTON_W)) {
            act();
        } else if (onButton(mx, my, right() - CANCEL_RIGHT, BUTTON_W)) {
            close();
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.asking != null) {
                erase(this.asking);
            } else {
                act();
            }
            return true;
        }
        if (this.asking == null && (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP)) {
            move(key == GLFW.GLFW_KEY_DOWN ? 1 : -1);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    /** Erase Disk's list, over the disks the machine last reported, of the system of {@code era}. */
    static QuinceDisksPopup erase(final QuinceLook look, final DiskToolClient disks, final Era era) {
        return new QuinceDisksPopup(look, disks, true, toErase(disks.report(), era));
    }

    /** Set Startup's list, over the disks the machine last reported. */
    static QuinceDisksPopup startup(final QuinceLook look, final DiskToolClient disks) {
        return new QuinceDisksPopup(look, disks, false, toStartFrom(disks.report()));
    }

    /** Whether the machine has a volume to erase: one the system reads, other than the one it starts from. */
    static boolean anyToErase(final DiskReport report, final Era era) {
        for (final Volume volume : toErase(report, era)) {
            if (volume.choosable()) {
                return true;
            }
        }
        return false;
    }

    /** Whether the machine has a volume with a system on it to start from. */
    static boolean anyToStartFrom(final DiskReport report) {
        return !toStartFrom(report).isEmpty();
    }

    /* The alert that asks before erasing: the caution sign, the question, Cancel, and Erase as the default. */
    private void renderAsk(final GuiGraphics g, final Font font, final QuinceLook.Colours c) {
        final boolean platinum = this.look == QuinceLook.PLATINUM;
        QuinceArt.sprite(g, platinum ? QuinceArt.CAUTION_YELLOW : QuinceArt.CAUTION, x() + INSET, y() + INSET,
                c.ink());
        final Component question = Component.literal(GameText.resolve(COMPLETELY.with(this.asking.name())));
        int ty = y() + INSET;
        for (final FormattedCharSequence line : font.split(question, ASK_TEXT_W)) {
            Draw.text(g, font, line, x() + ASK_TEXT_X, ty, c.ink());
            ty += LINE_H;
        }
        button(g, font, right() - ASK_CANCEL_RIGHT, ASK_BUTTON_W, GameText.resolve(CANCEL), false, true);
        button(g, font, right() - ASK_ACTION_RIGHT, ASK_BUTTON_W, GameText.resolve(ERASE), true, true);
    }

    /* A push button {@code w} wide whose left edge is at {@code bx}, along the foot of the dialog. */
    private void button(final GuiGraphics g, final Font font, final int bx, final int w, final String label,
                        final boolean primary, final boolean enabled) {
        final int by = bottom() - BUTTONS_UP;
        QuinceArt.button(g, this.look, bx, by, w, buttonHeight(), primary, false);
        if (enabled) {
            Draw.textCentered(g, font, label, bx + w / 2, by + 3, QuinceArt.buttonInk(this.look, primary));
        } else {
            final QuinceLook.Colours c = this.look.colours();
            QuinceArt.dimmed(g, font, label, bx + (w - font.width(label)) / 2, by + 3, c.ink(), c.button());
        }
    }

    private boolean onButton(final double mx, final double my, final int bx, final int w) {
        final int by = bottom() - BUTTONS_UP;
        return mx >= bx && mx < bx + w && my >= by && my < by + buttonHeight();
    }

    private int buttonHeight() {
        return this.look == QuinceLook.PLATINUM ? PLATINUM_BUTTON_H : CLASSIC_BUTTON_H;
    }

    /* The default button: Erase... asks first; OK has the machine start from the volume picked. */
    private void act() {
        if (this.picked < 0) {
            return;
        }
        final Volume volume = this.volumes.get(this.picked);
        if (this.erasing) {
            this.asking = volume;
            setPreferredSize(ASK_WIDTH, ASK_HEIGHT);
        } else {
            this.disks.change(DiskCommand.startup(volume.disk(), volume.number()));
            close();
        }
    }

    /* Erases the volume asked about, formatting it again in the file system it has. */
    private void erase(final Volume volume) {
        this.disks.change(DiskCommand.format(volume.disk(), volume.number(), volume.fs()));
        close();
    }

    /* Picks the next or the previous row that can be picked, keeping it in view. */
    private void move(final int step) {
        int index = this.picked;
        do {
            index += step;
        } while (index >= 0 && index < this.volumes.size() && !this.volumes.get(index).choosable());
        if (index < 0 || index >= this.volumes.size()) {
            return;
        }
        this.picked = index;
        if (index < this.top) {
            this.top = index;
        } else if (index >= this.top + MOST_ROWS) {
            this.top = index - MOST_ROWS + 1;
        }
    }

    /* The row picked first: on Set Startup the volume the machine starts from now, else the first that can be. */
    private int firstPicked() {
        for (int i = 0; i < this.volumes.size(); i++) {
            if (!this.erasing && this.volumes.get(i).startup()) {
                return i;
            }
        }
        for (int i = 0; i < this.volumes.size(); i++) {
            if (this.volumes.get(i).choosable()) {
                return i;
            }
        }
        return -1;
    }

    private int shown() {
        return Math.min(MOST_ROWS, this.volumes.size());
    }

    /*
     * The volumes the system reads, each with its size and file system: the one it starts from is listed and never
     * chosen, as is one its era cannot format again.
     */
    private static List<Volume> toErase(final DiskReport report, final Era era) {
        final List<Volume> out = new ArrayList<>();
        for (final DiskReport.Disk disk : report.disks()) {
            for (final DiskReport.Part part : disk.parts()) {
                final FileSystemType fs = part.fileSystem();
                if (fs == null || !fs.readBy(Platform.QUINCE, era)) {
                    continue;
                }
                final boolean startup = part.mounted();
                final String detail = startup ? GameText.resolve(STARTUP_DISK)
                        : size(part.sizeMb()) + "  " + fs.displayName();
                out.add(new Volume(disk.index(), part.number(), nameOf(part), part.fs(), detail,
                        !startup && fs.formattableIn(era), startup));
            }
        }
        return out;
    }

    /*
     * The volumes with a system on them, each with the system's name. On a disk the running system starts from,
     * only its own volume is listed unless the disk has a Master Boot Record, whose active partition chooses; any
     * volume of another disk is.
     */
    private static List<Volume> toStartFrom(final DiskReport report) {
        final List<Volume> out = new ArrayList<>();
        for (final DiskReport.Disk disk : report.disks()) {
            final boolean running = disk.parts().stream().anyMatch(part -> part.mounted() && !part.swap());
            for (final DiskReport.Part part : disk.parts()) {
                if (part.system().isEmpty()) {
                    continue;
                }
                final boolean startup = part.mounted();
                if (running && !startup && disk.scheme() != PartitionScheme.MBR) {
                    continue;
                }
                out.add(new Volume(disk.index(), part.number(), nameOf(part), part.fs(), part.system(), true,
                        startup));
            }
        }
        return out;
    }

    private static String nameOf(final DiskReport.Part part) {
        return part.label().isEmpty() ? GameText.resolve(UNTITLED) : part.label();
    }

    private static String size(final long mb) {
        return mb >= 1024 ? String.format(Locale.ROOT, "%.1f GB", mb / 1024.0) : mb + " MB";
    }

    /**
     * One volume of the list.
     *
     * @param disk      its disk, by the index the disk tools give it
     * @param number    its partition's number on that disk
     * @param name      its label, or "untitled"
     * @param fs        its file system by its serialized name
     * @param detail    what the row says at its right: its size and file system, the system on it, or that it is
     *                  the startup disk
     * @param choosable whether it can be picked
     * @param startup   whether the machine runs from it now
     */
    record Volume(int disk, int number, String name, String fs, String detail, boolean choosable, boolean startup) {
    }
}
