/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.MicroblockLayout;
import dev.jstech.core.multipart.PartBox;
import dev.jstech.core.multipart.SpaceDrawing;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * A microblock's quads, cut from the block it was cut from: every face of each of its boxes, wearing the texture its
 * block shows on that side, the part of it that face covers, as a block model with its texture locked shows it, and
 * tinted as the block is (grass by its biome). A face on the space's side is kept under that side, so the game hides
 * it against a neighbour that covers it; the rest are kept under no side, but those the space's drawing hides. Worked
 * out once for each way a microblock is drawn and kept until the models are baked again.
 */
public final class MicroblockQuads {

    private static final Map<Key, List<BakedQuad>> QUADS = new ConcurrentHashMap<>();
    private static final Map<Block, Look> LOOKS = new ConcurrentHashMap<>();
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final float SIDE = PartBox.SIDE;
    private static final int NO_TINT = -1;

    private MicroblockQuads() {
    }

    /** Forgets what it worked out, as the models are baked again. */
    public static void forget() {
        QUADS.clear();
        LOOKS.clear();
    }

    /**
     * The quads of {@code drawn}'s boxes kept under {@code side}, or under no side for null, moved by {@code offset}
     * pixels; the faces its drawing hides are left out.
     */
    public static List<BakedQuad> quads(final MicroblockLayout.Drawn drawn, @Nullable final Direction side,
                                        final float[] offset) {
        return QUADS.computeIfAbsent(new Key(drawn.block().piece().material(), drawn.boxes(), side, offset[0],
                offset[1], offset[2]), MicroblockQuads::bake);
    }

    /** The layers the block {@code block} was cut from is drawn in. */
    public static ChunkRenderTypeSet renderTypes(final Microblock block) {
        return renderTypes(block.piece().material());
    }

    /** The layers {@code material} is drawn in. */
    public static ChunkRenderTypeSet renderTypes(final Block material) {
        return look(material).types();
    }

    /** The sprite the block {@code material} breaks into, for the space's particles. */
    public static TextureAtlasSprite particle(final Block material) {
        return look(material).particle();
    }

    private static List<BakedQuad> bake(final Key key) {
        final Look look = look(key.material());
        final List<BakedQuad> out = new ArrayList<>();
        for (final SpaceDrawing.DrawnBox drawn : key.boxes()) {
            final PartBox box = drawn.box();
            final Vector3f from = new Vector3f(box.x0() + key.dx(), box.y0() + key.dy(), box.z0() + key.dz());
            final Vector3f to = new Vector3f(box.x1() + key.dx(), box.y1() + key.dy(), box.z1() + key.dz());
            for (final Direction face : Direction.values()) {
                if (drawn.hides(face.get3DDataValue()) || sideOf(face, from, to) != key.side()) {
                    continue;
                }
                final Face skin = look.faces().get(face);
                final BlockElementFace element = new BlockElementFace(null, skin.tint(), "", new BlockFaceUV(
                        lockedUv(face, from, to), 0));
                out.add(BAKERY.bakeQuad(from, to, element, skin.sprite(), face, BlockModelRotation.X0_Y0, null,
                        true));
            }
        }
        return List.copyOf(out);
    }

    /* The side a face lies on when it lies on the space's side, else none. */
    private static @Nullable Direction sideOf(final Direction face, final Vector3f from, final Vector3f to) {
        final float at = switch (face) {
            case DOWN -> from.y();
            case UP -> to.y();
            case NORTH -> from.z();
            case SOUTH -> to.z();
            case WEST -> from.x();
            case EAST -> to.x();
        };
        final boolean onSide = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? at >= SIDE : at <= 0;
        return onSide ? face : null;
    }

    /* The game's own uv for a face of a box with none given: the face's rectangle in the block, so it locks. */
    private static float[] lockedUv(final Direction face, final Vector3f from, final Vector3f to) {
        return switch (face) {
            case DOWN -> new float[] {from.x(), SIDE - to.z(), to.x(), SIDE - from.z()};
            case UP -> new float[] {from.x(), from.z(), to.x(), to.z()};
            case NORTH -> new float[] {SIDE - to.x(), SIDE - to.y(), SIDE - from.x(), SIDE - from.y()};
            case SOUTH -> new float[] {from.x(), SIDE - to.y(), to.x(), SIDE - from.y()};
            case WEST -> new float[] {from.z(), SIDE - to.y(), to.z(), SIDE - from.y()};
            case EAST -> new float[] {SIDE - to.z(), SIDE - to.y(), SIDE - from.z(), SIDE - from.y()};
        };
    }

    /* What the block looks like on each side, read from its own model: the sprite and tint of its first quad there. */
    private static Look look(final Block material) {
        return LOOKS.computeIfAbsent(material, block -> {
            final BlockState state = block.defaultBlockState();
            final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            final RandomSource random = RandomSource.create(0L);
            final Map<Direction, Face> faces = new EnumMap<>(Direction.class);
            for (final Direction face : Direction.values()) {
                final List<BakedQuad> quads = model.getQuads(state, face, random, ModelData.EMPTY, null);
                faces.put(face, quads.isEmpty() ? new Face(model.getParticleIcon(ModelData.EMPTY), NO_TINT)
                        : new Face(quads.getFirst().getSprite(), quads.getFirst().getTintIndex()));
            }
            return new Look(faces, model.getRenderTypes(state, random, ModelData.EMPTY),
                    model.getParticleIcon(ModelData.EMPTY));
        });
    }

    /** The quads of boxes of one block under one side, moved so far. */
    private record Key(Block material, List<SpaceDrawing.DrawnBox> boxes, @Nullable Direction side, float dx,
                       float dy, float dz) {
    }

    /** What a block shows on one side: its sprite, and whether and how it is tinted. */
    private record Face(TextureAtlasSprite sprite, int tint) {
    }

    /** What a block looks like: each side, the layers it is drawn in, and what it breaks into. */
    private record Look(Map<Direction, Face> faces, ChunkRenderTypeSet types, TextureAtlasSprite particle) {
    }
}
