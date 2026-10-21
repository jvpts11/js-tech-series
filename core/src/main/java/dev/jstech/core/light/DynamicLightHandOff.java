/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.light;

import dev.jstech.core.JsCore;
import dev.jstech.core.resource.IRuntimeFiles;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.stepaside.StepAside;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * What the Core hands LambDynamicLights while its own dynamic light steps aside for it: the light every item gives
 * held, as the mods' files of {@code jscore:held_lights} say, written as the files LambDynamicLights reads
 * ({@link HeldLightFiles}), so a flashlight of the series lights the way with either. Made as the player's game loads
 * its resources, from the files in the mods' jars; nothing while the Core draws dynamic light itself. A block that
 * gives light needs nothing: LambDynamicLights lights a held block by its own light.
 */
@TextHolder
public final class DynamicLightHandOff implements IRuntimeFiles {

    public static final TextKey TITLE = TextKey.of("jscore.pack.dynamic_light_handoff",
            "J's Core: the items' light for LambDynamicLights");

    private static final String FOLDER = "assets/" + JsCore.MODID + "/dynamiclights/item/";

    private DynamicLightHandOff() {
    }

    /** Declares the pack, from the Core's client constructor. */
    public static void declare() {
        RuntimePacks.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "dynamic_light_handoff"),
                PackType.CLIENT_RESOURCES, TITLE, new DynamicLightHandOff());
    }

    /** The light each item is handed with, by id, as the mods' files say, whether or not it is handed now. */
    public static Map<String, Integer> handed() {
        return HeldLightFiles.levels(heldLightFiles());
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        if (!StepAside.DYNAMIC_LIGHT.steps()) {
            return out;
        }
        handed().forEach((item, level) -> {
            final byte[] bytes = HeldLightFiles.lambFile(item, level).getBytes(StandardCharsets.UTF_8);
            out.put(FOLDER + HeldLightFiles.fileName(item) + ".json", () -> bytes);
        });
        return out;
    }

    /* The text of the data map's file in every installed mod's jar. */
    private static List<String> heldLightFiles() {
        final List<String> out = new ArrayList<>();
        for (final IModFileInfo mod : ModList.get().getModFiles()) {
            final Path file = mod.getFile().findResource("data", JsCore.MODID, "data_maps", "item",
                    "held_lights.json");
            if (Files.isRegularFile(file)) {
                try {
                    out.add(Files.readString(file, StandardCharsets.UTF_8));
                } catch (final IOException unreadable) {
                    // A file that cannot be read hands nothing; the rest still do.
                }
            }
        }
        return out;
    }
}
