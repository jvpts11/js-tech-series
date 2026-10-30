/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.crawler;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.space.client.skin.Picture;
import dev.jstech.space.client.skin.SkinAtlas;
import dev.jstech.space.client.skin.SkinDrawing;
import dev.jstech.space.crawler.CrawlerBlocks;
import dev.jstech.space.crawler.CrawlerEngineBlockEntity;
import dev.jstech.space.crawler.CrawlerLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Vector3f;

/**
 * Draws a formed crawler-transporter whole from its engine, as the approved crawler is drawn: four trucks at its
 * corners, each of two tracks, each track a loop of separate shoes round its sprocket and idler with the road wheels
 * along its foot and two return rollers on top, the truck's frame over them with its guide tube and four leveling jacks
 * up into the hull; the hull with its panelled sides and its deck, the jack pins the launcher rests on, the railing
 * round the deck, the cabs at two opposite corners with their beacons, the exhaust stacks either side and a ladder up
 * one side. With a launcher on it the jacks are up and everything over them is raised.
 *
 * <p>Drawn from the middle of the ground under the engine, along the crawler's length, up, and across it to its right.
 */
public final class CrawlerRenderer implements BlockEntityRenderer<CrawlerEngineBlockEntity> {

    /* The approved crawler's measures, in blocks: its hull, its trucks, their tracks and frames. */
    private static final float X0 = -7.5F;
    private static final float X1 = 7.5F;
    private static final float CZ = 5.4F;
    private static final float HULL_TOP = 3.0F;
    private static final float TRUCK_IN = 2.3F;
    private static final float TRACK_L = 4.2F;
    private static final float TRACK_H = 1.2F;
    private static final float TRACK_W = 1.15F;
    private static final float FRAME_T = 0.32F;
    private static final float JACKS = 0.55F;
    /* A shoe's thickness and how far apart the shoes are, at most. */
    private static final float SHOE = 0.13F;
    private static final float SHOE_PITCH = 0.27F;
    private static final int ROAD_WHEELS = 5;
    /* Where the jack pins stand on the deck, from its corner, and how tall and wide they are. */
    private static final float PIN_IN = 3.0F;
    private static final float PIN_HIGH = 0.35F;
    private static final float PIN_RADIUS = 0.32F;
    /* The railing's height, its lower rail's, and how far apart its posts are. */
    private static final float RAIL_HIGH = 0.8F;
    private static final float RAIL_LOW = 0.45F;
    private static final float POSTS = 1.5F;
    /* A cab's half sizes, how far it reaches below the hull and above the deck, and its middle off the hull. */
    private static final float CAB_HALF_X = 0.85F;
    private static final float CAB_HALF_Z = 0.9F;
    private static final float CAB_BELOW = 0.5F;
    private static final float CAB_ABOVE = 0.25F;
    private static final float CAB_IN = 0.95F;
    /* The exhaust stacks: where along the hull, how far out of its side, how tall. */
    private static final float[] STACKS = {-1.0F, 1.0F};
    private static final float STACK_OUT = 0.15F;
    private static final float STACK_HIGH = 1.3F;
    /* The ladder's rails along the hull, how far out of its side, and how far apart its rungs are. */
    private static final float LADDER_FROM = -3.1F;
    private static final float LADDER_TO = -2.5F;
    private static final float LADDER_OUT = 0.25F;
    private static final float RUNGS = 0.3F;
    private static final int SEEN_FROM = 192;
    private static final float HALF = 0.5F;

    public CrawlerRenderer(final BlockEntityRendererProvider.Context context) {
        // Nothing to keep: the crawler is drawn from its measures each frame.
    }

