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
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.space.client.rocket.FlightComputerFace;
import dev.jstech.space.client.universe.SystemSpaceRenderer;
import dev.jstech.space.crew.CrewRole;
import dev.jstech.space.crew.CrewTexts;
import dev.jstech.space.crew.ShipConsole;
import dev.jstech.space.crew.ShipConsoleMenu;
import dev.jstech.space.crew.ShipCrew;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.network.ClimbWorldsPayload;
import dev.jstech.space.ship.ShipAboard;
import dev.jstech.space.ship.ShipDestination;
import dev.jstech.space.ship.ShipDestinations;
import dev.jstech.space.ship.ShipHomeWay;
import dev.jstech.space.ship.ShipHudTexts;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.shipyard.ShipyardPlans;
import dev.jstech.space.universe.SolarBody;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

/**
 * A small ship's console's screen, its approved pages as large as the window holds them, opened by the first two keys
 * along the top. On the crew page its owner chooses a row by its left key or by clicking it, gives it a role by a
 * right key, turns the pages by the keys along the top, and names a player by typing their name and pressing ADD or
 * Enter; everyone else reads it. On the destination page, in orbit over a world, a pilot chooses a place of it to come
 * down onto by its left key or by clicking it, or lets it go by CLEAR or by choosing under the ship.
 */
public final class ShipConsoleScreen extends AbstractContainerScreen<ShipConsoleMenu> {

    private float scale = 1;
    private boolean destinations;
    private int page;
    private int destinationPage;
    private int chosen = -1;
    private String scratch = "";
    private int seats;
    private int bunks;

