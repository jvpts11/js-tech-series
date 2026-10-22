/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import dev.jstech.core.gui.screen.CommonProps;
import dev.jstech.core.gui.screen.ContainerProps;
import dev.jstech.core.gui.screen.ElementType;
import dev.jstech.core.gui.screen.ElementTypes;
import dev.jstech.core.gui.screen.ScreenSchema;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * Writes the JSON schema of a screen's file to {@code assets/<mod>/jstech/schemas/screen.json}, from the codecs of
 * every kind of component declared in the run ({@link ScreenSchema}): a screen's file names it in its
 * {@code "$schema"}, or an editor's settings map the screens' folder to it, and the editor completes the file as it is
 * typed. The Core writes it with its own kinds; a mod that declares kinds of its own adds this to its data run.
 */
public final class ScreenSchemaProvider implements DataProvider {

    /** Where in a mod's assets the schema is written. */
    public static final String FILE = "jstech/schemas/screen.json";

    private final PackOutput output;
    private final String modid;

    public ScreenSchemaProvider(final PackOutput output, final String modid) {
        this.output = output;
        this.modid = modid;
    }

    @Override
    public String getName() {
        return modid + ":screen_schema";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<ScreenSchema.Kind> kinds = new ArrayList<>();
        final List<ElementType<?>> declared = ElementTypes.all();
        declared.sort(Comparator.comparing(type -> type.id().toString()));
        for (final ElementType<?> type : declared) {
            kinds.add(new ScreenSchema.Kind(nameOf(type.id()), keysOf(type.props()), type.holds()));
        }
        return DataProvider.saveStable(cache, ScreenSchema.of(kinds, keysOf(CommonProps.MAP_CODEC),
                keysOf(ContainerProps.MAP_CODEC)), ContentFiles.resourcePackFile(output,
                ResourceLocation.fromNamespaceAndPath(modid, FILE)));
    }

    /* A kind's {@code type} as a file writes it: a kind of the Core's by its name alone. */
    private static String nameOf(final ResourceLocation id) {
        return id.getNamespace().equals(JsCore.MODID) ? id.getPath() : id.toString();
    }

    /* The names of the fields a codec reads, in the order it declares them. */
    private static List<String> keysOf(final MapCodec<?> codec) {
        return codec.keys(JsonOps.INSTANCE).map(JsonElement::getAsString).toList();
    }
}
