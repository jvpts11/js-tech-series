/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.material;

import com.google.common.base.Suppliers;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.jstech.core.JsCore;
import dev.jstech.core.log.CoreLogs;
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
import dev.jstech.core.material.OrePiecesBlock;
import dev.jstech.core.material.RegisteredThing;
import dev.jstech.core.resource.CachedRuntimeFiles;
import dev.jstech.core.resource.IRuntimeFiles;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

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

    private static final Logger LOG = CoreLogs.CONTENT.logger();
    private static final int SIZE = FormArt.SIZE;
    /** The way the art is drawn, raised whenever it changes, so what was kept on disk is drawn again. */
    private static final int DRAWING = 4;
    private static final int QUARTER = 90;
    private static final int CHANNEL = 0xFF;
    /*
     * The ways an ore's pieces lie over its vein: each piece its box's corners on the ground and its height, in
     * sixteenths of a block, x0, z0, x1, z1 and height.
     */
    private static final int[][][] PIECES = {
        {{4, 5, 10, 10, 3}, {9, 3, 13, 7, 2}, {3, 9, 6, 12, 2}},
        {{5, 4, 11, 9, 2}, {3, 8, 7, 12, 3}},
        {{6, 6, 11, 11, 3}, {10, 3, 13, 6, 2}, {3, 4, 6, 7, 1}, {11, 10, 13, 13, 1}}
    };

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
                    .append(' ').append(thing.rock() == null ? "" : thing.rock().texture() + " "
                            + thing.rock().top() + " " + thing.rock().bottom()).append('\n');
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
                final HostRock rock = thing.rock();
                out.put(assets + "blockstates/" + path + ".json", json(blockState(thing.id())));
                out.put(assets + "models/item/" + path + ".json", json(parent(namespace + ":block/" + path)));
                if (rock != null && rock.hasEnds()) {
                    // An ore in a rock with ends of its own, sandstone's, wears them as the rock does.
                    out.put(assets + "models/block/" + path + ".json", json(cubeBottomTop(thing.id())));
                    out.put(assets + "textures/block/" + path + ".png",
                            () -> png(blockTexture(thing, rock.texture(), read)));
                    out.put(assets + "textures/block/" + path + "_top.png",
                            () -> png(blockTexture(thing, rock.top(), read)));
                    out.put(assets + "textures/block/" + path + "_bottom.png",
                            () -> png(blockTexture(thing, rock.bottom(), read)));
                } else {
                    out.put(assets + "models/block/" + path + ".json", json(cubeAll(thing.id())));
                    out.put(assets + "textures/block/" + path + ".png", () -> png(blockTexture(thing,
                            rock == null ? null : rock.texture(), read)));
                }
            } else {
                out.put(assets + "models/item/" + path + ".json", json(itemModel(thing)));
                out.put(assets + "textures/item/" + path + ".png", () -> png(itemTexture(thing, read)));
            }
        }
        // The pieces of each ore over its veins: three ways of lying, each turned any way, cut from the picture of its
        // ore in plain stone, the Core's drawn here or the game's own (copper, gold).
        final Map<ResourceLocation, RegisteredThing> drawn = new HashMap<>();
        MaterialUses.registered().forEach(thing -> drawn.put(thing.id(), thing));
        for (final Map.Entry<Material, ResourceLocation> each : MaterialUses.pieces().entrySet()) {
            final HostRock rock = MaterialUses.pieceRock(each.getKey());
            if (rock == null) {
                continue;
            }
            final ResourceLocation ore = BuiltInRegistries.BLOCK.getKey(MaterialUses.oreBlock(each.getKey(), rock)
                    .get());
            final RegisteredThing own = drawn.get(ore);
            final Supplier<int[]> picture = Suppliers.memoize(() -> own != null
                    ? blockTexture(own, rock.texture(), read) : gamePicture(ore, read));
            final ResourceLocation id = each.getValue();
            final String assets = "assets/" + id.getNamespace() + "/";
            out.put(assets + "blockstates/" + id.getPath() + ".json", json(piecesState(id)));
            for (int layout = 0; layout < OrePiecesBlock.LAYOUTS; layout++) {
                final int way = layout;
                out.put(assets + "models/block/" + id.getPath() + "_" + layout + ".json",
                        () -> piecesModel(ore, way, picture.get()).toString().getBytes(StandardCharsets.UTF_8));
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
        final int colour = form.oreColoured() ? material.oreLook().speckColour(material.colour())
                : material.colour();
        if (form.art().isPresent()) {
            final GameTexture template = picture(texture(form.art().get()), read);
            return FormArt.recolour(template.texels(), template.width(), colour);
        }
        final String name = form.id().getPath();
        final Optional<String> fromTheGame = FormArt.gameTexture(name, material.kind() == MaterialKind.GEM);
        if (fromTheGame.isPresent()) {
            final GameTexture source = picture(ResourceLocation.withDefaultNamespace(fromTheGame.get()), read);
            return FormArt.recolour(source.texels(), source.width(), colour);
        }
        return FormArt.drawn(name, colour);
    }

    /* The texture of {@code thing}'s block, an ore's face drawn on {@code face}, its rock's face there. */
    private static int[] blockTexture(final RegisteredThing thing, final @Nullable ResourceLocation face,
                                      final Map<ResourceLocation, Optional<GameTexture>> read) {
        final Material material = thing.thing().material();
        final HostRock rock = thing.rock();
        if (rock == null || face == null) {
            return itemTexture(thing, read);
        }
        final OreLook look = material.oreLook();
        final boolean deepslate = rock == HostRocks.DEEPSLATE;
        final GameTexture ore = picture(ResourceLocation.withDefaultNamespace(
                "textures/" + look.pattern().texture(deepslate) + ".png"), read);
        final GameTexture gameRock = picture(ResourceLocation.withDefaultNamespace(
                "textures/" + look.pattern().rock(deepslate) + ".png"), read);
        final GameTexture host = host(rock, face, gameRock, read);
        final int[] specks = FormArt.specks(ore.texels(), ore.width(), gameRock.texels(), gameRock.width());
        return FormArt.ore(host.texels(), host.width(), specks, look.speckColour(material.colour()), look.sheen(),
                look.second(), look.lowHue(), look.highHue());
    }

    /*
     * The texture an ore's face in {@code rock} is drawn on: the rock's own face as declared, {@code face}, or, for a
     * rock of another mod found by its tags whose texture is elsewhere, the one its block's own files draw it with.
     * Where neither can be read whole, a full block's face of it at least, the game's own rock of the ore's pattern,
     * {@code gameRock}, rather than no ore drawn at all.
     */
    private static GameTexture host(final HostRock rock, final ResourceLocation face, final GameTexture gameRock,
                                    final Map<ResourceLocation, Optional<GameTexture>> read) {
        final List<ResourceLocation> tried = new ArrayList<>();
        tried.add(face);
        if (rock.found()) {
            BlockTextures.of(rock.rock()).ifPresent(tried::add);
        }
        for (final ResourceLocation candidate : tried) {
            final Optional<GameTexture> found = read.computeIfAbsent(texture(candidate), GameTexture::read);
            if (found.isPresent() && found.get().width() >= SIZE
                    && found.get().texels().length >= SIZE * found.get().width()) {
                return found.get();
            }
        }
        LOG.warn("No texture of {} could be read whole ({}); its ores are drawn on the game's own rock",
                rock.rock(), tried);
        return gameRock;
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

    /* A block with sides of its own texture and a top and a bottom of their own, {@code <path>_top} and so on. */
    private static JsonObject cubeBottomTop(final ResourceLocation id) {
        final String block = id.getNamespace() + ":block/" + id.getPath();
        final JsonObject textures = new JsonObject();
        textures.addProperty("side", block);
        textures.addProperty("top", block + "_top");
        textures.addProperty("bottom", block + "_bottom");
        final JsonObject model = parent("minecraft:block/cube_bottom_top");
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

    /*
     * The picture of a block another mod or the game draws, {@code block}'s texture of its own name, none where it
     * cannot be read whole at the art's size.
     */
    private static int @Nullable [] gamePicture(final ResourceLocation block,
                                                final Map<ResourceLocation, Optional<GameTexture>> read) {
        return read.computeIfAbsent(texture(block.withPrefix("block/")), GameTexture::read)
                .filter(picture -> picture.width() == SIZE && picture.texels().length == SIZE * SIZE)
                .map(GameTexture::texels).orElse(null);
    }

    /* The pieces' block state: each way they lie, turned to each side. */
    private static JsonObject piecesState(final ResourceLocation id) {
        final JsonObject variants = new JsonObject();
        final String[] sides = {"north", "east", "south", "west"};
        for (int layout = 0; layout < OrePiecesBlock.LAYOUTS; layout++) {
            for (int turn = 0; turn < sides.length; turn++) {
                final JsonObject model = new JsonObject();
                model.addProperty("model", id.getNamespace() + ":block/" + id.getPath() + "_" + layout);
                if (turn > 0) {
                    model.addProperty("y", turn * QUARTER);
                }
                variants.add("facing=" + sides[turn] + ",layout=" + layout, model);
            }
        }
        final JsonObject state = new JsonObject();
        state.add("variants", variants);
        return state;
    }

    /*
     * The pieces' model lying the way {@code layout} says: each piece a box whose faces are cut from the ore's picture,
     * {@code ore}'s texture, where the mineral shows most in a window of the piece's size, or where the piece lies on
     * it when the picture could not be read.
     */
    private static JsonObject piecesModel(final ResourceLocation ore, final int layout,
                                          final int @Nullable [] picture) {
        final String texture = ore.getNamespace() + ":block/" + ore.getPath();
        final JsonObject textures = new JsonObject();
        textures.addProperty("ore", texture);
        textures.addProperty("particle", texture);
        final JsonArray elements = new JsonArray();
        for (final int[] piece : PIECES[layout]) {
            final int x0 = piece[0];
            final int z0 = piece[1];
            final int x1 = piece[2];
            final int z1 = piece[3];
            final int height = piece[4];
            final JsonObject element = new JsonObject();
            element.add("from", numbers(x0, 0, z0));
            element.add("to", numbers(x1, height, z1));
            // Each face its own cut of the ore, of its own size, where the mineral shows most.
            final JsonObject top = face(picture, x0, z0, x1 - x0, z1 - z0);
            final JsonObject across = face(picture, x0, z0, x1 - x0, height);
            final JsonObject along = face(picture, z0, x0, z1 - z0, height);
            final JsonObject faces = new JsonObject();
            faces.add("up", top);
            faces.add("north", across);
            faces.add("south", across);
            faces.add("east", along);
            faces.add("west", along);
            element.add("faces", faces);
            elements.add(element);
        }
        final JsonObject model = new JsonObject();
        model.addProperty("ambientocclusion", false);
        model.add("textures", textures);
        model.add("elements", elements);
        return model;
    }

    /* The window of the picture {@code width} by {@code height} whose colours vary most: where its mineral shows. */
    private static int[] richest(final int[] picture, final int width, final int height) {
        double best = -1;
        int[] at = {0, 0};
        for (int y = 0; y + height <= SIZE; y++) {
            for (int x = 0; x + width <= SIZE; x++) {
                final double spread = spread(picture, x, y, width, height);
                if (spread > best) {
                    best = spread;
                    at = new int[] {x, y};
                }
            }
        }
        return at;
    }

    /* How far the window's colours lie from their mean, the three channels added. */
    private static double spread(final int[] picture, final int x0, final int y0, final int width, final int height) {
        final double[] sum = new double[3];
        final double[] squares = new double[3];
        final int count = width * height;
        for (int y = y0; y < y0 + height; y++) {
            for (int x = x0; x < x0 + width; x++) {
                final int argb = picture[y * SIZE + x];
                for (int c = 0; c < 3; c++) {
                    final int value = argb >> (16 - 8 * c) & CHANNEL;
                    sum[c] += value;
                    squares[c] += (double) value * value;
                }
            }
        }
        double spread = 0;
        for (int c = 0; c < 3; c++) {
            final double mean = sum[c] / count;
            spread += Math.sqrt(Math.max(0, squares[c] / count - mean * mean));
        }
        return spread;
    }

    /*
     * A face {@code width} by {@code height} cut from the ore's picture where its mineral shows most, or, the picture
     * unread, where the piece lies on it, from {@code x, y}.
     */
    private static JsonObject face(final int @Nullable [] picture, final int x, final int y, final int width,
                                   final int height) {
        final int[] at = picture == null ? new int[] {x, y} : richest(picture, width, height);
        final JsonObject face = new JsonObject();
        face.add("uv", numbers(at[0], at[1], at[0] + width, at[1] + height));
        face.addProperty("texture", "#ore");
        return face;
    }

    private static JsonArray numbers(final int... values) {
        final JsonArray out = new JsonArray();
        for (final int value : values) {
            out.add(value);
        }
        return out;
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
