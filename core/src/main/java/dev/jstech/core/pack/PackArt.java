/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.pack;

import com.google.gson.JsonObject;
import dev.jstech.core.resource.IRuntimeFiles;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;

/**
 * The art and the words a content pack's things need on a player's game, made as it loads under every resource pack,
 * so a pack that brings its own replaces any of it: each namespace's English names, a block state and a model for
 * each block, its whole cube drawn with {@code <namespace>:block/<path>}, a machine's controller as the game's
 * furnaces are drawn, with its {@code _front}, {@code _side} and {@code _top}, turned the way it faces, and each item
 * flat, drawn with {@code <namespace>:item/<path>}. The textures themselves are the packs' own.
 */
final class PackArt implements IRuntimeFiles {

    private static final String[] FACINGS = {"north", "east", "south", "west"};
    private static final int QUARTER = 90;

    private final List<ContentPacks.Declared> declared;

    PackArt(final List<ContentPacks.Declared> declared) {
        this.declared = declared;
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        final Map<String, JsonObject> languages = new LinkedHashMap<>();
        final Map<String, Boolean> tabbed = new LinkedHashMap<>();
        for (final ContentPacks.Declared each : this.declared) {
            final PackDeclaration declaration = each.declaration();
            final ResourceLocation id = ResourceLocation.parse(declaration.id());
            final String ns = id.getNamespace();
            final String path = id.getPath();
            final JsonObject words = languages.computeIfAbsent(ns, name -> new JsonObject());
            switch (declaration) {
                case PackDeclaration.Block block -> {
                    words.addProperty("block." + ns + "." + path, block.name());
                    out.put(asset(ns, "blockstates", path), json(state(model(ns, "block/" + path), "")));
                    out.put(asset(ns, "models/block", path), json(cube(ns, path)));
                    if (block.item()) {
                        out.put(asset(ns, "models/item", path), json(parent(ns + ":block/" + path)));
                        tabbed.putIfAbsent(ns, false);
                    }
                }
                case PackDeclaration.Item item -> {
                    words.addProperty("item." + ns + "." + path, item.name());
                    out.put(asset(ns, "models/item", path), json(flat(ns, path)));
                    tabbed.putIfAbsent(ns, false);
                }
                case PackDeclaration.Machine machine -> {
                    words.addProperty("block." + ns + "." + path, machine.name());
                    out.put(asset(ns, "blockstates", path), json(facing(ns + ":block/" + path)));
                    out.put(asset(ns, "models/block", path), json(orientable(ns, path)));
                    out.put(asset(ns, "models/item", path), json(parent(ns + ":block/" + path)));
                    tabbed.putIfAbsent(ns, false);
                }
                case PackDeclaration.Material material -> words.addProperty(ContentPacks.nameKey(id),
                        material.name());
                case PackDeclaration.Processing processing -> words.addProperty(ns + ".recipe_kind." + path,
                        processing.name());
                case PackDeclaration.Tab tab -> {
                    words.addProperty("itemGroup." + ns + "." + path, tab.name());
                    tabbed.put(ns, true);
                }
            }
        }
        // A namespace whose packs declared no tab has one called by the namespace itself.
        tabbed.forEach((ns, named) -> {
            if (!named) {
                languages.computeIfAbsent(ns, name -> new JsonObject()).addProperty("itemGroup." + ns + ".content",
                        ns);
            }
        });
        languages.forEach((ns, words) -> out.put("assets/" + ns + "/lang/en_us.json", json(words)));
        return out;
    }

    private static String asset(final String namespace, final String folder, final String path) {
        return "assets/" + namespace + "/" + folder + "/" + path + ".json";
    }

    private static JsonObject state(final JsonObject model, final String variant) {
        final JsonObject variants = new JsonObject();
        variants.add(variant, model);
        final JsonObject state = new JsonObject();
        state.add("variants", variants);
        return state;
    }

    /* A controller's state: its model turned a quarter for each way it may face, north first. */
    private static JsonObject facing(final String model) {
        final JsonObject variants = new JsonObject();
        for (int i = 0; i < FACINGS.length; i++) {
            final JsonObject turned = model(model);
            if (i > 0) {
                turned.addProperty("y", i * QUARTER);
            }
            variants.add("facing=" + FACINGS[i], turned);
        }
        final JsonObject state = new JsonObject();
        state.add("variants", variants);
        return state;
    }

    private static JsonObject model(final String model) {
        final JsonObject variant = new JsonObject();
        variant.addProperty("model", model);
        return variant;
    }

    private static JsonObject model(final String namespace, final String path) {
        return model(namespace + ":" + path);
    }

    private static JsonObject cube(final String namespace, final String path) {
        final JsonObject textures = new JsonObject();
        textures.addProperty("all", namespace + ":block/" + path);
        final JsonObject model = parent("minecraft:block/cube_all");
        model.add("textures", textures);
        return model;
    }

    private static JsonObject orientable(final String namespace, final String path) {
        final JsonObject textures = new JsonObject();
        textures.addProperty("front", namespace + ":block/" + path + "_front");
        textures.addProperty("side", namespace + ":block/" + path + "_side");
        textures.addProperty("top", namespace + ":block/" + path + "_top");
        final JsonObject model = parent("minecraft:block/orientable");
        model.add("textures", textures);
        return model;
    }

    private static JsonObject flat(final String namespace, final String path) {
        final JsonObject textures = new JsonObject();
        textures.addProperty("layer0", namespace + ":item/" + path);
        final JsonObject model = parent("minecraft:item/generated");
        model.add("textures", textures);
        return model;
    }

    private static JsonObject parent(final String parent) {
        final JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        return model;
    }

    private static Supplier<byte[]> json(final JsonObject json) {
        final byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        return () -> bytes;
    }
}
