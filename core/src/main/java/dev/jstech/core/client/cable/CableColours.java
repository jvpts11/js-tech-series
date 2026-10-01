/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.cable;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CoreCables;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * The colours of the rings on dyed wires: a ring's quad is tinted with the number of its dye, and the cable block turns
 * that number into the dye's colour.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class CableColours {

    private static final int UNTINTED = -1;
    private static final int DYES = DyeColor.values().length;

    private CableColours() {
    }

    @SubscribeEvent
    public static void onBlockColours(final RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint >= 0 && tint < DYES
                ? DyeColor.byId(tint).getTextureDiffuseColor() : UNTINTED, CoreCables.BLOCK.get());
    }
}
