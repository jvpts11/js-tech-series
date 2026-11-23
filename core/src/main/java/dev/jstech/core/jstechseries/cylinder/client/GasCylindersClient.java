/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.cylinder.client;

import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/** The Gas Cylinder on a player's game: its shoulder, its model's second layer, painted in the colour of its gas. */
public final class GasCylindersClient {

    /* The model's layer the shoulder is drawn in, and what leaves a layer's own colours as they are. */
    private static final int SHOULDER = 1;
    private static final int UNTINTED = -1;

    private GasCylindersClient() {
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterColorHandlersEvent.Item.class, event -> event.register(GasCylindersClient::tint,
                GasCylinders.CYLINDER.get()));
    }

    private static int tint(final ItemStack stack, final int layer) {
        return layer == SHOULDER ? GasCylinders.shoulderOf(stack) : UNTINTED;
    }
}
