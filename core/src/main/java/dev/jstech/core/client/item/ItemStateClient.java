/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.item;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.audio.AudioTexts;
import dev.jstech.core.item.ItemModePayload;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.item.ItemTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Items that hold something, on a player's game: the key that changes the mode of the item in the main hand, and the
 * lines of a tooltip that say what an item holds. The key has none of its own until the player gives it one, since
 * the game and the mods beside it already take nearly every key; the tooltip of an item with modes says so.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class ItemStateClient {

    public static final KeyMapping CHANGE_MODE = new KeyMapping(ItemTexts.MODE_KEY.key(),
            InputConstants.UNKNOWN.getValue(), AudioTexts.KEY_CATEGORY.key());

    private ItemStateClient() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(final RegisterKeyMappingsEvent event) {
        event.register(CHANGE_MODE);
    }

    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        while (CHANGE_MODE.consumeClick()) {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && ItemStates.of(minecraft.player.getMainHandItem()).hasModes()) {
                PacketDistributor.sendToServer(new ItemModePayload(Screen.hasShiftDown()));
            }
        }
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
            final Text hint = CHANGE_MODE.isUnbound() ? ItemTexts.MODE_NO_KEY.text()
                    : ItemTexts.MODE_HINT.with(GameText.of(CHANGE_MODE.getTranslatedKeyMessage()));
            tooltip.add(GameText.component(hint).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
