/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.item;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.item.ItemTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Items that hold something, on a player's game: the mode key ({@link CoreKeys#CHANGE_ITEM_MODE}) is sent to the
 * server only while the main hand holds an item with modes, and the lines of a tooltip say what an item holds. The key
 * has none of its own until the player gives it one, since the game and the mods beside it already take nearly every
 * key; the tooltip of an item with modes says so.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class ItemStateClient {

    private ItemStateClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        KeyActionsClient.sendsWhen(CoreKeys.CHANGE_ITEM_MODE, () -> {
            final Minecraft minecraft = Minecraft.getInstance();
            return minecraft.player != null && ItemStates.of(minecraft.player.getMainHandItem()).hasModes();
        });
    }

    @SubscribeEvent
    public static void onTooltip(final ItemTooltipEvent event) {
        final List<Text> lines = ItemStates.describe(event.getItemStack());
        if (lines.isEmpty()) {
            return;
        }
        final List<Component> tooltip = event.getToolTip();
        for (final Text line : lines) {
            tooltip.add(GameText.component(line).withStyle(ChatFormatting.GRAY));
        }
        if (ItemStates.of(event.getItemStack()).hasModes()) {
            final KeyMapping key = KeyActionsClient.mapping(CoreKeys.CHANGE_ITEM_MODE);
            final Text hint = key.isUnbound() ? ItemTexts.MODE_NO_KEY.text()
                    : ItemTexts.MODE_HINT.with(GameText.of(key.getTranslatedKeyMessage()));
            tooltip.add(GameText.component(hint).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
