/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.tool;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.layout.ToolHudLayout;
import dev.jstech.core.item.ItemMode;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.tool.ToolHeat;
import dev.jstech.core.tool.ToolSpec;
import dev.jstech.core.tool.ToolTexts;
import dev.jstech.core.tool.Tools;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * What the HUD shows of a tool in hand. The game's name of the item above the hotbar, shown a while when it comes into
 * the hand, gains the mode it is in, and shows again when the mode changes; under it, small, what the item works on,
 * how much of how much. Two short bars under the crosshair, where the game draws its attack indicator: how charged a
 * tool held in use is, and how hot a tool that heats is, the mark at the end of the heat.
 */
final class ToolHud {

    /* The game shows an item's name this many ticks, times the player's setting for how long notices stay. */
    private static final int SHOWN_TICKS = 40;
    /* It fades out over its last ten ticks, as the game's does. */
    private static final float FADE_TICKS = 10.0F;
    /* The game's name sits this far above the screen's bottom, lower by this much when the player cannot be hurt. */
    private static final int NAME_ABOVE = 59;
    private static final int NO_BARS_LOWER = 14;
    /* With a line under it, the name rises this much, and the line sits this far below where the name would. */
    private static final int RISES = 6;
    private static final int LINE_BELOW = 3;
    private static final float SMALL = 0.75F;
    private static final int OPAQUE = 255;
    private static final ResourceLocation BARS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "tool_bars");

    private static ItemStack lastItem = ItemStack.EMPTY;
    private static String lastMode = "";
    private static Component lastName = Component.empty();
    private static int shown;

    private ToolHud() {
    }

    /** Puts the bars under the crosshair in the game's layers, above the crosshair. */
    static void onRegisterLayers(final RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, BARS, ToolHud::drawBars);
    }

    /** Counts down how long the name stays, and starts it again for an item new to the hand or a mode new to it. */
    static void onTick(final ClientTickEvent.Post event) {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        final ItemStack held = player.getMainHandItem();
        final String mode = ItemStates.mode(held).map(ItemMode::id).orElse("");
        if (held.isEmpty()) {
            shown = 0;
        } else if (!held.is(lastItem.getItem()) || !held.getHoverName().equals(lastName) || !mode.equals(lastMode)) {
            shown = (int) (SHOWN_TICKS * Minecraft.getInstance().options.notificationDisplayTime().get());
        } else if (shown > 0) {
            shown--;
        }
        lastItem = held.copy();
        lastName = held.getHoverName();
        lastMode = mode;
    }

    /** Draws the name with its mode in place of the game's, for an item that has modes or works on something. */
    static void onLayer(final RenderGuiLayerEvent.Pre event) {
        if (!VanillaGuiLayers.SELECTED_ITEM_NAME.equals(event.getName())) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || !ours(player.getMainHandItem())) {
            return;
        }
        event.setCanceled(true);
        /* The ring of modes names the item itself. */
        if (shown <= 0 || minecraft.gameMode == null || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR
                || minecraft.screen instanceof ModeRadialScreen) {
            return;
        }
        final int alpha = Math.min(OPAQUE, (int) (shown * 256.0F / FADE_TICKS));
        if (alpha <= 0) {
            return;
        }
        drawName(event.getGuiGraphics(), minecraft, player.getMainHandItem(), alpha);
    }

    /* Whether the HUD writes the name of {@code stack} itself: an item with modes, or one that works on something. */
    private static boolean ours(final ItemStack stack) {
        return ItemStates.of(stack).hasModes() || Tools.spec(stack).flatMap(ToolSpec::power).isPresent();
    }

    private static void drawName(final GuiGraphics g, final Minecraft minecraft, final ItemStack held,
                                 final int alpha) {
        final Font font = minecraft.font;
        final MutableComponent name = Component.empty().append(held.getHoverName())
                .withStyle(held.getRarity().color());
        if (held.has(DataComponents.CUSTOM_NAME)) {
            name.withStyle(ChatFormatting.ITALIC);
        }
        final Optional<ItemMode> mode = ItemStates.mode(held);
        final Component line = mode.isPresent()
                ? GameText.component(ToolTexts.NAMED_MODE.with(GameText.of(name), mode.get().name())) : name;
        final Optional<Text> source = ToolReadings.held(held).stream().map(ToolReadings.Reading::text)
                .flatMap(Optional::stream).findFirst();
        final boolean unhurt = minecraft.gameMode != null && !minecraft.gameMode.canHurtPlayer();
        final int y = g.guiHeight() - Math.max(minecraft.gui.leftHeight, NAME_ABOVE) + (unhurt ? NO_BARS_LOWER : 0);
        final int width = font.width(line);
        final int colour = FastColor.ARGB32.color(alpha, ToolHudPalette.get().current());
        g.drawStringWithBackdrop(font, line, (g.guiWidth() - width) / 2, source.isPresent() ? y - RISES : y, width,
                colour);
        source.ifPresent(text -> {
            final String written = GameText.component(text).getString();
            final int smallWidth = Math.round(font.width(written) * SMALL);
            Draw.textScaled(g, font, written, (g.guiWidth() - smallWidth) / 2, y + LINE_BELOW,
                    FastColor.ARGB32.color(alpha, ToolHudPalette.get().hint()), SMALL);
        });
    }

    /* The charge and the heat, under the crosshair, in the first person. */
    private static void drawBars(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        final ToolHudPalette.Colours colours = ToolHudPalette.get();
        final int x = g.guiWidth() / 2 + ToolHudLayout.BAR_LEFT;
        final int middle = g.guiHeight() / 2;
        if (player.isUsingItem()) {
            final ItemStack using = player.getUseItem();
            if (Tools.spec(using).map(ToolSpec::charge).orElse(0) > 0) {
                bar(g, x, middle + ToolHudLayout.CHARGE_ROW,
                        ToolHudLayout.barFill(Tools.charged(using, player.getTicksUsingItem())), colours.charge(),
                        colours);
            }
        }
        final ItemStack held = player.getMainHandItem();
        final Optional<ToolHeat> heat = Tools.spec(held).flatMap(ToolSpec::heat);
        if (heat.isPresent()) {
            final double now = heat.get().heat(minecraft.level, held);
            if (now > 0) {
                final int fill = ToolHudLayout.barFill(now / heat.get().most());
                final int row = middle + ToolHudLayout.HEAT_ROW;
                bar(g, x, row, fill, colours.heat(), colours);
                g.fill(x + fill, row - 1, x + fill + 1, row - 1 + ToolHudLayout.MARK_HEIGHT, colours.overheat());
            }
        }
    }

    private static void bar(final GuiGraphics g, final int x, final int y, final int fill, final int colour,
                            final ToolHudPalette.Colours colours) {
        g.fill(x, y, x + ToolHudLayout.BAR_WIDTH, y + ToolHudLayout.BAR_HEIGHT, colours.hudGround());
        if (fill > 0) {
            g.fill(x, y, x + fill, y + ToolHudLayout.BAR_HEIGHT, colour);
        }
    }
}
