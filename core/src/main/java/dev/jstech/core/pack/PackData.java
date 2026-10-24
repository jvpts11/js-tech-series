/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.jstech.core.resource.IRuntimeFiles;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The data a content pack's blocks need on the server, made as it loads its data under every data pack, so a pack
 * that brings its own replaces any of it: each block with an item drops itself, as long as a blast leaves it, and is
 * in the tags of the tool that breaks it fast and of the tier it needs; a machine's terminal breaks with a pickaxe.
 */
final class PackData implements IRuntimeFiles {

    private static final String PICKAXE = "pickaxe";
    private static final String WOOD = "wood";

    private final List<ContentPacks.Declared> declared;

    PackData(final List<ContentPacks.Declared> declared) {
        this.declared = declared;
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        final Map<String, Set<String>> tags = new LinkedHashMap<>();
        for (final ContentPacks.Declared each : this.declared) {
            switch (each.declaration()) {
                case PackDeclaration.Block block -> {
                    if (block.item()) {
                        out.put(lootOf(block.id()), json(selfDrop(block.id())));
                    }
                    if (!block.tool().isEmpty()) {
                        tag(tags, "minecraft:mineable/" + block.tool(), block.id());
                    }
                    if (!block.needs().isEmpty() && !block.needs().equals(WOOD)) {
                        tag(tags, "minecraft:needs_" + block.needs() + "_tool", block.id());
                    }
                }
                case PackDeclaration.Machine machine -> {
                    out.put(lootOf(machine.id()), json(selfDrop(machine.id())));
                    tag(tags, "minecraft:mineable/" + PICKAXE, machine.id());
                }
                default -> {
                    // The other declarations have no data of their own: a material's things are the Core's.
                }
            }
        }
        tags.forEach((tag, values) -> {
            final int colon = tag.indexOf(':');
            final JsonArray listed = new JsonArray();
            values.forEach(listed::add);
            final JsonObject file = new JsonObject();
            file.addProperty("replace", false);
            file.add("values", listed);
            out.put("data/" + tag.substring(0, colon) + "/tags/block/" + tag.substring(colon + 1) + ".json",
                    json(file));
        });
        return out;
    }

    private static void tag(final Map<String, Set<String>> tags, final String tag, final String id) {
        tags.computeIfAbsent(tag, name -> new LinkedHashSet<>()).add(id);
    }

    private static String lootOf(final String id) {
        final int colon = id.indexOf(':');
        return "data/" + id.substring(0, colon) + "/loot_table/blocks/" + id.substring(colon + 1) + ".json";
    }

    /* A block's own drop, once, as long as the blast that broke it leaves it. */
    private static JsonObject selfDrop(final String id) {
        final JsonObject entry = new JsonObject();
        entry.addProperty("type", "minecraft:item");
        entry.addProperty("name", id);
        final JsonArray entries = new JsonArray();
        entries.add(entry);
        final JsonObject survives = new JsonObject();
        survives.addProperty("condition", "minecraft:survives_explosion");
        final JsonArray conditions = new JsonArray();
        conditions.add(survives);
        final JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.add("entries", entries);
        pool.add("conditions", conditions);
        final JsonArray pools = new JsonArray();
        pools.add(pool);
        final JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        table.add("pools", pools);
        return table;
    }

    private static Supplier<byte[]> json(final JsonObject json) {
        final byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        return () -> bytes;
    }
}
