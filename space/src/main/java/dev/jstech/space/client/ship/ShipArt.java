/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.material.GameTexture;
import dev.jstech.core.resource.IRuntimeFiles;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.ship.ShipPartModels;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.fml.ModList;

/**
 * The starship parts' textures that show the game's own blocks, a crafting table, a furnace, a barrel, a bunk's wool,
 * made as the player's game loads its resources: the mod ships each such model's texture with those faces left grey,
 * at {@code textures/models/ship/base/<model>_<finish>.png}, and beside the model, in
 * {@code jsmodels/ship/<model>.game.json}, which texel of which of the game's textures each of them shows. Each is
 * drawn in from the game's own files, so the parts show the game's blocks as the game draws them and nothing of the
 * game's art ships in the mod; a resource pack can still replace the whole texture.
 */
@TextHolder
public final class ShipArt implements IRuntimeFiles {

    public static final TextKey TITLE = TextKey.of("jsspace.pack.ship_art",
            "J's Space: the game's blocks aboard starships");
    /* The game's block textures are this wide; a texel of the file beside a model counts from one of them. */
    private static final int GAME_WIDTH = 16;
    private static final int CHANNEL = 0xFF;

    private ShipArt() {
    }

    /** Declares the pack, from the mod's client constructor. */
    public static void declare() {
        RuntimePacks.declare(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_art"),
                PackType.CLIENT_RESOURCES, TITLE, new ShipArt());
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        for (final String name : ShipPartModels.names()) {
            final Optional<String> faces = GameTexture.fileText(ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
                    "jsmodels/ship/" + name + ".game.json"));
            if (faces.isEmpty()) {
                continue;
            }
            for (final String finish : ShipPartModels.FINISHES) {
                out.put("assets/" + JsSpace.MODID + "/textures/models/ship/" + name + "_" + finish + ".png",
                        () -> drawn(name, finish, faces.get()));
            }
        }
        return out;
    }

    /* The texture of the model {@code name} in {@code finish}: its own, the game's faces {@code faces} drawn in. */
    private static byte[] drawn(final String name, final String finish, final String faces) {
        try (NativeImage image = base(name, finish)) {
            final int width = image.getWidth();
            for (final JsonElement each : JsonParser.parseString(faces).getAsJsonObject().getAsJsonArray("faces")) {
                final JsonObject face = each.getAsJsonObject();
                final ResourceLocation from = ResourceLocation.parse(face.get("texture").getAsString());
                final GameTexture source = GameTexture.read(GameTexture.file(from))
                        .orElseThrow(() -> new IllegalStateException("there is no texture " + from + " to draw from"));
                final double scale = face.get("scale").getAsDouble();
                final JsonArray texels = face.getAsJsonArray("texels");
                for (int k = 0; k + 1 < texels.size(); k += 2) {
                    final int to = texels.get(k).getAsInt();
                    final int at = texels.get(k + 1).getAsInt();
                    final int row = at / GAME_WIDTH * source.width() / GAME_WIDTH;
                    final int column = at % GAME_WIDTH * source.width() / GAME_WIDTH;
                    image.setPixelRGBA(to % width, to / width, abgr(scaled(source.texels()[row * source.width()
                            + column], scale)));
                }
            }
            return image.asByteArray();
        } catch (final IOException unreadable) {
            throw new UncheckedIOException(unreadable);
        }
    }

    /* The texture the mod ships for the model {@code name} in {@code finish}, the game's faces left grey. */
    private static NativeImage base(final String name, final String finish) throws IOException {
        final Path path = ModList.get().getModFileById(JsSpace.MODID).getFile().findResource("assets", JsSpace.MODID,
                "textures/models/ship/base/" + name + "_" + finish + ".png");
        try (InputStream in = Files.newInputStream(path)) {
            return NativeImage.read(in);
        }
    }

    /* {@code argb} with its colour, not its alpha, brightened {@code scale} times, as far as white. */
    private static int scaled(final int argb, final double scale) {
        if (scale == 1.0) {
            return argb;
        }
        final int red = Math.min(CHANNEL, (int) Math.round((argb >> 16 & CHANNEL) * scale));
        final int green = Math.min(CHANNEL, (int) Math.round((argb >> 8 & CHANNEL) * scale));
        final int blue = Math.min(CHANNEL, (int) Math.round((argb & CHANNEL) * scale));
        return (argb >>> 24) << 24 | red << 16 | green << 8 | blue;
    }

    /* The game's images keep a texel as ABGR; the art is read as ARGB. The swap is its own inverse. */
    private static int abgr(final int argb) {
        return (argb >>> 24) << 24 | (argb & CHANNEL) << 16 | (argb >> 8 & CHANNEL) << 8 | (argb >> 16 & CHANNEL);
    }
}
