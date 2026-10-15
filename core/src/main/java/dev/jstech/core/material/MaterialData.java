/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/**
 * The data the things of the materials' uses need, made as the server loads its data: each in its common tag and the
 * tag of its form ({@code c:dusts/tin} in {@code c:dusts}), each block mineable with a pickaxe, each ore in the tag of
 * its rock, and each block's drops: a block of a material drops itself, an ore its raw form (more with Fortune), or
 * itself with Silk Touch. A data pack can replace any of these files.
 */
@TextHolder
public final class MaterialData implements IRuntimeFiles {

    public static final TextKey TITLE = TextKey.of("jscore.pack.material_data", "J's Core: the materials' data");

    private static final String PICKAXE = "minecraft:mineable/pickaxe";
    private static final String STONE_TOOL = "minecraft:needs_stone_tool";
    private static final String ONE_DROP = "c:ore_rates/singular";

    private MaterialData() {
    }

    /** Declares the pack, from the Core's constructor. */
    public static void declare() {
        RuntimePacks.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "material_data"),
                PackType.SERVER_DATA, TITLE, new MaterialData());
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        final Map<String, Set<String>> itemTags = new LinkedHashMap<>();
        final Map<String, Set<String>> blockTags = new LinkedHashMap<>();
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        for (final RegisteredThing thing : MaterialUses.registered()) {
            final Form form = thing.thing().form();
            final String id = thing.id().toString();
            final String folder = "c:" + form.tagFolder();
            final String own = folder + "/" + thing.thing().material().id().getPath();
            add(itemTags, own, id);
            add(itemTags, folder, "#" + own);
            if (!thing.isBlock()) {
                continue;
            }
            add(blockTags, own, id);
            add(blockTags, folder, "#" + own);
            add(blockTags, PICKAXE, id);
            add(blockTags, STONE_TOOL, id);
            final JsonObject loot;
            if (thing.rock() != null) {
                final String inGround = "c:ores_in_ground/" + thing.rock().id().getPath();
                add(itemTags, inGround, id);
                add(blockTags, inGround, id);
                add(itemTags, ONE_DROP, id);
                add(blockTags, ONE_DROP, id);
                loot = oreLoot(thing);
            } else {
                loot = selfLoot(thing);
            }
            out.put("data/" + thing.id().getNamespace() + "/loot_table/blocks/" + thing.id().getPath() + ".json",
                    json(loot));
        }
        itemTags.forEach((tag, values) -> out.put(tagPath("item", tag), json(tag(values))));
        blockTags.forEach((tag, values) -> out.put(tagPath("block", tag), json(tag(values))));
        return out;
    }

    private static void add(final Map<String, Set<String>> tags, final String tag, final String value) {
        tags.computeIfAbsent(tag, unused -> new LinkedHashSet<>()).add(value);
    }

    private static String tagPath(final String registry, final String tag) {
        final ResourceLocation id = ResourceLocation.parse(tag);
        return "data/" + id.getNamespace() + "/tags/" + registry + "/" + id.getPath() + ".json";
    }

    private static JsonObject tag(final Set<String> values) {
        // A form's tag gathers the materials' tags ("#c:dusts/tin"), each of which this same pack writes.
        final JsonArray entries = new JsonArray();
        values.forEach(entries::add);
        final JsonObject out = new JsonObject();
        out.addProperty("replace", false);
        out.add("values", entries);
        return out;
    }

    private static JsonObject selfLoot(final RegisteredThing thing) {
        final JsonObject entry = item(thing.id().toString());
        final JsonArray conditions = new JsonArray();
        conditions.add(condition("minecraft:survives_explosion"));
        return lootTable(thing, entry, conditions);
    }

    /* As the game's own ores: the ore itself with Silk Touch, otherwise what it drops, more with Fortune. */
    private static JsonObject oreLoot(final RegisteredThing thing) {
        final Material material = thing.thing().material();
        final ResourceLocation dropped = BuiltInRegistries.ITEM.getKey(MaterialUses.item(material,
                material.oreDrop()).get());
        final JsonObject silk = item(thing.id().toString());
        final JsonArray silkConditions = new JsonArray();
        final JsonObject matchTool = condition("minecraft:match_tool");
        final JsonObject levels = new JsonObject();
        levels.addProperty("min", 1);
        final JsonObject silkTouch = new JsonObject();
        silkTouch.addProperty("enchantments", "minecraft:silk_touch");
        silkTouch.add("levels", levels);
        final JsonArray enchantments = new JsonArray();
        enchantments.add(silkTouch);
        final JsonObject predicates = new JsonObject();
        predicates.add("minecraft:enchantments", enchantments);
        final JsonObject predicate = new JsonObject();
        predicate.add("predicates", predicates);
        matchTool.add("predicate", predicate);
        silkConditions.add(matchTool);
        silk.add("conditions", silkConditions);
        final JsonObject drop = item(dropped.toString());
        final JsonArray functions = new JsonArray();
        final JsonObject fortune = new JsonObject();
        fortune.addProperty("function", "minecraft:apply_bonus");
        fortune.addProperty("enchantment", "minecraft:fortune");
        fortune.addProperty("formula", "minecraft:ore_drops");
        functions.add(fortune);
        final JsonObject decay = new JsonObject();
        decay.addProperty("function", "minecraft:explosion_decay");
        functions.add(decay);
        drop.add("functions", functions);
        final JsonArray children = new JsonArray();
        children.add(silk);
        children.add(drop);
        final JsonObject alternatives = new JsonObject();
        alternatives.addProperty("type", "minecraft:alternatives");
        alternatives.add("children", children);
        return lootTable(thing, alternatives, new JsonArray());
    }

    private static JsonObject lootTable(final RegisteredThing thing, final JsonObject entry,
                                        final JsonArray conditions) {
        final JsonArray entries = new JsonArray();
        entries.add(entry);
        final JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1.0);
        pool.addProperty("bonus_rolls", 0.0);
        pool.add("entries", entries);
        if (!conditions.isEmpty()) {
            pool.add("conditions", conditions);
        }
        final JsonArray pools = new JsonArray();
        pools.add(pool);
        final JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:block");
        table.add("pools", pools);
        table.addProperty("random_sequence", thing.id().getNamespace() + ":blocks/" + thing.id().getPath());
        return table;
    }

    private static JsonObject item(final String id) {
        final JsonObject out = new JsonObject();
        out.addProperty("type", "minecraft:item");
        out.addProperty("name", id);
        return out;
    }

    private static JsonObject condition(final String type) {
        final JsonObject out = new JsonObject();
        out.addProperty("condition", type);
        return out;
    }

    private static Supplier<byte[]> json(final JsonElement element) {
        final byte[] bytes = element.toString().getBytes(StandardCharsets.UTF_8);
        return () -> bytes;
    }
}
