/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.content.GeoLook;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelDefinition;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a block entity with a model of the Core's: standing on the middle of its block's floor and facing the way
 * its block faces, as Bedrock's models of blocks do; drawn as the variant its block's state chooses; moved by its
 * driver and by what the server plays on it; holding what it holds at its locators. Only a block entity in sight is
 * drawn, as the game draws any, and only one near enough to see move is animated: one farther keeps its last pose.
 *
 * <pre>{@code
 * event.registerBlockEntityRenderer(MY_MACHINE.get(), context -> KitBlockEntityRenderer.<MachineEntity>of(
 *         ResourceLocation.fromNamespaceAndPath(MODID, "press"))
 *         .driver((machine, animator, seconds) -> animator.play("main", machine.running() ? "work" : "idle",
 *                 seconds))
 *         .build());
 * }</pre>
 *
 * @param <T> the block entity
 */
public final class KitBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    private final Function<? super T, Optional<KitModel>> model;
    private final IKitDriver<? super T> driver;
    private final IHeldThings<? super T> held;
    private final int viewDistance;
    private final double animateDistance;

    /** How far away, in blocks, a block entity is drawn unless its renderer says. */
    public static final int VIEW_DISTANCE = 64;
    /** How far away, in blocks, a block entity drawn is animated unless its renderer says. */
    public static final double ANIMATE_DISTANCE = 48;

    private KitBlockEntityRenderer(final Builder<T> builder) {
        this.model = builder.model;
        this.driver = builder.driver;
        this.held = builder.held;
        this.viewDistance = builder.viewDistance;
        this.animateDistance = builder.animateDistance;
    }

    /** A renderer drawing every block entity of its kind with the declared model {@code model}. */
    public static <T extends BlockEntity> Builder<T> of(final ResourceLocation model) {
        return new Builder<T>(entity -> KitModels.get(model));
    }

    /** A renderer drawing each block entity with the declared model it names. */
    public static <T extends BlockEntity> Builder<T> of(final Function<? super T, ResourceLocation> model) {
        return new Builder<T>(entity -> KitModels.get(model.apply(entity)));
    }

    /**
     * A renderer drawing each block entity of a family laid out for GeckoLib with the Core's models instead: the
     * model, the texture and the animation file its look names, read as they are.
     */
    public static <T extends BlockEntity> Builder<T> look(final GeoLook<? super T> look) {
        return new Builder<T>(entity -> KitModels.look(look.modelOf(entity), look.textureOf(entity),
                look.animationFile()));
    }

    @Override
    public void render(final T entity, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        final Optional<KitModel> found = this.model.apply(entity);
        if (found.isEmpty()) {
            return;
        }
        final KitModel base = found.get();
        final ModelDefinition.Variant variant = base.definition().variantFor(KitStates.of(entity.getBlockState()));
        final KitInstance instance = KitAnimations.of(entity, base.drawnAs(variant));
        final Direction facing = KitStates.facing(entity.getBlockState());
        final double seconds = KitClock.seconds(partialTick);
        final BlockPos pos = entity.getBlockPos();
        final Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (camera.distanceToSqr(Vec3.atCenterOf(pos)) <= this.animateDistance * this.animateDistance) {
            this.driver.drive(entity, instance.animator(), seconds);
            instance.animate(KitClock.frame(), seconds, this.driver.movesBones(), listener(entity, instance,
                    pos, facing));
        }
        poses.pushPose();
        place(poses, facing);
        KitRenderer.draw(instance, variant, entity, poses, buffers, light, overlay, held(), entity.getLevel());
        poses.popPose();
    }

    @Override
    public int getViewDistance() {
        return this.viewDistance;
    }

    @Override
    public boolean shouldRenderOffScreen(final T entity) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(final T entity) {
        final Optional<KitModel> found = this.model.apply(entity);
        final BlockPos pos = entity.getBlockPos();
        if (found.isEmpty()) {
            return new AABB(pos);
        }
        // The model turns with its block: the box it can reach whichever way it faces, about its block's floor.
        final AABB box = found.get().bounds();
        final double reach = Math.max(Math.max(Math.max(Math.abs(box.minX), Math.abs(box.maxX)),
                Math.max(Math.abs(box.minZ), Math.abs(box.maxZ))), Math.max(Math.abs(box.minY), Math.abs(box.maxY)));
        return new AABB(pos.getX() + 0.5 - reach, pos.getY() - reach, pos.getZ() + 0.5 - reach,
                pos.getX() + 0.5 + reach, pos.getY() + reach, pos.getZ() + 0.5 + reach).minmax(new AABB(pos));
    }

    /** Stands a model on the middle of a block's floor, facing as the block does. */
    static void place(final PoseStack poses, final Direction facing) {
        poses.translate(0.5, 0, 0.5);
        switch (facing) {
            case SOUTH -> poses.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> poses.mulPose(Axis.YP.rotationDegrees(90));
            case EAST -> poses.mulPose(Axis.YP.rotationDegrees(270));
            case UP -> poses.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> poses.mulPose(Axis.XN.rotationDegrees(90));
            default -> {
                // A model faces north as it is drawn.
            }
        }
    }

    @SuppressWarnings("unchecked")
    private IHeldThings<T> held() {
        return (IHeldThings<T>) this.held;
    }

    private IAnimationListener listener(final T entity, final KitInstance instance, final BlockPos pos,
                                        final Direction facing) {
        return KitEvents.of(entity, this.driver, instance, entity.getLevel(), () -> {
            final PoseStack placed = new PoseStack();
            placed.translate(pos.getX(), pos.getY(), pos.getZ());
            place(placed, facing);
            return new Matrix4f(placed.last().pose());
        }, SoundSource.BLOCKS);
    }

    /**
     * A renderer being declared: its model, and what drives it, what it holds, how far it is seen and animated.
     *
     * @param <T> the block entity
     */
    public static final class Builder<T extends BlockEntity> {

        private final Function<? super T, Optional<KitModel>> model;
        private IKitDriver<? super T> driver = IKitDriver.none();
        private IHeldThings<? super T> held = IHeldThings.none();
        private int viewDistance = VIEW_DISTANCE;
        private double animateDistance = ANIMATE_DISTANCE;

        private Builder(final Function<? super T, Optional<KitModel>> model) {
            this.model = Objects.requireNonNull(model, "model");
        }

        /** The code that chooses what it plays and moves its bones. */
        public Builder<T> driver(final IKitDriver<? super T> chosen) {
            this.driver = Objects.requireNonNull(chosen, "driver");
            return this;
        }

        /** What it holds at its locators. */
        public Builder<T> held(final IHeldThings<? super T> things) {
            this.held = Objects.requireNonNull(things, "held");
            return this;
        }

        /** How far away it is drawn, in blocks. */
        public Builder<T> viewDistance(final int blocks) {
            this.viewDistance = blocks;
            return this;
        }

        /** How far away it is animated, in blocks; farther, it keeps its last pose. */
        public Builder<T> animateDistance(final double blocks) {
            this.animateDistance = blocks;
            return this;
        }

        /** The renderer. */
        public KitBlockEntityRenderer<T> build() {
            return new KitBlockEntityRenderer<>(this);
        }
    }
}
