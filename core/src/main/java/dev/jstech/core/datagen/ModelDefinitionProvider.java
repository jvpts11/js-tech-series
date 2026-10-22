/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelFormatException;
import dev.jstech.core.model.ModelReaders;
import dev.jstech.core.model.ModelShaping;
import dev.jstech.core.util.DeclarationError;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the definition of every model a mod declares to {@code assets/<mod>/jsmodels/<path>.model.json}, once it has
 * read the model: every file the declaration names has to be there, the model has to read, and every bone it names
 * (to glow, to hide, to show) has to be one of the model's; and a file of more triangles than a model may have
 * without saying how many it is drawn with has to say it. A declaration that has drifted from its files stops the
 * generation instead of drawing nothing, or too much, in the game.
 */
public final class ModelDefinitionProvider implements DataProvider {

    private final PackOutput output;
    private final ModContent content;
    private final ExistingFileHelper files;

    public ModelDefinitionProvider(final PackOutput output, final ModContent content, final ExistingFileHelper files) {
        this.output = output;
        this.content = content;
        this.files = files;
    }

    @Override
    public String getName() {
        return this.content.modid() + ":models";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        for (final DeclaredModel model : this.content.declaredModels()) {
            check(model);
            written.add(DataProvider.saveStable(cache, model.definition().toJson(),
                    ContentFiles.resourcePackFile(this.output, DeclaredModel.definitionFile(model.id()))));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }

    private void check(final DeclaredModel model) {
        final ModelDefinition definition = model.definition();
        final List<String> named = new ArrayList<>(definition.textures());
        named.add(definition.model());
        named.addAll(definition.animations());
        for (final ModelDefinition.Variant variant : definition.variants()) {
            if (!variant.model().isEmpty()) {
                named.add(variant.model());
            }
            if (!variant.texture().isEmpty()) {
                named.add(variant.texture());
            }
        }
        for (final String file : named) {
            if (!this.files.exists(ResourceLocation.parse(file), PackType.CLIENT_RESOURCES)) {
                throw DeclarationError.of(model.id().getNamespace(), "the model " + model.id(), "names " + file
                        + ", which is not there", "put the file there, or name the one that is");
            }
        }
        final Set<String> bones = new HashSet<>();
        final List<ModelData> drawn = read(model, definition.model());
        for (final ModelData data : drawn) {
            data.bones().forEach(bone -> bones.add(bone.name()));
        }
        budget(model, definition.model(), drawn.getFirst());
        for (final ModelDefinition.Variant variant : definition.variants()) {
            if (!variant.model().isEmpty()) {
                budget(model, variant.model(), read(model, variant.model()).getFirst());
            }
        }
        final Set<String> wanted = new HashSet<>(definition.glow());
        definition.variants().forEach(variant -> {
            wanted.addAll(variant.hidden());
            wanted.addAll(variant.shown());
        });
        wanted.removeAll(bones);
        if (!wanted.isEmpty()) {
            throw DeclarationError.of(model.id().getNamespace(), "the model " + model.id(), "names the bones "
                    + wanted + ", which its file " + definition.model() + " has none of", "name bones it has: "
                    + bones);
        }
    }

    /* A model's file within the triangles it may be drawn with, or the generation stops saying how it is not. */
    private static void budget(final DeclaredModel model, final String file, final ModelData data) {
        ModelShaping.overBudget(data, model.definition().detail()).ifPresent(problem -> {
            throw DeclarationError.of(model.id().getNamespace(), "the model " + model.id(), "in " + file + " "
                    + problem, "say on its declaration how many triangles it is drawn with, triangles(...) and "
                    + "far(...), or make it of fewer");
        });
    }

    private List<ModelData> read(final DeclaredModel model, final String file) {
        final ResourceLocation location = ResourceLocation.parse(file);
        final String folder = location.getPath().contains("/")
                ? location.getPath().substring(0, location.getPath().lastIndexOf('/') + 1) : "";
        try {
            return ModelReaders.read(location.getPath(), bytes(location), beside -> {
                final ResourceLocation besideFile = ResourceLocation.fromNamespaceAndPath(location.getNamespace(),
                        folder + beside);
                return this.files.exists(besideFile, PackType.CLIENT_RESOURCES) ? bytes(besideFile) : null;
            });
        } catch (final ModelFormatException broken) {
            throw DeclarationError.of(model.id().getNamespace(), "the model " + model.id(), "has a file that does"
                    + " not read: " + broken.getMessage(), "save it again from the program that made it");
        }
    }

    private byte[] bytes(final ResourceLocation file) {
        try (InputStream in = this.files.getResource(file, PackType.CLIENT_RESOURCES).open()) {
            return in.readAllBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException("the model file " + file + " could not be read", e);
        }
    }
}
