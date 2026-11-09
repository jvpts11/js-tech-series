/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.WorkbenchBlockEntity;
import dev.jstech.computers.gui.layout.WorkbenchLayout;
import dev.jstech.computers.menu.WorkbenchMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * An Electronics Workbench's screen, as the approved design draws it: the bench's own top above, looking like the bench
 * of its era, over the game's inventory. Its picture holds what never changes (the top's edge, its mat and grid, the
 * era's tools and gear, the trays, the groove, the part's tray and the power strip); on it the screen draws the name
 * plate and its words, what the bench makes, the strip's lamp lit while energy reaches it, what it draws and the energy
 * it holds, the groove filling in the colour of the era's tool as the part comes, and the part faded in its tray until
 * it is done.
 */
@PaletteHolder
public final class WorkbenchScreen extends CoreContainerScreen<WorkbenchMenu> {

    /** The colours drawn on every bench's picture. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "screen/workbench",
            new Colours(0xFF5FE07A, 0xFFC8FFD0, 0xFFF0C030, 0xFFFFE080, 0x8C8B8B8B, 0xFF404040));
    /** Each era's: its plate, the words on it and along its edge, and the glow of its tool. */
    public static final Palette<EraLook> VINTAGE_LOOK = Palettes.declare(JsComputers.MODID,
            "screen/workbench_vintage", new EraLook(0xFFD8CDB0, 0xFF767060, 0xFF3A3020, 0xFF372717, 0xFFE05030,
                    0xFFEFA797));
    public static final Palette<EraLook> LEGACY_LOOK = Palettes.declare(JsComputers.MODID,
            "screen/workbench_legacy", new EraLook(0xFFC4C9D0, 0xFF6B6E72, 0xFF2A2E34, 0xFF242628, 0xFFE05030,
                    0xFFEFA797));
    public static final Palette<EraLook> TRANSITION_LOOK = Palettes.declare(JsComputers.MODID,
            "screen/workbench_transition", new EraLook(0xFF9AA0A8, 0xFF54585C, 0xFF1C1D20, 0xFFCDCED0, 0xFFFFA040,
                    0xFFFFCF9F));
    public static final Palette<EraLook> STANDARD_LOOK = Palettes.declare(JsComputers.MODID,
            "screen/workbench_standard", new EraLook(0xFF2A2C31, 0xFF17181A, 0xFFE8E8E6, 0xFF484847, 0xFF8AB0D0,
                    0xFFC4D7E7));
    public static final Palette<EraLook> ADVANCED_LOOK = Palettes.declare(JsComputers.MODID,
            "screen/workbench_advanced", new EraLook(0xFFEEF0F2, 0xFF828485, 0xFF3A3D42, 0xFF404243, 0xFFEAF6FF,
                    0xFFF4FAFF));

    /* The part's veil sits over the items the screen draws. */
    private static final int OVER_ITEMS = 200;
    private static final int LAMP_RADIUS = 2;
    private static final int TEXTURE_SIZE_X = WorkbenchLayout.WIDTH;
    private static final int TEXTURE_SIZE_Y = WorkbenchLayout.HEIGHT;

    public WorkbenchScreen(final WorkbenchMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = WorkbenchLayout.WIDTH;
        this.imageHeight = WorkbenchLayout.HEIGHT;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final Era era = this.menu.era();
        final EraLook look = look(era);
        Draw.blended(() -> g.blit(picture(era), this.leftPos, this.topPos, 0, 0, WorkbenchLayout.WIDTH,
                WorkbenchLayout.HEIGHT, TEXTURE_SIZE_X, TEXTURE_SIZE_Y));
        plate(g, look);
        strip(g, PALETTE.get(), era);
        groove(g, look);
    }

