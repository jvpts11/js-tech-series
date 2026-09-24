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
import dev.jstech.computers.operation.payload.KvmSelectPayload;
import dev.jstech.computers.operation.payload.OpenKvmPayload;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The KVM Switch's channel bar: one monitor, several machines. A rack holding two or more computers
 * needs the switch to say which of them the screen means, so this is what the monitor shows first:
 * pick a channel (click it, or press its number key) and the machine's own session opens on top.
 */
public final class KvmChannelScreen extends AbstractComputerScreen<MonitorSessionMenu> {

    private static final int W = 260;
    private static final int ROW_H = 20;
    private static final int PAD = 10;

    /** The bar the rack last sent, kept until the session that shows it is built. */
    @Nullable
    private static OpenKvmPayload pending;

    private final BlockPos rackPos;
    private final BlockPos monitorPos;
    private final int activeChannel;
    private final List<OpenKvmPayload.Channel> channels;

    public KvmChannelScreen(final MonitorSessionMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = W;
        this.imageHeight = 40;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        this.rackPos = menu.hostPos();
        this.monitorPos = menu.monitorPos();
        this.activeChannel = pending == null ? -1 : pending.activeChannel();
        this.channels = pending == null ? List.of() : pending.channels();
        this.imageHeight = this.panelHeight();
    }

    /** The bar the rack is showing, said before the session that shows it is opened. */
    public static void expect(final OpenKvmPayload payload) {
        pending = payload;
    }

    /** The rack's own generation, so the switch wears the monitor that rack would really have. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return this.getMenu().hardwareEra();
    }

    private int panelHeight() {
        return PAD * 2 + 26 + channels.size() * ROW_H;
    }

    private int left() {
        return this.leftPos;
    }

    private int top() {
        return this.topPos;
    }

    private void select(final int index) {
        if (index < 0 || index >= channels.size()) {
            return;
        }
        PacketDistributor.sendToServer(new KvmSelectPayload(rackPos, monitorPos,
                channels.get(index).slot()));
        /*
         * The rack puts the chosen machine's screen up, and this one goes when that one arrives. Closing
         * here as well sent a close right behind the choice, which the server took for the screen it had
         * just opened: the player then sat at a machine the server did not know they were looking at.
         */
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int x = left();
        final int y = top() + 26;
        for (int i = 0; i < channels.size(); i++) {
            final int rowY = y + i * ROW_H;
            if (mouseX >= x + PAD && mouseX < x + W - PAD && mouseY >= rowY && mouseY < rowY + ROW_H - 2) {
                select(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        // The channel keys of a real switch: 1..8 pick a machine straight off the bar.
        if (keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_8) {
            select(keyCode - InputConstants.KEY_1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = left();
        final int y = top();
        JsTechTheme.window(g, x, y, W, panelHeight());
        JsTechTheme.headerBar(g, x + 6, y + 6, W - 12);
        JsTechTheme.text(g, font, GameText.resolve(MonitorScreenTexts.KVM_TITLE), x + 12, y + 11, JsTechTheme.text());
        final String count = GameText.resolve(MonitorScreenTexts.KVM_MACHINES.with(channels.size()));
        JsTechTheme.text(g, font, count, x + W - 12 - font.width(count), y + 11, JsTechTheme.dim());

        for (int i = 0; i < channels.size(); i++) {
            final OpenKvmPayload.Channel channel = channels.get(i);
            final int rowY = y + 26 + i * ROW_H;
            final boolean hovered = mouseX >= x + PAD && mouseX < x + W - PAD
                    && mouseY >= rowY && mouseY < rowY + ROW_H - 2;
            final boolean active = channel.slot() == activeChannel;
            JsTechTheme.panel(g, x + PAD, rowY, W - PAD * 2, ROW_H - 2);
            if (hovered || active) {
                g.fill(x + PAD, rowY, x + PAD + 2, rowY + ROW_H - 2,
                        active ? JsTechTheme.accent() : JsTechTheme.dim());
            }
            JsTechTheme.text(g, font, GameText.resolve(MonitorScreenTexts.KVM_KEY.with(i + 1)), x + PAD + 8, rowY + 6,
                    active ? JsTechTheme.accent() : JsTechTheme.dim());
            JsTechTheme.text(g, font, GameText.resolve(channel.name()), x + PAD + 34, rowY + 6, JsTechTheme.text());
            final String state = GameText.resolve(
                    channel.running() ? MonitorScreenTexts.KVM_ONLINE : MonitorScreenTexts.KVM_OFF);
            JsTechTheme.text(g, font, state, x + W - PAD - 8 - font.width(state), rowY + 6,
                    channel.running() ? JsTechTheme.green() : JsTechTheme.red());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
