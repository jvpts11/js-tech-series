/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;
import org.slf4j.Logger;

/**
 * Where a starship engine part's model blows out, read from the file beside it,
 * {@code jsmodels/ship/<model>.engines.json}, written with the model from the shapes it is drawn with: each bell's
 * mouth and each air engine nozzle's exit, in the model's own space (the ship's centre line at x 0, the hull's floor
 * at y 0, the part's anchor at z 0, its tail toward z), the way it blows and how wide it is. A part whose model has no
 * such file blows nothing.
 */
final class ShipEngineExhausts {

    private static final Logger LOG = LogUtils.getLogger();
    private static final Codec<List<Exhaust>> FILE = RecordCodecBuilder.create(instance -> instance.group(
            Exhaust.CODEC.listOf().fieldOf("exhausts").forGetter(exhausts -> exhausts)
    ).apply(instance, List::copyOf));
    private static final Map<ResourceLocation, List<Exhaust>> READ = new HashMap<>();
    private static long readAt = -1;

    private ShipEngineExhausts() {
    }

    /** Where the model {@code model} blows out; none for a model with no engines. */
    static List<Exhaust> of(final ResourceLocation model) {
        if (readAt != KitModels.reloadedAt()) {
            READ.clear();
            readAt = KitModels.reloadedAt();
        }
        return READ.computeIfAbsent(model, ShipEngineExhausts::read);
    }

    private static List<Exhaust> read(final ResourceLocation model) {
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(model.getNamespace(),
                "jsmodels/" + model.getPath() + ".engines.json");
        final Optional<Resource> found = Minecraft.getInstance().getResourceManager().getResource(file);
        if (found.isEmpty()) {
            return List.of();
        }
        try (Reader reader = found.get().openAsReader()) {
            return FILE.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader)).resultOrPartial(problem -> LOG.warn(
                    "The engines {} cannot be read: {}", file, problem)).orElse(List.of());
        } catch (final IOException | RuntimeException wrong) {
            LOG.warn("The engines {} cannot be read", file, wrong);
            return List.of();
        }
    }

    /**
     * Where an engine blows out.
     *
     * @param kind   what blows there
     * @param at     the middle of its mouth, in the model's own space, in blocks
     * @param way    the way it blows, in the model's own space
     * @param radius how wide its mouth is, in blocks
     */
    public record Exhaust(Kind kind, Vector3f at, Vector3f way, float radius) {

        public static final Codec<Exhaust> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StableCodecs.byName(Kind.class).fieldOf("kind").forGetter(Exhaust::kind),
                ExtraCodecs.VECTOR3F.fieldOf("at").forGetter(Exhaust::at),
                ExtraCodecs.VECTOR3F.fieldOf("way").forGetter(Exhaust::way),
                Codec.FLOAT.fieldOf("radius").forGetter(Exhaust::radius)
        ).apply(instance, Exhaust::new));

        public Exhaust {
            at = new Vector3f(at);
            way = new Vector3f(way).normalize();
        }

        /** What blows out of an engine's mouth. */
        public enum Kind implements IStableName {
            /** A main engine's bell, or its magnetic nozzle: glowing at its mouth, its jet where it pushes. */
            BELL("bell"),
            /** An air engine's nozzle, swung back for flight: its exhaust where it breathes. */
            NOZZLE("nozzle");

            private final String serializedName;

            Kind(final String serializedName) {
                this.serializedName = serializedName;
            }

            @Override
            public String serializedName() {
                return this.serializedName;
            }
        }
    }
}
