/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jei;

import dev.jstech.core.item.ItemStates;
import dev.jstech.core.jstechseries.cylinder.CylinderTexts;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.jstechseries.cylinder.GasMarking;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.text.GameText;
import java.util.List;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.subtypes.UidContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * The Core's gases as JEI's own ingredients, as a bucket names its fluid: each drawn as the Gas Cylinder painted for
 * it, listed among the items, found in the recipes that take or make it, and dragged into a pattern's slot as a
 * cylinder marked for it. A gas had no name in JEI until a machine had made it into a cylinder.
 */
final class GasIngredients {

    /** JEI's type of the Core's gases. */
    static final IIngredientType<MatterStack> TYPE = () -> MatterStack.class;
    /** How much of a gas the list shows, a bucket's worth. */
    static final long LISTED = 1_000L;

    private GasIngredients() {
    }

    /** Every gas of the Core, as the list shows them. */
    static List<MatterStack> all() {
        return GasMarking.gases().stream().map(gas -> new MatterStack(gas, LISTED)).toList();
    }

    /** What JEI asks of a gas: its name, its ids, and the cylinder it is cheated in as, a full one. */
    static final class Helper implements IIngredientHelper<MatterStack> {

        @Override
        public IIngredientType<MatterStack> getIngredientType() {
            return TYPE;
        }

        @Override
        public String getDisplayName(final MatterStack gas) {
            return GameText.resolve(gas.material().name());
        }

        @Override
        public String getUniqueId(final MatterStack gas, final UidContext context) {
            return "gas:" + gas.material().id();
        }

        @Override
        public ResourceLocation getResourceLocation(final MatterStack gas) {
            return gas.material().id();
        }

        @Override
        public long getAmount(final MatterStack gas) {
            return gas.amount();
        }

        @Override
        public MatterStack copyWithAmount(final MatterStack gas, final long amount) {
            return gas.withAmount(amount);
        }

        @Override
        public MatterStack copyIngredient(final MatterStack gas) {
            return gas;
        }

        @Override
        public ItemStack getCheatItemStack(final MatterStack gas) {
            final ItemStack cylinder = GasCylinders.marked(gas.material());
            cylinder.set(ItemStates.GAS.get(), new MatterStack(gas.material(),
                    GasCylinders.VOLUME * GasCylinders.PRESSURE));
            return cylinder;
        }

        @Override
        public String getErrorInfo(final @Nullable MatterStack gas) {
            return String.valueOf(gas);
        }
    }

    /** How JEI draws a gas: the cylinder painted for it, and a tooltip of its name, formula and use. */
    static final class Renderer implements IIngredientRenderer<MatterStack> {

        @Override
        public void render(final GuiGraphics graphics, final MatterStack gas) {
            graphics.renderItem(GasCylinders.marked(gas.material()), 0, 0);
        }

        @Override
        public List<Component> getTooltip(final MatterStack gas, final TooltipFlag flag) {
            return List.of(GameText.component(gas.material().name()),
                    GameText.component(CylinderTexts.aGas(gas.material())).copy().withStyle(ChatFormatting.GRAY),
                    GameText.component(CylinderTexts.DRAG).copy().withStyle(ChatFormatting.GRAY));
        }
    }
}
