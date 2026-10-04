/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.WorkshopLayout;
import dev.jstech.computers.operation.payload.workshop.WorkshopActionPayload;
import dev.jstech.computers.operation.payload.workshop.WorkshopStatePayload;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.CellGrid;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.client.os.WorkshopAppTexts.ANVIL;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ANVIL_CARD;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ANVIL_EMPTY;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ANVIL_NOTHING;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ANVIL_WAS;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ANVIL_WITH;
import static dev.jstech.computers.client.os.WorkshopAppTexts.CARDS;
import static dev.jstech.computers.client.os.WorkshopAppTexts.CLEAR_GRID;
import static dev.jstech.computers.client.os.WorkshopAppTexts.CLUE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.CRAFT;
import static dev.jstech.computers.client.os.WorkshopAppTexts.CRAFTING_TABLE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.CRAFTING_TABLE_CARD;
import static dev.jstech.computers.client.os.WorkshopAppTexts.CRAFT_ALL;
import static dev.jstech.computers.client.os.WorkshopAppTexts.EMPTY_GRID;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ENCHANTING;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ENCHANTING_CARD;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ENCHANT_COST;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ENCHANT_WAS;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ENOUGH_FOR;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ENOUGH_FOR_NONE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.FURNACE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.FURNACE_CARD;
import static dev.jstech.computers.client.os.WorkshopAppTexts.FURNACE_IDLE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.FURNACE_LEFT;
import static dev.jstech.computers.client.os.WorkshopAppTexts.FURNACE_SMELTING;
import static dev.jstech.computers.client.os.WorkshopAppTexts.INGREDIENT;
import static dev.jstech.computers.client.os.WorkshopAppTexts.IN_INVENTORY;
import static dev.jstech.computers.client.os.WorkshopAppTexts.INVENTORY;
import static dev.jstech.computers.client.os.WorkshopAppTexts.LEVEL;
import static dev.jstech.computers.client.os.WorkshopAppTexts.LEVELS;
import static dev.jstech.computers.client.os.WorkshopAppTexts.LOADING;
import static dev.jstech.computers.client.os.WorkshopAppTexts.NAME;
import static dev.jstech.computers.client.os.WorkshopAppTexts.NEEDS_CARD;
import static dev.jstech.computers.client.os.WorkshopAppTexts.NEEDS_NONE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.NOTHING_SMELTING;
import static dev.jstech.computers.client.os.WorkshopAppTexts.NO_CARD;
import static dev.jstech.computers.client.os.WorkshopAppTexts.NO_CARDS;
import static dev.jstech.computers.client.os.WorkshopAppTexts.NO_FUEL;
import static dev.jstech.computers.client.os.WorkshopAppTexts.NO_LAPIS;
import static dev.jstech.computers.client.os.WorkshopAppTexts.NO_RESULT;
import static dev.jstech.computers.client.os.WorkshopAppTexts.OFFERS_NOTE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.ONE_LEVEL;
import static dev.jstech.computers.client.os.WorkshopAppTexts.RECIPE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.REPAIR_RENAME;
import static dev.jstech.computers.client.os.WorkshopAppTexts.RUNS_ON_COMPUTER;
import static dev.jstech.computers.client.os.WorkshopAppTexts.SMELTING;
import static dev.jstech.computers.client.os.WorkshopAppTexts.SMELTING_LINE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.SPEED;
import static dev.jstech.computers.client.os.WorkshopAppTexts.SPEED_LINE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.SPEED_ONLY;
import static dev.jstech.computers.client.os.WorkshopAppTexts.TAB_ANVIL;
import static dev.jstech.computers.client.os.WorkshopAppTexts.TAB_CRAFTING;
import static dev.jstech.computers.client.os.WorkshopAppTexts.TAB_ENCHANTING;
import static dev.jstech.computers.client.os.WorkshopAppTexts.TAB_FURNACE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.TAKE_IT;
import static dev.jstech.computers.client.os.WorkshopAppTexts.THE_OFFERS;
import static dev.jstech.computers.client.os.WorkshopAppTexts.TITLE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.WHAT_IT_TAKES;
import static dev.jstech.computers.client.os.WorkshopAppTexts.WINDOW_CLOSED;
import static dev.jstech.computers.client.os.WorkshopAppTexts.WINDOW_CLOSED_NOTE;
import static dev.jstech.computers.client.os.WorkshopAppTexts.YOUR_EXPERIENCE;

/**
 * The Workshop, by Autodeck: the personal-use cards of a Personal Computer, one tab each (a tab whose card is not in
 * the computer is dim, with "no card"). The station of the tab that is up sits on top with what is on it detailed at
 * its right, the player's inventory below as the desktop lays its real slots there, and the status bar names the
 * cards, what the furnace is doing and the player's level. Items go in and out with the cursor as in any container;
 * a shift-click on the inventory sends a whole stack to the tab's station.
 *
 * <p>Everything shown comes from the computer; the window keeps the last state it was sent and asks again a few times
 * a second, so the furnace's progress moves while it is open.
 */
