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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * A vehicle's key, as a mod declares it with its own look and names it in its vehicles' kinds: cut for one vehicle,
 * it lets whoever carries it drive the vehicle, and locks and unlocks it used on it with the sneak key held. A key of
 * an older generation of the vehicle's keys opens nothing.
 *
 * <pre>{@code
 * CONTENT.item("car_key", properties -> new VehicleKeyItem(properties.stacksTo(1))).named("Car Key").register();
 * }</pre>
 */
public class VehicleKeyItem extends Item {

    public VehicleKeyItem(final Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        final VehicleKey key = stack.get(Vehicles.KEY.get());
        if (key == null) {
            lines.add(GameText.component(VehicleTexts.KEY_BLANK));
            return;
        }
        lines.add(GameText.component(VehicleTexts.KEY_OF.with(GameText.of(key.name().isEmpty()
                ? Component.literal(key.vehicle().toString().substring(0, 8)) : Component.translatable(key.name())))));
    }
}
