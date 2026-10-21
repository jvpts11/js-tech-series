/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.pack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reads a content pack's file into what it declares. A file holds one declaration, an object with its {@code type},
 * or a list of them:
 *
 * <pre>{@code
 * [
 *   {"type": "block", "id": "mypack:copper_lamp", "name": "Copper Lamp", "strength": 3, "sound": "metal",
 *    "light": 15, "tool": "pickaxe", "needs": "stone"},
 *   {"type": "item", "id": "mypack:token", "name": "Token", "stack": 16},
 *   {"type": "material", "id": "mypack:mythril", "name": "Mythril", "kind": "metal", "colour": "#7AD7FF",
 *    "forms": ["ingot", "dust", "block"], "ores": ["stone", "deepslate"], "properties": {"jscore:density": 5200}},
 *   {"type": "processing", "id": "mypack:smelting", "name": "Smelting", "machine": "mypack:smelter"},
 *   {"type": "machine", "id": "mypack:smelter", "name": "Smelter"},
 *   {"type": "tab", "id": "mypack:main", "name": "My Pack", "icon": "mypack:token"}
 * ]
 * }</pre>
 *
 * <p>What is missing takes its default; what cannot stand stops the game loading with one paragraph naming the file,
 * the declaration and how to put it right, as a mod's own declarations do. Pure: a test reads a file with no game.
 */
public final class PackReader {

    /** The ids a pack may give: a namespace and a path, of the letters the game's ids are made of. */
    public static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    /** The longest a block's strength or resistance may be, as the game's bedrock-like blocks reach. */
    public static final float MOST_STRENGTH = 3_600_000f;
    /** The brightest light a block gives, and the largest stack an item makes. */
    public static final int BRIGHTEST = 15;
    public static final int LARGEST_STACK = 99;
    /** The sounds a block may make, by the names of the game's kinds of sound. */
    public static final Set<String> SOUNDS = Set.of("stone", "wood", "metal", "glass", "wool", "gravel", "sand",
            "grass", "copper", "deepslate", "amethyst");
    /** The tools that break a block fast, and the tiers a block may need to drop anything. */
    public static final Set<String> TOOLS = Set.of("pickaxe", "axe", "shovel", "hoe");
    public static final Set<String> TIERS = Set.of("wood", "stone", "iron", "diamond");
    /** The kinds a material may be, as the Core names them. */
    public static final Set<String> KINDS = Set.of("metal", "crystal", "gem", "nonmetal_solid", "mineral", "liquid",
            "gas", "exotic");

    private static final float STONE_STRENGTH = 1.5f;
    private static final float STONE_RESISTANCE = 6.0f;
    private static final float MACHINE_STRENGTH = 3.5f;
    private static final int FULL_STACK = 64;
    private static final Pattern COLOUR = Pattern.compile("#[0-9A-Fa-f]{6}");
    private static final int HEX = 16;

    private PackReader() {
    }

    /** What the file named {@code source} declares, in its order. */
    public static List<PackDeclaration> read(final String source, final String json) {
        final JsonElement all;
        try {
            all = JsonParser.parseString(json);
        } catch (final JsonParseException wrong) {
            throw DeclarationError.of(null, "the file " + source, "is not JSON (" + wrong.getMessage() + ")",
                    "write it as an object with its type, or a list of them");
        }
        final List<JsonObject> objects = new ArrayList<>();
        if (all.isJsonObject()) {
            objects.add(all.getAsJsonObject());
        } else if (all.isJsonArray()) {
            for (final JsonElement each : all.getAsJsonArray()) {
                if (!each.isJsonObject()) {
                    throw DeclarationError.of(null, "the file " + source, "holds a list with something not an object",
                            "make each thing of the list an object with its type");
                }
                objects.add(each.getAsJsonObject());
            }
        } else {
            throw DeclarationError.of(null, "the file " + source, "holds neither an object nor a list",
                    "write it as an object with its type, or a list of them");
        }
        final List<PackDeclaration> declared = new ArrayList<>();
        for (final JsonObject object : objects) {
            declared.add(declaration(source, object));
        }
        return List.copyOf(declared);
    }

    private static PackDeclaration declaration(final String source, final JsonObject json) {
        final String type = string(json, "type", "").toLowerCase(Locale.ROOT);
        final String id = string(json, "id", "");
        if (!ID.matcher(id).matches()) {
            throw DeclarationError.of(null, "a " + (type.isEmpty() ? "declaration" : type) + " of " + source,
                    "has the id \"" + id + "\", which is not namespace:path in lower case",
                    "give it an id such as mypack:copper_lamp");
        }
        final String what = "the " + type + " " + id + " of " + source;
        final String name = string(json, "name", "");
        if (name.isBlank()) {
            throw DeclarationError.of(null, what, "has no name", "give it one, \"name\": \"Copper Lamp\"");
        }
        return switch (type) {
            case "block" -> block(what, id, name, json);
            case "item" -> new PackDeclaration.Item(id, name, whole(json, "stack", FULL_STACK, 1, LARGEST_STACK,
                    what));
            case "material" -> material(what, id, name, json);
            case "processing" -> new PackDeclaration.Processing(id, name, optionalId(json, "machine", what));
            case "machine" -> new PackDeclaration.Machine(id, name, decimal(json, "strength", MACHINE_STRENGTH,
                    what));
            case "tab" -> new PackDeclaration.Tab(id, name, optionalId(json, "icon", what));
            default -> throw DeclarationError.of(null, what, "has the type \"" + type + "\", which is none a pack "
                    + "declares", "give it one of block, item, material, processing, machine and tab");
        };
    }

