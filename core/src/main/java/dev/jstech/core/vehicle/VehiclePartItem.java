/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.text.GameText;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A vehicle's part out of a vehicle, carrying how worn it is: used on a vehicle with an empty place of its kind, it
 * goes in there. Its bar shows how worn it is.
 */
public class VehiclePartItem extends Item {

    private static final int FULL_BAR = 13;

    public VehiclePartItem(final Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final VehiclePartStack part = stack.get(Vehicles.PART.get());
        if (part == null) {
            return super.getName(stack);
        }
        return VehicleParts.anywhere(part.part()).<Component>map(def -> Component.translatable(def.name()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public boolean isBarVisible(final ItemStack stack) {
        final VehiclePartStack part = stack.get(Vehicles.PART.get());
        return part != null && part.health() < 1;
    }

    @Override
    public int getBarWidth(final ItemStack stack) {
        final VehiclePartStack part = stack.get(Vehicles.PART.get());
        return part == null ? FULL_BAR : Math.round(part.health() * FULL_BAR);
    }

    @Override
    public int getBarColor(final ItemStack stack) {
        final VehiclePartStack part = stack.get(Vehicles.PART.get());
        final float health = part == null ? 1 : part.health();
        return Mth.hsvToRgb(health / 3, 1, 1);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        final VehiclePartStack part = stack.get(Vehicles.PART.get());
        if (part != null) {
            VehicleParts.anywhere(part.part()).ifPresent(def -> lines.add(GameText.component(def.kind().title())));
        }
    }
}
