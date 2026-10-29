/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.RocketKit;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.rocket.RocketPartModels;
import dev.jstech.space.rocket.RocketStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The rocket being designed, drawn in the hangar itself while its design screen is open, standing on the middle of the
 * bay's floor: each part with its own model, turned as it was set; a part being placed see-through, cyan, where it
 * will go or under the mouse; the part under the mouse outlined; and the hazard stripes painted round the rocket's
 * place on the floor. Where a part has no model, as one a data pack adds, its box is outlined.
 */
public final class RocketDesignScene {

    /* A part turned a quarter turn is turned this many degrees, clockwise seen from above. */
    private static final float QUARTER = 90.0F;
    /* The stripes round the rocket: a block clear of its widest part, half a block wide, just above the floor. */
    private static final double CLEAR = 1.0;
    private static final double STRIPE = 0.5;
    private static final double FLOOR = 0.01;
    private static final float CHANNEL = 255.0F;
    private static final ResourceLocation HAZARD = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "textures/misc/hangar_hazard.png");

    private static @Nullable Shown shown;

    private RocketDesignScene() {
    }

    /** Hands the game's bus what this answers, once, from the mod's client. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, RocketDesignScene::onRender);
    }

    /** Draws {@code scene} from the next frame on, in place of what was drawn. */
    public static void show(final Shown scene) {
        shown = scene;
    }

    /** Draws nothing more. */
    public static void hide() {
        shown = null;
    }

    /** What is drawn now, if anything is; for a test to look at. */
    public static Optional<Shown> shown() {
        return Optional.ofNullable(shown);
    }

    /**
     * The box each part of {@code stack} fills where it stands on {@code pad}, from the bottom up: as wide as its
     * widest end, as tall as it was built; a part the world does not know a block of each.
     */
    public static List<AABB> boxes(final Map<ResourceLocation, RocketPart> parts, final RocketStack stack,
                                   final BlockPos pad) {
        final List<AABB> boxes = new ArrayList<>();
        double y = pad.getY();
        for (final RocketStack.Section section : stack.sections()) {
            final RocketPart part = parts.get(section.part().id());
            final double tall = tall(part, section.part());
            final double half = (part == null ? 1 : Math.max(part.diameter(), part.topDiameter())) / 2.0;
            boxes.add(new AABB(pad.getX() + 0.5 - half, y, pad.getZ() + 0.5 - half, pad.getX() + 0.5 + half,
                    y + tall, pad.getZ() + 0.5 + half));
            y += tall;
        }
        return List.copyOf(boxes);
    }

    /**
     * The boxes of the boosters round the part filling {@code core}, each a ring's place, the first to the east and
     * the rest round from it, each as tall as its segments stacked.
     */
    public static List<AABB> boosterBoxes(final Map<ResourceLocation, RocketPart> parts,
                                          final RocketStack.Section section, final AABB core) {
        final List<AABB> boxes = new ArrayList<>();
        final int count = section.boosters().size();
        for (int i = 0; i < count; i++) {
            final List<RocketStack.Part> booster = section.boosters().get(i);
            final RocketPart foot = booster.isEmpty() ? null : parts.get(booster.get(0).id());
            final double half = (foot == null ? 1 : foot.diameter()) / 2.0;
            double tall = 0;
            for (final RocketStack.Part segment : booster) {
                tall += tall(parts.get(segment.id()), segment);
            }
            final Vec3 at = boosterAt(core, half, i, count);
            boxes.add(new AABB(at.x - half, core.minY, at.z - half, at.x + half, core.minY + tall, at.z + half));
        }
        return List.copyOf(boxes);
    }

    /* Draws the scene, after the world's block entities, into the world's buffers. */
    private static void onRender(final RenderLevelStageEvent event) {
        final Shown scene = shown;
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (scene == null || level == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            return;
        }
        final Vec3 camera = event.getCamera().getPosition();
        final PoseStack poses = event.getPoseStack();
        final MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        stripes(poses, buffers, scene, level);
        final Vec3 foot = Vec3.atBottomCenterOf(scene.pad());
        rocket(poses, buffers, scene, level, scene.stack(), foot, scene.ghosts(), "design");
        scene.loose().ifPresent(loose -> rocket(poses, buffers, scene, level, new RocketStack(loose.run()),
                loose.foot(), Set.of(-1), "held"));
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        final List<AABB> boxes = boxes(scene.parts(), scene.stack(), scene.pad());
        for (int k = 0; k < boxes.size(); k++) {
            final RocketStack.Section section = scene.stack().sections().get(k);
            if (!modelled(section.part().id())) {
                outline(poses, lines, boxes.get(k), c.stencil());
            }
            for (final AABB booster : boosterBoxes(scene.parts(), section, boxes.get(k))) {
                outline(poses, lines, booster, c.stencil());
            }
        }
        scene.outlined().filter(k -> k >= 0 && k < boxes.size()).ifPresent(k -> outline(poses, lines,
                boxes.get(k).inflate(0.05), c.yellow()));
        poses.popPose();
        buffers.endBatch();
    }

    /*
     * Draws {@code stack} standing on {@code foot}, the parts at {@code ghosts} see-through (any part at all when it
     * holds -1), their boosters with them.
     */
    private static void rocket(final PoseStack poses, final MultiBufferSource buffers, final Shown scene,
                               final ClientLevel level, final RocketStack stack, final Vec3 foot,
                               final Set<Integer> ghosts, final String name) {
        final List<RocketPartModels.Placed> placed = RocketPartModels.of(scene.parts(), scene.kits(), stack, false);
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        for (final RocketPartModels.Placed part : placed) {
            final Optional<KitModel> model = KitModels.get(part.model());
            if (model.isEmpty()) {
                continue;
            }
            final KitInstance instance = KitAnimations.part(scene.terminal(), name + ":" + part.section() + "@"
                    + part.y() + ":" + part.model(), model.get());
            final int light = LevelRenderer.getLightColor(level, BlockPos.containing(foot.x, foot.y + part.y(),
                    foot.z));
            poses.pushPose();
            poses.translate(foot.x, foot.y + part.y(), foot.z);
            poses.mulPose(Axis.YN.rotationDegrees(QUARTER * part.turns()));
            if (ghosts.contains(-1) || ghosts.contains(part.section())) {
                KitRenderer.drawTinted(instance, poses, buffers, c.holo(), light);
            } else {
                KitRenderer.draw(instance, ModelDefinition.Variant.NONE, scene.terminal(), poses, buffers, light,
                        OverlayTexture.NO_OVERLAY, null, level, false);
            }
            poses.popPose();
        }
    }

    /* The hazard stripes round the rocket's place, in half-block tiles, the stripes running on from tile to tile. */
    private static void stripes(final PoseStack poses, final MultiBufferSource buffers, final Shown scene,
                                final ClientLevel level) {
        final double inner = scene.width() / 2.0 + CLEAR;
        final double outer = inner + STRIPE;
        final double cx = scene.pad().getX() + 0.5;
        final double cz = scene.pad().getZ() + 0.5;
        final double y = scene.pad().getY() + FLOOR;
        final VertexConsumer into = buffers.getBuffer(RenderType.entityCutout(HAZARD));
        final int light = LevelRenderer.getLightColor(level, scene.pad());
        final PoseStack.Pose pose = poses.last();
        for (double a = -outer; a < outer - 1e-6; a += STRIPE) {
            for (double b = -outer; b < outer - 1e-6; b += STRIPE) {
                final boolean ring = a < -inner + 1e-6 || a >= inner - 1e-6 || b < -inner + 1e-6
                        || b >= inner - 1e-6;
                if (!ring) {
                    continue;
                }
                final double x0 = cx + a;
                final double z0 = cz + b;
                final float u = (float) (x0 - Math.floor(x0));
                final float v = (float) (z0 - Math.floor(z0));
                corner(into, pose, x0, y, z0, u, v, light);
                corner(into, pose, x0, y, z0 + STRIPE, u, v + (float) STRIPE, light);
                corner(into, pose, x0 + STRIPE, y, z0 + STRIPE, u + (float) STRIPE, v + (float) STRIPE, light);
                corner(into, pose, x0 + STRIPE, y, z0, u + (float) STRIPE, v, light);
            }
        }
    }

    private static void corner(final VertexConsumer into, final PoseStack.Pose pose, final double x, final double y,
                               final double z, final float u, final float v, final int light) {
        into.addVertex(pose, (float) x, (float) y, (float) z).setColor(1.0F, 1.0F, 1.0F, 1.0F).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0, 1, 0);
    }

    private static void outline(final PoseStack poses, final VertexConsumer lines, final AABB box, final int colour) {
        LevelRenderer.renderLineBox(poses, lines, box, FastColor.ARGB32.red(colour) / CHANNEL,
                FastColor.ARGB32.green(colour) / CHANNEL, FastColor.ARGB32.blue(colour) / CHANNEL, 1.0F);
    }

    /* Whether the part {@code id} has a model of its own to be drawn with. */
    private static boolean modelled(final ResourceLocation id) {
        return id.getNamespace().equals(JsSpace.MODID) && RocketPartModels.names().stream().anyMatch(
                name -> name.equals(id.getPath()) || name.startsWith(id.getPath() + "_"));
    }

    private static double tall(@Nullable final RocketPart part, final RocketStack.Part placed) {
        if (part == null) {
            return 1;
        }
        return part.height() * (part.stretches() ? placed.layers() : 1);
    }

    /* Where the booster {@code i} of {@code count} round {@code core} stands, its middle, touching the core. */
    private static Vec3 boosterAt(final AABB core, final double half, final int i, final int count) {
        final double angle = 2 * Math.PI * i / Math.max(1, count);
        final double reach = core.getXsize() / 2 + half;
        final Vec3 middle = core.getCenter();
        return new Vec3(middle.x + reach * Math.cos(angle), middle.y, middle.z + reach * Math.sin(angle));
    }

    /**
     * What the scene draws.
     *
     * @param terminal the hangar's terminal, which the models' poses are kept with
     * @param pad      the block the rocket stands on the middle of
     * @param parts    the parts the world knows
     * @param kits     their kits
     * @param stack    the rocket as it stands, with a part being placed already set where it will go
     * @param ghosts   the places of the parts being placed, drawn see-through
     * @param loose    a part being placed away from any node, drawn see-through where the mouse is
     * @param outlined the place of the part under the mouse, outlined
     * @param width    how wide the rocket stands, its boosters with it, in blocks
     */
    public record Shown(HangarTerminalBlockEntity terminal, BlockPos pad, Map<ResourceLocation, RocketPart> parts,
                        Map<ResourceLocation, RocketKit> kits, RocketStack stack, Set<Integer> ghosts,
                        Optional<Loose> loose, Optional<Integer> outlined, int width) {
    }

    /**
     * Parts being placed, away from any node.
     *
     * @param run  the parts, from the bottom up
     * @param foot where their foot is, the middle of it
     */
    public record Loose(List<RocketStack.Section> run, Vec3 foot) {
    }
}
