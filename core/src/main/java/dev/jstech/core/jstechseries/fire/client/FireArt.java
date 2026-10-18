/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.fire.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.material.GameTexture;
import dev.jstech.core.jstechseries.fire.CharredBlocks;
import dev.jstech.core.material.FormArt;
import dev.jstech.core.resource.IRuntimeFiles;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;

/**
 * How what fire leaves looks, made as the player's game loads its resources: charred wood from the game's own oak,
 * its grain and rings kept and its colour burnt to charcoal, and ash from the game's fine grey powder. Nothing of the
 * game's art ships in the Core; a resource pack can replace any of these textures.
 */
@TextHolder
public final class FireArt implements IRuntimeFiles {

    public static final TextKey TITLE = TextKey.of("jscore.pack.fire_art", "J's Core: what fire leaves");

    private FireArt() {
    }

    /** Declares the pack, from the Core's client constructor. */
    public static void declare() {
        RuntimePacks.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "fire_art"),
                PackType.CLIENT_RESOURCES, TITLE, new FireArt());
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        for (final CharredBlocks.BurntTexture texture : CharredBlocks.TEXTURES) {
            out.put("assets/" + JsCore.MODID + "/textures/block/" + texture.name() + ".png", () -> burnt(texture));
        }
        return out;
    }

    /* The game's texture the burnt one is drawn from, its light kept and its colour turned. */
    private static byte[] burnt(final CharredBlocks.BurntTexture texture) {
        final ResourceLocation from = ResourceLocation.withDefaultNamespace(texture.from());
        final GameTexture source = GameTexture.read(GameTexture.file(from))
                .orElseThrow(() -> new IllegalStateException("there is no texture " + from + " to draw from"));
        return GameTexture.png(FormArt.recolour(source.texels(), source.width(), texture.colour()), FormArt.SIZE);
    }
}
