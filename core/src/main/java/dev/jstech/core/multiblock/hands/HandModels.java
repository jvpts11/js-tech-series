/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelReaders;
import dev.jstech.core.util.DeclarationError;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

/*
 * Reads the model a structure is worked by, from the mod that ships it: the server has no resource packs, so the
 * model's own file is what both it and the player's game measure the parts by.
 */
final class HandModels {

    private HandModels() {
    }

    /* The model of {@code model} as its mod ships it. */
    static ModelData read(final DeclaredModel model) {
        final ResourceLocation file = ResourceLocation.parse(model.definition().model());
        final String folder = file.getPath().contains("/")
                ? file.getPath().substring(0, file.getPath().lastIndexOf('/') + 1) : "";
        final byte[] bytes = bytes(file).orElseThrow(() -> DeclarationError.of(model.id().getNamespace(),
                "the model " + model.id(), "has no file " + file + " in any mod", "ship the model's file under "
                        + "assets/" + file.getNamespace() + "/" + file.getPath()));
        return ModelReaders.read(file.getPath(), bytes, beside -> bytes(ResourceLocation.fromNamespaceAndPath(
                file.getNamespace(), folder + beside)).orElse(null)).getFirst();
    }

    /* The bytes of {@code file} under some mod's assets, or none. */
    private static Optional<byte[]> bytes(final ResourceLocation file) {
        for (final IModFileInfo mod : ModList.get().getModFiles()) {
            final Path path = mod.getFile().findResource("assets", file.getNamespace(), file.getPath());
            if (Files.isRegularFile(path)) {
                try {
                    return Optional.of(Files.readAllBytes(path));
                } catch (final IOException unreadable) {
                    throw new UncheckedIOException(unreadable);
                }
            }
        }
        return Optional.empty();
    }
}
