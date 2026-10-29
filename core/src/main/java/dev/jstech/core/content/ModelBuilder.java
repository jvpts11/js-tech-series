/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelReaders;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about one model drawn with the Core's models, said once: its file, of any format the Core reads, the
 * textures it is painted with, the animation files it plays from, and how it is drawn. The data run writes its
 * definition, and checks that every file it names is there. Every file is a path under the mod's {@code assets}
 * folder.
 *
 * <pre>{@code
 * public static final DeclaredModel FAN = CONTENT.model("fan")
 *         .file("jsmodels/fan.bbmodel")
 *         .texture("textures/block/fan.png")
 *         .animations("animations/fan.animation.json")
 *         .glow("lamp")
 *         .variant(on -> on.when("powered", "true").show("lamp"))
 *         .register();
 * }</pre>
 */
public final class ModelBuilder {

    private final ModContent content;
    private final String path;
    private @Nullable String file;
    private final List<String> textures = new ArrayList<>();
    private final List<String> animations = new ArrayList<>();
    private final Set<String> glow = new LinkedHashSet<>();
    private final List<ModelDefinition.Variant> variants = new ArrayList<>();
    private boolean translucent;
    private boolean cracks = true;
    private int glowLight;
    private int glowColour = ModelDefinition.WHITE;
    private double scale = 1.0;
    private int triangles;
    private int farFrom;
    private int farTriangles;
    private @Nullable ResourceLocation formedFrom;

    ModelBuilder(final ModContent content, final String path) {
        this.content = content;
        this.path = path;
    }

    /** The model file, of any format the Core reads. */
    public ModelBuilder file(final String modelFile) {
        this.file = modelFile;
        return this;
    }

    /** A texture it is painted with; the first is for every face that names none. */
    public ModelBuilder texture(final String textureFile) {
        this.textures.add(textureFile);
        return this;
    }

    /** Animation files it plays from, besides the animations its own file carries. */
    public ModelBuilder animations(final String... animationFiles) {
        this.animations.addAll(List.of(animationFiles));
        return this;
    }

    /** It is see-through, its parts drawn back to front. */
    public ModelBuilder translucent() {
        this.translucent = true;
        return this;
    }

    /** Bones that glow in the dark, besides those whose names end in {@code _glow}. */
    public ModelBuilder glow(final String... bones) {
        this.glow.addAll(List.of(bones));
        return this;
    }

    /** A block drawn with it shows no cracks as it is broken, as one whose model is not the block's shape. */
    public ModelBuilder noCracks() {
        this.cracks = false;
        return this;
    }

    /**
     * Its glowing parts glow in {@code colour} ({@code 0xRRGGBB}), their halo and their light, and cast light of
     * {@code level} (1 to 15) on the world around them, as a lamp's bulb does; 0 casts none.
     */
    public ModelBuilder glowLight(final int level, final int colour) {
        this.glowLight = level;
        this.glowColour = colour;
        return this;
    }

    /**
     * It is drawn {@code times} bigger than its file says, its file's units taken as they are, a metre a block: a
     * part drawn by a CAD program at its real size becomes as big in the game.
     */
    public ModelBuilder scale(final double times) {
        this.scale = times;
        return this;
    }

    /** It is drawn with {@code most} triangles at most, its meshes brought down to them as they are read. */
    public ModelBuilder triangles(final int most) {
        this.triangles = most;
        return this;
    }

    /** From {@code blocks} away it is drawn with {@code most} triangles at most. */
    public ModelBuilder far(final int blocks, final int most) {
        this.farFrom = blocks;
        this.farTriangles = most;
        return this;
    }

    /**
     * It is the formed model of the structure of the pattern {@code pattern}, drawn by the terminal as the structure
     * stands, its origin the middle of the terminal's floor: each block of the structure that merges into it takes
     * the shape of its own part of it, worked out by the data run, which stops when a block of the pattern has no
     * face of the model or a face lies more than a block outside the pattern ({@code FormedShapes}).
     */
    public ModelBuilder formedFrom(final ResourceLocation pattern) {
        this.formedFrom = pattern;
        return this;
    }

    /** A variant chosen by the state of the block it is drawn for, as {@code variant} says. */
    public ModelBuilder variant(final Consumer<VariantBuilder> variant) {
        final VariantBuilder builder = new VariantBuilder();
        variant.accept(builder);
        this.variants.add(builder.build(this.content.modid()));
        return this;
    }

    /**
     * Declares the model.
     *
     * @throws DeclarationError when it names no file, or one of no format the Core reads
     */
    public DeclaredModel register() {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(this.content.modid(), this.path);
        if (this.file == null) {
            throw DeclarationError.of(this.content.modid(), "the model " + id, "names no model file",
                    "name it with file(...) before register()");
        }
        if (!ModelReaders.readsModel(this.file)) {
            throw DeclarationError.of(this.content.modid(), "the model " + id, "names " + this.file
                    + ", of no format the Core reads", "use a file ending in one of " + ModelReaders.MODEL_ENDINGS);
        }
        final String modid = this.content.modid();
        final ModelDefinition.Detail detail;
        try {
            detail = new ModelDefinition.Detail(this.scale, this.triangles, this.farFrom, this.farTriangles);
        } catch (final IllegalArgumentException wrong) {
            throw DeclarationError.of(modid, "the model " + id, "has a detail that cannot be: " + wrong.getMessage(),
                    "give scale(...) a size above 0, and far(...) a distance and the triangles there");
        }
        final DeclaredModel model = new DeclaredModel(id, new ModelDefinition(full(modid, this.file),
                this.textures.stream().map(each -> full(modid, each)).toList(),
                this.animations.stream().map(each -> full(modid, each)).toList(), this.translucent, this.glow,
                this.cracks, this.variants, this.glowLight, this.glowColour, detail));
        this.content.declare(model);
        if (this.formedFrom != null) {
            FormedShapes.declare(id, this.formedFrom);
        }
        return model;
    }

    /* A path under the mod's assets as the definition names it, with the mod before it; one naming a mod stays. */
    private static String full(final String modid, final String file) {
        return file.indexOf(':') >= 0 ? file : modid + ":" + file;
    }

    /** One variant of a model, chosen by the state of the block it is drawn for. */
    public static final class VariantBuilder {

        private final Map<String, String> when = new LinkedHashMap<>();
        private final Set<String> hidden = new LinkedHashSet<>();
        private final Set<String> shown = new LinkedHashSet<>();
        private String model = "";
        private String texture = "";

        VariantBuilder() {
        }

        /** It is chosen when the block's property {@code property} has the value {@code value}, as written. */
        public VariantBuilder when(final String property, final String value) {
            this.when.put(property, value);
            return this;
        }

        /** It is drawn from another model file. */
        public VariantBuilder model(final String modelFile) {
            this.model = modelFile;
            return this;
        }

        /** It is painted with another texture. */
        public VariantBuilder texture(final String textureFile) {
            this.texture = textureFile;
            return this;
        }

        /** It hides these bones. */
        public VariantBuilder hide(final String... bones) {
            this.hidden.addAll(List.of(bones));
            return this;
        }

        /** It shows these bones, which the model hides. */
        public VariantBuilder show(final String... bones) {
            this.shown.addAll(List.of(bones));
            return this;
        }

        ModelDefinition.Variant build(final String modid) {
            return new ModelDefinition.Variant(this.when, this.model.isEmpty() ? "" : full(modid, this.model),
                    this.texture.isEmpty() ? "" : full(modid, this.texture), this.hidden, this.shown);
        }
    }
}