@PaletteHolder
public final class WorkshopApp implements IInventoryBandApp {

    private static WorkshopApp active;

    private final BlockPos host;
    private final BlockPos monitorPos;
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private WorkshopStatePayload state;
    private String status = "";
    private int statusFrames;
    private int refreshFrames;
    private int tab;
    private boolean tabChosen;
    private int lastX;
    private int lastY;
    private int lastW;
    private int lastMouseX;
    private int lastMouseY;
    /* Where each Workshop slot, and each result, was drawn last frame, for the tooltips: {x, y, kind, index}. */
    private final List<int[]> drawnCells = new ArrayList<>();

    private final Panel root = new Panel();
    private final TabStrip tabs;
    private final CellGrid grid;
    private final CellGrid result;
    private final CellGrid furnaceIn;
    private final CellGrid furnaceOut;
    private final CellGrid enchantItem;
    private final CellGrid anvilLeft;
    private final CellGrid anvilRight;
    private final CellGrid anvilResult;
    private final Button craft;
    private final Button craftAll;
    private final Button clearGrid;
    private final Button takeAnvil;
    private final TextField anvilName;

    private static final int CELL = WorkshopLayout.CELL;
    private static final int PAD = WorkshopLayout.PAD;
    private static final int REFRESH_EVERY_FRAMES = 20;
    private static final int STATUS_FRAMES = 200;
    private static final int TICKS_PER_SECOND = 20;
    private static final int OFFERS = 3;
    private static final int BUTTON_GAP = 2;
    private static final int BUTTON_PAD = 5;
    /* What a drawn cell shows: a slot of the Workshop, or one of the two results. */
    private static final int SLOT = 0;
    private static final int CRAFT_RESULT = 1;
    private static final int ANVIL_RESULT = 2;
    private static final ResourceLocation[] ORBS = {
        ResourceLocation.withDefaultNamespace("container/enchanting_table/level_1"),
        ResourceLocation.withDefaultNamespace("container/enchanting_table/level_2"),
        ResourceLocation.withDefaultNamespace("container/enchanting_table/level_3")};
    /** This window's own colours: what is at work, and what the player cannot afford. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/workshop",
            new Colours(0xFF2E8B3E, 0xFFB03A2E));

    public WorkshopApp(final BlockPos host, final BlockPos monitorPos) {
        this.host = host;
        this.monitorPos = monitorPos;
        tabs = root.add(new TabStrip(List.of(GameText.resolve(TAB_CRAFTING), GameText.resolve(TAB_FURNACE),
                GameText.resolve(TAB_ENCHANTING), GameText.resolve(TAB_ANVIL)))
                .fitToLabels(12)
                .setDisabled(index -> state != null && !holds(state.cards(), index),
                        () -> GameText.resolve(NO_CARD))
                .setOnSelect(index -> {
                    tab = index;
                    tabChosen = true;
                }));
        grid = root.add(cells(3, Workshop.GRID));
        result = root.add(new CellGrid(1, 1, 1, CELL)
                .setWells(true)
                .setRenderer((g, ctx, i, cx, cy, w, h, over) -> drawStack(g, ctx, craftResult(), cx, cy,
                        CRAFT_RESULT, 0))
                .setOnClick((i, button, shift) -> send(WorkshopActionPayload.CRAFT, 0)));
        furnaceIn = root.add(cells(1, Workshop.FURNACE_IN));
        furnaceOut = root.add(cells(1, Workshop.FURNACE_OUT));
        enchantItem = root.add(cells(1, Workshop.ENCHANT_ITEM));
        anvilLeft = root.add(cells(1, Workshop.ANVIL_LEFT));
        anvilRight = root.add(cells(1, Workshop.ANVIL_RIGHT));
        anvilResult = root.add(new CellGrid(1, 1, 1, CELL)
                .setWells(true)
                .setRenderer((g, ctx, i, cx, cy, w, h, over) -> drawStack(g, ctx,
                        state == null ? ItemStack.EMPTY : state.anvilResult(), cx, cy, ANVIL_RESULT, 0))
                .setOnClick((i, button, shift) -> send(WorkshopActionPayload.ANVIL_TAKE, 0)));
        craft = root.add(new Button(GameText.resolve(CRAFT), () -> send(WorkshopActionPayload.CRAFT, 0)))
                .setPrimary(true).setLabelScale(Texts.SMALL);
        craftAll = root.add(new Button(() -> GameText.resolve(CRAFT_ALL.with(craftTotal())),
                () -> send(WorkshopActionPayload.CRAFT, 1)).setLabelScale(Texts.SMALL));
        clearGrid = root.add(new Button(GameText.resolve(CLEAR_GRID),
                () -> send(WorkshopActionPayload.CLEAR_GRID, 0)).setLabelScale(Texts.SMALL));
        takeAnvil = root.add(new Button(GameText.resolve(TAKE_IT), () -> send(WorkshopActionPayload.ANVIL_TAKE, 0)))
                .setPrimary(true).setLabelScale(Texts.SMALL);
        anvilName = root.add(new TextField(Workshop.MAX_NAME).setOnCommit(name -> PacketDistributor.sendToServer(
                new WorkshopActionPayload(host, monitorPos, WorkshopActionPayload.ANVIL_NAME, 0, 0, name))));
        active = this;
        send(WorkshopActionPayload.REFRESH, 0);
    }

    /** Delivers the computer's answer to the live window. */
    public static void accept(final WorkshopStatePayload payload) {
        if (active == null) {
            return;
        }
        active.state = payload;
        final String said = GameText.resolve(payload.status());
        if (!said.isEmpty()) {
            active.status = said;
            active.statusFrames = STATUS_FRAMES;
        }
        if (!active.anvilName.isFocused()) {
            active.anvilName.sync(payload.anvilName());
        }
        if (!active.tabChosen || !holds(payload.cards(), active.tab)) {
            active.tab = firstCard(payload.cards(), active.tab);
        }
    }

