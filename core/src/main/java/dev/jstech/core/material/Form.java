/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * A form a material takes: an ingot, a dust, a plate, a block, an ore. A form says which kinds of material it makes
 * sense for, whether it is an item, a block or an ore, the common tag folder its items are filed under
 * ({@code c:dusts/iron}) and what its items are called ("%s Dust"). Declared once, with {@link Forms}.
 */
public final class Form {

    private final ResourceLocation id;
    private final Shape shape;
    private final String tagFolder;
    private final Set<MaterialKind> kinds;
    private final TextKey name;
    private final @Nullable ResourceLocation art;

    /** What a form's thing is in the game. */
    public enum Shape {
        /** An item. */
        ITEM,
        /** A block placed in the world, and its item. */
        BLOCK,
        /** An ore: a block in each rock its mineral's veins cross, each dropping the same raw item. */
        ORE
    }

    private Form(final Builder builder) {
        this.id = builder.id;
        this.shape = builder.shape;
        this.tagFolder = builder.tagFolder;
        this.kinds = Set.copyOf(builder.kinds);
        this.name = builder.name;
        this.art = builder.art;
    }

    public ResourceLocation id() {
        return this.id;
    }

    public Shape shape() {
        return this.shape;
    }

    /** The folder of the common tags its items are filed under: {@code dusts} for {@code c:dusts/iron}. */
    public String tagFolder() {
        return this.tagFolder;
    }

    /** The kinds of material it makes sense for. */
    public Set<MaterialKind> kinds() {
        return this.kinds;
    }

    /** What its things are called, with the material's name in it: "%s Dust". */
    public TextKey name() {
        return this.name;
    }

    /**
     * The texture its things are drawn from, recoloured in each material's colour, in its mod's own resources
     * ({@code mymod:item/pellet} for {@code assets/mymod/textures/item/pellet.png}); none for the Core's forms, which
     * the Core draws itself.
     */
    public Optional<ResourceLocation> art() {
        return Optional.ofNullable(this.art);
    }

    /** Whether a material of {@code kind} can take this form. */
    public boolean fits(final MaterialKind kind) {
        return this.kinds.contains(kind);
    }

    /** The common item tag {@code material}'s things of this form are filed under: {@code c:dusts/iron}. */
    public TagKey<Item> itemTag(final Material material) {
        return TagKey.create(Registries.ITEM, commonTag(material));
    }

    /** The common block tag, for a form that is a block: {@code c:storage_blocks/iron}. */
    public TagKey<Block> blockTag(final Material material) {
        return TagKey.create(Registries.BLOCK, commonTag(material));
    }

    /** The id the Core registers {@code material}'s thing of this form under: {@code jscore:iron_dust}. */
    public ResourceLocation thingId(final Material material) {
        return ResourceLocation.fromNamespaceAndPath(material.id().getNamespace(),
                material.id().getPath() + "_" + this.id.getPath());
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    private ResourceLocation commonTag(final Material material) {
        return ResourceLocation.fromNamespaceAndPath("c", this.tagFolder + "/" + material.id().getPath());
    }

    /** How a form is declared. */
    public static final class Builder {

        private final ResourceLocation id;
        private final Shape shape;
        private final String tagFolder;
        private final TextKey name;
        private final Set<MaterialKind> kinds = EnumSet.noneOf(MaterialKind.class);
        private @Nullable ResourceLocation art;

        Builder(final ResourceLocation id, final Shape shape, final String tagFolder, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.shape = Objects.requireNonNull(shape, "shape");
            this.tagFolder = Objects.requireNonNull(tagFolder, "tagFolder");
            this.name = Objects.requireNonNull(name, "name");
        }

        /** The kinds of material it makes sense for. */
        public Builder fits(final MaterialKind... fit) {
            this.kinds.addAll(Set.of(fit));
            return this;
        }

        /**
         * The texture its things are drawn from, in its mod's resources: a grey picture whose light and shade each
         * material's colour is laid over, as the Core's own forms are drawn.
         */
        public Builder art(final ResourceLocation texture) {
            this.art = Objects.requireNonNull(texture, "texture");
            return this;
        }

        /**
         * Declares the form.
         *
         * @throws DeclarationError when it fits no kind, or a form of that id is declared already
         */
        public Form declare() {
            if (this.kinds.isEmpty()) {
                throw DeclarationError.of(this.id.getNamespace(), "the form " + this.id, "fits no kind of material",
                        "say which kinds it makes sense for with fits(MaterialKind.METAL, ...)");
            }
            return Forms.declare(new Form(this));
        }
    }
}
