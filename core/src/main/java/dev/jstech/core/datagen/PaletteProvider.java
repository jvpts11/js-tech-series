/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonObject;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolders;
import dev.jstech.core.palette.PaletteRoles;
import dev.jstech.core.palette.Palettes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes every palette a mod declares to {@code assets/<mod>/palettes/}, one file each, from the colours the code
 * declares: the file a resource pack puts its own in place of. A mod that declares no palette writes nothing.
 */
public final class PaletteProvider implements DataProvider {

    private final PackOutput output;
    private final String modid;

    public PaletteProvider(final PackOutput output, final String modid) {
        this.output = output;
        this.modid = modid;
    }

    @Override
    public String getName() {
        return this.modid + ":palettes";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        PaletteHolders.load(this.modid);
        final List<CompletableFuture<?>> written = new ArrayList<>();
        final List<Palette<?>> palettes = new ArrayList<>(Palettes.of(this.modid));
        palettes.sort(Comparator.comparing(palette -> palette.id()));
        for (final Palette<?> palette : palettes) {
            final JsonObject file = new JsonObject();
            PaletteRoles.read(palette.declared()).forEach((role, colour) ->
                    file.addProperty(role, PaletteRoles.format(colour)));
            written.add(DataProvider.saveStable(cache, file, this.output
                    .getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                    .resolve(palette.file().getNamespace()).resolve(palette.file().getPath())));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }
}