    /** The live Workshop window, for a test. */
    @Nullable
    public static WorkshopApp active() {
        return active;
    }

    /** The last state the computer sent, for a test. */
    @Nullable
    public WorkshopStatePayload state() {
        return state;
    }

    /** The tab that is up. */
    public int activeTab() {
        return tab;
    }

    /** The content-local centre of tab {@code index}, where a test clicks it. */
    public int[] tabCenter(final int index) {
        final int[] at = tabs.tabCenter(index);
        return new int[] {at[0] - lastX, at[1] - lastY};
    }

    public BlockPos host() {
        return host;
    }

    public BlockPos monitorPos() {
        return monitorPos;
    }

    /** The tab whose station a shift-click on the inventory sends a stack to. */
    public int shiftInsertTab() {
        return tab;
    }

    @Override
    public void markActive() {
        active = this;
    }

    @Override
    public void onRestored() {
        active = this;
        send(WorkshopActionPayload.REFRESH, 0);
    }

    @Override
    public void onClosed() {
        // As a table gives back what was left on it, the grid, the enchanting item and the anvil's two go back.
        send(WorkshopActionPayload.CLOSED, 0);
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        active = this;
        skin = osSkin;
    }

    @Override
    public String title() {
        return GameText.resolve(TITLE);
    }

    @Override
    public int defaultWidth() {
        return WorkshopLayout.DEFAULT_W;
    }

    @Override
    public int defaultHeight() {
        return minHeight();
    }

    @Override
    public int minWidth() {
        return WorkshopLayout.MIN_W;
    }

    @Override
    public int minHeight() {
        return WorkshopLayout.minContentHeight() + DesktopWindow.TITLE_H + 8;
    }

    @Override
    public int invCellContentX(final int col) {
        return PAD + WorkshopLayout.BAND_PAD + col * CELL;
    }

    @Override
    public int invCellContentY(final int row, final int contentHeight) {
        return WorkshopLayout.bandTop(contentHeight) + WorkshopLayout.BAND_PAD + WorkshopLayout.rowYOffset(row);
    }

