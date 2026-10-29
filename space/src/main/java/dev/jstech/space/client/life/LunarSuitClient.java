/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.life;

import dev.jstech.core.client.model.kit.KitArmorModel;
import dev.jstech.space.api.SuitModule;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.LunarSuit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * The lunar suit worn, drawn with its model by J's Core's armour kit: each piece its own parts, the gold visor lowered
 * over the helmet's while the module is fitted.
 */
public final class LunarSuitClient {

    private LunarSuitClient() {
    }

    /** Hands the game the suit's pieces to draw, once, from the mod's client entry point. */
    public static void listen(final IEventBus modEventBus) {
        modEventBus.addListener(RegisterClientExtensionsEvent.class, event -> event.registerItem(
                KitArmorModel.of(LunarSuit.MODEL.id())
                        .shownWhile(LunarSuit.GOLD_VISOR_BONE, LunarSuitClient::glareCut).extensions(),
                LunarSuit.HELMET.get(), LunarSuit.SUIT.get(), LunarSuit.TROUSERS.get(), LunarSuit.BOOTS.get()));
    }

    /** Whether a module that cuts the Sun's glare is fitted to {@code piece}, as the gold visor is to a helmet. */
    public static boolean glareCut(final ItemStack piece) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        for (final SuitModule module : LifeSupport.modules(level, piece)) {
            if (module.kind() == SuitModule.Kind.GLARE) {
                return true;
            }
        }
        return false;
    }
}
