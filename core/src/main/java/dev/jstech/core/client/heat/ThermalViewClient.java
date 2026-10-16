/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.heat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.heat.HeatReadings;
import dev.jstech.core.heat.ThermalScale;
import dev.jstech.core.heat.ThermalSight;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.OptionalDouble;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

/**
 * The thermal view of a player who sees heat ({@link ThermalSight}): the world laid under a dark veil, what is as warm
 * as the air left in it, and over it, in false colour, what is warmer or colder, the blocks that hold heat, the liquids
 * far from the air's temperature and the living things, from violet through red and orange to a white heat, and from
 * blue through cyan to an icy white for the cold. Beside the crosshair, how hot what they look at is, in kelvin and in
 * degrees Celsius; the air's own temperature for a block that holds none.
 */
@PaletteHolder
public final class ThermalViewClient {

    /** The HUD layer of the reading beside the crosshair. */
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "thermal_view");
    /** The colours of the view, from the coldest to the hottest, and of its reading. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "thermal_view", new Colours(
            0xD8 << 24 | 0x0C0A1E, 0xFF << 24 | 0xE8FAFF, 0xFF << 24 | 0x36C2E8, 0xFF << 24 | 0x2152B0,
            0xFF << 24 | 0x2A1B5C, 0xFF << 24 | 0x8E2C8E, 0xFF << 24 | 0xE0472B, 0xFF << 24 | 0xF59A22,
            0xFF << 24 | 0xFCE65A, 0xFF << 24 | 0xFFFFF2, 0xFF << 24 | 0xFFFFFF, 0x96 << 24));

    /* How far out from a block its colour stands, in blocks; how far living things are seen, in blocks. */
    private static final double GROW = 0.006;
    private static final double ENTITY_REACH = 48.0;
    private static final int FROM_CROSSHAIR = 10;
    private static final int PADDING = 4;
    private static final int LINE = 11;
    private static final int CHANNEL = 255;

    private ThermalViewClient() {
    }

    /** Hands the buses the view and its reading, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, ThermalViewClient::onRender);
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.CROSSHAIR,
                LAYER, ThermalViewClient::renderHud));
    }

    /** The false colour of {@code warmth}, from -1 for the coldest to 1 for the hottest, as the palette paints it. */
    public static int colourOf(final double warmth) {
        final Colours c = PALETTE.get();
        final int[] hot = {c.air(), c.warm(), c.hot(), c.hotter(), c.hottest(), c.white()};
        final int[] cold = {c.air(), c.cool(), c.cold(), c.coldest()};
        final int[] ramp = warmth >= 0 ? hot : cold;
        final double along = Math.min(1.0, Math.abs(warmth)) * (ramp.length - 1);
        final int below = (int) Math.floor(along);
        final int above = Math.min(ramp.length - 1, below + 1);
        return blend(ramp[below], ramp[above], along - below);
    }

    private static void onRender(final RenderLevelStageEvent event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || level == null
                || minecraft.player == null || !ThermalSight.sees(minecraft.player)) {
            return;
        }
        veil(PALETTE.get().veil());
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack poses = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        final PoseStack.Pose pose = poses.last();
        for (final HeatScan.Found seen : HeatScan.seen()) {
            final double air = HeatClimate.ambient(level, seen.pos());
            HeatBoxes.block(consumer, pose, seen.pos(), GROW, colourOf(ThermalScale.warmth(seen.kelvin(), air)),
                    HeatScan::seenAt);
        }
        final float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        final boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        for (final Entity entity : level.entitiesForRendering()) {
            if (entity == minecraft.player && firstPerson || entity.distanceToSqr(camera) > ENTITY_REACH
                    * ENTITY_REACH) {
                continue;
            }
            final OptionalDouble kelvin = ThermalSight.of(entity);
            if (kelvin.isPresent()) {
                final double air = HeatClimate.ambient(level, entity.blockPosition());
                final Vec3 at = entity.getPosition(partial);
                final AABB box = entity.getBoundingBox().move(at.subtract(entity.position())).inflate(GROW);
                HeatBoxes.box(consumer, pose, box, colourOf(ThermalScale.warmth(kelvin.getAsDouble(), air)));
            }
        }
        poses.popPose();
        buffers.endBatch(RenderType.debugQuads());
    }

    /* A veil over all the window has drawn of the world, through nothing, so what is drawn after stands out. */
    private static void veil(final int colour) {
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        final Matrix4fStack view = RenderSystem.getModelViewStack();
        view.pushMatrix();
        view.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        // With no projection and no view, the window's corners are at one either way.
        final BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        quad.addVertex(-1.0F, -1.0F, 0.0F).setColor(colour);
        quad.addVertex(1.0F, -1.0F, 0.0F).setColor(colour);
        quad.addVertex(1.0F, 1.0F, 0.0F).setColor(colour);
        quad.addVertex(-1.0F, 1.0F, 0.0F).setColor(colour);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        view.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    private static void renderHud(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (minecraft.player == null || level == null || minecraft.screen != null || minecraft.options.hideGui
                || !ThermalSight.sees(minecraft.player) || minecraft.hitResult == null
                || minecraft.hitResult.getType() == HitResult.Type.MISS) {
            return;
        }
        final double kelvin;
        if (minecraft.hitResult instanceof EntityHitResult hit) {
            kelvin = ThermalSight.of(hit.getEntity()).orElse(HeatClimate.ambient(level,
                    hit.getEntity().blockPosition()));
        } else if (minecraft.hitResult instanceof BlockHitResult hit) {
            kelvin = ThermalSight.of(level, hit.getBlockPos()).orElse(HeatClimate.ambient(level, hit.getBlockPos()));
        } else {
            return;
        }
        final String line = GameText.resolve(HeatReadings.line(kelvin));
        final int x = g.guiWidth() / 2 + FROM_CROSSHAIR;
        final int y = g.guiHeight() / 2 + FROM_CROSSHAIR / 2;
        final Colours c = PALETTE.get();
        g.fill(x, y, x + minecraft.font.width(line) + PADDING * 2, y + LINE + PADDING, c.ground());
        Draw.text(g, minecraft.font, line, x + PADDING, y + PADDING / 2 + 1, c.text());
    }

    /* Between two colours, {@code share} of the way from the first, opaque. */
    private static int blend(final int from, final int to, final double share) {
        int colour = CHANNEL << 24;
        for (int shift = 0; shift <= 16; shift += 8) {
            final int a = from >> shift & CHANNEL;
            final int b = to >> shift & CHANNEL;
            colour |= (int) Math.round(a + (b - a) * share) << shift;
        }
        return colour;
    }

    /**
     * The colours of the thermal view.
     *
     * @param veil    the veil over the world, what is as warm as the air
     * @param coldest the coldest seen
     * @param cold    colder
     * @param cool    a little colder than the air
     * @param air     what is just apart from the air, either way
     * @param warm    a little warmer than the air: a body
     * @param hot     hot
     * @param hotter  hotter
     * @param hottest nearly the hottest seen
     * @param white   the hottest seen
     * @param text    the reading's text
     * @param ground  the ground behind the reading
     */
    public record Colours(int veil, int coldest, int cold, int cool, int air, int warm, int hot, int hotter,
                          int hottest, int white, int text, int ground) {
    }
}
