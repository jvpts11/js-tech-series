/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.model.ModelDefinition;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/**
 * A model a mod declared, drawn with the Core's models: its name, by which renderers ask for it, and how it is drawn.
 * The data run writes its definition to {@code assets/<mod>/jsmodels/<path>.model.json}.
 *
 * @param id         its name
 * @param definition how it is drawn
 */
public record DeclaredModel(ResourceLocation id, ModelDefinition definition) {

    /** The folder of {@code assets/<mod>/} the definitions and the model files of the Core's models are in. */
    public static final String FOLDER = "jsmodels";
    /** The ending of a model's definition file. */
    public static final String DEFINITION_ENDING = ".model.json";

    public DeclaredModel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(definition, "definition");
    }

    /** The definition file of the model {@code id}. */
    public static ResourceLocation definitionFile(final ResourceLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), FOLDER + "/" + id.getPath()
                + DEFINITION_ENDING);
    }
}
