/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.jstech.core.motion.DeclaredMotion;
import dev.jstech.core.motion.MotionProfiles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

/**
 * Writes every motion profile a mod declares to {@code assets/<mod>/motions/}, one file each, from the timings the
 * code declares: the file a resource pack puts its own in place of. A mod that declares none writes nothing.
 */
public final class MotionProfileProvider implements DataProvider {

    private final PackOutput output;
    private final String modid;

    public MotionProfileProvider(final PackOutput output, final String modid) {
        this.output = output;
        this.modid = modid;
    }

    @Override
    public String getName() {
        return modid + ":motions";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        for (final DeclaredMotion motion : MotionProfiles.of(modid)) {
            final JsonElement file = JsonParser.parseString(new String(motion.declared().json(),
                    StandardCharsets.UTF_8));
            written.add(DataProvider.saveStable(cache, file, output
                    .getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                    .resolve(motion.file().getNamespace()).resolve(motion.file().getPath())));
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }
}
