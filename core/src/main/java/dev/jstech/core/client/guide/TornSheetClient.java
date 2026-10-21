/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.tooltip.ItemTooltip;
import dev.jstech.core.client.gui.tooltip.ItemTooltips;
import dev.jstech.core.client.live.LiveScreen;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.TornSheets;
import dev.jstech.core.live.LiveRate;
import dev.jstech.core.text.GameText;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A torn sheet on a player's game: its tooltip says which manual and which page it was torn from, and in an item frame
 * it shows its page, filling the frame as a map does, drawn as the manual draws it in the reader's language and as
 * their resource packs lay it out. A page no manual holds any longer leaves the sheet an item in the frame.
 */
public final class TornSheetClient {

    /** How much of the frame's face the sheet covers, as the frame's inner board is twelve pixels of sixteen. */
    private static final float FACE = 12F / 16F;
    /** The side of the frame's face in the units the sheet is drawn in, as a map's 128. */
    private static final float SIDE = 128F;
    /** How many of the picture's pixels a pixel of the page is drawn with, so its small letters stay sharp. */
    private static final int SHARPNESS = 2;
    /* The page of each sheet, laid out once for the manuals as they were read. */
    private static final Map<TornSheets.Sheet, ManualScreen> PAGES = new HashMap<>();
    private static final AtomicInteger FRAMES_DRAWN = new AtomicInteger();

    private static GuideLibrary laidFor;

    private TornSheetClient() {
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    public static void listen() {
        ItemTooltips.add(JsCore.MODID, TornSheetClient::describe);
        NeoForge.EVENT_BUS.addListener(RenderItemInFrameEvent.class, TornSheetClient::onRenderItemInFrame);
    }

    /** How many times a frame has shown a sheet's page, its picture drawn; for a test to look at. */
    public static int framesDrawn() {
        return FRAMES_DRAWN.get();
    }

    /** The sheet's tooltip: the manual and the page it was torn from, and that the manual keeps its page. */
    public static void describe(final ItemStack stack, final ItemTooltip tooltip) {
        final TornSheets.Sheet sheet = stack.get(TornSheets.SHEET.get());
        if (sheet == null) {
            return;
        }
        final Component title = GuideLibrary.loaded().manual(sheet.manual())
                .<Component>map(manual -> Component.translatable(manual.titleKey()))
                .orElseGet(() -> Component.literal(sheet.manual()));
        tooltip.line(GameText.component(TornSheets.TORN_FROM.with(GameText.of(title), sheet.folio()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.line(GameText.component(TornSheets.A_COPY).withStyle(ChatFormatting.DARK_GRAY));
    }

    /*
     * The frame has already turned the item by its rotation in eighths of a turn; a sheet turns as a map does, by
     * quarters, so the eighths are undone and the quarters put back before it is laid flat on the face. The page
     * keeps its own shape, as tall as the face and centred across it.
     */
    public static void onRenderItemInFrame(final RenderItemInFrameEvent event) {
        final TornSheets.Sheet sheet = event.getItemStack().get(TornSheets.SHEET.get());
        if (sheet == null) {
            return;
        }
        final Optional<ManualScreen> page = pageOf(sheet);
        if (page.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        final ItemFrame frame = event.getItemFrameEntity();
        final double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()
                .distanceToSqr(frame.position());
        if (!LiveRate.shows(distance)) {
            return;
        }
        final GuideStyle style = page.get().style();
        final int width = style.pageWidth();
        final int height = style.pageHeight();
        final ManualScreen screen = page.get();
        final LiveScreen picture = LiveScreens.ask(sheet, (graphics, w, h, partialTick) -> screen.drawSheet(graphics,
                sheet.target(), sheet.pagesIn()), width, height, SHARPNESS, Tube.COLOUR, distance);
        final PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(-frame.getRotation() * 45F));
        pose.mulPose(Axis.ZP.rotationDegrees((frame.getRotation() % 4) * 90F + 180F));
        final float unit = FACE / SIDE;
        pose.scale(unit, unit, unit);
        pose.translate(-SIDE / 2F, -SIDE / 2F, -1F);
        final float across = SIDE * width / height;
        final float left = (SIDE - across) / 2F;
        final int light = frame.getType() == EntityType.GLOW_ITEM_FRAME ? LightTexture.FULL_BRIGHT
                : event.getPackedLight();
        LiveScreens.draw(pose, event.getMultiBufferSource(), picture, left, 0F, left + across, SIDE, light);
        pose.popPose();
        if (picture.ready()) {
            FRAMES_DRAWN.incrementAndGet();
        }
    }

    /* The sheet's manual laid out as a page apart, once for the manuals as they were read; none when it is gone. */
    private static Optional<ManualScreen> pageOf(final TornSheets.Sheet sheet) {
        final GuideLibrary library = GuideLibrary.loaded();
        if (library != laidFor) {
            PAGES.clear();
            laidFor = library;
        }
        final ManualScreen known = PAGES.get(sheet);
        if (known != null) {
            return Optional.of(known);
        }
        final Optional<GuideManual> manual = library.manual(sheet.manual());
        final Optional<GuideStyle> style = manual.flatMap(found -> library.style(found.style()));
        if (manual.isEmpty() || style.isEmpty()) {
            return Optional.empty();
        }
        final ManualScreen laid = ManualScreen.sheet(manual.get(), style.get());
        PAGES.put(sheet, laid);
        return Optional.of(laid);
    }
}
