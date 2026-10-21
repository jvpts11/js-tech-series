/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.variant;

import com.google.gson.JsonObject;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.material.BlockTextures;
import dev.jstech.core.resource.IRuntimeFiles;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.variant.Variant;
import dev.jstech.core.variant.Variants;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/**
 * How the variants look, made as the player's game loads its resources: each drawn with its kind's model, the
 * textures the kind names given the texture its material's own block is drawn with ({@link BlockTextures}), so a
 * frame of another mod's maple shows that mod's maple planks. Nothing is drawn: the files only point at textures the
 * game already has. A resource pack can replace any of these files.
 */
@TextHolder
public final class VariantArt implements IRuntimeFiles {

    public static final TextKey TITLE = TextKey.of("jscore.pack.variant_art", "J's Core: the variants' look");

    private VariantArt() {
    }

    /** Declares the pack, from the Core's client constructor. */
    public static void declare() {
        RuntimePacks.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "variant_art"),
                PackType.CLIENT_RESOURCES, TITLE, new VariantArt());
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        for (final Variant variant : Variants.registered()) {
            final String namespace = variant.id().getNamespace();
            final String path = variant.id().getPath();
            final String assets = "assets/" + namespace + "/";
            final String model = namespace + ":block/" + path;
            out.put(assets + "blockstates/" + path + ".json", json(blockState(model)));
            out.put(assets + "models/block/" + path + ".json", json(blockModel(variant)));
            out.put(assets + "models/item/" + path + ".json", json(parent(model)));
        }
        return out;
    }

    private static JsonObject blockModel(final Variant variant) {
        final ResourceLocation texture = BlockTextures.of(variant.material().block())
                .orElse(variant.material().source().withPrefix("block/"));
        final JsonObject textures = new JsonObject();
        for (final String name : variant.kind().textures()) {
            textures.addProperty(name, texture.toString());
        }
        final JsonObject model = parent(variant.kind().template().toString());
        model.add("textures", textures);
        return model;
    }

    /* One look for every state of the block: a kind's model turns itself, if it turns. */
    private static JsonObject blockState(final String model) {
        final JsonObject variant = new JsonObject();
        variant.addProperty("model", model);
        final JsonObject variants = new JsonObject();
        variants.add("", variant);
        final JsonObject state = new JsonObject();
        state.add("variants", variants);
        return state;
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
