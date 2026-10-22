/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigSide;
import dev.jstech.core.config.format.ConfigFormats;
import dev.jstech.core.worldgen.modify.StructureSetModifier;
import dev.jstech.core.worldgen.modify.WorldgenModifiers;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.ModifiableStructureInfo;
import net.neoforged.neoforge.common.world.StructureModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * The structures of every mod as a server's operator tunes them, in one settings file, {@code jstech-structures.toml}
 * in the game's {@code config} folder, read as the game starts: structures turned off, structure sets spread out or
 * drawn closer, and the biomes a structure may stand in. A structure is named by its id, or every structure of a mod
 * by {@code "mod:*"}. {@code /jstech dump structures} lists every structure set with its placement and its
 * structures, so the operator knows the names. Worlds made after a change take it; chunks already made keep what
 * they have.
 *
 * <p>Turning off and biomes are a structure modifier of NeoForge's the Core ships
 * ({@code data/jscore/neoforge/structure_modifier/operator.json}), spacing a change to each set's placement as the
 * server starts ({@link WorldgenModifiers}), after the datapacks' own modifiers.
 */
public final class OperatorStructures {

    /** The section of the file. */
    public static final String STRUCTURES = "structures";
    /** The id the operator's changes are reported under. */
    public static final ResourceLocation OPERATOR = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "operator");
    /** How far a set may be spread out or drawn closer. */
    public static final double LEAST_SPACING = 0.1;
    public static final double MOST_SPACING = 16;
    /** The structures that are never made. */
    public static final ConfigKey<List<String>> OFF = ConfigKey.of(STRUCTURES + ".off", Codec.STRING.listOf(),
            List.<String>of())
            .comment("The structures never made, by their ids, as in \"minecraft:pillager_outpost\", or every "
                    + "structure of a mod as \"somemod:*\". /jstech dump structures lists them.")
            .named("Structures turned off");
    /** How far apart each structure set's structures are, as a share of what the set says. */
    public static final ConfigKey<Map<String, Double>> SPACING = ConfigKey.of(STRUCTURES + ".spacing",
            Codec.unboundedMap(Codec.STRING, Codec.doubleRange(LEAST_SPACING, MOST_SPACING)), Map.<String, Double>of())
            .comment("How far apart the structures of each structure set are, as a share of their own spacing, by "
                    + "the set's id, as in \"minecraft:villages\" = 2.0 (half as many, twice as far apart), or "
                    + "every set of a mod as \"somemod:*\". Only sets placed by a spacing and a separation change.")
            .named("Structure spacing");
    /** The biomes a structure may stand in, over its own. */
    public static final ConfigKey<List<String>> BIOMES = ConfigKey.of(STRUCTURES + ".biomes", Codec.STRING.listOf(),
            List.<String>of())
            .comment("The biomes a structure may stand in, over those it says, each written \"structure = biome, "
                    + "#biome_tag, ...\", as in \"minecraft:igloo = minecraft:snowy_plains, #minecraft:is_taiga\".")
            .named("Structure biomes");
    /** The file, each setting passed into what it sets. */
    public static final ConfigFile FILE = ConfigFile.builder("jstech-structures", ConfigSide.COMMON, ConfigFormats.TOML)
            .comment("The structures of every mod, as a server's operator tunes them. Read as the game starts; worlds "
                    + "made afterwards take a change, and chunks already made keep what they have.",
                    "Values outside their range are pulled to the nearer end when the file is read.")
            .sectionComment(STRUCTURES, "Turning structures off, spreading them out and choosing their biomes.")
            .sectionNamed(STRUCTURES, "Structures")
            .key(OFF, OperatorStructures::setOff)
            .key(SPACING, OperatorStructures::setSpacing)
            .key(BIOMES, OperatorStructures::setBiomes)
            .build();

    private static final DeferredRegister<MapCodec<? extends StructureModifier>> MODIFIERS = DeferredRegister.create(
            NeoForgeRegistries.Keys.STRUCTURE_MODIFIER_SERIALIZERS, JsCore.MODID);
    /** {@code jscore:operator}: the structure modifier that turns structures off and sets their biomes. */
    public static final DeferredHolder<MapCodec<? extends StructureModifier>, MapCodec<Modifier>> MODIFIER =
            MODIFIERS.register("operator", () -> Modifier.CODEC);
    private static final String ALL_OF_A_MOD = ":*";
    private static final String TAG = "#";
    /* The fields of a placement that spreads its structures by spacing and separation, the game's and the Core's. */
    private static final String SPACING_FIELD = "spacing";
    private static final String SEPARATION_FIELD = "separation";

    private static volatile List<String> off = List.of();
    private static volatile Map<String, Double> spacing = Map.of();
    private static volatile List<String> biomes = List.of();

    private OperatorStructures() {
    }

    /** Registers the structure modifier, from the Core's constructor. */
    public static void register(final IEventBus modEventBus) {
        MODIFIERS.register(modEventBus);
    }

    public static void setOff(final List<String> ids) {
        off = List.copyOf(ids);
    }

    public static void setSpacing(final Map<String, Double> shares) {
        spacing = Map.copyOf(shares);
    }

    public static void setBiomes(final List<String> entries) {
        biomes = List.copyOf(entries);
    }

    /** Whether {@code pattern} names {@code id}: the id itself, or {@code "mod:*"} for its mod. */
    public static boolean names(final String pattern, final ResourceLocation id) {
        final String said = pattern.trim().toLowerCase(Locale.ROOT);
        return said.endsWith(ALL_OF_A_MOD) ? id.getNamespace().equals(said.substring(0, said.length() - 2))
                : said.equals(id.toString());
    }

    /** Whether the operator turned {@code structure} off. */
    public static boolean isOff(final ResourceLocation structure) {
        return off.stream().anyMatch(pattern -> names(pattern, structure));
    }

    /** The share of its own spacing {@code set}'s structures stand apart at: the operator's, else 1. */
    public static double spacingOf(final ResourceLocation set) {
        Optional<Double> byMod = Optional.empty();
        for (final Map.Entry<String, Double> said : spacing.entrySet()) {
            if (said.getKey().trim().equals(set.toString())) {
                return said.getValue();
            }
            if (names(said.getKey(), set)) {
                byMod = Optional.of(said.getValue());
            }
        }
        return byMod.orElse(1.0);
    }

    /**
     * The biomes the operator gives {@code structure}, as the words after its {@code =} in their order (a biome's id,
     * or a tag's after {@code #}); none when the operator gives it none.
     */
    public static Optional<List<String>> biomesOf(final ResourceLocation structure) {
        for (final String entry : biomes) {
            final int equals = entry.indexOf('=');
            if (equals > 0 && names(entry.substring(0, equals), structure)) {
                final List<String> named = new ArrayList<>();
                for (final String word : entry.substring(equals + 1).split(",")) {
                    if (!word.isBlank()) {
                        named.add(word.trim());
                    }
                }
                return Optional.of(named);
            }
        }
        return Optional.empty();
    }

    /**
     * The spacing and the separation of a set, {@code share} times as far apart: never closer than a chunk, the
     * separation always under the spacing.
     */
    public static Spacing spaced(final int spacing, final int separation, final double share) {
        final int apart = Math.max(1, (int) Math.round(spacing * share));
        final int least = Math.max(0, Math.min(apart - 1, (int) Math.round(separation * share)));
        return new Spacing(apart, least);
    }

    /**
     * Spreads out or draws closer every structure set the operator names, as the server starts; what was done for
     * each.
     */
    public static List<WorldgenModifiers.Made> space(final RegistryAccess registries) {
        final List<WorldgenModifiers.Made> out = new ArrayList<>();
        final Registry<StructureSet> sets = registries.registryOrThrow(Registries.STRUCTURE_SET);
        for (final Holder.Reference<StructureSet> set : sets.holders().toList()) {
            final double share = spacingOf(set.key().location());
            final Optional<JsonObject> written = share == 1 ? Optional.empty()
                    : StructureSetModifier.placementOf(set.value(), registries);
            if (written.isEmpty() || !written.get().has(SPACING_FIELD) || !written.get().has(SEPARATION_FIELD)) {
                continue;
            }
            final Spacing now = spaced(written.get().get(SPACING_FIELD).getAsInt(),
                    written.get().get(SEPARATION_FIELD).getAsInt(), share);
            final JsonObject fields = new JsonObject();
            fields.addProperty(SPACING_FIELD, now.spacing());
            fields.addProperty(SEPARATION_FIELD, now.separation());
            final List<String> problems = new ArrayList<>();
            final StructureSet changed = StructureSetModifier.withPlacement(set.value(), fields, registries, problems);
            if (problems.isEmpty()) {
                WorldgenModifiers.rebind(set, changed);
            }
            out.add(new WorldgenModifiers.Made(OPERATOR, StructureSetModifier.KIND, set.key().location(),
                    problems.isEmpty(), problems));
        }
        return out;
    }

    /* The biomes {@code words} name, from the biomes {@code server} holds; a word naming nothing is passed over. */
    private static HolderSet<Biome> biomesNamed(final MinecraftServer server, final List<String> words) {
        final Registry<Biome> registry = server.registryAccess().registryOrThrow(Registries.BIOME);
        final List<Holder<Biome>> found = new ArrayList<>();
        for (final String word : words) {
            if (word.startsWith(TAG)) {
                final ResourceLocation tag = ResourceLocation.tryParse(word.substring(1));
                if (tag != null) {
                    registry.getTag(TagKey.create(Registries.BIOME, tag)).ifPresent(named -> named.forEach(
                            found::add));
                }
            } else {
                final ResourceLocation id = ResourceLocation.tryParse(word);
                if (id != null) {
                    registry.getHolder(ResourceKey.create(Registries.BIOME, id)).ifPresent(found::add);
                }
            }
        }
        return HolderSet.direct(found);
    }

    /**
     * The spacing of a set spread at random.
     *
     * @param spacing    how many chunks apart its structures are tried
     * @param separation how many chunks at least stand between two
     */
    public record Spacing(int spacing, int separation) {
    }

    /** {@code jscore:operator}: turns the structures the operator names off, and gives them the biomes it says. */
    public enum Modifier implements StructureModifier {

        /** The one there is; what it does is the settings file's. */
        INSTANCE;

        public static final MapCodec<Modifier> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public void modify(final Holder<Structure> structure, final StructureModifier.Phase phase,
                           final ModifiableStructureInfo.StructureInfo.Builder builder) {
            if (phase != StructureModifier.Phase.MODIFY || structure.unwrapKey().isEmpty()) {
                return;
            }
            final ResourceLocation id = structure.unwrapKey().get().location();
            if (isOff(id)) {
                builder.getStructureSettings().setBiomes(HolderSet.direct(List.of()));
                return;
            }
            final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            final Optional<List<String>> named = biomesOf(id);
            if (named.isPresent() && server != null) {
                builder.getStructureSettings().setBiomes(biomesNamed(server, named.get()));
            }
        }

        @Override
        public MapCodec<? extends StructureModifier> codec() {
            return CODEC;
        }
    }
}
