/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import dev.jstech.core.connect.texture.CtmLogic;
import dev.jstech.core.connect.texture.CtmTexture;
import dev.jstech.core.connect.texture.CtmTextures;
import dev.jstech.core.connect.texture.OptifineRule;
import dev.jstech.core.connect.texture.OptifineRules;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

/**
 * The connected textures the resource packs in use say, read each time the game's resources are: OptiFine's and
 * Continuity's rules under {@code optifine/ctm} and {@code mcpatcher/ctm}, in the order those mods weigh them (rules
 * by texture before rules by block, the heavier first, then by file name); CTM's {@code ctm} sections of textures'
 * {@code .mcmeta} files, the logics written as data under {@code ctm_logic} besides the Core's own, and CTM's
 * overrides in block models. A file that does not read is left out, and the log says why.
 *
 * @param byTile      the rules by texture, by the texture's id, in the order they are tried
 * @param byBlock     the rules by block, by the block's id, in order
 * @param byTag       the rules by a tag of blocks, in order, tried on every block
 * @param ctm         CTM's textures, by the texture's id
 * @param logics      CTM's logics written as data, by id
 * @param overrides   CTM's overrides, by the model's id, then by tint index
 * @param tileFiles   every file a tile or a companion is drawn from, as {@code namespace:path.png}
 */
