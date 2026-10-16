/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import dev.jstech.core.multipart.CoreSpaces;
import dev.jstech.core.multipart.MicroShape;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.MicroblockLayout;
import dev.jstech.core.multipart.PartBox;
import dev.jstech.core.multipart.SpaceSlots;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/**
 * The microblock item's model: for each stack, the piece it carries, cut from its block and set in the middle of the
 * slot, held and shown in a frame the way the item's own model says a block is. A stack carrying none shows the
 * item's own model.
 */
public final class MicroblockItemModel extends BakedModelWrapper<BakedModel> {

    private final ItemOverrides overrides = new ItemOverrides() {
        @Override
        public BakedModel resolve(final BakedModel model, final ItemStack stack, @Nullable final ClientLevel level,
                                  @Nullable final LivingEntity entity, final int seed) {
            final Microblock.MicroblockPiece piece = stack.get(CoreSpaces.PIECE.get());
            return piece == null ? model : PIECES.computeIfAbsent(piece, made -> new Piece(originalModel, made));
        }
    };

    private static final Map<Microblock.MicroblockPiece, BakedModel> PIECES = new ConcurrentHashMap<>();
    private static final float SIDE = PartBox.SIDE;
    private static final int NORTH = 2;

    public MicroblockItemModel(final BakedModel base) {
        super(base);
    }

    /** Forgets the pieces it put together, as the models are baked again. */
    public static void forget() {
        PIECES.clear();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overrides;
    }

    /* The piece standing in the middle of the slot: a piece on a face upright, a post up, a corner centred. */
    private static List<BakedQuad> quadsOf(final Microblock.MicroblockPiece piece) {
        final int thickness = piece.size() * 2;
        final float half = (SIDE - thickness) / 2F;
        final Microblock block;
        final float[] offset;
        switch (piece.shape()) {
            case FACE, HOLLOW, FACADE -> {
                block = new Microblock(piece, SpaceSlots.face(NORTH), SpaceSlots.Y);
                offset = new float[] {0F, 0F, half};
            }
            case CORNER -> {
                block = new Microblock(piece, SpaceSlots.corner(false, false, false), SpaceSlots.Y);
                offset = new float[] {half, half, half};
            }
            default -> {
                block = new Microblock(new Microblock.MicroblockPiece(MicroShape.POST, piece.size(),
                        piece.material()), SpaceSlots.CENTRE, SpaceSlots.Y);
                offset = new float[] {0F, 0F, 0F};
            }
        }
        final MicroblockLayout.Drawn drawn = MicroblockLayout.of(List.of(block)).drawn().getFirst();
        final List<BakedQuad> out = new ArrayList<>(MicroblockQuads.quads(drawn, null, offset));
        for (final Direction side : Direction.values()) {
            out.addAll(MicroblockQuads.quads(drawn, side, offset));
        }
        return List.copyOf(out);
    }

    /** One piece's model: its quads under no side, and the item's own way of being held. */
    private static final class Piece extends BakedModelWrapper<BakedModel> {

        private final Microblock.MicroblockPiece piece;
        private @Nullable List<BakedQuad> quads;

        private Piece(final BakedModel base, final Microblock.MicroblockPiece piece) {
            super(base);
            this.piece = piece;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                                        final RandomSource random) {
            if (side != null) {
                return List.of();
            }
            if (this.quads == null) {
                this.quads = quadsOf(this.piece);
            }
            return this.quads;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable final BlockState state, @Nullable final Direction side,
                                        final RandomSource random, final ModelData data,
                                        @Nullable final RenderType renderType) {
            return getQuads(state, side, random);
        }

        /* Glass in a glass layer: the piece is drawn in the layers its block is. */
        @Override
        public List<RenderType> getRenderTypes(final ItemStack stack, final boolean fabulous) {
            final List<RenderType> out = new ArrayList<>();
            for (final RenderType chunk : MicroblockQuads.renderTypes(this.piece.material())) {
                out.add(RenderTypeHelper.getEntityRenderType(chunk, fabulous));
            }
            return out.isEmpty() ? super.getRenderTypes(stack, fabulous) : out;
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