    /* How far the face keeps from the window's edges, in its pixels; how fast the caret blinks, in ticks. */
    private static final int MARGIN = 10;
    private static final int HIDDEN = -10_000;
    private static final int BLINK = 10;
    private static final double THOUSAND = 1000;
    private static final double HALF = 0.5;

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
        if (this.destinations) {
            return clickDestinations(x, y) || super.mouseClicked(mouseX, mouseY, button);
        }
        final ShipConsoleFace.View view = view();
        final int key = FlightComputerFace.keyAt(this.font, List.of(), ShipConsoleFace.rightLegends(view), x, y,
                ShipConsoleFace.TOP_KEYS);
        if (key == FlightComputerBlockEntity.TOP + ShipConsoleFace.DEST_KEY) {
            this.destinations = true;
            return true;
        }
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
        if (owner() && !this.destinations && this.scratch.length() < ShipCrew.LONGEST_NAME
                && (Character.isLetterOrDigit(typed) || typed == '_')) {
            this.scratch = this.scratch + typed;
            return true;
        }
        return super.charTyped(typed, modifiers);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || !owner() || this.destinations) {
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
        if (this.destinations) {
            ShipDestinationFace.paint(graphics, this.imageWidth, this.imageHeight, destinationView());
        } else {
            ShipConsoleFace.paint(graphics, this.imageWidth, this.imageHeight, view());
        }
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

    /*
     * A click on the destination page at {@code (x, y)} of the face: the crew page opened, its rows' pages turned, a
     * place chosen by its left key or its row, or let go by CLEAR or by choosing under the ship; whether it did one.
     */
    private boolean clickDestinations(final double x, final double y) {
        final ShipDestinationFace.View view = destinationView();
        final int key = FlightComputerFace.keyAt(this.font, List.of(), ShipDestinationFace.rightLegends(view), x, y,
                ShipConsoleFace.TOP_KEYS);
        if (key == FlightComputerBlockEntity.TOP + ShipConsoleFace.CREW_KEY) {
            this.destinations = false;
            return true;
        }
        if (key == FlightComputerBlockEntity.TOP + ShipConsoleFace.PREVIOUS_KEY && this.destinationPage > 0) {
            this.destinationPage--;
            return true;
        }
        if (key == FlightComputerBlockEntity.TOP + ShipConsoleFace.NEXT_KEY
                && this.destinationPage + 1 < view.pages()) {
            this.destinationPage++;
            return true;
        }
        if (!view.pilots() || view.world().isEmpty()) {
            return false;
        }
        if (key == FlightComputerBlockEntity.RIGHT + ShipDestinationFace.clearKey()) {
            ShipConsole.CLEAR.send(true);
            return true;
        }
        final int row = key >= FlightComputerBlockEntity.LEFT && key < FlightComputerBlockEntity.RIGHT
                ? key - FlightComputerBlockEntity.LEFT : rowAt(x, y);
        if (row < 0 || row >= view.page().size()) {
            return false;
        }
        final ShipDestination place = view.page().get(row).place();
        if (place == null) {
            ShipConsole.CLEAR.send(true);
        } else {
            ShipConsole.CHOOSE.send(place);
        }
        return true;
    }

    /*
     * What the destination page shows: the world below the server found as it opened, where the ship comes down now
     * as the player's game sees it, the places of the world and under the ship last, each how far from there, the one
     * the console keeps chosen, and where the ship comes down.
     */
    private ShipDestinationFace.View destinationView() {
        final ShipDestinations.Offer offer = this.menu.offer();
        final SolarBody body = Arrays.stream(SolarBody.values()).filter(each -> each.name().equals(offer.body()))
                .findFirst().orElse(null);
        final ClimbWorldsPayload.ClimbWorld world = body == null ? null : SystemSpaceRenderer.worldBelow(body);
        final double[] under = world == null || this.minecraft == null || this.minecraft.player == null ? null
                : ShipHomeWay.below(body, this.minecraft.player.getX(), this.minecraft.player.getZ(), world.width());
        final List<ShipDestinationFace.Row> rows = new ArrayList<>();
        for (final ShipDestination place : offer.places()) {
            final String far = under == null ? "" : GameText.resolve(CrewTexts.KM.with(ShipHudHome.grouped(
                    Math.hypot(place.at().getX() + HALF - under[0], place.at().getZ() + HALF - under[1]) / THOUSAND)));
            rows.add(new ShipDestinationFace.Row(place.kind(), GameText.resolve(place.kind().word()),
                    GameText.resolve(place.kind().title().with(place.number())), far, place));
        }
        rows.add(new ShipDestinationFace.Row(null, GameText.resolve(CrewTexts.NONE),
                GameText.resolve(CrewTexts.UNDER_THE_SHIP), GameText.resolve(CrewTexts.KM.with(0)), null));
        // The place the console keeps chosen, or under the ship, the last row, for none.
        final Optional<ShipDestination> kept = this.menu.console().destination();
        int chosen = rows.size() - 1;
        for (int i = 0; i < rows.size(); i++) {
            if (kept.isPresent() && kept.get().equals(rows.get(i).place())) {
                chosen = i;
            }
        }
        final int pages = Math.max(1, (rows.size() + ShipConsoleFace.ROWS - 1) / ShipConsoleFace.ROWS);
        this.destinationPage = Math.min(this.destinationPage, pages - 1);
        final int first = this.destinationPage * ShipConsoleFace.ROWS;
        final List<ShipDestinationFace.Row> shown = rows.subList(first, Math.min(rows.size(),
                first + ShipConsoleFace.ROWS));
        final String below = under == null ? "" : GameText.resolve(ShipHudTexts.BELOW.with(Text.literal(
                ShipHudHome.grouped(under[0])), Text.literal(ShipHudHome.grouped(under[1]))));
        final String downTo = kept.map(place -> GameText.resolve(place.kind().title().with(place.number())))
                .orElse(GameText.resolve(CrewTexts.UNDER_THE_SHIP));
        final String downToAt = kept.map(place -> GameText.resolve(CrewTexts.AT.with(ShipHudHome.grouped(
                place.at().getX()), ShipHudHome.grouped(place.at().getZ())))).orElse("");
        return new ShipDestinationFace.View(body == null ? "" : GameText.resolve(ShipHudTexts.worldName(body)), below,
                shown, this.destinationPage, pages, chosen >= first && chosen < first + shown.size() ? chosen - first
                : -1, this.menu.viewer().holds(CrewRole.PILOT), downTo, downToAt);
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
