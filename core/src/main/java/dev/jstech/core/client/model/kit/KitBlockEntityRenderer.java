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
import dev.jstech.core.multiblock.IStructureController;
import dev.jstech.core.multiblock.Orientation;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Draws a block entity with a model of the Core's: standing on the middle of its block's floor and facing the way
 * its block faces, as Bedrock's models of blocks do; drawn as the variant its block's state chooses; moved by its
 * driver and by what the server plays on it; holding what it holds at its locators. The controller of a structure
 * whose blocks merge into its model draws, once formed, its structure's model instead, standing as the structure
 * stands. Only a block entity in sight is drawn, as the game draws any, and only one near enough to see move is
 * animated: one farther keeps its last pose.
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
    private final @Nullable ResourceLocation formed;
    private final IKitDriver<? super T> driver;
    private final IHeldThings<? super T> held;
    private final @Nullable IKitExtras<? super T> extras;
    private final @Nullable Function<? super T, AABB> box;
    private final Predicate<? super T> drawn;
    private final boolean upAndDownAboutMiddle;
    private final int viewDistance;
    private final double animateDistance;

    /** How far away, in blocks, a block entity is drawn unless its renderer says. */
    public static final int VIEW_DISTANCE = 64;
    /** How far away, in blocks, a block entity drawn is animated unless its renderer says. */
    public static final double ANIMATE_DISTANCE = 48;
    private static final double HALF_BLOCK = 0.5;

    private KitBlockEntityRenderer(final Builder<T> builder) {
        this.model = builder.model;
        this.formed = builder.formed;
        this.driver = builder.driver;
        this.held = builder.held;
        this.extras = builder.extras;
        this.box = builder.box;
        this.drawn = builder.drawn;
        this.upAndDownAboutMiddle = builder.upAndDownAboutMiddle;
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
     * A renderer drawing each block entity of a family of geometry files laid out as GeckoLib lays them: the model,
     * the texture and the animation file its look names, read as they are.
     */
    public static <T extends BlockEntity> Builder<T> look(final GeoLook<? super T> look) {
        return new Builder<T>(entity -> KitModels.look(look.modelOf(entity), look.textureOf(entity),
                look.animationFile()));
    }

    /**
     * A renderer drawing a structure's controller only as the declared model {@code model} of its formed structure,
     * and nothing while it is not formed, its block's own model showing then.
     */
    public static <T extends BlockEntity> Builder<T> formedOnly(final ResourceLocation model) {
        return new Builder<T>(entity -> Optional.empty()).formed(model);
    }

    @Override
    public void render(final T entity, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light, final int overlay) {
        if (!this.drawn.test(entity)) {
            return;
        }
        final Optional<Orientation> standing = standing(entity);
        final Optional<KitModel> found = standing.isPresent() ? KitModels.get(this.formed) : this.model.apply(entity);
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
            instance.animate(KitClock.frame(), seconds, driver(), entity, listener(entity, instance, pos, facing,
                    standing));
        }
        poses.pushPose();
        if (standing.isPresent()) {
            stand(poses, standing.get());
        } else {
            place(poses, facing, this.upAndDownAboutMiddle);
        }
        // Inside a whole block no light reaches: a model drawn from one, or spread over a structure, takes its
        // surroundings' light.
        final int lit = standing.isPresent() || entity.getBlockState().canOcclude() ? lightAround(entity.getLevel(),
                pos, light) : light;
        KitRenderer.draw(instance, variant, entity, poses, buffers, lit, overlay, held(), entity.getLevel(), true);
        if (this.extras != null) {
            this.extras.draw(entity, instance, poses, buffers, lit, overlay, partialTick);
        }
        poses.popPose();
    }

    /**
     * The light around the block at {@code pos}, brightest of its own, {@code own}, and its six neighbours', the
     * light of blocks and of the sky each at its brightest.
     */
    public static int lightAround(@Nullable final Level level, final BlockPos pos, final int own) {
        if (level == null) {
            return own;
        }
        int block = LightTexture.block(own);
        int sky = LightTexture.sky(own);
        for (final Direction way : Direction.values()) {
            final int there = LevelRenderer.getLightColor(level, pos.relative(way));
            block = Math.max(block, LightTexture.block(there));
            sky = Math.max(sky, LightTexture.sky(there));
        }
        return LightTexture.pack(block, sky);
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
        if (this.box != null) {
            return this.box.apply(entity);
        }
        final Optional<KitModel> found = standing(entity).isPresent() ? KitModels.get(this.formed)
                : this.model.apply(entity);
        final BlockPos pos = entity.getBlockPos();
        if (found.isEmpty()) {
            return new AABB(pos);
        }
        // The model turns with its block or its structure: the box it can reach whichever way it stands.
        final AABB box = found.get().bounds();
        final double reach = Math.max(Math.max(Math.max(Math.abs(box.minX), Math.abs(box.maxX)),
                Math.max(Math.abs(box.minZ), Math.abs(box.maxZ))), Math.max(Math.abs(box.minY), Math.abs(box.maxY)));
        return new AABB(pos.getX() + 0.5 - reach, pos.getY() - reach, pos.getZ() + 0.5 - reach,
                pos.getX() + 0.5 + reach, pos.getY() + reach, pos.getZ() + 0.5 + reach).minmax(new AABB(pos));
    }

    /** Stands a model on the middle of a block's floor, facing as the block does. */
    static void place(final PoseStack poses, final Direction facing) {
        place(poses, facing, false);
    }

    /*
     * Stands a structure's model as its structure stands: made as the structure stands facing north, as its pattern
     * is drawn, on its controller's floor, it is turned, laid down or mirrored about the controller's middle.
     */
    static void stand(final PoseStack poses, final Orientation orientation) {
        final Matrix4f turn = new Matrix4f().zero();
        turn.set(orientation.xFrom(), 0, orientation.xSign());
        turn.set(orientation.yFrom(), 1, orientation.ySign());
        turn.set(orientation.zFrom(), 2, orientation.zSign());
        turn.m33(1);
        poses.translate(0.5, 0.5, 0.5);
        poses.mulPose(turn);
        poses.translate(0, -0.5, 0);
    }

    /*
     * Stands a model on the middle of a block's floor, facing as the block does; faced up or down, turned about the
     * middle of its block rather than of its floor, when {@code aboutMiddle} says.
     */
    private static void place(final PoseStack poses, final Direction facing, final boolean aboutMiddle) {
        final boolean upright = aboutMiddle && facing.getAxis() == Direction.Axis.Y;
        poses.translate(0.5, upright ? HALF_BLOCK : 0, 0.5);
        turn(poses, facing);
        if (upright) {
            poses.translate(0, -HALF_BLOCK, 0);
        }
    }

    private static void turn(final PoseStack poses, final Direction facing) {
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

    /* How the structure the block entity controls stands, when it is formed and draws a model formed; or none. */
    private Optional<Orientation> standing(final T entity) {
        if (this.formed == null || !(entity instanceof IStructureController controller) || !controller.merged()) {
            return Optional.empty();
        }
        return controller.formedOrientation();
    }

    @SuppressWarnings("unchecked")
    private IHeldThings<T> held() {
        return (IHeldThings<T>) this.held;
    }

    @SuppressWarnings("unchecked")
    private IKitDriver<T> driver() {
        return (IKitDriver<T>) this.driver;
    }

    private IAnimationListener listener(final T entity, final KitInstance instance, final BlockPos pos,
                                        final Direction facing, final Optional<Orientation> standing) {
        return KitEvents.of(entity, this.driver, instance, entity.getLevel(), () -> {
            final PoseStack placed = new PoseStack();
            placed.translate(pos.getX(), pos.getY(), pos.getZ());
            if (standing.isPresent()) {
                stand(placed, standing.get());
            } else {
                place(placed, facing, this.upAndDownAboutMiddle);
            }
            return new Matrix4f(placed.last().pose());
        }, SoundSource.BLOCKS);
    }

    /**
     * A renderer being declared: its model, and what drives it, what it holds and draws besides, the box it is drawn
     * within and while, how it turns, how far it is seen and animated.
     *
     * @param <T> the block entity
     */
    public static final class Builder<T extends BlockEntity> {

        private final Function<? super T, Optional<KitModel>> model;
        private @Nullable ResourceLocation formed;
        private IKitDriver<? super T> driver = IKitDriver.none();
        private IHeldThings<? super T> held = IHeldThings.none();
        private @Nullable IKitExtras<? super T> extras;
        private @Nullable Function<? super T, AABB> box;
        private Predicate<? super T> drawn = entity -> true;
        private boolean upAndDownAboutMiddle;
        private int viewDistance = VIEW_DISTANCE;
        private double animateDistance = ANIMATE_DISTANCE;

        private Builder(final Function<? super T, Optional<KitModel>> model) {
            this.model = Objects.requireNonNull(model, "model");
        }

        /**
         * Once the structure it controls has formed and its blocks merged, it draws the declared model
         * {@code structureModel} of the whole structure, standing as the structure stands.
         */
        public Builder<T> formed(final ResourceLocation structureModel) {
            this.formed = Objects.requireNonNull(structureModel, "structureModel");
            return this;
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

        /** What it draws besides its model, every frame, where its model stands. */
        public Builder<T> extras(final IKitExtras<? super T> more) {
            this.extras = Objects.requireNonNull(more, "extras");
            return this;
        }

        /**
         * The box it is drawn within, in the world's blocks, in place of its model's: for a machine that knows how far
         * it reaches better than its model at rest says, as one whose parts ride out of it.
         */
        public Builder<T> box(final Function<? super T, AABB> bounds) {
            this.box = Objects.requireNonNull(bounds, "box");
            return this;
        }

        /** Draws it only while {@code shown} holds: a reader drawn by its block's own model in some of its forms. */
        public Builder<T> drawnWhile(final Predicate<? super T> shown) {
            this.drawn = Objects.requireNonNull(shown, "drawnWhile");
            return this;
        }

        /**
         * Faced up or down, it turns about the middle of its block rather than of its floor, so a model made to stand
         * on a wall comes as close to the block above or below as it comes to the wall; facing a side, it stands as
         * ever.
         */
        public Builder<T> upAndDownAboutMiddle() {
            this.upAndDownAboutMiddle = true;
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
