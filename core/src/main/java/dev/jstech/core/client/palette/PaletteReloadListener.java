/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.palette;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jstech.core.JsCore;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolders;
import dev.jstech.core.palette.PaletteRoles;
import dev.jstech.core.palette.Palettes;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;

/**
 * Reads every declared palette back from the resource packs, each time the packs are loaded.
 *
 * <p>The file the top-most pack holds for a palette wins, as it does for a texture. A role it names takes the colour
 * it gives, and a role it leaves out keeps the colour the code declares, so a pack can change one colour and no
 * other. A palette no pack speaks for keeps its declared colours, and one whose file cannot be read is said so in
 * the log and keeps them too: a broken pack never leaves a screen with nothing to paint.
 */
public final class PaletteReloadListener implements ResourceManagerReloadListener {

    @Override
    public void onResourceManagerReload(final ResourceManager manager) {
        PaletteHolders.loadAll();
        for (final Palette<?> palette : Palettes.all()) {
            final Optional<Resource> file = manager.getResource(palette.file());
            if (file.isEmpty()) {
                Palettes.reset(palette);
                continue;
            }
            try (Reader reader = file.get().openAsReader()) {
                Palettes.load(palette, colours(palette, GsonHelper.parse(reader)));
            } catch (final IOException | RuntimeException unreadable) {
                JsCore.LOGGER.warn("The palette {} could not be read, so it keeps its own colours: {}",
                        palette.id(), unreadable.getMessage());
                Palettes.reset(palette);
            }
        }
    }

    /** The colours a palette file gives, by role; a value that is no colour is left out and said so. */
    private static Map<String, Integer> colours(final Palette<?> palette, final JsonObject file) {
        final Map<String, Integer> out = new LinkedHashMap<>();
        for (final Map.Entry<String, JsonElement> role : file.entrySet()) {
            final Integer colour = role.getValue().isJsonPrimitive()
                    ? PaletteRoles.parse(role.getValue().getAsString()) : null;
            if (colour == null) {
                JsCore.LOGGER.warn("The palette {} gives {} a colour that is not one: {}", palette.id(),
                        role.getKey(), role.getValue());
            } else {
                out.put(role.getKey(), colour);
            }
        }
        return out;
    }
}
