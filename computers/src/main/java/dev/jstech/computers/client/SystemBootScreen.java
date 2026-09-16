/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The system coming up: what the monitor shows between the self-test ending and the desktop or the prompt opening.
 *
 * <p>The machine is the one bringing it up and the one keeping the time. This screen joins it wherever it has got
 * to, draws for as long as the machine says is left, and waits: the machine is what puts the system in front of the
 * player when it is ready. Closing this changes nothing, which is the point of the machine owning it.
 */
public final class SystemBootScreen extends Screen {

    private static final int W = 340;
    private static final int H = 214;

    /** Drawn for this long when the machine did not say, which only a stale packet leaves. */
    private static final int FALLBACK_TICKS = 60;

    private final BlockPos computerPos;
    private final BlockPos monitorPos;
    private final ResourceLocation osId;
    private final String osName;
    private final int totalTicks;

    private int ticks;

    public SystemBootScreen(final BlockPos computerPos, final BlockPos monitorPos, final ResourceLocation osId,
                            final String osName, final int remainingTicks, final int totalTicks) {
        super(Component.literal("Starting"));
        this.computerPos = computerPos;
        this.monitorPos = monitorPos;
        this.osId = osId;
        this.osName = osName;
        this.totalTicks = totalTicks > 0 ? totalTicks : FALLBACK_TICKS;
        this.ticks = Math.max(0, this.totalTicks - Math.max(0, remainingTicks));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.ticks < this.totalTicks) {
            this.ticks++;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        /*
         * Screen.render paints the dimmed backdrop itself; painting it again after our own drawing would wash the
         * whole sequence out, so super runs FIRST and the content after.
         */
        super.render(g, mouseX, mouseY, partialTick);
        final int x = (width - W) / 2;
        final int y = (height - H) / 2;
        MonitorFrame.renderBody(g, x, y, W, H, era(), font);
        g.fill(x, y, x + W, y + H, 0xFF05070A);

        g.drawCenteredString(font, osName.isEmpty() ? "Starting" : osName, x + W / 2, y + H / 2 - 16, 0xFFE6ECF6);
        g.drawCenteredString(font, "starting", x + W / 2, y + H / 2 - 4, 0xFF7D8A9C);

        final int barW = 140;
        final int bx = x + (W - barW) / 2;
        final int by = y + H / 2 + 12;
        g.fill(bx, by, bx + barW, by + 3, 0xFF1D2530);
        g.fill(bx, by, bx + Math.min(barW, barW * this.ticks / this.totalTicks), by + 3, 0xFF39D6C4);
    }

    /** The host computer's era, so the monitor bezel matches the machine the system is coming up on. */
    private HardwareEra era() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(this.computerPos)
                instanceof AbstractComputerBlockEntity computer && computer.displayEra() != null) {
            return computer.displayEra();
        }
        return HardwareEra.STANDARD;
    }

    /** The system coming up, for the per-system sequences that read it. */
    public ResourceLocation system() {
        return this.osId;
    }

    /** The monitor this is drawn on, for the per-system sequences that read it. */
    public BlockPos monitor() {
        return this.monitorPos;
    }
}
