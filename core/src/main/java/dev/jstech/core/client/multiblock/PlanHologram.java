/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import com.mojang.blaze3d.platform.NativeImage;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
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
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
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
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * The hologram of a plan being built ({@code PlannedBuild}), drawn as the Structure Projector draws its multiblock:
 * every block still missing stands where it goes as a ghost of itself, pale and see-through, the layer being built
 * next glowing amber; a wrong block is outlined in red; each part has its box drawn round it in a colour of its own,
 * its name, its size and how far it stands written beside it or over it, facing the player, a leader running from the
 * box to the words; and a line across the top of the
 * screen says what is being built now and what it needs. Whoever keeps a plan says what to show, every frame
 * ({@link #show}); a part that forms draws its own model, and leaves the hologram.
 */
@PaletteHolder
public final class PlanHologram {

    /** The hologram's own colours: the next layer's glow, a wrong block's mark, the ground under the words. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "plan_hologram", new Colours(
            0xFF << 24 | 0xFFB43C, 0xFF << 24 | 0xF0564A, 0xD8 << 24 | 0x1A1E24, 0xFF << 24 | 0xE6E6E6,
            0xFF << 24 | 0xFFD278, 0x20 << 24 | 0xFFFFFF, 0xF0 << 24 | 0x18191B, 0x1C << 24,
            0xD7 << 24 | 0x0E1218, 0xFF << 24 | 0xE8B52A, 0xFF << 24 | 0xEEF2F6, 0xFF << 24 | 0xA0ACB6));
    /** How far from the player a plan's hologram is drawn, in blocks. */
    public static final double RANGE = 64.0;
    private static final ResourceLocation BANNER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "plan_banner");
    private static final float GHOST_ALPHA = 0.65F;
    /* How the world shades a face by the way it looks: the bottom, north and south, east and west. */
    private static final float SHADE_DOWN = 0.5F;
    private static final float SHADE_NORTH_SOUTH = 0.8F;
    private static final float SHADE_EAST_WEST = 0.6F;
    private static final float GHOST_SCALE = 0.98F;
    /* A ghost's lines a hair over its faces, so they never flicker with them; every other row of its texels. */
    private static final float SCAN_SCALE = 0.985F;
    private static final int SCAN_TEXELS = 16;
    /* How much of its part's colour a ghost takes, and how strongly its edges show. */
    private static final float TINT = 0.12F;
    private static final float EDGE_ALPHA = 0.55F;
    private static final float TEXT_SCALE = 0.025F;
    /* A part's name stands larger than its size and state under it, on one plate with them. */
    private static final float NAME_SCALE = 1.6F;
    private static final int LINE_HEIGHT = 10;
    private static final int PLATE_PAD = 2;
    private static final float PLATE_DEPTH = 0.01F;
    private static final double LABEL_GAP = 0.6;
    private static final double GROW = 0.004;
    private static final int BANNER_TOP = 6;
    private static final int BANNER_PAD = 3;
    private static final float CHANNEL = 255.0F;
    private static final ResourceLocation TAG_LAYER = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "plan_tags");
    /*
     * A part's tag on the screen, as the approved drawings lay tags out on a screen 540 of their pixels high: the
     * heights it is tried at over its part, the nearest first, then under it, and the other way round for a part in
     * the screen's lower half; its padding, its size and state written smaller under its name, the room kept round it
     * from other tags, and the gap kept from the screen's edges, in the screen's scaled pixels.
     */
    private static final int[] TAG_UPS = {150, 185, 220, 255, 115, 80, 45};
    private static final int[] TAG_DOWNS = {-110, -145, -180, -75, -40};
    private static final float TAG_SCREEN = 540F;
    private static final float TAG_UPPER = 280F;
    private static final int TAG_PAD = 5;
    private static final int TAG_TOP = 2;
    private static final float TAG_SMALL = 0.75F;
    private static final int TAG_ROOM = 3;
    private static final int TAG_EDGE = 2;

    private static final List<Supplier<List<Shown>>> SOURCES = new CopyOnWriteArrayList<>();
    private static volatile @Nullable Text banner;
    /* The tags of the parts of the holograms drawn in the last frame. */
    private static volatile List<Tag> tags = List.of();
    /* The picture of a ghost's lines, made the first time a ghost is drawn. */
    private static @Nullable ResourceLocation scanLines;

    private PlanHologram() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, PlanHologram::onRender);
        modBus.addListener(RegisterGuiLayersEvent.class, event -> {
            event.registerAbove(VanillaGuiLayers.CHAT, TAG_LAYER, PlanHologram::drawTags);
            event.registerAbove(TAG_LAYER, BANNER, PlanHologram::drawBanner);
        });
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
            tags = List.of();
            return;
        }
        final Camera camera = event.getCamera();
        final PoseStack pose = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final Matrix4f view = new Matrix4f(event.getModelViewMatrix());
        final Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
        final List<Tag> frameTags = new ArrayList<>();
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
                if (shown.labels() == Labels.TAGS) {
                    tagsOf(shown, camera.getPosition(), view, projection, frameTags);
                }
                if (shown.banner().isPresent() && distance < nearestDistance) {
                    nearest = shown.banner().get();
                    nearestDistance = distance;
                }
            }
        }
        banner = nearest;
        tags = List.copyOf(frameTags);
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        buffers.endBatch(RenderType.lines());
        buffers.endBatch();
        // The ghosts' lines last, over the ghosts already drawn.
        for (final Supplier<List<Shown>> source : SOURCES) {
            for (final Shown shown : source.get()) {
                if (shown.middle().distanceToSqr(camera.getPosition()) <= RANGE * RANGE) {
                    scans(pose, camera.getPosition(), buffers, shown);
                }
            }
        }
        buffers.endBatch();
    }

    /*
     * The ghosts of the blocks still missing, each a little of its part's colour, its edges drawn in it and lines
     * across it as a screen's; the next layer's amber; and the red mark of a wrong block.
     */
    private static void ghosts(final PoseStack pose, final Vec3 eye, final MultiBufferSource buffers,
                               final BlockRenderDispatcher blocks, final Shown shown) {
        final Colours c = PALETTE.get();
        final VertexConsumer faded = new FadedConsumer(buffers.getBuffer(Sheets.translucentCullBlockSheet()),
                GHOST_ALPHA);
        // The ghosts of a plan stand as one shell: a face against another ghost is not drawn.
        final Set<BlockPos> ghosts = new HashSet<>();
        for (final Place place : shown.places()) {
            if (place.look() != Look.WRONG) {
                ghosts.add(place.at());
            }
        }
        final RandomSource random = RandomSource.create();
        for (final Place place : shown.places()) {
            final BlockPos at = place.at();
            if (place.look() == Look.WRONG) {
                pose.pushPose();
                pose.translate(-eye.x, -eye.y, -eye.z);
                box(pose, buffers.getBuffer(RenderType.lines()), new AABB(at).inflate(GROW), c.wrong());
                pose.popPose();
                continue;
            }
            final boolean next = place.look() == Look.NEXT;
            final int tint = next ? c.next() : place.colour();
            final float share = next ? 1F : TINT;
            final float[] colour = {1F - share + share * red(tint), 1F - share + share * green(tint),
                    1F - share + share * blue(tint)};
            pose.pushPose();
            pose.translate(at.getX() - eye.x + 0.5, at.getY() - eye.y + 0.5, at.getZ() - eye.z + 0.5);
            pose.pushPose();
            pose.scale(GHOST_SCALE, GHOST_SCALE, GHOST_SCALE);
            pose.translate(-0.5, -0.5, -0.5);
            final BakedModel model = blocks.getBlockModel(place.ghost());
            for (final Direction face : Direction.values()) {
                if (!ghosts.contains(at.relative(face))) {
                    quads(pose, faded, model.getQuads(place.ghost(), face, random, ModelData.EMPTY, null), colour);
                }
            }
            quads(pose, faded, model.getQuads(place.ghost(), null, random, ModelData.EMPTY, null), colour);
            pose.popPose();
            pose.popPose();
            if (!next) {
                pose.pushPose();
                pose.translate(-eye.x, -eye.y, -eye.z);
                LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), new AABB(at).deflate(GROW),
                        red(place.colour()), green(place.colour()), blue(place.colour()), EDGE_ALPHA);
                pose.popPose();
            }
        }
    }

    /* A ghost's quads in {@code colour}, each shaded as the world shades the way it faces. */
    private static void quads(final PoseStack pose, final VertexConsumer to, final List<BakedQuad> quads,
                              final float[] colour) {
        for (final BakedQuad quad : quads) {
            final float shade = quad.isShade() ? shade(quad.getDirection()) : 1F;
            to.putBulkData(pose.last(), quad, colour[0] * shade, colour[1] * shade, colour[2] * shade, 1F,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }

    /* How bright the world draws a face looking {@code way}: the top whole, the sides less, the bottom least. */
    private static float shade(final Direction way) {
        return switch (way) {
            case DOWN -> SHADE_DOWN;
            case UP -> 1F;
            case NORTH, SOUTH -> SHADE_NORTH_SOUTH;
            case EAST, WEST -> SHADE_EAST_WEST;
        };
    }

    /* The lines across the ghosts of {@code shown}, over them once they are drawn, as a screen's. */
    private static void scans(final PoseStack pose, final Vec3 eye, final MultiBufferSource buffers,
                              final Shown shown) {
        final Set<BlockPos> ghosts = new HashSet<>();
        for (final Place place : shown.places()) {
            if (place.look() != Look.WRONG) {
                ghosts.add(place.at());
            }
        }
        final VertexConsumer to = buffers.getBuffer(RenderType.entityTranslucentCull(scanLines(PALETTE.get())));
        for (final BlockPos at : ghosts) {
            pose.pushPose();
            pose.translate(at.getX() - eye.x + 0.5, at.getY() - eye.y + 0.5, at.getZ() - eye.z + 0.5);
            pose.scale(SCAN_SCALE, SCAN_SCALE, SCAN_SCALE);
            scan(pose, to, ghosts, at);
            pose.popPose();
        }
    }

    /*
     * The lines across the faces of the ghost at {@code place} not against another of {@code ghosts}, the pose
     * standing on its middle.
     */
    private static void scan(final PoseStack pose, final VertexConsumer to, final Set<BlockPos> ghosts,
                             final BlockPos place) {
        final PoseStack.Pose at = pose.last();
        final float h = 0.5F;
        for (final Direction face : Direction.values()) {
            if (ghosts.contains(place.relative(face))) {
                continue;
            }
            final Vec3 n = Vec3.atLowerCornerOf(face.getNormal());
            final Direction first = face.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP;
            final Vec3 u = Vec3.atLowerCornerOf(first.getNormal());
            final Vec3 v = n.cross(u);
            final Vec3 centre = n.scale(h);
            final Vec3[] corners = {centre.subtract(u.scale(h)).subtract(v.scale(h)),
                    centre.add(u.scale(h)).subtract(v.scale(h)), centre.add(u.scale(h)).add(v.scale(h)),
                    centre.subtract(u.scale(h)).add(v.scale(h))};
            final float[][] uv = {{0, 1}, {0, 0}, {1, 0}, {1, 1}};
            for (int k = 0; k < corners.length; k++) {
                to.addVertex(at, (float) corners[k].x, (float) corners[k].y, (float) corners[k].z)
                        .setColor(-1).setUv(uv[k][0], uv[k][1]).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT).setNormal(at, (float) n.x, (float) n.y, (float) n.z);
            }
        }
    }

    /* The picture of a ghost's lines: every other row of its texels dark, the rest clear. */
    private static ResourceLocation scanLines(final Colours c) {
        if (scanLines == null) {
            final NativeImage image = new NativeImage(SCAN_TEXELS, SCAN_TEXELS, true);
            for (int y = 0; y < SCAN_TEXELS; y++) {
                for (int x = 0; x < SCAN_TEXELS; x++) {
                    // Kept as ABGR: the scan's dark is black, so only its alpha moves.
                    image.setPixelRGBA(x, y, y % 2 == 1 ? c.scan() : 0);
                }
            }
            scanLines = Minecraft.getInstance().getTextureManager().register("jscore_plan_scan",
                    new DynamicTexture(image));
        }
        return scanLines;
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
            if (shown.labels() == Labels.TAGS) {
                // Its words are a tag on the screen, laid out over the frame drawn.
                pose.popPose();
                continue;
            }
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
            // Its name large in its colour over its size and state, both on one dark plate, the plate seen through.
            final Component name = GameText.component(part.name());
            final Component state = GameText.component(part.state());
            final float nameHigh = LINE_HEIGHT * NAME_SCALE;
            final float top = -(nameHigh + LINE_HEIGHT) / 2;
            final float wide = Math.max(font.width(name) * NAME_SCALE, font.width(state));
            plate(pose, buffers.getBuffer(RenderType.textBackgroundSeeThrough()), -PLATE_PAD, top - PLATE_PAD,
                    wide + PLATE_PAD, top + nameHigh + LINE_HEIGHT + PLATE_PAD, c.ground());
            pose.pushPose();
            pose.translate(0, top, 0);
            pose.scale(NAME_SCALE, NAME_SCALE, 1F);
            words(font, name, 0, part.colour(), pose, buffers);
            pose.popPose();
            words(font, state, top + nameHigh, c.words(), pose, buffers);
            pose.popPose();
        }
    }

    /* Words of a part's label at {@code y}: faint where something stands between, as the projector writes; whole. */
    private static void words(final Font font, final Component text, final float y, final int colour,
                              final PoseStack pose, final MultiBufferSource buffers) {
        final Colours c = PALETTE.get();
        font.drawInBatch(text, 0, y, c.faint(), false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0,
                LightTexture.FULL_BRIGHT);
        font.drawInBatch(text, 0, y, colour, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0,
                LightTexture.FULL_BRIGHT);
    }

    /* The dark plate under a part's words, a hair behind them. */
    private static void plate(final PoseStack pose, final VertexConsumer to, final float x0, final float y0,
                              final float x1, final float y1, final int colour) {
        final Matrix4f at = pose.last().pose();
        to.addVertex(at, x0, y0, PLATE_DEPTH).setColor(colour).setLight(LightTexture.FULL_BRIGHT);
        to.addVertex(at, x0, y1, PLATE_DEPTH).setColor(colour).setLight(LightTexture.FULL_BRIGHT);
        to.addVertex(at, x1, y1, PLATE_DEPTH).setColor(colour).setLight(LightTexture.FULL_BRIGHT);
        to.addVertex(at, x1, y0, PLATE_DEPTH).setColor(colour).setLight(LightTexture.FULL_BRIGHT);
    }

    /* The tags of the parts of {@code shown}: where the top of each one's box falls on the screen, as drawn now. */
    private static void tagsOf(final Shown shown, final Vec3 eye, final Matrix4f view, final Matrix4f projection,
                               final List<Tag> out) {
        final Minecraft minecraft = Minecraft.getInstance();
        for (final Part part : shown.parts()) {
            final Vec3 top = new Vec3(part.box().getCenter().x, part.box().maxY, part.box().getCenter().z);
            final Vector4f at = new Vector4f((float) (top.x - eye.x), (float) (top.y - eye.y),
                    (float) (top.z - eye.z), 1F);
            view.transform(at);
            projection.transform(at);
            if (at.w <= 0) {
                continue;
            }
            out.add(new Tag((at.x / at.w + 1) / 2 * minecraft.getWindow().getGuiScaledWidth(),
                    (1 - at.y / at.w) / 2 * minecraft.getWindow().getGuiScaledHeight(), part.name(), part.state()));
        }
    }

    /*
     * The parts' tags on the screen, each lifted over its part, or hung under it, at the first height where it meets
     * no tag laid before it and stays on the screen; its name over its size and state on a dark ground, a line along
     * its top, a leader down to the top of its part's box.
     */
    private static void drawTags(final GuiGraphics g, final DeltaTracker delta) {
        final List<Tag> now = tags;
        final Minecraft minecraft = Minecraft.getInstance();
        if (now.isEmpty() || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        final Colours c = PALETTE.get();
        final Font font = minecraft.font;
        final float scale = g.guiHeight() / TAG_SCREEN;
        final int high = TAG_TOP * 2 + font.lineHeight + Math.round(font.lineHeight * TAG_SMALL);
        final List<int[]> laid = new ArrayList<>();
        for (final Tag tag : now) {
            final String name = GameText.resolve(tag.name()).toUpperCase(Locale.ROOT);
            final String state = GameText.resolve(tag.state());
            final int wide = Math.max(font.width(name), Math.round(font.width(state) * TAG_SMALL)) + TAG_PAD * 2;
            final int x = Math.round(tag.x());
            final int y = Math.round(tag.y());
            final boolean upper = tag.y() < TAG_UPPER * scale;
            int top = Integer.MIN_VALUE;
            for (final int[] heights : upper ? new int[][] {TAG_UPS, TAG_DOWNS} : new int[][] {TAG_DOWNS, TAG_UPS}) {
                for (int k = 0; k < heights.length && top == Integer.MIN_VALUE; k++) {
                    final int lift = Math.round(heights[k] * scale);
                    final int tried = lift >= 0 ? y - lift - high : y - lift;
                    if (clear(g, laid, x - wide / 2, tried, wide, high)) {
                        top = tried;
                    }
                }
            }
            if (top == Integer.MIN_VALUE) {
                final int lift = Math.round((upper ? TAG_UPS[0] : TAG_DOWNS[0]) * scale);
                top = lift >= 0 ? y - lift - high : y - lift;
            }
            final int x0 = x - wide / 2;
            laid.add(new int[] {x0 - TAG_ROOM, top - TAG_ROOM, x0 + wide + TAG_ROOM, top + high + TAG_ROOM});
            g.fill(x0, top, x0 + wide, top + high, c.tagGround());
            g.hLine(x0, x0 + wide - 1, top, c.tagLine());
            if (top + high <= y) {
                g.vLine(x, top + high - 1, y, c.tagLine());
            } else {
                g.vLine(x, y, top, c.tagLine());
            }
            g.fill(x - 1, y - 1, x + 2, y + 2, c.tagLine());
            Draw.text(g, font, name, x0 + TAG_PAD, top + TAG_TOP, c.tagName(), c.tagGround());
            Draw.textScaled(g, font, state, x0 + TAG_PAD, top + TAG_TOP + font.lineHeight, c.tagWords(), TAG_SMALL);
        }
    }

    /* Whether a tag at {@code x, y}, {@code wide} by {@code high}, stays on the screen and meets none laid. */
    private static boolean clear(final GuiGraphics g, final List<int[]> laid, final int x, final int y,
                                 final int wide, final int high) {
        final int[] box = {x - TAG_ROOM, y - TAG_ROOM, x + wide + TAG_ROOM, y + high + TAG_ROOM};
        if (box[0] < TAG_EDGE || box[2] > g.guiWidth() - TAG_EDGE || box[1] < TAG_EDGE
                || box[3] > g.guiHeight() - TAG_EDGE) {
            return false;
        }
        return laid.stream().allMatch(other -> box[2] <= other[0] || box[0] >= other[2] || box[3] <= other[1]
                || box[1] >= other[3]);
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
                c.bannerGround());
        Draw.text(g, minecraft.font, words, x, BANNER_TOP, c.banner(), c.bannerGround());
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
     * @param labels how its parts' words are written
     */
    public record Shown(Vec3 middle, List<Place> places, List<Part> parts, Optional<Text> banner, Labels labels) {

        public Shown {
            places = List.copyOf(places);
            parts = List.copyOf(parts);
            Objects.requireNonNull(labels, "labels");
        }

        /** A hologram whose parts' words stand beside them. */
        public Shown(final Vec3 middle, final List<Place> places, final List<Part> parts,
                     final Optional<Text> banner) {
            this(middle, places, parts, banner, Labels.BESIDE);
        }
    }

    /**
     * A place of a hologram.
     *
     * @param at     where it is in the world
     * @param ghost  the block that goes there, drawn as its ghost
     * @param look   how it is shown
     * @param colour the colour of its part, which its ghost is tinted a little with and its edges drawn in
     */
    public record Place(BlockPos at, BlockState ghost, Look look, int colour) {
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

    /** How a hologram writes its parts' words. */
    public enum Labels {
        /** Beside each part's box, in the world, facing the player, a leader from its side: as up a stack of parts. */
        BESIDE,
        /**
         * As name tags on the screen, each lifted over its part or hung under it on a leader from the top of its box,
         * at the first height where it meets no other tag and stays on the screen: as over parts laid side by side.
         * Its name is written in capitals; its size and state as they are given.
         */
        TAGS
    }

    /* A part's tag, where the top of its box falls on the screen, in its scaled pixels. */
    private record Tag(float x, float y, Text name, Text state) {
    }

    /**
     * The hologram's colours.
     *
     * @param next         the glow of the layer being built next
     * @param wrong        the mark of a wrong block
     * @param ground       the dark plate under a part's words
     * @param words        the words of a part's size and state
     * @param banner       the line across the top of the screen
     * @param faint        the words as they show through what stands between, faint
     * @param bannerGround the dark ground under the banner
     * @param scan         the dark of every other line drawn across a ghost
     * @param tagGround    the dark ground of a part's tag on the screen
     * @param tagLine      the line along a tag's top, and its leader
     * @param tagName      a tag's name
     * @param tagWords     a tag's size and state
     */
    public record Colours(int next, int wrong, int ground, int words, int banner, int faint, int bannerGround,
                          int scan, int tagGround, int tagLine, int tagName, int tagWords) {
    }
}
