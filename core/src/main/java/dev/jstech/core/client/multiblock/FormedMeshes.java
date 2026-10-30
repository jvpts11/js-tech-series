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
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.damage.FormedDamage;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.IFormedMeshes;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.core.multiblock.shape.MeshOutline;
import dev.jstech.core.multiblock.shape.ModelTriangles;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The faces of formed models' blocks on a player's game, from the models it draws: each model's faces at rest shared
 * out among its blocks once, as the data run shared them for the shape files, and the outline of each block drawn
 * from its own faces, in place of the outline of its shape's boxes, the sharp edges and the cuts at its walls. Worked
 * out again when the models or the shape files are read again.
 */
public final class FormedMeshes implements IFormedMeshes {

    private final Map<ResourceLocation, Shared> shared = new ConcurrentHashMap<>();

    /* The outline's colour, as the game's own: black, less than half seen. */
    private static final float OUTLINE_ALPHA = 0.4F;
    private static final int AXES = 3;
    private static final FormedMeshes INSTANCE = new FormedMeshes();

    private FormedMeshes() {
    }

    /** Hands the Core and the game's bus what this answers, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        FormedShapes.meshes(INSTANCE);
        NeoForge.EVENT_BUS.addListener(RenderHighlightEvent.Block.class, FormedMeshes::onHighlight);
    }

    /**
     * Gives {@code sink} the lines of the outline of the merged block at {@code pos}, in its own space, from its faces;
     * false, giving nothing, for any other block, or one whose model is not known.
     */
    public static boolean outline(final Level level, final BlockPos pos, final BlockState state,
                                  final Shapes.DoubleLineConsumer sink) {
        if (!(state.getBlock() instanceof FormedPartBlock)
                || !(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part) || part.places().isEmpty()) {
            return false;
        }
        boolean drawn = false;
        final double[] local = new double[AXES];
        final double[] from = new double[AXES];
        final double[] to = new double[AXES];
        // Its own model's outline, and those of the models reaching into it, each turned as its model stands.
        for (final FormedPlace place : part.places()) {
            // A block a strike reached is outlined by its faces as the strike left them.
            final Optional<float[]> struck = FormedDamage.lines(level, pos, place);
            final Optional<float[]> lines = struck.isPresent() ? struck : FormedShapes.lines(level, place);
            if (lines.isEmpty()) {
                continue;
            }
            final float[] all = lines.get();
            for (int at = 0; at + MeshOutline.LINE <= all.length; at += MeshOutline.LINE) {
                local[0] = all[at];
                local[1] = all[at + 1];
                local[2] = all[at + 2];
                CellSpace.toWorld(local, place.turn(), from);
                local[0] = all[at + 3];
                local[1] = all[at + 4];
                local[2] = all[at + 5];
                CellSpace.toWorld(local, place.turn(), to);
                sink.consume(from[0], from[1], from[2], to[0], to[1], to[2]);
            }
            drawn = true;
        }
        return drawn;
    }

    @Override
    public @Nullable float[] triangles(final FormedPlace place, final FormedShape shape) {
        final Shared model = shared(place.model(), shape);
        return model == null ? null : model.faces().get(place.cell());
    }

    @Override
    public @Nullable float[] lines(final FormedPlace place, final FormedShape shape) {
        final Shared model = shared(place.model(), shape);
        if (model == null) {
            return null;
        }
        final float[] faces = model.faces().get(place.cell());
        return faces == null ? null : model.lines().computeIfAbsent(place.cell(), cell -> MeshOutline.lines(faces));
    }

    /** Draws one line of an outline, black and less than half seen, as the game outlines a block. */
    public static void line(final VertexConsumer lines, final PoseStack.Pose at, final float x1, final float y1,
                            final float z1, final float x2, final float y2, final float z2) {
        final float dx = x2 - x1;
        final float dy = y2 - y1;
        final float dz = z2 - z1;
        final float length = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) {
            return;
        }
        lines.addVertex(at, x1, y1, z1).setColor(0F, 0F, 0F, OUTLINE_ALPHA).setNormal(at, dx / length, dy / length,
                dz / length);
        lines.addVertex(at, x2, y2, z2).setColor(0F, 0F, 0F, OUTLINE_ALPHA).setNormal(at, dx / length, dy / length,
                dz / length);
    }

    /* The model's faces shared out among its blocks, worked out again for a model or a shape file read anew. */
    private @Nullable Shared shared(final ResourceLocation id, final FormedShape shape) {
        final Optional<KitModel> model = KitModels.get(id);
        if (model.isEmpty()) {
            return null;
        }
        final Shared known = this.shared.get(id);
        if (known != null && known.model() == model.get() && known.shape() == shape) {
            return known;
        }
        final KitModel drawn = model.get();
        final float[] faces = ModelTriangles.of(drawn.data(), drawn.geometry(), drawn.scale());
        final MeshCells.Split split = MeshCells.split(faces, shape.origin(), shape.cells().keySet(), shape.apart());
        final Shared made = new Shared(drawn, shape, split.pieces(), new ConcurrentHashMap<>());
        this.shared.put(id, made);
        return made;
    }

    /* The outline of a merged block on the ground, from its faces; one aboard a structure is outlined there. */
    private static void onHighlight(final RenderHighlightEvent.Block event) {
        final BlockPos pos = event.getTarget().getBlockPos();
        final Level level = Minecraft.getInstance().level;
        if (level == null || YardLayout.isYardBlock(pos.getX())) {
            return;
        }
        final BlockState state = level.getBlockState(pos);
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack.Pose at = event.getPoseStack().last();
        final VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        final float x = (float) (pos.getX() - camera.x);
        final float y = (float) (pos.getY() - camera.y);
        final float z = (float) (pos.getZ() - camera.z);
        final boolean drawn = outline(level, pos, state, (x1, y1, z1, x2, y2, z2) -> line(lines, at,
                x + (float) x1, y + (float) y1, z + (float) z1, x + (float) x2, y + (float) y2, z + (float) z2));
        if (drawn) {
            event.setCanceled(true);
        }
    }

    /* One model's faces shared out among its blocks, and the outlines worked out from them so far. */
    private record Shared(KitModel model, FormedShape shape, Map<Long, float[]> faces, Map<Long, float[]> lines) {
    }
}
