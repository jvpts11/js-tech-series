/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.script;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.data.DataRegistries;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.material.Materials;
import dev.jstech.core.progression.IAxisStep;
import dev.jstech.core.progression.Knowledge;
import dev.jstech.core.progression.Knowledges;
import dev.jstech.core.progression.Milestone;
import dev.jstech.core.progression.Milestones;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.progression.ProgressionAxis;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.unify.Unification;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * What a modpack's scripts reach of the Core, whatever the scripting mod: KubeJS hands it to every script as
 * {@code JsCore}, and CraftTweaker's {@code mods.jscore.Core} calls the same. Teams and what they know, the milestones
 * they passed and how far they came along each axis, the materials and their forms, and which item stands for which:
 *
 * <pre>{@code
 * PlayerEvents.loggedIn(event => {
 *   const team = JsCore.teamOf(event.server, event.player.uuid)
 *   if (!JsCore.knows(event.server, team, 'mymod:basics')) JsCore.grant(event.server, team, 'mymod:basics')
 * })
 * }</pre>
 *
 * Everything is named by id, written as {@code namespace:path}; an id nothing declared is refused, naming it.
 */
public final class CoreScripts {

    /** The name scripts reach it under. */
    public static final String NAME = "JsCore";

    private CoreScripts() {
    }

    /** The team of the player {@code player}, by its id: their own when they are on none. */
    public static String teamOf(final MinecraftServer server, final UUID player) {
        return CoreTeams.teamOf(server, player);
    }

    /** Every piece of knowledge declared, by id. */
    public static List<String> knowledge() {
        return Knowledges.all().stream().map(known -> known.id().toString()).toList();
    }

    /** Whether {@code team} knows the whole of {@code knowledge}. */
    public static boolean knows(final MinecraftServer server, final String team, final String knowledge) {
        return TeamProgress.knows(server, team, knowledgeOf(knowledge).id());
    }

    /** How much of {@code knowledge} {@code team} knows, from 0 to 1. */
    public static double share(final MinecraftServer server, final String team, final String knowledge) {
        return TeamProgress.share(server, team, knowledgeOf(knowledge).id());
    }

    /** Has {@code team} know the whole of {@code knowledge}; whether it knows more now. */
    public static boolean grant(final MinecraftServer server, final String team, final String knowledge) {
        return grant(server, team, knowledge, 1.0);
    }

    /** Has {@code team} know {@code share} of {@code knowledge}, from 0 to 1, if more than it knew. */
    public static boolean grant(final MinecraftServer server, final String team, final String knowledge,
                                final double share) {
        return TeamProgress.grant(server, team, knowledgeOf(knowledge), share);
    }

    /** Has {@code team} forget {@code knowledge}. */
    public static void forget(final MinecraftServer server, final String team, final String knowledge) {
        TeamProgress.forget(server, team, knowledgeOf(knowledge).id());
    }

    /** Every milestone declared, by id. */
    public static List<String> milestones() {
        final List<String> out = new ArrayList<>();
        for (final ResourceLocation set : Milestones.sets()) {
            Milestones.of(set).forEach(milestone -> out.add(milestone.id().toString()));
        }
        return out;
    }

    /** Whether {@code team} has passed {@code milestone}. */
    public static boolean reached(final MinecraftServer server, final String team, final String milestone) {
        return TeamProgress.reached(server, team, milestoneOf(milestone));
    }

    /** Has {@code team} pass {@code milestone}; whether it had not before. */
    public static boolean reach(final MinecraftServer server, final String team, final String milestone) {
        return TeamProgress.reach(server, team, milestoneOf(milestone));
    }

    /** Every progression axis, by id. */
    public static List<String> axes() {
        return ProgressionAxes.all().stream().map(axis -> axis.id().toString()).toList();
    }

    /** The names of {@code axis}'s steps, first to last. */
    public static List<String> steps(final String axis) {
        return axisOf(axis).steps().stream().map(IAxisStep::serializedName).toList();
    }

    /** The furthest step {@code team} has reached along {@code axis}, by its name. */
    public static String step(final MinecraftServer server, final String team, final String axis) {
        return TeamProgress.reached(server, team, axisOf(axis)).serializedName();
    }

    /** Moves {@code team} to the step named {@code step} along {@code axis} if it is further; whether it moved. */
    public static boolean reachStep(final MinecraftServer server, final String team, final String axis,
                                    final String step) {
        return reachStep(server, team, axisOf(axis), step);
    }

    /** Every material of the catalogue, by id. */
    public static List<String> materials() {
        return Materials.all().stream().map(material -> material.id().toString()).toList();
    }

