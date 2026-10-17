/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.room;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.JsCore;
import dev.jstech.core.air.AirMath;
import dev.jstech.core.air.AirMix;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.room.RoomOutline;
import dev.jstech.core.room.RoomTexts;
import dev.jstech.core.room.RoomView;
import dev.jstech.core.room.RoomViews;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;

/**
 * A sealed room as a player sees it through goggles or an Atmosphere Detector: its outline in moving dashes, seen
 * through its walls, the blocks it leaks through marked, and beside the crosshair what its air reads, its leaks named.
 */
@PaletteHolder
public final class RoomViewClient {

    /** The view's colours. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "room_view", new Colours(
            0xFF << 24 | 0x55FFFF, 0x70 << 24 | 0xFF4040, 0x48 << 24 | 0xFF5555, 0xC8 << 24 | 0x101418,
            0xFF << 24 | 0xFFFFFF, 0xFF << 24 | 0xFFB13B, 0xFF << 24 | 0xFF6060, 0xFF << 24 | 0x6EE07A,
            0xFF << 24 | 0x9A9A9A));
    /* A dash and the gap after it, in blocks, and how fast they move along, in blocks a tick. */
    private static final double DASH = 0.35;
    private static final double GAP = 0.2;
    private static final double CRAWL = 0.02;
    private static final float LINE_WIDTH = 2.0F;
    private static final double LIFT = 0.002;
    private static final float CHANNEL = 255F;
    /* Where the words stand from the crosshair, their margin, and the gap between lines, in the GUI's pixels. */
    private static final int PANEL_X = 24;
    private static final int PANEL_Y = 8;
    private static final int MARGIN = 4;
    private static final int LINE = 10;

    private RoomViewClient() {
    }

    /** Hands the game's bus what this answers, once, from the Core's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, RoomViewClient::onStage);
        NeoForge.EVENT_BUS.addListener(RenderGuiEvent.Post.class, event -> panel(event.getGuiGraphics()));
    }

    /** The room shown now, if any. */
    public static RoomView shown() {
        final ClientLevel level = Minecraft.getInstance().level;
        return level == null ? RoomView.NONE : RoomViews.seen(level.getGameTime());
    }

    private static void onStage(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final RoomView view = shown();
        final ClientLevel level = Minecraft.getInstance().level;
        if (!view.shown() || level == null) {
            return;
        }
        final Colours c = PALETTE.get();
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(-camera.x, -camera.y, -camera.z);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        marks(pose, view.leaks(), c);
        outline(pose, view.outline(), c, level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(
                false));
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        pose.popPose();
    }

