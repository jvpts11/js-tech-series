/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import java.util.Objects;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;

/**
 * How a family of machines drawn from geometry files looks, said once: which model each one is drawn with (by era,
 * by kind), the folder their textures are in, and the animation they share. Each model is a file of its own, its
 * texture an atlas named after it, so a family's files sit side by side, laid out as GeckoLib and Bedrock lay them:
 *
 * <pre>{@code
 * assets/<mod>/geo/<model>.geo.json
 * assets/<mod>/textures/block/<folder>/<model>.png
 * assets/<mod>/animations/<animation>.animation.json
 * }</pre>
 *
 * <p>It is declared with the blocks, where both sides see it; the player's game draws the family with the Core's
 * models, from the same files:
 *
 * <pre>{@code
 * GeoLook<RackEntity> RACK = GeoLook.of(MODID, "rack", "rack", rack -> rack.modelName());
 * CONTENT.block("server_rack", ServerRackBlock::new).geo(RACK)...
 * event.registerBlockEntityRenderer(RACK_ENTITY.get(), context -> KitBlockEntityRenderer.look(RACK).build());
 * }</pre>
 *
 * @param modid     the mod whose files they are
 * @param folder    the folder of {@code textures/block/} the atlases are in
 * @param animation the animation file the family shares, by name
 * @param model     which model a machine of the family is drawn with, by name
 * @param <T>       what is drawn: a block entity, an item
 */
public record GeoLook<T>(String modid, String folder, String animation, Function<? super T, String> model) {

    public GeoLook {
        Objects.requireNonNull(modid, "modid");
        Objects.requireNonNull(folder, "folder");
        Objects.requireNonNull(animation, "animation");
        Objects.requireNonNull(model, "model");
    }

    /** The family of {@code modid}'s models in {@code folder}, sharing {@code animation}. */
    public static <T> GeoLook<T> of(final String modid, final String folder, final String animation,
                                    final Function<? super T, String> model) {
        return new GeoLook<>(modid, folder, animation, model);
    }

    /**
     * The model file of the model called {@code name}: one of the family's mod, or, for a name that says its mod
     * ({@code othermod:computer_standard_neutral_reader}), one another mod brings to the family in its own files.
     */
    public ResourceLocation modelFile(final String name) {
        return ResourceLocation.fromNamespaceAndPath(namespaceOf(name), "geo/" + pathOf(name) + ".geo.json");
    }

    /** The atlas of the model called {@code name}, in its mod's files as its model is. */
    public ResourceLocation textureFile(final String name) {
        return ResourceLocation.fromNamespaceAndPath(namespaceOf(name),
                "textures/block/" + this.folder + "/" + pathOf(name) + ".png");
    }

    /** The animation file the family shares. */
    public ResourceLocation animationFile() {
        return ResourceLocation.fromNamespaceAndPath(this.modid, "animations/" + this.animation + ".animation.json");
    }

    /** The model file {@code drawn} is drawn with. */
    public ResourceLocation modelOf(final T drawn) {
        return modelFile(this.model.apply(drawn));
    }

    /** The atlas {@code drawn} is painted with. */
    public ResourceLocation textureOf(final T drawn) {
        return textureFile(this.model.apply(drawn));
    }

    /* The mod a model's files are in: the one its name says before a colon, or the family's. */
    private String namespaceOf(final String name) {
        final int colon = name.indexOf(':');
        return colon < 0 ? this.modid : name.substring(0, colon);
    }

    /* A model's name without the mod it says. */
    private static String pathOf(final String name) {
        final int colon = name.indexOf(':');
        return colon < 0 ? name : name.substring(colon + 1);
    }
}