    /** Hands the mod's bus the renderer, once, from the mod's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(CrawlerBlocks.ENGINE_ENTITY.get(), CrawlerRenderer::new));
    }

    @Override
    public void render(final CrawlerEngineBlockEntity engine, final float partialTick, final PoseStack pose,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Optional<Direction> along = engine.along();
        final Level level = engine.getLevel();
        if (along.isEmpty() || level == null) {
            return;
        }
        final SkinAtlas atlas = SkinAtlas.of(CrawlerRenderer.class, CrawlerRenderer::skins);
        final SkinDrawing drawing = SkinDrawing.in(pose, buffers, atlas, level, engine.getBlockPos(),
                engine.getBlockPos().below(CrawlerLayout.TRACKS), along.get());
        final CrawlerSkins.Colours c = CrawlerSkins.PALETTE.get();
        final float lift = engine.lifted() ? (float) CrawlerEngineBlockEntity.LIFT : 0;
        final float bottom = TRACK_H + FRAME_T + JACKS + lift;
        final float top = HULL_TOP + lift;
        int seed = 0;
        for (final float tx : new float[] {X0 + TRUCK_IN, X1 - TRUCK_IN}) {
            for (final int side : new int[] {-1, 1}) {
                truck(drawing, tx, side * (CZ - TRACK_W - 0.08F), bottom, seed, c);
                seed += 2;
            }
        }
        hull(drawing, bottom, top, c);
        deckThings(drawing, top, c);
        cab(drawing, X0 - CAB_HALF_X, -CZ + CAB_IN, bottom, top, c);
        cab(drawing, X1 + CAB_HALF_X, CZ - CAB_IN, bottom, top, c);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(final CrawlerEngineBlockEntity engine) {
        return true;
    }

    /* Round the whole crawler, its cabs and its stacks. */
    @Override
    public AABB getRenderBoundingBox(final CrawlerEngineBlockEntity engine) {
        return new AABB(engine.getBlockPos()).inflate(CrawlerLayout.HALF_LENGTH + 3, 0,
                CrawlerLayout.HALF_LENGTH + 3).expandTowards(0, -CrawlerLayout.TRACKS, 0).expandTowards(0, 3, 0);
    }

    @Override
    public int getViewDistance() {
        return SEEN_FROM;
    }

    /* Every skin of the crawler, sized to the faces it is laid on. */
    private static Map<String, Picture> skins() {
        final float hull = HULL_TOP - (TRACK_H + FRAME_T + JACKS);
        final float cabHigh = HULL_TOP + CAB_ABOVE - (TRACK_H + FRAME_T + JACKS - CAB_BELOW);
        final Map<String, Picture> out = new HashMap<>();
        out.put("hull_pz", CrawlerSkins.hullSide(X1 - X0, hull, 81));
        out.put("hull_nz", CrawlerSkins.hullSide(X1 - X0, hull, 82));
        out.put("hull_px", CrawlerSkins.hullSide(2 * CZ, hull, 83));
        out.put("hull_nx", CrawlerSkins.hullSide(2 * CZ, hull, 84));
        out.put("deck", CrawlerSkins.deckTop(X1 - X0, 2 * CZ, new double[][] {{PIN_IN, CZ - PIN_IN},
                {PIN_IN, CZ + PIN_IN}, {X1 - X0 - PIN_IN, CZ - PIN_IN}, {X1 - X0 - PIN_IN, CZ + PIN_IN}}));
        out.put("cab_x", CrawlerSkins.cabSide(2 * CAB_HALF_Z, cabHigh));
        out.put("cab_z", CrawlerSkins.cabSide(2 * CAB_HALF_X, cabHigh));
        out.put("cab_y", CrawlerSkins.cabSide(2 * CAB_HALF_X, 2 * CAB_HALF_Z));
        out.put("roof", CrawlerSkins.plate(2 * CAB_HALF_X, 2 * CAB_HALF_Z, CrawlerSkins.PALETTE.get().roof()));
        final float truckLong = TRACK_L - 0.8F;
        final float truckWide = 2 * TRACK_W + 0.2F;
        out.put("truck_x", CrawlerSkins.frame(truckWide, FRAME_T));
        out.put("truck_z", CrawlerSkins.frame(truckLong, FRAME_T));
        out.put("truck_y", CrawlerSkins.frame(truckLong, truckWide));
        out.put("truck_top", CrawlerSkins.plate(truckLong, truckWide, CrawlerSkins.PALETTE.get().truckTop()));
        final Track t = new Track(0, TRACK_L, 0);
        out.put("track_x", CrawlerSkins.frame(t.frameWide(), t.frameHigh()));
        out.put("track_z", CrawlerSkins.frame(t.frameLong(), t.frameHigh()));
        out.put("track_y", CrawlerSkins.frame(t.frameLong(), t.frameWide()));
        return out;
    }

