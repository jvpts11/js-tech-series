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
import dev.jstech.core.input.KeyHold;
import dev.jstech.core.text.GameText;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The manuals on a player's game: read from the resource packs whenever they are, opened by a manual item's use, and
 * opened at an item's page by the manual key, held a moment over the item under the pointer in any inventory, or
 * over the item in the main hand. While the key is held a small bar fills, under the slot or under the crosshair, in
 * the look of the manual it opens. An item with a page says so at the foot of its tooltip.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class GuideClient {

    /** How long the key is held over an item before its page opens, so a stray press opens nothing. */
    public static final long HOLD_MILLIS = 400L;

    /** Where the bar stands: below a slot, and below the crosshair. */
    private static final int BELOW_SLOT = 17;
    private static final int BELOW_CROSSHAIR = 9;
    /** Over the items and the tooltip of the inventory it is drawn on. */
    private static final float OVER_TOOLTIP = 500.0F;

    private static final KeyHold HOLD = new KeyHold(HOLD_MILLIS);

    private GuideClient() {
    }

    @SubscribeEvent
    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new GuideReloadListener());
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        GuideHooks.useOpener((manual, entry) -> Minecraft.getInstance().execute(() -> open(manual, entry)));
    }

    /**
     * Opens a manual at an entry's page, or, when the entry is empty or not in it, where it was last closed.
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
        final String item = withPage(stack);
        if (item.isEmpty()) {
            return false;
        }
        final GuideLibrary library = GuideLibrary.loaded();
        final Optional<String> entry = library.pageOf(item);
        final Optional<GuideManual> manual = entry.flatMap(library::manualFor);
        return manual.isPresent() && open(manual.get().id(), entry.get());
    }

    /** How far the manual key has been held over an item with a page, from 0 to 1; 0 when it is not. */
    public static float holdProgress() {
        return HOLD.progress(Util.getMillis());
    }

    @SubscribeEvent
    public static void onTooltip(final ItemTooltipEvent event) {
        if (withPage(event.getItemStack()).isEmpty()) {
            return;
        }
        final KeyMapping key = KeyActionsClient.mapping(CoreKeys.OPEN_IN_MANUAL);
        if (key.isUnbound()) {
            return;
        }
        event.getToolTip().add(GameText.component(GuideTexts.HOLD_TO_OPEN.with(GameText.of(
                key.getTranslatedKeyMessage()))).withStyle(ChatFormatting.DARK_GRAY));
    }

    /* Out of every screen, the key is held over the item in the main hand. */
    @SubscribeEvent
    public static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return;
        }
        final ItemStack hand = minecraft.player.getMainHandItem();
        if (HOLD.follow(withPage(hand), keyHeld(), Util.getMillis())) {
            openPageOf(hand);
        }
    }

    @SubscribeEvent
    public static void onHudDrawn(final RenderGuiEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || HOLD.over().isEmpty()) {
            return;
        }
        final GuiGraphics g = event.getGuiGraphics();
        drawBar(g, (g.guiWidth() - GuideHoldBar.WIDTH) / 2, g.guiHeight() / 2 + BELOW_CROSSHAIR, g.guiWidth());
    }

    /* In an inventory, the key is held over the item under the pointer; any other screen only lets it go. */
    @SubscribeEvent
    public static void onScreenDrawn(final ScreenEvent.Render.Post event) {
        final long now = Util.getMillis();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            HOLD.follow("", keyHeld(), now);
            return;
        }
        final Slot slot = screen.getSlotUnderMouse();
        final ItemStack under = slot == null ? ItemStack.EMPTY : slot.getItem();
        if (HOLD.follow(withPage(under), keyHeld(), now)) {
            openPageOf(under);
            return;
        }
        if (slot == null || HOLD.over().isEmpty()) {
            return;
        }
        final GuiGraphics g = event.getGuiGraphics();
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, OVER_TOOLTIP);
        drawBar(g, screen.getGuiLeft() + slot.x + 8 - GuideHoldBar.WIDTH / 2,
                screen.getGuiTop() + slot.y + BELOW_SLOT, screen.width);
        g.pose().popPose();
    }

    /* The bar of the manual the held item opens in, kept inside the screen's width. */
    private static void drawBar(final GuiGraphics g, final int x, final int y, final int screenWidth) {
        final GuideLibrary library = GuideLibrary.loaded();
        final Optional<GuideStyle> style = library.pageOf(HOLD.over()).flatMap(library::manualFor)
                .flatMap(manual -> library.style(manual.style()));
        final int left = Math.max(0, Math.min(screenWidth - GuideHoldBar.WIDTH, x));
        GuideHoldBar.draw(g, left, y, holdProgress(), style.map(GuideStyle::holdBar).orElse(GuideStyle.HoldBar.PLAIN));
    }

    /* The id of the stack's item when it has a page, or empty. */
    private static String withPage(final ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return GuideLibrary.loaded().pageOf(id.toString()).isPresent() ? id.toString() : "";
    }

    /*
     * Whether the manual key is down. The game lets go of its key bindings while a screen is open, so the key itself
     * is read as well.
     */
    private static boolean keyHeld() {
        final KeyMapping key = KeyActionsClient.mapping(CoreKeys.OPEN_IN_MANUAL);
        if (key.isUnbound()) {
            return false;
        }
        if (key.isDown()) {
            return true;
        }
        return key.getKey().getType() == InputConstants.Type.KEYSYM
                && InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key.getKey().getValue());
    }
}
