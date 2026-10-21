/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import dev.jstech.core.JsCore;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.Materials;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * A wood, a stone or a metal found through the common tags, which variants are made of: its family, its id
 * ({@code minecraft:oak}, {@code create:limestone}, {@code c:tin}) and the block its variants are made from and drawn
 * with (the oak's planks, the limestone itself, a block of tin).
 *
 * @param family its family
 * @param id     its id: a wood by its planks' mod and name, a stone by its block's id, a metal by its common tag's name
 * @param source the id of the block its variants copy and are drawn with
 */
public record FoundMaterial(VariantFamily family, ResourceLocation id, ResourceLocation source) {

    private static final String WOOD_KEY = JsCore.MODID + ".variant.wood.";
    private static final String METAL_KEY = JsCore.MODID + ".variant.metal.";

    public FoundMaterial {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(source, "source");
    }

    /** The block its variants copy and are drawn with. */
    public Block block() {
        return BuiltInRegistries.BLOCK.get(this.source);
    }

    /** The mod it comes from: the mod of its block. */
    public String modid() {
        return this.source.getNamespace();
    }

    /**
     * What it is called in the player's language: a stone by its own block's name ("Limestone"), a metal by the Core's
     * material of that name when there is one ("Tin"), a wood by the Core's words for the game's own woods; otherwise
     * the words of its id, which a resource pack translates under {@code jscore.variant.wood.<mod>.<name>} or
     * {@code jscore.variant.metal.<name>}.
     */
    public Text name() {
        final String english = VariantPlan.englishOf(this.id.getPath());
        return switch (this.family) {
            case STONE -> TextKey.of(block().getDescriptionId(), english).text();
            case METAL -> {
                for (final Material material : Materials.all()) {
                    if (material.kind() == MaterialKind.METAL && material.id().getPath().equals(this.id.getPath())) {
                        yield material.name();
                    }
                }
                yield TextKey.of(METAL_KEY + keyPart(this.id.getPath()), english).text();
            }
            case WOOD -> TextKey.of(WOOD_KEY + keyPart(this.id.getNamespace()) + "." + keyPart(this.id.getPath()),
                    english).text();
        };
    }

    /* A part of an id as a part of a key, whose words are joined by dots. */
    private static String keyPart(final String part) {
        return part.replace('/', '.');
    }
}
