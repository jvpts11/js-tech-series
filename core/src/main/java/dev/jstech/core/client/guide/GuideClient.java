/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.datafixers.util.Either;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.guide.GuideHooks;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuidePages;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.input.KeyHold;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The manuals on a player's game: read from the resource packs whenever they are, opened by a manual item's use, and
 * opened at an item's page by the manual key, held a moment over the item under the pointer in any inventory, or
 * over the item in the main hand. An item with a page says so at the foot of its tooltip, and while the key is held a
 * small bar fills, in the look of the manual it opens: in the tooltip, under that line, or under the crosshair.
 */
public final class GuideClient {

    /** How long the key is held over an item before its page opens, so a stray press opens nothing. */
    public static final long HOLD_MILLIS = 1500L;

    /** Where the bar stands out of every screen: below the crosshair. */
    private static final int BELOW_CROSSHAIR = 9;

    private static final KeyHold HOLD = new KeyHold(HOLD_MILLIS);

    private GuideClient() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the mod's constructor on a player's game. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class, GuideClient::onRegisterReloadListeners);
        modBus.addListener(FMLClientSetupEvent.class, GuideClient::onClientSetup);
        modBus.addListener(RegisterClientTooltipComponentFactoriesEvent.class, GuideClient::onRegisterTooltips);
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, GuideClient::onTooltip);
        NeoForge.EVENT_BUS.addListener(RenderTooltipEvent.GatherComponents.class, GuideClient::onTooltipGathered);
        NeoForge.EVENT_BUS.addListener(RenderTooltipEvent.Pre.class, GuideClient::onTooltipShown);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, GuideClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, GuideClient::onHudDrawn);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, GuideClient::onScreenDrawn);
    }

    public static void onRegisterReloadListeners(final RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new GuideReloadListener());
    }

    public static void onClientSetup(final FMLClientSetupEvent event) {
        GuideHooks.useOpener((manual, entry) -> Minecraft.getInstance().execute(() -> open(manual, entry)));
    }

    /**
     * Opens a manual at an entry's page, or, when the entry is empty or not in it, where it was last closed.
     *
     * @return whether the manual was found
     */
    public static boolean open(final String manualId, final String entry) {
        return open(manualId, entry, null);
    }

    /**
     * Opens a manual at an entry's page from {@code parent}, which closing the manual goes back to.
     *
     * @return whether the manual was found
     */
    public static boolean open(final String manualId, final String entry, @Nullable final Screen parent) {
        final GuideLibrary library = GuideLibrary.loaded();
        final Optional<GuideManual> manual = library.manual(manualId);
        if (manual.isEmpty()) {
            return false;
        }
        final Optional<GuideStyle> style = library.style(manual.get().style());
        if (style.isEmpty()) {
            return false;
        }
        Minecraft.getInstance().setScreen(new ManualScreen(manual.get(), style.get(), entry, parent));
        return true;
    }

    /**
     * Opens the page of an item, in the manual the key opens it in.
     *
     * @return whether the item has a page
     */
    public static boolean openPageOf(final ItemStack stack) {
        return openPageOf(stack, null);
    }

    /**
     * Opens the page of an item from {@code parent}, which closing the manual goes back to.
     *
     * @return whether the item has a page
     */
    public static boolean openPageOf(final ItemStack stack, @Nullable final Screen parent) {
        final String entry = pageFor(stack);
        if (entry.isEmpty()) {
            return false;
        }
        final Optional<GuideManual> manual = GuideLibrary.loaded().manualFor(entry);
        return manual.isPresent() && open(manual.get().id(), entry, parent);
    }

    /** Whether an item has a page in a manual. */
    public static boolean hasPage(final ItemStack stack) {
        final String entry = pageFor(stack);
        return !entry.isEmpty() && GuideLibrary.loaded().manualFor(entry).isPresent();
    }

    /** How far the manual key has been held over an item with a page, from 0 to 1; 0 when it is not. */
    public static float holdProgress() {
        return HOLD.progress(Util.getMillis());
    }

    public static void onRegisterTooltips(final RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(HoldBarTooltip.class, bar -> bar);
    }

    public static void onTooltip(final ItemTooltipEvent event) {
        if (pageFor(event.getItemStack()).isEmpty()) {
            return;
        }
        holdLine().ifPresent(event.getToolTip()::add);
    }

    /* While the key is held over the item, its bar fills in the tooltip, right under the line that says to hold it. */
    public static void onTooltipGathered(final RenderTooltipEvent.GatherComponents event) {
        final String entry = pageFor(event.getItemStack());
        final Optional<Component> line = holdLine();
        if (entry.isEmpty() || !entry.equals(HOLD.over()) || line.isEmpty()) {
            return;
        }
        final String said = line.get().getString();
        final List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        int under = elements.size();
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i).left().map(text -> text.getString().equals(said)).orElse(false)) {
                under = i + 1;
            }
        }
        elements.add(under, Either.right(new HoldBarTooltip(barKind())));
    }

    /*
     * The bar runs from one side of the tooltip to the other: the lines are final here, wrapped to the screen, so the
     * widest of them is the width the tooltip is drawn at.
     */
    public static void onTooltipShown(final RenderTooltipEvent.Pre event) {
        HoldBarTooltip bar = null;
        int widest = 0;
        for (final ClientTooltipComponent line : event.getComponents()) {
            if (line instanceof HoldBarTooltip found) {
                bar = found;
            } else {
                widest = Math.max(widest, line.getWidth(event.getFont()));
            }
        }
        if (bar != null) {
            bar.spanTo(widest);
        }
    }

    /* Out of every screen, the key is held over the item in the main hand. */
    public static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.player == null) {
            return;
        }
        final ItemStack hand = minecraft.player.getMainHandItem();
        if (HOLD.follow(pageFor(hand), keyHeld(), Util.getMillis())) {
            openPageOf(hand);
        }
    }

    public static void onHudDrawn(final RenderGuiEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || HOLD.over().isEmpty()) {
            return;
        }
        final GuiGraphics g = event.getGuiGraphics();
        GuideHoldBar.draw(g, (g.guiWidth() - GuideHoldBar.WIDTH) / 2, g.guiHeight() / 2 + BELOW_CROSSHAIR,
                GuideHoldBar.WIDTH, holdProgress(), barKind());
    }

    /*
     * In an inventory, the key is held over the item under the pointer; any other screen only lets it go. The bar
     * itself is a line of that item's tooltip.
     */
    public static void onScreenDrawn(final ScreenEvent.Render.Post event) {
        final long now = Util.getMillis();
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) {
            HOLD.follow("", keyHeld(), now);
            return;
        }
        final Slot slot = screen.getSlotUnderMouse();
        final ItemStack under = slot == null ? ItemStack.EMPTY : slot.getItem();
        if (HOLD.follow(pageFor(under), keyHeld(), now)) {
            openPageOf(under);
        }
    }

    /* The tooltip line that says which key to hold, or none while the key is unbound. */
    private static Optional<Component> holdLine() {
        final KeyMapping key = KeyActionsClient.mapping(CoreKeys.OPEN_IN_MANUAL);
        if (key.isUnbound()) {
            return Optional.empty();
        }
        return Optional.of(GameText.component(GuideTexts.HOLD_TO_OPEN.with(GameText.of(
                key.getTranslatedKeyMessage()))).withStyle(ChatFormatting.DARK_GRAY));
    }

    /* The look of the bar of the manual the held item's page opens in. */
    private static GuideStyle.HoldBar barKind() {
        final GuideLibrary library = GuideLibrary.loaded();
        return library.manualFor(HOLD.over()).flatMap(manual -> library.style(manual.style()))
                .map(GuideStyle::holdBar).orElse(GuideStyle.HoldBar.PLAIN);
    }

    /*
     * The entry a stack is the page of, or empty: the one its item's mod chose for that very stack, when there is
     * such an entry, else the one covering its item.
     */
    private static String pageFor(final ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        final GuideLibrary library = GuideLibrary.loaded();
        final String chosen = GuidePages.entryOf(stack);
        if (!chosen.isEmpty() && library.entry(chosen).isPresent()) {
            return chosen;
        }
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return library.pageOf(id.toString()).orElse("");
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
