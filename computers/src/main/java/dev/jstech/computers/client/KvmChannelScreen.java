/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.gui.layout.KvmChannelLayout;
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

    /** The bar the rack last sent, kept until the session that shows it is built. */
    @Nullable
    private static OpenKvmPayload pending;

    private final BlockPos rackPos;
    private final BlockPos monitorPos;
    private final int activeChannel;
    private final List<OpenKvmPayload.Channel> channels;

    public KvmChannelScreen(final MonitorSessionMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = KvmChannelLayout.WIDTH;
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
        return KvmChannelLayout.height(channels.size());
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
        for (int i = 0; i < channels.size(); i++) {
            if (hover((int) mouseX, (int) mouseY, KvmChannelLayout.PAD, KvmChannelLayout.rowY(i),
                    KvmChannelLayout.WIDTH - 2 * KvmChannelLayout.PAD, KvmChannelLayout.ROW_BOX_H)) {
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
        JsTechTheme.window(g, x, y, KvmChannelLayout.WIDTH, panelHeight());
        JsTechTheme.headerBar(g, x + KvmChannelLayout.HEADER_X, y + KvmChannelLayout.HEADER_Y,
                KvmChannelLayout.HEADER_W);
        JsTechTheme.text(g, font, GameText.resolve(MonitorScreenTexts.KVM_TITLE), x + KvmChannelLayout.TITLE_X,
                y + KvmChannelLayout.TITLE_Y, JsTechTheme.text());
        final String count = GameText.resolve(MonitorScreenTexts.KVM_MACHINES.with(channels.size()));
        JsTechTheme.text(g, font, count, x + KvmChannelLayout.WIDTH - KvmChannelLayout.MARGIN - font.width(count),
                y + KvmChannelLayout.TITLE_Y, JsTechTheme.dim());

        for (int i = 0; i < channels.size(); i++) {
            final OpenKvmPayload.Channel channel = channels.get(i);
            final int rowY = y + KvmChannelLayout.rowY(i);
            final boolean hovered = hover(mouseX, mouseY, KvmChannelLayout.PAD, KvmChannelLayout.rowY(i),
                    KvmChannelLayout.WIDTH - 2 * KvmChannelLayout.PAD, KvmChannelLayout.ROW_BOX_H);
            final boolean active = channel.slot() == activeChannel;
            JsTechTheme.panel(g, x + KvmChannelLayout.PAD, rowY,
                    KvmChannelLayout.WIDTH - KvmChannelLayout.PAD * 2, KvmChannelLayout.ROW_BOX_H);
            if (hovered || active) {
                g.fill(x + KvmChannelLayout.PAD, rowY, x + KvmChannelLayout.PAD + KvmChannelLayout.MARK_W,
                        rowY + KvmChannelLayout.ROW_BOX_H, active ? JsTechTheme.accent() : JsTechTheme.dim());
            }
            JsTechTheme.text(g, font, GameText.resolve(MonitorScreenTexts.KVM_KEY.with(i + 1)),
                    x + KvmChannelLayout.ROW_KEY_X, rowY + KvmChannelLayout.ROW_TEXT_DY,
                    active ? JsTechTheme.accent() : JsTechTheme.dim());
            JsTechTheme.text(g, font, GameText.resolve(channel.name()), x + KvmChannelLayout.ROW_NAME_X,
                    rowY + KvmChannelLayout.ROW_TEXT_DY, JsTechTheme.text());
            final String state = GameText.resolve(
                    channel.running() ? MonitorScreenTexts.KVM_ONLINE : MonitorScreenTexts.KVM_OFF);
            JsTechTheme.text(g, font, state,
                    x + KvmChannelLayout.WIDTH - KvmChannelLayout.ROW_STATE_MARGIN - font.width(state),
                    rowY + KvmChannelLayout.ROW_TEXT_DY, channel.running() ? JsTechTheme.green() : JsTechTheme.red());
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
