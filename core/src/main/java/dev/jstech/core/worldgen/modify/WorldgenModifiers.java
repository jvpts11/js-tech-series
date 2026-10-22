/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.modify;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import dev.jstech.core.worldgen.structure.OperatorStructures;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * Worldgen modifiers: changes to the world generation of the game and of every mod written as data, in a datapack's
 * {@code data/<namespace>/jstech/worldgen_modifiers/<name>.json}, made as the server starts and before its worlds are
 * made. The Core's kinds put a new density function in the place of one ({@link DensityFunctionModifier}) and change
 * a structure set's placement and what it holds ({@link StructureSetModifier}); a biome's features and spawns and a
 * structure's biomes are NeoForge's own biome and structure modifiers'. Modifiers are made in the order of their
 * {@code "priority"} (0 when unsaid, the lowest first), then of their ids; a modifier that cannot be made says why in
 * the log and leaves what it named as it was. They take hold for worlds made after the server starts: a
 * {@code /reload} reads them again only for the next start.
 */
public final class WorldgenModifiers {

    /** The folder of a datapack's data the modifiers are read from. */
    public static final String FOLDER = "jstech/worldgen_modifiers";
    private static final String JSON = ".json";
    private static final String PRIORITY = "priority";
    private static final Map<ResourceLocation, MapCodec<? extends IWorldgenModifier>> KINDS = new ConcurrentHashMap<>();
    /** A modifier as its file writes it, its kind named by {@code "type"}. */
    public static final Codec<IWorldgenModifier> CODEC = ResourceLocation.CODEC.partialDispatch("type",
            modifier -> DataResult.success(modifier.kind()), WorldgenModifiers::kindCodec);
    private static final List<Made> MADE = new CopyOnWriteArrayList<>();

    static {
        declareKind(DensityFunctionModifier.KIND, DensityFunctionModifier.CODEC);
        declareKind(StructureSetModifier.KIND, StructureSetModifier.CODEC);
    }

    private WorldgenModifiers() {
    }

    /** Declares a kind of worldgen modifier a datapack names by {@code kind}, read with {@code codec}. */
    public static void declareKind(final ResourceLocation kind, final MapCodec<? extends IWorldgenModifier> codec) {
        if (KINDS.putIfAbsent(kind, codec) != null) {
            throw new IllegalStateException("a kind of worldgen modifier is already called " + kind);
        }
    }

    /** Makes the datapacks' modifiers, and then the operator's changes to structures, as every server starts. */
    public static void listen() {
        // After NeoForge's modifiers and the settings of the world, which load at the normal priority.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, ServerAboutToStartEvent.class,
                event -> start(event.getServer()));
    }

    /**
     * Puts {@code value} in the place of what {@code holder} held, so everything that names it by its id takes the
     * new one: how a kind of modifier makes its change in a frozen registry, as the server starts. The registry still
     * finds the old value's id from the old value, and none from the new one.
     */
    public static <T> void rebind(final Holder.Reference<T> holder, final T value) {
        holder.bindValue(value);
    }

    /** What was made as the server last started, in order, with what went wrong. */
    public static List<Made> made() {
        return List.copyOf(MADE);
    }

    /**
     * Reads every modifier the datapacks of {@code resources} hold, in the order they are made, what cannot be read
     * added to {@code problems}.
     */
    public static List<Read> read(final ResourceManager resources, final RegistryAccess registries,
                                  final List<String> problems) {
        final RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        final List<Read> out = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, Resource> file : resources.listResources(FOLDER,
                path -> path.getPath().endsWith(JSON)).entrySet()) {
            final ResourceLocation id = idOf(file.getKey());
            try (Reader reader = file.getValue().openAsReader()) {
                final JsonElement json = JsonParser.parseReader(reader);
                final int priority = json.isJsonObject() && json.getAsJsonObject().has(PRIORITY)
                        ? json.getAsJsonObject().get(PRIORITY).getAsInt() : 0;
                final DataResult<IWorldgenModifier> parsed = CODEC.parse(ops, json);
                parsed.error().ifPresent(wrong -> problems.add(id + ": " + wrong.message()));
                parsed.result().ifPresent(modifier -> out.add(new Read(id, priority, modifier)));
            } catch (final IOException | RuntimeException e) {
                problems.add(id + ": " + e.getMessage());
            }
        }
        out.sort(Comparator.comparingInt(Read::priority).thenComparing(read -> read.id().toString()));
        return out;
    }

    /** Makes each of {@code modifiers} in turn in {@code registries}; what each did. */
    public static List<Made> make(final List<Read> modifiers, final RegistryAccess registries) {
        final List<Made> out = new ArrayList<>();
        for (final Read read : modifiers) {
            final List<String> problems = new ArrayList<>();
            final boolean done = read.modifier().apply(registries, problems);
            out.add(new Made(read.id(), read.modifier().kind(), read.modifier().changes(), done, problems));
        }
        return out;
    }

    private static void start(final MinecraftServer server) {
        final List<String> unread = new ArrayList<>();
        final List<Made> made = new ArrayList<>(make(read(server.getResourceManager(), server.registryAccess(),
                unread), server.registryAccess()));
        made.addAll(OperatorStructures.space(server.registryAccess()));
        MADE.clear();
        MADE.addAll(made);
        unread.forEach(problem -> JsCore.LOGGER.warn("A worldgen modifier could not be read: {}", problem));
        for (final Made one : made) {
            if (!one.done()) {
                JsCore.LOGGER.warn("The worldgen modifier {} left {} as it was: {}", one.id(), one.target(),
                        String.join("; ", one.problems()));
            }
        }
    }

    private static DataResult<MapCodec<? extends IWorldgenModifier>> kindCodec(final ResourceLocation kind) {
        final MapCodec<? extends IWorldgenModifier> codec = KINDS.get(kind);
        return codec == null ? DataResult.error(() -> "no kind of worldgen modifier is called " + kind + "; there are "
                + KINDS.keySet()) : DataResult.success(codec);
    }

    /* A file's id: its namespace, and its path inside the folder with no ending. */
    private static ResourceLocation idOf(final ResourceLocation file) {
        final String path = file.getPath();
        return ResourceLocation.fromNamespaceAndPath(file.getNamespace(),
                path.substring(FOLDER.length() + 1, path.length() - JSON.length()));
    }

    /**
     * A modifier read from its file.
     *
     * @param id       its file's id
     * @param priority where it stands in the order they are made
     * @param modifier what it changes
     */
    public record Read(ResourceLocation id, int priority, IWorldgenModifier modifier) {
    }

    /**
     * A modifier made, or not.
     *
     * @param id       its file's id, or the operator's setting it came from
     * @param kind     its kind
     * @param target   what it changed
     * @param done     whether the change was made
     * @param problems why not
     */
    public record Made(ResourceLocation id, ResourceLocation kind, ResourceLocation target, boolean done,
                       List<String> problems) {

        public Made {
            problems = List.copyOf(problems);
        }
    }
}
