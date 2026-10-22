/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.moving.MovingStructureEntity;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.YardLayout;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

/**
 * Draws what of a moving structure is drawn with the world's entities: its block entities' own drawings, a chest's
 * lid and a sign's words, turned and placed as the structure is; the cracks of a block of it being broken; and, under
 * a shader pack, its blocks' meshes too, handed to the pack's drawing as an entity's are. Each block entity takes the
 * light of the world where it is and its own, as the blocks do.
 */
public final class MovingStructureRenderer extends EntityRenderer<MovingStructureEntity> {

    public MovingStructureRenderer(final EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public ResourceLocation getTextureLocation(final MovingStructureEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override
    public boolean shouldRender(final MovingStructureEntity entity, final Frustum frustum, final double x,
                                final double y, final double z) {
        final ClientStructure structure = ClientMovingStructures.of(entity);
        return structure != null && structure.ready() && !StructureHandOffs.hidden(structure)
                && frustum.isVisible(structure.drawnBox())
                && StructureLayers.withinReach(structure.drawnBox(), new Vec3(x, y, z));
    }

    @Override
    public void render(final MovingStructureEntity entity, final float yaw, final float partialTick,
                       final PoseStack pose, final MultiBufferSource buffers, final int light) {
        final ClientStructure structure = ClientMovingStructures.of(entity);
        if (structure == null || !structure.ready() || StructureHandOffs.hidden(structure)) {
            return;
        }
        final StructurePose drawn = structure.drawnAt(partialTick);
        pose.pushPose();
        // The game stands the pose where it draws the entity, its own smoothing of where it was told the entity is;
        // the blocks are drawn where the structure's pose puts them, so what is drawn here stands there too.
        pose.translate(drawn.x() - Mth.lerp(partialTick, entity.xOld, entity.getX()),
                drawn.y() - Mth.lerp(partialTick, entity.yOld, entity.getY()),
                drawn.z() - Mth.lerp(partialTick, entity.zOld, entity.getZ()));
        drawWithItsEntity(structure, drawn, partialTick, pose, buffers);
        pose.popPose();
    }

    /**
     * Draws what goes with a structure's entity, {@code pose} standing where its pivot is drawn at {@code drawn}: under
     * a shader pack its blocks, and its block entities and the cracks of its blocks being broken.
     */
    static void drawWithItsEntity(final ClientStructure structure, final StructurePose drawn, final float partialTick,
                                  final PoseStack pose, final MultiBufferSource buffers) {
        pose.pushPose();
        pose.mulPose(new Quaternionf((float) drawn.qx(), (float) drawn.qy(), (float) drawn.qz(), (float) drawn.qw()));
        if (StructureLayers.shaderPack()) {
            structure.meshes().emit(pose, buffers);
        }
        final StructureFrame frame = new StructureFrame(drawn, structure.pivotX(), structure.pivotY(),
                structure.pivotZ());
        final Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        final Vec3 seen = new Vec3(frame.yardX(eye.x, eye.y, eye.z), frame.yardY(eye.x, eye.y, eye.z),
                frame.yardZ(eye.x, eye.y, eye.z));
        final BlockEntityRenderDispatcher dispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        final Level level = structure.level();
        for (final BlockEntity blockEntity : structure.meshes().blockEntities()) {
            drawBlockEntity(dispatcher, blockEntity, structure, frame, seen, level, partialTick, pose, buffers);
        }
        drawCracks(structure, level, pose, buffers);
        pose.popPose();
    }

    private static <E extends BlockEntity> void drawBlockEntity(final BlockEntityRenderDispatcher dispatcher,
                                                                final E blockEntity, final ClientStructure structure,
                                                                final StructureFrame frame, final Vec3 seen,
                                                                final Level level, final float partialTick,
                                                                final PoseStack pose,
                                                                final MultiBufferSource buffers) {
        // A structure set down is drawn where it lands though its plot is cleared, its block entities with it.
        if (blockEntity.isRemoved() && !structure.landed()) {
            return;
        }
        final BlockEntityRenderer<E> renderer = dispatcher.getRenderer(blockEntity);
        if (renderer == null || !renderer.shouldRender(blockEntity, seen)) {
            return;
        }
        final BlockPos at = blockEntity.getBlockPos();
        pose.pushPose();
        pose.translate((float) (at.getX() - structure.pivotX()), (float) (at.getY() - structure.pivotY()),
                (float) (at.getZ() - structure.pivotZ()));
        final double cx = at.getX() + 0.5;
        final double cy = at.getY() + 0.5;
        final double cz = at.getZ() + 0.5;
        final BlockPos there = BlockPos.containing(frame.worldX(cx, cy, cz), frame.worldY(cx, cy, cz),
                frame.worldZ(cx, cy, cz));
        // Its own light where it is in the yard, no brighter in sky than the world there; landing, the world's light,
        // the yard about to be cleared.
        final int own = structure.landed() ? LevelRenderer.getLightColor(level, there)
                : LevelRenderer.getLightColor(level, at);
        final int lit = LightTexture.pack(Math.max(LightTexture.block(own), level.getBrightness(LightLayer.BLOCK,
                there)), Math.min(LightTexture.sky(own), level.getBrightness(LightLayer.SKY, there)));
        renderer.render(blockEntity, partialTick, pose, buffers, lit, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /* The cracks of the blocks of the structure being broken, each on its block. */
    private static void drawCracks(final ClientStructure structure, final Level level, final PoseStack pose,
                                   final MultiBufferSource buffers) {
        final Long2ObjectMap<SortedSet<BlockDestructionProgress>> breaking =
                Minecraft.getInstance().levelRenderer.destructionProgress;
        if (breaking.isEmpty()) {
            return;
        }
        for (final Long2ObjectMap.Entry<SortedSet<BlockDestructionProgress>> each : breaking.long2ObjectEntrySet()) {
            final BlockPos at = BlockPos.of(each.getLongKey());
            if (!YardLayout.isYardBlock(at.getX()) || YardLayout.plotOfBlock(at.getX(), at.getZ())
                    != structure.plot() || each.getValue().isEmpty()) {
                continue;
            }
            final int stage = each.getValue().last().getProgress();
            if (stage < 0 || stage >= ModelBakery.DESTROY_TYPES.size()) {
                continue;
            }
            final BlockState state = level.getBlockState(at);
            pose.pushPose();
            pose.translate((float) (at.getX() - structure.pivotX()), (float) (at.getY() - structure.pivotY()),
                    (float) (at.getZ() - structure.pivotZ()));
            final VertexConsumer cracks = new SheetedDecalTextureGenerator(
                    buffers.getBuffer(ModelBakery.DESTROY_TYPES.get(stage)), pose.last(), 1.0F);
            Minecraft.getInstance().getBlockRenderer().renderBreakingTexture(state, at, level, pose, cracks,
                    ModelData.EMPTY);
            pose.popPose();
        }
    }
}
