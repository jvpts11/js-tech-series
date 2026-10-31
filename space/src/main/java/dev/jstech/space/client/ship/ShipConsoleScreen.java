/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.PlanSpot;
import dev.jstech.space.client.rocket.FlightComputerFace;
import dev.jstech.space.crew.CrewRole;
import dev.jstech.space.crew.ShipConsole;
import dev.jstech.space.crew.ShipConsoleMenu;
import dev.jstech.space.crew.ShipCrew;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.ship.ShipAboard;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.shipyard.ShipyardPlans;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/**
 * A small ship's console's screen: its approved crew page as large as the window holds it. Its owner chooses a row by
 * its left key or by clicking it, gives it a role by a right key, turns the pages by the keys along the top, and names
 * a player by typing their name and pressing ADD or Enter. Everyone else reads it.
 */
public final class ShipConsoleScreen extends AbstractContainerScreen<ShipConsoleMenu> {

    private float scale = 1;
    private int page;
    private int chosen = -1;
    private String scratch = "";
    private int seats;
    private int bunks;

    /* How far the face keeps from the window's edges, in its pixels; how fast the caret blinks, in ticks. */
    private static final int MARGIN = 10;
    private static final int HIDDEN = -10_000;
    private static final int BLINK = 10;

    public ShipConsoleScreen(final ShipConsoleMenu menu, final Inventory inventory, final Component title) {
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
        if (button != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        final double x = (mouseX - this.leftPos) / this.scale;
        final double y = (mouseY - this.topPos) / this.scale;
        final ShipConsoleFace.View view = view();
        final int key = FlightComputerFace.keyAt(this.font, List.of(), ShipConsoleFace.rightLegends(view), x, y);
        if (key == FlightComputerBlockEntity.TOP + ShipConsoleFace.PREVIOUS_KEY && this.page > 0) {
            this.page--;
            this.chosen = -1;
            return true;
        }
        if (key == FlightComputerBlockEntity.TOP + ShipConsoleFace.NEXT_KEY && this.page + 1 < view.pages()) {
            this.page++;
            this.chosen = -1;
            return true;
        }
        if (!view.owner()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        final int row = key >= FlightComputerBlockEntity.LEFT && key < FlightComputerBlockEntity.RIGHT
                ? key - FlightComputerBlockEntity.LEFT : rowAt(x, y);
        if (row >= 0 && row < view.page().size()) {
            this.chosen = view.page().get(row).kind() == ShipConsoleFace.Row.Kind.OWNER ? -1 : row;
            return true;
        }
        if (key >= FlightComputerBlockEntity.RIGHT) {
            press(key - FlightComputerBlockEntity.RIGHT, view);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(final char typed, final int modifiers) {
        if (owner() && this.scratch.length() < ShipCrew.LONGEST_NAME
                && (Character.isLetterOrDigit(typed) || typed == '_')) {
            this.scratch = this.scratch + typed;
            return true;
        }
        return super.charTyped(typed, modifiers);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || !owner()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !this.scratch.isEmpty()) {
            this.scratch = this.scratch.substring(0, this.scratch.length() - 1);
        } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            add();
        }
        // A letter typed into the scratchpad never closes the screen, as the inventory's key would.
        return true;
    }

    @Override
    protected void init() {
        this.scale = Math.max(0.5F, Math.min((this.width - 2F * MARGIN) / FlightComputerFace.WIDTH,
                (this.height - 2F * MARGIN) / FlightComputerFace.HEIGHT));
        this.imageWidth = Math.round(FlightComputerFace.WIDTH * this.scale);
        this.imageHeight = Math.round(FlightComputerFace.HEIGHT * this.scale);
        super.init();
        count();
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX,
                            final int mouseY) {
        graphics.pose().pushPose();
        graphics.pose().translate(this.leftPos, this.topPos, 0);
        ShipConsoleFace.paint(graphics, this.imageWidth, this.imageHeight, view());
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // The face writes everything it shows.
    }

    /* A right key pressed: the chosen row given its role, or the name typed named. */
    private void press(final int key, final ShipConsoleFace.View view) {
        if (key == ShipConsoleFace.ROLES.size()) {
            add();
            return;
        }
        if (key < 0 || key >= ShipConsoleFace.ROLES.size() || this.chosen < 0 || this.chosen >= view.page().size()) {
            return;
        }
        final ShipConsoleFace.Row row = view.page().get(this.chosen);
        final CrewRole role = ShipConsoleFace.ROLES.get(key);
        final int who = switch (row.kind()) {
            case TEAM -> ShipConsole.RoleSet.TEAM;
            case OTHERS -> ShipConsole.RoleSet.OTHERS;
            default -> ShipConsole.RoleSet.NAMED;
        };
        ShipConsole.SET.send(new ShipConsole.RoleSet(who, row.id(), role));
        if (row.kind() == ShipConsoleFace.Row.Kind.NAMED && role == CrewRole.NONE) {
            this.chosen = -1;
        }
    }

    private void add() {
        if (!this.scratch.isBlank()) {
            ShipConsole.ADD.send(this.scratch);
            this.scratch = "";
        }
    }

    /* The row of the page open at {@code (x, y)} of the face, by its text, or -1. */
    private static int rowAt(final double x, final double y) {
        if (x < FlightComputerFace.SX0 || x >= ShipConsoleFace.ROWS_RIGHT) {
            return -1;
        }
        for (int i = 0; i < ShipConsoleFace.ROWS; i++) {
            final int top = FlightComputerFace.sideKeyY(i) - 2;
            if (y >= top && y < top + 14) {
                return i;
            }
        }
        return -1;
    }

    private boolean owner() {
        return this.menu.setsRoles();
    }

    /* The ship's seats and bunks, from its plan: the real bound of its crew. */
    private void count() {
        final Optional<ShipAboard> ship = this.menu.console().ship();
        if (ship.isEmpty() || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        final List<PlanSpot> spots = ShipyardPlans.of(this.minecraft.level, ship.get().design(), Orientation.NORTH)
                .spots();
        this.seats = (int) spots.stream().filter(spot -> spot.block().equals(ShipKitBlocks.PILOT_SEAT.getId()))
                .count();
        this.bunks = (int) spots.stream().filter(spot -> spot.block().equals(ShipKitBlocks.BUNK_FRAME.getId()))
                .count();
    }

    private ShipConsoleFace.View view() {
        final List<ShipConsoleFace.Row> rows = this.menu.console().crew().map(crew -> ShipConsoleFace.rows(crew,
                this.menu.teamName())).orElse(List.of());
        final int pages = Math.max(1, (rows.size() + ShipConsoleFace.ROWS - 1) / ShipConsoleFace.ROWS);
        this.page = Math.min(this.page, pages - 1);
        final List<ShipConsoleFace.Row> shown = rows.subList(Math.min(rows.size(), this.page * ShipConsoleFace.ROWS),
                Math.min(rows.size(), (this.page + 1) * ShipConsoleFace.ROWS));
        final String name = this.menu.console().ship().map(aboard -> aboard.design().name()).orElse("");
        final long tick = this.minecraft == null || this.minecraft.level == null ? 0
                : this.minecraft.level.getGameTime();
        return new ShipConsoleFace.View(name, this.seats, this.bunks, shown, this.page, pages, owner(), this.chosen,
                this.menu.viewer(), this.scratch, (tick / BLINK) % 2 == 0);
    }
}
