/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.FirmwareActionPayload;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The boot manager's menu: which system this machine will start, chosen before it starts one.
 *
 * <p>The machine is standing at the menu, not this screen: the count runs on the server, so walking away lets the
 * machine go on by itself exactly as it would if nobody had opened a monitor at all. A key stops that count, and
 * then the machine waits there for as long as it takes.
 */
public final class BootMenuScreen extends AbstractComputerScreen<MonitorSessionMenu> {

    private static final int W = MonitorGlass.WIDTH;
    private static final int H = MonitorGlass.HEIGHT;

    /** How far the ruled box and the help under it stand from the edges of the glass. */
    private static final int MARGIN = 14;

    /** How many entries fit inside that box, and how tall it is to hold them. */
    private static final int BOX_ROWS = 8;
    private static final int BOX_H = 92;

    /** The one grey these managers drew everything in, and the ground they drew it on. */
    private static final int TEXT = 0xFFBDBDBD;
    private static final int FRAME = 0xFFBDBDBD;

    /** The list the machine last sent, kept until the session that shows it is built. */
    @Nullable
    private static Standing pending;

    private final BlockPos computerPos;
    private final BlockPos monitorPos;
    private final BootMenu menu;

    private int remaining;
    private int at;
    /** Whether a key has already told the machine to stop counting. */
    private boolean held;