    /*
     * A truck at {@code tx} along and {@code zo} across: its two tracks, its frame on them, its guide tube and four
     * jacks up to the hull's underside at {@code bottom}.
     */
    private static void truck(final SkinDrawing d, final float tx, final float zo, final float bottom, final int seed,
                              final CrawlerSkins.Colours c) {
        int k = 0;
        for (final float zt : new float[] {zo - TRACK_W / 2 - 0.04F, zo + TRACK_W / 2 + 0.04F}) {
            track(d, new Track(tx - TRACK_L / 2, tx + TRACK_L / 2, zt), seed + k, c);
            k++;
        }
        final float fy = TRACK_H;
        final float[] min = {tx - TRACK_L / 2 + 0.4F, fy, zo - TRACK_W - 0.1F};
        final float[] max = {tx + TRACK_L / 2 - 0.4F, fy + FRAME_T, zo + TRACK_W + 0.1F};
        d.box(min, max, face -> switch (face) {
            case PLUS_Y -> SkinDrawing.Paint.skin("truck_top");
            case MINUS_Y -> SkinDrawing.Paint.skin("truck_y");
            case PLUS_X, MINUS_X -> SkinDrawing.Paint.skin("truck_x");
            case PLUS_Z, MINUS_Z -> SkinDrawing.Paint.skin("truck_z");
        });
        final float ft = fy + FRAME_T;
        d.rod(new Vector3f(tx, ft, zo), new Vector3f(tx, bottom + 0.05F, zo), 0.42F, 18, c.tube());
        for (final float dx : new float[] {-0.75F, 0.75F}) {
            for (final float dz : new float[] {-0.55F, 0.55F}) {
                d.rod(new Vector3f(tx + dx, ft, zo + dz), new Vector3f(tx + dx, ft + 0.35F, zo + dz), 0.17F, 12,
                        c.jackFoot());
                d.rod(new Vector3f(tx + dx, ft + 0.35F, zo + dz), new Vector3f(tx + dx, bottom + 0.05F, zo + dz),
                        0.1F, 10, c.jackRod());
            }
        }
    }

    /*
     * One track: a loop of shoes, each a heavy plate with its grouser and the pins at its ends, round the sprocket at
     * one end and the idler at the other; inside it its frame, the road wheels along its foot and two return rollers.
     */
    private static void track(final SkinDrawing d, final Track t, final int seed, final CrawlerSkins.Colours c) {
        final Random shades = new Random(seed);
        final float r = t.radius();
        final float straight = t.xb() - t.xa();
        final float round = (float) (2 * straight + 2 * Math.PI * r);
        final int shoes = (int) (round / SHOE_PITCH);
        final float pitch = round / shoes;
        final float w = TRACK_W;
        for (int k = 0; k < shoes; k++) {
            final float s = (k + HALF) * pitch;
            final Vector3f pos;
            final Vector3f tan;
            final Vector3f nor;
            if (s < straight) {
                pos = new Vector3f(t.xa() + s, t.yc() - r, t.zc());
                tan = new Vector3f(1, 0, 0);
                nor = new Vector3f(0, -1, 0);
            } else if (s < straight + Math.PI * r) {
                final double a = -Math.PI / 2 + (s - straight) / r;
                nor = new Vector3f((float) Math.cos(a), (float) Math.sin(a), 0);
                pos = new Vector3f(t.xb(), t.yc(), t.zc()).add(new Vector3f(nor).mul(r));
                tan = new Vector3f((float) -Math.sin(a), (float) Math.cos(a), 0);
            } else if (s < 2 * straight + Math.PI * r) {
                final float back = (float) (s - straight - Math.PI * r);
                pos = new Vector3f(t.xb() - back, t.yc() + r, t.zc());
                tan = new Vector3f(-1, 0, 0);
                nor = new Vector3f(0, 1, 0);
            } else {
                final double a = Math.PI / 2 + (s - 2 * straight - Math.PI * r) / r;
                nor = new Vector3f((float) Math.cos(a), (float) Math.sin(a), 0);
                pos = new Vector3f(t.xa(), t.yc(), t.zc()).add(new Vector3f(nor).mul(r));
                tan = new Vector3f((float) -Math.sin(a), (float) Math.cos(a), 0);
            }
            final float shade = 0.9F + 0.2F * shades.nextFloat();
            final float half = pitch * 0.44F;
            d.member(new Vector3f(pos).sub(new Vector3f(tan).mul(half)), new Vector3f(pos).add(new Vector3f(tan)
                    .mul(half)), w, SHOE, shaded(c.shoe(), shade));
            final Vector3f grouser = new Vector3f(pos).add(new Vector3f(nor).mul(SHOE / 2 + 0.03F));
            d.member(new Vector3f(grouser).sub(new Vector3f(tan).mul(0.04F)), new Vector3f(grouser)
                    .add(new Vector3f(tan).mul(0.04F)), w - 0.06F, 0.06F, shaded(c.shoeLit(), shade));
            for (final int sz : new int[] {-1, 1}) {
                final Vector3f pin = new Vector3f(pos).add(0, 0, sz * (w / 2 - 0.12F))
                        .sub(new Vector3f(nor).mul(SHOE / 2 + 0.005F));
                d.member(new Vector3f(pin).sub(new Vector3f(tan).mul(0.035F)), new Vector3f(pin)
                        .add(new Vector3f(tan).mul(0.035F)), 0.07F, 0.02F, c.shoePin());
            }
        }
        final float inner = t.inner();
        d.box(new float[] {t.xa() - 0.15F, t.yc() - inner + 0.18F, t.zc() - w / 2 + 0.06F},
                new float[] {t.xb() + 0.15F, t.yc() + inner - 0.12F, t.zc() + w / 2 - 0.06F}, face -> switch (face) {
                    case PLUS_X, MINUS_X -> SkinDrawing.Paint.skin("track_x");
                    case PLUS_Z, MINUS_Z -> SkinDrawing.Paint.skin("track_z");
                    case PLUS_Y, MINUS_Y -> SkinDrawing.Paint.skin("track_y");
                });
        final float wheelY = t.yc() - inner + 0.2F;
        for (int k = 0; k < ROAD_WHEELS; k++) {
            final float wx = t.xa() + 0.1F + k * (straight - 0.2F) / (ROAD_WHEELS - 1);
            d.capped(new Vector3f(wx, wheelY, t.zc() - w / 2 + 0.03F), new Vector3f(wx, wheelY, t.zc() + w / 2 - 0.03F),
                    0.19F, 14, c.wheel(), c.wheelCap());
            for (final int sz : new int[] {-1, 1}) {
                d.rod(new Vector3f(wx, wheelY, t.zc() + sz * (w / 2 - 0.03F)),
                        new Vector3f(wx, wheelY, t.zc() + sz * (w / 2 - 0.01F)), 0.07F, 10, c.hub());
            }
        }
        for (final float k : new float[] {0.3F, 0.7F}) {
            final float rx = t.xa() + straight * k;
            d.rod(new Vector3f(rx, t.yc() + inner - 0.1F, t.zc() - w / 2 + 0.05F),
                    new Vector3f(rx, t.yc() + inner - 0.1F, t.zc() + w / 2 - 0.05F), 0.1F, 12, c.wheel());
        }
        for (final float[] end : new float[][] {{t.xb(), inner - 0.02F}, {t.xa(), inner - 0.06F}}) {
            for (final int sz : new int[] {-1, 1}) {
                d.capped(new Vector3f(end[0], t.yc(), t.zc() + sz * (w / 2 - 0.08F)),
                        new Vector3f(end[0], t.yc(), t.zc() + sz * (w / 2 + 0.02F)), end[1] * 0.55F, 16, c.idler(),
                        c.idlerCap());
                d.rod(new Vector3f(end[0], t.yc(), t.zc() + sz * (w / 2 + 0.02F)),
                        new Vector3f(end[0], t.yc(), t.zc() + sz * (w / 2 + 0.06F)), end[1] * 0.2F, 10, c.axle());
            }
        }
    }

