/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.geo;

import dev.jstech.core.content.GeoLook;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;

/**
 * The GeckoLib model of anything drawn the way a {@link GeoLook} says, on the player's game: one class for every
 * family, in place of a model class copied for each. Only a mod that draws with GeckoLib, and so brings it, loads it.
 *
 * @param <T> what is drawn
 */
public final class LookGeoModel<T extends GeoAnimatable> extends GeoModel<T> {

    private final Function<T, ResourceLocation> model;
    private final Function<T, ResourceLocation> texture;
    private final Function<T, ResourceLocation> animation;

    /** The model {@code look} says each of its family is drawn with. */
    public LookGeoModel(final GeoLook<? super T> look) {
        this(look::modelOf, look::textureOf, drawn -> look.animationFile());
    }

    /** A model whose files each drawn thing names itself, as an item that knows the machine it places does. */
    public LookGeoModel(final Function<T, ResourceLocation> model, final Function<T, ResourceLocation> texture,
                        final Function<T, ResourceLocation> animation) {
        this.model = Objects.requireNonNull(model, "model");
        this.texture = Objects.requireNonNull(texture, "texture");
        this.animation = Objects.requireNonNull(animation, "animation");
    }

    @Override
    public ResourceLocation getModelResource(final T drawn) {
        return this.model.apply(drawn);
    }

    @Override
    public ResourceLocation getTextureResource(final T drawn) {
        return this.texture.apply(drawn);
    }

    @Override
    public ResourceLocation getAnimationResource(final T drawn) {
        return this.animation.apply(drawn);
    }
}