    /* The room's outline, in dashes moving along it. */
    private static void outline(final PoseStack pose, final List<RoomOutline.Segment> segments, final Colours c,
                                final double time) {
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
        RenderSystem.lineWidth(LINE_WIDTH);
        final BufferBuilder lines = Tesselator.getInstance().begin(VertexFormat.Mode.LINES,
                DefaultVertexFormat.POSITION_COLOR_NORMAL);
        final PoseStack.Pose last = pose.last();
        final double shift = time * CRAWL % (DASH + GAP);
        for (final RoomOutline.Segment segment : segments) {
            final Vec3 from = new Vec3(segment.x1(), segment.y1(), segment.z1());
            final Vec3 to = new Vec3(segment.x2(), segment.y2(), segment.z2());
            final double length = from.distanceTo(to);
            final Vec3 along = to.subtract(from).normalize();
            for (double at = shift - DASH; at < length; at += DASH + GAP) {
                final double start = Math.max(0, at);
                final double end = Math.min(length, at + DASH);
                if (end <= start) {
                    continue;
                }
                vertex(lines, last, from.add(along.scale(start)), along, c.outline());
                vertex(lines, last, from.add(along.scale(end)), along, c.outline());
            }
        }
        final MeshData mesh = lines.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    /* The blocks the room leaks through, tinted, and a hole outlined. */
    private static void marks(final PoseStack pose, final List<RoomView.Leak> leaks, final Colours c) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        final BufferBuilder quads = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        final Matrix4f matrix = pose.last().pose();
        for (final RoomView.Leak leak : leaks) {
            final AABB box = new AABB(leak.block()).inflate(LIFT);
            for (final Direction face : Direction.values()) {
                face(quads, matrix, box, face, leak.hole() ? c.hole() : c.leak());
            }
        }
        final MeshData mesh = quads.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    /* One face of {@code box}, in {@code colour}. */
    private static void face(final BufferBuilder quads, final Matrix4f matrix, final AABB box, final Direction face,
                             final int colour) {
        final float r = (colour >> 16 & 0xFF) / CHANNEL;
        final float g = (colour >> 8 & 0xFF) / CHANNEL;
        final float b = (colour & 0xFF) / CHANNEL;
        final float a = (colour >>> 24) / CHANNEL;
        final double x0 = face == Direction.EAST ? box.maxX : box.minX;
        final double x1 = face == Direction.WEST ? box.minX : box.maxX;
        final double y0 = face == Direction.UP ? box.maxY : box.minY;
        final double y1 = face == Direction.DOWN ? box.minY : box.maxY;
        final double z0 = face == Direction.SOUTH ? box.maxZ : box.minZ;
        final double z1 = face == Direction.NORTH ? box.minZ : box.maxZ;
        switch (face.getAxis()) {
            case X -> {
                quads.addVertex(matrix, (float) x0, (float) y0, (float) z0).setColor(r, g, b, a);
                quads.addVertex(matrix, (float) x0, (float) y1, (float) z0).setColor(r, g, b, a);
                quads.addVertex(matrix, (float) x0, (float) y1, (float) z1).setColor(r, g, b, a);
                quads.addVertex(matrix, (float) x0, (float) y0, (float) z1).setColor(r, g, b, a);
            }
            case Y -> {
                quads.addVertex(matrix, (float) x0, (float) y0, (float) z0).setColor(r, g, b, a);
                quads.addVertex(matrix, (float) x1, (float) y0, (float) z0).setColor(r, g, b, a);
                quads.addVertex(matrix, (float) x1, (float) y0, (float) z1).setColor(r, g, b, a);
                quads.addVertex(matrix, (float) x0, (float) y0, (float) z1).setColor(r, g, b, a);
            }
            case Z -> {
                quads.addVertex(matrix, (float) x0, (float) y0, (float) z0).setColor(r, g, b, a);
                quads.addVertex(matrix, (float) x1, (float) y0, (float) z0).setColor(r, g, b, a);
                quads.addVertex(matrix, (float) x1, (float) y1, (float) z0).setColor(r, g, b, a);
                quads.addVertex(matrix, (float) x0, (float) y1, (float) z0).setColor(r, g, b, a);
            }
        }
    }

    private static void vertex(final BufferBuilder lines, final PoseStack.Pose pose, final Vec3 at, final Vec3 along,
                               final int colour) {
        lines.addVertex(pose, (float) at.x, (float) at.y, (float) at.z).setColor(colour)
                .setNormal(pose, (float) along.x, (float) along.y, (float) along.z);
    }

    /* Beside the crosshair, what the room's air reads: its size, its pressure, its oxygen, its leaks, who shows it. */
    private static void panel(final GuiGraphics graphics) {
        final Minecraft minecraft = Minecraft.getInstance();
        final RoomView view = shown();
        if (!view.shown() || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        final Colours c = PALETTE.get();
        final double oxygen = AirMath.partial(view.share(AirMix.OXYGEN), view.pressure());
        final boolean breathable = AirMath.breathable(view.share(AirMix.OXYGEN), view.pressure());
        final List<Line> lines = List.of(
                new Line(RoomTexts.ROOM.with(view.volume()), c.title()),
                new Line(RoomTexts.pressure(view.pressure(), view.change()), view.change() < 0 ? c.warning()
                        : c.title()),
                new Line(RoomTexts.oxygen(oxygen, breathable), breathable ? c.fine() : c.warning()),
                new Line(RoomTexts.leaks(view.leaks()), view.leaks().isEmpty() ? c.fine() : c.danger()),
                new Line(view.source() == RoomView.Source.DETECTOR ? RoomTexts.SEEN_DETECTOR.text()
                        : RoomTexts.SEEN_GOGGLES.text(), c.dim()));
        final Font font = minecraft.font;
        int width = 0;
        for (final Line line : lines) {
            width = Math.max(width, font.width(GameText.component(line.text())));
        }
        // Beside the crosshair on the right, or on the left where the words would run off the screen.
        final int right = graphics.guiWidth() / 2 + PANEL_X;
        final int x = right + width + MARGIN < graphics.guiWidth() ? right
                : graphics.guiWidth() / 2 - PANEL_X - width;
        final int y = graphics.guiHeight() / 2 + PANEL_Y;
        graphics.fill(x - MARGIN, y - MARGIN, x + width + MARGIN, y + lines.size() * LINE + MARGIN - 2, c.panel());
        for (int i = 0; i < lines.size(); i++) {
            Draw.text(graphics, font, GameText.component(lines.get(i).text()), x, y + i * LINE, lines.get(i).colour());
        }
    }

    /* A line of the panel, in its colour. */
    private record Line(Text text, int colour) {
    }

    /**
     * The view's colours.
     *
     * @param outline the room's outline
     * @param leak    a block it leaks through
     * @param hole    a hole in it
     * @param panel   the ground of its words
     * @param title   its size, and a steady pressure
     * @param warning a falling pressure, or thin oxygen
     * @param danger  its leaks
     * @param fine    air fine to breathe, and no leaks
     * @param dim     who shows it
     */
    public record Colours(int outline, int leak, int hole, int panel, int title, int warning, int danger, int fine,
                          int dim) {
    }
}
