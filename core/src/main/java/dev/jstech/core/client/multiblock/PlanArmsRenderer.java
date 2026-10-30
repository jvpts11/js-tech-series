/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.multiblock.plan.ArmMotion;
import dev.jstech.core.multiblock.plan.ArmReach;
import dev.jstech.core.multiblock.plan.PlanArms;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Draws a machine's robot arms ({@link PlanArms}) on the player's game, each where its work has it this frame: its
 * model's bones turned to put its tool on its block, elbow up ({@link ArmReach}), the block it carries in its grip, the
 * block waiting for it in its feeder, and the sparks of its weld. An arm's model stands straight up at rest, its base
 * on its rail; its bones turn in order, each from the one before: {@link #TURRET} about the upright, then
 * {@link #UPPER}, {@link #FORE} and {@link #TOOL}, each leaning forward about its own sideways axis, and its tool's
 * grip a block's middle {@link ArmMotion#WRIST} out past its wrist.
 */
public final class PlanArmsRenderer {

    /** The bones an arm's model turns. */
    public static final String TURRET = "turret";
    public static final String UPPER = "upper";
    public static final String FORE = "fore";
    public static final String TOOL = "tool";
    /* The Core's sparks, thrown from a weld every few ticks. */
    private static final ResourceLocation SPARKS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "sparks");
    private static final int SPARK_EVERY = 4;
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final double HALF = 0.5;
    /* The block an arm carries, or that waits for it, drawn a hair under a block so it never flickers with one. */
    private static final float CARRIED = 0.98F;
    /* When each owner's arms last threw sparks, by arm, on this game's clock. */
    private static final Map<BlockEntity, Map<Integer, Long>> SPARKED = new WeakHashMap<>();
    private static final IKitDriver<ArmReach.Pose> DRIVER = new Driver();

    private PlanArmsRenderer() {
    }

    /**
     * Draws the arms {@code arms} of {@code owner}, its block entity renderer's pose stack on its block, each arm with
     * the model {@code model}, {@code partialTick} into the tick.
     */
    public static void draw(final BlockEntity owner, final PlanArms arms, final ResourceLocation model,
                            final PoseStack poses, final MultiBufferSource buffers, final int overlay,
                            final float partialTick) {
        final Level level = owner.getLevel();
        final Optional<KitModel> found = KitModels.get(model);
        if (level == null || found.isEmpty()) {
            return;
        }
        final double ticks = level.getGameTime() + (double) partialTick;
        final BlockPos at = owner.getBlockPos();
        for (int arm = 0; arm < arms.arms().size(); arm++) {
            final PlanArms.Arm rigged = arms.arms().get(arm);
            final ArmMotion.Rig rig = rigged.rig();
            final ArmMotion.State state = arms.state(arm, ticks);
            final double[] base = rig.base(state.rail());
            final double[] tool = state.tool();
            final ArmReach.Pose pose = ArmReach.solve(rig.shoulder(state.rail()), new double[] {tool[0],
                tool[1] + ArmMotion.WRIST, tool[2]}, rigged.upper(), rigged.fore());
            final int light = LevelRenderer.getLightColor(level, BlockPos.containing(base[0], base[1] + 1, base[2]));
            final KitInstance instance = KitAnimations.part(owner, "arm" + arm, found.get());
            instance.animate(KitClock.frame(), KitClock.now(), DRIVER, pose, IAnimationListener.NONE);
            poses.pushPose();
            poses.translate(base[0] - at.getX(), base[1] - at.getY(), base[2] - at.getZ());
            KitRenderer.draw(instance, ModelDefinition.Variant.NONE, owner, poses, buffers, light, overlay, null,
                    level, true);
            poses.popPose();
            final Optional<PlanArms.Job> job = arms.job(arm);
            if (job.isEmpty()) {
                continue;
            }
            final BlockState block = BuiltInRegistries.BLOCK.get(job.get().block()).defaultBlockState();
            final double into = ticks - job.get().start();
            if (state.holding()) {
                block(block, tool, at, poses, buffers, light, overlay);
            } else if (into >= 0 && into < ArmMotion.FETCH + ArmMotion.GRIP) {
                // Waiting in its feeder for the arm to grip it.
                final double[] feeder = rig.feeder();
                block(block, feeder, at, poses, buffers, light, overlay);
            }
            if (state.welding()) {
                spark(owner, arm, level, new Vec3(tool[0], job.get().at().getY() + 1.0, tool[2]));
            }
        }
    }

    /* Draws {@code block} with its middle at {@code middle}. */
    private static void block(final BlockState block, final double[] middle, final BlockPos at, final PoseStack poses,
                              final MultiBufferSource buffers, final int light, final int overlay) {
        poses.pushPose();
        poses.translate(middle[0] - at.getX(), middle[1] - at.getY(), middle[2] - at.getZ());
        poses.scale(CARRIED, CARRIED, CARRIED);
        poses.translate(-HALF, -HALF, -HALF);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block, poses, buffers, light, overlay);
        poses.popPose();
    }

    /* Throws the weld's sparks at {@code seam}, every few ticks of the arm's weld. */
    private static void spark(final BlockEntity owner, final int arm, final Level level, final Vec3 seam) {
        final Map<Integer, Long> last = SPARKED.computeIfAbsent(owner, holder -> new HashMap<>());
        final long now = level.getGameTime();
        if (now - last.getOrDefault(arm, Long.MIN_VALUE / 2) >= SPARK_EVERY) {
            last.put(arm, now);
            ClientEffects.play(SPARKS, seam, UP);
        }
    }

    /* Turns an arm's bones to its pose. */
    private static final class Driver implements IKitDriver<ArmReach.Pose> {

        @Override
        public void drive(final ArmReach.Pose pose, final ModelAnimator animator, final double seconds) {
            // Nothing plays on an arm; its bones are turned below.
        }

        @Override
        public void move(final ArmReach.Pose pose, final ModelPose bones, final double seconds) {
            turn(bones, TURRET, new Quaternionf().rotateY((float) pose.yaw()));
            turn(bones, UPPER, new Quaternionf().rotateX((float) pose.upper()));
            turn(bones, FORE, new Quaternionf().rotateX((float) pose.fore()));
            turn(bones, TOOL, new Quaternionf().rotateX((float) pose.tool()));
        }

        @Override
        public boolean movesBones() {
            return true;
        }

        private static void turn(final ModelPose bones, final String name, final Quaternionf turn) {
            final int bone = bones.index(name);
            if (bone >= 0) {
                bones.setTurn(bone, turn);
            }
        }
    }
}
