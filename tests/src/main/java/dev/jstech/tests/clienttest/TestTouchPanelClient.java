/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.client.live.LiveScreen;
import dev.jstech.core.client.live.LiveScreens;
import dev.jstech.core.client.live.ScreenTouchClient;
import dev.jstech.core.client.live.TouchPanel;
import dev.jstech.core.gui.Tube;
import dev.jstech.core.live.LiveRate;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.TestTouchPanel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * The pump's panel on the player's game, as a mod builds a screen in the world of the Core's components: its name and
 * its flow, and its Start and Stop buttons named for the server, in the series' style; the one under the crosshair
 * lights, and the action bar says what a right click does.
 */
public final class TestTouchPanelClient {

    /** The panel every pump's screen draws. */
    public static final TouchPanel PANEL = new TouchPanel(() -> Styles.get(ResourceLocation.fromNamespaceAndPath(
            JsCore.MODID, "jstech")).orElseGet(Styles::game));

    /* Texture pixels to a pixel of the screen, and how far the picture stands off the face. */
    private static final int SHARPNESS = 2;
    private static final float OFF_FACE = 0.005F;
    /* Where the parts sit on the screen: the line of text along its top, the buttons under it. */
    private static final int EDGE = 6;
    private static final int TEXT_Y = 6;
    private static final int TITLE_WIDTH = 36;
    private static final int FLOW_WIDTH = 48;
    private static final int TEXT_HEIGHT = 9;
    private static final int BUTTONS_Y = 26;
    private static final int BUTTON_WIDTH = 40;
    private static final int BUTTON_HEIGHT = 16;

    private TestTouchPanelClient() {
    }

    /** Builds the panel, names its buttons to the Core and registers its renderer, from the test mod's client. */
    public static void register(final IEventBus modBus) {
        PANEL.add(new Label(() -> GameText.resolve(TestTouchPanel.TITLE), Label.Tone.ACCENT))
                .setBounds(EDGE, TEXT_Y, TITLE_WIDTH, TEXT_HEIGHT);
        PANEL.add(new Label(() -> GameText.resolve(TestTouchPanel.FLOW)).setAlign(Label.Align.RIGHT))
                .setBounds(TestTouchPanel.WIDTH - EDGE - FLOW_WIDTH, TEXT_Y, FLOW_WIDTH, TEXT_HEIGHT);
        PANEL.add(TestTouchPanel.START, new Button(() -> GameText.resolve(TestTouchPanel.START_LABEL), () -> { }))
                .setBounds(EDGE, BUTTONS_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        PANEL.add(TestTouchPanel.STOP, new Button(() -> GameText.resolve(TestTouchPanel.STOP_LABEL), () -> { }))
                .setBounds(TestTouchPanel.WIDTH - EDGE - BUTTON_WIDTH, BUTTONS_Y, BUTTON_WIDTH, BUTTON_HEIGHT);
        ScreenTouchClient.targets(TestTouchPanel.PANEL, PANEL);
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(
                TestTouchPanel.PANEL_ENTITY.get(), context -> new PanelRenderer()));
    }

    /* The screen across the middle half of the north face, upright as seen from the north: its left is to the east. */
    private static final class PanelRenderer implements BlockEntityRenderer<TestTouchPanel.PanelEntity> {

        @Override
        public void render(final TestTouchPanel.PanelEntity panel, final float partialTick, final PoseStack pose,
                           final MultiBufferSource buffers, final int light, final int overlay) {
            final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            final double distance = eye.distanceToSqr(Vec3.atCenterOf(panel.getBlockPos()));
            if (!LiveRate.shows(distance)) {
                return;
            }
            final LiveScreen picture = LiveScreens.ask(panel.getBlockPos(), PANEL.painter(panel.getBlockPos()),
                    TestTouchPanel.WIDTH, TestTouchPanel.HEIGHT, SHARPNESS, Tube.COLOUR, distance);
            final float top = (float) (1.0 - TestTouchPanel.AREA.top());
            final float tall = (float) (TestTouchPanel.AREA.bottom() - TestTouchPanel.AREA.top());
            pose.pushPose();
            pose.translate(1.0F, top, -OFF_FACE);
            pose.scale(-1.0F / TestTouchPanel.WIDTH, -tall / TestTouchPanel.HEIGHT, 1.0F);
            LiveScreens.draw(pose, buffers, picture, 0, 0, TestTouchPanel.WIDTH, TestTouchPanel.HEIGHT,
                    LightTexture.FULL_BRIGHT);
            pose.popPose();
        }
    }
}
