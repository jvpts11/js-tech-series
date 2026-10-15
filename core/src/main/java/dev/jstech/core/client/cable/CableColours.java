/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.cable;

import dev.jstech.core.cable.CoreCables;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * The colours of the rings on dyed wires: a ring's quad is tinted with the number of its dye, and the cable block turns
 * that number into the dye's colour.
 */
public final class CableColours {

    private static final int UNTINTED = -1;
    private static final int DYES = DyeColor.values().length;

    private CableColours() {
    }

    /** Hands the mod's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterColorHandlersEvent.Block.class, CableColours::onBlockColours);
    }

    public static void onBlockColours(final RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> tint >= 0 && tint < DYES
                ? DyeColor.byId(tint).getTextureDiffuseColor() : UNTINTED, CoreCables.BLOCK.get());
    }
}
