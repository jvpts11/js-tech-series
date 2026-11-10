/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.computers.JsComputers;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What each chain machine gives off while it works, as its model's file beside it says
 * ({@code jsmodels/chain/<machine>.emitters.json}, written with the model from its approved mock): each emitter an
 * effect of J's Core, where on the machine it plays and which way, in blocks from the middle of its terminal's floor
 * as it stands facing north; the channel of the material at work whose colour it takes, if any; and the mode it shows
 * only in, if any. Read once a machine, again when the resource packs change.
 */
public final class ChainEmitters {

    private static final Map<String, List<Emitter>> READ = new ConcurrentHashMap<>();

    private ChainEmitters() {
    }

    /** Hands the mod's bus what forgets what was read whenever the resource packs change. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener((ResourceManagerReloadListener) manager -> READ.clear()));
    }

    /** The emitters of the machine of {@code id}; none when its file is missing. */
    public static List<Emitter> of(final String id) {
        return READ.computeIfAbsent(id, ChainEmitters::read);
    }

    private static List<Emitter> read(final String id) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "jsmodels/chain/" + id + ".emitters.json");
        final Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            return List.of();
        }
        final List<Emitter> out = new ArrayList<>();
        try (Reader reader = resource.get().openAsReader()) {
            final JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (final JsonElement each : root.getAsJsonArray("emitters")) {
                final JsonObject line = each.getAsJsonObject();
                out.add(new Emitter(ResourceLocation.parse(line.get("effect").getAsString()),
                        triple(line.getAsJsonArray("at")), triple(line.getAsJsonArray("way")),
                        line.has("tint") ? line.get("tint").getAsString() : null,
                        line.has("mode") ? line.get("mode").getAsString() : null));
            }
        } catch (final IOException | RuntimeException broken) {
            JsComputers.LOGGER.warn("The emitters of the chain machine {} could not be read: {}", id,
                    broken.getMessage());
            return List.of();
        }
        return List.copyOf(out);
    }

    private static double[] triple(final JsonArray array) {
        return new double[] {array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    /**
     * One emitter.
     *
     * @param effect the effect it plays
     * @param at     where on the machine, in blocks from the middle of its terminal's floor, facing north
     * @param way    which way its particles leave
     * @param tint   the channel whose material's colour it takes; none for its own colours
     * @param mode   the mode it shows only in; none for every mode
     */
    public record Emitter(ResourceLocation effect, double[] at, double[] way, @Nullable String tint,
                          @Nullable String mode) {

        public Emitter {
            at = at.clone();
            way = way.clone();
        }
    }
}