    @Override
    public int invBandBottom(final int contentHeight) {
        return WorkshopLayout.bandTop(contentHeight) + WorkshopLayout.BAND_PAD
                + WorkshopLayout.rowYOffset(WorkshopLayout.INV_ROWS - 1) + CELL;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        lastX = x;
        lastY = y;
        lastW = width;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        drawnCells.clear();
        if (++refreshFrames >= REFRESH_EVERY_FRAMES) {
            refreshFrames = 0;
            send(WorkshopActionPayload.REFRESH, 0);
        }
        if (statusFrames > 0 && --statusFrames == 0) {
            status = "";
        }
        g.fill(x, y, x + width, y + height, skin.windowBg());
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        final int sx = x + PAD;
        final int sy = y + WorkshopLayout.STATION_TOP;
        final int sw = WorkshopLayout.stationWidth(width);
        final int dx = x + WorkshopLayout.detailsX(width);
        tabs.setBounds(x, y, width, WorkshopLayout.TAB_H);
        tabs.setSelected(tab);
        skin.field(g, sx, sy, sw, WorkshopLayout.STATION_H, false);
        skin.panel(g, dx, sy, WorkshopLayout.DETAILS_W, WorkshopLayout.STATION_H);
        final boolean loaded = state != null;
        final WorkshopCard card = cardOf(tab);
        final boolean open = loaded && card.in(state.cards());
        layoutCrafting(sx, sy, dx, open && card == WorkshopCard.CRAFTING_TABLE);
        layoutFurnace(sx, sy, open && card == WorkshopCard.FURNACE);
        layoutEnchanting(sx, sy, open && card == WorkshopCard.ENCHANTING);
        layoutAnvil(sx, sy, sw, dx, open && card == WorkshopCard.ANVIL);
        if (!loaded) {
            Draw.text(g, font, GameText.resolve(LOADING), sx + PAD, sy + PAD, skin.dim());
        } else if (!open) {
            Draw.text(g, font, Texts.clip(font, GameText.resolve(NEEDS_CARD.with(GameText.resolve(cardName(card)))),
                    sw - PAD * 2), sx + PAD, sy + PAD, skin.dim());
        } else {
            drawStation(g, font, card, sx, sy, sw, mouseX, mouseY);
            drawDetails(g, font, card, dx, sy);
        }
        drawBand(g, font, x + PAD, y + WorkshopLayout.bandTop(height));
        drawStatusBar(g, font, x, y + height - WorkshopLayout.STATUS_H, width);
        root.render(g, ctx);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (state == null) {
            return;
        }
        final int offer = offerAt(mouseX, mouseY);
        if (offer >= 0) {
            send(WorkshopActionPayload.ENCHANT, offer);
            return;
        }
        root.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return root.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void renderTooltip(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY) {
        if (state == null) {
            return;
        }
        for (final int[] cell : drawnCells) {
            if (mouseX < cell[0] || mouseX >= cell[0] + CELL || mouseY < cell[1] || mouseY >= cell[1] + CELL) {
                continue;
            }
            final ItemStack stack = switch (cell[2]) {
                case CRAFT_RESULT -> craftResult();
                case ANVIL_RESULT -> state.anvilResult();
                default -> state.slot(cell[3]);
            };
            if (!stack.isEmpty()) {
                g.renderComponentTooltip(font, Screen.getTooltipFromItem(Minecraft.getInstance(), stack), mouseX,
                        mouseY);
            }
            return;
        }
        final int offer = offerAt(mouseX, mouseY);
        if (offer >= 0 && offer < state.offers().size()) {
            final WorkshopStatePayload.Offer shown = state.offers().get(offer);
            g.renderComponentTooltip(font, List.of(GameText.component(CLUE.with(GameText.of(shown.clue()))),
                    Component.literal(levelsText(shown.levels())).withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    // stations

    private void layoutCrafting(final int sx, final int sy, final int dx, final boolean show) {
        grid.place(sx + WorkshopLayout.GRID_X, sy + WorkshopLayout.GRID_Y);
        result.place(sx + WorkshopLayout.RESULT_X, sy + WorkshopLayout.RESULT_Y);
        grid.setVisible(show);
        result.setVisible(show);
        final int by = sy + WorkshopLayout.STATION_H - WorkshopLayout.BUTTON_H - PAD;
        final int bx = dx + PAD;
        final Font font = Minecraft.getInstance().font;
        final int craftW = Texts.smallWidth(font, craft.label()) + BUTTON_PAD;
        final int allW = Texts.smallWidth(font, craftAll.label()) + BUTTON_PAD;
        final int clearW = Texts.smallWidth(font, clearGrid.label()) + BUTTON_PAD;
        craft.setBounds(bx, by, craftW, WorkshopLayout.BUTTON_H);
        craftAll.setBounds(bx + craftW + BUTTON_GAP, by, allW, WorkshopLayout.BUTTON_H);
        clearGrid.setBounds(bx + craftW + allW + BUTTON_GAP * 2, by, clearW, WorkshopLayout.BUTTON_H);
        craft.setVisible(show);
        craftAll.setVisible(show);
        clearGrid.setVisible(show);
        craft.setEnabled(!craftResult().isEmpty());
        craftAll.setEnabled(!craftResult().isEmpty());
    }

    private void layoutFurnace(final int sx, final int sy, final boolean show) {
        furnaceIn.place(sx + WorkshopLayout.FURNACE_IN_X, sy + WorkshopLayout.FURNACE_IN_Y);
        furnaceOut.place(sx + WorkshopLayout.FURNACE_OUT_X, sy + WorkshopLayout.FURNACE_IN_Y);
        furnaceIn.setVisible(show);
        furnaceOut.setVisible(show);
    }

    private void layoutEnchanting(final int sx, final int sy, final boolean show) {
        enchantItem.place(sx + WorkshopLayout.ENCHANT_ITEM_X, sy + WorkshopLayout.ENCHANT_ITEM_Y);
        enchantItem.setVisible(show);
    }

    private void layoutAnvil(final int sx, final int sy, final int sw, final int dx, final boolean show) {
        final int fieldX = sx + WorkshopLayout.ANVIL_LEFT_X + WorkshopLayout.NAME_LABEL_W;
        anvilName.setBounds(fieldX, sy + WorkshopLayout.NAME_Y, sx + sw - PAD - fieldX, WorkshopLayout.FIELD_H);
        anvilLeft.place(sx + WorkshopLayout.ANVIL_LEFT_X, sy + WorkshopLayout.ANVIL_ROW_Y);
        anvilRight.place(sx + WorkshopLayout.ANVIL_RIGHT_X, sy + WorkshopLayout.ANVIL_ROW_Y);
        anvilResult.place(sx + WorkshopLayout.ANVIL_RESULT_X, sy + WorkshopLayout.ANVIL_ROW_Y);
        final Font font = Minecraft.getInstance().font;
        takeAnvil.setBounds(dx + PAD, sy + WorkshopLayout.STATION_H - WorkshopLayout.BUTTON_H - PAD,
                Texts.smallWidth(font, takeAnvil.label()) + BUTTON_PAD, WorkshopLayout.BUTTON_H);
        anvilName.setVisible(show);
        anvilLeft.setVisible(show);
        anvilRight.setVisible(show);
        anvilResult.setVisible(show);
        takeAnvil.setVisible(show);
        takeAnvil.setEnabled(state != null && !state.anvilResult().isEmpty() && affordable(state.anvilCost()));
    }

    /* What only drawing can show on a station: arrows, notes, the offers. */
    private void drawStation(final GuiGraphics g, final Font font, final WorkshopCard card, final int sx,
                             final int sy, final int sw, final int mouseX, final int mouseY) {
        switch (card) {
            case CRAFTING_TABLE -> arrow(g, sx + WorkshopLayout.GRID_X + 3 * CELL + 6,
                    sy + WorkshopLayout.RESULT_Y + 5, 18, craftResult().isEmpty() ? 0f : 1f);
            case FURNACE -> {
                arrow(g, sx + WorkshopLayout.FURNACE_ARROW_X, sy + WorkshopLayout.FURNACE_IN_Y + 5,
                        WorkshopLayout.FURNACE_ARROW_W, furnaceFraction());
                note(g, font, sx, sy, WorkshopLayout.FURNACE_IN_X, NO_FUEL, RUNS_ON_COMPUTER);
            }
            case ENCHANTING -> {
                note(g, font, sx, sy, WorkshopLayout.ENCHANT_ITEM_X, NO_LAPIS, NEEDS_NONE);
                drawOffers(g, font, sx, sy, sw, mouseX, mouseY);
            }
            case ANVIL -> {
                Draw.text(g, font, GameText.resolve(NAME), sx + WorkshopLayout.ANVIL_LEFT_X,
                        sy + WorkshopLayout.NAME_Y + 2, skin.text());
                Draw.text(g, font, "+", sx + WorkshopLayout.ANVIL_LEFT_X + CELL + 5,
                        sy + WorkshopLayout.ANVIL_ROW_Y + 5, skin.text());
                arrow(g, sx + WorkshopLayout.ANVIL_ARROW_X, sy + WorkshopLayout.ANVIL_ROW_Y + 5,
                        WorkshopLayout.ANVIL_ARROW_W, state.anvilResult().isEmpty() ? 0f : 1f);
            }
        }
    }

    /* The two-line note centred under a station's one cell, where the fuel or the lapis would have gone. */
    private void note(final GuiGraphics g, final Font font, final int sx, final int sy, final int cellX,
                      final TextKey bold, final TextKey line) {
        final int cx = sx + cellX + CELL / 2;
        final String first = GameText.resolve(bold);
        final String second = GameText.resolve(line);
        Texts.small(g, font, first, Math.max(sx + 2, cx - Texts.smallWidth(font, first) / 2),
                sy + WorkshopLayout.NOTE_Y, skin.text());
        Texts.small(g, font, second, Math.max(sx + 2, cx - Texts.smallWidth(font, second) / 2),
                sy + WorkshopLayout.NOTE_Y + WorkshopLayout.BODY_LINE, skin.dim());
    }

    private void drawOffers(final GuiGraphics g, final Font font, final int sx, final int sy, final int sw,
                            final int mouseX, final int mouseY) {
        final int ow = WorkshopLayout.offerWidth(sw);
        for (int i = 0; i < OFFERS; i++) {
            final int ox = sx + WorkshopLayout.OFFERS_X;
            final int oy = sy + WorkshopLayout.offerY(i);
            final WorkshopStatePayload.Offer offer = i < state.offers().size() ? state.offers().get(i) : null;
            final boolean there = offer != null && offer.required() > 0;
            final boolean can = there && affordable(Math.max(offer.required(), offer.levels()));
            final boolean over = can && mouseX >= ox && mouseX < ox + ow && mouseY >= oy
                    && mouseY < oy + WorkshopLayout.OFFER_H;
            skin.field(g, ox, oy, ow, WorkshopLayout.OFFER_H, over);
            if (!there) {
                continue;
            }
            g.blitSprite(ORBS[i], ox + 3, oy + 3, 16, 16);
            final int ink = can ? skin.text() : skin.dim();
            final String needed = Integer.toString(offer.required());
            Draw.text(g, font, needed, ox + 22, oy + 7, can ? PALETTE.get().working() : skin.dim());
            final String cost = levelsText(offer.levels());
            final int costW = Texts.smallWidth(font, cost);
            Texts.small(g, font, cost, ox + ow - costW - 4, oy + 8, ink);
            final int clueX = ox + 24 + font.width(needed) + 4;
            final String clue = GameText.resolve(CLUE.with(GameText.of(offer.clue())));
            Texts.small(g, font, Texts.clip(font, clue, Texts.smallFits(ox + ow - costW - 8 - clueX)), clueX, oy + 8,
                    ink);
        }
    }

    // details

    private void drawDetails(final GuiGraphics g, final Font font, final WorkshopCard card, final int dx,
                             final int sy) {
        final ItemStack subject = subject(card);
        final ResourceLocation icon = SkinSprites.find("device", card.id(), card.id(), skin.iconSet());
        if (SkinSprites.exists(icon)) {
            SkinSprites.draw(g, icon, dx + PAD, sy + PAD + 1, 16, 16, 16);
        }
        final int tx = dx + PAD + 20;
        final int tw = WorkshopLayout.DETAILS_W - 20 - PAD * 2;
        final String title = subject.isEmpty() ? GameText.resolve(stationName(card))
                : subject.getHoverName().getString();
        Draw.text(g, font, Texts.clip(font, title, tw), tx, sy + PAD + 1, skin.text());
        Texts.small(g, font, GameText.resolve(cardName(card)), tx, sy + PAD + 11, skin.accent());
        final Lines lines = new Lines(g, font, dx + PAD, sy + WorkshopLayout.HEADER_H,
                WorkshopLayout.DETAILS_W - PAD * 2);
        switch (card) {
            case CRAFTING_TABLE -> {
                lines.key(RECIPE);
                lines.value(recipeLine(), skin.text());
                lines.key(IN_INVENTORY);
                lines.value(GameText.resolve(state.craftMore() > 0 ? ENOUGH_FOR.with(state.craftMore())
                        : ENOUGH_FOR_NONE.text()), skin.text());
            }
            case FURNACE -> {
                lines.key(SMELTING);
                final ItemStack input = state.slot(Workshop.FURNACE_IN);
                final ItemStack output = state.slot(Workshop.FURNACE_OUT);
                lines.value(input.isEmpty() || state.smeltsInto().isEmpty() ? GameText.resolve(NOTHING_SMELTING)
                        : GameText.resolve(SMELTING_LINE.with(GameText.of(input.getHoverName()),
                        GameText.of(state.smeltsInto().getHoverName()), input.getCount(), output.getCount())),
                        skin.text());
                lines.key(SPEED);
                lines.value(speedLine(input.getCount()), skin.text());
                lines.key(WINDOW_CLOSED);
                lines.value(GameText.resolve(WINDOW_CLOSED_NOTE), skin.dim());
            }
            case ENCHANTING -> {
                lines.key(YOUR_EXPERIENCE);
                lines.value(GameText.resolve(LEVEL.with(state.level())), PALETTE.get().working());
                lines.key(THE_OFFERS);
                lines.value(GameText.resolve(OFFERS_NOTE), skin.text());
                lines.key(WHAT_IT_TAKES);
                lines.value(GameText.resolve(ENCHANT_COST), skin.text());
                lines.value(GameText.resolve(ENCHANT_WAS), skin.dim());
            }
            case ANVIL -> {
                lines.key(REPAIR_RENAME);
                lines.value(anvilLine(), skin.text());
                if (!state.anvilResult().isEmpty()) {
                    lines.key(WHAT_IT_TAKES);
                    lines.value(levelsText(state.anvilCost()), affordable(state.anvilCost()) ? skin.text()
                            : PALETTE.get().lacking());
                    lines.value(GameText.resolve(ANVIL_WAS.with(state.anvilLevels())), skin.dim());
                }
            }
        }
    }

    private String recipeLine() {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (int i = Workshop.GRID; i < Workshop.GRID + Workshop.GRID_SIZE; i++) {
            final ItemStack cell = state.slot(i);
            if (!cell.isEmpty()) {
                counts.merge(cell.getHoverName().getString(), 1, Integer::sum);
            }
        }
        if (counts.isEmpty()) {
            return GameText.resolve(EMPTY_GRID);
        }
        if (craftResult().isEmpty()) {
            return GameText.resolve(NO_RESULT);
        }
        final List<String> parts = new ArrayList<>();
        counts.forEach((name, count) -> parts.add(GameText.resolve(INGREDIENT.with(count, name))));
        return String.join(", ", parts);
    }

    private String speedLine(final int left) {
        if (state.ticksPerItem() <= 0) {
            return GameText.resolve(SPEED_ONLY.with(state.furnaceSpeed()));
        }
        final double each = state.ticksPerItem() / (double) TICKS_PER_SECOND;
        final int totalTicks = Math.max(0, left * state.ticksPerItem() - state.progressTicks());
        final int seconds = (totalTicks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND;
        final String eachText = each == Math.floor(each) ? Integer.toString((int) each)
                : String.format(Locale.ROOT, "%.1f", each);
        return GameText.resolve(SPEED_LINE.with(state.furnaceSpeed(), eachText,
                String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)));
    }

    private String anvilLine() {
        final ItemStack left = state.slot(Workshop.ANVIL_LEFT);
        if (left.isEmpty()) {
            return GameText.resolve(ANVIL_EMPTY);
        }
        if (state.anvilResult().isEmpty()) {
            return GameText.resolve(ANVIL_NOTHING);
        }
        final ItemStack right = state.slot(Workshop.ANVIL_RIGHT);
        return right.isEmpty() ? left.getHoverName().getString()
                : GameText.resolve(ANVIL_WITH.with(GameText.of(left.getHoverName()),
                GameText.resolve(INGREDIENT.with(right.getCount(), right.getHoverName().getString()))));
    }

    // the band and the status bar

    private void drawBand(final GuiGraphics g, final Font font, final int bx, final int by) {
        Draw.text(g, font, GameText.resolve(INVENTORY), bx + 2, by - 9, skin.dim());
        skin.panel(g, bx, by, WorkshopLayout.BAND_W, WorkshopLayout.BAND_H);
        for (int r = 0; r < WorkshopLayout.INV_ROWS; r++) {
            for (int c = 0; c < WorkshopLayout.INV_COLS; c++) {
                final int cx = bx + WorkshopLayout.BAND_PAD + c * CELL;
                final int cy = by + WorkshopLayout.BAND_PAD + WorkshopLayout.rowYOffset(r);
                g.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, skin.fieldBg());
                Draw.outline(g, cx, cy, CELL - 2, CELL - 2, skin.edge());
            }
        }
    }

    private void drawStatusBar(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        skin.statusBar(g, x, y, width, WorkshopLayout.STATUS_H);
        if (state == null) {
            return;
        }
        final int ty = y + 3;
        final String level = GameText.resolve(LEVEL.with(state.level()));
        final int levelX = x + width - PAD - 2 - Texts.smallWidth(font, level);
        Texts.small(g, font, level, levelX, ty, skin.text());
        int at = x + PAD + 2;
        final String cards = status.isEmpty() ? cardsLine() : status;
        Texts.small(g, font, Texts.clip(font, cards, Texts.smallFits(levelX - at - 8)), at, ty, skin.text());
        if (!status.isEmpty() || !WorkshopCard.FURNACE.in(state.cards())) {
            return;
        }
        at += Texts.smallWidth(font, cards) + 12;
        final ItemStack input = state.slot(Workshop.FURNACE_IN);
        final boolean smelting = state.ticksPerItem() > 0;
        final String furnace = smelting ? GameText.resolve(FURNACE_SMELTING.with(state.furnaceSpeed()))
                : input.isEmpty() ? GameText.resolve(FURNACE_IDLE)
                : GameText.resolve(FURNACE_LEFT.with(input.getCount(), input.getHoverName().getString()));
        if (at + Texts.smallWidth(font, furnace) < levelX - 8) {
            Texts.small(g, font, furnace, at, ty, smelting ? PALETTE.get().working() : skin.dim());
        }
    }

    private String cardsLine() {
        final List<String> names = new ArrayList<>();
        for (final WorkshopCard card : WorkshopCard.values()) {
            if (card.in(state.cards())) {
                names.add(GameText.resolve(stationName(card)));
            }
        }
        return names.isEmpty() ? GameText.resolve(NO_CARDS) : GameText.resolve(CARDS.with(String.join(", ", names)));
    }

    // helpers

    private CellGrid cells(final int side, final int first) {
        return new CellGrid(side, side, side, CELL)
                .setWells(true)
                .setRenderer((g, ctx, i, cx, cy, w, h, over) -> drawStack(g, ctx,
                        state == null ? ItemStack.EMPTY : state.slot(first + i), cx, cy, SLOT, first + i))
                .setOnClick((i, button, shift) -> PacketDistributor.sendToServer(new WorkshopActionPayload(host,
                        monitorPos, WorkshopActionPayload.CLICK, first + i, button, "")));
    }

    private void drawStack(final GuiGraphics g, final UiContext ctx, final ItemStack stack, final int cx,
                           final int cy, final int kind, final int index) {
        drawnCells.add(new int[] {cx, cy, kind, index});
        if (!stack.isEmpty()) {
            DesktopItems.itemWithCount(g, ctx.font(), stack, cx + 1, cy + 1, null);
        }
    }

    /* An arrow {@code w} wide, filled from the left by {@code fraction}. */
    private void arrow(final GuiGraphics g, final int x, final int y, final int w, final float fraction) {
        final int head = 5;
        final int shaft = w - head;
        final int track = skin.edge();
        final int fill = PALETTE.get().working();
        final int filled = Math.round(Math.max(0f, Math.min(1f, fraction)) * w);
        for (int i = 0; i < w; i++) {
            final int colour = i < filled ? fill : track;
            if (i < shaft) {
                g.fill(x + i, y + 2, x + i + 1, y + 6, colour);
            } else {
                final int rise = 4 - (i - shaft);
                g.fill(x + i, y + 4 - rise, x + i + 1, y + 4 + rise, colour);
            }
        }
    }

    private float furnaceFraction() {
        return state.ticksPerItem() <= 0 ? 0f : Math.min(1f, state.progressTicks() / (float) state.ticksPerItem());
    }

    private ItemStack craftResult() {
        return state == null ? ItemStack.EMPTY : state.craftResult();
    }

    /* How many the grid and the inventory make together. */
    private int craftTotal() {
        return state == null || state.craftResult().isEmpty() ? 0 : state.craftMore() + 1;
    }

    private ItemStack subject(final WorkshopCard card) {
        return switch (card) {
            case CRAFTING_TABLE -> craftResult();
            case FURNACE -> ItemStack.EMPTY;
            case ENCHANTING -> state.slot(Workshop.ENCHANT_ITEM);
            case ANVIL -> state.slot(Workshop.ANVIL_LEFT);
        };
    }

    private boolean affordable(final int levels) {
        final var player = Minecraft.getInstance().player;
        return player != null && (player.getAbilities().instabuild || state.level() >= levels);
    }

    private int offerAt(final double mouseX, final double mouseY) {
        if (state == null || cardOf(tab) != WorkshopCard.ENCHANTING
                || !WorkshopCard.ENCHANTING.in(state.cards())) {
            return -1;
        }
        final int ox = lastX + PAD + WorkshopLayout.OFFERS_X;
        final int ow = WorkshopLayout.offerWidth(WorkshopLayout.stationWidth(lastW));
        for (int i = 0; i < OFFERS; i++) {
            final int oy = lastY + WorkshopLayout.STATION_TOP + WorkshopLayout.offerY(i);
            if (mouseX >= ox && mouseX < ox + ow && mouseY >= oy && mouseY < oy + WorkshopLayout.OFFER_H
                    && i < state.offers().size() && state.offers().get(i).required() > 0) {
                return i;
            }
        }
        return -1;
    }

    private void send(final int action, final int index) {
        PacketDistributor.sendToServer(WorkshopActionPayload.of(host, monitorPos, action, index));
    }

    private static String levelsText(final int levels) {
        return levels == 1 ? GameText.resolve(WorkshopAppTexts.ONE_LEVEL) : GameText.resolve(LEVELS.with(levels));
    }

    private static int firstCard(final int cards, final int fallback) {
        for (final WorkshopCard card : WorkshopCard.values()) {
            if (card.in(cards)) {
                return card.index();
            }
        }
        return fallback;
    }

    /* The card of tab {@code index}; the first one for an index past the tabs. */
    private static WorkshopCard cardOf(final int index) {
        final WorkshopCard card = WorkshopCard.byIndex(index);
        return card == null ? WorkshopCard.CRAFTING_TABLE : card;
    }

    /* Whether {@code cards} holds the card of tab {@code index}. */
    private static boolean holds(final int cards, final int index) {
        final WorkshopCard card = WorkshopCard.byIndex(index);
        return card != null && card.in(cards);
    }

    private static TextKey cardName(final WorkshopCard card) {
        return switch (card) {
            case CRAFTING_TABLE -> CRAFTING_TABLE_CARD;
            case FURNACE -> FURNACE_CARD;
            case ENCHANTING -> ENCHANTING_CARD;
            case ANVIL -> ANVIL_CARD;
        };
    }

    private static TextKey stationName(final WorkshopCard card) {
        return switch (card) {
            case CRAFTING_TABLE -> CRAFTING_TABLE;
            case FURNACE -> FURNACE;
            case ENCHANTING -> ENCHANTING;
            case ANVIL -> ANVIL;
        };
    }

    /* The details panel's lines, down from the header: a key in small capitals, then its value, wrapped. */
    private final class Lines {

        private final GuiGraphics g;
        private final Font font;
        private final int x;
        private final int width;
        private int y;

        private Lines(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
            this.g = g;
            this.font = font;
            this.x = x;
            this.y = y;
            this.width = width;
        }

        private void key(final TextKey key) {
            Texts.small(g, font, GameText.resolve(key), x, y, skin.dim());
            y += WorkshopLayout.BODY_LINE;
        }

        private void value(final String text, final int colour) {
            final String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            for (final String word : words) {
                final String tried = line.isEmpty() ? word : line + " " + word;
                if (Texts.smallWidth(font, tried) > width && !line.isEmpty()) {
                    Texts.small(g, font, line.toString(), x, y, colour);
                    y += WorkshopLayout.BODY_LINE;
                    line = new StringBuilder(word);
                } else {
                    line = new StringBuilder(tried);
                }
            }
            if (!line.isEmpty()) {
                Texts.small(g, font, line.toString(), x, y, colour);
                y += WorkshopLayout.BODY_LINE;
            }
            y += 1;
        }
    }

    private record Colours(int working, int lacking) {
    }
}
