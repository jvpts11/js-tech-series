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
import dev.jstech.core.worldgen.vein.DepositType;
import dev.jstech.core.worldgen.vein.Deposits;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * Writes every kind of vein a mod declares ({@link Deposits#deposit}) to {@code data/<mod>/jstech/deposits/}, one file
 * each: the file a data pack writes over. A mod that declares none writes nothing.
 */
public final class DepositProvider implements DataProvider {

    private final PackOutput output;
    private final String modid;

    public DepositProvider(final PackOutput output, final String modid) {
        this.output = output;
        this.modid = modid;
    }

    /** Whether the mod {@code modid} declares any kind of vein. */
    public static boolean declares(final String modid) {
        return Deposits.declared().keySet().stream().anyMatch(id -> id.getNamespace().equals(modid));
    }

    @Override
    public String getName() {
        return this.modid + ":deposits";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, DepositType> kind : Deposits.declared().entrySet()) {
            if (!kind.getKey().getNamespace().equals(this.modid)) {
                continue;
            }
            final JsonElement file = DepositType.CODEC.encodeStart(JsonOps.INSTANCE, kind.getValue()).getOrThrow();
            written.add(DataProvider.saveStable(cache, file, this.output.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve(kind.getKey().getNamespace()).resolve("jstech").resolve("deposits")
                    .resolve(kind.getKey().getPath() + ".json")));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }
}