record PackRules(Map<String, List<OptifineRule>> byTile, Map<String, List<OptifineRule>> byBlock,
                 List<OptifineRule> byTag, Map<String, CtmTexture> ctm, Map<String, CtmLogic> logics,
                 Map<String, Map<Integer, CtmTexture>> overrides, Set<String> tileFiles) {

    /** No rules: what the game draws while another mod draws connected textures, or none are in the packs. */
    static final PackRules NONE = new PackRules(Map.of(), Map.of(), List.of(), Map.of(), Map.of(), Map.of(),
            Set.of());

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<String> FOLDERS = List.of("optifine/ctm", "mcpatcher/ctm");
    private static final String PROPERTIES = ".properties";
    private static final String PNG = ".png";
    /* A texture's ctm section, as the game reads the sections of a texture's metadata: as it is written. */
    private static final MetadataSectionSerializer<JsonObject> SECTION = new MetadataSectionSerializer<>() {

        @Override
        public String getMetadataSectionName() {
            return "ctm";
        }

        @Override
        public JsonObject fromJson(final JsonObject json) {
            return json;
        }
    };
    private static final String MODELS = "models";
    private static final String LOGICS = "ctm_logic";
    private static final String OVERRIDES = "ctm_overrides";
    private static final Comparator<OptifineRule> ORDER = Comparator.comparingInt(OptifineRule::weight).reversed()
            .thenComparing(OptifineRule::file);

    /* The rules read last, swapped in whole. */
    private static volatile PackRules current = NONE;

    PackRules {
        byTile = Map.copyOf(byTile);
        byBlock = Map.copyOf(byBlock);
        byTag = List.copyOf(byTag);
        ctm = Map.copyOf(ctm);
        logics = Map.copyOf(logics);
        overrides = Map.copyOf(overrides);
        tileFiles = Set.copyOf(tileFiles);
    }

    /** The rules read last. */
    static PackRules current() {
        return current;
    }

    /** Whether there are none. */
    boolean isEmpty() {
        return this.byTile.isEmpty() && this.byBlock.isEmpty() && this.byTag.isEmpty() && this.ctm.isEmpty()
                && this.overrides.isEmpty();
    }

    /** Reads every rule of the packs in {@code resources}, and keeps them as the current ones. */
    static PackRules load(final ResourceManager resources) {
        final List<OptifineRule> rules = new ArrayList<>();
        for (final String folder : FOLDERS) {
            resources.listResources(folder, id -> id.getPath().endsWith(PROPERTIES)).forEach((id, resource) ->
                    read(id, resource).ifPresent(rules::add));
        }
        final Map<String, List<OptifineRule>> byTile = new HashMap<>();
        final Map<String, List<OptifineRule>> byBlock = new HashMap<>();
        final List<OptifineRule> byTag = new ArrayList<>();
        final Set<String> files = new LinkedHashSet<>();
        rules.sort(ORDER);
        for (final OptifineRule rule : rules) {
            if (rule.byTiles()) {
                rule.matchTiles().forEach(tile -> byTile.computeIfAbsent(tile, key -> new ArrayList<>()).add(rule));
            } else {
                for (final OptifineRule.BlockFilter block : rule.matchBlocks()) {
                    if (block.tag()) {
                        byTag.add(rule);
                    } else {
                        byBlock.computeIfAbsent(block.id(), key -> new ArrayList<>()).add(rule);
                    }
                }
            }
            for (final OptifineRule.Tile tile : rule.tiles()) {
                final String file = OptifineRules.tileFile(rule.file(), tile);
                if (!file.isEmpty()) {
                    files.add(file);
                }
            }
        }
        final Map<String, CtmTexture> ctm = ctmTextures(resources, files);
        final PackRules read = new PackRules(byTile, byBlock, byTag.stream().distinct().toList(), ctm,
                logics(resources), overrides(resources), files);
        current = read;
        return read;
    }

    /** Forgets every rule, as when another mod draws connected textures. */
    static void clear() {
        current = NONE;
    }

    /** The texture id of a texture file: {@code namespace:textures/path.png} is {@code namespace:path}. */
    static String textureOf(final ResourceLocation file) {
        final String path = file.getPath();
        return file.getNamespace() + ":" + path.substring("textures/".length(), path.length() - PNG.length());
    }

    /** The file a texture id is drawn from. */
    static String fileOf(final String texture) {
        final ResourceLocation id = ResourceLocation.parse(texture);
        return id.getNamespace() + ":textures/" + id.getPath() + ".png";
    }

    private static Optional<OptifineRule> read(final ResourceLocation id, final Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            return Optional.of(OptifineRules.parse(id.toString(), reader.lines().collect(Collectors.joining("\n"))));
        } catch (final IOException | RuntimeException e) {
            LOGGER.warn("Connected texture {} is left out: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    /*
     * Every texture whose metadata has a ctm section, its companions and proxies added to the files to draw. The game
     * reads a texture's .mcmeta as the texture's metadata, so it is asked of each texture rather than listed.
     */
    private static Map<String, CtmTexture> ctmTextures(final ResourceManager resources, final Set<String> files) {
        final Map<String, CtmTexture> out = new HashMap<>();
        resources.listResources("textures", id -> id.getPath().endsWith(PNG)).forEach((id, resource) -> {
            try {
                resource.metadata().getSection(SECTION).map(CtmTextures::of).ifPresent(texture -> {
                    out.put(textureOf(id), texture);
                    texture.textures().forEach(companion -> files.add(fileOf(withNamespace(companion,
                            id.getNamespace()))));
                    texture.proxy().ifPresent(proxy -> files.add(fileOf(withNamespace(proxy, id.getNamespace()))));
                });
            } catch (final IOException | RuntimeException e) {
                LOGGER.warn("Connected texture {} is left out: {}", id, e.getMessage());
            }
        });
        return out;
    }

    /* The logics written as data: the Core's own OptiFine layout, then every pack's, which may write over it. */
    private static Map<String, CtmLogic> logics(final ResourceManager resources) {
        final Map<String, CtmLogic> out = new HashMap<>();
        out.put(CtmLogic.OPTIFINE_FULL, CtmLogic.optifineFull());
        resources.listResources(LOGICS, id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (BufferedReader reader = resource.openAsReader()) {
                final String path = id.getPath();
                out.put(id.getNamespace() + ":" + path.substring(LOGICS.length() + 1, path.length()
                        - ".json".length()), CtmLogic.parse(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (final IOException | RuntimeException e) {
                LOGGER.warn("Connected texture logic {} is left out: {}", id, e.getMessage());
            }
        });
        return out;
    }

    /* The overrides of every block model that has them; a model file without the word is not read as JSON. */
    private static Map<String, Map<Integer, CtmTexture>> overrides(final ResourceManager resources) {
        final Map<String, Map<Integer, CtmTexture>> out = new HashMap<>();
        resources.listResources(MODELS, id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
            try (BufferedReader reader = resource.openAsReader()) {
                final String text = reader.lines().collect(Collectors.joining("\n"));
                if (!text.contains(OVERRIDES)) {
                    return;
                }
                final Map<Integer, CtmTexture> overrides = CtmTextures.overridesOf(JsonParser.parseString(text)
                        .getAsJsonObject());
                if (!overrides.isEmpty()) {
                    final String path = id.getPath();
                    out.put(id.getNamespace() + ":" + path.substring(MODELS.length() + 1, path.length()
                            - ".json".length()), overrides);
                }
            } catch (final IOException | RuntimeException e) {
                LOGGER.warn("Connected texture overrides of {} are left out: {}", id, e.getMessage());
            }
        });
        return out;
    }

    /** A texture named in a pack, given the namespace of the file it was named in when it names none. */
    static String withNamespace(final String texture, final String namespace) {
        return texture.contains(":") ? texture : namespace + ":" + texture;
    }
}
