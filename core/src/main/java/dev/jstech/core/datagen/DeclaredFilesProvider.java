/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.worldgen.reservoir.ReservoirType;
import dev.jstech.core.worldgen.reservoir.Reservoirs;
import dev.jstech.core.worldgen.vein.DepositType;
import dev.jstech.core.worldgen.vein.Deposits;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

/**
 * Writes what a mod declared in code, of one kind of data file, to the files a data pack writes over: one file each,
 * under {@code data/<mod>/<folder>/}, read with the kind's own codec. A mod that declares none writes nothing.
 *
 * <pre>{@code
 * generator.addProvider(event.includeServer(), DeclaredFilesProvider.deposits(output, MODID));
 * generator.addProvider(event.includeServer(), DeclaredFilesProvider.reservoirs(output, MODID));
 * }</pre>
 *
 * @param <T> what each file holds
 */
public final class DeclaredFilesProvider<T> implements DataProvider {

    private final PackOutput output;
    private final String modid;
    private final String kind;
    private final String folder;
    private final Codec<T> codec;
    private final Supplier<Map<ResourceLocation, T>> declared;

    public DeclaredFilesProvider(final PackOutput output, final String modid, final String kind, final String folder,
                                 final Codec<T> codec, final Supplier<Map<ResourceLocation, T>> declared) {
        this.output = output;
        this.modid = modid;
        this.kind = kind;
        this.folder = folder;
        this.codec = codec;
        this.declared = declared;
    }

    /** The kinds of vein {@code modid} declares, to {@code data/<mod>/jstech/deposits/}. */
    public static DeclaredFilesProvider<DepositType> deposits(final PackOutput output, final String modid) {
        return new DeclaredFilesProvider<>(output, modid, "deposits", "jstech/deposits", DepositType.CODEC,
                Deposits::declared);
    }

    /** The kinds of reservoir {@code modid} declares, to {@code data/<mod>/jstech/reservoirs/}. */
    public static DeclaredFilesProvider<ReservoirType> reservoirs(final PackOutput output, final String modid) {
        return new DeclaredFilesProvider<>(output, modid, "reservoirs", "jstech/reservoirs", ReservoirType.CODEC,
                Reservoirs::declared);
    }

    /** Whether {@code modid} declared anything this writes. */
    public boolean declares() {
        return this.declared.get().keySet().stream().anyMatch(id -> id.getNamespace().equals(this.modid));
    }

    @Override
    public String getName() {
        return this.modid + ":" + this.kind;
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        final Path data = this.output.getOutputFolder(PackOutput.Target.DATA_PACK);
        for (final Map.Entry<ResourceLocation, T> one : this.declared.get().entrySet()) {
            if (!one.getKey().getNamespace().equals(this.modid)) {
                continue;
            }
            final JsonElement file = this.codec.encodeStart(JsonOps.INSTANCE, one.getValue()).getOrThrow();
            written.add(DataProvider.saveStable(cache, file, data.resolve(one.getKey().getNamespace())
                    .resolve(this.folder).resolve(one.getKey().getPath() + ".json")));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }
}
