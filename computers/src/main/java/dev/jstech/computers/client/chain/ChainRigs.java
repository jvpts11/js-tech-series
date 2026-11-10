/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.computers.JsComputers;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * How a rigged chain machine is driven, as its model's file beside it says ({@code jsmodels/chain/<machine>.rig.json},
 * written with the model from its approved mock): the points, lengths and travels its renderer places its moving bones
 * by, in blocks from the middle of its terminal's floor as it stands facing north. Read once a machine, again when the
 * resource packs change; an empty rig for a machine whose file is missing, which its renderer draws at rest.
 */
public final class ChainRigs {

    private static final Map<String, JsonObject> READ = new ConcurrentHashMap<>();

    private ChainRigs() {
    }

    /** Hands the mod's bus what forgets what was read whenever the resource packs change. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener((ResourceManagerReloadListener) manager -> READ.clear()));
    }

    /** The rig of the machine of {@code id}; empty when its file is missing or broken. */
    public static JsonObject of(final String id) {
        return READ.computeIfAbsent(id, ChainRigs::read);
    }

    /** The point at {@code key} of {@code rig}, in blocks. */
    public static Vector3f point(final JsonObject rig, final String key) {
        final JsonArray at = rig.getAsJsonArray(key);
        return new Vector3f(at.get(0).getAsFloat(), at.get(1).getAsFloat(), at.get(2).getAsFloat());
    }

    /** The turn at {@code key} of {@code rig}, written as x, y, z, w. */
    public static Quaternionf turn(final JsonObject rig, final String key) {
        final JsonArray at = rig.getAsJsonArray(key);
        return new Quaternionf(at.get(0).getAsFloat(), at.get(1).getAsFloat(), at.get(2).getAsFloat(),
                at.get(3).getAsFloat());
    }

    /** The number at {@code key} of {@code rig}. */
    public static float number(final JsonObject rig, final String key) {
        return rig.get(key).getAsFloat();
    }

    private static JsonObject read(final String id) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "jsmodels/chain/" + id + ".rig.json");
        final Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            return new JsonObject();
        }
        try (Reader reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (final IOException | RuntimeException broken) {
            JsComputers.LOGGER.warn("The rig of the chain machine {} could not be read: {}", id, broken.getMessage());
            return new JsonObject();
        }
    }
}
