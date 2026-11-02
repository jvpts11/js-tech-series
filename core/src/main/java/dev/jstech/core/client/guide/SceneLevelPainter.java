/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.client.model.kit.SceneRenderer;
import dev.jstech.core.log.CoreLogs;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.slf4j.Logger;

/**
 * Paints the blocks of a manual's scene as the game draws blocks in its world: each block's model shaded as its
 * neighbours shade it, and each block entity through its renderer, the Core's models among them, all in full light.
 * The scene is drawn into its surface by the {@link SceneRenderer}.
 *
 * <p>One that keeps its meshes builds the blocks' models once into buffers on the graphics card, for the layers shown,
 * and after that only draws them, so the scene of a whole building turns under the reader's drag as smoothly as the
 * scene of a furnace: it builds them again only when another layer is shown, and lets them go when it is closed.
 * Translucent blocks are drawn afresh each time, so they stay sorted back to front however the scene is turned.
 */
final class SceneLevelPainter implements SceneRenderer.IScenePainter, AutoCloseable {

    private final SceneLevel level;
    private final boolean keeps;
    /*
     * The meshes kept on the graphics card, by the kind of layer they are drawn in; the layers they were built for; and
     * the blocks with translucent layers, left out of them to be drawn each time.
     */
    private final Map<RenderType, VertexBuffer> meshes = new LinkedHashMap<>();
    private int builtFor = NOT_BUILT;
    private List<Map.Entry<BlockPos, BlockState>> unsorted = List.of();

    private static final Logger LOGGER = CoreLogs.GUIDE.logger();
    /** What every block's random choice of model is made with, so a scene looks the same each time it is drawn. */
    private static final long SEED = 42L;
    /* The layers shown when no meshes are built. */
    private static final int NOT_BUILT = -1;

    /** A painter that draws every block afresh each time it paints, keeping nothing. */
    SceneLevelPainter(final SceneLevel level) {
        this(level, false);
    }

    private SceneLevelPainter(final SceneLevel level, final boolean keeps) {
        this.level = level;
        this.keeps = keeps;
    }

    /** A painter that keeps its meshes on the graphics card between paints, which whoever makes it closes. */
    static SceneLevelPainter keeping(final SceneLevel level) {
        return new SceneLevelPainter(level, true);
    }

    @Override
    public void paint(final PoseStack pose, final MultiBufferSource.BufferSource buffers) {
        if (this.keeps) {
            if (this.builtFor != this.level.shown()) {
                this.build();
            }
            this.drawMeshes(pose);
            this.blocks(this.unsorted, pose, buffers::getBuffer, SceneLevelPainter::sorted);
        } else {
            this.blocks(this.level.shownStates().entrySet(), pose, buffers::getBuffer, type -> true);
        }
        this.entities(pose, buffers);
    }

    /** Lets its meshes on the graphics card go; painted again, it builds them anew. */
    @Override
    public void close() {
        this.meshes.values().forEach(VertexBuffer::close);
        this.meshes.clear();
        this.unsorted = List.of();
        this.builtFor = NOT_BUILT;
    }

    /*
     * The models of the blocks {@code placed}, in each of the kinds of layer {@code which} takes, shaded by the blocks
     * round them: into the buffer {@code into} gives for each kind. The blocks with a layer it left out.
     */
    private List<Map.Entry<BlockPos, BlockState>> blocks(final Collection<Map.Entry<BlockPos, BlockState>> placed,
                                                         final PoseStack pose,
                                                         final Function<RenderType, VertexConsumer> into,
                                                         final Predicate<RenderType> which) {
        final List<Map.Entry<BlockPos, BlockState>> left = new ArrayList<>();
        final BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        final RandomSource random = RandomSource.create();
        for (final Map.Entry<BlockPos, BlockState> block : placed) {
            final BlockPos pos = block.getKey();
            final BlockState state = block.getValue();
            if (state.getRenderShape() != RenderShape.MODEL) {
                continue;
            }
            final BakedModel model = dispatcher.getBlockModel(state);
            final BlockEntity entity = this.level.getBlockEntity(pos);
            final ModelData data = model.getModelData(this.level, pos, state,
                    entity == null ? ModelData.EMPTY : entity.getModelData());
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            random.setSeed(SEED);
            boolean skipped = false;
            for (final RenderType type : model.getRenderTypes(state, random, data)) {
                if (!which.test(type)) {
                    skipped = true;
                    continue;
                }
                random.setSeed(SEED);
                dispatcher.renderBatched(state, pos, this.level, pose, into.apply(type), true, random, data, type);
            }
            pose.popPose();
            if (skipped) {
                left.add(block);
            }
        }
        return left;
    }

    /* Builds the meshes of every block's model in the layers shown, the translucent ones aside, about the origin. */
    private void build() {
        this.close();
        final Map<RenderType, ByteBufferBuilder> memory = new LinkedHashMap<>();
        final Map<RenderType, BufferBuilder> builders = new LinkedHashMap<>();
        try {
            this.unsorted = List.copyOf(this.blocks(this.level.shownStates().entrySet(), new PoseStack(),
                    type -> builders.computeIfAbsent(type, kind -> {
                        final ByteBufferBuilder bytes = new ByteBufferBuilder(kind.bufferSize());
                        memory.put(kind, bytes);
                        return new BufferBuilder(bytes, kind.mode(), kind.format());
                    }), type -> !sorted(type)));
            builders.forEach((type, builder) -> {
                final MeshData mesh = builder.build();
                if (mesh != null) {
                    final VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
                    buffer.bind();
                    buffer.upload(mesh);
                    VertexBuffer.unbind();
                    this.meshes.put(type, buffer);
                }
            });
        } finally {
            memory.values().forEach(ByteBufferBuilder::close);
        }
        this.builtFor = this.level.shown();
    }

    /* The kept meshes, each in its kind of layer, seen through {@code pose} as the surface sees the scene. */
    private void drawMeshes(final PoseStack pose) {
        final Matrix4f seen = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose());
        this.meshes.forEach((type, mesh) -> {
            type.setupRenderState();
            mesh.bind();
            mesh.drawWithShader(seen, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
            type.clearRenderState();
        });
    }

    /** Every block entity through its renderer, in full light; one that fails is left out with a line in the log. */
    private void entities(final PoseStack pose, final MultiBufferSource buffers) {
        for (final Map.Entry<BlockPos, BlockEntity> shown : this.level.shownEntities().entrySet()) {
            final BlockPos pos = shown.getKey();
            final BlockEntity entity = shown.getValue();
            final BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher()
                    .getRenderer(entity);
            if (renderer == null) {
                continue;
            }
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            try {
                renderer.render(entity, 0.0F, pose, buffers, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (final RuntimeException e) {
                LOGGER.warn("A manual's scene could not draw the block entity of {}", entity.getBlockState(), e);
            }
            pose.popPose();
        }
    }

    /* Whether a kind of layer is sorted back to front as it is drawn, as translucent blocks are. */
    private static boolean sorted(final RenderType type) {
        return type.sortOnUpload;
    }
}
