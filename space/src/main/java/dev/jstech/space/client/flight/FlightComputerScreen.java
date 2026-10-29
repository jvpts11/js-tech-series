/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.flight;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.space.client.rocket.FlightComputerFace;
import dev.jstech.space.client.rocket.FlightComputerPages;
import dev.jstech.space.flight.FlightComputer;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.flight.FlightComputerMenu;
import dev.jstech.space.flight.FlightPage;
import dev.jstech.space.rocket.CabinReadings;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Flight Computer's screen: its approved face as large as the window holds it, its keys pressed by clicking the
 * keys or their legends, and on the landing page the spot clicked on the map marked to land on. The landing map is
 * asked for whenever what it shows changes: the page opened on it, the world gone to, the mark, the zoom.
 */
public final class FlightComputerScreen extends AbstractContainerScreen<FlightComputerMenu> {

    private float scale = 1;
    /* What the landing map was last asked for as, so it is asked for again only when that changes. */
    private Object asked = "";

    /* How far the face keeps from the window's edges, in its pixels. */
    private static final int MARGIN = 10;
    private static final int HIDDEN = -10_000;

    public FlightComputerScreen(final FlightComputerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.titleLabelX = HIDDEN;
        this.inventoryLabelY = HIDDEN;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            final double x = (mouseX - this.leftPos) / this.scale;
            final double y = (mouseY - this.topPos) / this.scale;
            final FlightComputerFace.View view = view();
            final int key = FlightComputerFace.keyAt(this.font, view, x, y);
            if (key >= 0) {
                FlightComputer.PRESS.send(key);
                return true;
            }
            if (view.page() == FlightPage.LANDING) {
                final Optional<BlockPos> spot = FlightComputerPages.onMap(view, x, y);
                if (spot.isPresent()) {
                    FlightComputer.MARK.send(spot.get());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void init() {
        this.scale = Math.max(0.5F, Math.min((this.width - 2F * MARGIN) / FlightComputerFace.WIDTH,
                (this.height - 2F * MARGIN) / FlightComputerFace.HEIGHT));
        this.imageWidth = Math.round(FlightComputerFace.WIDTH * this.scale);
        this.imageHeight = Math.round(FlightComputerFace.HEIGHT * this.scale);
        super.init();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        final FlightComputerBlockEntity computer = this.menu.computer();
        if (computer.page() != FlightPage.LANDING) {
            return;
        }
        final Object wanted = Objects.hash(computer.target(), computer.mark(), computer.zoom());
        if (!wanted.equals(this.asked)) {
            this.asked = wanted;
            FlightComputer.MAP.send(true);
        }
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX,
                            final int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(this.leftPos, this.topPos, 0);
        FlightComputerFace.paint(graphics, this.imageWidth, this.imageHeight, view());
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // The face writes everything it shows.
    }

    private FlightComputerFace.View view() {
        final long tick = this.minecraft == null || this.minecraft.level == null ? 0
                : this.minecraft.level.getGameTime();
        return FlightComputerPages.view(this.menu.computer(), CabinReadings.AT_REST, tick);
    }
}
