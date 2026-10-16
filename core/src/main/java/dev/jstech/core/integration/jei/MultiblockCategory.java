/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jei;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.multiblock.MultiblockShows;
import dev.jstech.core.client.recipeview.MultiblockCard;
import dev.jstech.core.client.recipeview.MultiblockEntry;
import dev.jstech.core.multiblock.MultiblockViewLayout;
import dev.jstech.core.multiblock.MultiblockViewTexts;
import dev.jstech.core.text.GameText;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Every multiblock that can be shown before it is built, in JEI: its preview, its layers, its materials as the
 * ingredients it takes, so looking up the uses of a block finds the multiblocks built of it, and its controller as
 * what it makes, so looking up the controller finds how to build it.
 */
final class MultiblockCategory implements IRecipeCategory<MultiblockEntry> {

    private final IDrawable icon;

    /** The JEI recipe type of the multiblocks. */
    static final RecipeType<MultiblockEntry> TYPE = RecipeType.create(JsCore.MODID, "multiblocks",
            MultiblockEntry.class);
    /* A slot's picture is drawn a pixel outside the 16 pixels of what it holds. */
    private static final int SLOT_INSET = 1;

    MultiblockCategory(final IGuiHelper gui) {
        final List<ItemStack> tools = MultiblockShows.tools();
        this.icon = gui.createDrawableItemStack(tools.isEmpty() ? new ItemStack(Items.BRICKS) : tools.getFirst());
    }

    @Override
    public RecipeType<MultiblockEntry> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return GameText.component(MultiblockViewTexts.CATEGORY);
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return MultiblockViewLayout.WIDTH;
    }

    @Override
    public int getHeight() {
        return MultiblockViewLayout.HEIGHT;
    }

    @Override
    public void setRecipe(final IRecipeLayoutBuilder builder, final MultiblockEntry entry, final IFocusGroup focuses) {
        final List<MultiblockEntry.Material> materials = entry.materials();
        for (int index = 0; index < materials.size(); index++) {
            final MultiblockEntry.Material material = materials.get(index);
            final MultiblockViewLayout.Point at = MultiblockViewLayout.material(index);
            final IRecipeSlotBuilder slot = builder.addInputSlot(at.x() + SLOT_INSET, at.y() + SLOT_INSET)
                    .setStandardSlotBackground().addItemStacks(material.stacks());
            if (material.controller()) {
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(GameText.component(
                        MultiblockViewTexts.CONTROLLER)));
            }
        }
        entry.choice().ifPresent(choice -> {
            final List<ItemStack> stacks = choice.stacks();
            for (int index = 0; index < Math.min(stacks.size(), MultiblockViewLayout.MOST_CHOICES); index++) {
                final MultiblockViewLayout.Point at = MultiblockViewLayout.choice(index);
                // The last slot takes the rest of the choice in turn, so no block that will do goes unshown.
                final List<ItemStack> held = index == MultiblockViewLayout.MOST_CHOICES - 1
                        ? stacks.subList(index, stacks.size()) : List.of(stacks.get(index));
                builder.addInputSlot(at.x() + SLOT_INSET, at.y() + SLOT_INSET).setStandardSlotBackground()
                        .addItemStacks(held);
            }
        });
        if (!entry.controller().isEmpty()) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(entry.controller());
        }
    }

    @Override
    public void createRecipeExtras(final IRecipeExtrasBuilder builder, final MultiblockEntry entry,
                                   final IFocusGroup focuses) {
        final CardWidget widget = new CardWidget(new MultiblockCard(entry, true));
        builder.addWidget(widget);
        builder.addGuiEventListener(widget);
    }

    @Override
    public ResourceLocation getRegistryName(final MultiblockEntry entry) {
        return entry.id();
    }

    /* The card drawn over the whole recipe, which takes its clicks and drags. */
    private record CardWidget(MultiblockCard card) implements IRecipeWidget, IJeiGuiEventListener {

        private static final ScreenPosition ORIGIN = new ScreenPosition(0, 0);
        private static final ScreenRectangle AREA = new ScreenRectangle(0, 0, MultiblockViewLayout.WIDTH,
                MultiblockViewLayout.HEIGHT);

        @Override
        public ScreenPosition getPosition() {
            return ORIGIN;
        }

        @Override
        public void drawWidget(final GuiGraphics guiGraphics, final double mouseX, final double mouseY) {
            this.card.draw(guiGraphics, mouseX, mouseY);
        }

        @Override
        public void getTooltip(final ITooltipBuilder tooltip, final double mouseX, final double mouseY) {
            tooltip.addAll(this.card.tooltip(mouseX, mouseY));
        }

        @Override
        public ScreenRectangle getArea() {
            return AREA;
        }

        @Override
        public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
            return this.card.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                    final double dragY) {
            return this.card.mouseDragged(mouseX, mouseY, dragX, dragY);
        }
    }
}
