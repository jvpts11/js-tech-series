/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.material;

import com.google.gson.JsonObject;
import dev.jstech.core.JsCore;
import dev.jstech.core.material.CatalogueItems;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.FormArt;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialLiquid;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.OreLook;
import dev.jstech.core.material.RegisteredThing;
import dev.jstech.core.resource.CachedRuntimeFiles;
import dev.jstech.core.resource.IRuntimeFiles;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/**
 * How the things of the materials' uses look, made as the player's game loads its resources: every texture drawn in
 * its material's colour, from the game's own texture for the forms the game has (the iron ingot for every ingot), from
 * the Core's templates for the rest, and, for an ore, from its rock's texture with the specks of one of the game's ores
 * laid on it; and the models and block states that show them. Nothing of the game's art ships in the Core: it is read
 * from the game's own files each time. A resource pack can replace any of these files.
 */
@TextHolder
public final class MaterialArt implements IRuntimeFiles {

    public static final TextKey TITLE = TextKey.of("jscore.pack.material_art", "J's Core: the materials' art");

    private static final int SIZE = FormArt.SIZE;
    /** The way the art is drawn, raised whenever it changes, so what was kept on disk is drawn again. */
    private static final int DRAWING = 1;

    private MaterialArt() {
    }

    /** Declares the pack, from the Core's client constructor: drawn once, then kept on disk while nothing changes. */
    public static void declare() {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "material_art");
        RuntimePacks.declare(id, PackType.CLIENT_RESOURCES, TITLE, CachedRuntimeFiles.of(id, new MaterialArt(),
                MaterialArt::fingerprint));
    }

    /**
     * What the art is drawn from, which tells whether the art kept on disk still holds: the way it is drawn, every mod
     * with its version (whose textures it is drawn from), and each thing with its material's colour, its ore's look and
     * its rock.
     */
    public static String fingerprint() {
        final StringBuilder out = new StringBuilder().append(DRAWING).append('\n')
                .append(CachedRuntimeFiles.modsFingerprint());
        for (final RegisteredThing thing : MaterialUses.registered()) {
            final Material material = thing.thing().material();
            out.append(thing.id()).append(' ').append(material.colour()).append(' ').append(material.oreLook())
                    .append(' ').append(thing.rock() == null ? "" : thing.rock().texture()).append('\n');
        }
        return out.toString();
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        // The game's textures each file is drawn from, read once per reload however many files draw on them.
        final Map<ResourceLocation, Optional<GameTexture>> read = new ConcurrentHashMap<>();
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        for (final RegisteredThing thing : MaterialUses.registered()) {
            final String namespace = thing.id().getNamespace();
            final String path = thing.id().getPath();
            final String assets = "assets/" + namespace + "/";
            if (thing.isBlock()) {
                out.put(assets + "blockstates/" + path + ".json", json(blockState(thing.id())));
                out.put(assets + "models/block/" + path + ".json", json(cubeAll(thing.id())));
                out.put(assets + "models/item/" + path + ".json", json(parent(namespace + ":block/" + path)));
                out.put(assets + "textures/block/" + path + ".png", () -> png(blockTexture(thing, read)));
            } else {
                out.put(assets + "models/item/" + path + ".json", json(itemModel(thing)));
                out.put(assets + "textures/item/" + path + ".png", () -> png(itemTexture(thing, read)));
            }
        }
        for (final MaterialLiquid liquid : MaterialLiquids.registered()) {
            final ResourceLocation id = liquid.id();
            final String assets = "assets/" + id.getNamespace() + "/";
            out.put(assets + "blockstates/" + id.getPath() + ".json", json(blockState(id)));
            out.put(assets + "models/block/" + id.getPath() + ".json", json(particleOnly()));
            out.put(assets + "models/item/" + liquid.bucketId().getPath() + ".json", json(bucket(id)));
        }
        for (final CatalogueItems.Entry entry : CatalogueItems.registered()) {
            out.put("assets/" + entry.id().getNamespace() + "/models/item/" + entry.id().getPath() + ".json",
                    json(generated(entry.art())));
        }
        return out;
    }

    /* A liquid block shows only its fluid, which the game draws; the model gives the particles of breaking it. */
    private static JsonObject particleOnly() {
        final JsonObject textures = new JsonObject();
        textures.addProperty("particle", "minecraft:block/water_still");
        final JsonObject model = new JsonObject();
        model.add("textures", textures);
        return model;
    }

    /* A bucket with the fluid drawn in it by the game's loader of fluid containers, in the fluid's own tint. */
    private static JsonObject bucket(final ResourceLocation fluid) {
        final JsonObject model = parent("neoforge:item/bucket");
        model.addProperty("loader", "neoforge:fluid_container");
        model.addProperty("fluid", fluid.toString());
        model.addProperty("apply_tint", true);
        return model;
    }

    private static JsonObject generated(final ResourceLocation texture) {
        final JsonObject textures = new JsonObject();
        textures.addProperty("layer0", texture.toString());
        final JsonObject model = parent("minecraft:item/generated");
        model.add("textures", textures);
        return model;
    }

    private static int[] itemTexture(final RegisteredThing thing,
                                     final Map<ResourceLocation, Optional<GameTexture>> read) {
        final Form form = thing.thing().form();
        final Material material = thing.thing().material();
        if (form.art().isPresent()) {
            final GameTexture template = picture(texture(form.art().get()), read);
            return FormArt.recolour(template.texels(), template.width(), material.colour());
        }
        final String name = form.id().getPath();
        final Optional<String> fromTheGame = FormArt.gameTexture(name, material.kind() == MaterialKind.GEM);
        if (fromTheGame.isPresent()) {
            final GameTexture source = picture(ResourceLocation.withDefaultNamespace(fromTheGame.get()), read);
            return FormArt.recolour(source.texels(), source.width(), material.colour());
        }
        return FormArt.drawn(name, material.colour());
    }

    private static int[] blockTexture(final RegisteredThing thing,
                                      final Map<ResourceLocation, Optional<GameTexture>> read) {
        final Material material = thing.thing().material();
        final HostRock rock = thing.rock();
        if (rock == null) {
            return itemTexture(thing, read);
        }
        final OreLook look = material.oreLook();
        final boolean deepslate = rock == HostRocks.DEEPSLATE;
        final GameTexture ore = picture(ResourceLocation.withDefaultNamespace(
                "textures/" + look.pattern().texture(deepslate) + ".png"), read);
        final GameTexture gameRock = picture(ResourceLocation.withDefaultNamespace(
                "textures/" + OreLook.Pattern.rock(deepslate) + ".png"), read);
        final GameTexture host = picture(texture(rockTexture(rock, read)), read);
        final int[] specks = FormArt.specks(ore.texels(), ore.width(), gameRock.texels(), gameRock.width());
        return FormArt.ore(host.texels(), host.width(), specks, look.speckColour(material.colour()), look.second(),
                look.lowHue(), look.highHue());
    }

    /*
     * The texture an ore in {@code rock} is drawn on: the rock's own as declared, or, for a rock of another mod found
     * by its tags whose texture is elsewhere, the one its block's own files draw it with.
     */
    private static ResourceLocation rockTexture(final HostRock rock,
                                                final Map<ResourceLocation, Optional<GameTexture>> read) {
        if (!rock.found() || read.computeIfAbsent(texture(rock.texture()), GameTexture::read).isPresent()) {
            return rock.texture();
        }
        return BlockTextures.of(rock.rock()).orElse(rock.texture());
    }

    private static ResourceLocation texture(final ResourceLocation id) {
        return id.withPath("textures/" + id.getPath() + ".png");
    }

    private static GameTexture picture(final ResourceLocation file,
                                       final Map<ResourceLocation, Optional<GameTexture>> read) {
        return read.computeIfAbsent(file, GameTexture::read)
                .orElseThrow(() -> new IllegalStateException("there is no texture " + file + " to draw from"));
    }

    private static byte[] png(final int[] argb) {
        return GameTexture.png(argb, SIZE);
    }

    private static JsonObject itemModel(final RegisteredThing thing) {
        final JsonObject textures = new JsonObject();
        textures.addProperty("layer0", thing.id().getNamespace() + ":item/" + thing.id().getPath());
        // A rod is held as a stick is, the rest as any item.
        final JsonObject model = parent(thing.thing().form() == Forms.ROD ? "minecraft:item/handheld"
                : "minecraft:item/generated");
        model.add("textures", textures);
        return model;
    }

    private static JsonObject cubeAll(final ResourceLocation id) {
        final JsonObject textures = new JsonObject();
        textures.addProperty("all", id.getNamespace() + ":block/" + id.getPath());
        final JsonObject model = parent("minecraft:block/cube_all");
        model.add("textures", textures);
        return model;
    }

    private static JsonObject blockState(final ResourceLocation id) {
        final JsonObject model = new JsonObject();
        model.addProperty("model", id.getNamespace() + ":block/" + id.getPath());
        final JsonObject variants = new JsonObject();
        variants.add("", model);
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
