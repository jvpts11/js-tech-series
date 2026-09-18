/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.FirmwareActionPayload;
import dev.jstech.computers.os.boot.BootMenu;
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

    private static final int W = 340;
    private static final int H = 214;

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
            PacketDistributor.sendToServer(new FirmwareActionPayload(this.computerPos, this.monitorPos,
                    FirmwareActionPayload.ACTION_HOLD_BOOT_MENU, 0L, -1));
        }
        if (this.menu.entries().isEmpty()) {
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
        g.fill(x, y, x + W, y + H, 0xFF000000);

        g.drawString(font, this.menu.title(), x + 12, y + 12, 0xFFE6ECF6, false);
        int ty = y + 34;
        for (int i = 0; i < this.menu.entries().size(); i++) {
            final BootMenu.Entry entry = this.menu.entries().get(i);
            final boolean on = i == this.at;
            if (on) {
                g.fill(x + 10, ty - 2, x + W - 10, ty + 9, 0xFF7D8A9C);
            }
            g.drawString(font, entry.label(), x + 14, ty, on ? 0xFF000000 : 0xFFE6ECF6, false);
            ty += 12;
        }
        g.drawString(font, "Use the Up and Down keys to select which entry is highlighted.",
                x + 12, y + H - 34, 0xFF7D8A9C, false);
        final String below = this.held
                ? "Press Enter to boot the selected system."
                : "The highlighted entry will be booted automatically in "
                        + this.menu.secondsLeft(this.remaining) + "s.";
        g.drawString(font, below, x + 12, y + H - 22, 0xFF7D8A9C, false);
    }

    /**
     * Boots what is highlighted, or opens the firmware when that is what is highlighted.
     *
     * <p>The entry is named by where it sits in the list rather than by the disk it is on, because a disk
     * carries several systems now and two entries of this list can share one. The machine builds the same list
     * from the same disks, so the place in it is the same on both sides.
     */
    private void choose() {
        final BootMenu.Entry entry = this.menu.entries().get(this.at);
        PacketDistributor.sendToServer(new FirmwareActionPayload(this.computerPos, this.monitorPos,
                entry.isFirmware() ? FirmwareActionPayload.ACTION_OPEN_SETUP
                        : FirmwareActionPayload.ACTION_BOOT_ONCE,
                entry.isFirmware() ? 0L : this.at, -1));
    }

}
