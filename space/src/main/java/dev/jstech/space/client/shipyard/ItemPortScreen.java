/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.space.JsSpace;
import dev.jstech.space.gui.ItemPortLayout;
import dev.jstech.space.hangar.HangarTexts;
import dev.jstech.space.hangar.ItemPortBlockEntity;
import dev.jstech.space.hangar.ItemPortMenu;
import dev.jstech.space.hangar.ItemPortView;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Item Port's screen, as the approved one draws it: the machine's steel face with a display across its top, the
 * ship the dock builds and how many of its blocks are laid, and under it what the ship still lacks, a block a row
 * with its count and its bar, a row the dock and the port have enough of dimmed and marked ok, the list scrolling
 * under the mouse's wheel; the port's hold under the display, and the player's inventory. A block the ship does not
 * lack, held over the hold, says so.
 */
public final class ItemPortScreen extends AbstractContainerScreen<ItemPortMenu> {

    private int scrolled;
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/gui/item_port.png");
    private static final int SHEET = 256;
    private static final float SMALL = 0.75F;
    /* The space kept between a name cut short and what stands right of it. */
    private static final int GAP = 4;

    public ItemPortScreen(final ItemPortMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = ItemPortLayout.WIDTH;
        this.imageHeight = ItemPortLayout.HEIGHT;
        this.titleLabelX = ItemPortLayout.TITLE_X;
        this.titleLabelY = ItemPortLayout.TITLE_Y;
        this.inventoryLabelX = ItemPortLayout.TITLE_X;
        this.inventoryLabelY = ItemPortLayout.INVENTORY_LABEL_Y;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        final Slot hovered = this.hoveredSlot;
        final ItemStack carried = this.menu.getCarried();
        if (hovered != null && hovered.index < ItemPortBlockEntity.PLACES && !carried.isEmpty()
                && !this.menu.view().takes(carried.getItem())) {
            final ItemPortView view = this.menu.view();
            final Component why = view.building() ? GameText.component(HangarTexts.PORT_REFUSES.with(view.ship()))
                    : GameText.component(HangarTexts.PORT_IDLE);
            graphics.renderComponentTooltip(this.font, List.of(carried.getHoverName(),
                    why.copy().withStyle(ChatFormatting.RED)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final int most = Math.max(0, this.menu.view().rows().size() - ItemPortLayout.ROWS_SHOWN);
        this.scrolled = Mth.clamp(this.scrolled - (int) Math.signum(scrollY), 0, most);
        return true;
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float partialTick, final int mouseX,
                            final int mouseY) {
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, SHEET, SHEET);
        display(graphics);
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        small(graphics, GameText.component(HangarTexts.PORT_HOLD).getString().toUpperCase(Locale.ROOT),
                ItemPortLayout.HOLD_LABEL_X, ItemPortLayout.HOLD_LABEL_Y, ItemPortPalette.PALETTE.get().stamp());
    }

    /* The display: the ship and its count over its bar, then what it lacks, a row each, and the scroll bar. */
    private void display(final GuiGraphics graphics) {
        final ItemPortPalette.Colours c = ItemPortPalette.PALETTE.get();
        final ItemPortView view = this.menu.view();
        final int x0 = this.leftPos + ItemPortLayout.DISPLAY_X;
        final int y0 = this.topPos + ItemPortLayout.DISPLAY_Y;
        final int w = ItemPortLayout.DISPLAY_W;
        final int m = ItemPortLayout.MARGIN;
        if (!view.building()) {
            small(graphics, GameText.component(HangarTexts.PORT_IDLE).getString(), x0 + m, y0 + m, c.greenDim());
            return;
        }
        // A long name is cut short of what stands right of it, so the two never run into each other.
        final String laid = GameText.component(HangarTexts.PORT_BLOCKS.with(view.laid(), view.blocks())).getString();
        small(graphics, fitted(view.ship(), w - 2 * m - smallWidth(laid) - GAP), x0 + m, y0 + m, c.green());
        smallRight(graphics, laid, x0 + w - m, y0 + m, c.green());
        bar(graphics, x0 + m, y0 + ItemPortLayout.BAR_Y, w - 2 * m, share(view.laid(), view.blocks()), c.green(),
                c.track());
        final List<ItemPortView.Row> rows = view.rows();
        this.scrolled = Mth.clamp(this.scrolled, 0, Math.max(0, rows.size() - ItemPortLayout.ROWS_SHOWN));
        for (int k = 0; k < ItemPortLayout.ROWS_SHOWN && this.scrolled + k < rows.size(); k++) {
            final ItemPortView.Row row = rows.get(this.scrolled + k);
            final int y = y0 + ItemPortLayout.LIST_Y + k * ItemPortLayout.ROW;
            final boolean done = row.enough();
            graphics.renderItem(new ItemStack(row.item()), x0 + m, y);
            String count = GameText.component(HangarTexts.PORT_ROW.with(row.have(), row.lacking())).getString();
            if (done) {
                count += "  " + GameText.component(HangarTexts.PORT_OK).getString();
            }
            final int room = w - 9 - ItemPortLayout.NAME_X - smallWidth(count) - GAP;
            small(graphics, fitted(row.item().getDescription().getString(), room), x0 + ItemPortLayout.NAME_X, y + 1,
                    done ? c.greenDim() : c.green());
            smallRight(graphics, count, x0 + w - 9, y + 1, c.greenDim());
            bar(graphics, x0 + ItemPortLayout.NAME_X, y + ItemPortLayout.ROW_BAR_Y, w - ItemPortLayout.NAME_X - 12,
                    share(row.have(), row.lacking()), done ? c.greenDim() : c.green(), c.track());
        }
        // The list's scroll bar: its track, and as much of it as the rows shown are of all.
        final int sx = x0 + w - m - ItemPortLayout.SCROLL_W;
        final int top = y0 + ItemPortLayout.LIST_Y;
        final int track = ItemPortLayout.DISPLAY_H - ItemPortLayout.LIST_Y - m;
        graphics.fill(sx, top, sx + ItemPortLayout.SCROLL_W, top + track, c.track());
        if (!rows.isEmpty()) {
            final int thumb = Math.max(4, track * Math.min(ItemPortLayout.ROWS_SHOWN, rows.size()) / rows.size());
            final int from = rows.size() <= ItemPortLayout.ROWS_SHOWN ? 0
                    : (track - thumb) * this.scrolled / (rows.size() - ItemPortLayout.ROWS_SHOWN);
            graphics.fill(sx, top + from, sx + ItemPortLayout.SCROLL_W, top + from + thumb, c.greenDim());
        }
    }

    /* A bar of the display: its track, and {@code share} of it filled. */
    private static void bar(final GuiGraphics graphics, final int x, final int y, final int width,
                            final double share, final int colour, final int track) {
        graphics.fill(x, y, x + width, y + ItemPortLayout.BAR_H, track);
        graphics.fill(x, y, x + (int) Math.round(width * Math.clamp(share, 0, 1)), y + ItemPortLayout.BAR_H,
                colour);
    }

    private static double share(final int part, final int whole) {
        return whole <= 0 ? 0 : part / (double) whole;
    }

    /* The small text the display is written in, from its left. */
    private void small(final GuiGraphics graphics, final String text, final int x, final int y, final int colour) {
        Draw.textScaled(graphics, this.font, text, x, y, colour, SMALL);
    }

    /* The small text, ending at {@code right}. */
    private void smallRight(final GuiGraphics graphics, final String text, final int right, final int y,
                            final int colour) {
        small(graphics, text, right - smallWidth(text), y, colour);
    }

    /* How wide {@code text} is in the small text, in the screen's pixels. */
    private int smallWidth(final String text) {
        return Math.round(this.font.width(text) * SMALL);
    }

    /* As much of {@code text} as fits {@code room} of the screen's pixels in the small text. */
    private String fitted(final String text, final int room) {
        return this.font.plainSubstrByWidth(text, Math.max(0, (int) (room / SMALL)));
    }
}
