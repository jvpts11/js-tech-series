/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.JsCore;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

/**
 * Seeing what flows along a line: a player who holds an item of {@link #SEES_FLOW} (a wrench), or wears one (goggles),
 * reads beside the crosshair what the wire they look at carries, which way and how much, and how its end is set. A mod
 * puts its own wrench or goggles in the tag.
 */
@TextHolder
public final class FlowSight {

    /** The items that let a player see what flows: held in a hand, or worn on the head. */
    public static final TagKey<Item> SEES_FLOW = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "sees_flow"));

    public static final TextKey FLOW = TextKey.of("jscore.cable.flow", "%s toward %s, %s");
    public static final TextKey NOTHING = TextKey.of("jscore.cable.flow.nothing", "Nothing flows");
    public static final TextKey PER_TICK = TextKey.of("jscore.cable.flow.per_tick", "%s mB a tick");
    public static final TextKey ITEMS_PER_TICK = TextKey.of("jscore.cable.flow.items_per_tick", "%s a tick");

    private FlowSight() {
    }

    /** Whether {@code player} sees what flows: an item of the tag in a hand or on the head. */
    public static boolean sees(final Player player) {
        return player.getMainHandItem().is(SEES_FLOW) || player.getOffhandItem().is(SEES_FLOW)
                || player.getItemBySlot(EquipmentSlot.HEAD).is(SEES_FLOW);
    }
}
