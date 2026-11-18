/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.jei;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.chain.machine.MaskWriting;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Mask Writing in JEI (computers/docs/design/survival.md, "Masks"): a page a mask from the Legacy on, asked from the
 * mask and from the Mask Blank, showing how it is made: the blank in the station's mask slot becomes the mask in one
 * layer's time, never used up, on the station of its era, through the computer linked to it and the lithography
 * program, and in half the time on a computer two eras newer. Its words and its ink are {@link MaskWritingLook}'s.
 */
final class MaskWritingCategory implements IRecipeCategory<MaskWritingCategory.Page> {

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    /** The JEI recipe type of a mask written by a computer. */
    static final RecipeType<Page> TYPE = RecipeType.create(JsComputers.MODID, "mask_writing", Page.class);
    /* Where things stand on the page, in the GUI's pixels. */
    private static final int WIDTH = 232;
    private static final int HEIGHT = 130;
    /* The blank's and the mask's slots each over its line, the line under the blank as wide as about 80 pixels. */
    private static final int IN_SLOT_X = 4;
    private static final int NEVER_X = 92;
    private static final int BLANK_X = 35;
    private static final int MASK_X = 119;
    private static final int SLOT_Y = 8;
    private static final int ARROW_X = 74;
    private static final int ARROW_Y = 8;
    private static final int NAME_X = 141;
    private static final int UNDER_Y = 30;
    private static final int RULE_Y = 41;
    private static final int ROW_X = 7;
    private static final int ROW_TEXT_X = 30;
    private static final int FIRST_ROW = 48;
    private static final int ROW_PITCH = 22;
    private static final int LINE = 9;
    private static final int FOOT_Y = 121;

    MaskWritingCategory(final IGuiHelper gui) {
        this.icon = gui.createDrawableItemStack(new ItemStack(ChainChips.MASK_BLANK.get()));
        this.arrow = gui.getRecipeArrow();
    }

    /** One page a mask a computer writes, on every station that writes it. */
    static List<Page> pages() {
        final List<Page> out = new ArrayList<>();
        for (final ChainMachine station : ChainMachines.all()) {
            for (final MaskWriting.Offer offer : MaskWriting.offers(station, HardwareEra.SERIES.getLast())) {
                out.add(new Page(station, offer));
            }
        }
        return out;
    }

    /** The stations whose pages these are, which show them from their terminal. */
    static List<ItemLike> stations() {
        final List<ItemLike> out = new ArrayList<>();
        for (final ChainMachine station : ChainMachines.all()) {
            if (!MaskWriting.offers(station, HardwareEra.SERIES.getLast()).isEmpty()) {
                out.add(ChainMachines.terminalOf(station).get());
            }
        }
        return out;
    }

    @Override
    public RecipeType<Page> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return GameText.component(MaskWritingLook.TITLE);
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder, final Page page, final IFocusGroup focuses) {
        builder.addInputSlot(BLANK_X, SLOT_Y).setStandardSlotBackground()
                .addItemStack(new ItemStack(ChainChips.MASK_BLANK.get()));
        builder.addOutputSlot(MASK_X, SLOT_Y).setStandardSlotBackground().addItemStack(new ItemStack(page.offer()
                .mask()));
        builder.addSlot(RecipeIngredientRole.CATALYST, ROW_X, FIRST_ROW)
                .addItemStack(new ItemStack(ChainMachines.terminalOf(page.station()).get()));
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, ROW_X, FIRST_ROW + ROW_PITCH)
                .addItemStack(new ItemStack(computerOf(page.offer().computer())));
        builder.addSlot(RecipeIngredientRole.RENDER_ONLY, ROW_X, FIRST_ROW + ROW_PITCH * 2)
                .addItemStack(InstallerStacks.forProgram(Programs.LITHOGRAPHY));
    }

    @Override
    public void draw(final Page page, final IRecipeSlotsView slots, final GuiGraphics g, final double mouseX,
                     final double mouseY) {
        final Font font = Minecraft.getInstance().font;
        final MaskWritingLook.Colours ink = MaskWritingLook.PALETTE.get();
        final int seconds = page.station().seconds();
        final String time = GameText.resolve(MaskWritingLook.SECONDS.with(seconds));
        Draw.text(g, font, time, ARROW_X + (arrow.getWidth() - font.width(time)) / 2, 0, ink.text());
        arrow.draw(g, ARROW_X, ARROW_Y);
        // The mask's name, its kind over the chip and the era it is for, as its item names it.
        final String name = new ItemStack(page.offer().mask()).getHoverName().getString();
        final int open = name.indexOf(" (");
        Draw.text(g, font, open < 0 ? name : name.substring(0, open), NAME_X, SLOT_Y + 1, ink.text());
        if (open >= 0 && name.endsWith(")")) {
            Draw.text(g, font, name.substring(open + 2, name.length() - 1), NAME_X, SLOT_Y + 1 + LINE, ink.dim());
        }
        Draw.text(g, font, GameText.resolve(MaskWritingLook.IN_THE_SLOT), IN_SLOT_X, UNDER_Y, ink.dim());
        Draw.text(g, font, GameText.resolve(MaskWritingLook.NEVER_USED_UP), NEVER_X, UNDER_Y, ink.dim());
        g.fill(ROW_X - 1, RULE_Y, WIDTH - ROW_X, RULE_Y + 1, ink.rule());
        row(g, font, ink, 0, GameText.of(new ItemStack(ChainMachines.terminalOf(page.station()).get())
                .getHoverName()), MaskWritingLook.ITS_SLOT.text());
        row(g, font, ink, 1, MaskWritingLook.COMPUTER.with(page.offer().computer().name()),
                MaskWritingLook.LINKED.text());
        row(g, font, ink, 2, MaskWritingLook.PROGRAM.text(), MaskWritingLook.PICK.with(page.offer().name()));
        page.offer().faster().ifPresent(newer -> Draw.text(g, font, GameText.resolve(MaskWritingLook.FASTER.with(
                MaskWritingLook.SECONDS.with(Math.max(1, seconds / 2)), newer.name())), ROW_X, FOOT_Y, ink.dim()));
    }

    @Override
    public ResourceLocation getRegistryName(final Page page) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mask_writing/" + page.station().id() + "/"
                + BuiltInRegistries.ITEM.getKey(page.offer().mask()).getPath());
    }

    /* A row under the rule: its icon's slot is the layout's, its two lines beside it. */
    private static void row(final GuiGraphics g, final Font font, final MaskWritingLook.Colours ink, final int index,
                            final Text first, final Text second) {
        final int y = FIRST_ROW + index * ROW_PITCH;
        Draw.text(g, font, GameText.resolve(first), ROW_TEXT_X, y, ink.text());
        Draw.text(g, font, GameText.resolve(second), ROW_TEXT_X, y + LINE, ink.dim());
    }

    /* A Personal Computer of {@code era}, the one the page shows. */
    private static ItemLike computerOf(final Era era) {
        if (era == HardwareEra.VINTAGE) {
            return ComputingModule.VINTAGE_PERSONAL_COMPUTER.get();
        }
        if (era == HardwareEra.LEGACY) {
            return ComputingModule.LEGACY_PERSONAL_COMPUTER.get();
        }
        return era == HardwareEra.TRANSITION ? ComputingModule.TRANSITION_PERSONAL_COMPUTER.get()
                : ComputingModule.PERSONAL_COMPUTER.get();
    }

    /**
     * One mask a computer writes.
     *
     * @param station the station that writes it
     * @param offer   the mask, as the lithography program offers it
     */
    record Page(ChainMachine station, MaskWriting.Offer offer) {
    }
}
