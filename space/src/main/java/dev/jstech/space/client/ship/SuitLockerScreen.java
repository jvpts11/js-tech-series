/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.gui.SuitLockerLayout;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.space.life.SuitSupplies;
import dev.jstech.space.ship.ShipFixtureTexts;
import dev.jstech.space.ship.SuitLockerEntity;
import dev.jstech.space.ship.SuitLockerMenu;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

/**
 * The suit locker's screen, as the approved one draws it: the locker open on the airlock's wall, the four bottles'
 * cradles on its shelf, the helmet, the suit, the trousers and the boots down its rack, the kit box at its foot; an
 * empty place shows its thing faintly, as the game's armour places do, and its hint says what it takes; the player's
 * inventory under it.
 */
public final class SuitLockerScreen extends AbstractContainerScreen<SuitLockerMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/gui/suit_locker.png");
    private static final ResourceLocation GHOSTS = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/gui/suit_locker_ghosts.png");
    private static final int SHEET = 256;
    private static final int ICON = 16;
    private static final int GHOST_SHEET_WIDTH = 96;
    /* Which ghost each place shows, and what its hint says it takes. */
    private static final Map<SuitLockerEntity.Place, Integer> GHOST = Map.of(SuitLockerEntity.Place.HELMET, 0,
            SuitLockerEntity.Place.SUIT, 1, SuitLockerEntity.Place.TROUSERS, 2, SuitLockerEntity.Place.BOOTS, 3,
            SuitLockerEntity.Place.BOTTLE, 4, SuitLockerEntity.Place.KIT, 5);
    private static final Map<SuitLockerEntity.Place, TextKey> TAKES = Map.of(
            SuitLockerEntity.Place.HELMET, ShipFixtureTexts.TAKES_HELMET,
            SuitLockerEntity.Place.SUIT, ShipFixtureTexts.TAKES_SUIT,
            SuitLockerEntity.Place.TROUSERS, ShipFixtureTexts.TAKES_TROUSERS,
            SuitLockerEntity.Place.BOOTS, ShipFixtureTexts.TAKES_BOOTS,
            SuitLockerEntity.Place.BOTTLE, ShipFixtureTexts.TAKES_BOTTLE,
            SuitLockerEntity.Place.KIT, ShipFixtureTexts.TAKES_KIT);

    public SuitLockerScreen(final SuitLockerMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = SuitLockerLayout.WIDTH;
        this.imageHeight = SuitLockerLayout.HEIGHT;
        this.titleLabelX = SuitLockerLayout.TITLE_X;
        this.titleLabelY = SuitLockerLayout.TITLE_Y;
        this.inventoryLabelX = SuitLockerLayout.TITLE_X;
        this.inventoryLabelY = SuitLockerLayout.INVENTORY_LABEL_Y;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        final Slot hovered = this.hoveredSlot;
        if (hovered != null && !hovered.hasItem() && this.menu.getCarried().isEmpty()
                && hovered.index < SuitLockerEntity.PLACES.size()) {
            final SuitLockerEntity.Place place = this.menu.place(hovered.index);
            graphics.renderComponentTooltip(this.font, List.of(shown(place).getDescription(),
                    GameText.component(TAKES.get(place)).copy().withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX,
                            final int mouseY) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, SHEET, SHEET);
        for (int k = 0; k < SuitLockerEntity.PLACES.size(); k++) {
            if (this.menu.getSlot(k).hasItem()) {
                continue;
            }
            final int[] at = SuitLockerLayout.PLACES.get(k);
            graphics.blit(GHOSTS, this.leftPos + at[0], this.topPos + at[1], GHOST.get(this.menu.place(k)) * ICON,
                    0, ICON, ICON, GHOST_SHEET_WIDTH, ICON);
        }
    }

    /* The thing an empty place shows faintly, whose name its hint gives. */
    private static Item shown(final SuitLockerEntity.Place place) {
        return switch (place) {
            case HELMET -> LunarSuit.HELMET.get();
            case SUIT -> LunarSuit.SUIT.get();
            case TROUSERS -> LunarSuit.TROUSERS.get();
            case BOOTS -> LunarSuit.BOOTS.get();
            case BOTTLE -> SuitSupplies.OXYGEN_BOTTLE.get();
            case KIT -> SuitSupplies.SUIT_PATCH.get();
        };
    }
}
