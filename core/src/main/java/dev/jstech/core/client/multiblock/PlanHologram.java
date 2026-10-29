/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The hologram of a plan being built ({@code PlannedBuild}), drawn as the Structure Projector draws its multiblock:
 * every block still missing stands where it goes as a ghost of itself, pale and see-through, the layer being built
 * next glowing amber; a wrong block is outlined in red; each part has its box drawn round it in a colour of its own,
 * its name, its size and how far it stands written beside it, facing the player; and a line across the top of the
 * screen says what is being built now and what it needs. Whoever keeps a plan says what to show, every frame
 * ({@link #show}); a part that forms draws its own model, and leaves the hologram.
 */
@PaletteHolder
public final class PlanHologram {

    /** The hologram's own colours: the next layer's glow, a wrong block's mark, the ground under the words. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "plan_hologram", new Colours(
            0xFF << 24 | 0xFFB43C, 0xFF << 24 | 0xF0564A, 0xC0 << 24 | 0x1A1E24, 0xFF << 24 | 0xC8CCD2,
            0xFF << 24 | 0xFFC850, 0x20 << 24 | 0xFFFFFF));
    /** How far from the player a plan's hologram is drawn, in blocks. */
    public static final double RANGE = 64.0;
    private static final ResourceLocation BANNER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "plan_banner");
    private static final float GHOST_ALPHA = 0.45F;
    private static final float GHOST_SCALE = 0.98F;
    private static final float TEXT_SCALE = 0.025F;
    private static final int LINE_HEIGHT = 10;
    private static final double LABEL_GAP = 0.6;
    private static final double GROW = 0.004;
    private static final int BANNER_TOP = 6;
    private static final int BANNER_PAD = 3;
    private static final float CHANNEL = 255.0F;

    private static final List<Supplier<List<Shown>>> SOURCES = new CopyOnWriteArrayList<>();
    private static volatile @Nullable Text banner;

    private PlanHologram() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, PlanHologram::onRender);
        modBus.addListener(RegisterGuiLayersEvent.class, event -> event.registerAbove(VanillaGuiLayers.CHAT, BANNER,
                PlanHologram::drawBanner));
    }

    /** Shows what {@code source} gives each frame: the holograms of the plans it keeps near the player. */
    public static void show(final Supplier<List<Shown>> source) {
        SOURCES.add(source);
    }

    /** The line across the top of the screen last drawn, if one was; for a test to read. */
    public static Optional<Text> banner() {
        return Optional.ofNullable(banner);
    }

    /* Every hologram near the player, drawn after the world's see-through blocks. */
    private static void onRender(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            banner = null;
            return;
        }
        final Camera camera = event.getCamera();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Text nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (final Supplier<List<Shown>> source : SOURCES) {
            for (final Shown shown : source.get()) {
                final double distance = shown.middle().distanceToSqr(camera.getPosition());
                if (distance > RANGE * RANGE) {
                    continue;
                }
                ghosts(pose, camera.getPosition(), buffers, minecraft.getBlockRenderer(), shown);
                parts(pose, camera, minecraft.font, buffers, shown);
                if (shown.banner().isPresent() && distance < nearestDistance) {
                    nearest = shown.banner().get();
                    nearestDistance = distance;
                }
            }
        }
        banner = nearest;
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(RenderType.lines());
        buffers.endBatch();
    }

    /* The ghosts of the blocks still missing, the next layer's amber, and the red mark of a wrong block. */
    private static void ghosts(final PoseStack pose, final Vec3 eye, final MultiBufferSource buffers,
                               final BlockRenderDispatcher blocks, final Shown shown) {
        final Colours c = PALETTE.get();
        final VertexConsumer faded = new FadedConsumer(buffers.getBuffer(Sheets.translucentCullBlockSheet()),
                GHOST_ALPHA);
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (final Place place : shown.places()) {
            final BlockPos at = place.at();
            if (place.look() == Look.WRONG) {
                pose.pushPose();
                pose.translate(-eye.x, -eye.y, -eye.z);
                box(pose, lines, new AABB(at).inflate(GROW), c.wrong());
                pose.popPose();
                continue;
            }
            final boolean next = place.look() == Look.NEXT;
            pose.pushPose();
            pose.translate(at.getX() - eye.x + 0.5, at.getY() - eye.y + 0.5, at.getZ() - eye.z + 0.5);
            pose.scale(GHOST_SCALE, GHOST_SCALE, GHOST_SCALE);
            pose.translate(-0.5, -0.5, -0.5);
            blocks.getModelRenderer().renderModel(pose.last(), faded, place.ghost(),
                    blocks.getBlockModel(place.ghost()), next ? red(c.next()) : 1F, next ? green(c.next()) : 1F,
                    next ? blue(c.next()) : 1F, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY,
                    null);
            pose.popPose();
        }
    }

    /* Each part's box in its colour, and beside it, facing the player, its name and how far it stands. */
    private static void parts(final PoseStack pose, final Camera camera, final Font font,
                              final MultiBufferSource buffers, final Shown shown) {
        final Colours c = PALETTE.get();
        final Vec3 eye = camera.getPosition();
        final Vec3 right = new Vec3(camera.getLeftVector()).reverse();
        for (final Part part : shown.parts()) {
            final AABB box = part.box();
            // Asked for again for each part: the words drawn after the last one took the buffers on to their own.
            final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
            pose.pushPose();
            pose.translate(-eye.x, -eye.y, -eye.z);
            box(pose, lines, box.inflate(GROW * 2), part.colour());
            // A leader from the box's side out to its words.
            final Vec3 middle = box.getCenter();
            final double half = Math.max(box.getXsize(), box.getZsize()) / 2;
            final Vec3 side = middle.add(right.scale(half));
            final Vec3 label = middle.add(right.scale(half + LABEL_GAP));
            line(pose, lines, side, label, part.colour());
            pose.popPose();
            pose.pushPose();
            pose.translate(label.x - eye.x, label.y - eye.y, label.z - eye.z);
            pose.mulPose(camera.rotation());
            pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
            final List<Line> words = List.of(new Line(GameText.component(part.name()), part.colour()),
                    new Line(GameText.component(part.state()), c.words()));
            // As the projector writes: the ground and faint words seen through what stands between, then the words.
            for (int i = 0; i < words.size(); i++) {
                final float y = (i - words.size() / 2.0F) * LINE_HEIGHT;
                font.drawInBatch(words.get(i).text(), 0, y, c.faint(), false, pose.last().pose(), buffers,
                        Font.DisplayMode.SEE_THROUGH, c.ground(), LightTexture.FULL_BRIGHT);
                font.drawInBatch(words.get(i).text(), 0, y, words.get(i).colour(), false, pose.last().pose(),
                        buffers, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
            }
            pose.popPose();
        }
    }

    /* The line across the top of the screen: what is being built now, of the nearest plan. */
    private static void drawBanner(final GuiGraphics g, final DeltaTracker delta) {
        final Text line = banner;
        final Minecraft minecraft = Minecraft.getInstance();
        if (line == null || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        final Colours c = PALETTE.get();
        final String words = GameText.resolve(line);
        final int width = minecraft.font.width(words);
        final int x = (g.guiWidth() - width) / 2;
        g.fill(x - BANNER_PAD, BANNER_TOP - BANNER_PAD, x + width + BANNER_PAD, BANNER_TOP + 8 + BANNER_PAD,
                c.ground());
        Draw.text(g, minecraft.font, words, x, BANNER_TOP, c.banner(), c.ground());
    }

    private static void box(final PoseStack pose, final VertexConsumer lines, final AABB box, final int colour) {
        LevelRenderer.renderLineBox(pose, lines, box, red(colour), green(colour), blue(colour), 1F);
    }

    private static void line(final PoseStack pose, final VertexConsumer lines, final Vec3 from, final Vec3 to,
                             final int colour) {
        final Vec3 way = to.subtract(from).normalize();
        final PoseStack.Pose at = pose.last();
        lines.addVertex(at, (float) from.x, (float) from.y, (float) from.z).setColor(colour)
                .setNormal(at, (float) way.x, (float) way.y, (float) way.z);
        lines.addVertex(at, (float) to.x, (float) to.y, (float) to.z).setColor(colour)
                .setNormal(at, (float) way.x, (float) way.y, (float) way.z);
    }

    private static float red(final int colour) {
        return FastColor.ARGB32.red(colour) / CHANNEL;
    }

    private static float green(final int colour) {
        return FastColor.ARGB32.green(colour) / CHANNEL;
    }

    private static float blue(final int colour) {
        return FastColor.ARGB32.blue(colour) / CHANNEL;
    }

    /** How a place of the hologram is shown. */
    public enum Look {
        /** A block still missing: its ghost. */
        MISSING,
        /** A block of the layer being built next: its ghost, glowing amber. */
        NEXT,
        /** Another block where it goes: outlined in red. */
        WRONG
    }

    /**
     * A plan's hologram as shown now.
     *
     * @param middle where it stands, for how near the player is
     * @param places the places shown, those not yet right
     * @param parts  its parts, each with its box and words
     * @param banner the line across the top of the screen while it is the nearest; none for no line
     */
    public record Shown(Vec3 middle, List<Place> places, List<Part> parts, Optional<Text> banner) {

        public Shown {
            places = List.copyOf(places);
            parts = List.copyOf(parts);
        }
    }

    /**
     * A place of a hologram.
     *
     * @param at    where it is in the world
     * @param ghost the block that goes there, drawn as its ghost
     * @param look  how it is shown
     */
    public record Place(BlockPos at, BlockState ghost, Look look) {
    }

    /**
     * A part of a hologram.
     *
     * @param name   what it is
     * @param state  its size and how far it stands
     * @param colour the colour of its box and name
     * @param box    the box round it, in the world
     */
    public record Part(Text name, Text state, int colour, AABB box) {
    }

    /* One line of words beside a part, and its colour. */
    private record Line(Component text, int colour) {
    }

    /**
     * The hologram's colours.
     *
     * @param next   the glow of the layer being built next
     * @param wrong  the mark of a wrong block
     * @param ground the dark ground under the words and the banner
     * @param words  the words of a part's size and state
     * @param banner the line across the top of the screen
     * @param faint  the words as they show through what stands between, faint
     */
    public record Colours(int next, int wrong, int ground, int words, int banner, int faint) {
    }
}
