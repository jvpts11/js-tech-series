/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.material;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.JsCore;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.FormArt;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.OreLook;
import dev.jstech.core.material.RegisteredThing;
import dev.jstech.core.resource.IRuntimeFiles;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

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

    private MaterialArt() {
    }

    /** Declares the pack, from the Core's client constructor. */
    public static void declare() {
        RuntimePacks.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "material_art"),
                PackType.CLIENT_RESOURCES, TITLE, new MaterialArt());
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        // The game's textures each file is drawn from, read once per reload however many files draw on them.
        final Map<ResourceLocation, Optional<Picture>> read = new ConcurrentHashMap<>();
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
        return out;
    }

    private static int[] itemTexture(final RegisteredThing thing,
                                     final Map<ResourceLocation, Optional<Picture>> read) {
        final Form form = thing.thing().form();
        final Material material = thing.thing().material();
        if (form.art().isPresent()) {
            final Picture template = picture(texture(form.art().get()), read);
            return FormArt.recolour(template.texels(), template.width(), material.colour());
        }
        final String name = form.id().getPath();
        final Optional<String> fromTheGame = FormArt.gameTexture(name, material.kind() == MaterialKind.GEM);
        if (fromTheGame.isPresent()) {
            final Picture source = picture(ResourceLocation.withDefaultNamespace(fromTheGame.get()), read);
            return FormArt.recolour(source.texels(), source.width(), material.colour());
        }
        return FormArt.drawn(name, material.colour());
    }

    private static int[] blockTexture(final RegisteredThing thing,
                                      final Map<ResourceLocation, Optional<Picture>> read) {
        final Material material = thing.thing().material();
        final HostRock rock = thing.rock();
        if (rock == null) {
            return itemTexture(thing, read);
        }
        final OreLook look = material.oreLook();
        final boolean deepslate = rock == HostRocks.DEEPSLATE;
        final Picture ore = picture(ResourceLocation.withDefaultNamespace(
                "textures/" + look.pattern().texture(deepslate) + ".png"), read);
        final Picture gameRock = picture(ResourceLocation.withDefaultNamespace(
                "textures/" + OreLook.Pattern.rock(deepslate) + ".png"), read);
        final Picture host = picture(texture(rock.texture()), read);
        final int[] specks = FormArt.specks(ore.texels(), ore.width(), gameRock.texels(), gameRock.width());
        return FormArt.ore(host.texels(), host.width(), specks, look.speckColour(material.colour()), look.second(),
                look.lowHue(), look.highHue());
    }

    private static ResourceLocation texture(final ResourceLocation id) {
        return id.withPath("textures/" + id.getPath() + ".png");
    }

    private static Picture picture(final ResourceLocation file, final Map<ResourceLocation, Optional<Picture>> read) {
        return read.computeIfAbsent(file, MaterialArt::readPicture)
                .orElseThrow(() -> new IllegalStateException("there is no texture " + file + " to draw from"));
    }

    /*
     * The game's own textures come from its own files, never a resource pack's, so the art is the same whatever pack
     * a player chose; a mod's from its own jar.
     */
    private static Optional<Picture> readPicture(final ResourceLocation file) {
        try {
            if (ResourceLocation.DEFAULT_NAMESPACE.equals(file.getNamespace())) {
                final IoSupplier<InputStream> found = Minecraft.getInstance().getVanillaPackResources()
                        .getResource(PackType.CLIENT_RESOURCES, file);
                if (found == null) {
                    return Optional.empty();
                }
                try (InputStream in = found.get()) {
                    return Optional.of(Picture.read(in));
                }
            }
            for (final IModFileInfo mod : ModList.get().getModFiles()) {
                final Path path = mod.getFile().findResource("assets", file.getNamespace(), file.getPath());
                if (Files.isRegularFile(path)) {
                    try (InputStream in = Files.newInputStream(path)) {
                        return Optional.of(Picture.read(in));
                    }
                }
            }
            return Optional.empty();
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    private static byte[] png(final int[] argb) {
        try (NativeImage image = new NativeImage(SIZE, SIZE, true)) {
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    image.setPixelRGBA(x, y, Picture.swap(argb[y * SIZE + x]));
                }
            }
            return image.asByteArray();
        } catch (final IOException unwritable) {
            throw new UncheckedIOException(unwritable);
        }
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

    /* A texture read, as ARGB texels row by row. */
    private record Picture(int[] texels, int width) {

        private static Picture read(final InputStream in) throws IOException {
            try (NativeImage image = NativeImage.read(in)) {
                // A texture that moves is its frames one under another, each as tall as it is wide: the first is read.
                final int width = image.getWidth();
                final int rows = Math.min(image.getHeight(), width);
                final int[] texels = new int[width * rows];
                for (int y = 0; y < rows; y++) {
                    for (int x = 0; x < width; x++) {
                        texels[y * width + x] = swap(image.getPixelRGBA(x, y));
                    }
                }
                return new Picture(texels, width);
            }
        }

        /* The game's images keep a texel as ABGR; the art is drawn in ARGB. The swap is its own inverse. */
        private static int swap(final int texel) {
            final int alpha = texel >>> 24;
            final int first = texel >> 16 & 0xFF;
            final int green = texel >> 8 & 0xFF;
            final int last = texel & 0xFF;
            return alpha << 24 | last << 16 | green << 8 | first;
        }
    }
}
