/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * A rock ores sit in: stone, deepslate, granite, a mod's limestone. A mineral has an ore in every rock its veins can
 * cross, drawn from the rock's own texture with the mineral laid over it, and named after the rock ("Deepslate %s
 * Ore"). Declared once, with {@link HostRocks}.
 */
public final class HostRock {

    private final ResourceLocation id;
    private final Supplier<? extends Block> rock;
    private final ResourceLocation texture;
    /** What an ore in it is called; none for a rock of another mod found by its tags, named after its own name. */
    private final @Nullable TextKey oreName;
    private final String orePrefix;
    private final boolean found;

    private HostRock(final Builder builder) {
        this.id = builder.id;
        this.rock = builder.rock;
        this.texture = builder.texture;
        this.oreName = builder.oreName;
        this.orePrefix = builder.orePrefix;
        this.found = builder.found;
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** The rock's block. */
    public Block rock() {
        return this.rock.get();
    }

    /** The rock's texture, which its ores are drawn on: {@code minecraft:block/granite}. */
    public ResourceLocation texture() {
        return this.texture;
    }

    /**
     * What {@code material}'s ore in it is called: "Deepslate Tin Ore" by the rock's own sentence, or, for a rock of
     * another mod found by its tags, "Tin Ore in Limestone", the rock in its own mod's words in the player's language.
     */
    public Text oreName(final Text material) {
        if (this.oreName != null) {
            return this.oreName.with(material);
        }
        return HostRocks.ORE_IN.with(material, TextKey.of(rock().getDescriptionId(), englishOf(this.id)));
    }

    /**
     * Whether it is a rock of another mod the Core found by the game's common tags, rather than one a mod declared:
     * its ores are registered once every mod has registered its blocks.
     */
    public boolean found() {
        return this.found;
    }

    /**
     * The id of {@code material}'s ore in this rock: {@code jscore:tin_ore} in stone, {@code jscore:deepslate_tin_ore}
     * in deepslate, as the game names its own.
     */
    public ResourceLocation oreId(final Material material) {
        return ResourceLocation.fromNamespaceAndPath(material.id().getNamespace(),
                this.orePrefix + material.id().getPath() + "_ore");
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    /* The words of an id read as a name, for what is written in English: limestone_bricks as Limestone Bricks. */
    private static String englishOf(final ResourceLocation id) {
        final StringBuilder out = new StringBuilder();
        for (final String word : id.getPath().split("_")) {
            if (!word.isEmpty()) {
                if (!out.isEmpty()) {
                    out.append(' ');
                }
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
        }
        return out.toString();
    }

    /** How a host rock is declared. */
    public static final class Builder {

        private final ResourceLocation id;
        private final Supplier<? extends Block> rock;
        private final @Nullable TextKey oreName;
        private ResourceLocation texture;
        private String orePrefix;
        private boolean found;

        Builder(final ResourceLocation id, final Supplier<? extends Block> rock, final TextKey oreName) {
            this.id = Objects.requireNonNull(id, "id");
            this.rock = Objects.requireNonNull(rock, "rock");
            this.oreName = Objects.requireNonNull(oreName, "oreName");
            this.texture = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath());
            this.orePrefix = id.getPath() + "_";
        }

        /* A rock of another mod found by the common tags: named after itself, its ores' ids after its own id. */
        Builder(final ResourceLocation block, final Supplier<? extends Block> rock) {
            this.id = Objects.requireNonNull(block, "block");
            this.rock = Objects.requireNonNull(rock, "rock");
            this.oreName = null;
            this.texture = ResourceLocation.fromNamespaceAndPath(block.getNamespace(), "block/" + block.getPath());
            this.orePrefix = block.getNamespace() + "_" + block.getPath() + "_";
            this.found = true;
        }

        /** The texture its ores are drawn on, when it is not {@code <namespace>:block/<path>}. */
        public Builder texture(final ResourceLocation drawnOn) {
            this.texture = Objects.requireNonNull(drawnOn, "drawnOn");
            return this;
        }

        /** What its ores' ids start with, when it is not {@code <path>_}: nothing for stone, {@code nether_}. */
        public Builder orePrefix(final String prefix) {
            this.orePrefix = Objects.requireNonNull(prefix, "prefix");
            return this;
        }

        /**
         * Declares the rock.
         *
         * @throws DeclarationError when a rock of that id is declared already
         */
        public HostRock declare() {
            return HostRocks.declare(new HostRock(this));
        }
    }
}
