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
import dev.jstech.core.light.LightKind;
import dev.jstech.core.light.LightKinds;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * Writes every kind of light a mod declares ({@link LightKinds#declare}) to {@code data/<mod>/jstech/lights/}, one
 * file each: the file a data pack writes over. A mod that declares none writes nothing.
 *
 * <pre>{@code
 * generator.addProvider(event.includeServer(), new LightKindProvider(output, MODID));
 * }</pre>
 */
public final class LightKindProvider implements DataProvider {

    private final PackOutput output;
    private final String modid;

    public LightKindProvider(final PackOutput output, final String modid) {
        this.output = output;
        this.modid = modid;
    }

    @Override
    public String getName() {
        return this.modid + ":lights";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, LightKind> kind : LightKinds.declared().entrySet()) {
            if (!kind.getKey().getNamespace().equals(this.modid)) {
                continue;
            }
            final JsonElement file = LightKind.CODEC.encodeStart(JsonOps.INSTANCE, kind.getValue()).getOrThrow();
            written.add(DataProvider.saveStable(cache, file, this.output.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve(kind.getKey().getNamespace()).resolve("jstech").resolve("lights")
                    .resolve(kind.getKey().getPath() + ".json")));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }
}
