/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.ServerState;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.util.thread.EffectiveSide;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus;

/**
 * The names given in this world to materials that can be named, as the discoverer of an element names it: kept with
 * the world and sent to every player, so the material and all its things take the name for everyone, in every
 * language, since a name is a name. Which names are allowed, and who may give one, is for the mod that lets its
 * materials be named to say; here they are only kept.
 */
public final class MaterialNames {

    /** The names given, by material id. */
    public static final ServerState<Map<ResourceLocation, Given>> NAMES = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "material_names"),
            Codec.unboundedMap(ResourceLocation.CODEC, Given.CODEC), Map.<ResourceLocation, Given>of())
            .synced(ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, Given.STREAM_CODEC))
            .server();

    private MaterialNames() {
    }

    /** Keeps the names with the world, from the Core's constructor. */
    @ApiStatus.Internal
    public static void declare() {
        CoreStates.register(NAMES);
    }

    /**
     * The name given to {@code material} in this world, on the side this is asked on: the player's game reads what the
     * server sent it. None for a material that cannot be named, or with no world.
     */
    public static Optional<Given> given(final Material material) {
        if (!material.nameable()) {
            return Optional.empty();
        }
        if (EffectiveSide.get().isClient()) {
            return Optional.ofNullable(NAMES.client().get(material.id()));
        }
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? Optional.empty() : Optional.ofNullable(NAMES.get(server).get(material.id()));
    }

    /** Every name given in the world {@code server} runs. */
    public static Map<ResourceLocation, Given> all(final MinecraftServer server) {
        return NAMES.get(server);
    }

    /**
     * Gives {@code material} the name {@code given} in the world {@code server} runs, and sends it to every player.
     *
     * @throws IllegalArgumentException when the material cannot be named
     */
    public static void give(final MinecraftServer server, final Material material, final Given given) {
        if (!material.nameable()) {
            throw new IllegalArgumentException(material + " cannot be named");
        }
        NAMES.update(server, names -> {
            final Map<ResourceLocation, Given> out = new HashMap<>(names);
            out.put(material.id(), Objects.requireNonNull(given, "given"));
            return Map.copyOf(out);
        });
    }

    /** Takes back the name given to {@code material}; false when it had none. */
    public static boolean take(final MinecraftServer server, final Material material) {
        if (!NAMES.get(server).containsKey(material.id())) {
            return false;
        }
        NAMES.update(server, names -> {
            final Map<ResourceLocation, Given> out = new HashMap<>(names);
            out.remove(material.id());
            return Map.copyOf(out);
        });
        return true;
    }

    /**
     * A name given in a world.
     *
     * @param name   the name: "Joaonium"
     * @param symbol its symbol: "Jo"
     * @param by     the player who gave it
     */
    public record Given(String name, String symbol, UUID by) {

        /** How it is kept with the world. */
        public static final Codec<Given> CODEC = RecordCodecBuilder.create(given -> given.group(
                Codec.STRING.fieldOf("name").forGetter(Given::name),
                Codec.STRING.fieldOf("symbol").forGetter(Given::symbol),
                UUIDUtil.CODEC.fieldOf("by").forGetter(Given::by)
        ).apply(given, Given::new));
        /** How it is sent. */
        public static final StreamCodec<ByteBuf, Given> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Given::name, ByteBufCodecs.STRING_UTF8, Given::symbol,
                UUIDUtil.STREAM_CODEC, Given::by, Given::new);

        public Given {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(symbol, "symbol");
            Objects.requireNonNull(by, "by");
        }
    }
}
