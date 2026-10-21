/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jstech.core.JsCore;
import dev.jstech.core.resource.IRuntimeFiles;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/**
 * The data the variants need, made as the server loads its data: each variant drops itself (but when an explosion
 * destroys it), and is in the tag of the tool its family is broken with and in its kind's tags. A data pack can
 * replace any of these files.
 */
@TextHolder
public final class VariantData implements IRuntimeFiles {

    public static final TextKey TITLE = TextKey.of("jscore.pack.variant_data", "J's Core: the variants' data");

    private VariantData() {
    }

    /** Declares the pack, from the Core's constructor. */
    public static void declare() {
        RuntimePacks.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "variant_data"),
                PackType.SERVER_DATA, TITLE, new VariantData());
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        final Map<String, Set<String>> tags = new LinkedHashMap<>();
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        for (final Variant variant : Variants.registered()) {
            final String id = variant.id().toString();
            tags.computeIfAbsent(variant.kind().family().toolTag(), unused -> new LinkedHashSet<>()).add(id);
            for (final String tag : variant.kind().tags()) {
                tags.computeIfAbsent(tag, unused -> new LinkedHashSet<>()).add(id);
            }
            out.put("data/" + variant.id().getNamespace() + "/loot_table/blocks/" + variant.id().getPath() + ".json",
                    json(selfLoot(variant.id())));
        }
        tags.forEach((tag, values) -> {
            final ResourceLocation at = ResourceLocation.parse(tag);
            final JsonArray entries = new JsonArray();
            values.forEach(entries::add);
            final JsonObject file = new JsonObject();
            file.addProperty("replace", false);
            file.add("values", entries);
            out.put("data/" + at.getNamespace() + "/tags/block/" + at.getPath() + ".json", json(file));
        });
        return out;
    }

    private static JsonObject selfLoot(final ResourceLocation id) {
        final JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", id.toString());
        final JsonArray entries = new JsonArray();
        entries.add(entry);
        final JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        final JsonArray conditions = new JsonArray();
        conditions.add(survives);
        final JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);
        pool.addProperty("bonus_rolls", 0.0);
        pool.add("entries", entries);
        pool.add("conditions", conditions);
        final JsonArray pools = new JsonArray();
        pools.add(pool);
        final JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        table.add("pools", pools);
        table.addProperty("random_sequence", id.getNamespace() + ":blocks/" + id.getPath());
        return table;
    }

    private static Supplier<byte[]> json(final JsonElement element) {
        final byte[] bytes = element.toString().getBytes(StandardCharsets.UTF_8);
        return () -> bytes;
    }
}
