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
import dev.jstech.core.model.ModelDefinition;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

/**
 * Draws an entity with a model of the Core's: standing where the entity stands, turned the way it looks, moved by its
 * driver and by what the server plays on it, holding what it holds at its locators. Its keyframes read the entity's
 * own queries ready-made ({@link EntityQueries}).
 *
 * <pre>{@code
 * event.registerEntityRenderer(MY_ROBOT.get(), context -> KitEntityRenderer.of(context,
 *         ResourceLocation.fromNamespaceAndPath(MODID, "robot"), robot -> robot.isWorking() ? ... ));
 * }</pre>
 *
 * @param <T> the entity
 */
public final class KitEntityRenderer<T extends Entity> extends EntityRenderer<T> {

    private final Function<? super T, ResourceLocation> model;
    private final IKitDriver<? super T> driver;
    private final IHeldThings<T> held;

    private KitEntityRenderer(final EntityRendererProvider.Context context,
                              final Function<? super T, ResourceLocation> model, final IKitDriver<? super T> driver,
                              final IHeldThings<T> held) {
        super(context);
        this.model = model;
        this.driver = driver;
        this.held = held;
    }

    /** A renderer drawing every entity of its kind with the model {@code model}, moved by {@code driver}. */
    public static <T extends Entity> KitEntityRenderer<T> of(final EntityRendererProvider.Context context,
                                                             final ResourceLocation model,
                                                             final IKitDriver<? super T> driver) {
        return of(context, entity -> model, driver, IHeldThings.none());
    }

    /** A renderer drawing each entity with the model it names, driven and holding as given. */
    public static <T extends Entity> KitEntityRenderer<T> of(final EntityRendererProvider.Context context,
                                                             final Function<? super T, ResourceLocation> model,
                                                             final IKitDriver<? super T> driver,
                                                             final IHeldThings<T> held) {
        return new KitEntityRenderer<>(context, Objects.requireNonNull(model, "model"),
                Objects.requireNonNull(driver, "driver"), Objects.requireNonNull(held, "held"));
    }

    @Override
    public void render(final T entity, final float yaw, final float partialTick, final PoseStack poses,
                       final MultiBufferSource buffers, final int light) {
        final Optional<KitModel> found = KitModels.get(this.model.apply(entity));
        if (found.isPresent()) {
            final KitInstance instance = KitAnimations.of(entity, found.get());
            final double seconds = KitClock.seconds(partialTick);
            EntityQueries.hand(instance.animator().context(), entity, partialTick);
            this.driver.drive(entity, instance.animator(), seconds);
            instance.animate(KitClock.frame(), seconds, this.driver, entity, KitEvents.of(entity, this.driver,
                    instance, entity.level(), () -> new Matrix4f().translation((float) entity.getX(),
                            (float) entity.getY(), (float) entity.getZ()).rotateY((float) Math.toRadians(180 - yaw)),
                    SoundSource.NEUTRAL));
            poses.pushPose();
            // A model faces north as it is drawn; an entity looking at yaw 0 faces south.
            poses.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
            KitRenderer.draw(instance, ModelDefinition.Variant.NONE, entity, poses, buffers, light,
                    OverlayTexture.NO_OVERLAY, this.held, entity.level(), true);
            poses.popPose();
        }
        super.render(entity, yaw, partialTick, poses, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(final T entity) {
        return KitModels.get(this.model.apply(entity)).map(found -> found.texture(0))
                .orElse(MissingTextureAtlasSprite.getLocation());
    }
}
