/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.operation.payload.FirmwareActionPayload;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The boot manager's menu: which system this machine will start, chosen before it starts one.
 *
 * <p>The machine is standing at the menu, not this screen: the count runs on the server, so walking away lets the
 * machine go on by itself exactly as it would if nobody had opened a monitor at all. A key stops that count, and
 * then the machine waits there for as long as it takes.
 */
public final class BootMenuScreen extends Screen {

    private static final int W = 340;
    private static final int H = 214;

    private final BlockPos computerPos;
    private final BlockPos monitorPos;
    private final BootMenu menu;

    private int remaining;
    private int at;
    /** Whether a key has already told the machine to stop counting. */
    private boolean held;

    public BootMenuScreen(final BlockPos computerPos, final BlockPos monitorPos, final BootMenu menu,
                          final int remaining) {
        super(Component.literal("Boot Manager"));
        this.computerPos = computerPos;
        this.monitorPos = monitorPos;
        this.menu = menu;
        this.remaining = Math.max(0, remaining);
        this.at = menu.defaultIndex();
        this.held = remaining <= 0;
    }

    @Override
    public void tick() {
        super.tick();
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
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        /*
         * Screen.render paints the dimmed backdrop itself; painting it again after our own drawing would wash the
         * menu out, so super runs FIRST and the content after.
         */
        super.render(g, mouseX, mouseY, partialTick);
        final int x = (width - W) / 2;
        final int y = (height - H) / 2;
        MonitorFrame.renderBody(g, x, y, W, H, era(), font);
        g.fill(x, y, x + W, y + H, 0xFF000000);

        g.drawString(font, "GNU GRUB  version 2.12", x + 12, y + 12, 0xFFE6ECF6, false);
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

    /** Boots what is highlighted, or opens the firmware when that is what is highlighted. */
    private void choose() {
        final BootMenu.Entry entry = this.menu.entries().get(this.at);
        PacketDistributor.sendToServer(new FirmwareActionPayload(this.computerPos, this.monitorPos,
                entry.isFirmware() ? FirmwareActionPayload.ACTION_OPEN_SETUP
                        : FirmwareActionPayload.ACTION_BOOT_ONCE,
                entry.isFirmware() ? 0L : entry.slot(), -1));
    }

    /** The host computer's era, so the monitor bezel matches the machine standing at the menu. */
    private HardwareEra era() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(this.computerPos)
                instanceof AbstractComputerBlockEntity computer && computer.displayEra() != null) {
            return computer.displayEra();
        }
        return HardwareEra.STANDARD;
    }
}
