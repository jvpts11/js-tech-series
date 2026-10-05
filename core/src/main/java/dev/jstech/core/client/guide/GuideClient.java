/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.guide.GuideHooks;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The manuals on a player's game: read from the resource packs whenever they are, opened by a manual item's use, and
 * opened at an item's page by the manual key, held a moment over the item under the pointer in any inventory, or
 * pressed with the item in the main hand. An item with a page says so at the foot of its tooltip.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class GuideClient {

    /** How long the key is held over an item before its page opens, so a stray press opens nothing. */
    public static final long HOLD_MILLIS = 400L;

    private static ItemStack holding = ItemStack.EMPTY;
    private static long heldSince;

    private GuideClient() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new GuideReloadListener());
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        GuideHooks.useOpener((manual, entry) -> Minecraft.getInstance().execute(() -> open(manual, entry)));
        KeyActionsClient.onPress(CoreKeys.OPEN_IN_MANUAL, () -> {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.screen == null) {
                openPageOf(minecraft.player.getMainHandItem());
            }
        });
    }

    /**
     * Opens a manual at an entry's page, or at its cover when the entry is empty or not in it.
     *
     * @return whether the manual was found
     */
    public static boolean open(final String manualId, final String entry) {
        final GuideLibrary library = GuideLibrary.loaded();
        final Optional<GuideManual> manual = library.manual(manualId);
        if (manual.isEmpty()) {
            return false;
        }
        final Optional<GuideStyle> style = library.style(manual.get().style());
        if (style.isEmpty()) {
            return false;
        }
        Minecraft.getInstance().setScreen(new ManualScreen(manual.get(), style.get(), entry));
        return true;
    }

    /**
     * Opens the page of an item, in the manual the key opens it in.
     *
     * @return whether the item has a page
     */
    public static boolean openPageOf(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        final GuideLibrary library = GuideLibrary.loaded();
        final Optional<String> entry = library.pageOf(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (entry.isEmpty()) {
            return false;
        }
        final Optional<GuideManual> manual = library.manualFor(entry.get());
        return manual.isPresent() && open(manual.get().id(), entry.get());
    }

    @SubscribeEvent
    public static void onTooltip(final ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || GuideLibrary.loaded().pageOf(BuiltInRegistries.ITEM.getKey(stack.getItem())
                .toString()).isEmpty()) {
            return;
        }
        final KeyMapping key = KeyActionsClient.mapping(CoreKeys.OPEN_IN_MANUAL);
        if (key.isUnbound()) {
            return;
        }
        event.getToolTip().add(GameText.component(GuideTexts.HOLD_TO_OPEN.with(GameText.of(
                key.getTranslatedKeyMessage()))).withStyle(ChatFormatting.DARK_GRAY));
    }

    @SubscribeEvent
    public static void onScreenDrawn(final ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            holding = ItemStack.EMPTY;
            return;
        }
        final Slot slot = screen.getSlotUnderMouse();
        final ItemStack under = slot == null ? ItemStack.EMPTY : slot.getItem();
        if (under.isEmpty() || !keyHeld()) {
            holding = ItemStack.EMPTY;
            return;
        }
        final long now = Util.getMillis();
        if (holding.isEmpty() || !ItemStack.isSameItem(holding, under)) {
            holding = under.copy();
            heldSince = now;
            return;
        }
        if (now - heldSince >= HOLD_MILLIS) {
            holding = ItemStack.EMPTY;
            openPageOf(under);
        }
    }

    private static boolean keyHeld() {
        final KeyMapping key = KeyActionsClient.mapping(CoreKeys.OPEN_IN_MANUAL);
        if (key.isUnbound() || key.getKey().getType() != InputConstants.Type.KEYSYM) {
            return false;
        }
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getKey().getValue());
    }
}