    /* The hull from {@code bottom} to {@code top}: its four panelled sides, its deck, its dark belly. */
    private static void hull(final SkinDrawing d, final float bottom, final float top, final CrawlerSkins.Colours c) {
        d.box(new float[] {X0, bottom, -CZ}, new float[] {X1, top, CZ}, face -> switch (face) {
            case PLUS_Z -> SkinDrawing.Paint.skin("hull_pz");
            case MINUS_Z -> SkinDrawing.Paint.skin("hull_nz");
            case PLUS_X -> SkinDrawing.Paint.skin("hull_px");
            case MINUS_X -> SkinDrawing.Paint.skin("hull_nx");
            case PLUS_Y -> SkinDrawing.Paint.skin("deck");
            case MINUS_Y -> SkinDrawing.Paint.tile(c.belly());
        });
    }

    /* What stands on and beside the deck at {@code top}: the jack pins, the railing, the stacks and the ladder. */
    private static void deckThings(final SkinDrawing d, final float top, final CrawlerSkins.Colours c) {
        for (final float px : new float[] {X0 + PIN_IN, X1 - PIN_IN}) {
            for (final float pz : new float[] {-PIN_IN, PIN_IN}) {
                d.capped(new Vector3f(px, top, pz), new Vector3f(px, top + PIN_HIGH, pz), PIN_RADIUS, 16, c.pin(),
                        c.yellow());
            }
        }
        for (final float zz : new float[] {-CZ + 0.15F, CZ - 0.15F}) {
            d.rod(new Vector3f(X0 + 0.3F, top + RAIL_HIGH, zz), new Vector3f(X1 - 0.3F, top + RAIL_HIGH, zz), 0.035F,
                    6, c.yellow());
            d.rod(new Vector3f(X0 + 0.3F, top + RAIL_LOW, zz), new Vector3f(X1 - 0.3F, top + RAIL_LOW, zz), 0.03F,
                    6, c.yellow());
            for (float xx = X0 + 0.3F; xx < X1 - 0.2F; xx += POSTS) {
                d.rod(new Vector3f(xx, top, zz), new Vector3f(xx, top + RAIL_HIGH, zz), 0.03F, 6, c.yellow());
            }
        }
        for (final int sz : new int[] {-1, 1}) {
            for (final float xx : STACKS) {
                final float bz = sz * (CZ + STACK_OUT);
                d.capped(new Vector3f(xx, top - 0.4F, bz), new Vector3f(xx, top - 0.4F + STACK_HIGH, bz), 0.13F, 12,
                        c.stack(), c.stackMouth());
                d.box(new float[] {xx - 0.18F, top + 0.9F, bz - 0.18F}, new float[] {xx + 0.18F, top + 0.95F,
                        bz + 0.18F}, face -> SkinDrawing.Paint.tile(c.stackCap()));
            }
        }
        final float lz = -CZ - LADDER_OUT;
        for (final float xx : new float[] {LADDER_FROM, LADDER_TO}) {
            d.rod(new Vector3f(xx, 0.2F, lz), new Vector3f(xx, top + RAIL_HIGH, lz), 0.035F, 6, c.yellow());
        }
        for (float yy = 0.4F; yy < top; yy += RUNGS) {
            d.rod(new Vector3f(LADDER_FROM, yy, lz), new Vector3f(LADDER_TO, yy, lz), 0.025F, 6, c.yellow());
        }
    }

