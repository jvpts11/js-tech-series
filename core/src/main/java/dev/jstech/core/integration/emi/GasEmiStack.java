/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.emi;

import dev.emi.emi.api.stack.EmiStack;
import dev.jstech.core.jstechseries.cylinder.CylinderTexts;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.material.Material;
import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A gas of the Core as one of EMI's stacks, as a bucket names its fluid: drawn as the Gas Cylinder painted for it,
 * listed among the items and found in the recipes that take or make it; as an item, the cylinder marked for it.
 */
final class GasEmiStack extends EmiStack {

    private final Material gas;

    GasEmiStack(final Material gas, final long amount) {
        this.gas = gas;
        this.amount = amount;
    }

    @Override
    public EmiStack copy() {
        final GasEmiStack copy = new GasEmiStack(this.gas, this.amount);
        copy.chance = this.chance;
        return copy;
    }

    @Override
    public void render(final GuiGraphics draw, final int x, final int y, final float delta, final int flags) {
        draw.renderItem(GasCylinders.marked(this.gas), x, y);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public DataComponentPatch getComponentChanges() {
        return DataComponentPatch.EMPTY;
    }

    @Override
    public Object getKey() {
        return this.gas;
    }

    @Override
    public ResourceLocation getId() {
        return this.gas.id();
    }

    @Override
    public ItemStack getItemStack() {
        return GasCylinders.marked(this.gas);
    }

    @Override
    public List<Component> getTooltipText() {
        return List.of(getName(), GameText.component(CylinderTexts.aGas(this.gas)).copy()
                        .withStyle(ChatFormatting.GRAY),
                GameText.component(CylinderTexts.DRAG).copy().withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Component getName() {
        return GameText.component(this.gas.name());
    }
}
