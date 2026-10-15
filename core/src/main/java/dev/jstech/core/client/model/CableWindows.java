/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import dev.jstech.core.cable.BundleShape;
import dev.jstech.core.cable.CableDrawing;
import dev.jstech.core.cable.CableFlowView;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.cable.WireFlow;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a cable block's model draws in the see-through layer: the glass in its wires' windows, and behind it what each
 * wire carries, a liquid at its level and a gas filling it, in its own colour and moving as the game's flowing water
 * does. A wire's fitting, where it turns, is solid and shows neither. A line of items shows its items otherwise.
 */
final class CableWindows {

    /** The game's flowing water, which a gas is drawn in, in the gas's colour. */
    private static final ResourceLocation FLOWING = ResourceLocation.withDefaultNamespace("block/water_flow");
    private static final float FLOW_SIZE = 32F;
    private static final double INSET = 1.0;
    private static final int OPAQUE = 0xFF << 24;
    private static final int GAS = 0xA0 << 24;
    private static final int RGB = 0xFFFFFF;
    private static final int NO_TINT = -1;
    private static final int BUCKET = 1000;

    private CableWindows() {
    }

    /**
     * Whether {@code piece} is a wire's core where it turns or branches: a cube as wide as the wire, where a straight
     * wire is one box the length of the block and an arm runs from the core to a face.
     */
    static boolean isCore(final BundleShape.Piece piece, final int thickness) {
        final BundleShape.Box box = piece.box();
        return box.maxX() - box.minX() == thickness && box.maxY() - box.minY() == thickness
                && box.maxZ() - box.minZ() == thickness;
    }

    /** The glass and the flows of {@code key}'s wires. */
    static List<BakedQuad> make(final Key key) {
        final TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        final CableDrawing drawing = key.drawing();
        final List<BakedQuad> out = new ArrayList<>();
        for (final BundleShape.Piece piece : drawing.shape().pieces()) {
            if (piece.kind() != BundleShape.Kind.JACKET) {
                continue;
            }
            final Wire wire = drawing.wires().get(piece.strand());
            final CableType type = wire.type();
            final Optional<ResourceLocation> glass = type.window();
            if (glass.isEmpty() || isCore(piece, type.thickness()) && type.fitting().isPresent()) {
                continue;
            }
            final TextureAtlasSprite pane = atlas.getSprite(glass.get());
            for (final Direction face : Direction.values()) {
                if (!piece.hides(face.get3DDataValue())) {
                    out.add(CableBakedModel.jacket(piece, face, pane, type.thickness()));
                }
            }
            final WireFlow flow = key.view().of(piece.strand());
            final Look look = flow.isEmpty() ? null : lookOf(type, flow, atlas);
            if (look != null) {
                fluid(out, piece, isCore(piece, type.thickness()), flow, look);
            }
        }
        return List.copyOf(out);
    }

    /*
     * The fluid inside a piece: a box a pixel inside its walls (along the wire too, for a core), as high as the wire is
     * full when it lies flat.
     */
    private static void fluid(final List<BakedQuad> out, final BundleShape.Piece piece, final boolean core,
                              final WireFlow flow, final Look look) {
        final BundleShape.Box box = piece.box();
        final double[] min = {box.minX(), box.minY(), box.minZ()};
        final double[] max = {box.maxX(), box.maxY(), box.maxZ()};
        for (int axis = 0; axis < 3; axis++) {
            if (axis != piece.axis() || core) {
                min[axis] += INSET;
                max[axis] -= INSET;
            }
        }
        if (piece.axis() != 1) {
            max[1] = min[1] + (max[1] - min[1]) * flow.level() / WireFlow.FULL;
        }
        final BundleShape.Box inside = new BundleShape.Box(min[0], min[1], min[2], max[0], max[1], max[2]);
        for (final Direction face : Direction.values()) {
            final int axis = CableBakedModel.index(face.getAxis());
            if (axis == piece.axis() && piece.hides(face.get3DDataValue())) {
                continue;
            }
            final int uAxis = CableBakedModel.uAxis(face);
            final int vAxis = 3 - axis - uAxis;
            out.add(CableBakedModel.quad(inside, face, look.sprite(), NO_TINT, (corner, position) -> new float[] {
                    (float) position[uAxis], (float) position[vAxis]}, FLOW_SIZE, look.colour()));
        }
    }

    /* How what {@code flow} carries is drawn in a wire of {@code type}; none for items, which are drawn otherwise. */
    private static @Nullable Look lookOf(final CableType type, final WireFlow flow, final TextureAtlas atlas) {
        final ResourceLocation id = ResourceLocation.tryParse(flow.what());
        if (id == null || type.grid() == GridKind.ITEM) {
            return null;
        }
        final Fluid fluid = BuiltInRegistries.FLUID.get(id);
        if (fluid != Fluids.EMPTY) {
            final FluidStack stack = new FluidStack(fluid, BUCKET);
            final IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid);
            final int tint = client.getTintColor(stack);
            return new Look(atlas.getSprite(client.getFlowingTexture(stack)), type.grid() == GridKind.FLUID
                    ? tint : GAS | tint & RGB);
        }
        final Optional<Material> material = Materials.byId(id);
        if (material.isPresent()) {
            final boolean gas = material.get().roomState() == MatterState.GAS || type.grid() == GridKind.GAS;
            return new Look(atlas.getSprite(FLOWING), (gas ? GAS : OPAQUE) | material.get().colour() & RGB);
        }
        return null;
    }

    /**
     * The windows of one set of wires with what flows in them: two blocks that hold the same are drawn from the same
     * quads.
     *
     * @param drawing the wires
     * @param view    what each carries
     */
    record Key(CableDrawing drawing, CableFlowView view) {
    }

    /* A sprite and the colour it is drawn in. */
    private record Look(TextureAtlasSprite sprite, int colour) {
    }
}