    /* A cab with its middle at {@code cx} along and {@code cz} across: its box, its roof's rim and its beacon. */
    private static void cab(final SkinDrawing d, final float cx, final float cz, final float bottom, final float top,
                            final CrawlerSkins.Colours c) {
        d.box(new float[] {cx - CAB_HALF_X, bottom - CAB_BELOW, cz - CAB_HALF_Z},
                new float[] {cx + CAB_HALF_X, top + CAB_ABOVE, cz + CAB_HALF_Z}, face -> switch (face) {
                    case PLUS_Y -> SkinDrawing.Paint.skin("roof");
                    case MINUS_Y -> SkinDrawing.Paint.skin("cab_y");
                    case PLUS_X, MINUS_X -> SkinDrawing.Paint.skin("cab_x");
                    case PLUS_Z, MINUS_Z -> SkinDrawing.Paint.skin("cab_z");
                });
        d.rod(new Vector3f(cx, top + CAB_ABOVE, cz), new Vector3f(cx, top + CAB_ABOVE + 0.2F, cz), 0.09F, 10,
                c.beacon());
        // The roof's rim, a little wider than the cab, in the jacks' dark steel.
        d.box(new float[] {cx - CAB_HALF_X - 0.1F, top + 0.22F, cz - CAB_HALF_Z - 0.1F},
                new float[] {cx + CAB_HALF_X + 0.1F, top + 0.3F, cz + CAB_HALF_Z + 0.1F},
                face -> SkinDrawing.Paint.tile(c.jackFoot()));
    }

    /* {@code colour} darkened or brightened by {@code shade}, as a shoe of the approved tracks is. */
    private static int shaded(final int colour, final float shade) {
        final int r = Math.min(255, Math.round((colour >> 16 & 0xFF) * shade));
        final int g = Math.min(255, Math.round((colour >> 8 & 0xFF) * shade));
        final int b = Math.min(255, Math.round((colour & 0xFF) * shade));
        return colour >>> 24 << 24 | r << 16 | g << 8 | b;
    }

    /*
     * A track from {@code x0} to {@code x1} along, its middle {@code zc} across: the ends of its straight runs, the
     * loop's height and radius, its frame's measures.
     */
    private record Track(float x0, float x1, float zc) {

        float yc() {
            return TRACK_H / 2;
        }

        float radius() {
            return TRACK_H / 2 - SHOE / 2;
        }

        float xa() {
            return this.x0 + TRACK_H / 2;
        }

        float xb() {
            return this.x1 - TRACK_H / 2;
        }

        float inner() {
            return radius() - SHOE / 2 - 0.02F;
        }

        float frameLong() {
            return xb() - xa() + 0.3F;
        }

        float frameHigh() {
            return 2 * inner() - 0.3F;
        }

        float frameWide() {
            return TRACK_W - 0.12F;
        }
    }
}
