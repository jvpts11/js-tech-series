/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.jstech.core.client.multiblock.FadedConsumer;
import dev.jstech.core.tool.AreaMining;
import dev.jstech.core.tool.BuildMode;
import dev.jstech.core.tool.BuildingTools;
import dev.jstech.core.tool.CopiedRegion;
import dev.jstech.core.tool.MiningShape;
import dev.jstech.core.tool.Tools;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * What a tool shows in the world before it works. A tool that mines an area or a vein outlines every block it will
 * take with a box of its own, as the game outlines the block aimed at; a far harder block, one it is not made for and
 * air have none. A building tool shows the blocks it will place where they will go, translucent with a white edge, as
 * many as the inventory holds. Claims are the server's to weigh, so a block on claimed land shows and is left as the
 * tool works.
 */
final class ToolPreviews {

    /* How see-through a ghost is, from nothing to whole. */
    private static final float GHOST = 0.75F;
    /* A ghost's edge sits a hair outside its block, so it shows over the faces of the block beside it. */
    private static final double EDGE_OUT = 0.002;
    private static final int TICKS_KEPT = 4;

    /* The blocks last worked out, and the aim, the tool and the tick they were worked out for. */
    private static List<BlockPos> outlined = List.of();
    private static Map<BlockPos, BlockState> ghosts = Map.of();
    private static Aim aim;

    private ToolPreviews() {
    }

    /** Outlines the blocks an area or vein will take, beside the game's outline of the block aimed at. */
    static void onHighlight(final RenderHighlightEvent.Block event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        final ItemStack tool = player.getMainHandItem();
        final Optional<MiningShape> shape = Tools.mines(minecraft.level, tool);
        if (shape.isEmpty() || !Tools.works(minecraft.level, tool)) {
            return;
        }
        final BlockHitResult hit = event.getTarget();
        final Aim now = new Aim(hit.getBlockPos(), hit.getDirection(), tool.getItem(), tool.getComponentsPatch()
                .hashCode(), minecraft.level.getGameTime() / TICKS_KEPT);
        if (!now.equals(aim)) {
            aim = now;
            outlined = AreaMining.around(minecraft.level, player, tool, hit.getBlockPos(), hit.getDirection(),
                    shape.get());
            ghosts = Map.of();
        }
        final Vec3 camera = event.getCamera().getPosition();
        final VertexConsumer lines = event.getMultiBufferSource().getBuffer(RenderType.lines());
        final int colour = ToolHudPalette.get().outline();
        for (final BlockPos pos : outlined) {
            box(event.getPoseStack(), lines, new AABB(pos).move(-camera.x, -camera.y, -camera.z), colour);
        }
    }

    /** Draws the ghosts of what a building tool will place, after the translucent blocks. */
    static void onStage(final RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        final ItemStack tool = player.getMainHandItem();
        final Optional<BuildMode> mode = Tools.builds(tool);
        if (mode.isEmpty() || mode.get() instanceof BuildMode.Copy) {
            return;
        }
        final Aim now = new Aim(hit.getBlockPos(), hit.getDirection(), tool.getItem(), tool.getComponentsPatch()
                .hashCode(), minecraft.level.getGameTime() / TICKS_KEPT);
        if (!now.equals(aim)) {
            aim = now;
            ghosts = ghosts(minecraft.level, player, tool, hit.getBlockPos(), hit.getDirection(), mode.get());
            outlined = List.of();
        }
        if (ghosts.isEmpty()) {
            return;
        }
        draw(event.getPoseStack(), event.getCamera(), minecraft.renderBuffers().bufferSource());
    }

    /* Where the ghosts go and what they are, as many of each block as the inventory holds. */
    private static Map<BlockPos, BlockState> ghosts(final Level level, final LocalPlayer player, final ItemStack tool,
                                                     final BlockPos clicked, final Direction face,
                                                     final BuildMode mode) {
        final Map<BlockPos, BlockState> wanted = new HashMap<>();
        if (mode instanceof BuildMode.Paste) {
            final CopiedRegion copy = tool.get(BuildingTools.COPY.get());
            if (copy == null) {
                return Map.of();
            }
            for (final Map.Entry<BlockPos, BlockState> block : BuildingTools.pasted(copy, clicked, face,
                    player.getDirection()).entrySet()) {
                if (level.getBlockState(block.getKey()).canBeReplaced()) {
                    wanted.put(block.getKey(), block.getValue());
                }
            }
        } else {
            final BlockState state = level.getBlockState(clicked);
            for (final BlockPos pos : BuildingTools.targets(level, player, clicked, face, mode)) {
                wanted.put(pos, state);
            }
        }
        final Map<Item, Integer> left = new HashMap<>();
        final Map<BlockPos, BlockState> shown = new HashMap<>();
        for (final Map.Entry<BlockPos, BlockState> block : wanted.entrySet()) {
            final Item item = block.getValue().getBlock().asItem();
            final int has = left.computeIfAbsent(item, counted -> held(player, counted));
            if (has > 0) {
                left.put(item, has - 1);
                shown.put(block.getKey(), block.getValue());
            }
        }
        return shown;
    }

    /* How many of {@code item} the player can place: as many as wanted in creative, else the plain stacks held. */
    private static int held(final LocalPlayer player, final Item item) {
        if (player.getAbilities().instabuild) {
            return Integer.MAX_VALUE;
        }
        int count = 0;
        for (final ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && stack.getComponentsPatch().isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void draw(final PoseStack pose, final Camera camera, final MultiBufferSource.BufferSource buffers) {
        final Vec3 at = camera.getPosition();
        final BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        final VertexConsumer faded = new FadedConsumer(buffers.getBuffer(Sheets.translucentCullBlockSheet()), GHOST);
        final List<BlockPos> order = new ArrayList<>(ghosts.keySet());
        for (final BlockPos pos : order) {
            final BlockState state = ghosts.get(pos);
            pose.pushPose();
            pose.translate(pos.getX() - at.x, pos.getY() - at.y, pos.getZ() - at.z);
            blocks.getModelRenderer().renderModel(pose.last(), faded, state, blocks.getBlockModel(state), 1F, 1F, 1F,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            pose.popPose();
        }
        buffers.endBatch(Sheets.translucentCullBlockSheet());
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        final int colour = ToolHudPalette.get().ghostEdge();
        for (final BlockPos pos : order) {
            box(pose, lines, new AABB(pos).inflate(EDGE_OUT).move(-at.x, -at.y, -at.z), colour);
        }
        buffers.endBatch(RenderType.lines());
    }

    private static void box(final PoseStack pose, final VertexConsumer lines, final AABB box, final int colour) {
        LevelRenderer.renderLineBox(pose, lines, box, FastColor.ARGB32.red(colour) / 255.0F,
                FastColor.ARGB32.green(colour) / 255.0F, FastColor.ARGB32.blue(colour) / 255.0F,
                FastColor.ARGB32.alpha(colour) / 255.0F);
    }

    /* Where the player aims, with what, and when: what the blocks shown were worked out for. */
    private record Aim(BlockPos pos, Direction face, Item item, int components, long tick) {
    }
}