    private static PackDeclaration block(final String what, final String id, final String name,
                                         final JsonObject json) {
        final float strength = decimal(json, "strength", STONE_STRENGTH, what);
        final float resistance = decimal(json, "resistance", Math.max(strength, STONE_RESISTANCE), what);
        return new PackDeclaration.Block(id, name, strength, resistance,
                oneOf(json, "sound", "stone", SOUNDS, what), whole(json, "light", 0, 0, BRIGHTEST, what),
                oneOf(json, "tool", "", TOOLS, what), oneOf(json, "needs", "", TIERS, what),
                !json.has("item") || json.get("item").getAsBoolean());
    }

    private static PackDeclaration material(final String what, final String id, final String name,
                                            final JsonObject json) {
        final String kind = oneOf(json, "kind", "metal", KINDS, what);
        final String colour = string(json, "colour", "#FFFFFF");
        if (!COLOUR.matcher(colour).matches()) {
            throw DeclarationError.of(null, what, "has the colour \"" + colour + "\", which is not #RRGGBB",
                    "write it as #7AD7FF");
        }
        final Map<String, JsonElement> properties = new LinkedHashMap<>();
        if (json.has("properties")) {
            if (!json.get("properties").isJsonObject()) {
                throw DeclarationError.of(null, what, "has properties that are not an object",
                        "write them as {\"jscore:density\": 5200}");
            }
            json.getAsJsonObject("properties").entrySet().forEach(each -> properties.put(each.getKey(),
                    each.getValue()));
        }
        return new PackDeclaration.Material(id, name, kind, Integer.parseInt(colour.substring(1), HEX),
                json.has("liquid") && json.get("liquid").getAsBoolean(), strings(json, "forms", what),
                strings(json, "ores", what), properties);
    }

    private static String string(final JsonObject json, final String key, final String otherwise) {
        return json.has(key) && json.get(key).isJsonPrimitive() ? json.get(key).getAsString() : otherwise;
    }

    private static String optionalId(final JsonObject json, final String key, final String what) {
        final String id = string(json, key, "");
        if (!id.isEmpty() && !ID.matcher(id).matches()) {
            throw DeclarationError.of(null, what, "names \"" + id + "\" as its " + key + ", which is not "
                    + "namespace:path", "name it by its id, such as mypack:smelter");
        }
        return id;
    }

    private static String oneOf(final JsonObject json, final String key, final String otherwise,
                                final Set<String> allowed, final String what) {
        final String value = string(json, key, otherwise).toLowerCase(Locale.ROOT);
        if (!value.isEmpty() && !allowed.contains(value)) {
            throw DeclarationError.of(null, what, "has the " + key + " \"" + value + "\", which is none the Core "
                    + "knows", "give it one of " + String.join(", ", allowed.stream().sorted().toList()));
        }
        return value;
    }

    private static float decimal(final JsonObject json, final String key, final float otherwise,
                                 final String what) {
        final float value = json.has(key) ? json.get(key).getAsFloat() : otherwise;
        if (value < 0 || value > MOST_STRENGTH || Float.isNaN(value)) {
            throw DeclarationError.of(null, what, "has the " + key + " " + value + ", outside 0 to " + MOST_STRENGTH,
                    "give it a number in between: stone is 1.5, iron 5");
        }
        return value;
    }

    private static int whole(final JsonObject json, final String key, final int otherwise, final int least,
                             final int most, final String what) {
        final int value = json.has(key) ? json.get(key).getAsInt() : otherwise;
        if (value < least || value > most) {
            throw DeclarationError.of(null, what, "has the " + key + " " + value + ", outside " + least + " to "
                    + most, "give it a whole number in between");
        }
        return value;
    }

    private static List<String> strings(final JsonObject json, final String key, final String what) {
        if (!json.has(key)) {
            return List.of();
        }
        if (!json.get(key).isJsonArray()) {
            throw DeclarationError.of(null, what, "has " + key + " that are not a list",
                    "write them as a list, [\"ingot\", \"dust\"]");
        }
        final List<String> out = new ArrayList<>();
        final JsonArray array = json.getAsJsonArray(key);
        array.forEach(each -> out.add(each.getAsString()));
        return out;
    }
}
