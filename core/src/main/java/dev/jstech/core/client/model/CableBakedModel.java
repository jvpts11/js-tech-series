/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.BundleShape;
import dev.jstech.core.cable.CableDrawing;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.connect.ConnectedFaces;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

/**
 * The Core cable block's model: the parts and plugs its multipart model places, and its wires, made from the boxes
 * {@link BundleShape} lays out. A wire's jacket is laid by where each face lies in the block, so its pattern runs on
 * from block to block; the junction box and the rings are flat colours from the Core's housing texture, a ring taking
 * its dye's colour from the block's tint.
 *
 * <p>The quads of each set of wires are made once, the first time it is drawn, and kept until the models are baked
 * again; they are drawn with the world's mesh, which is built again only when a block changes.
 */
public final class CableBakedModel extends BakedModelWrapper<BakedModel> {

    /** Each set of wires' quads, kept until the models are baked again. */
    private static final Map<CableDrawing, List<BakedQuad>> WIRES = new ConcurrentHashMap<>();
    private static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());
    /** The Core's housing texture: the housing, its seam, the screws and the white of a ring, a quarter each. */
    private static final ResourceLocation HOUSING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "block/cable/housing");
    private static final float PIXEL = 1F / 16F;
    private static final float JACKET_SIZE = 32F;
    private static final float HOUSING_SIZE = 16F;
    private static final float SWATCH = 8F;
    private static final float SWATCH_INSET = 2F;
    private static final float SWATCH_SPAN = 4F;
    private static final int NO_TINT = -1;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;

    /** @param base the block's multipart model, which draws its parts and plugs */
    public CableBakedModel(final BakedModel base) {
        super(base);
    }

    /** Forgets the quads it made, as the models are baked again. */
    public static void forget() {
        WIRES.clear();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                                    final RandomSource random, final ModelData data,
                                    @Nullable final RenderType renderType) {
        final List<BakedQuad> own = super.getQuads(state, side, random, data, renderType);
        final CableDrawing drawing = data.get(CableDrawing.PROPERTY);
        if (drawing == null || drawing.isEmpty() || side != null
                || renderType != null && renderType != RenderType.cutout()) {
            return own;
        }
        final List<BakedQuad> out = new ArrayList<>(own);
        out.addAll(WIRES.computeIfAbsent(drawing, CableBakedModel::make));
        return out;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(final BlockState state, final RandomSource random,
                                             final ModelData data) {
        return ChunkRenderTypeSet.union(super.getRenderTypes(state, random, data), CUTOUT);
    }

    /* Every box of the wires, the junction box and the rings, each face a quad. */
    private static List<BakedQuad> make(final CableDrawing drawing) {
        final TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        final TextureAtlasSprite housing = atlas.getSprite(HOUSING);
        final BundleShape shape = drawing.shape();
        final List<BakedQuad> out = new ArrayList<>();
        for (final BundleShape.Piece piece : shape.pieces()) {
            for (final Direction face : Direction.values()) {
                if (piece.hides(face.get3DDataValue())) {
                    continue;
                }
                out.add(switch (piece.kind()) {
                    case JACKET -> {
                        final Wire wire = drawing.wires().get(piece.strand());
                        yield jacket(piece, face, atlas.getSprite(wire.type().jacket()), wire.type().thickness());
                    }
                    case RING -> flat(piece.box(), face, housing, 1, 1,
                            drawing.wires().get(piece.strand()).colour().map(dye -> dye.getId()).orElse(NO_TINT));
                    case HOUSING -> flat(piece.box(), face, housing, 0, 0, NO_TINT);
                    case HOUSING_EDGE -> flat(piece.box(), face, housing, 1, 0, NO_TINT);
                    case SCREW -> flat(piece.box(), face, housing, 0, 1, NO_TINT);
                });
            }
        }
        return List.copyOf(out);
    }

    /*
     * A face of a jacket. The texture is laid by where the face lies in the block, as the jacket's texture is drawn:
     * a side with the length along u takes the top left strip, one with the length along v the strip beside it, both
     * at the length's own place in the block; an end takes the corner square.
     */
    private static BakedQuad jacket(final BundleShape.Piece piece, final Direction face,
                                    final TextureAtlasSprite sprite, final int thickness) {
        final int axis = piece.axis();
        final BundleShape.Box box = piece.box();
        final int normalAxis = index(face.getAxis());
        final int uAxis = uAxis(face);
        return quad(box, face, sprite, NO_TINT, (corner, position) -> {
            if (normalAxis == axis) {
                return new float[] {corner[0] * thickness, (1F - corner[1]) * thickness};
            }
            final int across = 3 - axis - normalAxis;
            final float spread = (float) ((position[across] - box.min(across)) / (box.max(across) - box.min(across)))
                    * thickness;
            return uAxis == axis
                    ? new float[] {(float) position[axis], spread}
                    : new float[] {HOUSING_SIZE + spread, (float) position[axis]};
        }, JACKET_SIZE);
    }

    /* A face of a flat colour: the middle of one of the housing texture's four swatches, by column and row. */
    private static BakedQuad flat(final BundleShape.Box box, final Direction face, final TextureAtlasSprite sprite,
                                  final int column, final int row, final int tint) {
        final float u0 = column * SWATCH + SWATCH_INSET;
        final float v0 = row * SWATCH + SWATCH_INSET;
        return quad(box, face, sprite, tint, (corner, position) -> new float[] {u0 + corner[0] * SWATCH_SPAN,
                v0 + (1F - corner[1]) * SWATCH_SPAN}, HOUSING_SIZE);
    }

    /*
     * The face of the box on {@code face}, its corners counter-clockwise seen from outside, each given its texture
     * place, in pixels of a texture {@code size} across, by {@code uv} from where the corner sits.
     */
    private static BakedQuad quad(final BundleShape.Box box, final Direction face, final TextureAtlasSprite sprite,
                                  final int tint, final ICornerUv uv, final float size) {
        final Vec3i normal = face.getNormal();
        final Direction up = ConnectedFaces.up(face);
        final Direction right = ConnectedFaces.right(face);
        final QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setDirection(face);
        baker.setTintIndex(tint);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(true);
        // Top left, bottom left, bottom right, top right: counter-clockwise seen from outside.
        final float[][] corners = {{0F, 1F}, {0F, 0F}, {1F, 0F}, {1F, 1F}};
        for (final float[] corner : corners) {
            final double[] position = new double[3];
            final int normalAxis = index(face.getAxis());
            position[normalAxis] = face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? box.max(normalAxis) : box.min(normalAxis);
            along(position, box, right, corner[0]);
            along(position, box, up, corner[1]);
            final float[] place = uv.at(corner, position);
            baker.addVertex((float) position[0] * PIXEL, (float) position[1] * PIXEL, (float) position[2] * PIXEL)
                    .setColor(255, 255, 255, 255)
                    .setUv(sprite.getU0() + (sprite.getU1() - sprite.getU0()) * place[0] / size,
                            sprite.getV0() + (sprite.getV1() - sprite.getV0()) * place[1] / size)
                    .setUv2(0, 0)
                    .setNormal(normal.getX(), normal.getY(), normal.getZ());
        }
        return baker.bakeQuad();
    }

    /* Sets the corner's place along {@code way}'s axis: from the box's edge it starts at, a fraction across. */
    private static void along(final double[] position, final BundleShape.Box box, final Direction way,
                              final float fraction) {
        final int axis = index(way.getAxis());
        final double low = box.min(axis);
        final double high = box.max(axis);
        position[axis] = way.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? low + (high - low) * fraction
                : high - (high - low) * fraction;
    }

    /* The axis a face's texture runs u along, as a jacket's texture is drawn: x on every face but east and west. */
    private static int uAxis(final Direction face) {
        return face.getAxis() == Direction.Axis.X ? Z : X;
    }

    /* An axis as the shape numbers it: x 0, y 1, z 2. */
    private static int index(final Direction.Axis axis) {
        return switch (axis) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }

    /** Where a corner of a face takes its texture from, in pixels of the texture. */
    @FunctionalInterface
    private interface ICornerUv {

        /**
         * @param corner   the corner, right then up, each 0 or 1
         * @param position where it sits in the block, in pixels
         */
        float[] at(float[] corner, double[] position);
    }
}