    public BootMenuScreen(final MonitorSessionMenu session, final Inventory inventory, final Component title) {
        super(session, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        this.computerPos = session.hostPos();
        this.monitorPos = session.monitorPos();
        final Standing standing = pending != null ? pending : new Standing(BootMenu.NONE, 0);
        this.menu = standing.menu();
        this.remaining = Math.max(0, standing.remaining());
        this.at = this.menu.defaultIndex();
        this.held = standing.remaining() <= 0;
    }

    /** The list the machine is standing at, said before the session that shows it is opened. */
    public static void expect(final BootMenu menu, final int remaining) {
        pending = new Standing(menu, remaining);
    }

    /** One machine standing at its boot manager: what it lists, and how long before it goes on by itself. */
    private record Standing(BootMenu menu, int remaining) {
    }

    /** The machine's own generation, so the bezel is the monitor that machine would really have. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return this.getMenu().hardwareEra();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (!this.held && this.remaining > 0) {
            this.remaining--;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        /*
         * Escape leaves the monitor, the way it leaves every other screen. The machine is the one standing at
         * the boot manager, not this screen, so walking away from the keyboard changes nothing about where it
         * has got to: opening the monitor again comes back to the same list. Without this the only way out of
         * the menu was to boot something, which made looking at it a decision a player could not take back.
         */
        if (keyCode == InputConstants.KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        // Any key at all stops the count, which is what a boot manager has always done.
        if (!this.held) {
            this.held = true;
            this.remaining = 0;
            PacketDistributor.sendToServer(FirmwareActionPayload.of(this.computerPos, this.monitorPos,
                    FirmwareActionPayload.ACTION_HOLD_BOOT_MENU, 0L, -1));
        }
        if (this.menu.entries().isEmpty()) {
            return true;
        }
        if (!this.menu.manager().listsSystems()) {
            loaderKey(keyCode);
            return true;
        }
        switch (keyCode) {
            case InputConstants.KEY_UP -> this.at = (this.at - 1 + this.menu.entries().size())
                    % this.menu.entries().size();
            case InputConstants.KEY_DOWN -> this.at = (this.at + 1) % this.menu.entries().size();
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> choose();
            default -> { }
        }
        return true;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = this.leftPos;
        final int y = this.topPos;
        MonitorFrame.renderBody(g, x, y, W, H, screenEra(), font);
        if (!this.menu.manager().listsSystems()) {
            LoaderMenuPainter.draw(g, font, x, y, this.menu, this.remaining, this.held);
            return;
        }
        g.fill(x, y, x + W, y + H, 0xFF000000);

        /*
         * A boot manager of this kind drew a ruled box with the systems inside it and put its help underneath,
         * which is what tells a player at a glance that the list is the thing to act on and the rest is not.
         */
        wallCentered(g, GameText.resolve(this.menu.title()), x + W / 2, y + 12, TEXT);
        final int boxTop = y + 30;
        final int boxBottom = boxTop + BOX_H;
        rule(g, x + MARGIN, boxTop, W - 2 * MARGIN, BOX_H);
        /*
         * A machine with more systems than the box has rows walks its list through the window rather than
         * drawing entries past the rule and over the help underneath, and the window follows the cursor so
         * every entry can still be reached and booted.
         */
        final int count = this.menu.entries().size();
        final int rows = Math.min(count, BOX_ROWS);
        final int from = count <= BOX_ROWS ? 0
                : Math.max(0, Math.min(count - BOX_ROWS, this.at - BOX_ROWS / 2));
        int ty = boxTop + 5;
        for (int i = from; i < from + rows; i++) {
            final BootMenu.Entry entry = this.menu.entries().get(i);
            final boolean on = i == this.at;
            if (on) {
                g.fill(x + MARGIN + 2, ty - 1, x + W - MARGIN - 2, ty + WALL_ROW, FRAME);
            }
            /* The star marks the entry the machine boots on its own, which is not always the one highlighted. */
            final String mark = i == this.menu.defaultIndex() ? "*" : " ";
            wall(g, wallClip(mark + GameText.resolve(entry.label()), W - 2 * MARGIN - 14), x + MARGIN + 6, ty,
                    on ? 0xFF000000 : TEXT);
            ty += WALL_ROW + 2;
        }
        int hy = boxBottom + 8;
        wall(g, GameText.resolve(FirmwareScreenTexts.LOADER_HELP_FIRST), x + MARGIN, hy, TEXT);
        hy += WALL_ROW;
        wall(g, GameText.resolve(FirmwareScreenTexts.LOADER_HELP_SECOND), x + MARGIN, hy, TEXT);
        hy += WALL_ROW;
        wall(g, GameText.resolve(this.held ? FirmwareScreenTexts.LOADER_HELD.text()
                        : FirmwareScreenTexts.LOADER_COUNTDOWN.with(this.menu.secondsLeft(this.remaining))),
                x + MARGIN, hy, TEXT);
    }

    /** The single rule around the list, which is the whole of that manager's furniture. */
    private void rule(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        g.fill(x, y, x + w, y + 1, FRAME);
        g.fill(x, y + h - 1, x + w, y + h, FRAME);
        g.fill(x, y, x + 1, y + h, FRAME);
        g.fill(x + w - 1, y, x + w, y + h, FRAME);
    }

    /**
     * A key at a loader, which has no cursor to move: Enter boots what it would have booted by itself, and a
     * number chooses the entry that number is written beside. Anything else has only stopped the count.
     */
    private void loaderKey(final int keyCode) {
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            this.at = this.menu.defaultIndex();
            choose();
            return;
        }
        final int chosen = keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_9
                ? keyCode - InputConstants.KEY_1
                : keyCode >= InputConstants.KEY_NUMPAD1 && keyCode <= InputConstants.KEY_NUMPAD9
                        ? keyCode - InputConstants.KEY_NUMPAD1 : -1;
        if (chosen >= 0 && chosen < this.menu.entries().size()) {
            this.at = chosen;
            choose();
        }
    }

    /**
     * Does what the entry the cursor is on says: boots it, opens the firmware, or starts the machine over.
     *
     * <p>The entry is named by where it sits in the list rather than by the disk it is on, because a disk
     * carries several systems now and two entries of this list can share one. The machine builds the same list
     * from the same disks, so the place in it is the same on both sides.
     */
    private void choose() {
        final BootMenu.Entry entry = this.menu.entries().get(this.at);
        if (entry.isFirmware() || entry.isRestart()) {
            PacketDistributor.sendToServer(FirmwareActionPayload.of(this.computerPos, this.monitorPos,
                    entry.isFirmware() ? FirmwareActionPayload.ACTION_OPEN_SETUP
                            : FirmwareActionPayload.ACTION_RESTART_FROM_MENU, 0L, -1));
            return;
        }
        /*
         * The disk and the system, not the row. A disk carries several systems and two entries can share one,
         * so the choice has to name both; a row number only means anything to a list, and this list is not the
         * only one that offers this action.
         */
        PacketDistributor.sendToServer(new FirmwareActionPayload(this.computerPos, this.monitorPos,
                FirmwareActionPayload.ACTION_BOOT_ONCE, entry.slot(), -1, entry.osId()));
    }

}
