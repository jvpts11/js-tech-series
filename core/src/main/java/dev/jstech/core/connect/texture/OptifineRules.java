/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the {@code .properties} files resource packs write for OptiFine's and Continuity's connected textures, under
 * {@code optifine/ctm} (or {@code mcpatcher/ctm}), into {@link OptifineRule}s, as those mods read them: the methods
 * by their names and their old ones ({@code glass}, {@code bookshelf}, {@code sandstone}), the tiles by index, range,
 * name and path, the blocks with the values of their properties, the faces, biomes and heights, and the rules a file's
 * name gives when it names no textures or blocks of its own. Besides, a block may be a tag ({@code #c:glass_blocks})
 * wherever blocks are named, and any method may name the blocks and textures it joins. Pure.
 */
public final class OptifineRules {

    private static final String DEFAULT_NAMESPACE = "minecraft";
    private static final String SUFFIX = ".properties";
    private static final String PNG = ".png";
    private static final String BLOCK_PREFIX = "block_";
    private static final String TEXTURES = "textures/";
    private static final String TILDE = "~/";
    private static final String OPTIFINE_FOLDER = "optifine/";
    private static final int MOST_LOOPS = 9;
    private static final Pattern RANGE = Pattern.compile("(\\d+)-(\\d+)");
    private static final Pattern HEIGHTS = Pattern.compile("(\\(-?\\d+\\)|-?\\d+)(?:-(\\(-?\\d+\\)|-?\\d+))?");
    private static final Pattern OVERRIDE = Pattern.compile("ctm\\.(\\d+)");
    private static final Map<String, OptifineRule.Method> METHODS = methods();
    private static final Map<String, Integer> FACES = faces();

    private OptifineRules() {
    }

    /**
     * The rule the file {@code file} ({@code namespace:path}, its path ending in {@code .properties}) says in
     * {@code text}.
     *
     * @throws IllegalArgumentException when it names no method or one not known, too few tiles, or a value that does
     *                                  not read
     */
    public static OptifineRule parse(final String file, final String text) {
        final Properties keys = new Properties();
        try {
            keys.load(new StringReader(text));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final String name = baseName(file);
        final String methodWord = value(keys, "method").orElse("default").toLowerCase(Locale.ROOT);
        final OptifineRule.Method method = METHODS.get(methodWord);
        if (method == null) {
            throw new IllegalArgumentException(file + " names a method no pack knows: " + methodWord);
        }
        final List<OptifineRule.Tile> tiles = tiles(value(keys, "tiles").orElseThrow(
                () -> new IllegalArgumentException(file + " names no tiles")));
        final OptifineRule.Random random = random(file, keys);
        requireTiles(file, method, tiles.size(), random);
        List<String> matchTiles = words(keys, "matchTiles").stream().map(OptifineRules::tileId).toList();
        List<OptifineRule.BlockFilter> matchBlocks = words(keys, "matchBlocks").stream()
                .map(OptifineRules::blockFilter).toList();
        if (matchTiles.isEmpty() && matchBlocks.isEmpty()) {
            // A file that names neither is named for what it draws on: block_<name> for a block, else a texture.
            if (name.startsWith(BLOCK_PREFIX)) {
                matchBlocks = List.of(blockFilter(name.substring(BLOCK_PREFIX.length())));
            } else {
                matchTiles = List.of(tileId(name));
            }
        }
        final OptifineRule.Connect connect = value(keys, "connect").map(word -> connect(file, word))
                .orElse(matchTiles.isEmpty() ? OptifineRule.Connect.BLOCK : OptifineRule.Connect.TILE);
        return new OptifineRule(file, method, tiles, matchTiles, matchBlocks, connect,
                words(keys, "connectBlocks").stream().map(OptifineRules::blockFilter).toList(),
                words(keys, "connectTiles").stream().map(OptifineRules::tileId).toList(), faces(file, keys),
                where(file, keys), integer(file, keys, "weight", 0), flag(keys, "innerSeams"), random,
                overlay(file, keys), overrides(file, keys), value(keys, "name").orElse(""));
    }

    /** Where a tile of a rule read from {@code file} is: {@code namespace:path}, its path ending in {@code .png}. */
    public static String tileFile(final String file, final OptifineRule.Tile tile) {
        final String namespace = file.contains(":") ? file.substring(0, file.indexOf(':')) : DEFAULT_NAMESPACE;
        final String path = file.contains(":") ? file.substring(file.indexOf(':') + 1) : file;
        final String folder = path.contains("/") ? path.substring(0, path.lastIndexOf('/') + 1) : "";
        return switch (tile.kind()) {
            case IN_FOLDER -> namespace + ":" + folder + withPng(tile.value());
            case PATH -> {
                final String value = tile.value().startsWith(TILDE) ? OPTIFINE_FOLDER
                        + tile.value().substring(TILDE.length()) : tile.value();
                yield value.contains(":") ? withPng(value) : DEFAULT_NAMESPACE + ":" + withPng(value);
            }
            case SKIP, DEFAULT -> "";
        };
    }

    /**
     * The id a tile file ({@code namespace:path.png}) is drawn by: a texture's own id for a file under
     * {@code textures/}, else its path without {@code .png}.
     */
    public static String spriteOf(final String tileFile) {
        final int colon = tileFile.indexOf(':');
        final String namespace = tileFile.substring(0, colon);
        String path = tileFile.substring(colon + 1);
        if (path.endsWith(PNG)) {
            path = path.substring(0, path.length() - PNG.length());
        }
        if (path.startsWith(TEXTURES)) {
            path = path.substring(TEXTURES.length());
        }
        return namespace + ":" + path;
    }

    /** A texture's id as a file names it: {@code glass} is {@code minecraft:block/glass}, a path is kept. */
    public static String tileId(final String word) {
        String value = word;
        String namespace = DEFAULT_NAMESPACE;
        if (value.contains(":")) {
            namespace = value.substring(0, value.indexOf(':'));
            value = value.substring(value.indexOf(':') + 1);
        }
        if (value.startsWith(TEXTURES)) {
            value = value.substring(TEXTURES.length());
        }
        if (value.endsWith(PNG)) {
            value = value.substring(0, value.length() - PNG.length());
        }
        return namespace + ":" + (value.contains("/") ? value : "block/" + value);
    }

    /** A block as a file names it, {@code namespace:name:property=value,...}, or a tag, {@code #namespace:path}. */
    public static OptifineRule.BlockFilter blockFilter(final String word) {
        if (word.startsWith("#")) {
            final String tag = word.substring(1);
            return new OptifineRule.BlockFilter(tag.contains(":") ? tag : DEFAULT_NAMESPACE + ":" + tag, true,
                    Map.of());
        }
        final List<String> id = new ArrayList<>();
        final Map<String, List<String>> properties = new LinkedHashMap<>();
        for (final String part : word.split(":")) {
            final int equals = part.indexOf('=');
            if (equals < 0) {
                id.add(part);
            } else {
                properties.put(part.substring(0, equals), List.of(part.substring(equals + 1).split(",")));
            }
        }
        final String block = id.size() >= 2 ? id.get(0) + ":" + id.get(1) : DEFAULT_NAMESPACE + ":" + id.getFirst();
        return new OptifineRule.BlockFilter(block, false, properties);
    }

    private static List<OptifineRule.Tile> tiles(final String value) {
        final List<OptifineRule.Tile> out = new ArrayList<>();
        for (final String word : value.trim().split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            final Matcher range = RANGE.matcher(word);
            if (range.matches()) {
                final int from = Integer.parseInt(range.group(1));
                final int to = Integer.parseInt(range.group(2));
                for (int i = from; i <= to; i++) {
                    out.add(new OptifineRule.Tile(OptifineRule.TileKind.IN_FOLDER, Integer.toString(i)));
                }
            } else if ("<skip>".equals(word)) {
                out.add(new OptifineRule.Tile(OptifineRule.TileKind.SKIP, ""));
            } else if ("<default>".equals(word)) {
                out.add(new OptifineRule.Tile(OptifineRule.TileKind.DEFAULT, ""));
            } else if (word.contains("/") || word.contains(":")) {
                out.add(new OptifineRule.Tile(OptifineRule.TileKind.PATH, word));
            } else {
                out.add(new OptifineRule.Tile(OptifineRule.TileKind.IN_FOLDER, word));
            }
        }
        return List.copyOf(out);
    }

    private static void requireTiles(final String file, final OptifineRule.Method method, final int count,
                                     final OptifineRule.Random random) {
        final boolean repeats = method == OptifineRule.Method.REPEAT || method == OptifineRule.Method.OVERLAY_REPEAT;
        final int needed = repeats ? random.width() * random.height() : method.tiles();
        if (repeats ? count != needed : count < needed) {
            throw new IllegalArgumentException(file + " gives " + count + " tiles where its method "
                    + method.word() + " takes " + needed);
        }
    }

    private static OptifineRule.Random random(final String file, final Properties keys) {
        final List<Integer> weights = new ArrayList<>();
        for (final String word : words(keys, "weights")) {
            weights.add(number(file, "weights", word));
        }
        final int loops = Math.clamp(integer(file, keys, "randomLoops", 0), 0, MOST_LOOPS);
        final int symmetry = switch (value(keys, "symmetry").orElse("none").toLowerCase(Locale.ROOT)) {
            case "opposite" -> TilePicks.OPPOSITE_FACES;
            case "all" -> TilePicks.ALL_FACES;
            default -> TilePicks.EACH_FACE;
        };
        return new OptifineRule.Random(weights, loops, symmetry, flag(keys, "linked"),
                Math.max(1, integer(file, keys, "width", 1)), Math.max(1, integer(file, keys, "height", 1)));
    }

    private static OptifineRule.Overlay overlay(final String file, final Properties keys) {
        return new OptifineRule.Overlay(integer(file, keys, "tintIndex", -1), value(keys, "tintBlock"),
                value(keys, "layer").orElse("cutout").toLowerCase(Locale.ROOT));
    }

    private static Map<Integer, Integer> overrides(final String file, final Properties keys) {
        final Map<Integer, Integer> out = new HashMap<>();
        for (final String key : keys.stringPropertyNames()) {
            final Matcher matcher = OVERRIDE.matcher(key);
            if (matcher.matches()) {
                out.put(Integer.parseInt(matcher.group(1)), number(file, key, keys.getProperty(key).trim()));
            }
        }
        return out;
    }

    private static int faces(final String file, final Properties keys) {
        final List<String> words = words(keys, "faces");
        if (words.isEmpty()) {
            return OptifineRule.ALL_FACES;
        }
        int faces = 0;
        for (final String word : words) {
            final Integer bits = FACES.get(word.toLowerCase(Locale.ROOT));
            if (bits == null) {
                throw new IllegalArgumentException(file + " names a face no block has: " + word);
            }
            faces |= bits;
        }
        return faces;
    }

    private static OptifineRule.Where where(final String file, final Properties keys) {
        final List<String> biomes = new ArrayList<>();
        boolean inverted = false;
        for (final String word : words(keys, "biomes")) {
            String biome = word;
            if (biome.startsWith("!")) {
                inverted = true;
                biome = biome.substring(1);
            }
            biomes.add(biome.contains(":") ? biome : DEFAULT_NAMESPACE + ":" + biome);
        }
        final List<OptifineRule.Heights> heights = new ArrayList<>();
        for (final String word : words(keys, "heights")) {
            final Matcher matcher = HEIGHTS.matcher(word);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(file + " gives heights that do not read: " + word);
            }
            final int from = height(matcher.group(1));
            heights.add(new OptifineRule.Heights(from, matcher.group(2) == null ? from : height(matcher.group(2))));
        }
        // The old way of giving one range.
        if (heights.isEmpty() && (keys.containsKey("minHeight") || keys.containsKey("maxHeight"))) {
            heights.add(new OptifineRule.Heights(integer(file, keys, "minHeight", Integer.MIN_VALUE),
                    integer(file, keys, "maxHeight", Integer.MAX_VALUE)));
        }
        return new OptifineRule.Where(biomes, inverted, heights);
    }

    private static int height(final String word) {
        return Integer.parseInt(word.replace("(", "").replace(")", ""));
    }

    private static OptifineRule.Connect connect(final String file, final String word) {
        for (final OptifineRule.Connect connect : OptifineRule.Connect.values()) {
            if (connect.word().equalsIgnoreCase(word.trim())) {
                return connect;
            }
        }
        throw new IllegalArgumentException(file + " joins by something no pack knows: " + word);
    }

    private static Optional<String> value(final Properties keys, final String key) {
        final String value = keys.getProperty(key);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value.trim());
    }

    private static List<String> words(final Properties keys, final String key) {
        return value(keys, key).map(value -> List.of(value.split("\\s+"))).orElse(List.of());
    }

    private static int integer(final String file, final Properties keys, final String key, final int otherwise) {
        return value(keys, key).map(value -> number(file, key, value)).orElse(otherwise);
    }

    private static int number(final String file, final String key, final String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException(file + " gives " + key + " as no number: " + value, e);
        }
    }

    private static boolean flag(final Properties keys, final String key) {
        return value(keys, key).map(Boolean::parseBoolean).orElse(false);
    }

    private static String baseName(final String file) {
        final String path = file.contains(":") ? file.substring(file.indexOf(':') + 1) : file;
        final String last = path.substring(path.lastIndexOf('/') + 1);
        return last.endsWith(SUFFIX) ? last.substring(0, last.length() - SUFFIX.length()) : last;
    }

    private static String withPng(final String value) {
        return value.endsWith(PNG) ? value : value + PNG;
    }

    private static Map<String, OptifineRule.Method> methods() {
        final Map<String, OptifineRule.Method> out = new HashMap<>();
        for (final OptifineRule.Method method : OptifineRule.Method.values()) {
            out.put(method.word(), method);
        }
        // The old names of MCPatcher's day, and what a file that names none means.
        out.put("glass", OptifineRule.Method.CTM);
        out.put("bookshelf", OptifineRule.Method.HORIZONTAL);
        out.put("sandstone", OptifineRule.Method.TOP);
        out.put("default", OptifineRule.Method.CTM);
        return Map.copyOf(out);
    }

    private static Map<String, Integer> faces() {
        return Map.of("bottom", 1, "down", 1, "top", 1 << 1, "up", 1 << 1, "north", 1 << 2, "south", 1 << 3,
                "west", 1 << 4, "east", 1 << 5, "sides", OptifineRule.SIDE_FACES, "all", OptifineRule.ALL_FACES);
    }
}