    /** The thing of {@code material} in {@code form} ({@code "jscore:tin"}, {@code "ingot"}), or nothing. */
    public static ItemStack material(final String material, final String form) {
        final Material found = Materials.byId(idOf(material, "material"))
                .orElseThrow(() -> unknown("material", material));
        final Form shape = Forms.byId(idOf(form.contains(":") ? form : "jscore:" + form, "form"))
                .orElseThrow(() -> unknown("form", form));
        final Optional<Supplier<? extends ItemLike>> provided = found.provided(shape);
        if (provided.isPresent()) {
            return new ItemStack(provided.get().get());
        }
        return BuiltInRegistries.ITEM.getOptional(shape.thingId(found)).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    /** The id of the item that stands for {@code item} in recipes and drops; the item itself when none does. */
    public static String unified(final String item) {
        final Item found = BuiltInRegistries.ITEM.getOptional(idOf(item, "item"))
                .orElseThrow(() -> unknown("item", item));
        return BuiltInRegistries.ITEM.getKey(Unification.unified(new ItemStack(found)).getItem()).toString();
    }

    /** Every property a material may have, by id. */
    public static List<String> properties() {
        return MaterialProperties.all().stream().map(property -> property.id().toString()).toList();
    }

    /**
     * The value of {@code property} for {@code material}, as the world's data gives it or the material itself, written
     * as JSON ({@code "5200"}, {@code "\"Cu\""}); nothing when the material has none.
     */
    public static String property(final String material, final String property) {
        final Material found = Materials.byId(idOf(material, "material"))
                .orElseThrow(() -> unknown("material", material));
        final MaterialProperty<?> declared = MaterialProperties.byId(idOf(property, "property"))
                .orElseThrow(() -> unknown("property", property));
        return propertyValue(found, declared);
    }

    /**
     * Every kind of data the Core's registries read from data packs, by id: the deposits, the machines and
     * multiblocks declared as data, the dimensions' rules, the skies, the weathers and the rest.
     */
    public static List<String> dataKinds() {
        return DataRegistries.all().stream().map(registry -> registry.id().toString()).toList();
    }

    /** The files of the kind of data {@code kind} the world loaded, by id. */
    public static List<String> dataIds(final String kind) {
        return dataOf(kind).entries().keySet().stream().map(ResourceLocation::toString).sorted().toList();
    }

    /** The file {@code id} of the kind of data {@code kind}, written as JSON; nothing when the world has none. */
    public static String data(final String kind, final String id) {
        return written(dataOf(kind), idOf(id, "file"));
    }

    /**
     * Every data map of every mod, by id: what is attached as data to the game's blocks, items, creatures and biomes,
     * the Core's among them (how blocks seal, their physical properties, the hazards of items, their density).
     */
    public static List<String> dataMaps() {
        final List<String> out = new ArrayList<>();
        RegistryManager.getDataMaps().values().forEach(maps -> maps.keySet().forEach(id -> out.add(id.toString())));
        out.sort(null);
        return out;
    }

    /**
     * The value the data map {@code map} gives {@code entry}, a block, an item or anything else of the map's registry
     * by id, written as JSON; nothing when it gives none.
     */
    public static String dataMap(final String map, final String entry) {
        final ResourceLocation mapId = idOf(map, "map");
        for (final var byRegistry : RegistryManager.getDataMaps().entrySet()) {
            final DataMapType<?, ?> found = byRegistry.getValue().get(mapId);
            if (found != null) {
                return mapValue(found, idOf(entry, "entry"));
            }
        }
        throw new IllegalArgumentException("no data map " + map + " is declared");
    }

    private static <S extends IAxisStep> boolean reachStep(final MinecraftServer server, final String team,
                                                           final ProgressionAxis<S> axis, final String step) {
        final S named = axis.byName(step);
        if (named == null) {
            throw new IllegalArgumentException("the axis " + axis.id() + " has no step " + step + "; its steps are "
                    + axis.steps().stream().map(IAxisStep::serializedName).toList());
        }
        return TeamProgress.reach(server, team, axis, named);
    }

    private static <T> String propertyValue(final Material material, final MaterialProperty<T> property) {
        return MaterialValues.value(material, property, false).map(value -> json(property.codec(), value))
                .orElse("");
    }

    private static DataRegistry<?> dataOf(final String kind) {
        final ResourceLocation id = idOf(kind, "kind");
        return DataRegistries.all().stream().filter(registry -> registry.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no kind of data " + kind + " is declared"));
    }

    private static <T> String written(final DataRegistry<T> registry, final ResourceLocation id) {
        return registry.get(id).map(value -> json(registry.codec(), value)).orElse("");
    }

    @SuppressWarnings("unchecked")
    private static <R, V> String mapValue(final DataMapType<R, V> map, final ResourceLocation entry) {
        final Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.get(map.registryKey().location());
        if (registry == null) {
            return "";
        }
        final Optional<Holder.Reference<R>> holder = registry.getHolder(entry);
        if (holder.isEmpty()) {
            throw unknown("entry", entry.toString());
        }
        final V value = holder.get().getData(map);
        return value == null ? "" : json(map.codec(), value);
    }

    /* A value as its codec writes it to JSON, with the server's registries where there is a server. */
    private static <T> String json(final Codec<T> codec, final T value) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        final DynamicOps<JsonElement> ops = server == null ? JsonOps.INSTANCE
                : server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        return codec.encodeStart(ops, value).result().map(JsonElement::toString).orElse("");
    }

    private static Knowledge knowledgeOf(final String id) {
        return Knowledges.byId(idOf(id, "knowledge")).orElseThrow(() -> unknown("knowledge", id));
    }

    private static Milestone milestoneOf(final String id) {
        return Milestones.byId(idOf(id, "milestone")).orElseThrow(() -> unknown("milestone", id));
    }

    private static ProgressionAxis<?> axisOf(final String id) {
        final ProgressionAxis<?> axis = ProgressionAxes.byId(idOf(id, "axis"));
        if (axis == null) {
            throw unknown("axis", id);
        }
        return axis;
    }

    private static ResourceLocation idOf(final String written, final String what) {
        final ResourceLocation id = ResourceLocation.tryParse(written);
        if (id == null) {
            throw new IllegalArgumentException("the " + what + " " + written + " is not an id; write it as "
                    + "namespace:path");
        }
        return id;
    }

    private static IllegalArgumentException unknown(final String what, final String id) {
        return new IllegalArgumentException("no " + what + " " + id + " is declared");
    }
}
