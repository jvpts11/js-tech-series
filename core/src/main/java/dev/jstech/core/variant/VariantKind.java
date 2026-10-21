/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import dev.jstech.core.content.ContentTab;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * A kind of variant a mod declared: one block for every wood, every stone or every metal found (a frame of every
 * wood, a casing of every metal), made by itself from the material's block, with no list kept by hand. Declared from
 * the mod's constructor with {@link Variants#kind}, its name a sentence of the mod's ("%s Casing", here
 * {@code CASING_NAME}):
 *
 * <pre>{@code
 * public static final VariantKind CASINGS = Variants.kind(MODID, "casing", VariantFamily.METAL, CASING_NAME)
 *         .model("mymod:block/casing_template", "plate", "particle")
 *         .tagged("mymod:casings")
 *         .tab(MY_SECTION)
 *         .declare();
 * }</pre>
 *
 * Each variant copies its material's block (hardness, sound, the tool it asks for), is named after the kind and the
 * material in the player's language ("Tin Casing"), is drawn with the kind's model and the material's own texture in
 * the model's textures, drops itself, and is in the tag of the tool its family is broken with and the kind's tags.
 */
public final class VariantKind {

    private final ResourceLocation id;
    private final VariantFamily family;
    private final TextKey name;
    private final ResourceLocation template;
    private final List<String> textures;
    private final List<String> tags;
    private final IVariantBlockMaker maker;

    private VariantKind(final Builder builder) {
        this.id = builder.id;
        this.family = builder.family;
        this.name = builder.name;
        this.template = builder.template;
        this.textures = List.copyOf(builder.textures);
        this.tags = List.copyOf(builder.tags);
        this.maker = builder.maker;
    }

    /** Its id: the declaring mod's, with the kind's name, as {@code mymod:casing}. */
    public ResourceLocation id() {
        return this.id;
    }

    /** The family of materials it is made for. */
    public VariantFamily family() {
        return this.family;
    }

    /** What its variants are called, with the material's name where {@code %s} is. */
    public TextKey name() {
        return this.name;
    }

    /** What the variant of {@code material} is called: "Tin Casing". */
    public Text nameOf(final FoundMaterial material) {
        return this.name.with(material.name());
    }

    /** The model its variants are drawn with, as {@code mymod:block/casing_template}. */
    public ResourceLocation template() {
        return this.template;
    }

    /** The textures of the model given the material's own texture. */
    public List<String> textures() {
        return this.textures;
    }

    /** The block tags its variants are in, beside the tag of the tool their family is broken with. */
    public List<String> tags() {
        return this.tags;
    }

    /** What makes the block of each variant. */
    public IVariantBlockMaker maker() {
        return this.maker;
    }

    /** Its variants, one for every material made into one, once the game registered them; none before. */
    public List<Variant> variants() {
        return Variants.of(this);
    }

    /** The items of its variants, in the order they were made. */
    public List<Item> items() {
        final List<Item> out = new ArrayList<>();
        for (final Variant variant : variants()) {
            out.add(variant.item());
        }
        return out;
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    /** Builds a kind; {@link #declare()} ends it. */
    public static final class Builder {

        private final ResourceLocation id;
        private final VariantFamily family;
        private final TextKey name;
        private final List<String> textures = new ArrayList<>(List.of("all"));
        private final List<String> tags = new ArrayList<>();
        private ResourceLocation template = ResourceLocation.withDefaultNamespace("block/cube_all");
        private IVariantBlockMaker maker = IVariantBlockMaker.PLAIN;
        private ContentTab.Section section;

        Builder(final ResourceLocation id, final VariantFamily family, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.family = Objects.requireNonNull(family, "family");
            this.name = Objects.requireNonNull(name, "name");
        }

        /**
         * Draws its variants with the model {@code template}, the material's own texture in each of {@code textures};
         * without it a variant is a cube drawn all over with its material ({@code minecraft:block/cube_all}, "all").
         */
        public Builder model(final String template, final String... textures) {
            this.template = ResourceLocation.parse(template);
            this.textures.clear();
            this.textures.addAll(List.of(textures));
            return this;
        }

        /** Puts its variants in the block tags {@code tags} too. */
        public Builder tagged(final String... tags) {
            this.tags.addAll(List.of(tags));
            return this;
        }

        /** Makes each variant's block with {@code maker} rather than as a plain block. */
        public Builder made(final IVariantBlockMaker maker) {
            this.maker = Objects.requireNonNull(maker, "maker");
            return this;
        }

        /** Shows its variants in {@code section} of a creative tab. */
        public Builder tab(final ContentTab.Section section) {
            this.section = Objects.requireNonNull(section, "section");
            return this;
        }

        /** Declares the kind, from the mod's constructor, before the game registers its things. */
        public VariantKind declare() {
            final VariantKind kind = new VariantKind(this);
            Variants.add(kind);
            if (this.section != null) {
                this.section.withEach(kind::items);
            }
            return kind;
        }
    }
}