    /* The words on the top and the inventory's; the bench's name is on its plate, drawn with the top. */
    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        final Era era = this.menu.era();
        Draw.textScaled(g, this.font, GameText.resolve(makes(era)), WorkbenchLayout.MAKES_X,
                WorkbenchLayout.MAKES_Y, look(era).makes(), WorkbenchLayout.SMALL);
        Draw.text(g, this.font, this.playerInventoryTitle, WorkbenchLayout.INVENTORY_WORD_X,
                WorkbenchLayout.INVENTORY_WORD_Y, PALETTE.get().inventoryWord());
        part(g);
    }

    /* The colours of the bench of {@code era}. */
    private static EraLook look(final Era era) {
        if (era.isAtMost(HardwareEra.VINTAGE)) {
            return VINTAGE_LOOK.get();
        }
        if (era.isAtMost(HardwareEra.LEGACY)) {
            return LEGACY_LOOK.get();
        }
        if (era.isAtMost(HardwareEra.TRANSITION)) {
            return TRANSITION_LOOK.get();
        }
        return era.isAtMost(HardwareEra.STANDARD) ? STANDARD_LOOK.get() : ADVANCED_LOOK.get();
    }

    /* The picture of the bench of {@code era}. */
    private static ResourceLocation picture(final Era era) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/workbench/"
                + HardwareEra.withParts(era).path() + ".png");
    }

    /* What a bench of {@code era} makes, along its front edge. */
    private static TextKey makes(final Era era) {
        if (era.isAtMost(HardwareEra.VINTAGE)) {
            return WorkbenchTexts.MAKES_VINTAGE;
        }
        if (era.isAtMost(HardwareEra.LEGACY)) {
            return WorkbenchTexts.MAKES_LEGACY;
        }
        if (era.isAtMost(HardwareEra.TRANSITION)) {
            return WorkbenchTexts.MAKES_TRANSITION;
        }
        return era.isAtMost(HardwareEra.STANDARD) ? WorkbenchTexts.MAKES_STANDARD : WorkbenchTexts.MAKES_ADVANCED;
    }

    /* The name plate riveted to the top's edge, as wide as the bench's name, and the name on it. */
    private void plate(final GuiGraphics g, final EraLook look) {
        final String name = this.title.getString();
        final int width = Math.round(this.font.width(name) * WorkbenchLayout.SMALL) + 2 * WorkbenchLayout.PLATE_PADDING;
        final int x = this.leftPos + WorkbenchLayout.PLATE_X;
        final int y = this.topPos + WorkbenchLayout.PLATE_Y;
        g.fill(x, y, x + width, y + WorkbenchLayout.PLATE_HEIGHT, look.plate());
        Draw.outline(g, x, y, width, WorkbenchLayout.PLATE_HEIGHT, look.plateEdge());
        Draw.textScaled(g, this.font, name, this.leftPos + WorkbenchLayout.TITLE_X,
                this.topPos + WorkbenchLayout.TITLE_Y, look.ink(), WorkbenchLayout.SMALL);
    }

    /* The power strip: its lamp lit while energy reaches the bench, its draw, and the bar of the energy it holds. */
    private void strip(final GuiGraphics g, final Colours c, final Era era) {
        if (this.menu.powered()) {
            disc(g, this.leftPos + WorkbenchLayout.LAMP_X, this.topPos + WorkbenchLayout.LAMP_Y, c.lamp());
            final int glint = this.leftPos + WorkbenchLayout.LAMP_X - 1;
            g.fill(glint, this.topPos + WorkbenchLayout.LAMP_Y - 1, glint + 1, this.topPos + WorkbenchLayout.LAMP_Y,
                    c.lampGlint());
        }
        final String watts = GameText.resolve(WorkbenchTexts.WATTS.with(WorkbenchBlockEntity.watts(era)));
        final int textWidth = Math.round(this.font.width(watts) * WorkbenchLayout.SMALL);
        Draw.textScaled(g, this.font, watts, this.leftPos + WorkbenchLayout.WATTS_RIGHT - textWidth,
                this.topPos + WorkbenchLayout.WATTS_Y, c.lamp(), WorkbenchLayout.SMALL);
        final int x0 = this.leftPos + WorkbenchLayout.BAR_X;
        final int x1 = this.leftPos + WorkbenchLayout.BAR_RIGHT;
        final int bottom = this.topPos + WorkbenchLayout.BAR_BOTTOM;
        final int top = bottom - (int) ((WorkbenchLayout.BAR_BOTTOM - WorkbenchLayout.BAR_Y - 1) * this.menu.stored());
        for (int y = bottom - WorkbenchLayout.BAR_SEGMENT; y >= top; y -= WorkbenchLayout.BAR_SEGMENT) {
            g.fill(x0 + 1, y, x1 - 1, y + 2, c.bar());
            g.fill(x0 + 1, y, x1 - 1, y + 1, c.barTop());
        }
    }

    /* The groove filled as far as the part has come, in the glow of the era's tool. */
    private void groove(final GuiGraphics g, final EraLook look) {
        final int x0 = this.leftPos + WorkbenchLayout.GROOVE_X;
        final int y = this.topPos + WorkbenchLayout.GROOVE_Y;
        final int head = x0 + (int) ((WorkbenchLayout.GROOVE_RIGHT - WorkbenchLayout.GROOVE_X) * this.menu.progress());
        if (head > x0 + 1) {
            g.fill(x0 + 1, y + 1, head, y + 3, look.glow());
            g.fill(x0 + 1, y + 1, head, y + 2, look.glowLight());
        }
    }

    /* The part under way, faded in its tray until it is done; a finished one waiting there is drawn by its slot. */
    private void part(final GuiGraphics g) {
        final ItemStack underWay = this.menu.underWay();
        if (underWay.isEmpty() || this.menu.getSlot(WorkbenchBlockEntity.TRAYS).hasItem()) {
            return;
        }
        g.renderItem(underWay, WorkbenchLayout.PART_X, WorkbenchLayout.PART_Y);
        g.pose().pushPose();
        g.pose().translate(0, 0, OVER_ITEMS);
        g.fill(WorkbenchLayout.PART_X, WorkbenchLayout.PART_Y, WorkbenchLayout.PART_X + 16,
                WorkbenchLayout.PART_Y + 16, PALETTE.get().veil());
        g.pose().popPose();
    }

    /* A lamp of {@link #LAMP_RADIUS} round ({@code cx}, {@code cy}), as the design draws its discs. */
    private static void disc(final GuiGraphics g, final int cx, final int cy, final int colour) {
        for (int y = cy - LAMP_RADIUS; y <= cy + LAMP_RADIUS; y++) {
            for (int x = cx - LAMP_RADIUS; x <= cx + LAMP_RADIUS; x++) {
                final double dx = x + 0.5 - cx;
                final double dy = y + 0.5 - cy;
                if (dx * dx + dy * dy <= LAMP_RADIUS * LAMP_RADIUS) {
                    g.fill(x, y, x + 1, y + 1, colour);
                }
            }
        }
    }

    /**
     * The colours of one era's bench drawn on its picture.
     *
     * @param plate     the name plate
     * @param plateEdge its edge
     * @param ink       the name on it
     * @param makes     what the bench makes, along its front edge
     * @param glow      the groove's fill, the glow of the era's tool
     * @param glowLight the fill's lit top
     */
    public record EraLook(int plate, int plateEdge, int ink, int makes, int glow, int glowLight) {
    }

    /**
     * The colours drawn on every bench's picture.
     *
     * @param lamp          the strip's lamp lit, and its display's figures
     * @param lampGlint     the lamp's glint
     * @param bar           the bar of the energy held
     * @param barTop        each of its segments' lit top
     * @param veil          what fades the part under way in its tray
     * @param inventoryWord the inventory's word, in the game's own grey
     */
    public record Colours(int lamp, int lampGlint, int bar, int barTop, int veil, int inventoryWord) {
    }
}
